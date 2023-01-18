package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

import javax.ws.rs.container.ContainerRequestContext;

interface AuthMechanism {
    AuthenticatedUser getAuthenticatedUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse;
}
