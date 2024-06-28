package edu.harvard.iq.dataverse.users;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.providers.saml.SamlUserData;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import io.vavr.control.Option;

import javax.ejb.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class SamlSessionRegistry {
    private final Map<String, SamlUserSession> samlUsers = new ConcurrentHashMap<>();

    // -------------------- LOGIC --------------------

    public void register(DataverseSession dvSession, SamlUserData userData) {
        samlUsers.compute(userData.getCompositeId(), (compositeKey, existingSession) -> {
            if (existingSession == null) {
                return new SamlUserSession(userData.getIdpEntityId(), dvSession);
            } else {
                existingSession.addDataverseSession(dvSession);
                return existingSession;
            }
        });
    }

    public List<DataverseSession> unregister(DataverseSession session) {
        cleanUp();
        if (session == null || !session.getUser().isAuthenticated()) {
            return Collections.emptyList();
        }

        return resolvePersistentUserId((AuthenticatedUser) session.getUser())
                .map(persistentUserId -> removeDataverseSession(persistentUserId, session))
                .getOrElse(Collections.emptyList());
    }

    public List<DataverseSession> unregister(AuthenticatedUser authenticatedUser) {
        cleanUp();
        return resolvePersistentUserId(authenticatedUser)
                .map(samlUsers::remove)
                .map(SamlUserSession::getDataverseSessions)
                .getOrElse(Collections.emptyList());
    }

    public Option<String> findEntityId(AuthenticatedUser authenticatedUser) {
        return resolvePersistentUserId(authenticatedUser)
                .flatMap(persistentUserId -> Option.of(samlUsers.get(persistentUserId)))
                .map(SamlUserSession::getEntityId);
    }

    // -------------------- PRIVATE --------------------

    /**
     * Removes sessions that were actually invalidated, but somehow not unregistered.
     * In case of huge amount of users should be invoked independently (eg. by @Schedule).
     */
    private void cleanUp() {
        samlUsers.entrySet()
                .removeIf(e -> {
                    e.getValue().cleanUpDataverseSessions();
                    return e.getValue().hasNoDataverseSessions();
                });
    }

    private Option<String> resolvePersistentUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getAuthenticatedUserLookup() == null) {
            return Option.none();
        }

        return Option.of(authenticatedUser.getAuthenticatedUserLookup().getPersistentUserId());
    }

    private List<DataverseSession> removeDataverseSession(String persistentUserId, DataverseSession session) {
        List<DataverseSession> removed = new ArrayList<>();

        samlUsers.compute(persistentUserId, (k, samlSession) -> {
            if (samlSession == null) {
                return null;
            }

            samlSession.removeDataverseSession(session.getSessionId()).forEach(removed::add);
            if (samlSession.hasNoDataverseSessions()) {
                return null;
            }

            return samlSession;
        });

        return removed;
    }

    // -------------------- INNER CLASSES --------------------

    private static class SamlUserSession {
        private final String entityId;
        private final Map<UUID, DataverseSession> dataverseSessions = new HashMap<>();

        SamlUserSession(String entityId, DataverseSession session) {
            this.entityId = entityId;
            dataverseSessions.put(session.getSessionId(), session);
        }

        String getEntityId() {
            return entityId;
        }

        List<DataverseSession> getDataverseSessions() {
            return new ArrayList<>(dataverseSessions.values());
        }

        boolean hasNoDataverseSessions() {
            return dataverseSessions.isEmpty();
        }

        void addDataverseSession(DataverseSession session) {
            dataverseSessions.put(session.getSessionId(), session);
        }

        Option<DataverseSession> removeDataverseSession(UUID sessionId) {
            return Option.of(dataverseSessions.remove(sessionId));
        }

        void cleanUpDataverseSessions() {
            dataverseSessions.values().removeIf(session -> !session.getUser().isAuthenticated());
        }
    }
}
