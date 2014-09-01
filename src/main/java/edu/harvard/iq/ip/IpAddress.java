package edu.harvard.iq.ip;

import java.util.Arrays;

/**
 * @author michael
 */
public class IpAddress {
    
    public static IpAddress valueOf( String s ) {
        String[] comps = s.split("\\.");
        if ( comps.length != 4 ) {
            throw new IllegalArgumentException("IpAddress string expected to be in xxx.xxx.xxx.xxx format");
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
