package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookResponseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeaccessionDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.metadataimport.ForeignMetadataImportServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
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
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.primefaces.context.RequestContext;
import java.text.DateFormat;
import javax.faces.model.SelectItem;
import java.util.HashSet;
import java.util.logging.Level;
import javax.faces.component.UIInput;

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
    GuestbookResponseServiceBean guestbookServiceBean;
    @EJB
    DataverseLinkingServiceBean dvLinkingService;
    @EJB
    DatasetLinkingServiceBean dsLinkingService;
    @Inject
    DatasetVersionUI datasetVersionUI;

    private static final DateFormat displayDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

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

    private List<Dataverse> dataversesForLinking = new ArrayList();
    private Long linkingDataverseId;
    private List<SelectItem> linkingDVSelectItems;
    private Dataverse linkingDataverse;
    
    // Version tab lists
    private List<DatasetVersion> versionTabList = new ArrayList();
    private List<DatasetVersion> versionTabListForPostLoad = new ArrayList();

    
    // Used to store results of permissions checks
    private Map<String, Boolean> datasetPermissionMap = new HashMap<>(); // { Permission human_name : Boolean }
    private Map<Long, Boolean> fileDownloadPermissionMap = new HashMap<>(); // { FileMetadata.id : Boolean }

    private DataFile selectedDownloadFile;

    private Long maxFileUploadSizeInBytes = null;
    
    
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
    
    public DataFile getSelectedDownloadFile() {
        return selectedDownloadFile;
    }

    public void setSelectedDownloadFile(DataFile selectedDownloadFile) {
        this.selectedDownloadFile = selectedDownloadFile;
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
    
    /**
     * Convenience method for "Download File" button display logic
     * 
     * Used by the dataset.xhtml render logic when listing files
     *       > Assume user already has view access to the file list
     * 
     * @param fileMetadata
     * @return boolean
     */
    public boolean canDownloadFile(FileMetadata fileMetadata){
      
        if (fileMetadata == null){
            return false;
        }
       
        if ((fileMetadata.getId() == null) || (fileMetadata.getDataFile().getId() == null)){
            return false;
        } 
        
        // --------------------------------------------------------------------        
        // Grab the fileMetadata.id and restriction flag                
        // --------------------------------------------------------------------
        Long fid = fileMetadata.getId();
        boolean isRestrictedFile = fileMetadata.isRestricted();
        
        
        // --------------------------------------------------------------------
        // Has this file been checked? Look at the DatasetPage hash
        // --------------------------------------------------------------------
        if (this.fileDownloadPermissionMap.containsKey(fid)){
            // Yes, return previous answer
            return this.fileDownloadPermissionMap.get(fid);
        }

        // --------------------------------------------------------------------
        // (1) Is the file Unrestricted ?        
        // --------------------------------------------------------------------
        if (!isRestrictedFile){
            // Yes, save answer and return true
            this.fileDownloadPermissionMap.put(fid, true);
            return true;
        }
        
        // --------------------------------------------------------------------
        // Conditions (2) through (4) are for Restricted files
        // --------------------------------------------------------------------
        
        // --------------------------------------------------------------------
        // (2) Is user authenticated?
        // No?  Then no button...
        // --------------------------------------------------------------------
        if (!(this.session.getUser().isAuthenticated())){
            this.fileDownloadPermissionMap.put(fid, false);
            return false;
        }
        
        // --------------------------------------------------------------------
        // (3) Does the User have DownloadFile Permission at the **Dataset** level 
        // --------------------------------------------------------------------
        if (this.doesSessionUserHaveDataSetPermission(Permission.DownloadFile)){
            // Yes, save answer and return true
            this.fileDownloadPermissionMap.put(fid, true);
            return true;
        }
  
        // --------------------------------------------------------------------
        // (4) Does the user has DownloadFile permission on the DataFile            
        // --------------------------------------------------------------------
        if (this.permissionService.on(fileMetadata.getDataFile()).has(Permission.DownloadFile)){
            this.fileDownloadPermissionMap.put(fid, true);
            return true;
        }
        
        // --------------------------------------------------------------------
        // (6) No download....
        // --------------------------------------------------------------------
        this.fileDownloadPermissionMap.put(fid, false);
       
        return false;
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

    public Template getSelectedTemplate() {;
        return selectedTemplate;
    }

    public void setSelectedTemplate(Template selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }

    public void updateSelectedTemplate(ValueChangeEvent event) {

        selectedTemplate = (Template) event.getNewValue();
        if (selectedTemplate != null) {
            workingVersion = dataset.getEditVersion(selectedTemplate);
            updateDatasetFieldInputLevels();
        } else {
            dataset = new Dataset();
            dataset.setOwner(dataverseService.find(ownerId));
            workingVersion = dataset.getCreateVersion();
            updateDatasetFieldInputLevels();

            dataset.setIdentifier(datasetService.generateIdentifierSequence(protocol, authority, separator));
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
        
         if (!dataverseService.find(ownerId).isMetadataBlockRoot()) {
             dvIdForInputLevel = dataverseService.find(ownerId).getMetadataRootId();
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
        System.out.print("handle change");
        System.out.print("new value " + selectedTemplate.getId());
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
        //System.out.println(s);
    }
    
    /**
     *  See table in: https://github.com/IQSS/dataverse/issues/1618
     * 
     *  Can the user see a reminder to publish button?
     * 
     *   (1) Logged in user
     *   (2) Is geospatial file?
     *   (3) File has NOT been released
     *   (4) No existing Map
     *   (5) Can Edit Dataset
     *   
     * @param FileMetadata fm
     * @return boolean
     */
    public boolean canSeeMapButtonReminderToPublish(FileMetadata fm){
        if (fm==null){

            return false;
        }       
        
        // (1) Is there an authenticated user?
        //
        if (!(this.session.getUser().isAuthenticated())){
            return false;
        }

        // (2) Is this file a Shapefile 
        //  TODO: or a Tabular file tagged as Geospatial?
        //
        if (!(this.isShapefileType(fm))){
              return false;
        }

        // (3) Is this DataFile released?  Yes, don't need reminder
        //
        if (fm.getDataFile().isReleased()){
            return false;
        }
        
        // (4) Does a map already exist?  Yes, don't need reminder
        //
        if (this.hasMapLayerMetadata(fm)){
            return false;
        }

        // (5) If so, can the logged in user edit the Dataset to which this FileMetadata belongs?
        if (!this.doesSessionUserHaveDataSetPermission(Permission.EditDataset)){
            return false;
        }

        // Looks good
        //
        return true;
    }
    
     /**
     * Should there be a Map Data Button for this file?
     *  see table in: https://github.com/IQSS/dataverse/issues/1618
     *  (1) Is the user logged in?
     *  (2) Is this file a Shapefile or a Tabular file tagged as Geospatial?
     *  (3) Does the logged in user have permission to edit the Dataset to which this FileMetadata belongs?
     *  (4) Any of these conditions:
     *        9a) File Published 
     *        (b) Draft: File Previously published  
     * @param fm FileMetadata
     * @return boolean
     */
    public boolean canUserSeeMapDataButton(FileMetadata fm){

        if (fm==null){
            return false;
        }  
        
        
        // (1) Is there an authenticated user?
        if (!(this.session.getUser().isAuthenticated())){
            return false;
        }

        //  (2) Is this file a Shapefile or a Tabular file tagged as Geospatial?
        //  TO DO:  EXPAND FOR TABULAR FILES TAGGED AS GEOSPATIAL!
        //
        if (!(this.isShapefileType(fm))){
            return false;
        }

        //  (3) Does the user have Edit Dataset permissions?
        //
        if (!this.doesSessionUserHaveDataSetPermission(Permission.EditDataset)){
            return false;
        }
             
        //  (4) Is File released?
        //
        if (fm.getDataFile().isReleased()){
            return true;
        }
        
        // Nope
        return false;
    }
    
    /**
     * Should there be a Explore WorldMap Button for this file?
     *   See table in: https://github.com/IQSS/dataverse/issues/1618
     * 
     *  (1) Does the file have MapLayerMetadata?
     *  (2) Is there DownloadFile permission for this file?
     * 
     * @param fm FileMetadata
     * @return boolean
     */
    public boolean canUserSeeExploreWorldMapButton(FileMetadata fm){
        if (fm==null){
            return false;
        }
        
        /* -----------------------------------------------------
           Does a Map Exist?
         ----------------------------------------------------- */
        if (!(this.hasMapLayerMetadata(fm))){
            // Nope: no button
            return false;
        }
                
        
        /* -----------------------------------------------------
            Does user have DownloadFile permission for this file?
             Yes: User can view button!
         ----------------------------------------------------- */                    
        if (this.canDownloadFile(fm)){
            return true;
        }
                      
        // Nope: Can't see button
        //
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
            return;
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

    
   public String init() {
        // System.out.println("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes
        String nonNullDefaultIfKeyNotFound = "";
        this.maxFileUploadSizeInBytes = systemConfig.getMaxFileUploadSize();
        
        guestbookResponse = new GuestbookResponse();
        protocol = settingsService.getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        authority = settingsService.getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
        separator = settingsService.getValueForKey(SettingsServiceBean.Key.DoiSeparator, nonNullDefaultIfKeyNotFound);
        if (dataset.getId() != null || versionId != null || persistentId != null) { // view mode for a dataset     
          
            
           DatasetVersionServiceBean.RetrieveDatasetVersionResponse retrieveDatasetVersionResponse = null;
           
            // ---------------------------------------
            // Set the workingVersion and Dataset
            // ---------------------------------------           
            if (persistentId != null) {
                // Set Working Version and Dataset by PersistentID
               retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByPersistentId(persistentId, version);                     

            } else if (dataset.getId() != null){
               // Set Working Version and Dataset by Datasaet Id and Version
               retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionById(dataset.getId(), version);

            } else if (versionId != null) {
               // Set Working Version and Dataset by DatasaetVersion Id
               retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByVersionId(versionId);                     
              
            } else if (retrieveDatasetVersionResponse == null){
               return "/404.xhtml";
            }
           
                   
            if (retrieveDatasetVersionResponse == null){
               return "/404.xhtml";
            }

           this.workingVersion = retrieveDatasetVersionResponse.getDatasetVersion();
           this.dataset = this.workingVersion.getDataset();
           
           // end: Set the workingVersion and Dataset
           // ---------------------------------------
           
           
           // Is the DatasetVersion or Dataset null?
           //
           if (workingVersion == null || this.dataset == null){
               return "/404.xhtml";
           }
           
           // Is the Dataset harvested?
           if (dataset.isHarvested()) {
               return "/404.xhtml";
           }

           // If this DatasetVersion is unpublished and permission is doesn't have permissions:
           //  > Go to the Login page
           //
           if (!(workingVersion.isReleased() || workingVersion.isDeaccessioned()) && !permissionService.on(dataset).has(Permission.ViewUnpublishedDataset)) {
               return "/loginpage.xhtml" + DataverseHeaderFragment.getRedirectPage();
           }
         
           if (!retrieveDatasetVersionResponse.wasRequestedVersionRetrieved()){
              //msg("checkit " + retrieveDatasetVersionResponse.getDifferentVersionMessage());
              JsfHelper.addWarningMessage(retrieveDatasetVersionResponse.getDifferentVersionMessage());//JH.localize("dataset.message.metadataSuccess"));
           }

            ownerId = dataset.getOwner().getId();
            datasetNextMajorVersion = this.dataset.getNextMajorVersionString();
            datasetNextMinorVersion = this.dataset.getNextMinorVersionString();
            datasetVersionUI = datasetVersionUI.initDatasetVersionUI(workingVersion, false);
            updateDatasetFieldInputLevels();
            displayCitation = dataset.getCitation(true, workingVersion);
            setVersionTabList(resetVersionTabList());  
            setReleasedVersionTabList(resetReleasedVersionTabList());

            // populate MapLayerMetadata
            this.loadMapLayerMetadataLookup();  // A DataFile may have a related MapLayerMetadata object

        } else if (ownerId != null) {
            // create mode for a new child dataset
            editMode = EditMode.CREATE;
            dataset.setOwner(dataverseService.find(ownerId));
            dataset.setProtocol(protocol);
            dataset.setAuthority(authority);
            dataset.setDoiSeparator(separator);
            dataset.setIdentifier(datasetService.generateIdentifierSequence(protocol, authority, separator));

            if (dataset.getOwner() == null) {
                return "/404.xhtml";
            } else if (!permissionService.on(dataset.getOwner()).has(Permission.AddDataset)) {
                return "/loginpage.xhtml" + DataverseHeaderFragment.getRedirectPage();
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
            return "/404.xhtml";
        }

        return null;
    }
    

    public String saveGuestbookResponse(String type) {
        boolean valid = true;
        if (dataset.getGuestbook() != null) {
            if (dataset.getGuestbook().isNameRequired()) {
                if (this.guestbookResponse.getName() == null) {
                    valid = false;
                } else {
                    valid &= !this.guestbookResponse.getName().isEmpty();
                }
            }
            if (dataset.getGuestbook().isEmailRequired()) {
                if (this.guestbookResponse.getEmail() == null) {
                    valid = false;
                } else {
                    valid &= !this.guestbookResponse.getEmail().isEmpty();
                }
            }
            if (dataset.getGuestbook().isInstitutionRequired()) {
                if (this.guestbookResponse.getInstitution() == null) {
                    valid = false;
                } else {
                    valid &= !this.guestbookResponse.getInstitution().isEmpty();
                }
            }
            if (dataset.getGuestbook().isPositionRequired()) {
                if (this.guestbookResponse.getPosition() == null) {
                    valid = false;
                } else {
                    valid &= !this.guestbookResponse.getPosition().isEmpty();
                }
            }
        }

        if (dataset.getGuestbook() != null && !dataset.getGuestbook().getCustomQuestions().isEmpty()) {
            for (CustomQuestion cq : dataset.getGuestbook().getCustomQuestions()) {
                if (cq.isRequired()) {
                    for (CustomQuestionResponse cqr : this.guestbookResponse.getCustomQuestionResponses()) {
                        if (cqr.getCustomQuestion().equals(cq)) {
                            valid &= (cqr.getResponse() != null && !cqr.getResponse().isEmpty());
                        }
                    }
                }
            }
        }

        if (!valid) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "Please complete required fields for download and re-try."));
            return "";
        }
            
        Command cmd;
        try {
            if (this.guestbookResponse != null) {
                if (!type.equals("multiple")) {
                    cmd = new CreateGuestbookResponseCommand(session.getUser(), this.guestbookResponse, dataset);
                    commandEngine.submit(cmd);
                } else {
                    for (FileMetadata fmd : this.selectedFiles) {
                        DataFile df = fmd.getDataFile();
                        if (df != null) {
                            this.guestbookResponse.setDataFile(df);
                            cmd = new CreateGuestbookResponseCommand(session.getUser(), this.guestbookResponse, dataset);
                            commandEngine.submit(cmd);
                        }
                    }
                }
            }
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Guestbook Response Save Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        
        if (type.equals("multiple")){
            //return callDownloadServlet(getSelectedFilesIdsString());
            callDownloadServlet(getSelectedFilesIdsString());
        }
       
        if ((type.equals("download") || type.isEmpty())) {
            //return callDownloadServlet(downloadFormat, this.selectedDownloadFile.getId());
            callDownloadServlet(downloadFormat, this.selectedDownloadFile.getId());
        }

        if (type.equals("explore")) {
            String retVal = getDataExploreURLComplete(this.selectedDownloadFile.getId());
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(retVal);
                return retVal;
            } catch (IOException ex) {
                logger.info("Failed to issue a redirect to file download url.");
            }
        }
        return "";
    }
    
    public String exploreOutputLink(FileMetadata fm, String type){
        createSilentGuestbookEntry(fm, type);
        String retVal = getDataExploreURLComplete(fm.getDataFile().getId());
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(retVal);
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url.");
        }
        return "";
    }
    
    //private String callDownloadServlet(String multiFileString){
    private void callDownloadServlet(String multiFileString){

        String fileDownloadUrl = "/api/access/datafiles/" + multiFileString;
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(fileDownloadUrl);
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url.");
        }

        //return fileDownloadUrl;
    }
    
    //private String callDownloadServlet( String downloadType, Long fileId){
    private void callDownloadServlet( String downloadType, Long fileId){
        
        String fileDownloadUrl = "/api/access/datafile/" + fileId;
                    
        if (downloadType != null && downloadType.equals("bundle")){
            fileDownloadUrl = "/api/access/datafile/bundle/" + this.selectedDownloadFile.getId();
        }
        if (downloadType != null && downloadType.equals("original")){
            fileDownloadUrl = "/api/access/datafile/" + this.selectedDownloadFile.getId() + "?format=original";
        }
        if (downloadType != null && downloadType.equals("RData")){
            fileDownloadUrl = "/api/access/datafile/" + this.selectedDownloadFile.getId() + "?format=RData";
        }
        if (downloadType != null && downloadType.equals("var")){
            fileDownloadUrl = "/api/meta/datafile/" + this.selectedDownloadFile.getId();
        }
        if (downloadType != null && downloadType.equals("tab")){
            fileDownloadUrl = "/api/access/datafile/" + this.selectedDownloadFile.getId()+ "?format=tab";
        }
                    logger.fine("Returning file download url: " + fileDownloadUrl);
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(fileDownloadUrl);
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url.");
        }
        //return fileDownloadUrl;       
    }

    public String getApiTokenKey() {
        ApiToken apiToken;

        if (session.getUser() == null) {
            // ?
            return null;
        }
        if (session.getUser().isAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();
            apiToken = authService.findApiTokenByUser(au);
            if (apiToken != null) {
                return "key=" + apiToken.getTokenString();
            }
            // Generate if not available?
            // Or should it just be generated inside the authService
            // automatically? 
            apiToken = authService.generateApiTokenForUser(au);
            if (apiToken != null) {
                return "key=" + apiToken.getTokenString();
            }
        }
        return "";

    }

    private void resetVersionUI() {
        
        datasetVersionUI = datasetVersionUI.initDatasetVersionUI(workingVersion, true);
        if (session.getUser().isAuthenticated()) {
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

    public void edit(EditMode editMode) {
        this.editMode = editMode;
        workingVersion = dataset.getEditVersion();

        if (editMode == EditMode.INFO) {
            // ?
        } else if (editMode == EditMode.FILE) {
            JH.addMessage(FacesMessage.SEVERITY_INFO, JH.localize("dataset.message.editFiles"));
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
            cmd = new UpdateDatasetCommand(dataset, session.getUser());
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
            cmd = new UpdateDatasetCommand(dataset, session.getUser());
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
            PublishDataverseCommand cmd = new PublishDataverseCommand((AuthenticatedUser) session.getUser(), dataset.getOwner());
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
                        cmd = new DeaccessionDatasetVersionCommand(session.getUser(), setDatasetVersionDeaccessionReasonAndURL(dv), true);
                        DatasetVersion datasetv = commandEngine.submit(cmd);
                    }
                }
            } else {
                for (DatasetVersion dv : selectedDeaccessionVersions) {
                    cmd = new DeaccessionDatasetVersionCommand(session.getUser(), setDatasetVersionDeaccessionReasonAndURL(dv), false);
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
        if (session.getUser() instanceof AuthenticatedUser) {
            try {
                if (editMode == EditMode.CREATE) {
                    cmd = new PublishDatasetCommand(dataset, (AuthenticatedUser) session.getUser(), minor);
                } else {
                    cmd = new PublishDatasetCommand(dataset, (AuthenticatedUser) session.getUser(), minor);
                }
                dataset = commandEngine.submit(cmd);
                JsfHelper.addSuccessMessage(JH.localize("dataset.message.publishSuccess"));                        
                if (workingVersion.isInReview()) {
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
                JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.message.publishFailure"));
                logger.severe(ex.getMessage());
            }
        } else {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "Only authenticated users can release Datasets.");
        }
        return returnToDatasetOnly();
    }

    public String registerDataset() {
        Command<Dataset> cmd;
        try {
            cmd = new UpdateDatasetCommand(dataset, session.getUser());
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

    // some experimental code below - commented out, for now;
    
    public void refresh() { // String flashmessage) { 
        logger.fine("refreshing");
        
        //if (flashmessage != null) {
        //    logger.info("flash message: "+flashmessage);
        //}
        
        // refresh the working copy of the Dataset and DatasetVersion:
        dataset = datasetService.find(dataset.getId());

        logger.fine("refreshing working version");
        if (versionId == null) {
            if (editMode == EditMode.FILE) {
                workingVersion = dataset.getEditVersion();
            } else {
                if (!dataset.isReleased()) {
                    workingVersion = dataset.getLatestVersion();
                } else {
                    workingVersion = dataset.getReleasedVersion();
                }
            }
        } else {
            logger.fine("refreshing working version, from version id.");
            workingVersion = datasetVersionService.find(versionId);
        }
        displayCitation = dataset.getCitation(false, workingVersion);
        JsfHelper.addSuccessMessage(JH.localize("dataset.message.files.ingestSuccess"));
    }

    public String deleteDataset() {

        Command cmd;
        try {
            cmd = new DestroyDatasetCommand(dataset, session.getUser());
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

    public String deleteDatasetVersion() {
        Command cmd;
        try {
            cmd = new DeleteDatasetVersionCommand(session.getUser(), dataset);
            commandEngine.submit(cmd);
            JsfHelper.addSuccessMessage(JH.localize("datasetVersion.message.deleteSuccess"));
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.message.deleteFailure"));
            logger.severe(ex.getMessage());
        }

        return returnToDatasetOnly();
    }

    private List<FileMetadata> selectedFiles; // = new ArrayList<>();

    public List<FileMetadata> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<FileMetadata> selectedFiles) {
        this.selectedFiles = selectedFiles;
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

    public String saveLinkedDataset() {
        if (linkingDataverseId == null) {
            JsfHelper.addFlashMessage("You must select a linking dataverse.");
            return "";
        }
        linkingDataverse = dataverseService.find(linkingDataverseId);
        LinkDatasetCommand cmd = new LinkDatasetCommand(session.getUser(), linkingDataverse, dataset);
        try {
            commandEngine.submit(cmd);
            JsfHelper.addFlashMessage("This dataset is now linked to " + linkingDataverse.getDisplayName());
            //JsfHelper.addSuccessMessage(JH.localize("dataset.message.linkSuccess")+ linkingDataverse.getDisplayName());
            //return "";

        } catch (CommandException ex) {
            String msg = "There was a problem linking this dataset to yours: " + ex;
            System.out.print("in catch exception... " + ex);
            logger.severe(msg);
            /**
             * @todo how do we get this message to show up in the GUI?
             */
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetNotLinked", msg);
            FacesContext.getCurrentInstance().addMessage(null, message);
            //return "";

        }
        return returnToLatestVersion();
    }

    List<FileMetadata> previouslyRestrictedFiles = null;
    
    public boolean isShowAccessPopup() {
        if (previouslyRestrictedFiles != null) {
            for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                if (fmd.isRestricted() && !previouslyRestrictedFiles.contains(fmd)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public void setShowAccessPopup(boolean showAccessPopup) {} // dummy set method


    public void restrictFiles(boolean restricted) {
        // since we are restricted files, first set the previously restricted file list, so we can compare for
        // determinin whether to show the access popup
        if (previouslyRestrictedFiles == null) {
            previouslyRestrictedFiles = new ArrayList();
            for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                if (fmd.isRestricted()) {
                    previouslyRestrictedFiles.add(fmd);
                }
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
                    fileNames = fileNames.concat(fmd.getLabel());
                }
            }
            fmd.setRestricted(restricted);
        }
        if (fileNames != null) {
            String successMessage = JH.localize("file.restricted.success");
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

    private List<FileMetadata> filesToBeDeleted = new ArrayList();

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

        filesToBeDeleted.addAll(selectedFiles);
        // remove from the files list
        //dataset.getLatestVersion().getFileMetadatas().removeAll(selectedFiles);
        Iterator fmit = dataset.getEditVersion().getFileMetadatas().iterator();
        while (fmit.hasNext()) {
            FileMetadata fmd = (FileMetadata) fmit.next();

            fmd.getDataFile().setModificationTime(new Timestamp(new Date().getTime()));
            for (FileMetadata markedForDelete : selectedFiles) {

                if (markedForDelete.getId() == null && markedForDelete.getDataFile().getFileSystemName().equals(fmd.getDataFile().getFileSystemName())) {
                    fmit.remove();
                    break;
                }
                if (markedForDelete.getId() != null && markedForDelete.getId().equals(fmd.getId())) {
                    fmit.remove();
                    break;
                }
            }
        }

        if (fileNames != null) {
            String successMessage = JH.localize("file.deleted.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);
        }
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
               
        /*
         * Save and/or ingest files, if there are any:
        
         * All the back end-specific ingest logic has been moved into 
         * the IngestServiceBean! -- L.A.
         */
        // File deletes (selected by the checkboxes on the page)
        if (this.filesToBeDeleted != null) {

            // First Remove Any that have never been ingested:
            Iterator<DataFile> dfIt = newFiles.iterator();
            while (dfIt.hasNext()) {
                DataFile dfn = dfIt.next();
                for (FileMetadata markedForDelete : this.filesToBeDeleted) {
                    if (markedForDelete.getDataFile().getFileSystemName().equals(dfn.getFileSystemName())) {
                        dfIt.remove();
                    }
                }
            }

            dfIt = dataset.getFiles().iterator();
            while (dfIt.hasNext()) {
                DataFile dfn = dfIt.next();
                for (FileMetadata markedForDelete : this.filesToBeDeleted) {
                    if (markedForDelete.getId() == null && markedForDelete.getDataFile().getFileSystemName().equals(dfn.getFileSystemName())) {
                        dfIt.remove();
                    }
                }
            }

            // this next iterator is likely unnecessary (because the metadata object
            // was already deleted from the filemetadatas list associated with this
            // version, when it was added to the "filestobedeleted" list. 
            Iterator<FileMetadata> fmIt = dataset.getEditVersion().getFileMetadatas().iterator();

            while (fmIt.hasNext()) {
                FileMetadata dfn = fmIt.next();
                dfn.getDataFile().setModificationTime(new Timestamp(new Date().getTime()));
                for (FileMetadata markedForDelete : this.filesToBeDeleted) {
                    if (markedForDelete.getId() == null && markedForDelete.getDataFile().getFileSystemName().equals(dfn.getDataFile().getFileSystemName())) {
                        fmIt.remove();
                        break;
                    }
                }
            }
//delete for files that have been injested....

            for (FileMetadata fmd : filesToBeDeleted) {

                if (fmd.getId() != null && fmd.getId().intValue() > 0) {
                    Command cmd;
                    /* TODO: 
                     * I commented-out the code that was going through the filemetadatas
                     * associated with the version... Because the new delete button 
                     * functionality has already deleted the selected filemetadatas
                     * from the list. 
                     * I'm leaving that dead code commented-out, so that we can
                     * review it before it's removed for good. 
                     * -- L.A. 4.0 beta12
                     */
                    /*
                     fmIt = dataset.getEditVersion().getFileMetadatas().iterator();
                     while (fmIt.hasNext()) {
                     FileMetadata dfn = fmIt.next();
                     if (fmd.getId().equals(dfn.getId())) {
                     */
                    try {
                        Long idToRemove = fmd.getId(); ///dfn.getId();
                        logger.log(Level.INFO, "deleting file, filemetadata id {0}", idToRemove);

                        // finally, check if this file is being used as the default thumbnail
                        // for its dataset: 
                        if (fmd.getDataFile().equals(dataset.getThumbnailFile())) {
                            logger.info("deleting the dataset thumbnail designation");
                            dataset.setThumbnailFile(null);
                        }
                        cmd = new DeleteDataFileCommand(fmd.getDataFile(), session.getUser());
                        commandEngine.submit(cmd);

                        ///fmIt.remove();
                        Long fileIdToRemove = fmd.getDataFile().getId();
                        int i = dataset.getFiles().size();
                        for (int j = 0; j < i; j++) {
                            Iterator<FileMetadata> tdIt = dataset.getFiles().get(j).getFileMetadatas().iterator();
                            while (tdIt.hasNext()) {
                                FileMetadata dsTest = tdIt.next();
                                if (dsTest.getId().equals(idToRemove)) {
                                    tdIt.remove();
                                }
                            }
                        }

                        if (!(dataset.isReleased())) {
                            Iterator<DataFile> dfrIt = dataset.getFiles().iterator();
                            while (dfrIt.hasNext()) {
                                DataFile dsTest = dfrIt.next();
                                if (dsTest.getId().equals(fileIdToRemove)) {
                                    dfrIt.remove();
                                }
                            }
                        }

                    } catch (CommandException ex) {
                        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Data file Delete Failed", " - " + ex.toString()));
                        logger.severe(ex.getMessage());
                    }

                    /*}
                     }*/
                }
            }
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
                workingVersion.setLicense(DatasetVersion.License.CC0);
                if ( selectedTemplate != null ) {
                    if ( session.getUser().isAuthenticated() ) {
                        cmd = new CreateDatasetCommand(dataset, (AuthenticatedUser) session.getUser(), false, null, selectedTemplate); 
                    } else {
                        JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.create.authenticatedUsersOnly"));
                        return null;
                    }
                } else {
                   cmd = new CreateDatasetCommand(dataset, session.getUser());
                }
                
            } else {
                cmd = new UpdateDatasetCommand(dataset, session.getUser());
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
        editMode = null;

        // Call Ingest Service one more time, to 
        // queue the data ingest jobs for asynchronous execution: 
        ingestService.startIngestJobs(dataset, (AuthenticatedUser) session.getUser());

        return returnToDraftVersion();
    }
    
    private void populateDatasetUpdateFailureMessage(){
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

    public boolean isDuplicate(FileMetadata fileMetadata) {
        String thisMd5 = fileMetadata.getDataFile().getmd5();
        if (thisMd5 == null) {
            return false;
        }

        Map<String, Integer> MD5Map = new HashMap<String, Integer>();

        // TODO: 
        // think of a way to do this that doesn't involve populating this 
        // map for every file on the page? 
        // man not be that much of a problem, if we paginate and never display 
        // more than a certain number of files... Still, needs to be revisited
        // before the final 4.0. 
        // -- L.A. 4.0
        Iterator<FileMetadata> fmIt = workingVersion.getFileMetadatas().iterator();
        while (fmIt.hasNext()) {
            FileMetadata fm = fmIt.next();
            String md5 = fm.getDataFile().getmd5();
            if (md5 != null) {
                if (MD5Map.get(md5) != null) {
                    MD5Map.put(md5, MD5Map.get(md5).intValue() + 1);
                } else {
                    MD5Map.put(md5, 1);
                }
            }
        }

        return MD5Map.get(thisMd5) != null && MD5Map.get(thisMd5).intValue() > 1;
    }

    private HttpClient getClient() {
        // TODO: 
        // cache the http client? -- L.A. 4.0 alpha
        return new HttpClient();
    }

    public boolean showFileUploadFileComponent(){
        
        if ((this.editMode == this.editMode.FILE) || (this.editMode == this.editMode.CREATE)){
           return true;
        }
        return false;
    }
    
    
    public void handleDropBoxUpload(ActionEvent e) {
        // Read JSON object from the output of the DropBox Chooser: 
        JsonReader dbJsonReader = Json.createReader(new StringReader(dropBoxSelection));
        JsonArray dbArray = dbJsonReader.readArray();
        dbJsonReader.close();
               
        for (int i = 0; i < dbArray.size(); i++) {
            JsonObject dbObject = dbArray.getJsonObject(i);

            // Extract the payload:
            String fileLink = dbObject.getString("link");
            String fileName = dbObject.getString("name");
            int fileSize = dbObject.getInt("bytes");

            logger.fine("DropBox url: " + fileLink + ", filename: " + fileName + ", size: " + fileSize);
            
            /* ----------------------------
               If the file is too big:
                - Add error mesage      
                - Go to the next file
            // ---------------------------- */
            if ((!this.isUnlimitedUploadFileSize())&&(fileSize > this.getMaxFileUploadSizeInBytes())){
                String warningMessage = "Dropbox file \"" + fileName + "\" exceeded the limit of " + fileSize + " bytes and was not uploaded.";
                FacesContext.getCurrentInstance().addMessage(e.getComponent().getClientId(), new FacesMessage(FacesMessage.SEVERITY_ERROR, "upload failure", warningMessage));
                continue;  // skip to next file, and add error mesage                
            }
            
            DataFile dFile = null;

            // Make http call, download the file: 
            GetMethod dropBoxMethod = new GetMethod(fileLink);
            int status = 0;
            InputStream dropBoxStream = null;
            try {
                status = getClient().executeMethod(dropBoxMethod);
                if (status == 200) {
                    dropBoxStream = dropBoxMethod.getResponseBodyAsStream();

                    // If we've made it this far, we must have been able to
                    // make a successful HTTP call to the DropBox server and 
                    // obtain an InputStream - so we can now create a new
                    // DataFile object: 
                    dFile = ingestService.createDataFile(workingVersion, dropBoxStream, fileName, null);
                    newFiles.add(dFile);
                }
            } catch (IOException ex) {
                logger.warning("Failed to access DropBox url: " + fileLink + "!");
                continue;
            } finally {
                if (dropBoxMethod != null) {
                    dropBoxMethod.releaseConnection();
                }
                if (dropBoxStream != null) {
                    try {
                        dropBoxStream.close();
                    } catch (Exception ex) {
                        //logger.whocares("...");
                    }
                }
            }
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        DataFile dFile = null;
        List<DataFile> dFileList = null;

        String warningMessage = null;

        try {
            dFileList = ingestService.createDataFiles(workingVersion, uFile.getInputstream(), uFile.getFileName(), uFile.getContentType());
        } catch (IOException ioex) {
            logger.warning("Failed to process and/or save the file " + uFile.getFileName() + "; " + ioex.getMessage());
            return;
        }

        String duplicateFileNames = null;
        boolean multipleFiles = dFileList.size() > 1;
        boolean multipleDupes = false;

        if (dFileList != null) {
            for (int i = 0; i < dFileList.size(); i++) {
                dFile = dFileList.get(i);

                // Check for ingest warnings: 
                if (dFile.isIngestProblem()) {
                    if (dFile.getIngestReportMessage() != null) {
                        if (warningMessage == null) {
                            warningMessage = dFile.getIngestReportMessage();
                        } else {
                            warningMessage = warningMessage.concat("; " + dFile.getIngestReportMessage());
                        }
                    }
                    dFile.setIngestDone();
                }

                if (!isDuplicate(dFile.getFileMetadata())) {
                    newFiles.add(dFile);
                } else {
                    if (duplicateFileNames == null) {
                        duplicateFileNames = dFile.getFileMetadata().getLabel();
                    } else {
                        duplicateFileNames = duplicateFileNames.concat(", " + dFile.getFileMetadata().getLabel());
                        multipleDupes = true;
                    }

                    // remove the file from the dataset (since createDataFiles has already linked
                    // it to the dataset!
                    // first, through the filemetadata list, then through tht datafiles list:
                    Iterator<FileMetadata> fmIt = dataset.getEditVersion().getFileMetadatas().iterator();
                    while (fmIt.hasNext()) {
                        FileMetadata fm = fmIt.next();
                        if (fm.getId() == null && dFile.getFileSystemName().equals(fm.getDataFile().getFileSystemName())) {
                            fmIt.remove();
                            break;
                        }
                    }

                    Iterator<DataFile> dfIt = dataset.getFiles().iterator();
                    while (dfIt.hasNext()) {
                        DataFile dfn = dfIt.next();
                        if (dfn.getId() == null && dFile.getFileSystemName().equals(dfn.getFileSystemName())) {
                            dfIt.remove();
                            break;
                        }
                    }
                }
            }
        }

        if (duplicateFileNames != null) {
            String duplicateFilesErrorMessage = null;
            if (multipleDupes) {
                duplicateFilesErrorMessage = "The following files already exist in the dataset: " + duplicateFileNames;
            } else {
                if (multipleFiles) {
                    duplicateFilesErrorMessage = "The following file already exists in the dataset: " + duplicateFileNames;
                } else {
                    duplicateFilesErrorMessage = "This file already exists in this dataset. Please upload another file.";
                }
            }
            if (warningMessage == null) {
                warningMessage = duplicateFilesErrorMessage;
            } else {
                warningMessage = warningMessage.concat("; " + duplicateFilesErrorMessage);
            }
        }

        if (warningMessage != null) {
            logger.fine("trying to send faces message to " + event.getComponent().getClientId());
            FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(), new FacesMessage(FacesMessage.SEVERITY_ERROR, "upload failure", warningMessage));
            logger.severe(warningMessage);
        }
    }

    public boolean isLocked() {
        if (dataset != null) {
            logger.fine("checking lock status of dataset " + dataset.getId());
            if (dataset.isLocked()) {
                // refresh the dataset and version, if the current working
                // version of the dataset is locked:
                refresh();
            }
            Dataset lookedupDataset = datasetService.find(dataset.getId());
            DatasetLock datasetLock = null;
            if (lookedupDataset != null) {
                datasetLock = lookedupDataset.getDatasetLock();
                if (datasetLock != null) {
                    logger.fine("locked!");
                    return true;
                }
            }
        }
        return false;
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
    
   private void createSilentGuestbookEntry(FileMetadata fileMetadata, String format){
        initGuestbookResponse(fileMetadata, format);
        Command cmd;
        try {
            if (this.guestbookResponse != null) {
                cmd = new CreateGuestbookResponseCommand(session.getUser(), guestbookResponse, dataset);
                commandEngine.submit(cmd);
            }
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Guestbook Response Save Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        
    }
    
    //public String startMultipleFileDownload (){
    public void startMultipleFileDownload (){
        if (this.selectedFiles.isEmpty() ){
            return;
        }
        
        for (FileMetadata fmd : this.selectedFiles){
            DataFile df = fmd.getDataFile();
            // todo: cleanup this: "create" method doesn't necessarily
            // mean a response wikk be created (e.g. when dataset in draft)
            createSilentGuestbookEntry(df.getFileMetadata(), "");
        }

        //return 
        callDownloadServlet(getSelectedFilesIdsString());

    }
    
    //public String startFileDownload(FileMetadata fileMetadata, String format) {
    public void startFileDownload(FileMetadata fileMetadata, String format) {
        createSilentGuestbookEntry(fileMetadata, format);
        callDownloadServlet(format, fileMetadata.getDataFile().getId());
    }
    
    private String downloadFormat;

    public String getDownloadFormat() {
        return downloadFormat;
    }

    public void setDownloadFormat(String downloadFormat) {
        this.downloadFormat = downloadFormat;
    }
    
    private String downloadType = "";

    public String getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(String downloadType) {
        this.downloadType = downloadType;
    }
    
  
   public void initGuestbookResponse(FileMetadata fileMetadata){
         initGuestbookResponse(fileMetadata, "");
    }
    
    public void initGuestbookMultipleResponse(){
         initGuestbookResponse(null, "download");
    }

    public void initGuestbookResponse(FileMetadata fileMetadata, String downloadFormat) {
        if (fileMetadata != null){
           this.setSelectedDownloadFile(fileMetadata.getDataFile());
        }
        setDownloadFormat(downloadFormat);
        if (fileMetadata == null){
            setDownloadType("multiple");
        } else {
            setDownloadType("download");
        }
        if(this.workingVersion != null && this.workingVersion.isDraft()){
            this.guestbookResponse = null;
            return;
        }
        this.guestbookResponse = new GuestbookResponse();
        
        User user = session.getUser();
        if (this.dataset.getGuestbook() != null) {
            this.guestbookResponse.setGuestbook(this.dataset.getGuestbook());
            this.guestbookResponse.setName("");
            this.guestbookResponse.setEmail("");
            this.guestbookResponse.setInstitution("");
            this.guestbookResponse.setPosition("");
            this.guestbookResponse.setSessionId(session.toString());
            if (user.isAuthenticated()) {
                AuthenticatedUser aUser = (AuthenticatedUser) user;
                this.guestbookResponse.setName(aUser.getName());
                this.guestbookResponse.setAuthenticatedUser(aUser);
                this.guestbookResponse.setEmail(aUser.getEmail());
                this.guestbookResponse.setInstitution(aUser.getAffiliation());
                this.guestbookResponse.setPosition(aUser.getPosition());
                this.guestbookResponse.setSessionId(session.toString());
            }
            if (fileMetadata != null){
                this.guestbookResponse.setDataFile(fileMetadata.getDataFile());
            }            
        } else {
            if (fileMetadata != null){
                 this.guestbookResponse = guestbookServiceBean.initDefaultGuestbookResponse(dataset, fileMetadata.getDataFile(), user, session);
            } else {
                 this.guestbookResponse = guestbookServiceBean.initDefaultGuestbookResponse(dataset, null, user, session);
            }          
        }
        if (this.dataset.getGuestbook() != null && !this.dataset.getGuestbook().getCustomQuestions().isEmpty()) {
            this.guestbookResponse.setCustomQuestionResponses(new ArrayList());
            for (CustomQuestion cq : this.dataset.getGuestbook().getCustomQuestions()) {
                CustomQuestionResponse cqr = new CustomQuestionResponse();
                cqr.setGuestbookResponse(guestbookResponse);
                cqr.setCustomQuestion(cq);
                cqr.setResponse("");
                if (cq.getQuestionType().equals("options")) {
                    //response select Items
                    cqr.setResponseSelectItems(setResponseUISelectItems(cq));
                }
                this.guestbookResponse.getCustomQuestionResponses().add(cqr);
            }
        }
        this.guestbookResponse.setDownloadtype("Download");
        if(downloadFormat.toLowerCase().equals("subset")){
            this.guestbookResponse.setDownloadtype("Subset");
            setDownloadFormat("subset");
            setDownloadType("subset");
        }
        if(downloadFormat.toLowerCase().equals("explore")){
            setDownloadFormat("explore");
            setDownloadType("explore");
            this.guestbookResponse.setDownloadtype("Explore");
        }
        this.guestbookResponse.setDataset(dataset);
    }

    private List<SelectItem> setResponseUISelectItems(CustomQuestion cq) {
        List<SelectItem> retList = new ArrayList();
        for (CustomQuestionValue cqv : cq.getCustomQuestionValues()) {
            SelectItem si = new SelectItem(cqv.getValueString(), cqv.getValueString());
            retList.add(si);
        }
        return retList;
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

    private List<DatasetVersion> resetReleasedVersionTabList() {
        List<DatasetVersion> retList = new ArrayList();
        for (DatasetVersion version : dataset.getVersions()) {
            if (version.isReleased() || version.isArchived()) {
                retList.add(version);
            }
        }
        return retList;
    }

    public void downloadDatasetCitationXML() {
        downloadCitationXML(null);
    }

    public void downloadDatafileCitationXML(FileMetadata fileMetadata) {
        downloadCitationXML(fileMetadata);
    }

    public void downloadCitationXML(FileMetadata fileMetadata) {

        String xml = datasetService.createCitationXML(workingVersion, fileMetadata);
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("text/xml");
        String fileNameString = "";
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation: 
            fileNameString = "attachment;filename=" + getFileNameDOI() + ".xml";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI() + "-" + fileMetadata.getLabel().replaceAll("\\.tab$", "-endnote.xml");
        }
        response.setHeader("Content-Disposition", fileNameString);
        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(xml.getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (Exception e) {

        }
    }

    private String getFileNameDOI() {
        Dataset ds = workingVersion.getDataset();
        return "DOI:" + ds.getAuthority() + "_" + ds.getIdentifier().toString();
    }

    public void downloadDatasetCitationRIS() {

        downloadCitationRIS(null);

    }

    public void downloadDatafileCitationRIS(FileMetadata fileMetadata) {
        downloadCitationRIS(fileMetadata);
    }

    public void downloadCitationRIS(FileMetadata fileMetadata) {

        String risFormatDowload = datasetService.createCitationRIS(workingVersion, fileMetadata);
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("application/download");

        String fileNameString = "";
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation: 
            fileNameString = "attachment;filename=" + getFileNameDOI() + ".ris";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI() + "-" + fileMetadata.getLabel().replaceAll("\\.tab$", ".ris");
        }
        response.setHeader("Content-Disposition", fileNameString);

        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(risFormatDowload.getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (Exception e) {

        }
    }

    public String getDataExploreURL() {
        String TwoRavensUrl = settingsService.getValueForKey(SettingsServiceBean.Key.TwoRavensUrl);

        if (TwoRavensUrl != null && !TwoRavensUrl.equals("")) {
            return TwoRavensUrl;
        }

        return "";
    }

    public String getDataExploreURLComplete(Long fileid) {
        String TwoRavensUrl = settingsService.getValueForKey(SettingsServiceBean.Key.TwoRavensUrl);
        String TwoRavensDefaultLocal = "/dataexplore/gui.html?dfId=";

        if (TwoRavensUrl != null && !TwoRavensUrl.equals("")) {
            // If we have TwoRavensUrl set up as, as an optional 
            // configuration service, it must mean that TwoRavens is sitting 
            // on some remote server. And that in turn means that we must use 
            // full URLs to pass data and metadata to it. 
            String tabularDataURL = getTabularDataFileURL(fileid);
            String tabularMetaURL = getVariableMetadataURL(fileid);
            return TwoRavensUrl + "?ddiurl=" + tabularMetaURL + "&dataurl=" + tabularDataURL + "&" + getApiTokenKey();
        }

        // For a local TwoRavens setup it's enough to call it with just 
        // the file id:
        return TwoRavensDefaultLocal + fileid + "&" + getApiTokenKey();
    }

    public String getVariableMetadataURL(Long fileid) {
        String myHostURL = systemConfig.getDataverseSiteUrl();
        String metaURL = myHostURL + "/api/meta/datafile/" + fileid;

        return metaURL;
    }

    public String getTabularDataFileURL(Long fileid) {
        String myHostURL = systemConfig.getDataverseSiteUrl();;
        String dataURL = myHostURL + "/api/access/datafile/" + fileid;

        return dataURL;
    }

    private FileMetadata fileMetadataSelected = null;

    public void  setFileMetadataSelected(FileMetadata fm){
       setFileMetadataSelected(fm, null); 
    }
    
    public void setFileMetadataSelected(FileMetadata fm, String guestbook) {
        if (guestbook != null) {
            if (guestbook.equals("create")) {
                createSilentGuestbookEntry(fm, "Subset");
            } else {
                initGuestbookResponse(fm, "Subset");
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
            logger.info(successMessage);
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

    private String[] selectedTags = {};

    public String[] getSelectedTags() {

        selectedTags = null;
        selectedTags = new String[0];

        if (fileMetadataSelectedForTagsPopup != null) {
            if (fileMetadataSelectedForTagsPopup.getDataFile() != null
                    && fileMetadataSelectedForTagsPopup.getDataFile().getTags() != null
                    && fileMetadataSelectedForTagsPopup.getDataFile().getTags().size() > 0) {

                selectedTags = new String[fileMetadataSelectedForTagsPopup.getDataFile().getTags().size()];

                for (int i = 0; i < fileMetadataSelectedForTagsPopup.getDataFile().getTags().size(); i++) {
                    selectedTags[i] = fileMetadataSelectedForTagsPopup.getDataFile().getTags().get(i).getTypeLabel();
                }
            }
        }
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

    /* This method handles saving both "tabular file tags" and 
     * "file categories" (which are also considered "tags" in 4.0)
    */
    public void saveFileTagsAndCategories() {
        // 1. File categories:
        // we don't need to do anything for the file categories that the user
        // selected from the pull down list; that was done directly from the 
        // page with the FileMetadata.setCategoriesByName() method. 
        // So here we only need to take care of the new, custom category
        // name, if entered: 
        
        logger.fine("New category name: " + newCategoryName);

        if (fileMetadataSelectedForTagsPopup != null && newCategoryName != null) {
            logger.fine("Adding new category, for file " + fileMetadataSelectedForTagsPopup.getLabel());
            fileMetadataSelectedForTagsPopup.addCategoryByName(newCategoryName);
        } else {
            logger.fine("No FileMetadata selected, or no category specified!");
        }
        newCategoryName = null;
        
        // 2. Tabular DataFile Tags: 

        if (selectedTags != null) {
        
            if (fileMetadataSelectedForTagsPopup != null && fileMetadataSelectedForTagsPopup.getDataFile() != null) {
                fileMetadataSelectedForTagsPopup.getDataFile().setTags(null);
                for (int i = 0; i < selectedTags.length; i++) {
                    
                    DataFileTag tag = new DataFileTag();
                    try {
                        tag.setTypeByLabel(selectedTags[i]);
                        tag.setDataFile(fileMetadataSelectedForTagsPopup.getDataFile());
                        fileMetadataSelectedForTagsPopup.getDataFile().addTag(tag);
                        
                    } catch (IllegalArgumentException iax) {
                        // ignore 
                    }
                }
                
                // success message: 
                String successMessage = JH.localize("file.assignedTabFileTags.success");
                logger.fine(successMessage);
                successMessage = successMessage.replace("{0}", fileMetadataSelectedForTagsPopup.getLabel());
                JsfHelper.addFlashMessage(successMessage);
            }
            // reset:
            selectedTags = null;
        }
        
        fileMetadataSelectedForTagsPopup = null;

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

    public String getFileDateToDisplay(FileMetadata fileMetadata) {
        Date fileDate = null;
        DataFile datafile = fileMetadata.getDataFile();
        if (datafile != null) {
            boolean fileHasBeenReleased = datafile.isReleased();
            if (fileHasBeenReleased) {
                Timestamp filePublicationTimestamp = datafile.getPublicationDate();
                if (filePublicationTimestamp != null) {
                    fileDate = filePublicationTimestamp;
                }
            } else {
                Timestamp fileCreateTimestamp = datafile.getCreateDate();
                if (fileCreateTimestamp != null) {
                    fileDate = fileCreateTimestamp;
                }
            }
        }
        if (fileDate != null) {
            return displayDateFormat.format(fileDate);
        }

        return "";
    }

    

    public boolean isDownloadPopupRequired() {
        // Each of these conditions is sufficient reason to have to 
        // present the user with the popup: 
        
        //0. if version is draft then Popup "not required"    
        if (!workingVersion.isReleased()){
            return false;
        }
        
        // 1. License and Terms of Use:
        if (!DatasetVersion.License.CC0.equals(workingVersion.getLicense())
                && !(workingVersion.getTermsOfUse() == null
                || workingVersion.getTermsOfUse().equals(""))) {
            return true;
        }

        // 2. Terms of Access:
        if (!(workingVersion.getTermsOfAccess() == null) && !workingVersion.getTermsOfAccess().equals("")) {
            return true;
        }

        // 3. Guest Book: 
        if (dataset.getGuestbook() != null && dataset.getGuestbook().isEnabled() && dataset.getGuestbook().getDataverse() != null ) {
            return true;
        }

        return false;
    }



    public void requestAccess(DataFile file) {
        file.getFileAccessRequesters().add((AuthenticatedUser) session.getUser());
        datafileService.save(file);

        // create notifications
        for (AuthenticatedUser au : permissionService.getUsersWithPermissionOn(Permission.ManageDatasetPermissions, dataset)) {
            userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.REQUESTFILEACCESS, dataset.getId());
        }
    }
}
