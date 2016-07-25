package edu.harvard.iq.dataverse.confirmemail;

import edu.harvard.iq.dataverse.util.SystemConfig;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author bsilverstein
 */
public class ConfirmEmailInitResponse {

    private boolean emailFound;
    private String confirmUrl;
    private ConfirmEmailData confirmEmailData;

    public ConfirmEmailInitResponse(boolean emailFound) {
        this.emailFound = emailFound;
    }

    public ConfirmEmailInitResponse(boolean emailFound, ConfirmEmailData confirmEmailData) {
        this.emailFound = emailFound;
        this.confirmEmailData = confirmEmailData;
        // default to localhost
        String finalHostname = "localhost";
        String configuredHostname = System.getProperty(SystemConfig.FQDN);
        if (configuredHostname != null) {
            if (configuredHostname.equals("localhost")) {
                // must be a dev environment
                finalHostname = "localhost:8181";
            } else {
                finalHostname = configuredHostname;
            }
        } else {
            try {
                finalHostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ex) {
                // just use the dev address
            }
        }
        this.confirmUrl = "https://" + finalHostname + "/confirmemail.xhtml?token=" + confirmEmailData.getToken();
    }

    public boolean isEmailFound() {
        return emailFound;
    }

    public String getConfirmUrl() {
        return confirmUrl;
    }

    public ConfirmEmailData getConfirmEmailData() {
        return confirmEmailData;
    }
}
