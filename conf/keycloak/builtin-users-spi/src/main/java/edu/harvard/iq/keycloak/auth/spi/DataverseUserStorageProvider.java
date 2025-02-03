package edu.harvard.iq.keycloak.auth.spi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.StorageId;

import java.util.List;

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
    private final EntityManager em;

    public DataverseUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        this.em = session.getProvider(JpaConnectionProvider.class, "user-store").getEntityManager();
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        logger.infof("Fetching user by ID: %s", id);
        String persistenceId = StorageId.externalId(id);

        DataverseBuiltinUser builtinUser = em.find(DataverseBuiltinUser.class, persistenceId);
        if (builtinUser == null) {
            logger.infof("User not found for external ID: %s", persistenceId);
            return null;
        }

        DataverseAuthenticatedUser authenticatedUser = getAuthenticatedUserByUsername(builtinUser.getUsername());
        return (authenticatedUser != null) ? new DataverseUserAdapter(session, realm, model, builtinUser, authenticatedUser) : null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        logger.infof("Fetching user by username: %s", username);
        List<DataverseBuiltinUser> users = em.createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class)
                .setParameter("username", username)
                .getResultList();

        if (users.isEmpty()) {
            logger.infof("User not found by username: %s", username);
            return null;
        }

        DataverseAuthenticatedUser authenticatedUser = getAuthenticatedUserByUsername(username);
        return (authenticatedUser != null) ? new DataverseUserAdapter(session, realm, model, users.get(0), authenticatedUser) : null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        logger.infof("Fetching user by email: %s", email);
        List<DataverseAuthenticatedUser> authUsers = em.createNamedQuery("DataverseAuthenticatedUser.findByEmail", DataverseAuthenticatedUser.class)
                .setParameter("email", email)
                .getResultList();

        if (authUsers.isEmpty()) {
            logger.infof("User not found by email: %s", email);
            return null;
        }

        String username = authUsers.get(0).getUserIdentifier();
        List<DataverseBuiltinUser> builtinUsers = em.createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class)
                .setParameter("username", username)
                .getResultList();

        return (builtinUsers.isEmpty()) ? null : new DataverseUserAdapter(session, realm, model, builtinUsers.get(0), authUsers.get(0));
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        logger.infof("Checking credential configuration for user: %s, credentialType: %s", user.getUsername(), credentialType);
        return false;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        logger.infof("Validating credentials for user: %s", user.getUsername());

        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel userCredential)) {
            return false;
        }

        return DataverseAPIService.canLogInAsBuiltinUser(user.getUsername(), userCredential.getValue());
    }

    @Override
    public void close() {
        logger.info("Closing DataverseUserStorageProvider");
        if (em != null) {
            em.close();
        }
    }

    /**
     * Retrieves an authenticated user from Dataverse by username.
     *
     * @param username The username to look up.
     * @return The authenticated user or null if not found.
     */
    private DataverseAuthenticatedUser getAuthenticatedUserByUsername(String username) {
        try {
            return em.createNamedQuery("DataverseAuthenticatedUser.findByIdentifier", DataverseAuthenticatedUser.class)
                    .setParameter("identifier", username)
                    .getSingleResult();
        } catch (Exception e) {
            logger.infof("Could not find authenticated user by username: %s", username);
            return null;
        }
    }
}
