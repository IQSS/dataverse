package edu.harvard.iq.dataverse.dataset;

public class DatasetThumbnail {

    private String filename;
    private String base64image;

    public DatasetThumbnail(String filename, String base64image) {
        this.filename = filename;
        this.base64image = base64image;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getBase64image() {
        return base64image;
    }

    public void setBase64image(String base64image) {
        this.base64image = base64image;
    }

}
