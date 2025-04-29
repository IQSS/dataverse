package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@Provider
public class ApiBlockingFilter implements ContainerRequestFilter {

    private static final Logger logger = Logger.getLogger(ApiBlockingFilter.class.getName());

    public static final String UNBLOCK_KEY_QUERYPARAM = "unblock-key";
    
    private static final Map<String, String> POLICY_ERROR_MESSAGES = new HashMap<>();
    static {
        POLICY_ERROR_MESSAGES.put("drop", "Endpoint blocked. Access denied.");
        POLICY_ERROR_MESSAGES.put("localhost-only", "Endpoint restricted to localhost access only.");
        POLICY_ERROR_MESSAGES.put("unblock-key", "Endpoint requires an unblock key for access.");
    }
    
    @Inject
    private SettingsServiceBean settingsService;

    @Context
    private ResourceInfo resourceInfo;

    private String endpointList = null;
    
    private JsonObject errorJson = null;

    private List<Pattern> blockedApiEndpointPatterns = new ArrayList<>();
    
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

        String fullPath = (classPath + "/" + methodPath).replaceAll("//", "/");
        logger.info("Full path is " + fullPath);
        String newEndpointList = settingsService.getValueForKey(SettingsServiceBean.Key.BlockedApiEndpoints, "");
        if(!newEndpointList.equals(endpointList)) {
            endpointList = newEndpointList;
            updateBlockedPoints();
        }
        
        for (Pattern blockedEndpointPattern : blockedApiEndpointPatterns) {
            if (blockedEndpointPattern.matcher(fullPath).matches()) {
                requestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(errorJson)
                        .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                        .build());
                return;
            }
        }
    }
    
    private void updateBlockedPoints() {
        blockedApiEndpointPatterns.clear();
        
        String policy = settingsService.getValueForKey(SettingsServiceBean.Key.BlockedApiPolicy, "drop");
        
        String currentErrorMessage = POLICY_ERROR_MESSAGES.getOrDefault(policy, 
            "Endpoint blocked. Please contact the dataverse administrator.");
        
        errorJson = Json.createObjectBuilder()
        .add("status", "error")
        .add("message", currentErrorMessage)
        .build();

        for ( String endpoint : endpointList.split(",") ) {
            String endpointPrefix = canonicalize(endpoint);
            if ( ! endpointPrefix.isEmpty() ) {
                logger.log(Level.INFO, "Blocking API endpoint: {0}", endpointPrefix);
                blockedApiEndpointPatterns.add(Pattern.compile(convertPathToRegex(endpointPrefix)));
            }
        }
    }

    private String convertPathToRegex(String path) {
        logger.info("Pattern: " + "^" + path.replaceAll("\\{[^}]+\\}", "[^/]+").replace("/", "\\/") + "\\/.*$");
        return "^" + path.replaceAll("\\{[^}]+\\}", "[^/]+").replace("/", "\\/") + "\\/.*$";
    }
    
    /**
     * Creates a canonical representation of {@code in}: trimmed spaces and slashes
     * @param in the raw string
     * @return {@code in} with no trailing and leading spaces and slashes.
     */
    private String canonicalize( String in ) {
        in = in.trim();
        if ( in.startsWith("/") ) {
            in = in.substring(1);
        }
        if ( in.endsWith("/") ) {
            in = in.substring(0, in.length()-1);
        }
        return in;
    } 
}