package edu.harvard.iq.keycloak.auth.spi.services;

import edu.harvard.iq.keycloak.auth.spi.models.DataverseAuthenticatedUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseBuiltinUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseUser;
import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.StorageId;

import java.util.List;

public class DataverseUserService {

    private static final Logger logger = Logger.getLogger(DataverseUserService.class);

    private final EntityManager em;

    public DataverseUserService(KeycloakSession session) {
        this.em = session.getProvider(JpaConnectionProvider.class, "user-store").getEntityManager();
    }

    public DataverseUser getUserById(String id) {
        logger.debugf("Fetching user by ID: %s", id);
        String persistenceId = StorageId.externalId(id);

        DataverseBuiltinUser builtinUser = em.find(DataverseBuiltinUser.class, persistenceId);
        if (builtinUser == null) {
            logger.debugf("User not found for external ID: %s", persistenceId);
            return null;
        }

        DataverseAuthenticatedUser authenticatedUser = getAuthenticatedUserByUsername(builtinUser.getUsername());

        return new DataverseUser(authenticatedUser, builtinUser);
    }

    public DataverseUser getUserByUsername(String username) {
        logger.debugf("Fetching user by username: %s", username);
        List<DataverseBuiltinUser> users = em.createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class)
                .setParameter("username", username)
                .getResultList();

        if (users.isEmpty()) {
            logger.debugf("User not found by username: %s", username);
            return null;
        }

        DataverseAuthenticatedUser authenticatedUser = getAuthenticatedUserByUsername(username);

        return new DataverseUser(authenticatedUser, users.get(0));
    }

    public DataverseUser getUserByEmail(String email) {
        logger.debugf("Fetching user by email: %s", email);
        List<DataverseAuthenticatedUser> authUsers = em.createNamedQuery("DataverseAuthenticatedUser.findByEmail", DataverseAuthenticatedUser.class)
                .setParameter("email", email)
                .getResultList();

        if (authUsers.isEmpty()) {
            logger.debugf("User not found by email: %s", email);
            return null;
        }

        String username = authUsers.get(0).getUserIdentifier();
        List<DataverseBuiltinUser> builtinUsers = em.createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class)
                .setParameter("username", username)
                .getResultList();

        return new DataverseUser(authUsers.get(0), builtinUsers.get(0));
    }

    public void close() {
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
            logger.debugf("Could not find authenticated user by username: %s", username);
            return null;
        }
    }
}
