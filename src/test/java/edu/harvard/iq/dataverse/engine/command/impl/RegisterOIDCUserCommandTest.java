package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.api.dto.UserDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2UserRecord;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidFieldsCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@LocalJvmSettings
class RegisterOIDCUserCommandTest {

    private static final String TEST_BEARER_TOKEN = "Bearer test";
    private static final String TEST_USERNAME = "username";
    private static final AuthenticatedUserDisplayInfo TEST_MISSING_CLAIMS_DISPLAY_INFO = new AuthenticatedUserDisplayInfo(
            null,
            null,
            null,
            "",
            ""
    );
    private static final AuthenticatedUserDisplayInfo TEST_VALID_DISPLAY_INFO = new AuthenticatedUserDisplayInfo(
            "FirstName",
            "LastName",
            "user@example.com",
            "",
            ""
    );

    private UserDTO testUserDTO;

    @Mock
    private CommandContext contextStub;

    @Mock
    private AuthenticationServiceBean authServiceStub;

    @InjectMocks
    private RegisterOIDCUserCommand sut;

    private OAuth2UserRecord oAuth2UserRecordStub;
    private UserRecordIdentifier userRecordIdentifierMock;
    private AuthenticatedUser existingTestUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        setUpDefaultUserDTO();

        userRecordIdentifierMock = mock(UserRecordIdentifier.class);
        oAuth2UserRecordStub = mock(OAuth2UserRecord.class);
        existingTestUser = new AuthenticatedUser();

        when(oAuth2UserRecordStub.getUserRecordIdentifier()).thenReturn(userRecordIdentifierMock);
        when(contextStub.authentication()).thenReturn(authServiceStub);

        sut = new RegisterOIDCUserCommand(makeRequest(), TEST_BEARER_TOKEN, testUserDTO);
    }

    private void setUpDefaultUserDTO() {
        testUserDTO = new UserDTO();
        testUserDTO.setTermsAccepted(true);
        testUserDTO.setFirstName("FirstName");
        testUserDTO.setLastName("LastName");
        testUserDTO.setUsername("username");
        testUserDTO.setEmailAddress("user@example.com");
    }

    @Test
    public void execute_completedUserDTOWithUnacceptedTerms_missingClaimsInProvider_provideMissingClaimsFeatureFlagDisabled() throws AuthorizationException {
        testUserDTO.setTermsAccepted(false);
        testUserDTO.setEmailAddress(null);
        testUserDTO.setUsername(null);
        testUserDTO.setFirstName(null);
        testUserDTO.setLastName(null);

        when(authServiceStub.getAuthenticatedUserByEmail(testUserDTO.getEmailAddress())).thenReturn(null);
        when(authServiceStub.getAuthenticatedUser(testUserDTO.getUsername())).thenReturn(null);
        when(authServiceStub.verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN)).thenReturn(oAuth2UserRecordStub);

        when(oAuth2UserRecordStub.getUsername()).thenReturn(null);
        when(oAuth2UserRecordStub.getDisplayInfo()).thenReturn(TEST_MISSING_CLAIMS_DISPLAY_INFO);

        assertThatThrownBy(() -> sut.execute(contextStub))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(exception -> {
                    InvalidFieldsCommandException ex = (InvalidFieldsCommandException) exception;
                    assertThat(ex.getFieldErrors())
                            .containsEntry("termsAccepted", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userShouldAcceptTerms"))
                            .containsEntry("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.fieldRequired", List.of("emailAddress")))
                            .containsEntry("username", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.fieldRequired", List.of("username")))
                            .containsEntry("firstName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.fieldRequired", List.of("firstName")))
                            .containsEntry("lastName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.fieldRequired", List.of("lastName")));
                });
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-provide-missing-claims")
    public void execute_uncompletedUserDTOWithUnacceptedTerms_missingClaimsInProvider_provideMissingClaimsFeatureFlagEnabled() throws AuthorizationException {
        testUserDTO.setTermsAccepted(false);
        testUserDTO.setEmailAddress(null);
        testUserDTO.setUsername(null);
        testUserDTO.setFirstName(null);
        testUserDTO.setLastName(null);

        when(oAuth2UserRecordStub.getUsername()).thenReturn(null);
        when(oAuth2UserRecordStub.getDisplayInfo()).thenReturn(TEST_MISSING_CLAIMS_DISPLAY_INFO);

        when(authServiceStub.getAuthenticatedUserByEmail(testUserDTO.getEmailAddress())).thenReturn(null);
        when(authServiceStub.getAuthenticatedUser(testUserDTO.getUsername())).thenReturn(null);
        when(authServiceStub.verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN)).thenReturn(oAuth2UserRecordStub);

        assertThatThrownBy(() -> sut.execute(contextStub))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(exception -> {
                    InvalidFieldsCommandException ex = (InvalidFieldsCommandException) exception;
                    assertThat(ex.getFieldErrors())
                            .containsEntry("termsAccepted", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userShouldAcceptTerms"))
                            .containsEntry("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldRequired", List.of("emailAddress")))
                            .containsEntry("username", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldRequired", List.of("username")))
                            .containsEntry("firstName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldRequired", List.of("firstName")))
                            .containsEntry("lastName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldRequired", List.of("lastName")));
                });
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-provide-missing-claims")
    public void execute_acceptedTerms_unavailableEmailAndUsername_missingClaimsInProvider_provideMissingClaimsFeatureFlagEnabled() throws AuthorizationException {
        when(authServiceStub.getAuthenticatedUserByEmail(testUserDTO.getEmailAddress())).thenReturn(existingTestUser);
        when(authServiceStub.getAuthenticatedUser(testUserDTO.getUsername())).thenReturn(existingTestUser);
        when(authServiceStub.verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN)).thenReturn(oAuth2UserRecordStub);

        when(oAuth2UserRecordStub.getUsername()).thenReturn(null);
        when(oAuth2UserRecordStub.getDisplayInfo()).thenReturn(TEST_MISSING_CLAIMS_DISPLAY_INFO);

        assertThatThrownBy(() -> sut.execute(contextStub))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(exception -> {
                    InvalidFieldsCommandException ex = (InvalidFieldsCommandException) exception;
                    assertThat(ex.getFieldErrors())
                            .containsEntry("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.emailAddressInUse"))
                            .containsEntry("username", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.usernameInUse"))
                            .doesNotContainKey("termsAccepted");
                });
    }

    @Test
    void execute_throwsPermissionException_onAuthorizationException() throws AuthorizationException {
        String testAuthorizationExceptionMessage = "Authorization failed";
        when(contextStub.authentication().verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN))
                .thenThrow(new AuthorizationException(testAuthorizationExceptionMessage));

        assertThatThrownBy(() -> sut.execute(contextStub))
                .isInstanceOf(PermissionException.class)
                .hasMessageContaining(testAuthorizationExceptionMessage);

        verify(contextStub.authentication(), times(1)).verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN);
    }

    @Test
    void execute_throwsIllegalCommandException_ifUserAlreadyRegisteredWithToken() throws AuthorizationException {
        when(contextStub.authentication().verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN))
                .thenReturn(oAuth2UserRecordStub);
        when(contextStub.authentication().lookupUser(userRecordIdentifierMock)).thenReturn(new AuthenticatedUser());

        assertThatThrownBy(() -> sut.execute(contextStub))
                .isInstanceOf(IllegalCommandException.class)
                .hasMessageContaining(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userAlreadyRegisteredWithToken"));

        verify(contextStub.authentication(), times(1)).lookupUser(userRecordIdentifierMock);
    }

    @Test
    void execute_throwsInvalidFieldsCommandException_ifUserDTOHasClaimsAndProvideMissingClaimsFeatureFlagIsDisabled() throws AuthorizationException {
        when(contextStub.authentication().verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN))
                .thenReturn(oAuth2UserRecordStub);

        assertThatThrownBy(() -> sut.execute(contextStub))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(exception -> {
                    InvalidFieldsCommandException ex = (InvalidFieldsCommandException) exception;
                    assertThat(ex.getFieldErrors())
                            .containsEntry("username", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.unableToSetFieldViaJSON", List.of("username")))
                            .containsEntry("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.unableToSetFieldViaJSON", List.of("emailAddress")))
                            .containsEntry("firstName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.unableToSetFieldViaJSON", List.of("firstName")))
                            .containsEntry("lastName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.unableToSetFieldViaJSON", List.of("lastName")))
                            .containsEntry("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.unableToSetFieldViaJSON", List.of("emailAddress")));
                });
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-provide-missing-claims")
    void execute_happyPath_withoutAffiliationAndPosition_missingClaimsInProvider_provideMissingClaimsFeatureFlagEnabled() throws AuthorizationException, CommandException {
        when(authServiceStub.verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN)).thenReturn(oAuth2UserRecordStub);

        when(oAuth2UserRecordStub.getUsername()).thenReturn(null);
        when(oAuth2UserRecordStub.getDisplayInfo()).thenReturn(TEST_MISSING_CLAIMS_DISPLAY_INFO);

        sut.execute(contextStub);

        verify(authServiceStub, times(1)).createAuthenticatedUser(
                eq(userRecordIdentifierMock),
                eq(testUserDTO.getUsername()),
                eq(new AuthenticatedUserDisplayInfo(
                        testUserDTO.getFirstName(),
                        testUserDTO.getLastName(),
                        testUserDTO.getEmailAddress(),
                        "",
                        "")
                ),
                eq(true)
        );
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-provide-missing-claims")
    void execute_happyPath_withAffiliationAndPosition_missingClaimsInProvider_provideMissingClaimsFeatureFlagEnabled() throws AuthorizationException, CommandException {
        testUserDTO.setPosition("test position");
        testUserDTO.setAffiliation("test affiliation");

        when(authServiceStub.verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN)).thenReturn(oAuth2UserRecordStub);

        when(oAuth2UserRecordStub.getUsername()).thenReturn(null);
        when(oAuth2UserRecordStub.getDisplayInfo()).thenReturn(TEST_MISSING_CLAIMS_DISPLAY_INFO);

        sut.execute(contextStub);

        verify(authServiceStub, times(1)).createAuthenticatedUser(
                eq(userRecordIdentifierMock),
                eq(testUserDTO.getUsername()),
                eq(new AuthenticatedUserDisplayInfo(
                        testUserDTO.getFirstName(),
                        testUserDTO.getLastName(),
                        testUserDTO.getEmailAddress(),
                        testUserDTO.getAffiliation(),
                        testUserDTO.getPosition())
                ),
                eq(true)
        );
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-provide-missing-claims")
    void execute_conflictingClaimsInProvider_provideMissingClaimsFeatureFlagEnabled() throws AuthorizationException {
        when(authServiceStub.verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN)).thenReturn(oAuth2UserRecordStub);

        when(oAuth2UserRecordStub.getUsername()).thenReturn(TEST_USERNAME);
        when(oAuth2UserRecordStub.getDisplayInfo()).thenReturn(TEST_VALID_DISPLAY_INFO);

        testUserDTO.setUsername("conflictingUsername");
        testUserDTO.setFirstName("conflictingFirstName");
        testUserDTO.setLastName("conflictingLastName");
        testUserDTO.setEmailAddress("conflictingemail@example.com");

        assertThatThrownBy(() -> sut.execute(contextStub))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(exception -> {
                    InvalidFieldsCommandException ex = (InvalidFieldsCommandException) exception;
                    assertThat(ex.getFieldErrors())
                            .containsEntry("username", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldAlreadyPresentInProvider", List.of("username")))
                            .containsEntry("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldAlreadyPresentInProvider", List.of("emailAddress")))
                            .containsEntry("firstName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldAlreadyPresentInProvider", List.of("firstName")))
                            .containsEntry("lastName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldAlreadyPresentInProvider", List.of("lastName")))
                            .containsEntry("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldAlreadyPresentInProvider", List.of("emailAddress")));
                });
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-provide-missing-claims")
    void execute_happyPath_withoutAffiliationAndPosition_claimsInProvider_provideMissingClaimsFeatureFlagEnabled() throws AuthorizationException, CommandException {
        testUserDTO.setTermsAccepted(true);
        testUserDTO.setEmailAddress(null);
        testUserDTO.setUsername(null);
        testUserDTO.setFirstName(null);
        testUserDTO.setLastName(null);

        when(authServiceStub.verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN)).thenReturn(oAuth2UserRecordStub);

        when(oAuth2UserRecordStub.getUsername()).thenReturn(TEST_USERNAME);
        when(oAuth2UserRecordStub.getDisplayInfo()).thenReturn(TEST_VALID_DISPLAY_INFO);

        sut.execute(contextStub);

        verify(authServiceStub, times(1)).createAuthenticatedUser(
                eq(userRecordIdentifierMock),
                eq(TEST_USERNAME),
                eq(new AuthenticatedUserDisplayInfo(
                        TEST_VALID_DISPLAY_INFO.getFirstName(),
                        TEST_VALID_DISPLAY_INFO.getLastName(),
                        TEST_VALID_DISPLAY_INFO.getEmailAddress(),
                        "",
                        "")
                ),
                eq(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", ""})
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-provide-missing-claims")
    void execute_happyPath_withoutAffiliationAndPosition_blankClaimInProviderProvidedInJson_provideMissingClaimsFeatureFlagEnabled(String testBlankUsername) throws AuthorizationException, CommandException {
        String testUsernameNotBlank = "usernameNotBlank";
        testUserDTO.setUsername(testUsernameNotBlank);
        testUserDTO.setTermsAccepted(true);
        testUserDTO.setEmailAddress(null);
        testUserDTO.setFirstName(null);
        testUserDTO.setLastName(null);

        when(authServiceStub.verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN)).thenReturn(oAuth2UserRecordStub);

        when(oAuth2UserRecordStub.getUsername()).thenReturn(testBlankUsername);
        when(oAuth2UserRecordStub.getDisplayInfo()).thenReturn(TEST_VALID_DISPLAY_INFO);

        sut.execute(contextStub);

        verify(authServiceStub, times(1)).createAuthenticatedUser(
                eq(userRecordIdentifierMock),
                eq(testUsernameNotBlank),
                eq(new AuthenticatedUserDisplayInfo(
                        TEST_VALID_DISPLAY_INFO.getFirstName(),
                        TEST_VALID_DISPLAY_INFO.getLastName(),
                        TEST_VALID_DISPLAY_INFO.getEmailAddress(),
                        "",
                        "")
                ),
                eq(true)
        );
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-handle-tos-acceptance-in-idp")
    void execute_doNotThrowUnacceptedTermsError_unacceptedTermsInUserDTOAndAllClaimsInProvider_handleTosAcceptanceInIdpFeatureFlagEnabled() throws AuthorizationException {
        testUserDTO.setTermsAccepted(false);
        testUserDTO.setEmailAddress(null);
        testUserDTO.setUsername(null);
        testUserDTO.setFirstName(null);
        testUserDTO.setLastName(null);

        when(authServiceStub.verifyOIDCBearerTokenAndGetOAuth2UserRecord(TEST_BEARER_TOKEN)).thenReturn(oAuth2UserRecordStub);

        when(oAuth2UserRecordStub.getUsername()).thenReturn(TEST_USERNAME);
        when(oAuth2UserRecordStub.getDisplayInfo()).thenReturn(TEST_VALID_DISPLAY_INFO);

        assertDoesNotThrow(() -> sut.execute(contextStub));
    }
}
