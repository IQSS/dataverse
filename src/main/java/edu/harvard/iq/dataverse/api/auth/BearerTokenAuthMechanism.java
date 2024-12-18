package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.FeatureFlags;

import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.api.auth.AuthUtil.extractBearerTokenFromHeaderParam;

public class BearerTokenAuthMechanism implements AuthMechanism {
    private static final Logger logger = Logger.getLogger(BearerTokenAuthMechanism.class.getCanonicalName());

    @Inject
    protected AuthenticationServiceBean authSvc;
    @Inject
    protected UserServiceBean userSvc;

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        if (!FeatureFlags.API_BEARER_AUTH.enabled()) {
            return null;
        }

        Optional<String> bearerToken = getRequestBearerToken(containerRequestContext);
        if (bearerToken.isEmpty()) {
            return null;
        }

        AuthenticatedUser authUser;
        try {
            authUser = authSvc.lookupUserByOIDCBearerToken(bearerToken.get());
        } catch (AuthorizationException e) {
            logger.log(Level.WARNING, "Authorization failed: {0}", e.getMessage());
            throw new WrappedUnauthorizedAuthErrorResponse(e.getMessage());
        }

        if (authUser == null) {
            logger.log(Level.WARNING, "Bearer token detected, OIDC provider validated the token but no linked UserAccount");
            throw new WrappedForbiddenAuthErrorResponse(BundleUtil.getStringFromBundle("bearerTokenAuthMechanism.errors.tokenValidatedButNoRegisteredUser"));
        }

        return userSvc.updateLastApiUseTime(authUser);
    }

    /**
     * Retrieve the raw, encoded token value from the Authorization Bearer HTTP header as defined in RFC 6750
     *
     * @return An {@link Optional} either empty if not present or the raw token from the header
     */
    public static Optional<String> getRequestBearerToken(ContainerRequestContext containerRequestContext) {
        String headerParamBearerToken = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        return extractBearerTokenFromHeaderParam(headerParamBearerToken);
    }
}
