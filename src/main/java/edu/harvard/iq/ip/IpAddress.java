package edu.harvard.iq.ip;

/**
 * Base class for IP addresses. There are two concrete subclasses - IPv4 adn IPv6.
 * 
 * @author michael
 */
public abstract class IpAddress {
    
    public static IpAddress valueOf( String s ) {
        try {
            return IPv4Address.valueOf( s );
        } catch ( IllegalArgumentException iae ) {
            return IPv6Address.valueOf( s );
        }
    }
    
    
    public abstract boolean isLocalhost();
    
}
