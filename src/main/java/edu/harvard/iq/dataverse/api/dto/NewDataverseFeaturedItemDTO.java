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

    public String getContent() {
        return content;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public InputStream getFileInputStream() {
        return fileInputStream;
    }

    public String getImageFileName() {
        return imageFileName;
    }
}
