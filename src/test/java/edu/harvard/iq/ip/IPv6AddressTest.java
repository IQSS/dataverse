/*
 *  (C) Michael Bar-Sinai
 */
package edu.harvard.iq.ip;

import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class IPv6AddressTest {
    
    public IPv6AddressTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testValueOfNoExpansion() {
        int[] expected = new int[]{0x2001,0xdb8,0x85a3,0x0,0x0,0x8a2e,0x370,0x7334};
        IPv6Address adr = IPv6Address.valueOf("2001:db8:85a3:0:0:8a2e:370:7334");
        for ( int i=0; i<8; i++ ) {
            assertEquals(expected[i], adr.get(i));
        }
    }
    
    @Test
    public void testValueOfWithExpansion() {
        int[] expected = new int[]{0x2001,0xdb8,0x85a3,0x0,0,0x8a2e,0x370,0x7334};
        IPv6Address adr = IPv6Address.valueOf("2001:db8:85a3::8a2e:370:7334");
        for ( int i=0; i<8; i++ ) {
            assertEquals("At index " + i + ": expecting " + expected[i] + ", got " + adr.get(i),
                         expected[i], adr.get(i));
        }
        
        expected = new int[]{0x2001,0xdb8,0x0,0x0,0x0,0x0,0x370,0x7334};
        adr = IPv6Address.valueOf("2001:db8::370:7334");
        for ( int i=0; i<8; i++ ) {
            assertEquals("At index " + i + ": expecting " + expected[i] + ", got " + adr.get(i),
                         expected[i], adr.get(i));
        }
    }
    
    @Test
    public void testValueOfWithExpansionZerosAtStart() {
        int[] expected = new int[]{0,0,0,0,0,0x8a2e,0x370,0x7334};
        IPv6Address adr = IPv6Address.valueOf("::8a2e:370:7334");
        for ( int i=0; i<8; i++ ) {
            assertEquals("At index " + i + ": expecting " + expected[i] + ", got " + adr.get(i),
                         expected[i], adr.get(i));
        }
        
        expected = new int[]{0,0,0,0,0,0,0,0x7334};
        adr = IPv6Address.valueOf("::7334");
        System.out.println("adr = " + adr);
        for ( int i=0; i<8; i++ ) {
            assertEquals("At index " + i + ": expecting " + expected[i] + ", got " + adr.get(i),
                         expected[i], adr.get(i));
        }
    }
    
    @Test
    public void testValueOfWithExpansionZerosAtEnd() {
        int[] expected = new int[]{0x2001,0x8a2e,0,0,0,0,0,0};
        IPv6Address adr = IPv6Address.valueOf("2001:8a2e::");
        for ( int i=0; i<8; i++ ) {
            assertEquals("At index " + i + ": expecting " + expected[i] + ", got " + adr.get(i),
                         expected[i], adr.get(i));
        }
        
        expected = new int[]{0x1337,0,0,0,0,0,0,0};
        adr = IPv6Address.valueOf("1337::");
        for ( int i=0; i<8; i++ ) {
            assertEquals("At index " + i + ": expecting " + expected[i] + ", got " + adr.get(i),
                         expected[i], adr.get(i));
        }
    }
    
    
    @Test
    public void testValueOfWithExpansionSpecialCases() {
        int[] expected = new int[]{0,0,0,0,0,0,0,0};
        IPv6Address adr = IPv6Address.valueOf("::");
        System.out.println("adr = " + adr);
        for ( int i=0; i<8; i++ ) {
            assertEquals("At index " + i + ": expecting " + expected[i] + ", got " + adr.get(i),
                         expected[i], adr.get(i));
        }
        
        expected = new int[]{0,0,0,0,0,0,0,1};
        adr = IPv6Address.valueOf("::1");
        for ( int i=0; i<8; i++ ) {
            assertEquals("At index " + i + ": expecting " + expected[i] + ", got " + adr.get(i),
                         expected[i], adr.get(i));
        }
    }
    
    @Test
    public void testLocalhostness() {
        assertTrue( IPv6Address.valueOf("::1").isLocalhost() );
        assertFalse( IPv6Address.valueOf("fff::1").isLocalhost() );
    }
    
    @Test( expected=IllegalArgumentException.class )
    public void testIllegalLength() {
        IPv6Address.valueOf("0:1:2:3");
    }
    
    @Test( expected=IllegalArgumentException.class )
    public void testIllegalLengthPrefix() {
        IPv6Address.valueOf(":1:2:3");
    }
    
    @Test( expected=IllegalArgumentException.class )
    public void testIllegalLengthSuffix() {
        IPv6Address.valueOf("1:2:3:");
    }
    
    @Test( expected=IllegalArgumentException.class )
    public void testIllegalNumber() {
        IPv6Address.valueOf("::xxx");
    }
    
    @Test
    public void testCompareTo() {
        IPv6Address[] expected = new IPv6Address[]{
                IPv6Address.valueOf("::"),
                IPv6Address.valueOf("::1"),
                IPv6Address.valueOf("::2"),
                IPv6Address.valueOf("::1:0"),
                IPv6Address.valueOf("::1:1"),
                IPv6Address.valueOf("1::")
        };
        
        IPv6Address[] scrambled = new IPv6Address[]{expected[5], expected[4], expected[3], expected[2],expected[0],expected[1]};
        Arrays.sort(scrambled);
        assertArrayEquals( expected, scrambled );
    }
}
