package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.api.auth.doubles.SignedUrlContainerRequestTestFake;
import edu.harvard.iq.dataverse.api.auth.doubles.SignedUrlUriInfoTestFake;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import edu.harvard.iq.dataverse.util.signing.FixedSigningSecret;
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

public class SignedUrlAuthMechanismTest {

    private static final String TEST_SIGNED_URL_TOKEN = "test-signed-url-token";
    private static final String TEST_SIGNED_URL_USER_ID = "test-user";
    // The signing key is the server's signing secret + the user's token.
    private static final String TEST_SIGNING_SECRET = SignedUrlUriInfoTestFake.TEST_SIGNING_SECRET;

    private SignedUrlAuthMechanism sut;

    private final AuthenticatedUser testAuthenticatedUser = new AuthenticatedUser();

    @BeforeEach
    public void setUp() {
        sut = new SignedUrlAuthMechanism();
        sut.signingSecretSvc = FixedSigningSecret.withSecret(TEST_SIGNING_SECRET);
    }

    @Test
    public void testFindUserFromRequest_SignedUrlTokenNotProvided() throws WrappedAuthErrorResponse {
        sut.authSvc = mock(AuthenticationServiceBean.class);

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(null, null);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertNull(actual);
    }

    @Test
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
    public void testFindUserFromRequest_SignedUrlTokenProvided_UserDoesNotExistForTheGivenId_UserNotAuthenticated() {
        AuthenticationServiceBean authenticationServiceBeanStub = mock(AuthenticationServiceBean.class);
        when(authenticationServiceBeanStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(null);

        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);
        WrappedUnauthorizedAuthErrorResponse wrappedUnauthorizedAuthErrorResponse = assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        assertEquals(RESPONSE_MESSAGE_BAD_SIGNED_URL, wrappedUnauthorizedAuthErrorResponse.getMessage());
    }

    // End-to-end validation through the REAL SignedUrlAuthMechanism (URLDecoder.decode + isValidUrl),
    // which the isValidUrl-only tests in UrlSignerUtilTest do not exercise. These tests sign with the
    // secret + the API token - exactly as the mechanism reconstructs the key.

    private void givenUserWithSigningKey(String key) {
        AuthenticationServiceBean authStub = mock(AuthenticationServiceBean.class);
        when(authStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        ApiToken apiToken = mock(ApiToken.class);
        when(apiToken.getTokenString()).thenReturn(key);
        when(authStub.findApiTokenByUser(testAuthenticatedUser)).thenReturn(apiToken);
        sut.authSvc = authStub;
    }

    @Test
    public void testEndToEnd_tamperedSignedUrl_userNotAuthenticated() {
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String base = "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi:10.5072/FK2/ABC";
        String signedUrl = UrlSignerUtil.signUrl(base, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNING_SECRET + TEST_SIGNED_URL_TOKEN);
        // Alter the signed portion of the URL after signing -> the signature must no longer validate.
        String tampered = signedUrl.replace("FK2/ABC", "FK2/HACKED");

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, tampered);

        assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(request));
    }

    @Test
    public void testEndToEnd_urlSignedWithBareTokenOnly_rejected() {
        // A URL signed with only the bare API token (no server secret in the key) must not validate:
        // otherwise a leaked/expired token or a guest key derived from the public URL could be used
        // to forge a signed URL without knowing the server's secret.
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String base = "http://localhost:8080/api/v1/datasets/1";
        String signedUrl = UrlSignerUtil.signUrl(base, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNED_URL_TOKEN);

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, signedUrl);

        assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(request));
    }

    // The anonymous guestbook-response download flow: there is no API token, so Access signs with
    // the secret + a key derived from the download URL itself (its path, URL-decoded), and the
    // mechanism reconstructs the same key from the request's absolute path.

    @Test
    public void testEndToEnd_guestSignedDownloadUrl_authenticatedAsGuest() throws WrappedAuthErrorResponse {
        sut.authSvc = mock(AuthenticationServiceBean.class);
        String base = "http://localhost:8080/api/access/datafile/42?gbrecs=true";
        String guestKey = URLDecoder.decode("http://localhost:8080/api/access/datafile/42", StandardCharsets.UTF_8);
        String signedUrl = UrlSignerUtil.signUrl(base, 1000, "guest", "GET", TEST_SIGNING_SECRET + guestKey);

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, "guest", signedUrl);

        assertEquals(GuestUser.get(), sut.findUserFromRequest(request));
    }

    @Test
    public void testEndToEnd_guestUrlForgedWithoutSecret_rejected() {
        // The guest key is derived from the public download URL, which anyone knows: without the
        // server-side secret in the signing key, anyone could forge guest download URLs.
        sut.authSvc = mock(AuthenticationServiceBean.class);
        String base = "http://localhost:8080/api/access/datafile/42?gbrecs=true";
        String guestKey = "http://localhost:8080/api/access/datafile/42";
        String forged = UrlSignerUtil.signUrl(base, 1000, "guest", "GET", guestKey);

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, "guest", forged);

        assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(request));
    }

    @Test
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
            String signedUrl = UrlSignerUtil.signUrl(url, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNING_SECRET + TEST_SIGNED_URL_TOKEN);
            ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, signedUrl);
            User found = assertDoesNotThrow(() -> sut.findUserFromRequest(request),
                    "signed URL must authenticate when used verbatim: " + signedUrl);
            assertEquals(testAuthenticatedUser, found,
                    "signed URL must authenticate when used verbatim: " + signedUrl);
        }
    }

    @Test
    public void testEndToEnd_tamperedEscapedUrlUsedVerbatim_rejected() {
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String url = "http://localhost:8080/api/v1/datasets/:persistentId/userPermissions?persistentId=doi%3A10.5072%2FFK2%2FABC";
        String signedUrl = UrlSignerUtil.signUrl(url, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNING_SECRET + TEST_SIGNED_URL_TOKEN);
        String tampered = signedUrl.replace("FK2%2FABC", "FK2%2FHACKED");

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, tampered);

        assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(request));
    }

    @Test
    public void testEndToEnd_clientStackReEncodesSignatureParams_authenticates() {
        // Why the decoded fallback must stay: every signed URL carries ':' in its own until timestamp
        // (and clients/proxies like Apache HttpClient, OkHttp or mod_proxy may percent-encode it in
        // flight). A URL signed in decoded form and presented in a re-encoded variant must validate.
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String base = "http://localhost:8080/api/v1/datasets/42";
        String signedUrl = UrlSignerUtil.signUrl(base, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNING_SECRET + TEST_SIGNED_URL_TOKEN);
        // Simulate a stack that re-encodes ':' in query values; the path is left alone.
        int queryStart = signedUrl.indexOf('?');
        String reEncoded = signedUrl.substring(0, queryStart)
                + signedUrl.substring(queryStart).replace(":", "%3A");

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, reEncoded);

        User found = assertDoesNotThrow(() -> sut.findUserFromRequest(request),
                "re-encoded variant of a decoded-form-signed URL must still authenticate");
        assertEquals(testAuthenticatedUser, found,
                "re-encoded variant of a decoded-form-signed URL must still authenticate");
    }

    // Runs the real rdm flow: un-escape, sign, request the original (encoded) URL + signature, then the
    // server URL-decodes the request and checks it. Returns true iff the user authenticates.
    private boolean validatesEndToEndAsRdmClient(String urlAsClientBuilds) {
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String canonical = URLDecoder.decode(urlAsClientBuilds, StandardCharsets.UTF_8);
        String signed = UrlSignerUtil.signUrl(canonical, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNING_SECRET + TEST_SIGNED_URL_TOKEN);
        String requestUri = urlAsClientBuilds + signed.substring(canonical.length());
        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, requestUri);
        try {
            return testAuthenticatedUser.equals(sut.findUserFromRequest(request));
        } catch (WrappedAuthErrorResponse e) {
            return false;
        }
    }

    @Test
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

    @Test
    public void testConstructorInjectionWiresTheCollaborators() throws WrappedAuthErrorResponse {
        AuthenticationServiceBean auth = mock(AuthenticationServiceBean.class);
        when(auth.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        ApiToken apiToken = mock(ApiToken.class);
        when(apiToken.getTokenString()).thenReturn(TEST_SIGNED_URL_TOKEN);
        when(auth.findApiTokenByUser(testAuthenticatedUser)).thenReturn(apiToken);
        SignedUrlAuthMechanism mechanism = new SignedUrlAuthMechanism(auth,
                mock(PrivateUrlServiceBean.class), FixedSigningSecret.withSecret(TEST_SIGNING_SECRET));

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);

        assertEquals(testAuthenticatedUser, mechanism.findUserFromRequest(request));
    }
}
