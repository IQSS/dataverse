package edu.harvard.iq.dataverse.datafile;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.common.files.mime.ApplicationMimeType;
import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.datasetutility.VirusFoundException;
import edu.harvard.iq.dataverse.license.TermsOfUseFactory;
import edu.harvard.iq.dataverse.license.TermsOfUseFormMapper;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestError;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestException;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import io.vavr.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestReport.createIngestFailureReport;
import static edu.harvard.iq.dataverse.util.FileUtil.calculateChecksum;
import static edu.harvard.iq.dataverse.util.FileUtil.canIngestAsTabular;
import static edu.harvard.iq.dataverse.util.FileUtil.getFilesTempDirectory;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class DataFileCreator {

    private static final Logger logger = LoggerFactory.getLogger(DataFileCreator.class);

    @Inject
    private SettingsServiceBean settingsService;
    @Inject
    private AntivirFileScanner antivirFileScanner;
    @Inject
    private ArchiveUncompressedSizeCalculator uncompressedCalculator;
    @Inject
    private FileTypeDetector fileTypeDetector;
    @Inject
    private TermsOfUseFactory termsOfUseFactory;
    @Inject
    private TermsOfUseFormMapper termsOfUseFormMapper;


    public List<DataFile> createDataFiles(InputStream inputStream, String fileName, String suppliedContentType) throws IOException {
        Path tempFile = null;
        try {
            Long fileSizeLimit = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaxFileUploadSizeInBytes);
            // save the file, in the temporary location for now:
            tempFile = FileUtil.limitedInputStreamToTempFile(inputStream, fileSizeLimit);
            return createDataFiles(tempFile, fileName, suppliedContentType, fileSizeLimit);
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    // -------------------- PRIVATE --------------------

    private List<DataFile> createDataFiles(Path tempFile, String fileName, String suppliedContentType, Long fileSizeLimit) throws IOException {

        logger.info("mime type supplied: " + suppliedContentType);

        String recognizedType = fileTypeDetector.determineFileType(tempFile.toFile(), fileName);
        logger.info("File utility recognized the file as " + recognizedType);

        String finalType = isDetectedContentTypeBetterThanSupplied(suppliedContentType, recognizedType) ?
                recognizedType : suppliedContentType;

        if (settingsService.isTrueForKey(SettingsServiceBean.Key.AntivirusScannerEnabled)
                && !antivirFileScanner.isFileOverSizeLimit(tempFile.toFile(), recognizedType)) {
            Stopwatch watch = new Stopwatch();
            watch.start();
            AntivirScannerResponse scannerResponse = antivirFileScanner.scan(tempFile);
            watch.stop();
            logger.info("Antivirus scanning took: " + watch.elapsedMillis());
            if (scannerResponse.isFileInfected()) {
                logger.warn("There was an attempt to upload file infected with virus. Scanner message: {}", scannerResponse.getMessage());
                throw new VirusFoundException(scannerResponse.getMessage());
            }
        }


        IngestError errorKey = null;
        long zipFileUnpackFilesLimit = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.ZipUploadFilesLimit);

        if (finalType.equals("application/fits-gzipped")) {
            // if this is a gzipped FITS file, we'll uncompress it, and ingest it as
            // a regular FITS file:
            try {
                return Lists.newArrayList(unpackFitsGzippedAndCreateDataFile(tempFile, fileName, fileSizeLimit));
            } catch (IOException | FileExceedsMaxSizeException ioex) {
                logger.warn("Failed to unpack fits-gzipped file. Saving the file as is.", ioex);
            }
        } else if (finalType.equals("application/zip") && zipFileUnpackFilesLimit > 0) {
            // If it's a ZIP file, we are going to unpack it and create multiple
            // DataFile objects from its contents:
            try {
                List<DataFile> datafiles = unpackZipAndCreateDataFiles(tempFile, fileSizeLimit, zipFileUnpackFilesLimit);
                if (!datafiles.isEmpty()) {
                    return datafiles;
                }
            } catch(IllegalArgumentException iaex) {
                // Note:
                // ZipInputStream documentation doesn't even mention that
                // getNextEntry() throws an IllegalArgumentException!
                // but that's what happens if the file name of the next
                // entry is not valid in the current CharSet.
                //      -- L.A.
                logger.warn("Failed to unpack Zip file. (Unknown Character Set used in a file name?) Saving the file as is.", iaex);
                errorKey = IngestError.UNZIP_FAIL;
            } catch (IOException ioex) {
                logger.warn("Failed to unzip the file. Saving the file as is.", ioex);
                errorKey = IngestError.UNZIP_FAIL;

            } catch (FileExceedsMaxSizeException femsx) {
                logger.warn("One of the unzipped files exceeds the size limit resorting to saving the file as is.", femsx);
                errorKey = IngestError.UNZIP_SIZE_FAIL;
            } catch (IngestException ie) {
                errorKey = ie.getErrorKey();
            }

        } else if (finalType.equals(ShapefileHandler.SHAPEFILE_FILE_TYPE)) {
            try {
                return createDataFilesFromReshapedShapeFile(tempFile, fileSizeLimit);
            } catch (FileExceedsMaxSizeException femsx) {
                logger.error("One of the unzipped shape files exceeded the size limit; giving up. " + femsx.getMessage());
                throw new IOException("One of the unzipped shape files exceeded the size limit", femsx);
            }
        }

        Long uncompressedSize = uncompressedCalculator.calculateUncompressedSize(tempFile, finalType, fileName);

        // Finally, if none of the special cases above were applicable (or
        // if we were unable to unpack an uploaded file, etc.), we'll just
        // create and return a single DataFile:

        DataFile datafile = createSingleDataFile(tempFile, fileName, finalType, uncompressedSize);

        if (errorKey != null) {

            if (errorKey.equals(IngestError.UNZIP_FILE_LIMIT_FAIL) && fileSizeLimit != null) {
                datafile.setIngestReport(createIngestFailureReport(datafile, errorKey, fileSizeLimit.toString()));
            } else {
                datafile.setIngestReport(createIngestFailureReport(datafile, errorKey));
            }

            datafile.setIngestProblem();
        }

        return Lists.newArrayList(datafile);
    }

    /**
     *  Is detected it any better than the type that was supplied to us,
     *  if any?
     *  This is not as trivial a task as one might expect...
     *  We may need a list of "good" mime types, that should always
     *  be chosen over other choices available. Maybe it should
     *  even be a weighed list... as in, "application/foo" should
     *  be chosen over "application/foo-with-bells-and-whistles".
     *
     *  For now the logic will be as follows:
     *  1. If the contentType supplied (by the browser, most likely)
     *  is some form of "unknown", we always discard it in favor of
     *  whatever our own utilities have determined;
     *  2. We should NEVER trust the browser when it comes to the
     *  following "ingestable" types: Stata, SPSS, R;
     *  2a. We are willing to TRUST the browser when it comes to
     *   the CSV and XSLX ingestable types.
     *  3. We should ALWAYS trust our utilities when it comes to
     *  ingestable types.
     */
    private boolean isDetectedContentTypeBetterThanSupplied(String suppliedContentType, String recognizedType) {
        return isUndeterminedMimeType(suppliedContentType)
                || isIngestableButNotCsvOrXlsx(suppliedContentType)
                || isTrustedDetectedMimeType(recognizedType);
    }

    private boolean isUndeterminedMimeType(String mimeType) {
        return mimeType == null || mimeType.equals("")
                || mimeType.equals(ApplicationMimeType.UNDETERMINED_DEFAULT.getMimeValue())
                || mimeType.equals(ApplicationMimeType.UNDETERMINED_BINARY.getMimeValue());
    }
    private boolean isIngestableButNotCsvOrXlsx(String mimeType) {
        return canIngestAsTabular(mimeType)
                && !mimeType.equals(TextMimeType.CSV.getMimeValue())
                && !mimeType.equals(TextMimeType.CSV_ALT.getMimeValue())
                && !mimeType.equals(ApplicationMimeType.XLSX.getMimeValue());
    }
    private boolean isTrustedDetectedMimeType(String recognizedType) {
        if (canIngestAsTabular(recognizedType)
                || recognizedType.equals("application/fits-gzipped")
                || recognizedType.equals(ShapefileHandler.SHAPEFILE_FILE_TYPE)
                || recognizedType.equals(ApplicationMimeType.ZIP.getMimeValue())) {
            return true;
        }
        return false;
    }

    /**
     * Creates {@link DataFile} from uncompressed gzip file that is passed
     * a an argument
     */
    private DataFile unpackFitsGzippedAndCreateDataFile(Path tempFile, String fileName, Long fileSizeLimit) throws IOException {
        String finalFileName = fileName;
        // if the file name had the ".gz" extension, remove it,
        // since we are going to uncompress it:
        if (fileName != null && fileName.matches(".*\\.gz$")) {
            finalFileName = fileName.replaceAll("\\.gz$", "");
        }

        try (InputStream uncompressedIn = new GZIPInputStream(Files.newInputStream(tempFile))) {

            Path unZippedTempFile = FileUtil.limitedInputStreamToTempFile(uncompressedIn, fileSizeLimit);
            return createSingleDataFile(unZippedTempFile, finalFileName, ApplicationMimeType.FITS
                    .getMimeValue(), 0L);
        }
    }

    private List<DataFile> unpackZipAndCreateDataFiles(Path tempFile, Long fileSizeLimit, Long zipFileUnpackFilesLimit) throws IOException {
        List<DataFile> datafiles = new ArrayList<>();

        try (ZipInputStream unZippedIn = new ZipInputStream(Files.newInputStream(tempFile))) {

            ZipEntry zipEntry = unZippedIn.getNextEntry();
            while (zipEntry != null) {

                // Note that some zip entries may be directories - we
                // simply skip them:

                if (!zipEntry.isDirectory()) {
                    logger.info("ZipEntry, file: " + zipEntry.getName());

                    if (datafiles.size() >= zipFileUnpackFilesLimit) {
                        logger.warn("Zip upload - too many files.");
                        throw new IngestException(IngestError.UNZIP_FILE_LIMIT_FAIL);
                    }

                    Tuple2<String, String> names = extractDirectoryAndFileName(zipEntry);
                    String directoryName = names._1();
                    String shortName = names._2();


                    // Check if it's a "fake" file - a zip archive entry
                    // created for a MacOS X filesystem element: (these
                    // start with "._")
                    if (StringUtils.isNotBlank(shortName) && !shortName.startsWith("._") && !shortName.startsWith(".DS_Store")) {
                        // OK, this seems like an OK file entry - we'll try
                        // to read it and create a DataFile with it:
                        // TODO: cleanup of already unpacked files when there was some failure (for example reaching zipFileUnpackFilesLimit)
                        Path unZippedTempFile = FileUtil.limitedInputStreamToTempFile(unZippedIn, fileSizeLimit);
                        String recognizedType = fileTypeDetector.determineFileType(unZippedTempFile.toFile(), shortName);
                        logger.info("File utility recognized unzipped file as " + recognizedType);
                        DataFile datafile = createSingleDataFile(unZippedTempFile, shortName, recognizedType, 0L);

                        logger.info("setting the directory label to " + directoryName);
                        datafile.getFileMetadata().setDirectoryLabel(directoryName);

                        datafiles.add(datafile);
                    }
                }
                zipEntry = unZippedIn.getNextEntry();

            }
        }

        return datafiles;
    }

    private Tuple2<String, String> extractDirectoryAndFileName(ZipEntry zipEntry) {
        String fileEntryName = StringUtils.defaultString(zipEntry.getName());
        String normalizedEntryName = fileEntryName
                .replace('\\', '/')
                .replaceAll("[/][/]*", "/")
                .replaceFirst("^[/]", "");

        int dirAndFileNameDividerPos = StringUtils.lastIndexOf(normalizedEntryName, '/');
        if (dirAndFileNameDividerPos != -1) {
            return new Tuple2<>(
                    StringUtils.substring(normalizedEntryName, 0, dirAndFileNameDividerPos),
                    StringUtils.substring(normalizedEntryName, dirAndFileNameDividerPos + 1));
        }
        return new Tuple2<>(null, normalizedEntryName);
    }

    /**
     * Shape files may have to be split into multiple files,
     * one zip archive per each complete set of shape files.
     */
    private List<DataFile> createDataFilesFromReshapedShapeFile(Path tempFile, Long fileSizeLimit) throws IOException {

        IngestServiceShapefileHelper shpIngestHelper = new IngestServiceShapefileHelper(tempFile.toFile(), Paths.get(getFilesTempDirectory()).toFile());

        List<DataFile> datafiles = new ArrayList<>();

        for (File finalFile : shpIngestHelper.processFile()) {
            String finalType = fileTypeDetector.determineFileType(finalFile, finalFile.getName());

            try (FileInputStream finalFileInputStream = new FileInputStream(finalFile)) {
                Path unZippedShapeTempFile = FileUtil.limitedInputStreamToTempFile(finalFileInputStream, fileSizeLimit);
                DataFile newDatafile = createSingleDataFile(unZippedShapeTempFile, finalFile.getName(), finalType, 0L);
                datafiles.add(newDatafile);

            }
        }

        return datafiles;
    }

    /**
     * This method creates a DataFile;
     * This method should only be called by the upper-level methods that handle
     * file upload and creation for individual use cases - a single file upload,
     * an upload of a zip archive that needs to be unpacked and turned into
     * individual files, etc., and once the file name and mime type have already
     * been figured out.
     */
    private DataFile createSingleDataFile(Path filePath, String fileName, String contentType, Long uncompressedSize) throws IOException {

        DataFile datafile = new DataFile(contentType);
        datafile.setCreateDate(new Timestamp(new Date().getTime()));
        datafile.setModificationTime(new Timestamp(new Date().getTime()));
        /**
         * @todo Think more about when permissions on files are modified.
         * Obviously, here at create time files have some sort of permissions,
         * even if these permissions are *implied*, by ViewUnpublishedDataset at
         * the dataset level, for example.
         */
        datafile.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile.setUncompressedSize(uncompressedSize);
        FileMetadata fmd = new FileMetadata();

        // TODO: add directoryLabel?
        fmd.setLabel(fileName);

        FileTermsOfUse termsOfUse = termsOfUseFactory.createTermsOfUse();
        fmd.setTermsOfUse(termsOfUse);

        fmd.setTermsOfUseForm(termsOfUseFormMapper.mapToForm(termsOfUse));

        fmd.setDataFile(datafile);
        datafile.getFileMetadatas().add(fmd);

        datafile.setStorageIdentifier(FileUtil.generateStorageIdentifier());
        Path destinationPath = Paths.get(getFilesTempDirectory(), datafile.getStorageIdentifier());
        Files.move(filePath, destinationPath);

        DataFile.ChecksumType checksumType = DataFile.ChecksumType.fromString(settingsService.getValueForKey(SettingsServiceBean.Key.FileFixityChecksumAlgorithm));

        datafile.setChecksumType(checksumType);
        datafile.setChecksumValue(calculateChecksum(destinationPath, checksumType));

        return datafile;
    }

}
