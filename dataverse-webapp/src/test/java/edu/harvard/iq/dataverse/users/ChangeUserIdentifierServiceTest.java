package edu.harvard.iq.dataverse.users;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
public class ChangeUserIdentifierServiceTest {

    @InjectMocks
    private ChangeUserIdentifierService changeUserIdentifierService;

    @Mock
    private AuthenticationServiceBean authenticationService;

    @Mock
    private BuiltinUserServiceBean builtinUserService;

    @Test
    public void changeUserIdentifier_notSuperuser() {
        // given
        AuthenticatedUser user = MocksFactory.makeAuthenticatedUser("Jurek","Kiler");
        user.setSuperuser(false);

        // when
        Exception exception = Assertions.assertThrows(SecurityException.class, () -> {
            changeUserIdentifierService.changeUserIdentifier(user, "oldId", "newId");
        });
        Assertions.assertEquals("Only superusers can change userIdentifiers", exception.getMessage());
    }

    @Test
    public void changeUserIdentifier_OldUserDoesNotExist() {
        // given
        AuthenticatedUser user = MocksFactory.makeAuthenticatedUser("Jurek","Kiler");
        user.setSuperuser(true);
        Mockito.when(authenticationService.getAuthenticatedUser("oldId")).thenReturn(null);

        // when
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            changeUserIdentifierService.changeUserIdentifier(user, "oldId", "newId");
        });
        Assertions.assertEquals("User oldId not found in AuthenticatedUser",
                exception.getMessage());
    }

    @Test
    public void changeUserIdentifier_NoOldIdProvided() {
        // given
        AuthenticatedUser user = MocksFactory.makeAuthenticatedUser("Jurek","Kiler");
        user.setSuperuser(true);

        // when
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            changeUserIdentifierService.changeUserIdentifier(user, null, "newId");
        });
        Assertions.assertEquals("Old identifier provided to change is empty.",
                exception.getMessage());

        exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            changeUserIdentifierService.changeUserIdentifier(user, "", "newId");
        });
        Assertions.assertEquals("Old identifier provided to change is empty.",
                exception.getMessage());
    }

    @Test
    public void changeUserIdentifier_NoNewIdProvided() {
        // given
        AuthenticatedUser user = MocksFactory.makeAuthenticatedUser("Jurek","Kiler");
        user.setSuperuser(true);

        // when
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            changeUserIdentifierService.changeUserIdentifier(user, "oldId", null);
        });
        Assertions.assertEquals("New identifier provided to change is empty.",
                exception.getMessage());

        exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            changeUserIdentifierService.changeUserIdentifier(user, "oldId", "");
        });
        Assertions.assertEquals("New identifier provided to change is empty.",
                exception.getMessage());
    }

    @Test
    public void changeUserIdentifier_NewIdSameAsOldId() {
        // given
        AuthenticatedUser user = MocksFactory.makeAuthenticatedUser("Jurek","Kiler");
        user.setSuperuser(true);

        // when
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            changeUserIdentifierService.changeUserIdentifier(user, "oldId", "oldId");
        });
        Assertions.assertEquals("New identifier must differ from the old.",
                exception.getMessage());
    }

    @Test
    public void changeUserIdentifier_alreadyExists() {
        // given
        AuthenticatedUser user = MocksFactory.makeAuthenticatedUser("Jurek","Kiler");
        user.setSuperuser(true);
        Mockito.when(authenticationService.getAuthenticatedUser(any())).thenReturn(user);

        // when
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            changeUserIdentifierService.changeUserIdentifier(user, "oldId", "newId");
        });
        Assertions.assertEquals("Validation of submitted data failed. Details:  User newId already exists. Cannot use this as new identifier",
                exception.getMessage());
    }

    @Test
    public void changeUserIdentifier_invalidNewId() {
        // given
        AuthenticatedUser user = MocksFactory.makeAuthenticatedUser("Jurek","Kiler");
        user.setSuperuser(true);
        Mockito.when(authenticationService.getAuthenticatedUser(anyString())).thenReturn(user, null);

        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setId(1L);
        Mockito.when(builtinUserService.findByUserName("oldId")).thenReturn(builtinUser);

        String EXPECTED_ERROR_MSG = "Invalid value: >>>x<<< for userName at BuiltinUser{id=1, userName=x} - Username must be between 2 and 60 characters.";

        // when
        Exception exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            changeUserIdentifierService.changeUserIdentifier(user, "oldId", "x");
        });
        Assertions.assertTrue(exception.getMessage().contains(EXPECTED_ERROR_MSG));
    }
}
