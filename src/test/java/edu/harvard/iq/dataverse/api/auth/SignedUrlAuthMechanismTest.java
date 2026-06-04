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

    // ---- End-to-end signature validation through the REAL SignedUrlAuthMechanism ----
    // These drive the actual validation path: sign a URL, then validate it the way the server does
    // (URLDecoder.decode(requestUri) -> UrlSignerUtil.isValidUrl), proving a signed URL produced for
    // an rdm-integration-style URL actually authenticates the user (and that tampering does not).
    // No signing-secret is configured in this unit context, so the signing key is just the API token.

    private void givenUserWithSigningKey(String key) {
        AuthenticationServiceBean authStub = Mockito.mock(AuthenticationServiceBean.class);
        Mockito.when(authStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        ApiToken apiToken = Mockito.mock(ApiToken.class);
        Mockito.when(apiToken.getTokenString()).thenReturn(key);
        Mockito.when(authStub.findApiTokenByUser(testAuthenticatedUser)).thenReturn(apiToken);
        sut.authSvc = authStub;
    }

    @Test
    public void testEndToEnd_rawPersistentIdSignedUrl_authenticatesUser() throws WrappedAuthErrorResponse {
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        String base = "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi:10.5072/FK2/ABC";
        // The client signs this URL and requests it verbatim (a raw DOI is unchanged by URLDecoder.decode).
        String signedUrl = UrlSignerUtil.signUrl(base, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNED_URL_TOKEN);

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, signedUrl);

        assertEquals(testAuthenticatedUser, sut.findUserFromRequest(request),
                "a signed raw-DOI URL must authenticate end to end (decode + validate)");
    }

    @Test
    public void testEndToEnd_escapedPersistentIdReconstructedRequest_authenticatesUser() throws WrappedAuthErrorResponse {
        givenUserWithSigningKey(TEST_SIGNED_URL_TOKEN);
        // The signing client un-escapes before signing, so the server signs the DECODED form...
        String decodedBase = "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi:10.5072/FK2/ABC";
        String signedDecoded = UrlSignerUtil.signUrl(decodedBase, 1000, TEST_SIGNED_URL_USER_ID, "GET", TEST_SIGNED_URL_TOKEN);
        // ...but the actual request carries the ESCAPED persistentId (the URL the caller built). The
        // server's URLDecoder.decode turns it back into the signed (decoded) form, so it validates.
        String escapedBase = "http://localhost:8080/api/v1/datasets/:persistentId?persistentId=doi%3A10.5072%2FFK2%2FABC";
        String requestUri = escapedBase + signedDecoded.substring(decodedBase.length());

        ContainerRequestContext request = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID, requestUri);

        assertEquals(testAuthenticatedUser, sut.findUserFromRequest(request),
                "an escaped persistentId must authenticate end to end (server signs decoded; validation decodes the request)");
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
}
