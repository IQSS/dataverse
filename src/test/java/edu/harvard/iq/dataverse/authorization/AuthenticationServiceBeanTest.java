package edu.harvard.iq.dataverse.authorization;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2Exception;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2UserRecord;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@LocalJvmSettings
public class AuthenticationServiceBeanTest {

    private AuthenticationServiceBean sut;
    private static final String TEST_BEARER_TOKEN = "Bearer test";

    @BeforeEach
    public void setUp() {
        sut = new AuthenticationServiceBean();
        sut.authProvidersRegistrationService = Mockito.mock(AuthenticationProvidersRegistrationServiceBean.class);
        sut.em = Mockito.mock(EntityManager.class);
    }

    @Test
    void testLookupUserByOIDCBearerToken_no_OIDCProvider() {
        // Given no OIDC providers are configured
        Mockito.when(sut.authProvidersRegistrationService.getAuthenticationProvidersMap()).thenReturn(Map.of());

        // When invoking lookupUserByOIDCBearerToken
        AuthorizationException exception = assertThrows(AuthorizationException.class,
                () -> sut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN));

        // Then the exception message should indicate no OIDC provider is configured
        assertEquals(BundleUtil.getStringFromBundle("authenticationServiceBean.errors.bearerTokenDetectedNoOIDCProviderConfigured"), exception.getMessage());
    }

    @Test
    void testLookupUserByOIDCBearerToken_oneProvider_invalidToken_1() throws ParseException, OAuth2Exception, IOException {
        // Given a single OIDC provider that cannot find a user
        OIDCAuthProvider oidcAuthProviderStub = stubOIDCAuthProvider("OIEDC");
        BearerAccessToken token = BearerAccessToken.parse(TEST_BEARER_TOKEN);
        Mockito.when(oidcAuthProviderStub.getUserInfo(token)).thenReturn(Optional.empty());

        // When invoking lookupUserByOIDCBearerToken
        AuthorizationException exception = assertThrows(AuthorizationException.class,
                () -> sut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN));

        // Then the exception message should indicate an unauthorized token
        assertEquals(BundleUtil.getStringFromBundle("authenticationServiceBean.errors.unauthorizedBearerToken"), exception.getMessage());
    }

    @Test
    void testLookupUserByOIDCBearerToken_oneProvider_invalidToken_2() throws ParseException, IOException, OAuth2Exception {
        // Given a single OIDC provider that throws an IOException
        OIDCAuthProvider oidcAuthProviderStub = stubOIDCAuthProvider("OIEDC");
        BearerAccessToken token = BearerAccessToken.parse(TEST_BEARER_TOKEN);
        Mockito.when(oidcAuthProviderStub.getUserInfo(token)).thenThrow(IOException.class);

        // When invoking lookupUserByOIDCBearerToken
        AuthorizationException exception = assertThrows(AuthorizationException.class,
                () -> sut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN));

        // Then the exception message should indicate an unauthorized token
        assertEquals(BundleUtil.getStringFromBundle("authenticationServiceBean.errors.unauthorizedBearerToken"), exception.getMessage());
    }

    @Test
    void testLookupUserByOIDCBearerToken_oneProvider_validToken() throws ParseException, IOException, AuthorizationException, OAuth2Exception {
        // Given a single OIDC provider that returns a valid user identifier
        setUpOIDCProviderWhichValidatesToken();

        // Setting up an authenticated user is found
        AuthenticatedUser authenticatedUser = setupAuthenticatedUserByAuthPrvIDQueryWithResult(new AuthenticatedUser());

        // When invoking lookupUserByOIDCBearerToken
        User actualUser = sut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN);

        // Then the actual user should match the expected authenticated user
        assertEquals(authenticatedUser, actualUser);
    }

    @Test
    void testLookupUserByOIDCBearerToken_oneProvider_validToken_noAccount() throws ParseException, IOException, AuthorizationException, OAuth2Exception {
        // Given a single OIDC provider that returns a valid user identifier
        setUpOIDCProviderWhichValidatesToken();

        // Setting up an authenticated user is not found
        setupAuthenticatedUserQueryWithNoResult();

        // When invoking lookupUserByOIDCBearerToken
        User actualUser = sut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN);

        // Then no user should be found, and result should be null
        assertNull(actualUser);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-use-builtin-user-on-id-match")
    void testLookupUserByOIDCBearerToken_oneProvider_validToken_userNotPresentAsBuiltin_useBuiltinUserOnIdMatchFeatureFlagEnabled()
            throws ParseException, IOException, AuthorizationException, OAuth2Exception {

        // Given a single OIDC provider that returns a valid user identifier
        setUpOIDCProviderWhichValidatesToken();

        // Spy on the SUT to verify method calls
        AuthenticationServiceBean spySut = Mockito.spy(sut);

        // Setting up an authenticated user is found (but only after the second call to lookupUser, that is, not coming from the builtin user provider)
        AuthenticatedUser authenticatedUser = setupAuthenticatedUserByAuthPrvIDQueryWithResult(new AuthenticatedUser(), true);

        // When invoking lookupUserByOIDCBearerToken
        User actualUser = spySut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN);

        // Then the actual user should match the expected authenticated user
        assertEquals(authenticatedUser, actualUser);

        // Capture calls to lookupUser
        ArgumentCaptor<String> providerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);

        // Ensure lookupUser is called twice
        Mockito.verify(spySut, Mockito.times(2)).lookupUser(providerIdCaptor.capture(), userIdCaptor.capture());

        // Assert that the first call was with expected parameters
        assertEquals(BuiltinAuthenticationProvider.PROVIDER_ID, providerIdCaptor.getAllValues().get(0));
        assertEquals("testUsername", userIdCaptor.getAllValues().get(0));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-use-builtin-user-on-id-match")
    void testLookupUserByOIDCBearerToken_oneProvider_validToken_userIsPresentAsBuiltin_useBuiltinUserOnIdMatchFeatureFlagEnabled() throws ParseException, IOException, AuthorizationException, OAuth2Exception {
        // Given a single OIDC provider that returns a valid user identifier
        setUpOIDCProviderWhichValidatesToken();

        // Spy on the SUT to verify method calls
        AuthenticationServiceBean spySut = Mockito.spy(sut);

        // Setting up an authenticated user is found
        AuthenticatedUser authenticatedUser = setupAuthenticatedUserByAuthPrvIDQueryWithResult(new AuthenticatedUser());

        // When invoking lookupUserByOIDCBearerToken
        User actualUser = spySut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN);

        // Then the actual user should match the expected authenticated user
        assertEquals(authenticatedUser, actualUser);

        // Capture calls to lookupUser
        ArgumentCaptor<String> providerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);

        // Ensure lookupUser is called once
        Mockito.verify(spySut, Mockito.times(1)).lookupUser(providerIdCaptor.capture(), userIdCaptor.capture());

        // Assert that lookupUser is called with expected parameters
        assertEquals(BuiltinAuthenticationProvider.PROVIDER_ID, providerIdCaptor.getAllValues().get(0));
        assertEquals("testUsername", userIdCaptor.getAllValues().get(0));
    }

    private void setupAuthenticatedUserQueryWithNoResult() {
        TypedQuery<AuthenticatedUserLookup> queryStub = Mockito.mock(TypedQuery.class);
        Mockito.when(queryStub.getSingleResult()).thenThrow(new NoResultException());
        Mockito.when(sut.em.createNamedQuery("AuthenticatedUserLookup.findByAuthPrvID_PersUserId", AuthenticatedUserLookup.class)).thenReturn(queryStub);
    }

    private void setUpOIDCProviderWhichValidatesToken() throws ParseException, IOException, OAuth2Exception {
        OIDCAuthProvider oidcAuthProviderStub = stubOIDCAuthProvider("OIDC");

        BearerAccessToken token = BearerAccessToken.parse(TEST_BEARER_TOKEN);

        // Stub the UserInfo returned by the provider
        UserInfo userInfoStub = Mockito.mock(UserInfo.class);
        Mockito.when(oidcAuthProviderStub.getUserInfo(token)).thenReturn(Optional.of(userInfoStub));

        // Stub OAuth2UserRecord and its associated UserRecordIdentifier
        OAuth2UserRecord oAuth2UserRecordStub = Mockito.mock(OAuth2UserRecord.class);
        UserRecordIdentifier userRecordIdentifierStub = Mockito.mock(UserRecordIdentifier.class);
        Mockito.when(userRecordIdentifierStub.getUserIdInRepo()).thenReturn("testUserId");
        Mockito.when(userRecordIdentifierStub.getUserRepoId()).thenReturn("testRepoId");
        Mockito.when(oAuth2UserRecordStub.getUserRecordIdentifier()).thenReturn(userRecordIdentifierStub);
        Mockito.when(oAuth2UserRecordStub.getUsername()).thenReturn("testUsername");

        // Stub the OIDCAuthProvider to return OAuth2UserRecord
        Mockito.when(oidcAuthProviderStub.getUserRecord(userInfoStub)).thenReturn(oAuth2UserRecordStub);
    }

    private OIDCAuthProvider stubOIDCAuthProvider(String providerID) {
        OIDCAuthProvider oidcAuthProviderStub = Mockito.mock(OIDCAuthProvider.class);
        Mockito.when(oidcAuthProviderStub.getId()).thenReturn(providerID);
        Mockito.when(sut.authProvidersRegistrationService.getAuthenticationProvidersMap()).thenReturn(Map.of(providerID, oidcAuthProviderStub));
        return oidcAuthProviderStub;
    }

    private AuthenticatedUser setupAuthenticatedUserByAuthPrvIDQueryWithResult(AuthenticatedUser authenticatedUser) {
        return setupAuthenticatedUserByAuthPrvIDQueryWithResult(authenticatedUser, false);
    }

    private AuthenticatedUser setupAuthenticatedUserByAuthPrvIDQueryWithResult(AuthenticatedUser authenticatedUser, boolean returnNullOnFirstCall) {
        TypedQuery<AuthenticatedUserLookup> queryStub = Mockito.mock(TypedQuery.class);
        AuthenticatedUserLookup lookupStub = Mockito.mock(AuthenticatedUserLookup.class);
        Mockito.when(lookupStub.getAuthenticatedUser()).thenReturn(authenticatedUser);
        if (returnNullOnFirstCall) {
            Mockito.when(queryStub.getSingleResult())
                    .thenThrow(new NoResultException())
                    .thenReturn(lookupStub);
        } else {
            Mockito.when(queryStub.getSingleResult()).thenReturn(lookupStub);
        }
        Mockito.when(sut.em.createNamedQuery("AuthenticatedUserLookup.findByAuthPrvID_PersUserId", AuthenticatedUserLookup.class)).thenReturn(queryStub);
        return authenticatedUser;
    }
}
