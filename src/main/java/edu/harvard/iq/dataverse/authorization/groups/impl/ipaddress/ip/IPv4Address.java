package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * 
 * @author michael
 */
public class IPv4Address extends IpAddress implements Comparable<IPv4Address> {
    
    public static IPv4Address valueOf( String input ) {
        String[] comps = input.split("\\.");
        if ( comps.length != 4 ) {
            throw new IllegalArgumentException("IPv4Address string expected to be in xxx.xxx.xxx.xxx format (only 4 byte ipv4 addresses are supported)");
        }
        short[] arr = new short[4];
        for ( int i=0; i<4; i++ ) {
            arr[i] = Short.parseShort(comps[i]);
        }
        return new IPv4Address(arr);
    }
    protected final short[] bytes = new short[4];
    
    public IPv4Address( short[] arr ) {
        System.arraycopy(arr, 0, bytes, 0, 4);
    }
    
    public IPv4Address( short a, short b, short c, short d ) {
        this( new short[]{a,b,c,d} );
    }
    
    public IPv4Address( int a, int b, int c, int d ) {
        this( new short[]{(short)a,(short)b,(short)c,(short)d} );
    }
    
    public IPv4Address( BigInteger bits ) {
        this( bits.longValue() );
    }
    
    public IPv4Address( long l ) {
        bytes[0] = (short)((l >>> 24) & 0xFF);
        bytes[1] = (short)((l >>> 16) & 0xFF);
        bytes[2] = (short)((l >>>  8) & 0xFF);
        bytes[3] = (short)(l & 0xFF);
    }
    
    @Override
    public boolean isLocalhost() {
        return Arrays.equals( new short[]{127,0,0,1} , bytes);
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d.%d", get(0), get(1), get(2), get(3));
    }

    public short get(int idx) {
        return bytes[idx];
    }

    public short[] getBytes() {
        return bytes;
    }
    
    public long toLong() {
        return (get(0)<<24) | (get(1)<<16) | (get(2)<<8) | get(3);
    }
    
    public BigInteger toBigInteger() {
        BigInteger res = BigInteger.ZERO;
        for ( int i=0; i<3; i++ ) {
            res = res.add(BigInteger.valueOf(get(i)))
                     .shiftLeft(8);
        }
        return res.add(BigInteger.valueOf(get(3)));
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Arrays.hashCode(this.bytes);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if ( ! (obj instanceof IPv4Address) ) {
            return false;
        }
        final IPv4Address other = (IPv4Address) obj;
        return Arrays.equals(this.bytes, other.bytes);
    }

    @Override
    public int compareTo(IPv4Address o) {
        for ( int i=0; i<4; i++ ) {
            if ( get(i) != o.get(i) ) {
                return get(i)-o.get(i);
            }
        }
        return 0;
    }
    
}
