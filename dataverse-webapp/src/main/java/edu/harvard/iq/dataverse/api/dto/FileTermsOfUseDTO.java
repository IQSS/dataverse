package edu.harvard.iq.dataverse.api.dto;

import java.io.Serializable;

public class FileTermsOfUseDTO implements Serializable {

    private final String termsType;
    private final String license;
    private final String accessConditions;
    private final String accessConditionsCustomText;

    // -------------------- CONSTRUCTORS --------------------
    public FileTermsOfUseDTO(String termsType, String license, String accessConditions, String accessConditionsCustomText) {
        this.termsType = termsType;
        this.license = license;
        this.accessConditions = accessConditions;
        this.accessConditionsCustomText = accessConditionsCustomText;
    }

    // -------------------- GETTERS --------------------
    public String getTermsType() {
        return termsType;
    }

    public String getLicense() {
        return license;
    }

    public String getAccessConditions() {
        return accessConditions;
    }

    public String getAccessConditionsCustomText() {
        return accessConditionsCustomText;
    }
}
