package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip;

/**
 * Base class for IP addresses. There are two concrete subclasses - IPv4 adn IPv6.
 * 
 * @author michael
 */
public abstract class IpAddress {
    
    public static IpAddress valueOf( String s ) {
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
    
    
    public abstract boolean isLocalhost();
    
}
