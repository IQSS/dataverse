package edu.harvard.iq.dataverse.authorization;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2Exception;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2UserRecord;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@LocalJvmSettings
public class AuthenticationServiceBeanTest {

    private AuthenticationServiceBean sut;
    private static final String TEST_BEARER_TOKEN = "Bearer test";
    private static final String TEST_ORCID_USER_ID = "0000-0000-0000-0000";
    private static final String TEST_GOOGLE_USER_ID = "111111111111111111111";
    private static final String TEST_GITHUB_USER_ID = "11111111";

    @BeforeEach
    public void setUp() {
        sut = new AuthenticationServiceBean();
        sut.authProvidersRegistrationService = Mockito.mock(AuthenticationProvidersRegistrationServiceBean.class);
        sut.em = Mockito.mock(EntityManager.class);
        sut.userService = Mockito.mock(UserServiceBean.class);
        Mockito.when(sut.userService.save(Mockito.any(AuthenticatedUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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
        setUpOIDCProviderWithGenericUser();

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
        setUpOIDCProviderWithGenericUser();

        // Setting up an authenticated user is not found
        setupAuthenticatedUserQueryWithNoResult();

        // When invoking lookupUserByOIDCBearerToken
        User actualUser = sut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN);

        // Then no user should be found, and result should be null
        assertNull(actualUser);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "oidc-user-property-sync")
    void testLookupUserByOIDCBearerToken_oneProvider_validToken_updatesUserProperties() throws ParseException, IOException, AuthorizationException, OAuth2Exception {
        // Given a single OIDC provider that returns a valid user identifier with updated properties
        OAuth2UserRecord record = setupOidcUserRecordBasics();
        AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo("NewFirst", "NewLast", "new@example.org", "", "");
        Mockito.when(record.getDisplayInfo()).thenReturn(displayInfo);
        Mockito.when(record.getAvailableEmailAddresses()).thenReturn(List.of("new@example.org"));

        // Setting up an authenticated user with outdated properties
        AuthenticatedUser oldAuthenticatedUser = new AuthenticatedUser();
        oldAuthenticatedUser.setFirstName("OldFirst");
        oldAuthenticatedUser.setLastName("OldLast");
        oldAuthenticatedUser.setEmail("old@example.org");
        setupAuthenticatedUserByAuthPrvIDQueryWithResult(oldAuthenticatedUser);

        // When invoking lookupUserByOIDCBearerToken
        AuthenticatedUser actualUser = sut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN);

        // Verify that save() was called with the updated user
        Mockito.verify(sut.userService, Mockito.times(1)).save(Mockito.any());

        // Then the user properties should be updated to match the OIDC token
        assertEquals("NewFirst", actualUser.getFirstName());
        assertEquals("NewLast", actualUser.getLastName());
        assertEquals("new@example.org", actualUser.getEmail());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "oidc-user-property-sync")
    void testLookupUserByOIDCBearerToken_oneProvider_validToken_noPropertyChanges_doesNotSave() throws ParseException, IOException, AuthorizationException, OAuth2Exception {
        // Given a single OIDC provider that returns a valid user identifier with same properties
        OAuth2UserRecord record = setupOidcUserRecordBasics();
        AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo("SameFirst", "SameLast", "same@example.org", "", "");
        Mockito.when(record.getDisplayInfo()).thenReturn(displayInfo);
        Mockito.when(record.getAvailableEmailAddresses()).thenReturn(List.of("same@example.org"));

        // Setting up an authenticated user with identical properties
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setFirstName("SameFirst");
        authenticatedUser.setLastName("SameLast");
        authenticatedUser.setEmail("same@example.org");
        setupAuthenticatedUserByAuthPrvIDQueryWithResult(authenticatedUser);

        // When invoking lookupUserByOIDCBearerToken
        sut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN);

        // Then save should never be called since nothing changed
        Mockito.verify(sut.userService, Mockito.never()).save(Mockito.any());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "oidc-user-property-sync")
    void testLookupUserByOIDCBearerToken_oneProvider_validToken_onlyEmailChanges() throws ParseException, IOException, AuthorizationException, OAuth2Exception {
        // Given a single OIDC provider that returns a valid user identifier with updated email only
        OAuth2UserRecord record = setupOidcUserRecordBasics();
        AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo("SameFirst", "SameLast", "new@example.org", "", "");
        Mockito.when(record.getDisplayInfo()).thenReturn(displayInfo);
        Mockito.when(record.getAvailableEmailAddresses()).thenReturn(List.of("new@example.org"));

        // Setting up an authenticated user with the same name but different email
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setFirstName("SameFirst");
        authenticatedUser.setLastName("SameLast");
        authenticatedUser.setEmail("old@example.org");
        setupAuthenticatedUserByAuthPrvIDQueryWithResult(authenticatedUser);

        // When invoking lookupUserByOIDCBearerToken
        AuthenticatedUser actualUser = sut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN);

        // Then only the email should be updated
        assertEquals("SameFirst", actualUser.getFirstName());
        assertEquals("SameLast", actualUser.getLastName());
        assertEquals("new@example.org", actualUser.getEmail());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "oidc-user-property-sync")
    void testLookupUserByOIDCBearerToken_oneProvider_validToken_displayInfoNull_doesNotUpdateNames() throws ParseException, IOException, AuthorizationException, OAuth2Exception {
        // Given a single OIDC provider that returns no display info but a valid email
        OAuth2UserRecord record = setupOidcUserRecordBasics();
        Mockito.when(record.getDisplayInfo()).thenReturn(null);
        Mockito.when(record.getAvailableEmailAddresses()).thenReturn(List.of("new@example.org"));

        // Setting up an authenticated user with existing properties
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setFirstName("ExistingFirst");
        authenticatedUser.setLastName("ExistingLast");
        authenticatedUser.setEmail("old@example.org");
        setupAuthenticatedUserByAuthPrvIDQueryWithResult(authenticatedUser);

        // When invoking lookupUserByOIDCBearerToken
        AuthenticatedUser actualUser = sut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN);

        // Then names should remain unchanged since displayInfo is null
        assertEquals("ExistingFirst", actualUser.getFirstName());
        assertEquals("ExistingLast", actualUser.getLastName());
        // But email should still be updated
        assertEquals("new@example.org", actualUser.getEmail());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "oidc-user-property-sync")
    void testLookupUserByOIDCBearerToken_oneProvider_validToken_emailNullInRecord_doesNotUpdateEmail() throws ParseException, IOException, AuthorizationException, OAuth2Exception {
        // Given a single OIDC provider that returns a valid user identifier with no email
        OAuth2UserRecord record = setupOidcUserRecordBasics();
        AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo("NewFirst", "NewLast", "", "", "");
        Mockito.when(record.getDisplayInfo()).thenReturn(displayInfo);
        Mockito.when(record.getAvailableEmailAddresses()).thenReturn(null);

        // Setting up an authenticated user with an existing email
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setFirstName("OldFirst");
        authenticatedUser.setLastName("OldLast");
        authenticatedUser.setEmail("existing@example.org");
        setupAuthenticatedUserByAuthPrvIDQueryWithResult(authenticatedUser);

        // When invoking lookupUserByOIDCBearerToken
        AuthenticatedUser actualUser = sut.lookupUserByOIDCBearerToken(TEST_BEARER_TOKEN);

        // Then email should remain unchanged
        assertEquals("existing@example.org", actualUser.getEmail());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-use-builtin-user-on-id-match")
    void testLookupUserByOIDCBearerToken_oneProvider_validToken_userNotPresentAsBuiltin_useBuiltinUserOnIdMatchFeatureFlagEnabled()
            throws ParseException, IOException, AuthorizationException, OAuth2Exception {

        // Given a single OIDC provider that returns a valid user identifier
        setUpOIDCProviderWithBuiltinUserAttributes();

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
        setUpOIDCProviderWithBuiltinUserAttributes();

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

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-use-shib-user-on-id-match")
    void testLookupUserByOIDCBearerToken_oneProvider_validToken_userIsPresentAsShibboleth_useShibUserOnIdMatchFeatureFlagEnabled() throws ParseException, IOException, AuthorizationException, OAuth2Exception {
        // Given a single OIDC provider that returns a valid user identifier
        setUpOIDCProviderWithShibAttributes();

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
        assertEquals(ShibAuthenticationProvider.PROVIDER_ID, providerIdCaptor.getAllValues().get(0));
        assertEquals("testIdp|testPersistentId", userIdCaptor.getAllValues().get(0));
    }

    private static Stream<Arguments> oAuthProvider() {
        return Stream.of(
                Arguments.of(OrcidOAuth2AP.PROVIDER_ID, TEST_ORCID_USER_ID),
                Arguments.of(GoogleOAuth2AP.PROVIDER_ID, TEST_GOOGLE_USER_ID),
                Arguments.of(GitHubOAuth2AP.PROVIDER_ID, TEST_GITHUB_USER_ID)
        );
    }

    @ParameterizedTest
    @MethodSource("oAuthProvider")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-use-oauth-user-on-id-match")
    void testLookupUserByOIDCBearerToken_oneProvider_validToken_userIsPresentAsOAuth_useOAuthUserOnIdMatchFeatureFlagEnabled(String providerId, String expectedUserId) throws ParseException, IOException, AuthorizationException, OAuth2Exception {
        // Given a single OIDC provider that returns a valid user identifier with OAuth attributes
        setUpOIDCProviderWithOAuthAttributes(providerId);

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
        assertEquals(providerId, providerIdCaptor.getAllValues().get(0));
        assertEquals(expectedUserId, userIdCaptor.getAllValues().get(0));
    }

    private void setupAuthenticatedUserQueryWithNoResult() {
        TypedQuery<AuthenticatedUserLookup> queryStub = Mockito.mock(TypedQuery.class);
        Mockito.when(queryStub.getSingleResult()).thenThrow(new NoResultException());
        Mockito.when(sut.em.createNamedQuery("AuthenticatedUserLookup.findByAuthPrvID_PersUserId", AuthenticatedUserLookup.class)).thenReturn(queryStub);
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

    private OIDCAuthProvider stubOIDCAuthProvider(String providerID) {
        OIDCAuthProvider oidcAuthProviderStub = Mockito.mock(OIDCAuthProvider.class);
        Mockito.when(oidcAuthProviderStub.getId()).thenReturn(providerID);
        Mockito.when(sut.authProvidersRegistrationService.getAuthenticationProvidersMap()).thenReturn(Map.of(providerID, oidcAuthProviderStub));
        return oidcAuthProviderStub;
    }

    private OAuth2UserRecord setupOidcUserRecordBasics() throws ParseException, IOException, OAuth2Exception {
        OIDCAuthProvider oidcAuthProviderStub = stubOIDCAuthProvider("OIDC");
        BearerAccessToken token = BearerAccessToken.parse(TEST_BEARER_TOKEN);

        // Stub the UserInfo returned by the provider
        UserInfo userInfoStub = Mockito.mock(UserInfo.class);
        Mockito.when(oidcAuthProviderStub.getUserInfo(token)).thenReturn(Optional.of(userInfoStub));

        // Stub the OIDCAuthProvider to return a mock OAuth2UserRecord
        OAuth2UserRecord oAuth2UserRecordStub = Mockito.mock(OAuth2UserRecord.class);
        Mockito.when(oidcAuthProviderStub.getUserRecord(userInfoStub)).thenReturn(oAuth2UserRecordStub);

        // Stub the common, basic user identifiers
        UserRecordIdentifier userRecordIdentifierStub = Mockito.mock(UserRecordIdentifier.class);
        Mockito.when(userRecordIdentifierStub.getUserIdInRepo()).thenReturn("testUserId");
        Mockito.when(userRecordIdentifierStub.getUserRepoId()).thenReturn("testRepoId");
        Mockito.when(oAuth2UserRecordStub.getUserRecordIdentifier()).thenReturn(userRecordIdentifierStub);
        Mockito.when(oAuth2UserRecordStub.getUsername()).thenReturn("testUsername");

        return oAuth2UserRecordStub;
    }

    private void setUpOIDCProviderWithGenericUser() throws ParseException, IOException, OAuth2Exception {
        setupOidcUserRecordBasics();
    }

    private void setUpOIDCProviderWithBuiltinUserAttributes() throws ParseException, IOException, OAuth2Exception {
        OAuth2UserRecord oAuth2UserRecordStub = setupOidcUserRecordBasics();
        Mockito.when(oAuth2UserRecordStub.hasBuiltinAttributes()).thenReturn(true);
    }

    private void setUpOIDCProviderWithShibAttributes() throws ParseException, IOException, OAuth2Exception {
        OAuth2UserRecord oAuth2UserRecordStub = setupOidcUserRecordBasics();
        Mockito.when(oAuth2UserRecordStub.hasShibAttributes()).thenReturn(true);
        Mockito.when(oAuth2UserRecordStub.getIdp()).thenReturn("testIdp");
        Mockito.when(oAuth2UserRecordStub.getShibUniquePersistentIdentifier()).thenReturn("testPersistentId");
    }

    private void setUpOIDCProviderWithOAuthAttributes(String providerId) throws ParseException, IOException, OAuth2Exception {
        OAuth2UserRecord oAuth2UserRecordStub = setupOidcUserRecordBasics();
        Mockito.when(oAuth2UserRecordStub.hasOAuthAttributes()).thenReturn(true);

        switch (providerId) {
            case OrcidOAuth2AP.PROVIDER_ID -> {
                Mockito.when(oAuth2UserRecordStub.getIdp()).thenReturn("http://orcid.org/oauth/authorize");
                Mockito.when(oAuth2UserRecordStub.getOidcUserId()).thenReturn("http://orcid.org/" + TEST_ORCID_USER_ID);
            }
            case GoogleOAuth2AP.PROVIDER_ID -> {
                Mockito.when(oAuth2UserRecordStub.getIdp()).thenReturn("http://google.com/accounts/o8/id");
                Mockito.when(oAuth2UserRecordStub.getOidcUserId()).thenReturn(TEST_GOOGLE_USER_ID);
            }
            case GitHubOAuth2AP.PROVIDER_ID -> {
                Mockito.when(oAuth2UserRecordStub.getIdp()).thenReturn("http://github.com/login/oauth/authorize");
                Mockito.when(oAuth2UserRecordStub.getOidcUserId()).thenReturn(TEST_GITHUB_USER_ID);
            }
        }
    }
}
