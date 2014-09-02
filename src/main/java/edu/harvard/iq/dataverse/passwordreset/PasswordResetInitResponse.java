package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.util.SystemConfig;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class PasswordResetInitResponse {

    /**
     * @todo Do we really need emailFound? Just check if passwordResetData is
     * null or not instead?
     */
    private boolean emailFound;
    private String resetUrl;
    private PasswordResetData passwordResetData;

    public PasswordResetInitResponse(boolean emailFound) {
        this.emailFound = emailFound;
    }

    public PasswordResetInitResponse(boolean emailFound, PasswordResetData passwordResetData) {
        this.emailFound = emailFound;
        this.passwordResetData = passwordResetData;
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
        this.resetUrl = "https://" + finalHostname + "/passwordreset.xhtml?token=" + passwordResetData.getToken();
    }

    public boolean isEmailFound() {
        return emailFound;
    }

    public String getResetUrl() {
        return resetUrl;
    }

    public PasswordResetData getPasswordResetData() {
        return passwordResetData;
    }

}
