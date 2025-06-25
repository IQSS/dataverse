package edu.harvard.iq.keycloak.auth.spi.providers;

import edu.harvard.iq.keycloak.auth.spi.adapters.DataverseUserAdapter;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseUser;
import edu.harvard.iq.keycloak.auth.spi.services.DataverseAuthenticationService;
import edu.harvard.iq.keycloak.auth.spi.services.DataverseUserService;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

/**
 * DataverseUserStorageProvider integrates Keycloak with Dataverse user storage.
 * It enables authentication and retrieval of users from a Dataverse-based user store.
 */
public class DataverseUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator {

    private static final Logger logger = Logger.getLogger(DataverseUserStorageProvider.class);

    private final ComponentModel model;
    private final KeycloakSession session;
    private final DataverseUserService dataverseUserService;

    public DataverseUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;

        String datasource = model.getConfig().getFirst("datasource");
        logger.debugf("Using datasource: %s", datasource);
        this.dataverseUserService = new DataverseUserService(session, datasource);
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        DataverseUser dataverseUser = dataverseUserService.getUserById(id);
        return (dataverseUser != null) ? new DataverseUserAdapter(session, realm, model, dataverseUser) : null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        DataverseUser dataverseUser = dataverseUserService.getUserByUsername(username);
        return (dataverseUser != null) ? new DataverseUserAdapter(session, realm, model, dataverseUser) : null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        DataverseUser dataverseUser = dataverseUserService.getUserByEmail(email);
        return (dataverseUser != null) ? new DataverseUserAdapter(session, realm, model, dataverseUser) : null;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        logger.debugf("Checking credential configuration for user: %s, credentialType: %s", user.getUsername(), credentialType);
        return false;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        logger.debugf("Validating credentials for user: %s", user.getUsername());

        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel userCredential)) {
            return false;
        }

        DataverseAuthenticationService dataverseAuthenticationService = new DataverseAuthenticationService(dataverseUserService);
        return dataverseAuthenticationService.canLogInAsBuiltinUser(user.getUsername(), userCredential.getValue());
    }

    @Override
    public void close() {
        logger.debug("Closing DataverseUserStorageProvider");
        this.dataverseUserService.close();
    }
}
