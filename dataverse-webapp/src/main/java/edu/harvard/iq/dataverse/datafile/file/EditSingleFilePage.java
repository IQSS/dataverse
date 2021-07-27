package edu.harvard.iq.dataverse.datafile.file;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.datafile.page.FileDownloadHelper;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnailService;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.provenance.ProvPopupFragmentBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.apache.commons.lang3.math.NumberUtils;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


@ViewScoped
@Named("EditSingleFilePage")
public class EditSingleFilePage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(EditSingleFilePage.class.getCanonicalName());

    private DatasetDao datasetDao;
    private DatasetService datasetService;
    private DataFileServiceBean datafileService;
    private SettingsServiceBean settingsService;
    private PermissionsWrapper permissionsWrapper;
    private FileDownloadHelper fileDownloadHelper;
    private ProvPopupFragmentBean provPopupFragmentBean;
    private SingleFileFacade singleFileFacade;
    private DatasetThumbnailService datasetThumnailService;

    private Dataset dataset = new Dataset();
    private String editedFileIdString = null;
    private FileMetadata fileMetadata;

    private Long ownerId;
    private Long versionId;
    private DatasetVersion workingVersion;

    private String persistentId;
    private String versionString = "";

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public EditSingleFilePage() {
    }

    @Inject
    public EditSingleFilePage(DatasetDao datasetDao, DatasetService datasetService,
                              DataFileServiceBean datafileService,
                              SettingsServiceBean settingsService,
                              PermissionsWrapper permissionsWrapper,
                              FileDownloadHelper fileDownloadHelper, ProvPopupFragmentBean provPopupFragmentBean,
                              SingleFileFacade singleFileFacade, DatasetThumbnailService datasetThumbnailService) {
        this.datasetService = datasetService;
        this.datasetDao = datasetDao;
        this.datafileService = datafileService;
        this.settingsService = settingsService;
        this.permissionsWrapper = permissionsWrapper;
        this.fileDownloadHelper = fileDownloadHelper;
        this.provPopupFragmentBean = provPopupFragmentBean;
        this.singleFileFacade = singleFileFacade;
        this.datasetThumnailService = datasetThumbnailService;
    }

    // -------------------- GETTERS --------------------

    public DataFile getSingleFile() {
        return fileMetadata.getDataFile();
    }

    public String getSelectedFileIds() {
        return editedFileIdString;
    }

    /**
     * Returns list of metadata - always with single element.
     * although we operate only on one edited file metadata, we still need list of file metadata
     * to handle p:dataTable display (and possibly engine command usage)
     */
    public List<FileMetadata> getFileMetadatas() {
        return Lists.newArrayList(fileMetadata);
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

        // Check if they have permission to modify this dataset:

        if (!permissionsWrapper.canCurrentUserUpdateDataset(dataset)) {
            return permissionsWrapper.notAuthorized();
        }
        if (datasetDao.isInReview(dataset) && !permissionsWrapper.canUpdateAndPublishDataset(dataset)) {
            return permissionsWrapper.notAuthorized();
        }

        long selectedFileId = NumberUtils.toLong(editedFileIdString);
        if (selectedFileId == 0) {
            return permissionsWrapper.notFound();
        }
        logger.fine("The page is called with " + selectedFileId + " file id.");

        populateFileMetadata(selectedFileId);

        // if no filemetadatas can be found for the specified file ids
        // and version id - send them to the "not found" page.
        if (fileMetadata == null) {
            return permissionsWrapper.notFound();
        }

        if (fileMetadata.getDatasetVersion().getId() != null) {
            versionString = "DRAFT";
        }

        return null;
    }

    public boolean isInstallationPublic() {
        return settingsService.isTrueForKey(SettingsServiceBean.Key.PublicInstall);
    }

    public String save() {

        Try<Dataset> updateFileOperation = singleFileFacade.saveFileChanges(fileMetadata, provPopupFragmentBean.getProvenanceUpdates())
                .onFailure(ex -> populateDatasetUpdateFailureMessage());

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
            JsfHelper.addFlashSuccessMessage(successMessage);
        }
    }

    public void deleteDatasetLogoAndUseThisDataFileAsThumbnailInstead() {
        logger.log(Level.FINE, "For dataset id {0} the current thumbnail is from a dataset logo rather than a dataset file," +
                " blowing away the logo and using this FileMetadata id instead: {1}", new Object[]{dataset.getId(), fileMetadata});

        Try.of(() -> datasetService.changeDatasetThumbnail(dataset, fileMetadata.getDataFile()))
                .onSuccess(datasetThumbnail -> dataset = datasetDao.find(dataset.getId()))
                .onFailure(ex -> logger.log(Level.WARNING, "Problem setting thumbnail for dataset id: " + dataset.getId(), ex));
    }

    public boolean isThumbnailIsFromDatasetLogoRatherThanDatafile() {
        DatasetThumbnail datasetThumbnail = datasetThumnailService.getThumbnail(dataset);
        return datasetThumbnail != null && !datasetThumbnail.isFromDataFile();
    }

    public void saveFileTagsAndCategories(FileMetadata selectedFile,
                                          Collection<String> selectedFileMetadataTags,
                                          Collection<String> selectedDataFileTags) {

        selectedFile.getCategories().clear();
        selectedFileMetadataTags.forEach(selectedFile::addCategoryByName);

        setTagsForTabularData(selectedDataFileTags, selectedFile);
    }

    public String getPageTitle() {
        return BundleUtil.getStringFromBundle("file.editSingleFile") + " - " + workingVersion.getParsedTitle();
    }

    // -------------------- PRIVATE ---------------------

    private void populateDatasetUpdateFailureMessage() {

        JsfHelper.addErrorMessage(getBundleString("dataset.message.filesFailure"), "");
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

    private void populateFileMetadata(long selectedFileId) {
        for (FileMetadata fileMetadata : workingVersion.getFileMetadatas()) {
            if (selectedFileId == fileMetadata.getDataFile().getId()) {
                logger.fine("Success! - found the file id " + selectedFileId + " in the edit version.");
                this.fileMetadata = fileMetadata;
                break;
            }
        }
    }

    // -------------------- SETTERS --------------------

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
