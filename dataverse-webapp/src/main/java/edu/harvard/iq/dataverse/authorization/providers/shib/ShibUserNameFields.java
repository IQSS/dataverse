package edu.harvard.iq.dataverse.authorization.providers.shib;

public class ShibUserNameFields {

    String firstName;
    String lastName;

    public ShibUserNameFields(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public String toString() {
        return "ShibUserNameFields{" + "firstName=" + firstName + ", lastName=" + lastName + '}';
    }
}
