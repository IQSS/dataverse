package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Meta-data about the a request a user makes. Holds the actual HTTP request,
 * as well as global group memberships and other useful data.
 * 
 * @author michael
 */
public class UserRequestMetadata {
   
    private final IpAddress ipAddress;
    
    private final HttpServletRequest httpRequest;
    
    private Set<Group> groups = new HashSet<>();
    
    public UserRequestMetadata( HttpServletRequest aHttpServletRequest ) {
        httpRequest = aHttpServletRequest;
        
        ipAddress = IpAddress.valueOf( (httpRequest.getHeader("X-Forwarded-For") != null)
                    ? httpRequest.getHeader("X-Forwarded-For")
                    : httpRequest.getRemoteAddr() );
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
