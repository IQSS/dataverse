package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.FeatureFlags;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SessionCookieAuthMechanism implements AuthMechanism {
    @Inject
    DataverseSession session;

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        if (FeatureFlags.API_SESSION_AUTH.enabled() || isAccessApi(containerRequestContext)) {
            return session.getUser();
        }
        return null;
    }

    private boolean isAccessApi(ContainerRequestContext containerRequestContext) {
        if (containerRequestContext.getMethod().equals("GET")) {
            Pattern pattern = Pattern.compile("/api.*/access/"); // /api/v1/access/ or /api/access/
            Matcher matcher = pattern.matcher(containerRequestContext.getUriInfo().getAbsolutePath().toString().toLowerCase());
            return matcher.find();
        }
        return false;
    }
}
