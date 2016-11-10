package edu.harvard.iq.dataverse.batch.jobs.importer.checksum;

public class ChecksumRecord {

    private String type;

    private String path;

    private String value;

    public void ChecksumRecord() {

    }

    public void ChecksumRecord(String type, String path, String value) {
        this.type = type;
        this.path = path;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
