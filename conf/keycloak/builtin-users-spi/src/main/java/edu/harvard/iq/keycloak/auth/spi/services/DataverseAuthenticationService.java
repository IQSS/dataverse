package edu.harvard.iq.keycloak.auth.spi.services;

import edu.harvard.iq.keycloak.auth.spi.models.DataverseBuiltinUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseUser;

public class DataverseAuthenticationService {

    private final DataverseUserService dataverseUserService;

    public DataverseAuthenticationService(DataverseUserService dataverseUserService) {
        this.dataverseUserService = dataverseUserService;
    }

    /**
     * Validates if a Dataverse built-in user can log in with the given credentials.
     *
     * @param usernameOrEmail The username or email of the Dataverse built-in user.
     * @param password        The password to be validated.
     * @return {@code true} if the user can log in, {@code false} otherwise.
     */
    public boolean canLogInAsBuiltinUser(String usernameOrEmail, String password) {
        DataverseUser dataverseUser = this.dataverseUserService.getUserByUsername(usernameOrEmail);

        if (dataverseUser == null) {
            dataverseUser = this.dataverseUserService.getUserByEmail(usernameOrEmail);
        }

        if (dataverseUser == null) {
            return false;
        }

        DataverseBuiltinUser builtinUser = dataverseUser.getBuiltinUser();
        return PasswordEncryption.getVersion(builtinUser.getPasswordEncryptionVersion())
                .check(password, builtinUser.getEncryptedPassword());
    }
}
