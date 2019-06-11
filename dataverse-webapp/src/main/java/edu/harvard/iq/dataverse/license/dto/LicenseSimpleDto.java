package edu.harvard.iq.dataverse.license.dto;

/**
 * Helper class used for reordering page.
 */
public class LicenseSimpleDto {

    private Long licenseId;

    private String localizedText;

    // -------------------- CONSTRUCTORS --------------------

    public LicenseSimpleDto(Long licenseId, String text) {
        this.licenseId = licenseId;
        this.localizedText = text;
    }

    // -------------------- GETTERS --------------------

    /**
     * ID of the owner's license.
     *
     * @return licenseId
     */
    public Long getLicenseId() {
        return licenseId;
    }

    /**
     * License text that is already localized.
     *
     * @return localizedText
     */
    public String getLocalizedText() {
        return localizedText;
    }
}
