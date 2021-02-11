package edu.harvard.iq.dataverse.util;

import java.io.File;
import java.io.IOException;

public class FileTypeDetection {
    // Question: why do we need this utility? - as opposed to just calling the 
    // static method in FileUtil directly? - L.A. 
    public static String determineFileType(File file, String fileName) throws IOException {
        return FileUtil.determineFileType(file, fileName);
    }

}
