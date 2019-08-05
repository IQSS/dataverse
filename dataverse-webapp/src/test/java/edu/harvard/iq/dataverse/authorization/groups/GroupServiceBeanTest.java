package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
import edu.harvard.iq.dataverse.mocks.MockExplicitGroupService;
import edu.harvard.iq.dataverse.mocks.MockRoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.AllUsers;
import edu.harvard.iq.dataverse.persistence.group.AuthenticatedUsers;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.group.GroupException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static edu.harvard.iq.dataverse.util.CollectionLiterals.listOf;
import static edu.harvard.iq.dataverse.util.CollectionLiterals.setOf;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author michael
 */
public class GroupServiceBeanTest {

    public GroupServiceBeanTest() {
    }

    @Test
    public void testFlattenGroupsCollection() throws GroupException {
        // Setup
        MockRoleAssigneeServiceBean roleAssigneeSvc = new MockRoleAssigneeServiceBean();
        ExplicitGroupProvider prv = new ExplicitGroupProvider(null, roleAssigneeSvc, new ArrayList<>());
        ExplicitGroup gA = new ExplicitGroup();
        gA.setDisplayName("A");
        ExplicitGroup gAa = new ExplicitGroup();
        gAa.setDisplayName("Aa");
        ExplicitGroup gAb = new ExplicitGroup();
        gAb.setDisplayName("Ab");
        ExplicitGroup gAstar = new ExplicitGroup();
        gAstar.setDisplayName("A*");
        Dataverse dv = MocksFactory.makeDataverse();
        Stream.of(gA, gAa, gAb, gAstar).forEach(g -> {
            g.setId(MocksFactory.nextId());
            g.setOwner(dv);
            g.setGroupAliasInOwner(g.getDisplayName());
            roleAssigneeSvc.add(g);
            g.updateAlias();
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

        Set<Group> grps = setOf(AllUsers.get(), gA);

        List<Group> result = sut.flattenGroupsCollection(grps).collect(toList());

        assertEquals("Groups should appear only once", result.size(), new HashSet<>(result).size());

        grps.addAll(listOf(gAa, gAb, gAstar, AuthenticatedUsers.get()));
        assertEquals("All groups should appear", grps, new HashSet<>(result));

    }

    @Test
    public void testCollectAncestors() throws GroupException {
        // Setup
        RoleAssigneeServiceBean roleAssigneeSvc = mock(RoleAssigneeServiceBean.class);
        
        MockExplicitGroupService explicitGroupSvc = new MockExplicitGroupService(roleAssigneeSvc);

        ExplicitGroup gA = new ExplicitGroup();
        gA.setDisplayName("A");
        ExplicitGroup gAa = new ExplicitGroup();
        gAa.setDisplayName("Aa");
        ExplicitGroup gAb = new ExplicitGroup();
        gAb.setDisplayName("Ab");
        ExplicitGroup gAstar = new ExplicitGroup();
        gAstar.setDisplayName("A*");
        Dataverse dv = MocksFactory.makeDataverse();
        Stream.of(gA, gAa, gAb, gAstar).forEach(g -> {
            g.setId(MocksFactory.nextId());
            g.setOwner(dv);
            g.setGroupAliasInOwner(g.getDisplayName());
            g.updateAlias();
            explicitGroupSvc.registerGroup(g);
        });
        
        when(roleAssigneeSvc.getRoleAssignee(gA.getIdentifier())).thenReturn(gA);
        when(roleAssigneeSvc.getRoleAssignee(gAa.getIdentifier())).thenReturn(gAa);
        when(roleAssigneeSvc.getRoleAssignee(gAb.getIdentifier())).thenReturn(gAb);
        when(roleAssigneeSvc.getRoleAssignee(gAstar.getIdentifier())).thenReturn(gAstar);
        when(roleAssigneeSvc.getRoleAssignee(AuthenticatedUsers.get().getIdentifier())).thenReturn(AuthenticatedUsers.get());

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
        sut.setup();

        assertEquals(setOf(gA), sut.collectAncestors(setOf(gA)));
        assertEquals(setOf(gA, gAb), sut.collectAncestors(setOf(gAb)));
        assertEquals(setOf(gA, gAa, AuthenticatedUsers.get()),
                     sut.collectAncestors(setOf(AuthenticatedUsers.get())));
        assertEquals(setOf(gA, gAb, gAa, gAstar),
                     sut.collectAncestors(setOf(gAstar)));

    }


}
