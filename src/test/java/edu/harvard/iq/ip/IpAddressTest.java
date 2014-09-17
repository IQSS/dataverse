package edu.harvard.iq.ip;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class IpAddressTest {
    
    
    /**
     * Test of valueOf method, of class IpAddress.
     */
    @Test
    public void testValueOf() {
        assertEquals( new IpAddress(1,2,3,4), IpAddress.valueOf("1.2.3.4") );
        assertEquals( new IpAddress(127,0,0,1), IpAddress.valueOf("127.0.0.1") );
    }
    
    @Test( expected=IllegalArgumentException.class )
    public void testValueOf_bad() {
        IpAddress.valueOf("1.2.3");
    }

    /**
     * Test of toString method, of class IpAddress.
     */
    @Test
    public void testToString() {
        assertEquals( "127.0.0.1", new IpAddress( 127,0,0,1).toString() );
    }
    
}
