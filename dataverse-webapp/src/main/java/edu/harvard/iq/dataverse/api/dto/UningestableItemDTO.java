package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.util.FileUtil;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

public class UningestableItemDTO implements Serializable {
    private Long dataFileId;
    private String fileName;
    private String originalFormat;
    private String md5;
    private String unf;

    // -------------------- GETTERS --------------------


    public Long getDataFileId() {
        return dataFileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOriginalFormat() {
        return originalFormat;
    }

    public String getMd5() {
        return md5;
    }

    public String getUnf() {
        return unf;
    }

    // -------------------- LOGIC --------------------

    public static UningestableItemDTO fromDatafile(DataFile file) {
        UningestableItemDTO item = new UningestableItemDTO();
        item.dataFileId = file.getId();
        item.fileName = file.getFileMetadata().getLabel();
        item.originalFormat = extractAndFormatExtension(file);
        item.md5 = file.getChecksumType() == DataFile.ChecksumType.MD5
                ? file.getChecksumValue() : StringUtils.EMPTY;
        item.unf = file.getUnf();
        return item;
    }

    // -------------------- PRIVATE --------------------

    private static String extractAndFormatExtension(DataFile file) {
        String extension = FileUtil.generateOriginalExtension(file.isTabularData()
                ? file.getDataTable().getOriginalFileFormat()
                : file.getContentType());
        return extension.replaceFirst("\\.", StringUtils.EMPTY).toUpperCase();
    }
}
