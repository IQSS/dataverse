package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.DvObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import java.io.InputStream;

public class UpdatedDataverseFeaturedItemDTO extends AbstractDataverseFeaturedItemDTO {
    private boolean keepFile;

    public static UpdatedDataverseFeaturedItemDTO fromFormData(String content,
                                                               int displayOrder,
                                                               boolean keepFile,
                                                               InputStream imageFileInputStream,
                                                               FormDataContentDisposition contentDispositionHeader,
                                                               String type,
                                                               DvObject dvObject) {
        UpdatedDataverseFeaturedItemDTO updatedDataverseFeaturedItemDTO = new UpdatedDataverseFeaturedItemDTO();

        updatedDataverseFeaturedItemDTO.content = content;
        updatedDataverseFeaturedItemDTO.displayOrder = displayOrder;
        updatedDataverseFeaturedItemDTO.keepFile = keepFile;
        updatedDataverseFeaturedItemDTO.type = type;
        updatedDataverseFeaturedItemDTO.dvObject = dvObject;

        if (imageFileInputStream != null) {
            updatedDataverseFeaturedItemDTO.imageFileInputStream = imageFileInputStream;
            updatedDataverseFeaturedItemDTO.imageFileName = FileUtil.decodeFileName(contentDispositionHeader.getFileName());
        }

        return updatedDataverseFeaturedItemDTO;
    }

    public void setKeepFile(boolean keepFile) {
        this.keepFile = keepFile;
    }

    public boolean isKeepFile() {
        return keepFile;
    }
}
