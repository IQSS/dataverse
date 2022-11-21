package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtil;
import edu.harvard.iq.dataverse.datafile.DataFileCreator;
import edu.harvard.iq.dataverse.datafile.FileService;
import edu.harvard.iq.dataverse.datafile.pojo.RsyncInfo;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnailService;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.datasetutility.VirusFoundException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestUtil;
import edu.harvard.iq.dataverse.license.TermsOfUseFormMapper;
import edu.harvard.iq.dataverse.license.TermsOfUseSelectItemsFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestRequest;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.TermsOfUseForm;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.provenance.ProvPopupFragmentBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.common.FileSizeUtil.bytesToHumanReadable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;


/**
 * @author Leonid Andreev
 */
@ViewScoped
@Named("EditDatafilesPage")
public class EditDatafilesPage implements java.io.Serializable {

    private static final long TEMP_VALID_TIME_MILLIS = 24 * 60 * 60 * 1000;

	private static final Logger logger = Logger.getLogger(EditDatafilesPage.class.getCanonicalName());

	private static final int NUMBER_OF_SCROLL_ROWS = 25;

    public enum FileEditMode {
        EDIT, UPLOAD, CREATE
    }

    private DatasetDao datasetDao;
    private DataFileServiceBean datafileDao;
    private DataFileCreator dataFileCreator;
    private PermissionServiceBean permissionService;
    private IngestServiceBean ingestService;
    private DataverseSession session;
    private SettingsServiceBean settingsService;
    private SystemConfig systemConfig;
    private DataverseRequestServiceBean dvRequestService;
    private PermissionsWrapper permissionsWrapper;
    private FileDownloadHelper fileDownloadHelper;
    private ProvPopupFragmentBean provPopupFragmentBean;
    private SettingsWrapper settingsWrapper;
    private DatasetVersionServiceBean datasetVersionService;
    private TermsOfUseFormMapper termsOfUseFormMapper;
    private TermsOfUseSelectItemsFactory termsOfUseSelectItemsFactory;
    private DatasetService datasetService;
    private FileService fileService;
    private DatasetThumbnailService datasetThumbnailService;
    private ImageThumbConverter imageThumbConverter;
    private DuplicatesService duplicatesService;

    private Dataset dataset = new Dataset();

    private String selectedFileIdsString = null;
    private FileEditMode mode = FileEditMode.EDIT;
    private List<FileMetadata> fileMetadatas = new ArrayList<>();


    private Long ownerId;
    private Long versionId;
    private List<DataFile> newFiles = new ArrayList<>();
    private List<DataFile> uploadedFiles = new ArrayList<>();
    private DataFileUploadInfo dataFileUploadInfo = new DataFileUploadInfo();
    private DatasetVersion workingVersion;
    private String dropBoxSelection = "";

    private String persistentId;

    private String versionString = "";

    private long currentBatchSize = 0L;

    private boolean saveEnabled = false;

    private Long maxFileUploadSizeInBytes = null;
    private Long multipleUploadFilesLimit = null;

    private List<SelectItem> termsOfUseSelectItems;
    private List<FileMetadata> selectedFiles;
    private List<DataFile> filesToBeDeleted = new ArrayList<>();

    private Boolean hasRsyncScript = false;

    /** The contents of the script. */
    private String rsyncScript = "";
    private String rsyncScriptFilename;
    private String warningMessageForPopUp;

    private String uploadWarningMessage = null;
    private String uploadSuccessMessage = null;
    private String uploadComponentId = null;

    private boolean uploadInProgress = false;

    private Map<String, String> temporaryThumbnailsMap = new HashMap<>();
    private Set<String> fileLabelsExisting = null;

    private Boolean lockedFromEditsVar;

    private FileMetadata fileMetadataSelectedForThumbnailPopup = null;
    private boolean alreadyDesignatedAsDatasetThumbnail = false;
    private FileMetadata selectedFile = null;
    private FileMetadata fileMetadataSelectedForIngestOptionsPopup = null;
    private String ingestLanguageEncoding = null;
    private String savedLabelsTempFile = null;

    private boolean hasDuplicates;
    private List<DuplicatesService.DuplicateGroup> duplicatesList = new ArrayList<>();
    private List<FileMetadata> filesTableBackup = new ArrayList<>();

    // -------------------- CONSTRUCTORS --------------------

    public EditDatafilesPage() { }

    @Inject
    public EditDatafilesPage(DatasetDao datasetDao, DataFileServiceBean datafileDao,
                             DataFileCreator dataFileCreator, PermissionServiceBean permissionService,
                             IngestServiceBean ingestService, DataverseSession session,
                             SettingsServiceBean settingsService, SystemConfig systemConfig,
                             DataverseRequestServiceBean dvRequestService, PermissionsWrapper permissionsWrapper,
                             FileDownloadHelper fileDownloadHelper, ProvPopupFragmentBean provPopupFragmentBean,
                             SettingsWrapper settingsWrapper, DatasetVersionServiceBean datasetVersionService,
                             TermsOfUseFormMapper termsOfUseFormMapper, TermsOfUseSelectItemsFactory termsOfUseSelectItemsFactory,
                             DatasetService datasetService, FileService fileService,
                             DatasetThumbnailService datasetThumbnailService, ImageThumbConverter imageThumbConverter,
                             DuplicatesService duplicatesService) {
        this.datasetDao = datasetDao;
        this.datafileDao = datafileDao;
        this.dataFileCreator = dataFileCreator;
        this.permissionService = permissionService;
        this.ingestService = ingestService;
        this.session = session;
        this.settingsService = settingsService;
        this.systemConfig = systemConfig;
        this.dvRequestService = dvRequestService;
        this.permissionsWrapper = permissionsWrapper;
        this.fileDownloadHelper = fileDownloadHelper;
        this.provPopupFragmentBean = provPopupFragmentBean;
        this.settingsWrapper = settingsWrapper;
        this.datasetVersionService = datasetVersionService;
        this.termsOfUseFormMapper = termsOfUseFormMapper;
        this.termsOfUseSelectItemsFactory = termsOfUseSelectItemsFactory;
        this.datasetService = datasetService;
        this.fileService = fileService;
        this.datasetThumbnailService = datasetThumbnailService;
        this.imageThumbConverter = imageThumbConverter;
        this.duplicatesService = duplicatesService;
    }

    // -------------------- GETTERS --------------------

    public String getSelectedFileIds() {
        return selectedFileIdsString;
    }

    public FileEditMode getMode() {
        return mode;
    }

    public Long getMaxFileUploadSizeInBytes() {
        return maxFileUploadSizeInBytes;
    }

    // The number of files the GUI user is allowed to upload in one batch,
    // via drag-and-drop, or through the file select dialog. Now configurable
    // in the Settings table.
    public Long getMaxNumberOfFiles() {
        return multipleUploadFilesLimit;
    }

    public String getGlobalId() {
        return persistentId;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public String getDropBoxSelection() {
        return dropBoxSelection;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setSelectedFileIds(String selectedFileIds) {
        selectedFileIdsString = selectedFileIds;
    }

    public DatasetVersion getWorkingVersion() {
        return workingVersion;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public long getCurrentBatchSize() {
        return currentBatchSize;
    }

    public List<FileMetadata> getSelectedFiles() {
        return selectedFiles;
    }

    public String getVersionString() {
        return versionString;
    }

    public Boolean isHasRsyncScript() {
        return hasRsyncScript;
    }

    public String getRsyncScript() {
        return rsyncScript;
    }

    public String getRsyncScriptFilename() {
        return rsyncScriptFilename;
    }

    public String getWarningMessageForPopUp() {
        return warningMessageForPopUp;
    }

    public FileMetadata getSelectedFile() {
        return selectedFile;
    }

    public FileMetadata getFileMetadataSelectedForIngestOptionsPopup() {
        return fileMetadataSelectedForIngestOptionsPopup;
    }

    public void setMode(FileEditMode mode) {
        this.mode = mode;
    }

    public List<SelectItem> getTermsOfUseSelectItems() {
        return termsOfUseSelectItems;
    }

    public boolean getHasDuplicates() {
        return hasDuplicates;
    }

    public List<DuplicatesService.DuplicateGroup> getDuplicatesList() {
        return duplicatesList;
    }

    // -------------------- LOGIC --------------------

    public List<FileMetadata> getFileMetadatas() {

        logger.fine(fileMetadatas != null
            ? String.format("Returning a list of %d file metadatas.", fileMetadatas.size())
            : "File metadatas list hasn't been initialized yet.");
        return fileMetadatas;
    }

    /**
     *   The method below is for setting up the p:dataTable component
     *   used to display the uploaded files, or the files selected for editing.
     *   It supplies the value of the component attribute "scrollable".
     *   When we have more than NUMBER_OF_SCROLL_ROWS worth of files (currently
     *   set to 25), we will add a scroller to the table, showing NUMBER_OF_SCROLL_ROWS
     *   at a time; thus making the page a little bit more usable.
     *   When there is fewer rows, however, the attribute needs to be set to
     *   false - because otherwise some (idiosyncratic) amount of white space
     *   is added to the bottom of the table, making the page look silly.
     */
    public boolean isScrollable() {
        return !(fileMetadatas == null || fileMetadatas.size() <= NUMBER_OF_SCROLL_ROWS + 1);
    }

    /**
     *  This may be null, signifying unlimited download size.
     */
    public String getHumanMaxFileUploadSize() {
        return getMaxFileUploadSizeInBytes() == null
                ? StringUtils.EMPTY
                : bytesToHumanReadable(getMaxFileUploadSizeInBytes());
    }

    public boolean isUnlimitedUploadFileSize() {
        return maxFileUploadSizeInBytes == null;
    }

    public String getHumanMaxBatchUploadSize() {
        Long batchSize = getMaxBatchSize();
        return batchSize == null || batchSize.equals(0L)
                ? StringUtils.EMPTY
                : bytesToHumanReadable(batchSize);
    }

    public void reset() { }

    public String getDropBoxKey() {
        // Site-specific DropBox application registration key is configured
        // via a JVM option under glassfish.
        // if (true) return "some-test-key";  // for debugging
        return settingsService.getValueForKey(Key.DropboxKey);
    }

    public String initCreateMode(DatasetVersion version, List<DataFile> newFilesList, List<FileMetadata> selectedFileMetadatasList) {
        logger.fine("Initializing Edit Files page in CREATE mode;");
        if (version == null) {
            return permissionsWrapper.notFound();
        }

        maxFileUploadSizeInBytes = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaxFileUploadSizeInBytes);
        multipleUploadFilesLimit = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MultipleUploadFilesLimit);
        workingVersion = version;
        dataset = version.getDataset();
        mode = FileEditMode.CREATE;
        newFiles = newFilesList;
        uploadedFiles = new ArrayList<>();
        selectedFiles = selectedFileMetadatasList;
        termsOfUseSelectItems = termsOfUseSelectItemsFactory.buildLicenseSelectItems();
        saveEnabled = true;
        return null;
    }


    public String init() {
        fileMetadatas = new ArrayList<>();
        newFiles = new ArrayList<>();
        uploadedFiles = new ArrayList<>();
        cleanupTempFiles();

        maxFileUploadSizeInBytes = settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes);
        multipleUploadFilesLimit = settingsService.getValueForKeyAsLong(Key.MultipleUploadFilesLimit);
        termsOfUseSelectItems = termsOfUseSelectItemsFactory.buildLicenseSelectItems();

        if (dataset.getId() != null) {
            // Set Working Version and Dataset by Dataset Id
            dataset = datasetDao.find(dataset.getId());
            // Is the Dataset harvested? (we don't allow editing of harvested files)
            if (dataset == null || dataset.isHarvested()) {
                return permissionsWrapper.notFound();
            }
        } else {
            return permissionsWrapper.notFound();
        }
        workingVersion = dataset.getEditVersion();

        if (!permissionsWrapper.canCurrentUserUpdateDataset(dataset)) {
            return permissionsWrapper.notAuthorized();
        }
        if (datasetDao.isInReview(dataset) && !permissionsWrapper.canUpdateAndPublishDataset(dataset)) {
            return permissionsWrapper.notAuthorized();
        }

        if (mode == FileEditMode.EDIT) {
            Set<Long> selectedFileIds = Arrays.stream(StringUtils.split(StringUtils.trimToEmpty(selectedFileIdsString), ','))
                    .map(NumberUtils::toLong)
                    .filter(fileId -> fileId != 0)
                    .collect(toSet());

            if (selectedFileIds.isEmpty()) {
                logger.fine("No numeric file ids supplied to the page, in the edit mode. Redirecting to the 404 page.");
                return permissionsWrapper.notFound();
            }

            logger.fine("The page is called with " + selectedFileIds.size() + " file ids.");

            populateFileMetadatas(selectedFileIds);
            setUpRsync();
            if (fileMetadatas.isEmpty()) {
                return permissionsWrapper.notFound();
            }
        }

        saveEnabled = true;
        if (mode == FileEditMode.UPLOAD && workingVersion.getFileMetadatas().isEmpty() && settingsWrapper.isRsyncUpload()) {
            setUpRsync();
        }
        return null;
    }

    public boolean isInUploadMode() {
        return mode == FileEditMode.UPLOAD;
    }

    public String getMultiUploadDetailsMessage() {
        return getStringFromBundle("dataset.message.uploadFilesSingle.message",
                systemConfig.getGuidesBaseUrl(session.getLocale()), systemConfig.getGuidesVersion());
    }

    public boolean isInstallationPublic() {
        return settingsService.isTrueForKey(SettingsServiceBean.Key.PublicInstall);
    }

    // This deleteFilesCompleted method is used in editFilesFragment.xhtml
    public void deleteFilesCompleted() { }

    public void deleteFiles() {
        logger.fine("entering bulk file delete (EditDataFilesPage)");

        String fileNames = selectedFiles.stream()
                .map(FileMetadata::getLabel)
                .collect(joining(", "));

        for (FileMetadata markedForDelete : getSelectedFiles()) {
            logger.fine(String.format("delete requested on file %s, file metadata id: %d, datafile id: %d, page is in edit mode %s",
                    markedForDelete.getLabel(), markedForDelete.getId(), markedForDelete.getDataFile().getId(), mode.name()));

            // Has this filemetadata been saved already? Or is it a brand new filemetadata, created as part of a brand
            // new version, created when the user clicked 'delete', that hasn't been saved in the db yet?
            if (!markedForDelete.isNew()) {
                logger.fine("this is a filemetadata from an existing draft version");
                // so all we remove is the file from the fileMetadatas (from the file metadatas attached to the
                // editVersion, and from the display list of file metadatas that are being edited) and let the delete be
                // handled in the command (by adding it to the filesToBeDeleted list):

                fileMetadatas.remove(markedForDelete);
                filesToBeDeleted.add(markedForDelete.getDataFile());
            } else {
                logger.fine("this is a brand-new (unsaved) filemetadata");
                // If the bean is in the 'CREATE' mode, the page is using dataset.getEditVersion().getFileMetadatas()
                // directly, so there's no need to delete this meta from the local fileMetadatas list. (but doing both
                // just adds a no-op and won't cause an error)

                // delete the filemetadata from the local display list and version
                removeFileMetadataFromList(fileMetadatas, markedForDelete);
                removeFileMetadataFromList(dataset.getEditVersion().getFileMetadatas(), markedForDelete);
            }

            if (markedForDelete.getDataFile().isNew()) {
                DataFile dataFileToDelete = markedForDelete.getDataFile();
                logger.fine("this is a brand new file.");
                // the file was just added during this step, so in addition to removing it from the fileMetadatas lists
                // (above), we also remove it from the newFiles list and the dataset's files, so it never gets saved.

                removeDataFileFromList(dataset.getFiles(), dataFileToDelete);
                removeDataFileFromList(newFiles, dataFileToDelete);
                deleteTempFile(dataFileToDelete);
                updateCurrentBatchSizeForDeletedDataFile(dataFileToDelete);
            }
        }
        logger.fine("Files was removed from the list - changes will persist after save changes will be executed");
        JsfHelper.addFlashSuccessMessage(getStringFromBundle("file.deleted.success", fileNames));
        hasDuplicates = hasDuplicatesInUploadedFiles(newFiles);
    }

    /**
     * The method is used to clean temporary files on various events
     * such as closing the upload tab or logging out.
     */
	@PreDestroy
	void cleanTempFilesOnViewDestroy() {
        newFiles.forEach(this::deleteTempFile);
        uploadedFiles.forEach(this::deleteTempFile);
    }

    public String save() {
        // Once all the filemetadatas pass the validation, we'll only allow the user to try to save once – this it to
        // prevent them from creating multiple DRAFT versions, if the page gets stuck in that state where it
        // successfully creates a new version, but can't complete the remaining tasks. -- L.A. 4.2

        if (!saveEnabled) {
            return StringUtils.EMPTY;
        }

        int oldFilesNumber = workingVersion.getFileMetadatas().size();
        int newFilesNumber = newFiles.size();
        int expectedFilesTotal = oldFilesNumber + newFilesNumber;

        if (newFilesNumber > 0) {
            // SEK 10/15/2018 only apply the following tests if dataset has already been saved.
            if (dataset.getId() != null) {
                Dataset lockTest = datasetDao.find(dataset.getId());
                // SEK 09/19/18 Get Dataset again to test for lock just in case the user downloads the rsync script via
                // the api while the edit files page is open and has already loaded a file in http upload for Dual Mode
                if (dataset.isLockedFor(DatasetLock.Reason.DcmUpload) || lockTest.isLockedFor(DatasetLock.Reason.DcmUpload)) {
                    logger.log(Level.INFO, "Couldn''t save dataset: {0}", "DCM script has been downloaded for " +
                            "this dataset. Additional files are not permitted.");
                    populateDatasetUpdateFailureMessage();
                    return null;
                }
                for (DatasetVersion version : lockTest.getVersions()) {
                    if (version.isHasPackageFile()) {
                        logger.log(Level.INFO, ResourceBundle.getBundle("Bundle").getString("file.api.alreadyHasPackageFile"));
                        populateDatasetUpdateFailureMessage();
                        return null;
                    }
                }
            }

            for (DataFile newFile : newFiles) {
                TermsOfUseForm termsOfUseForm = newFile.getFileMetadata().getTermsOfUseForm();
                FileTermsOfUse termsOfUse = termsOfUseFormMapper.mapToFileTermsOfUse(termsOfUseForm);
                newFile.getFileMetadata().setTermsOfUse(termsOfUse);
            }

            // Try to save the NEW files permanently:
            List<DataFile> filesAdded = ingestService.saveAndAddFilesToDataset(workingVersion, newFiles);

            // reset the working list of fileMetadatas, as to only include the ones
            // that have been added to the version successfully:
            fileMetadatas.clear();
            for (DataFile addedFile : filesAdded) {
                fileMetadatas.add(addedFile.getFileMetadata());
            }
        }

        if (settingsService.isTrueForKey(SettingsServiceBean.Key.ProvCollectionEnabled)) {
            provPopupFragmentBean.updatePageMetadatasWithProvFreeform(fileMetadatas);
            try {
                // Note that the user may have uploaded provenance metadata file(s) for some of the new files that have
                // since failed to be permanently saved in storage (in the ingestService.saveAndAddFilesToDataset()
                // step, above); these files have been dropped from the fileMetadatas list, and we are not adding them
                // to the dataset; but the provenance update set still has entries for these failed files, so we are
                // passing the fileMetadatas list to the saveStagedProvJson() method below - so that it doesn't attempt
                // to save the entries that are no longer valid.
                provPopupFragmentBean.saveStagedProvJson(false, fileMetadatas);
            } catch (AbstractApiBean.WrappedResponse ex) {
                JsfHelper.addFlashErrorMessage(getStringFromBundle("file.metadataTab.provenance.error"));
                Logger.getLogger(EditDatafilesPage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        logger.fine("issuing the dataset update command");
        Map<Long, String> deleteStorageLocations = null;

        if (!filesToBeDeleted.isEmpty()) {
            deleteStorageLocations = datafileDao.getPhysicalFilesToDelete(filesToBeDeleted);
        }

        Try<Dataset> updateDatasetOperation = Try.of(() -> datasetVersionService.updateDatasetVersion(workingVersion, filesToBeDeleted, true))
                .onSuccess(updatedDataset -> dataset = updatedDataset)
                .onFailure(ex -> {
                    logger.log(Level.SEVERE, "Couldn't update dataset with id: " + workingVersion.getDataset().getId(), ex);
                    populateDatasetUpdateFailureMessage();
                });

        if (updateDatasetOperation.isFailure()) {
            return StringUtils.EMPTY;
        }

        // Have we just deleted some draft datafiles successfully? Finalize the physical file deletes:
        // (DataFileService will double-check that the datafiles no longer exist in the database, before attempting to
        // delete the physical files)
        if (deleteStorageLocations != null) {
            datafileDao.finalizeFileDeletes(deleteStorageLocations);
        }

        saveEnabled = false;

        if (!newFiles.isEmpty()) {
            logger.fine("clearing newfiles list.");
            newFiles.clear();
        }

        workingVersion = dataset.getEditVersion();
        logger.fine("working version id: " + workingVersion.getId());

        int filesTotal = workingVersion.getFileMetadatas().size();
        if (newFilesNumber == 0 || filesTotal == expectedFilesTotal) {
            JsfHelper.addFlashSuccessMessage(getStringFromBundle("dataset.message.filesSuccess"));
        } else if (filesTotal == oldFilesNumber) {
            JsfHelper.addFlashErrorMessage(getStringFromBundle("dataset.message.addFiles.Failure"));
        } else {
            JsfHelper.addFlashWarningMessage(getStringFromBundle(
                    "dataset.message.addFiles.partialSuccess", filesTotal - oldFilesNumber, newFilesNumber));
        }

        // Call Ingest Service one more time to queue the data ingest jobs for asynchronous execution:
        if (mode == FileEditMode.UPLOAD) {
            ingestService.startIngestJobsForDataset(dataset, (AuthenticatedUser) session.getUser());
        }

        logger.fine("Redirecting to the dataset page, from the edit/upload page.");
        return returnToDraftVersion();
    }

    public String returnToDatasetOnly() {
        dataset = datasetDao.find(dataset.getId());
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId().asString() + "&faces-redirect=true";
    }

    public String getPageTitle(boolean datasetPage) {
        return datasetPage || showFileUploadFragment()
                ? getStringFromBundle("file.uploadFiles")
                : getStringFromBundle("file.editFiles") + " - " + workingVersion.getParsedTitle();
    }

    public String cancel() {
        uploadInProgress = false;
        // Files that have been finished and are now in the lower list on the page
        for (DataFile newFile : newFiles) {
            deleteTempFile(newFile);
        }
        // Files in the upload process but not yet finished
        for (DataFile newFile : uploadedFiles) {
            deleteTempFile(newFile);
        }
        if (workingVersion.getId() != null) {
            return returnToDraftVersion();
        }
        return returnToDatasetOnly();
    }

    public boolean allowMultipleFileUpload() {
        return true;
    }

    public boolean showFileUploadFragment() {
        return mode == FileEditMode.UPLOAD || mode == FileEditMode.CREATE;
    }

    public boolean showFileUploadComponent() {
        return mode == FileEditMode.UPLOAD || mode == FileEditMode.CREATE;
    }

    /**
     * Using information from the DropBox choose, ingest the chosen files
     * https://www.dropbox.com/developers/dropins/chooser/js
     */
    public void handleDropBoxUpload(ActionEvent event) throws IOException {
        if (!uploadInProgress) {
            uploadInProgress = true;
        }
        logger.fine("handleDropBoxUpload");
        uploadComponentId = event.getComponent().getClientId();

        // Read JSON object from the output of the DropBox Chooser:
        JsonReader dbJsonReader = Json.createReader(new StringReader(dropBoxSelection));
        JsonArray dbArray = dbJsonReader.readArray();
        dbJsonReader.close();

        // Iterate through the Dropbox file information (JSON)
        List<String> localWarningMessages = new ArrayList<>();
        for (int i = 0; i < dbArray.size(); i++) {
            JsonObject dbObject = dbArray.getJsonObject(i);

            // Parse information for a single file
            String fileLink = dbObject.getString("link");
            String fileName = dbObject.getString("name");
            int fileSize = dbObject.getInt("bytes");

            logger.fine("DropBox url: " + fileLink + ", filename: " + fileName + ", size: " + fileSize);

            // Check file size
            //  - Max size NOT specified in db: default is unlimited
            //  - Max size specified in db: check too make sure file is within limits
            if (!isUnlimitedUploadFileSize() && fileSize > getMaxFileUploadSizeInBytes()) {
                String warningMessage = "Dropbox file \"" + fileName + "\" exceeded the limit of " + fileSize + " bytes and was not uploaded.";
                localWarningMessages.add(warningMessage);
                continue;
            }

            GetMethod dropBoxMethod = new GetMethod(fileLink);
            List<DataFile> datafiles = new ArrayList<>();

            // Send it through the ingest service
            try (InputStream dropBoxStream = getDropBoxContent(dropBoxMethod)) {
                // Note: A single uploaded file may produce multiple datafiles -
                // for example, multiple files can be extracted from an uncompressed
                // zip file.
                datafiles = dataFileCreator.createDataFiles(dropBoxStream, fileName, "application/octet-stream");
            } catch (IOException | FileExceedsMaxSizeException ex) {
                logger.log(Level.SEVERE, "Error during ingest of DropBox file {0} from link {1}", new Object[]{fileName, fileLink});
                continue;
            } catch (VirusFoundException e) {
                localWarningMessages.add(getStringFromBundle("dataset.file.uploadScannerWarning"));
                continue;
            } finally {
                dropBoxMethod.releaseConnection();
            }

            uploadWarningMessage = processUploadedFileList(datafiles);
            logger.fine("Warning message during upload: " + uploadWarningMessage);

            if (!uploadInProgress) {
                logger.warning("Upload in progress cancelled");
                for (DataFile newFile : datafiles) {
                    deleteTempFile(newFile);
                }
            }
        }

        if (!localWarningMessages.isEmpty()) {
            uploadWarningMessage = uploadWarningMessage == null
                    ? StringUtils.join(localWarningMessages, "; ")
                    : uploadWarningMessage + "; " + StringUtils.join(localWarningMessages, "; ");
        }
    }

    public void downloadRsyncScript() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("application/download");

        String contentDispositionString = "attachment;filename=" + rsyncScriptFilename;
        response.setHeader("Content-Disposition", contentDispositionString);

        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(getRsyncScript().getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (IOException e) {
            String error = "Problem getting bytes from rsync script: " + e;
            logger.warning(error);
            return;
        }

        // If the script has been successfully downloaded, lock the dataset:
        String lockInfoMessage = "script downloaded";
        DatasetLock lock = datasetDao.addDatasetLock(dataset.getId(), DatasetLock.Reason.DcmUpload, session.getUser() != null ? ((AuthenticatedUser) session.getUser()).getId() : null, lockInfoMessage);
        if (lock != null) {
            dataset.addLock(lock);
        } else {
            logger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", dataset.getId());
        }
    }

    public void uploadFinished() {
        // This method is triggered from the page, by the <p:upload ... onComplete=... attribute.
        // Note that its behavior is different from that of of <p:upload ... onStart=...
        // that's triggered only once, even for a multiple file upload. In contrast,
        // onComplete=... gets executed for each of the completed multiple upload events.
        // So when you drag-and-drop a bunch of files, you CANNOT rely on onComplete=...
        // to notify the page when the batch finishes uploading! There IS a way
        // to detect ALL the current uploads completing: the p:upload widget has
        // the property "files", that contains the list of all the files currently
        // uploading; so checking on the size of the list tells you if any uploads
        // are still in progress. Once it's zero, you know it's all done.
        // This is super important - because if the user is uploading 1000 files
        // via drag-and-drop, you don't want to re-render the entire page each
        // time every single of the 1000 uploads finishes!
        // (check editFilesFragment.xhtml for the exact code handling this; and
        // http://stackoverflow.com/questions/20747201/when-multiple-upload-is-finished-in-pfileupload
        // for more info). -- 4.6
        logger.fine("upload finished");

        // Add the file(s) added during this last upload event, single or multiple,
        // to the full list of new files, and the list of filemetadatas
        // used to render the page:

        for (DataFile dataFile : uploadedFiles) {
            fileMetadatas.add(dataFile.getFileMetadata());
            newFiles.add(dataFile);
        }
        if (uploadInProgress) {
            uploadedFiles = new ArrayList<>();
            uploadInProgress = false;
        }
        // refresh the warning message below the upload component, if exists:
        if (uploadComponentId != null && uploadWarningMessage != null) {
            FacesContext.getCurrentInstance()
                    .addMessage(uploadComponentId, new FacesMessage(FacesMessage.SEVERITY_ERROR, getStringFromBundle("dataset.file.uploadWarning"), uploadWarningMessage));
        } else if (uploadComponentId != null && uploadSuccessMessage != null) {
            FacesContext.getCurrentInstance()
                    .addMessage(uploadComponentId, new FacesMessage(FacesMessage.SEVERITY_INFO, getStringFromBundle("dataset.file.uploadWorked"), uploadSuccessMessage));
        }

        uploadWarningMessage = null;
        uploadSuccessMessage = null;

        hasDuplicates = hasDuplicatesInUploadedFiles(newFiles);
    }

    public Long getMaxBatchSize() {
        return settingsService.getValueForKeyAsLong(Key.SingleUploadBatchMaxSize);
    }

    /**
     * Handle native file replace
     */
    public void handleFileUpload(FileUploadEvent event) throws IOException {
        if (!uploadInProgress) {
            uploadInProgress = true;
        }

        UploadedFile uploadedFile = event.getFile();

        long fileSize = uploadedFile.getSize();
        if (getMaxBatchSize() > 0 && (currentBatchSize + fileSize) > getMaxBatchSize()) {
            uploadWarningMessage = getStringFromBundle("dataset.file.uploadBatchTooBig");
            uploadComponentId = event.getComponent().getClientId();
            return;
        }
        currentBatchSize += fileSize;

        List<DataFile> dFileList;

        try (InputStream inputStream = uploadedFile.getInputStream()) {
            // Note: A single uploaded file may produce multiple datafiles -
            // for example, multiple files can be extracted from an uncompressed
            // zip file.
            dFileList = dataFileCreator.createDataFiles(inputStream, uploadedFile.getFileName(), uploadedFile.getContentType());
            dataFileUploadInfo.addSizeAndDataFiles(fileSize, dFileList);
        } catch (IOException | FileExceedsMaxSizeException ex) {
            logger.warning("Failed to process and/or save the file " + uploadedFile.getFileName() + "; " + ex.getMessage());
            return;
        } catch (VirusFoundException e) {
            uploadWarningMessage = getStringFromBundle("dataset.file.uploadScannerWarning");
            uploadComponentId = event.getComponent().getClientId();
            return;
        }

        // These raw datafiles are then post-processed, in order to drop any files
        // already in the dataset/already uploaded, and to correct duplicate file names, etc.
        String warningMessage = processUploadedFileList(dFileList);

        if (warningMessage != null) {
            uploadWarningMessage = warningMessage;
            // save the component id of the p:upload widget, so that we could
            // send an info message there, from elsewhere in the code:
            uploadComponentId = event.getComponent().getClientId();
        }
        if (!uploadInProgress) {
            logger.warning("Upload in progress cancelled");
            for (DataFile newFile : dFileList) {
                deleteTempFile(newFile);
            }
        }
    }

    public boolean isTemporaryPreviewAvailable(String fileSystemId, String mimeType) {
        if (temporaryThumbnailsMap.get(fileSystemId) != null && !temporaryThumbnailsMap.get(fileSystemId).isEmpty()) {
            return true;
        }

        if ("".equals(temporaryThumbnailsMap.get(fileSystemId))) {
            // we've already looked once - and there's no thumbnail.
            return false;
        }

        String filesRootDirectory = systemConfig.getFilesDirectory();
        String fileSystemName = filesRootDirectory + "/temp/" + fileSystemId;
        String imageThumbFileName = fileSystemName + ".thumb" + ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE;

        // ATTENTION! TODO: the current version of the method below may not be checking if files are already cached!
        if ("application/pdf".equals(mimeType)) {
            imageThumbConverter.generatePDFThumbnailFromFile(fileSystemName, ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE, imageThumbFileName);
        } else if (mimeType != null && mimeType.startsWith("image/")) {
            imageThumbConverter.generateImageThumbnailFromFile(fileSystemName, ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE, imageThumbFileName);
        }

        File imageThumbFile = new File(imageThumbFileName);
        if (imageThumbFile.exists()) {
            String previewAsBase64 = imageThumbConverter.getImageAsBase64FromFile(imageThumbFile);
            if (previewAsBase64 != null) {
                temporaryThumbnailsMap.put(fileSystemId, previewAsBase64);
                return true;
            } else {
                temporaryThumbnailsMap.put(fileSystemId, "");
            }
        }
        return false;
    }

    public String getTemporaryPreviewAsBase64(String fileSystemId) {
        return temporaryThumbnailsMap.get(fileSystemId);
    }

    public boolean isLocked() {
        if (dataset != null) {
            logger.log(Level.FINE, "checking lock status of dataset {0}", dataset.getId());
            Dataset lookedupDataset = datasetDao.find(dataset.getId());
            if (lookedupDataset != null && lookedupDataset.isLocked()) {
                logger.fine("locked!");
                return true;
            }
        }
        return false;
    }

    public boolean isThumbnailAvailable(FileMetadata fileMetadata) {
        // new and optimized logic:
        // - check download permission here (should be cached - so it's free!)
        // - only then ask the file service if the thumbnail is available/exists.
        // the service itself no longer checks download permissions.
        return fileDownloadHelper.canUserDownloadFile(fileMetadata)
                && datafileDao.isThumbnailAvailable(fileMetadata.getDataFile());
    }

    public boolean isLockedFromEdits() {
        if (null == lockedFromEditsVar) {
            try {
                permissionService.checkEditDatasetLock(dataset, dvRequestService.getDataverseRequest(),
                        new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromEditsVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromEditsVar = true;
            }
        }
        return lockedFromEditsVar;
    }

    // Methods for edit functions that are performed on one file at a time,
    // in popups that block the rest of the page:

    public boolean isDesignatedDatasetThumbnail(FileMetadata fileMetadata) {
        return fileMetadata != null && fileMetadata.getDataFile() != null && fileMetadata.getDataFile().getId() != null
                && fileMetadata.getDataFile().equals(dataset.getThumbnailFile());
    }

    /**
     * @todo For consistency, we should disallow users from setting the
     * thumbnail to a restricted file. We enforce this rule in the newer
     * workflow in dataset-widgets.xhtml. The logic to show the "Set Thumbnail"
     * button is in editFilesFragment.xhtml and it would be nice to move it to
     * Java since it's getting long and a bit complicated.
     */
    public void setFileMetadataSelectedForThumbnailPopup(FileMetadata fm) {
        fileMetadataSelectedForThumbnailPopup = fm;
        alreadyDesignatedAsDatasetThumbnail = getUseAsDatasetThumbnail();
    }

    public void clearFileMetadataSelectedForThumbnailPopup() {
        fileMetadataSelectedForThumbnailPopup = null;
    }

    public boolean getUseAsDatasetThumbnail() {
        return isDesignatedDatasetThumbnail(fileMetadataSelectedForThumbnailPopup);
    }

    public void setUseAsDatasetThumbnail(boolean useAsThumbnail) {
        if (fileMetadataSelectedForThumbnailPopup != null && fileMetadataSelectedForThumbnailPopup.getDataFile() != null) {
            if (useAsThumbnail) {
                dataset.setThumbnailFile(fileMetadataSelectedForThumbnailPopup.getDataFile());
            } else if (getUseAsDatasetThumbnail()) {
                dataset.setThumbnailFile(null);
            }
        }
    }

    public void saveAsDesignatedThumbnail() {
        logger.fine("saving as the designated thumbnail");
        // We don't need to do anything specific to save this setting, because
        // the setUseAsDatasetThumbnail() method, above, has already updated the
        // file object appropriately.
        // However, once the "save" button is pressed, we want to show a success message, if this is
        // a new image has been designated as such:
        if (getUseAsDatasetThumbnail() && !alreadyDesignatedAsDatasetThumbnail) {
            String successMessage = getStringFromBundle("file.assignedDataverseImage.success",
                    fileMetadataSelectedForThumbnailPopup.getLabel());
            logger.fine(successMessage);
            JsfHelper.addFlashSuccessMessage(successMessage);
        }

        // And reset the selected fileMetadata:
        fileMetadataSelectedForThumbnailPopup = null;
    }

    public void deleteDatasetLogoAndUseThisDataFileAsThumbnailInstead() {
        logger.log(Level.FINE, "For dataset id {0} the current thumbnail is from a dataset logo rather than a dataset file, blowing away the logo and using this FileMetadata id instead: {1}", new Object[]{dataset.getId(), fileMetadataSelectedForThumbnailPopup});

        Try.of(() -> datasetService.changeDatasetThumbnail(dataset, fileMetadataSelectedForThumbnailPopup.getDataFile()))
                .onFailure(ex -> logger.log(Level.SEVERE, "Problem setting thumbnail for dataset id " + dataset.getId(), ex))
                .onSuccess(datasetThumbnail -> dataset = datasetDao.find(dataset.getId()));
    }

    public boolean isThumbnailIsFromDatasetLogoRatherThanDatafile() {
        DatasetThumbnail datasetThumbnail = datasetThumbnailService.getThumbnail(dataset);
        return datasetThumbnail != null && !datasetThumbnail.isFromDataFile();
    }

    public void refreshTagsPopUp(FileMetadata fm) {
        setSelectedFile(fm);
    }

    public void saveFileTagsAndCategories(FileMetadata selectedFile,
                                          Collection<String> selectedFileMetadataTags,
                                          Collection<String> selectedDataFileTags) {
        selectedFile.getCategories().clear();
        selectedFileMetadataTags.forEach(selectedFile::addCategoryByName);
        setTagsForTabularData(selectedDataFileTags, selectedFile);
    }

    public void clearFileMetadataSelectedForIngestOptionsPopup() {
        fileMetadataSelectedForIngestOptionsPopup = null;
    }

    public String getIngestLanguageEncoding() {
        return ingestLanguageEncoding == null
                ? getStringFromBundle("editdatafilepage.defaultLanguageEncoding")
                : ingestLanguageEncoding;
    }

    public void handleLabelsFileUpload(FileUploadEvent event) {
        logger.fine("entering handleUpload method.");
        UploadedFile file = event.getFile();

        if (file == null) {
            return;
        }

        InputStream uploadStream;
        try {
            uploadStream = file.getInputStream();
        } catch (IOException ioe) {
            logger.info("the file " + file.getFileName() + " failed to upload!");

            String msg = getStringFromBundle("dataset.file.uploadFailure.detailmsg", file.getFileName());
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, getStringFromBundle("dataset.file.uploadFailure"), msg);
            FacesContext.getCurrentInstance().addMessage(null, message);
            return;
        }
        savedLabelsTempFile = saveTempFile(uploadStream);
        logger.fine(file.getFileName() + " is successfully uploaded.");
        FacesMessage message = new FacesMessage(getStringFromBundle("dataset.file.upload", file.getFileName()));
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void saveAdvancedOptions() {
        // Language encoding for SPSS SAV (and, possibly, other tabular ingests:)
        if (ingestLanguageEncoding != null && fileMetadataSelectedForIngestOptionsPopup != null
                && fileMetadataSelectedForIngestOptionsPopup.getDataFile() != null) {
            if (fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest() == null) {
                IngestRequest ingestRequest = new IngestRequest();
                ingestRequest.setDataFile(fileMetadataSelectedForIngestOptionsPopup.getDataFile());
                fileMetadataSelectedForIngestOptionsPopup.getDataFile().setIngestRequest(ingestRequest);

            }
            fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest().setTextEncoding(ingestLanguageEncoding);
        }
        ingestLanguageEncoding = null;

        // Extra labels for SPSS POR (and, possibly, other tabular ingests)
        // (we are adding this parameter to the IngestRequest now, instead of back when it was uploaded. This is because
        // we want the user to be able to hit cancel and bail out, until they actually click 'save' in the "advanced
        // options" popup) -- L.A. 4.0 beta 11
        if (savedLabelsTempFile != null && fileMetadataSelectedForIngestOptionsPopup != null
                && fileMetadataSelectedForIngestOptionsPopup.getDataFile() != null) {
            if (fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest() == null) {
                IngestRequest ingestRequest = new IngestRequest();
                ingestRequest.setDataFile(fileMetadataSelectedForIngestOptionsPopup.getDataFile());
                fileMetadataSelectedForIngestOptionsPopup.getDataFile().setIngestRequest(ingestRequest);
            }
            fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest().setLabelsFile(savedLabelsTempFile);
        }
        savedLabelsTempFile = null;
        fileMetadataSelectedForIngestOptionsPopup = null;
    }

    public void updateTermsOfUseForSelectedFiles(TermsOfUseForm termsOfUseForm) {
        for (FileMetadata selectedFile : selectedFiles) {
            TermsOfUseForm termsOfUseCopy = new TermsOfUseForm();
            termsOfUseCopy.setTypeWithLicenseId(termsOfUseForm.getTypeWithLicenseId());
            termsOfUseCopy.setRestrictType(termsOfUseForm.getRestrictType());
            termsOfUseCopy.setCustomRestrictText(termsOfUseForm.getCustomRestrictText());
            selectedFile.setTermsOfUseForm(termsOfUseCopy);
        }
    }

    public void initDuplicatesDialog() {
        duplicatesList = listDuplicates();
    }

    public boolean hasDuplicatesSelected() {
        return duplicatesList.stream()
                .map(DuplicatesService.DuplicateGroup::getDuplicates)
                .flatMap(Collection::stream)
                .anyMatch(DuplicatesService.DuplicateItem::isSelected);
    }

    public boolean hasDuplicatesWithWorkingVersion() {
        return duplicatesList.stream()
                .anyMatch(g -> !g.getExistingDuplicatesLabels().isEmpty());
    }

    public void deleteSelectedDuplicates() {
        selectedFiles = duplicatesList.stream()
                .map(DuplicatesService.DuplicateGroup::getDuplicates)
                .flatMap(Collection::stream)
                .filter(DuplicatesService.DuplicateItem::isSelected)
                .map(f -> f.getDataFile().getFileMetadata())
                .collect(Collectors.toList());
        deleteFiles();
        duplicatesList = listDuplicates();
        selectedFiles.clear();

        // We store the contents of the datatable and clear it.
        // The reason is following: when we're removing a row it happens that contents of deleted row
        // (ie. file label and description) are inserted into the next remaining row. So we store the data,
        // destroy current table and then reconstruct it.
        filesTableBackup.addAll(fileMetadatas);
        fileMetadatas.clear();
    }

    public void duplicatesDeletionFinished() {
        // We reconstruct the contents of files table.
        fileMetadatas.addAll(filesTableBackup);
        filesTableBackup.clear();
    }

    // -------------------- PRIVATE --------------------

    private List<DuplicatesService.DuplicateGroup> listDuplicates() {
        return duplicatesService.listDuplicates(listExistingFiles(), newFiles);
    }

    private boolean hasDuplicatesInUploadedFiles(List<DataFile> files) {
        return duplicatesService.hasDuplicatesInUploadedFiles(listExistingFiles(), files);
    }

    private List<DataFile> listExistingFiles() {
        return workingVersion.getFileMetadatas().stream()
                .map(FileMetadata::getDataFile)
                .collect(Collectors.toList());
    }

    private void updateCurrentBatchSizeForDeletedDataFile(DataFile dataFileToDelete) {
        dataFileUploadInfo.removeFromDataFilesToSave(dataFileToDelete);
        if (dataFileUploadInfo.canSubtractSize(dataFileToDelete)) {
            currentBatchSize -= dataFileUploadInfo.getSourceFileSize(dataFileToDelete);
        }
    }

    private void cleanupTempFiles() {
        final long purgeTime = System.currentTimeMillis() - TEMP_VALID_TIME_MILLIS;
        final File tempDirectory = new File(FileUtil.getFilesTempDirectory());
        if (!tempDirectory.exists() || !tempDirectory.isDirectory()) {
            logger.warning("Failed to cleanup temporary file " + FileUtil.getFilesTempDirectory());
        }
        final File[] tempFilesList = tempDirectory.listFiles();
        for (File tempFile : (tempFilesList != null ? tempFilesList : new File[0])) {
            if (tempFile.isFile() && tempFile.lastModified() < purgeTime && !tempFile.delete()) {
                logger.warning("Failed to delete temporary file " + tempFile.getName());
            }
        }
    }

    private void deleteTempFile(DataFile dataFile) {
        // Before we remove the file from the list and forget about it:
        //
        // The physical uploaded file is still sitting in the temporary directory. If it were saved, it would be moved
        // into its permanent location. But since the user chose not to save it, we have to delete the temp file too.
        //
        // Eventually, we will likely add a dedicated mechanism for managing temp files, similar to (or part of) the
        // storage access framework, that would allow us to handle specialized configurations - highly sensitive/private
        // data, that has to be kept encrypted even in temp files, and such. But for now, we just delete the file
        // directly on the local filesystem:

        try {
            List<Path> generatedTempFiles = ingestService.listGeneratedTempFiles(
                    Paths.get(FileUtil.getFilesTempDirectory()), dataFile.getStorageIdentifier());
            if (generatedTempFiles != null) {
                for (Path generated : generatedTempFiles) {
                    logger.fine("(Deleting generated thumbnail file " + generated.toString() + ")");
                    try {
                        Files.delete(generated);
                    } catch (IOException ioex) {
                        logger.warning("Failed to delete generated file " + generated.toString());
                    }
                }
            }
            Files.delete(Paths.get(FileUtil.getFilesTempDirectory() + "/" + dataFile.getStorageIdentifier()));
        } catch (IOException ioe) {
            // safe to ignore - it's just a temp file.
            logger.warning("Failed to delete temporary file " + FileUtil.getFilesTempDirectory() + "/"
                    + dataFile.getStorageIdentifier());
        }
    }

    private void removeFileMetadataFromList(List<FileMetadata> metadatas, FileMetadata toDelete) {
        Iterator<FileMetadata> iterator = metadatas.iterator();
        while (iterator.hasNext()) {
            FileMetadata fileMetadata = iterator.next();
            if (toDelete.getDataFile().getStorageIdentifier().equals(fileMetadata.getDataFile().getStorageIdentifier())) {
                iterator.remove();
                break;
            }
        }
    }

    private void removeDataFileFromList(List<DataFile> datafiles, DataFile toDelete) {
        Iterator<DataFile> iterator = datafiles.iterator();
        while (iterator.hasNext()) {
            DataFile file = iterator.next();
            if (toDelete.getStorageIdentifier().equals(file.getStorageIdentifier())) {
                iterator.remove();
                break;
            }
        }
    }

    private void populateDatasetUpdateFailureMessage() {
        JsfHelper.addErrorMessage(getStringFromBundle("dataset.message.filesFailure"), "");
    }

    private String returnToDraftVersion() {
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId().asString() + "&version=DRAFT&faces-redirect=true";
    }

    /** Download a file from drop box */
    private InputStream getDropBoxContent(GetMethod dropBoxMethod) throws IOException {
        // Make http call, download the file:
        int status;

        try {
            HttpClient httpclient = new HttpClient();
            status = httpclient.executeMethod(dropBoxMethod);
            if (status != 200) {
                logger.log(Level.WARNING, "Failed to get DropBox InputStream for file: {0}, status code: {1}",
                        new Object[] {dropBoxMethod.getPath(), status});
                throw new IOException("Non 200 status code returned from dropbox");
            }
            return dropBoxMethod.getResponseBodyAsStream();
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to access DropBox url: {0}!", dropBoxMethod.getPath());
            throw ex;
        }
    }

    private void setUpRsync() {
        logger.fine("setUpRsync called...");
        if (DataCaptureModuleUtil.rsyncSupportEnabled(settingsService.getValueForKey(SettingsServiceBean.Key.UploadMethods))
                && dataset.getFiles().isEmpty()) {

            Try<Option<RsyncInfo>> rsyncFetchOperation = Try.of(() -> fileService.retrieveRsyncScript(dataset, workingVersion))
                    .onFailure(ex -> logger.log(Level.WARNING, "There was a problem with getting rsync script", ex));

            rsyncFetchOperation.onSuccess(this::setupScriptInfo);
        }
    }

    private void setupScriptInfo(Option<RsyncInfo> rsyncScript) {
        rsyncScript.peek(rsyncInfo -> {
            setRsyncScript(rsyncInfo.getRsyncScript());
            rsyncScriptFilename = rsyncInfo.getRsyncScriptFileName();
            setHasRsyncScript(true);
        })
                .onEmpty(() -> setHasRsyncScript(false));
    }

    /**
     * After uploading via the site or Dropbox,
     * check the list of DataFile objects
     */
    private String processUploadedFileList(List<DataFile> dFileList) {
        if (dFileList == null) {
            return null;
        }

        DataFile dataFile;
        String warningMessage = null;

        // NOTE: for native file uploads, the dFileList will only
        // contain 1 file--method is called for every file even if the UI shows "simultaneous uploads"

        // Iterate through list of DataFile objects
        for (DataFile currentFile : dFileList) {
            dataFile = currentFile;
            // Check for ingest warnings
            if (dataFile.isIngestProblem()) {
                if (dataFile.getIngestReport() != null) {
                    warningMessage = warningMessage == null
                            ? dataFile.getIngestReport().getIngestReportMessage()
                            : warningMessage.concat("; " + dataFile.getIngestReport().getIngestReportMessage());
                }
                dataFile.setIngestDone();
            }

            // Let's check if filename is a duplicate of another
            // file already uploaded, or already in the dataset:
            dataFile.getFileMetadata().setLabel(createNewNameIfDuplicated(dataFile.getFileMetadata()));
            if (isTemporaryPreviewAvailable(dataFile.getStorageIdentifier(), dataFile.getContentType())) {
                dataFile.setPreviewImageAvailable(true);
            }
            uploadedFiles.add(dataFile);
        }

        if (warningMessage != null) {
            logger.severe(warningMessage);
            return warningMessage;
        }
        return null;
    }

    private String createNewNameIfDuplicated(FileMetadata fileMetadata) {
        if (fileLabelsExisting == null) {
            fileLabelsExisting = IngestUtil.existingPathNamesAsSet(workingVersion);
        }
        return IngestUtil.createNewNameIfDuplicated(fileMetadata, fileLabelsExisting);
    }

    private void setTagsForTabularData(Collection<String> selectedDataFileTags, FileMetadata fileMetadata) {
        fileMetadata.getDataFile().getTags().clear();

        selectedDataFileTags.forEach(selectedTag -> {
            DataFileTag tag = new DataFileTag();
            tag.setTypeByLabel(selectedTag);
            tag.setDataFile(fileMetadata.getDataFile());
            fileMetadata.getDataFile().addTag(tag);
        });
    }

    private String saveTempFile(InputStream input) {
        if (input == null) {
            return null;
        }
        byte[] buffer = new byte[8192];
        int bytesRead;
        File labelsFile;
        FileOutputStream output = null;
        try {
            labelsFile = File.createTempFile("tempIngestLabels.", ".txt");
            output = new FileOutputStream(labelsFile);
            while ((bytesRead = input.read(buffer)) > -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException ioex) {
            return null;
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
        if (labelsFile != null) {
            return labelsFile.getAbsolutePath();
        }
        return null;
    }

    private void populateFileMetadatas(Set<Long> selectedFileIds) {
        for (FileMetadata fileMetadata : workingVersion.getFileMetadatas()) {
            Long fileId = fileMetadata.getDataFile().getId();

            if (selectedFileIds.contains(fileId)) {
                logger.fine("Success! - found the file id " + fileId + " in the edit version.");
                fileMetadatas.add(fileMetadata);
                selectedFileIds.remove(fileId);
            }

            if (selectedFileIds.isEmpty()) {
                break;
            }
        }
    }

    // -------------------- SETTERS --------------------

    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public void setDropBoxSelection(String dropBoxSelection) {
        this.dropBoxSelection = dropBoxSelection;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public void setCurrentBatchSize(long currentBatchSize) {
        this.currentBatchSize = currentBatchSize;
    }

    public void setSelectedFiles(List<FileMetadata> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }

    public void setVersionString(String versionString) {
        this.versionString = versionString;
    }

    public void setHasRsyncScript(Boolean hasRsyncScript) {
        this.hasRsyncScript = hasRsyncScript;
    }

    public void setRsyncScript(String rsyncScript) {
        this.rsyncScript = rsyncScript;
    }

    public void setWarningMessageForPopUp(String warningMessageForPopUp) {
        this.warningMessageForPopUp = warningMessageForPopUp;
    }

    public void setSelectedFile(FileMetadata fm) {
        selectedFile = fm;
    }

    public void setFileMetadataSelectedForIngestOptionsPopup(FileMetadata fm) {
        fileMetadataSelectedForIngestOptionsPopup = fm;
    }

    public void setIngestLanguageEncoding(String ingestLanguageEncoding) {
        this.ingestLanguageEncoding = ingestLanguageEncoding;
    }

    public void setIngestEncoding(String ingestEncoding) {
        this.ingestLanguageEncoding = ingestEncoding;
    }
}
