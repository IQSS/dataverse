package edu.harvard.iq.ip;

import java.util.Arrays;

/**
 * @author michael
 */
public class IpAddress {
    
    public static IpAddress valueOf( String s ) {
        String[] comps = s.split("\\.");
        if ( comps.length != 4 ) {
            throw new IllegalArgumentException("IpAddress string expected to be in xxx.xxx.xxx.xxx format (only 4 byte ipv4 addresses are supported)");
        }
        short[] arr = new short[4];
        for ( int i=0; i<4; i++ ) {
            arr[i] = Short.parseShort(comps[i]);
        }
        return new IpAddress(arr);
    }
    
    private final short[] address = new short[4];
    
    public IpAddress( short[] arr ) {
        System.arraycopy(arr, 0, address, 0, 4);
    }
    
    public IpAddress( short a, short b, short c, short d ) {
        this( new short[]{a,b,c,d} );
    }
    
    public IpAddress( int a, int b, int c, int d ) {
        this( new short[]{(short)a,(short)b,(short)c,(short)d} );
    }

    // Adding hooks for IPv6 support; both IPv4 and IPv6 should be 
    // supported; with the boolean methods below provided for identifying
    // which kind it is. 
    
    public boolean isIPv4() {
        // hard-coded for now. 
        return true;
    }
    
    public boolean isIPv6() {
        return false; 
    }
    
    public short[] getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + Arrays.hashCode(this.address);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if ( ! (obj instanceof IpAddress) ) {
            return false;
        }
        final IpAddress other = (IpAddress) obj;
        return Arrays.equals(this.address, other.address);
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d.%d", address[0], address[1], address[2], address[3]);
    }
    
}
