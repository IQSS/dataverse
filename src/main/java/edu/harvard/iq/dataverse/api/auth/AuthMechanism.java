package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.authorization.users.User;

import javax.ws.rs.container.ContainerRequestContext;

interface AuthMechanism {
    User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse;
}
