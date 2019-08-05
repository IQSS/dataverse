package edu.harvard.iq.dataverse.common.files.mime;

public enum ShapefileMimeType {
    
    SHAPEFILE_FILE_TYPE("application/zipped-shapefile", "Shapefile as ZIP Archive");

    private String mimeValue;
    private String friendlyName;

    ShapefileMimeType(String mimeType, String friendlyName) {
        this.mimeValue = mimeType;
        this.friendlyName = friendlyName;
    }

    public String getMimeValue() {
        return mimeValue;
    }

    public String getFriendlyName() {
        return friendlyName;
    }
}
