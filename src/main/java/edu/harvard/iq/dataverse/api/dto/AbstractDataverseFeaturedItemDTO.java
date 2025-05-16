package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;

import java.io.InputStream;

public abstract class AbstractDataverseFeaturedItemDTO {
    protected String content;
    protected int displayOrder;
    protected InputStream imageFileInputStream;
    protected String imageFileName;
    protected String type;
    protected DvObject dvObject;

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setImageFileInputStream(InputStream imageFileInputStream) {
        this.imageFileInputStream = imageFileInputStream;
    }

    public InputStream getImageFileInputStream() {
        return imageFileInputStream;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setDvObject(String type, DvObject dvObject) {
        DataverseFeaturedItem.DvObjectFeaturedItem item = DataverseFeaturedItem.sanitizeDvObject(type, dvObject);
        this.type = item.type;
        this.dvObject = item.dvObject;
    }

    public DvObject getDvObject() {
        return dvObject;
    }
    public String getType() {
        return type;
    }
}
