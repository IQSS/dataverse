package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.api.auth.doubles.SignedUrlContainerRequestTestFake;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.container.ContainerRequestContext;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static edu.harvard.iq.dataverse.api.auth.SignedUrlAuthMechanism.RESPONSE_MESSAGE_BAD_SIGNED_URL;
import static org.junit.jupiter.api.Assertions.*;

public class SignedUrlAuthMechanismTest {

    private static final String TEST_SIGNED_URL_TOKEN = "test-signed-url-token";
    private static final String TEST_SIGNED_URL_USER_ID = "test-user";

    private SignedUrlAuthMechanism sut;

    private final AuthenticatedUser testAuthenticatedUser = new AuthenticatedUser();

    @BeforeEach
    public void setUp() {
        sut = new SignedUrlAuthMechanism();
    }

    @Test
    public void testFindUserFromRequest_SignedUrlTokenNotProvided() throws WrappedAuthErrorResponse {
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(null, null);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertNull(actual);
    }

    @Test
    public void testFindUserFromRequest_SignedUrlTokenProvided_UserExists_ValidSignedUrl_UserAuthenticated() throws WrappedAuthErrorResponse {
        AuthenticationServiceBean authenticationServiceBeanStub = Mockito.mock(AuthenticationServiceBean.class);
        Mockito.when(authenticationServiceBeanStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        ApiToken apiTokenStub = Mockito.mock(ApiToken.class);
        Mockito.when(apiTokenStub.getTokenString()).thenReturn(TEST_SIGNED_URL_TOKEN);
        Mockito.when(authenticationServiceBeanStub.findApiTokenByUser(testAuthenticatedUser)).thenReturn(apiTokenStub);

        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertEquals(testAuthenticatedUser, actual);
    }

    @Test
    public void testFindUserFromRequest_SignedUrlTokenProvided_UserExists_InvalidSignedUrl_UserNotAuthenticated() {
        AuthenticationServiceBean authenticationServiceBeanStub = Mockito.mock(AuthenticationServiceBean.class);
        Mockito.when(authenticationServiceBeanStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        ApiToken apiTokenStub = Mockito.mock(ApiToken.class);
        Mockito.when(apiTokenStub.getTokenString()).thenReturn("different-token-from-the-signed-url");
        Mockito.when(authenticationServiceBeanStub.findApiTokenByUser(testAuthenticatedUser)).thenReturn(apiTokenStub);

        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);
        WrappedUnauthorizedAuthErrorResponse wrappedUnauthorizedAuthErrorResponse = assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        assertEquals(RESPONSE_MESSAGE_BAD_SIGNED_URL, wrappedUnauthorizedAuthErrorResponse.getMessage());
    }

    @Test
    public void testFindUserFromRequest_SignedUrlTokenProvided_UserExists_UserApiTokenDoesNotExist_UserNotAuthenticated() {
        AuthenticationServiceBean authenticationServiceBeanStub = Mockito.mock(AuthenticationServiceBean.class);
        Mockito.when(authenticationServiceBeanStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        Mockito.when(authenticationServiceBeanStub.findApiTokenByUser(testAuthenticatedUser)).thenReturn(null);

        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);
        WrappedUnauthorizedAuthErrorResponse wrappedUnauthorizedAuthErrorResponse = assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        assertEquals(RESPONSE_MESSAGE_BAD_SIGNED_URL, wrappedUnauthorizedAuthErrorResponse.getMessage());
    }

    @Test
    public void testFindUserFromRequest_SignedUrlTokenProvided_UserDoesNotExistForTheGivenId_UserNotAuthenticated() {
        AuthenticationServiceBean authenticationServiceBeanStub = Mockito.mock(AuthenticationServiceBean.class);
        Mockito.when(authenticationServiceBeanStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(null);

        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);
        WrappedUnauthorizedAuthErrorResponse wrappedUnauthorizedAuthErrorResponse = assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        assertEquals(RESPONSE_MESSAGE_BAD_SIGNED_URL, wrappedUnauthorizedAuthErrorResponse.getMessage());
    }

    // End-to-end validation through the REAL SignedUrlAuthMechanism (URLDecoder.decode + isValidUrl),
    // which the isValidUrl-only tests in UrlSignerUtilTest do not exercise. No signing secret is
    // configured here, so the signing key is just the API token.

    private void givenUserWithSigningKey(String key) {
        AuthenticationServiceBean authStub = Mockito.mock(AuthenticationServiceBean.class);
        Mockito.when(authStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        ApiToken apiToken = Mockito.mock(ApiToken.class);
        Mockito.when(apiToken.getTokenString()).thenReturn(key);
        Mockito.when(authStub.findApiTokenByUser(testAuthenticatedUser)).thenReturn(apiToken);
        sut.authSvc = authStub;
    }

    @Test
    public void testEndToEnd_tamperedSignedUrl_userNotAuthenticated() {
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String base = "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi:10.5072/FK2/ABC";
        String signedUrl = UrlSignerUtil.signUrl(base, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNED_URL_TOKEN);
        // Alter the signed portion of the URL after signing -> the signature must no longer validate.
        String tampered = signedUrl.replace("FK2/ABC", "FK2/HACKED");

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, tampered);

        assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(request));
    }

    // Runs the real rdm flow: un-escape, sign, request the original (encoded) URL + signature, then the
    // server URL-decodes the request and checks it. Returns true iff the user authenticates.
    private boolean validatesEndToEndAsRdmClient(String urlAsClientBuilds) {
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String canonical = URLDecoder.decode(urlAsClientBuilds, StandardCharsets.UTF_8);
        String signed = UrlSignerUtil.signUrl(canonical, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNED_URL_TOKEN);
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
}
