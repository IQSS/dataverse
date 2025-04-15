package edu.harvard.iq.dataverse.api.dto;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class NewDataverseFeaturedItemDTO {
    private String content;
    private int displayOrder;
    private InputStream imageFileInputStream;
    private String imageFileName;

    public static NewDataverseFeaturedItemDTO fromFormData(String content,
                                                           int displayOrder,
                                                           InputStream imageFileInputStream,
                                                           FormDataContentDisposition contentDispositionHeader) {
        NewDataverseFeaturedItemDTO newDataverseFeaturedItemDTO = new NewDataverseFeaturedItemDTO();

        newDataverseFeaturedItemDTO.content = content;
        newDataverseFeaturedItemDTO.displayOrder = displayOrder;

        if (imageFileInputStream != null) {
            newDataverseFeaturedItemDTO.imageFileInputStream = imageFileInputStream;
            // https://github.com/eclipse-ee4j/jersey/issues/1700
            newDataverseFeaturedItemDTO.imageFileName = new String(contentDispositionHeader.getFileName().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        }

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
