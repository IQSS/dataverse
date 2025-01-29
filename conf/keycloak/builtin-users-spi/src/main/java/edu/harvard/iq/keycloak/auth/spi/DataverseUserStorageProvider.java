package edu.harvard.iq.keycloak.auth.spi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryMethodsProvider;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DataverseUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        UserQueryMethodsProvider {

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
        logger.info("getUserById: " + id);
        DataverseBuiltinUser user = em.find(DataverseBuiltinUser.class, id);
        if (user == null) {
            logger.info("could not find builtin user by id: " + id);
            return null;
        }
        String username = user.getUsername();
        DataverseAuthenticatedUser authenticatedUser = getAuthenticatedUserByUsername(username);
        if (authenticatedUser == null) {
            return null;
        }
        return new DataverseUserAdapter(session, realmModel, model, user, authenticatedUser);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realmModel, String username) {
        logger.info("getUserByUsername: " + username);
        TypedQuery<DataverseBuiltinUser> query = em.createNamedQuery("DataverseUser.findByUsername", DataverseBuiltinUser.class);
        query.setParameter("username", username);
        List<DataverseBuiltinUser> builtinUsersResult = query.getResultList();
        if (builtinUsersResult.isEmpty()) {
            logger.info("User not found: " + username);
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
        logger.info("getUserByEmail is not supported in DataverseUserStorageProvider");
        return null;
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
    public boolean isValid(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        logger.info("isValid is not supported in DataverseUserStorageProvider");
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
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        // TODO search by email too
        String search = params.get(UserModel.SEARCH);
        logger.info("searchForUserStream: " + search);
        String lower = search != null ? search.toLowerCase() : "";
        TypedQuery<DataverseBuiltinUser> query = em.createNamedQuery("DataverseUser.findByUsername", DataverseBuiltinUser.class);
        query.setParameter("username", lower);
        return query.getResultStream().map(entity -> new DataverseUserAdapter(session, realm, model, entity, getAuthenticatedUserByUsername(entity.getUsername())));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realmModel, GroupModel groupModel, Integer integer, Integer integer1) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realmModel, String s, String s1) {
        return Stream.empty();
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
