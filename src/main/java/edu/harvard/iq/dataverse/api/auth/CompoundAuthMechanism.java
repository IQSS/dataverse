package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Guillermo Portas
 * Compound authentication mechanism that attempts to authenticate a user through the different authentication mechanisms (ordered by priority) of which it is composed.
 * If no user is returned from any of the inner authentication mechanisms, a Guest user is returned.
 */
public class CompoundAuthMechanism implements AuthMechanism {

    private final List<AuthMechanism> authMechanisms = new ArrayList<>();

    @Inject
    public CompoundAuthMechanism(ApiKeyAuthMechanism apiKeyAuthMechanism, WorkflowKeyAuthMechanism workflowKeyAuthMechanism, SignedUrlAuthMechanism signedUrlAuthMechanism, BearerTokenAuthMechanism bearerTokenAuthMechanism, SessionCookieAuthMechanism sessionCookieAuthMechanism) {
        // Auth mechanisms should be ordered by priority here
        add(apiKeyAuthMechanism, workflowKeyAuthMechanism, signedUrlAuthMechanism, bearerTokenAuthMechanism, sessionCookieAuthMechanism);
    }

    public CompoundAuthMechanism(AuthMechanism... authMechanisms) {
        add(authMechanisms);
    }

    public void add(AuthMechanism... authMechanisms) {
        this.authMechanisms.addAll(Arrays.asList(authMechanisms));
    }

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        containerRequestContext.setProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM, ApiConstants.AUTH_MECHANISM_NONE);
        User user = null;
        for (AuthMechanism authMechanism : authMechanisms) {
            User userFromRequest = authMechanism.findUserFromRequest(containerRequestContext);
            if (userFromRequest != null) {
                user = userFromRequest;
                containerRequestContext.setProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM, getAuthMechanismTag(authMechanism));
                break;
            }
        }
        if (user == null) {
            user = GuestUser.get();
        }
        return user;
    }

    private String getAuthMechanismTag(AuthMechanism authMechanism) {
        if (authMechanism instanceof ApiKeyAuthMechanism) {
            return ApiConstants.AUTH_MECHANISM_API_KEY;
        }
        if (authMechanism instanceof WorkflowKeyAuthMechanism) {
            return ApiConstants.AUTH_MECHANISM_WORKFLOW_KEY;
        }
        if (authMechanism instanceof SignedUrlAuthMechanism) {
            return ApiConstants.AUTH_MECHANISM_SIGNED_URL;
        }
        if (authMechanism instanceof BearerTokenAuthMechanism) {
            return ApiConstants.AUTH_MECHANISM_BEARER_TOKEN;
        }
        if (authMechanism instanceof SessionCookieAuthMechanism) {
            return ApiConstants.AUTH_MECHANISM_SESSION_COOKIE;
        }
        return ApiConstants.AUTH_MECHANISM_NONE;
    }
}
