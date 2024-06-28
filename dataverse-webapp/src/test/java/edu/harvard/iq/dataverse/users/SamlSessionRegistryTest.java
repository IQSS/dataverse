package edu.harvard.iq.dataverse.users;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.saml.SamlUserData;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserLookup;
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
        SamlUserData samlUserData = new SamlUserData("userId-1", "entityId-1");
        AuthenticatedUser user = newAuthenticateduser(samlUserData);
        DataverseSession session = produceSession(user);
        DataverseSession anotherSession = produceSession(user);

        // when
        registry.register(session, samlUserData);
        registry.register(anotherSession, samlUserData);
        List<DataverseSession> unregistered = registry.unregister(session);
        List<DataverseSession> anotherUnregistered = registry.unregister(anotherSession);

        // then
        assertThat(unregistered).containsExactly(session);
        assertThat(anotherUnregistered).containsExactly(anotherSession);
    }

    @Test
    void addAndRemoveByUser() {
        // given
        SamlUserData samlUserData = new SamlUserData("userId-1", "entityId-1");
        AuthenticatedUser user = newAuthenticateduser(samlUserData);
        DataverseSession session = produceSession(user);
        DataverseSession anotherSession = produceSession(user);

        // when
        registry.register(session, samlUserData);
        registry.register(anotherSession, samlUserData);
        List<DataverseSession> unregistered = registry.unregister(user);

        // then
        assertThat(unregistered).containsExactlyInAnyOrder(session, anotherSession);
    }

    @Test
    void removeInvalidatedSessionsWhenUnregistering() {
        // given
        SamlUserData samlUserData = new SamlUserData("userId-1", "entityId-1");
        AuthenticatedUser user = newAuthenticateduser(samlUserData);
        DataverseSession session = produceSession(user);
        DataverseSession anotherSession = produceSession(user);

        // when
        registry.register(session, samlUserData);
        registry.register(anotherSession, samlUserData);
        anotherSession.setUser(null);
        List<DataverseSession> unregistered = registry.unregister(user);

        // then
        assertThat(unregistered).containsExactly(session);
    }

    @Test
    void findEntityId() {
        // given
        SamlUserData samlUser1_1 = new SamlUserData("userId-1.1", "entityId-1");
        SamlUserData samlUser1_2 = new SamlUserData("userId-1.2", "entityId-1");
        SamlUserData samlUser2_1 = new SamlUserData("userId-2.1", "entityId-2");
        SamlUserData samlUser2_2 = new SamlUserData("userId-2.2", "entityId-2");
        AuthenticatedUser user1_1 = newAuthenticateduser(samlUser1_1);
        AuthenticatedUser user1_2 = newAuthenticateduser(samlUser1_2);
        AuthenticatedUser user2_1 = newAuthenticateduser(samlUser2_1);
        AuthenticatedUser user2_2_1 = newAuthenticateduser(samlUser2_2);
        AuthenticatedUser user2_2_2 = newAuthenticateduser(samlUser2_2);
        DataverseSession session1_1 = produceSession(user1_1);
        DataverseSession session1_2 = produceSession(user1_2);
        DataverseSession session2_1 = produceSession(user2_1);
        DataverseSession session2_2_1 = produceSession(user2_2_1);
        DataverseSession session2_2_2 = produceSession(user2_2_2);
        registry.register(session1_1, samlUser1_1);
        registry.register(session1_2, samlUser1_2);
        registry.register(session2_1, samlUser2_1);
        registry.register(session2_2_1, samlUser2_2);
        registry.register(session2_2_2, samlUser2_2);

        // when and then
        assertThat(registry.findEntityId(user1_1)).contains("entityId-1");
        assertThat(registry.findEntityId(user1_2)).contains("entityId-1");
        assertThat(registry.findEntityId(user2_1)).contains("entityId-2");
        assertThat(registry.findEntityId(user2_2_1)).contains("entityId-2");
        assertThat(registry.findEntityId(user2_2_2)).contains("entityId-2");

        // given
        registry.unregister(session2_1);

        // when and then
        assertThat(registry.findEntityId(user1_1)).contains("entityId-1");
        assertThat(registry.findEntityId(user1_2)).contains("entityId-1");
        assertThat(registry.findEntityId(user2_1)).isEmpty();
        assertThat(registry.findEntityId(user2_2_1)).contains("entityId-2");
        assertThat(registry.findEntityId(user2_2_2)).contains("entityId-2");

        // given
        registry.unregister(user2_2_1);

        // when and then
        assertThat(registry.findEntityId(user1_1)).contains("entityId-1");
        assertThat(registry.findEntityId(user1_2)).contains("entityId-1");
        assertThat(registry.findEntityId(user2_1)).isEmpty();
        assertThat(registry.findEntityId(user2_2_1)).isEmpty();
        assertThat(registry.findEntityId(user2_2_2)).isEmpty();
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

    private static AuthenticatedUser newAuthenticateduser(SamlUserData samlUserData) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setAuthenticatedUserLookup(new AuthenticatedUserLookup());
        authenticatedUser.getAuthenticatedUserLookup().setPersistentUserId(samlUserData.getCompositeId());
        authenticatedUser.getAuthenticatedUserLookup().setAuthenticationProviderId("saml");
        return authenticatedUser;
    }
}