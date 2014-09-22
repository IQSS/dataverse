package edu.harvard.iq.dataverse.passwordreset;

public class PasswordChangeAttemptResponse {

    private final boolean changed;
    private final String messageSummary;
    private final String messageDetail;

    public PasswordChangeAttemptResponse(boolean changed, String messageSummary, String messageDetail) {
        this.changed = changed;
        this.messageSummary = messageSummary;
        this.messageDetail = messageDetail;
    }

    public boolean isChanged() {
        return changed;
    }

    public String getMessageSummary() {
        return messageSummary;
    }

    public String getMessageDetail() {
        return messageDetail;
    }

}
