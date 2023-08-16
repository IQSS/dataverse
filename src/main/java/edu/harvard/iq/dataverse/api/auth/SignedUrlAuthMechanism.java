package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static edu.harvard.iq.dataverse.util.UrlSignerUtil.SIGNED_URL_TOKEN;
import static edu.harvard.iq.dataverse.util.UrlSignerUtil.SIGNED_URL_USER;

/**
 * @author Guillermo Portas
 * Authentication mechanism that attempts to authenticate a user from a Signed URL provided in an API request.
 */
public class SignedUrlAuthMechanism implements AuthMechanism {

    public static final String RESPONSE_MESSAGE_BAD_SIGNED_URL = "Bad signed URL";

    @Inject
    protected AuthenticationServiceBean authSvc;

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        String signedUrlRequestParameter = getSignedUrlRequestParameter(containerRequestContext);
        if (signedUrlRequestParameter == null) {
            return null;
        }
        AuthenticatedUser authUser = getAuthenticatedUserFromSignedUrl(containerRequestContext);
        if (authUser != null) {
            return authUser;
        }
        throw new WrappedAuthErrorResponse(RESPONSE_MESSAGE_BAD_SIGNED_URL);
    }

    private String getSignedUrlRequestParameter(ContainerRequestContext containerRequestContext) {
        return containerRequestContext.getUriInfo().getQueryParameters().getFirst(SIGNED_URL_TOKEN);
    }

    private AuthenticatedUser getAuthenticatedUserFromSignedUrl(ContainerRequestContext containerRequestContext) {
        AuthenticatedUser authUser = null;
        // The signedUrl contains a param telling which user this is supposed to be for.
        // We don't trust this. So we lookup that user, and get their API key, and use
        // that as a secret in validating the signedURL. If the signature can't be
        // validated with their key, the user (or their API key) has been changed and
        // we reject the request.
        UriInfo uriInfo = containerRequestContext.getUriInfo();
        String userId = uriInfo.getQueryParameters().getFirst(SIGNED_URL_USER);
        AuthenticatedUser targetUser = authSvc.getAuthenticatedUser(userId);
        ApiToken userApiToken = authSvc.findApiTokenByUser(targetUser);
        if (targetUser != null && userApiToken != null) {
            String signedUrl = URLDecoder.decode(uriInfo.getRequestUri().toString(), StandardCharsets.UTF_8);
            String requestMethod = containerRequestContext.getMethod();
            String signedUrlSigningKey = JvmSettings.API_SIGNING_SECRET.lookupOptional().orElse("") + userApiToken.getTokenString();
            boolean isSignedUrlValid = UrlSignerUtil.isValidUrl(signedUrl, userId, requestMethod, signedUrlSigningKey);
            if (isSignedUrlValid) {
                authUser = targetUser;
            }
        }
        return authUser;
    }
}
