package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
    
    private String[] p(String a, String b ) {
        return new String[]{a,b};
    }
    
    @Test
    public void testMappedIPv6() {
        for ( String[] tp : Arrays.asList(
            p("::1","0000:0000:0000:0000:0000:0000:0000:0001"),
            p("2001:db8::1","2001:0db8:0000:0000:0000:0000:0000:0001"),
            p("::192.168.0.1","0000:0000:0000:0000:0000:0000:c0a8:0001"),
            p("::ffff:192.168.0.1","0000:0000:0000:0000:0000:ffff:c0a8:0001"),
            p("::ffff:65.112.10.85","::ffff:4170:a55"),
            p("::192.168.0.1","0:0:0:0:0:0:c0a8:1")
        ) ) {
            assertEquals( IpAddress.valueOf(tp[1]), IpAddress.valueOf(tp[0]));
        }
        
    }

}
