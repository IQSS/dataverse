package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.authorization.users.User;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @author Guillermo Portas
 * Dedicated filter to authenticate the user requesting an API endpoint that requires user authentication.
 */
@AuthRequired
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    @Inject
    private CompoundAuthMechanism compoundAuthMechanism;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        try {
            User user = compoundAuthMechanism.findUserFromRequest(containerRequestContext);
            containerRequestContext.setProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER, user);
        } catch (WrappedAuthErrorResponse e) {
            containerRequestContext.abortWith(e.getResponse());
        }
    }
}
