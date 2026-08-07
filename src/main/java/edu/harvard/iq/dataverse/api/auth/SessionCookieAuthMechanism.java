package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.FeatureFlags;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

public class SessionCookieAuthMechanism implements AuthMechanism {
    @Inject
    DataverseSession session;

    public static final String ACCESS_PATH_PREFIX = "access/";

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        if (FeatureFlags.API_SESSION_AUTH.enabled() || isAccessApi(containerRequestContext)) {
            return session.getUser();
        }
        return null;
    }

    private boolean isAccessApi(ContainerRequestContext containerRequestContext) {
        String requestPath = containerRequestContext.getUriInfo() != null ? containerRequestContext.getUriInfo().getPath() : "";
        return ("GET".equalsIgnoreCase(containerRequestContext.getMethod()) &&
                requestPath.startsWith(ACCESS_PATH_PREFIX));
    }
}
