package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UrlSignerUtilTest {

    @Test
    public void testSignAndValidate() {

        final String url = "http://localhost:8080/api/test1";
        final String get = "GET";
        final String post = "POST";

        final String user1 = "Alice";
        final String user2 = "Bob";
        final int tooQuickTimeout = -1;
        final int longTimeout = 1000;
        final String key = "abracadabara open sesame";
        final String badkey = "abracadabara open says me";

        Logger.getLogger(UrlSignerUtil.class.getName()).setLevel(Level.FINE);
        
        String signedUrl1 = UrlSignerUtil.signUrl(url, longTimeout, user1, get, key);
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl1, user1, get,  key));
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl1, user1, null, key));
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl1, null, get, key));

        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1, null, get, badkey));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1, user2, get, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1, user1, post, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1.replace(user1, user2), user1, get, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1.replace(user1, user2), user2, get, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1.replace(user1, user2), null, get, key));

        String signedUrl2 = UrlSignerUtil.signUrl(url, null, null, null, key);
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl2, null, null, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl2, null, post, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl2, user1, null, key));

        String signedUrl3 = UrlSignerUtil.signUrl(url, tooQuickTimeout, user1, get, key);

        assertFalse(UrlSignerUtil.isValidUrl(signedUrl3, user1, get, key));
    }

    @Test
    public void testSignAndValidateWithParams() {
        final String url1 = "http://localhost:8080/api/test1?p1=true&p2=test";
        final String url3 = "localhost:8080/api/test1?p1=true&p2&p3=1";
        final int longTimeout = 1000;
        final String user1 = "Alice";
        final String key = "abracadabara open sesame";

        String signedUrl1 = UrlSignerUtil.signUrl(url1, longTimeout, user1, "GET", key);
        assertTrue(signedUrl1.contains("test1?p1=true&p2=test"));
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl1, user1, "GET", key));

        // Works with a non-absolute URL and with params that have no value.
        String signedUrl3 = UrlSignerUtil.signUrl(url3, longTimeout, user1, "GET", key);
        assertTrue(signedUrl3.contains("&p2&"));
    }

    @Test
    public void testSignUrlThrowsOnReservedSigningParameters() {
        final int longTimeout = 1000;
        final String user1 = "Alice";
        final String key = "abracadabara open sesame";

        // A base URL that already contains a param the algorithm appends is a caller bug: signUrl
        // must refuse it instead of signing a different URL than the caller intended.
        String[] badUrls = new String[] {
            "http://localhost:8080/api/test1?p1=true&until=2999-01-01",
            "http://localhost:8080/api/test1?user=Fred",
            "http://localhost:8080/api/test1?p1=true&method=POST&p2=test",
            "http://localhost:8080/api/test1?p1=true&token=abracadabara",
            "http://localhost:8080/api/test1?until", // reserved name without a value
            "http://localhost:8080/api/test1?p1=true&token=abracadabara#frag",
        };
        for (String badUrl : badUrls) {
            assertThrows(IllegalArgumentException.class,
                    () -> UrlSignerUtil.signUrl(badUrl, longTimeout, user1, "GET", key),
                    "signUrl must reject a base URL already containing a signing param: " + badUrl);
        }

        // Names that merely contain a reserved name are fine, as is a reserved name inside the
        // fragment, which is not part of the query.
        UrlSignerUtil.signUrl("http://localhost:8080/api/test1?tokens=1&xtoken=2&user2=3", longTimeout, user1, "GET", key);
        UrlSignerUtil.signUrl("http://localhost:8080/api/test1#frag?token=1", longTimeout, user1, "GET", key);

        // The Dataverse request-level params "signed" and "key" are NOT the utility's concern:
        // signUrl leaves them untouched, byte for byte. Callers that must not sign them strip them
        // (Access) or reject the URL (requestSignedUrl) before calling signUrl.
        String withSignedAndKey = "http://localhost:8080/api/test1?p1=true&signed=true&key=abc";
        String signedUrl = UrlSignerUtil.signUrl(withSignedAndKey, longTimeout, user1, "GET", key);
        assertTrue(signedUrl.startsWith(withSignedAndKey + "&"));
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl, user1, "GET", key));
    }

    @Test
    public void testFindReservedParameter() {
        assertEquals("token", UrlSignerUtil.findReservedParameter(
                "http://x/api?a=1&token=y", UrlSignerUtil.signingParameters));
        assertEquals("until", UrlSignerUtil.findReservedParameter(
                "http://x/api?until", UrlSignerUtil.signingParameters));
        assertNull(UrlSignerUtil.findReservedParameter(
                "http://x/api?signed=true&key=abc", UrlSignerUtil.signingParameters));
        assertEquals("signed", UrlSignerUtil.findReservedParameter(
                "http://x/api?signed=true&key=abc", UrlSignerUtil.reservedParameters));
        assertNull(UrlSignerUtil.findReservedParameter(
                "http://x/api?tokens=1&xtoken=2&user2=3", UrlSignerUtil.reservedParameters));
        assertNull(UrlSignerUtil.findReservedParameter(
                "http://x/api", UrlSignerUtil.reservedParameters));
        assertNull(UrlSignerUtil.findReservedParameter(
                "http://x/api#frag?token=1", UrlSignerUtil.reservedParameters));
    }

    @Test
    public void testSignAndValidateSpecialCharacters() {
        final int longTimeout = 1000;
        final String user = "Alice";
        final String method = "GET";
        final String key = "abracadabara open sesame";

        // DOIs with ':' and '/', pre-encoded values, spaces, unicode and embedded URLs must all sign
        // byte-exact and validate over those exact bytes. End-to-end validation including the
        // server-side URL decoding is covered in SignedUrlAuthMechanismTest.
        String[] baseUrls = new String[] {
            "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi:10.5072/FK2/ABC123&foo=bar",
            "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi%3A10.5072%2FFK2%2FABC123",
            "http://localhost:8080/api/v1/search?q=hello%20world&persistentId=doi:10.1/2",
            "http://localhost:8080/api/v1/search?q=hello world&pid=doi:10.1/2",
            "http://localhost:8080/api/v1/search?q=café&name=測試",
            "http://localhost:8080/api/v1/redirect?url=http%3A%2F%2Fexample.com%2Ff%3Fa%3D1%26b%3D2"
        };
        for (String baseUrl : baseUrls) {
            String signedUrl = UrlSignerUtil.signUrl(baseUrl, longTimeout, user, method, key);
            // The base URL is preserved byte-for-byte in the signed URL (no re-encoding).
            assertTrue(signedUrl.startsWith(baseUrl + "&"),
                    "base URL must be preserved byte-for-byte: " + signedUrl);
            assertTrue(UrlSignerUtil.isValidUrl(signedUrl, user, method, key),
                    "signed URL should validate when used verbatim: " + signedUrl);
        }
    }

    @Test
    public void testSignedUrlIsByteExact() {
        // Byte-exact contract: the signature is over the URL as provided, so a re-encoded variant
        // must fail validation. This is the regression that URIBuilder normalization caused.
        final int longTimeout = 1000;
        final String user = "Alice";
        final String method = "GET";
        final String key = "abracadabara open sesame";

        String baseUrl = "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi:10.5072/FK2/ABC123";
        String signedUrl = UrlSignerUtil.signUrl(baseUrl, longTimeout, user, method, key);
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl, user, method, key));

        // Re-encoding ':' and '/' in the DOI changes the signed bytes, so it must be rejected.
        String reEncoded = signedUrl.replace("doi:10.5072/FK2/ABC123", "doi%3A10.5072%2FFK2%2FABC123");
        assertFalse(UrlSignerUtil.isValidUrl(reEncoded, user, method, key),
                "a re-encoded variant must not validate (byte-exact contract)");
    }

    @Test
    public void testSignAndValidateEmptyQuerySegments() {
        // Degenerate-but-legal query shapes must round-trip byte-exactly through sign + validate,
        // so suffix-reconstructing clients (signed.substring(base.length())) keep working.
        final String user = "Alice";
        final String method = "GET";
        final String key = "abracadabara open sesame";
        String[] baseUrls = new String[] {
            "http://localhost:8080/api/v1/x?&a=b",
            "http://localhost:8080/api/v1/x?a=1&",
            "http://localhost:8080/api/v1/x?a=1&&b=2",
        };
        for (String baseUrl : baseUrls) {
            String signedUrl = UrlSignerUtil.signUrl(baseUrl, 1000, user, method, key);
            assertTrue(signedUrl.startsWith(baseUrl + "&"),
                    "base URL must be preserved byte-for-byte: " + signedUrl);
            assertTrue(UrlSignerUtil.isValidUrl(signedUrl, user, method, key),
                    "signed URL should validate when used verbatim: " + signedUrl);
        }
    }

    @Test
    public void testTokenOrSecretAloneDoesNotValidate() {
        final String baseUrl = "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi:10.5072/FK2/ABC123";
        final String user = "Alice";
        final String method = "GET";
        final String secret = "test-only-signing-secret";
        final String apiKey = "some-api-token";

        String signedUrl = UrlSignerUtil.signUrl(baseUrl, 1000, user, method, secret + apiKey);

        // SignedUrlAuthMechanism reconstructs the key as <signing secret> + <api token>; the
        // signature must validate against exactly that combination and nothing weaker.
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl, user, method, secret + apiKey));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl, user, method, apiKey),
                "the API token alone must not validate a URL signed with the secret");
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl, user, method, secret),
                "the secret alone must not validate a URL signed with secret+token");
    }
}
