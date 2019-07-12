package edu.harvard.iq.dataverse.license.dto;

import java.util.Objects;

import edu.harvard.iq.dataverse.license.FileTermsOfUse.RestrictType;

public class RestrictedTermsOfUseDTO {

    private RestrictType restrictType;

    private String restrictCustomText;


    // -------------------- CONSTRUCTORS --------------------

    public RestrictedTermsOfUseDTO(RestrictType restrictType, String restrictCustomText) {
        this.restrictType = Objects.requireNonNull(restrictType);
        this.restrictCustomText = restrictCustomText;
    }

    // -------------------- GETTERS --------------------

    public RestrictType getRestrictType() {
        return restrictType;
    }

    public String getRestrictCustomText() {
        return restrictCustomText;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
        return Objects.hash(restrictCustomText, restrictType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RestrictedTermsOfUseDTO other = (RestrictedTermsOfUseDTO) obj;
        return Objects.equals(restrictCustomText, other.restrictCustomText) && restrictType == other.restrictType;
    }
}
