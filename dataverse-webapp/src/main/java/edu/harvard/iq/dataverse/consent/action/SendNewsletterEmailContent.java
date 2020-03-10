package edu.harvard.iq.dataverse.consent.action;

public class SendNewsletterEmailContent {

    private String firstName;
    private String lastName;
    private String email;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated /* Only used for Jackson parser */
    public SendNewsletterEmailContent() {
    }

    public SendNewsletterEmailContent(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // -------------------- GETTERS --------------------

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }
}
