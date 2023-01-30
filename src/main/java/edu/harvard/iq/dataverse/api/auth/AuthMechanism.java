package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.authorization.users.User;

import javax.ws.rs.container.ContainerRequestContext;

/**
 * @author Guillermo Portas
 * This interface defines the common behavior for any kind of Dataverse API authentication mechanism.
 * Any implementation must correspond to a particular Dataverse API authentication credential type.
 */
interface AuthMechanism {

    /**
     * Returns the user associated with a particular authentication credential provided in a request.
     * If the credential is not provided, it is expected to return a null user.
     * If the credential is provided, but is invalid, it will throw a WrappedAuthErrorResponse exception.
     *
     * @param containerRequestContext a ContainerRequestContext implementation.
     * @return a user that can be null.
     * @throws edu.harvard.iq.dataverse.api.auth.WrappedAuthErrorResponse if there is a credential provided, but invalid.
     */
    User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse;
}
