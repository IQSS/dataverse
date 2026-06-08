package edu.harvard.iq.dataverse.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.joda.time.LocalDateTime;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple class to sign/validate URLs.
 *
 */
public class UrlSignerUtil {

    private static final Logger logger = Logger.getLogger(UrlSignerUtil.class.getName());

    public static final String SIGNED_URL_TOKEN="token";
    public static final String SIGNED_URL_METHOD="method";
    public static final String SIGNED_URL_USER="user";
    public static final String SIGNED_URL_UNTIL="until";
    public static final String SIGNED_URL_KEY="key"; // do not propagate the key since it's a credential
    public static final String SIGNED_URL_SIGNED="signed"; // we need to remove this when returning a signed url to prevent a loop of signing
    public static final List<String> reservedParameters = List.of(SIGNED_URL_UNTIL, SIGNED_URL_USER, SIGNED_URL_METHOD, SIGNED_URL_TOKEN, SIGNED_URL_KEY, SIGNED_URL_SIGNED);
    /**
     *
     * @param baseUrl - the URL to sign - cannot contain query params
     *                "until","user", "method", or "token"
     * @param timeout - how many minutes to make the URL valid for (note - time skew
     *                between the creator and receiver could affect the validation
     * @param user    - a string representing the user - should be understood by the
     *                creator/receiver
     * @param method  - one of the HTTP methods
     * @param key     - a secret key shared by the creator/receiver. In Dataverse
     *                this could be an APIKey (when sending URL to a tool that will
     *                use it to retrieve info from Dataverse)
     * @return - the signed URL
     */
    public static String signUrl(String baseUrl, Integer timeout, String user, String method, String key) {

        // Strip reserved signing params that may already be in the base URL, using exact-string
        // surgery rather than URIBuilder. The URL must be signed exactly as provided (the pre-6.10
        // behavior): validation reconstructs the signing string from the URL-decoded request, so
        // re-encoding here (e.g. percent-encoding ':' and '/' in DOIs) would change the signed bytes
        // and the signature would no longer match.
        baseUrl = stripReservedParameters(baseUrl);
        boolean firstParam = !baseUrl.contains("?");
        StringBuilder signedUrlBuilder = new StringBuilder(baseUrl);

        if (timeout != null) {
            LocalDateTime validTime = LocalDateTime.now();
            validTime = validTime.plusMinutes(timeout);
            validTime.toString();
            signedUrlBuilder.append(firstParam ? "?" : "&").append(SIGNED_URL_UNTIL + "=").append(validTime);
            firstParam = false;
        }
        if (user != null) {
            signedUrlBuilder.append(firstParam ? "?" : "&").append(SIGNED_URL_USER + "=").append(user);
            firstParam = false;
        }
        if (method != null) {
            signedUrlBuilder.append(firstParam ? "?" : "&").append(SIGNED_URL_METHOD + "=").append(method);
            firstParam=false;
        }
        signedUrlBuilder.append(firstParam ? "?" : "&").append(SIGNED_URL_TOKEN + "=");
        logger.fine("String to sign: " + signedUrlBuilder.toString() + "<key>");
        String signedUrl = signedUrlBuilder.toString();
        signedUrl= signedUrl + (DigestUtils.sha512Hex(signedUrl + key));
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(
                    "URL signature is " + (isValidUrl(signedUrl, user, method, key) ? "valid" : "invalid"));
        }
        return signedUrl;
    }

    /**
     * Removes the reserved signing parameters from the query, preserving the exact bytes of the path
     * and of every other parameter (unlike URIBuilder, which would re-encode and break the MAC).
     */
    static String stripReservedParameters(String baseUrl) {
        int queryStart = baseUrl.indexOf('?');
        if (queryStart < 0) {
            return baseUrl;
        }
        String path = baseUrl.substring(0, queryStart);
        String query = baseUrl.substring(queryStart + 1);
        StringBuilder kept = new StringBuilder();
        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            String name = (equals < 0) ? pair : pair.substring(0, equals);
            if (reservedParameters.contains(name)) {
                continue;
            }
            if (kept.length() > 0) {
                kept.append('&');
            }
            kept.append(pair);
        }
        return kept.length() > 0 ? path + "?" + kept : path;
    }

    /**
     * This method will only return true if the URL and parameters except the
     * "token" are unchanged from the original/match the values sent to this method,
     * and the "token" parameter matches what this method recalculates using the
     * shared key. The method also assures that the "until" timestamp is after the
     * current time.
     * 
     * @param signedUrl - the signed URL as received from Dataverse
     * @param method    - an HTTP method. If provided, the method in the URL must
     *                  match
     * @param user      - a string representing the user, if provided the value must
     *                  match the one in the url
     * @param key       - the shared secret key to be used in validation
     * @return - true if valid, false if not: e.g. the key is not the same as the
     *         one used to generate the "token" any part of the URL preceding the
     *         "token" has been altered the method doesn't match (e.g. the server
     *         has received a POST request and the URL only allows GET) the user
     *         string doesn't match (e.g. the server knows user A is logged in, but
     *         the URL is only for user B) the url has expired (was used after the
     *         until timestamp)
     */
    public static boolean isValidUrl(String signedUrl, String user, String method, String key) {
        boolean valid = true;
        try {
            URL url = new URL(signedUrl);
            List<NameValuePair> params = URLEncodedUtils.parse(url.getQuery(), StandardCharsets.UTF_8);
            String hash = null;
            String dateString = null;
            String allowedMethod = null;
            String allowedUser = null;
            for (NameValuePair nvp : params) {
                if (nvp.getName().equals(SIGNED_URL_TOKEN)) {
                    hash = nvp.getValue();
                    logger.fine("Hash: " + hash);
                }
                if (nvp.getName().equals(SIGNED_URL_UNTIL)) {
                    dateString = nvp.getValue();
                    logger.fine("Until: " + dateString);
                }
                if (nvp.getName().equals(SIGNED_URL_METHOD)) {
                    allowedMethod = nvp.getValue();
                    logger.fine("Method: " + allowedMethod);
                }
                if (nvp.getName().equals(SIGNED_URL_USER)) {
                    allowedUser = nvp.getValue();
                    logger.fine("User: " + allowedUser);
                }
            }

            int index = signedUrl.indexOf(((dateString==null && allowedMethod==null && allowedUser==null) ? "?":"&") + "token=");
            // Assuming the token is last - doesn't have to be, but no reason for the URL
            // params to be rearranged either, and this should only cause false negatives if
            // it does happen
            String urlToHash = signedUrl.substring(0, index + 7);
            logger.fine("String to hash: " + urlToHash + "<key>");
            String newHash = DigestUtils.sha512Hex(urlToHash + key);
            logger.fine("Calculated Hash: " + newHash);
            if (!hash.equals(newHash)) {
                logger.fine("Hash doesn't match");
                valid = false;
            }
            if (dateString != null && LocalDateTime.parse(dateString).isBefore(LocalDateTime.now())) {
                logger.fine("Url is expired");
                valid = false;
            }
            if (method != null && !method.equals(allowedMethod)) {
                logger.fine("Method doesn't match");
                valid = false;
            }
            if (user != null && !user.equals(allowedUser)) {
                logger.fine("User doesn't match");
                valid = false;
            }
        } catch (Throwable t) {
            // Want to catch anything like null pointers, etc. to force valid=false upon any
            // error
            logger.warning("Bad URL: " + signedUrl + " : " + t.getMessage());
            valid = false;
        }
        return valid;
    }

    public static boolean hasToken(String urlString) {
        try {
            URL url = new URL(urlString);
            List<NameValuePair> params = URLEncodedUtils.parse(url.getQuery(), StandardCharsets.UTF_8);
            for (NameValuePair nvp : params) {
                if (nvp.getName().equals(SIGNED_URL_TOKEN)) {
                    return true;
                }
            }
        } catch (MalformedURLException mue) {
            logger.fine("Bad url string: " + urlString);
        }
        return false;
    }
}
