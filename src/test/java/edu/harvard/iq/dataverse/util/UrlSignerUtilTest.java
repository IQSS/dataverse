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
        // and then validate when the signed URL is presented back verbatim (as the server returns it).
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
        // Documents and locks in the byte-exact contract: the signature is computed over the URL
        // string exactly as provided. A client that re-encodes the URL before presenting it back
        // (e.g. percent-encoding the DOI) must fail validation. This is the regression that broke
        // signing for special characters when the URL was normalized via URIBuilder.
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

    /**
     * Signs {@code baseUrl} and asserts that the signed URL is exactly the provided URL followed by
     * a well-formed signature (until/user/method/token), and that it validates when presented back
     * verbatim. Returns the signed URL so callers can make further (encoding) assertions on it.
     */
    private static String assertSignedExactlyAndValid(String baseUrl, String user, String key) {
        String signed = UrlSignerUtil.signUrl(baseUrl, 1000, user, "GET", key);
        // What is signed is exactly what was provided (no re-encoding/normalization of the base URL)...
        assertTrue(signed.startsWith(baseUrl),
                "what is signed must be exactly what was provided: " + baseUrl + "\n  got: " + signed);
        // ...plus only the signature parameters, appended after it.
        String signature = signed.substring(baseUrl.length());
        String separator = baseUrl.contains("?") ? "&" : "\\?";
        assertTrue(signature.matches(separator + "until=[^&]+&user=" + user + "&method=GET&token=[0-9a-f]{128}"),
                "only the signature parameters may be appended after the provided URL: " + signature);
        // ...and the signature validates when the signed URL is used verbatim (as the client does).
        assertTrue(UrlSignerUtil.isValidUrl(signed, user, "GET", key),
                "signed URL must validate when presented back verbatim: " + signed);
        return signed;
    }

    @Test
    public void testSignAndValidateRdmIntegrationUrls() {
        // Backend regression guard: sign every URL shape that rdm-integration sends through the
        // signing client and confirm (a) what is signed is exactly the URL provided plus a valid
        // signature, (b) the signature validates verbatim, and (c) the encoding is preserved exactly
        // (escaped values stay escaped, raw values stay raw). URL templates mirror rdm-integration:
        // plugin/impl/globus/streams.go, dataverse/dataverse_read.go, dataverse/dataverse_write.go,
        // plugin/impl/dataverse/query.go. If the backend ever re-encodes the URL again (the original
        // regression), these assertions fail.
        final String user = "rdmUser";
        final String key = "abracadabara open sesame";
        final String s = "https://demo.dataverse.org";
        final String pid = "doi:10.5072/FK2/ABC123";           // raw, as most rdm paths send it
        final String escPid = "doi%3A10.5072%2FFK2%2FABC123";  // url.QueryEscape form

        // Paths that send the persistentId RAW (GetNodeMap, CheckPermission, all globus flows,
        // all write flows, the dataverse plugin). The ':' and '/' in the DOI must survive unchanged.
        String[] rawPidUrls = new String[] {
            s + "/api/v1/datasets/:persistentId/versions/:latest/files?persistentId=" + pid,
            s + "/api/v1/datasets/:persistentId?persistentId=" + pid,
            s + "/api/v1/admin/permissions/:persistentId?persistentId=" + pid + "&unblock-key=UNBLOCK",
            s + "/api/v1/datasets/:persistentId/requestGlobusUploadPaths?persistentId=" + pid,
            s + "/api/v1/datasets/:persistentId/addGlobusFiles?persistentId=" + pid,
            s + "/api/v1/datasets/:persistentId/requestGlobusDownload?persistentId=" + pid,
            s + "/api/v1/datasets/:persistentId/monitorGlobusDownload?persistentId=" + pid,
            s + "/api/v1/datasets/:persistentId/globusDownloadParameters?persistentId=" + pid + "&downloadId=globus-task-123",
            s + "/api/v1/datasets/:persistentId/add?persistentId=" + pid,
            s + "/api/v1/datasets/:persistentId/addFiles?persistentId=" + pid,
            s + "/api/v1/datasets/:persistentId/replaceFiles?persistentId=" + pid,
            s + "/api/v1/datasets/:persistentId/deleteFiles?persistentId=" + pid,
            s + "/api/v1/datasets/:persistentId/cleanStorage?persistentId=" + pid,
        };
        for (String baseUrl : rawPidUrls) {
            String signed = assertSignedExactlyAndValid(baseUrl, user, key);
            assertTrue(signed.contains("persistentId=" + pid),
                    "raw persistentId must stay raw (not percent-encoded): " + signed);
            assertFalse(signed.contains(escPid),
                    "raw persistentId must not be escaped by the backend: " + signed);
        }

        // Paths that send the persistentId URL-ESCAPED (GetDatasetMetadata, GetDatasetUserPermissions).
        // The escaped value must remain escaped (the backend must not decode then re-encode it).
        String[] escapedPidUrls = new String[] {
            s + "/api/v1/datasets/:persistentId?persistentId=" + escPid + "&excludeFiles=true",
            s + "/api/v1/datasets/:persistentId/userPermissions?persistentId=" + escPid,
        };
        for (String baseUrl : escapedPidUrls) {
            String signed = assertSignedExactlyAndValid(baseUrl, user, key);
            assertTrue(signed.contains("persistentId=" + escPid),
                    "escaped persistentId must stay escaped: " + signed);
            assertFalse(signed.contains("persistentId=" + pid),
                    "escaped persistentId must not be decoded to raw: " + signed);
        }

        // mydata/retrieve: URL-escaped search term, '+' for spaces, and repeated query parameters.
        String mydata = s + "/api/v1/mydata/retrieve?selected_page=1&dvobject_types=Dataset"
                + "&published_states=Published&published_states=Unpublished&published_states=Draft"
                + "&role_ids=1&role_ids=6&mydata_search_term=text%3A%22hello+world%22";
        String signedMydata = assertSignedExactlyAndValid(mydata, user, key);
        assertTrue(signedMydata.contains("mydata_search_term=text%3A%22hello+world%22"),
                "escaped search term (including '%3A', '%22' and '+') must be preserved exactly: " + signedMydata);
        assertTrue(signedMydata.contains("published_states=Published&published_states=Unpublished&published_states=Draft"),
                "repeated query parameters must be preserved: " + signedMydata);

        // Numeric-id / no-persistentId paths (datafile, files, numeric dataset id, users/:me).
        String[] otherUrls = new String[] {
            s + "/api/v1/access/datafile/123/metadata/ddi",
            s + "/api/v1/access/datafile/123",
            s + "/api/v1/files/123",
            s + "/api/v1/users/:me",
            s + "/api/v1/datasets/42/versions/:latest?excludeFiles=true",
        };
        for (String baseUrl : otherUrls) {
            assertSignedExactlyAndValid(baseUrl, user, key);
        }

        // noSlashPermissionUrl sends an empty leading query segment ("?&unblock-key=..."); the backend
        // drops the empty segment, so it does not round-trip byte-for-byte, but it still signs and
        // validates (the client uses the returned signed URL verbatim).
        String emptySegment = s + "/api/v1/admin/permissions/42?&unblock-key=UNBLOCK";
        String signedEmptySegment = UrlSignerUtil.signUrl(emptySegment, 1000, user, "GET", key);
        assertTrue(UrlSignerUtil.isValidUrl(signedEmptySegment, user, "GET", key),
                "permissions URL with an empty leading query segment must validate: " + signedEmptySegment);
    }
}
