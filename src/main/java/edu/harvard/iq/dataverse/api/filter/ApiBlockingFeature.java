package edu.harvard.iq.dataverse.api.filter;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.util.StringUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A DynamicFeature that registers ApiBlockingFilter for Dataverse API endpoints.
 * This approach avoids the need to inject ResourceInfo at request time, which
 * is no longer supported via @Context in Jakarta EE 11 / Payara 7.
 */
@Provider
public class ApiBlockingFeature implements DynamicFeature {

    private static final Logger logger = Logger.getLogger(ApiBlockingFeature.class.getName());

    @Inject
    private SettingsServiceBean settingsService;

    @Inject
    private PasswordValidatorServiceBean passwordValidatorService;

    @Inject
    private HttpServletRequest httpServletRequest;

    // Policies
    private static final String DROP = "drop";
    private static final String LOCALHOST_ONLY = "localhost-only";
    private static final String UNBLOCK_KEY = "unblock-key";

    private static final Map<String, String> POLICY_ERROR_MESSAGES = new HashMap<>();
    static {
        POLICY_ERROR_MESSAGES.put(DROP, "Endpoint blocked. Access denied.");
        POLICY_ERROR_MESSAGES.put(LOCALHOST_ONLY, "Endpoint restricted to localhost access only.");
        POLICY_ERROR_MESSAGES.put(UNBLOCK_KEY, "Endpoint requires an unblock key for access.");
    }

    private String policy = null;
    private JsonObject errorJson = null;
    private volatile List<Pattern> blockedApiEndpointPatterns = new ArrayList<>();
    private String key;

    // If any of the JvmSettings are not set, revert to checking the db settings on every call
    private boolean checkSettings = false;
    private String endpointList = null;

    @PostConstruct
    public void init() {
        // Check JvmSettings first for BlockedApiPolicy
        policy = JvmSettings.API_BLOCKED_POLICY.lookupOptional().orElse(settingsService.getValueForKey(SettingsServiceBean.Key.BlockedApiPolicy, DROP));

        if(!(DROP.equals(policy) || LOCALHOST_ONLY.equals(policy) || UNBLOCK_KEY.equals(policy))) {
            logger.severe("Invalid BlockedApiPolicy setting: " + policy + ". Using policy 'drop'");
            policy = DROP;
        }
        Optional<String> jvmEndpointList = JvmSettings.API_BLOCKED_ENDPOINTS.lookupOptional();
        if (!jvmEndpointList.isPresent()) {
            checkSettings = true;
        }
        endpointList = jvmEndpointList
                .orElse(settingsService.getValueForKey(SettingsServiceBean.Key.BlockedApiEndpoints, ""));
        logger.info("Using policy: " + policy + " to block API endpoints: " + endpointList);
        if (!(endpointList.contains("admin") && endpointList.contains("builtin-users"))) {
            logger.warning(
                    "Not blocking admin and builtin-user endpoints is a security issue unless you are blocking them in an external proxy.");
        }
        if (UNBLOCK_KEY.equals(policy)) {
            Optional<String> jvmKey = JvmSettings.API_BLOCKED_KEY.lookupOptional();
            if (!jvmKey.isPresent()) {
                checkSettings = true;
            }
            key = jvmKey.orElse(settingsService.getValueForKey(SettingsServiceBean.Key.BlockedApiKey));
            if (StringUtil.isBlank(key)) {
                logger.severe(
                        "Using unblock-key policy and no unblock key found in JvmSettings.API_BLOCKED_KEY or SettingsService.BlockedApiKey");
            } else if (passwordValidatorService.validate(key).size() == 0) {
                logger.warning("Weak unblock key detected. Please use a stronger key for better security.");
            }
        }
        updateBlockedPoints(endpointList);
        if(checkSettings) {
            logger.warning("Not all required dataverse.api.blocked.* settings not found. Dataverse use deprecated db settings and check for updates on every API call.");
        }
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Class<?> clazz = resourceInfo.getResourceClass();
        Method method = resourceInfo.getResourceMethod();

        if (clazz.getName().startsWith("edu.harvard.iq.dataverse")) {
            String classPath = "";
            String methodPath = "";

            if (clazz.isAnnotationPresent(Path.class)) {
                classPath = clazz.getAnnotation(Path.class).value();
            }

            if (method.isAnnotationPresent(Path.class)) {
                methodPath = method.getAnnotation(Path.class).value();
            }

            String fullPath = (classPath + "/" + methodPath).replaceAll("//", "/");
            context.register(new ApiBlockingFilter(this, fullPath));
        }
    }

    public boolean shouldBlock(String fullPath, ContainerRequestContext requestContext) {
        if (checkSettings) {
            updateSettingsIfChanged();
        }

        boolean isBlockableEndpoint = false;
        List<Pattern> currentPatterns = blockedApiEndpointPatterns;
        for (Pattern blockedEndpointPattern : currentPatterns) {
            if (blockedEndpointPattern.matcher(fullPath).matches()) {
                isBlockableEndpoint = true;
                break;
            }
        }
        
        if (!isBlockableEndpoint) {
            return false;
        }

        // Blockable endpoint - now check policy
        return isBlocked(policy, requestContext);
    }

    private synchronized void updateSettingsIfChanged() {
        // Backward compatibility, e.g. for setup scripts, dev environments where
        // dynamic update from the db settings is expected
        String newPolicy = settingsService.getValueForKey(SettingsServiceBean.Key.BlockedApiPolicy,
                JvmSettings.API_BLOCKED_POLICY.lookupOptional().orElse(DROP));
        String newEndpointList = settingsService.getValueForKey(SettingsServiceBean.Key.BlockedApiEndpoints,
                JvmSettings.API_BLOCKED_ENDPOINTS.lookupOptional().orElse(""));
        
        boolean changed = false;
        if (!newPolicy.equals(policy)) {
            policy = newPolicy;
            changed = true;
        }
        if (!endpointList.equals(newEndpointList)) {
            endpointList = newEndpointList;
            changed = true;
        }

        if (changed) {
            updateBlockedPoints(endpointList);
        }

        if (policy.equals(UNBLOCK_KEY)) {
            key = settingsService.getValueForKey(SettingsServiceBean.Key.BlockedApiKey,
                    JvmSettings.API_BLOCKED_KEY.lookupOptional().orElse(""));
            if (StringUtil.isBlank(key)) {
                logger.severe(
                        "Using unblock-key policy and no unblock key found in JvmSettings.API_BLOCKED_KEY or SettingsService.BlockedApiKey");
            }
        }
    }

    private boolean isBlocked(String policy, ContainerRequestContext requestContext) {
        switch (policy) {
            case DROP:
                return true;
            case LOCALHOST_ONLY:
                if (httpServletRequest == null) {
                    logger.warning("Unable to obtain HttpServletRequest");
                    return true;
                }
                IpAddress origin = new DataverseRequest(null, httpServletRequest).getSourceAddress();
                if (!origin.isLocalhost()) {
                    return true;
                }
                break;
            case UNBLOCK_KEY:
                String providedKey = requestContext.getHeaderString(ApiBlockingFilter.UNBLOCK_KEY_HEADER);
                if (StringUtil.isBlank(providedKey)) {
                    providedKey = requestContext.getUriInfo().getQueryParameters().getFirst(ApiBlockingFilter.UNBLOCK_KEY_QUERYPARAM);
                }
                // Must have a non-blank key defined and the query param must match it
                if (StringUtil.isNotBlank(key) && key.equals(providedKey)) {
                    return false;
                }
                return true;
        }
        return false;
    }

    private void updateBlockedPoints(String endpointList) {
        List<Pattern> newPatterns = new ArrayList<>();

        String currentErrorMessage = POLICY_ERROR_MESSAGES.getOrDefault(policy,
                "Endpoint blocked. Please contact the dataverse administrator.");

        errorJson = JsonUtil.createObjectBuilder().add("status", "error").add("message", currentErrorMessage).build();

        for (String endpoint : endpointList.split(",")) {
            String endpointPrefix = canonicalize(endpoint);
            if (!endpointPrefix.isEmpty()) {
                logger.log(Level.INFO, "Blocking API endpoint: {0}", endpointPrefix);
                newPatterns.add(Pattern.compile(convertPathToRegex(endpointPrefix)));
            }
        }
        blockedApiEndpointPatterns = newPatterns;
    }

    private String convertPathToRegex(String path) {
        return "^" + path.replaceAll("\\{[^}]+\\}", "[^/]+").replace("/", "\\/") + "(\\/.*)?$";
    }

    private String canonicalize(String in) {
        in = in.trim();
        if (in.startsWith("/")) {
            in = in.substring(1);
        }
        if (in.endsWith("/")) {
            in = in.substring(0, in.length() - 1);
        }
        return in;
    }

    public JsonObject getErrorJson() {
        return errorJson;
    }
}
