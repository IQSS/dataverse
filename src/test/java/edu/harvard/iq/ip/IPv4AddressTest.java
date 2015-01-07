package edu.harvard.iq.ip;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv4Address;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class IPv4AddressTest {
    
    
    /**
     * Test of valueOf method, of class IpAddress.
     */
    @Test
    public void testValueOf() {
        assertEquals( new IPv4Address(1,2,3,4), IPv4Address.valueOf("1.2.3.4") );
        assertEquals( new IPv4Address(127,0,0,1), IPv4Address.valueOf("127.0.0.1") );
    }
    
    @Test( expected=IllegalArgumentException.class )
    public void testValueOf_bad() {
        IPv4Address.valueOf("1.2.3");
    }
    
    @Test
    public void testLocalhostness() {
        assertTrue( IPv4Address.valueOf("127.0.0.1").isLocalhost() );
        assertFalse( IPv4Address.valueOf("67.3.44.11").isLocalhost() );
    }
    
    /**
     * Test of toString method, of class IpAddress.
     */
    @Test
    public void testToString() {
        assertEquals( "127.0.0.1", new IPv4Address( 127,0,0,1).toString() );
    }
    
    
    @Test
    public void testComparator() {
        IPv4Address[] expected = new IPv4Address[]{
                new IPv4Address(127, 20, 20, 40),
                new IPv4Address(127, 20, 30, 40),
                new IPv4Address(127, 20, 30, 41),
                new IPv4Address(128, 00, 00, 00)
        };
        
        IPv4Address[] scrambled = new IPv4Address[]{expected[3], expected[2],expected[0],expected[1]};
        Arrays.sort(scrambled);
        assertArrayEquals( expected, scrambled );
    }
    
    @Test
    public void testLongRoundtrip() {
        for ( IPv4Address addr : Arrays.asList(
                new IPv4Address(127,0,36,255),
                new IPv4Address(0,0,0,0),
                new IPv4Address(127,0,0,1),
                new IPv4Address(128,0,0,1),
                new IPv4Address(0,0,127,1),
                new IPv4Address(0,0,128,1),
                new IPv4Address(128,128,128,128),
                new IPv4Address(127,127,127,127),
                new IPv4Address(255,255,255,255),
                new IPv4Address(255,0,34,1)) ) {
            assertEquals( addr, new IPv4Address(addr.toLong()) );
        }
    }
    
    @Test
    public void testBitOffset() {
        for ( int i=0; i<64; i++ ) {
            System.out.println("0x1<<" + i + "=\t" + (1l<<i));
        }
    }
    
}
