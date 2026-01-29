package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.auth.doubles.BearerTokenKeyContainerRequestTestFake;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.container.ContainerRequestContext;

import static org.junit.jupiter.api.Assertions.*;

@LocalJvmSettings
@JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth")
class BearerTokenAuthMechanismTest {

    private static final String TEST_BEARER_TOKEN = "Bearer test";

    private BearerTokenAuthMechanism sut;

    @BeforeEach
    public void setUp() {
        sut = new BearerTokenAuthMechanism();
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);
        sut.userSvc = Mockito.mock(UserServiceBean.class);
    }

    @Test
    void testFindUserFromRequest_no_token() throws WrappedAuthErrorResponse {
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake(null);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertNull(actual);
    }

    @Test
    void testFindUserFromRequest_invalid_token() throws AuthorizationException {
        String testErrorMessage = "test error";
        Mockito.when(sut.authSvc.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN)).thenThrow(new AuthorizationException(testErrorMessage));

        // when
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake(TEST_BEARER_TOKEN);
        WrappedUnauthorizedAuthErrorResponse wrappedUnauthorizedAuthErrorResponse = assertThrows(WrappedUnauthorizedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        //then
        assertEquals(testErrorMessage, wrappedUnauthorizedAuthErrorResponse.getMessage());
    }

    @Test
    void testFindUserFromRequest_validToken_accountExists() throws WrappedAuthErrorResponse, AuthorizationException {
        AuthenticatedUser testAuthenticatedUser = new AuthenticatedUser();
        Mockito.when(sut.authSvc.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN)).thenReturn(testAuthenticatedUser);
        Mockito.when(sut.userSvc.updateLastApiUseTime(testAuthenticatedUser)).thenReturn(testAuthenticatedUser);

        // when
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake(TEST_BEARER_TOKEN);
        User actual = sut.findUserFromRequest(testContainerRequest);

        //then
        assertEquals(testAuthenticatedUser, actual);
        Mockito.verify(sut.userSvc, Mockito.atLeastOnce()).updateLastApiUseTime(testAuthenticatedUser);
    }

    @Test
    void testFindUserFromRequest_validToken_noAccount() throws AuthorizationException {
        Mockito.when(sut.authSvc.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN)).thenReturn(null);

        // when
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake(TEST_BEARER_TOKEN);
        WrappedForbiddenAuthErrorResponse wrappedForbiddenAuthErrorResponse = assertThrows(WrappedForbiddenAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        //then
        assertEquals(BundleUtil.getStringFromBundle("bearerTokenAuthMechanism.errors.tokenValidatedButNoRegisteredUser"), wrappedForbiddenAuthErrorResponse.getMessage());
    }
}
