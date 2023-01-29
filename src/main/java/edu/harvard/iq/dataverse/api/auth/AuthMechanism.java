package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.authorization.users.User;

import javax.ws.rs.container.ContainerRequestContext;

/**
 * @author Guillermo Portas
 * Defines the common behavior for any kind of Dataverse API authentication mechanism.
 */
interface AuthMechanism {
    User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse;
}
