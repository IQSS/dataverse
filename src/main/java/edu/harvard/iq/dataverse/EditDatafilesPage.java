package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.provenance.ProvPopupFragmentBean;
import edu.harvard.iq.dataverse.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.FileSizeChecker;
import edu.harvard.iq.dataverse.datasetutility.FileReplaceException;
import edu.harvard.iq.dataverse.datasetutility.FileReplacePageHelper;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.DataAccessOption;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.S3AccessIO;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtil;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.RequestRsyncScriptCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDataFilesCommand;
import edu.harvard.iq.dataverse.ingest.IngestRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestUtil;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.storageuse.UploadSessionQuotaLimit;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.WebloaderUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.EjbUtil;
import edu.harvard.iq.dataverse.util.FileMetadataUtil;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import edu.harvard.iq.dataverse.util.file.CreateDataFileResult;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import org.apache.commons.io.IOUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.event.FacesEvent;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.primefaces.PrimeFaces;

/**
 *
 * @author Leonid Andreev
 */
@ViewScoped
@Named("EditDatafilesPage")
public class EditDatafilesPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(EditDatafilesPage.class.getCanonicalName());

    public enum FileEditMode {

        EDIT, UPLOAD, CREATE, REPLACE
    };

    public enum Referrer {
        DATASET, FILE
    };

    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseServiceBean dataverseService;
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
    @Inject
    PermissionsWrapper permissionsWrapper;
    @Inject
    FileDownloadHelper fileDownloadHelper;
    @Inject
    ProvPopupFragmentBean provPopupFragmentBean;
    @Inject
    SettingsWrapper settingsWrapper;
    @Inject
    LicenseServiceBean licenseServiceBean;
    @Inject
    DataFileCategoryServiceBean dataFileCategoryService;
    @Inject
    EditDataFilesPageHelper editDataFilesPageHelper;

    private Dataset dataset = new Dataset();

    private FileReplacePageHelper fileReplacePageHelper;

    private String selectedFileIdsString = null;
    private FileEditMode mode;
    private Referrer referrer = Referrer.DATASET;
    private List<Long> selectedFileIdsList = new ArrayList<>();
    private List<FileMetadata> fileMetadatas = new ArrayList<>();
    ;

    
    private Long ownerId;
    private Long versionId;
    private List<DataFile> newFiles = new ArrayList<>();
    private List<DataFile> uploadedFiles = new ArrayList<>();
    private List<DataFile> uploadedInThisProcess = new ArrayList<>();

    private DatasetVersion workingVersion;
    private DatasetVersion clone;
    private String dropBoxSelection = "";
    private String displayCitation;
    private boolean tabularDataTagsUpdated = false;

    private String persistentId;

    private String versionString = "";

    private boolean saveEnabled = false;

    // Used to store results of permissions checks
    private final Map<String, Boolean> datasetPermissionMap = new HashMap<>(); // { Permission human_name : Boolean }

    // Size limit of an individual file: (set for the storage volume used)
    private Long maxFileUploadSizeInBytes = null;
    // Total amount of data that the user should be allowed to upload.
    // Will be calculated in real time based on various level quotas - 
    // for this user and/or this collection/dataset, etc. We should 
    // assume that it may change during the user session.
    private Long maxTotalUploadSizeInBytes = null;
    private Long maxIngestSizeInBytes = null;
    // CSV: 4.8 MB, DTA: 976.6 KB, XLSX: 5.7 MB, etc.
    private String humanPerFormatTabularLimits = null;
    private Integer multipleUploadFilesLimit = null; 
    
    //MutableBoolean so it can be passed from DatasetPage, supporting DatasetPage.cancelCreate()
    private MutableBoolean uploadInProgress = null;

    private final int NUMBER_OF_SCROLL_ROWS = 25;

    private DataFile singleFile = null;
    private UploadSessionQuotaLimit uploadSessionQuota = null; 

    public DataFile getSingleFile() {
        return singleFile;
    }

    public void setSingleFile(DataFile singleFile) {
        this.singleFile = singleFile;
    }

    public String getSelectedFileIds() {
        return selectedFileIdsString;
    }

    public DataFile getFileToReplace() {
        if (!this.isFileReplaceOperation()) {
            return null;
        }
        if (this.fileReplacePageHelper == null) {
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

    public Referrer getReferrer() {
        return referrer;
    }

    public void setReferrer(Referrer referrer) {
        this.referrer = referrer;
    }

    public List<FileMetadata> getFileMetadatas() {

        // -------------------------------------
        // Handle a Replace operation
        //  - The List<FileMetadata> comes from a different source
        // -------------------------------------
        if (isFileReplaceOperation()) {
            if (fileReplacePageHelper.wasPhase1Successful()) {
                logger.fine("Replace: File metadatas 'list' of 1 from the fileReplacePageHelper.");
                return fileReplacePageHelper.getNewFileMetadatasBeforeSave();
            } else {
                logger.fine("Replace: replacement file not yet uploaded.");
                return null;
            }
        }

        if (fileMetadatas != null) {
            logger.fine("Returning a list of " + fileMetadatas.size() + " file metadatas.");
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

        logger.fine("scroll height percentage: " + perc);
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

    public String getHumanMaxFileUploadSizeInBytes() {
        return FileSizeChecker.bytesToHumanReadable(this.maxFileUploadSizeInBytes);
    }

    public boolean isUnlimitedUploadFileSize() {

        return this.maxFileUploadSizeInBytes == null;
    }
    
    public Long getMaxTotalUploadSizeInBytes() {
        return maxTotalUploadSizeInBytes;
    }
    
    public String getHumanMaxTotalUploadSizeInBytes() {
        return FileSizeChecker.bytesToHumanReadable(maxTotalUploadSizeInBytes);
    }
    
    public boolean isStorageQuotaEnforced() {
        return uploadSessionQuota != null; 
    }

    public Long getMaxIngestSizeInBytes() {
        return maxIngestSizeInBytes;
    }

    public String getHumanMaxIngestSizeInBytes() {
        return FileSizeChecker.bytesToHumanReadable(this.maxIngestSizeInBytes);
    }

    public String getHumanPerFormatTabularLimits() {
        return humanPerFormatTabularLimits;
    }

    public String populateHumanPerFormatTabularLimits() {
        String keyPrefix = ":TabularIngestSizeLimit:";
        List<String> formatLimits = new ArrayList<>();
        for (Setting setting : settingsService.listAll()) {
            String name = setting.getName();
            if (!name.startsWith(keyPrefix)) {
                continue;
            }
            String tabularName = setting.getName().substring(keyPrefix.length());
            String bytes = setting.getContent();
            String humanReadableSize = FileSizeChecker.bytesToHumanReadable(Long.valueOf(bytes));
            formatLimits.add(tabularName + ": " + humanReadableSize);
        }
        return String.join(", ", formatLimits);
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
    public boolean doesSessionUserHaveDataSetPermission(Permission permissionToCheck) {
        if (permissionToCheck == null) {
            return false;
        }

        String permName = permissionToCheck.getHumanName();

        // Has this check already been done? 
        // 
        if (this.datasetPermissionMap.containsKey(permName)) {
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

    public String initCreateMode(String modeToken, DatasetVersion version, MutableBoolean inProgress, List<DataFile> newFilesList, List<DataFile> uploadedFilesList, List<FileMetadata> selectedFileMetadatasList) {
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

        workingVersion = version;
        dataset = version.getDataset();
        mode = FileEditMode.CREATE;
        uploadInProgress = inProgress;
        newFiles = newFilesList;
        uploadedFiles = uploadedFilesList;
        selectedFiles = selectedFileMetadatasList;

        this.maxFileUploadSizeInBytes = systemConfig.getMaxFileUploadSizeForStore(dataset.getEffectiveStorageDriverId());
        if (systemConfig.isStorageQuotasEnforced()) {
            this.uploadSessionQuota = datafileService.getUploadSessionQuotaLimit(dataset);
            if (this.uploadSessionQuota != null) {
                this.maxTotalUploadSizeInBytes = uploadSessionQuota.getRemainingQuotaInBytes();
            }
        } else {
            this.maxTotalUploadSizeInBytes = null; 
        }
        this.maxIngestSizeInBytes = systemConfig.getTabularIngestSizeLimit();
        this.humanPerFormatTabularLimits = populateHumanPerFormatTabularLimits();
        this.multipleUploadFilesLimit = systemConfig.getMultipleUploadFilesLimit();
        
        logger.fine("done");

        saveEnabled = true;
        
        return null;
    }
    
    public boolean isQuotaExceeded() {
        return systemConfig.isStorageQuotasEnforced() && uploadSessionQuota != null && uploadSessionQuota.getRemainingQuotaInBytes() == 0;
    }

    public String init() {
        // default mode should be EDIT
        if (mode == null) {
            mode = FileEditMode.EDIT;
        }

        newFiles = new ArrayList<>();
        uploadedFiles = new ArrayList<>();
        uploadInProgress = new MutableBoolean(false);

        if (dataset.getId() != null) {
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

        workingVersion = dataset.getOrCreateEditVersion();

        //TODO: review if we we need this check; 
        // as getEditVersion should either return the exisiting draft or create a new one      
        if (workingVersion == null || !workingVersion.isDraft()) {
            // Sorry, we couldn't find/obtain a draft version for this dataset!
            return permissionsWrapper.notFound();
        }

        // Check if they have permission to modify this dataset: 
        if (!permissionService.on(dataset).has(Permission.EditDataset)) {
            return permissionsWrapper.notAuthorized();
        }
        
        clone = workingVersion.cloneDatasetVersion();
        this.maxFileUploadSizeInBytes = systemConfig.getMaxFileUploadSizeForStore(dataset.getEffectiveStorageDriverId());
        if (systemConfig.isStorageQuotasEnforced()) {
            this.uploadSessionQuota = datafileService.getUploadSessionQuotaLimit(dataset);
            if (this.uploadSessionQuota != null) {
                this.maxTotalUploadSizeInBytes = uploadSessionQuota.getRemainingQuotaInBytes();
            }
        }
        this.maxIngestSizeInBytes = systemConfig.getTabularIngestSizeLimit();
        this.humanPerFormatTabularLimits = populateHumanPerFormatTabularLimits();
        this.multipleUploadFilesLimit = systemConfig.getMultipleUploadFilesLimit();        
        
        hasValidTermsOfAccess = isHasValidTermsOfAccess();
        if (!hasValidTermsOfAccess) {
            PrimeFaces.current().executeScript("PF('blockDatasetForm').show()");
            PrimeFaces.current().executeScript("PF('accessPopup').show()");
            JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.message.editTerms.label"), BundleUtil.getStringFromBundle("dataset.message.editTerms.message"));
            return "";
        }
        // -------------------------------------------
        //  Is this a file replacement operation?
        // -------------------------------------------
        if (mode == FileEditMode.REPLACE) {
            /*
            http://localhost:8080/editdatafiles.xhtml?mode=REPLACE&datasetId=26&fid=726
             */
            DataFile fileToReplace = loadFileToReplace();
            if (fileToReplace == null) {
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
        } else if (mode == FileEditMode.EDIT) {

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
                        if (FileEditMode.EDIT == mode && Referrer.FILE == referrer) {
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

            if (FileEditMode.EDIT == mode && Referrer.FILE == referrer) {
                if (fileMetadatas.get(0).getDatasetVersion().getId() != null) {
                    versionString = "DRAFT";
                }
            }

        }

        saveEnabled = true;
        if (mode == FileEditMode.UPLOAD && workingVersion.getFileMetadatas().isEmpty() && rsyncUploadSupported()) {
            setUpRsync();
        }

        if (isHasPublicStore()){
            JH.addMessage(FacesMessage.SEVERITY_WARN, getBundleString("dataset.message.label.fileAccess"), getBundleString("dataset.message.publicInstall"));
        }
       
        return null;
    }

    private void msg(String s) {
        System.out.println(s);
    }

    /**
     * For single file replacement, load the file to replace
     *
     * @return
     */
    private DataFile loadFileToReplace() {

        Map<String, String> params = FacesContext.getCurrentInstance().
                getExternalContext().getRequestParameterMap();

        if (params.containsKey("fid")) {
            String fid = params.get("fid");
            if ((!fid.isEmpty()) && (StringUtils.isNumeric(fid))) {
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

    public void restrictFiles(boolean restricted) throws UnsupportedOperationException {

        if (restricted) { // get values from access popup
            workingVersion.getTermsOfUseAndAccess().setTermsOfAccess(termsOfAccess);
            workingVersion.getTermsOfUseAndAccess().setFileAccessRequest(fileAccessRequest);
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
            JsfHelper.addSuccessMessage(successMessage);
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

    public void deleteReplacementFile() throws FileReplaceException {
        if (!isFileReplaceOperation()) {
            throw new FileReplaceException("Only use this for File Replace Operations");
        }

        if (!fileReplacePageHelper.wasPhase1Successful()) {
            throw new FileReplaceException("Should only be called if Phase 1 was successful");
        }

        fileReplacePageHelper.resetReplaceFileHelper();

        String successMessage = getBundleString("file.deleted.replacement.success");
        logger.fine(successMessage);
        JsfHelper.addFlashMessage(successMessage);

    }

    private Boolean hasValidTermsOfAccess = null;

    public Boolean isHasValidTermsOfAccess() {
        //cache in page to limit processing
        if (hasValidTermsOfAccess != null) {
            return hasValidTermsOfAccess;
        } else {
            if (!isHasRestrictedFiles()) {
                hasValidTermsOfAccess = true;
                return hasValidTermsOfAccess;
            } else {
                hasValidTermsOfAccess = TermsOfUseAndAccessValidator.isTOUAValid(workingVersion.getTermsOfUseAndAccess(), null);
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

    public Boolean isHasRestrictedFiles() {
        //cache in page to limit processing
        if (hasRestrictedFiles != null) {
            return hasRestrictedFiles;
        } else {
            hasRestrictedFiles = workingVersion.isHasRestrictedFile();
            return hasRestrictedFiles;
        }
    }

    /**
     *
     * @param msgName - from the bundle e.g. "file.deleted.success"
     * @return
     */
    private String getBundleString(String msgName) {

        return BundleUtil.getStringFromBundle(msgName);
    }

    // This deleteFilesCompleted method is used in editFilesFragment.xhtml
    public void deleteFilesCompleted() {

    }

    public void deleteFiles() {
        deleteFiles(this.selectedFiles);
    }

    public void deleteDuplicateFiles() {
        List<FileMetadata> filesForDelete = new ArrayList();
        for (DataFile df : newFiles) {
            if (df.isMarkedAsDuplicate()) {
                filesForDelete.add(df.getFileMetadata());
            }
        }
        deleteFiles(filesForDelete);
    }

    private void deleteFiles(List<FileMetadata> filesForDelete) {
        logger.fine("entering bulk file delete (EditDataFilesPage)");
        if (isFileReplaceOperation()) {
            try {
                deleteReplacementFile();
            } catch (FileReplaceException ex) {
                Logger.getLogger(EditDatafilesPage.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }

        /*
        If selected files are empty it means that we are dealing 
        with a duplicate files delete situation
        so we are adding the marked as dup files as selected
        and moving on accordingly.
         */
        String fileNames = null;
        for (FileMetadata fmd : filesForDelete) {
            // collect the names of the files, 
            // to show in the success message:
            if (fileNames == null) {
                fileNames = fmd.getLabel();
            } else {
                fileNames = fileNames.concat(", " + fmd.getLabel());
            }
        }

        for (FileMetadata markedForDelete : filesForDelete) {
            logger.fine("delete requested on file " + markedForDelete.getLabel());
            logger.fine("file metadata id: " + markedForDelete.getId());
            logger.fine("datafile id: " + markedForDelete.getDataFile().getId());
            logger.fine("page is in edit mode " + mode.name());

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

                // ToDo - FileMetadataUtil.removeFileMetadataFromList should handle these two
                // removes so they could be put after this if clause and the else clause could
                // be removed.
                dataset.getOrCreateEditVersion().getFileMetadatas().remove(markedForDelete);
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
                FileMetadataUtil.removeFileMetadataFromList(fileMetadatas, markedForDelete);
                // 2. delete the filemetadata from the version: 
                FileMetadataUtil.removeFileMetadataFromList(dataset.getOrCreateEditVersion().getFileMetadatas(), markedForDelete);
            }

            if (markedForDelete.getDataFile().getId() == null) {
                logger.fine("this is a brand new file.");
                // the file was just added during this step, so in addition to 
                // removing it from the fileMetadatas lists (above), we also remove it from
                // the newFiles list and the dataset's files, so it never gets saved.

                FileMetadataUtil.removeDataFileFromList(dataset.getFiles(), markedForDelete.getDataFile());
                FileMetadataUtil.removeDataFileFromList(newFiles, markedForDelete.getDataFile());
                FileUtil.deleteTempFile(markedForDelete.getDataFile(), dataset, ingestService);
                // Also remove checksum from the list of newly uploaded checksums (perhaps odd
                // to delete and then try uploading the same file again, but it seems like it
                // should be allowed/the checksum list is part of the state to clean-up
                if (checksumMapNew != null && markedForDelete.getDataFile().getChecksumValue() != null) {
                    checksumMapNew.remove(markedForDelete.getDataFile().getChecksumValue());
                }

            }
        }

        if (fileNames != null) {
            String successMessage;
            if (mode == FileEditMode.UPLOAD) {
                if (fileNames.contains(", ")) {
                    successMessage = getBundleString("file.deleted.upload.success.multiple");
                } else {
                    successMessage = getBundleString("file.deleted.upload.success.single");
                }
            } else {
                successMessage = getBundleString("file.deleted.success");
                successMessage = successMessage.replace("{0}", fileNames);
            }
            logger.fine(successMessage);
            JsfHelper.addFlashMessage(successMessage);
        }
    }

    /**
     * Save for File Replace operations
     *
     * @return
     * @throws FileReplaceException
     */
    public String saveReplacementFile() throws FileReplaceException {

        // Ahh, make sure it's a file replace operation
        //
        if (!isFileReplaceOperation()) {
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
        if (fileReplacePageHelper == null) {
            throw new NullPointerException("fileReplacePageHelper cannot be null");
        }

        // Make sure phase 1 ran -- button shouldn't be visible if it did not
        //
        if (!fileReplacePageHelper.wasPhase1Successful()) {
            throw new FileReplaceException("Save should only be called when a replacement file has been chosen.  (Phase 1 has to have completed)");

        }

        // Run save!!
        //
        if (fileReplacePageHelper.runSaveReplacementFile_Phase2()) {
            JsfHelper.addSuccessMessage(getBundleString("file.message.replaceSuccess"));
            // It worked!!!  Go to page of new file!!
            if (Referrer.FILE == referrer) {
                return returnToFileLandingPageAfterReplace(fileReplacePageHelper.getFirstNewlyAddedFile());
            } else {
                return returnToDraftVersion();
            }
        } else {
            // Uh oh.
            String errMsg = fileReplacePageHelper.getErrorMessages();

            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, getBundleString("dataset.save.fail"), errMsg));
            logger.severe("Dataset save failed for replace operation: " + errMsg);
            return null;
        }

    }

    public String save() {

        Collection<String> duplicates = IngestUtil.findDuplicateFilenames(workingVersion, newFiles);
        if (!duplicates.isEmpty()) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.message.filesFailure"), BundleUtil.getStringFromBundle("dataset.message.editMetadata.duplicateFilenames", new ArrayList<>(duplicates)));
            return null;
        }
        if (!saveEnabled) {
            return "";
        }
        //Mirroring the checks for DcmUpload, make sure that the db version of the dataset is not locked. 
        //Also checking local version to save time - if data.isLocked() is true, the UpdateDatasetVersionCommand below would fail
        if (dataset.getId() != null) {
            Dataset lockTest = datasetService.find(dataset.getId());
            if (dataset.isLockedFor(DatasetLock.Reason.EditInProgress) || lockTest.isLockedFor(DatasetLock.Reason.EditInProgress)) {
                logger.log(Level.INFO, "Couldn''t save dataset: {0}", "It is locked."
                        + "");
                JH.addMessage(FacesMessage.SEVERITY_FATAL, getBundleString("dataset.locked.editInProgress.message"), BundleUtil.getStringFromBundle("dataset.locked.editInProgress.message.details", Arrays.asList(BrandingUtil.getSupportTeamName(null))));
                return null;
            }
        }

        if (isFileReplaceOperation()) {
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
                        logger.log(Level.INFO, BundleUtil.getStringFromBundle("file.api.alreadyHasPackageFile")
                                + "");
                        populateDatasetUpdateFailureMessage();
                        return null;
                    }
                }
            }

            // Try to save the NEW files permanently: 
            List<DataFile> filesAdded = ingestService.saveAndAddFilesToDataset(workingVersion, newFiles, null, true); 
            
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

        if (systemConfig.isProvCollectionEnabled()) {
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
        }
        logger.fine("issuing the dataset update command");
        // We are creating a new draft version or updating an existing draft;
        // We'll use an Update command for this:
        if (workingVersion.getId() != null) {
            for (int i = 0; i < workingVersion.getFileMetadatas().size(); i++) {
                for (FileMetadata fileMetadata : fileMetadatas) {
                    if (fileMetadata.getDataFile().getStorageIdentifier() != null) {
                        if (fileMetadata.getDataFile().getStorageIdentifier().equals(workingVersion.getFileMetadatas().get(i).getDataFile().getStorageIdentifier())) {
                            workingVersion.getFileMetadatas().set(i, fileMetadata);
                        }
                    }
                }
            }
        }
        // Moves DataFile updates from popupFragment to page for saving
        // This does not seem to collide with the tags updating below
        if (systemConfig.isProvCollectionEnabled() && provJsonChanges) {
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

        Map<Long, String> deleteStorageLocations = null;

        if (!filesToBeDeleted.isEmpty()) {
            deleteStorageLocations = datafileService.getPhysicalFilesToDelete(filesToBeDeleted);
        }

        Command<Dataset> cmd;
        try {
            cmd = new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest(), filesToBeDeleted, clone);
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
            // FacesContext.getCurrentInstance().addMessage(null, new
            // FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " +
            // ex.toString()));
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
        saveEnabled = false;

        if (newFiles.size() > 0) {
            logger.fine("clearing newfiles list.");
            newFiles.clear();
            /*
             - We decided not to bother obtaining persistent ids for new files 
             as they are uploaded and created. The identifiers will be assigned 
             later, when the version is published. 
             */
        }

        workingVersion = dataset.getOrCreateEditVersion();
        logger.fine("working version id: " + workingVersion.getId());

        if (FileEditMode.EDIT == mode && Referrer.FILE == referrer) {
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

        if (FileEditMode.EDIT == mode && Referrer.FILE == referrer && fileMetadatas.size() > 0) {
            // If this was a "single file edit", i.e. an edit request sent from 
            // the individual File Landing page, we want to redirect back to 
            // the landing page. BUT ONLY if the file still exists - i.e., if 
            // the user hasn't just deleted it!
            versionString = "DRAFT";
            return returnToFileLandingPage();
        }

        logger.fine("Redirecting to the dataset page, from the edit/upload page.");
        return returnToDraftVersion();
    }

    public boolean canPublishDataset() {
        return permissionsWrapper.canIssuePublishDatasetCommand(dataset);
    }

    private void populateDatasetUpdateFailureMessage() {

        JH.addMessage(FacesMessage.SEVERITY_FATAL, getBundleString("dataset.message.filesFailure"));
    }

    private String returnToDraftVersion() {
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId().asString() + "&version=DRAFT&faces-redirect=true";
    }

    private String returnToDatasetOnly() {
        dataset = datasetService.find(dataset.getId());
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId().asString() + "&faces-redirect=true";
    }

    private String returnToFileLandingPage() {
        Long fileId = fileMetadatas.get(0).getDataFile().getId();
        if (versionString != null && versionString.equals("DRAFT")) {
            return "/file.xhtml?fileId=" + fileId + "&version=DRAFT&faces-redirect=true";
        }
        return "/file.xhtml?fileId=" + fileId + "&faces-redirect=true";

    }

    private String returnToFileLandingPageAfterReplace(DataFile newFile) {

        if (newFile == null) {
            throw new NullPointerException("newFile cannot be null!");
        }
        //Long datasetVersionId = newFile.getOwner().getLatestVersion().getId();
        return "/file.xhtml?fileId=" + newFile.getId() + "&version=DRAFT&faces-redirect=true";
    }

    public String cancel() {
        uploadInProgress.setValue(false);
        //Files that have been finished and are now in the lower list on the page
        for (DataFile newFile : newFiles) {
            FileUtil.deleteTempFile(newFile, dataset, ingestService);
        }

        //Files in the upload process but not yet finished
        for (DataFile newFile : uploadedFiles) {
            FileUtil.deleteTempFile(newFile, dataset, ingestService);
        }

        if (Referrer.FILE == referrer) {
            return returnToFileLandingPage();
        }
        if (workingVersion.getId() != null) {
            return returnToDraftVersion();
        }
        return returnToDatasetOnly();
    }

    /**
     * Is this page in File Replace mode
     *
     * @return
     */
    public boolean isFileReplaceOperation() {
        return (mode == FileEditMode.REPLACE) && (fileReplacePageHelper != null);
    }

    public boolean allowMultipleFileUpload() {

        return !isFileReplaceOperation();
    }

    public boolean showFileUploadFragment() {
        return mode == FileEditMode.UPLOAD || mode == FileEditMode.CREATE || mode == FileEditMode.REPLACE;
    }

    public boolean showFileUploadComponent() {
        if (mode == FileEditMode.UPLOAD || mode == FileEditMode.CREATE) {
            return true;
        }

        if (isFileReplaceOperation()) {
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
    private InputStream getDropBoxInputStream(String fileLink) {
        if (fileLink == null) {
            return null;
        }

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(fileLink);
            
            // Use the non-deprecated execute method with ClassicHttpResponse
            ClassicHttpResponse response = httpClient.executeOpen(null, httpGet, null);
            
            int status = response.getCode();
            
            if (status == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // Return the content as a stream directly
                    return entity.getContent();
                }
            }
            
            logger.log(Level.WARNING, "Failed to get DropBox InputStream for file: {0}. Status code: {1}", new Object[]{fileLink, status});
            response.close();
            return null;
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to access DropBox url: {0}!", fileLink);
            return null;
        }
    } // end: getDropBoxInputStream

    /**
     * Using information from the DropBox choose, ingest the chosen files
     * https://www.dropbox.com/developers/dropins/chooser/js
     *
     * @param event
     */
    public void handleDropBoxUpload(ActionEvent event) {
        if (uploadInProgress.isFalse()) {
            uploadInProgress.setValue(true);
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

            // -----------------------------------------------------------
            // Download the file
            // -----------------------------------------------------------
            InputStream dropBoxStream = this.getDropBoxInputStream(fileLink);
            if (dropBoxStream == null) {
                logger.severe("Could not retrieve dropbox input stream for: " + fileLink);
                continue;  // Error skip this file
            }

            // -----------------------------------------------------------
            // Is this a FileReplaceOperation?  If so, then diverge!
            // -----------------------------------------------------------
            if (this.isFileReplaceOperation()) {
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
                //CreateDataFileResult createDataFilesResult = FileUtil.createDataFiles(workingVersion, dropBoxStream, fileName, "application/octet-stream", null, null, systemConfig);
                Command<CreateDataFileResult> cmd = new CreateNewDataFilesCommand(dvRequestService.getDataverseRequest(), workingVersion, dropBoxStream, fileName, "application/octet-stream", null, uploadSessionQuota, null);
                CreateDataFileResult createDataFilesResult = commandEngine.submit(cmd);
                datafiles = createDataFilesResult.getDataFiles();
                Optional.ofNullable(editDataFilesPageHelper.getHtmlErrorMessage(createDataFilesResult)).ifPresent(errorMessage -> errorMessages.add(errorMessage));

            } catch (CommandException ex) {
                this.logger.log(Level.SEVERE, "Error during ingest of DropBox file {0} from link {1}", new Object[]{fileName, fileLink});
                continue;
            } /*catch (FileExceedsMaxSizeException ex){
                this.logger.log(Level.SEVERE, "Error during ingest of DropBox file {0} from link {1}: {2}", new Object[]{fileName, fileLink, ex.getMessage()});
                continue;
            }*/ finally {
                // -----------------------------------------------------------
                // close the  dropBoxStream
                // -----------------------------------------------------------
                try {
                    dropBoxStream.close();
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Failed to close the dropBoxStream for file: {0}", fileLink);
                }
            }

            if (datafiles == null) {
                this.logger.log(Level.SEVERE, "Failed to create DataFile for DropBox file {0} from link {1}", new Object[]{fileName, fileLink});
                continue;
            } else {
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
            if (uploadInProgress.isFalse()) {
                logger.warning("Upload in progress cancelled");
                for (DataFile newFile : datafiles) {
                    FileUtil.deleteTempFile(newFile, dataset, ingestService);
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

        uploadInProgress.setValue(true);
    }

    private Boolean hasRsyncScript = false;

    public Boolean isHasRsyncScript() {
        return hasRsyncScript;
    }

    public void setHasRsyncScript(Boolean hasRsyncScript) {
        this.hasRsyncScript = hasRsyncScript;
    }

    private void setUpRsync() {
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
                FacesContext.getCurrentInstance().addMessage(uploadComponentId,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Problem getting rsync script (EJBException): " + EjbUtil.ejbExceptionToString(ex),
                                "Problem getting rsync script (EJBException):"));
            } catch (RuntimeException ex) {
                logger.warning("Problem getting rsync script (RuntimeException): " + ex.getLocalizedMessage());
                FacesContext.getCurrentInstance().addMessage(uploadComponentId,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Problem getting rsync script (RuntimeException): " + ex.getMessage(),
                                "Problem getting rsync script (RuntimeException):"));
            } catch (CommandException cex) {
                logger.warning("Problem getting rsync script (Command Exception): " + cex.getLocalizedMessage());
                FacesContext.getCurrentInstance().addMessage(uploadComponentId,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Problem getting rsync script (Command Exception): " + cex.getMessage(),
                                "Problem getting rsync script (Command Exception):"));
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
        DatasetLock lock = datasetService.addDatasetLock(dataset.getId(), DatasetLock.Reason.DcmUpload, session.getUser() != null ? ((AuthenticatedUser) session.getUser()).getId() : null, lockInfoMessage);
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

    @Deprecated
    public void requestDirectUploadUrl() {

        S3AccessIO<?> s3io = FileUtil.getS3AccessForDirectUpload(dataset);
        if (s3io == null) {
            FacesContext.getCurrentInstance().addMessage(uploadComponentId, new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.file.uploadWarning"), "Direct upload not supported for this dataset"));
        }
        String url = null;
        String storageIdentifier = null;
        try {
            url = s3io.generateTemporaryS3UploadUrl();
            storageIdentifier = FileUtil.getStorageIdentifierFromLocation(s3io.getStorageLocation());
        } catch (IOException io) {
            logger.warning(io.getMessage());
            FacesContext.getCurrentInstance().addMessage(uploadComponentId, new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.file.uploadWarning"), "Issue in connecting to S3 store for direct upload"));
        }

        PrimeFaces.current().executeScript("uploadFileDirectly('" + url + "','" + storageIdentifier + "')");
    }

    public void requestDirectUploadUrls() {

        Map<String, String> paramMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        String sizeString = paramMap.get("fileSize");
        long fileSize = Long.parseLong(sizeString);

        S3AccessIO<?> s3io = FileUtil.getS3AccessForDirectUpload(dataset);
        if (s3io == null) {
            FacesContext.getCurrentInstance().addMessage(uploadComponentId,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            BundleUtil.getStringFromBundle("dataset.file.uploadWarning"),
                            "Direct upload not supported for this dataset"));
        }
        JsonObjectBuilder urls = null;
        String storageIdentifier = null;
        try {
            storageIdentifier = FileUtil.getStorageIdentifierFromLocation(s3io.getStorageLocation());
            urls = s3io.generateTemporaryS3UploadUrls(dataset.getGlobalId().asString(), storageIdentifier, fileSize);

        } catch (IOException io) {
            logger.warning(io.getMessage());
            FacesContext.getCurrentInstance().addMessage(uploadComponentId,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            BundleUtil.getStringFromBundle("dataset.file.uploadWarning"),
                            "Issue in connecting to S3 store for direct upload"));
        }

        PrimeFaces.current().executeScript(
                "uploadFileDirectly('" + urls.build().toString() + "','" + storageIdentifier + "','" + fileSize + "')");
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

        if (uploadInProgress.isTrue()) {
            uploadedFiles.clear();
            uploadInProgress.setValue(false);
        }

        // refresh the warning message below the upload component, if exists:
        if (uploadComponentId != null) {
            if(!errorMessages.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(uploadComponentId, new FacesMessage(FacesMessage.SEVERITY_ERROR,  BundleUtil.getStringFromBundle("dataset.file.uploadFailure"), editDataFilesPageHelper.consolidateHtmlErrorMessages(errorMessages)));
            }

            if (uploadWarningMessage != null) {
                if (existingFilesWithDupeContent != null || newlyUploadedFilesWithDupeContent != null) {
                    setWarningMessageForAlreadyExistsPopUp(uploadWarningMessage);
                    setHeaderForAlreadyExistsPopUp();
                    setLabelForDeleteFilesPopup();
                    PrimeFaces.current().ajax().update("datasetForm:fileAlreadyExistsPopup");
                    PrimeFaces.current().executeScript("PF('fileAlreadyExistsPopup').show();");
                } else {
                    //adding back warnings in non-replace situations
                    FacesContext.getCurrentInstance().addMessage(uploadComponentId, new FacesMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.file.uploadWarning"), uploadWarningMessage));
                }

            } else if (uploadSuccessMessage != null) {
                FacesContext.getCurrentInstance().addMessage(uploadComponentId, new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.file.uploadWorked"), uploadSuccessMessage));
            }
        }

        if (isFileReplaceOperation() && fileReplacePageHelper.wasPhase1Successful() && fileReplacePageHelper.hasContentTypeWarning()) {
            //RequestContext context = RequestContext.getCurrentInstance();
            //RequestContext.getCurrentInstance().update("datasetForm:fileTypeDifferentPopup");
            PrimeFaces.current().ajax().update("datasetForm:fileTypeDifferentPopup");
            //context.execute("PF('fileTypeDifferentPopup').show();");
            PrimeFaces.current().executeScript("PF('fileTypeDifferentPopup').show();");
        }

        if (isFileReplaceOperation() && fileReplacePageHelper.getAddReplaceFileHelper().isDuplicateFileErrorFound()) {
            FacesContext.getCurrentInstance().addMessage(uploadComponentId, new FacesMessage(FacesMessage.SEVERITY_ERROR, fileReplacePageHelper.getAddReplaceFileHelper().getDuplicateFileErrorString(), fileReplacePageHelper.getAddReplaceFileHelper().getDuplicateFileErrorString()));
        }

        if (isFileReplaceOperation() && !fileReplacePageHelper.getAddReplaceFileHelper().isDuplicateFileErrorFound() && fileReplacePageHelper.getAddReplaceFileHelper().isDuplicateFileWarningFound()) {
            setWarningMessageForAlreadyExistsPopUp(fileReplacePageHelper.getAddReplaceFileHelper().getDuplicateFileWarningString());
            setHeaderForAlreadyExistsPopUp();
            setLabelForDeleteFilesPopup();
            PrimeFaces.current().ajax().update("datasetForm:fileAlreadyExistsPopup");
            PrimeFaces.current().executeScript("PF('fileAlreadyExistsPopup').show();");
        }
        // We clear the following duplicate warning labels, because we want to 
        // only inform the user of the duplicates dropped in the current upload 
        // attempt - for ex., one batch of drag-and-dropped files, or a single 
        // file uploaded through the file chooser. 
        newlyUploadedFilesWithDupeContent = null;
        existingFilesWithDupeContent = null;
        multipleDupesExisting = false;
        multipleDupesNew = false;
        uploadWarningMessage = null;
        uploadSuccessMessage = null;
        errorMessages = new ArrayList<>();
    }

    private String warningMessageForFileTypeDifferentPopUp;

    public String getWarningMessageForFileTypeDifferentPopUp() {
        return warningMessageForFileTypeDifferentPopUp;
    }

    public void setWarningMessageForFileTypeDifferentPopUp(String warningMessageForPopUp) {
        this.warningMessageForFileTypeDifferentPopUp = warningMessageForPopUp;
    }

    private String warningMessageForAlreadyExistsPopUp;

    public String getWarningMessageForAlreadyExistsPopUp() {
        return warningMessageForAlreadyExistsPopUp;
    }

    public void setWarningMessageForAlreadyExistsPopUp(String warningMessageForAlreadyExistsPopUp) {
        this.warningMessageForAlreadyExistsPopUp = warningMessageForAlreadyExistsPopUp;
    }

    private String headerForAlreadyExistsPopUp;

    public String getHeaderForAlreadyExistsPopUp() {
        return headerForAlreadyExistsPopUp;
    }

    public void setHeaderForAlreadyExistsPopUp(String headerForAlreadyExistsPopUp) {
        this.headerForAlreadyExistsPopUp = headerForAlreadyExistsPopUp;
    }

    private String labelForDeleteFilesPopup;

    public String getLabelForDeleteFilesPopup() {
        return labelForDeleteFilesPopup;
    }

    public void setLabelForDeleteFilesPopup(String labelForDeleteFilesPopup) {
        this.labelForDeleteFilesPopup = labelForDeleteFilesPopup;
    }

    public void setLabelForDeleteFilesPopup() {
        this.labelForDeleteFilesPopup = ((multipleDupesExisting || multipleDupesNew) ? BundleUtil.getStringFromBundle("file.delete.duplicate.multiple")
                : BundleUtil.getStringFromBundle("file.delete.duplicate.single"));
    }

    //((multipleDupesExisting|| multipleDupesNew) ? BundleUtil.getStringFromBundle("file.addreplace.already_exists.header.multiple"):  BundleUtil.getStringFromBundle("file.addreplace.already_exists.header"));
    public void setHeaderForAlreadyExistsPopUp() {

        this.headerForAlreadyExistsPopUp = ((multipleDupesExisting || multipleDupesNew) ? BundleUtil.getStringFromBundle("file.addreplace.already_exists.header.multiple") : BundleUtil.getStringFromBundle("file.addreplace.already_exists.header"));
    }

    private void handleReplaceFileUpload(FacesEvent event, InputStream inputStream,
            String fileName,
            String contentType,
            FileUploadEvent nativeUploadEvent,
            ActionEvent dropboxUploadEvent
    ) {

        fileReplacePageHelper.resetReplaceFileHelper();

        saveEnabled = false;

        uploadComponentId = event.getComponent().getClientId();

        if (fileReplacePageHelper.handleNativeFileUpload(inputStream, null,
                fileName,
                contentType,
                null,
                null
        )) {
            saveEnabled = true;

            /**
             * If the file content type changed, let the user know
             */
            if (fileReplacePageHelper.hasContentTypeWarning()) {
                //Add warning to popup instead of page for Content Type Difference
                setWarningMessageForFileTypeDifferentPopUp(fileReplacePageHelper.getContentTypeWarning());
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

            if (nativeUploadEvent != null) {
//                uploadWarningMessage += " nativeUploadEvent ";

            }
            if (dropboxUploadEvent != null) {
//                uploadWarningMessage += " dropboxUploadEvent ";
            }
            //FacesContext.getCurrentInstance().addMessage(
            //    uploadComponentId,                         
            //    new FacesMessage(FacesMessage.SEVERITY_ERROR, "upload failure", uploadWarningMessage));
        }
    }

    private void handleReplaceFileUpload(String fullStorageLocation,
            String fileName,
            String contentType,
            String checkSumValue,
            ChecksumType checkSumType) {

        fileReplacePageHelper.resetReplaceFileHelper();
        saveEnabled = false;
        String storageIdentifier = DataAccess.getStorageIdFromLocation(fullStorageLocation);
        if (fileReplacePageHelper.handleNativeFileUpload(null, storageIdentifier, fileName, contentType, checkSumValue, checkSumType)) {
            saveEnabled = true;

            /**
             * If the file content type changed, let the user know
             */
            if (fileReplacePageHelper.hasContentTypeWarning()) {
                //Add warning to popup instead of page for Content Type Difference
                setWarningMessageForFileTypeDifferentPopUp(fileReplacePageHelper.getContentTypeWarning());
            }
        } else {
            uploadWarningMessage = fileReplacePageHelper.getErrorMessages();
        }
    }

    private String uploadWarningMessage = null;
    private List<String> errorMessages = new ArrayList<>();
    private String uploadSuccessMessage = null;
    private String uploadComponentId = null;

    /**
     * Handle native file replace
     *
     * @param event
     * @throws java.io.IOException
     */
    public void handleFileUpload(FileUploadEvent event) throws IOException {

        if (uploadInProgress.isFalse()) {
            uploadInProgress.setValue(true);
        }

        //resetting marked as dup in case there are multiple uploads 
        //we only want to delete as dupes those that we uploaded in this 
        //session
        newFiles.forEach((df) -> {
            df.setMarkedAsDuplicate(false);
        });

        if (event == null) {
            throw new NullPointerException("event cannot be null");
        }

        UploadedFile uFile = event.getFile();
        if (uFile == null) {
            throw new NullPointerException("uFile cannot be null");
        }

        /**
         * For File Replace, take a different code path
         */
        if (isFileReplaceOperation()) {

            handleReplaceFileUpload(event, uFile.getInputStream(),
                    uFile.getFileName(),
                    uFile.getContentType(),
                    event,
                    null);
            if (fileReplacePageHelper.wasPhase1Successful() && fileReplacePageHelper.hasContentTypeWarning()) {
                //RequestContext context = RequestContext.getCurrentInstance();
                //RequestContext.getCurrentInstance().update("datasetForm:fileTypeDifferentPopup");
                //context.execute("PF('fileTypeDifferentPopup').show();");
                PrimeFaces.current().ajax().update("datasetForm:fileTypeDifferentPopup");
                PrimeFaces.current().executeScript("PF('fileTypeDifferentPopup').show();");
            }
            /*
            

            if(fileReplacePageHelper.){
                    //RequestContext context = RequestContext.getCurrentInstance();
                    //RequestContext.getCurrentInstance().update("datasetForm:fileTypeDifferentPopup");
                    //context.execute("PF('fileTypeDifferentPopup').show();");
                    PrimeFaces.current().ajax().update("datasetForm:fileTypeDifferentPopup");
                    PrimeFaces.current().executeScript("PF('fileTypeDifferentPopup').show();");
            }
             */
            return;

        }

        List<DataFile> dFileList = null;

        try {
            // Note: A single uploaded file may produce multiple datafiles - 
            // for example, multiple files can be extracted from an uncompressed
            // zip file.
            ///CreateDataFileResult createDataFilesResult = FileUtil.createDataFiles(workingVersion, uFile.getInputStream(), uFile.getFileName(), uFile.getContentType(), null, null, systemConfig);
            
            Command<CreateDataFileResult> cmd;
            if (mode == FileEditMode.CREATE) {
                // This is a file upload in the context of creating a brand new
                // dataset that does not yet exist in the database. We must 
                // use the version of the Create New Files constructor that takes
                // the parent Dataverse as the extra argument:
                cmd = new CreateNewDataFilesCommand(dvRequestService.getDataverseRequest(), workingVersion, uFile.getInputStream(), uFile.getFileName(), uFile.getContentType(), null, uploadSessionQuota, null, null, null, workingVersion.getDataset().getOwner());
            } else {
                cmd = new CreateNewDataFilesCommand(dvRequestService.getDataverseRequest(), workingVersion, uFile.getInputStream(), uFile.getFileName(), uFile.getContentType(), null, uploadSessionQuota, null);
            }
            CreateDataFileResult createDataFilesResult = commandEngine.submit(cmd);

        
            dFileList = createDataFilesResult.getDataFiles();
            String createDataFilesError = editDataFilesPageHelper.getHtmlErrorMessage(createDataFilesResult);
            if(createDataFilesError != null) {
                errorMessages.add(createDataFilesError);
                uploadComponentId = event.getComponent().getClientId();
            }

        } catch (IOException ioex) {
            // shouldn't we try and communicate to the user what happened?
            logger.warning("Failed to process and/or save the file " + uFile.getFileName() + "; " + ioex.getMessage());
            return;
        } catch (CommandException cex) {
            // shouldn't we try and communicate to the user what happened?
            errorMessages.add(cex.getMessage());
            uploadComponentId = event.getComponent().getClientId();
            return;
        } finally {
            try {
                uFile.delete();
            } catch (IOException ioex) {
                logger.warning("Failed to delete temp file uploaded via PrimeFaces " + uFile.getFileName());
            }
        }
        /*catch (FileExceedsMaxSizeException ex) {
            logger.warning("Failed to process and/or save the file " + uFile.getFileName() + "; " + ex.getMessage());
            return;
        }*/

        // -----------------------------------------------------------
        // These raw datafiles are then post-processed, in order to drop any files 
        // already in the dataset/already uploaded, and to correct duplicate file names, etc. 
        // -----------------------------------------------------------
        String warningMessage = processUploadedFileList(dFileList);

        if (warningMessage != null) {
            uploadWarningMessage = warningMessage;
            FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(), new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.file.uploadWarning"), warningMessage));
            // save the component id of the p:upload widget, so that we could 
            // send an info message there, from elsewhere in the code:
            uploadComponentId = event.getComponent().getClientId();
        }

        if (uploadInProgress.isFalse()) {
            logger.warning("Upload in progress cancelled");
            for (DataFile newFile : dFileList) {
                FileUtil.deleteTempFile(newFile, dataset, ingestService);
            }
        }
    }

    /**
     * External, aka "Direct" Upload. 
     * The file(s) have been uploaded to physical storage (such as S3) directly,
     * this call is to create and add the DataFiles to the Dataset on the Dataverse 
     * side. The method does NOT finalize saving the datafiles in the database -
     * that will happen when the user clicks 'Save', similar to how the "normal"
     * uploads are handled. 
     *
     * @param event
     */
    public void handleExternalUpload() {
        Map<String, String> paramMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        this.uploadComponentId = paramMap.get("uploadComponentId");
        String fullStorageIdentifier = paramMap.get("fullStorageIdentifier");
        String fileName = paramMap.get("fileName");
        String contentType = paramMap.get("contentType");
        String checksumTypeString = paramMap.get("checksumType");
        String checksumValue = paramMap.get("checksumValue");
        ChecksumType checksumType = null;
        if (!checksumTypeString.isBlank()) {
            checksumType = ChecksumType.fromString(checksumTypeString);
        }

        //Should only be one colon with curent design
        int lastColon = fullStorageIdentifier.lastIndexOf(':');
        String storageLocation = fullStorageIdentifier.substring(0,lastColon) + "/" + dataset.getAuthorityForFileStorage() + "/" + dataset.getIdentifierForFileStorage() + "/" + fullStorageIdentifier.substring(lastColon+1);
        storageLocation = DataAccess.expandStorageIdentifierIfNeeded(storageLocation);

        if (uploadInProgress.isFalse()) {
            uploadInProgress.setValue(true);
        }
        logger.fine("handleExternalUpload");

        StorageIO<DvObject> sio;
        String localWarningMessage = null;
        try {
            sio = DataAccess.getDirectStorageIO(storageLocation);

            //Populate metadata
            sio.open(DataAccessOption.READ_ACCESS);
            //get file size
            long fileSize = sio.getSize();

            if (StringUtils.isEmpty(contentType)) {
                contentType = FileUtil.MIME_TYPE_UNDETERMINED_DEFAULT;
            }

            /* ----------------------------
                Check file size
                - Max size NOT specified in db: default is unlimited
                - Max size specified in db: check too make sure file is within limits
            // ---------------------------- */
            /**
             * @todo: this file size limit check is now redundant here, since the new
             * CreateNewFilesCommand is going to perform it (and the quota 
             * checks too, if enabled
             */
            if ((!this.isUnlimitedUploadFileSize()) && (fileSize > this.getMaxFileUploadSizeInBytes())) {
                String warningMessage = "Uploaded file \"" + fileName + "\" exceeded the limit of " + fileSize + " bytes and was not uploaded.";
                sio.delete();
                localWarningMessage = warningMessage;
            } else {
                // -----------------------------------------------------------
                // Is this a FileReplaceOperation?  If so, then diverge!
                // -----------------------------------------------------------
                if (this.isFileReplaceOperation()) {
                    this.handleReplaceFileUpload(storageLocation, fileName, contentType, checksumValue, checksumType);
                    if (fileReplacePageHelper.getNewFileMetadatasBeforeSave() != null) {
                        this.setFileMetadataSelectedForTagsPopup(fileReplacePageHelper.getNewFileMetadatasBeforeSave().get(0));
                    }
                    return;
                }
                // -----------------------------------------------------------
                List<DataFile> datafiles = new ArrayList<>();

                // -----------------------------------------------------------
                // Execute the CreateNewDataFiles command:
                // -----------------------------------------------------------
                
                Dataverse parent = null; 
                
                if (mode == FileEditMode.CREATE) {
                    // This is a file upload in the context of creating a brand new
                    // dataset that does not yet exist in the database. We must 
                    // pass the parent Dataverse to the CreateNewFiles command
                    // constructor. The RequiredPermission on the command in this 
                    // scenario = Permission.AddDataset on the parent dataverse.
                    parent = workingVersion.getDataset().getOwner();
                }
                
                try {
  
                    Command<CreateDataFileResult> cmd = new CreateNewDataFilesCommand(dvRequestService.getDataverseRequest(), workingVersion, null, fileName, contentType, fullStorageIdentifier, uploadSessionQuota, checksumValue, checksumType, fileSize, parent);
                    CreateDataFileResult createDataFilesResult = commandEngine.submit(cmd);
                    datafiles = createDataFilesResult.getDataFiles();
                    Optional.ofNullable(editDataFilesPageHelper.getHtmlErrorMessage(createDataFilesResult)).ifPresent(errorMessage -> errorMessages.add(errorMessage));
                } catch (CommandException ex) {
                    logger.log(Level.SEVERE, "Error during ingest of file {0}", new Object[]{fileName});
                }

                if (datafiles == null) {
                    logger.log(Level.SEVERE, "Failed to create DataFile for file {0}", new Object[]{fileName});
                } else {
                    // -----------------------------------------------------------
                    // Check if there are duplicate files or ingest warnings
                    // -----------------------------------------------------------
                    uploadWarningMessage = processUploadedFileList(datafiles);
                }
                if (uploadInProgress.isFalse()) {
                    logger.warning("Upload in progress cancelled");
                    for (DataFile newFile : datafiles) {
                        FileUtil.deleteTempFile(newFile, dataset, ingestService);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create DataFile for file {0}: {1}", new Object[]{fileName, e.getMessage()});
        }
        if (localWarningMessage != null) {
            if (uploadWarningMessage == null) {
                uploadWarningMessage = localWarningMessage;
            } else {
                uploadWarningMessage = localWarningMessage.concat("; " + uploadWarningMessage);
            }
        }
    }

    /**
     * After uploading via the site or Dropbox, check the list of DataFile
     * objects
     *
     * @param dFileList
     */
    private String existingFilesWithDupeContent = null;
    private String uploadedFilesWithDupeContentToExisting = null;
    private String uploadedFilesWithDupeContentToNewlyUploaded = null;
    private String newlyUploadedFilesWithDupeContent = null;

    private boolean multipleDupesExisting = false;
    private boolean multipleDupesNew = false;

    public String getExistingFilesWithDupeContent() {
        return existingFilesWithDupeContent;
    }

    public void setExistingFilesWithDupeContent(String existingFilesWithDupeContent) {
        this.existingFilesWithDupeContent = existingFilesWithDupeContent;
    }

    public String getUploadedFilesWithDupeContentToExisting() {
        return uploadedFilesWithDupeContentToExisting;
    }

    public void setUploadedFilesWithDupeContentToExisting(String uploadedFilesWithDupeContentToExisting) {
        this.uploadedFilesWithDupeContentToExisting = uploadedFilesWithDupeContentToExisting;
    }

    public String getUploadedFilesWithDupeContentToNewlyUploaded() {
        return uploadedFilesWithDupeContentToNewlyUploaded;
    }

    public void setUploadedFilesWithDupeContentToNewlyUploaded(String uploadedFilesWithDupeContentToNewlyUploaded) {
        this.uploadedFilesWithDupeContentToNewlyUploaded = uploadedFilesWithDupeContentToNewlyUploaded;
    }

    public String getNewlyUploadedFilesWithDupeContent() {
        return newlyUploadedFilesWithDupeContent;
    }

    public void setNewlyUploadedFilesWithDupeContent(String newlyUploadedFilesWithDupeContent) {
        this.newlyUploadedFilesWithDupeContent = newlyUploadedFilesWithDupeContent;
    }

    public boolean isMultipleDupesExisting() {
        return multipleDupesExisting;
    }

    public void setMultipleDupesExisting(boolean multipleDupesExisting) {
        this.multipleDupesExisting = multipleDupesExisting;
    }

    public boolean isMultipleDupesNew() {
        return multipleDupesNew;
    }

    public void setMultipleDupesNew(boolean multipleDupesNew) {
        this.multipleDupesNew = multipleDupesNew;
    }

    private String processUploadedFileList(List<DataFile> dFileList) {
        if (dFileList == null) {
            return null;
        }

        uploadedInThisProcess = new ArrayList();

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
                DataFile existingFile = fileAlreadyExists.get(dataFile);

                // String alreadyExists = dataFile.getFileMetadata().getLabel() + " at " + existingFile.getDirectoryLabel() != null ? existingFile.getDirectoryLabel() + "/" + existingFile.getDisplayName() : existingFile.getDisplayName();
                String uploadedDuplicateFileName = dataFile.getFileMetadata().getLabel();
                String existingFileName = existingFile.getDisplayName();
                List<String> args = Arrays.asList(existingFileName);
                String inLineMessage = BundleUtil.getStringFromBundle("dataset.file.inline.message", args);

                if (existingFilesWithDupeContent == null) {
                    existingFilesWithDupeContent = existingFileName;
                    uploadedFilesWithDupeContentToExisting = uploadedDuplicateFileName;
                } else {
                    existingFilesWithDupeContent = existingFilesWithDupeContent.concat(", " + existingFileName);
                    uploadedFilesWithDupeContentToExisting = uploadedFilesWithDupeContentToExisting.concat(", " + uploadedDuplicateFileName);
                    multipleDupesExisting = true;
                }
                //now we are marking as duplicate and
                //allowing the user to decide whether to delete
                //   deleteTempFile(dataFile);
                dataFile.setMarkedAsDuplicate(true);
                dataFile.setDuplicateFilename(inLineMessage);

            } else if (isFileAlreadyUploaded(dataFile)) {
                DataFile existingFile = checksumMapNew.get(dataFile.getChecksumValue());
                String alreadyUploadedWithSame = existingFile.getDisplayName();
                String newlyUploadedDupe = dataFile.getFileMetadata().getLabel();
                if (newlyUploadedFilesWithDupeContent == null) {
                    newlyUploadedFilesWithDupeContent = newlyUploadedDupe;
                    uploadedFilesWithDupeContentToNewlyUploaded = alreadyUploadedWithSame;
                } else {
                    newlyUploadedFilesWithDupeContent = newlyUploadedFilesWithDupeContent.concat(", " + newlyUploadedDupe);
                    uploadedFilesWithDupeContentToNewlyUploaded = uploadedFilesWithDupeContentToNewlyUploaded.concat(", " + alreadyUploadedWithSame);
                    multipleDupesNew = true;
                }
                //now we are marking as duplicate and
                //allowing the user to decide whether to delete
                dataFile.setMarkedAsDuplicate(true);
                List<String> args = Arrays.asList(existingFile.getDisplayName());
                String inLineMessage = BundleUtil.getStringFromBundle("dataset.file.inline.message", args);
                dataFile.setDuplicateFilename(inLineMessage);
            } else {
                // OK, this one is not a duplicate, we want it. 
                // But let's check if its filename is a duplicate of another 
                // file already uploaded, or already in the dataset:
                /*
                dataFile.getFileMetadata().setLabel(duplicateFilenameCheck(dataFile.getFileMetadata()));
                if (isTemporaryPreviewAvailable(dataFile.getStorageIdentifier(), dataFile.getContentType())) {
                    dataFile.setPreviewImageAvailable(true);
                }
                uploadedFiles.add(dataFile);
                 */
                // We are NOT adding the fileMetadata to the list that is being used
                // to render the page; we'll do that once we know that all the individual uploads
                // in this batch (as in, a bunch of drag-and-dropped files) have finished. 
                //fileMetadatas.add(dataFile.getFileMetadata());
            }

            dataFile.getFileMetadata().setLabel(duplicateFilenameCheck(dataFile.getFileMetadata()));
            if (isTemporaryPreviewAvailable(dataFile.getStorageIdentifier(), dataFile.getContentType())) {
                dataFile.setPreviewImageAvailable(true);
            }
            uploadedFiles.add(dataFile);
            uploadedInThisProcess.add(dataFile);
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
        if (existingFilesWithDupeContent != null) {
            String duplicateFilesErrorMessage = null;
            List<String> args = Arrays.asList(uploadedFilesWithDupeContentToExisting, existingFilesWithDupeContent);

            if (multipleDupesExisting) {
                duplicateFilesErrorMessage = BundleUtil.getStringFromBundle("dataset.files.exist", args);
            } else {
                duplicateFilesErrorMessage = BundleUtil.getStringFromBundle("dataset.file.exist", args);
            }
            if (warningMessage == null) {
                warningMessage = duplicateFilesErrorMessage;
            } else {
                warningMessage = warningMessage.concat(" " + duplicateFilesErrorMessage);
            }
        }

        if (newlyUploadedFilesWithDupeContent != null) {
            String duplicateFilesErrorMessage = null;
            List<String> args = Arrays.asList(newlyUploadedFilesWithDupeContent, uploadedFilesWithDupeContentToNewlyUploaded);

            if (multipleDupesNew) {
                duplicateFilesErrorMessage = BundleUtil.getStringFromBundle("dataset.files.duplicate", args);
            } else {
                duplicateFilesErrorMessage = BundleUtil.getStringFromBundle("dataset.file.duplicate", args);
            }
            if (warningMessage == null) {
                warningMessage = duplicateFilesErrorMessage;
            } else {
                warningMessage = warningMessage.concat(" " + duplicateFilesErrorMessage);
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

        // Retrieve via MPCONFIG. Has sane default /tmp/dataverse from META-INF/microprofile-config.properties
        String filesRootDirectory = JvmSettings.FILES_DIRECTORY.lookup();

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

    private Map<String, DataFile> checksumMapOld = null; // checksums of the files already in the dataset
    private Map<String, DataFile> checksumMapNew = null; // checksums of the new files already uploaded
    private Map<DataFile, DataFile> fileAlreadyExists = null;

    private void initChecksumMap() {
        checksumMapOld = new HashMap<>();

        Iterator<FileMetadata> fmIt = workingVersion.getFileMetadatas().iterator();

        while (fmIt.hasNext()) {
            FileMetadata fm = fmIt.next();
            if (fm.getDataFile() != null && fm.getDataFile().getId() != null) {
                String chksum = fm.getDataFile().getChecksumValue();
                if (chksum != null) {
                    checksumMapOld.put(chksum, fm.getDataFile());

                }
            }
        }

    }

    private boolean isFileAlreadyInDataset(DataFile dataFile) {
        if (checksumMapOld == null) {
            initChecksumMap();
        }

        if (fileAlreadyExists == null) {
            fileAlreadyExists = new HashMap<>();
        }

        String chksum = dataFile.getChecksumValue();

        if (checksumMapOld.get(chksum) != null) {
            fileAlreadyExists.put(dataFile, checksumMapOld.get(chksum));
        }

        return chksum == null ? false : checksumMapOld.get(chksum) != null;
    }

    private boolean isFileAlreadyUploaded(DataFile dataFile) {

        if (checksumMapNew == null) {
            checksumMapNew = new HashMap<>();
        }

        return FileUtil.isFileAlreadyUploaded(dataFile, checksumMapNew, fileAlreadyExists);

    }

    public boolean isLocked() {
        if (dataset != null) {
            logger.log(Level.FINE, "checking lock status of dataset {0}", dataset.getId());
            if (dataset.isLocked()) {
                // refresh the dataset and version, if the current working
                // version of the dataset is locked:
            }
            Dataset lookedupDataset = datasetService.find(dataset.getId());

            if ((lookedupDataset != null) && lookedupDataset.isLocked()) {
                logger.fine("locked!");
                return true;
            }
        }
        return false;
    }

    private Boolean lockedFromEditsVar;

    public boolean isLockedFromEdits() {
        if (null == lockedFromEditsVar) {
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

    public void setFileMetadataSelected(FileMetadata fm) {
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

    public boolean isDesignatedDatasetThumbnail(FileMetadata fileMetadata) {
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
    public void setFileMetadataSelectedForThumbnailPopup(FileMetadata fm) {
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
        DatasetThumbnail datasetThumbnail = dataset.getDatasetThumbnail(ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
        return datasetThumbnail != null && !datasetThumbnail.isFromDataFile();
    }

    /* 
     * Items for the "Tags (Categories)" popup.
     *
     */
    private FileMetadata fileMetadataSelectedForTagsPopup = null;

    public void setFileMetadataSelectedForTagsPopup(FileMetadata fm) {
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

    public void refreshTagsPopUp(FileMetadata fm) {
                if(!isHasValidTermsOfAccess()){
                PrimeFaces.current().executeScript("PF('blockDatasetForm').show()");
                PrimeFaces.current().executeScript("PF('accessPopup').show()");
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.message.editTerms.label"), BundleUtil.getStringFromBundle("dataset.message.editTerms.message"));
                return; 
        }
        setFileMetadataSelectedForTagsPopup(fm);
        refreshCategoriesByName();
        refreshTabFileTagsByName();
        PrimeFaces.current().executeScript("PF('editFileTagsPopup').show()");
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
        List<String> datasetFileCategories = dataFileCategoryService.mergeDatasetFileCategories(dataset.getCategories());
        for (String category: datasetFileCategories ){
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
    
    public void handleSelection(final AjaxBehaviorEvent event) {
        if (selectedTags != null) {
            selectedTags = selectedTags.clone();
        }
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

    /* 
     * Items for the "Advanced (Ingest) Options" popup. 
     * 
     */
    private FileMetadata fileMetadataSelectedForIngestOptionsPopup = null;

    public void setFileMetadataSelectedForIngestOptionsPopup(FileMetadata fm) {
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
                uploadStream = file.getInputStream();
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

    public boolean rsyncUploadSupported() {
        // ToDo - rsync was written before multiple store support and currently is hardcoded to use the DataAccess.S3 store. 
        // When those restrictions are lifted/rsync can be configured per store, the test in the 
        // Dataset Util method should be updated
        if (settingsWrapper.isRsyncUpload() && !DatasetUtil.isRsyncAppropriateStorageDriver(dataset)) {
            //dataset.file.upload.setUp.rsync.failed.detail
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.file.upload.setUp.rsync.failed"), BundleUtil.getStringFromBundle("dataset.file.upload.setUp.rsync.failed.detail"));
            FacesContext.getCurrentInstance().addMessage(null, message);
        }

        return settingsWrapper.isRsyncUpload() && DatasetUtil.isRsyncAppropriateStorageDriver(dataset);
    }
    
    // Globus must be one of the upload methods listed in the :UploadMethods setting
    // and the dataset's store must be in the list allowed by the GlobusStores
    // setting
    public boolean globusUploadSupported() {
        return settingsWrapper.isGlobusUpload()
                && settingsWrapper.isGlobusEnabledStorageDriver(dataset.getEffectiveStorageDriverId());
    }
    
    public boolean webloaderUploadSupported() {
        return settingsWrapper.isWebloaderUpload() && StorageIO.isDirectUploadEnabled(dataset.getEffectiveStorageDriverId());
    }

    private void populateFileMetadatas() {
        fileMetadatas = new ArrayList<>();
        if (selectedFileIdsList == null || selectedFileIdsList.isEmpty()) {
            return;
        }

        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            for (Long id : selectedFileIdsList) {
                if (id.intValue() == fmd.getDataFile().getId().intValue()) {
                    fileMetadatas.add(fmd);
                }
            }
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
    
    //Determines whether this Dataset uses a public store and therefore doesn't support embargoed or restricted files
    public boolean isHasPublicStore() {
        return settingsWrapper.isTrueForKey(SettingsServiceBean.Key.PublicInstall, StorageIO.isPublicStore(dataset.getEffectiveStorageDriverId()));
    }
    
    public String getWebloaderUrlForDataset(Dataset d) {
        String localeCode = session.getLocaleCode();
        User user = session.getUser();
        if (user instanceof AuthenticatedUser) {
            ApiToken apiToken = authService.getValidApiTokenForUser((AuthenticatedUser) user);
            return WebloaderUtil.getWebloaderUrl(d, apiToken, localeCode,
                    settingsService.getValueForKey(SettingsServiceBean.Key.WebloaderUrl));
        } else {
            // Shouldn't normally happen (seesion timeout? bug?)
            logger.warning("getWebloaderUrlForDataset called for non-Authenticated user");
            return null;
        }
    }
}
