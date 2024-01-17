/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetVersionServiceBean.RetrieveDatasetVersionResponse;
import edu.harvard.iq.dataverse.dataaccess.SwiftAccessIO;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RestrictFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.export.ExportService;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean.MakeDataCountEntry;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.validation.ConstraintViolation;

import org.primefaces.PrimeFaces;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author skraffmi
 * 
 */

@ViewScoped
@Named("FilePage")
public class FilePage implements java.io.Serializable {
    
    private FileMetadata fileMetadata;
    private Long fileId;  
    private String version;
    private String toolType;
    private DataFile file;   
    private GuestbookResponse guestbookResponse;
    private int selectedTabIndex;
    private Dataset editDataset;
    private Dataset dataset;
    private List<DatasetVersion> datasetVersionsForTab;
    private List<FileMetadata> fileMetadatasForTab;
    private String persistentId;
    private List<ExternalTool> configureTools;
    private List<ExternalTool> exploreTools;
    private List<ExternalTool> toolsWithPreviews;
    private List<ExternalTool> queryTools;
    private Long datasetVersionId;
    /**
     * Have the terms been met so that the Preview tab can show the preview?
     */
    private boolean termsMet;

    @EJB
    DataFileServiceBean datafileService;
    
    @EJB
    DatasetVersionServiceBean datasetVersionService;

    @EJB
    PermissionServiceBean permissionService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    FileDownloadServiceBean fileDownloadService;
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    @EJB
    AuthenticationServiceBean authService;
    
    @EJB
    DatasetServiceBean datasetService;
    
    @EJB
    SystemConfig systemConfig;


    @Inject
    DataverseSession session;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    ExternalToolServiceBean externalToolService;
    @EJB
    PrivateUrlServiceBean privateUrlService;
    @EJB
    AuxiliaryFileServiceBean auxiliaryFileService;

    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @Inject
    FileDownloadHelper fileDownloadHelper;
    @Inject
    MakeDataCountLoggingServiceBean mdcLogService;
    @Inject
    SettingsWrapper settingsWrapper;
    @Inject
    EmbargoServiceBean embargoService;

    private static final Logger logger = Logger.getLogger(FilePage.class.getCanonicalName());

    private boolean fileDeleteInProgress = false;
    public String init() {
     
        
        if (fileId != null || persistentId != null) {
            // ---------------------------------------
            // Set the file and datasetVersion 
            // ---------------------------------------           
            if (fileId != null) {
                file = datafileService.find(fileId);

            } else if (persistentId != null) {
                file = datafileService.findByGlobalId(persistentId);
                if (file != null) {
                    fileId = file.getId();
                }

            }

            if (file == null || fileId == null) {
                return permissionsWrapper.notFound();
            }

            // Is the Dataset harvested?
            if (file.getOwner().isHarvested()) {
                // if so, we'll simply forward to the remote URL for the original
                // source of this harvested dataset:
                String originalSourceURL = file.getOwner().getRemoteArchiveURL();
                if (originalSourceURL != null && !originalSourceURL.equals("")) {
                    logger.fine("redirecting to " + originalSourceURL);
                    try {
                        FacesContext.getCurrentInstance().getExternalContext().redirect(originalSourceURL);
                    } catch (IOException ioex) {
                        // must be a bad URL...
                        // we don't need to do anything special here - we'll redirect
                        // to the local 404 page, below.
                        logger.warning("failed to issue a redirect to " + originalSourceURL);
                    }
                }

                return permissionsWrapper.notFound();
            }
            RetrieveDatasetVersionResponse retrieveDatasetVersionResponse;
            Long getDatasetVersionID = null;
            if (datasetVersionId == null) {
                retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(file.getOwner().getVersions(), version);
                getDatasetVersionID = retrieveDatasetVersionResponse.getDatasetVersion().getId();
            } else {
                getDatasetVersionID = datasetVersionId;
            }
            fileMetadata = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(getDatasetVersionID, fileId);

            if (fileMetadata == null) {
                logger.fine("fileMetadata is null! Checking finding most recent version file was in.");
                fileMetadata = datafileService.findMostRecentVersionFileIsIn(file);
                if (fileMetadata == null) {
                    return permissionsWrapper.notFound();
                }
            }
            
            // If this DatasetVersion is unpublished and permission is doesn't have permissions:
            //  > Go to the Login page
            //
            // Check permisisons       
            Boolean authorized = (fileMetadata.getDatasetVersion().isReleased())
                    || (!fileMetadata.getDatasetVersion().isReleased() && this.canViewUnpublishedDataset());

            if (!authorized) {
                return permissionsWrapper.notAuthorized();
            }
            
            //termsOfAccess = fileMetadata.getDatasetVersion().getTermsOfUseAndAccess().getTermsOfAccess();
            //fileAccessRequest = fileMetadata.getDatasetVersion().getTermsOfUseAndAccess().isFileAccessRequest();

            this.guestbookResponse = this.guestbookResponseService.initGuestbookResponseForFragment(fileMetadata, session);

            if(fileMetadata.getDatasetVersion().isPublished()) {
                MakeDataCountEntry entry = new MakeDataCountEntry(FacesContext.getCurrentInstance(), dvRequestService, fileMetadata.getDatasetVersion());
                mdcLogService.logEntry(entry);
            }

           
            // Find external tools based on their type, the file content type, and whether
            // ingest has created a derived file for that type
            // Currently, tabular data files are the only type of derived file created, so
            // isTabularData() works - true for tabular types where a .tab file has been
            // created and false for other mimetypes
            String contentType = file.getContentType();
            //For tabular data, indicate successful ingest by returning a contentType for the derived .tab file
            if (file.isTabularData()) {
                contentType=DataFileServiceBean.MIME_TYPE_TSV_ALT;
            }
            configureTools = externalToolService.findFileToolsByTypeAndContentType(ExternalTool.Type.CONFIGURE, contentType);
            exploreTools = externalToolService.findFileToolsByTypeAndContentType(ExternalTool.Type.EXPLORE, contentType);
            queryTools = externalToolService.findFileToolsByTypeAndContentType(ExternalTool.Type.QUERY, contentType);
            Collections.sort(exploreTools, CompareExternalToolName);
            toolsWithPreviews  = sortExternalTools();

            if (toolType != null) {
                if (toolType.equals("PREVIEW")) {
                    if (!toolsWithPreviews.isEmpty()) {
                        setSelectedTool(toolsWithPreviews.get(0));
                    }
                }
                if (toolType.equals("QUERY")) {
                    if (!queryTools.isEmpty()) {
                        setSelectedTool(queryTools.get(0));
                    }
                }
            } else {
                if (!getAllAvailableTools().isEmpty()){
                    setSelectedTool(getAllAvailableTools().get(0));
                }
            }

        } else {
            return permissionsWrapper.notFound();
        }
        
        hasRestrictedFiles = fileMetadata.getDatasetVersion().isHasRestrictedFile();
        hasValidTermsOfAccess = null;
        hasValidTermsOfAccess = isHasValidTermsOfAccess();
        if(!hasValidTermsOfAccess && canUpdateDataset() ){
            JsfHelper.addWarningMessage(BundleUtil.getStringFromBundle("dataset.message.editMetadata.invalid.TOUA.message"));
        }
        
        displayPublishMessage();
        return null;
    }
    
    private void displayPublishMessage(){
        if (fileMetadata.getDatasetVersion().isDraft()  && canUpdateDataset()
                &&   (canPublishDataset() || !fileMetadata.getDatasetVersion().getDataset().isLockedFor(DatasetLock.Reason.InReview))){
            JsfHelper.addWarningMessage(datasetService.getReminderString(fileMetadata.getDatasetVersion().getDataset(), canPublishDataset(), true, isValid()));
        }               
    }
    
    public boolean isValid() {
        if (!fileMetadata.getDatasetVersion().isDraft()) {
            return true;
        }
        DatasetVersion newVersion = fileMetadata.getDatasetVersion().cloneDatasetVersion();
        newVersion.setDatasetFields(newVersion.initDatasetFields());
        return newVersion.isValid();
    }

    private boolean canViewUnpublishedDataset() {
        return permissionsWrapper.canViewUnpublishedDataset( dvRequestService.getDataverseRequest(), fileMetadata.getDatasetVersion().getDataset());
    }
    
    public boolean canPublishDataset(){
        return permissionsWrapper.canIssuePublishDatasetCommand(fileMetadata.getDatasetVersion().getDataset());
    }
   

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    public Long getDatasetVersionId() {
        return datasetVersionId;
    }

    public void setDatasetVersionId(Long datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }

    // findPreviewTools would be a better name
    private List<ExternalTool> sortExternalTools(){
        List<ExternalTool> retList = new ArrayList<>();
        List<ExternalTool> previewTools = externalToolService.findFileToolsByTypeAndContentType(ExternalTool.Type.PREVIEW, file.getContentType());
        for (ExternalTool previewTool : previewTools) {
            if (externalToolService.meetsRequirements(previewTool, file)) {
                retList.add(previewTool);
            }
        }
        Collections.sort(retList, CompareExternalToolName);
        return retList;
    }
    
    private String termsGuestbookPopupAction = "";

    public void setTermsGuestbookPopupAction(String popupAction){
        if(popupAction != null && popupAction.length() > 0){
            logger.info("TGPA set to " + popupAction);
            this.termsGuestbookPopupAction = popupAction;
        }

    }

    public String getTermsGuestbookPopupAction(){
        return termsGuestbookPopupAction;
    }

    public boolean isDownloadPopupRequired() {  
        if(fileMetadata.getId() == null || fileMetadata.getDatasetVersion().getId() == null ){
            return false;
        }
        return FileUtil.isDownloadPopupRequired(fileMetadata.getDatasetVersion());
    }
    
    public boolean isRequestAccessPopupRequired() {  
        if(fileMetadata.getId() == null || fileMetadata.getDatasetVersion().getId() == null ){
            return false;
        }
        return FileUtil.isRequestAccessPopupRequired(fileMetadata.getDatasetVersion());
    }

    public boolean isGuestbookAndTermsPopupRequired() {  
        if(fileMetadata.getId() == null || fileMetadata.getDatasetVersion().getId() == null ){
            return false;
        }
        return FileUtil.isGuestbookAndTermsPopupRequired(fileMetadata.getDatasetVersion());
    }
    
    public boolean isGuestbookPopupRequiredAtDownload(){
        // Only show guestbookAtDownload if guestbook at request is disabled (legacy behavior)
        DatasetVersion workingVersion = fileMetadata.getDatasetVersion();
        return FileUtil.isGuestbookPopupRequired(workingVersion) && !workingVersion.getDataset().getEffectiveGuestbookEntryAtRequest();
    }

    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public DataFile getFile() {
        return file;
    }

    public void setFile(DataFile file) {
        this.file = file;
    }
    
    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
     
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
    public List< String[]> getExporters(){
        List<String[]> retList = new ArrayList<>();
        String myHostURL = systemConfig.getDataverseSiteUrl();
        for (String [] provider : ExportService.getInstance().getExportersLabels() ){
            String formatName = provider[1];
            String formatDisplayName = provider[0];
            
            Exporter exporter = null; 
            try {
                exporter = ExportService.getInstance().getExporter(formatName);
            } catch (ExportException ex) {
                exporter = null;
            }
            if (exporter != null && exporter.isAvailableToUsers()) {
                // Not all metadata exports should be presented to the web users!
                // Some are only for harvesting clients.
                
                String[] temp = new String[2];
                temp[0] = formatDisplayName;
                temp[1] = myHostURL + "/api/datasets/export?exporter=" + formatName + "&persistentId=" + fileMetadata.getDatasetVersion().getDataset().getGlobalId().asString();
                retList.add(temp);
            }
        }
        return retList;  
    }
    
    public String saveProvFreeform(String freeformTextInput, DataFile dataFileFromPopup) throws CommandException {
        editDataset = this.file.getOwner();
        file.setProvEntityName(dataFileFromPopup.getProvEntityName()); //passing this value into the file being saved here is pretty hacky.
        Command cmd;
        
        for (FileMetadata fmw : editDataset.getOrCreateEditVersion().getFileMetadatas()) {
            if (fmw.getDataFile().equals(this.fileMetadata.getDataFile())) {
                cmd = new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(), file, freeformTextInput);
                commandEngine.submit(cmd);
            }
        }
        
        save();
        init();
        return returnToDraftVersion();
    }
    
    public String restrictFile(boolean restricted) throws CommandException{
        String fileNames = null;
        editDataset = this.file.getOwner();
        if (restricted) { // get values from access popup
            editDataset.getOrCreateEditVersion().getTermsOfUseAndAccess().setTermsOfAccess(termsOfAccess);
            editDataset.getOrCreateEditVersion().getTermsOfUseAndAccess().setFileAccessRequest(fileAccessRequest);   
        }
        //using this method to update the terms for datasets that are out of compliance 
        // with Terms of Access requirement - may get her with a file that is already restricted
        // we'll allow it 
        try {
            Command cmd;
            for (FileMetadata fmw : editDataset.getOrCreateEditVersion().getFileMetadatas()) {
                if (fmw.getDataFile().equals(this.fileMetadata.getDataFile())) {
                    fileNames += fmw.getLabel();
                    cmd = new RestrictFileCommand(fmw.getDataFile(), dvRequestService.getDataverseRequest(), restricted);
                    commandEngine.submit(cmd);
                }
            }

        } catch (CommandException ex) {
            if (ex.getLocalizedMessage().contains("is already restricted")) {
                //ok we're just updating the terms here
            } else {
                throw ex;
            }
        }   
        if (fileNames != null) {
            String successMessage = BundleUtil.getStringFromBundle("file.restricted.success");
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);
        }
        save();
        init();
        return returnToDraftVersion();
    }
    
    private List<FileMetadata> filesToBeDeleted = new ArrayList<>();

    public String deleteFile() {

        String fileNames = this.getFileMetadata().getLabel();

        editDataset = this.getFileMetadata().getDataFile().getOwner();

        FileMetadata markedForDelete = null;

        for (FileMetadata fmd : editDataset.getOrCreateEditVersion().getFileMetadatas()) {

            if (fmd.getDataFile().getId().equals(fileId)) {
                markedForDelete = fmd;
            }
        }

        if (markedForDelete.getId() != null) {
            // the file already exists as part of this dataset
            // so all we remove is the file from the fileMetadatas (for display)
            // and let the delete be handled in the command (by adding it to the filesToBeDeleted list
            editDataset.getOrCreateEditVersion().getFileMetadatas().remove(markedForDelete);
            filesToBeDeleted.add(markedForDelete);

        } else {
            List<FileMetadata> filesToKeep = new ArrayList<>();
            for (FileMetadata fmo : editDataset.getOrCreateEditVersion().getFileMetadatas()) {
                if (!fmo.getDataFile().getId().equals(this.getFile().getId())) {
                    filesToKeep.add(fmo);
                }
            }
            editDataset.getOrCreateEditVersion().setFileMetadatas(filesToKeep);
        }

        fileDeleteInProgress = true;
        save();
        return returnToDatasetOnly();

    }
    
    private int activeTabIndex;

    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    public void setActiveTabIndex(int activeTabIndex) {
        this.activeTabIndex = activeTabIndex;
    }
    
    public void tabChanged(TabChangeEvent event) {
        TabView tv = (TabView) event.getComponent();
        this.activeTabIndex = tv.getActiveIndex();
        if (this.activeTabIndex == 1 || this.activeTabIndex == 2 ) {
            setFileMetadatasForTab(loadFileMetadataTabList());
        } else {
            setFileMetadatasForTab( new ArrayList<>());         
        }
    }
    
    
    private List<FileMetadata> loadFileMetadataTabList() {
        List<DataFile> allfiles = allRelatedFiles();
        List<FileMetadata> retList = new ArrayList<>();
        for (DatasetVersion versionLoop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            boolean foundFmd = false;
            
            if (versionLoop.isReleased() || versionLoop.isDeaccessioned() || permissionService.on(fileMetadata.getDatasetVersion().getDataset()).has(Permission.ViewUnpublishedDataset)) {
                foundFmd = false;
                for (DataFile df : allfiles) {
                    FileMetadata fmd = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(versionLoop.getId(), df.getId());
                    if (fmd != null) {
                        fmd.setContributorNames(datasetVersionService.getContributorsNames(versionLoop));
                        FileVersionDifference fvd = new FileVersionDifference(fmd, getPreviousFileMetadata(fmd));
                        fmd.setFileVersionDifference(fvd);
                        retList.add(fmd);
                        foundFmd = true;
                        break;
                    }
                }
                //no File metadata found make dummy one
                if (!foundFmd) {
                    FileMetadata dummy = new FileMetadata();
                    dummy.setDatasetVersion(versionLoop);
                    dummy.setDataFile(null);
                    FileVersionDifference fvd = new FileVersionDifference(dummy, getPreviousFileMetadata(versionLoop));
                    dummy.setFileVersionDifference(fvd);
                    retList.add(dummy);
                }
            }
        }
        return retList;
    }
    
    private FileMetadata getPreviousFileMetadata(DatasetVersion currentversion) {
        List<DataFile> allfiles = allRelatedFiles();
        boolean foundCurrent = false;
        DatasetVersion priorVersion = null;
        for (DatasetVersion versionLoop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            if (foundCurrent) {
                priorVersion = versionLoop;
                break;
            }
            if (versionLoop.equals(currentversion)) {
                foundCurrent = true;
            }

        }
        if (priorVersion != null && priorVersion.getFileMetadatasSorted() != null) {
            for (FileMetadata fmdTest : priorVersion.getFileMetadatasSorted()) {
                for (DataFile fileTest : allfiles) {
                    if (fmdTest.getDataFile().equals(fileTest)) {
                        return fmdTest;
                    }
                }
            }
        }

        return null;

    }
    
    private FileMetadata getPreviousFileMetadata(FileMetadata fmdIn){
        
        DataFile dfPrevious = datafileService.findPreviousFile(fmdIn.getDataFile());
        DatasetVersion dvPrevious = null;
        boolean gotCurrent = false;
        for (DatasetVersion dvloop: fileMetadata.getDatasetVersion().getDataset().getVersions()){
            if(gotCurrent){
                dvPrevious  = dvloop;
                break;
            }
             if(dvloop.equals(fmdIn.getDatasetVersion())){
                 gotCurrent = true;
             }
        } 
        
        List<DataFile> allfiles = allRelatedFiles();
        
        if (dvPrevious != null && dvPrevious.getFileMetadatasSorted() != null) {
            for (FileMetadata fmdTest : dvPrevious.getFileMetadatasSorted()) {
                for (DataFile fileTest : allfiles) {
                    if (fmdTest.getDataFile().equals(fileTest)) {
                        return fmdTest;
                    }
                }
            }
        }
        
        Long dfId = fmdIn.getDataFile().getId();
        if (dfPrevious != null){
            dfId = dfPrevious.getId();
        }
        Long versionId = null;       
        if (dvPrevious !=null){
            versionId = dvPrevious.getId();
        }
        
        FileMetadata fmd = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(versionId, dfId);
        
        return fmd;
    }
    
    public List<FileMetadata> getFileMetadatasForTab() {
        return fileMetadatasForTab;
    }

    public void setFileMetadatasForTab(List<FileMetadata> fileMetadatasForTab) {
        this.fileMetadatasForTab = fileMetadatasForTab;
    }
    
    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }
    
    
    public List<DatasetVersion> getDatasetVersionsForTab() {
        return datasetVersionsForTab;
    }

    public void setDatasetVersionsForTab(List<DatasetVersion> datasetVersionsForTab) {
        this.datasetVersionsForTab = datasetVersionsForTab;
    }

    public boolean isTermsMet() {
        return termsMet;
    }

    public void setTermsMet(boolean termsMet) {
        this.termsMet = termsMet;
    }

    public String save() {
        // Validate
        Set<ConstraintViolation> constraintViolations = editDataset.getOrCreateEditVersion().validate();
        if (!constraintViolations.isEmpty()) {
             //JsfHelper.addFlashMessage(JH.localize("dataset.message.validationError"));
             fileDeleteInProgress = false;
             JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.message.validationError"));
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "See below for details."));
            return "";
        }
               

        Command<Dataset> cmd;
        boolean updateCommandSuccess = false;
        Long deleteFileId = null;
        String deleteStorageLocation = null;

        if (!filesToBeDeleted.isEmpty()) { 
            // We want to delete the file (there's always only one file with this page)
            editDataset.getOrCreateEditVersion().getFileMetadatas().remove(filesToBeDeleted.get(0));
            deleteFileId = filesToBeDeleted.get(0).getDataFile().getId();
            deleteStorageLocation = datafileService.getPhysicalFileToDelete(filesToBeDeleted.get(0).getDataFile());
        }
        
        try {
            cmd = new UpdateDatasetVersionCommand(editDataset, dvRequestService.getDataverseRequest(), filesToBeDeleted);
            commandEngine.submit(cmd);
            updateCommandSuccess = true;

        } catch (EJBException ex) {
            
            StringBuilder error = new StringBuilder();
            error.append(ex).append(" ");
            error.append(ex.getMessage()).append(" ");
            
            
            Throwable cause = ex;
            while (cause.getCause()!= null) {
                cause = cause.getCause();
                error.append(cause).append(" ");
                error.append(cause.getMessage()).append(" ");
            }
            return null;
        } catch (CommandException ex) {
            fileDeleteInProgress = false;
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.save.fail"), " - " + ex.toString()));
            return null;
        }


        if (fileDeleteInProgress) {
            
            if (updateCommandSuccess) {
                if (deleteStorageLocation != null) {
                    // Finalize the delete of the physical file 
                    // (File service will double-check that the datafile no 
                    // longer exists in the database, before proceeding to 
                    // delete the physical file)
                    try {
                        datafileService.finalizeFileDelete(deleteFileId, deleteStorageLocation);
                    } catch (IOException ioex) {
                        logger.warning("Failed to delete the physical file associated with the deleted datafile id="
                                + deleteFileId + ", storage location: " + deleteStorageLocation);
                    }
                }
            }
            
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("file.message.deleteSuccess"));
            fileDeleteInProgress = false;
        } else {
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("file.message.editSuccess"));
        }
        
        setVersion("DRAFT");
        return "";
    }
    
    private Boolean thumbnailAvailable = null; 
    
    public boolean isThumbnailAvailable(FileMetadata fileMetadata) {
        // new and optimized logic: 
        // - check download permission here (should be cached - so it's free!)
        // - only then ask the file service if the thumbnail is available/exists.
        // the service itself no longer checks download permissions.
        // (Also, cache the result the first time the check is performed... 
        // remember - methods referenced in "rendered=..." attributes are 
        // called *multiple* times as the page is loading!)
        
        if (thumbnailAvailable != null) {
            return thumbnailAvailable;
        }
                
        if (!fileDownloadHelper.canDownloadFile(fileMetadata)) {
            thumbnailAvailable = false;
        } else {
            thumbnailAvailable = datafileService.isThumbnailAvailable(fileMetadata.getDataFile());
        }
        
        return thumbnailAvailable;
    }
    
    private String returnToDatasetOnly(){
        
         return "/dataset.xhtml?persistentId=" + editDataset.getGlobalId().asString()  + "&version=DRAFT" + "&faces-redirect=true";   
    }
    
    private String returnToDraftVersion(){ 
        
         return "/file.xhtml?fileId=" + fileId + "&version=DRAFT&faces-redirect=true";    
    }
    
    public FileDownloadServiceBean getFileDownloadService() {
        return fileDownloadService;
    }

    public void setFileDownloadService(FileDownloadServiceBean fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }
    
    
    public GuestbookResponseServiceBean getGuestbookResponseService() {
        return guestbookResponseService;
    }

    public void setGuestbookResponseService(GuestbookResponseServiceBean guestbookResponseService) {
        this.guestbookResponseService = guestbookResponseService;
    }
    
    
    public GuestbookResponse getGuestbookResponse() {
        return guestbookResponse;
    }

    public void setGuestbookResponse(GuestbookResponse guestbookResponse) {
        this.guestbookResponse = guestbookResponse;
    }
    
    
    public boolean canUpdateDataset() {
        return permissionsWrapper.canUpdateDataset(dvRequestService.getDataverseRequest(), this.file.getOwner());
    }
    
    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }
    
   private Boolean hasValidTermsOfAccess = null;
    
    public Boolean isHasValidTermsOfAccess() {
        //cache in page to limit processing
        if (hasValidTermsOfAccess != null){
            return hasValidTermsOfAccess;
        } else {
            if (!isHasRestrictedFiles()){
               hasValidTermsOfAccess = true;
               return hasValidTermsOfAccess;
            } else {
                hasValidTermsOfAccess = TermsOfUseAndAccessValidator.isTOUAValid(fileMetadata.getDatasetVersion().getTermsOfUseAndAccess(), null);
                return hasValidTermsOfAccess;
            }
        }    
    }
    
    public boolean getHasValidTermsOfAccess(){
        return isHasValidTermsOfAccess(); //HasValidTermsOfAccess
    }
    
    public void setHasValidTermsOfAccess(boolean value){
        //dummy for ui
    }
    
    private Boolean hasRestrictedFiles = null;
    
    public Boolean isHasRestrictedFiles(){
        //cache in page to limit processing
        if (hasRestrictedFiles != null){
            return hasRestrictedFiles;
        } else {
            hasRestrictedFiles = fileMetadata.getDatasetVersion().isHasRestrictedFile();
            return hasRestrictedFiles;
        }
    }
    
    public boolean isSwiftStorage () {
        Boolean swiftBool = false;
        if (file.getStorageIdentifier().startsWith("swift://")){
            swiftBool = true;
        }
        return swiftBool;
    }
    
    public boolean showComputeButton () {
        if (isSwiftStorage() && (settingsService.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl) != null)) {
            return true;
        }
        
        return false;
    }
    
    public SwiftAccessIO getSwiftObject() {
        try {
            StorageIO<DataFile> storageIO = getFile().getStorageIO();
            if (storageIO != null && storageIO instanceof SwiftAccessIO) {
                return (SwiftAccessIO)storageIO;
            } else {
                logger.fine("FilePage: Failed to cast storageIO as SwiftAccessIO");
            } 
        } catch (IOException e) {
            logger.fine("FilePage: Failed to get storageIO");
        }
        return null;
    }


    public String getSwiftContainerName(){
        SwiftAccessIO swiftObject = getSwiftObject();
        try {
            swiftObject.open();
            return swiftObject.getSwiftContainerName();
        } catch (IOException e){
            logger.info("FilePage: Failed to open swift object");
        }
        return "";
    }

    public String getComputeUrl() throws IOException {
        SwiftAccessIO swiftObject = getSwiftObject();
        if (swiftObject != null) {
            swiftObject.open();
            //generate a temp url for a file
            if (isHasPublicStore()) {
                return settingsService.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl) + "?" + this.getFile().getOwner().getGlobalId().asString() + "=" + swiftObject.getSwiftFileName();
            }
            return settingsService.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl) + "?" + this.getFile().getOwner().getGlobalId().asString() + "=" + swiftObject.getSwiftFileName() + "&temp_url_sig=" + swiftObject.getTempUrlSignature() + "&temp_url_expires=" + swiftObject.getTempUrlExpiry();
        }
        return "";
    }

    private List<DataFile> allRelatedFiles() {
        List<DataFile> dataFiles = new ArrayList<>();
        DataFile dataFileToTest = fileMetadata.getDataFile();
        Long rootDataFileId = dataFileToTest.getRootDataFileId();
        if (rootDataFileId < 0) {
            dataFiles.add(dataFileToTest);
        } else {
            dataFiles.addAll(datafileService.findAllRelatedByRootDatafileId(rootDataFileId));
        }

        return dataFiles;
    }
    
    // wrappermethod to see if the file has been deleted (or replaced) in the current version
    public boolean isDeletedFile () {
        if (file.getDeleted() == null) {
            file.setDeleted(datafileService.hasBeenDeleted(file));
        }
        
        return file.getDeleted();
    }
    
    /**
     * To help with replace development 
     * @return 
     */
    public boolean isReplacementFile(){
   
        return this.datafileService.isReplacementFile(this.getFile());
    }

    public boolean isPubliclyDownloadable() {
        return FileUtil.isPubliclyDownloadable(fileMetadata);
    }

    private Boolean lockedFromEditsVar;
    private Boolean lockedFromDownloadVar; 
    
    /**
     * Authors are not allowed to edit but curators are allowed - when Dataset is inReview
     * For all other locks edit should be locked for all editors.
     */
    public boolean isLockedFromEdits() {
        if(null == dataset) {
            dataset = fileMetadata.getDataFile().getOwner();
        }
        
        if(null == lockedFromEditsVar) {
            try {
                permissionService.checkEditDatasetLock(dataset, dvRequestService.getDataverseRequest(), new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromEditsVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromEditsVar = true;
            }
            if (!isHasValidTermsOfAccess()){
                lockedFromEditsVar = true;
            }
        }
        return lockedFromEditsVar;
    }
    
    public boolean isLockedFromDownload(){
        if(null == dataset) {
            dataset = fileMetadata.getDataFile().getOwner();
        }
        if (null == lockedFromDownloadVar) {
            try {
                permissionService.checkDownloadFileLock(dataset, dvRequestService.getDataverseRequest(), new CreateNewDatasetCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromDownloadVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromDownloadVar = true;
            }
        }
        return lockedFromDownloadVar;       
    }

    public String getPublicDownloadUrl() {
        try {
            StorageIO<DataFile> storageIO = getFile().getStorageIO();
            if (storageIO instanceof SwiftAccessIO) {
                String fileDownloadUrl = null;
                try {
                    SwiftAccessIO<DataFile> swiftIO = (SwiftAccessIO<DataFile>) storageIO;
                    swiftIO.open();
                    //if its a public store, lets just give users the permanent URL!
                    if (isHasPublicStore()){
                        fileDownloadUrl = swiftIO.getRemoteUrl();
                    } else {
                        //TODO: if a user has access to this file, they should be given the swift url
                        // perhaps even we could use this as the "private url"
                        fileDownloadUrl = swiftIO.getTemporarySwiftUrl();
                    }
                    logger.info("Swift url: " + fileDownloadUrl);
                    return fileDownloadUrl;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        
        return FileUtil.getPublicDownloadUrl(systemConfig.getDataverseSiteUrl(), persistentId, fileId);
    }

    public List<ExternalTool> getConfigureTools() {
        return configureTools;
    }

    public List<ExternalTool> getExploreTools() {
        return exploreTools;
    }
    
    public List<ExternalTool> getToolsWithPreviews() {
        return toolsWithPreviews;
    }
    
    public List<ExternalTool> getQueryTools() {
        return queryTools;
    }
    
    
    public List<ExternalTool> getAllAvailableTools(){
        List<ExternalTool> externalTools = new ArrayList<>();
        externalTools.addAll(queryTools);
        for (ExternalTool pt : toolsWithPreviews){
            if (!externalTools.contains(pt)){
                externalTools.add(pt);
            }
        }        
        return externalTools;
    }
    
    public String getToolType() {
        return toolType;
    }

    public void setToolType(String toolType) {
        this.toolType = toolType;
    }
    
    private ExternalTool selectedTool;

    public ExternalTool getSelectedTool() {
        return selectedTool;
    }

    public void setSelectedTool(ExternalTool selectedTool) {
        this.selectedTool = selectedTool;
    }
    
    public String preview(ExternalTool externalTool) {
        ApiToken apiToken = null;
        User user = session.getUser();
        if (fileMetadata.getDatasetVersion().isDraft() || fileMetadata.getDatasetVersion().isDeaccessioned() || (fileMetadata.getDataFile().isRestricted()) || (FileUtil.isActivelyEmbargoed(fileMetadata))) {
            apiToken=authService.getValidApiTokenForUser(user);
        }
        if(externalTool == null){
            return "";
        }
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, file, apiToken, getFileMetadata(), session.getLocaleCode());
        String toolUrl = externalToolHandler.getToolUrlForPreviewMode();
        return toolUrl;
    }
    
    //Provenance fragment bean calls this to show error dialogs after popup failure
    //This can probably be replaced by calling JsfHelper from the provpopup bean
    public void showProvError() {
        JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("file.metadataTab.provenance.error"));
    }
    
    private static final Comparator<ExternalTool> CompareExternalToolName = new Comparator<ExternalTool>() {
        @Override
        public int compare(ExternalTool o1, ExternalTool o2) {
            return o1.getDisplayName().toUpperCase().compareTo(o2.getDisplayName().toUpperCase());
        }
    };

    public void showPreview(GuestbookResponse guestbookResponse) {
        boolean response = fileDownloadHelper.writeGuestbookAndShowPreview(guestbookResponse);
        if (response == true) {
            termsMet = true;
        } else {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.guestbookResponse.showPreview.errorMessage"), BundleUtil.getStringFromBundle("dataset.guestbookResponse.showPreview.errorDetail"));
        }
    }
    
    private String termsOfAccess;
    private boolean fileAccessRequest;

    public String getTermsOfAccess() {
        return termsOfAccess;
    }

    public void setTermsOfAccess(String termsOfAccess) {
        this.termsOfAccess = termsOfAccess;
    }

    public boolean isFileAccessRequest() {
        return fileAccessRequest;
    }

    public void setFileAccessRequest(boolean fileAccessRequest) {
        this.fileAccessRequest = fileAccessRequest;
    }
    public boolean isAnonymizedAccess() {
        if(session.getUser() instanceof PrivateUrlUser) {
            return ((PrivateUrlUser)session.getUser()).hasAnonymizedAccess();
        }
        return false;
    }
    
    public boolean isValidEmbargoSelection() {
        if (!fileMetadata.getDataFile().isReleased()) {
            return true;
        }
        return false;
    }
    
    public boolean isExistingEmbargo() {
        if (!fileMetadata.getDataFile().isReleased() && (fileMetadata.getDataFile().getEmbargo() != null)) {
            return true;
        }
        return false;
    }
    
    public boolean isEmbargoForWholeSelection() {
        return isValidEmbargoSelection();
    }
    
    public Embargo getSelectionEmbargo() {
        return selectionEmbargo;
    }

    public void setSelectionEmbargo(Embargo selectionEmbargo) {
        this.selectionEmbargo = selectionEmbargo;
    }

    private Embargo selectionEmbargo = new Embargo();
    
    private boolean removeEmbargo=false;

    public boolean isRemoveEmbargo() {
        return removeEmbargo;
    }

    public void setRemoveEmbargo(boolean removeEmbargo) {
        boolean existing = this.removeEmbargo;
        this.removeEmbargo = removeEmbargo;
        if (existing != this.removeEmbargo) {
            logger.info("State flip");
            selectionEmbargo = new Embargo();
            if (removeEmbargo) {
                selectionEmbargo = new Embargo(null, null);
            }
        }
        PrimeFaces.current().resetInputs("fileForm:embargoInputs");
    }
    
    public String saveEmbargo() {
        
        if(isRemoveEmbargo() || (selectionEmbargo.getDateAvailable()==null && selectionEmbargo.getReason()==null)) {
            selectionEmbargo=null;
        }
        
        Embargo emb = null;
        // Note: this.fileMetadata.getDataFile() is not the same object as this.file.
        // (Not sure there's a good reason for this other than that's the way it is.)
        // So changes to this.fileMetadata.getDataFile() will not be saved with
        // editDataset = this.file.getOwner() set as it is below.
        if (!file.isReleased()) {
            emb = file.getEmbargo();
            if (emb != null) {
                logger.fine("Before: " + emb.getDataFiles().size());
                emb.getDataFiles().remove(fileMetadata.getDataFile());
                logger.fine("After: " + emb.getDataFiles().size());
            }
            if (selectionEmbargo != null) {
                embargoService.merge(selectionEmbargo);
            }
            file.setEmbargo(selectionEmbargo);
            if (emb != null && !emb.getDataFiles().isEmpty()) {
                emb = null;
            }
        }
        if(selectionEmbargo!=null) {
            embargoService.save(selectionEmbargo, ((AuthenticatedUser)session.getUser()).getIdentifier());
        }
        // success message:
        String successMessage = BundleUtil.getStringFromBundle("file.assignedEmbargo.success");
        logger.fine(successMessage);
        successMessage = successMessage.replace("{0}", "Selected Files");
        JsfHelper.addFlashMessage(successMessage);
        selectionEmbargo = new Embargo();

        //Caller has to set editDataset before calling save()
        editDataset = this.file.getOwner();
        
        save();
        init();
        if(emb!=null) {
            embargoService.deleteById(emb.getId(),((AuthenticatedUser)session.getUser()).getIdentifier());
        }
        return returnToDraftVersion();
    }
    
    public void clearEmbargoPopup() {
        setRemoveEmbargo(false);
        selectionEmbargo = new Embargo();
        PrimeFaces.current().resetInputs("fileForm:embargoInputs");
    }
    
    public void clearSelectionEmbargo() {
        selectionEmbargo = new Embargo();
        PrimeFaces.current().resetInputs("fileForm:embargoInputs");
    }
    
    public boolean isCantRequestDueToEmbargo() {
        return FileUtil.isActivelyEmbargoed(fileMetadata);
    }
    
    public String getEmbargoPhrase() {
        //Should only be getting called when there is an embargo
        if(file.isReleased()) {
            if(FileUtil.isActivelyEmbargoed(file)) {
                return BundleUtil.getStringFromBundle("embargoed.until");
            } else {
                return BundleUtil.getStringFromBundle("embargoed.wasthrough");
            }
        } else {
            return BundleUtil.getStringFromBundle("embargoed.willbeuntil");
        }
    }
    
    public String getToolTabTitle(){
        if (getAllAvailableTools().size() > 1) {
            return BundleUtil.getStringFromBundle("file.toolTab.header");
        }
        if( getSelectedTool() != null ){
           if(getSelectedTool().isPreviewTool()){
               return BundleUtil.getStringFromBundle("file.previewTab.header");
           } 
           if(getSelectedTool().isQueryTool()){
               return BundleUtil.getStringFromBundle("file.queryTab.header");
           }          
        } 
        return BundleUtil.getStringFromBundle("file.toolTab.header");
    }
    
    public String getIngestMessage() {
        return BundleUtil.getStringFromBundle("file.ingestFailed.message", Arrays.asList(settingsWrapper.getGuidesBaseUrl(), settingsWrapper.getGuidesVersion()));
    }
    
    //Determines whether this File uses a public store and therefore doesn't support embargoed or restricted files
    public boolean isHasPublicStore() {
        return settingsWrapper.isTrueForKey(SettingsServiceBean.Key.PublicInstall, StorageIO.isPublicStore(DataAccess.getStorageDriverFromIdentifier(file.getStorageIdentifier())));
    }
    
    //Allows use of fileDownloadHelper in file.xhtml
    public FileDownloadHelper getFileDownloadHelper() {
        return fileDownloadHelper;
    }

    public void setFileDownloadHelper(FileDownloadHelper fileDownloadHelper) {
        this.fileDownloadHelper = fileDownloadHelper;
    }

    /**
     * This method only exists because in file-edit-button-fragment.xhtml we
     * call bean.editFileMetadata() and we need both FilePage (this bean) and
     * DatasetPage to have the method defined to prevent errors in server.log.
     */
    public String editFileMetadata(){
        return "";
    }

}
