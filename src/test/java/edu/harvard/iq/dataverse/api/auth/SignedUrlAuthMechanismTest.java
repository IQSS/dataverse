package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.api.auth.doubles.SignedUrlContainerRequestTestFake;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.container.ContainerRequestContext;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static edu.harvard.iq.dataverse.api.auth.SignedUrlAuthMechanism.RESPONSE_MESSAGE_BAD_SIGNED_URL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@LocalJvmSettings
public class SignedUrlAuthMechanismTest {

    private static final String TEST_SIGNED_URL_TOKEN = "test-signed-url-token";
    private static final String TEST_SIGNED_URL_USER_ID = "test-user";
    // A signing secret must be configured for signed-URL authentication to be accepted at all (the
    // mechanism rejects signed URLs when none is set); the signing key is then this secret + the token.
    private static final String TEST_SIGNING_SECRET = "test-signing-secret";

    private SignedUrlAuthMechanism sut;

    private final AuthenticatedUser testAuthenticatedUser = new AuthenticatedUser();

    @BeforeEach
    public void setUp() {
        sut = new SignedUrlAuthMechanism();
    }

    @Test
    public void testFindUserFromRequest_SignedUrlTokenNotProvided() throws WrappedAuthErrorResponse {
        sut.authSvc = mock(AuthenticationServiceBean.class);

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(null, null);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertNull(actual);
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = TEST_SIGNING_SECRET)
    public void testFindUserFromRequest_SignedUrlTokenProvided_UserExists_ValidSignedUrl_UserAuthenticated() throws WrappedAuthErrorResponse {
        AuthenticationServiceBean authenticationServiceBeanStub = mock(AuthenticationServiceBean.class);
        when(authenticationServiceBeanStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        ApiToken apiTokenStub = mock(ApiToken.class);
        when(apiTokenStub.getTokenString()).thenReturn(TEST_SIGNED_URL_TOKEN);
        when(authenticationServiceBeanStub.findApiTokenByUser(testAuthenticatedUser)).thenReturn(apiTokenStub);

        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertEquals(testAuthenticatedUser, actual);
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = TEST_SIGNING_SECRET)
    public void testFindUserFromRequest_SignedUrlTokenProvided_UserExists_InvalidSignedUrl_UserNotAuthenticated() {
        AuthenticationServiceBean authenticationServiceBeanStub = mock(AuthenticationServiceBean.class);
        when(authenticationServiceBeanStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        ApiToken apiTokenStub = mock(ApiToken.class);
        when(apiTokenStub.getTokenString()).thenReturn("different-token-from-the-signed-url");
        when(authenticationServiceBeanStub.findApiTokenByUser(testAuthenticatedUser)).thenReturn(apiTokenStub);

        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);
        WrappedUnauthorizedAuthErrorResponse wrappedUnauthorizedAuthErrorResponse = assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        assertEquals(RESPONSE_MESSAGE_BAD_SIGNED_URL, wrappedUnauthorizedAuthErrorResponse.getMessage());
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = TEST_SIGNING_SECRET)
    public void testFindUserFromRequest_SignedUrlTokenProvided_UserExists_UserApiTokenDoesNotExist_UserNotAuthenticated() {
        AuthenticationServiceBean authenticationServiceBeanStub = mock(AuthenticationServiceBean.class);
        when(authenticationServiceBeanStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        when(authenticationServiceBeanStub.findApiTokenByUser(testAuthenticatedUser)).thenReturn(null);

        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);
        WrappedUnauthorizedAuthErrorResponse wrappedUnauthorizedAuthErrorResponse = assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        assertEquals(RESPONSE_MESSAGE_BAD_SIGNED_URL, wrappedUnauthorizedAuthErrorResponse.getMessage());
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = TEST_SIGNING_SECRET)
    public void testFindUserFromRequest_SignedUrlTokenProvided_UserDoesNotExistForTheGivenId_UserNotAuthenticated() {
        AuthenticationServiceBean authenticationServiceBeanStub = mock(AuthenticationServiceBean.class);
        when(authenticationServiceBeanStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(null);

        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);
        WrappedUnauthorizedAuthErrorResponse wrappedUnauthorizedAuthErrorResponse = assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        assertEquals(RESPONSE_MESSAGE_BAD_SIGNED_URL, wrappedUnauthorizedAuthErrorResponse.getMessage());
    }

    // End-to-end validation through the REAL SignedUrlAuthMechanism (URLDecoder.decode + isValidUrl),
    // which the isValidUrl-only tests in UrlSignerUtilTest do not exercise. These tests configure a
    // signing secret (required for signed-URL auth to be accepted) and sign with signUrlWithApiKey, so
    // the signing key is the secret + the API token - exactly as the mechanism reconstructs it.

    private void givenUserWithSigningKey(String key) {
        AuthenticationServiceBean authStub = mock(AuthenticationServiceBean.class);
        when(authStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        ApiToken apiToken = mock(ApiToken.class);
        when(apiToken.getTokenString()).thenReturn(key);
        when(authStub.findApiTokenByUser(testAuthenticatedUser)).thenReturn(apiToken);
        sut.authSvc = authStub;
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = TEST_SIGNING_SECRET)
    public void testEndToEnd_tamperedSignedUrl_userNotAuthenticated() {
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String base = "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi:10.5072/FK2/ABC";
        String signedUrl = UrlSignerUtil.signUrlWithApiKey(base, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNED_URL_TOKEN);
        // Alter the signed portion of the URL after signing -> the signature must no longer validate.
        String tampered = signedUrl.replace("FK2/ABC", "FK2/HACKED");

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, tampered);

        assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(request));
    }

    @Test
    public void testEndToEnd_noSigningSecret_signedUrlRejected() {
        // With no signing secret configured, a URL signed with only the bare API token would still hash
        // valid (key = "" + token) - but the mechanism must refuse it, otherwise a leaked/expired token
        // or a guest key derived from the public URL could be used to forge a signed URL.
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String base = "http://localhost:8080/api/v1/datasets/1";
        String signedUrl = UrlSignerUtil.signUrl(base, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNED_URL_TOKEN);

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, signedUrl);

        assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(request));
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = TEST_SIGNING_SECRET)
    public void testFindUserFromRequest_malformedPrivateUrlUser_standard401Not500() {
        // The user query param is attacker-controlled and reachable unauthenticated: a private-url
        // user id with a non-numeric suffix, or one whose dataset has no private URL, must produce
        // the standard 401 - not an unhandled NumberFormatException/NullPointerException 500.
        sut.authSvc = mock(AuthenticationServiceBean.class);
        sut.privateUrlSvc = mock(PrivateUrlServiceBean.class); // returns null for any dataset id

        ContainerRequestContext nonNumericSuffix = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, "!abc");
        assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(nonNumericSuffix));

        ContainerRequestContext noPrivateUrlForDataset = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, "!999999");
        assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(noPrivateUrlForDataset));
    }

    // The primary signed-URL contract: whatever URL is submitted for signing - percent-escapes, '+'
    // and all - the returned signed URL must authenticate when presented back VERBATIM. No client-side
    // decoding, re-encoding or reconstruction may be required.
    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = TEST_SIGNING_SECRET)
    public void testEndToEnd_escapedUrlSignedAndUsedVerbatim_authenticates() {
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        List<String> urls = List.of(
            // url.QueryEscape'd persistentId, as rdm-integration's userPermissions/metadata calls send it
            "http://localhost:8080/api/v1/datasets/:persistentId/userPermissions?persistentId=doi%3A10.5072%2FFK2%2FABC",
            "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi%3A10.5072%2FFK2%2FABC&excludeFiles=true",
            // escaped search term: '+' for space, %3A / %22 for ':' and '"'
            "http://localhost:8080/api/v1/mydata/retrieve?selected_page=1&mydata_search_term=text%3A%22hello+world%22",
            // a value with an escaped literal '%' - decoding this twice would corrupt it
            "http://localhost:8080/api/v1/search?q=100%2525done"
        );
        for (String url : urls) {
            String signedUrl = UrlSignerUtil.signUrlWithApiKey(url, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNED_URL_TOKEN);
            ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, signedUrl);
            try {
                assertEquals(testAuthenticatedUser, sut.findUserFromRequest(request),
                        "signed URL must authenticate when used verbatim: " + signedUrl);
            } catch (WrappedAuthErrorResponse e) {
                fail("signed URL must authenticate when used verbatim: " + signedUrl);
            }
        }
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = TEST_SIGNING_SECRET)
    public void testEndToEnd_tamperedEscapedUrlUsedVerbatim_rejected() {
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String url = "http://localhost:8080/api/v1/datasets/:persistentId/userPermissions?persistentId=doi%3A10.5072%2FFK2%2FABC";
        String signedUrl = UrlSignerUtil.signUrlWithApiKey(url, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNED_URL_TOKEN);
        String tampered = signedUrl.replace("FK2%2FABC", "FK2%2FHACKED");

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, tampered);

        assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(request));
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = TEST_SIGNING_SECRET)
    public void testEndToEnd_clientStackReEncodesSignatureParams_authenticates() {
        // Why the decoded fallback must stay: every signed URL carries ':' in its own until timestamp
        // (and clients/proxies like Apache HttpClient, OkHttp or mod_proxy may percent-encode it in
        // flight). A URL signed in decoded form and presented in a re-encoded variant must validate.
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String base = "http://localhost:8080/api/v1/datasets/42";
        String signedUrl = UrlSignerUtil.signUrlWithApiKey(base, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNED_URL_TOKEN);
        // Simulate a stack that re-encodes ':' in query values; the path is left alone.
        int queryStart = signedUrl.indexOf('?');
        String reEncoded = signedUrl.substring(0, queryStart)
                + signedUrl.substring(queryStart).replace(":", "%3A");

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, reEncoded);

        try {
            assertEquals(testAuthenticatedUser, sut.findUserFromRequest(request),
                    "re-encoded variant of a decoded-form-signed URL must still authenticate");
        } catch (WrappedAuthErrorResponse e) {
            fail("re-encoded variant of a decoded-form-signed URL must still authenticate");
        }
    }

    // Runs the real rdm flow: un-escape, sign, request the original (encoded) URL + signature, then the
    // server URL-decodes the request and checks it. Returns true iff the user authenticates.
    private boolean validatesEndToEndAsRdmClient(String urlAsClientBuilds) {
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String canonical = URLDecoder.decode(urlAsClientBuilds, StandardCharsets.UTF_8);
        String signed = UrlSignerUtil.signUrlWithApiKey(canonical, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNED_URL_TOKEN);
        String requestUri = urlAsClientBuilds + signed.substring(canonical.length());
        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, requestUri);
        try {
            return testAuthenticatedUser.equals(sut.findUserFromRequest(request));
        } catch (WrappedAuthErrorResponse e) {
            return false;
        }
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = TEST_SIGNING_SECRET)
    public void testEndToEnd_allRdmIntegrationUrls_authenticate() {
        final String s = "https://demo.dataverse.org";
        final String pid = "doi:10.5072/FK2/ABC";          // raw, as most rdm paths send it
        final String escPid = "doi%3A10.5072%2FFK2%2FABC";  // url.QueryEscape form (GetDatasetMetadata, GetDatasetUserPermissions)

        // Every URL shape rdm-integration signs - each must authenticate end to end.
        List<String> urls = List.of(
            // raw persistentId in the query (GetNodeMap, CheckPermission, globus, writes, dataverse plugin)
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
            // url-escaped persistentId (GetDatasetMetadata, GetDatasetUserPermissions)
            s + "/api/v1/datasets/:persistentId?persistentId=" + escPid + "&excludeFiles=true",
            s + "/api/v1/datasets/:persistentId/userPermissions?persistentId=" + escPid,
            // mydata/retrieve: url-escaped search term, '+' for spaces, repeated query params
            s + "/api/v1/mydata/retrieve?selected_page=1&dvobject_types=Dataset"
                + "&published_states=Published&published_states=Unpublished&published_states=Draft"
                + "&role_ids=1&role_ids=6&mydata_search_term=text%3A%22hello+world%22",
            // numeric-id / no-persistentId paths
            s + "/api/v1/access/datafile/123/metadata/ddi",
            s + "/api/v1/access/datafile/123",
            s + "/api/v1/files/123",
            s + "/api/v1/users/:me",
            s + "/api/v1/datasets/42/versions/:latest?excludeFiles=true"
        );
        for (String url : urls) {
            assertTrue(validatesEndToEndAsRdmClient(url), "signed URL must authenticate end to end: " + url);
        }
    }
}
