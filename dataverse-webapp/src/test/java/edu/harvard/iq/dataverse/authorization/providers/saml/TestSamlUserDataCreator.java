package edu.harvard.iq.dataverse.authorization.providers.saml;

public class TestSamlUserDataCreator {

    public static SamlUserData create(String name, String surname, String email) {
        SamlUserData data = new SamlUserData();
        data.setName(name);
        data.setSurname(surname);
        data.setEmail(email);
        return data;
    }
}
