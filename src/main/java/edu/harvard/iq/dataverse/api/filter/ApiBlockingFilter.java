package edu.harvard.iq.dataverse.api.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Filter registered by ApiBlockingFeature to block specific Dataverse API endpoints.
 */
public class ApiBlockingFilter implements ContainerRequestFilter {

    private static final Logger logger = Logger.getLogger(ApiBlockingFilter.class.getName());

    public static final String UNBLOCK_KEY_QUERYPARAM = "unblock-key";
    public static final String UNBLOCK_KEY_HEADER = "X-Dataverse-unblock-key";

    private final ApiBlockingFeature feature;
    private final String fullPath;

    public ApiBlockingFilter(ApiBlockingFeature feature, String fullPath) {
        this.feature = feature;
        this.fullPath = fullPath;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (feature.shouldBlock(fullPath, requestContext)) {
            logger.fine("Blocked " + fullPath);
            requestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(feature.getErrorJson())
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                    .build());
        }
    }
}