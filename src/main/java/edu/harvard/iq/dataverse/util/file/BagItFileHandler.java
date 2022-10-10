package edu.harvard.iq.dataverse.util.file;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.bagit.BagValidation;
import edu.harvard.iq.dataverse.util.bagit.BagValidator;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProviderFactory;
import edu.harvard.iq.dataverse.util.bagit.data.FileUtilWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 *
 * @author adaybujeda
 */
public class BagItFileHandler {

    private static final Logger logger = Logger.getLogger(BagItFileHandler.class.getCanonicalName());

    public static final String FILE_TYPE = "application/zipped-bagit";

    private final FileUtilWrapper fileUtil;
    private final FileDataProviderFactory fileDataProviderFactory;
    private final BagValidator bagValidator;
    private final BagItFileHandlerPostProcessor postProcessor;

    public BagItFileHandler(FileUtilWrapper fileUtil, FileDataProviderFactory fileDataProviderFactory, BagValidator bagValidator, BagItFileHandlerPostProcessor postProcessor) {
        this.fileUtil = fileUtil;
        this.fileDataProviderFactory = fileDataProviderFactory;
        this.bagValidator = bagValidator;
        this.postProcessor = postProcessor;
    }

    public boolean isBagItPackage(String uploadedFilename, File file) throws IOException {
        try(FileDataProvider fileDataProvider = fileDataProviderFactory.getFileDataProvider(file)) {
            boolean isBagItPackage = bagValidator.hasBagItPackage(fileDataProvider);
            logger.fine(String.format("action=isBagItPackage uploadedFilename=%s file=%s isBagItPackage=%s", uploadedFilename, file.getName(), isBagItPackage));
            return isBagItPackage;
        }
    }

    public CreateDataFileResult handleBagItPackage(SystemConfig systemConfig, DatasetVersion datasetVersion, String uploadedFilename, File bagItPackageFile) throws IOException {
        logger.info(String.format("action=handleBagItPackage start uploadedFilename=%s file=%s", uploadedFilename, bagItPackageFile.getName()));
        try {
            List<DataFile> packageDataFiles = processBagItPackage(systemConfig, datasetVersion, uploadedFilename, bagItPackageFile);
            if(packageDataFiles.isEmpty()) {
                return CreateDataFileResult.error(uploadedFilename, FILE_TYPE, Collections.emptyList());
            }

            BagValidation bagValidation = validateBagItPackage(uploadedFilename, packageDataFiles);
            if(bagValidation.success()) {
                List<DataFile> finalItems = postProcessor.process(packageDataFiles);
                logger.info(String.format("action=handleBagItPackage result=success uploadedFilename=%s file=%s", uploadedFilename, bagItPackageFile.getName()));
                return CreateDataFileResult.success(uploadedFilename, FILE_TYPE, finalItems);
            }

            // BagIt package has errors
            // Capture errors and return to caller
            List<String> errors = bagValidation.getAllErrors();
            logger.info(String.format("action=handleBagItPackage result=errors uploadedFilename=%s file=%s errors=%s", uploadedFilename, bagItPackageFile.getName(), errors.size()));
            return CreateDataFileResult.error(uploadedFilename, FILE_TYPE, errors);

        } catch (BagItFileHandlerException e) {
            logger.severe(String.format("action=handleBagItPackage result=error uploadedFilename=%s file=%s message=%s", uploadedFilename, bagItPackageFile.getName(), e.getMessage()));
            return CreateDataFileResult.error(uploadedFilename, FILE_TYPE, Arrays.asList(e.getMessage()));
        } finally {
            fileUtil.deleteFile(bagItPackageFile.toPath());
        }
    }

    private BagValidation validateBagItPackage(String uploadedFilename, List<DataFile> packageDataFiles) throws IOException {
        try(FileDataProvider fileDataProvider = fileDataProviderFactory.getFileDataProvider(uploadedFilename, packageDataFiles)) {
            BagValidation bagValidation = bagValidator.validateChecksums(fileDataProvider);
            logger.info(String.format("action=validateBagItPackage uploadedFilename=%s bagValidation=%s", uploadedFilename, bagValidation.report()));
            return bagValidation;
        }
    }

    private List<DataFile> processBagItPackage(SystemConfig systemConfig, DatasetVersion datasetVersion, String uploadedFilename, File bagItPackageFile) throws IOException, BagItFileHandlerException {
        int numberOfFilesLimit = systemConfig.getZipUploadFilesLimit();
        Long sizeOfFilesLimit = systemConfig.getMaxFileUploadSizeForStore(datasetVersion.getDataset().getEffectiveStorageDriverId());
        DataFile.ChecksumType checksumAlgorithm = systemConfig.getFileFixityChecksumAlgorithm();

        List<DataFile> packageDataFiles = new LinkedList<>();

        try(FileDataProvider fileDataProvider = fileDataProviderFactory.getFileDataProvider(bagItPackageFile)) {
            List<Path> zipFileEntries = fileDataProvider.getFilePaths();
            if (zipFileEntries.size() > numberOfFilesLimit) {
                throw new BagItFileHandlerException(String.format("Zip file: %s exceeds the number of files limit. Total: %s limit: %s", uploadedFilename, zipFileEntries.size(), numberOfFilesLimit));
            }

            for(Path zipEntry: zipFileEntries) {
                Optional<FileDataProvider.InputStreamProvider> zipEntryStream = fileDataProvider.getInputStreamProvider(zipEntry);

                if(zipEntryStream.isEmpty()) {
                    logger.warning(String.format("action=handleBagIt result=no-input-stream file=%s zipEntry=%s", uploadedFilename, zipEntry));
                    continue;
                }

                String fileName = zipEntry.getFileName().toString();
                DataFile datafile = null;
                try {
                    File zipEntryAsFile = fileUtil.saveInputStreamInTempFile(zipEntryStream.get().getInputStream(), sizeOfFilesLimit);
                    datafile = fileUtil.createSingleDataFile(datasetVersion, zipEntryAsFile, null, fileName,
                            FileUtil.MIME_TYPE_UNDETERMINED_DEFAULT, checksumAlgorithm, null, false);
                } catch(FileExceedsMaxSizeException e) {
                    throw new BagItFileHandlerException(String.format("Zip entry: %s for file: %s exceeds the size limit", zipEntry, uploadedFilename), e);
                }

                if(datafile == null) {
                    logger.warning(String.format("action=handleBagIt result=null-datafile file=%s zipEntry=%s", uploadedFilename, zipEntry));
                    continue;
                }

                if(zipEntry.getParent() != null) {
                    // Set directory
                    datafile.getFileMetadata().setDirectoryLabel(zipEntry.getParent().toString());
                }

                try {
                    String tempFileLocation = fileUtil.getFilesTempDirectory() + "/" + datafile.getStorageIdentifier();
                    String contentType = fileUtil.determineFileType(new File(tempFileLocation), fileName);
                    logger.fine(String.format("action=handleBagIt contentType=%s file=%s zipEntry=%s", contentType, uploadedFilename, zipEntry));
                    if (StringUtil.nonEmpty(contentType)) {
                        datafile.setContentType(contentType);
                    }
                } catch (Exception e) {
                    logger.warning(String.format("action=handleBagIt message=unable-to-get-content-type file=%s zipEntry=%s error=%s", uploadedFilename, zipEntry, e.getMessage()));
                }

                packageDataFiles.add(datafile);
            }
        }

        return packageDataFiles;
    }

    private static class BagItFileHandlerException extends Exception {
        public BagItFileHandlerException(String message) {
            super(message);
        }
        public BagItFileHandlerException(String message, Throwable e) {
            super(message, e);
        }
    }
}
