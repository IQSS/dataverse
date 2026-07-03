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
        User user = null;
        for (AuthMechanism authMechanism : authMechanisms) {
            User userFromRequest = authMechanism.findUserFromRequest(containerRequestContext);
            if (userFromRequest != null) {
                user = userFromRequest;
                // Session-cookie is the only mechanism downstream code needs to
                // recognize (AuthFilter's CSRF hardening, the token bootstrap
                // endpoint), so it is the only one tagged — the property is
                // simply absent for every other mechanism.
                if (authMechanism instanceof SessionCookieAuthMechanism) {
                    containerRequestContext.setProperty(
                            ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                            ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
                }
                break;
            }
        }
        if (user == null) {
            user = GuestUser.get();
        }
        return user;
    }

    /**
     * Whether this request was authenticated via the session cookie, per the
     * tag {@link #findUserFromRequest} set. Lives next to the code that writes
     * the tag so readers ({@code AuthFilter}, the CSRF token endpoint) share
     * one definition.
     */
    public static boolean isSessionCookieRequest(ContainerRequestContext containerRequestContext) {
        return ApiConstants.AUTH_MECHANISM_SESSION_COOKIE.equals(
                containerRequestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM));
    }
}
