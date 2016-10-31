package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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
    @EJB
    PermissionServiceBean permissionService;
    @Inject
    SwordAuth swordAuth;
    @Inject
    UrlManager urlManager;

    private HttpServletRequest httpRequest;

    @Override
    public MediaResource getMediaResourceRepresentation(String uri, Map<String, String> map, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {

        AuthenticatedUser user = swordAuth.auth(authCredentials);
        DataverseRequest dvReq = new DataverseRequest(user, httpRequest);
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
                 *
                 * This ticket is mostly about terms of use:
                 * https://github.com/IQSS/dataverse/issues/183
                 */
                boolean getMediaResourceRepresentationSupported = false;
                if (getMediaResourceRepresentationSupported) {
                    Dataverse dvThatOwnsDataset = dataset.getOwner();
                    /**
                     * @todo Add Dataverse 4 style permission check here. Is
                     * there a Command we use for downloading files as zip?
                     */
                    boolean authorized = false;
                    if (authorized) {
                        /**
                         * @todo Zip file download is being implemented in
                         * https://github.com/IQSS/dataverse/issues/338
                         */
                        InputStream fixmeInputStream = new ByteArrayInputStream("FIXME: replace with zip of all dataset files".getBytes());
                        String contentType = "application/zip";
                        String packaging = UriRegistry.PACKAGE_SIMPLE_ZIP;
                        boolean isPackaged = true;
                        MediaResource mediaResource = new MediaResource(fixmeInputStream, contentType, packaging, isPackaged);
                        return mediaResource;
                    } else {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "user " + user.getDisplayInfo().getTitle() + " is not authorized to get a media resource representation of the dataset with global ID " + dataset.getGlobalId());
                    }
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Downloading files via the SWORD-based Dataverse Data Deposit API is not (yet) supported: https://github.com/IQSS/dataverse/issues/183");
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
         * @todo: Perhaps create a new version of a dataset here?
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
        AuthenticatedUser user = swordAuth.auth(authCredentials);
        DataverseRequest dvReq = new DataverseRequest(user, httpRequest);
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
                        logger.fine("preparing to delete file id " + fileIdLong);
                        DataFile fileToDelete = dataFileService.find(fileIdLong);
                        if (fileToDelete != null) {
                            Dataset dataset = fileToDelete.getOwner();
                            SwordUtil.datasetLockCheck(dataset);
                            Dataset datasetThatOwnsFile = fileToDelete.getOwner();
                            Dataverse dataverseThatOwnsFile = datasetThatOwnsFile.getOwner();
                            /**
                             * @todo it would be nice to have this check higher
                             * up. Do we really need the file ID? Should the
                             * last argument to isUserAllowedOn be changed from
                             * "dataset" to "fileToDelete"?
                             */
                            UpdateDatasetCommand updateDatasetCommand = new UpdateDatasetCommand(dataset, dvReq, fileToDelete);
                            if (!permissionService.isUserAllowedOn(user, updateDatasetCommand, dataset)) {
                                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "User " + user.getDisplayInfo().getTitle() + " is not authorized to modify " + dataverseThatOwnsFile.getAlias());
                            }
                            try {
                                commandEngine.submit(updateDatasetCommand);
                            } catch (CommandException ex) {
                                throw SwordUtil.throwSpecialSwordErrorWithoutStackTrace(UriRegistry.ERROR_BAD_REQUEST, "Could not delete file: " + ex);
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
        AuthenticatedUser user = swordAuth.auth(authCredentials);
        DataverseRequest dvReq = new DataverseRequest(user, httpRequest);

        urlManager.processUrl(uri);
        String globalId = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("study") && globalId != null) {
            logger.fine("looking up dataset with globalId " + globalId);
            Dataset dataset = datasetService.findByGlobalId(globalId);
            if (dataset == null) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find dataset with global ID of " + globalId);
            }
            UpdateDatasetCommand updateDatasetCommand = new UpdateDatasetCommand(dataset, dvReq);
            if (!permissionService.isUserAllowedOn(user, updateDatasetCommand, dataset)) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "user " + user.getDisplayInfo().getTitle() + " is not authorized to modify dataset with global ID " + dataset.getGlobalId());
            }
            SwordUtil.datasetLockCheck(dataset);

            // Right now we are only supporting UriRegistry.PACKAGE_SIMPLE_ZIP but
            // in the future maybe we'll support other formats? Rdata files? Stata files?
            /**
             * @todo decide if we want non zip files to work. Technically, now
             * that we're letting ingestService.createDataFiles unpack the zip
             * for us, the following *does* work:
             *
             * curl--data-binary @path/to/trees.png -H "Content-Disposition:
             * filename=trees.png" -H "Content-Type: image/png" -H "Packaging:
             * http://purl.org/net/sword/package/SimpleZip"
             *
             * We *might* want to continue to force API users to only upload zip
             * files so that some day we can support a including a file or files
             * that contain the metadata (i.e. description) for each file in the
             * zip: https://github.com/IQSS/dataverse/issues/723
             */
            if (!deposit.getPackaging().equals(UriRegistry.PACKAGE_SIMPLE_ZIP)) {
                throw new SwordError(UriRegistry.ERROR_CONTENT, 415, "Package format " + UriRegistry.PACKAGE_SIMPLE_ZIP + " is required but format specified in 'Packaging' HTTP header was " + deposit.getPackaging());
            }

            String uploadedZipFilename = deposit.getFilename();
            DatasetVersion editVersion = dataset.getEditVersion();

            if (deposit.getInputStream() == null) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Deposit input stream was null.");
            }

            int bytesAvailableInInputStream = 0;
            try {
                bytesAvailableInInputStream = deposit.getInputStream().available();
            } catch (IOException ex) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not determine number of bytes available in input stream: " + ex);
            }

            if (bytesAvailableInInputStream == 0) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Bytes available in input stream was " + bytesAvailableInInputStream + ". Please check the file you are attempting to deposit.");
            }

            /**
             * @todo Think about if we should instead pass in "application/zip"
             * rather than letting ingestService.createDataFiles() guess the
             * contentType by passing it "null". See also the note above about
             * SimpleZip vs. other contentTypes.
             */
            String guessContentTypeForMe = null;
            List<DataFile> dataFiles = new ArrayList<>();
            try {
                try {
                    dataFiles = ingestService.createDataFiles(editVersion, deposit.getInputStream(), uploadedZipFilename, guessContentTypeForMe);
                } catch (EJBException ex) {
                    Throwable cause = ex.getCause();
                    if (cause != null) {
                        if (cause instanceof IllegalArgumentException) {
                            /**
                             * @todo should be safe to remove this catch of
                             * EJBException and IllegalArgumentException once
                             * this ticket is resolved:
                             *
                             * IllegalArgumentException: MALFORMED when
                             * uploading certain zip files
                             * https://github.com/IQSS/dataverse/issues/1021
                             */
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Exception caught calling ingestService.createDataFiles. Problem with zip file, perhaps: " + cause);
                        } else {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Exception caught calling ingestService.createDataFiles: " + cause);
                        }
                    } else {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Exception caught calling ingestService.createDataFiles. No cause: " + ex.getMessage());
                    }
                }
            } catch (IOException ex) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to add file(s) to dataset: " + ex.getMessage());
            }
            if (!dataFiles.isEmpty()) {
                Set<ConstraintViolation> constraintViolations = editVersion.validate();
                if (constraintViolations.size() > 0) {
                    ConstraintViolation violation = constraintViolations.iterator().next();
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to add file(s) to dataset: " + violation.getMessage() + " The invalid value was \"" + violation.getInvalidValue() + "\".");
                } else {
                    ingestService.addFiles(editVersion, dataFiles);
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "No files to add to dataset. Perhaps the zip file was empty.");
            }

            try {
                dataset = commandEngine.submit(updateDatasetCommand);
            } catch (CommandException ex) {
                throw returnEarly("Couldn't update dataset " + ex);
            } catch (EJBException ex) {
                /**
                 * @todo stop bothering to catch an EJBException once this has
                 * been implemented:
                 *
                 * Have commands catch ConstraintViolationException and turn
                 * them into something that inherits from CommandException ·
                 * https://github.com/IQSS/dataverse/issues/1009
                 */
                Throwable cause = ex;
                StringBuilder sb = new StringBuilder();
                sb.append(ex.getLocalizedMessage());
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                    sb.append(cause + " ");
                    if (cause instanceof ConstraintViolationException) {
                        ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                        for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                            sb.append(" Invalid value \"").append(violation.getInvalidValue()).append("\" for ")
                                    .append(violation.getPropertyPath()).append(" at ")
                                    .append(violation.getLeafBean()).append(" - ")
                                    .append(violation.getMessage());
                        }
                    }
                }
                throw returnEarly("EJBException: " + sb.toString());
            }

            ingestService.startIngestJobs(dataset, user);

            ReceiptGenerator receiptGenerator = new ReceiptGenerator();
            String baseUrl = urlManager.getHostnamePlusBaseUrlPath(uri);
            DepositReceipt depositReceipt = receiptGenerator.createDatasetReceipt(baseUrl, dataset);
            return depositReceipt;
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to determine target type or identifier from URL: " + uri);
        }
    }

    /**
     * @todo get rid of this method
     */
    private SwordError returnEarly(String error) {
        SwordError swordError = new SwordError(error);
        StackTraceElement[] emptyStackTrace = new StackTraceElement[0];
        swordError.setStackTrace(emptyStackTrace);
        return swordError;
    }

    public void setHttpRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

}
