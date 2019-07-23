package edu.harvard.iq.dataverse.files.mime;

public enum ImageMimeType {

    FITSIMAGE("image/fits");

    private String mimeValue;

    ImageMimeType(String mimeType) {
        this.mimeValue = mimeType;
    }

    public String getMimeValue() {
        return mimeValue;
    }
}
