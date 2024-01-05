package edu.harvard.iq.dataverse.api.auth;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.auth.doubles.BearerTokenKeyContainerRequestTestFake;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2Exception;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.container.ContainerRequestContext;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static edu.harvard.iq.dataverse.api.auth.BearerTokenAuthMechanism.*;
import static org.junit.jupiter.api.Assertions.*;

public class BearerTokenAuthMechanismTest {

    private static final String TEST_API_KEY = "test-api-key";

    private BearerTokenAuthMechanism sut;

    @BeforeEach
    public void setUp() {
        sut = new BearerTokenAuthMechanism();
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth")
    public void testFindUserFromRequest_no_token() throws WrappedAuthErrorResponse {
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);
        sut.userSvc = Mockito.mock(UserServiceBean.class);

        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake(null);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertNull(actual);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth")
    public void testFindUserFromRequest_invalid_token() throws WrappedAuthErrorResponse {
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);
        sut.userSvc = Mockito.mock(UserServiceBean.class);
        Mockito.when(sut.authSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class)).thenReturn(Collections.emptySet());
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake("Bearer ");
        WrappedAuthErrorResponse wrappedAuthErrorResponse = assertThrows(WrappedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        //then
        assertEquals(INVALID_BEARER_TOKEN, wrappedAuthErrorResponse.getMessage());
    }
    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth")
    public void testFindUserFromRequest_no_OidcProvider() throws WrappedAuthErrorResponse {
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);
        sut.userSvc = Mockito.mock(UserServiceBean.class);
        Mockito.when(sut.authSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class)).thenReturn(Collections.emptySet());
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake("Bearer " +TEST_API_KEY);
        WrappedAuthErrorResponse wrappedAuthErrorResponse = assertThrows(WrappedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        //then
        assertEquals(BEARER_TOKEN_DETECTED_NO_OIDC_PROVIDER_CONFIGURED, wrappedAuthErrorResponse.getMessage());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth")
    public void testFindUserFromRequest_oneProvider_invalidToken_1() throws WrappedAuthErrorResponse, ParseException, IOException, OAuth2Exception {

        sut.userSvc = Mockito.mock(UserServiceBean.class);
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);

        OIDCAuthProvider oidcAuthProvider = Mockito.mock(OIDCAuthProvider.class);
        String providerID = "OIEDC";
        Mockito.when(oidcAuthProvider.getId()).thenReturn(providerID);
        // ensure that a valid OIDCAuthProvider is available within the AuthenticationServiceBean
        Mockito.when(sut.authSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class)).thenReturn(Collections.singleton(providerID));
        Mockito.when(sut.authSvc.getAuthenticationProvider(providerID)).thenReturn(oidcAuthProvider);

        // ensure that the OIDCAuthProvider returns a valid UserRecordIdentifier for a given Token
        BearerAccessToken token = BearerAccessToken.parse("Bearer " + TEST_API_KEY);
        Mockito.when(oidcAuthProvider.getUserIdentifierForValidToken(token)).thenReturn(Optional.empty());

        // when
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake("Bearer " + TEST_API_KEY);
        WrappedAuthErrorResponse wrappedAuthErrorResponse = assertThrows(WrappedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        //then
        assertEquals(UNAUTHORIZED_BEARER_TOKEN, wrappedAuthErrorResponse.getMessage());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth")
    public void testFindUserFromRequest_oneProvider_invalidToken_2() throws WrappedAuthErrorResponse, ParseException, IOException, OAuth2Exception {

        sut.userSvc = Mockito.mock(UserServiceBean.class);
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);

        OIDCAuthProvider oidcAuthProvider = Mockito.mock(OIDCAuthProvider.class);
        String providerID = "OIEDC";
        Mockito.when(oidcAuthProvider.getId()).thenReturn(providerID);
        // ensure that a valid OIDCAuthProvider is available within the AuthenticationServiceBean
        Mockito.when(sut.authSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class)).thenReturn(Collections.singleton(providerID));
        Mockito.when(sut.authSvc.getAuthenticationProvider(providerID)).thenReturn(oidcAuthProvider);

        // ensure that the OIDCAuthProvider returns a valid UserRecordIdentifier for a given Token
        BearerAccessToken token = BearerAccessToken.parse("Bearer " + TEST_API_KEY);
        Mockito.when(oidcAuthProvider.getUserIdentifierForValidToken(token)).thenThrow(OAuth2Exception.class);

        // when
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake("Bearer " + TEST_API_KEY);
        WrappedAuthErrorResponse wrappedAuthErrorResponse = assertThrows(WrappedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        //then
        assertEquals(UNAUTHORIZED_BEARER_TOKEN, wrappedAuthErrorResponse.getMessage());
    }
    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth")
    public void testFindUserFromRequest_oneProvider_validToken() throws WrappedAuthErrorResponse, ParseException, IOException, OAuth2Exception {

        sut.userSvc = Mockito.mock(UserServiceBean.class);
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);

        OIDCAuthProvider oidcAuthProvider = Mockito.mock(OIDCAuthProvider.class);
        String providerID = "OIEDC";
        Mockito.when(oidcAuthProvider.getId()).thenReturn(providerID);
        // ensure that a valid OIDCAuthProvider is available within the AuthenticationServiceBean
        Mockito.when(sut.authSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class)).thenReturn(Collections.singleton(providerID));
        Mockito.when(sut.authSvc.getAuthenticationProvider(providerID)).thenReturn(oidcAuthProvider);

        // ensure that the OIDCAuthProvider returns a valid UserRecordIdentifier for a given Token
        UserRecordIdentifier userinfo = new UserRecordIdentifier(providerID, "KEY");
        BearerAccessToken token = BearerAccessToken.parse("Bearer " + TEST_API_KEY);
        Mockito.when(oidcAuthProvider.getUserIdentifierForValidToken(token)).thenReturn(Optional.of(userinfo));

        // ensures that the AuthenticationServiceBean can retrieve an Authenticated user based on the UserRecordIdentifier
        AuthenticatedUser testAuthenticatedUser = new AuthenticatedUser();
        Mockito.when(sut.authSvc.lookupUser(userinfo)).thenReturn(testAuthenticatedUser);
        Mockito.when(sut.userSvc.updateLastApiUseTime(testAuthenticatedUser)).thenReturn(testAuthenticatedUser);

        // when
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake("Bearer " + TEST_API_KEY);
        User actual = sut.findUserFromRequest(testContainerRequest);

        //then
        assertEquals(testAuthenticatedUser, actual);
        Mockito.verify(sut.userSvc, Mockito.atLeastOnce()).updateLastApiUseTime(testAuthenticatedUser);

    }
    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth")
    public void testFindUserFromRequest_oneProvider_validToken_noAccount() throws WrappedAuthErrorResponse, ParseException, IOException, OAuth2Exception {

        sut.userSvc = Mockito.mock(UserServiceBean.class);
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);

        OIDCAuthProvider oidcAuthProvider = Mockito.mock(OIDCAuthProvider.class);
        String providerID = "OIEDC";
        Mockito.when(oidcAuthProvider.getId()).thenReturn(providerID);
        // ensure that a valid OIDCAuthProvider is available within the AuthenticationServiceBean
        Mockito.when(sut.authSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class)).thenReturn(Collections.singleton(providerID));
        Mockito.when(sut.authSvc.getAuthenticationProvider(providerID)).thenReturn(oidcAuthProvider);

        // ensure that the OIDCAuthProvider returns a valid UserRecordIdentifier for a given Token
        UserRecordIdentifier userinfo = new UserRecordIdentifier(providerID, "KEY");
        BearerAccessToken token = BearerAccessToken.parse("Bearer " + TEST_API_KEY);
        Mockito.when(oidcAuthProvider.getUserIdentifierForValidToken(token)).thenReturn(Optional.of(userinfo));

        // ensures that the AuthenticationServiceBean can retrieve an Authenticated user based on the UserRecordIdentifier
        Mockito.when(sut.authSvc.lookupUser(userinfo)).thenReturn(null);


        // when
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake("Bearer " + TEST_API_KEY);
        User actual = sut.findUserFromRequest(testContainerRequest);

        //then
        assertNull(actual);

    }
}
