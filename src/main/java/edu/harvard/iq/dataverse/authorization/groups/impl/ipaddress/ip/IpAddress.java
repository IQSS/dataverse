package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip;

/**
 * Base class for IP addresses. There are two concrete subclasses - IPv4 adn IPv6.
 * 
 * @author michael
 */
public abstract class IpAddress {
    
    /**
     * Parse a given string into a IPv4 or IPv6 address.
     * @param s IP address as string representation
     * @return An object to interact with an IP. If s is null will return null.
     * @throws IllegalArgumentException if s is not a valid ip
     */
    public static IpAddress valueOf( String s ) {
        if ( s == null ) return null;
        if ( s.contains(".") ) {
            if ( s.contains(":") ){
                return IPv6Address.valueOfMapped(s);
            } else {
                return IPv4Address.valueOf(s);
            }
            
        } else {
            return IPv6Address.valueOf( s );
        }
    }
    
    /**
     * Parse a given string into a IPv4 or IPv6 address or return default.
     * @param s IP address as string representation
     * @param def A default value to return if s is null or fails to parse.
     * @return An object to interact with an IP, either the parsed one or the default value.
     * @throws IllegalArgumentException if def is null.
     */
    public static IpAddress valueOf(String s, IpAddress def) {
        if (def == null) { throw new IllegalArgumentException("Default IP address may not be null."); }
        try {
            return valueOf(s);
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }
    
    
    public abstract boolean isLocalhost();
    
}
