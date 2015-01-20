package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

/**
 * Meta-data about the a request a user makes. Holds the actual HTTP request,
 * as well as global group memberships and other useful data.
 * 
 * @author michael
 */
public class UserRequestMetadata {
   
    private static final Logger logger = Logger.getLogger(UserRequestMetadata.class.getName());
    
    private final IpAddress ipAddress;
    
    private final HttpServletRequest httpRequest;
    
    private final Set<Group> groups = new HashSet<>();
    
    public UserRequestMetadata( HttpServletRequest aHttpServletRequest ) {
        httpRequest = aHttpServletRequest;
        String remoteAddressStr = null;
        try {
            remoteAddressStr = httpRequest.getHeader("X-Forwarded-For");
        } catch ( NullPointerException npe ) {
            // ignore
        }
        
        if ( remoteAddressStr == null ) {
            try {
                remoteAddressStr = httpRequest.getRemoteAddr();
            } catch ( NullPointerException npe ) {
                logger.warning("HTTP request has no remote server address");
            }
        }
        
        if ( remoteAddressStr == null ) {
            remoteAddressStr = "0.0.0.0";
            logger.log(Level.INFO, "Error parsing remote address: {0}. Setting address to undefined (0.0.0.0).", remoteAddressStr);
        }
        ipAddress = IpAddress.valueOf( remoteAddressStr );
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public Set<Group> getGroupMemberships() {
        return groups;
    }
    
    /**
     * Adds the user to the group for this request only - the group membership
     * is not persisted.
     * 
     * @param g group the user is a member of, for the described request.
     */
    public void addGroupMembership( Group g ) {
        groups.add(g);
    }
}
