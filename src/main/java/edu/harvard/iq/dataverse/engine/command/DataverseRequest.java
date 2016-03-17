package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.logging.Level;
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
    
    public DataverseRequest(User aUser, HttpServletRequest aHttpServletRequest) {
        this.user = aUser;
        String remoteAddressStr = null;
        
        try {
            remoteAddressStr = aHttpServletRequest.getRemoteAddr();
        } catch (Exception _npe) {}
        
        /*
            It was reported earlier, that HttpServletRequest.getRemoteAddr() does 
            not supply the correct address of the incoming request, when Glassfish 
            is running behind the Apache proxy. This is NOT true - can be easily 
            confirmed with the log message below (will be switched to .fine when 
            released). When this constructor is called by the ApiBlockingFileter, 
            the remoteAddressStr will correctly show the remote address; whether
            we are behind a proxy, or not.
            As of now (4.2.3), this is the ONLY situation where we check the remote
            IP address for the purposes of Authentication/Authorization. 
        
            HOWEVER, this log message below consistently shows NULL when non-API,  
            page requests are coming in. This is true BOTH with or without the proxy. 
            So this must mean that when the DataverseRequest object is created 
            somewhere outside the ApiBlockingFilter, it likely doesn't get the 
            HttpServletRequest object properly passed to the constructor. 
            Which further means that in order to enable IP groups, we'll have 
            to fix that, and make sure the IP address is properly set for such 
            requests. 
        
            But, once again, as of now this sourceAddress is only being used for 
            determining if an API request is coming from localhost. 
        
            (It appears that for all the regular page requests, the DataverseRequest
            object gets created in DataverseRequestServiceBean.java, like this: 
                dataverseRequest = new DataverseRequest(dataverseSessionSvc.getUser(), httpRequest);
            with the httpRequest supplied by the @Context... why this isn't working
            properly - I don't know)
    
            -- L.A. 4.2.3
        */
        
        logger.fine("DataverseRequest: Obtained remote address: "+remoteAddressStr);
        
        if ( remoteAddressStr == null ) {
            remoteAddressStr = "0.0.0.0";
        }
        sourceAddress = IpAddress.valueOf( remoteAddressStr );
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
    
    
}
