package edu.harvard.iq.dataverse.confirmemail;

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

    public ConfirmEmailInitResponse(boolean emailFound, ConfirmEmailData confirmEmailData, String confirmUrl) {
        this.emailFound = emailFound;
        this.confirmEmailData = confirmEmailData;
        this.confirmUrl = confirmUrl;
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
