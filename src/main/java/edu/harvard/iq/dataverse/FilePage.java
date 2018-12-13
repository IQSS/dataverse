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
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.datasetutility.WorldMapPermissionHelper;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RestrictFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolation;
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
    SystemConfig systemConfig;


    @Inject
    DataverseSession session;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    ExternalToolServiceBean externalToolService;

    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @Inject
    FileDownloadHelper fileDownloadHelper;
    @Inject WorldMapPermissionHelper worldMapPermissionHelper;

    public WorldMapPermissionHelper getWorldMapPermissionHelper() {
        return worldMapPermissionHelper;
    }

    public void setWorldMapPermissionHelper(WorldMapPermissionHelper worldMapPermissionHelper) {
        this.worldMapPermissionHelper = worldMapPermissionHelper;
    }

    private static final Logger logger = Logger.getLogger(FilePage.class.getCanonicalName());

    private boolean fileDeleteInProgress = false;
    public String init() {
     
        
         if ( fileId != null || persistentId != null ) { 
           
            // ---------------------------------------
            // Set the file and datasetVersion 
            // ---------------------------------------           
            if (fileId != null) {
               file = datafileService.find(fileId);   

            }  else if (persistentId != null){
               file = datafileService.findByGlobalId(persistentId);
               if (file != null){
                                  fileId = file.getId();
               }

            }
            
            if (file == null || fileId == null){
               return permissionsWrapper.notFound();
            }
            
            // Is the Dataset harvested?
            if (file.getOwner().isHarvested()) {
                // if so, we'll simply forward to the remote URL for the original
                // source of this harvested dataset:
                String originalSourceURL = file.getOwner().getRemoteArchiveURL();
                if (originalSourceURL != null && !originalSourceURL.equals("")) {
                    logger.fine("redirecting to "+originalSourceURL);
                    try {
                        FacesContext.getCurrentInstance().getExternalContext().redirect(originalSourceURL);
                    } catch (IOException ioex) {
                        // must be a bad URL...
                        // we don't need to do anything special here - we'll redirect
                        // to the local 404 page, below.
                        logger.warning("failed to issue a redirect to "+originalSourceURL);
                    }
                }

                return permissionsWrapper.notFound();
            }

                RetrieveDatasetVersionResponse retrieveDatasetVersionResponse;
                retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(file.getOwner().getVersions(), version);
                Long getDatasetVersionID = retrieveDatasetVersionResponse.getDatasetVersion().getId();
                fileMetadata = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(getDatasetVersionID, fileId);

          
            if (fileMetadata == null){
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

            
            Boolean authorized = (fileMetadata.getDatasetVersion().isReleased()) ||
                    (!fileMetadata.getDatasetVersion().isReleased() && this.canViewUnpublishedDataset());
            
            if (!authorized ) {
                return permissionsWrapper.notAuthorized();
            }         
           
           this.guestbookResponse = this.guestbookResponseService.initGuestbookResponseForFragment(fileMetadata, session);
           
          //  this.getFileDownloadHelper().setGuestbookResponse(guestbookResponse);

            if (file.isTabularData()) {
                configureTools = externalToolService.findByType(ExternalTool.Type.CONFIGURE);
                exploreTools = externalToolService.findByType(ExternalTool.Type.EXPLORE);
            }

        } else {

            return permissionsWrapper.notFound();
        }

        return null;
    }
    
    private boolean canViewUnpublishedDataset() {
        return permissionsWrapper.canViewUnpublishedDataset( dvRequestService.getDataverseRequest(), fileMetadata.getDatasetVersion().getDataset());
    }
   

    public FileMetadata getFileMetadata() {
        return fileMetadata;
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
        for (String [] provider : ExportService.getInstance(settingsService).getExportersLabels() ){
            String formatName = provider[1];
            String formatDisplayName = provider[0];
            
            Exporter exporter = null; 
            try {
                exporter = ExportService.getInstance(settingsService).getExporter(formatName);
            } catch (ExportException ex) {
                exporter = null;
            }
            if (exporter != null && exporter.isAvailableToUsers()) {
                // Not all metadata exports should be presented to the web users!
                // Some are only for harvesting clients.
                
                String[] temp = new String[2];            
                temp[0] = formatDisplayName;
                temp[1] = myHostURL + "/api/datasets/export?exporter=" + formatName + "&persistentId=" + fileMetadata.getDatasetVersion().getDataset().getGlobalIdString();
                retList.add(temp);
            }
        }
        return retList;  
    }
    
    public String saveProvFreeform(String freeformTextInput, DataFile dataFileFromPopup) throws CommandException {
        editDataset = this.file.getOwner();
        file.setProvEntityName(dataFileFromPopup.getProvEntityName()); //passing this value into the file being saved here is pretty hacky.
        Command cmd;
        
        for (FileMetadata fmw : editDataset.getEditVersion().getFileMetadatas()) {
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
        String termsOfAccess = this.fileMetadata.getDatasetVersion().getTermsOfUseAndAccess().getTermsOfAccess();        
        Boolean allowRequest = this.fileMetadata.getDatasetVersion().getTermsOfUseAndAccess().isFileAccessRequest();
        editDataset = this.file.getOwner();
        
        Command cmd;
        for (FileMetadata fmw : editDataset.getEditVersion().getFileMetadatas()) {
            if (fmw.getDataFile().equals(this.fileMetadata.getDataFile())) {
                fileNames += fmw.getLabel();
                //fmw.setRestricted(restricted);
                cmd = new RestrictFileCommand(fmw.getDataFile(), dvRequestService.getDataverseRequest(), restricted);
                commandEngine.submit(cmd);
            }
        }
        
        editDataset.getEditVersion().getTermsOfUseAndAccess().setTermsOfAccess(termsOfAccess);
        editDataset.getEditVersion().getTermsOfUseAndAccess().setFileAccessRequest(allowRequest);
        
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
        
        for (FileMetadata fmd : editDataset.getEditVersion().getFileMetadatas() ){
            
            if (fmd.getDataFile().getId().equals(this.getFile().getId())){
                markedForDelete = fmd;
            }
        }

            if (markedForDelete.getId() != null) {
                // the file already exists as part of this dataset
                // so all we remove is the file from the fileMetadatas (for display)
                // and let the delete be handled in the command (by adding it to the filesToBeDeleted list
                editDataset.getEditVersion().getFileMetadatas().remove(markedForDelete);
                filesToBeDeleted.add(markedForDelete);
                
            } else {
                 List<FileMetadata> filesToKeep = new ArrayList<>();
                 for (FileMetadata fmo: editDataset.getEditVersion().getFileMetadatas()){
                      if (!fmo.getDataFile().getId().equals(this.getFile().getId())){
                          filesToKeep.add(fmo);
                      }
                 }
                 editDataset.getEditVersion().setFileMetadatas(filesToKeep);
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
    
    public String save() {
        // Validate
        Set<ConstraintViolation> constraintViolations = this.fileMetadata.getDatasetVersion().validate();
        if (!constraintViolations.isEmpty()) {
             //JsfHelper.addFlashMessage(JH.localize("dataset.message.validationError"));
             fileDeleteInProgress = false;
             JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.message.validationError"));
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "See below for details."));
            return "";
        }
               

        Command<Dataset> cmd;
        try {
            cmd = new UpdateDatasetVersionCommand(editDataset, dvRequestService.getDataverseRequest(), filesToBeDeleted);
            commandEngine.submit(cmd);

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
        
         return "/dataset.xhtml?persistentId=" + editDataset.getGlobalIdString()  + "&version=DRAFT" + "&faces-redirect=true";   
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
            if (settingsService.isTrueForKey(SettingsServiceBean.Key.PublicInstall, false)) {
                return settingsService.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl) + "?" + this.getFile().getOwner().getGlobalIdString() + "=" + swiftObject.getSwiftFileName();
            }
            return settingsService.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl) + "?" + this.getFile().getOwner().getGlobalIdString() + "=" + swiftObject.getSwiftFileName() + "&temp_url_sig=" + swiftObject.getTempUrlSignature() + "&temp_url_expires=" + swiftObject.getTempUrlExpiry();
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
    
    public boolean isDraftReplacementFile(){
        /*
        This method tests to see if the file has been replaced in a draft version of the dataset
        Since it must must work when you are on prior versions of the dataset 
        it must accrue all replacement files that may have been created
        */
        if(null == dataset) {
            dataset = fileMetadata.getDataFile().getOwner();
        }
        
        DataFile dataFileToTest = fileMetadata.getDataFile(); 
        
        DatasetVersion currentVersion = dataset.getLatestVersion();
        
        if (!currentVersion.isDraft()){
            return false;
        }
        
        if (dataset.getReleasedVersion() == null){
            return false;
        }
        
        List<DataFile> dataFiles = new ArrayList<>();
        
        dataFiles.add(dataFileToTest);
        
        while (datafileService.findReplacementFile(dataFileToTest.getId()) != null ){
            dataFiles.add(datafileService.findReplacementFile(dataFileToTest.getId()));
            dataFileToTest = datafileService.findReplacementFile(dataFileToTest.getId());
        }
        
        if(dataFiles.size() <2){
            return false;
        }
        
        int numFiles = dataFiles.size();
        
        DataFile current = dataFiles.get(numFiles - 1 );       
        
        DatasetVersion publishedVersion = dataset.getReleasedVersion();
        
        if( datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(publishedVersion.getId(), current.getId()) == null){
            return true;
        }
        
        return false;
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
                    //if its a public install, lets just give users the permanent URL!
                    if (systemConfig.isPublicInstall()){                        
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
        
        return FileUtil.getPublicDownloadUrl(systemConfig.getDataverseSiteUrl(), persistentId);
    }

    public List<ExternalTool> getConfigureTools() {
        return configureTools;
    }

    public List<ExternalTool> getExploreTools() {
        return exploreTools;
    }
    
    //Provenance fragment bean calls this to show error dialogs after popup failure
    //This can probably be replaced by calling JsfHelper from the provpopup bean
    public void showProvError() {
        JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("file.metadataTab.provenance.error"));
    }

}
