package edu.harvard.iq.dataverse.users;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class SamlSessionRegistry {
    private static final Logger logger = LoggerFactory.getLogger(SamlSessionRegistry.class);

    private Map<AuthenticatedUser, List<DataverseSession>> userToSessionIndex = new HashMap<>();

    // -------------------- LOGIC --------------------

    @Lock(LockType.WRITE)
    public void register(DataverseSession session) {
        User user = session.getUser();
        if (!user.isAuthenticated()) {
            logger.warn("Cannot register session with non-authenticated user: " + user);
            throw new IllegalArgumentException("Cannot register session with not authenticated user");
        }
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
        List<DataverseSession> sessions = userToSessionIndex.computeIfAbsent(authenticatedUser, au -> new ArrayList<>());
        sessions.add(session);
    }

    @Lock(LockType.WRITE)
    public List<DataverseSession> unregister(DataverseSession session) {
        cleanUp();
        User user;
        List<DataverseSession> sessions;
        if (session == null
                || !(user = session.getUser()).isAuthenticated()
                || (sessions = userToSessionIndex.get(user)) == null) {
            return Collections.emptyList();
        }
        List<DataverseSession> removed = sessions.stream()
                .filter(s -> session.getSessionId().equals(s.getSessionId()))
                .collect(Collectors.toList());
        sessions.removeAll(removed);
        if (sessions.isEmpty()) {
            userToSessionIndex.remove(user);
        }
        return removed;
    }

    @Lock(LockType.WRITE)
    public List<DataverseSession> unregister(AuthenticatedUser authenticatedUser) {
        cleanUp();
        if (authenticatedUser == null) {
            logger.trace("Cannot unregister session for null user");
            return Collections.emptyList();
        }
        List<DataverseSession> removed = userToSessionIndex.remove(authenticatedUser);
        return removed != null ? removed : Collections.emptyList();
    }

    // -------------------- PRIVATE --------------------

    /**
     * Removes sessions that were actually invalidated, but somehow not unregistered.
     * In case of huge amount of users should be invoked independently (eg. by @Schedule).
     */
    private void cleanUp() {
        for (List<DataverseSession> sessions : userToSessionIndex.values()) {
            sessions.removeIf(s -> !s.getUser().isAuthenticated());
        }
        userToSessionIndex.entrySet()
                .removeIf(e -> e.getValue().isEmpty());
    }
}
