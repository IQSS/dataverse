package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.api.batchjob.FileRecordJobResource;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
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
    private final String invocationId;
    private final HttpServletRequest httpServletRequest;
    
    private final static String undefined = "0.0.0.0";
    
    private final static String MDKEY_PREFIX="mdkey.";
    
    private static final Logger logger = Logger.getLogger(DataverseRequest.class.getName());
    
    private static String headerToUse = null;
    
    private static final HashSet<String> ALLOWED_HEADERS = new HashSet<String>(Arrays.asList( 
            "x-forwarded-for",
            "proxy-client-ip",
            "wl-proxy-client-ip",
            "http_x_forwarded_for",
            "http_x_forwarded",
            "http_x_cluster_client_ip",
            "http_client_ip",
            "http_forwarded_for",
            "http_forwarded",
            "http_via",
            "remote_addr" ));
     
    static {
        String header = System.getProperty("dataverse.useripaddresssourceheader");
        // Security check - make sure any supplied header is one that is used to forward
        // IP addresses (case insensitive)
        if((header!=null) && (ALLOWED_HEADERS.contains(header.toLowerCase()))) {
            headerToUse=header;
        }
    }
    
    public DataverseRequest(User aUser, HttpServletRequest aHttpServletRequest) {
        this.user = aUser;
        httpServletRequest = aHttpServletRequest;
        
        IpAddress address = null;

        if (aHttpServletRequest != null) {
           
            if (headerToUse != null) {
                /*
                 * The optional case of using a header to determine the IP address is discussed
                 * at length in https://github.com/IQSS/dataverse/pull/6973 and the related
                 * issue.
                 * 
                 * The code here is intended to support the use case of a single proxy (load
                 * balancer, etc.) as well as providing partial support for the case where two
                 * proxies exist (e.g. a campus proxy and an AWS load balancer). In this case,
                 * the IP address returned should be that of the proxy nearer the user which
                 * would be the correct address to use for making IPGroup access control
                 * determinations. This does limit the accuracy of any Make Data Count
                 * geolocation determined since it is the proxy's IP that would be geolocated.
                 * For a campus proxy, this may be acceptable. This code should be safe in that
                 * it won't pick up a spoofed address in any case, but beyond the two proxy
                 * case, it is unlikely to provide useful results (why would you want the IP of
                 * an intermediate proxy?).
                 */
                // One can have multiple instances of a given header. They SHOULD be in order.
                Enumeration<String> ipEnumeration = aHttpServletRequest.getHeaders(headerToUse);
                if (ipEnumeration.hasMoreElements()) {
                    // Always get the last header, which SHOULD be from the proxy closest to
                    // Dataverse
                    String ip = ipEnumeration.nextElement();
                    while (ipEnumeration.hasMoreElements()) {
                        ip = ipEnumeration.nextElement();
                    }
                    // Always get the last value if more than one in the string, which should be the
                    // IP address closest to the reporting proxy
                    int index = ip.lastIndexOf(',');
                    if (index >= 0) {
                        ip = ip.substring(index + 1);
                    }
                    ip=ip.trim();
                    /*
                     * We should have a valid, single IP address string here. The IpAddress.valueOf
                     * call will throw an exception if it can't be parsed into a valid address (e.g.
                     * 4 '.' separated short ints for v4), so we just check for null here
                     */
                    if (ip != null) {
                        // This conversion will throw an IllegalArgumentException if it can't be parsed.
                        try {
                            address = IpAddress.valueOf(ip);
                        } catch (IllegalArgumentException iae) {
                            logger.warning("Ignoring invalid IP address received in header " + headerToUse + " : " + ip);
                            address = null;
                        }
                        if (address!= null && address.isLocalhost()) {
                            // Not allowed since it is hard to image why a localhost request would be
                            // proxied and we want to protect
                            // the internal admin apis that can be restricted to localhost access
                            logger.warning("Ignoring localhost received as IP address in header " + headerToUse + " : " + ip);
                            address = null;
                        }
                    }
                }
            }
            /*
             * If there was no header/no usable value from the header, use the
             * remoteAddress.
             * 
             */
            if (address == null) {
                // use the request remote address
                String remoteAddressFromRequest = aHttpServletRequest.getRemoteAddr();
                if (remoteAddressFromRequest != null) {
                    String remoteAddressStr = remoteAddressFromRequest;
                    try {
                        address = IpAddress.valueOf(remoteAddressStr);
                    } catch (IllegalArgumentException iae) {
                        address = IpAddress.valueOf(undefined);
                    }
                }
            }
            
            String headerParamWFKey = aHttpServletRequest.getHeader(AbstractApiBean.DATAVERSE_WORKFLOW_INVOCATION_HEADER_NAME);
            String queryParamWFKey = aHttpServletRequest.getParameter("invocationId");
                    
            invocationId = headerParamWFKey!=null ? headerParamWFKey : queryParamWFKey;

        } else {
            invocationId=null;
        }
        
        sourceAddress = address;
    }

    public DataverseRequest( User aUser, IpAddress aSourceAddress ) {
        user = aUser;
        sourceAddress = aSourceAddress;
        invocationId=null;
        httpServletRequest=null;
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

    public String getWFInvocationId() {
        return invocationId;
    }
    
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }
    
    public String getSystemMetadataBlockKeyFor(String blockName) {
        String key = null;
        if (httpServletRequest != null) {
            key = httpServletRequest.getHeader(MDKEY_PREFIX + blockName);
            logger.log(Level.FINE, ((key==null)? "Didn't find": "Found") + "system metadata block key for " + blockName + " in header");
            if (key == null) {
                key = httpServletRequest.getParameter(MDKEY_PREFIX + blockName);
                logger.log(Level.FINE, ((key==null)? "Didn't find": "Found") + "system metadata block key for " + blockName + " in query parameter");
            }
        }
        return key;
    }
    
}
