/*
 *  (C) Michael Bar-Sinai
 */
package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.groups.GroupException;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsersGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsersProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddressRange;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MockRoleAssigneeServiceBean;
import org.junit.Test;

import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeAuthenticatedUser;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeDataverse;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeExplicitGroup;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeRequest;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.nextId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author michael
 */
public class ExplicitGroupTest {


    MockRoleAssigneeServiceBean roleAssigneeSvc = new MockRoleAssigneeServiceBean();
    IpGroupProvider ipGroupProvider = new IpGroupProvider(null);
    ExplicitGroupProvider prv = new ExplicitGroupProvider(null, roleAssigneeSvc, Lists.newArrayList(ipGroupProvider,
            AuthenticatedUsersProvider.get(), AllUsersGroupProvider.get()));

    public ExplicitGroupTest() {
    }

    @Test(expected = GroupException.class)
    public void addGroupToSelf() throws Exception {
        ExplicitGroup sut = new ExplicitGroup();
        sut.setDisplayName("a group");
        sut.add(sut);
        fail("A group cannot be added to itself.");
    }

    @Test(expected = GroupException.class)
    public void addGroupToDescendant() throws GroupException {
        Dataverse dv = makeDataverse();
        ExplicitGroup root = new ExplicitGroup();
        root.setId(nextId());
        root.setGroupAliasInOwner("top");
        ExplicitGroup sub = new ExplicitGroup();
        sub.setGroupAliasInOwner("sub");
        sub.setId(nextId());
        ExplicitGroup subSub = new ExplicitGroup();
        subSub.setGroupAliasInOwner("subSub");
        subSub.setId(nextId());
        root.setOwner(dv);
        sub.setOwner(dv);
        subSub.setOwner(dv);

        sub.add(subSub);
        root.add(sub);
        subSub.add(root);
        fail("A group cannot contain its parent");
    }

    @Test(expected = GroupException.class)
    public void addGroupToUnrealtedGroup() throws GroupException {
        Dataverse dv1 = makeDataverse();
        Dataverse dv2 = makeDataverse();
        ExplicitGroup g1 = new ExplicitGroup();
        ExplicitGroup g2 = new ExplicitGroup();
        g1.setOwner(dv1);
        g2.setOwner(dv2);

        g1.add(g2);
        fail("An explicit group cannot contain an explicit group defined in "
                     + "a dataverse that's not an ancestor of that group's owner dataverse.");

    }

    @Test
    public void addGroup() throws GroupException {
        Dataverse dvParent = makeDataverse();
        Dataverse dvSub = makeDataverse();
        dvSub.setOwner(dvParent);

        ExplicitGroup g1 = new ExplicitGroup();
        ExplicitGroup g2 = new ExplicitGroup();
        g1.setOwner(dvSub);
        g2.setOwner(dvParent);

        g1.add(g2);
        assertTrue(g1.structuralContains(g2));
    }

    @Test
    public void adds() throws GroupException {
        Dataverse dvParent = makeDataverse();
        ExplicitGroup g1 = new ExplicitGroup();
        g1.setOwner(dvParent);

        AuthenticatedUser au1 = makeAuthenticatedUser("Lauren", "Ipsum");
        g1.add(au1);
        g1.add(GuestUser.get());

        assertTrue(g1.structuralContains(GuestUser.get()));
        assertTrue(g1.structuralContains(au1));
        assertFalse(g1.structuralContains(makeAuthenticatedUser("Sima", "Kneidle")));
        assertFalse(g1.structuralContains(AllUsers.get()));
    }


    @Test
    public void recursiveStructuralContainment() throws GroupException {
        Dataverse dvParent = makeDataverse();
        ExplicitGroup parentGroup = roleAssigneeSvc.add(makeExplicitGroup());
        ExplicitGroup childGroup = roleAssigneeSvc.add(makeExplicitGroup());
        ExplicitGroup grandChildGroup = roleAssigneeSvc.add(makeExplicitGroup());
        parentGroup.setOwner(dvParent);
        childGroup.setOwner(dvParent);
        grandChildGroup.setOwner(dvParent);

        childGroup.add(grandChildGroup);
        parentGroup.add(childGroup);

        AuthenticatedUser au = roleAssigneeSvc.add(makeAuthenticatedUser("Jane", "Doe"));
        grandChildGroup.add(au);
        childGroup.add(GuestUser.get());

        assertTrue(grandChildGroup.structuralContains(au));
        assertTrue(childGroup.structuralContains(au));
        assertTrue(parentGroup.structuralContains(au));

        assertTrue(childGroup.structuralContains(GuestUser.get()));
        assertTrue(parentGroup.structuralContains(GuestUser.get()));

        grandChildGroup.remove(au);

        assertFalse(grandChildGroup.structuralContains(au));
        assertFalse(childGroup.structuralContains(au));
        assertFalse(parentGroup.structuralContains(au));

        childGroup.add(AuthenticatedUsers.get());

        assertFalse(grandChildGroup.structuralContains(au));
        assertFalse(childGroup.structuralContains(au));
        assertFalse(parentGroup.structuralContains(au));
        assertTrue(childGroup.structuralContains(AuthenticatedUsers.get()));

        final IpGroup ipGroup = new IpGroup();
        grandChildGroup.add(ipGroup);
        ipGroup.add(IpAddressRange.make(IpAddress.valueOf("0.0.1.1"), IpAddress.valueOf("0.0.255.255")));

        assertTrue(grandChildGroup.structuralContains(ipGroup));
        assertTrue(childGroup.structuralContains(ipGroup));
        assertTrue(parentGroup.structuralContains(ipGroup));
    }

    @Test
    public void recursiveLogicalContainment() throws GroupException {
        Dataverse dvParent = makeDataverse();
        ExplicitGroup parentGroup = roleAssigneeSvc.add(makeExplicitGroup("parent"));
        ExplicitGroup childGroup = roleAssigneeSvc.add(makeExplicitGroup("child"));
        ExplicitGroup grandChildGroup = roleAssigneeSvc.add(makeExplicitGroup("grandChild"));
        parentGroup.setOwner(dvParent);
        childGroup.setOwner(dvParent);
        grandChildGroup.setOwner(dvParent);

        childGroup.add(grandChildGroup);
        parentGroup.add(childGroup);

        AuthenticatedUser au = roleAssigneeSvc.add(makeAuthenticatedUser("Jane", "Doe"));
        grandChildGroup.add(au);
        childGroup.add(GuestUser.get());
        DataverseRequest auReq = makeRequest(au);
        DataverseRequest guestReq = makeRequest();

        assertTrue(prv.contains(auReq, grandChildGroup));
        assertTrue(prv.contains(auReq, childGroup));
        assertTrue(prv.contains(auReq, parentGroup));

        assertTrue(prv.contains(guestReq, childGroup));
        assertTrue(prv.contains(guestReq, parentGroup));

        grandChildGroup.remove(au);

        assertFalse(prv.contains(auReq, grandChildGroup));
        assertFalse(prv.contains(auReq, childGroup));
        assertFalse(prv.contains(auReq, parentGroup));

        childGroup.add(AuthenticatedUsers.get());

        assertFalse(prv.contains(auReq, grandChildGroup));
        assertTrue(prv.contains(auReq, childGroup));
        assertTrue(prv.contains(auReq, parentGroup));

        final IpGroup ipGroup = roleAssigneeSvc.add(new IpGroup());
        grandChildGroup.add(ipGroup);
        ipGroup.add(IpAddressRange.make(IpAddress.valueOf("0.0.1.1"), IpAddress.valueOf("0.0.255.255")));
        final IpAddress ip = IpAddress.valueOf("0.0.128.128");
        final DataverseRequest request = new DataverseRequest(GuestUser.get(), ip);

        assertTrue(ipGroupProvider.contains(request, ipGroup));
        assertTrue(prv.contains(request, grandChildGroup));
        assertTrue(prv.contains(request, parentGroup));

        childGroup.add(GuestUser.get());
        assertTrue(prv.contains(guestReq, childGroup));
        assertTrue(prv.contains(guestReq, parentGroup));
        assertFalse(prv.contains(guestReq, grandChildGroup));

    }
}


