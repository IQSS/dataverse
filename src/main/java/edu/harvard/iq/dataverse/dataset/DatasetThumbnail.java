package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;

public class DatasetThumbnail {

    private String base64image;
    private final DataFile dataFile;
    /** Whether the base64 image has been lazily loaded yet. */
    private boolean base64Loaded;

    /**
     * Constructor with pre-computed base64 image (eager mode).
     */
    public DatasetThumbnail(String base64image, DataFile dataFile) {
        this.base64image = base64image;
        this.dataFile = dataFile;
        this.base64Loaded = base64image != null;
    }

    /**
     * Constructor with only DataFile reference (lazy mode).
     * Base64 image will be generated on first access via getBase64image().
     */
    public DatasetThumbnail(DataFile dataFile) {
        this.base64image = null;
        this.dataFile = dataFile;
        this.base64Loaded = false;
    }

    public String getBase64image() {
        if (!base64Loaded && dataFile != null) {
            base64image = ImageThumbConverter.getImageThumbnailAsBase64(dataFile, ImageThumbConverter.DEFAULT_DATASETLOGO_SIZE);
            base64Loaded = true;
        }
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

    public String getFilename() {
        if (dataFile != null) {
            return dataFile.getDisplayName();
        } else {
            return null;
        }
    }
}
