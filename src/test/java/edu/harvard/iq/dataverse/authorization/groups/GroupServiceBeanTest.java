package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
import edu.harvard.iq.dataverse.mocks.MockRoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author michael
 */
public class GroupServiceBeanTest {
    
    public GroupServiceBeanTest() {
    }

    @Test
    public void testFlattenGroupsCollection() throws GroupException {
        // Setup
        MockRoleAssigneeServiceBean roleAssigneeSvc = new MockRoleAssigneeServiceBean();
        ExplicitGroupProvider prv = new ExplicitGroupProvider(null, roleAssigneeSvc);
        ExplicitGroup gA = new ExplicitGroup(prv);
        gA.setDisplayName("A");
        ExplicitGroup gAa = new ExplicitGroup(prv);
        gAa.setDisplayName("Aa");
        ExplicitGroup gAb = new ExplicitGroup(prv);
        gAb.setDisplayName("Ab");
        ExplicitGroup gAstar = new ExplicitGroup(prv);
        gAstar.setDisplayName("A*");
        Dataverse dv = MocksFactory.makeDataverse();
        Stream.of( gA, gAa, gAb, gAstar).forEach( g -> {
            g.setId( MocksFactory.nextId() );
            g.setOwner(dv);
            g.setGroupAliasInOwner( g.getDisplayName() );
            roleAssigneeSvc.add(g);
            g.updateAlias();
        });
        
        // create some containment hierarchy.
        gA.add(gAa);
        gA.add(gAb);
        gAb.add(gAstar);
        gAa.add(gAstar);
        gAa.add( AuthenticatedUsers.get() );
        
        // Test
        GroupServiceBean sut = new GroupServiceBean();
        sut.roleAssigneeSvc = roleAssigneeSvc;
        
        Set<Group> grps = new HashSet<>();
        grps.add( AllUsers.get() );
        grps.add( gA );
                
        List<Group> result = sut.flattenGroupsCollection(grps).collect(toList());
        
        assertEquals( "Groups should appear only once", result.size(), new HashSet<>(result).size() );
        
        grps.addAll( Arrays.asList(gAa, gAb, gAstar, AuthenticatedUsers.get()) );
        assertEquals( "All groups should appear", grps, new HashSet<>(result) );
        
    }
    
}
