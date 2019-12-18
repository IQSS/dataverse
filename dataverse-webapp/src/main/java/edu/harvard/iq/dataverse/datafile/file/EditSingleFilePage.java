package edu.harvard.iq.dataverse.datafile.file;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.datafile.page.FileDownloadHelper;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.provenance.ProvPopupFragmentBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.faces.application.FacesMessage;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

@ViewScoped
@Named("EditSingleFilePage")
public class EditSingleFilePage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(EditSingleFilePage.class.getCanonicalName());

    private DatasetDao datasetDao;
    private DatasetService datasetService;
    private DataFileServiceBean datafileService;
    private PermissionServiceBean permissionService;
    private DataverseSession session;
    private SettingsServiceBean settingsService;
    private DataverseRequestServiceBean dvRequestService;
    private PermissionsWrapper permissionsWrapper;
    private FileDownloadHelper fileDownloadHelper;
    private ProvPopupFragmentBean provPopupFragmentBean;
    private SingleFileFacade singleFileFacade;

    private Dataset dataset = new Dataset();
    private String editedFileIdString = null;
    private Long selectedFileId;
    private FileMetadata fileMetadata;

    // although we operate only one edited file metadata, we still need list of file metadata
    // to handle p:dataTable display (and possibly engine command usage)
    private List<FileMetadata> fileMetadatas;

    private Long ownerId;
    private Long versionId;
    private DatasetVersion workingVersion;
    private boolean datasetUpdateRequired = false;
    private boolean tabularDataTagsUpdated = false;

    private String persistentId;
    private String versionString = "";


    private DataFile singleFile = null;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public EditSingleFilePage() {
    }

    @Inject
    public EditSingleFilePage(DatasetDao datasetDao, DatasetService datasetService,
                              DataFileServiceBean datafileService, PermissionServiceBean permissionService,
                              DataverseSession session, SettingsServiceBean settingsService,
                              DataverseRequestServiceBean dvRequestService, PermissionsWrapper permissionsWrapper,
                              FileDownloadHelper fileDownloadHelper, ProvPopupFragmentBean provPopupFragmentBean,
                              SingleFileFacade singleFileFacade) {
        this.datasetService = datasetService;
        this.datasetDao = datasetDao;
        this.datafileService = datafileService;
        this.permissionService = permissionService;
        this.session = session;
        this.settingsService = settingsService;
        this.dvRequestService = dvRequestService;
        this.permissionsWrapper = permissionsWrapper;
        this.fileDownloadHelper = fileDownloadHelper;
        this.provPopupFragmentBean = provPopupFragmentBean;
        this.singleFileFacade = singleFileFacade;
    }

    // -------------------- GETTERS --------------------

    public DataFile getSingleFile() {
        return singleFile;
    }

    public String getSelectedFileIds() {
        return editedFileIdString;
    }

    public List<FileMetadata> getFileMetadatas() {

        if (fileMetadata != null) {
            logger.fine("Returning file metadata.");
        } else {
            logger.fine("File metadata hasn't been initialized yet.");
        }

        return fileMetadatas;
    }

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    public String getGlobalId() {
        return persistentId;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public Dataset getDataset() {
        return dataset;
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

    public String getVersionString() {
        return versionString;
    }

    /**
     * @param msgName - from the bundle e.g. "file.deleted.success"
     * @return
     */
    private String getBundleString(String msgName) {

        return BundleUtil.getStringFromBundle(msgName);
    }

    public boolean getUseAsDatasetThumbnail() {
        return isDesignatedDatasetThumbnail(fileMetadata);
    }

    // -------------------- LOGIC --------------------

    public String init() {

        if (dataset.getId() != null) {
            // Set Working Version and Dataset by Dataset Id and Version
            dataset = datasetDao.find(dataset.getId());
            // Is the Dataset harvested? (because we don't allow editing of harvested
            // files!)
            if (dataset == null || dataset.isHarvested()) {
                return permissionsWrapper.notFound();
            }
        } else {
            return permissionsWrapper.notFound();
        }


        workingVersion = dataset.getEditVersion();
        if (workingVersion == null || !workingVersion.isDraft()) {
            // Sorry, we couldn't find/obtain a draft version for this dataset!
            return permissionsWrapper.notFound();
        }

        // Check if they have permission to modify this dataset:

        if (!permissionService.on(dataset).has(Permission.EditDataset)) {
            return permissionsWrapper.notAuthorized();
        }
        if (datasetDao.isInReview(dataset) && !permissionsWrapper.canUpdateAndPublishDataset(dvRequestService.getDataverseRequest(), dataset)) {
            return permissionsWrapper.notAuthorized();
        }

        if (StringUtils.isNotEmpty(editedFileIdString)) {
            try {
                Long fileId = Long.parseLong(editedFileIdString);
                singleFile = datafileService.find(fileId);
                selectedFileId = fileId;
            } catch (NumberFormatException nfe) {
                // do nothing...
                logger.warning("Couldn't parse editedFileIdString =" + editedFileIdString + " to Long");
                JH.addMessage(FacesMessage.SEVERITY_ERROR, "File id is not a number!");
                return "";
            }
        }

        if (singleFile == null) {
            logger.fine("No numeric file ids supplied to the page, in the edit mode. Redirecting to the 404 page.");
            // If no valid file IDs specified, send them to the 404 page...
            return permissionsWrapper.notFound();
        }

        logger.fine("The page is called with " + selectedFileId + " file id.");

        populateFileMetadatas();

        // if no filemetadatas can be found for the specified file ids
        // and version id - send them to the "not found" page.
        if (fileMetadata == null) {
            return permissionsWrapper.notFound();
        }

        if (fileMetadata.getDatasetVersion().getId() != null) {
            versionString = "DRAFT";
        }

        if (settingsService.isTrueForKey(SettingsServiceBean.Key.PublicInstall)) {
            JH.addMessage(FacesMessage.SEVERITY_WARN, getBundleString("dataset.message.publicInstall"));
        }

        return null;
    }

    public String save() {

        if (!datasetUpdateRequired) {
            return returnToFileLandingPage();
        }

        updateEntityWithUpdatedFile();

        Try<Dataset> updateFileOperation = Try.of(() -> singleFileFacade.saveFileChanges(fileMetadata,
                                                                                         provPopupFragmentBean.getProvenanceUpdates()))
                .onFailure(ex -> {
                    logger.log(Level.SEVERE, "Unable to update file with dataset id: " + dataset.getId(), ex);
                    populateDatasetUpdateFailureMessage();
                });

        if (updateFileOperation.isFailure()) {
            return "";
        }

        workingVersion = dataset.getEditVersion();
        JsfHelper.addFlashSuccessMessage(getBundleString("file.message.editSuccess"));

        versionString = "DRAFT";
        return returnToFileLandingPage();

    }

    public String cancel() {
        return returnToFileLandingPage();
    }

    public boolean isThumbnailAvailable(FileMetadata fileMetadata) {
        // new and optimized logic:
        // - check download permission here (should be cached - so it's free!)
        // - only then ask the file service if the thumbnail is available/exists.
        // the service itself no longer checks download permissions.
        if (!fileDownloadHelper.canUserDownloadFile(fileMetadata)) {
            return false;
        }

        return datafileService.isThumbnailAvailable(fileMetadata.getDataFile());
    }

    public boolean isDesignatedDatasetThumbnail(FileMetadata fileMetadata) {
        if (fileMetadata != null && fileMetadata.getDataFile() != null && fileMetadata.getDataFile().getId() != null) {
            return fileMetadata.getDataFile().equals(dataset.getThumbnailFile());
        }
        return false;
    }

    public void saveAsDesignatedThumbnail() {
        logger.fine("saving as the designated thumbnail");
        // We don't need to do anything specific to save this setting, because
        // the setUseAsDatasetThumbnail() method, above, has already updated the
        // file object appropriately.
        // However, once the "save" button is pressed, we want to show a success message, if this is
        // a new image has been designated as such:
        if (isDesignatedDatasetThumbnail(fileMetadata)) {
            String successMessage = getBundleString("file.assignedDataverseImage.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileMetadata.getLabel());
            JsfHelper.addFlashMessage(successMessage);
        }

        datasetUpdateRequired = true;
    }

    public void deleteDatasetLogoAndUseThisDataFileAsThumbnailInstead() {
        logger.log(Level.FINE, "For dataset id {0} the current thumbnail is from a dataset logo rather than a dataset file," +
                " blowing away the logo and using this FileMetadata id instead: {1}", new Object[]{dataset.getId(), fileMetadata});

        Try.of(() -> datasetService.changeDatasetThumbnail(dataset, fileMetadata.getDataFile()))
                .onSuccess(datasetThumbnail -> dataset = datasetDao.find(dataset.getId()))
                .onFailure(ex -> logger.log(Level.WARNING, "Problem setting thumbnail for dataset id: " + dataset.getId(), ex));
    }

    public boolean isThumbnailIsFromDatasetLogoRatherThanDatafile() {
        DatasetThumbnail datasetThumbnail = DatasetUtil.getThumbnail(dataset);
        return datasetThumbnail != null && !datasetThumbnail.isFromDataFile();
    }

    public void saveFileTagsAndCategories(FileMetadata selectedFile,
                                          Collection<String> selectedFileMetadataTags,
                                          Collection<String> selectedDataFileTags) {

        selectedFile.getCategories().clear();
        selectedFileMetadataTags.forEach(selectedFile::addCategoryByName);

        setTagsForTabularData(selectedDataFileTags, selectedFile);
        datasetUpdateRequired = true;
    }

    public void handleDescriptionChange(final AjaxBehaviorEvent event) {
        datasetUpdateRequired = true;
    }

    public void handleNameChange(final AjaxBehaviorEvent event) {
        datasetUpdateRequired = true;
    }

    // -------------------- PRIVATE ---------------------

    private void populateDatasetUpdateFailureMessage() {

        JH.addMessage(FacesMessage.SEVERITY_FATAL, getBundleString("dataset.message.filesFailure"));
    }

    private void updateEntityWithUpdatedFile() {
        workingVersion.getFileMetadatas().stream()
                .filter(fmd -> fmd.getDataFile().getStorageIdentifier().equals(fileMetadata.getDataFile().getStorageIdentifier()))
                .forEach(fmd -> fmd = fileMetadata);
    }

    private String returnToFileLandingPage() {
        Long fileId = fileMetadata.getDataFile().getId();
        if (versionString != null && versionString.equals("DRAFT")) {
            return "/file.xhtml?fileId=" + fileId + "&version=DRAFT&faces-redirect=true";
        }
        return "/file.xhtml?fileId=" + fileId + "&faces-redirect=true";

    }

    private void setTagsForTabularData(Collection<String> selectedDataFileTags, FileMetadata fmd) {
        fmd.getDataFile().getTags().clear();

        selectedDataFileTags.forEach(selectedTag -> {
            DataFileTag tag = new DataFileTag();
            tag.setTypeByLabel(selectedTag);
            tag.setDataFile(fmd.getDataFile());
            fmd.getDataFile().addTag(tag);
        });
    }

    private void populateFileMetadatas() {

        Long datasetVersionId = workingVersion.getId();

        if (datasetVersionId != null) {
            // The version has a database id - this is an existing version,
            // that had been saved previously. So we can look up the file metadatas
            // by the file and version ids:
            logger.fine("attempting to retrieve file metadata for version id " + datasetVersionId + " and file id " + selectedFileId);
            FileMetadata fileMetadata = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(datasetVersionId, selectedFileId);
            if (fileMetadata != null) {
                logger.fine("Success!");
                this.fileMetadata = fileMetadata;
            } else {
                logger.fine("Failed to find file metadata.");
            }
        } else {
            logger.fine("Brand new edit version - no database id.");
            for (FileMetadata fileMetadata : workingVersion.getFileMetadatas()) {

                if (selectedFileId.equals(fileMetadata.getDataFile().getId())) {
                    logger.fine("Success! - found the file id " + selectedFileId + " in the brand new edit version.");
                    this.fileMetadata = fileMetadata;
                    break;
                }
            }
        }
        fileMetadatas = new ArrayList<>();
        fileMetadatas.add(fileMetadata);
    }

    // -------------------- SETTERS --------------------

    public void setSingleFile(DataFile singleFile) {
        this.singleFile = singleFile;
    }

    public void setSelectedFileIds(String selectedFileIds) {
        editedFileIdString = selectedFileIds;
    }

    public void setFileMetadatas(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
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

    public void setVersionString(String versionString) {
        this.versionString = versionString;
    }

    public void setUseAsDatasetThumbnail(boolean useAsThumbnail) {
        if (fileMetadata != null) {
            if (fileMetadata.getDataFile() != null) {
                if (useAsThumbnail) {
                    dataset.setThumbnailFile(fileMetadata.getDataFile());
                } else if (getUseAsDatasetThumbnail()) {
                    dataset.setThumbnailFile(null);
                }
            }
        }
    }
}
