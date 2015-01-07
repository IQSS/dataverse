/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
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
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseTemplateCountCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.metadataimport.ForeignMetadataImportServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.ByteArrayOutputStream;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
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
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.primefaces.context.RequestContext;
import java.net.URLEncoder;
import java.text.DateFormat;
import javax.faces.model.SelectItem;
import java.util.Collection;

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
    private String protocol = "";
    private String authority = "";
    private String separator = "";
    private boolean acceptedTerms = false;

    public boolean isAcceptedTerms() {
        return acceptedTerms;
    }

    public void setAcceptedTerms(boolean acceptedTerms) {
        this.acceptedTerms = acceptedTerms;
    }

    private final Map<Long, MapLayerMetadata> mapLayerMetadataLookup = new HashMap<>();
    
    private GuestbookResponse guestbookResponse = new GuestbookResponse();
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
        return globalId;
    }

    public void setGlobalId(String globalId) {
        this.globalId = globalId;
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

    private void updateDatasetFieldInputLevels() {
        Long dvIdForInputLevel = ownerId;

        if (!dataverseService.find(ownerId).isMetadataBlockRoot()) {
            dvIdForInputLevel = dataverseService.find(ownerId).getMetadataRootId();
        }
        for (DatasetField dsf : workingVersion.getFlatDatasetFields()) {
            DataverseFieldTypeInputLevel dsfIl = dataverseFieldTypeInputLevelService.findByDataverseIdDatasetFieldTypeId(dvIdForInputLevel, dsf.getDatasetFieldType().getId());
            if (dsfIl != null) {
                dsf.setRequired(dsfIl.isRequired());
                dsf.getDatasetFieldType().setRequiredDV(dsfIl.isRequired());
                dsf.setInclude(dsfIl.isInclude());
            } else {
                dsf.setRequired(dsf.getDatasetFieldType().isRequired());
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

        // The shapefile must be public.  RP 12/2015
        //
        if (!(fm.getDataFile().isReleased())){
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
     * Using a DataFile id, retreive an associated MapLayerMetadata object
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
        String nonNullDefaultIfKeyNotFound = "";
        protocol = settingsService.getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        authority = settingsService.getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
        separator = settingsService.getValueForKey(SettingsServiceBean.Key.DoiSeparator, nonNullDefaultIfKeyNotFound);
        if (dataset.getId() != null || globalId != null) { // view mode for a dataset     
            if (dataset.getId() != null) {
                dataset = datasetService.find(dataset.getId());
            }
            if (globalId != null) {
                dataset = datasetService.findByGlobalId(globalId);
            }

            if (dataset == null) {
                return "/404.xhtml";
            }
            // now get the correct version
            if (versionId == null) {
                // If we don't have a version ID, we will get the latest published version; if not published, then go ahead and get the latest
                workingVersion = dataset.getReleasedVersion();
                if (workingVersion == null) {
                    workingVersion = dataset.getLatestVersion();
                }
            } else {
                workingVersion = datasetVersionService.find(versionId);
            }

            if (workingVersion == null) {
                return "/404.xhtml";
            } else if (!workingVersion.isReleased() && !permissionService.on(dataset).has(Permission.ViewUnpublishedDataset)) {
                return "/loginpage.xhtml" + DataverseHeaderFragment.getRedirectPage();
            }

            ownerId = dataset.getOwner().getId();
            datasetNextMajorVersion = this.dataset.getNextMajorVersionString();
            datasetNextMinorVersion = this.dataset.getNextMinorVersionString();
            datasetVersionUI = datasetVersionUI.initDatasetVersionUI(workingVersion);
            updateDatasetFieldInputLevels();
            displayCitation = dataset.getCitation(false, workingVersion);
            setVersionTabList(resetVersionTabList());
            setReleasedVersionTabList(resetReleasedVersionTabList());

            // populate MapLayerMetadata
            this.loadMapLayerMetadataLookup();  // A DataFile may have a related MapLayerMetadata object

        } else if (ownerId != null) {
            // create mode for a new child dataset
            editMode = EditMode.CREATE;
            dataset.setOwner(dataverseService.find(ownerId));
            dataset.setIdentifier(datasetService.generateIdentifierSequence(protocol, authority, separator));

            if (dataset.getOwner() == null) {
                return "/404.xhtml";
            } else if (!permissionService.on(dataset.getOwner()).has(Permission.AddDataset)) {
                return "/loginpage.xhtml" + DataverseHeaderFragment.getRedirectPage();
            }

            dataverseTemplates = dataverseService.find(ownerId).getTemplates();
            if (dataverseService.find(ownerId).isTemplateRoot()) {
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
    
    public String saveGuestbookResponse() {
        
        boolean valid = true;
        
        if (workingVersion.getLicense() != null && workingVersion.getLicense().equals(DatasetVersion.License.CC0)){
            valid &= this.acceptedTerms;
        }
        
        if(dataset.getGuestbook() != null){
            if (dataset.getGuestbook().isNameRequired()){
                valid &= !guestbookResponse.getName().isEmpty();
            }
            if (dataset.getGuestbook().isEmailRequired()){
                valid &= !guestbookResponse.getEmail().isEmpty();
            }
            if (dataset.getGuestbook().isInstitutionRequired()){
                valid &= !guestbookResponse.getInstitution().isEmpty();
            }
            if (dataset.getGuestbook().isPositionRequired()){
                valid &= !guestbookResponse.getPosition().isEmpty();
            }
        }
        
        if (dataset.getGuestbook() != null && !dataset.getGuestbook().getCustomQuestions().isEmpty()){
            for (CustomQuestion cq: dataset.getGuestbook().getCustomQuestions()){
                if (cq.isRequired()){
                    for(CustomQuestionResponse cqr: guestbookResponse.getCustomQuestionResponses()){
                        if(cqr.getCustomQuestion().equals(cq)){
                            valid &= (cqr.getResponse() != null && !cqr.getResponse().isEmpty());
                        }
                    }
                }
            }
        }
        

        if (!valid) {
            logger.info("Guestbook response isn't valid.");
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "See below for details."));
            return "";
        }
        
        Command cmd;
        try {
            cmd = new CreateGuestbookResponseCommand(session.getUser(), guestbookResponse, dataset);
           commandEngine.submit(cmd);
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Guestbook Response Save Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());

        }
        
        if (guestbookResponse.getDataFile() != null) {
            String fileDownloadUrl = "/api/access/datafile/" + guestbookResponse.getDataFile().getId();
            logger.info("Returning file download url: "+fileDownloadUrl);
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(fileDownloadUrl);
            } catch (IOException ex) {
                logger.info("Failed to issue a redirect to file download url.");
            }
            return fileDownloadUrl; 
        }
        return "";
    }

    private String getUserName(User user) {
        if (user.isAuthenticated()) {
            AuthenticatedUser authUser = (AuthenticatedUser) user;
            return authUser.getName();
        }
        return "Guest";
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
            } else {
                return "key=";
            }
        } else {
            return "";
        }

    }
    private void resetVersionUI() {
        datasetVersionUI = datasetVersionUI.initDatasetVersionUI(workingVersion);
        User user = session.getUser();

        //Get builtin user to supply default values for contact email author name, etc.
        String userIdentifier = user.getIdentifier();
        userIdentifier = userIdentifier.startsWith("@") ? userIdentifier.substring(1) : userIdentifier;
        BuiltinUser builtinUser = builtinUserService.findByUserName(userIdentifier);

        //On create set pre-populated fields
        for (DatasetField dsf : dataset.getEditVersion().getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.depositor) && dsf.isEmpty()) {
                dsf.getDatasetFieldValues().get(0).setValue(getUserName(user));
            }
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.dateOfDeposit) && dsf.isEmpty()) {
                dsf.getDatasetFieldValues().get(0).setValue(new SimpleDateFormat("yyyy-MM-dd").format(new Timestamp(new Date().getTime())));
            }

            // the following only applies if this is a "real", builit-in user - has an account:           
            if (user.isBuiltInUser() && builtinUser != null) {
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContact) && dsf.isEmpty()) {
                    for (DatasetFieldCompoundValue contactValue : dsf.getDatasetFieldCompoundValues()) {
                        for (DatasetField subField : contactValue.getChildDatasetFields()) {
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactName)) {
                                subField.getDatasetFieldValues().get(0).setValue(builtinUser.getLastName() + ", " + builtinUser.getFirstName());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactAffiliation)) {
                                subField.getDatasetFieldValues().get(0).setValue(builtinUser.getAffiliation());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactEmail)) {
                                subField.getDatasetFieldValues().get(0).setValue(builtinUser.getEmail());
                            }
                        }
                    }
                }
                
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.author) && dsf.isEmpty()) {
                    for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                        for (DatasetField subField : authorValue.getChildDatasetFields()) {
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorName)) {
                                subField.getDatasetFieldValues().get(0).setValue(builtinUser.getLastName() + ", " + builtinUser.getFirstName());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorAffiliation)) {
                                subField.getDatasetFieldValues().get(0).setValue(builtinUser.getAffiliation());
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
            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Upload + Edit Dataset Files", " - You can drag and drop your files from your desktop, directly into the upload widget."));
        } else if (editMode == EditMode.METADATA) {
            datasetVersionUI = datasetVersionUI.initDatasetVersionUI(workingVersion);
            updateDatasetFieldInputLevels();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Edit Dataset Metadata", " - Add more metadata about your dataset to help others easily find it."));
        }
    }

    public String releaseDraft() {
        if (releaseRadio == 1) {
            return releaseDataset(false);
        } else {
            return releaseDataset(true);
        }
    }

    public String releaseDataset() {
        return releaseDataset(false);
    }

    public String deaccessionDataset() {
        return "";
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
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Deaccession Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetDeaccessioned", "Your selected versions have been deaccessioned.");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return "/dataset.xhtml?id=" + dataset.getId() + "&faces-redirect=true";
    }

    private DatasetVersion setDatasetVersionDeaccessionReasonAndURL(DatasetVersion dvIn) {
        int deaccessionReasonCode = getDeaccessionReasonRadio();
        String deacessionReasonDetail = getDeaccessionReasonText() != null ? getDeaccessionReasonText() : "";
        switch (deaccessionReasonCode) {
            case 1:
                dvIn.setVersionNote("There is identifiable data in one or more files. " + deacessionReasonDetail);
                break;
            case 2:
                dvIn.setVersionNote("The research article has been retracted. " + deacessionReasonDetail);
                break;
            case 3:
                dvIn.setVersionNote("The dataset has been transferred to another repository. " + deacessionReasonDetail);
                break;
            case 4:
                dvIn.setVersionNote("IRB request. " + deacessionReasonDetail);
                break;
            case 5:
                dvIn.setVersionNote("Legal issue or Data Usage Agreement. " + deacessionReasonDetail);
                break;
            case 6:
                dvIn.setVersionNote("Not a valid dataset. " + deacessionReasonDetail);
                break;
            case 7:
                dvIn.setVersionNote(deacessionReasonDetail);
                break;
        }
        dvIn.setArchiveNote(getDeaccessionForwardURLFor());
        return dvIn;
    }

    private String releaseDataset(boolean minor) {
        Command<Dataset> cmd;
        try {
            if (editMode == EditMode.CREATE) {
                cmd = new PublishDatasetCommand(dataset, session.getUser(), minor);
            } else {
                cmd = new PublishDatasetCommand(dataset, session.getUser(), minor);
            }
            dataset = commandEngine.submit(cmd);
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Release Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetReleased", "Your dataset is now public.");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return "/dataset.xhtml?id=" + dataset.getId() + "&faces-redirect=true";
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
        return "/dataset.xhtml?id=" + dataset.getId() + "&faces-redirect=true";
    }

    public void refresh(ActionEvent e) {
        refresh();
    }

    public void refresh() {
        logger.fine("refreshing");
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
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Delete Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetDeleted", "Your dataset has been deleted.");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return "/dataverse.xhtml?alias=" + dataset.getOwner().getAlias() + "&faces-redirect=true";
    }

    public String deleteDatasetVersion() {
        Command cmd;
        try {
            cmd = new DeleteDatasetVersionCommand(session.getUser(), dataset);
            commandEngine.submit(cmd);
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Version Delete Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetDeleted", "Your dataset has been deleted.");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return "/dataset.xhtml?id=" + dataset.getId() + "&faces-redirect=true";
    }

    private List<FileMetadata> selectedFiles;

    public List<FileMetadata> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<FileMetadata> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }
    

    public String save() {
        // Validate
        Set<ConstraintViolation> constraintViolations = workingVersion.validate();
        if (!constraintViolations.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "See below for details."));
            return "";
        }

        dataset.setProtocol(protocol);
        dataset.setAuthority(authority);
        dataset.setDoiSeparator(separator);

        /*
         * Save and/or ingest files, if there are any:
        
         * All the back end-specific ingest logic has been moved into 
         * the IngestServiceBean! -- L.A.
         */
        // File deletes (selected by the checkboxes on the page)
        //
        // First Remove Any that have never been ingested;
        if (this.selectedFiles != null) {
            Iterator<DataFile> dfIt = newFiles.iterator();
            while (dfIt.hasNext()) {
                DataFile dfn = dfIt.next();
                for (FileMetadata markedForDelete : this.selectedFiles) {
                    if (markedForDelete.getDataFile().getFileSystemName().equals(dfn.getFileSystemName())) {
                        dfIt.remove();
                    }
                }
            }

            dfIt = dataset.getFiles().iterator();
            while (dfIt.hasNext()) {
                DataFile dfn = dfIt.next();
                for (FileMetadata markedForDelete : this.selectedFiles) {
                    if (markedForDelete.getId() == null && markedForDelete.getDataFile().getFileSystemName().equals(dfn.getFileSystemName())) {
                        dfIt.remove();
                    }
                }
            }

            Iterator<FileMetadata> fmIt = dataset.getEditVersion().getFileMetadatas().iterator();

            while (fmIt.hasNext()) {
                FileMetadata dfn = fmIt.next();
                dfn.getDataFile().setModificationTime(new Timestamp(new Date().getTime()));
                for (FileMetadata markedForDelete : this.selectedFiles) {
                    if (markedForDelete.getId() == null && markedForDelete.getDataFile().getFileSystemName().equals(dfn.getDataFile().getFileSystemName())) {
                        fmIt.remove();
                        break;
                    }
                }
            }
//delete for files that have been injested....

            for (FileMetadata fmd : selectedFiles) {
                if (fmd.getId() != null && fmd.getId().intValue() > 0) {
                    Command cmd;
                    fmIt = dataset.getEditVersion().getFileMetadatas().iterator();
                    while (fmIt.hasNext()) {
                        FileMetadata dfn = fmIt.next();
                        if (fmd.getId().equals(dfn.getId())) {
                            try {
                                Long idToRemove = dfn.getId();
                                cmd = new DeleteDataFileCommand(fmd.getDataFile(), session.getUser());
                                commandEngine.submit(cmd);
                                fmIt.remove();
                                Long fileIdToRemove = dfn.getDataFile().getId();
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
                        }
                    }
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
                cmd = new CreateDatasetCommand(dataset, session.getUser());
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
            error.append(ex + " ");
            error.append(ex.getMessage() + " ");
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                error.append(cause + " ");
                error.append(cause.getMessage() + " ");
            }
            logger.fine("Couldn't save dataset: " + error.toString());
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + error.toString()));
            return null;
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + ex.toString()));
            logger.severe(ex.getMessage());
            return null;
        }
        newFiles.clear();
        editMode = null;

        // Call Ingest Service one more time, to 
        // queue the data ingest jobs for asynchronous execution: 
        ingestService.startIngestJobs(dataset, (AuthenticatedUser) session.getUser());

        return "/dataset.xhtml?id=" + dataset.getId() + "&versionId=" + dataset.getLatestVersion().getId() + "&faces-redirect=true";
    }

    public String cancel() {
        return "/dataset.xhtml?id=" + dataset.getId() + "&faces-redirect=true";
        /*
        // reset values
        dataset = datasetService.find(dataset.getId());
        workingVersion = dataset.getLatestVersion();
        if (workingVersion.isDeaccessioned() && dataset.getReleasedVersion() != null) {
            workingVersion = dataset.getReleasedVersion();
        }
        ownerId = dataset.getOwner().getId();
        setVersionTabList(resetVersionTabList());
        setReleasedVersionTabList(resetReleasedVersionTabList());
        newFiles.clear();
        editMode = null;
        */
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
                        duplicateFileNames = duplicateFileNames.concat(", "+dFile.getFileMetadata().getLabel());
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
            logger.fine("trying to send faces message to "+event.getComponent().getClientId());
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

    private List<DatasetVersion> versionTabList = new ArrayList();

    public List<DatasetVersion> getVersionTabList() {
        return versionTabList;
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

    public void initGuestbookResponse(FileMetadata fileMetadata) {
        
        if(this.guestbookResponse == null){
            this.guestbookResponse= new GuestbookResponse();
        }
        User user = session.getUser();
        if (this.dataset.getGuestbook() != null) {
            this.guestbookResponse.setGuestbook(this.dataset.getGuestbook());
                this.guestbookResponse.setName("");
                this.guestbookResponse.setEmail("");
                this.guestbookResponse.setInstitution("");
                this.guestbookResponse.setPosition(""); 
            if (user.isAuthenticated()) {
                AuthenticatedUser aUser = (AuthenticatedUser) user;
                this.guestbookResponse.setName(aUser.getName());
                this.guestbookResponse.setAuthenticatedUser(aUser);
                this.guestbookResponse.setEmail(aUser.getEmail());
                this.guestbookResponse.setInstitution(aUser.getAffiliation());
            }
            /*
            if (user.isBuiltInUser()) {
                BuiltinUser bUser = (BuiltinUser) user;
                this.guestbookResponse.setPosition(bUser.getPosition());
            }*/
        } else {
            this.guestbookResponse = guestbookServiceBean.initDefaultGuestbookResponse(dataset, null, user);
        }
        if (this.dataset.getGuestbook() != null && !this.dataset.getGuestbook().getCustomQuestions().isEmpty()){
            this.guestbookResponse.setCustomQuestionResponses(new ArrayList());
            for (CustomQuestion cq : this.dataset.getGuestbook().getCustomQuestions()){
                CustomQuestionResponse cqr = new CustomQuestionResponse();
                cqr.setGuestbookResponse(guestbookResponse);
                cqr.setCustomQuestion(cq);
                cqr.setResponse("");
                if (cq.getQuestionType().equals("options")){
                        //response select Items
                       cqr.setResponseSelectItems(setResponseUISelectItems(cq));
                }
                this.guestbookResponse.getCustomQuestionResponses().add(cqr);
            }
            
        }
        if (fileMetadata != null) {
            this.guestbookResponse.setDataFile(fileMetadata.getDataFile());
        }
        this.guestbookResponse.setDataset(dataset);
    }
    
    private List <SelectItem> setResponseUISelectItems(CustomQuestion cq){
        List  <SelectItem> retList = new ArrayList();
        for (CustomQuestionValue cqv: cq.getCustomQuestionValues()){
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

    private boolean canIssueUpdateCommand() {
        try {
            if (permissionService.on(dataset).canIssueCommand("UpdateDatasetCommand")) {
                return true;
            } else {
                return false;
            }
        } catch (ClassNotFoundException ex) {

        }
        return false;
    }

    private List<DatasetVersion> resetVersionTabList() {
        List<DatasetVersion> retList = new ArrayList();

        if (canIssueUpdateCommand()) {
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
            BuiltinUser builtinUser = builtinUserService.findByUserName(id);
            if (builtinUser != null) {
                if (contNames.isEmpty()) {
                    contNames = builtinUser.getDisplayName();
                } else {
                    contNames = contNames + ", " + builtinUser.getDisplayName();
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
    
    public void downloadCitationXML (FileMetadata fileMetadata) {
        
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
    
    public void setFileMetadataSelected(FileMetadata fm) {
        fileMetadataSelected = fm;
        logger.fine("set the file for the advanced options popup ("+fileMetadataSelected.getLabel()+")");
    }

    public FileMetadata getFileMetadataSelected() {
        if (fileMetadataSelected != null) {
            logger.fine("returning file metadata for the advanced options popup ("+fileMetadataSelected.getLabel()+")");
        } else {
            logger.fine("file metadata for the advanced options popup is null.");
        }
        return fileMetadataSelected;
    }
    
    /* Items for the "Advanced Options" popup. 
     * 
     * Tabular File Tags: 
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
        
        if (fileMetadataSelected != null) {
            if (fileMetadataSelected.getDataFile() != null 
                    && fileMetadataSelected.getDataFile().getTags() != null
                    && fileMetadataSelected.getDataFile().getTags().size() > 0) {
                
                selectedTags = new String[ fileMetadataSelected.getDataFile().getTags().size()];
                
                for (int i = 0; i < fileMetadataSelected.getDataFile().getTags().size(); i++) {
                    selectedTags[i] = fileMetadataSelected.getDataFile().getTags().get(i).getTypeLabel();
                }
            }
        }
        return selectedTags;
    }
 
    public void setSelectedTags(String[] selectedTags) {
        this.selectedTags = selectedTags;
    }
    
    public boolean getUseAsDatasetThumbnail() {
        
        if (fileMetadataSelected != null) {
            if (fileMetadataSelected.getDataFile() != null) {
                if (fileMetadataSelected.getDataFile().getId() != null) {
                    if (fileMetadataSelected.getDataFile().getOwner() != null) {
                        if (fileMetadataSelected.getDataFile().equals(fileMetadataSelected.getDataFile().getOwner().getThumbnailFile())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    public void setUseAsDatasetThumbnail(boolean useAsThumbnail) {
        if (fileMetadataSelected != null) {
            if (fileMetadataSelected.getDataFile() != null) {
                if (fileMetadataSelected.getDataFile().getId() != null) { // ?
                    if (fileMetadataSelected.getDataFile().getOwner() != null) {
                        if (useAsThumbnail) {
                            fileMetadataSelected.getDataFile().getOwner().setThumbnailFile(fileMetadataSelected.getDataFile());
                        } else if (getUseAsDatasetThumbnail()) {
                            fileMetadataSelected.getDataFile().getOwner().setThumbnailFile(null);
                        }
                    }
                }
            }
        }
    }
    
    public void saveAdvancedOptions() {
        // DataFile Tags: 
        
        if (selectedTags != null) {
            if (fileMetadataSelected != null && fileMetadataSelected.getDataFile() != null) {
                fileMetadataSelected.getDataFile().setTags(null);
                for (int i = 0; i < selectedTags.length; i++) {
                    DataFileTag tag = new DataFileTag();
                    try { 
                        tag.setTypeByLabel(selectedTags[i]);
                        tag.setDataFile(fileMetadataSelected.getDataFile());
                        fileMetadataSelected.getDataFile().addTag(tag);
                    } catch (IllegalArgumentException iax) {
                        // ignore 
                    }
                }
            }
            // reset:
            selectedTags = null;
        }
        
        // Use-as-the-thumbnail assignment (do nothing?)
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
    
    
    private String newCategoryName = null; 
    
    public String getNewCategoryName() {
        return newCategoryName;
    }
    
    public void setNewCategoryName(String newCategoryName) {
        this.newCategoryName = newCategoryName;
    }
    
    public void addFileCategory() {
        logger.info("New category name: "+newCategoryName);
        
        if (fileMetadataSelected != null && newCategoryName != null) {
            logger.info("Adding new category, for file "+fileMetadataSelected.getLabel());
            fileMetadataSelected.addCategoryByName(newCategoryName);
        } else {
            logger.info("No FileMetadata selected!");
        }
    }
    
    
    public boolean isDownloadPopupRequired() {
        // Each of these conditions is sufficient reason to have to 
        // present the user with the popup: 
        
        // 1. License and Terms of Use:
        
        if (!"CC0".equals(workingVersion.getLicense()) && 
                !(workingVersion.getTermsOfUse() == null ||
                  workingVersion.getTermsOfUse().equals(""))) {
            return true; 
        }
        
        // 2. Terms of Access:
        
        if (!(workingVersion.getTermsOfAccess() == null) && !workingVersion.getTermsOfAccess().equals("")) {
            return true; 
        }
        
        // 3. Guest Book: 
        
        if (dataset.getGuestbook() != null) {
            return true;
        }
        
        return false; 
    }
}
