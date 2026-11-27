package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchPermissionsServiceBeanTest {

    @InjectMocks
    private SearchPermissionsServiceBean searchPermissionsService;

    @Mock
    private AuthenticationServiceBean authSvc;

    @Mock
    private RoleAssigneeServiceBean roleAssigneeService;

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testConvertToIndexableString_User() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setId(123L);
        user.setUserIdentifier("john.doe");
        
        when(authSvc.getAuthenticatedUser("john.doe")).thenReturn(user);

        String result = searchPermissionsService.convertToIndexableString(user.getIdentifier());
        assertEquals(IndexServiceBean.getGroupPerUserPrefix() + user.getId(), result);
    }

    @Test
    public void testConvertToIndexableString_ExplicitGroup() {
        ExplicitGroupProvider provider = mock(ExplicitGroupProvider.class);
        when(provider.getGroupProviderAlias()).thenReturn("explicit");
        ExplicitGroup group = new ExplicitGroup(provider);
        
        DvObject owner = mock(DvObject.class);
        when(owner.getId()).thenReturn(1L);
        group.setOwner(owner);
        group.setGroupAliasInOwner("admins");
        group.updateAlias();
        
        // identifier: &explicit/1-admins
        // alias: 1-admins
        
        String result = searchPermissionsService.convertToIndexableString(group.getIdentifier());
        assertEquals(IndexServiceBean.getGroupPrefix() + group.getAlias(), result);
    }

    @Test
    public void testConvertToIndexableString_IpGroup() {
        IpGroupProvider provider = mock(IpGroupProvider.class);
        when(provider.getGroupProviderAlias()).thenReturn("ip");
        IpGroup group = new IpGroup(provider);
        group.setPersistedGroupAlias("office");
        
        // alias: ip/office
        // identifier: &ip/office
        
        String result = searchPermissionsService.convertToIndexableString(group.getIdentifier());
        assertEquals(IndexServiceBean.getGroupPrefix() + group.getAlias(), result);
    }

    @Test
    public void testConvertToIndexableString_AllUsers() {
        Group group = AllUsers.get();
        // identifier: :AllUsers
        // alias: builtIn/all-users
        when(roleAssigneeService.getRoleAssignee(group.getIdentifier())).thenReturn(group);
        String result = searchPermissionsService.convertToIndexableString(group.getIdentifier());
        assertEquals(IndexServiceBean.getGroupPrefix() + group.getAlias(), result);
    }

    @Test
    public void testConvertToIndexableString_AuthenticatedUsers() {
        Group group = AuthenticatedUsers.get();
        // identifier: :authenticated-users
        // alias: builtIn/authenticated-users
        when(roleAssigneeService.getRoleAssignee(group.getIdentifier())).thenReturn(group);
        String result = searchPermissionsService.convertToIndexableString(group.getIdentifier());
        assertEquals(IndexServiceBean.getGroupPrefix() + group.getAlias(), result);
    }
}
