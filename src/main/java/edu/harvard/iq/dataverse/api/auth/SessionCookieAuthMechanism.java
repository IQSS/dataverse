package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.FeatureFlags;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;

public class SessionCookieAuthMechanism implements AuthMechanism {
    @Inject
    DataverseSession session;

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        if (FeatureFlags.API_SESSION_AUTH.enabled()) {
            return session.getUser();
        }
        return null;
    }
}
