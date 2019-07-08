package edu.harvard.iq.dataverse.api.dto;

public class FileDTO {

    String label;
    boolean restricted;
    DataFileDTO dataFile;

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
