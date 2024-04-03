package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddressRange;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author michael
 */
public class IpGroupTest {
    
    public IpGroupTest() {
    }

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
        
        assertTrue( sut.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.2.3.4"))) );
        assertTrue( sut.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("11::fff"))) );
        
        sut.remove( allIPv4 );
        assertFalse( sut.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.2.3.4"))) );
        
        sut.remove( allIPv6 );
        assertFalse( sut.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("11::fff"))) );
        
        sut.add( IpAddressRange.make(IpAddress.valueOf("0.0.0.0"), IpAddress.valueOf("168.0.0.0")) );
        assertFalse( sut.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("169.0.0.0"))) );
        assertTrue( sut.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("167.0.0.0"))) );
        
    }

 
    /**
     * Test of isEditable method, of class IpGroup.
     */
    @Test
    public void testIsEditable() {
        IpGroup instance = new IpGroup();
        assertTrue( instance.isEditable() );
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
        a.add( IpAddressRange.make(IpAddress.valueOf("0.0.0.0"), IpAddress.valueOf("1.1.1.1")));
        
        assertFalse( a.equals("banana") );
        assertFalse( a.equals( null ) );
        assertTrue( a.equals( a) );
        
        IpGroup aa = new IpGroup();
        aa.setId(a.getId());
        aa.setDescription("A's description");
        aa.setDisplayName("A");
        aa.setPersistedGroupAlias("&ip/a");
        aa.add( IpAddressRange.make(IpAddress.valueOf("0.0.0.0"), IpAddress.valueOf("1.1.1.1")));
        
        assertTrue( a.equals(aa) );
        aa.add( IpAddressRange.make(IpAddress.valueOf("9.0.0.0"), IpAddress.valueOf("9.1.1.1")));
        assertFalse( a.equals(aa) );
        
    }
    
}
