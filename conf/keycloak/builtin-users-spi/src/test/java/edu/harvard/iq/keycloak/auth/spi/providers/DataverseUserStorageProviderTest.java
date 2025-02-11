package edu.harvard.iq.keycloak.auth.spi.providers;

import edu.harvard.iq.keycloak.auth.spi.models.DataverseAuthenticatedUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseBuiltinUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataverseUserStorageProviderTest {

    private EntityManager entityManagerMock;
    private RealmModel realmStub;
    private DataverseUserStorageProvider sut;

    @BeforeEach
    void setUp() {
        entityManagerMock = mock(EntityManager.class);
        realmStub = mock(RealmModel.class);
        KeycloakSession sessionMock = mock(KeycloakSession.class);

        JpaConnectionProvider jpaConnectionProviderMock = mock(JpaConnectionProvider.class);
        when(sessionMock.getProvider(JpaConnectionProvider.class, "user-store")).thenReturn(jpaConnectionProviderMock);
        when(jpaConnectionProviderMock.getEntityManager()).thenReturn(entityManagerMock);

        sut = new DataverseUserStorageProvider(sessionMock, mock(ComponentModel.class));
    }

    @Test
    void getUserById_userExists() {
        String testUserId = "123";
        String testUsername = "testuser";

        DataverseBuiltinUser builtinUser = new DataverseBuiltinUser();
        builtinUser.setId(1);
        builtinUser.setUsername(testUsername);

        when(entityManagerMock.find(DataverseBuiltinUser.class, "123")).thenReturn(builtinUser);
        TypedQuery<DataverseAuthenticatedUser> authUserQuery = mock(TypedQuery.class);
        when(entityManagerMock.createNamedQuery("DataverseAuthenticatedUser.findByIdentifier", DataverseAuthenticatedUser.class))
                .thenReturn(authUserQuery);
        when(authUserQuery.setParameter("identifier", testUsername)).thenReturn(authUserQuery);

        DataverseAuthenticatedUser authUser = new DataverseAuthenticatedUser();
        authUser.setUserIdentifier(testUsername);
        when(authUserQuery.getSingleResult()).thenReturn(authUser);

        UserModel user = sut.getUserById(realmStub, testUserId);
        assertNotNull(user);
        assertEquals(testUsername, user.getUsername());
    }

    @Test
    void getUserById_userNotFound() {
        when(entityManagerMock.find(DataverseBuiltinUser.class, "123")).thenReturn(null);
        assertNull(sut.getUserById(realmStub, "123"));
    }

    @Test
    void getUserByUsername_userExists() {
        String testUsername = "testuser";

        DataverseBuiltinUser builtinUser = new DataverseBuiltinUser();
        builtinUser.setUsername(testUsername);
        builtinUser.setId(1);

        DataverseAuthenticatedUser authUser = new DataverseAuthenticatedUser();
        authUser.setUserIdentifier(testUsername);

        TypedQuery<DataverseBuiltinUser> builtinUserQuery = mock(TypedQuery.class);
        TypedQuery<DataverseAuthenticatedUser> authUserQuery = mock(TypedQuery.class);

        when(entityManagerMock.createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class))
                .thenReturn(builtinUserQuery);
        when(builtinUserQuery.setParameter("username", testUsername)).thenReturn(builtinUserQuery);
        when(builtinUserQuery.getResultList()).thenReturn(Collections.singletonList(builtinUser));

        when(entityManagerMock.createNamedQuery("DataverseAuthenticatedUser.findByIdentifier", DataverseAuthenticatedUser.class))
                .thenReturn(authUserQuery);
        when(authUserQuery.setParameter("identifier", testUsername)).thenReturn(authUserQuery);
        when(authUserQuery.getSingleResult()).thenReturn(authUser);

        UserModel user = sut.getUserByUsername(realmStub, testUsername);
        assertNotNull(user);
        assertEquals(testUsername, user.getUsername());
    }

    @Test
    void getUserByUsername_userNotFound() {
        TypedQuery<DataverseBuiltinUser> query = mock(TypedQuery.class);
        when(entityManagerMock.createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class))
                .thenReturn(query);
        when(query.setParameter("username", "unknown")).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        assertNull(sut.getUserByUsername(realmStub, "unknown"));
    }

    @Test
    void getUserByEmail_userExists() {
        String testEmail = "test@dataverse.org";
        String testUsername = "testuser";

        DataverseAuthenticatedUser authUser = new DataverseAuthenticatedUser();
        authUser.setEmail(testEmail);
        authUser.setId(1);
        authUser.setUserIdentifier(testUsername);

        DataverseBuiltinUser builtinUser = new DataverseBuiltinUser();
        builtinUser.setUsername(testUsername);
        builtinUser.setId(1);

        TypedQuery<DataverseAuthenticatedUser> authUserQuery = mock(TypedQuery.class);
        TypedQuery<DataverseBuiltinUser> builtinUserQuery = mock(TypedQuery.class);

        when(entityManagerMock.createNamedQuery("DataverseAuthenticatedUser.findByEmail", DataverseAuthenticatedUser.class))
                .thenReturn(authUserQuery);
        when(authUserQuery.setParameter("email", testEmail)).thenReturn(authUserQuery);
        when(authUserQuery.getResultList()).thenReturn(Collections.singletonList(authUser));

        when(entityManagerMock.createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class))
                .thenReturn(builtinUserQuery);
        when(builtinUserQuery.setParameter("username", testUsername)).thenReturn(builtinUserQuery);
        when(builtinUserQuery.getResultList()).thenReturn(Collections.singletonList(builtinUser));

        UserModel user = sut.getUserByEmail(realmStub, testEmail);
        assertNotNull(user);
        assertEquals(testUsername, user.getUsername());
    }

    @Test
    void getUserByEmail_userNotFound() {
        TypedQuery<DataverseAuthenticatedUser> query = mock(TypedQuery.class);
        when(entityManagerMock.createNamedQuery("DataverseAuthenticatedUser.findByEmail", DataverseAuthenticatedUser.class))
                .thenReturn(query);
        when(query.setParameter("email", "unknown@dataverse.org")).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        assertNull(sut.getUserByEmail(realmStub, "unknown@dataverse.org"));
    }
}
