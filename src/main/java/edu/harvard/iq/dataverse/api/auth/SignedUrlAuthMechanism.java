package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.*;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

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
    @Inject
    protected PrivateUrlServiceBean privateUrlSvc;
    
    private static final Logger logger = Logger.getLogger(SignedUrlAuthMechanism.class.getCanonicalName());

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        String signedUrlRequestParameter = getSignedUrlRequestParameter(containerRequestContext);
        if (signedUrlRequestParameter == null) {
            return null;
        }
        User user = getAuthenticatedUserFromSignedUrl(containerRequestContext);
        if (user != null) {
            return user;
        }
        throw new WrappedUnauthorizedAuthErrorResponse(RESPONSE_MESSAGE_BAD_SIGNED_URL);
    }

    private String getSignedUrlRequestParameter(ContainerRequestContext containerRequestContext) {
        return containerRequestContext.getUriInfo().getQueryParameters().getFirst(SIGNED_URL_TOKEN);
    }

    private User getAuthenticatedUserFromSignedUrl(ContainerRequestContext containerRequestContext) {
        User user = null;
        // Without a signing secret we never issue signed URLs (signUrlWithApiKey refuses on the sign
        // side), so we must not accept them here either. Otherwise a bare API token - or, for a guest,
        // the public request URL - would be enough to forge a URL whose signature validates against the
        // "" + token key computed below. Reject so findUserFromRequest returns the standard 401.
        if (!UrlSignerUtil.isSigningSecretConfigured()) {
            logger.warning("Rejecting signed URL authentication: no signing secret configured (dataverse.api.signing-secret).");
            return null;
        }
        // The signedUrl contains a param telling which user this is supposed to be for.
        // We don't trust this. So we look up that user, and get their API key, and use
        // that as a secret in validating the signedURL. If the signature can't be
        // validated with their key, the user (or their API key) has been changed, and
        // we reject the request.
        // If User is Guest we can return a generic guest user with key made from URI
        UriInfo uriInfo = containerRequestContext.getUriInfo();
        String userId = uriInfo.getQueryParameters().getFirst(SIGNED_URL_USER);
        if (userId == null) {
            // A token param was present (that is why this mechanism ran) but no user param: this can
            // never be a URL we signed, and dereferencing userId below would throw a NullPointerException.
            return null;
        }
        User targetUser = null;
        ApiToken userApiToken = null;
        if (userId.equalsIgnoreCase("guest")) {
            targetUser = GuestUser.get();
            userApiToken = new ApiToken();
            userApiToken.setTokenString(uriInfo.getAbsolutePath().toASCIIString()); //TODO find a better one for here and in Access.java
        } else if (!userId.startsWith(PrivateUrlUser.PREFIX)) {
            targetUser = authSvc.getAuthenticatedUser(userId);
            userApiToken = authSvc.findApiTokenByUser((AuthenticatedUser) targetUser);
        } else {
            PrivateUrl privateUrl = privateUrlSvc.getPrivateUrlFromDatasetId(Long.parseLong(userId.substring(PrivateUrlUser.PREFIX.length())));
            userApiToken = new ApiToken();
            userApiToken.setTokenString(privateUrl.getToken());
            targetUser = privateUrlSvc.getPrivateUrlUserFromToken(privateUrl.getToken());
        }
        if (targetUser != null && userApiToken != null) {
            String signedUrl = URLDecoder.decode(uriInfo.getRequestUri().toString(), StandardCharsets.UTF_8);
            
            logger.fine("Original URL: " + containerRequestContext.getUriInfo().getRequestUri().toString());
            String forwardedProto = containerRequestContext.getHeaderString("X-Forwarded-Proto");
            logger.fine("X-Forwarded-Proto is: " + forwardedProto);
            

            if (forwardedProto != null && !forwardedProto.isEmpty()) {
                if ("https".equalsIgnoreCase(forwardedProto) && signedUrl.toLowerCase().startsWith("http:")) {
                    signedUrl = "https" + signedUrl.substring(4);
                }
            }

            String requestMethod = containerRequestContext.getMethod();
            String signedUrlSigningKey = UrlSignerUtil.getApiSigningKey(userApiToken.getTokenString());
            boolean isSignedUrlValid = UrlSignerUtil.isValidUrl(signedUrl, userId, requestMethod, signedUrlSigningKey);
            if (isSignedUrlValid) {
                user = targetUser;
            }
        }
        return user;
    }
}
