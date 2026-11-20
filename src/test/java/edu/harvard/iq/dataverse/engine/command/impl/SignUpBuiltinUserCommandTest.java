package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.api.dto.UserDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidFieldsCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.persistence.EntityManager;

import java.util.Date;
import java.util.List;

import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignUpBuiltinUserCommandTest {

    private static final String PASSWORD = "Str0ngPassw0rd";

    private CommandContext context;
    private AuthenticationServiceBean authService;
    private BuiltinUserServiceBean builtinUserService;
    private PasswordValidatorServiceBean passwordValidator;
    private UserNotificationServiceBean notificationService;
    private EntityManager entityManager;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        context = mock(CommandContext.class);
        authService = mock(AuthenticationServiceBean.class);
        builtinUserService = mock(BuiltinUserServiceBean.class);
        passwordValidator = mock(PasswordValidatorServiceBean.class);
        notificationService = mock(UserNotificationServiceBean.class);
        entityManager = mock(EntityManager.class);

        when(context.authentication()).thenReturn(authService);
        when(context.builtinUsers()).thenReturn(builtinUserService);
        when(context.passwordValidation()).thenReturn(passwordValidator);
        when(context.notifications()).thenReturn(notificationService);
        when(context.em()).thenReturn(entityManager);
        when(entityManager.merge(any(AuthenticatedUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordValidator.validate(anyString(), any(Date.class), anyBoolean())).thenReturn(List.of());

        userDTO = new UserDTO();
        userDTO.setUsername("alovelace");
        userDTO.setFirstName("Ada");
        userDTO.setLastName("Lovelace");
        userDTO.setEmailAddress("ada@example.edu");
        userDTO.setAffiliation("Harvard University");
        userDTO.setPosition("Researcher");
        userDTO.setTermsAccepted(true);
    }

    @Test
    void execute_createsBuiltinUserAndSendsWelcomeNotification() throws Exception {
        AuthenticatedUser createdUser = new AuthenticatedUser();
        when(authService.createAuthenticatedUser(any(UserRecordIdentifier.class), eq("alovelace"), any(AuthenticatedUserDisplayInfo.class), eq(false)))
                .thenReturn(createdUser);

        AuthenticatedUser result = new SignUpBuiltinUserCommand(makeRequest(), userDTO, PASSWORD).execute(context);

        assertThat(result).isSameAs(createdUser);

        ArgumentCaptor<UserRecordIdentifier> recordId = ArgumentCaptor.forClass(UserRecordIdentifier.class);
        ArgumentCaptor<AuthenticatedUserDisplayInfo> displayInfo = ArgumentCaptor.forClass(AuthenticatedUserDisplayInfo.class);
        verify(authService).createAuthenticatedUser(recordId.capture(), eq("alovelace"), displayInfo.capture(), eq(false));
        assertThat(recordId.getValue().getUserRepoId()).isEqualTo(BuiltinAuthenticationProvider.PROVIDER_ID);
        assertThat(recordId.getValue().getUserIdInRepo()).isEqualTo("alovelace");
        assertThat(displayInfo.getValue().getEmailAddress()).isEqualTo("ada@example.edu");
        assertThat(displayInfo.getValue().getAffiliation()).isEqualTo("Harvard University");

        ArgumentCaptor<BuiltinUser> savedBuiltinUser = ArgumentCaptor.forClass(BuiltinUser.class);
        verify(builtinUserService).save(savedBuiltinUser.capture());
        assertThat(savedBuiltinUser.getValue().getUserName()).isEqualTo("alovelace");
        assertThat(savedBuiltinUser.getValue().getEncryptedPassword()).isNotBlank().isNotEqualTo(PASSWORD);

        verify(notificationService).sendNotification(eq(createdUser), any(), eq(UserNotification.Type.CREATEACC), eq(null));
    }

    @Test
    void execute_reportsEveryInvalidField() {
        userDTO.setUsername("ada@example.edu");
        userDTO.setFirstName(" ");
        userDTO.setLastName(null);
        userDTO.setEmailAddress("not-an-email");
        userDTO.setTermsAccepted(false);
        when(passwordValidator.validate(anyString(), any(Date.class), anyBoolean())).thenReturn(List.of("Too short."));

        assertThatThrownBy(() -> new SignUpBuiltinUserCommand(makeRequest(), userDTO, "short").execute(context))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(e -> assertThat(((InvalidFieldsCommandException) e).getFieldErrors())
                        .containsEntry(SignUpBuiltinUserCommand.FIELD_USERNAME, BundleUtil.getStringFromBundle("user.username.invalid"))
                        .containsEntry(SignUpBuiltinUserCommand.FIELD_EMAIL_ADDRESS, BundleUtil.getStringFromBundle("oauth2.newAccount.emailInvalid"))
                        .containsEntry(SignUpBuiltinUserCommand.FIELD_PASSWORD, "Too short.")
                        .containsEntry(SignUpBuiltinUserCommand.FIELD_TERMS_ACCEPTED, BundleUtil.getStringFromBundle("user.acccountterms.required"))
                        .containsKeys(SignUpBuiltinUserCommand.FIELD_FIRST_NAME, SignUpBuiltinUserCommand.FIELD_LAST_NAME));

        verify(builtinUserService, never()).save(any());
    }

    @Test
    void execute_rejectsUsernameAndEmailAlreadyInUse() {
        when(authService.identifierExists("alovelace")).thenReturn(true);
        when(authService.getAuthenticatedUserByEmail("ada@example.edu")).thenReturn(new AuthenticatedUser());

        assertThatThrownBy(() -> new SignUpBuiltinUserCommand(makeRequest(), userDTO, PASSWORD).execute(context))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(e -> assertThat(((InvalidFieldsCommandException) e).getFieldErrors())
                        .containsEntry(SignUpBuiltinUserCommand.FIELD_USERNAME, BundleUtil.getStringFromBundle("user.username.taken"))
                        .containsEntry(SignUpBuiltinUserCommand.FIELD_EMAIL_ADDRESS, BundleUtil.getStringFromBundle("user.email.taken")));
    }

    @Test
    void execute_rejectsUsernameTakenByBuiltinUserOnly() {
        when(builtinUserService.findByUserName("alovelace")).thenReturn(new BuiltinUser());

        assertThatThrownBy(() -> new SignUpBuiltinUserCommand(makeRequest(), userDTO, PASSWORD).execute(context))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(e -> assertThat(((InvalidFieldsCommandException) e).getFieldErrors())
                        .containsEntry(SignUpBuiltinUserCommand.FIELD_USERNAME, BundleUtil.getStringFromBundle("user.username.taken")));
    }

    @Test
    void execute_requiresPassword() {
        assertThatThrownBy(() -> new SignUpBuiltinUserCommand(makeRequest(), userDTO, null).execute(context))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(e -> assertThat(((InvalidFieldsCommandException) e).getFieldErrors())
                        .containsKey(SignUpBuiltinUserCommand.FIELD_PASSWORD));
    }

    @Test
    void execute_reportsUsernameTakenWhenCreationLosesRace() {
        when(authService.createAuthenticatedUser(any(), anyString(), any(), anyBoolean())).thenReturn(null);

        assertThatThrownBy(() -> new SignUpBuiltinUserCommand(makeRequest(), userDTO, PASSWORD).execute(context))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(e -> assertThat(((InvalidFieldsCommandException) e).getFieldErrors())
                        .containsEntry(SignUpBuiltinUserCommand.FIELD_USERNAME, BundleUtil.getStringFromBundle("user.username.taken")));

        verify(builtinUserService, never()).save(any());
    }

    @Test
    void execute_savesAuthenticatedOrcidWhenProvided() throws Exception {
        String orcidUrl = "https://orcid.org/0000-0002-1825-0097";
        AuthenticatedUser createdUser = new AuthenticatedUser();
        when(authService.createAuthenticatedUser(any(UserRecordIdentifier.class), eq("alovelace"), any(AuthenticatedUserDisplayInfo.class), eq(false)))
                .thenReturn(createdUser);

        AuthenticatedUser result = new SignUpBuiltinUserCommand(makeRequest(), userDTO, PASSWORD, orcidUrl).execute(context);

        assertThat(result.getAuthenticatedOrcid()).isEqualTo(orcidUrl);
        verify(entityManager).merge(createdUser);
    }

    @Test
    void execute_doesNotTouchOrcidWhenNotProvided() throws Exception {
        AuthenticatedUser createdUser = new AuthenticatedUser();
        when(authService.createAuthenticatedUser(any(UserRecordIdentifier.class), eq("alovelace"), any(AuthenticatedUserDisplayInfo.class), eq(false)))
                .thenReturn(createdUser);

        AuthenticatedUser result = new SignUpBuiltinUserCommand(makeRequest(), userDTO, PASSWORD).execute(context);

        assertThat(result.getAuthenticatedOrcid()).isNull();
        verify(entityManager, never()).merge(any());
    }

    @Test
    void execute_rejectsOrcidAlreadyLinkedToAnotherAccount() {
        String orcidUrl = "https://orcid.org/0000-0002-1825-0097";
        when(authService.lookupUserByOrcid(orcidUrl)).thenReturn(new AuthenticatedUser());

        assertThatThrownBy(() -> new SignUpBuiltinUserCommand(makeRequest(), userDTO, PASSWORD, orcidUrl).execute(context))
                .isInstanceOf(InvalidFieldsCommandException.class)
                .satisfies(e -> assertThat(((InvalidFieldsCommandException) e).getFieldErrors())
                        .containsEntry(SignUpBuiltinUserCommand.FIELD_ORCID, BundleUtil.getStringFromBundle("signUpBuiltinUserCommand.errors.orcidInUse")));

        verify(builtinUserService, never()).save(any());
    }
}
