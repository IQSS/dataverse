package edu.harvard.iq.dataverse.util;

import java.io.File;
import java.io.IOException;

public class FileTypeDetection {

    public static String determineFileType(File file) throws IOException {
        return FileUtil.determineFileType(file, file.getName());
    }

}
