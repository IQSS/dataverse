package edu.harvard.iq.dataverse.mail;

public class EmailContent {

    private String subject;
    private String messageText;
    private String footer;

    // -------------------- CONSTRUCTORS --------------------

    public EmailContent(String subject, String messageText, String footer) {
        this.subject = subject;
        this.messageText = messageText;
        this.footer = footer;
    }

    // -------------------- GETTERS --------------------

    public String getSubject() {
        return subject;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getFooter() {
        return footer;
    }
}
