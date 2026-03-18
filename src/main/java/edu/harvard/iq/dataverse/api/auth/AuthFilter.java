package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.util.SystemConfig;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * @author Guillermo Portas
 * Dedicated filter to authenticate the user requesting an API endpoint that requires user authentication.
 */
@AuthRequired
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    private static final Logger logger = Logger.getLogger(AuthFilter.class.getCanonicalName());

    @Inject
    private CompoundAuthMechanism compoundAuthMechanism;

    @Inject
    private DataverseSession session;

    @Inject
    private SystemConfig systemConfig;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        try {
            User user = compoundAuthMechanism.findUserFromRequest(containerRequestContext);
            containerRequestContext.setProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER, user);
            applySessionAuthHardening(containerRequestContext);
        } catch (WrappedAuthErrorResponse e) {
            containerRequestContext.abortWith(e.getResponse());
        }
    }

    private void applySessionAuthHardening(ContainerRequestContext containerRequestContext)
            throws WrappedAuthErrorResponse {
        if (!FeatureFlags.API_SESSION_AUTH_HARDENING.enabled()) {
            return;
        }
        if (!isSessionCookieRequest(containerRequestContext)) {
            return;
        }

        if (!isOriginOrRefererAllowed(containerRequestContext)) {
            throw new WrappedForbiddenAuthErrorResponse(
                    "Request origin validation failed for session-cookie authentication.");
        }
        if (!isCsrfTokenValid(containerRequestContext)) {
            throw new WrappedForbiddenAuthErrorResponse(
                    "Missing or invalid CSRF token for session-cookie authentication.");
        }
    }

    private boolean isSessionCookieRequest(ContainerRequestContext containerRequestContext) {
        Object authMechanism = containerRequestContext
                .getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM);
        return ApiConstants.AUTH_MECHANISM_SESSION_COOKIE.equals(authMechanism);
    }

    private boolean isOriginOrRefererAllowed(ContainerRequestContext containerRequestContext) {
        String allowedOrigin = toOrigin(systemConfig.getDataverseSiteUrl());
        if (allowedOrigin == null) {
            logger.warning("Unable to validate Origin/Referer for session hardening: dataverse site URL is invalid.");
            return false;
        }
        String originHeader = containerRequestContext.getHeaderString("Origin");
        String refererHeader = containerRequestContext.getHeaderString("Referer");
        boolean hasOrigin = originHeader != null && !originHeader.isBlank();
        boolean hasReferer = refererHeader != null && !refererHeader.isBlank();

        if (!hasOrigin && !hasReferer) {
            return false;
        }
        if (hasOrigin && !allowedOrigin.equals(toOrigin(originHeader))) {
            return false;
        }
        if (hasReferer && !allowedOrigin.equals(toOrigin(refererHeader))) {
            return false;
        }
        return true;
    }

    private boolean isCsrfTokenValid(ContainerRequestContext containerRequestContext) {
        String requestToken = containerRequestContext.getHeaderString(ApiConstants.CSRF_TOKEN_HEADER);
        return requestToken != null && !requestToken.isBlank() && session.matchesApiCsrfToken(requestToken);
    }

    private String toOrigin(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            if (port == -1 || port == defaultPort(scheme)) {
                return scheme + "://" + host;
            }
            return scheme + "://" + host + ":" + port;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int defaultPort(String scheme) {
        if ("http".equals(scheme)) {
            return 80;
        }
        if ("https".equals(scheme)) {
            return 443;
        }
        return -1;
    }
}
