package edu.harvard.iq.dataverse.confirmemail;

/**
 *
 * @author bsilverstein
 */
public class ConfirmEmailExecResponse {

    private String tokenQueried;
    private ConfirmEmailData confirmEmailData;

    public ConfirmEmailExecResponse(String tokenQueried, ConfirmEmailData confirmEmailData) {
        this.tokenQueried = tokenQueried;
        this.confirmEmailData = confirmEmailData;
    }

    public String getTokenQueried() {
        return tokenQueried;
    }

    public ConfirmEmailData getConfirmEmailData() {
        return confirmEmailData;
    }

    public void setConfirmEmailData(ConfirmEmailData confirmEmailData) {
        this.confirmEmailData = confirmEmailData;
    }

}
