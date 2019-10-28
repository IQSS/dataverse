package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsersGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsersProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MockRoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.AuthenticatedUsers;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.group.GroupException;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.group.IpAddressRange;
import edu.harvard.iq.dataverse.persistence.group.IpGroup;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import org.junit.Test;

import static edu.harvard.iq.dataverse.mocks.MockRequestFactory.makeRequest;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeAuthenticatedUser;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeDataverse;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeExplicitGroup;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExplicitGroupProviderTest {

    private MockRoleAssigneeServiceBean roleAssigneeSvc = new MockRoleAssigneeServiceBean();
    private IpGroupProvider ipGroupProvider = new IpGroupProvider(null);

    private ExplicitGroupProvider prv = new ExplicitGroupProvider(null, roleAssigneeSvc, Lists.newArrayList(ipGroupProvider,
            AuthenticatedUsersProvider.get(), AllUsersGroupProvider.get()));


    @Test
    public void recursiveLogicalContainment()  {
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
