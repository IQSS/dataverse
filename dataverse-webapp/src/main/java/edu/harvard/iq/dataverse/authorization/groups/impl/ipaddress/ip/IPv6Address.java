package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip;

import java.util.Arrays;

/**
 * 
 * @author michael
 */
 public class IPv6Address extends IpAddress implements Comparable<IPv6Address> {
    
    public static IPv6Address valueOf( String in ) {
        if ( in.contains("%") ) {
            // remove network interface name, if present.
            in = in.split("%")[0];
        }
        if ( in.contains(".") ) {
            return valueOfMapped( in );
        }
        if ( in.contains("::") ) {
            // expand the :: abbreviation
            int existingFields = 0;
            for ( String cmp : in.split(":") ) {
                if ( ! cmp.trim().isEmpty() ) {
                    existingFields++;
                }
            }

            int missingFieldCount = 8-existingFields;
            StringBuilder sb = new StringBuilder( in.startsWith("::") ? "" : ":" );
            for ( int i=0; i<missingFieldCount; i++ ) {
                sb.append("0:");
            }
            if ( in.endsWith("::") ) {
                sb.setLength( sb.length()-1 );
            }
            in = in.replace( "::", sb.toString() );
            
        }
        
        // Invariant: in is expanded (no "::" abbreviation)
        String[] comps = in.split(":", -1);
        if ( comps.length != 8 ) {
            throw new IllegalArgumentException("IPv6 requires 8 words (or the usage of the :: abbreviation)");
        }
        int[] words = new int[8];
        
        // Invariant: in is of the form "n:n:n:n:n:n:n:n", where n is hopefully a hex number.
        int wordIdx = 0;
        for ( String comp : comps ) {
            try {
                words[wordIdx++] = Integer.parseInt(comp, 16);
            } catch ( NumberFormatException nfe ) {
                throw new IllegalArgumentException("Numbers in IPv6 addresses should be in hexadecimal notation.", nfe);
            }
        }
        
        return new IPv6Address( words, false );
    }
    
    public static IPv6Address valueOfMapped( String in ) {
        // Split parts
        int lastColon = in.lastIndexOf(":");
        String ipv4Part = in.substring(lastColon+1);
        String ipv6Part = in.substring(0, lastColon+1) + "0:0";
        
        // Parse
        short[] ipv4bytes = IPv4Address.valueOf(ipv4Part).bytes;
        int[] ipv6words = IPv6Address.valueOf(ipv6Part).words;
        
        // merge
        ipv6words[6]=(((int)ipv4bytes[0])<<8)+ipv4bytes[1];
        ipv6words[7]=(((int)ipv4bytes[2])<<8)+ipv4bytes[3];
        return new IPv6Address(ipv6words);
    }
    
    private final int[] words;
    
    /**
     * Constructor that does not copy the int array - but can be used
     * only from within this class. Especially made for the {@link valueOf} method.
     * @param words
     * @param dummy 
     */
    private IPv6Address( int[] words, boolean dummy ) {
        this.words = words;
    }
    
    public IPv6Address(int[] words) {
        if ( words.length != 8 ) throw new IllegalArgumentException("IPv6 address requires exactly 8 ints. Consider using the valueOf method to support abbreviations");
        this.words = Arrays.copyOf(words, words.length);
    }
    
    public IPv6Address( long[] longs ) {
        words = new int[]{
            (int)(longs[0] >>> 32),
            (int)(longs[0] & 0xffffffffl),
            (int)(longs[1] >>> 32),
            (int)(longs[1] & 0xffffffffl),
            (int)(longs[2] >>> 32),
            (int)(longs[2] & 0xffffffffl),
            (int)(longs[3] >>> 32),
            (int)(longs[3] & 0xffffffffl)
        };
    }
    
    public IPv6Address( int w1, int w2, int w3, int w4, int w5, int w6, int w7, int w8 ) {
        words = new int[]{w1, w2, w3, w4, w5, w6, w7, w8};
    }
    
    public int get(int idx) {
        return words[idx];
    }
    
    public long[] toLongArray() {
        long[] retVal = new long[4];
        for ( int i=0; i<4; i++ ) {
            retVal[i] = words[2*i];
            retVal[i] = (retVal[i]<<32);
            retVal[i] =  retVal[i] + words[2*i+1];
        }
        return retVal;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Arrays.hashCode(this.words);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if ( ! (obj instanceof IPv6Address) ) {
            return false;
        }
        final IPv6Address other = (IPv6Address) obj;
        return Arrays.equals(this.words, other.words);
    }

    @Override
    public boolean isLocalhost() {
        return Arrays.equals(words, new int[]{0,0,0,0,0,0,0,1} );
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for ( int i=0; i<words.length; i++ ) {
            sb.append( Integer.toString(words[i], 16) )
              .append( i<7 ? ":" : "" );
        }
        return sb.toString();
    }

    @Override
    public int compareTo(IPv6Address o) {
        for ( int i=0; i<8; i++ ) {
            if ( get(i) != o.get(i) ) {
                return get(i)-o.get(i);
            }
        }
        return 0;
    }
}