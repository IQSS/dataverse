package edu.harvard.iq.dataverse.common;

import edu.harvard.iq.dataverse.common.files.mime.ShapefileMimeType;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class FriendlyFileTypeUtil {

    public static String getUserFriendlyFileType(DataFile dataFile) {
        String fileType = dataFile.getContentType();

        if (fileType.equalsIgnoreCase(ShapefileMimeType.SHAPEFILE_FILE_TYPE.getMimeValue())) {
            return ShapefileMimeType.SHAPEFILE_FILE_TYPE.getFriendlyName();
        }
        if (fileType.contains(";")) {
            fileType = fileType.substring(0, fileType.indexOf(";"));
        }

        return Optional.ofNullable(BundleUtil.getStringFromPropertyFile(fileType, "MimeTypeDisplay"))
                .filter(bundleName -> !bundleName.isEmpty())
                .orElse(BundleUtil.getStringFromPropertyFile("application/octet-stream", "MimeTypeDisplay"));
    }
    
    
    public static String getUserFriendlyOriginalType(DataFile dataFile) {
        if (!dataFile.isTabularData()) {
            return null;
        }

        String fileType = dataFile.getOriginalFileFormat();

        if (StringUtils.isNotEmpty(fileType)) {
            if (fileType.contains(";")) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }

            return Optional.ofNullable(BundleUtil.getStringFromPropertyFile(fileType, "MimeTypeDisplay"))
                    .filter(bundleName -> !bundleName.isEmpty())
                    .orElse(fileType);
        }

        return "UNKNOWN";
    }
}
