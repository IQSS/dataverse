package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.*;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import edu.harvard.iq.dataverse.util.signing.ApiSigningSecretServiceBean;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.UrlSignerUtil.SIGNED_URL_TOKEN;
import static edu.harvard.iq.dataverse.util.UrlSignerUtil.SIGNED_URL_USER;

/**
 * @author Guillermo Portas
 * Authentication mechanism that attempts to authenticate a user from a Signed URL provided in an API request.
 */
public class SignedUrlAuthMechanism implements AuthMechanism {

    public static final String RESPONSE_MESSAGE_BAD_SIGNED_URL = "Bad signed URL";

    protected AuthenticationServiceBean authSvc;
    protected PrivateUrlServiceBean privateUrlSvc;
    protected ApiSigningSecretServiceBean signingSecretSvc;

    // @Inject on a constructor is preferred over @Inject on the ServiceBeans. This change has not yet been made to the other AUthMechanism implementations
    @Inject
    public SignedUrlAuthMechanism(AuthenticationServiceBean authSvc, PrivateUrlServiceBean privateUrlSvc,
            ApiSigningSecretServiceBean signingSecretSvc) {
        this.authSvc = authSvc;
        this.privateUrlSvc = privateUrlSvc;
        this.signingSecretSvc = signingSecretSvc;
    }

    SignedUrlAuthMechanism() {
        // tests assign the collaborators directly
        //ToDo: remove this constructor (only used for tests). Once it's gone and tests are updated, the *ServiceBean members can be made final
    }
    
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
            // The user param is attacker-controlled: a non-numeric suffix or a dataset without a
            // private URL must yield the standard 401, not an unhandled 500.
            PrivateUrl privateUrl = null;
            try {
                privateUrl = privateUrlSvc.getPrivateUrlFromDatasetId(Long.parseLong(userId.substring(PrivateUrlUser.PREFIX.length())));
            } catch (NumberFormatException e) {
                return null;
            }
            if (privateUrl == null) {
                return null;
            }
            userApiToken = new ApiToken();
            userApiToken.setTokenString(privateUrl.getToken());
            targetUser = privateUrlSvc.getPrivateUrlUserFromToken(privateUrl.getToken());
        }
        if (targetUser != null && userApiToken != null) {
            String rawUrl = uriInfo.getRequestUri().toString();
            logger.log(Level.FINE, "Original URL: {0}", rawUrl);
            String forwardedProto = containerRequestContext.getHeaderString("X-Forwarded-Proto");
            logger.log(Level.FINE, "X-Forwarded-Proto is: {0}", forwardedProto);
            rawUrl = applyForwardedProto(rawUrl, forwardedProto);

            String requestMethod = containerRequestContext.getMethod();
            String signedUrlSigningKey = signingSecretSvc.getSigningKey(userApiToken.getTokenString());
            if (isSignedUrlValid(rawUrl, userId, requestMethod, signedUrlSigningKey)) {
                user = targetUser;
            }
        }
        return user;
    }

    // Primary contract: the signature is checked against the exact bytes on the wire, so a URL that
    // was signed in the very form the client presents it - percent-escapes included - works verbatim,
    // with no client-side decoding or reconstruction. The fallback (the only behavior before 6.11)
    // covers URLs signed in their URL-decoded form and presented as an encoded variant; it also
    // absorbs clients and proxies that re-encode characters in flight (every signed URL carries ':'
    // in its "until" timestamp, a favorite of normalizing HTTP stacks).
    private static boolean isSignedUrlValid(String rawUrl, String userId, String requestMethod, String signingKey) {
        if (UrlSignerUtil.isValidUrl(rawUrl, userId, requestMethod, signingKey)) {
            return true;
        }
        try {
            String decoded = URLDecoder.decode(rawUrl, StandardCharsets.UTF_8);
            // When decoding is the identity (no %-escapes or '+'), the fallback would hash the same
            // bytes again; skip it.
            return !decoded.equals(rawUrl) && UrlSignerUtil.isValidUrl(decoded, userId, requestMethod, signingKey);
        } catch (IllegalArgumentException e) {
            // Not URL-decodable (e.g. a bare '%'): there is no decoded variant to check against.
            logger.fine("Signed URL is not URL-decodable, skipping the decoded-form check: " + e.getMessage());
            return false;
        }
    }

    // Behind a TLS-terminating proxy the request URI is http:// while the URL was signed as
    // https://; restore the original protocol before validating the signature.
    private static String applyForwardedProto(String signedUrl, String forwardedProto) {
        if ("https".equalsIgnoreCase(forwardedProto) && signedUrl.regionMatches(true, 0, "http:", 0, 5)) {
            return "https" + signedUrl.substring(4);
        }
        return signedUrl;
    }
}
