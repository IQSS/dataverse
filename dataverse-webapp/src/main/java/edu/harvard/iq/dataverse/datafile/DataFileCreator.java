package edu.harvard.iq.dataverse.datafile;

import com.github.junrar.ContentDescription;
import com.github.junrar.Junrar;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.files.mime.ApplicationMimeType;
import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.datasetutility.VirusFoundException;
import edu.harvard.iq.dataverse.ingest.IngestServiceShapefileHelper;
import edu.harvard.iq.dataverse.license.TermsOfUseFactory;
import edu.harvard.iq.dataverse.license.TermsOfUseFormMapper;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestError;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ExternalRarDataUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static edu.harvard.iq.dataverse.common.FileSizeUtil.bytesToHumanReadable;
import static edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestReport.createIngestFailureReport;
import static edu.harvard.iq.dataverse.util.FileUtil.calculateChecksum;
import static edu.harvard.iq.dataverse.util.FileUtil.canIngestAsTabular;
import static edu.harvard.iq.dataverse.util.FileUtil.determineFileType;
import static edu.harvard.iq.dataverse.util.FileUtil.getFilesTempDirectory;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class DataFileCreator {

    private static final Logger logger = LoggerFactory.getLogger(DataFileCreator.class.getCanonicalName());

    @Inject
    private SettingsServiceBean settingsService;
    @Inject
    private FileService fileService;
    @Inject
    private TermsOfUseFactory termsOfUseFactory;
    @Inject
    private TermsOfUseFormMapper termsOfUseFormMapper;

    public List<DataFile> createDataFiles(DatasetVersion version, InputStream inputStream, String fileName, String suppliedContentType) throws IOException {
        List<DataFile> datafiles = new ArrayList<>();

        IngestError errorKey = null;

        // save the file, in the temporary location for now:
        Path tempFile = null;

        Long fileSizeLimit = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaxFileUploadSizeInBytes);
        Long uncompressedSize = 0L;

        if (getFilesTempDirectory() != null) {
            tempFile = saveInputStreamInTempFile(inputStream, fileSizeLimit).toPath();
        } else {
            throw new IOException("Temp directory is not configured.");
        }

        if (settingsService.isTrueForKey(SettingsServiceBean.Key.AntivirusScannerEnabled) &&
                tempFile
                        .toFile()
                        .length() <= settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.AntivirusScannerMaxFileSize)) {

            String scannerMessage = fileService.scan(tempFile.toFile());

            if (scannerMessage.contains("FOUND")) {
                logger.warn("There was an attempt to upload file infected with virus. Scanner message: {} Dataset version: {}", scannerMessage, version);
                if (!tempFile.toFile().delete()) {
                    logger.warn("Unable to remove temporary file {}", tempFile.toFile().getAbsolutePath());
                }
                throw new VirusFoundException(scannerMessage);
            }
        }

        logger.info("mime type supplied: " + suppliedContentType);
        // Let's try our own utilities (Jhove, etc.) to determine the file type
        // of the uploaded file. (We may already have a mime type supplied for this
        // file - maybe the type that the browser recognized on upload; or, if
        // it's a harvest, maybe the remote server has already given us the type
        // for this file... with our own type utility we may or may not do better
        // than the type supplied:
        //  -- L.A.
        String recognizedType = null;
        String finalType = null;
        try {
            recognizedType = determineFileType(tempFile.toFile(), fileName);
            logger.info("File utility recognized the file as " + recognizedType);
            if (recognizedType != null && !recognizedType.equals("")) {
                // is it any better than the type that was supplied to us,
                // if any?
                // This is not as trivial a task as one might expect...
                // We may need a list of "good" mime types, that should always
                // be chosen over other choices available. Maybe it should
                // even be a weighed list... as in, "application/foo" should
                // be chosen over "application/foo-with-bells-and-whistles".

                // For now the logic will be as follows:
                //
                // 1. If the contentType supplied (by the browser, most likely)
                // is some form of "unknown", we always discard it in favor of
                // whatever our own utilities have determined;
                // 2. We should NEVER trust the browser when it comes to the
                // following "ingestable" types: Stata, SPSS, R;
                // 2a. We are willing to TRUST the browser when it comes to
                //  the CSV and XSLX ingestable types.
                // 3. We should ALWAYS trust our utilities when it comes to
                // ingestable types.

                if (suppliedContentType == null
                        || suppliedContentType.equals("")
                        || suppliedContentType.equalsIgnoreCase(ApplicationMimeType.UNDETERMINED_DEFAULT.getMimeValue())
                        || suppliedContentType.equalsIgnoreCase(ApplicationMimeType.UNDETERMINED_BINARY.getMimeValue())
                        || (canIngestAsTabular(suppliedContentType)
                        && !suppliedContentType.equalsIgnoreCase(TextMimeType.CSV.getMimeValue())
                        && !suppliedContentType.equalsIgnoreCase(TextMimeType.CSV_ALT.getMimeValue())
                        && !suppliedContentType.equalsIgnoreCase(ApplicationMimeType.XLSX.getMimeValue()))
                        || canIngestAsTabular(recognizedType)
                        || recognizedType.equals("application/fits-gzipped")
                        || recognizedType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)
                        || recognizedType.equals(ApplicationMimeType.ZIP.getMimeValue())) {
                    finalType = recognizedType;
                }
            }

        } catch (Exception ex) {
            logger.warn("Failed to run the file utility mime type check on file " + fileName, ex);
        }

        if (finalType == null) {
            finalType = (suppliedContentType == null || suppliedContentType.equals(""))
                    ? ApplicationMimeType.UNDETERMINED_DEFAULT.getMimeValue()
                    : suppliedContentType;
        }

        // A few special cases:

        // if this is a gzipped FITS file, we'll uncompress it, and ingest it as
        // a regular FITS file:

        if (finalType.equals("application/fits-gzipped")) {

            InputStream uncompressedIn = null;
            String finalFileName = fileName;
            // if the file name had the ".gz" extension, remove it,
            // since we are going to uncompress it:
            if (fileName != null && fileName.matches(".*\\.gz$")) {
                finalFileName = fileName.replaceAll("\\.gz$", "");
            }

            DataFile datafile = null;
            try {
                uncompressedIn = new GZIPInputStream(new FileInputStream(tempFile.toFile()));
                File unZippedTempFile = saveInputStreamInTempFile(uncompressedIn, fileSizeLimit);
                DataFile.ChecksumType checksumType = DataFile.ChecksumType.fromString(settingsService.getValueForKey(SettingsServiceBean.Key.FileFixityChecksumAlgorithm));
                datafile = createSingleDataFile(version, unZippedTempFile, finalFileName, ApplicationMimeType.UNDETERMINED_DEFAULT
                        .getMimeValue(), checksumType, uncompressedSize);
            } catch (IOException | FileExceedsMaxSizeException ioex) {
                datafile = null;
            } finally {
                if (uncompressedIn != null) {
                    try {
                        uncompressedIn.close();
                    } catch (IOException e) {
                    }
                }
            }

            // If we were able to produce an uncompressed file, we'll use it
            // to create and return a final DataFile; if not, we're not going
            // to do anything - and then a new DataFile will be created further
            // down, from the original, uncompressed file.
            if (datafile != null) {
                // remove the compressed temp file:
                try {
                    tempFile.toFile().delete();
                } catch (SecurityException ex) {
                    logger.warn("Failed to delete temporary file " + tempFile.toString(), ex);
                }

                datafiles.add(datafile);
                return datafiles;
            }
        } else if ("application/vnd.rar".equals(finalType)) {
            try {
                List<ContentDescription> contentsDescription = Junrar.getContentsDescription(tempFile.toFile());
                uncompressedSize = contentsDescription.stream()
                        .mapToLong(d -> d.size)
                        .sum();
            } catch (UnsupportedRarV5Exception r5e) {
                uncompressedSize = new ExternalRarDataUtil(
                    settingsService.getValueForKey(SettingsServiceBean.Key.RarDataUtilCommand),
                    settingsService.getValueForKey(SettingsServiceBean.Key.RarDataUtilOpts),
                    settingsService.getValueForKey(SettingsServiceBean.Key.RarDataLineBeforeResultDelimiter))
                    .checkRarExternally(tempFile, fileName);
            } catch (RarException re) {
                logger.warn("Exception during rar file scan: " + fileName, re);
            }
        }
        // If it's a ZIP file, we are going to unpack it and create multiple
        // DataFile objects from its contents:
        else if (finalType.equals("application/zip") && settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.ZipUploadFilesLimit) > 0) {

            ZipInputStream unZippedIn = null;
            ZipEntry zipEntry = null;

            long fileNumberLimit = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.ZipUploadFilesLimit);

            try {
                unZippedIn = new ZipInputStream(new FileInputStream(tempFile.toFile()));

                while (true) {
                    try {
                        zipEntry = unZippedIn.getNextEntry();
                    } catch (IllegalArgumentException iaex) {
                        // Note:
                        // ZipInputStream documentation doesn't even mention that
                        // getNextEntry() throws an IllegalArgumentException!
                        // but that's what happens if the file name of the next
                        // entry is not valid in the current CharSet.
                        //      -- L.A.
                        errorKey = IngestError.UNZIP_FAIL;
                        logger.warn("Failed to unpack Zip file. (Unknown Character Set used in a file name?) Saving the file as is.", iaex);
                        throw new IOException();
                    }

                    if (zipEntry == null) {
                        break;
                    }
                    // Note that some zip entries may be directories - we
                    // simply skip them:

                    if (!zipEntry.isDirectory()) {
                        if (datafiles.size() >= fileNumberLimit) {
                            logger.warn("Zip upload - too many files.");
                            errorKey = IngestError.UNZIP_FILE_LIMIT_FAIL;
                            throw new IOException();
                        }

                        String fileEntryName = zipEntry.getName();
                        logger.info("ZipEntry, file: " + fileEntryName);

                        if (fileEntryName != null && !fileEntryName.equals("")) {

                            String shortName = fileEntryName.replaceFirst("^.*[\\/]", "");

                            // Check if it's a "fake" file - a zip archive entry
                            // created for a MacOS X filesystem element: (these
                            // start with "._")
                            if (!shortName.startsWith("._") && !shortName.startsWith(".DS_Store") && !"".equals(shortName)) {
                                // OK, this seems like an OK file entry - we'll try
                                // to read it and create a DataFile with it:

                                File unZippedTempFile = saveInputStreamInTempFile(unZippedIn, fileSizeLimit, false);
                                DataFile.ChecksumType checksumType = DataFile.ChecksumType.fromString(settingsService.getValueForKey(SettingsServiceBean.Key.FileFixityChecksumAlgorithm));
                                DataFile datafile = createSingleDataFile(version, unZippedTempFile, shortName, ApplicationMimeType.UNDETERMINED_DEFAULT
                                        .getMimeValue(), checksumType, uncompressedSize,false);

                                if (!fileEntryName.equals(shortName)) {
                                    // If the filename looks like a hierarchical folder name (i.e., contains slashes and backslashes),
                                    // we'll extract the directory name, then a) strip the leading and trailing slashes;
                                    // and b) replace all the back slashes with regular ones and b) replace any multiple
                                    // slashes with a single slash:
                                    String directoryName = fileEntryName
                                            .replaceFirst("[\\/][\\/]*[^\\/]*$", "")
                                            .replaceFirst("^[\\/]*", "")
                                            .replaceAll("[\\/][\\/]*", "/");
                                    if (!"".equals(directoryName)) {
                                        logger.info("setting the directory label to " + directoryName);
                                        datafile.getFileMetadata().setDirectoryLabel(directoryName);
                                    }
                                }

                                if (datafile != null) {
                                    // We have created this datafile with the mime type "unknown";
                                    // Now that we have it saved in a temporary location,
                                    // let's try and determine its real type:

                                    String tempFileName = getFilesTempDirectory() + "/" + datafile.getStorageIdentifier();

                                    try {
                                        recognizedType = determineFileType(new File(tempFileName), shortName);
                                        logger.info("File utility recognized unzipped file as " + recognizedType);
                                        if (recognizedType != null && !recognizedType.equals("")) {
                                            datafile.setContentType(recognizedType);
                                        }
                                    } catch (Exception ex) {
                                        logger.warn("Failed to run the file utility mime type check on file " + fileName, ex);
                                    }

                                    datafiles.add(datafile);
                                }
                            }
                        }
                    }
                    unZippedIn.closeEntry();

                }

            } catch (IOException ioex) {
                // just clear the datafiles list and let
                // ingest default to creating a single DataFile out
                // of the unzipped file.
                logger.warn("Failed to unzip the file. Saving the file as is.", ioex);
                if (errorKey == IngestError.UNZIP_FILE_LIMIT_FAIL) {
                    uncompressedSize = extractZipContentsSize(tempFile);
                } else if (errorKey == null) {
                    errorKey = IngestError.UNZIP_FAIL;
                }

                datafiles.clear();
            } catch (FileExceedsMaxSizeException femsx) {
                logger.warn("One of the unzipped files exceeds the size limit resorting to saving the file as is, unzipped.", femsx);
                errorKey = IngestError.UNZIP_SIZE_FAIL;
                datafiles.clear();
            } finally {
                if (unZippedIn != null) {
                    try {
                        unZippedIn.close();
                    } catch (Exception zEx) {
                    }
                }
            }
            if (datafiles.size() > 0) {
                // remove the uploaded zip file:
                try {
                    Files.delete(tempFile);
                } catch (IOException ioex) {
                    // do nothing - it's just a temp file.
                    logger.warn("Could not remove temp file " + tempFile.getFileName().toString(), ioex);
                }
                // and return:
                return datafiles;
            }
        } else if (finalType.equals("application/zip")
                && settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.ZipUploadFilesLimit) == 0) {
            uncompressedSize = extractZipContentsSize(tempFile);
        } else if ("application/x-7z-compressed".equals(finalType)) {
            long size = 0L;
            try {
                SevenZFile archive = new SevenZFile(tempFile.toFile());
                SevenZArchiveEntry entry;
                while ((entry = archive.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        size += entry.getSize();
                    }
                }
            } catch (IOException ioe) {
                logger.warn("Exception while checking contents of 7z file: " + fileName, ioe);
            }
            uncompressedSize = size;
        } else if ("application/gzip".equals(finalType) || "application/x-compressed-tar".equals(finalType)) {
            Long maxInputSize = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.GzipMaxInputFileSizeInBytes);
            Long maxOutputSize = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.GzipMaxOutputFileSizeInBytes);

            maxInputSize = maxInputSize != null ? maxInputSize : 0L;
            maxOutputSize = maxOutputSize != null ? maxOutputSize : 0L;

            long inputSize = tempFile.toFile().length();
            if (inputSize > 0L && inputSize <= maxInputSize) {
                File outputFile = null;
                try (GZIPInputStream output = new GZIPInputStream(new FileInputStream(tempFile.toFile()))) {
                    outputFile = saveInputStreamInTempFile(output, maxOutputSize);
                    uncompressedSize = outputFile.length();
                } catch (FileExceedsMaxSizeException femse) {
                    logger.warn(
                        String.format("The contents of file [%s] exceed the max allowed size of uncompressed output", fileName),
                        femse);
                } catch (IOException ioe) {
                    logger.warn("Exception while trying to uncompress file: " + fileName, ioe);
                } finally {
                    if (outputFile != null) {
                        outputFile.delete();
                    }
                }
            }
        } else if (finalType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)) {
            // Shape files may have to be split into multiple files,
            // one zip archive per each complete set of shape files:

            //File rezipFolder = new File(this.getFilesTempDirectory());
            File rezipFolder = getShapefileUnzipTempDirectory();

            IngestServiceShapefileHelper shpIngestHelper;
            shpIngestHelper = new IngestServiceShapefileHelper(tempFile.toFile(), rezipFolder);

            boolean didProcessWork = shpIngestHelper.processFile();
            if (!(didProcessWork)) {
                logger.error("Processing of zipped shapefile failed.");
                return null;
            }

            try {
                for (File finalFile : shpIngestHelper.getFinalRezippedFiles()) {
                    FileInputStream finalFileInputStream = new FileInputStream(finalFile);
                    finalType = determineContentType(finalFile);
                    if (finalType == null) {
                        logger.warn("Content type is null; but should default to 'MIME_TYPE_UNDETERMINED_DEFAULT'");
                        continue;
                    }

                    File unZippedShapeTempFile = saveInputStreamInTempFile(finalFileInputStream, fileSizeLimit);
                    DataFile.ChecksumType checksumType = DataFile.ChecksumType.fromString(settingsService.getValueForKey(SettingsServiceBean.Key.FileFixityChecksumAlgorithm));
                    DataFile new_datafile = createSingleDataFile(version, unZippedShapeTempFile, finalFile.getName(),
                            finalType, checksumType, uncompressedSize);
                    if (new_datafile != null) {
                        datafiles.add(new_datafile);
                    } else {
                        logger.error("Could not add part of rezipped shapefile. new_datafile was null: " + finalFile.getName());
                    }
                    finalFileInputStream.close();

                }
            } catch (FileExceedsMaxSizeException femsx) {
                logger.error("One of the unzipped shape files exceeded the size limit; giving up. " + femsx.getMessage(), femsx);
                datafiles.clear();
            }

            // Delete the temp directory used for unzipping
            FileUtils.deleteDirectory(rezipFolder);

            if (datafiles.size() > 0) {
                // remove the uploaded zip file:
                try {
                    Files.delete(tempFile);
                } catch (IOException ioex) {
                    // do nothing - it's just a temp file.
                    logger.warn("Could not remove temp file " + tempFile.getFileName().toString(), ioex);
                } catch (SecurityException se) {
                    logger.warn("Unable to delete: " + tempFile.toString() + "due to Security Exception: "
                                        + se.getMessage(), se);
                }
                return datafiles;
            } else {
                logger.error("No files added from directory of rezipped shapefiles");
            }
            return null;

        }
        // Finally, if none of the special cases above were applicable (or
        // if we were unable to unpack an uploaded file, etc.), we'll just
        // create and return a single DataFile:
        DataFile.ChecksumType checksumType = DataFile.ChecksumType.fromString(settingsService.getValueForKey(SettingsServiceBean.Key.FileFixityChecksumAlgorithm));
        DataFile datafile = createSingleDataFile(version, tempFile.toFile(), fileName, finalType, checksumType, uncompressedSize);

        if (datafile != null) {

            if (errorKey != null) {

                if (errorKey.equals(IngestError.UNZIP_FILE_LIMIT_FAIL) && fileSizeLimit != null) {
                    datafile.setIngestReport(createIngestFailureReport(datafile, errorKey, fileSizeLimit.toString()));
                } else {
                    datafile.setIngestReport(createIngestFailureReport(datafile, errorKey));
                }

                datafile.SetIngestProblem();
            }
            datafiles.add(datafile);

            return datafiles;
        }

        return null;
    } // end createDataFiles
    
    
    // -------------------- PRIVATE --------------------
    
    /*
     * This method creates a DataFile;
     * The bytes from the suppplied InputStream have already been saved in the temporary location.
     * This method should only be called by the upper-level methods that handle
     * file upload and creation for individual use cases - a single file upload,
     * an upload of a zip archive that needs to be unpacked and turned into
     * individual files, etc., and once the file name and mime type have already
     * been figured out.
     */

    private DataFile createSingleDataFile(DatasetVersion version, File tempFile, String fileName, String contentType,
                          DataFile.ChecksumType checksumType, Long uncompressedSize) {
        return createSingleDataFile(version, tempFile, fileName, contentType, checksumType, uncompressedSize, false);
    }

    private DataFile createSingleDataFile(DatasetVersion version, File tempFile, String fileName, String contentType, DataFile.ChecksumType checksumType, Long uncompressedSize, boolean addToDataset) {

        if (tempFile == null) {
            return null;
        }

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

        if (addToDataset) {
            datafile.setOwner(version.getDataset());
        }
        fmd.setDataFile(datafile);
        datafile.getFileMetadatas().add(fmd);
        if (addToDataset) {
            if (version.getFileMetadatas() == null) {
                version.setFileMetadatas(new ArrayList<>());
            }
            version.addFileMetadata(fmd);
            fmd.setDatasetVersion(version);
            version.getDataset().getFiles().add(datafile);
        }

        datafile.setStorageIdentifier(FileUtil.generateStorageIdentifier());
        if (!tempFile.renameTo(new File(getFilesTempDirectory() + "/" + datafile.getStorageIdentifier()))) {
            return null;
        }

        try {
            // We persist "SHA1" rather than "SHA-1".
            datafile.setChecksumType(checksumType);
            datafile.setChecksumValue(calculateChecksum(getFilesTempDirectory() + "/" + datafile.getStorageIdentifier(), datafile
                    .getChecksumType()));
        } catch (Exception cksumEx) {
            logger.warn("Could not calculate " + checksumType + " signature for the new file " + fileName, cksumEx);
        }

        return datafile;
    }
    
    private long extractZipContentsSize(Path tempFile) {
        long size = 0;
        try (ZipFile zipFile = new ZipFile(tempFile.toFile())) {
            Enumeration<ZipArchiveEntry> zipEntries = zipFile.getEntries();
            while (zipEntries.hasMoreElements()) {
                ZipArchiveEntry zipEntry = zipEntries.nextElement();
                if (!zipEntry.isDirectory()) {
                    size += zipEntry.getSize();
                }
            }
        } catch (IOException ioe) {
            logger.warn("Exception encountered: ", ioe);
        }
        return size;
    }

    private File saveInputStreamInTempFile(InputStream inputStream, Long fileSizeLimit)
            throws IOException, FileExceedsMaxSizeException {
        return saveInputStreamInTempFile(inputStream, fileSizeLimit, true);
    }

    private File saveInputStreamInTempFile(InputStream inputStream, Long fileSizeLimit, boolean closeStream)
            throws IOException, FileExceedsMaxSizeException {
        Path tempFile = Files.createTempFile(Paths.get(getFilesTempDirectory()), "tmp", "upload");

        try (OutputStream outStream = Files.newOutputStream(tempFile)) {
            logger.info("Will attempt to save the file as: " + tempFile.toString());

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            long totalBytesRead = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (fileSizeLimit != null && totalBytesRead > fileSizeLimit) {
                    try {
                        tempFile.toFile().delete();
                    } catch (Exception ex) {
                        logger.error("There was a problem with deleting temporary file");
                    }

                    throw new FileExceedsMaxSizeException(MessageFormat.format(BundleUtil.getStringFromBundle("file.addreplace.error.file_exceeds_limit"),
                                                                               bytesToHumanReadable(fileSizeLimit)));
                }
                outStream.write(buffer, 0, bytesRead);
            }
        } finally {
            if (closeStream) {
                inputStream.close();
            }
        }

        return tempFile.toFile();
    }
    

    /**
     * Returns a content type string for a FileObject
     */
    private String determineContentType(File fileObject) {
        if (fileObject == null) {
            return null;
        }
        String contentType;
        try {
            contentType = determineFileType(fileObject, fileObject.getName());
        } catch (Exception ex) {
            logger.warn("FileUtil.determineFileType failed for file with name: " + fileObject.getName(), ex);
            contentType = null;
        }

        if ((contentType == null) || (contentType.equals(""))) {
            contentType = ApplicationMimeType.UNDETERMINED_DEFAULT.getMimeValue();
        }
        return contentType;

    }
    


    /**
     * For the restructuring of zipped shapefiles, create a timestamped directory.
     * This directory is deleted after successful restructuring.
     * <p>
     * Naming convention: getFilesTempDirectory() + "shp_" + "yyyy-MM-dd-hh-mm-ss-SSS"
     */
    private static File getShapefileUnzipTempDirectory() {

        String tempDirectory = getFilesTempDirectory();
        if (tempDirectory == null) {
            logger.error("Failed to retrieve tempDirectory, null was returned");
            return null;
        }
        String datestampedFileName = "shp_" + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SSS").format(new Date());
        String datestampedFolderName = tempDirectory + "/" + datestampedFileName;

        File datestampedFolder = new File(datestampedFolderName);
        if (!datestampedFolder.isDirectory()) {
            /* Note that "createDirectories()" must be used - not
             * "createDirectory()", to make sure all the parent
             * directories that may not yet exist are created as well.
             */
            try {
                Files.createDirectories(Paths.get(datestampedFolderName));
            } catch (IOException ex) {
                logger.error("Failed to create temp. directory to unzip shapefile: " + datestampedFolderName, ex);
                return null;
            }
        }
        return datestampedFolder;
    }
}
