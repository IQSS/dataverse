package edu.harvard.iq.dataverse.persistence.group;

import edu.harvard.iq.dataverse.persistence.MocksFactory;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author michael
 */
public class IpGroupTest {


    /**
     * Test of isEditable method, of class IpGroup.
     */
    @Test
    public void testIsEditable() {
        IpGroup instance = new IpGroup();
        assertTrue(instance.isEditable());
    }

    /**
     * Test of equals method, of class IpGroup.
     */
    @Test
    public void testEquals() {
        IpGroup a = new IpGroup();
        a.setId(MocksFactory.nextId());
        a.setDescription("A's description");
        a.setDisplayName("A");
        a.setPersistedGroupAlias("&ip/a");
        a.add(IpAddressRange.make(IpAddress.valueOf("0.0.0.0"), IpAddress.valueOf("1.1.1.1")));

        assertFalse(a.equals("banana"));
        assertFalse(a.equals(null));
        assertTrue(a.equals(a));

        IpGroup aa = new IpGroup();
        aa.setId(a.getId());
        aa.setDescription("A's description");
        aa.setDisplayName("A");
        aa.setPersistedGroupAlias("&ip/a");
        aa.add(IpAddressRange.make(IpAddress.valueOf("0.0.0.0"), IpAddress.valueOf("1.1.1.1")));

        assertTrue(a.equals(aa));
        aa.add(IpAddressRange.make(IpAddress.valueOf("9.0.0.0"), IpAddress.valueOf("9.1.1.1")));
        assertFalse(a.equals(aa));

    }

}
