package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author michael
 */
public class IpAddressRangeTest {
    
    @Test
    public void testIPv6In() {
        IPv6Range sut = new IPv6Range( IPv6Address.valueOf("::10"), IPv6Address.valueOf("::1:1") );
        testRange( Boolean.TRUE, sut, 
                        IPv6Address.valueOf("::10"),
                        IPv6Address.valueOf("::11"),
                        IPv6Address.valueOf("::ff"),
                        IPv6Address.valueOf("::1:0"),
                        IPv6Address.valueOf("::1:1"),
                        IPv6Address.valueOf("::1:1") );
    }
    
    @Test
    public void testIPv6Out() {
        IPv6Range sut = new IPv6Range( IPv6Address.valueOf("::10"), IPv6Address.valueOf("::1:1") );
        testRange( Boolean.FALSE, sut, 
                        IPv6Address.valueOf("::"),
                        IPv6Address.valueOf("::1"),
                        IPv6Address.valueOf("::9"),
                        IPv6Address.valueOf("700::"),
                        IPv6Address.valueOf("::1:2") );
    }
    
    @Test
    public void testIPv6NotApplicable() {
        IPv6Range sut = new IPv6Range( IPv6Address.valueOf("::10"), IPv6Address.valueOf("::1:1") );
        testRange( null, sut, 
                        IPv4Address.valueOf("1.2.3.4"));
    }
    
    @Test
    public void testIPv4In() {
        IPv4Range sut = new IPv4Range( IPv4Address.valueOf("127.0.0.2"), IPv4Address.valueOf("127.0.1.10"));
        testRange( Boolean.TRUE, sut, 
                    IPv4Address.valueOf("127.0.0.2"),
                    IPv4Address.valueOf("127.0.0.2"),
                    IPv4Address.valueOf("127.0.0.255"),
                    IPv4Address.valueOf("127.0.1.2"),
                    IPv4Address.valueOf("127.0.1.9"),
                    IPv4Address.valueOf("127.0.1.10")
                );
    }
    
    @Test
    public void testIPv4Out() {
        IPv4Range sut = new IPv4Range( IPv4Address.valueOf("127.0.0.2"), IPv4Address.valueOf("127.0.1.10"));
        testRange( Boolean.FALSE, sut, 
                    IPv4Address.valueOf("127.0.0.1"),
                    IPv4Address.valueOf("126.0.0.2"),
                    IPv4Address.valueOf("127.0.1.11"),
                    IPv4Address.valueOf("237.0.1.11"),
                    IPv4Address.valueOf("127.0.1.23")
                );
    }
    
    @Test
    public void testIPv4NotApplicable() {
        IPv4Range sut = new IPv4Range( IPv4Address.valueOf("127.0.0.2"), IPv4Address.valueOf("127.0.1.10"));
        testRange( null, sut, 
                    IPv6Address.valueOf("::1")
                );
    }
    
    @Test
    public void testSingleAddress() {
        assertTrue( new IPv4Range( IPv4Address.valueOf("127.5.5.5"), IPv4Address.valueOf("127.5.5.5")).isSingleAddress() );
        assertFalse( new IPv4Range( IPv4Address.valueOf("17.5.5.5"), IPv4Address.valueOf("127.5.5.5")).isSingleAddress() );
        
        assertTrue( new IPv6Range( IPv6Address.valueOf("::1:1"), IPv6Address.valueOf("::1:1")).isSingleAddress() );
        assertFalse( new IPv6Range( IPv6Address.valueOf("::1:1"), IPv6Address.valueOf("::1:2")).isSingleAddress() );
    }
    
    public void testRange( Boolean expected, IpAddressRange range, IpAddress... addresses ) {
        for ( IpAddress ipa : addresses ) {
            assertEquals(expected, range.contains(ipa), "Testing " + ipa + " in " + range);
        }
    }
    
}
