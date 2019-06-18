package edu.harvard.iq.dataverse.license;

import java.io.Serializable;

/**
 * Terms of use used to populate values of
 * {@link FileTermsOfUse} in forms
 *
 * @author madryk
 */
public class TermsOfUseForm implements Serializable {

    private String typeWithLicenseId;
    private String restrictType;
    private String customRestrictText;

    // -------------------- GETTERS --------------------

    /**
     * Returns terms of use type joined with license id
     * (corresponds to joined value of {@link FileTermsOfUse#getTermsOfUseType()}
     * and license id)<br/>
     * For example string 'LICENSE_BASED:2' if corresponding
     * {@link FileTermsOfUse} is license based and have assigned
     * license with id 2)
     */
    public String getTypeWithLicenseId() {
        return typeWithLicenseId;
    }

    /**
     * Returns restrict type (corresponds to {@link FileTermsOfUse#getRestrictType()}
     */
    public String getRestrictType() {
        return restrictType;
    }

    /**
     * Returns custom restrict text if
     * (corresponds to {@link FileTermsOfUse#getRestrictCustomText()})
     */
    public String getCustomRestrictText() {
        return customRestrictText;
    }

    // -------------------- SETTERS --------------------

    public void setTypeWithLicenseId(String typeWithLicenseId) {
        this.typeWithLicenseId = typeWithLicenseId;
    }

    public void setRestrictType(String restrictType) {
        this.restrictType = restrictType;
    }

    public void setCustomRestrictText(String customRestrictText) {
        this.customRestrictText = customRestrictText;
    }
}
