package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.group.IpAddressRange;
import edu.harvard.iq.dataverse.persistence.group.IpGroup;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IpGroupProviderTest {

    private IpGroupProvider ipGroupProvider = new IpGroupProvider(null);

    /**
     * Test of contains method, of class IpGroup.
     */
    @Test
    public void testContains() {
        IpGroup sut = new IpGroup();
        sut.setId(MocksFactory.nextId());
        sut.setDescription("A's description");
        sut.setDisplayName("A");
        sut.setPersistedGroupAlias("&ip/a");
        final IpAddressRange allIPv4 = IpAddressRange.make(IpAddress.valueOf("0.0.0.0"), IpAddress.valueOf("255.255.255.255"));
        final IpAddressRange allIPv6 = IpAddressRange.make(IpAddress.valueOf("0:0:0:0:0:0:0:0"),
                                                           IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));

        sut.add(allIPv4);
        sut.add(allIPv6);

        assertTrue(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.2.3.4")), sut));
        assertTrue(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("11::fff")), sut));

        sut.remove(allIPv4);
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.2.3.4")), sut));

        sut.remove(allIPv6);
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("11::fff")), sut));

        sut.add(IpAddressRange.make(IpAddress.valueOf("0.0.0.0"), IpAddress.valueOf("168.0.0.0")));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("169.0.0.0")), sut));
        assertTrue(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("167.0.0.0")), sut));

    }
}
