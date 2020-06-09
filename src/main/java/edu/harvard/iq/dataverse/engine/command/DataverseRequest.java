package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetricsServiceBean;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.ejb.Stateful;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 * 
 * A request in the dataverse context. Similar to an HTTP request (and indeed 
 * wraps one) but has more data that's specific to the Dataverse application.
 * 
 * @author michael
 */

@Stateful
public class DataverseRequest {
    
    @Inject
    SettingsWrapper settingsWrapper;
    
    private final User user;
    private final IpAddress sourceAddress;
    
    private static final Logger logger = Logger.getLogger(DataverseRequest.class.getCanonicalName());

    private static String headerToUse = System.getProperty("dataverse.useripaddresssourceheader");
    
    private static final HashSet<String> ALLOWED_HEADERS = new HashSet<String>(Arrays.asList( 
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
            "REMOTE_ADDR" ));
     
    
    public DataverseRequest(User aUser, HttpServletRequest aHttpServletRequest) {
        this.user = aUser;

        final String undefined = "0.0.0.0";
        final String localhost = "127.0.0.1";
        String saneDefault = undefined;
        String remoteAddressStr = saneDefault;

        if (aHttpServletRequest != null) {
            //Security check - make sure any supplied header is one that is used to forward IP addresses
            if (headerToUse != null && ALLOWED_HEADERS.contains(headerToUse)) {
                String ip = "Not Found";
                ip = aHttpServletRequest.getHeader(headerToUse);
                if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                    remoteAddressStr = ip;
                }
            }
            /*
             * If there was no header/no value from the header, or the header claims the
             * request is from localhost, use the remoteAddress. (Hard to imagine a
             * legitimate case where a header would be localhost, but misconfiguration could
             * allow a nefarious agent to try spoofing a localhost address via the header
             * (e.g. to gain access to admin functions.)
             * 
             */
            if (remoteAddressStr.equals(saneDefault) || remoteAddressStr.equals(localhost)) {
                // use the request remote address
                String remoteAddressFromRequest = aHttpServletRequest.getRemoteAddr();
                if (remoteAddressFromRequest != null) {
                    remoteAddressStr = remoteAddressFromRequest;
                }
            }
        }
        sourceAddress = IpAddress.valueOf(remoteAddressStr);
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
