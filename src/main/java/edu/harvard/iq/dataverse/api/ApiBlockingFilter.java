package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.eclipse.jetty.util.StringUtil;

@Provider
public class ApiBlockingFilter implements ContainerRequestFilter {

    private static final Logger logger = Logger.getLogger(ApiBlockingFilter.class.getName());

    public static final String UNBLOCK_KEY_QUERYPARAM = "unblock-key";
    public static final String UNBLOCK_KEY_HEADER = "X-Dataverse-unblock-key";
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

    @Inject
    private SettingsServiceBean settingsService;

    @Inject
    private PasswordValidatorServiceBean passwordValidatorService;

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest httpServletRequest;

    private String policy = null;

    private JsonObject errorJson = null;

    private List<Pattern> blockedApiEndpointPatterns = new ArrayList<>();

    private String key;

    // If any of the JvmSettings are not set, revert to checking the db settings on
    // every call
    private boolean checkSettings = false;

    private String endpointList = null;

    @PostConstruct
    public void init() {
        // Check JvmSettings first for BlockedApiPolicy
        Optional<String> jvmPolicy = JvmSettings.API_BLOCKED_POLICY.lookupOptional();
        if (!jvmPolicy.isPresent()) {
            checkSettings = true;
        }
        policy = jvmPolicy.orElse(settingsService.getValueForKey(SettingsServiceBean.Key.BlockedApiPolicy, DROP));

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
    public void filter(ContainerRequestContext requestContext) throws IOException {

        Method method = resourceInfo.getResourceMethod();
        Class<?> clazz = resourceInfo.getResourceClass();

        String classPath = "";
        String methodPath = "";

        if (clazz.isAnnotationPresent(Path.class)) {
            classPath = clazz.getAnnotation(Path.class).value();
        }

        if (method.isAnnotationPresent(Path.class)) {
            methodPath = method.getAnnotation(Path.class).value();
        }

        if (checkSettings) {
            // Backward compatibility, e.g. for setup scripts, dev environments where
            // dynamic update from the db settings is expected
            policy = settingsService.getValueForKey(SettingsServiceBean.Key.BlockedApiPolicy,
                    JvmSettings.API_BLOCKED_POLICY.lookupOptional().orElse(DROP));
            String newEndpointList = settingsService.getValueForKey(SettingsServiceBean.Key.BlockedApiEndpoints,
                    JvmSettings.API_BLOCKED_ENDPOINTS.lookupOptional().orElse(""));
            if (!endpointList.equals(newEndpointList)) {
                endpointList = newEndpointList;
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
        String fullPath = (classPath + "/" + methodPath).replaceAll("//", "/");
        logger.fine("Full path is " + fullPath);

        boolean isBlockableEndpoint = false;
        for (Pattern blockedEndpointPattern : blockedApiEndpointPatterns) {
            if (blockedEndpointPattern.matcher(fullPath).matches()) {
                isBlockableEndpoint = true;
                break;
            }
        }
        if (!isBlockableEndpoint) {
            return;
        }
        // Blocakble endpoint - now check policy
        if (isBlocked(policy, requestContext)) {
            logger.fine("Blocked " + fullPath);
            requestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(errorJson)
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON).build());
            return;
        }
    }

    private boolean isBlocked(String policy, ContainerRequestContext requestContext) {
        switch (policy) {
        case DROP:
            return true;
        case LOCALHOST_ONLY:
            if (httpServletRequest == null) {
                logger.warning("Unable to obtain HttpServletRequest from ContainerRequestContext");
                // Handle the case where HttpServletRequest is not available
                return true;
            }
            IpAddress origin = new DataverseRequest(null, httpServletRequest).getSourceAddress();
            if (!origin.isLocalhost()) {
                return true;
            }
            break;
        case UNBLOCK_KEY:
            String providedKey = requestContext.getHeaderString(UNBLOCK_KEY_HEADER);
            if (StringUtil.isBlank(providedKey)) {
                providedKey = requestContext.getUriInfo().getQueryParameters().getFirst(UNBLOCK_KEY_QUERYPARAM);
            }
            // Must have a non-blank key defined and the query param must match it
            if (StringUtil.isNotBlank(key) && key.equals(providedKey)) {
                return false;
            }
            // Otherwise we have a blocked endpoint and the key doesn't work (not set or
            // doesn't match what's sent)
            return true;
        }
        return false;
    }

    private void updateBlockedPoints(String endpointList) {
        blockedApiEndpointPatterns.clear();

        String currentErrorMessage = POLICY_ERROR_MESSAGES.getOrDefault(policy,
                "Endpoint blocked. Please contact the dataverse administrator.");

        errorJson = Json.createObjectBuilder().add("status", "error").add("message", currentErrorMessage).build();

        for (String endpoint : endpointList.split(",")) {
            String endpointPrefix = canonicalize(endpoint);
            if (!endpointPrefix.isEmpty()) {
                logger.log(Level.INFO, "Blocking API endpoint: {0}", endpointPrefix);
                blockedApiEndpointPatterns.add(Pattern.compile(convertPathToRegex(endpointPrefix)));
            }
        }
    }

    private String convertPathToRegex(String path) {
        return "^" + path.replaceAll("\\{[^}]+\\}", "[^/]+").replace("/", "\\/") + "(\\/.*)?$";
    }

    /**
     * Creates a canonical representation of {@code in}: trimmed spaces and slashes
     * 
     * @param in the raw string
     * @return {@code in} with no trailing and leading spaces and slashes.
     */
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
}