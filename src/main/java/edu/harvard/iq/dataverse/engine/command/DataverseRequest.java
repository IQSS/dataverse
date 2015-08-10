package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.User;
import javax.servlet.http.HttpServletRequest;

/**
 * 
 * A request in the dataverse context. Similar to an HTTP request (and indeed 
 * wraps one) but has more data that's specific to the Dataverse application.
 * 
 * @author michael
 */
public class DataverseRequest {
    
    private final User user;
    private final IpAddress sourceAddress;
    private final HttpServletRequest httpRequest;

    public DataverseRequest(User aUser, HttpServletRequest aHttpServletRequest) {
        this.user = aUser;
        httpRequest = aHttpServletRequest;
        String remoteAddressStr = null;
        try {
            remoteAddressStr = httpRequest.getHeader("X-Forwarded-For");
        } catch ( NullPointerException _npe ) {
            // ignore
        }
        
        if ( remoteAddressStr == null ) {
            try {
                remoteAddressStr = httpRequest.getRemoteAddr();
            } catch ( NullPointerException _npe ) {}
        }
        
        if ( remoteAddressStr == null ) {
            remoteAddressStr = "0.0.0.0";
        }
        sourceAddress = IpAddress.valueOf( remoteAddressStr );
    }

    public User getUser() {
        return user;
    }

    /**
     * @return The IP address from which this request arrived.
     */
    public IpAddress getSourceAddress() {
        return sourceAddress;
    }

    
    @Override
    public String toString() {
        return "[DataverseRequest user:" + getUser() + "@" + getSourceAddress() + "]";
                
    }
    
    
}
