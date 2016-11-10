package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookResponseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreatePrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeaccessionDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeletePrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetPrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.ingest.IngestRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.metadataimport.ForeignMetadataImportServiceBean;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlUtil;
import edu.harvard.iq.dataverse.search.SearchFilesServiceBean;
import edu.harvard.iq.dataverse.search.SortBy;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileSortFieldAndOrder;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import org.apache.commons.httpclient.HttpClient;
import org.primefaces.context.RequestContext;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.HashSet;
import javax.faces.model.SelectItem;
import java.util.logging.Level;
import edu.harvard.iq.dataverse.datasetutility.TwoRavensHelper;
import edu.harvard.iq.dataverse.datasetutility.WorldMapPermissionHelper;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;

import javax.faces.event.AjaxBehaviorEvent;

import javax.faces.context.ExternalContext;
import org.apache.commons.lang.StringEscapeUtils;

import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DatasetPage")
public class DatasetPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());

    public enum EditMode {

        CREATE, INFO, FILE, METADATA, LICENSE
    };

    public enum DisplayMode {

        INIT, SAVE
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
    DataverseServiceBean dataverseService;
    @EJB
    DatasetFieldServiceBean fieldService;
    @EJB
    VariableServiceBean variableService;
    @EJB
    IngestServiceBean ingestService;
    @EJB
    ForeignMetadataImportServiceBean metadataImportService;
    @EJB
    EjbDataverseEngine commandEngine;
    @Inject
    DataverseSession session;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    MapLayerMetadataServiceBean mapLayerMetadataService;
    @EJB
    BuiltinUserServiceBean builtinUserService;
    @EJB
    DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    AuthenticationServiceBean authService;
    @EJB
    SystemConfig systemConfig;
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    @EJB
    FileDownloadServiceBean fileDownloadService;
    @EJB
    DataverseLinkingServiceBean dvLinkingService;
    @EJB
    DatasetLinkingServiceBean dsLinkingService;
    @EJB
    SearchFilesServiceBean searchFilesService;
    @EJB
    DataverseRoleServiceBean dataverseRoleService;
    @EJB
    PrivateUrlServiceBean privateUrlService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    DatasetVersionUI datasetVersionUI;
    @Inject PermissionsWrapper permissionsWrapper;
    @Inject FileDownloadHelper fileDownloadHelper;
    @Inject TwoRavensHelper twoRavensHelper;
    @Inject WorldMapPermissionHelper worldMapPermissionHelper;



    private Dataset dataset = new Dataset();
    private EditMode editMode;

    private Long ownerId;
    private Long versionId;
    private int selectedTabIndex;
    private List<DataFile> newFiles = new ArrayList();
    private DatasetVersion workingVersion;
    private int releaseRadio = 1;
    private int deaccessionRadio = 0;
    private int deaccessionReasonRadio = 0;
    private String datasetNextMajorVersion = "1.0";
    private String datasetNextMinorVersion = "";
    private String dropBoxSelection = "";
    private String deaccessionReasonText = "";
    private String displayCitation;
    private String deaccessionForwardURLFor = "";
    private String showVersionList = "false";
    private List<Template> dataverseTemplates = new ArrayList();
    private Template defaultTemplate;
    private Template selectedTemplate;
    private String globalId;
    private String persistentId;
    private String version;
    private String protocol = "";
    private String authority = "";
    private String separator = "";

    private boolean noDVsAtAll = false;

    private boolean noDVsRemaining = false;
    
    private boolean stateChanged = false;

    private List<Dataverse> dataversesForLinking = new ArrayList();
    private Long linkingDataverseId;
    private List<SelectItem> linkingDVSelectItems;
    private Dataverse linkingDataverse;
    
    // Version tab lists
    private List<DatasetVersion> versionTabList = new ArrayList();
    private List<DatasetVersion> versionTabListForPostLoad = new ArrayList();

    
    // Used to store results of permissions checks
    private final Map<String, Boolean> datasetPermissionMap = new HashMap<>(); // { Permission human_name : Boolean }
    

    
    private DataFile selectedDownloadFile;

    private Long maxFileUploadSizeInBytes = null;
    
    private String dataverseSiteUrl = ""; 
    
    private boolean removeUnusedTags;

    public boolean isRemoveUnusedTags() {
        return removeUnusedTags;
    }

    public void setRemoveUnusedTags(boolean removeUnusedTags) {
        this.removeUnusedTags = removeUnusedTags;
    }

    private List<FileMetadata> fileMetadatas;
    private String fileSortField;
    private String fileSortOrder;

    private LazyFileMetadataDataModel lazyModel;

    public LazyFileMetadataDataModel getLazyModel() {
        return lazyModel;
    }

    public void setLazyModel(LazyFileMetadataDataModel lazyModel) {
        this.lazyModel = lazyModel;
    }
    
    private String fileLabelSearchTerm;

    public String getFileLabelSearchTerm() {
        return fileLabelSearchTerm;
    }

    public void setFileLabelSearchTerm(String fileLabelSearchTerm) {
        this.fileLabelSearchTerm = fileLabelSearchTerm;
    }
    
    private List<FileMetadata> fileMetadatasSearch;
    
    public List<FileMetadata> getFileMetadatasSearch() {
        return fileMetadatasSearch;
    }

    public void setFileMetadatasSearch(List<FileMetadata> fileMetadatasSearch) {
        this.fileMetadatasSearch = fileMetadatasSearch;
    }
    
    public void updateFileSearch(){  
        logger.info("updating file search list");
        if (readOnly) {
            this.fileMetadatasSearch = selectFileMetadatasForDisplay(this.fileLabelSearchTerm); 
        } else {
            this.fileMetadatasSearch = datafileService.findFileMetadataByDatasetVersionIdLabelSearchTerm(workingVersion.getId(), this.fileLabelSearchTerm, "", "");
        }
    }
    
        private Long numberOfFilesToShow = new Long(25);

    public Long getNumberOfFilesToShow() {
        return numberOfFilesToShow;
    }

    public void setNumberOfFilesToShow(Long numberOfFilesToShow) {
        this.numberOfFilesToShow = numberOfFilesToShow;
    }
    
    public void showAll(){
        setNumberOfFilesToShow(new Long(fileMetadatasSearch.size()));
    }
    
    private List<FileMetadata> selectFileMetadatasForDisplay(String searchTerm) {
        Set<Long> searchResultsIdSet = null; 
        
        if (searchTerm != null && !searchTerm.equals("")) {
            List<Integer> searchResultsIdList = datafileService.findFileMetadataIdsByDatasetVersionIdLabelSearchTerm(workingVersion.getId(), searchTerm, "", "");
            searchResultsIdSet = new HashSet<>();
            for (Integer id : searchResultsIdList) {
                searchResultsIdSet.add(id.longValue());
            }
        }
        
        List<FileMetadata> retList = new ArrayList<>(); 
        
        for (FileMetadata fileMetadata : workingVersion.getFileMetadatasSorted()) {
            if (searchResultsIdSet == null || searchResultsIdSet.contains(fileMetadata.getId())) {
                retList.add(fileMetadata);
            }
        }
        
        return retList;
    }
    
    /*
        Save the setting locally so db isn't hit repeatedly
    
        This may be "null", signifying unlimited download size
    */
    public Long getMaxFileUploadSizeInBytes(){
        return this.maxFileUploadSizeInBytes;
    }
    
    public boolean isUnlimitedUploadFileSize(){
        
        if (this.maxFileUploadSizeInBytes == null){
            return true;
        }
        return false;
    }

    public boolean isMetadataExportEnabled() {
        return metadataExportEnabled;
    }

    public String getDataverseSiteUrl() {
        return this.dataverseSiteUrl;
    }
    
    public void setDataverseSiteUrl(String dataverseSiteUrl) {
        this.dataverseSiteUrl = dataverseSiteUrl;
    }
    
    public DataFile getSelectedDownloadFile() {
        return selectedDownloadFile;
    }

    public void setSelectedDownloadFile(DataFile selectedDownloadFile) {
        this.selectedDownloadFile = selectedDownloadFile;
    }
    
    public List<DataFile> getNewFiles() {
        return newFiles;
    }
    
    public void setNewFiles(List<DataFile> newFiles) {
        this.newFiles = newFiles;
    }
    
    public Dataverse getLinkingDataverse() {
        return linkingDataverse;
    }

    public void setLinkingDataverse(Dataverse linkingDataverse) {
        this.linkingDataverse = linkingDataverse;
    }

    public List<SelectItem> getLinkingDVSelectItems() {
        return linkingDVSelectItems;
    }

    public void setLinkingDVSelectItems(List<SelectItem> linkingDVSelectItems) {
        this.linkingDVSelectItems = linkingDVSelectItems;
    }

    public Long getLinkingDataverseId() {
        return linkingDataverseId;
    }

    public void setLinkingDataverseId(Long linkingDataverseId) {
        this.linkingDataverseId = linkingDataverseId;
    }

    public List<Dataverse> getDataversesForLinking() {
        return dataversesForLinking;
    }

    public void setDataversesForLinking(List<Dataverse> dataversesForLinking) {
        this.dataversesForLinking = dataversesForLinking;
    }
    
    public void updateReleasedVersions(){
        
        setReleasedVersionTabList(resetReleasedVersionTabList());
        
    }

    public void updateLinkableDataverses() {
        dataversesForLinking = new ArrayList();
        linkingDVSelectItems = new ArrayList();
        
        //Since this is a super user we are getting all dataverses
        dataversesForLinking = dataverseService.findAll();
        if (dataversesForLinking.isEmpty()) {
            setNoDVsAtAll(true);
            return;
        }
        
        dataversesForLinking.remove(dataset.getOwner());
        Dataverse testDV = dataset.getOwner();
        while(testDV.getOwner() != null){
            dataversesForLinking.remove(testDV.getOwner());
            testDV = testDV.getOwner();
        }                      
        
        for (Dataverse removeLinked : dsLinkingService.findLinkingDataverses(dataset.getId())) {
            dataversesForLinking.remove(removeLinked);
        }
        for (Dataverse removeLinked : dvLinkingService.findLinkingDataverses(dataset.getOwner().getId())) {
            dataversesForLinking.remove(removeLinked);
        }

        if (dataversesForLinking.isEmpty()) {
            setNoDVsRemaining(true);            
            return;
        }

        for (Dataverse selectDV : dataversesForLinking) {
            linkingDVSelectItems.add(new SelectItem(selectDV.getId(), selectDV.getDisplayName()));
        }

        if (!dataversesForLinking.isEmpty() && dataversesForLinking.size() == 1 && dataversesForLinking.get(0) != null) {
            linkingDataverse = dataversesForLinking.get(0);
            linkingDataverseId = linkingDataverse.getId();
        }
    }

    public void updateSelectedLinkingDV(ValueChangeEvent event) {
        linkingDataverseId = (Long) event.getNewValue();
    }

    public boolean isNoDVsAtAll() {
        return noDVsAtAll;
    }

    public void setNoDVsAtAll(boolean noDVsAtAll) {
        this.noDVsAtAll = noDVsAtAll;
    }

    public boolean isNoDVsRemaining() {
        return noDVsRemaining;
    }
    


    public boolean isThumbnailAvailable(FileMetadata fileMetadata) {
        // new and optimized logic: 
        // - check download permission here (should be cached - so it's free!)
        // - only then ask the file service if the thumbnail is available/exists.
        // the service itself no longer checks download permissions.  
        
        if (!this.fileDownloadHelper.canDownloadFile(fileMetadata)) {
            return false;
        }
     
        return datafileService.isThumbnailAvailable(fileMetadata.getDataFile());
    }
    
    // Another convenience method - to cache Update Permission on the dataset: 
    public boolean canUpdateDataset() {
        return permissionsWrapper.canUpdateDataset(dvRequestService.getDataverseRequest(), this.dataset);
    }
    public boolean canPublishDataverse() {
        return permissionsWrapper.canIssuePublishDataverseCommand(dataset.getOwner());
    }

    public boolean canViewUnpublishedDataset() {
        return permissionsWrapper.canViewUnpublishedDataset( dvRequestService.getDataverseRequest(), dataset);
    }
        
    /* 
     * 4.2.1 optimization. 
     * HOWEVER, this doesn't appear to be saving us anything! 
     * i.e., it's just as cheap to use session.getUser().isAuthenticated() 
     * every time; it doesn't do any new db lookups. 
    */
    public boolean isSessionUserAuthenticated() {
        return session.getUser().isAuthenticated();
    }
    
    /**
     * For use in the Dataset page
     * @return 
     */
    public boolean isSuperUser(){
        
        if (!this.isSessionUserAuthenticated()){
            return false;
        }
        
        if (this.session.getUser().isSuperuser()){
            return true;
        }
        return false;
    }
    /* 
       TODO/OPTIMIZATION: This is still costing us N SELECT FROM GuestbookResponse queries, 
       where N is the number of files. This could of course be replaced by a query that'll 
       look up all N at once... Not sure if it's worth it; especially now that N
       will always be 10, for the initial page load. -- L.A. 4.2.1
     */
    public Long getGuestbookResponseCount(FileMetadata fileMetadata) {
        return guestbookResponseService.getCountGuestbookResponsesByDataFileId(fileMetadata.getDataFile().getId());
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
    
    public void setNoDVsRemaining(boolean noDVsRemaining) {
        this.noDVsRemaining = noDVsRemaining;
    }

    private final Map<Long, MapLayerMetadata> mapLayerMetadataLookup = new HashMap<>();

    private GuestbookResponse guestbookResponse;
    private Guestbook selectedGuestbook;

    public GuestbookResponse getGuestbookResponse() {
        return guestbookResponse;
    }

    public void setGuestbookResponse(GuestbookResponse guestbookResponse) {
        this.guestbookResponse = guestbookResponse;
    }

    public Guestbook getSelectedGuestbook() {
        return selectedGuestbook;
    }

    public void setSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }

    public void viewSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }

    public void reset() {
        dataset.setGuestbook(null);
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
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }    

    public String getShowVersionList() {
        return showVersionList;
    }

    public void setShowVersionList(String showVersionList) {
        this.showVersionList = showVersionList;
    }

    public String getShowOtherText() {
        return showOtherText;
    }

    public void setShowOtherText(String showOtherText) {
        this.showOtherText = showOtherText;
    }
    private String showOtherText = "false";

    public String getDeaccessionForwardURLFor() {
        return deaccessionForwardURLFor;
    }

    public void setDeaccessionForwardURLFor(String deaccessionForwardURLFor) {
        this.deaccessionForwardURLFor = deaccessionForwardURLFor;
    }
    private DatasetVersionDifference datasetVersionDifference;

    public String getDeaccessionReasonText() {
        return deaccessionReasonText;
    }

    public void setDeaccessionReasonText(String deaccessionReasonText) {
        this.deaccessionReasonText = deaccessionReasonText;
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

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
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

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }

    public int getReleaseRadio() {
        return releaseRadio;
    }

    public void setReleaseRadio(int releaseRadio) {
        this.releaseRadio = releaseRadio;
    }

    public String getDatasetNextMajorVersion() {
        return datasetNextMajorVersion;
    }

    public void setDatasetNextMajorVersion(String datasetNextMajorVersion) {
        this.datasetNextMajorVersion = datasetNextMajorVersion;
    }

    public String getDatasetNextMinorVersion() {
        return datasetNextMinorVersion;
    }

    public void setDatasetNextMinorVersion(String datasetNextMinorVersion) {
        this.datasetNextMinorVersion = datasetNextMinorVersion;
    }

    public int getDeaccessionReasonRadio() {
        return deaccessionReasonRadio;
    }

    public void setDeaccessionReasonRadio(int deaccessionReasonRadio) {
        this.deaccessionReasonRadio = deaccessionReasonRadio;
    }

    public int getDeaccessionRadio() {
        return deaccessionRadio;
    }

    public void setDeaccessionRadio(int deaccessionRadio) {
        this.deaccessionRadio = deaccessionRadio;
    }

    public List<Template> getDataverseTemplates() {
        return dataverseTemplates;
    }

    public void setDataverseTemplates(List<Template> dataverseTemplates) {
        this.dataverseTemplates = dataverseTemplates;
    }

    public Template getDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(Template defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public Template getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplate(Template selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }

    public void updateSelectedTemplate(ValueChangeEvent event) {
        
        selectedTemplate = (Template) event.getNewValue();
        if (selectedTemplate != null) {
            //then create new working version from the selected template
            workingVersion.updateDefaultValuesFromTemplate(selectedTemplate); 
            updateDatasetFieldInputLevels();
        } else { 
            workingVersion.initDefaultValues();
            updateDatasetFieldInputLevels();
        }
        resetVersionUI();
    }

    
    
    /*
    // Original
    private void updateDatasetFieldInputLevels() {
        Long dvIdForInputLevel = ownerId;

        if (!dataverseService.find(ownerId).isMetadataBlockRoot()) {
            dvIdForInputLevel = dataverseService.find(ownerId).getMetadataRootId();
        }
        for (DatasetField dsf : workingVersion.getFlatDatasetFields()) {
            DataverseFieldTypeInputLevel dsfIl = dataverseFieldTypeInputLevelService.findByDataverseIdDatasetFieldTypeId(dvIdForInputLevel, dsf.getDatasetFieldType().getId());
            if (dsfIl != null) {
                dsf.setInclude(dsfIl.isInclude());
            } else {
                dsf.setInclude(true);
            }
        }
    }*/

    /***
     *
     * Note: Updated to retrieve DataverseFieldTypeInputLevel objects in single query
     *
     */    
     private void updateDatasetFieldInputLevels() {
         Long dvIdForInputLevel = ownerId;
        
         // OPTIMIZATION (?): replaced "dataverseService.find(ownerId)" with 
         // simply dataset.getOwner()... saves us a few lookups.
         // TODO: could there possibly be any reason we want to look this 
         // dataverse up by the id here?? -- L.A. 4.2.1
         if (!dataset.getOwner().isMetadataBlockRoot()) {
             dvIdForInputLevel = dataset.getOwner().getMetadataRootId();
         }        
        
        /* ---------------------------------------------------------
            Map to hold DatasetFields  
            Format:  {  DatasetFieldType.id : DatasetField }
         --------------------------------------------------------- */
        // Initialize Map
        Map<Long, DatasetField> mapDatasetFields = new HashMap<>();   

        // Populate Map
         for (DatasetField dsf : workingVersion.getFlatDatasetFields()) {
            if (dsf.getDatasetFieldType().getId() != null){
                mapDatasetFields.put(dsf.getDatasetFieldType().getId(), dsf);
            }
        }      
        
        /* ---------------------------------------------------------
            Retrieve List of DataverseFieldTypeInputLevel objects
            Use the DatasetFieldType id's which are the Map's keys
         --------------------------------------------------------- */
        List<Long> idList = new ArrayList<Long>(mapDatasetFields.keySet());
        List<DataverseFieldTypeInputLevel> dsFieldTypeInputLevels = dataverseFieldTypeInputLevelService.findByDataverseIdAndDatasetFieldTypeIdList(dvIdForInputLevel, idList);
        
        /* ---------------------------------------------------------
            Iterate through List of DataverseFieldTypeInputLevel objects
            Call "setInclude" on its related DatasetField object
         --------------------------------------------------------- */
        for (DataverseFieldTypeInputLevel oneDSFieldTypeInputLevel : dsFieldTypeInputLevels){
            
            if (oneDSFieldTypeInputLevel != null) {
                // Is the DatasetField in the hash?    hash format: {  DatasetFieldType.id : DatasetField }
                DatasetField dsf = mapDatasetFields.get(oneDSFieldTypeInputLevel.getDatasetFieldType().getId());  
                if (dsf != null){
                    // Yes, call "setInclude"
                    dsf.setInclude(oneDSFieldTypeInputLevel.isInclude());
                    // remove from hash                
                    mapDatasetFields.remove(oneDSFieldTypeInputLevel.getDatasetFieldType().getId());    
                }
            }
        }  // end: updateDatasetFieldInputLevels
        
        /* ---------------------------------------------------------
            Iterate through any DatasetField objects remaining in the hash
            Call "setInclude(true) on each one
         --------------------------------------------------------- */
        for ( DatasetField dsf  : mapDatasetFields.values()) {
               if (dsf != null){
                   dsf.setInclude(true);
               }
        }
     }

    public void handleChange() {
        logger.fine("handle change");
        logger.fine("new value " + selectedTemplate.getId());
    }

    public void handleChangeButton() {

    }

    public boolean isShapefileType(FileMetadata fm) {
        if (fm == null) {
            return false;
        }
        if (fm.getDataFile() == null) {
            return false;
        }

        return fm.getDataFile().isShapefileType();
    }

    /*
     Check if the FileMetadata.dataFile has an associated MapLayerMetadata object
    
     The MapLayerMetadata objects have been fetched at page inception by "loadMapLayerMetadataLookup()" 
     */
    public boolean hasMapLayerMetadata(FileMetadata fm) {
        if (fm == null) {
            return false;
        }
        if (fm.getDataFile() == null) {
            return false;
        }
        return doesDataFileHaveMapLayerMetadata(fm.getDataFile());
    }

    /**
     * Check if a DataFile has an associated MapLayerMetadata object
     *
     * The MapLayerMetadata objects have been fetched at page inception by
     * "loadMapLayerMetadataLookup()"
     */
    private boolean doesDataFileHaveMapLayerMetadata(DataFile df) {
        if (df == null) {
            return false;
        }
        if (df.getId() == null) {
            return false;
        }
        return this.mapLayerMetadataLookup.containsKey(df.getId());
    }

    /**
     * Using a DataFile id, retrieve an associated MapLayerMetadata object
     *
     * The MapLayerMetadata objects have been fetched at page inception by
     * "loadMapLayerMetadataLookup()"
     */
    public MapLayerMetadata getMapLayerMetadata(DataFile df) {
        if (df == null) {
            return null;
        }
        return this.mapLayerMetadataLookup.get(df.getId());
    }

    private void msg(String s){
        // System.out.println(s);
    }
    
    /**
     * For development
     * 
     * Flag for whether to show sample insert statements for Geoconnect Debug
     * 
     * Conditions to meet: Person is superuser and GeoconnectDebug active 
     * 
     * @return 
     */
    public boolean isGeoconnectDebugAvailable(){

        if (!this.isSuperUser()){
            return false;
        }

        if (settingsService.isTrueForKey(SettingsServiceBean.Key.GeoconnectDebug, false)){
            return true;
        }    
        return false;
      
    }

    /**
     * Create a hashmap consisting of { DataFile.id : MapLayerMetadata object}
     *
     * Very few DataFiles will have associated MapLayerMetadata objects so only
     * use 1 query to get them
     */
    private void loadMapLayerMetadataLookup() {
        if (this.dataset == null) {
        }
        if (this.dataset.getId() == null) {
            return;
        }
        List<MapLayerMetadata> mapLayerMetadataList = mapLayerMetadataService.getMapLayerMetadataForDataset(this.dataset);
        if (mapLayerMetadataList == null) {
            return;
        }
        for (MapLayerMetadata layer_metadata : mapLayerMetadataList) {
            mapLayerMetadataLookup.put(layer_metadata.getDataFile().getId(), layer_metadata);
        }

    }// A DataFile may have a related MapLayerMetadata object

    
    
    private List<FileMetadata> displayFileMetadata;

    public List<FileMetadata> getDisplayFileMetadata() {
        return displayFileMetadata;
    }

    public void setDisplayFileMetadata(List<FileMetadata> displayFileMetadata) {
        this.displayFileMetadata = displayFileMetadata;
    }
    
    private boolean readOnly = true; 
    private boolean metadataExportEnabled;
    private String originalSourceUrl = null;

    public String getOriginalSourceUrl() {
        return originalSourceUrl; 
    }
    
    public void setOriginalSourceUrl(String originalSourceUrl) {
        this.originalSourceUrl = originalSourceUrl;
    }
    
    public String init() {
        return init(true);
    }
    
    public String initCitation() {
        return init(false);
    }     
    
    private String init(boolean initFull) {
        //System.out.println("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes
               
        this.maxFileUploadSizeInBytes = systemConfig.getMaxFileUploadSize();
        setDataverseSiteUrl(systemConfig.getDataverseSiteUrl());
        /**
         * For now having DDI export enabled is a proxy for having any metadata
         * export enabled (JSON, Dublin Core, etc.).
         */
        metadataExportEnabled = systemConfig.isDdiExportEnabled();

        guestbookResponse = new GuestbookResponse();
        
        String nonNullDefaultIfKeyNotFound = "";
        protocol = settingsService.getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        authority = settingsService.getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
        separator = settingsService.getValueForKey(SettingsServiceBean.Key.DoiSeparator, nonNullDefaultIfKeyNotFound);
        
        if (dataset.getId() != null || versionId != null || persistentId != null) { // view mode for a dataset     

            DatasetVersionServiceBean.RetrieveDatasetVersionResponse retrieveDatasetVersionResponse = null;

            // ---------------------------------------
            // Set the workingVersion and Dataset
            // ---------------------------------------           
            if (persistentId != null) {
                logger.fine("initializing DatasetPage with persistent ID " + persistentId);
                // Set Working Version and Dataset by PersistentID
                dataset = datasetService.findByGlobalId(persistentId);
                if (dataset == null) {
                    logger.warning("No such dataset: "+persistentId);
                    return permissionsWrapper.notFound();
                }
                logger.fine("retrived dataset, id="+dataset.getId());
                
                retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
                //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByPersistentId(persistentId, version);
                this.workingVersion = retrieveDatasetVersionResponse.getDatasetVersion();
                logger.info("retreived version: id: " + workingVersion.getId() + ", state: " + this.workingVersion.getVersionState());

            } else if (dataset.getId() != null) {
                // Set Working Version and Dataset by Datasaet Id and Version
                dataset = datasetService.find(dataset.getId());
                if (dataset == null) {
                    logger.warning("No such dataset: "+dataset);
                    return permissionsWrapper.notFound();
                }
                //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionById(dataset.getId(), version);
                retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
                this.workingVersion = retrieveDatasetVersionResponse.getDatasetVersion();
                logger.info("retreived version: id: " + workingVersion.getId() + ", state: " + this.workingVersion.getVersionState());

            } else if (versionId != null) {
                // TODO: 4.2.1 - this method is broken as of now!
                // Set Working Version and Dataset by DatasaetVersion Id
                //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByVersionId(versionId);

            } 

            if (retrieveDatasetVersionResponse == null) {
                return permissionsWrapper.notFound();
            }

            
            //this.dataset = this.workingVersion.getDataset();

            // end: Set the workingVersion and Dataset
            // ---------------------------------------
            // Is the DatasetVersion or Dataset null?
            //
            if (workingVersion == null || this.dataset == null) {
                return permissionsWrapper.notFound();
            }

            // Is the Dataset harvested?
            if (dataset.isHarvested()) {
                // if so, we'll simply forward to the remote URL for the original
                // source of this harvested dataset:
                String originalSourceURL = dataset.getRemoteArchiveURL();
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
                    return originalSourceURL;
                }

                return permissionsWrapper.notFound();
            }

            // Check permisisons           
            if (!(workingVersion.isReleased() || workingVersion.isDeaccessioned()) && !this.canViewUnpublishedDataset()) {
                return permissionsWrapper.notAuthorized();
            }

            if (!retrieveDatasetVersionResponse.wasRequestedVersionRetrieved()) {
                //msg("checkit " + retrieveDatasetVersionResponse.getDifferentVersionMessage());
                JsfHelper.addWarningMessage(retrieveDatasetVersionResponse.getDifferentVersionMessage());//JH.localize("dataset.message.metadataSuccess"));
            }
            
            // init the citation
            displayCitation = dataset.getCitation(true, workingVersion);
            

            if (initFull) {
                // init the list of FileMetadatas
                if (workingVersion.isDraft() && canUpdateDataset()) {
                    readOnly = false;
                } else {
                    // an attempt to retreive both the filemetadatas and datafiles early on, so that 
                    // we don't have to do so later (possibly, many more times than necessary):
                    datafileService.findFileMetadataOptimizedExperimental(dataset);
                }
                fileMetadatasSearch = workingVersion.getFileMetadatasSorted();

                ownerId = dataset.getOwner().getId();
                datasetNextMajorVersion = this.dataset.getNextMajorVersionString();
                datasetNextMinorVersion = this.dataset.getNextMinorVersionString();
                datasetVersionUI = datasetVersionUI.initDatasetVersionUI(workingVersion, false);
                updateDatasetFieldInputLevels();
                
                setExistReleasedVersion(resetExistRealeaseVersion());
                        //moving setVersionTabList to tab change event
                //setVersionTabList(resetVersionTabList());
                //setReleasedVersionTabList(resetReleasedVersionTabList());
                //SEK - lazymodel may be needed for datascroller in future release
                // lazyModel = new LazyFileMetadataDataModel(workingVersion.getId(), datafileService );
                // populate MapLayerMetadata
                this.loadMapLayerMetadataLookup();  // A DataFile may have a related MapLayerMetadata object
                this.guestbookResponse = guestbookResponseService.initGuestbookResponseForFragment(dataset, null, session);
                
            }
        } else if (ownerId != null) {
            // create mode for a new child dataset
            readOnly = false; 
            editMode = EditMode.CREATE;
            dataset.setOwner(dataverseService.find(ownerId));
            dataset.setProtocol(protocol);
            dataset.setAuthority(authority);
            dataset.setDoiSeparator(separator);
            dataset.setIdentifier(datasetService.generateIdentifierSequence(protocol, authority, separator));

            if (dataset.getOwner() == null) {
                return permissionsWrapper.notFound();
            } else if (!permissionService.on(dataset.getOwner()).has(Permission.AddDataset)) {
                return permissionsWrapper.notAuthorized(); 
            }

            dataverseTemplates = dataverseService.find(ownerId).getTemplates();
            if (!dataverseService.find(ownerId).isTemplateRoot()) {
                dataverseTemplates.addAll(dataverseService.find(ownerId).getParentTemplates());
            }
            defaultTemplate = dataverseService.find(ownerId).getDefaultTemplate();
            if (defaultTemplate != null) {
                selectedTemplate = defaultTemplate;
                for (Template testT : dataverseTemplates) {
                    if (defaultTemplate.getId().equals(testT.getId())) {
                        selectedTemplate = testT;
                    }
                }
                workingVersion = dataset.getEditVersion(selectedTemplate);
                updateDatasetFieldInputLevels();
            } else {
                workingVersion = dataset.getCreateVersion();
                updateDatasetFieldInputLevels();
            }

            resetVersionUI();

            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Add New Dataset", " - Enter metadata to create the dataset's citation. You can add more metadata about this dataset after it's created."));
        } else {
            return permissionsWrapper.notFound();
        }

        try {
            privateUrl = commandEngine.submit(new GetPrivateUrlCommand(dvRequestService.getDataverseRequest(), dataset));
            if (privateUrl != null) {
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.privateurl.infoMessageAuthor", Arrays.asList(getPrivateUrlLink(privateUrl))));
            }
        } catch (CommandException ex) {
            // No big deal. The user simply doesn't have access to create or delete a Private URL.
        }
        if (session.getUser() instanceof PrivateUrlUser) {
            PrivateUrlUser privateUrlUser = (PrivateUrlUser) session.getUser();
            if (dataset != null && dataset.getId().equals(privateUrlUser.getDatasetId())) {
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.privateurl.infoMessageReviewer"));
            }
        }
        return null;
    }
    
    public boolean isReadOnly() {
        return readOnly; 
    }

    private void resetVersionUI() {
        
        datasetVersionUI = datasetVersionUI.initDatasetVersionUI(workingVersion, true);
        if (isSessionUserAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();

            //On create set pre-populated fields
            for (DatasetField dsf : dataset.getEditVersion().getDatasetFields()) {
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.depositor) && dsf.isEmpty()) {
                    dsf.getDatasetFieldValues().get(0).setValue(au.getLastName() + ", " + au.getFirstName());
                }
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.dateOfDeposit) && dsf.isEmpty()) {
                    dsf.getDatasetFieldValues().get(0).setValue(new SimpleDateFormat("yyyy-MM-dd").format(new Timestamp(new Date().getTime())));
                }

                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContact) && dsf.isEmpty()) {
                    for (DatasetFieldCompoundValue contactValue : dsf.getDatasetFieldCompoundValues()) {
                        for (DatasetField subField : contactValue.getChildDatasetFields()) {
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactName)) {
                                subField.getDatasetFieldValues().get(0).setValue(au.getLastName() + ", " + au.getFirstName());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactAffiliation)) {
                                subField.getDatasetFieldValues().get(0).setValue(au.getAffiliation());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactEmail)) {
                                subField.getDatasetFieldValues().get(0).setValue(au.getEmail());
                            }
                        }
                    }
                }

                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.author) && dsf.isEmpty()) {
                    for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                        for (DatasetField subField : authorValue.getChildDatasetFields()) {
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorName)) {
                                subField.getDatasetFieldValues().get(0).setValue(au.getLastName() + ", " + au.getFirstName());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorAffiliation)) {
                                subField.getDatasetFieldValues().get(0).setValue(au.getAffiliation());
                            }
                        }
                    }
                }
            }
        }
    }
    
    private boolean bulkUpdateCheckVersion(){
        return workingVersion.isReleased();
    }
    
    private void refreshSelectedFiles(){
        if (readOnly) {
            dataset = datasetService.find(dataset.getId());
        }
        String termsOfAccess = workingVersion.getTermsOfUseAndAccess().getTermsOfAccess();
        boolean requestAccess = workingVersion.getTermsOfUseAndAccess().isFileAccessRequest();
        workingVersion = dataset.getEditVersion();
        workingVersion.getTermsOfUseAndAccess().setTermsOfAccess(termsOfAccess);
        workingVersion.getTermsOfUseAndAccess().setFileAccessRequest(requestAccess);
        List <FileMetadata> newSelectedFiles = new ArrayList();
        for (FileMetadata fmd : selectedFiles){
            for (FileMetadata fmdn: workingVersion.getFileMetadatas()){
                if (fmd.getDataFile().equals(fmdn.getDataFile())){
                    newSelectedFiles.add(fmdn);
                }
            }
        }
        
        selectedFiles.clear();
        for (FileMetadata fmdn : newSelectedFiles ){
            selectedFiles.add(fmdn);
        }
        readOnly = false;
    }
    
    private Integer chunkSize = 25;

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    public void viewAllButtonPress(){
        setChunkSize(fileMetadatasSearch.size());
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
        if (this.activeTabIndex == 3) {
            setVersionTabList(resetVersionTabList());
            setReleasedVersionTabList(resetReleasedVersionTabList());
        } else {
            releasedVersionTabList = new ArrayList();
            versionTabList = new ArrayList();
            if(this.activeTabIndex == 0) {
                 init();
            }          
        }
    }

    public void edit(EditMode editMode) {
        this.editMode = editMode;
        if (this.readOnly) {
            dataset = datasetService.find(dataset.getId());
        }
        workingVersion = dataset.getEditVersion();

        if (editMode == EditMode.INFO) {
            // ?
        } else if (editMode == EditMode.FILE) {
            // JH.addMessage(FacesMessage.SEVERITY_INFO, JH.localize("dataset.message.editFiles"));
            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Upload + Edit Dataset Files", " - You can drag and drop your files from your desktop, directly into the upload widget."));
        } else if (editMode.equals(EditMode.METADATA)) {
            datasetVersionUI = datasetVersionUI.initDatasetVersionUI(workingVersion, true);
            updateDatasetFieldInputLevels();
            JH.addMessage(FacesMessage.SEVERITY_INFO, JH.localize("dataset.message.editMetadata"));
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Edit Dataset Metadata", " - Add more metadata about your dataset to help others easily find it."));
        } else if (editMode.equals(EditMode.LICENSE)){
            JH.addMessage(FacesMessage.SEVERITY_INFO, JH.localize("dataset.message.editTerms"));
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Edit Dataset License and Terms", " - Update your dataset's license and terms of use."));
        }
        this.readOnly = false;
    }

    public String releaseDraft() {
        if (releaseRadio == 1) {
            return releaseDataset(true);
        } else {
            return releaseDataset(false);
        }
    }

    public String releaseMajor() {
        return releaseDataset(false);
    }

    public String sendBackToContributor() {
        Command<Dataset> cmd;
        workingVersion = dataset.getEditVersion();
        workingVersion.setInReview(false);
        try {
            cmd = new UpdateDatasetCommand(dataset, dvRequestService.getDataverseRequest());
            ((UpdateDatasetCommand) cmd).setValidateLenient(true); 
            dataset = commandEngine.submit(cmd);
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Dataset Submission Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());
            return "";
        }
        List<AuthenticatedUser> authUsers = permissionService.getUsersWithPermissionOn(Permission.PublishDataset, dataset);
        List<AuthenticatedUser> editUsers = permissionService.getUsersWithPermissionOn(Permission.EditDataset, dataset);
        for (AuthenticatedUser au : authUsers) {
            editUsers.remove(au);
        }
        for (AuthenticatedUser au : editUsers) {
            userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.RETURNEDDS, dataset.getLatestVersion().getId());
        }

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetSubmitted", "This dataset has been sent back to the contributor.");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return  returnToLatestVersion();
    }

    public String submitDataset() {
        Command<Dataset> cmd;
        workingVersion = dataset.getEditVersion();
        workingVersion.setInReview(true);
        try {
            cmd = new UpdateDatasetCommand(dataset, dvRequestService.getDataverseRequest());
            ((UpdateDatasetCommand) cmd).setValidateLenient(true); 
            dataset = commandEngine.submit(cmd);
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Dataset Submission Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());
            return "";
        }
        List<AuthenticatedUser> authUsers = permissionService.getUsersWithPermissionOn(Permission.PublishDataset, dataset);
        for (AuthenticatedUser au : authUsers) {
            userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.SUBMITTEDDS, dataset.getLatestVersion().getId());
        }

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetSubmitted", "Your dataset has been submitted for review.");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return  returnToLatestVersion();
    }
    
    public String releaseParentDVAndDataset(){
        releaseParentDV();
        return releaseDataset(false);
    }

    public String releaseDataset() {
        return releaseDataset(false);
    }
    
    private void releaseParentDV(){
        if (session.getUser() instanceof AuthenticatedUser) {
            PublishDataverseCommand cmd = new PublishDataverseCommand(dvRequestService.getDataverseRequest(), dataset.getOwner());
            try {
                commandEngine.submit(cmd);
                JsfHelper.addSuccessMessage(JH.localize("dataverse.publish.success"));

            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unexpected Exception calling  publish dataverse command", ex);
                JsfHelper.addErrorMessage(JH.localize("dataverse.publish.failure"));

            }
        } else {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseNotReleased", "Only authenticated users can release a dataverse.");
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
        
    }

    public String deaccessionVersions() {
        Command<DatasetVersion> cmd;
        try {
            if (selectedDeaccessionVersions == null) {
                for (DatasetVersion dv : this.dataset.getVersions()) {
                    if (dv.isReleased()) {
                        DatasetVersion deaccession = datasetVersionService.find(dv.getId());
                        cmd = new DeaccessionDatasetVersionCommand(dvRequestService.getDataverseRequest(), setDatasetVersionDeaccessionReasonAndURL(deaccession), true);
                        DatasetVersion datasetv = commandEngine.submit(cmd);
                    }
                }
            } else {
                for (DatasetVersion dv : selectedDeaccessionVersions) {
                    DatasetVersion deaccession = datasetVersionService.find(dv.getId());
                    cmd = new DeaccessionDatasetVersionCommand(dvRequestService.getDataverseRequest(), setDatasetVersionDeaccessionReasonAndURL(deaccession), false);
                    DatasetVersion datasetv = commandEngine.submit(cmd);
                }
            }
        } catch (CommandException ex) {
            logger.severe(ex.getMessage());
            JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.message.deaccessionFailure"));
        }
        JsfHelper.addSuccessMessage(JH.localize("datasetVersion.message.deaccessionSuccess"));
        return returnToDatasetOnly();
    }

    private DatasetVersion setDatasetVersionDeaccessionReasonAndURL(DatasetVersion dvIn) {
        int deaccessionReasonCode = getDeaccessionReasonRadio();
        String deacessionReasonDetail = getDeaccessionReasonText() != null ? ( getDeaccessionReasonText()).trim() : "";
        switch (deaccessionReasonCode) {
            case 1:
                dvIn.setVersionNote("There is identifiable data in one or more files.");
                break;
            case 2:
                dvIn.setVersionNote("The research article has been retracted.");
                break;
            case 3:
                dvIn.setVersionNote("The dataset has been transferred to another repository.");
                break;
            case 4:
                dvIn.setVersionNote("IRB request.");
                break;
            case 5:
                dvIn.setVersionNote("Legal issue or Data Usage Agreement.");
                break;
            case 6:
                dvIn.setVersionNote("Not a valid dataset.");
                break;
            case 7:
                break;
        }
        if (!deacessionReasonDetail.isEmpty()){
            if (!StringUtil.isEmpty(dvIn.getVersionNote())){
                dvIn.setVersionNote(dvIn.getVersionNote() + " " + deacessionReasonDetail);
            } else {
                dvIn.setVersionNote(deacessionReasonDetail);
            }
        }
        
        dvIn.setArchiveNote(getDeaccessionForwardURLFor());
        return dvIn;
    }

    private String releaseDataset(boolean minor) {
        Command<Dataset> cmd;
        //SEK we want to notify concerned users if a DS in review has been published.
        boolean notifyPublish = workingVersion.isInReview();
        if (session.getUser() instanceof AuthenticatedUser) {
            try {
                if (editMode == EditMode.CREATE) {
                    cmd = new PublishDatasetCommand(dataset, dvRequestService.getDataverseRequest(), minor);
                } else {
                    cmd = new PublishDatasetCommand(dataset, dvRequestService.getDataverseRequest(), minor);
                }
                dataset = commandEngine.submit(cmd);
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.publishSuccess"));
                if (notifyPublish) {
                    List<AuthenticatedUser> authUsers = permissionService.getUsersWithPermissionOn(Permission.PublishDataset, dataset);
                    List<AuthenticatedUser> editUsers = permissionService.getUsersWithPermissionOn(Permission.EditDataset, dataset);
                    for (AuthenticatedUser au : authUsers) {
                        editUsers.remove(au);
                    }
                    for (AuthenticatedUser au : editUsers) {
                        userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.PUBLISHEDDS, dataset.getLatestVersion().getId());
                    }
                }
            } catch (CommandException ex) {
                
                JsfHelper.addErrorMessage(ex.getLocalizedMessage());
                logger.severe(ex.getMessage());
            }
        } else {
            
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.only.authenticatedUsers"));
        }
        return returnToDatasetOnly();
    }

    public String registerDataset() {
        Command<Dataset> cmd;
        try {
            cmd = new UpdateDatasetCommand(dataset, dvRequestService.getDataverseRequest());
            ((UpdateDatasetCommand) cmd).setValidateLenient(true); 
            dataset = commandEngine.submit(cmd);
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Dataset Registration Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetRegistered", "Your dataset is now registered.");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return returnToDatasetOnly();
    }

    public void refresh(ActionEvent e) {
        refresh();
    }
    
    public void refresh() {
        logger.fine("refreshing");

        //dataset = datasetService.find(dataset.getId());
        dataset = null;

        logger.fine("refreshing working version");

        DatasetVersionServiceBean.RetrieveDatasetVersionResponse retrieveDatasetVersionResponse = null;

        if (persistentId != null) {
            //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByPersistentId(persistentId, version);
            dataset = datasetService.findByGlobalId(persistentId);
            retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
        } else if (versionId != null) {
            retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByVersionId(versionId);
        } else if (dataset.getId() != null) {
            //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionById(dataset.getId(), version);
            dataset = datasetService.find(dataset.getId());
            retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
        }

        if (retrieveDatasetVersionResponse == null) {
            // TODO: 
            // should probably redirect to the 404 page, if we can't find 
            // this version anymore. 
            // -- L.A. 4.2.3 
            return;
        }
        this.workingVersion = retrieveDatasetVersionResponse.getDatasetVersion();

        if (this.workingVersion == null) {
            // TODO: 
            // same as the above

            return;
        }


        if (dataset == null) {
            // this would be the case if we were retrieving the version by 
            // the versionId, above.
            this.dataset = this.workingVersion.getDataset();
        }

        

        if (readOnly) {
            datafileService.findFileMetadataOptimizedExperimental(dataset);
        } 
        fileMetadatasSearch = workingVersion.getFileMetadatasSorted();

        displayCitation = dataset.getCitation(true, workingVersion);
        stateChanged = false;
    }
    
    public String deleteDataset() {

        Command cmd;
        try {
            cmd = new DestroyDatasetCommand(dataset, dvRequestService.getDataverseRequest());
            commandEngine.submit(cmd);
            /* - need to figure out what to do 
             Update notification in Delete Dataset Method
             for (UserNotification und : userNotificationService.findByDvObject(dataset.getId())){
             userNotificationService.delete(und);
             } */
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.message.deleteFailure"));
            logger.severe(ex.getMessage());
        }
            JsfHelper.addSuccessMessage(JH.localize("dataset.message.deleteSuccess"));
        return "/dataverse.xhtml?alias=" + dataset.getOwner().getAlias() + "&faces-redirect=true";
    }
    
    public String editFileMetadata(){
        // If there are no files selected, return an empty string - which 
        // means, do nothing, don't redirect anywhere, stay on this page. 
        // The dialogue telling the user to select at least one file will 
        // be shown to them by an onclick javascript method attached to the 
        // filemetadata edit button on the page.
        // -- L.A. 4.2.1
        if (this.selectedFiles == null || this.selectedFiles.size() < 1) {
            return "";
        } 
        return "/editdatafiles.xhtml?selectedFileIds=" + getSelectedFilesIdsString() + "&datasetId=" + dataset.getId() +"&faces-redirect=true";
    }

    public String deleteDatasetVersion() {
        Command cmd;
        try {
            cmd = new DeleteDatasetVersionCommand(dvRequestService.getDataverseRequest(), dataset);
            commandEngine.submit(cmd);
            JsfHelper.addSuccessMessage(JH.localize("datasetVersion.message.deleteSuccess"));
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.message.deleteFailure"));
            logger.severe(ex.getMessage());
        }

        return returnToDatasetOnly();
    }

    private List<FileMetadata> selectedFiles = new ArrayList<>();

    public List<FileMetadata> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<FileMetadata> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }
    
    private List<FileMetadata> selectedRestrictedFiles; // = new ArrayList<>();

    public List<FileMetadata> getSelectedRestrictedFiles() {
        return selectedRestrictedFiles;
    }

    public void setSelectedRestrictedFiles(List<FileMetadata> selectedRestrictedFiles) {
        this.selectedRestrictedFiles = selectedRestrictedFiles;
    }
    
    private List<FileMetadata> selectedUnrestrictedFiles; // = new ArrayList<>();

    public List<FileMetadata> getSelectedUnrestrictedFiles() {
        return selectedUnrestrictedFiles;
    }

    public void setSelectedUnrestrictedFiles(List<FileMetadata> selectedUnrestrictedFiles) {
        this.selectedUnrestrictedFiles = selectedUnrestrictedFiles;
    }
    
    private List<FileMetadata> selectedDownloadableFiles;

    public List<FileMetadata> getSelectedDownloadableFiles() {
        return selectedDownloadableFiles;
    }

    public void setSelectedDownloadableFiles(List<FileMetadata> selectedDownloadableFiles) {
        this.selectedDownloadableFiles = selectedDownloadableFiles;
    }
    
    private List<FileMetadata> selectedNonDownloadableFiles;

    public List<FileMetadata> getSelectedNonDownloadableFiles() {
        return selectedNonDownloadableFiles;
    }

    public void setSelectedNonDownloadableFiles(List<FileMetadata> selectedNonDownloadableFiles) {
        this.selectedNonDownloadableFiles = selectedNonDownloadableFiles;
    }
    
            
    public void validateFilesForDownload(boolean guestbookRequired){
        setSelectedDownloadableFiles(new ArrayList<>());
        setSelectedNonDownloadableFiles(new ArrayList<>());
        
        if (this.selectedFiles.isEmpty()) {
            RequestContext requestContext = RequestContext.getCurrentInstance();
            requestContext.execute("PF('selectFilesForDownload').show()");
            return;
        }
        
        
        for (FileMetadata fmd : this.selectedFiles){
            if(this.fileDownloadHelper.canDownloadFile(fmd)){
                getSelectedDownloadableFiles().add(fmd);
            } else {
                getSelectedNonDownloadableFiles().add(fmd);
            }
        }
        
        if(!getSelectedDownloadableFiles().isEmpty() && getSelectedNonDownloadableFiles().isEmpty()){
            if (guestbookRequired){
                modifyGuestbookMultipleResponse();
            } else{
                startMultipleFileDownload(false);
            }        
        }

        if(getSelectedDownloadableFiles().isEmpty() && !getSelectedNonDownloadableFiles().isEmpty()){
            RequestContext requestContext = RequestContext.getCurrentInstance();
            requestContext.execute("PF('downloadInvalid').show()");
            return;
        } 
        
        if(!getSelectedDownloadableFiles().isEmpty() && !getSelectedNonDownloadableFiles().isEmpty()){
            RequestContext requestContext = RequestContext.getCurrentInstance();
            requestContext.execute("PF('downloadMixed').show()");
        }       

    }
    
    private boolean selectAllFiles;

    public boolean isSelectAllFiles() {
        return selectAllFiles;
    }

    public void setSelectAllFiles(boolean selectAllFiles) {
        this.selectAllFiles = selectAllFiles;
    }
    
    public void toggleSelectedFiles(){
        //method for when user clicks (de-)select all files
        this.selectedFiles = new ArrayList();
        if(this.selectAllFiles){
            for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                this.selectedFiles.add(fmd);
                fmd.setSelected(true);
            }
        } else {
            for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                fmd.setSelected(false);
            }           
        }
        updateFileCounts();
    }
       
    
    public void updateSelectedFiles(FileMetadata fmd){
        if(fmd.isSelected()){
            this.selectedFiles.add(fmd);
        } else{
            this.selectedFiles.remove(fmd);
        }
        updateFileCounts();
    }

    // helper Method
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
    
        // helper Method
    public String getSelectedDownloadableFilesIdsString() {        
        String downloadIdString = "";
        for (FileMetadata fmd : this.selectedDownloadableFiles){
            if (!StringUtil.isEmpty(downloadIdString)) {
                downloadIdString += ",";
            }
            downloadIdString += fmd.getDataFile().getId();
        }
        return downloadIdString;     
    }
    
        // helper Method
    public String getSelectedFilesIdsStringForDownload() {        
        String downloadIdString = "";
        for (FileMetadata fmd : this.selectedFiles){
            if (!StringUtil.isEmpty(downloadIdString)) {
                downloadIdString += ",";
            }
            downloadIdString += fmd.getDataFile().getId();
        }
        return downloadIdString;     
    }
    
    public String getDownloadableFilesIdsString() {        
        String downloadIdString = "";
        for (FileMetadata fmd : this.selectedDownloadableFiles){
            if (!StringUtil.isEmpty(downloadIdString)) {
                downloadIdString += ",";
            }
            downloadIdString += fmd.getDataFile().getId();
        }
        return downloadIdString;
      
    }
    
    public void updateFileCounts(){
        setSelectedUnrestrictedFiles(new ArrayList<FileMetadata>());
        setSelectedRestrictedFiles(new ArrayList<FileMetadata>());
        setTabularDataSelected(false);
        for (FileMetadata fmd : this.selectedFiles){
            if(fmd.isRestricted()){
                getSelectedRestrictedFiles().add(fmd);
            } else {
                getSelectedUnrestrictedFiles().add(fmd);
            }
            if(fmd.getDataFile().isTabularData()){
                setTabularDataSelected(true);
            }
        }
    }
    

        private List<String> getSuccessMessageArguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add(StringEscapeUtils.escapeHtml(dataset.getDisplayName()));
        String linkString = "<a href=\"/dataverse/" + linkingDataverse.getAlias() + "\">" + StringEscapeUtils.escapeHtml(linkingDataverse.getDisplayName()) + "</a>";
        arguments.add(linkString);
        return arguments;
    }
    
    public String saveLinkedDataset() {
        if (linkingDataverseId == null) {
            JsfHelper.addFlashMessage("You must select a linking dataverse.");
            return "";
        }
        linkingDataverse = dataverseService.find(linkingDataverseId);
        if (readOnly) {
            // Pass a "real", non-readonly dataset the the LinkDatasetCommand: 
            dataset = datasetService.find(dataset.getId());
        }
        LinkDatasetCommand cmd = new LinkDatasetCommand(dvRequestService.getDataverseRequest(), linkingDataverse, dataset);
        try {
            commandEngine.submit(cmd);
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.linkSuccess", getSuccessMessageArguments()));
        } catch (CommandException ex) {
            String msg = "There was a problem linking this dataset to yours: " + ex;
            logger.severe(msg);
            /**
             * @todo how do we get this message to show up in the GUI?
             */
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetNotLinked", msg);
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
        return returnToLatestVersion();
    }

    List<FileMetadata> previouslyRestrictedFiles = null;
    
    public boolean isShowAccessPopup() {
        
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {

            if (fmd.isRestricted()) {
            
                if (editMode == EditMode.CREATE) {
                    // if this is a brand new file, it's definitely not 
                    // of a previously restricted kind!
                    return true; 
                }
            
                if (previouslyRestrictedFiles != null) {
                    // We've already checked whether we are in the CREATE mode, 
                    // above; and that means we can safely assume this filemetadata
                    // has an existing db id. So it is safe to use the .contains()
                    // method below:
                    if (!previouslyRestrictedFiles.contains(fmd)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    public void setShowAccessPopup(boolean showAccessPopup) {} // dummy set method
    
    
    
        
    public String restrictSelectedFiles(boolean restricted){
        
        RequestContext requestContext = RequestContext.getCurrentInstance();
        if (selectedFiles.isEmpty()) {
            if (restricted) {
                requestContext.execute("PF('selectFilesForRestrict').show()");
            } else {
                requestContext.execute("PF('selectFilesForUnRestrict').show()");
            }
            return "";
        } else {
            boolean validSelection = false;
            for (FileMetadata fmd : selectedFiles) {
                if ((fmd.isRestricted() && !restricted) || (!fmd.isRestricted() && restricted)) {
                    validSelection = true;
                }
            }
            if (!validSelection) {
                if (restricted) {
                    requestContext.execute("PF('selectFilesForRestrict').show()");
                }
                if (!restricted) {
                    requestContext.execute("PF('selectFilesForUnRestrict').show()");
                }
                return "";
            }
        }
        
        if (editMode != EditMode.CREATE) {
            if (bulkUpdateCheckVersion()) {
                refreshSelectedFiles();
            }
            restrictFiles(restricted);
        }
        
        save();
        
        return  returnToDraftVersion();
    }

    public void restrictFiles(boolean restricted) {

        //if (previouslyRestrictedFiles == null) {
        // we don't need to buther with this "previously restricted" business 
        // when in Create mode... because all the files are new, so none could 
        // have been restricted previously;
        // (well, it looks like the code below should never be called in the 
        // CREATE mode in the first place... the edit files fragment uses
        // its own restrictFiles() method there; also, the fmd.getDataFile().equals(fmw.getDataFile()))
        // line is not going to work on a new file... so be mindful of all this
        // when the code between the 2 beans is merged in 4.3.
        if (editMode != EditMode.CREATE) {
            previouslyRestrictedFiles = new ArrayList();
            for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                if (fmd.isRestricted()) {
                    previouslyRestrictedFiles.add(fmd);
                }
            }

            String fileNames = null;
            for (FileMetadata fmw : workingVersion.getFileMetadatas()) {
                for (FileMetadata fmd : this.getSelectedFiles()) {
                    if (restricted && !fmw.isRestricted()) {
                    // collect the names of the newly-restrticted files, 
                        // to show in the success message:
                        // I don't think this does the right thing: 
                        // (adds too many files to the message; good thing this 
                        // message isn't used, normally)
                        if (fileNames == null) {
                            fileNames = fmd.getLabel();
                        } else {
                            fileNames = fileNames.concat(fmd.getLabel());
                        }
                    }
                    if (fmd.getDataFile().equals(fmw.getDataFile())) {
                        fmw.setRestricted(restricted);
                        if (workingVersion.isDraft() && !fmw.getDataFile().isReleased()) {
                            // We do not really need to check that the working version is 
                            // a draft here - it must be a draft, if we've gotten this
                            // far. But just in case. -- L.A. 4.2.1
                            fmw.getDataFile().setRestricted(restricted);
                        }
                    }
                }
            }
            if (fileNames != null) {
                String successMessage = JH.localize("file.restricted.success");
                logger.fine(successMessage);
                successMessage = successMessage.replace("{0}", fileNames);
                JsfHelper.addFlashMessage(successMessage);
            }
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

    private List<FileMetadata> filesToBeDeleted = new ArrayList();
    
    public String  deleteFilesAndSave(){
        if (bulkUpdateCheckVersion()){
           refreshSelectedFiles(); 
        }
        deleteFiles();
        return save();       
    }
    
    public void deleteFiles() {
        
        String fileNames = null;
        for (FileMetadata fmd : this.getSelectedFiles()) {
            // collect the names of the newly-restrticted files, 
            // to show in the success message:
            if (fileNames == null) {
                fileNames = fmd.getLabel();
            } else {
                fileNames = fileNames.concat(fmd.getLabel());
            }
        }

        for (FileMetadata markedForDelete : selectedFiles) {
            // TODO: 
            // the code below needs to be rewritten: 
            // markedForDelete.getId() == null DOES NOT mean that this file 
            // has just been uploaded! (markedForDelete is a FileMetadata, not 
            // a DataFile!! so this maybe an existing file, but a brand new
            // DRAFT version and a brand new filemetadata that hasn't been saved 
            // yet) -- L.A. 4.2, Sep. 15 
            if (markedForDelete.getId() != null) {
                dataset.getEditVersion().getFileMetadatas().remove(markedForDelete);
                filesToBeDeleted.add(markedForDelete);
            } else {
                // the file was just added during this step, so in addition to 
                // removing it from the fileMetadatas (for display), we also remove it from 
                // the newFiles list and the dataset's files, so it won't get uploaded at all
                Iterator fmit = dataset.getEditVersion().getFileMetadatas().iterator();
                while (fmit.hasNext()) {
                    FileMetadata fmd = (FileMetadata) fmit.next();
                    if (markedForDelete.getDataFile().getStorageIdentifier().equals(fmd.getDataFile().getStorageIdentifier())) {
                        fmit.remove();
                        break;
                    }
                }
                
                Iterator<DataFile> dfIt = dataset.getFiles().iterator();
                while (dfIt.hasNext()) {
                    DataFile dfn = dfIt.next();
                    if (markedForDelete.getDataFile().getStorageIdentifier().equals(dfn.getStorageIdentifier())) {
                        
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
                            Files.delete(Paths.get(ingestService.getFilesTempDirectory() + "/" + dfn.getStorageIdentifier()));
                        } catch (IOException ioEx) {
                            // safe to ignore - it's just a temp file. 
                            logger.warning("Failed to delete temporary file " + ingestService.getFilesTempDirectory() + "/" + dfn.getStorageIdentifier());
                        }
                        
                        dfIt.remove();

                    }
                }
                
                Iterator<DataFile> nfIt = newFiles.iterator();
                while (nfIt.hasNext()) {
                    DataFile dfn = nfIt.next();
                    if (markedForDelete.getDataFile().getStorageIdentifier().equals(dfn.getStorageIdentifier())) {
                        nfIt.remove();
                    }
                }                
                
            }
        }

     
        if (fileNames != null) {
            String successMessage = JH.localize("file.deleted.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);
        }
        
        /* 
           Do note that if we are deleting any files that have UNFs (i.e., 
           tabular files), we DO NEED TO RECALCULATE the UNF of the version!
           - but we will do this inside the UpdateDatasetCommand.
        */
    }

    public String save() {
        // Validate
        Set<ConstraintViolation> constraintViolations = workingVersion.validate();
        if (!constraintViolations.isEmpty()) {
             //JsfHelper.addFlashMessage(JH.localize("dataset.message.validationError"));
             JH.addMessage(FacesMessage.SEVERITY_ERROR, JH.localize("dataset.message.validationError"));
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "See below for details."));
            return "";
        }
               


        // One last check before we save the files - go through the newly-uploaded 
        // ones and modify their names so that there are no duplicates. 
        // (but should we really be doing it here? - maybe a better approach to do it
        // in the ingest service bean, when the files get uploaded.)
        // Finally, save the files permanently: 
        ingestService.addFiles(workingVersion, newFiles);

        // Use the API to save the dataset: 
        Command<Dataset> cmd;
        try {
            if (editMode == EditMode.CREATE) {
                if ( selectedTemplate != null ) {
                    if ( isSessionUserAuthenticated() ) {
                        cmd = new CreateDatasetCommand(dataset, dvRequestService.getDataverseRequest(), false, null, selectedTemplate); 
                    } else {
                        JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.create.authenticatedUsersOnly"));
                        return null;
                    }
                } else {
                   cmd = new CreateDatasetCommand(dataset, dvRequestService.getDataverseRequest());
                }
                
            } else {
                cmd = new UpdateDatasetCommand(dataset, dvRequestService.getDataverseRequest(), filesToBeDeleted);
                ((UpdateDatasetCommand) cmd).setValidateLenient(true);  
            }
            dataset = commandEngine.submit(cmd);
            if (editMode == EditMode.CREATE) {
                if (session.getUser() instanceof AuthenticatedUser) {
                    userNotificationService.sendNotification((AuthenticatedUser) session.getUser(), dataset.getCreateDate(), UserNotification.Type.CREATEDS, dataset.getLatestVersion().getId());
                }
            }
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
            logger.log(Level.FINE, "Couldn''t save dataset: {0}", error.toString());
            populateDatasetUpdateFailureMessage();
            return null;
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());
            populateDatasetUpdateFailureMessage();
            return null;
        }
        newFiles.clear();
        if (editMode != null){
                    if(editMode.equals(EditMode.CREATE)){
            JsfHelper.addSuccessMessage(JH.localize("dataset.message.createSuccess"));
        }
        if(editMode.equals(EditMode.METADATA)){
            JsfHelper.addSuccessMessage(JH.localize("dataset.message.metadataSuccess"));
        }
        if(editMode.equals(EditMode.LICENSE)){
            JsfHelper.addSuccessMessage(JH.localize("dataset.message.termsSuccess"));
        }
        if(editMode.equals(EditMode.FILE)){
            JsfHelper.addSuccessMessage(JH.localize("dataset.message.filesSuccess"));
        }
            
        } else {
             JsfHelper.addSuccessMessage(JH.localize("dataset.message.bulkFileUpdateSuccess"));
        }

        editMode = null;

        // Call Ingest Service one more time, to 
        // queue the data ingest jobs for asynchronous execution: 
        ingestService.startIngestJobs(dataset, (AuthenticatedUser) session.getUser());

        return returnToDraftVersion();
    }
    
    private void populateDatasetUpdateFailureMessage(){
            // null check would help here. :) -- L.A. 
            if (editMode == null) {
                JH.addMessage(FacesMessage.SEVERITY_FATAL, "mystery failure");
                return;
            }
            if (editMode.equals(EditMode.CREATE)) {
                JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.message.createFailure"));
            }
            if (editMode.equals(EditMode.METADATA)) {
                JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.message.metadataFailure"));
            }
            if (editMode.equals(EditMode.LICENSE)) {
                JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.message.termsFailure"));
            }
            if (editMode.equals(EditMode.FILE)) {
                JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.message.filesFailure"));
            }
    }
    
    private String returnToLatestVersion(){
         dataset = datasetService.find(dataset.getId());
         workingVersion = dataset.getLatestVersion();
         if (workingVersion.isDeaccessioned() && dataset.getReleasedVersion() != null) {
         workingVersion = dataset.getReleasedVersion();
         }
         setVersionTabList(resetVersionTabList()); 
         setReleasedVersionTabList(resetReleasedVersionTabList());
         newFiles.clear();
         editMode = null;         
         return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&version="+ workingVersion.getFriendlyVersionNumber() +  "&faces-redirect=true";       
    }
    
    private String returnToDatasetOnly(){
         dataset = datasetService.find(dataset.getId());
         editMode = null;         
         return "/dataset.xhtml?persistentId=" + dataset.getGlobalId()  +  "&faces-redirect=true";       
    }
    
    private String returnToDraftVersion(){      
         return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&version=DRAFT" + "&faces-redirect=true";    
    }

    public String cancel() {
        return  returnToLatestVersion();
    }

    private HttpClient getClient() {
        // TODO: 
        // cache the http client? -- L.A. 4.0 alpha
        return new HttpClient();
    }

    public void refreshLock() {
        //RequestContext requestContext = RequestContext.getCurrentInstance();
        logger.fine("checking lock");
        if (isStillLocked()) {
            logger.fine("(still locked)");
        
        } else {
            // OK, the dataset is no longer locked. 
            // let's tell the page to refresh:
            logger.fine("no longer locked!");
            stateChanged = true;
            //requestContext.execute("refreshPage();");
        }
    }

    /* 
    */

    public boolean isLockedInProgress() {
        if (dataset != null) {
            logger.fine("checking lock status of dataset " + dataset.getId());
            if (dataset.isLocked()) {
                return true;
            }
        }
        return false;
    }
    

    public boolean isStillLocked() {
        if (dataset != null && dataset.getId() != null) {
            logger.fine("checking lock status of dataset " + dataset.getId());
            if (datasetService.checkDatasetLock(dataset.getId())) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isLocked() {
        if (stateChanged) {
            return false; 
        }
        
        if (dataset != null) {

            if (dataset.isLocked()) {
                return true;
            }
        }
        return false;
    }
    
    public void setLocked(boolean locked) {
        // empty method, so that we can use DatasetPage.locked in a hidden 
        // input on the page. 
    }
    
    public boolean isStateChanged() {
        return stateChanged;
    }
    
    public void setStateChanged(boolean stateChanged) {
        // empty method, so that we can use DatasetPage.stateChanged in a hidden 
        // input on the page. 
    }

    public DatasetVersionUI getDatasetVersionUI() {
        return datasetVersionUI;
    }

    
    public List<DatasetVersion> getVersionTabList() {
        return versionTabList;
    }
    
    public List<DatasetVersion> getVersionTabListForPostLoad(){
        return this.versionTabListForPostLoad;
    }

    public void setVersionTabListForPostLoad(List<DatasetVersion> versionTabListForPostLoad) {

        this.versionTabListForPostLoad = versionTabListForPostLoad;
    }
    
    public Integer getCompareVersionsCount() {
        Integer retVal = 0;
        for (DatasetVersion dvTest : dataset.getVersions()) {
            if (!dvTest.isDeaccessioned()) {
                retVal++;
            }
        }
        return retVal;
    }
    
    /**
     * To improve performance, Version Differences
     * are retrieved/calculated after the page load
     * 
     * See: dataset-versions.xhtml, remoteCommand 'postLoadVersionTablList'
    */
    public void postLoadSetVersionTabList(){

        if (this.getVersionTabList().isEmpty() && workingVersion.isDeaccessioned()){
            setVersionTabList(resetVersionTabList());
        }
        this.setVersionTabListForPostLoad(this.getVersionTabList());
        

        //this.versionTabList = this.resetVersionTabList();        
    }

    /**
     * 
     * 
     * @param versionTabList 
     */
    public void setVersionTabList(List<DatasetVersion> versionTabList) {
        
        this.versionTabList = versionTabList;
    }

    private List<DatasetVersion> releasedVersionTabList = new ArrayList();

    public List<DatasetVersion> getReleasedVersionTabList() {
        return releasedVersionTabList;
    }

    public void setReleasedVersionTabList(List<DatasetVersion> releasedVersionTabList) {
        this.releasedVersionTabList = releasedVersionTabList;
    }

    private List<DatasetVersion> selectedVersions;

    public List<DatasetVersion> getSelectedVersions() {
        return selectedVersions;
    }

    public void setSelectedVersions(List<DatasetVersion> selectedVersions) {
        this.selectedVersions = selectedVersions;
    }

    private List<DatasetVersion> selectedDeaccessionVersions;

    public List<DatasetVersion> getSelectedDeaccessionVersions() {
        return selectedDeaccessionVersions;
    }

    public void setSelectedDeaccessionVersions(List<DatasetVersion> selectedDeaccessionVersions) {
        this.selectedDeaccessionVersions = selectedDeaccessionVersions;
    }

    public DatasetVersionDifference getDatasetVersionDifference() {
        return datasetVersionDifference;
    }

    public void setDatasetVersionDifference(DatasetVersionDifference datasetVersionDifference) {
        this.datasetVersionDifference = datasetVersionDifference;
    }
        
    public void startMultipleFileDownload (Boolean writeGuestbook){

        fileDownloadService.callDownloadServlet(getDownloadableFilesIdsString(), writeGuestbook);

    }
 
    private String downloadType = "";

    public String getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(String downloadType) {
        this.downloadType = downloadType;
    }
    
    
    public void modifyGuestbookMultipleResponse(){
        if (this.selectedFiles.isEmpty()) {
            RequestContext requestContext = RequestContext.getCurrentInstance();
            requestContext.execute("PF('selectFilesForDownload').show()");
            return;
        }
        
         this.guestbookResponse = this.guestbookResponseService.modifySelectedFileIds(guestbookResponse, getSelectedDownloadableFilesIdsString());
         this.guestbookResponse.setDownloadtype("Download");
         this.guestbookResponse.setFileFormat("Download");
        RequestContext requestContext = RequestContext.getCurrentInstance();
        requestContext.execute("PF('downloadPopup').show();handleResizeDialog('downloadPopup');");
    }
    
    public void initGuestbookMultipleResponse(String selectedFileIds){
         initGuestbookResponse(null, "download", selectedFileIds);
    }

    public void initGuestbookResponse(FileMetadata fileMetadata, String downloadFormat, String selectedFileIds) {
        
        this.guestbookResponse = guestbookResponseService.initGuestbookResponse(fileMetadata, downloadFormat, selectedFileIds, session);
    }



    public void compareVersionDifferences() {
        RequestContext requestContext = RequestContext.getCurrentInstance();
        if (this.selectedVersions.size() != 2) {
            requestContext.execute("openCompareTwo();");
        } else {
            //order depends on order of selection - needs to be chronological order
            if (this.selectedVersions.get(0).getId().intValue() > this.selectedVersions.get(1).getId().intValue()) {
                updateVersionDifferences(this.selectedVersions.get(0), this.selectedVersions.get(1));
            } else {
                updateVersionDifferences(this.selectedVersions.get(1), this.selectedVersions.get(0));
            }
        }
    }

    public void updateVersionDifferences(DatasetVersion newVersion, DatasetVersion originalVersion) {
        if (originalVersion == null) {
            setDatasetVersionDifference(newVersion.getDefaultVersionDifference());
        } else {
            setDatasetVersionDifference(new DatasetVersionDifference(newVersion, originalVersion));
        }
    }
    
    
    

    private List<DatasetVersion> resetVersionTabList() {
        //if (true)return null;
        List<DatasetVersion> retList = new ArrayList();

        if (permissionService.on(dataset).has(Permission.ViewUnpublishedDataset)) {
            for (DatasetVersion version : dataset.getVersions()) {
                version.setContributorNames(getContributorsNames(version));
                retList.add(version);
            }

        } else {
            for (DatasetVersion version : dataset.getVersions()) {
                if (version.isReleased() || version.isDeaccessioned()) {
                    version.setContributorNames(getContributorsNames(version));
                    retList.add(version);
                }
            }
        }
        return retList;
    }

    private String getContributorsNames(DatasetVersion version) {
        String contNames = "";
        for (String id : version.getVersionContributorIdentifiers()) {
            id = id.startsWith("@") ? id.substring(1) : id;
            AuthenticatedUser au = authService.getAuthenticatedUser(id);
            if (au != null) {
                if (contNames.isEmpty()) {
                    contNames = au.getName();
                } else {
                    contNames = contNames + ", " + au.getName();
                }
            }
        }
        return contNames;
    }
    
    private boolean existReleasedVersion;

    public boolean isExistReleasedVersion() {
        return existReleasedVersion;
    }

    public void setExistReleasedVersion(boolean existReleasedVersion) {
        this.existReleasedVersion = existReleasedVersion;
    }
    
    private boolean resetExistRealeaseVersion(){

        for (DatasetVersion version : dataset.getVersions()) {
            if (version.isReleased() || version.isArchived()) {
                return true;
            }
        }
        return false;
        
    }

    private List<DatasetVersion> resetReleasedVersionTabList() {
        List<DatasetVersion> retList = new ArrayList();
        for (DatasetVersion version : dataset.getVersions()) {
            if (version.isReleased() || version.isArchived()) {
                retList.add(version);
            }
        }
        return retList;
    }

    public String getDatasetPublishCustomText(){
        String datasetPublishCustomText = settingsService.getValueForKey(SettingsServiceBean.Key.DatasetPublishPopupCustomText);
        if( datasetPublishCustomText!= null && !datasetPublishCustomText.isEmpty()){
            return datasetPublishCustomText;
            
        }
        return "";
    }
    
    public Boolean isDatasetPublishPopupCustomTextOnAllVersions(){
        return  settingsService.isTrueForKey(SettingsServiceBean.Key.DatasetPublishPopupCustomTextOnAllVersions, false);
    }

    public String getVariableMetadataURL(Long fileid) {
        String myHostURL = getDataverseSiteUrl();
        String metaURL = myHostURL + "/api/meta/datafile/" + fileid;

        return metaURL;
    }

    public String getTabularDataFileURL(Long fileid) {
        String myHostURL = getDataverseSiteUrl();
        String dataURL = myHostURL + "/api/access/datafile/" + fileid;

        return dataURL;
    }

    public String getMetadataAsJsonUrl() {
        if (dataset != null) {
            Long datasetId = dataset.getId();
            if (datasetId != null) {
                String myHostURL = getDataverseSiteUrl();
                String metadataAsJsonUrl = myHostURL + "/api/datasets/" + datasetId;
                return metadataAsJsonUrl;
            }
        }
        return null;
    }
    
    public List< String[]> getExporters(){
        List<String[]> retList = new ArrayList();
        String myHostURL = getDataverseSiteUrl();
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
                temp[1] = myHostURL + "/api/datasets/export?exporter=" + formatName + "&persistentId=" + dataset.getGlobalId();
                retList.add(temp);
            }
        }
        return retList;  
    }
    

    private FileMetadata fileMetadataSelected = null;

    public void  setFileMetadataSelected(FileMetadata fm){
       setFileMetadataSelected(fm, null); 
    }
    
    public void setFileMetadataSelected(FileMetadata fm, String guestbook) {
        if (guestbook != null) {
            if (guestbook.equals("create")) {
                //
                /*
                FIX ME guestbook entry for subsetting
                */
                
                
                
                
               // guestbookResponseService.createSilentGuestbookEntry(fm, "Subset");
            } else {
                initGuestbookResponse(fm, "Subset", null);
            }
        }

        fileMetadataSelected = fm;
        logger.fine("set the file for the advanced options popup (" + fileMetadataSelected.getLabel() + ")");
    }

    public FileMetadata getFileMetadataSelected() {
        if (fileMetadataSelected != null) {
            logger.fine("returning file metadata for the advanced options popup (" + fileMetadataSelected.getLabel() + ")");
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
                    if (fileMetadata.getDataFile().getOwner() != null) {
                        if (fileMetadata.getDataFile().equals(fileMetadata.getDataFile().getOwner().getThumbnailFile())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /* 
     * Items for the "Designated this image as the Dataset thumbnail: 
     */
    
    private FileMetadata fileMetadataSelectedForThumbnailPopup = null; 

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

        if (fileMetadataSelectedForThumbnailPopup != null) {
            if (fileMetadataSelectedForThumbnailPopup.getDataFile() != null) {
                if (fileMetadataSelectedForThumbnailPopup.getDataFile().getId() != null) {
                    if (fileMetadataSelectedForThumbnailPopup.getDataFile().getOwner() != null) {
                        if (fileMetadataSelectedForThumbnailPopup.getDataFile().equals(fileMetadataSelectedForThumbnailPopup.getDataFile().getOwner().getThumbnailFile())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    
    
    public void setUseAsDatasetThumbnail(boolean useAsThumbnail) {
        if (fileMetadataSelectedForThumbnailPopup != null) {
            if (fileMetadataSelectedForThumbnailPopup.getDataFile() != null) {
                if (fileMetadataSelectedForThumbnailPopup.getDataFile().getId() != null) { // ?
                    if (fileMetadataSelectedForThumbnailPopup.getDataFile().getOwner() != null) {
                        if (useAsThumbnail) {
                            fileMetadataSelectedForThumbnailPopup.getDataFile().getOwner().setThumbnailFile(fileMetadataSelectedForThumbnailPopup.getDataFile());
                        } else if (getUseAsDatasetThumbnail()) {
                            fileMetadataSelectedForThumbnailPopup.getDataFile().getOwner().setThumbnailFile(null);
                        }
                    }
                }
            }
        }
    }

    public void saveAsDesignatedThumbnail() {
        // We don't need to do anything specific to save this setting, because
        // the setUseAsDatasetThumbnail() method, above, has already updated the
        // file object appropriately. 
        // However, once the "save" button is pressed, we want to show a success message, if this is 
        // a new image has been designated as such:
        if (getUseAsDatasetThumbnail() && !alreadyDesignatedAsDatasetThumbnail) {
            String successMessage = JH.localize("file.assignedDataverseImage.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileMetadataSelectedForThumbnailPopup.getLabel());
            JsfHelper.addFlashMessage(successMessage);
        }
        
        // And reset the selected fileMetadata:
        
        fileMetadataSelectedForThumbnailPopup = null;
    }
    
    /* 
     * Items for the "Tags (Categories)" popup.
     *
     */
    private FileMetadata fileMetadataSelectedForTagsPopup = null; 
    
    public void  setFileMetadataSelectedForTagsPopup(){

    }

    public void  setFileMetadataSelectedForTagsPopup(FileMetadata fm){
       fileMetadataSelectedForTagsPopup = fm; 
    }
    
    public FileMetadata getFileMetadataSelectedForTagsPopup() {
        return fileMetadataSelectedForTagsPopup;
    }
    
    public void clearFileMetadataSelectedForTagsPopup() {
        fileMetadataSelectedForTagsPopup = null;
    }
    
    public List <FileMetadata> getListFileMetadataSelectedForTagsPopup(){
        List<FileMetadata> retList = new ArrayList();
        for (FileMetadata fm : selectedFiles){
            retList.add(fm);
        }
        return retList;       
    }
    
    private List<String> categoriesByName;
    
    public void setCategoriesByName(List<String>  dummy){
        categoriesByName = dummy;
    }
    
    public void refreshTagsPopUp(){
        if (bulkUpdateCheckVersion()){
           refreshSelectedFiles();           
        }  
        updateFileCounts();
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
    
    private void refreshCategoriesByName(){
        categoriesByName= new ArrayList<>();
        for (String category: dataset.getCategoriesByName() ){
            categoriesByName.add(category);
        }
        refreshSelectedTags();
    }
    
    
    
    
    public List<String> getCategoriesByName() {
            return categoriesByName;
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
    
    public void handleSelection(final AjaxBehaviorEvent event) {
        if (selectedTags != null) {
            selectedTags = selectedTags.clone();
        }
    }
        

    
    private void refreshTabFileTagsByName(){
        
        tabFileTagsByName= new ArrayList<>();
        for (FileMetadata fm : selectedFiles) {
            if (fm.getDataFile().getTags() != null) {
                for (int i = 0; i < fm.getDataFile().getTags().size(); i++) {
                    if (!tabFileTagsByName.contains(fm.getDataFile().getTags().get(i).getTypeLabel())) {
                        tabFileTagsByName.add(fm.getDataFile().getTags().get(i).getTypeLabel());
                    }
                }
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
    
    private boolean tabularDataSelected = false;

    public boolean isTabularDataSelected() {
        return tabularDataSelected;
    }

    public void setTabularDataSelected(boolean tabularDataSelected) {
        this.tabularDataSelected = tabularDataSelected;
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
    
    private void refreshSelectedTags() {
        selectedTags = null;
        selectedTags = new String[0];
        
        List selectedCategoriesByName= new ArrayList<>();
        for (FileMetadata fm : selectedFiles) {
            if (fm.getCategories() != null) {
                for (int i = 0; i < fm.getCategories().size(); i++) {
                    if (!selectedCategoriesByName.contains(fm.getCategories().get(i).getName())) {
                    selectedCategoriesByName.add(fm.getCategories().get(i).getName());
                    }

                }

            }
        }

        if (selectedCategoriesByName.size() > 0) {
            selectedTags = new String[selectedCategoriesByName.size()];
            for (int i = 0; i < selectedCategoriesByName.size(); i++) {
                selectedTags[i] = (String) selectedCategoriesByName.get(i);
            }
        }
        Arrays.sort(selectedTags);
    }
        
    /* This method handles saving both "tabular file tags" and 
     * "file categories" (which are also considered "tags" in 4.0)
    */
    public String saveFileTagsAndCategories() {
        // 1. New Category name:
        // With we don't need to do anything for the file categories that the user
        // selected from the pull down list; that was done directly from the 
        // page with the FileMetadata.setCategoriesByName() method. 
        // So here we only need to take care of the new, custom category
        // name, if entered: 
        if (bulkUpdateCheckVersion()) {
            refreshSelectedFiles();
        }
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            if (selectedFiles != null && selectedFiles.size() > 0) {
                for (FileMetadata fm : selectedFiles) {
                    if (fm.getDataFile().equals(fmd.getDataFile())) {
                        fmd.setCategories(new ArrayList());
                        if (newCategoryName != null) {
                            fmd.addCategoryByName(newCategoryName);
                        }
                        // 2. Tabular DataFile Tags: 
                        if (selectedTags != null) {
                            for (int i = 0; i < selectedTags.length; i++) {
                                fmd.addCategoryByName(selectedTags[i]);
                            }
                        }
                        if (fmd.getDataFile().isTabularData()) {
                            fmd.getDataFile().setTags(null);
                            for (int i = 0; i < selectedTabFileTags.length; i++) {

                                DataFileTag tag = new DataFileTag();
                                try {
                                    tag.setTypeByLabel(selectedTabFileTags[i]);
                                    tag.setDataFile(fmd.getDataFile());
                                    fmd.getDataFile().addTag(tag);

                                } catch (IllegalArgumentException iax) {
                                    // ignore 
                                }
                            }
                        }
                    }
                }
            }
        }
               // success message: 
                String successMessage = JH.localize("file.assignedTabFileTags.success");
                logger.fine(successMessage);
                successMessage = successMessage.replace("{0}", "Selected Files");
                JsfHelper.addFlashMessage(successMessage);
         selectedTags = null;

        logger.fine("New category name: " + newCategoryName);

        newCategoryName = null;
        
        if (removeUnusedTags){
            removeUnusedFileTagsFromDataset();
        }
        save();
        return  returnToDraftVersion();
    }
    
    /*
    Remove unused file tags
    When updating datafile tags see if any custom tags are not in use.
    Remove them
    
    */
    private void removeUnusedFileTagsFromDataset() {
        categoriesByName = new ArrayList<>();
        for (FileMetadata fm : workingVersion.getFileMetadatas()) {
            if (fm.getCategories() != null) {
                for (int i = 0; i < fm.getCategories().size(); i++) {
                    if (!categoriesByName.contains(fm.getCategories().get(i).getName())) {
                        categoriesByName.add(fm.getCategories().get(i).getName());
                    }
                }
            }
        }
        List<DataFileCategory> datasetFileCategoriesToRemove = new ArrayList();

        for (DataFileCategory test : dataset.getCategories()) {
            boolean remove = true;
            for (String catByName : categoriesByName) {
                if (catByName.equals(test.getName())) {
                    remove = false;
                    break;
                }
            }
            if (remove) {
                datasetFileCategoriesToRemove.add(test);
            }
        }

        if (!datasetFileCategoriesToRemove.isEmpty()) {
            for (DataFileCategory remove : datasetFileCategoriesToRemove) {
                dataset.getCategories().remove(remove);
            }

        }

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
            return "UTF8 (default)";
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
                logger.warning("the file " + file.getFileName() + " failed to upload!");
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, "upload failure", "the file " + file.getFileName() + " failed to upload!");
                FacesContext.getCurrentInstance().addMessage(null, message);
                return;
            }

            savedLabelsTempFile = saveTempFile(uploadStream);

            logger.fine(file.getFileName() + " is successfully uploaded.");
            FacesMessage message = new FacesMessage("Succesful", file.getFileName() + " is uploaded.");
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
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
            return null;
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

    
    public boolean isDownloadButtonAvailable(){
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            if (this.fileDownloadHelper.canDownloadFile(fmd)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isFileAccessRequestMultiButtonRequired(){
        if (!isSessionUserAuthenticated() || !dataset.isFileAccessRequest()){
            return false;
        }
        if (workingVersion == null) {
            return false;
        }
        if (!workingVersion.getTermsOfUseAndAccess().isFileAccessRequest()){
           // return false;
        }
        for (FileMetadata fmd : workingVersion.getFileMetadatas()){
            if (!this.fileDownloadHelper.canDownloadFile(fmd)){
                return true;               
            }
        }
        return false;
    }

    public boolean isFileAccessRequestMultiButtonEnabled(){
        if (!isSessionUserAuthenticated() || !dataset.isFileAccessRequest()){
            return false;
        }
        if( this.selectedRestrictedFiles == null || this.selectedRestrictedFiles.isEmpty() ){
            return false;
        }
        for (FileMetadata fmd : this.selectedRestrictedFiles){
            if (!this.fileDownloadHelper.canDownloadFile(fmd)){
                return true;               
            }
        }
        return false;
    } 
    
    private Boolean downloadButtonAllEnabled = null;

    public boolean isDownloadAllButtonEnabled() {

        if (downloadButtonAllEnabled == null) {
            for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                if (!this.fileDownloadHelper.canDownloadFile(fmd)) {
                    downloadButtonAllEnabled = false;
                    break;
                }
            }
            downloadButtonAllEnabled = true;
        }
        return downloadButtonAllEnabled;
    }

    public boolean isDownloadSelectedButtonEnabled(){

        if( this.selectedFiles == null || this.selectedFiles.isEmpty() ){
            return false;
        }
        for (FileMetadata fmd : this.selectedFiles){
            if (this.fileDownloadHelper.canDownloadFile(fmd)){
                return true;               
            }
        }
        return false;
    } 
    
    public boolean isFileAccessRequestMultiSignUpButtonRequired(){
        if (isSessionUserAuthenticated()){
            return false;
        }
        // only show button if dataset allows an access request
        if (!dataset.isFileAccessRequest()){
            return false;
        }
        for (FileMetadata fmd : workingVersion.getFileMetadatas()){
            if (!this.fileDownloadHelper.canDownloadFile(fmd)){
                return true;               
            }
        }
        return false;
    }

    public boolean isFileAccessRequestMultiSignUpButtonEnabled(){
        if (isSessionUserAuthenticated()){
            return false;
        }
        if( this.selectedRestrictedFiles == null || this.selectedRestrictedFiles.isEmpty() ){
            return false;
        }
        // only show button if dataset allows an access request
        if (!dataset.isFileAccessRequest()){
            return false;
        }
        for (FileMetadata fmd : this.selectedRestrictedFiles){
            if (!this.fileDownloadHelper.canDownloadFile(fmd)){
                return true;               
            }
        }
        return false;
    }

    public boolean isDownloadPopupRequired() {
        return fileDownloadService.isDownloadPopupRequired(workingVersion);
    }
    
   public String requestAccessMultipleFiles(String fileIdString) {
            if (fileIdString.isEmpty()) {
            RequestContext requestContext = RequestContext.getCurrentInstance();
            requestContext.execute("PF('selectFilesForRequestAccess').show()");
            return "";
        }
       
        Long idForNotification = new Long(0);
        if (fileIdString != null) {
            String[] ids = fileIdString.split(",");
            for (String id : ids) {
                Long test = null;
                try {
                    test = new Long(id);
                } catch (NumberFormatException nfe) {
                    // do nothing...
                    test = null;
                }
                if (test != null) {
                    DataFile request = datafileService.find(test);
                    idForNotification = test;
                    requestAccess(request, false);
                }
            }
        }
        if (idForNotification.intValue() > 0) {
            sendRequestFileAccessNotification(idForNotification);
        }
        return returnToDatasetOnly();
    }

    public void requestAccess(DataFile file, boolean sendNotification) {       
        if (!file.getFileAccessRequesters().contains((AuthenticatedUser) session.getUser())) {
            file.getFileAccessRequesters().add((AuthenticatedUser) session.getUser());
            datafileService.save(file);

            // create notifications
            if (sendNotification) {
                sendRequestFileAccessNotification(file.getId());

            }
        }
    }

    private void sendRequestFileAccessNotification(Long fileId) {
        for (AuthenticatedUser au : permissionService.getUsersWithPermissionOn(Permission.ManageDatasetPermissions, dataset)) {
            userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.REQUESTFILEACCESS, fileId);
        }

    }

    public boolean isSortButtonEnabled() {
        /**
         * @todo The "Sort" Button seems to stop responding to mouse clicks
         * after a while so it can't be shipped in 4.2 and will be deferred, to
         * be picked up in https://github.com/IQSS/dataverse/issues/2506
         */
        return false;
    }

    public void updateFileListing(String fileSortField, String fileSortOrder) {
        this.fileSortField = fileSortField;
        this.fileSortOrder = fileSortOrder;
        fileMetadatas = populateFileMetadatas();
    }

    private List<FileMetadata> populateFileMetadatas() {
        if (isSortButtonEnabled()) {
            List<FileMetadata> fileMetadatasToSet = new ArrayList<>();
            Long datasetVersion = workingVersion.getId();
            if (datasetVersion != null) {
                int unlimited = 0;
                int maxResults = unlimited;
                List<FileMetadata> dataFilesNew = datafileService.findFileMetadataByDatasetVersionId(datasetVersion, maxResults, fileSortField, fileSortOrder);
                fileMetadatasToSet.addAll(dataFilesNew);
            }
            return fileMetadatasToSet;
        } else {
            return new ArrayList<>();
        }
    }

    public String getFileSortField() {
        return fileSortField;
    }

    public void setFileSortField(String fileSortField) {
        this.fileSortField = fileSortField;
    }

    public String getFileSortOrder() {
        return fileSortOrder;
    }

    public void setFileSortOrder(String fileSortOrder) {
        this.fileSortOrder = fileSortOrder;
    }

    public List<FileMetadata> getFileMetadatas() {
        if (isSortButtonEnabled()) {
            return fileMetadatas;
        } else {
            return new ArrayList<>();
        }
    }

    public String getFileSortFieldName() {
        return FileSortFieldAndOrder.label;
    }

    public String getFileSortFieldDate() {
        return FileSortFieldAndOrder.createDate;
    }

    public String getFileSortFieldSize() {
        return FileSortFieldAndOrder.size;
    }

    public String getFileSortFieldType() {
        return FileSortFieldAndOrder.type;
    }

    public String getSortByAscending() {
        return SortBy.ASCENDING;
    }

    public String getSortByDescending() {
        return SortBy.DESCENDING;
    }

    PrivateUrl privateUrl;

    public PrivateUrl getPrivateUrl() {
        return privateUrl;
    }

    public void setPrivateUrl(PrivateUrl privateUrl) {
        this.privateUrl = privateUrl;
    }

    public void initPrivateUrlPopUp() {
        if (privateUrl != null) {
            setPrivateUrlJustCreatedToFalse();
        }
    }

    boolean privateUrlWasJustCreated;

    public boolean isPrivateUrlWasJustCreated() {
        return privateUrlWasJustCreated;
    }

    public void setPrivateUrlJustCreatedToFalse() {
        privateUrlWasJustCreated = false;
    }

    public void createPrivateUrl() {
        try {
            PrivateUrl createdPrivateUrl = commandEngine.submit(new CreatePrivateUrlCommand(dvRequestService.getDataverseRequest(), dataset));
            privateUrl = createdPrivateUrl;
            JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.privateurl.infoMessageAuthor", Arrays.asList(getPrivateUrlLink(privateUrl))));
            privateUrlWasJustCreated = true;
        } catch (CommandException ex) {
            String msg = BundleUtil.getStringFromBundle("dataset.privateurl.noPermToCreate", PrivateUrlUtil.getRequiredPermissions(ex));
            logger.info("Unable to create a Private URL for dataset id " + dataset.getId() + ". Message to user: " + msg + " Exception: " + ex);
            JH.addErrorMessage(msg);
        }
    }

    public void disablePrivateUrl() {
        try {
            commandEngine.submit(new DeletePrivateUrlCommand(dvRequestService.getDataverseRequest(), dataset));
            privateUrl = null;
            JH.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.privateurl.disabledSuccess"));
        } catch (CommandException ex) {
            logger.info("CommandException caught calling DeletePrivateUrlCommand: " + ex);
        }
    }

    public boolean isUserCanCreatePrivateURL() {
        return dataset.getLatestVersion().isDraft();
    }

    public String getPrivateUrlLink(PrivateUrl privateUrl) {
        return privateUrl.getLink();
    }
    
    
    public FileDownloadHelper getFileDownloadHelper() {
        return fileDownloadHelper;
    }

    public void setFileDownloadHelper(FileDownloadHelper fileDownloadHelper) {
        this.fileDownloadHelper = fileDownloadHelper;
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
    
    
    public WorldMapPermissionHelper getWorldMapPermissionHelper() {
        return worldMapPermissionHelper;
    }

    public void setWorldMapPermissionHelper(WorldMapPermissionHelper worldMapPermissionHelper) {
        this.worldMapPermissionHelper = worldMapPermissionHelper;
    }

    public TwoRavensHelper getTwoRavensHelper() {
        return twoRavensHelper;
    }

    public void setTwoRavensHelper(TwoRavensHelper twoRavensHelper) {
        this.twoRavensHelper = twoRavensHelper;
    }

}
