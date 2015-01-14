package edu.harvard.iq.ip;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv6Address;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv4Address;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author michael
 */
public class IpAddressTest {
    
    @Test
    public void testValueOfIPv4() {
        assertEquals( new IPv4Address(127,0,0,1),
                    IpAddress.valueOf("127.0.0.1") );
        assertEquals( new IPv4Address(149,78,247,173),
                    IpAddress.valueOf("149.78.247.173") );
    }
    
    @Test
    public void testValueOfIPv6() {
        assertEquals( new IPv6Address(0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0x0, 0x0),
                      IpAddress.valueOf("a:b:c:d:e:f::"));
    }

    @Ignore
    @Test
    public void testValueOfPageKiteIp() {
        /**
         * @todo Remove @Ignore and get this passing to fix
         * https://github.com/IQSS/dataverse/issues/1281
         */
        IpAddress.valueOf("::ffff:65.112.10.85");
    }

}
