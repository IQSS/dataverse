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
    /**
     * @todo What should we use as a fallback IP address if IpAddress.valueOf()
     * is not able to parse it? The equivalent of "localhost" may not be a
     * secure choice! This question has been asked at
     * https://github.com/IQSS/dataverse/issues/909
     */
    private final String fallbackIp = "127.0.0.1";
	
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
                try {
                    return IpAddress.valueOf(xff); // XFF exit
                } catch (Exception ex) {
                    // xff is "::ffff:65.112.10.94" when trying to log in via http://pdurbin.pagekite.me
                    logger.info("\"" + xff + "\" from get(\"X-Forwarded-For\") but exception thrown: " + ex + ". Using fallback IP of " + fallbackIp + " instead.");
                    return IpAddress.valueOf(fallbackIp);
                }
            }
        }
        ServletRequest sr = (ServletRequest) ctxt.getRequest();
        try {
            return IpAddress.valueOf(sr.getRemoteHost());
        } catch (IllegalArgumentException ex) {
            // getRemoteHost is "0:0:0:0:0:0:0:1" from Harvard wireless
            logger.info("\"" + sr.getRemoteHost() + "\" from ServletRequest.getRemoteHost() passed but exception thrown: " + ex + ". Using fallback IP of " + fallbackIp + " instead.");
            return IpAddress.valueOf(fallbackIp);
        }
    }
}
