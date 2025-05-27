package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
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

    public void setType(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
    }
    public void setDvObject(DvObject dvObject) {
        this.dvObject = dvObject;
    }
    public DvObject getDvObject() {
        return dvObject;
    }
}
