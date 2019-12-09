package edu.harvard.iq.dataverse.api.dto;

public class FileDTO {

    String label;
    boolean restricted;
    DataFileDTO dataFile;
    private String termsOfUseType;
    private String licenseName;
    private String licenseUrl;

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getTermsOfUseType() {
        return termsOfUseType;
    }

    public void setTermsOfUseType(String termsOfUseType) {
        this.termsOfUseType = termsOfUseType;
    }

    public DataFileDTO getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFileDTO datafile) {
        this.dataFile = datafile;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }
}
