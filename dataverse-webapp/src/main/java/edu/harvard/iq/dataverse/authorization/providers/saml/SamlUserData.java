package edu.harvard.iq.dataverse.authorization.providers.saml;

import edu.harvard.iq.dataverse.authorization.common.ExternalIdpUserRecord;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class SamlUserData implements Serializable {
    private String id;
    private String idpEntityId;
    private String name;
    private String surname;
    private String email;

    private Map<String, List<String>> rawData;

    // -------------------- CONSTRUCTORS --------------------

    SamlUserData() { }

    public SamlUserData(String id, String idpEntityId) {
        this.id = id;
        this.idpEntityId = idpEntityId;
    }

    // -------------------- GETTERS --------------------

    public String getId() {
        return id;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }

    public String getCompositeId() {
        return idpEntityId + "|" + id;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public Map<String, List<String>> getRawData() {
        return rawData;
    }

    // -------------------- LOGIC --------------------

    public ExternalIdpUserRecord toExternalIdpUserRecord() {
        return new ExternalIdpUserRecord(SamlAuthenticationProvider.PROVIDER_ID, getCompositeId(), null,
                new AuthenticatedUserDisplayInfo(getName(), getSurname(), getEmail(), null, null));
    }

    public boolean isCompleteForLogin() {
        return StringUtils.isNoneBlank(name, surname, email);
    }

    public String printLoginData() {
        UnaryOperator<String> substituteIfEmpty = s -> StringUtils.isBlank(s) ? "?" : s;
        return String.format("Name: %s,\n Surname: %s,\n E-mail: %s.",
                substituteIfEmpty.apply(name), substituteIfEmpty.apply(surname), substituteIfEmpty.apply(email));
    }

    // -------------------- SETTERS --------------------

    void setId(String id) {
        this.id = id;
    }

    void setIdpEntityId(String idpEntityId) {
        this.idpEntityId = idpEntityId;
    }

    void setName(String name) {
        this.name = name;
    }

    void setSurname(String surname) {
        this.surname = surname;
    }

    void setEmail(String email) {
        this.email = email;
    }

    void setRawData(Map<String, List<String>> rawData) {
        this.rawData = rawData;
    }
}
