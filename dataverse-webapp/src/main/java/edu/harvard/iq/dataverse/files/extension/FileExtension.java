package edu.harvard.iq.dataverse.files.extension;

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
