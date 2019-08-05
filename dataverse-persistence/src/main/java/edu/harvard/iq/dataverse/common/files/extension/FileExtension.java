package edu.harvard.iq.dataverse.common.files.extension;

public enum FileExtension {
    SAVED_ORIGINAL_FILENAME_EXTENSION("orig");

    private String extension;

    FileExtension(String mimeType) {
        this.extension = mimeType;
    }

    public String getExtension() {
        return extension;
    }
}
