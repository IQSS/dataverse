package edu.harvard.iq.dataverse.notification;

public enum NotificationParameter {
    GRANTED_BY("grantedBy"),
    REJECTED_BY("rejectedBy"),
    REQUESTOR_ID("requestorId"),
    MESSAGE("message"),
    REPLY_TO("replyTo"),
    SEND_COPY("sendCopy");

    NotificationParameter(String key) {
        this.key = key;
    }

    private String key;

    public String key() {
        return key;
    }
}
