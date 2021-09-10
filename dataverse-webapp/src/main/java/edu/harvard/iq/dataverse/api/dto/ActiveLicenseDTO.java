package edu.harvard.iq.dataverse.api.dto;

/**
 * Helper class used for listing active licenses in @link /api/info/activeLicenses endpoint.
 */
public class ActiveLicenseDTO {
    private String license;

    // -------------------- CONSTRUCTORS --------------------

    public ActiveLicenseDTO(String license) {
        this.license = license;
    }

    // -------------------- GETTERS --------------------

    public String getLicense() {
        return license;
    }

    // -------------------- toString --------------------
    @Override
    public String toString() {
        return "'license' : '" + license + "'";
    }
}
