package edu.harvard.iq.dataverse.search.query;

import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author madryk
 */
@ExtendWith(MockitoExtension.class)
public class PermissionFilterQueryBuilderTest {

    @InjectMocks
    private PermissionFilterQueryBuilder permissionFilterQueryBuilder;
    
    @Mock
    private GroupServiceBean groupService;
    
    @Test
    public void buildPermissionFilterQuery__for_superuser() {
        // given
        AuthenticatedUser user = new AuthenticatedUser();
        user.setSuperuser(true);
        DataverseRequest request = new DataverseRequest(user, IpAddress.valueOf("127.0.0.1"));
        
        // when & then
        assertEquals("", permissionFilterQueryBuilder.buildPermissionFilterQuery(request));
    }
    
    @Test
    public void buildPermissionFilterQuery__for_guest() {
        // given
        DataverseRequest request = new DataverseRequest(GuestUser.get(), IpAddress.valueOf("127.0.0.1"));
        
        Group group = Mockito.mock(Group.class);
        when(group.getAlias()).thenReturn("groupContainingGuests");
        
        when(groupService.groupsFor(request)).thenReturn(Sets.newHashSet(group));
        when(groupService.collectAncestors(Sets.newHashSet(group))).thenReturn(Sets.newHashSet(group));
        
        // when & then
        String expectedFilterQuery = "{!join from=definitionPointDocId to=id}"
                + "discoverableBy:(group_groupContainingGuests)" 
                + " OR discoverableByPublicFrom:[* TO NOW]";
        
        assertEquals(expectedFilterQuery, permissionFilterQueryBuilder.buildPermissionFilterQuery(request));
    }
    
    @Test
    public void buildPermissionFilterQuery__for_authenticated_user() {
        // given
        AuthenticatedUser user = new AuthenticatedUser();
        user.setId(15L);
        
        DataverseRequest request = new DataverseRequest(user, IpAddress.valueOf("127.0.0.1"));
        
        Group group1 = Mockito.mock(Group.class);
        when(group1.getAlias()).thenReturn("someAlias1");
        
        Group group2 = Mockito.mock(Group.class);
        when(group2.getAlias()).thenReturn("someAlias2");
        
        Group group3 = Mockito.mock(Group.class);
        when(group3.getAlias()).thenReturn("someAlias3");
        
        Set<Group> directUserGroups = Sets.newLinkedHashSet();
        directUserGroups.add(group1);
        directUserGroups.add(group2);
        
        Set<Group> allUserGroups = Sets.newLinkedHashSet(directUserGroups);
        allUserGroups.add(group3);
        
        
        when(groupService.groupsFor(request)).thenReturn(directUserGroups);
        when(groupService.collectAncestors(directUserGroups)).thenReturn(allUserGroups);
        
        // when & then
        String expectedFilterQuery = "{!join from=definitionPointDocId to=id}"
                + "discoverableBy:(group_user15 OR group_someAlias1 OR group_someAlias2 OR group_someAlias3)" 
                + " OR discoverableByPublicFrom:[* TO NOW]";
        
        assertEquals(expectedFilterQuery, permissionFilterQueryBuilder.buildPermissionFilterQuery(request));
    }
}
