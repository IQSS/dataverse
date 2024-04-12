package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.api.auth.doubles.SignedUrlContainerRequestTestFake;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
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
        WrappedAuthErrorResponse wrappedAuthErrorResponse = assertThrows(WrappedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        assertEquals(RESPONSE_MESSAGE_BAD_SIGNED_URL, wrappedAuthErrorResponse.getMessage());
    }

    @Test
    public void testFindUserFromRequest_SignedUrlTokenProvided_UserExists_UserApiTokenDoesNotExist_UserNotAuthenticated() {
        AuthenticationServiceBean authenticationServiceBeanStub = Mockito.mock(AuthenticationServiceBean.class);
        Mockito.when(authenticationServiceBeanStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(testAuthenticatedUser);
        Mockito.when(authenticationServiceBeanStub.findApiTokenByUser(testAuthenticatedUser)).thenReturn(null);

        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);
        WrappedAuthErrorResponse wrappedAuthErrorResponse = assertThrows(WrappedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        assertEquals(RESPONSE_MESSAGE_BAD_SIGNED_URL, wrappedAuthErrorResponse.getMessage());
    }

    @Test
    public void testFindUserFromRequest_SignedUrlTokenProvided_UserDoesNotExistForTheGivenId_UserNotAuthenticated() {
        AuthenticationServiceBean authenticationServiceBeanStub = Mockito.mock(AuthenticationServiceBean.class);
        Mockito.when(authenticationServiceBeanStub.getAuthenticatedUser(TEST_SIGNED_URL_USER_ID)).thenReturn(null);

        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new SignedUrlContainerRequestTestFake(TEST_SIGNED_URL_TOKEN, TEST_SIGNED_URL_USER_ID);
        WrappedAuthErrorResponse wrappedAuthErrorResponse = assertThrows(WrappedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        assertEquals(RESPONSE_MESSAGE_BAD_SIGNED_URL, wrappedAuthErrorResponse.getMessage());
    }
}
