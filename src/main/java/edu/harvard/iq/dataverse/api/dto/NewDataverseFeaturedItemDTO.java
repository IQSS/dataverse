package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.DvObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import java.io.InputStream;

public class NewDataverseFeaturedItemDTO extends AbstractDataverseFeaturedItemDTO {

    public static NewDataverseFeaturedItemDTO fromFormData(String content,
                                                           int displayOrder,
                                                           InputStream imageFileInputStream,
                                                           FormDataContentDisposition contentDispositionHeader,
                                                           String type,
                                                           DvObject dvObject) throws IllegalArgumentException {
        NewDataverseFeaturedItemDTO newDataverseFeaturedItemDTO = new NewDataverseFeaturedItemDTO();

        newDataverseFeaturedItemDTO.content = content;
        newDataverseFeaturedItemDTO.displayOrder = displayOrder;
        newDataverseFeaturedItemDTO.type = type;
        newDataverseFeaturedItemDTO.dvObject = dvObject;

        if (imageFileInputStream != null) {
            newDataverseFeaturedItemDTO.imageFileInputStream = imageFileInputStream;
            newDataverseFeaturedItemDTO.imageFileName = FileUtil.decodeFileName(contentDispositionHeader.getFileName());
        }

        return newDataverseFeaturedItemDTO;
    }
}
