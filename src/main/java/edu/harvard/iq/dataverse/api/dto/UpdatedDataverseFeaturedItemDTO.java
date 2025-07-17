package edu.harvard.iq.dataverse.api.dto;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import java.io.InputStream;

public class UpdatedDataverseFeaturedItemDTO {
    private String content;
    private int displayOrder;
    private boolean keepFile;
    private InputStream imageFileInputStream;
    private String imageFileName;

    public static UpdatedDataverseFeaturedItemDTO fromFormData(String content,
                                                               int displayOrder,
                                                               boolean keepFile,
                                                               InputStream imageFileInputStream,
                                                               FormDataContentDisposition contentDispositionHeader) {
        UpdatedDataverseFeaturedItemDTO updatedDataverseFeaturedItemDTO = new UpdatedDataverseFeaturedItemDTO();

        updatedDataverseFeaturedItemDTO.content = content;
        updatedDataverseFeaturedItemDTO.displayOrder = displayOrder;
        updatedDataverseFeaturedItemDTO.keepFile = keepFile;

        if (imageFileInputStream != null) {
            updatedDataverseFeaturedItemDTO.imageFileInputStream = imageFileInputStream;
            updatedDataverseFeaturedItemDTO.imageFileName = contentDispositionHeader.getFileName();
        }

        return updatedDataverseFeaturedItemDTO;
    }

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

    public void setKeepFile(boolean keepFile) {
        this.keepFile = keepFile;
    }

    public boolean isKeepFile() {
        return keepFile;
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
}
