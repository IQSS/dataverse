package edu.harvard.iq.dataverse.dataset.file;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EditDatafilesPage;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.FileDownloadHelper;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileCategory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.provenance.ProvPopupFragmentBean;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

@ViewScoped
@Named("EditSingleFilePage")
public class EditSingleFilePage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(EditSingleFilePage.class.getCanonicalName());

    private DatasetServiceBean datasetService;
    private DataFileServiceBean datafileService;
    private PermissionServiceBean permissionService;
    private EjbDataverseEngine commandEngine;
    private DataverseSession session;
    private SettingsServiceBean settingsService;
    private IndexServiceBean indexService;
    private DataverseRequestServiceBean dvRequestService;
    private PermissionsWrapper permissionsWrapper;
    private FileDownloadHelper fileDownloadHelper;
    private ProvPopupFragmentBean provPopupFragmentBean;

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
    private DatasetVersion clone;
    private boolean datasetUpdateRequired = false;
    private boolean tabularDataTagsUpdated = false;

    private String persistentId;
    private String versionString = "";


    private boolean saveEnabled = false;
    private boolean isFileToBeDeleted = false;
    private DataFile singleFile = null;

    private List<String> tabFileTags = null;
    private String[] selectedTabFileTags = {};
    private String[] selectedTags = {};
    private List<String> tabFileTagsByName;
    private List<String> categoriesByName;
    private String newCategoryName = null;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public EditSingleFilePage() {
    }

    @Inject
    public EditSingleFilePage(DatasetServiceBean datasetService, DataFileServiceBean datafileService, PermissionServiceBean permissionService,
                              EjbDataverseEngine commandEngine, DataverseSession session, SettingsServiceBean settingsService,
                              IndexServiceBean indexService, DataverseRequestServiceBean dvRequestService, PermissionsWrapper permissionsWrapper,
                              FileDownloadHelper fileDownloadHelper, ProvPopupFragmentBean provPopupFragmentBean) {
        this.datasetService = datasetService;
        this.datafileService = datafileService;
        this.permissionService = permissionService;
        this.commandEngine = commandEngine;
        this.session = session;
        this.settingsService = settingsService;
        this.indexService = indexService;
        this.dvRequestService = dvRequestService;
        this.permissionsWrapper = permissionsWrapper;
        this.fileDownloadHelper = fileDownloadHelper;
        this.provPopupFragmentBean = provPopupFragmentBean;
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

    public List<String> getTabFileTags() {
        if (tabFileTags == null) {
            tabFileTags = DataFileTag.listTags();
        }
        return tabFileTags;
    }

    public String[] getSelectedTabFileTags() {
        return selectedTabFileTags;
    }

    public List<String> getCategoriesByName() {
        return categoriesByName;
    }

    public String[] getSelectedTags() {
        return selectedTags;
    }

    public String getNewCategoryName() {
        return newCategoryName;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        if (dataset.getId() != null) {
            // Set Working Version and Dataset by Dataset Id and Version
            dataset = datasetService.find(dataset.getId());
            // Is the Dataset harvested? (because we don't allow editing of harvested
            // files!)
            if (dataset == null || dataset.isHarvested()) {
                return permissionsWrapper.notFound();
            }
        } else {
            return permissionsWrapper.notFound();
        }


        workingVersion = dataset.getEditVersion();
        clone = workingVersion.cloneDatasetVersion();
        if (workingVersion == null || !workingVersion.isDraft()) {
            // Sorry, we couldn't find/obtain a draft version for this dataset!
            return permissionsWrapper.notFound();
        }

        // Check if they have permission to modify this dataset:

        if (!permissionService.on(dataset).has(Permission.EditDataset)) {
            return permissionsWrapper.notAuthorized();
        }
        if (datasetService.isInReview(dataset) && !permissionsWrapper.canUpdateAndPublishDataset(dvRequestService.getDataverseRequest(), dataset)) {
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

        saveEnabled = true;

        if (settingsService.isTrueForKey(SettingsServiceBean.Key.PublicInstall)) {
            JH.addMessage(FacesMessage.SEVERITY_WARN, getBundleString("dataset.message.publicInstall"));
        }

        return null;
    }

    public void deleteFile() {
        logger.fine("entering bulk file delete (EditDataFilesPage)");

        String fileName = null;

        if (fileName == null) {
            fileName = fileMetadata.getLabel();
        } else {
            fileName = fileName.concat(", " + fileMetadata.getLabel());
        }

        logger.fine("delete requested on file " + fileMetadata.getLabel());
        logger.fine("file metadata id: " + fileMetadata.getId());
        logger.fine("datafile id: " + fileMetadata.getDataFile().getId());

        // all we remove is the file from the fileMetadatas (from the
        // file metadatas attached to the editVersion, and from the
        // display list of file metadatas that are being edited)
        // and let the delete be handled in the command (by adding it to the
        // filesToBeDeleted list):
        dataset.getEditVersion().getFileMetadatas().remove(fileMetadata);
        fileMetadatas.remove(fileMetadata);
        isFileToBeDeleted = true;

        if (fileName != null) {
            String successMessage = getBundleString("file.deleted.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileName);
            JsfHelper.addFlashMessage(successMessage);
        }
    }

    public String save() {
        // Once all the filemetadatas pass the validation, we'll only allow the user
        // to try to save once; (this it to prevent them from creating multiple
        // DRAFT versions, if the page gets stuck in that state where it
        // successfully creates a new version, but can't complete the remaining
        // tasks. -- L.A. 4.2

        if (!saveEnabled) {
            return "";
        }

        Boolean provJsonChanges = false;

        if (settingsService.isTrueForKey(SettingsServiceBean.Key.ProvCollectionEnabled)) {
            Boolean provFreeChanges = provPopupFragmentBean.updatePageMetadatasWithProvFreeform(fileMetadatas);

            try {
                // Note that the user may have uploaded provenance metadata file(s)
                // for some of the new files that have since failed to be permanently saved
                // in storage (in the ingestService.saveAndAddFilesToDataset() step, above);
                // these files have been dropped from the fileMetadatas list, and we
                // are not adding them to the dataset; but the
                // provenance update set still has entries for these failed files,
                // so we are passing the fileMetadatas list to the saveStagedProvJson()
                // method below - so that it doesn't attempt to save the entries
                // that are no longer valid.
                provJsonChanges = provPopupFragmentBean.saveStagedProvJson(false, fileMetadatas);
            } catch (AbstractApiBean.WrappedResponse ex) {
                JsfHelper.addFlashErrorMessage(getBundleString("file.metadataTab.provenance.error"));
                Logger.getLogger(EditDatafilesPage.class.getName()).log(Level.SEVERE, null, ex);
            }
            //Always update the whole dataset if updating prov
            //The flow that happens when datasetUpdateRequired is false has problems with doing saving actions after its merge
            //This was the simplest way to work around this issue for prov. --MAD 4.8.6.
            datasetUpdateRequired = datasetUpdateRequired || provFreeChanges || provJsonChanges;
        }

        if (workingVersion.getId() == null || datasetUpdateRequired) {
            logger.fine("issuing the dataset update command");
            // We are creating a new draft version;
            // (OR, a full update of the dataset has been explicitly requested,
            // because of the nature of the updates the user has made).
            // We'll use an Update command for this:

            if (datasetUpdateRequired) {
                for (int i = 0; i < workingVersion.getFileMetadatas().size(); i++) {
                    if (fileMetadata.getDataFile().getStorageIdentifier() != null) {
                        if (fileMetadata.getDataFile().getStorageIdentifier().equals(workingVersion.getFileMetadatas().get(i).getDataFile().getStorageIdentifier())) {
                            workingVersion.getFileMetadatas().set(i, fileMetadata);
                        }
                    }
                }


                //Moves DataFile updates from popupFragment to page for saving
                //This does not seem to collide with the tags updating below
                if (settingsService.isTrueForKey(SettingsServiceBean.Key.ProvCollectionEnabled) && provJsonChanges) {
                    HashMap<String, ProvPopupFragmentBean.UpdatesEntry> provenanceUpdates = provPopupFragmentBean.getProvenanceUpdates();
                    for (int i = 0; i < dataset.getFiles().size(); i++) {
                        for (ProvPopupFragmentBean.UpdatesEntry ue : provenanceUpdates.values()) {
                            if (ue.dataFile.getStorageIdentifier() != null) {
                                if (ue.dataFile.getStorageIdentifier().equals(dataset.getFiles().get(i).getStorageIdentifier())) {
                                    dataset.getFiles().set(i, ue.dataFile);
                                }
                            }
                        }
                    }
                }

                // Tabular data tags are assigned to datafiles, not to
                // version-specfic filemetadatas!
                // So if tabular tags have been modified, we also need to
                // refresh the list of datafiles, as found in dataset.getFiles(),
                // similarly to what we've just done, above, for the filemetadatas.
                // Otherwise, when we call UpdateDatasetCommand, it's not going
                // to update the tags in the database (issue #2798).
                // TODO: Is the above still true/is this still necessary?
                // (and why?...)

                if (tabularDataTagsUpdated) {
                    for (int i = 0; i < dataset.getFiles().size(); i++) {
                        if (fileMetadata.getDataFile().getStorageIdentifier() != null) {
                            if (fileMetadata.getDataFile().getStorageIdentifier().equals(dataset.getFiles().get(i).getStorageIdentifier())) {
                                dataset.getFiles().set(i, fileMetadata.getDataFile());
                            }
                        }
                    }
                    tabularDataTagsUpdated = false;
                }
            }

            Map<Long, String> deleteStorageLocations = null;

            List<FileMetadata> fileDeleteList = new ArrayList<>();
            if (isFileToBeDeleted) {
                deleteStorageLocations = datafileService.getPhysicalFilesToDelete(fileMetadatas);
                fileDeleteList.add(fileMetadata);
            }

            Command<Dataset> cmd;
            try {
                cmd = new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest(), fileDeleteList, clone);
                ((UpdateDatasetVersionCommand) cmd).setValidateLenient(true);
                dataset = commandEngine.submit(cmd);

            } catch (EJBException ex) {
                StringBuilder error = new StringBuilder();
                error.append(ex).append(" ");
                error.append(ex.getMessage()).append(" ");
                Throwable cause = ex;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                    error.append(cause).append(" ");
                    error.append(cause.getMessage()).append(" ");
                }
                logger.log(Level.INFO, "Couldn''t save dataset: {0}", error.toString());
                populateDatasetUpdateFailureMessage();
                return null;
            } catch (CommandException ex) {
                //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + ex.toString()));
                logger.log(Level.INFO, "Couldn''t save dataset: {0}", ex.getMessage());
                populateDatasetUpdateFailureMessage();
                return null;
            }

            // Have we just deleted some draft datafiles (successfully)?
            // finalize the physical file deletes:
            // (DataFileService will double-check that the datafiles no
            // longer exist in the database, before attempting to delete
            // the physical files)
            if (deleteStorageLocations != null) {
                datafileService.finalizeFileDeletes(deleteStorageLocations);
            }

            datasetUpdateRequired = false;
            saveEnabled = false;
        } else {
            // This is an existing Draft version (and nobody has explicitly
            // requested that the entire dataset is updated). So we'll try to update
            // only the filemetadatas and/or files affected, and not the
            // entire version.
            Timestamp updateTime = new Timestamp(new Date().getTime());

            workingVersion.setLastUpdateTime(updateTime);
            dataset.setModificationTime(updateTime);

            StringBuilder saveError = new StringBuilder();

            if (fileMetadata.getDataFile().getCreateDate() == null) {
                fileMetadata.getDataFile().setCreateDate(updateTime);
                fileMetadata.getDataFile().setCreator((AuthenticatedUser) session.getUser());
            }
            fileMetadata.getDataFile().setModificationTime(updateTime);
            try {
                fileMetadata = datafileService.mergeFileMetadata(fileMetadata);
                logger.fine("Successfully saved DataFile " + fileMetadata.getLabel() + " in the database.");
            } catch (EJBException ex) {
                saveError.append(ex).append(" ");
                saveError.append(ex.getMessage()).append(" ");
                Throwable cause = ex;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                    saveError.append(cause).append(" ");
                    saveError.append(cause.getMessage()).append(" ");
                }
            }

            // Remove / delete file
            if(isFileToBeDeleted) {
                //  check if this file is being used as the default thumbnail
                if (fileMetadata.getDataFile().equals(dataset.getThumbnailFile())) {
                    logger.fine("deleting the dataset thumbnail designation");
                    dataset.setThumbnailFile(null);
                }

                if (!fileMetadata.getDataFile().isReleased()) {
                    // if file is draft (ie. new to this version, delete; otherwise just remove filemetadata object)
                    boolean deleteCommandSuccess = false;
                    Long dataFileId = fileMetadata.getDataFile().getId();
                    String deleteStorageLocation = null;

                    if (dataFileId != null) {

                        deleteStorageLocation = datafileService.getPhysicalFileToDelete(fileMetadata.getDataFile());

                        try {
                            commandEngine.submit(new DeleteDataFileCommand(fileMetadata.getDataFile(), dvRequestService.getDataverseRequest()));
                            dataset.getFiles().remove(fileMetadata.getDataFile());
                            workingVersion.getFileMetadatas().remove(fileMetadata);
                            // added this check to handle an issue where you could not delete a file that shared a category with a new file
                            // the relationship does not seem to cascade, yet somehow it was trying to merge the filemetadata
                            for (DataFileCategory cat : dataset.getCategories()) {
                                cat.getFileMetadatas().remove(fileMetadata);
                            }
                            deleteCommandSuccess = true;
                        } catch (CommandException cmde) {
                            logger.warning("Failed to delete DataFile id=" + dataFileId + " from the database; " + cmde.getMessage());
                        }
                        if (deleteCommandSuccess) {
                            if (deleteStorageLocation != null) {
                                // Finalize the delete of the physical file
                                // (File service will double-check that the datafile no
                                // longer exists in the database, before proceeding to
                                // delete the physical file)
                                try {
                                    datafileService.finalizeFileDelete(dataFileId, deleteStorageLocation, new DataAccess());
                                } catch (IOException ioex) {
                                    logger.warning("Failed to delete the physical file associated with the deleted datafile id="
                                            + dataFileId + ", storage location: " + deleteStorageLocation);
                                }
                            }
                        }
                    }
                } else {
                    datafileService.removeFileMetadata(fileMetadata);
                    workingVersion.getFileMetadatas().remove(fileMetadata);
                    fileMetadata = null;
                }
            }

            String saveErrorString = saveError.toString();
            if (saveErrorString != null && !saveErrorString.isEmpty()) {
                logger.log(Level.INFO, "Couldn''t save dataset: {0}", saveErrorString);
                populateDatasetUpdateFailureMessage();
                return null;
            }
        }

        workingVersion = dataset.getEditVersion();
        logger.fine("working version id: " + workingVersion.getId());

        JsfHelper.addFlashSuccessMessage(getBundleString("file.message.editSuccess"));

        if (fileMetadata != null && !isFileToBeDeleted) {
            // we want to redirect back to
            // the landing page. BUT ONLY if the file still exists - i.e., if
            // the user hasn't just deleted it!
            versionString = "DRAFT";
            return returnToFileLandingPage();
        }

        indexService.indexDataset(dataset, true);
        logger.fine("Redirecting to the dataset page, from the edit/upload page.");
        return returnToDraftVersion();
    }

    public String cancel() {
        return returnToFileLandingPage();
    }

    public boolean isThumbnailAvailable(FileMetadata fileMetadata) {
        // new and optimized logic:
        // - check download permission here (should be cached - so it's free!)
        // - only then ask the file service if the thumbnail is available/exists.
        // the service itself no longer checks download permissions.
        if (!fileDownloadHelper.canDownloadFile(fileMetadata)) {
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

            datasetUpdateRequired = true;
        }
    }

    public void deleteDatasetLogoAndUseThisDataFileAsThumbnailInstead() {
        logger.log(Level.FINE, "For dataset id {0} the current thumbnail is from a dataset logo rather than a dataset file, blowing away the logo and using this FileMetadata id instead: {1}", new Object[]{dataset.getId(), fileMetadata});
        try {
            DatasetThumbnail datasetThumbnail = commandEngine.submit(new UpdateDatasetThumbnailCommand(dvRequestService.getDataverseRequest(), dataset, UpdateDatasetThumbnailCommand.UserIntent.setDatasetFileAsThumbnail, fileMetadata.getDataFile().getId(), null));
            // look up the dataset again because the UpdateDatasetThumbnailCommand mutates (merges) the dataset
            dataset = datasetService.find(dataset.getId());
        } catch (CommandException ex) {
            String error = "Problem setting thumbnail for dataset id " + dataset.getId() + ".: " + ex;
            // show this error to the user?
            logger.info(error);
        }
    }

    public boolean isThumbnailIsFromDatasetLogoRatherThanDatafile() {
        DatasetThumbnail datasetThumbnail = DatasetUtil.getThumbnail(dataset);
        return datasetThumbnail != null && !datasetThumbnail.isFromDataFile();
    }

    public void refreshTagsPopUp(FileMetadata fm) {
        refreshCategoriesByName();
        refreshTabFileTagsByName();
    }

    public String saveNewCategory() {

        if (newCategoryName != null && !newCategoryName.isEmpty()) {
            categoriesByName.add(newCategoryName);
        }
        //Now increase size of selectedTags and add new category
        String[] temp = new String[selectedTags.length + 1];
        System.arraycopy(selectedTags, 0, temp, 0, selectedTags.length);
        selectedTags = temp;
        selectedTags[selectedTags.length - 1] = newCategoryName;
        //Blank out added category
        newCategoryName = "";
        return "";
    }

    /* This method handles saving both "tabular file tags" and
     * "file categories" (which are also considered "tags" in 4.0)
     */
    public void saveFileTagsAndCategories() {
        // 1. File categories:
        /*
        In order to get the cancel button to work we had to separate the selected tags
        from the file metadata and re-add them on save
        */

        fileMetadata.setCategories(new ArrayList<>());

        // New, custom file category (if specified):
        if (newCategoryName != null) {
            logger.fine("Adding new category, " + newCategoryName + " for file " + fileMetadata.getLabel());
            fileMetadata.addCategoryByName(newCategoryName);
        } else {
            logger.fine("no category specified");
        }
        newCategoryName = null;

        // File Categories selected from the list of existing categories:
        if (selectedTags != null) {
            for (String selectedTag : selectedTags) {

                fileMetadata.addCategoryByName(selectedTag);
            }
        }

        // 2. Tabular DataFile Tags:

        if (fileMetadata.getDataFile() != null && tabularDataTagsUpdated && selectedTabFileTags != null) {
            fileMetadata.getDataFile().setTags(null);
            for (String selectedTabFileTag : selectedTabFileTags) {
                DataFileTag tag = new DataFileTag();
                try {
                    tag.setTypeByLabel(selectedTabFileTag);
                    tag.setDataFile(fileMetadata.getDataFile());
                    fileMetadata.getDataFile().addTag(tag);

                } catch (IllegalArgumentException iax) {
                    // ignore
                }
            }

            datasetUpdateRequired = true;
        }
    }

    public void handleFileCategoriesSelection(final AjaxBehaviorEvent event) {
        if (selectedTags != null) {
            selectedTags = selectedTags.clone();
        }
    }

    public void handleTabularTagsSelection(final AjaxBehaviorEvent event) {
        tabularDataTagsUpdated = true;
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

    private String returnToDraftVersion() {
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId().asString() + "&version=DRAFT&faces-redirect=true";
    }

    private String returnToFileLandingPage() {
        Long fileId = fileMetadata.getDataFile().getId();
        if (versionString != null && versionString.equals("DRAFT")) {
            return "/file.xhtml?fileId=" + fileId + "&version=DRAFT&faces-redirect=true";
        }
        return "/file.xhtml?fileId=" + fileId + "&faces-redirect=true";

    }

    private void refreshTabFileTagsByName() {
        tabFileTagsByName = new ArrayList<>();
        if (fileMetadata.getDataFile().getTags() != null) {
            for (int i = 0; i < fileMetadata.getDataFile().getTags().size(); i++) {
                tabFileTagsByName.add(fileMetadata.getDataFile().getTags().get(i).getTypeLabel());
            }
        }
        refreshSelectedTabFileTags();
    }

    private void refreshSelectedTabFileTags() {
        selectedTabFileTags = null;
        selectedTabFileTags = new String[0];
        if (tabFileTagsByName.size() > 0) {
            selectedTabFileTags = new String[tabFileTagsByName.size()];
            for (int i = 0; i < tabFileTagsByName.size(); i++) {
                selectedTabFileTags[i] = tabFileTagsByName.get(i);
            }
        }
        Arrays.sort(selectedTabFileTags);
    }

    private void refreshCategoriesByName() {
        categoriesByName = new ArrayList<>();
        for (String category : dataset.getCategoriesByName()) {
            categoriesByName.add(category);
        }
        refreshSelectedTags();
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
        }
        else {
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

    private void refreshSelectedTags() {
        selectedTags = null;
        selectedTags = new String[0];
        List<String> selectedCategoriesByName = new ArrayList<>();

        if (fileMetadata.getCategories() != null) {
            for (int i = 0; i < fileMetadata.getCategories().size(); i++) {
                if (!selectedCategoriesByName.contains(fileMetadata.getCategories().get(i).getName())) {
                    selectedCategoriesByName.add(fileMetadata.getCategories().get(i).getName());
                }
            }
        }

        if (selectedCategoriesByName.size() > 0) {
            selectedTags = new String[selectedCategoriesByName.size()];
            for (int i = 0; i < selectedCategoriesByName.size(); i++) {
                selectedTags[i] = selectedCategoriesByName.get(i);
            }
        }
        Arrays.sort(selectedTags);
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

    public void setTabFileTags(List<String> tabFileTags) {
        this.tabFileTags = tabFileTags;
    }

    public void setSelectedTabFileTags(String[] selectedTabFileTags) {
        this.selectedTabFileTags = selectedTabFileTags;
    }

    public void setCategoriesByName(List<String> categoriesByName) {
        this.categoriesByName = categoriesByName;
    }

    public void setSelectedTags(String[] selectedTags) {
        this.selectedTags = selectedTags;
    }

    public void setNewCategoryName(String newCategoryName) {
        this.newCategoryName = newCategoryName;
    }
}
