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

public class DataverseUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator {

    private static final Logger logger = Logger.getLogger(DataverseUserStorageProvider.class);

    protected ComponentModel model;
    protected KeycloakSession session;
    protected EntityManager em;

    DataverseUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        em = session.getProvider(JpaConnectionProvider.class, "user-store").getEntityManager();
    }

    @Override
    public UserModel getUserById(RealmModel realmModel, String id) {
        logger.info("getUserById - id: " + id);
        String persistenceId = StorageId.externalId(id);
        logger.info("getUserById - persistenceId: " + persistenceId);
        DataverseBuiltinUser builtinUser = em.find(DataverseBuiltinUser.class, persistenceId);
        if (builtinUser == null) {
            logger.info("Could not find builtin user by id: " + persistenceId);
            return null;
        }
        String username = builtinUser.getUsername();
        DataverseAuthenticatedUser authenticatedUser = getAuthenticatedUserByUsername(username);
        if (authenticatedUser == null) {
            return null;
        }
        return new DataverseUserAdapter(session, realmModel, model, builtinUser, authenticatedUser);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realmModel, String username) {
        logger.info("getUserByUsername: " + username);
        TypedQuery<DataverseBuiltinUser> query = em.createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class);
        query.setParameter("username", username);
        List<DataverseBuiltinUser> builtinUsersResult = query.getResultList();
        if (builtinUsersResult.isEmpty()) {
            logger.info("Could not find builtin user by username: " + username);
            return null;
        }
        DataverseAuthenticatedUser authenticatedUser = getAuthenticatedUserByUsername(username);
        if (authenticatedUser == null) {
            return null;
        }
        return new DataverseUserAdapter(session, realmModel, model, builtinUsersResult.get(0), authenticatedUser);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realmModel, String email) {
        logger.info("getUserByEmail: " + email);
        TypedQuery<DataverseAuthenticatedUser> authenticatedUserQuery = em.createNamedQuery("DataverseAuthenticatedUser.findByEmail", DataverseAuthenticatedUser.class);
        authenticatedUserQuery.setParameter("email", email);
        List<DataverseAuthenticatedUser> authenticatedUsersResult = authenticatedUserQuery.getResultList();
        if (authenticatedUsersResult.isEmpty()) {
            logger.info("Could not find authenticated user by email: " + email);
            return null;
        }
        DataverseAuthenticatedUser authenticatedUser = authenticatedUsersResult.get(0);
        TypedQuery<DataverseBuiltinUser> builtinUserQuery = em.createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class);
        String username = authenticatedUser.getUserIdentifier();
        builtinUserQuery.setParameter("username", username);
        List<DataverseBuiltinUser> builtinUsersResult = builtinUserQuery.getResultList();
        if (builtinUsersResult.isEmpty()) {
            logger.info("Could not find builtin user by username: " + username);
            return null;
        }
        return new DataverseUserAdapter(session, realmModel, model, builtinUsersResult.get(0), authenticatedUser);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realmModel, UserModel userModel, String credentialType) {
        logger.info("isConfiguredFor called for user: " + userModel.getUsername() + " and credentialType: " + credentialType);
        return false;
    }

    @Override
    public void close() {
        logger.info("Closing DataverseUserStorageProvider");
        if (em != null) {
            em.close();
        }
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        logger.info("isValid called for user: " + user.getUsername());
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel userCredential))
            return false;
        String username = user.getUsername();
        String password = userCredential.getValue();
        return DataverseAPIService.canLogInAsBuiltinUser(username, password);
    }

    private DataverseAuthenticatedUser getAuthenticatedUserByUsername(String username) {
        TypedQuery<DataverseAuthenticatedUser> query = em.createNamedQuery("DataverseAuthenticatedUser.findByIdentifier", DataverseAuthenticatedUser.class);
        query.setParameter("identifier", username);
        DataverseAuthenticatedUser singleResult = query.getSingleResult();
        if (singleResult == null) {
            logger.info("Could not find authenticated user by username: " + username);
        }
        return singleResult;
    }
}
