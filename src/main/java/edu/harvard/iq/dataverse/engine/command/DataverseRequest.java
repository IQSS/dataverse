package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetricsServiceBean;

import java.util.logging.Logger;

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
    
    private static final Logger logger = Logger.getLogger(DataverseRequest.class.getCanonicalName());

    private static final String[] HEADERS_TO_TRY = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR" };

    
    public DataverseRequest(User aUser, HttpServletRequest aHttpServletRequest) {
        this.user = aUser;

        final String undefined = "0.0.0.0";
        String saneDefault = undefined;
        String remoteAddressStr = saneDefault;

        if (aHttpServletRequest != null) {
            String remoteAddressFromRequest = aHttpServletRequest.getRemoteAddr();
            if (remoteAddressFromRequest != null) {
                remoteAddressStr = remoteAddressFromRequest;
            }
        }
        sourceAddress = IpAddress.valueOf( remoteAddressStr );
        

        String ip = "Not Found";
        for (String header : HEADERS_TO_TRY) {
            ip = aHttpServletRequest.getHeader(header);
            logger.info("IP from : " + header + ": " + ip);
           // if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
           //     break;
           // }
        }

        
    }

    public DataverseRequest( User aUser, IpAddress aSourceAddress ) {
        user = aUser;
        sourceAddress = aSourceAddress;
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
    
    /**
     * Get an AuthenticatedUser or return null
     * @return 
     */
    public AuthenticatedUser getAuthenticatedUser(){
        
        User authUser = this.getUser();
        
        if (authUser instanceof AuthenticatedUser){
            return (AuthenticatedUser)authUser;
        }
        return null;
    }
    
}
