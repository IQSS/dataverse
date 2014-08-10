package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.MediaResource;
import org.swordapp.server.MediaResourceManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

public class MediaResourceManagerImpl implements MediaResourceManager {

    private static final Logger logger = Logger.getLogger(MediaResourceManagerImpl.class.getCanonicalName());
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataFileServiceBean dataFileService;
    @EJB
    IngestServiceBean ingestService;
    @Inject
    SwordAuth swordAuth;
    @Inject
    UrlManager urlManager;

    @Override
    public MediaResource getMediaResourceRepresentation(String uri, Map<String, String> map, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {

        AuthenticatedUser authUser = swordAuth.auth(authCredentials);
        urlManager.processUrl(uri);
        String globalId = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("study") && globalId != null) {
            logger.fine("looking up dataset with globalId " + globalId);
            Dataset dataset = datasetService.findByGlobalId(globalId);
            if (dataset != null) {
                /**
                 * @todo: support downloading of files (SWORD 2.0 Profile 6.4. -
                 * Retrieving the content)
                 * http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#protocoloperations_retrievingcontent
                 * https://redmine.hmdc.harvard.edu/issues/3595
                 */
                boolean getMediaResourceRepresentationSupported = false;
                if (getMediaResourceRepresentationSupported) {
                    Dataverse dvThatOwnsStudy = dataset.getOwner();
                    if (swordAuth.hasAccessToModifyDataverse(authUser, dvThatOwnsStudy)) {
                        InputStream fixmeInputStream = new ByteArrayInputStream("FIXME: replace with zip of all dataset files".getBytes());
                        String contentType = "application/zip";
                        String packaging = UriRegistry.PACKAGE_SIMPLE_ZIP;
                        boolean isPackaged = true;
                        MediaResource mediaResource = new MediaResource(fixmeInputStream, contentType, packaging, isPackaged);
                        return mediaResource;
                    } else {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "user " + authUser.getDisplayInfo().getTitle()+ " is not authorized to get a media resource representation of the dataset with global ID " + dataset.getGlobalId());
                    }
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Downloading files via the SWORD-based Dataverse Data Deposit API is not (yet) supported: https://redmine.hmdc.harvard.edu/issues/3595");
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Couldn't find dataset with global ID of " + globalId);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Couldn't dermine target type or identifier from URL: " + uri);
        }
    }

    @Override
    public DepositReceipt replaceMediaResource(String uri, Deposit deposit, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        /**
         * @todo: Perhaps create a new version of a study here?
         *
         * "The server MUST effectively replace all the existing content in the
         * item, although implementations may choose to provide versioning or
         * some other mechanism for retaining the overwritten content." --
         * http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#protocoloperations_editingcontent_binary
         *
         * Also, if you enable this method, think about the SwordError currently
         * being returned by replaceOrAddFiles with shouldReplace set to true
         * and an empty zip uploaded. If no files are unzipped the user will see
         * a error about this but the files will still be deleted!
         */
        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Replacing the files of a dataset is not supported. Please delete and add files separately instead.");
    }

    @Override
    public void deleteMediaResource(String uri, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        AuthenticatedUser authUser = swordAuth.auth(authCredentials);
        urlManager.processUrl(uri);
        String targetType = urlManager.getTargetType();
        String fileId = urlManager.getTargetIdentifier();
        if (targetType != null && fileId != null) {
            if ("file".equals(targetType)) {
                String fileIdString = urlManager.getTargetIdentifier();
                if (fileIdString != null) {
                    Long fileIdLong;
                    try {
                        fileIdLong = Long.valueOf(fileIdString);
                    } catch (NumberFormatException ex) {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "File id must be a number, not '" + fileIdString + "'. URL was: " + uri);
                    }
                    if (fileIdLong != null) {
                        logger.info("preparing to delete file id " + fileIdLong);
                        DataFile fileToDelete = dataFileService.find(fileIdLong);
                        if (fileToDelete != null) {
                            Dataset dataset = fileToDelete.getOwner();
                            SwordUtil.datasetLockCheck(dataset);
                            Dataset datasetThatOwnsFile = fileToDelete.getOwner();
                            Dataverse dataverseThatOwnsFile = datasetThatOwnsFile.getOwner();
                            if (swordAuth.hasAccessToModifyDataverse(authUser, dataverseThatOwnsFile)) {
                                try {
                                    /**
                                     * @todo with only one command, should we be
                                     * falling back on the permissions system to
                                     * enforce if the user can delete a file or
                                     * not. If we do, a 403 Forbidden is
                                     * returned. For now, we'll have belt and
                                     * suspenders and do our normal sword auth
                                     * check.
                                     */
                                    commandEngine.submit(new DeleteDataFileCommand(fileToDelete, authUser));
                                } catch (CommandException ex) {
                                    throw SwordUtil.throwSpecialSwordErrorWithoutStackTrace(UriRegistry.ERROR_BAD_REQUEST, "Could not delete file: " + ex);
                                }
                            } else {
                                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "User " + authUser.getDisplayInfo().getTitle()+ " is not authorized to modify " + dataverseThatOwnsFile.getAlias());
                            }
                        } else {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to find file id " + fileIdLong + " from URL: " + uri);
                        }
                    } else {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to find file id in URL: " + uri);
                    }
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not file file to delete in URL: " + uri);
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unsupported file type found in URL: " + uri);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Target or identifer not specified in URL: " + uri);
        }
    }

    @Override
    public DepositReceipt addResource(String uri, Deposit deposit, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        boolean shouldReplace = false;
        return replaceOrAddFiles(uri, deposit, authCredentials, swordConfiguration, shouldReplace);
    }

    DepositReceipt replaceOrAddFiles(String uri, Deposit deposit, AuthCredentials authCredentials, SwordConfiguration swordConfiguration, boolean shouldReplace) throws SwordError, SwordAuthException, SwordServerException {
        AuthenticatedUser vdcUser = swordAuth.auth(authCredentials);

        urlManager.processUrl(uri);
        String globalId = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("study") && globalId != null) {
            logger.fine("looking up study with globalId " + globalId);
            Dataset dataset = datasetService.findByGlobalId(globalId);
            if (dataset == null) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find study with global ID of " + globalId);
            }
            SwordUtil.datasetLockCheck(dataset);
//            DatasetLock datasetLock = dataset.getDatasetLock();
//            if (datasetLock != null) {
//                String message = "Unable to delete file due to dataset lock (" + datasetLock.getInfo() + "). Please try again later.";
//                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, message);
//            }
            Dataverse dvThatOwnsDataset = dataset.getOwner();
            if (!swordAuth.hasAccessToModifyDataverse(vdcUser, dvThatOwnsDataset)) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "user " + vdcUser.getDisplayInfo().getTitle() + " is not authorized to modify dataset with global ID " + dataset.getGlobalId());
            }

            // Right now we are only supporting UriRegistry.PACKAGE_SIMPLE_ZIP but
            // in the future maybe we'll support other formats? Rdata files? Stata files?
            if (!deposit.getPackaging().equals(UriRegistry.PACKAGE_SIMPLE_ZIP)) {
                throw new SwordError(UriRegistry.ERROR_CONTENT, 415, "Package format " + UriRegistry.PACKAGE_SIMPLE_ZIP + " is required but format specified in 'Packaging' HTTP header was " + deposit.getPackaging());
            }

            String uploadedZipFilename = deposit.getFilename();
            ZipInputStream ziStream = new ZipInputStream(deposit.getInputStream());
            ZipEntry zEntry;

            DatasetVersion editVersion = dataset.getEditVersion();
            List<DataFile> newFiles = new ArrayList<>();
            try {
                // copied from createStudyFilesFromZip in AddFilesPage
                while ((zEntry = ziStream.getNextEntry()) != null) {
                    // Note that some zip entries may be directories - we 
                    // simply skip them:
                    if (!zEntry.isDirectory()) {

                        String finalFileName = "UNKNOWN";
                        if (zEntry.getName() != null) {
                            String zentryFilename = zEntry.getName();
                            int ind = zentryFilename.lastIndexOf('/');

                            String dirName = "";
                            if (ind > -1) {
                                finalFileName = zentryFilename.substring(ind + 1);
                                if (ind > 0) {
                                    dirName = zentryFilename.substring(0, ind);
                                    dirName = dirName.replace('/', '-');
                                }
                            } else {
                                finalFileName = zentryFilename;
                            }

                        }

                        // skip junk files
                        if (".DS_Store".equals(finalFileName)) {
                            continue;
                        }

                        // http://superuser.com/questions/212896/is-there-any-way-to-prevent-a-mac-from-creating-dot-underscore-files
                        if (finalFileName.startsWith("._")) {
                            continue;
                        }

                        /**
                         * @todo confirm that this DVN 3.x zero-length file
                         * check that was put in because of
                         * https://redmine.hmdc.harvard.edu/issues/3273 is done
                         * in the back end, if it's still important in 4.0.
                         */
                        // We now have the unzipped file saved in the upload directory;
                        // zero-length dta files (for example) are skipped during zip
                        // upload in the GUI, so we'll skip them here as well
//                        if (tempUploadedFile.length() != 0) {
                        /**
                         * @todo set the category (or categories) for files once
                         * we can: https://redmine.hmdc.harvard.edu/issues/3717
                         */
                        // And, if this file was in a legit (non-null) directory, 
                        // we'll use its name as the file category: 
//                        tempFileBean.getFileMetadata().setCategory(dirName);
                        String guessContentTypeForMe = null;
                        DataFile dFile = ingestService.createDataFile(editVersion, ziStream, finalFileName, guessContentTypeForMe);
                        newFiles.add(dFile);
                    } else {
                        logger.fine("directory found: " + zEntry.getName());
                    }
                }
            } catch (IOException ex) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Problem with file: " + uploadedZipFilename);
            } catch (EJBException ex) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to add file(s) to dataset: " + ex.getMessage());
            }

            if (newFiles.isEmpty()) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Problem with zip file '" + uploadedZipFilename + "'. Number of files unzipped: " + newFiles.size());
            }

            ingestService.addFiles(editVersion, newFiles);

            Command<Dataset> cmd;
            cmd = new UpdateDatasetCommand(dataset, vdcUser);
            try {
                dataset = commandEngine.submit(cmd);
            } catch (CommandException ex) {
                throw returnEarly("couldn't update dataset");
            } catch (EJBException ex) {
                Throwable cause = ex;
                StringBuilder sb = new StringBuilder();
                sb.append(ex.getLocalizedMessage());
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                    sb.append(cause + " ");
                    if (cause instanceof ConstraintViolationException) {
                        ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                        for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                            sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ")
                                    .append(violation.getPropertyPath()).append(" at ")
                                    .append(violation.getLeafBean()).append(" - ")
                                    .append(violation.getMessage());
                        }
                    }
                }
                throw returnEarly("EJBException: " + sb.toString());
            }

            ingestService.startIngestJobs(dataset, vdcUser);

            ReceiptGenerator receiptGenerator = new ReceiptGenerator();
            String baseUrl = urlManager.getHostnamePlusBaseUrlPath(uri);
            DepositReceipt depositReceipt = receiptGenerator.createReceipt(baseUrl, dataset);
            return depositReceipt;
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to determine target type or identifier from URL: " + uri);
        }
    }

    /**
     * @todo This validation was in DVN 3.x and should go into the 4.0 ingest
     * service
     */
    // copied from AddFilesPage
//    private void validateFileName(List<String> existingFilenames, String fileName, Study study) throws SwordError {
//        if (fileName.contains("\\")
//                || fileName.contains("/")
//                || fileName.contains(":")
//                || fileName.contains("*")
//                || fileName.contains("?")
//                || fileName.contains("\"")
//                || fileName.contains("<")
//                || fileName.contains(">")
//                || fileName.contains("|")
//                || fileName.contains(";")
//                || fileName.contains("#")) {
//            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Invalid File Name - cannot contain any of the following characters: \\ / : * ? \" < > | ; . Filename was '" + fileName + "'");
//        }
//        if (existingFilenames.contains(fileName)) {
//            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Filename " + fileName + " already exists in study " + study.getGlobalId());
//        }
//    }
    private SwordError returnEarly(String error) {
        SwordError swordError = new SwordError(error);
        StackTraceElement[] emptyStackTrace = new StackTraceElement[0];
        swordError.setStackTrace(emptyStackTrace);
        return swordError;
    }
}
