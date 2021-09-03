package edu.harvard.iq.dataverse.datafile.file;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.files.mime.ApplicationMimeType;
import edu.harvard.iq.dataverse.datafile.file.exception.FileReplaceException;
import edu.harvard.iq.dataverse.datafile.file.exception.FileReplaceException.Reason;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
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
import java.util.Collection;
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
    private DatasetDao datasetDao;
    private DataFileServiceBean datafileService;
    private DataverseRequestServiceBean dvRequestService;
    private SettingsServiceBean settingsService;
    private ReplaceFileHandler replaceFileHandler;

    private long datasetId;
    private long fileId;
    private Dataset dataset;
    private DataFile fileToBeReplaced;
    private DataFile fileToBeSaved;
    private Map<String, String> temporaryThumbnailsMap = new HashMap<>();
    private List<String> tabFileTags;
    private FileMetadata fileMetadataSelectedForTagsPopup;
    private boolean uploadInProgress;
    private String dropBoxSelection = "";

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated /* JEE requirement*/
    public ReplaceDatafilesPage() {
    }

    @Inject
    public ReplaceDatafilesPage(ReplaceFileHandler replaceFileHandler, PermissionsWrapper permissionsWrapper, PermissionServiceBean permissionService,
                                DatasetDao datasetDao, DataFileServiceBean datafileService, DataverseRequestServiceBean dvRequestService,
                                SettingsServiceBean settingsService) {
        this.replaceFileHandler = replaceFileHandler;
        this.permissionsWrapper = permissionsWrapper;
        this.permissionService = permissionService;
        this.datasetDao = datasetDao;
        this.datafileService = datafileService;
        this.dvRequestService = dvRequestService;
        this.settingsService = settingsService;
    }

    // -------------------- GETTERS --------------------

    public long getDatasetId() {
        return datasetId;
    }

    public DataFile getFileToBeSaved() {
        return fileToBeSaved;
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

    public DataFile getFileToBeReplaced() {
        return fileToBeReplaced;
    }

    public Dataset getDataset() {
        return dataset;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        dataset = datasetDao.find(datasetId);
        fileToBeReplaced = datafileService.find(fileId);

        String permissionError = checkPermissions(dataset, fileToBeReplaced);

        if (!permissionError.isEmpty()) {
            return permissionError;
        }

        return StringUtils.EMPTY;

    }

    public String handleFileUpload(FileUploadEvent event) {

        UploadedFile uFile = event.getFile();

        Try<DataFile> dataFile = Try.withResources(() -> uFile.getInputStream())
                .of(inputStream -> replaceFileHandler.createDataFile(dataset,
                                                                                inputStream,
                                                                                uFile.getFileName(),
                                                                                uFile.getContentType()));

        if (isUploadedFileContainsErrors(dataFile)) {
            return StringUtils.EMPTY;
        }

        fileToBeSaved = dataFile.get();

        return StringUtils.EMPTY;
    }

    public boolean isUploadedFileContainsErrors(Try<DataFile> uploadedFile) {

        if (uploadedFile.isFailure()) {

            if (uploadedFile.getCause() instanceof FileReplaceException) {
                FileReplaceException.Reason reason = ((FileReplaceException) uploadedFile.getCause()).getReason();
                if (reason == Reason.ZIP_NOT_SUPPORTED) {
                    JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("file.addreplace.error.file_is_zip"), "");
                } else if (reason == Reason.VIRUS_DETECTED){
                    JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("file.addreplace.error.virus_detected"), "");
                }
            } else {
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("file.addreplace.error.generic"), "");
            }
            return true;
        }

        if (uploadedFile.get().getChecksumValue().equals(fileToBeReplaced.getChecksumValue())) {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("file.addreplace.error.replace.new_file_same_as_replacement"), "");
            return true;
        }

        return false;
    }

    public void deleteFiles() {
        deleteReplacementFile();
    }

    public void deleteReplacementFile() {

        fileToBeSaved = null;
        uploadInProgress = false;

        String successMessage = BundleUtil.getStringFromBundle("file.deleted.replacement.success");
        logger.info(successMessage);
        JsfHelper.addFlashSuccessMessage(successMessage);

    }

    public String handleDropBoxUpload(ActionEvent event) {
        if (!uploadInProgress) {
            uploadInProgress = true;
        }
        logger.info("handleDropBoxUpload");

        // -----------------------------------------------------------
        // Read JSON object from the output of the DropBox Chooser:
        // -----------------------------------------------------------
        JsonReader dbJsonReader = Json.createReader(new StringReader(dropBoxSelection));
        JsonArray dbArray = dbJsonReader.readArray();
        dbJsonReader.close();

        // -----------------------------------------------------------
        // Iterate through the Dropbox file information (JSON)
        // -----------------------------------------------------------
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
                // TODO: add error mesage for user
                continue; // skip to next file
            }

            GetMethod dropBoxMethod = new GetMethod(fileLink);

            Try<DataFile> dataFile = Try.withResources(() -> this.getDropBoxContent(dropBoxMethod))
               .of(dropBoxContent -> replaceFileHandler.createDataFile(dataset,
                        dropBoxContent,
                        fileName,
                        ApplicationMimeType.UNDETERMINED_DEFAULT.getMimeValue()))
               .andFinally(() -> dropBoxMethod.releaseConnection());

            if (isUploadedFileContainsErrors(dataFile)) {
                return StringUtils.EMPTY;
            }

            fileToBeSaved = dataFile.get();

            setFileMetadataSelectedForTagsPopup(fileToBeSaved.getFileMetadata());

        }

        return StringUtils.EMPTY;
    }

    public String saveReplacement() {
        if (!fileToBeSaved.getContentType().equals(fileToBeReplaced.getContentType())) {
            PrimeFaces.current().ajax().update("replaceFileForm:fileTypeDifferentPopup");
            PrimeFaces.current().executeScript("PF('fileTypeDifferentPopup').show();");
            return StringUtils.EMPTY;
        }
        return saveReplacementFile();
    }

    public String saveReplacementNoFileTypeCheck() {
        return saveReplacementFile();
    }

    public void saveFileTagsAndCategories(FileMetadata selectedFile,
                                          Collection<String> selectedFileMetadataTags,
                                          Collection<String> selectedDataFileTags) {

        selectedFile.getCategories().clear();
        selectedFileMetadataTags.forEach(selectedFile::addCategoryByName);

        setTagsForTabularData(selectedDataFileTags, selectedFile);

    }

    public List<FileMetadata> getFileMetadatas() {

        if (fileToBeSaved != null) {
            logger.fine("Replace: File metadatas 'list' of 1 from the fileReplacePageHelper.");
            return Lists.newArrayList(fileToBeSaved.getFileMetadata());
        }

        return Lists.newArrayList();
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
    }

    public boolean isLockedFromEdits() {

        return Try.of(() -> permissionService.checkEditDatasetLock(dataset, dvRequestService.getDataverseRequest(),
                                                                   new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest())))
                .getOrElse(true);
    }

    public String getDifferentContentTypeMessage() {
        String fileToBeSavedType = fileToBeSaved == null ? "" : fileToBeSaved.getFriendlyType();

        return BundleUtil.getStringFromBundle("file.addreplace.error.replace.new_file_has_different_content_type",
                                              fileToBeReplaced.getFriendlyType(), fileToBeSavedType);
    }

    public String returnToFileLandingPage() {
        Long fileId = fileToBeReplaced.getId();

        if (dataset.getLatestVersion().isDraft()) {
            return "/file.xhtml?fileId=" + fileId + "&version=DRAFT&faces-redirect=true";
        }
        return "/file.xhtml?fileId=" + fileId + "&faces-redirect=true";

    }

    public Long getMaxFileUploadSizeInBytes() {
        return settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaxFileUploadSizeInBytes);
    }

    public String getPageTitle() {
        return BundleUtil.getStringFromBundle("file.replaceFile") + " - " + dataset.getEditVersion().getParsedTitle();
    }

    // -------------------- PRIVATE --------------------

    private String checkPermissions(Dataset dataset, DataFile fileToBeReplaced) {

        if (dataset == null || dataset.isHarvested()) {
            return permissionsWrapper.notFound();
        }

        DatasetVersion workingVersion = dataset.getEditVersion();

        if (!isFileInDataset(workingVersion, fileToBeReplaced)) {
            return permissionsWrapper.notAuthorized();
        }

        if (!workingVersion.isDraft()) {
            return permissionsWrapper.notFound();
        }

        if (!permissionsWrapper.canCurrentUserUpdateDataset(dataset)) {
            return permissionsWrapper.notAuthorized();
        }

        if (fileToBeReplaced == null) {
            return permissionsWrapper.notFound();
        }

        return StringUtils.EMPTY;
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

    private boolean isFileInDataset(DatasetVersion datasetVersion, DataFile fileToCheck) {
        for (FileMetadata fileMetadata : datasetVersion.getFileMetadatas()) {
            if (fileMetadata.getDataFile().equals(fileToCheck)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Download a file from drop box
     *
     * @param dropBoxMethod
     * @return
     */
    private InputStream getDropBoxContent(GetMethod dropBoxMethod) throws IOException {
        // -----------------------------------------------------------
        // Make http call, download the file:
        // -----------------------------------------------------------
        int status = 0;

        try {
            HttpClient httpclient = new HttpClient();
            status = httpclient.executeMethod(dropBoxMethod);
            if (status != 200) {
                logger.log(Level.WARNING, "Failed to get DropBox InputStream for file: {0}, status code: {1}", new Object[] {dropBoxMethod.getPath(), status});
                throw new IOException("Non 200 status code returned from dropbox");
            }
            return dropBoxMethod.getResponseBodyAsStream();
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to access DropBox url: {0}!", dropBoxMethod.getPath());
            throw ex;
        }
    }

    /**
     * Save for File Replace operations
     */
    private String saveReplacementFile() {

        Try<DataFile> replacedFileOperation = Try.of(() -> replaceFileHandler.replaceFile(fileToBeReplaced, dataset, fileToBeSaved));

        if (replacedFileOperation.isFailure()) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                                                                BundleUtil.getStringFromBundle("dataset.save.fail"),
                                                                                ""));
            logger.severe("Dataset save failed for replace operation" + fileToBeReplaced.getDisplayName());
            return StringUtils.EMPTY;
        }

        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("file.message.replaceSuccess"));

        return returnToFileLandingPageAfterReplace(replacedFileOperation.get());
    }

    private String returnToFileLandingPageAfterReplace(DataFile newFile) {

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

    public void setDropBoxSelection(String dropBoxSelection) {
        this.dropBoxSelection = dropBoxSelection;
    }
}
