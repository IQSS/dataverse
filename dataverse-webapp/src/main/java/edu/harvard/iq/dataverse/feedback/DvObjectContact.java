package edu.harvard.iq.dataverse.feedback;

public class DvObjectContact {

    private final String name;
    private final String email;

    public DvObjectContact(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

}
