package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataFile;

public class DatasetThumbnail {

    private final String filename;
    private final String base64image;
    private final DataFile dataFile;

    public DatasetThumbnail(String filename, String base64image, DataFile dataFile) {
        this.filename = filename;
        this.base64image = base64image;
        this.dataFile = dataFile;
    }

    public String getFilename() {
        return filename;
    }

    public String getBase64image() {
        return base64image;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public boolean isFromDataFile() {
        if (dataFile != null) {
            return true;
        } else {
            return false;
        }
    }

}
