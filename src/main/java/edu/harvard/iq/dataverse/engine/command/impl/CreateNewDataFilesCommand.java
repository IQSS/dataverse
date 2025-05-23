package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.datasetutility.FileSizeChecker;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.ingest.IngestServiceShapefileHelper;
import edu.harvard.iq.dataverse.storageuse.UploadSessionQuotaLimit;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.file.BagItFileHandler;
import edu.harvard.iq.dataverse.util.file.BagItFileHandlerFactory;
import edu.harvard.iq.dataverse.util.file.CreateDataFileResult;
import edu.harvard.iq.dataverse.util.file.FileExceedsStorageQuotaException;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static edu.harvard.iq.dataverse.datasetutility.FileSizeChecker.bytesToHumanReadable;
import static edu.harvard.iq.dataverse.util.FileUtil.MIME_TYPE_UNDETERMINED_DEFAULT;
import static edu.harvard.iq.dataverse.util.FileUtil.createIngestFailureReport;
import static edu.harvard.iq.dataverse.util.FileUtil.determineFileType;
import static edu.harvard.iq.dataverse.util.FileUtil.determineRemoteFileType;
import static edu.harvard.iq.dataverse.util.FileUtil.getFilesTempDirectory;
import static edu.harvard.iq.dataverse.util.FileUtil.saveInputStreamInTempFile;
import static edu.harvard.iq.dataverse.util.FileUtil.useRecognizedType;

/**
 *
 * @author landreev
 */
// Note the commented out @RequiredPermissions. We need to use dynamic 
// permissions instead, to accommodate both adding files to an existing 
// dataset and files being uploaded in the context of creating a new dataset
// via the Add Dataset page. 
//@RequiredPermissions( Permission.EditDataset )
public class CreateNewDataFilesCommand extends AbstractCommand<CreateDataFileResult> {
    private static final Logger logger = Logger.getLogger(CreateNewDataFilesCommand.class.getCanonicalName());
    
    private final DatasetVersion version;
    private final InputStream inputStream;
    private final String fileName;
    private final String suppliedContentType; 
    private final UploadSessionQuotaLimit quota;
    // parent Dataverse must be specified when the command is called on Create 
    // of a new dataset that does not exist in the database yet (for the purposes
    // of authorization - see getRequiredPermissions() below):
    private final Dataverse parentDataverse;
    // With Direct Upload the following values already exist and are passed to the command:
    private final String newStorageIdentifier; 
    private final String newCheckSum; 
    private DataFile.ChecksumType newCheckSumType;
    private final Long newFileSize;
    private boolean replaceMode;

    public CreateNewDataFilesCommand(DataverseRequest aRequest, DatasetVersion version, InputStream inputStream, String fileName, String suppliedContentType, String newStorageIdentifier, UploadSessionQuotaLimit quota, String newCheckSum) {
        this(aRequest, version, inputStream, fileName, suppliedContentType, newStorageIdentifier, quota, newCheckSum, null);
    }

    public CreateNewDataFilesCommand(DataverseRequest aRequest, DatasetVersion version, InputStream inputStream, String fileName, String suppliedContentType, String newStorageIdentifier, UploadSessionQuotaLimit quota, String newCheckSum, DataFile.ChecksumType newCheckSumType) {
        this(aRequest, version, inputStream, fileName, suppliedContentType, newStorageIdentifier, quota, newCheckSum, newCheckSumType, null, null, false);
    }

    public CreateNewDataFilesCommand(DataverseRequest aRequest, DatasetVersion version, InputStream inputStream, String fileName, String suppliedContentType, String newStorageIdentifier, UploadSessionQuotaLimit quota, String newCheckSum, DataFile.ChecksumType newCheckSumType, Long newFileSize) {
        this(aRequest, version, inputStream, fileName, suppliedContentType, newStorageIdentifier, quota, newCheckSum, newCheckSumType, newFileSize, null, false);
    }

    public CreateNewDataFilesCommand(DataverseRequest aRequest, DatasetVersion version, InputStream inputStream, String fileName, String suppliedContentType, String newStorageIdentifier, UploadSessionQuotaLimit quota, String newCheckSum, DataFile.ChecksumType newCheckSumType, Long newFileSize, Dataverse dataverse) {
        this(aRequest, version, inputStream, fileName, suppliedContentType, newStorageIdentifier, quota, newCheckSum, newCheckSumType, newFileSize, dataverse, false);
    }

    public CreateNewDataFilesCommand(DataverseRequest aRequest, DatasetVersion version, InputStream inputStream, String fileName, String suppliedContentType, String newStorageIdentifier, UploadSessionQuotaLimit quota, String newCheckSum, DataFile.ChecksumType newCheckSumType, Long newFileSize, boolean replace) {
        this(aRequest, version, inputStream, fileName, suppliedContentType, newStorageIdentifier, quota, newCheckSum, newCheckSumType, newFileSize, null, replace);
    }

    // This version of the command must be used when files are created in the
    // context of creating a brand new dataset (from the Add Dataset page):
    
    public CreateNewDataFilesCommand(DataverseRequest aRequest, DatasetVersion version, InputStream inputStream, String fileName, String suppliedContentType, String newStorageIdentifier, UploadSessionQuotaLimit quota, String newCheckSum, DataFile.ChecksumType newCheckSumType, Long newFileSize, Dataverse dataverse, boolean replace) {
        super(aRequest, dataverse);
        
        this.version = version;
        this.inputStream = inputStream;
        this.fileName = fileName;
        this.suppliedContentType = suppliedContentType; 
        this.newStorageIdentifier = newStorageIdentifier; 
        this.newCheckSum = newCheckSum; 
        this.newCheckSumType = newCheckSumType;
        this.parentDataverse = dataverse;
        this.quota = quota;
        this.newFileSize = newFileSize;
        this.replaceMode = replace;
    }
    

    @Override
    public CreateDataFileResult execute(CommandContext ctxt) throws CommandException {
        List<DataFile> datafiles = new ArrayList<>();

        //When there is no checksum/checksumtype being sent (normal upload, needs to be calculated), set the type to the current default
        if(newCheckSumType == null) {
            newCheckSumType = ctxt.systemConfig().getFileFixityChecksumAlgorithm();
        }

        boolean isSuperuser = getRequest() !=null && getRequest().getAuthenticatedUser() != null && getRequest().getAuthenticatedUser().isSuperuser();
        // ignore the file count limit check if replacing (not createMode), is superuser, or hasFileCountLimit is false
        if (!replaceMode && !isSuperuser && version.getDataset() != null) {
            DvObjectContainer dvo = version.getDataset();
            Integer effectiveDatasetFileCountLimit = dvo.getEffectiveDatasetFileCountLimit();
            boolean hasFileCountLimit = !dvo.isDatasetFileCountLimitNotSet(effectiveDatasetFileCountLimit);
            if (hasFileCountLimit) {
                // Get the number of uploaded files
                DatasetServiceBean datasetService = CDI.current().select(DatasetServiceBean.class).get();
                long uploadedFileCount = datasetService.getDataFileCountByOwner(dvo.getId());
                if (uploadedFileCount >= effectiveDatasetFileCountLimit) {
                    throw new CommandExecutionException(BundleUtil.getStringFromBundle("file.add.count_exceeds_limit", Arrays.asList(String.valueOf(effectiveDatasetFileCountLimit))), this);
                }
            }
        }

        String warningMessage = null;

        // save the file, in the temporary location for now: 
        Path tempFile = null;

        Long fileSizeLimit = ctxt.systemConfig().getMaxFileUploadSizeForStore(version.getDataset().getEffectiveStorageDriverId());
        Long storageQuotaLimit = null; 
        
        if (ctxt.systemConfig().isStorageQuotasEnforced()) {
            if (quota != null) {
                storageQuotaLimit = quota.getRemainingQuotaInBytes();
            }
        }
        String finalType = null;
        File newFile = null;    // this File will be used for a single-file, local (non-direct) upload
        long fileSize = -1; 


        if (newStorageIdentifier == null) {
            var filesTempDirectory = getFilesTempDirectory();
            if (filesTempDirectory != null) {
                try {
                    tempFile = Files.createTempFile(Paths.get(filesTempDirectory), "tmp", "upload");
                    // "temporary" location is the key here; this is why we are not using
                    // the DataStore framework for this - the assumption is that
                    // temp files will always be stored on the local filesystem.
                    // -- L.A. Jul. 2014
                    logger.fine("Will attempt to save the file as: " + tempFile.toString());
                    Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ioex) {
                    throw new CommandExecutionException("Failed to save the upload as a temp file (temp disk space?)", ioex, this);
                }

                // A file size check, before we do anything else:
                // (note that "no size limit set" = "unlimited")
                // (also note, that if this is a zip file, we'll be checking
                // the size limit for each of the individual unpacked files)
                fileSize = tempFile.toFile().length();
                if (fileSizeLimit != null && fileSize > fileSizeLimit) {
                    try {
                        tempFile.toFile().delete();
                    } catch (Exception ex) {
                        // ignore - but log a warning
                        logger.warning("Could not remove temp file " + tempFile.getFileName());
                    }
                    throw new CommandExecutionException(MessageFormat.format(BundleUtil.getStringFromBundle("file.addreplace.error.file_exceeds_limit"), bytesToHumanReadable(fileSize), bytesToHumanReadable(fileSizeLimit)), this);
                }

            } else {
                throw new CommandExecutionException("Temp directory is not configured.", this);
            }
            
            logger.fine("mime type supplied: " + suppliedContentType);
            
            // Let's try our own utilities (Jhove, etc.) to determine the file type
            // of the uploaded file. (We may already have a mime type supplied for this
            // file - maybe the type that the browser recognized on upload; or, if
            // it's a harvest, maybe the remote server has already given us the type
            // for this file... with our own type utility we may or may not do better
            // than the type supplied:
            // -- L.A.
            String recognizedType = null;

            try {
                recognizedType = determineFileType(tempFile.toFile(), fileName);
                logger.fine("File utility recognized the file as " + recognizedType);
                if (recognizedType != null && !recognizedType.equals("")) {
                    if (useRecognizedType(suppliedContentType, recognizedType)) {
                        finalType = recognizedType;
                    }
                }

            } catch (Exception ex) {
                logger.warning("Failed to run the file utility mime type check on file " + fileName);
            }

            if (finalType == null) {
                finalType = (suppliedContentType == null || suppliedContentType.equals(""))
                        ? MIME_TYPE_UNDETERMINED_DEFAULT
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
                long uncompressedFileSize = -1; 
                try {
                    uncompressedIn = new GZIPInputStream(new FileInputStream(tempFile.toFile()));
                    File unZippedTempFile = saveInputStreamInTempFile(uncompressedIn, fileSizeLimit, storageQuotaLimit);
                    uncompressedFileSize = unZippedTempFile.length();
                    datafile = FileUtil.createSingleDataFile(version, unZippedTempFile, finalFileName, MIME_TYPE_UNDETERMINED_DEFAULT, ctxt.systemConfig().getFileFixityChecksumAlgorithm());
                } catch (IOException | FileExceedsMaxSizeException | FileExceedsStorageQuotaException ioex) {
                    // it looks like we simply skip the file silently, if its uncompressed size
                    // exceeds the limit. we should probably report this in detail instead.
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
                        // (this is very non-fatal)
                        logger.warning("Failed to delete temporary file " + tempFile.toString());
                    }

                    datafiles.add(datafile);
                    // Update quota if present
                    if (quota != null) {
                        quota.setTotalUsageInBytes(quota.getTotalUsageInBytes() + uncompressedFileSize);
                    }
                    return CreateDataFileResult.success(fileName, finalType, datafiles);
                }

                // If it's a ZIP file, we are going to unpack it and create multiple
                // DataFile objects from its contents:
            } else if (finalType.equals("application/zip")) {

                int fileNumberLimit = ctxt.systemConfig().getZipUploadFilesLimit();
                Long combinedUnzippedFileSize = 0L;

                try {
                    Charset charset = null;
                    /*
                	TODO: (?)
                	We may want to investigate somehow letting the user specify
                	the charset for the filenames in the zip file...
                    - otherwise, ZipInputStream bails out if it encounteres a file
                	name that's not valid in the current charest (i.e., UTF-8, in
                    our case). It would be a bit trickier than what we're doing for
                    SPSS tabular ingests - with the lang. encoding pulldown menu -
                	because this encoding needs to be specified *before* we upload and
                    attempt to unzip the file.
                	        -- L.A. 4.0 beta12
                	logger.info("default charset is "+Charset.defaultCharset().name());
                	if (Charset.isSupported("US-ASCII")) {
                    	logger.info("charset US-ASCII is supported.");
                    	charset = Charset.forName("US-ASCII");
                    	if (charset != null) {
                       	    logger.info("was able to obtain charset for US-ASCII");
                    	}

                	 }
                     */

                    /**
                     * Perform a quick check for how many individual files are
                     * inside this zip archive. If it's above the limit, we can
                     * give up right away, without doing any unpacking.
                     * This should be a fairly inexpensive operation, we just need
                     * to read the directory at the end of the file.
                     */


                    /**
                     * The ZipFile constructors in openZipFile will throw ZipException -
                     * a type of IOException - if there's something wrong 
                     * with this file as a zip. There's no need to intercept it
                     * here, it will be caught further below, with other IOExceptions,
                     * at which point we'll give up on trying to unpack it and
                     * then attempt to save it as is.
                     */

                    int numberOfUnpackableFiles = 0;

                    /**
                     * Note that we can't just use zipFile.size(),
                     * unfortunately, since that's the total number of entries,
                     * some of which can be directories. So we need to go
                     * through all the individual zipEntries and count the ones
                     * that are files.
                     */

                    try (var zipFile = openZipFile(tempFile, charset)) {
                        var zipEntries = filteredZipEntries(zipFile);
                        for (var entry : zipEntries) {
                            logger.fine("inside first zip pass; this entry: " + entry.getName());
                            numberOfUnpackableFiles++;
                            if (numberOfUnpackableFiles > fileNumberLimit) {
                                logger.warning("Zip upload - too many files in the zip to process individually.");
                                warningMessage = "The number of files in the zip archive is over the limit (" + fileNumberLimit
                                                 + "); please upload a zip archive with fewer files, if you want them to be ingested "
                                                 + "as individual DataFiles.";
                                throw new IOException();
                            }
                            // In addition to counting the files, we can
                            // also check the file size while we're here,
                            // provided the size limit is defined; if a single
                            // file is above the individual size limit, unzipped,
                            // we give up on unpacking this zip archive as well:
                            if (fileSizeLimit != null && entry.getSize() > fileSizeLimit) {
                                throw new FileExceedsMaxSizeException(MessageFormat.format(BundleUtil.getStringFromBundle("file.addreplace.error.file_exceeds_limit"), bytesToHumanReadable(entry.getSize()), bytesToHumanReadable(fileSizeLimit)));
                            }
                            // Similarly, we want to check if saving all these unpacked
                            // files is going to push the disk usage over the
                            // quota:
                            if (storageQuotaLimit != null) {
                                combinedUnzippedFileSize = combinedUnzippedFileSize + entry.getSize();
                                if (combinedUnzippedFileSize > storageQuotaLimit) {
                                    //throw new FileExceedsStorageQuotaException(MessageFormat.format(BundleUtil.getStringFromBundle("file.addreplace.error.quota_exceeded"), bytesToHumanReadable(combinedUnzippedFileSize), bytesToHumanReadable(storageQuotaLimit)));
                                    // change of plans: if the unzipped content inside exceeds the remaining quota,
                                    // we reject the upload outright, rather than accepting the zip
                                    // file as is.
                                    throw new CommandExecutionException(MessageFormat.format(BundleUtil.getStringFromBundle("file.addreplace.error.unzipped.quota_exceeded"), bytesToHumanReadable(storageQuotaLimit)), this);
                                }
                            }
                        }
                        // OK we're still here - that means we can proceed unzipping.

                        // reset:
                        combinedUnzippedFileSize = 0L;

                        for (var entry : zipEntries) {
                            if (datafiles.size() > fileNumberLimit) {
                                logger.warning("Zip upload - too many files.");
                                warningMessage = "The number of files in the zip archive is over the limit (" + fileNumberLimit
                                        + "); please upload a zip archive with fewer files, if you want them to be ingested "
                                        + "as individual DataFiles.";
                                throw new IOException();
                            }
                            var fileEntryName = entry.getName();
                            var shortName = getShortName(fileEntryName);
                            logger.fine("ZipEntry, file: " + fileEntryName);
                            String storageIdentifier = FileUtil.generateStorageIdentifier();
                            File unzippedFile = new File(getFilesTempDirectory() + "/" + storageIdentifier);
                            Files.copy(zipFile.getInputStream(entry), unzippedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            // No need to check the size of this unpacked file against the size limit,
                            // since we've already checked for that in the first pass.
                            DataFile datafile = FileUtil.createSingleDataFile(version, null, storageIdentifier, shortName,
                                MIME_TYPE_UNDETERMINED_DEFAULT,
                                ctxt.systemConfig().getFileFixityChecksumAlgorithm(), null, false);

                            if (!fileEntryName.equals(shortName)) {
                                // If the filename looks like a hierarchical folder name (i.e., contains slashes and backslashes),
                                // we'll extract the directory name; then subject it to some "aggressive sanitizing" - strip all
                                // the leading, trailing and duplicate slashes; then replace all the characters that
                                // don't pass our validation rules.
                                String directoryName = fileEntryName.replaceFirst("[\\\\/][\\\\/]*[^\\\\/]*$", "");
                                directoryName = StringUtil.sanitizeFileDirectory(directoryName, true);
                                // if (!"".equals(directoryName)) {
                                if (!StringUtil.isEmpty(directoryName)) {
                                    logger.fine("setting the directory label to " + directoryName);
                                    datafile.getFileMetadata().setDirectoryLabel(directoryName);
                                }
                            }

                            if (datafile != null) {
                                // We have created this datafile with the mime type "unknown";
                                // Now that we have it saved in a temporary location,
                                // let's try and determine its real type:

                                String tempFileName = getFilesTempDirectory() + "/" + datafile.getStorageIdentifier();

                                try {
                                    recognizedType = determineFileType(unzippedFile, shortName);
                                    // null the File explicitly, to release any open FDs:
                                    unzippedFile = null;
                                    logger.fine("File utility recognized unzipped file as " + recognizedType);
                                    if (recognizedType != null && !recognizedType.equals("")) {
                                        datafile.setContentType(recognizedType);
                                    }
                                } catch (Exception ex) {
                                    logger.warning("Failed to run the file utility mime type check on file " + fileName);
                                }

                                datafiles.add(datafile);
                                combinedUnzippedFileSize += datafile.getFilesize();
                            }
                        }
                    }

                } catch (IOException ioex) {
                    // just clear the datafiles list and let
                    // ingest default to creating a single DataFile out
                    // of the unzipped file.
                    logger.warning("Unzipping failed; rolling back to saving the file as is.");
                    if (warningMessage == null) {
                        warningMessage = BundleUtil.getStringFromBundle("file.addreplace.warning.unzip.failed");
                    }

                    datafiles.clear();
                } catch (FileExceedsMaxSizeException femsx) {
                    logger.warning("One of the unzipped files exceeds the size limit; resorting to saving the file as is. " + femsx.getMessage());
                    warningMessage =  BundleUtil.getStringFromBundle("file.addreplace.warning.unzip.failed.size", Arrays.asList(FileSizeChecker.bytesToHumanReadable(fileSizeLimit)));
                    datafiles.clear();
                } /*catch (FileExceedsStorageQuotaException fesqx) {
                    //logger.warning("One of the unzipped files exceeds the storage quota limit; resorting to saving the file as is. " + fesqx.getMessage());
                    //warningMessage =  BundleUtil.getStringFromBundle("file.addreplace.warning.unzip.failed.quota", Arrays.asList(FileSizeChecker.bytesToHumanReadable(storageQuotaLimit)));
                    //datafiles.clear();
                    throw new CommandExecutionException(fesqx.getMessage(), fesqx, this);
                }*/
                if (!datafiles.isEmpty()) {
                    // remove the uploaded zip file:
                    try {
                        Files.delete(tempFile);
                    } catch (IOException ioex) {
                        // do nothing - it's just a temp file.
                        logger.warning("Could not remove temp file " + tempFile.getFileName().toString());
                    }
                    // update the quota object: 
                    if (quota != null) {
                        quota.setTotalUsageInBytes(quota.getTotalUsageInBytes() + combinedUnzippedFileSize);
                    }
                    // and return:
                    return CreateDataFileResult.success(fileName, finalType, datafiles);
                }

            } else if (finalType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)) {
                // Shape files may have to be split into multiple files,
                // one zip archive per each complete set of shape files:

                // File rezipFolder = new File(this.getFilesTempDirectory());
                File rezipFolder = FileUtil.getShapefileUnzipTempDirectory();

                IngestServiceShapefileHelper shpIngestHelper;
                shpIngestHelper = new IngestServiceShapefileHelper(tempFile.toFile(), rezipFolder);

                boolean didProcessWork = shpIngestHelper.processFile();
                if (!(didProcessWork)) {
                    logger.severe("Processing of zipped shapefile failed.");
                    return CreateDataFileResult.error(fileName, finalType);
                }
                long combinedRezippedFileSize = 0L;

                try {
                    
                    for (File finalFile : shpIngestHelper.getFinalRezippedFiles()) {
                        FileInputStream finalFileInputStream = new FileInputStream(finalFile);
                        finalType = FileUtil.determineContentType(finalFile);
                        if (finalType == null) {
                            logger.warning("Content type is null; but should default to 'MIME_TYPE_UNDETERMINED_DEFAULT'");
                            continue;
                        }

                        File unZippedShapeTempFile = saveInputStreamInTempFile(finalFileInputStream, fileSizeLimit, storageQuotaLimit != null ? storageQuotaLimit - combinedRezippedFileSize : null);
                        DataFile new_datafile = FileUtil.createSingleDataFile(version, unZippedShapeTempFile, finalFile.getName(), finalType, ctxt.systemConfig().getFileFixityChecksumAlgorithm());
                        
                        String directoryName = null;
                        String absolutePathName = finalFile.getParent();
                        if (absolutePathName != null) {
                            if (absolutePathName.length() > rezipFolder.toString().length()) {
                                // This file lives in a subfolder - we want to 
                                // preserve it in the FileMetadata:
                                directoryName = absolutePathName.substring(rezipFolder.toString().length() + 1);

                                if (!StringUtil.isEmpty(directoryName)) {
                                    new_datafile.getFileMetadata().setDirectoryLabel(directoryName);
                                }
                            }
                        }
                        if (new_datafile != null) {
                            datafiles.add(new_datafile);
                            combinedRezippedFileSize += unZippedShapeTempFile.length();
                            // todo: can this new_datafile be null?
                        } else {
                            logger.severe("Could not add part of rezipped shapefile. new_datafile was null: " + finalFile.getName());
                        }
                        try {
                            finalFileInputStream.close();
                        } catch (IOException ioex) {
                            // this one can be ignored
                        }
                    }
                } catch (FileExceedsMaxSizeException | FileExceedsStorageQuotaException femsx) {
                    logger.severe("One of the unzipped shape files exceeded the size limit, or the storage quota; giving up. " + femsx.getMessage());
                    datafiles.clear();
                    // (or should we throw an exception, instead of skipping it quietly?
                } catch (IOException ioex) {
                    throw new CommandExecutionException("Failed to process one of the components of the unpacked shape file", ioex, this);
                    // todo? - maybe try to provide a more detailed explanation, of which repackaged component, etc.?
                }

                // Delete the temp directory used for unzipping
                // The try-catch is due to error encountered in using NFS for stocking file,
                // cf. https://github.com/IQSS/dataverse/issues/5909
                try {
                    if (rezipFolder!=null)
                        FileUtils.deleteDirectory(rezipFolder);
                } catch (IOException ioex) {
                    // do nothing - it's a temp folder.
                    logger.warning("Could not remove temp folder, error message : " + ioex.getMessage());
                }

                if (!datafiles.isEmpty()) {
                    // remove the uploaded zip file:
                    try {
                        Files.delete(tempFile);
                    } catch (IOException ioex) {
                        // ignore - it's just a temp file - but let's log a warning
                        logger.warning("Could not remove temp file " + tempFile.getFileName().toString());
                    } catch (SecurityException se) {
                        // same
                        logger.warning("Unable to delete: " + tempFile.toString() + "due to Security Exception: "
                                + se.getMessage());
                    }
                    // update the quota object: 
                    if (quota != null) {
                        quota.setTotalUsageInBytes(quota.getTotalUsageInBytes() + combinedRezippedFileSize);
                    }
                    return CreateDataFileResult.success(fileName, finalType, datafiles);
                } else {
                    logger.severe("No files added from directory of rezipped shapefiles");
                }
                return CreateDataFileResult.error(fileName, finalType);

            } else if (finalType.equalsIgnoreCase(BagItFileHandler.FILE_TYPE)) {
                
                try { 
                    Optional<BagItFileHandler> bagItFileHandler = CDI.current().select(BagItFileHandlerFactory.class).get().getBagItFileHandler();
                    if (bagItFileHandler.isPresent()) {
                        CreateDataFileResult result = bagItFileHandler.get().handleBagItPackage(ctxt.systemConfig(), version, fileName, tempFile.toFile());
                        return result;
                    }
                } catch (IOException ioex) {
                    throw new CommandExecutionException("Failed to process uploaded BagIt file", ioex, this);
                }
            }
            
            // These are the final File and its size that will be used to 
            // add create a single Datafile: 
            
            newFile = tempFile.toFile();
            fileSize = newFile.length();
            
        } else {
            // Direct upload.
            
            finalType = StringUtils.isBlank(suppliedContentType) ? FileUtil.MIME_TYPE_UNDETERMINED_DEFAULT : suppliedContentType;
            
            // Since this is a direct upload, and therefore no temp file associated 
            // with it, we may, OR MAY NOT know the size of the file. If this is 
            // a direct upload via the UI, the page must have already looked up 
            // the size, after the client confirmed that the upload had completed. 
            // (so that we can reject the upload here, i.e. before the user clicks
            // save, if it's over the size limit or storage quota). However, if 
            // this is a direct upload via the API, we will wait until the 
            // upload is finalized in the saveAndAddFiles method to enforce the 
            // limits. 
            if (newFileSize != null) {
                fileSize = newFileSize;
                
                // if the size is specified, and it's above the individual size 
                // limit for this store, we can reject it now:
                if (fileSizeLimit != null && fileSize > fileSizeLimit) {
                    throw new CommandExecutionException(MessageFormat.format(BundleUtil.getStringFromBundle("file.addreplace.error.file_exceeds_limit"), bytesToHumanReadable(fileSize), bytesToHumanReadable(fileSizeLimit)), this);
                }
            }
            
        }
        
        // Finally, if none of the special cases above were applicable (or 
        // if we were unable to unpack an uploaded file, etc.), we'll just 
        // create and return a single DataFile:
        
        
        // We have already checked that this file does not exceed the individual size limit; 
        // but if we are processing it as is, as a single file, we need to check if 
        // its size does not go beyond the allocated storage quota (if specified):
        
        if (storageQuotaLimit != null && fileSize > storageQuotaLimit) {
            if (newFile != null) {
                // Remove the temp. file, if this is a non-direct upload. 
                // If this is a direct upload, it will be a responsibility of the 
                // component calling the command to remove the file that may have
                // already been saved in the S3 volume. 
                try {
                    newFile.delete();
                } catch (Exception ex) {
                    // ignore - but log a warning
                    logger.warning("Could not remove temp file " + tempFile.getFileName());
                }
            }
            throw new CommandExecutionException(MessageFormat.format(BundleUtil.getStringFromBundle("file.addreplace.error.quota_exceeded"), bytesToHumanReadable(fileSize), bytesToHumanReadable(storageQuotaLimit)), this);
        } 
        
        DataFile datafile = FileUtil.createSingleDataFile(version, newFile, newStorageIdentifier, fileName, finalType, newCheckSumType, newCheckSum);

        if (datafile != null) {
            if (newStorageIdentifier != null) {
                // Direct upload case
                // Improve the MIMEType
                // Need the owner for the StorageIO class to get the file/S3 path from the
                // storageIdentifier
                // Currently owner is null, but using this flag will avoid making changes here
                // if that isn't true in the future
                boolean ownerSet = datafile.getOwner() != null;
                if (!ownerSet) {
                    datafile.setOwner(version.getDataset());
                }
                String type = determineRemoteFileType(datafile, fileName);
                if (!StringUtils.isBlank(type)) {
                    // Use rules for deciding when to trust browser supplied type
                    if (useRecognizedType(finalType, type)) {
                        datafile.setContentType(type);
                    }
                    logger.fine("Supplied type: " + suppliedContentType + ", finalType: " + finalType);
                }
                // Avoid changing
                if (!ownerSet) {
                    datafile.setOwner(null);
                }
            }

            if (warningMessage != null) {
                createIngestFailureReport(datafile, warningMessage);
                datafile.SetIngestProblem();
            }
            if (datafile.getFilesize() < 0) {
                datafile.setFilesize(fileSize);
            }
            datafiles.add(datafile);

            // Update the quota definition for the *current upload session*
            // This is relevant for the uploads going through the UI page 
            // (where there may be an appreciable amount of time between the user
            // uploading the files and clicking "save". The file size should be 
            // available here for both direct and local uploads via the UI. 
            // It is not yet available if this is direct-via-API - but 
            // for API uploads the quota check will be enforced during the final 
            // save. 
            if (fileSize > 0 && quota != null) {
                logger.info("Setting total usage in bytes to " + (quota.getTotalUsageInBytes() + fileSize));
                quota.setTotalUsageInBytes(quota.getTotalUsageInBytes() + fileSize);
            }

            return CreateDataFileResult.success(fileName, finalType, datafiles);
        }

        return CreateDataFileResult.error(fileName, finalType);
    }   // end createDataFiles

    private static List<? extends ZipEntry> filteredZipEntries(ZipFile zipFile) {
        var entries = Collections.list(zipFile.entries()).stream().filter(e -> {
            var entryName = e.getName();
            logger.fine("ZipEntry, file: " + entryName);
            return !e.isDirectory() && !entryName.isEmpty() && !isFileToSkip(entryName);
        }).toList();
        return entries;
    }

    private static ZipFile openZipFile(Path tempFile, Charset charset) throws IOException {
        if (charset != null) {
            return new ZipFile(tempFile.toFile(), charset);
        }
        else {
            return new ZipFile(tempFile.toFile());
        }
    }

    private static boolean isFileToSkip(String fileName) {
        // check if it's a "fake" file - a zip archive entry
        // created for a MacOS X filesystem element: (these
        // start with "._")
        var shortName = getShortName(fileName);
        return shortName.startsWith("._") || shortName.startsWith(".DS_Store") || "".equals(shortName);
    }

    private static String getShortName(String fileName) {
        return fileName.replaceFirst("^.*[\\/]", "");
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        Map<String, Set<Permission>> ret = new HashMap<>();

        ret.put("", new HashSet<>());
        
        if (parentDataverse != null) {
            // The command is called in the context of uploading files on 
            // create of a new dataset
            ret.get("").add(Permission.AddDataset);
        } else {
            // An existing dataset
            ret.get("").add(Permission.EditDataset);
        }

        return ret;
    }
}
