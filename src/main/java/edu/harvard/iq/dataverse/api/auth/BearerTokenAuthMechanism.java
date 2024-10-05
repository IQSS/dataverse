package edu.harvard.iq.dataverse.api.auth;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;

public class BearerTokenAuthMechanism implements AuthMechanism {
    private static final String BEARER_AUTH_SCHEME = "Bearer";
    private static final Logger logger = Logger.getLogger(BearerTokenAuthMechanism.class.getCanonicalName());

    public static final String UNAUTHORIZED_BEARER_TOKEN = "Unauthorized bearer token";
    public static final String BEARER_TOKEN_DETECTED_NO_OIDC_PROVIDER_CONFIGURED = "Bearer token detected, no OIDC provider configured";

    @Inject
    protected AuthenticationServiceBean authSvc;
    @Inject
    protected UserServiceBean userSvc;

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        if (FeatureFlags.API_BEARER_AUTH.enabled()) {
            Optional<String> bearerToken = getRequestApiKey(containerRequestContext);
            // No Bearer Token present, hence no user can be authenticated
            if (bearerToken.isEmpty()) {
                return null;
            }

            // Validate and verify provided Bearer Token, and retrieve email
            UserRecordIdentifier userRecordIdentifier = getUserRecordIdentifier(bearerToken.get());

            // retrieve Authenticated User from AuthService
            AuthenticatedUser authUser = authSvc.lookupUser(userRecordIdentifier);
            if (authUser != null) {
                // track the API usage
                authUser = userSvc.updateLastApiUseTime(authUser);
                return authUser;
            } else {
                // a valid Token was presented, but we have no associated user account.
                logger.log(Level.WARNING,
                        "Bearer token detected, OIDC provider found user record identifier {0} but no linked UserAccount",
                        userRecordIdentifier);
                // TODO: Instead of returning null, we should throw a meaningful error to the
                // client. Probably this will be a wrapped auth error response with an error
                // code and a string describing the problem.
                return null;
            }
        }
        return null;
    }

    /**
     * Verifies the given Bearer token and obtain information about the
     * corresponding user within respective AuthProvider.
     *
     * @param token The string containing the encoded JWT
     * @return
     */
    private UserRecordIdentifier getUserRecordIdentifier(String token) throws WrappedAuthErrorResponse {
        // Get list of all authentication providers using Open ID Connect
        // @TASK: Limited to OIDCAuthProviders, could be widened to OAuth2Providers.
        List<OIDCAuthProvider> providers = authSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class).stream()
                .map(providerId -> (OIDCAuthProvider) authSvc.getAuthenticationProvider(providerId))
                .collect(Collectors.toUnmodifiableList());
        // If not OIDC Provider are configured we cannot validate a Token
        if (providers.isEmpty()) {
            logger.log(Level.WARNING, "Bearer token detected, no OIDC provider configured");
            throw new WrappedAuthErrorResponse(BEARER_TOKEN_DETECTED_NO_OIDC_PROVIDER_CONFIGURED);
        }

        // Iterate over all OIDC providers if multiple. Sadly needed as do not know
        // which provided the Token.
        for (OIDCAuthProvider provider : providers) {
            final UserRecordIdentifier userRecordIdentifier = provider.getUserRecordIdentifier(token);
            if (userRecordIdentifier != null) {
                logger.log(Level.FINE, "Bearer token detected, provider {0} confirmed validity and provided identifier",
                        provider.getId());
                return userRecordIdentifier;
            }
        }

        // No UserInfo returned means we have an invalid access token.
        logger.log(Level.FINE, "Bearer token detected, yet no configured OIDC provider validated it.");
        throw new WrappedAuthErrorResponse(UNAUTHORIZED_BEARER_TOKEN);
    }

    /**
     * Retrieve the raw, encoded token value from the Authorization Bearer HTTP
     * header as defined in RFC 6750
     * 
     * @return An {@link Optional} either empty if not present or the raw token from
     *         the header
     */
    private Optional<String> getRequestApiKey(ContainerRequestContext containerRequestContext) {
        String headerParamApiKey = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (headerParamApiKey != null
                && headerParamApiKey.toLowerCase().startsWith(BEARER_AUTH_SCHEME.toLowerCase() + " ")) {
            return Optional.of(headerParamApiKey);
        } else {
            return Optional.empty();
        }
    }
}