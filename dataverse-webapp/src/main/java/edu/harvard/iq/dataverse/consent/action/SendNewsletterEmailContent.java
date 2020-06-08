package edu.harvard.iq.dataverse.consent.action;

public class SendNewsletterEmailContent {

    private String senderEmail;
    private String recipientFirstName;
    private String recipientLastName;
    private String recipientEmail;


    // -------------------- CONSTRUCTORS --------------------
    @Deprecated /* Only used for Jackson parser */
    public SendNewsletterEmailContent() {
    }

    public SendNewsletterEmailContent(String senderEmail, String recipientEmail, String recipientFirstName, String recipientLastName) {
        this.recipientFirstName = recipientFirstName;
        this.recipientLastName = recipientLastName;
        this.senderEmail = senderEmail;
        this.recipientEmail = recipientEmail;
    }

    // -------------------- GETTERS --------------------

    public String getRecipientFirstName() {
        return recipientFirstName;
    }

    public String getRecipientLastName() {
        return recipientLastName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }
}
