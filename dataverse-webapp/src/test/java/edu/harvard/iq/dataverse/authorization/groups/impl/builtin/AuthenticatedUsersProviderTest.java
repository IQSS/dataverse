package edu.harvard.iq.dataverse.authorization.groups.impl.builtin;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MockRequestFactory;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.group.AuthenticatedUsers;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthenticatedUsersProviderTest {

    private AuthenticatedUsersProvider provider = AuthenticatedUsersProvider.get();
    
    private GuestUser guestUser = GuestUser.get();
    
    private AuthenticatedUser authenticatedUser = MocksFactory.makeAuthenticatedUser("John", "Doe");
    
    @Mock
    private Group anyGroup;
    
    private DataverseRequest guestRequest;
    private DataverseRequest authenticatedUserRequest;
    
    
    @BeforeEach
    public void before() {
        guestRequest = MockRequestFactory.makeRequest(guestUser);
        authenticatedUserRequest = MockRequestFactory.makeRequest(authenticatedUser);
    }
    
    
    // -------------------- TESTS --------------------
    
    @Test
    public void create__IS_SINGLETON() {
        // when & then
        assertSame(provider, AuthenticatedUsersProvider.get());
    }
    
    @Test
    public void groupProviderAlias() {
        assertEquals("builtin/authenticated-users", provider.getGroupProviderAlias());
        
    }
    
    @Test
    public void groupFor__REQUEST() {
        // when & then
        assertThat(provider.groupsFor(guestRequest), empty());
        assertThat(provider.groupsFor(authenticatedUserRequest), contains(AuthenticatedUsers.get()));
        
    }
    
    @Test
    public void groupFor__ROLE_ASSIGNEE_AND_DV_OBJECT() {
        // when & then
        assertThat(provider.groupsFor(guestUser, MocksFactory.makeDataverse()), empty());
        assertThat(provider.groupsFor(guestUser, MocksFactory.makeDataset()), empty());
        assertThat(provider.groupsFor(guestUser, MocksFactory.makeDataFile()), empty());
        
        assertThat(provider.groupsFor(authenticatedUser, MocksFactory.makeDataverse()), contains(AuthenticatedUsers.get()));
        assertThat(provider.groupsFor(authenticatedUser, MocksFactory.makeDataset()), contains(AuthenticatedUsers.get()));
        assertThat(provider.groupsFor(authenticatedUser, MocksFactory.makeDataFile()), contains(AuthenticatedUsers.get()));
        
        assertThat(provider.groupsFor(anyGroup, MocksFactory.makeDataverse()), empty());
        assertThat(provider.groupsFor(anyGroup, MocksFactory.makeDataset()), empty());
        assertThat(provider.groupsFor(anyGroup, MocksFactory.makeDataFile()), empty());
        
    }
    
    @Test
    public void groupFor__REQUEST_AND_DV_OBJECT() {
        // when & then
        assertThat(provider.groupsFor(guestRequest, MocksFactory.makeDataverse()), empty());
        assertThat(provider.groupsFor(guestRequest, MocksFactory.makeDataset()), empty());
        assertThat(provider.groupsFor(guestRequest, MocksFactory.makeDataFile()), empty());
        
        assertThat(provider.groupsFor(authenticatedUserRequest, MocksFactory.makeDataverse()), contains(AuthenticatedUsers.get()));
        assertThat(provider.groupsFor(authenticatedUserRequest, MocksFactory.makeDataset()), contains(AuthenticatedUsers.get()));
        assertThat(provider.groupsFor(authenticatedUserRequest, MocksFactory.makeDataFile()), contains(AuthenticatedUsers.get()));
    }
    
    @Test
    public void contains__REQUEST() {
        // when & then
        assertFalse(provider.contains(guestRequest, AuthenticatedUsers.get()));
        assertTrue(provider.contains(authenticatedUserRequest, AuthenticatedUsers.get()));
    }
    
    @Test
    public void get__NOT_AUTHENTICATED_USERS_ALIAS() {
        // when & then
        assertNull(provider.get("somegroupalias"));
    }
    
    @Test
    public void get__AUTHENTICATED_USERS_ALIAS() {
        // when & then
        assertEquals(AuthenticatedUsers.get(), provider.get("builtin/authenticated-users"));
    }
}
