package edu.harvard.iq.keycloak.auth.spi.services;

import edu.harvard.iq.keycloak.auth.spi.models.DataverseAuthenticatedUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseBuiltinUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataverseUserServiceTest {

    private EntityManager entityManagerMock;
    private DataverseUserService sut;

    @BeforeEach
    void setUp() {
        entityManagerMock = mock(EntityManager.class);
        KeycloakSession sessionMock = mock(KeycloakSession.class);

        JpaConnectionProvider jpaConnectionProviderMock = mock(JpaConnectionProvider.class);
        when(sessionMock.getProvider(JpaConnectionProvider.class, "user-store")).thenReturn(jpaConnectionProviderMock);
        when(jpaConnectionProviderMock.getEntityManager()).thenReturn(entityManagerMock);

        sut = new DataverseUserService(sessionMock);
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

        DataverseUser user = sut.getUserById(testUserId);
        assertNotNull(user);
        assertEquals(testUsername, user.getBuiltinUser().getUsername());
    }

    @Test
    void getUserById_userNotFound() {
        when(entityManagerMock.find(DataverseBuiltinUser.class, "123")).thenReturn(null);
        assertNull(sut.getUserById("123"));
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

        DataverseUser user = sut.getUserByUsername(testUsername);
        assertNotNull(user);
        assertEquals(testUsername, user.getBuiltinUser().getUsername());
    }

    @Test
    void getUserByUsername_userNotFound() {
        TypedQuery<DataverseBuiltinUser> query = mock(TypedQuery.class);
        when(entityManagerMock.createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class))
                .thenReturn(query);
        when(query.setParameter("username", "unknown")).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        assertNull(sut.getUserByUsername("unknown"));
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

        DataverseUser user = sut.getUserByEmail(testEmail);
        assertNotNull(user);
        assertEquals(testUsername, user.getBuiltinUser().getUsername());
    }

    @Test
    void getUserByEmail_userNotFound() {
        TypedQuery<DataverseAuthenticatedUser> query = mock(TypedQuery.class);
        when(entityManagerMock.createNamedQuery("DataverseAuthenticatedUser.findByEmail", DataverseAuthenticatedUser.class))
                .thenReturn(query);
        when(query.setParameter("email", "unknown@dataverse.org")).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        assertNull(sut.getUserByEmail("unknown@dataverse.org"));
    }
}
