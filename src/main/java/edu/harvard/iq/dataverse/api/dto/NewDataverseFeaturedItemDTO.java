package edu.harvard.iq.dataverse.api.dto;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import java.io.InputStream;

public class NewDataverseFeaturedItemDTO {
    private String content;
    private int displayOrder;
    private InputStream fileInputStream;
    private String imageFileName;

    public static NewDataverseFeaturedItemDTO fromFormData(String content,
                                                           int order,
                                                           InputStream fileInputStream,
                                                           FormDataContentDisposition contentDispositionHeader) {
        NewDataverseFeaturedItemDTO newDataverseFeaturedItemDTO = new NewDataverseFeaturedItemDTO();
        newDataverseFeaturedItemDTO.content = content;
        newDataverseFeaturedItemDTO.displayOrder = order;
        newDataverseFeaturedItemDTO.fileInputStream = fileInputStream;
        newDataverseFeaturedItemDTO.imageFileName = contentDispositionHeader.getFileName();
        return newDataverseFeaturedItemDTO;
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

    public void setFileInputStream(InputStream fileInputStream) {
        this.fileInputStream = fileInputStream;
    }

    public InputStream getFileInputStream() {
        return fileInputStream;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getImageFileName() {
        return imageFileName;
    }
}
