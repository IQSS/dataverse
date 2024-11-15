package edu.harvard.iq.dataverse.engine.command.impl;

import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import edu.harvard.iq.dataverse.api.dto.UserDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.OIDCUserInfo;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@LocalJvmSettings
class RegisterOIDCUserCommandTest {

    private static final String TEST_BEARER_TOKEN = "Bearer test";

    private UserDTO userDTO;

    @Mock
    private CommandContext context;

    @Mock
    private AuthenticationServiceBean authServiceMock;

    @InjectMocks
    private RegisterOIDCUserCommand sut;

    private UserRecordIdentifier userRecordIdentifierMock;
    private UserInfo userInfoMock;
    private OIDCUserInfo OIDCUserInfoMock;
    private AuthenticatedUser existingTestUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        setUpDefaultUserDTO();

        userRecordIdentifierMock = mock(UserRecordIdentifier.class);
        userInfoMock = mock(UserInfo.class);
        OIDCUserInfoMock = new OIDCUserInfo(userRecordIdentifierMock, userInfoMock);
        existingTestUser = new AuthenticatedUser();

        when(context.authentication()).thenReturn(authServiceMock);
        sut = new RegisterOIDCUserCommand(makeRequest(), TEST_BEARER_TOKEN, userDTO);
    }

    private void setUpDefaultUserDTO() {
        userDTO = new UserDTO();
        userDTO.setTermsAccepted(true);
        userDTO.setFirstName("FirstName");
        userDTO.setLastName("LastName");
        userDTO.setUsername("username");
        userDTO.setEmailAddress("user@example.com");
    }

    @Test
    public void execute_completedUserDTOWithUnacceptedTerms_provideMissingClaimsDisabled() throws AuthorizationException {
        userDTO.setTermsAccepted(false);
        when(authServiceMock.getAuthenticatedUserByEmail(userDTO.getEmailAddress())).thenReturn(null);
        when(authServiceMock.getAuthenticatedUser(userDTO.getUsername())).thenReturn(null);
        when(authServiceMock.verifyOIDCBearerTokenAndGetUserIdentifier(TEST_BEARER_TOKEN)).thenReturn(OIDCUserInfoMock);

        assertThatThrownBy(() -> sut.execute(context))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(exception -> {
                    InvalidFieldsCommandException ex = (InvalidFieldsCommandException) exception;
                    assertThat(ex.getFieldErrors())
                            .containsEntry("termsAccepted", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userShouldAcceptTerms"))
                            .containsEntry("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.emailAddressFieldRequired"))
                            .containsEntry("username", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.usernameFieldRequired"))
                            .containsEntry("firstName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.firstNameFieldRequired"))
                            .containsEntry("lastName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsDisabled.lastNameFieldRequired"));
                });
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-provide-missing-claims")
    public void execute_uncompletedUserDTOWithUnacceptedTerms_provideMissingClaimsEnabled() throws AuthorizationException {
        userDTO.setTermsAccepted(false);
        userDTO.setEmailAddress(null);
        userDTO.setUsername(null);
        userDTO.setFirstName(null);
        userDTO.setLastName(null);
        when(authServiceMock.getAuthenticatedUserByEmail(userDTO.getEmailAddress())).thenReturn(null);
        when(authServiceMock.getAuthenticatedUser(userDTO.getUsername())).thenReturn(null);
        when(authServiceMock.verifyOIDCBearerTokenAndGetUserIdentifier(TEST_BEARER_TOKEN)).thenReturn(OIDCUserInfoMock);

        assertThatThrownBy(() -> sut.execute(context))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(exception -> {
                    InvalidFieldsCommandException ex = (InvalidFieldsCommandException) exception;
                    assertThat(ex.getFieldErrors())
                            .containsEntry("termsAccepted", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userShouldAcceptTerms"))
                            .containsEntry("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.emailAddressFieldRequired"))
                            .containsEntry("username", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.usernameFieldRequired"))
                            .containsEntry("firstName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.firstNameFieldRequired"))
                            .containsEntry("lastName", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.lastNameFieldRequired"));
                });
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-provide-missing-claims")
    public void execute_acceptedTerms_unavailableEmailAndUsername_provideMissingClaimsEnabled() throws AuthorizationException {
        when(authServiceMock.getAuthenticatedUserByEmail(userDTO.getEmailAddress())).thenReturn(existingTestUser);
        when(authServiceMock.getAuthenticatedUser(userDTO.getUsername())).thenReturn(existingTestUser);
        when(authServiceMock.verifyOIDCBearerTokenAndGetUserIdentifier(TEST_BEARER_TOKEN)).thenReturn(OIDCUserInfoMock);

        assertThatThrownBy(() -> sut.execute(context))
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
        when(context.authentication().verifyOIDCBearerTokenAndGetUserIdentifier(TEST_BEARER_TOKEN))
                .thenThrow(new AuthorizationException(testAuthorizationExceptionMessage));

        assertThatThrownBy(() -> sut.execute(context))
                .isInstanceOf(PermissionException.class)
                .hasMessageContaining(testAuthorizationExceptionMessage);

        verify(context.authentication(), times(1)).verifyOIDCBearerTokenAndGetUserIdentifier(TEST_BEARER_TOKEN);
    }

    @Test
    void execute_throwsIllegalCommandException_ifUserAlreadyRegisteredWithToken() throws AuthorizationException {
        when(context.authentication().verifyOIDCBearerTokenAndGetUserIdentifier(TEST_BEARER_TOKEN))
                .thenReturn(OIDCUserInfoMock);
        when(context.authentication().lookupUser(userRecordIdentifierMock)).thenReturn(new AuthenticatedUser());

        assertThatThrownBy(() -> sut.execute(context))
                .isInstanceOf(IllegalCommandException.class)
                .hasMessageContaining(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userAlreadyRegisteredWithToken"));

        verify(context.authentication(), times(1)).lookupUser(userRecordIdentifierMock);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-provide-missing-claims")
    void execute_happyPath_withoutAffiliationAndPosition_provideMissingClaimsEnabled() throws AuthorizationException, CommandException {
        when(authServiceMock.verifyOIDCBearerTokenAndGetUserIdentifier(TEST_BEARER_TOKEN)).thenReturn(OIDCUserInfoMock);

        sut.execute(context);

        verify(authServiceMock, times(1)).createAuthenticatedUser(
                eq(userRecordIdentifierMock),
                eq(userDTO.getUsername()),
                eq(new AuthenticatedUserDisplayInfo(
                        userDTO.getFirstName(),
                        userDTO.getLastName(),
                        userDTO.getEmailAddress(),
                        "",
                        "")
                ),
                eq(true)
        );
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth-provide-missing-claims")
    void execute_happyPath_withAffiliationAndPosition_provideMissingClaimsEnabled() throws AuthorizationException, CommandException {
        userDTO.setPosition("test position");
        userDTO.setAffiliation("test affiliation");

        when(authServiceMock.verifyOIDCBearerTokenAndGetUserIdentifier(TEST_BEARER_TOKEN)).thenReturn(OIDCUserInfoMock);

        sut.execute(context);

        verify(authServiceMock, times(1)).createAuthenticatedUser(
                eq(userRecordIdentifierMock),
                eq(userDTO.getUsername()),
                eq(new AuthenticatedUserDisplayInfo(
                        userDTO.getFirstName(),
                        userDTO.getLastName(),
                        userDTO.getEmailAddress(),
                        userDTO.getAffiliation(),
                        userDTO.getPosition())
                ),
                eq(true)
        );
    }
}
