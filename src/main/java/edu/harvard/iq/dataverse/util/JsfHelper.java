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
     * Finds the request IP address. Honors X-Forwarded-for headers.
     * @return the IP Address of the client issuing the request.
     */
    public IpAddress requestClientIpAddress() {
        ExternalContext ctxt = FacesContext.getCurrentInstance().getExternalContext();
        String xff = ctxt.getRequestHeaderMap().get("X-Forwarded-For");
        if ( xff != null ) {
            xff = xff.trim();
            if ( ! xff.isEmpty() ) {
                xff = xff.split(",")[0];
                return IpAddress.valueOf(xff); // XFF exit
            }
        }
        ServletRequest sr = (ServletRequest) ctxt.getRequest();
        try {
            return IpAddress.valueOf(sr.getRemoteHost());
        } catch (IllegalArgumentException ex) {
            // getRemoteHost is "0:0:0:0:0:0:0:1" from Harvard wireless
            String localhostIp = "127.0.0.1";
            /**
             * @todo should we use some other value besides localhost?
             */
            logger.info("\"" + sr.getRemoteHost() + "\" from ServletRequest.getRemoteHost() passed but exception thrown: " + ex + ". Using " + localhostIp + " instead.");
            return IpAddress.valueOf(localhostIp);
        }
    }
}
