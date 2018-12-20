package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.provenance.ProvPopupFragmentBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.FileReplaceException;
import edu.harvard.iq.dataverse.datasetutility.FileReplacePageHelper;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtil;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RequestRsyncScriptCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.ingest.IngestRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestUtil;
import edu.harvard.iq.dataverse.search.FileView;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.EjbUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonArray;
import javax.json.JsonReader;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.FacesEvent;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.primefaces.context.RequestContext;

/**
 *
 * @author Leonid Andreev
 */
@ViewScoped
@Named("EditDatafilesPage")
public class EditDatafilesPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(EditDatafilesPage.class.getCanonicalName());
    private FileView fileView;
    private boolean uploadWarningMessageIsNotAnError;

    public enum FileEditMode {

        EDIT, UPLOAD, CREATE, SINGLE, SINGLE_REPLACE
    };
    
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    IngestServiceBean ingestService;
    @EJB
    EjbDataverseEngine commandEngine;
    @Inject
    DataverseSession session;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    AuthenticationServiceBean authService;
    @EJB
    SystemConfig systemConfig;
    @EJB
    DataverseLinkingServiceBean dvLinkingService;
    @EJB
    IndexServiceBean indexService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject PermissionsWrapper permissionsWrapper;
    @Inject FileDownloadHelper fileDownloadHelper;
    @Inject ProvPopupFragmentBean provPopupFragmentBean;
    @Inject
    SettingsWrapper settingsWrapper;
    private final DateFormat displayDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    private Dataset dataset = new Dataset();
    
    private FileReplacePageHelper fileReplacePageHelper;


    private String selectedFileIdsString = null; 
    private FileEditMode mode = FileEditMode.EDIT; 
    private List<Long> selectedFileIdsList = new ArrayList<>(); 
    private List<FileMetadata> fileMetadatas = new ArrayList<>();;

    
    private Long ownerId;
    private Long versionId;
    private List<DataFile> newFiles = new ArrayList<>();;
    private List<DataFile> uploadedFiles = new ArrayList<>();; 
    private DatasetVersion workingVersion;
    private String dropBoxSelection = "";
    private String displayCitation;
    private boolean datasetUpdateRequired = false; 
    private boolean tabularDataTagsUpdated = false; 
    
    private String persistentId;
    
    private String versionString = "";
            
    
    private boolean saveEnabled = false; 

    // Used to store results of permissions checks
    private final Map<String, Boolean> datasetPermissionMap = new HashMap<>(); // { Permission human_name : Boolean }

    private Long maxFileUploadSizeInBytes = null;
    private Integer multipleUploadFilesLimit = null; 
    
    private final int NUMBER_OF_SCROLL_ROWS = 25;
    
    private DataFile singleFile = null;

    public DataFile getSingleFile() {
        return singleFile;
    }

    public void setSingleFile(DataFile singleFile) {
        this.singleFile = singleFile;
    }
    
    public String getSelectedFileIds() {
        return selectedFileIdsString;
    }
    
    public DataFile getFileToReplace(){
        if (!this.isFileReplaceOperation()){
            return null;
        }
        if (this.fileReplacePageHelper == null){
            return null;
        }
        return this.fileReplacePageHelper.getFileToReplace();
    }
    
    public void setSelectedFileIds(String selectedFileIds) {
        selectedFileIdsString = selectedFileIds;
    }
    
    public FileEditMode getMode() {
        return mode;
    }
    
    public void setMode(FileEditMode mode) {
        this.mode = mode;
    }
    
    public List<FileMetadata> getFileMetadatas() {
        
        // -------------------------------------
        // Handle a Replace operation
        //  - The List<FileMetadata> comes from a different source
        // -------------------------------------
        if (isFileReplaceOperation()){
            if (fileReplacePageHelper.wasPhase1Successful()){
                logger.fine("Replace: File metadatas 'list' of 1 from the fileReplacePageHelper.");
                return fileReplacePageHelper.getNewFileMetadatasBeforeSave();
            } else {
                logger.fine("Replace: replacement file not yet uploaded.");
                return null;
            }            
        }
        
        if (fileMetadatas != null) {
            logger.fine("Returning a list of "+fileMetadatas.size()+" file metadatas.");
        } else {
            logger.fine("File metadatas list hasn't been initialized yet.");
        }
        // [experimental] 
        // this would be a way to hide any already-uploaded files from the page
        // while a new upload is happening:
        // (the uploadStarted button on the page needs the update="filesTable"
        // attribute added for this to work)
        //if (uploadInProgress) {
        //    return null; 
        //}
        
        return fileMetadatas;
    }
    
    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }
    
    /* 
        The 2 methods below are for setting up the PrimeFaces:dataTabe component
        used to display the uploaded files, or the files selected for editing. 
    
        - isScrollable(): 
          this supplies the value of the component attribute "scrollable". 
          When we have more than NUMBER_OF_SCROLL_ROWS worth of files (currently
          set to 25), we will add a scroller to the table, showing NUMBER_OF_SCROLL_ROWS
          at a time; thus making the page a little bit more useable. 
          When there is fewer rows, however, the attribute needs to be set to 
          "false" - because otherwise some (idiosyncratic) amount of white space 
          is added to the bottom of the table, making the page look silly. 
    
        - getScrollHeightPercentage():
          this method calculates the *percentage* of the total length of the 
          list of files, such that the resulting table is always NUMBER_OF_SCROLL_ROWS 
          high. This is *the only way* to keep the number of files shown in the 
          table fixed as the size of the list grows! (the "scrollRows" attribute
          of the p:dataTable component only applies when "liveScroll=true" is being
          used). 
    */
    
    public boolean isScrollable() {
        return !(fileMetadatas == null || fileMetadatas.size() <= NUMBER_OF_SCROLL_ROWS + 1);
    }
    
    public String getScrollHeightPercentage() {
        int perc; 
        if (fileMetadatas == null || fileMetadatas.size() < NUMBER_OF_SCROLL_ROWS) {
            perc = 100;
        } else {
            perc = NUMBER_OF_SCROLL_ROWS * 100 / fileMetadatas.size();
        }
        
        if (perc == 0) {
            perc = 1;
        } else if (perc > 100) {
            perc = 100;
        }
        
        logger.fine("scroll height percentage: "+perc);
        return perc + "%";
    }
    
    /*
        Any settings, such as the upload size limits, should be saved locally - 
        so that the db doesn't get hit repeatedly. (this setting is initialized 
        in the init() method)
    
        This may be "null", signifying unlimited download size.
    */
    
    public Long getMaxFileUploadSizeInBytes() {
        return this.maxFileUploadSizeInBytes;
    }
    
    public boolean isUnlimitedUploadFileSize() {
        
        return this.maxFileUploadSizeInBytes == null;
    }
    
    /*
        The number of files the GUI user is allowed to upload in one batch, 
        via drag-and-drop, or through the file select dialog. Now configurable 
        in the Settings table. 
    */
    public Integer getMaxNumberOfFiles() {
        return this.multipleUploadFilesLimit;
    }
    /**
     * Check Dataset related permissions
     * 
     * @param permissionToCheck
     * @return 
     */
    public boolean doesSessionUserHaveDataSetPermission(Permission permissionToCheck){
        if (permissionToCheck == null){
            return false;
        }
               
        String permName = permissionToCheck.getHumanName();
       
        // Has this check already been done? 
        // 
        if (this.datasetPermissionMap.containsKey(permName)){
            // Yes, return previous answer
            return this.datasetPermissionMap.get(permName);
        }
        
        // Check the permission
        //
        boolean hasPermission = this.permissionService.userOn(this.session.getUser(), this.dataset).has(permissionToCheck);

        // Save the permission
        this.datasetPermissionMap.put(permName, hasPermission);
        
        // return true/false
        return hasPermission;
    }
    
    public void reset() {
        // ?
    }

    public String getGlobalId() {
        return persistentId;
    }
        
    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public String getDisplayCitation() {
        //displayCitation = dataset.getCitation(false, workingVersion);
        return displayCitation;
    }

    public void setDisplayCitation(String displayCitation) {
        this.displayCitation = displayCitation;
    }

    public String getDropBoxSelection() {
        return dropBoxSelection;
    }

    public String getDropBoxKey() {
        // Site-specific DropBox application registration key is configured 
        // via a JVM option under glassfish.
        //if (true)return "some-test-key";  // for debugging

        String configuredDropBoxKey = System.getProperty("dataverse.dropbox.key");
        if (configuredDropBoxKey != null) {
            return configuredDropBoxKey;
        }
        return "";
    }

    public void setDropBoxSelection(String dropBoxSelection) {
        this.dropBoxSelection = dropBoxSelection;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public DatasetVersion getWorkingVersion() {
        return workingVersion;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public String initCreateMode(String modeToken, DatasetVersion version, List<DataFile> newFilesList, List<FileMetadata> selectedFileMetadatasList) {
        if (modeToken == null) {
            logger.fine("Request to initialize Edit Files page with null token (aborting).");
            return null;
        }
        
        if (!modeToken.equals("CREATE")) {
            logger.fine("Request to initialize Edit Files page with token " + modeToken + " (aborting).");
            return null; 
        }
        
        logger.fine("Initializing Edit Files page in CREATE mode;");
        
        if (version == null) {
            return permissionsWrapper.notFound();
        }
        
        this.maxFileUploadSizeInBytes = systemConfig.getMaxFileUploadSize();
        this.multipleUploadFilesLimit = systemConfig.getMultipleUploadFilesLimit();
        
        workingVersion = version; 
        dataset = version.getDataset();
        mode = FileEditMode.CREATE;
        newFiles = newFilesList;
        uploadedFiles = new ArrayList<>();
        selectedFiles = selectedFileMetadatasList;
        
        logger.fine("done");
        
        saveEnabled = true;
        
        return null; 
    }
    
    
    public String init() {
        fileMetadatas = new ArrayList<>();
        
        newFiles = new ArrayList<>();
        uploadedFiles = new ArrayList<>(); 
        
        this.maxFileUploadSizeInBytes = systemConfig.getMaxFileUploadSize();
        this.multipleUploadFilesLimit = systemConfig.getMultipleUploadFilesLimit();
        
        if (dataset.getId() != null){
            // Set Working Version and Dataset by Datasaet Id and Version
            //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionById(dataset.getId(), null);
            dataset = datasetService.find(dataset.getId());
            // Is the Dataset harvested? (because we don't allow editing of harvested 
            // files!)
            if (dataset == null || dataset.isHarvested()) {
                return permissionsWrapper.notFound();
            }
        } else {
            // It could be better to show an error page of some sort, explaining
            // that the dataset id is mandatory... But 404 will do for now.
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

        // TODO: Think about why this call to populateFileMetadatas was added. It seems like it isn't needed after all.
//        populateFileMetadatas();

        // -------------------------------------------
        //  Is this a file replacement operation?
        // -------------------------------------------
        if (mode == FileEditMode.SINGLE_REPLACE){
            /*
            http://localhost:8080/editdatafiles.xhtml?mode=SINGLE_REPLACE&datasetId=26&fid=726
            */        
            DataFile fileToReplace = loadFileToReplace();
            if (fileToReplace == null){
                return permissionsWrapper.notFound();
            }
            
            //DataverseRequest dvRequest2 = createDataverseRequest(authUser);
            AddReplaceFileHelper addReplaceFileHelper = new AddReplaceFileHelper(dvRequestService.getDataverseRequest(),
                                                ingestService,
                                                datasetService,
                                                datafileService,
                                                permissionService,
                                                commandEngine,
                                                systemConfig);
                        
            fileReplacePageHelper = new FileReplacePageHelper(addReplaceFileHelper,
                                                dataset, 
                                                fileToReplace);

            populateFileMetadatas();
            singleFile = getFileToReplace();
        }else if (mode == FileEditMode.EDIT || mode == FileEditMode.SINGLE) {

            if (selectedFileIdsString != null) {
                String[] ids = selectedFileIdsString.split(",");

                for (String id : ids) {
                    Long test = null;
                    try {
                        test = new Long(id);
                    } catch (NumberFormatException nfe) {
                        // do nothing...
                        test = null;
                    }
                    if (test != null) {
                        if (mode == FileEditMode.SINGLE) {
                            singleFile = datafileService.find(test);
                        }
                        selectedFileIdsList.add(test);
                    }
                }
            }

            if (selectedFileIdsList.size() < 1) {
                logger.fine("No numeric file ids supplied to the page, in the edit mode. Redirecting to the 404 page.");
                // If no valid file IDs specified, send them to the 404 page...
                return permissionsWrapper.notFound();
            }

            logger.fine("The page is called with " + selectedFileIdsList.size() + " file ids.");

            populateFileMetadatas();
            setUpRsync();
            // and if no filemetadatas can be found for the specified file ids 
            // and version id - same deal, send them to the "not found" page. 
            // (at least for now; ideally, we probably want to show them a page 
            // with a more informative error message; something alonog the lines 
            // of - could not find the files for the ids specified; or, these 
            // datafiles are not present in the version specified, etc.
            if (fileMetadatas.size() < 1) {
                return permissionsWrapper.notFound();
            }
            
            if (FileEditMode.SINGLE == mode){
                if (fileMetadatas.get(0).getDatasetVersion().getId() != null){
                    versionString = "DRAFT";
                }
            }           
                       
        }
        
        saveEnabled = true; 
        if (mode == FileEditMode.UPLOAD && workingVersion.getFileMetadatas().isEmpty() && settingsWrapper.isRsyncUpload())  {
            setUpRsync();
        }

        if (mode == FileEditMode.UPLOAD) {
            if (settingsWrapper.getUploadMethodsCount() == 1){
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.message.uploadFiles.label"), BundleUtil.getStringFromBundle("dataset.message.uploadFilesSingle.message", Arrays.asList(systemConfig.getGuidesBaseUrl(), systemConfig.getGuidesVersion())));
            } else if (settingsWrapper.getUploadMethodsCount() > 1) {
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.message.uploadFiles.label"), BundleUtil.getStringFromBundle("dataset.message.uploadFilesMultiple.message", Arrays.asList(systemConfig.getGuidesBaseUrl(), systemConfig.getGuidesVersion())));
            }
            
        }
        
        if (settingsService.isTrueForKey(SettingsServiceBean.Key.PublicInstall, false)){
            JH.addMessage(FacesMessage.SEVERITY_WARN, getBundleString("dataset.message.publicInstall"));
        }   
        
        return null;
    }
    
    
    private void msg(String s){
        System.out.println(s);
    }
    
    /**
     * For single file replacement, load the file to replace
     * 
     * @return 
     */
    private DataFile loadFileToReplace(){
        
        Map<String, String> params =FacesContext.getCurrentInstance().
                                getExternalContext().getRequestParameterMap();
        
        if (params.containsKey("fid")){
            String fid = params.get("fid");
            if ((!fid.isEmpty()) && (StringUtils.isNumeric(fid))){
                selectedFileIdsList.add(Long.parseLong(fid));
                return datafileService.find(Long.parseLong(fid));
            }
        }
        return null;
        
    } // loadFileToReplace
    
    private List<FileMetadata> selectedFiles; // = new ArrayList<>();

    public List<FileMetadata> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<FileMetadata> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }
    
    private boolean selectAllFiles;

    public boolean isSelectAllFiles() {
        return selectAllFiles;
    }

    public void setSelectAllFiles(boolean selectAllFiles) {
        this.selectAllFiles = selectAllFiles;
    }
    
    public String getVersionString() {
        return versionString;
    }

    public void setVersionString(String versionString) {
        this.versionString = versionString;
    }
    
    public void toggleSelectedFiles(){
        this.selectedFiles = new ArrayList<>();
        if(this.selectAllFiles){
            if (mode == FileEditMode.CREATE) {
                for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                    this.selectedFiles.add(fmd);
                }
            } else {
                for (FileMetadata fmd : fileMetadatas) {
                    this.selectedFiles.add(fmd);
                }
            }
        }
    }
    
    public String getSelectedFilesIdsString() {        
        String downloadIdString = "";
        for (FileMetadata fmd : this.selectedFiles){
            if (!StringUtil.isEmpty(downloadIdString)) {
                downloadIdString += ",";
            }
            downloadIdString += fmd.getDataFile().getId();
        }
        return downloadIdString;
      
    }

    /*
    public void updateFileCounts(){
        
        setSelectedUnrestrictedFiles(new ArrayList<FileMetadata>());
        setSelectedRestrictedFiles(new ArrayList<FileMetadata>());
        for (FileMetadata fmd : this.selectedFiles){
            if(fmd.isRestricted()){
                getSelectedRestrictedFiles().add(fmd);
            } else {
                getSelectedUnrestrictedFiles().add(fmd);
            }
        }
    }*/
    
    List<FileMetadata> previouslyRestrictedFiles = null;
    
    public boolean isShowAccessPopup() {
        for (FileMetadata fmd : this.fileMetadatas) {
            
            if (fmd.isRestricted()) {
            
                if (fmd.getDataFile().getId() == null) {
                    // if this is a brand new file, it's definitely not 
                    // of a previously restricted kind!
                    return true; 
                }
            
                if (previouslyRestrictedFiles != null) {
                    boolean contains = false;
                    for (FileMetadata fmp : previouslyRestrictedFiles) {
                        // OK, we've already checked if it's a brand new file - 
                        // above. So we can safely assume that this datafile
                        // has a valid db id... so it is safe to use the 
                        // equals() method:
                        if (fmp.getDataFile().equals(fmd.getDataFile())) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public void setShowAccessPopup(boolean showAccessPopup) {} // dummy set method
     
    //This function was reverted to its pre-commands state as the current command
    //requires editDataset privlidges. If a non-admin user with only createDataset privlidges
    //attempts to restrict a datafile before the dataset is created, the operation
    //fails silently. This is because they are only granted editDataset permissions
    //for that scope after the creation is completed.  -Matthew 4.7.1
    public void restrictFiles(boolean restricted) throws UnsupportedOperationException{

        // since we are restricted files, first set the previously restricted file list, so we can compare for
        // determining whether to show the access popup
        previouslyRestrictedFiles = new ArrayList<>();
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            if (fmd.isRestricted()) {
                previouslyRestrictedFiles.add(fmd);
            }
        }
        
        String fileNames = null;
        
        for (FileMetadata fmd : this.getSelectedFiles()) {
            if (restricted && !fmd.isRestricted()) {
                // collect the names of the newly-restrticted files, 
                // to show in the success message:
                if (fileNames == null) {
                    fileNames = fmd.getLabel();
                } else {
                    fileNames = fileNames.concat(", " + fmd.getLabel());
                }
            }
            fmd.setRestricted(restricted);
            
//            Command cmd;
//            cmd = new RestrictFileCommand(fmd.getDataFile(), dvRequestService.getDataverseRequest(), restricted);
//            commandEngine.submit(cmd);
            
                                  
            if (workingVersion.isDraft() && !fmd.getDataFile().isReleased()) {
                // We do not really need to check that the working version is 
                // a draft here - it must be a draft, if we've gotten this
                // far. But just in case. -- L.A. 4.2.1
                  fmd.getDataFile().setRestricted(restricted);              
            }
        }
        if (fileNames != null) {
            String successMessage = getBundleString("file.restricted.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);    
        }
    } 

    public void restrictFilesDP(boolean restricted) {
        // since we are restricted files, first set the previously restricted file list, so we can compare for
        // determinin whether to show the access popup
        if (previouslyRestrictedFiles == null) {
            previouslyRestrictedFiles = new ArrayList<>();
            for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                if (fmd.isRestricted()) {
                    previouslyRestrictedFiles.add(fmd);
                }
            }
        }        
        
        String fileNames = null;       
        for (FileMetadata fmw : workingVersion.getFileMetadatas()) {
            for (FileMetadata fmd : this.getSelectedFiles()) {
                if (restricted && !fmw.isRestricted()) {
                // collect the names of the newly-restrticted files, 
                    // to show in the success message:
                    if (fileNames == null) {
                        fileNames = fmd.getLabel();
                    } else {
                        fileNames = fileNames.concat(", " + fmd.getLabel());
                    }
                }
                if (fmd.getDataFile().equals(fmw.getDataFile())) {
                    fmw.setRestricted(restricted);
                }
            }
        }
        if (fileNames != null) {
            String successMessage = getBundleString("file.restricted.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);    
        }
    } 
    
    public int getRestrictedFileCount() {
        int restrictedFileCount = 0;
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            if (fmd.isRestricted()) {
                restrictedFileCount++;
            }
        }

        return restrictedFileCount;
    }

    private List<FileMetadata> filesToBeDeleted = new ArrayList<>();

    
    public void deleteReplacementFile() throws FileReplaceException{
        if (!isFileReplaceOperation()){
            throw new FileReplaceException("Only use this for File Replace Operations");            
        }

        if (!fileReplacePageHelper.wasPhase1Successful()){
            throw new FileReplaceException("Should only be called if Phase 1 was successful");                        
        }
        
        fileReplacePageHelper.resetReplaceFileHelper();


        
        String successMessage = getBundleString("file.deleted.replacement.success");
        logger.fine(successMessage);
        JsfHelper.addFlashMessage(successMessage);
        
    }
    
    
    /**
     * 
     * @param msgName - from the bundle e.g. "file.deleted.success"
     * @return 
     */
    private String getBundleString(String msgName){
        
       return BundleUtil.getStringFromBundle(msgName);
    }
    
    // This deleteFilesCompleted method is used in editFilesFragment.xhtml
    public void deleteFilesCompleted(){
        
    }
        
    public void deleteFiles() {
        logger.fine("entering bulk file delete (EditDataFilesPage)");
        if (isFileReplaceOperation()){
            try {
                deleteReplacementFile();
            } catch (FileReplaceException ex) {
                Logger.getLogger(EditDatafilesPage.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }
        
        String fileNames = null;
        for (FileMetadata fmd : this.getSelectedFiles()) {
                // collect the names of the files, 
            // to show in the success message:
            if (fileNames == null) {
                fileNames = fmd.getLabel();
            } else {
                fileNames = fileNames.concat(", " + fmd.getLabel());
            }
        }

        for (FileMetadata markedForDelete : this.getSelectedFiles()) {
            logger.fine("delete requested on file "+markedForDelete.getLabel());
            logger.fine("file metadata id: "+markedForDelete.getId());
            logger.fine("datafile id: "+markedForDelete.getDataFile().getId());
            logger.fine("page is in edit mode "+mode.name());
            
                // has this filemetadata been saved already? (or is it a brand new
                // filemetadata, created as part of a brand new version, created when 
                // the user clicked 'delete', that hasn't been saved in the db yet?)
                if (markedForDelete.getId() != null) {
                    logger.fine("this is a filemetadata from an existing draft version");
                // so all we remove is the file from the fileMetadatas (from the
                // file metadatas attached to the editVersion, and from the
                // display list of file metadatas that are being edited)
                // and let the delete be handled in the command (by adding it to the
                // filesToBeDeleted list):

                    dataset.getEditVersion().getFileMetadatas().remove(markedForDelete);
                    fileMetadatas.remove(markedForDelete);
                    filesToBeDeleted.add(markedForDelete);
                } else {
                    logger.fine("this is a brand-new (unsaved) filemetadata");
                    // ok, this is a brand-new DRAFT version. 

                // if (mode != FileEditMode.CREATE) {
                // If the bean is in the 'CREATE' mode, the page is using
                // dataset.getEditVersion().getFileMetadatas() directly,
                // so there's no need to delete this meta from the local
                // fileMetadatas list. (but doing both just adds a no-op and won't cause an
                // error)

                    // 1. delete the filemetadata from the local display list: 
                removeFileMetadataFromList(fileMetadatas, markedForDelete);
                    // 2. delete the filemetadata from the version: 
                removeFileMetadataFromList(dataset.getEditVersion().getFileMetadatas(), markedForDelete);
                        }


            if (markedForDelete.getDataFile().getId() == null) {
                logger.fine("this is a brand new file.");
                // the file was just added during this step, so in addition to 
                // removing it from the fileMetadatas lists (above), we also remove it from
                // the newFiles list and the dataset's files, so it never gets saved.
                
                removeDataFileFromList(dataset.getFiles(), markedForDelete.getDataFile());
                removeDataFileFromList(newFiles, markedForDelete.getDataFile());
                deleteTempFile(markedForDelete.getDataFile());
                // Also remove checksum from the list of newly uploaded checksums (perhaps odd
                // to delete and then try uploading the same file again, but it seems like it
                // should be allowed/the checksum list is part of the state to clean-up
                checksumMapNew.remove(markedForDelete.getDataFile().getChecksumValue());
                    
                        }
                    }
        if (fileNames != null) {
            String successMessage = getBundleString("file.deleted.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);
                    }
                }
                
    private void deleteTempFile(DataFile dataFile) {
                        // Before we remove the file from the list and forget about 
                        // it:
                        // The physical uploaded file is still sitting in the temporary
                        // directory. If it were saved, it would be moved into its 
                        // permanent location. But since the user chose not to save it,
                        // we have to delete the temp file too. 
                        // 
                        // Eventually, we will likely add a dedicated mechanism
                        // for managing temp files, similar to (or part of) the storage 
                        // access framework, that would allow us to handle specialized
                        // configurations - highly sensitive/private data, that 
                        // has to be kept encrypted even in temp files, and such. 
                        // But for now, we just delete the file directly on the 
                        // local filesystem: 

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
                        } catch (IOException ioEx) {
                            // safe to ignore - it's just a temp file. 
            logger.warning("Failed to delete temporary file " + FileUtil.getFilesTempDirectory() + "/"
                    + dataFile.getStorageIdentifier());
        }
                        }
                        
    private void removeFileMetadataFromList(List<FileMetadata> fmds, FileMetadata fmToDelete) {
        Iterator<FileMetadata> fmit = fmds.iterator();
        while (fmit.hasNext()) {
            FileMetadata fmd = fmit.next();
            if (fmToDelete.getDataFile().getStorageIdentifier().equals(fmd.getDataFile().getStorageIdentifier())) {
                fmit.remove();
                break;
                    }
                }
                }                
                
    private void removeDataFileFromList(List<DataFile> dfs, DataFile dfToDelete) {
        Iterator<DataFile> dfit = dfs.iterator();
        while (dfit.hasNext()) {
            DataFile df = dfit.next();
            if (dfToDelete.getStorageIdentifier().equals(df.getStorageIdentifier())) {
                dfit.remove();
                break;
            }
        }
    }

    public String saveWithTermsOfUse() {
        logger.fine("saving terms of use, and the dataset version");
        //Set update required only if dataset already exists
        if (dataset.getId() != null){
             datasetUpdateRequired = true; 
        }
        return save();
    }
    
    
    /**
     * Save for File Replace operations
     * @return
     * @throws FileReplaceException 
     */
    public String saveReplacementFile() throws FileReplaceException{
        
        // Ahh, make sure it's a file replace operation
        //
        if (!isFileReplaceOperation()){
            throw new FileReplaceException("Only use this for File Replace Operations");
        }

        // Can we do a save?  
        //  (redundant but ok, also called in main "save" event before forking here)      
        //
        if (!saveEnabled) {
            return "";
        }
        // Sanity check 1
        //
        if (fileReplacePageHelper == null){
            throw new NullPointerException("fileReplacePageHelper cannot be null");
        }
        
        // Make sure phase 1 ran -- button shouldn't be visible if it did not
        //
        if (!fileReplacePageHelper.wasPhase1Successful()){
            throw new FileReplaceException("Save should only be called when a replacement file has been chosen.  (Phase 1 has to have completed)");
            
        }

        // Run save!!
        //
        if (fileReplacePageHelper.runSaveReplacementFile_Phase2()){
            JsfHelper.addSuccessMessage(getBundleString("file.message.replaceSuccess"));
            // It worked!!!  Go to page of new file!!
            return returnToFileLandingPageAfterReplace(fileReplacePageHelper.getFirstNewlyAddedFile());
        }else{
            // Uh oh.
            String errMsg = fileReplacePageHelper.getErrorMessages();
            
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, getBundleString("dataset.save.fail"), errMsg));
            logger.severe("Dataset save failed for replace operation: " + errMsg);
            return null;
        }
        
    }
    
    public String save() {
        
        
        /*
        // Validate
        Set<ConstraintViolation> constraintViolations = workingVersion.validate();
        if (!constraintViolations.isEmpty()) {
             //JsfHelper.addFlashMessage(getBundleString("dataset.message.validationError"));
            logger.fine("Constraint violation detected on SAVE: "+constraintViolations.toString());
             JH.addMessage(FacesMessage.SEVERITY_ERROR, getBundleString("dataset.message.validationError"));
             
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "See below for details."));
            return "";
        }
        }*/
        
        // Once all the filemetadatas pass the validation, we'll only allow the user 
        // to try to save once; (this it to prevent them from creating multiple
        // DRAFT versions, if the page gets stuck in that state where it 
        // successfully creates a new version, but can't complete the remaining
        // tasks. -- L.A. 4.2
        
        if (!saveEnabled) {
            return "";
        }
        if (isFileReplaceOperation()){
            try {
                return saveReplacementFile();
            } catch (FileReplaceException ex) {
                String errMsg = ex.getMessage();
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, getBundleString("dataset.save.fail"), errMsg));
                logger.log(Level.SEVERE, "Dataset save failed for replace operation: {0}", errMsg);
                return null;
            }            
        }

                
        int nOldFiles = workingVersion.getFileMetadatas().size();
        int nNewFiles = newFiles.size();
        int nExpectedFilesTotal = nOldFiles + nNewFiles; 
        
        if (nNewFiles > 0) {
            //SEK 10/15/2018 only apply the following tests if dataset has already been saved.
            if (dataset.getId() != null) {
                Dataset lockTest = datasetService.find(dataset.getId());
                //SEK 09/19/18 Get Dataset again to test for lock just in case the user downloads the rsync script via the api while the 
                // edit files page is open and has already loaded a file in http upload for Dual Mode
                if (dataset.isLockedFor(DatasetLock.Reason.DcmUpload) || lockTest.isLockedFor(DatasetLock.Reason.DcmUpload)) {
                    logger.log(Level.INFO, "Couldn''t save dataset: {0}", "DCM script has been downloaded for this dataset. Additonal files are not permitted."
                            + "");
                    populateDatasetUpdateFailureMessage();
                    return null;
                }
                for (DatasetVersion dv : lockTest.getVersions()) {
                    if (dv.isHasPackageFile()) {
                        logger.log(Level.INFO, ResourceBundle.getBundle("Bundle").getString("file.api.alreadyHasPackageFile")
                                + "");
                        populateDatasetUpdateFailureMessage();
                        return null;
                    }
                }
            }
                                
            // Try to save the NEW files permanently: 
            List<DataFile> filesAdded = ingestService.saveAndAddFilesToDataset(workingVersion, newFiles);
            
            // reset the working list of fileMetadatas, as to only include the ones
            // that have been added to the version successfully: 
            fileMetadatas.clear();
            for (DataFile addedFile : filesAdded) {
                fileMetadatas.add(addedFile.getFileMetadata());
            }
            filesAdded = null; 
        }
        //boolean newDraftVersion = false;    

        Boolean provJsonChanges = false;
        
        if(systemConfig.isProvCollectionEnabled()) {
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
                JsfHelper.addErrorMessage(getBundleString("file.metadataTab.provenance.error"));
                Logger.getLogger(EditDatafilesPage.class.getName()).log(Level.SEVERE, null, ex);
            }
            //Always update the whole dataset if updating prov
            //The flow that happens when datasetUpdateRequired is false has problems with doing saving actions after its merge
            //This was the simplest way to work around this issue for prov. --MAD 4.8.6.
            datasetUpdateRequired = datasetUpdateRequired || provFreeChanges || provJsonChanges;
        }
                
        if (workingVersion.getId() == null  || datasetUpdateRequired) {
            logger.fine("issuing the dataset update command");
            // We are creating a new draft version; 
            // (OR, a full update of the dataset has been explicitly requested, 
            // because of the nature of the updates the user has made).
            // We'll use an Update command for this: 
            
            //newDraftVersion = true;
            
            if (datasetUpdateRequired) {
                for (int i = 0; i < workingVersion.getFileMetadatas().size(); i++) {
                    for (FileMetadata fileMetadata : fileMetadatas) {
                        if (fileMetadata.getDataFile().getStorageIdentifier() != null) {
                            if (fileMetadata.getDataFile().getStorageIdentifier().equals(workingVersion.getFileMetadatas().get(i).getDataFile().getStorageIdentifier())) {
                                workingVersion.getFileMetadatas().set(i, fileMetadata);
                            }
                        }
                    }
                }
                
                
                //Moves DataFile updates from popupFragment to page for saving
                //This does not seem to collide with the tags updating below
                if(systemConfig.isProvCollectionEnabled() && provJsonChanges) {
                    HashMap<String,ProvPopupFragmentBean.UpdatesEntry> provenanceUpdates = provPopupFragmentBean.getProvenanceUpdates();
                    for (int i = 0; i < dataset.getFiles().size(); i++) {
                        for (ProvPopupFragmentBean.UpdatesEntry ue : provenanceUpdates.values()) { 
                            if (ue.dataFile.getStorageIdentifier() != null ) {
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
                        for (FileMetadata fileMetadata : fileMetadatas) {
                            if (fileMetadata.getDataFile().getStorageIdentifier() != null) {
                                if (fileMetadata.getDataFile().getStorageIdentifier().equals(dataset.getFiles().get(i).getStorageIdentifier())) {
                                    dataset.getFiles().set(i, fileMetadata.getDataFile());
                                }
                            }
                        }
                    }
                    tabularDataTagsUpdated = false;
                }
            }
                        
            Command<Dataset> cmd;
            try {
                cmd = new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest(), filesToBeDeleted);
                ((UpdateDatasetVersionCommand) cmd).setValidateLenient(true);
                dataset = commandEngine.submit(cmd);
            
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
                logger.log(Level.INFO, "Couldn''t save dataset: {0}", error.toString());
                populateDatasetUpdateFailureMessage();
                return null;
            } catch (CommandException ex) {
                //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + ex.toString()));
                logger.log(Level.INFO, "Couldn''t save dataset: {0}", ex.getMessage());
                populateDatasetUpdateFailureMessage();
                return null;
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
        
            for (FileMetadata fileMetadata : fileMetadatas) {

                if (fileMetadata.getDataFile().getCreateDate() == null) {
                    fileMetadata.getDataFile().setCreateDate(updateTime);
                    fileMetadata.getDataFile().setCreator((AuthenticatedUser) session.getUser());
                }
                fileMetadata.getDataFile().setModificationTime(updateTime);
                try {
                    //DataFile savedDatafile = datafileService.save(fileMetadata.getDataFile());
                    fileMetadata = datafileService.mergeFileMetadata(fileMetadata);
                    logger.fine("Successfully saved DataFile "+fileMetadata.getLabel()+" in the database.");
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
            }

            // Remove / delete any files that were removed
            for (FileMetadata fmd : filesToBeDeleted) {
                //  check if this file is being used as the default thumbnail
                if (fmd.getDataFile().equals(dataset.getThumbnailFile())) {
                    logger.fine("deleting the dataset thumbnail designation");
                    dataset.setThumbnailFile(null);
                }

                if (!fmd.getDataFile().isReleased()) {
                    // if file is draft (ie. new to this version, delete; otherwise just remove filemetadata object)
                    try {
                        commandEngine.submit(new DeleteDataFileCommand(fmd.getDataFile(), dvRequestService.getDataverseRequest()));
                        dataset.getFiles().remove(fmd.getDataFile());
                        workingVersion.getFileMetadatas().remove(fmd);
                        // added this check to handle issue where you could not deleter a file that shared a category with a new file
                        // the relationship does not seem to cascade, yet somehow it was trying to merge the filemetadata
                        // todo: clean this up some when we clean the create / update dataset methods
                        for (DataFileCategory cat : dataset.getCategories()) {
                            cat.getFileMetadatas().remove(fmd);
                        }
                    } catch (CommandException cmde) {
                        // TODO: 
                        // add diagnostics reporting for individual data files that 
                        // we failed to delete.
                    }
                } else {
                    datafileService.removeFileMetadata(fmd);
                    fmd.getDataFile().getFileMetadatas().remove(fmd);
                    workingVersion.getFileMetadatas().remove(fmd);
                }
            }
            
            String saveErrorString = saveError.toString();
            if (saveErrorString != null && !saveErrorString.isEmpty()) {
                logger.log(Level.INFO, "Couldn''t save dataset: {0}", saveErrorString);
                populateDatasetUpdateFailureMessage();
                return null;
            }

            
            // Refresh the instance of the dataset object:
            // (being in the UPLOAD mode more or less guarantees that the 
            // dataset object already exists in the database, but we'll check 
            // the id for null, just in case)
            if (mode == FileEditMode.UPLOAD) {
                if (dataset.getId() != null) {
                    dataset = datasetService.find(dataset.getId());
                }
            }
        }
        

        if (newFiles.size() > 0) {
            logger.fine("clearing newfiles list.");
            newFiles.clear();
            /*
             - We decided not to bother obtaining persistent ids for new files 
             as they are uploaded and created. The identifiers will be assigned 
             later, when the version is published. 
             
            logger.info("starting async job for obtaining persistent ids for files.");
            datasetService.obtainPersistentIdentifiersForDatafiles(dataset);
            */
        }
                
        workingVersion = dataset.getEditVersion();
        logger.fine("working version id: "+workingVersion.getId());
       
        if (mode == FileEditMode.SINGLE){
            JsfHelper.addSuccessMessage(getBundleString("file.message.editSuccess"));
            
        } else {
            int nFilesTotal = workingVersion.getFileMetadatas().size();
            if (nNewFiles == 0 || nFilesTotal == nExpectedFilesTotal) {
                JsfHelper.addSuccessMessage(getBundleString("dataset.message.filesSuccess"));
            } else if (nFilesTotal == nOldFiles) {
                JsfHelper.addErrorMessage(getBundleString("dataset.message.addFiles.Failure"));
            } else {
                String warningMessage = getBundleString("dataset.message.addFiles.partialSuccess");
                warningMessage = warningMessage.replace("{0}", "" + (nFilesTotal - nOldFiles));
                warningMessage = warningMessage.replace("{1}", "" + nNewFiles);
                JsfHelper.addWarningMessage(warningMessage);
            }
        }
        

        // Call Ingest Service one more time, to 
        // queue the data ingest jobs for asynchronous execution:
        if (mode == FileEditMode.UPLOAD) {
            ingestService.startIngestJobsForDataset(dataset, (AuthenticatedUser) session.getUser());
        }

        if (mode == FileEditMode.SINGLE && fileMetadatas.size() > 0) {
            // If this was a "single file edit", i.e. an edit request sent from 
            // the individual File Landing page, we want to redirect back to 
            // the landing page. BUT ONLY if the file still exists - i.e., if 
            // the user hasn't just deleted it!
            versionString = "DRAFT";
            return returnToFileLandingPage();
        }
        
        //if (newDraftVersion) {
        //    return returnToDraftVersionById();
        //}
        indexService.indexDataset(dataset, true);
        logger.fine("Redirecting to the dataset page, from the edit/upload page.");
        return returnToDraftVersion();
    }
    
    private void populateDatasetUpdateFailureMessage(){
            
        JH.addMessage(FacesMessage.SEVERITY_FATAL, getBundleString("dataset.message.filesFailure"));
    }
    
    
    
    private String returnToDraftVersion(){      
         return "/dataset.xhtml?persistentId=" + dataset.getGlobalId().asString() + "&version=DRAFT&faces-redirect=true";    
    }
    
    private String returnToDatasetOnly(){
         dataset = datasetService.find(dataset.getId());
         return "/dataset.xhtml?persistentId=" + dataset.getGlobalId().asString()  +  "&faces-redirect=true";       
    }
    
    private String returnToFileLandingPage() {
        Long fileId = fileMetadatas.get(0).getDataFile().getId();   
        if (versionString != null && versionString.equals("DRAFT")){
            return  "/file.xhtml?fileId=" + fileId  +  "&version=DRAFT&faces-redirect=true";
        }
        return  "/file.xhtml?fileId=" + fileId  +  "&faces-redirect=true";

    }

    private String returnToFileLandingPageAfterReplace(DataFile newFile) {
        
        if (newFile == null){
            throw new NullPointerException("newFile cannot be null!");
        }
        //Long datasetVersionId = newFile.getOwner().getLatestVersion().getId();
        return "/file.xhtml?fileId=" + newFile.getId()  + "&version=DRAFT&faces-redirect=true";
    }

    
    public String cancel() {
        uploadInProgress = false;
        if (mode == FileEditMode.SINGLE || mode == FileEditMode.SINGLE_REPLACE ) {
            return returnToFileLandingPage();
        }
        //Files that have been finished and are now in the lower list on the page
        for (DataFile newFile : newFiles) {
            deleteTempFile(newFile);
        }

        //Files in the upload process but not yet finished
        for (DataFile newFile : uploadedFiles) {
            deleteTempFile(newFile);
        }
        if (workingVersion.getId() != null) {
            return returnToDraftVersion();
        }
        return returnToDatasetOnly();
    }

    
    /* deprecated; super inefficient, when called repeatedly on a long list 
       of files! 
       leaving the code here, commented out, for illustration purposes. -- 4.6
    public boolean isDuplicate(FileMetadata fileMetadata) {

        Map<String, Integer> MD5Map = new HashMap<String, Integer>();

        // TODO: 
        // think of a way to do this that doesn't involve populating this 
        // map for every file on the page? 
        // may not be that much of a problem, if we paginate and never display 
        // more than a certain number of files... Still, needs to be revisited
        // before the final 4.0. 
        // -- L.A. 4.0

        // make a "defensive copy" to avoid java.util.ConcurrentModificationException from being thrown
        // when uploading 100+ files
        List<FileMetadata> wvCopy = new ArrayList<>(workingVersion.getFileMetadatas());
        Iterator<FileMetadata> fmIt = wvCopy.iterator();

        while (fmIt.hasNext()) {
            FileMetadata fm = fmIt.next();
            String md5 = fm.getDataFile().getChecksumValue();
            if (md5 != null) {
                if (MD5Map.get(md5) != null) {
                    MD5Map.put(md5, MD5Map.get(md5).intValue() + 1);
                } else {
                    MD5Map.put(md5, 1);
                }
            }
        }

        return MD5Map.get(thisMd5) != null && MD5Map.get(thisMd5).intValue() > 1;
    }*/
   
    private HttpClient getClient() {
        // TODO: 
        // cache the http client? -- L.A. 4.0 alpha
        return new HttpClient();
    }

    /**
     * Is this page in File Replace mode
     * 
     * @return 
     */
    public boolean isFileReplaceOperation(){
        return (mode == FileEditMode.SINGLE_REPLACE)&&(fileReplacePageHelper!= null);
    }
    
    public boolean allowMultipleFileUpload(){
        
        return !isFileReplaceOperation();
    }
    
    public boolean showFileUploadFragment(){
        return mode == FileEditMode.UPLOAD || mode == FileEditMode.CREATE || mode == FileEditMode.SINGLE_REPLACE;
    }
    
    
    public boolean showFileUploadComponent(){
        if (mode == FileEditMode.UPLOAD || mode == FileEditMode.CREATE) {
           return true;
        }
        
        if (isFileReplaceOperation()){
            //msg("fileReplacePageHelper.showFileUploadComponent(): "+ fileReplacePageHelper.showFileUploadComponent());
            return fileReplacePageHelper.showFileUploadComponent();
            }

        return false;
        //return false;
    }
    

    /**
     * Download a file from drop box
     * 
     * @param fileLink
     * @return 
     */
    private InputStream getDropBoxInputStream(String fileLink, GetMethod dropBoxMethod){
        
        if (fileLink == null){
            return null;
        }
        
        // -----------------------------------------------------------
        // Make http call, download the file: 
        // -----------------------------------------------------------
        int status = 0;
        //InputStream dropBoxStream = null;

        try {
            status = getClient().executeMethod(dropBoxMethod);
            if (status == 200) {
                return dropBoxMethod.getResponseBodyAsStream();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to access DropBox url: {0}!", fileLink);
            return null;
        } 

        logger.log(Level.WARNING, "Failed to get DropBox InputStream for file: {0}", fileLink);
        return null;
    } // end: getDropBoxInputStream
                  
    
    /**
     * Using information from the DropBox choose, ingest the chosen files
     *  https://www.dropbox.com/developers/dropins/chooser/js
     * 
     * @param event
     */
    public void handleDropBoxUpload(ActionEvent event) {
        if (!uploadInProgress) {
            uploadInProgress = true;
        }
        logger.fine("handleDropBoxUpload");
        uploadComponentId = event.getComponent().getClientId();
        
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
            if ((!this.isUnlimitedUploadFileSize()) && (fileSize > this.getMaxFileUploadSizeInBytes())) {
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

            
            dFile = null;
            dropBoxMethod = new GetMethod(fileLink);

            // -----------------------------------------------------------
            // Download the file
            // -----------------------------------------------------------
            InputStream dropBoxStream = this.getDropBoxInputStream(fileLink, dropBoxMethod);
            if (dropBoxStream==null){
                logger.severe("Could not retrieve dropgox input stream for: " + fileLink);
                continue;  // Error skip this file
            }
            
            // -----------------------------------------------------------
            // Is this a FileReplaceOperation?  If so, then diverge!
            // -----------------------------------------------------------
            if (this.isFileReplaceOperation()){
              this.handleReplaceFileUpload(event, dropBoxStream, fileName, FileUtil.MIME_TYPE_UNDETERMINED_DEFAULT, null, event);
              this.setFileMetadataSelectedForTagsPopup(fileReplacePageHelper.getNewFileMetadatasBeforeSave().get(0));
              return;
             }
            // -----------------------------------------------------------

            
            List<DataFile> datafiles = new ArrayList<>(); 
            
            // -----------------------------------------------------------
            // Send it through the ingest service
            // -----------------------------------------------------------
            try {

                // Note: A single uploaded file may produce multiple datafiles - 
                // for example, multiple files can be extracted from an uncompressed
                // zip file.
                //datafiles = ingestService.createDataFiles(workingVersion, dropBoxStream, fileName, "application/octet-stream");
                datafiles = FileUtil.createDataFiles(workingVersion, dropBoxStream, fileName, "application/octet-stream", systemConfig);
                
            } catch (IOException ex) {
                this.logger.log(Level.SEVERE, "Error during ingest of DropBox file {0} from link {1}", new Object[]{fileName, fileLink});
                continue;
            }/*catch (FileExceedsMaxSizeException ex){
                this.logger.log(Level.SEVERE, "Error during ingest of DropBox file {0} from link {1}: {2}", new Object[]{fileName, fileLink, ex.getMessage()});
                continue;
            }*/ finally {
                // -----------------------------------------------------------
                // release connection for dropBoxMethod
                // -----------------------------------------------------------
                
                if (dropBoxMethod != null) {
                    dropBoxMethod.releaseConnection();
                }
                
                // -----------------------------------------------------------
                // close the  dropBoxStream
                // -----------------------------------------------------------
                try {
                    dropBoxStream.close();
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Failed to close the dropBoxStream for file: {0}", fileLink);
                }
            }
            
            if (datafiles == null){
                this.logger.log(Level.SEVERE, "Failed to create DataFile for DropBox file {0} from link {1}", new Object[]{fileName, fileLink});
                continue;
            }else{    
                // -----------------------------------------------------------
                // Check if there are duplicate files or ingest warnings
                // -----------------------------------------------------------
                uploadWarningMessage = processUploadedFileList(datafiles);
                logger.fine("Warning message during upload: " + uploadWarningMessage);
                /*if (warningMessage != null){
                     logger.fine("trying to send faces message to " + event.getComponent().getClientId());
                     FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(), new FacesMessage(FacesMessage.SEVERITY_ERROR, "upload failure", warningMessage));
                     if (uploadWarningMessage == null) {
                         uploadWarningMessage = warningMessage;
                     } else {
                         uploadWarningMessage = uploadWarningMessage.concat("; "+warningMessage);
                     }
                }*/
            }
            if(!uploadInProgress) {
                logger.warning("Upload in progress cancelled");
                for (DataFile newFile : datafiles) {
                    deleteTempFile(newFile);
                }
            }
        }
        
        if (localWarningMessage != null) {
            if (uploadWarningMessage == null) {
                uploadWarningMessage = localWarningMessage;
            } else {
                uploadWarningMessage = localWarningMessage.concat("; " + uploadWarningMessage);
            }
        }
    }
    
    public void uploadStarted() {
        // uploadStarted() is triggered by PrimeFaces <p:upload onStart=... when an upload is 
        // started. It will be called *once*, even if it is a multiple file upload 
        // (either through drag-and-drop or select menu). 
        logger.fine("upload started");
        
        uploadInProgress = true;        
    }
    
    
    private Boolean hasRsyncScript = false;
    public Boolean isHasRsyncScript() {
        return hasRsyncScript;
    }

    public void setHasRsyncScript(Boolean hasRsyncScript) {
        this.hasRsyncScript = hasRsyncScript;
    }

    private  void setUpRsync() {
        logger.fine("setUpRsync called...");
        if (DataCaptureModuleUtil.rsyncSupportEnabled(settingsWrapper.getValueForKey(SettingsServiceBean.Key.UploadMethods))
                && dataset.getFiles().isEmpty()) { //only check for rsync if no files exist
            try {
                ScriptRequestResponse scriptRequestResponse = commandEngine.submit(new RequestRsyncScriptCommand(dvRequestService.getDataverseRequest(), dataset));
                logger.fine("script: " + scriptRequestResponse.getScript());
                if (scriptRequestResponse.getScript() != null && !scriptRequestResponse.getScript().isEmpty()) {
                    setRsyncScript(scriptRequestResponse.getScript());
                    rsyncScriptFilename = DataCaptureModuleUtil.getScriptName(workingVersion);
                    setHasRsyncScript(true);
                } else {
                    setHasRsyncScript(false);
                }
            } catch (EJBException ex) {
                logger.warning("Problem getting rsync script (EJBException): " + EjbUtil.ejbExceptionToString(ex));
            } catch (RuntimeException ex) {
                logger.warning("Problem getting rsync script (RuntimeException): " + ex.getLocalizedMessage());
            } catch (CommandException cex) {
                logger.warning("Problem getting rsync script (Command Exception): " + cex.getLocalizedMessage());
            }
        }
    }
    
    public void downloadRsyncScript() {

        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("application/download");

        String contentDispositionString;

        contentDispositionString = "attachment;filename=" + rsyncScriptFilename;
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
        DatasetLock lock = datasetService.addDatasetLock(dataset.getId(), DatasetLock.Reason.DcmUpload, session.getUser() != null ? ((AuthenticatedUser)session.getUser()).getId() : null, lockInfoMessage);
        if (lock != null) {
            dataset.addLock(lock);
        } else {
            logger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", dataset.getId());
        }
        
    }
    
        /**
     * The contents of the script.
     */
    private String rsyncScript = "";

    public String getRsyncScript() {
        return rsyncScript;
    }

    public void setRsyncScript(String rsyncScript) {
        this.rsyncScript = rsyncScript;
    }

    private String rsyncScriptFilename;

    public String getRsyncScriptFilename() {
        return rsyncScriptFilename;
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
        
        for (DataFile dataFile : uploadedFiles) {
            fileMetadatas.add(dataFile.getFileMetadata());
            newFiles.add(dataFile);
        }
        if(uploadInProgress) {
            uploadedFiles = new ArrayList<>();
            uploadInProgress = false;
        }
        // refresh the warning message below the upload component, if exists:
        if (uploadComponentId != null) {
            if (uploadWarningMessage != null) {
                if (uploadWarningMessageIsNotAnError) {
                    FacesContext.getCurrentInstance().addMessage(uploadComponentId, new FacesMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.file.uploadWarning"), uploadWarningMessage));
                } else {
                    FacesContext.getCurrentInstance().addMessage(uploadComponentId, new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.file.uploadWarning"), uploadWarningMessage));
                }
            } else if (uploadSuccessMessage != null) {
                FacesContext.getCurrentInstance().addMessage(uploadComponentId, new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.file.uploadWorked"), uploadSuccessMessage));
            }
        }

        if(isFileReplaceOperation() && fileReplacePageHelper.hasContentTypeWarning()){
                    RequestContext context = RequestContext.getCurrentInstance();
                    RequestContext.getCurrentInstance().update("datasetForm:fileTypeDifferentPopup");
                    context.execute("PF('fileTypeDifferentPopup').show();");
        }

        // We clear the following duplicate warning labels, because we want to 
        // only inform the user of the duplicates dropped in the current upload 
        // attempt - for ex., one batch of drag-and-dropped files, or a single 
        // file uploaded through the file chooser. 
        dupeFileNamesExisting = null; 
        dupeFileNamesNew = null;
        multipleDupesExisting = false;
        multipleDupesNew = false; 
        uploadWarningMessage = null;
        uploadSuccessMessage = null; 
    }
    
    private String warningMessageForPopUp;

    public String getWarningMessageForPopUp() {
        return warningMessageForPopUp;
    }

    public void setWarningMessageForPopUp(String warningMessageForPopUp) {
        this.warningMessageForPopUp = warningMessageForPopUp;
    }

    private void handleReplaceFileUpload(FacesEvent event, InputStream inputStream, 
                        String fileName, 
                        String contentType,
                        FileUploadEvent nativeUploadEvent,
                        ActionEvent dropboxUploadEvent
    ){

        fileReplacePageHelper.resetReplaceFileHelper();

        saveEnabled = false;
        
        uploadComponentId = event.getComponent().getClientId();
        
        if (fileReplacePageHelper.handleNativeFileUpload(inputStream,
                                    fileName,
                                    contentType
                                )){
            saveEnabled = true;

            /**
             * If the file content type changed, let the user know
             */
            if (fileReplacePageHelper.hasContentTypeWarning()){
                //Add warning to popup instead of page for Content Type Difference
                setWarningMessageForPopUp(fileReplacePageHelper.getContentTypeWarning());
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
                
        } else {
            // See the comment above, on how upload messages are displayed.
            uploadWarningMessage = fileReplacePageHelper.getErrorMessages();
//            uploadWarningMessage += " ******* ";
            
            
            if (nativeUploadEvent != null){
//                uploadWarningMessage += " nativeUploadEvent ";
                
            }
            if (dropboxUploadEvent != null){
//                uploadWarningMessage += " dropboxUploadEvent ";
            }
            //FacesContext.getCurrentInstance().addMessage(
            //    uploadComponentId,                         
            //    new FacesMessage(FacesMessage.SEVERITY_ERROR, "upload failure", uploadWarningMessage));
        }
    }

    private String uploadWarningMessage = null; 
    private String uploadSuccessMessage = null; 
    private String uploadComponentId = null; 
    
    
    
    /**
     * Handle native file replace
     * @param event 
     * @throws java.io.IOException 
     */
    public void handleFileUpload(FileUploadEvent event) throws IOException {
        if (!uploadInProgress) {
            uploadInProgress = true;
        }
        
        if (event == null){
            throw new NullPointerException("event cannot be null");
        }
        
        UploadedFile uFile = event.getFile();
        if (uFile == null){
            throw new NullPointerException("uFile cannot be null");
        }


        /**
         * For File Replace, take a different code path
         */
        if (isFileReplaceOperation()){

            handleReplaceFileUpload(event, uFile.getInputstream(),
                                    uFile.getFileName(),
                                    uFile.getContentType(),
                                    event,
                                    null);
            if(fileReplacePageHelper.hasContentTypeWarning()){
                    RequestContext context = RequestContext.getCurrentInstance();
                    RequestContext.getCurrentInstance().update("datasetForm:fileTypeDifferentPopup");
                    context.execute("PF('fileTypeDifferentPopup').show();");
            }
            return;
               
        }

   
        List<DataFile> dFileList = null;
        
        try {
            // Note: A single uploaded file may produce multiple datafiles - 
            // for example, multiple files can be extracted from an uncompressed
            // zip file. 
            dFileList = FileUtil.createDataFiles(workingVersion, uFile.getInputstream(), uFile.getFileName(), uFile.getContentType(), systemConfig);
            
        } catch (IOException ioex) {
            logger.warning("Failed to process and/or save the file " + uFile.getFileName() + "; " + ioex.getMessage());
            return;
        } /*catch (FileExceedsMaxSizeException ex) {
            logger.warning("Failed to process and/or save the file " + uFile.getFileName() + "; " + ex.getMessage());
            return;
        }*/

        // -----------------------------------------------------------
        // These raw datafiles are then post-processed, in order to drop any files 
        // already in the dataset/already uploaded, and to correct duplicate file names, etc. 
        // -----------------------------------------------------------
        String warningMessage = processUploadedFileList(dFileList);
        
        if (warningMessage != null){
            uploadWarningMessage = warningMessage;
            FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(), new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.file.uploadWarning"), warningMessage));
            // save the component id of the p:upload widget, so that we could 
            // send an info message there, from elsewhere in the code:
            uploadComponentId = event.getComponent().getClientId();
        }
        
        if(!uploadInProgress) {
            logger.warning("Upload in progress cancelled");
            for (DataFile newFile : dFileList) {
                deleteTempFile(newFile);
            }
        }
    }

    /**
     *  After uploading via the site or Dropbox, 
     *  check the list of DataFile objects
     * @param dFileList 
     */
    
    private String dupeFileNamesExisting = null; 
    private String dupeFileNamesNew = null;
    private boolean multipleDupesExisting = false;
    private boolean multipleDupesNew = false;
    private boolean uploadInProgress = false;
    
    private String processUploadedFileList(List<DataFile> dFileList) {
        if (dFileList == null) {
            return null;
        }

        DataFile dataFile;
        String warningMessage = null;

        // NOTE: for native file uploads, the dFileList will only 
        // contain 1 file--method is called for every file even if the UI shows "simultaneous uploads"
        
        // -----------------------------------------------------------
        // Iterate through list of DataFile objects
        // -----------------------------------------------------------
        for (DataFile dFileList1 : dFileList) {
            dataFile = dFileList1;
            // -----------------------------------------------------------
            // Check for ingest warnings
            // -----------------------------------------------------------
            if (dataFile.isIngestProblem()) {
                if (dataFile.getIngestReportMessage() != null) {
                    if (warningMessage == null) {
                        warningMessage = dataFile.getIngestReportMessage();
                    } else {
                        warningMessage = warningMessage.concat("; " + dataFile.getIngestReportMessage());
                    }
                }
                dataFile.setIngestDone();
            }

            // -----------------------------------------------------------
            // Check for duplicates -- e.g. file is already in the dataset, 
            // or if another file with the same checksum has already been 
            // uploaded.
            // -----------------------------------------------------------
            if (isFileAlreadyInDataset(dataFile)) {
                if (dupeFileNamesExisting == null) {
                    dupeFileNamesExisting = dataFile.getFileMetadata().getLabel();
                } else {
                    dupeFileNamesExisting = dupeFileNamesExisting.concat(", " + dataFile.getFileMetadata().getLabel());
                    multipleDupesExisting = true;
                }
                // remove temp file
                deleteTempFile(dataFile);
            } else if (isFileAlreadyUploaded(dataFile)) {
                if (dupeFileNamesNew == null) {
                    dupeFileNamesNew = dataFile.getFileMetadata().getLabel();
                } else {
                    dupeFileNamesNew = dupeFileNamesNew.concat(", " + dataFile.getFileMetadata().getLabel());
                    multipleDupesNew = true;
                }
                // remove temp file
                deleteTempFile(dataFile);
            } else {
                // OK, this one is not a duplicate, we want it. 
                // But let's check if its filename is a duplicate of another 
                // file already uploaded, or already in the dataset:
                dataFile.getFileMetadata().setLabel(duplicateFilenameCheck(dataFile.getFileMetadata()));
                if (isTemporaryPreviewAvailable(dataFile.getStorageIdentifier(), dataFile.getContentType())) {
                    dataFile.setPreviewImageAvailable(true);
                }
                uploadedFiles.add(dataFile);
                // We are NOT adding the fileMetadata to the list that is being used
                // to render the page; we'll do that once we know that all the individual uploads
                // in this batch (as in, a bunch of drag-and-dropped files) have finished. 
                //fileMetadatas.add(dataFile.getFileMetadata());
            }

            /*
             preserved old, pre 4.6 code - mainly as an illustration of how we used to do this. 
            
            if (!isDuplicate(dataFile.getFileMetadata())) {
                newFiles.add(dataFile);        // looks good
                fileMetadatas.add(dataFile.getFileMetadata());
            } else {
                if (duplicateFileNames == null) {
                    duplicateFileNames = dataFile.getFileMetadata().getLabel();
                } else {
                    duplicateFileNames = duplicateFileNames.concat(", " + dataFile.getFileMetadata().getLabel());
                    multipleDupes = true;
                }

                // remove the file from the dataset (since createDataFiles has already linked
                // it to the dataset!
                // first, through the filemetadata list, then through tht datafiles list:
                Iterator<FileMetadata> fmIt = dataset.getEditVersion().getFileMetadatas().iterator();
                while (fmIt.hasNext()) {
                    FileMetadata fm = fmIt.next();
                    if (fm.getId() == null && dataFile.getStorageIdentifier().equals(fm.getDataFile().getStorageIdentifier())) {
                        fmIt.remove();
                        break;
                    }
                }

                Iterator<DataFile> dfIt = dataset.getFiles().iterator();
                while (dfIt.hasNext()) {
                    DataFile dfn = dfIt.next();
                    if (dfn.getId() == null && dataFile.getStorageIdentifier().equals(dfn.getStorageIdentifier())) {
                        dfIt.remove();
                        break;
                    }
                }
            } */
        }
        
        // -----------------------------------------------------------
        // Format error message for duplicate files
        // (note the separate messages for the files already in the dataset, 
        // and the newly uploaded ones)
        // -----------------------------------------------------------
        if (dupeFileNamesExisting != null) {
            String duplicateFilesErrorMessage = null;
            if (multipleDupesExisting) {
                duplicateFilesErrorMessage =  getBundleString("dataset.files.exist") + dupeFileNamesExisting + getBundleString("dataset.file.skip");
            } else {
            	duplicateFilesErrorMessage = getBundleString("dataset.file.exist") + dupeFileNamesExisting;
            }
            if (warningMessage == null) {
                warningMessage = duplicateFilesErrorMessage;
            } else {
                warningMessage = warningMessage.concat("; " + duplicateFilesErrorMessage);
            }
        }

        if (dupeFileNamesNew != null) {
            String duplicateFilesErrorMessage = null;
            if (multipleDupesNew) {
            	duplicateFilesErrorMessage = getBundleString("dataset.files.duplicate") + dupeFileNamesNew + getBundleString("dataset.file.skip");
            } else {
            	duplicateFilesErrorMessage = getBundleString("dataset.file.duplicate") + dupeFileNamesNew + getBundleString("dataset.file.skip");
            }

            if (warningMessage == null) {
                warningMessage = duplicateFilesErrorMessage;
            } else {
                warningMessage = warningMessage.concat("; " + duplicateFilesErrorMessage);
            }
        }

        if (warningMessage != null) {
            logger.severe(warningMessage);
            return warningMessage;
        }

        return null;
    }
    
    private Map<String, String> temporaryThumbnailsMap = new HashMap<>();
    
    public boolean isTemporaryPreviewAvailable(String fileSystemId, String mimeType) {
        if (temporaryThumbnailsMap.get(fileSystemId) != null && !temporaryThumbnailsMap.get(fileSystemId).isEmpty()) {
            return true;
        }
        
        if ("".equals(temporaryThumbnailsMap.get(fileSystemId))) {
            // we've already looked once - and there's no thumbnail.
            return false;
        }
        
        String filesRootDirectory = System.getProperty("dataverse.files.directory");
        if (filesRootDirectory == null || filesRootDirectory.isEmpty()) {
            filesRootDirectory = "/tmp/files";
        }

        String fileSystemName = filesRootDirectory + "/temp/" + fileSystemId;
        
        String imageThumbFileName = null;
        
        // ATTENTION! TODO: the current version of the method below may not be checking if files are already cached!
        if ("application/pdf".equals(mimeType)) {
            imageThumbFileName = ImageThumbConverter.generatePDFThumbnailFromFile(fileSystemName, ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
        } else if (mimeType != null && mimeType.startsWith("image/")) {
            imageThumbFileName = ImageThumbConverter.generateImageThumbnailFromFile(fileSystemName, ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
        }
        
        if (imageThumbFileName != null) {
            File imageThumbFile = new File(imageThumbFileName);
            if (imageThumbFile.exists()) {
                String previewAsBase64 = ImageThumbConverter.getImageAsBase64FromFile(imageThumbFile); 
                if (previewAsBase64 != null) {
                    temporaryThumbnailsMap.put(fileSystemId, previewAsBase64);
                    return true;
                } else {
                    temporaryThumbnailsMap.put(fileSystemId, "");
                }
            }
        }
            
        return false;
    }
    
    public String getTemporaryPreviewAsBase64(String fileSystemId) {
        return temporaryThumbnailsMap.get(fileSystemId);
    }

    private Set<String> fileLabelsExisting = null; 
    
    private String duplicateFilenameCheck(FileMetadata fileMetadata) {
        if (fileLabelsExisting == null) {
            fileLabelsExisting = IngestUtil.existingPathNamesAsSet(workingVersion);
        }
        
        return IngestUtil.duplicateFilenameCheck(fileMetadata, fileLabelsExisting);
    }

    private  Map<String, Integer> checksumMapOld = null; // checksums of the files already in the dataset
    private  Map<String, Integer> checksumMapNew = null; // checksums of the new files already uploaded
    
    private void initChecksumMap() {
        checksumMapOld = new HashMap<>();

        Iterator<FileMetadata> fmIt = workingVersion.getFileMetadatas().iterator();

        while (fmIt.hasNext()) {
            FileMetadata fm = fmIt.next();
            if (fm.getDataFile() != null && fm.getDataFile().getId() != null) {
                String chksum = fm.getDataFile().getChecksumValue();
                if (chksum != null) {
                    checksumMapOld.put(chksum, 1);

                }
            }
        }

    }
    
    private boolean isFileAlreadyInDataset(DataFile dataFile) {
        if (checksumMapOld == null) {
            initChecksumMap();
        }
        
        String chksum = dataFile.getChecksumValue();
        
        return chksum == null ? false : checksumMapOld.get(chksum) != null;
    }
    
    private boolean isFileAlreadyUploaded(DataFile dataFile) {
        if (checksumMapNew == null) {
            checksumMapNew = new HashMap<>();
        }
        
        String chksum = dataFile.getChecksumValue();
        
        if (chksum == null) {
            return false;
        }
        
        if (checksumMapNew.get(chksum) != null) {
            return true;
        }
        
        checksumMapNew.put(chksum, 1);
        return false;
    }
    
 
    public boolean isLocked() {
        if (dataset != null) {
            logger.log(Level.FINE, "checking lock status of dataset {0}", dataset.getId());
            if (dataset.isLocked()) {
                // refresh the dataset and version, if the current working
                // version of the dataset is locked:
            }
            Dataset lookedupDataset = datasetService.find(dataset.getId());
            
            if ( (lookedupDataset!=null) && lookedupDataset.isLocked() ) {
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
        if (!fileDownloadHelper.canDownloadFile(fileMetadata)) {
            return false;
        }

        return datafileService.isThumbnailAvailable(fileMetadata.getDataFile());
    }
    

    
    private Boolean lockedFromEditsVar;
    
    public boolean isLockedFromEdits() {
        if(null == lockedFromEditsVar ) {
            try {
                permissionService.checkEditDatasetLock(dataset, dvRequestService.getDataverseRequest(), new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromEditsVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromEditsVar = true;
            }
        }
        return lockedFromEditsVar;
    }
    
    // Methods for edit functions that are performed on one file at a time, 
    // in popups that block the rest of the page:
    
    private FileMetadata fileMetadataSelected = null;

    public void  setFileMetadataSelected(FileMetadata fm){
       setFileMetadataSelected(fm, null); 
    }
    
    public void setFileMetadataSelected(FileMetadata fm, String guestbook) {

        fileMetadataSelected = fm;
        logger.log(Level.FINE, "set the file for the advanced options popup ({0})", fileMetadataSelected.getLabel());
    }

    public FileMetadata getFileMetadataSelected() {
        if (fileMetadataSelected != null) {
            logger.log(Level.FINE, "returning file metadata for the advanced options popup ({0})", fileMetadataSelected.getLabel());
        } else {
            logger.fine("file metadata for the advanced options popup is null.");
        }
        return fileMetadataSelected;
    }

    public void clearFileMetadataSelected() {
        fileMetadataSelected = null;
    }
    
    public boolean isDesignatedDatasetThumbnail (FileMetadata fileMetadata) {
        if (fileMetadata != null) {
            if (fileMetadata.getDataFile() != null) {
                if (fileMetadata.getDataFile().getId() != null) {
                    //if (fileMetadata.getDataFile().getOwner() != null) {
                        if (fileMetadata.getDataFile().equals(dataset.getThumbnailFile())) {
                            return true;
                        }
                    //}
                }
            }
        }
        return false;
    }
    
    /* 
     * Items for the "Designated this image as the Dataset thumbnail: 
     */
    
    private FileMetadata fileMetadataSelectedForThumbnailPopup = null; 

    /**
     * @param fm
     * @todo For consistency, we should disallow users from setting the
     * thumbnail to a restricted file. We enforce this rule in the newer
     * workflow in dataset-widgets.xhtml. The logic to show the "Set Thumbnail"
     * button is in editFilesFragment.xhtml and it would be nice to move it to
     * Java since it's getting long and a bit complicated.
     */
    public void  setFileMetadataSelectedForThumbnailPopup(FileMetadata fm){
       fileMetadataSelectedForThumbnailPopup = fm; 
       alreadyDesignatedAsDatasetThumbnail = getUseAsDatasetThumbnail();

    }
    
    public FileMetadata getFileMetadataSelectedForThumbnailPopup() {
        return fileMetadataSelectedForThumbnailPopup;
    }
    
    public void clearFileMetadataSelectedForThumbnailPopup() {
        fileMetadataSelectedForThumbnailPopup = null;
    }
    
    private boolean alreadyDesignatedAsDatasetThumbnail = false; 
    
    public boolean getUseAsDatasetThumbnail() {

        return isDesignatedDatasetThumbnail(fileMetadataSelectedForThumbnailPopup);
    }

    public void setUseAsDatasetThumbnail(boolean useAsThumbnail) {
        if (fileMetadataSelectedForThumbnailPopup != null) {
            if (fileMetadataSelectedForThumbnailPopup.getDataFile() != null) {
                if (useAsThumbnail) {
                    dataset.setThumbnailFile(fileMetadataSelectedForThumbnailPopup.getDataFile());
                } else if (getUseAsDatasetThumbnail()) {
                    dataset.setThumbnailFile(null);
                }
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
            String successMessage = getBundleString("file.assignedDataverseImage.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileMetadataSelectedForThumbnailPopup.getLabel());
            JsfHelper.addFlashMessage(successMessage);

            datasetUpdateRequired = true;
        }

        // And reset the selected fileMetadata:
        fileMetadataSelectedForThumbnailPopup = null;
    }

    public void deleteDatasetLogoAndUseThisDataFileAsThumbnailInstead() {
        logger.log(Level.FINE, "For dataset id {0} the current thumbnail is from a dataset logo rather than a dataset file, blowing away the logo and using this FileMetadata id instead: {1}", new Object[]{dataset.getId(), fileMetadataSelectedForThumbnailPopup});
        /**
         * @todo Rather than deleting and merging right away, try to respect how
         * this page seems to stage actions and giving the user a chance to
         * review before clicking "Save Changes".
         */
        try {
            DatasetThumbnail datasetThumbnail = commandEngine.submit(new UpdateDatasetThumbnailCommand(dvRequestService.getDataverseRequest(), dataset, UpdateDatasetThumbnailCommand.UserIntent.setDatasetFileAsThumbnail, fileMetadataSelectedForThumbnailPopup.getDataFile().getId(), null));
            // look up the dataset again because the UpdateDatasetThumbnailCommand mutates (merges) the dataset
            dataset = datasetService.find(dataset.getId());
        } catch (CommandException ex) {
            String error = "Problem setting thumbnail for dataset id " + dataset.getId() + ".: " + ex;
            // show this error to the user?
            logger.info(error);
        }
    }

    public boolean isThumbnailIsFromDatasetLogoRatherThanDatafile() {
        DatasetThumbnail datasetThumbnail = dataset.getDatasetThumbnail();
        return datasetThumbnail != null && !datasetThumbnail.isFromDataFile();
    }

    /* 
     * Items for the "Tags (Categories)" popup.
     *
     */
    private FileMetadata fileMetadataSelectedForTagsPopup = null; 

    public void  setFileMetadataSelectedForTagsPopup(FileMetadata fm){
        fileMetadataSelectedForTagsPopup = fm;
    }
    
    public FileMetadata getFileMetadataSelectedForTagsPopup() {
        return fileMetadataSelectedForTagsPopup;
    }
    
    public void clearFileMetadataSelectedForTagsPopup() {
        fileMetadataSelectedForTagsPopup = null;
    }
    
    /*
     * 1. Tabular File Tags: 
     */
    
    private List<String> tabFileTags = null;

    public List<String> getTabFileTags() {
        if (tabFileTags == null) {
            tabFileTags = DataFileTag.listTags();
        }
        return tabFileTags;
    }

    public void setTabFileTags(List<String> tabFileTags) {
        this.tabFileTags = tabFileTags;
    }
    
    private String[] selectedTabFileTags = {};

    public String[] getSelectedTabFileTags() {
        return selectedTabFileTags;
    }

    public void setSelectedTabFileTags(String[] selectedTabFileTags) {
        this.selectedTabFileTags = selectedTabFileTags;
    }

    private String[] selectedTags = {};
    
    public void refreshTagsPopUp(FileMetadata fm){
        setFileMetadataSelectedForTagsPopup(fm);
        refreshCategoriesByName();
        refreshTabFileTagsByName();
    }
    
    private List<String> tabFileTagsByName;

    public List<String> getTabFileTagsByName() {
        return tabFileTagsByName;
    }

    public void setTabFileTagsByName(List<String> tabFileTagsByName) {
        this.tabFileTagsByName = tabFileTagsByName;
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
    
    private void refreshCategoriesByName(){
        categoriesByName= new ArrayList<>();
        for (String category: dataset.getCategoriesByName() ){
            categoriesByName.add(category);
        }
        refreshSelectedTags();
    }
    
    
    private List<String> categoriesByName;

    public List<String> getCategoriesByName() {
        return categoriesByName;
    }

    public void setCategoriesByName(List<String> categoriesByName) {
        this.categoriesByName = categoriesByName;
    }
    
    private void refreshSelectedTags() {
        selectedTags = null;
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

    public String[] getSelectedTags() {
        return selectedTags;
    }

    public void setSelectedTags(String[] selectedTags) {
        this.selectedTags = selectedTags;
    }
    


    /*
     * "File Tags" (aka "File Categories"): 
    */
    
    private String newCategoryName = null;

    public String getNewCategoryName() {
        return newCategoryName;
    }

    public void setNewCategoryName(String newCategoryName) {
        this.newCategoryName = newCategoryName;
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
                
        // 2. Tabular DataFile Tags: 
        
        if (fileMetadataSelectedForTagsPopup.getDataFile() != null && tabularDataTagsUpdated && selectedTabFileTags != null) {
            fileMetadataSelectedForTagsPopup.getDataFile().setTags(null);
            for (String selectedTabFileTag : selectedTabFileTags) {
                DataFileTag tag = new DataFileTag();
                try {
                    tag.setTypeByLabel(selectedTabFileTag);
                    tag.setDataFile(fileMetadataSelectedForTagsPopup.getDataFile());
                    fileMetadataSelectedForTagsPopup.getDataFile().addTag(tag);

                } catch (IllegalArgumentException iax) {
                    // ignore 
                }
            }

            datasetUpdateRequired = true;
        }
        
        fileMetadataSelectedForTagsPopup = null;

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
        
    /* 
     * Items for the "Advanced (Ingest) Options" popup. 
     * 
     */
    private FileMetadata fileMetadataSelectedForIngestOptionsPopup = null; 

    public void  setFileMetadataSelectedForIngestOptionsPopup(FileMetadata fm){
       fileMetadataSelectedForIngestOptionsPopup = fm; 
    }
    
    public FileMetadata getFileMetadataSelectedForIngestOptionsPopup() {
        return fileMetadataSelectedForIngestOptionsPopup;
    }
    
    public void clearFileMetadataSelectedForIngestOptionsPopup() {
        fileMetadataSelectedForIngestOptionsPopup = null;
    }
    
    private String ingestLanguageEncoding = null;

    public String getIngestLanguageEncoding() {
        if (ingestLanguageEncoding == null) {
            return BundleUtil.getStringFromBundle("editdatafilepage.defaultLanguageEncoding");
        }
        return ingestLanguageEncoding;
    }

    public void setIngestLanguageEncoding(String ingestLanguageEncoding) {
        this.ingestLanguageEncoding = ingestLanguageEncoding;
    }

    public void setIngestEncoding(String ingestEncoding) {
        ingestLanguageEncoding = ingestEncoding;
    }

    private String savedLabelsTempFile = null;

    public void handleLabelsFileUpload(FileUploadEvent event) {
        logger.fine("entering handleUpload method.");
        UploadedFile file = event.getFile();

        if (file != null) {

            InputStream uploadStream = null;
            try {
                uploadStream = file.getInputstream();
            } catch (IOException ioex) {
                logger.info("the file " + file.getFileName() + " failed to upload!");
                List<String> args = Arrays.asList(file.getFileName());
                String msg = BundleUtil.getStringFromBundle("dataset.file.uploadFailure.detailmsg", args);
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.file.uploadFailure"), msg);
                FacesContext.getCurrentInstance().addMessage(null, message);
                return;
            }

            savedLabelsTempFile = saveTempFile(uploadStream);

            logger.fine(file.getFileName() + " is successfully uploaded.");
            List<String> args = Arrays.asList(file.getFileName());
            FacesMessage message = new FacesMessage(BundleUtil.getStringFromBundle("dataset.file.upload", args));
            FacesContext.getCurrentInstance().addMessage(null, message);
        }

        // process file (i.e., just save it in a temp location; for now):
    }

    private String saveTempFile(InputStream input) {
        if (input == null) {
            return null;
        }
        byte[] buffer = new byte[8192];
        int bytesRead = 0;
        File labelsFile = null;
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

    public void saveAdvancedOptions() {

        // Language encoding for SPSS SAV (and, possibly, other tabular ingests:) 
        if (ingestLanguageEncoding != null) {
            if (fileMetadataSelectedForIngestOptionsPopup != null && fileMetadataSelectedForIngestOptionsPopup.getDataFile() != null) {
                if (fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest() == null) {
                    IngestRequest ingestRequest = new IngestRequest();
                    ingestRequest.setDataFile(fileMetadataSelectedForIngestOptionsPopup.getDataFile());
                    fileMetadataSelectedForIngestOptionsPopup.getDataFile().setIngestRequest(ingestRequest);

                }
                fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest().setTextEncoding(ingestLanguageEncoding);
            }
        }
        ingestLanguageEncoding = null;

        // Extra labels for SPSS POR (and, possibly, other tabular ingests:)
        // (we are adding this parameter to the IngestRequest now, instead of back
        // when it was uploaded. This is because we want the user to be able to 
        // hit cancel and bail out, until they actually click 'save' in the 
        // "advanced options" popup) -- L.A. 4.0 beta 11
        if (savedLabelsTempFile != null) {
            if (fileMetadataSelectedForIngestOptionsPopup != null && fileMetadataSelectedForIngestOptionsPopup.getDataFile() != null) {
                if (fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest() == null) {
                    IngestRequest ingestRequest = new IngestRequest();
                    ingestRequest.setDataFile(fileMetadataSelectedForIngestOptionsPopup.getDataFile());
                    fileMetadataSelectedForIngestOptionsPopup.getDataFile().setIngestRequest(ingestRequest);
                }
                fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest().setLabelsFile(savedLabelsTempFile);
            }
        }
        savedLabelsTempFile = null;

        fileMetadataSelectedForIngestOptionsPopup = null;
    }

    private void populateFileMetadatas() {

        if (selectedFileIdsList != null) {

            Long datasetVersionId = workingVersion.getId();

            if (datasetVersionId != null) {
                // The version has a database id - this is an existing version, 
                // that had been saved previously. So we can look up the file metadatas
                // by the file and version ids:
                for (Long fileId : selectedFileIdsList) {
                    logger.fine("attempting to retrieve file metadata for version id " + datasetVersionId + " and file id " + fileId);
                    FileMetadata fileMetadata =  datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(datasetVersionId, fileId);
                    if (fileMetadata != null) {
                        logger.fine("Success!");
                        fileMetadatas.add(fileMetadata);
                    } else {
                        logger.fine("Failed to find file metadata.");
                    }
                }
            } else {
                logger.fine("Brand new edit version - no database id.");
                for (FileMetadata fileMetadata : workingVersion.getFileMetadatas()) {
                    for (Long fileId : selectedFileIdsList) {
                        if (fileId.equals(fileMetadata.getDataFile().getId())) {
                            logger.fine("Success! - found the file id "+fileId+" in the brand new edit version.");
                            fileMetadatas.add(fileMetadata);
                            selectedFileIdsList.remove(fileId);
                            break;
                        }
                    }
                    
                    // If we've already gone through all the file ids on the list - 
                    // we can stop going through the filemetadatas:
                    
                    if (selectedFileIdsList.size() < 1) {
                        break;
                    }
                }
            }
        }
    }
    
}
