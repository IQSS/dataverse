package edu.harvard.iq.dataverse.util;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        final String url2 = "http://localhost:8080/api/test1?p1=true&p2=test&until=2999-01-01&user=Fred&method=POST&token=abracadabara&signed=true";
        final String url3 = "localhost:8080/api/test1?p1=true&p2&until=2099-01-01";
        final int longTimeout = 1000;
        final String user1 = "Alice";
        final String key = "abracadabara open sesame";
        MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
        queryParameters.put("p1", List.of("true"));
        queryParameters.put("p2", List.of("test"));
        queryParameters.put("until", List.of("2099-01-01"));

        String signedUrl1 = UrlSignerUtil.signUrl(url1, longTimeout, user1, "GET", key);
        assertTrue(signedUrl1.contains("test1?p1=true&p2=test"));
        System.out.println(signedUrl1);

        String signedUrl2 = UrlSignerUtil.signUrl(url2, longTimeout, user1, "GET", key);
        assertTrue(signedUrl2.contains("&until=")); // contains the until param but not the bogus one passed in
        assertFalse(signedUrl2.contains("&until=2099-01-01"));
        assertTrue(signedUrl2.contains("&user=Alice")); // contains the user param but not the bogus one passed in
        assertFalse(signedUrl2.contains("&user=Fred"));
        assertTrue(signedUrl2.contains("&method=GET")); // contains the method param but not the bogus one passed in
        assertFalse(signedUrl2.contains("&method=POST"));
        assertTrue(signedUrl2.contains("&token=")); // contains the signed token param but not the bogus one passed in
        assertFalse(signedUrl2.contains("&token=abracadabara"));
        assertFalse(signedUrl2.contains("&signed")); // make sure we don't propagate the "signed" param
        System.out.println(signedUrl2);

        // This will log an error but will still return the signed url even if it's now a valid url
        // All callers of this method don't handle errors being returned, and it's highly unlikely that the url would be bad
        String signedUrl3 = UrlSignerUtil.signUrl(url3, longTimeout, user1, "GET", key);
        System.out.println(signedUrl3);
        assertTrue(signedUrl3.contains("&p2&")); // Show that this works with params that have no value
    }

    @Test
    public void testStripReservedParametersPreservesSpecialCharacters() {
        // The signature is a byte-exact MAC over the URL string, so removing the reserved signing
        // params must not re-encode anything else: a DOI's ':' and '/' must survive unchanged.
        String doiUrl = "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi:10.5072/FK2/ABC123&foo=bar";
        assertEquals(doiUrl, UrlSignerUtil.stripReservedParameters(doiUrl));

        // Reserved params (here token/user/signed) are removed; everything else is left byte-for-byte.
        String withReserved = "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi:10.5072/FK2/ABC123&token=spoofed&user=Mallory&signed=true&foo=bar";
        assertEquals("http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi:10.5072/FK2/ABC123&foo=bar",
                UrlSignerUtil.stripReservedParameters(withReserved));

        // A URL with no query string is returned unchanged.
        String noQuery = "http://localhost:8080/api/v1/datasets/:persistentId";
        assertEquals(noQuery, UrlSignerUtil.stripReservedParameters(noQuery));
    }

    @Test
    public void testSignAndValidateSpecialCharacters() {
        final int longTimeout = 1000;
        final String user = "Alice";
        final String method = "GET";
        final String key = "abracadabara open sesame";

        // DOIs (':' and '/'), pre-encoded values, spaces, unicode and embedded URLs must all sign
        // byte-exact and be accepted by isValidUrl over those exact bytes (the signing primitive;
        // end-to-end validation with the server's URLDecoder.decode is in SignedUrlAuthMechanismTest).
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
}
