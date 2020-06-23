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
    private String resetUrlPart;
    private PasswordResetData passwordResetData;

    public PasswordResetInitResponse(boolean emailFound) {
        this.emailFound = emailFound;
    }

    public PasswordResetInitResponse(boolean emailFound, PasswordResetData passwordResetData) {
        this.emailFound = emailFound;
        this.passwordResetData = passwordResetData;
        this.resetUrlPart = "/passwordreset.xhtml?token=" + passwordResetData.getToken();
    }

    public boolean isEmailFound() {
        return emailFound;
    }

    public String getResetUrl(String prefix) {
        return prefix + resetUrlPart;
    }

    public PasswordResetData getPasswordResetData() {
        return passwordResetData;
    }

}
