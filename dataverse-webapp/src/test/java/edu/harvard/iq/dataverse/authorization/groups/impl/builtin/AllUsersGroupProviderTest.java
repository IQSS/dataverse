package edu.harvard.iq.dataverse.authorization.groups.impl.builtin;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MockRequestFactory;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.group.AllUsers;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AllUsersGroupProviderTest {

    private AllUsersGroupProvider provider = AllUsersGroupProvider.get();
    
    @Mock
    private User anyUser;
    @Mock
    private Group anyGroup;
    @Mock
    private GuestUser guestUser;
    @Mock
    private AuthenticatedUser authenticatedUser;
    
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
        assertSame(provider, AllUsersGroupProvider.get());
    }
    
    @Test
    public void groupProviderAlias() {
        assertEquals("builtin/all-users", provider.getGroupProviderAlias());
        
    }
    
    @Test
    public void groupFor__REQUEST() {
        // when & then
        assertThat(provider.groupsFor(guestRequest), contains(AllUsers.get()));
        assertThat(provider.groupsFor(authenticatedUserRequest), contains(AllUsers.get()));
        
    }
    
    @Test
    public void groupFor__ROLE_ASSIGNEE_AND_DV_OBJECT() {
        // when & then
        assertThat(provider.groupsFor(anyUser, MocksFactory.makeDataverse()), contains(AllUsers.get()));
        assertThat(provider.groupsFor(anyUser, MocksFactory.makeDataset()), contains(AllUsers.get()));
        assertThat(provider.groupsFor(anyUser, MocksFactory.makeDataFile()), contains(AllUsers.get()));
        
        assertThat(provider.groupsFor(anyGroup, MocksFactory.makeDataverse()), empty());
        assertThat(provider.groupsFor(anyGroup, MocksFactory.makeDataset()), empty());
        assertThat(provider.groupsFor(anyGroup, MocksFactory.makeDataFile()), empty());
        
    }
    
    @Test
    public void groupFor__REQUEST_AND_DV_OBJECT() {
        // when & then
        assertThat(provider.groupsFor(guestRequest, MocksFactory.makeDataverse()), contains(AllUsers.get()));
        assertThat(provider.groupsFor(guestRequest, MocksFactory.makeDataset()), contains(AllUsers.get()));
        assertThat(provider.groupsFor(guestRequest, MocksFactory.makeDataFile()), contains(AllUsers.get()));
        
        assertThat(provider.groupsFor(authenticatedUserRequest, MocksFactory.makeDataverse()), contains(AllUsers.get()));
        assertThat(provider.groupsFor(authenticatedUserRequest, MocksFactory.makeDataset()), contains(AllUsers.get()));
        assertThat(provider.groupsFor(authenticatedUserRequest, MocksFactory.makeDataFile()), contains(AllUsers.get()));
    }
    
    @Test
    public void contains__ALWAYS_TRUE() {
        // when & then
        assertTrue(provider.contains(guestRequest, AllUsers.get()));
        assertTrue(provider.contains(authenticatedUserRequest, AllUsers.get()));
    }
    
    @Test
    public void get__NOT_ALL_USERS_ALIAS() {
        // when & then
        assertNull(provider.get("somegroupalias"));
    }
    
    @Test
    public void get__ALL_USERS_ALIAS() {
        // when & then
        assertEquals(AllUsers.get(), provider.get("builtin/all-users"));
    }
}
