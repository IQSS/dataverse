package edu.harvard.iq.dataverse.api.dto;

public class FileDTO {

    private String label;
    private String description;
    private boolean restricted;
    private DataFileDTO dataFile;
    private String termsOfUseType;
    private String licenseName;
    private String licenseUrl;
    private String accessConditions;
    private String accessConditionsCustomText;

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public DataFileDTO getDataFile() {
        return dataFile;
    }

    public String getTermsOfUseType() {
        return termsOfUseType;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public String getAccessConditions() {
        return accessConditions;
    }

    public String getAccessConditionsCustomText() {
        return accessConditionsCustomText;
    }


    public void setLabel(String label) {
        this.label = label;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public void setDataFile(DataFileDTO dataFile) {
        this.dataFile = dataFile;
    }

    public void setTermsOfUseType(String termsOfUseType) {
        this.termsOfUseType = termsOfUseType;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public void setAccessConditions(String accessConditions) {
        this.accessConditions = accessConditions;
    }

    public void setAccessConditionsCustomText(String accessConditionsCustomText) {
        this.accessConditionsCustomText = accessConditionsCustomText;
    }
}
