package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.ip.IpAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;

/**
 * Utility class for common JSF tasks.
 * @author michael
 */
public class JsfHelper {
	private static final Logger logger = Logger.getLogger(JsfHelper.class.getName());
	
	public static final JsfHelper JH = new JsfHelper();
    
	public void addMessage( FacesMessage.Severity s, String summary, String details ) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, summary, details));
	}
	public void addMessage( FacesMessage.Severity s, String summary ) {
		addMessage(s, summary, "");
	}
	
	public <T extends Enum<T>> T enumValue( String param, Class<T> enmClass, T defaultValue ) {
		if ( param == null ) return defaultValue;
		param = param.trim();
		try {
			return Enum.valueOf(enmClass, param);
		} catch ( IllegalArgumentException iar ) {
			logger.log(Level.WARNING, "Illegal value for enum {0}: ''{1}''", new Object[]{enmClass.getName(), param});
			return defaultValue;
		}
	}
    
    /**
     * Finds the request IP address. 
     * @return the IP Address of the client issuing the request.
     */
    public IpAddress requestClientIpAddress() {
        ExternalContext ctxt = FacesContext.getCurrentInstance().getExternalContext();
        
        ServletRequest sr = (ServletRequest) ctxt.getRequest();
        String remoteAddress = sr.getRemoteAddr();
        
        logger.fine("remote address returned: "+remoteAddress);
        
        // it appears that in some environments (for ex., some scenarios under 
        // MacOS X) for requests coming from localhost, getRemoteHost() returns 
        // the ipv6 version -- 0:0:0:0:0:0:0:1. 
        // At this point our IpAddress only supports 4 byte ipv4 addresses.
        // We'll want to add support for ipv6 at some point; but for now, 
        // to be able to recognize localhost logins, for testing and such, 
        // we'll just pass "127.0.0.1" to IpAddress.valueOf() instead.
        // Any other ipv6 address is going to result in an exception. 
        // -- L.A. - 4.0 beta 7
        
        if ("0:0:0:0:0:0:0:1".equals(remoteAddress)) {
            remoteAddress = "127.0.0.1";
        }
        
        try {
            return IpAddress.valueOf(remoteAddress);
        } catch (IllegalArgumentException ex) {
            logger.info("\"" + remoteAddress + "\" returned by ServletRequest.getRemoteAddress(), but exception thrown by IpAddress.valueOf(): " + ex);
            // If we can't obtain or parse an IP address, we can't make any decisions based on it - 
            // so we are returning null here; no "fall back" addresses.
            return null;
        }
    }
    
    /**
     * Tries to obtain the "real" address of a proxied request, from the
     * X-Forwarded-for header. 
     * @return IpAddress, if available and parseable.
     * (returns null if there's no X-Forwarded-For header, or if the address 
     * is unparseable).
     */
    public IpAddress proxiedRequestIpAddress() {
        ExternalContext ctxt = FacesContext.getCurrentInstance().getExternalContext();
        
        String xff = ctxt.getRequestHeaderMap().get("X-Forwarded-For");
        if ( xff != null ) {
            xff = xff.trim();
            if ( ! xff.isEmpty() ) {
                xff = xff.split(",")[0];
                try {
                    return IpAddress.valueOf(xff); // XFF exit
                } catch (Exception ex) {
                    // xff is "::ffff:65.112.10.94" when trying to log in via http://pdurbin.pagekite.me
                    // TODO: 
                    // modify IpAddress class to recognize ipv6 addresses
                    logger.info("\"" + xff + "\" from get(\"X-Forwarded-For\") but exception thrown: " + ex + ". Returning null.");
                    return null;
                }
            }
        }
        return null;
    }
}
