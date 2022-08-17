package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.saml.SamlUserData;
import edu.harvard.iq.dataverse.authorization.providers.saml.TestSamlUserDataCreator;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.harvard.iq.dataverse.authorization.SamlLoginIssue.Type;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class SamlDataUpdateServiceTest {

    @Mock
    private ActionLogServiceBean actionLog;

    @Mock
    private AuthenticationServiceBean authenticationService;

    @InjectMocks
    private SamlDataUpdateService service;

    @BeforeEach
    void setUp() {
        Mockito.lenient()
                .when(authenticationService.update(Mockito.any(AuthenticatedUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should update user if all data is valid")
    void updateIfValid() {
        // given
        AuthenticatedUser user = createTestUser();
        SamlUserData samlUserData = TestSamlUserDataCreator.create("NewName", "NewSurname", "new.mail@icm.ceon.pl");

        // when
        Either<SamlLoginIssue, AuthenticatedUser> result = service.updateUserIfNeeded(user, samlUserData);

        // then
        Mockito.verify(authenticationService, Mockito.times(1)).update(user);
        assertThat(result.isRight()).isTrue();
    }

    @Test
    @DisplayName("Should not update if data is unchanged")
    void doNotUpdateIfUnchanged() {
        // given
        AuthenticatedUser user = createTestUser();
        SamlUserData samlUserData = TestSamlUserDataCreator.create(user.getFirstName(), user.getLastName(), user.getEmail());

        // when
        Either<SamlLoginIssue, AuthenticatedUser> result = service.updateUserIfNeeded(user, samlUserData);

        // then
        Mockito.verify(authenticationService, Mockito.never()).update(Mockito.any());
        assertThat(result.isRight()).isTrue();
    }

    @Test
    @DisplayName("Should not update if new data is not complete")
    void doNotUpdateWhenIncompleteData() {
        // given
        AuthenticatedUser user = createTestUser();
        SamlUserData samlUserData = TestSamlUserDataCreator.create("", "", "");

        // when
        Either<SamlLoginIssue, AuthenticatedUser> result = service.updateUserIfNeeded(user, samlUserData);

        // then
        Mockito.verify(authenticationService, Mockito.never()).update(Mockito.any());
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).extracting(u -> u.type).isEqualTo(Type.INCOMPLETE_DATA);
    }

    @Test
    @DisplayName("Should not update if new data is invalid")
    void doNotUpdateWhenInvalidData() {
        // given
        AuthenticatedUser user = createTestUser();
        SamlUserData samlUserData = TestSamlUserDataCreator.create("FN", "LN", "123678.123.12");

        // when
        Either<SamlLoginIssue, AuthenticatedUser> result = service.updateUserIfNeeded(user, samlUserData);

        // then
        Mockito.verify(authenticationService, Mockito.never()).update(Mockito.any());
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft())
                .extracting(u -> u.type)
                .isEqualTo(Type.INVALID_DATA);
    }

    @Test
    @DisplayName("Should not update if new email is already used by another user")
    void doNotUpdateWhenDuplicatedEmail() {
        // given
        String duplicatedMail = "new.mail@icm.ceon.pl";
        AuthenticatedUser user = createTestUser();
        SamlUserData samlUserData = TestSamlUserDataCreator.create(user.getFirstName(), user.getLastName(), duplicatedMail);

        AuthenticatedUser existingUserWithSameEmail = new AuthenticatedUser();
        existingUserWithSameEmail.setUserIdentifier("other");
        existingUserWithSameEmail.setEmail(duplicatedMail);
        Mockito.when(authenticationService.getAuthenticatedUserByEmail(duplicatedMail))
                .thenReturn(existingUserWithSameEmail);

        // when
        Either<SamlLoginIssue, AuthenticatedUser> result = service.updateUserIfNeeded(user, samlUserData);

        // then
        Mockito.verify(authenticationService, Mockito.never()).update(Mockito.any());
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).extracting(u -> u.type)
                .isEqualTo(Type.DUPLICATED_EMAIL);
    }

    // -------------------- PRIVATE --------------------

    private AuthenticatedUser createTestUser() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setUserIdentifier("saml-user");
        user.setFirstName("FirstName");
        user.setLastName("LastName");
        user.setEmail("email@icm.ceon.pl");
        return user;
    }
}