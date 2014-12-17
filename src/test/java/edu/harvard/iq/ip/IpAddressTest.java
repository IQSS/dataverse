package edu.harvard.iq.ip;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv6Address;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv4Address;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class IpAddressTest {
    
    @Test
    public void testValueOfIPv4() {
        assertEquals( new IPv4Address(127,0,0,1),
                    IpAddress.valueOf("127.0.0.1") );
    }
    
    @Test
    public void testValueOfIPv6() {
        assertEquals( new IPv6Address(0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0x0, 0x0),
                      IpAddress.valueOf("a:b:c:d:e:f::"));
    }
    
}
