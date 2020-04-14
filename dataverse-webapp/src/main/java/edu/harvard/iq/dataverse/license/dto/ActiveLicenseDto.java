package edu.harvard.iq.dataverse.license.dto;

/**
 * Helper class used for listing active licenses in @link /api/info/activeLicenses endpoint.
 */
public class ActiveLicenseDto {
    private String name;

    // -------------------- CONSTRUCTORS --------------------
    public ActiveLicenseDto(String name) {
        this.name = name;
    }

    // -------------------- toString --------------------
    @Override
    public String toString() {
        return "'license' : '" + name + "'";
    }
}
