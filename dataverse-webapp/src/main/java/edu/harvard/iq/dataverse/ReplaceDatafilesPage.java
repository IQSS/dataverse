package edu.harvard.iq.dataverse;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.files.mime.ApplicationMimeType;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.FileReplaceException;
import edu.harvard.iq.dataverse.datasetutility.FileReplacePageHelper;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Try;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.FacesEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ViewScoped
@Named("ReplaceDatafilesPage")
public class ReplaceDatafilesPage implements Serializable {

    private static final Logger logger = Logger.getLogger(ReplaceDatafilesPage.class.getCanonicalName());

    private PermissionsWrapper permissionsWrapper;
    private PermissionServiceBean permissionService;
    private DatasetServiceBean datasetService;
    private DataFileServiceBean datafileService;
    private IngestServiceBean ingestService;
    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;
    private SystemConfig systemConfig;
    private SettingsServiceBean settingsService;

    private long datasetId;
    private long fileId;
    private Dataset dataset;
    private DataFile fileToBeReplaced;
    private FileReplacePageHelper fileReplacePageHelper;
    private Map<String, String> temporaryThumbnailsMap = new HashMap<>();
    private List<String> categoriesByName = new ArrayList<>();
    private List<FileMetadata> selectedFiles;
    private String[] selectedTags = {};
    private String[] selectedTabFileTags = {};
    private List<String> tabFileTags;
    private FileMetadata fileMetadataSelectedForTagsPopup;
    private List<String> tabFileTagsByName;
    private boolean uploadInProgress;
    private String warningMessageForPopUp;
    private String newCategoryName;
    private String dropBoxSelection = "";

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated /* JEE requirement*/
    public ReplaceDatafilesPage() {
    }

    @Inject
    public ReplaceDatafilesPage(PermissionsWrapper permissionsWrapper, PermissionServiceBean permissionService,
                                DatasetServiceBean datasetService, DataFileServiceBean datafileService, IngestServiceBean ingestService,
                                EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService, SystemConfig systemConfig,
                                SettingsServiceBean settingsService) {
        this.permissionsWrapper = permissionsWrapper;
        this.permissionService = permissionService;
        this.datasetService = datasetService;
        this.datafileService = datafileService;
        this.ingestService = ingestService;
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
        this.systemConfig = systemConfig;
        this.settingsService = settingsService;
    }

    // -------------------- GETTERS --------------------

    public long getDatasetId() {
        return datasetId;
    }

    public long getFileId() {
        return fileId;
    }

    public List<String> getTabFileTags() {
        if (tabFileTags == null) {
            tabFileTags = DataFileTag.listTags();
        }
        return tabFileTags;
    }

    public FileMetadata getFileMetadataSelectedForTagsPopup() {
        return fileMetadataSelectedForTagsPopup;
    }

    public String getDropBoxSelection() {
        return dropBoxSelection;
    }

    public String getNewCategoryName() {
        return newCategoryName;
    }

    public String getWarningMessageForPopUp() {
        return warningMessageForPopUp;
    }

    public String[] getSelectedTags() {
        return selectedTags;
    }

    public String[] getSelectedTabFileTags() {
        return selectedTabFileTags;
    }

    public List<String> getCategoriesByName() {
        return categoriesByName;
    }

    public DataFile getFileToBeReplaced() {
        return fileToBeReplaced;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public FileReplacePageHelper getFileReplacePageHelper() {
        return fileReplacePageHelper;
    }

    public List<FileMetadata> getSelectedFiles() {
        return selectedFiles;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        dataset = datasetService.find(datasetId);
        fileToBeReplaced = datafileService.find(fileId);

        String permissionError = checkPermissions(dataset, fileToBeReplaced);

        if (!permissionError.isEmpty()) {
            return permissionError;
        }


        AddReplaceFileHelper addReplaceFileHelper = new AddReplaceFileHelper(dvRequestService.getDataverseRequest(),
                                                                             ingestService,
                                                                             datafileService,
                                                                             permissionService,
                                                                             commandEngine);

        fileReplacePageHelper = new FileReplacePageHelper(addReplaceFileHelper,
                                                          dataset,
                                                          fileToBeReplaced);

        return StringUtils.EMPTY;
    }

    public void handleFileUpload(FileUploadEvent event) throws IOException {

        UploadedFile uFile = event.getFile();

        handleReplaceFileUpload(event, uFile.getInputstream(),
                                uFile.getFileName(),
                                uFile.getContentType());

        if (fileReplacePageHelper.hasContentTypeWarning()) {
            RequestContext context = RequestContext.getCurrentInstance();
            RequestContext.getCurrentInstance().update("replaceFileForm:fileTypeDifferentPopup");
            context.execute("PF('fileTypeDifferentPopup').show();");
        }

    }

    public void deleteFiles() {

        try {
            deleteReplacementFile();
        } catch (FileReplaceException ex) {
            Logger.getLogger(EditDatafilesPage.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    public void deleteReplacementFile() throws FileReplaceException {

        if (!fileReplacePageHelper.wasPhase1Successful()) {
            throw new FileReplaceException("Should only be called if Phase 1 was successful");
        }

        fileReplacePageHelper.resetReplaceFileHelper();

        String successMessage = BundleUtil.getStringFromBundle("file.deleted.replacement.success");
        logger.fine(successMessage);
        JsfHelper.addFlashMessage(successMessage);

    }

    public void handleDropBoxUpload(ActionEvent event) {
        if (!uploadInProgress) {
            uploadInProgress = true;
        }
        logger.fine("handleDropBoxUpload");

        // -----------------------------------------------------------
        // Read JSON object from the output of the DropBox Chooser:
        // -----------------------------------------------------------
        JsonReader dbJsonReader = Json.createReader(new StringReader(dropBoxSelection));
        JsonArray dbArray = dbJsonReader.readArray();
        dbJsonReader.close();

        // -----------------------------------------------------------
        // Iterate through the Dropbox file information (JSON)
        // -----------------------------------------------------------
        DataFile dFile = null;
        GetMethod dropBoxMethod = null;
        String localWarningMessage = null;
        for (int i = 0; i < dbArray.size(); i++) {
            JsonObject dbObject = dbArray.getJsonObject(i);

            // -----------------------------------------------------------
            // Parse information for a single file
            // -----------------------------------------------------------
            String fileLink = dbObject.getString("link");
            String fileName = dbObject.getString("name");
            int fileSize = dbObject.getInt("bytes");

            logger.fine("DropBox url: " + fileLink + ", filename: " + fileName + ", size: " + fileSize);


            /* ----------------------------
                Check file size
                - Max size NOT specified in db: default is unlimited
                - Max size specified in db: check too make sure file is within limits
            // ---------------------------- */
            Long fileUploadLimit = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaxFileUploadSizeInBytes);

            if ((fileUploadLimit != null) && (fileSize > fileUploadLimit)) {
                String warningMessage = "Dropbox file \"" + fileName + "\" exceeded the limit of " + fileSize + " bytes and was not uploaded.";
                //msg(warningMessage);
                //FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(), new FacesMessage(FacesMessage.SEVERITY_ERROR, "upload failure", warningMessage));
                if (localWarningMessage == null) {
                    localWarningMessage = warningMessage;
                } else {
                    localWarningMessage = localWarningMessage.concat("; " + warningMessage);
                }
                continue; // skip to next file, and add error mesage
            }


            dropBoxMethod = new GetMethod(fileLink);

            // -----------------------------------------------------------
            // Download the file
            // -----------------------------------------------------------
            InputStream dropBoxStream = this.getDropBoxInputStream(fileLink, dropBoxMethod);
            if (dropBoxStream == null) {
                logger.severe("Could not retrieve dropgox input stream for: " + fileLink);
                continue;  // Error skip this file
            }

            handleReplaceFileUpload(event, dropBoxStream, fileName, ApplicationMimeType.UNDETERMINED_DEFAULT.getMimeValue());
            setFileMetadataSelectedForTagsPopup(fileReplacePageHelper.getNewFileMetadatasBeforeSave().get(0));
            return;

        }

    }

    public String saveReplacement() {
        try {
            return saveReplacementFile();
        } catch (FileReplaceException ex) {
            String errMsg = ex.getMessage();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.save.fail"), errMsg));
            logger.log(Level.SEVERE, "Dataset save failed for replace operation: {0}", errMsg);
            return StringUtils.EMPTY;
        }
    }

    public void saveFileTagsAndCategories() {
        if (fileMetadataSelectedForTagsPopup == null) {
            logger.fine("No FileMetadata selected for the categories popup");
            return;
        }
        // 1. File categories:
        /*
        In order to get the cancel button to work we had to separate the selected tags
        from the file metadata and re-add them on save

        */

        fileMetadataSelectedForTagsPopup.setCategories(new ArrayList<>());

        // New, custom file category (if specified):
        if (newCategoryName != null) {
            logger.fine("Adding new category, " + newCategoryName + " for file " + fileMetadataSelectedForTagsPopup.getLabel());
            fileMetadataSelectedForTagsPopup.addCategoryByName(newCategoryName);
        } else {
            logger.fine("no category specified");
        }
        newCategoryName = null;

        // File Categories selected from the list of existing categories:
        if (selectedTags != null) {
            for (String selectedTag : selectedTags) {

                fileMetadataSelectedForTagsPopup.addCategoryByName(selectedTag);
            }
        }

        fileMetadataSelectedForTagsPopup = null;

    }

    public void clearFileMetadataSelectedForTagsPopup() {
        fileMetadataSelectedForTagsPopup = null;
    }

    public List<FileMetadata> getFileMetadatas() {

        if (fileReplacePageHelper.wasPhase1Successful()) {
            logger.fine("Replace: File metadatas 'list' of 1 from the fileReplacePageHelper.");
            return fileReplacePageHelper.getNewFileMetadatasBeforeSave();
        }

        return Lists.newArrayList();
    }

    public void uploadFinished() {
        // This method is triggered from the page, by the <p:upload ... onComplete=...
        // attribute.
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

        if (uploadInProgress) {
            uploadInProgress = false;
        }

        if (fileReplacePageHelper.hasContentTypeWarning()) {
            RequestContext context = RequestContext.getCurrentInstance();
            RequestContext.getCurrentInstance().update("datasetForm:fileTypeDifferentPopup");
            context.execute("PF('fileTypeDifferentPopup').show();");
        }
    }

    public void uploadStarted() {
        logger.fine("upload started");

        uploadInProgress = true;
    }

    public String getTemporaryPreviewAsBase64(String fileSystemId) {
        return temporaryThumbnailsMap.get(fileSystemId);
    }

    public boolean isDesignatedDatasetThumbnail(FileMetadata fileMetadata) {
        if (fileMetadata != null) {
            if (fileMetadata.getDataFile() != null) {
                if (fileMetadata.getDataFile().getId() != null) {
                    return fileMetadata.getDataFile().equals(dataset.getThumbnailFile());

                }
            }
        }
        return false;
    }

    public void refreshTagsPopUp(FileMetadata fm) {
        setFileMetadataSelectedForTagsPopup(fm);
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

    public void handleFileCategoriesSelection(final AjaxBehaviorEvent event) {
        if (selectedTags != null) {
            selectedTags = selectedTags.clone();
        }
    }

    /**
     * @param fm
     * @todo For consistency, we should disallow users from setting the
     * thumbnail to a restricted file. We enforce this rule in the newer
     * workflow in dataset-widgets.xhtml. The logic to show the "Set Thumbnail"
     * button is in editFilesFragment.xhtml and it would be nice to move it to
     * Java since it's getting long and a bit complicated.
     */
    public void setFileMetadataSelectedForThumbnailPopup(FileMetadata fm) {

    }

    public boolean isThumbnailIsFromDatasetLogoRatherThanDatafile() {
        DatasetThumbnail datasetThumbnail = DatasetUtil.getThumbnail(dataset);
        return datasetThumbnail != null && !datasetThumbnail.isFromDataFile();
    }

    public void setFileMetadataSelectedForIngestOptionsPopup(FileMetadata fm) {
    }

    public boolean isLockedFromEdits() {

        return Try.of(() -> permissionService.checkEditDatasetLock(dataset, dvRequestService.getDataverseRequest(),
                                                                   new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest())))
                .getOrElse(true);
    }

    public String returnToFileLandingPage() {
        Long fileId = fileReplacePageHelper.getFileToReplace().getId();

        if (dataset.getLatestVersion().isDraft()) {
            return "/file.xhtml?fileId=" + fileId + "&version=DRAFT&faces-redirect=true";
        }
        return "/file.xhtml?fileId=" + fileId + "&faces-redirect=true";

    }

    // -------------------- PRIVATE --------------------

    private String checkPermissions(Dataset dataset, DataFile fileToBeReplaced) {

        if (dataset == null || dataset.isHarvested()) {
            return permissionsWrapper.notFound();
        }

        DatasetVersion workingVersion = dataset.getEditVersion();

        if (!workingVersion.isDraft()) {
            return permissionsWrapper.notFound();
        }

        if (!permissionService.on(dataset).has(Permission.EditDataset)) {
            return permissionsWrapper.notAuthorized();
        }

        if (fileToBeReplaced == null) {
            return permissionsWrapper.notFound();
        }

        return StringUtils.EMPTY;
    }

    private InputStream getDropBoxInputStream(String fileLink, GetMethod dropBoxMethod) {

        if (fileLink == null) {
            return null;
        }

        // -----------------------------------------------------------
        // Make http call, download the file:
        // -----------------------------------------------------------
        int status = 0;

        try {
            status = new HttpClient().executeMethod(dropBoxMethod);
            if (status == 200) {
                return dropBoxMethod.getResponseBodyAsStream();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to access DropBox url: {0}!", fileLink);
            return null;
        }

        logger.log(Level.WARNING, "Failed to get DropBox InputStream for file: {0}", fileLink);
        return null;
    }

    /**
     * Save for File Replace operations
     *
     * @return
     * @throws FileReplaceException
     */
    private String saveReplacementFile() throws FileReplaceException {

        if (!fileReplacePageHelper.wasPhase1Successful()) {
            throw new FileReplaceException("Save should only be called when a replacement file has been chosen.  (Phase 1 has to have completed)");

        }

        if (fileReplacePageHelper.runSaveReplacementFile_Phase2()) {
            JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("file.message.replaceSuccess"));

            return returnToFileLandingPageAfterReplace(fileReplacePageHelper.getFirstNewlyAddedFile());
        } else {

            String errMsg = fileReplacePageHelper.getErrorMessages();

            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.save.fail"), errMsg));
            logger.severe("Dataset save failed for replace operation: " + errMsg);
            return StringUtils.EMPTY;
        }

    }

    private void handleReplaceFileUpload(FacesEvent event, InputStream inputStream,
                                         String fileName,
                                         String contentType
    ) {

        fileReplacePageHelper.resetReplaceFileHelper();

        if (fileReplacePageHelper.handleNativeFileUpload(inputStream,
                                                         fileName,
                                                         contentType)) {

            /**
             * If the file content type changed, let the user know
             */
            if (fileReplacePageHelper.hasContentTypeWarning()) {
                //Add warning to popup instead of page for Content Type Difference
                warningMessageForPopUp = fileReplacePageHelper.getContentTypeWarning();
                /*
                    Note on the info messages - upload errors, warnings and success messages:
                    Instead of trying to display the message here (commented out code below),
                    we only save the message, as a string - and it will be displayed by
                    the uploadFinished() method, triggered next, after the upload event
                    is processed and, as the name suggests, finished.
                    This is done in 2 stages like this so that when the upload component
                    is called for large numbers of files, in multiple mode, the page could
                    be updated and re-rendered just once, after all the uploads are finished -
                    and not after each individual upload. Of course for the "replace" upload
                    there is always only one... but we have to use this scheme for
                    consistency. -- L.A. 4.6.1

                */
                //FacesContext.getCurrentInstance().addMessage(
                //        uploadComponentId,
                //        new FacesMessage(FacesMessage.SEVERITY_ERROR, "upload warning", uploadWarningMessage));
            }
            // See the comment above, on how upload messages are displayed.

            // Commented out the success message below - since we probably don't
            // need it - the state of the page will indicate the success fairly
            // unambiguously: the primefaces upload and the dropbox upload components
            // will become disabled, and the uploaded file will appear on the page.
            // But feel free to un-comment it, if you feel it could be useful.
            // -- L.A. 4.6.1
            //uploadSuccessMessage = "Hey! It worked!";

        }
    }

    private void refreshCategoriesByName() {
        categoriesByName = new ArrayList<>();
        categoriesByName.addAll(dataset.getCategoriesByName());
        refreshSelectedTags();
    }

    private void refreshSelectedTags() {
        selectedTags = new String[0];
        List<String> selectedCategoriesByName = new ArrayList<>();

        if (fileMetadataSelectedForTagsPopup.getCategories() != null) {
            for (int i = 0; i < fileMetadataSelectedForTagsPopup.getCategories().size(); i++) {
                if (!selectedCategoriesByName.contains(fileMetadataSelectedForTagsPopup.getCategories().get(i).getName())) {
                    selectedCategoriesByName.add(fileMetadataSelectedForTagsPopup.getCategories().get(i).getName());
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

    private void refreshTabFileTagsByName() {
        tabFileTagsByName = new ArrayList<>();
        if (fileMetadataSelectedForTagsPopup.getDataFile().getTags() != null) {
            for (int i = 0; i < fileMetadataSelectedForTagsPopup.getDataFile().getTags().size(); i++) {
                tabFileTagsByName.add(fileMetadataSelectedForTagsPopup.getDataFile().getTags().get(i).getTypeLabel());
            }
        }
        refreshSelectedTabFileTags();
    }

    private void refreshSelectedTabFileTags() {
        selectedTabFileTags = new String[0];
        if (tabFileTagsByName.size() > 0) {
            selectedTabFileTags = new String[tabFileTagsByName.size()];
            for (int i = 0; i < tabFileTagsByName.size(); i++) {
                selectedTabFileTags[i] = tabFileTagsByName.get(i);
            }
        }
        Arrays.sort(selectedTabFileTags);
    }

    private String returnToFileLandingPageAfterReplace(DataFile newFile) {

        if (newFile == null) {
            throw new NullPointerException("newFile cannot be null!");
        }

        return "/file.xhtml?fileId=" + newFile.getId() + "&version=DRAFT&faces-redirect=true";
    }

    // -------------------- SETTERS --------------------

    public void setFileMetadataSelectedForTagsPopup(FileMetadata fm) {
        fileMetadataSelectedForTagsPopup = fm;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public void setDatasetId(long datasetId) {
        this.datasetId = datasetId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public void setSelectedFiles(List<FileMetadata> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }

    public void setSelectedTags(String[] selectedTags) {
        this.selectedTags = selectedTags;
    }

    public void setNewCategoryName(String newCategoryName) {
        this.newCategoryName = newCategoryName;
    }

    public void setDropBoxSelection(String dropBoxSelection) {
        this.dropBoxSelection = dropBoxSelection;
    }
}
