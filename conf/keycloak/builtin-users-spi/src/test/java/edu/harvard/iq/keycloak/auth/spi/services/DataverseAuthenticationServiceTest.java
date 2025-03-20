package edu.harvard.iq.keycloak.auth.spi.services;

import edu.harvard.iq.keycloak.auth.spi.models.DataverseBuiltinUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataverseAuthenticationServiceTest {

    private DataverseUserService dataverseUserServiceMock;
    private PasswordEncryption.Algorithm passwordEncryptionAlgorithmMock;
    private DataverseAuthenticationService sut;

    @BeforeEach
    void setUp() {
        dataverseUserServiceMock = mock(DataverseUserService.class);
        passwordEncryptionAlgorithmMock = mock(PasswordEncryption.Algorithm.class);
        sut = new DataverseAuthenticationService(dataverseUserServiceMock, passwordEncryptionAlgorithmMock);
    }

    @Test
    void canLogInAsBuiltinUser_userFoundByUsername_validCredentials() {
        setupUserMock("username", true, true);
        assertTrue(sut.canLogInAsBuiltinUser("username", "password"));
    }

    @Test
    void canLogInAsBuiltinUser_userFoundByUsername_invalidCredentials() {
        setupUserMock("username", true, false);
        assertFalse(sut.canLogInAsBuiltinUser("username", "password"));
    }

    @Test
    void canLogInAsBuiltinUser_userFoundByEmail_validCredentials() {
        setupUserMock("user@dataverse.org", false, true);
        assertTrue(sut.canLogInAsBuiltinUser("user@dataverse.org", "password"));
    }

    @Test
    void canLogInAsBuiltinUser_userFoundByEmail_invalidCredentials() {
        setupUserMock("user@dataverse.org", false, false);
        assertFalse(sut.canLogInAsBuiltinUser("user@dataverse.org", "password"));
    }

    private void setupUserMock(String identifier, boolean foundByUsername, boolean validPassword) {
        String encryptedPassword = "encryptedPassword";
        DataverseUser dataverseUserMock = mock(DataverseUser.class);
        DataverseBuiltinUser dataverseBuiltinUser = new DataverseBuiltinUser();
        dataverseBuiltinUser.setEncryptedPassword(encryptedPassword);

        when(dataverseUserMock.getBuiltinUser()).thenReturn(dataverseBuiltinUser);
        when(passwordEncryptionAlgorithmMock.check(anyString(), eq(encryptedPassword))).thenReturn(validPassword);

        if (foundByUsername) {
            when(dataverseUserServiceMock.getUserByUsername(identifier)).thenReturn(dataverseUserMock);
        } else {
            when(dataverseUserServiceMock.getUserByUsername(identifier)).thenReturn(null);
            when(dataverseUserServiceMock.getUserByEmail(identifier)).thenReturn(dataverseUserMock);
        }
    }
}
