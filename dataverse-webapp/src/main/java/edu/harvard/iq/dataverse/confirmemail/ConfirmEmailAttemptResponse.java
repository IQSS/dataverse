package edu.harvard.iq.dataverse.confirmemail;

/**
 *
 * @author bsilverstein
 */
public class ConfirmEmailAttemptResponse {

    private final boolean confirmed;
    private final String messageSummary;
    private final String messageDetail;

    public ConfirmEmailAttemptResponse(boolean confirmed, String messageSummary, String messageDetail) {
        this.confirmed = confirmed;
        this.messageSummary = messageSummary;
        this.messageDetail = messageDetail;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getMessageSummary() {
        return messageSummary;
    }

    public String getMessageDetail() {
        return messageDetail;
    }

}
