package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
import edu.harvard.iq.dataverse.mocks.MockExplicitGroupService;
import edu.harvard.iq.dataverse.mocks.MockRoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import static edu.harvard.iq.dataverse.util.CollectionLiterals.*;

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
        
        Set<Group> grps = setOf( AllUsers.get(), gA );
                
        List<Group> result = sut.flattenGroupsCollection(grps).collect(toList());
        
        assertEquals(result.size(), new HashSet<>(result).size(), "Groups should appear only once");
        
        grps.addAll( listOf(gAa, gAb, gAstar, AuthenticatedUsers.get()) );
        assertEquals(grps, new HashSet<>(result), "All groups should appear");
        
    }
    
    @Test
    public void testCollectAncestors() throws GroupException {
        // Setup
        MockRoleAssigneeServiceBean roleAssigneeSvc = new MockRoleAssigneeServiceBean();
        MockExplicitGroupService explicitGroupSvc = new MockExplicitGroupService();
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
            g.updateAlias();
            roleAssigneeSvc.add(g);
            explicitGroupSvc.registerGroup(g);
        });
        
        // create some containment hierarchy.
        gA.add(gAa);
        gA.add(gAb);
        gAb.add(gAstar);
        gAa.add(gAstar);
        gAa.add(AuthenticatedUsers.get());
        
        // Test
        GroupServiceBean sut = new GroupServiceBean();
        sut.roleAssigneeSvc = roleAssigneeSvc;
        sut.explicitGroupService = explicitGroupSvc;
        
        assertEquals( setOf(gA), sut.collectAncestors(setOf(gA)) );
        assertEquals( setOf(gA, gAb), sut.collectAncestors(setOf(gAb)) );
        assertEquals( setOf(gA, gAa, AuthenticatedUsers.get()), 
                      sut.collectAncestors(setOf(AuthenticatedUsers.get())) );
        assertEquals( setOf(gA, gAb, gAa, gAstar), 
                      sut.collectAncestors(setOf(gAstar)) );
        
    }
    
    
}
