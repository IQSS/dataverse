package edu.harvard.iq.dataverse.util.bagit.data;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.util.file.FileExceedsStorageQuotaException;
import edu.harvard.iq.dataverse.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Wrapper around static methods to facilitate testing
 *
 * @author adaybujeda
 */
public class FileUtilWrapper {

    private static final Logger logger = Logger.getLogger(FileUtilWrapper.class.getCanonicalName());

    public String getFilesTempDirectory() {
        return FileUtil.getFilesTempDirectory();
    }

    public InputStream newInputStream(Path path) throws IOException {
       return Files.newInputStream(path);
    }

    public Stream<Path> list(Path path) throws IOException {
        return Files.list(path);
    }

    public void deleteFile(Path filePath) {
        try {
            Files.delete(filePath);
        } catch (Exception e) {
            logger.warning(String.format("action=deleteFile result=error filePath=%s message=%s", filePath, e.getMessage()));
        }
    }

    public File saveInputStreamInTempFile(InputStream inputStream, Long fileSizeLimit) throws IOException, FileExceedsMaxSizeException {
        try {
            return FileUtil.saveInputStreamInTempFile(inputStream, fileSizeLimit);
        } catch (FileExceedsStorageQuotaException fesqx) {
            return null; 
        } 
    }

    public String determineFileType(File file, String fileName) throws IOException {
        return FileUtil.determineFileType(file, fileName);
    }

    public DataFile createSingleDataFile(DatasetVersion datasetVersion, File file, String storageIdentifier, String fileName, String contentType, DataFile.ChecksumType checksumType, String checksum, Boolean addToDataset) {
        return FileUtil.createSingleDataFile(datasetVersion, file, storageIdentifier, fileName, contentType, checksumType, checksum, addToDataset);
    }
}
