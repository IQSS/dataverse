package edu.harvard.iq.dataverse.users;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SamlSessionRegistryTest {

    private SamlSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SamlSessionRegistry();
    }

    // -------------------- TESTS --------------------

    @Test
    void addAndRemoveBySession() {
        // given
        AuthenticatedUser user = new AuthenticatedUser();
        DataverseSession session = produceSession(user);
        DataverseSession anotherSession = produceSession(user);

        // when
        registry.register(session);
        registry.register(anotherSession);
        List<DataverseSession> unregistered = registry.unregister(session);
        List<DataverseSession> anotherUnregistered = registry.unregister(anotherSession);

        // then
        assertThat(unregistered).containsExactly(session);
        assertThat(anotherUnregistered).containsExactly(anotherSession);
    }

    @Test
    void addAndRemoveByUser() {
        // given
        AuthenticatedUser user = new AuthenticatedUser();
        DataverseSession session = produceSession(user);
        DataverseSession anotherSession = produceSession(user);

        // when
        registry.register(session);
        registry.register(anotherSession);
        List<DataverseSession> unregistered = registry.unregister(user);

        // then
        assertThat(unregistered).containsExactlyInAnyOrder(session, anotherSession);
    }

    @Test
    void removeInvalidatedSessionsWhenUnregistering() {
        // given
        AuthenticatedUser user = new AuthenticatedUser();
        DataverseSession session = produceSession(user);
        DataverseSession anotherSession = produceSession(user);

        // when
        registry.register(session);
        registry.register(anotherSession);
        anotherSession.setUser(null);
        List<DataverseSession> unregistered = registry.unregister(user);

        // then
        assertThat(unregistered).containsExactly(session);
    }

    // -------------------- PRIVATE --------------------

    private DataverseSession produceSession(AuthenticatedUser user) {
        ActionLogServiceBean actionLogServiceBean = new ActionLogServiceBean() {
            @Override public void log(ActionLogRecord rec) { }
        };
        DataverseSession session = new DataverseSession(actionLogServiceBean, null);
        session.setUser(user);
        return session;
    }
}