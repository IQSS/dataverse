/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.MD5Checksum;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.metadataimport.ForeignMetadataImportServiceBean;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import java.util.logging.Level;
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
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.primefaces.context.RequestContext;
import org.primefaces.model.SelectableDataModel;
import java.util.ResourceBundle;
import java.text.MessageFormat;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DatasetPage")
public class DatasetPage implements java.io.Serializable {
    
    ResourceBundle rBundle =ResourceBundle.getBundle("DatasetBundle");

    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());

    public enum EditMode {

        CREATE, INFO, FILE, METADATA
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
    PermissionServiceBean permissionServiceBean;
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

    private Dataset dataset = new Dataset();
    private EditMode editMode;
    private Long ownerId;
    private Long versionId;
    private int selectedTabIndex;
    private List<DataFile> newFiles = new ArrayList();
    private DatasetVersion workingVersion;
    private DatasetVersionUI datasetVersionUI = new DatasetVersionUI();
    private List<DatasetField> deleteRecords = new ArrayList();
    private int releaseRadio = 1;
    private String datasetNextMajorVersion = "1.0";
    private String datasetNextMinorVersion = "";
    private String dropBoxSelection = "";
    private DatasetVersionDifference datasetVersionDifference;
    private Map<Long, Boolean> checked = new HashMap<>();

    private String displayCitation;

    public String getDisplayCitation() {
        return displayCitation;
    }

    public void setDisplayCitation(String displayCitation) {
        this.displayCitation = displayCitation;
    }

    public String getDropBoxSelection() {
        return dropBoxSelection;
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

    public void init() {
        if (dataset.getId() != null) { // view mode for a dataset           
            dataset = datasetService.find(dataset.getId());
            if (versionId == null) {
                if (!dataset.isReleased()) {
                    workingVersion = dataset.getLatestVersion();
                } else {
                    workingVersion = dataset.getReleasedVersion();
                }
            } else {
                workingVersion = datasetVersionService.find(versionId);
            }

            ownerId = dataset.getOwner().getId();
            if (dataset.getReleasedVersion() != null) {
                datasetNextMajorVersion = new Integer(dataset.getReleasedVersion().getVersionNumber().intValue() + 1).toString() + ".0";
                datasetNextMinorVersion = new Integer(dataset.getReleasedVersion().getVersionNumber().intValue()).toString() + "."
                        + new Integer(dataset.getReleasedVersion().getMinorVersionNumber().intValue() + 1).toString();
            }

            try {
                datasetVersionUI = new DatasetVersionUI(workingVersion);
            } catch (NullPointerException npe) {
                //This will happen when solr is down and will allow any link to be displayed.
                throw new RuntimeException("You do not have permission to view this dataset version."); // improve error handling
            }

            displayCitation = dataset.getCitation(false, workingVersion);
            setVersionTabList(resetVersionTabList());
        } else if (ownerId != null) {
            // create mode for a new child dataset
            editMode = EditMode.CREATE;
            workingVersion = dataset.getLatestVersion();

            dataset.setOwner(dataverseService.find(ownerId));
            datasetVersionUI = new DatasetVersionUI(workingVersion);
            dataset.setIdentifier(datasetService.generateIdentifierSequence("doi", "10.5072/FK2"));
            //On create set pre-populated fields
            for (DatasetField dsf : dataset.getEditVersion().getDatasetFields()) {
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.depositor)) {
                    dsf.getDatasetFieldValues().get(0).setValue(session.getUser().getLastName() + ", " + session.getUser().getFirstName());
                }
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.dateOfDeposit)) {
                    dsf.getDatasetFieldValues().get(0).setValue(new SimpleDateFormat("yyyy-MM-dd").format(new Timestamp(new Date().getTime())));
                }
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.distributorContact)) {
                    dsf.getDatasetFieldValues().get(0).setValue(session.getUser().getEmail());
                }
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.author)) {
                    for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                        for (DatasetField subField : authorValue.getChildDatasetFields()) {
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorName)) {
                                subField.getDatasetFieldValues().get(0).setValue(session.getUser().getLastName() + ", " + session.getUser().getFirstName());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorAffiliation)) {
                                subField.getDatasetFieldValues().get(0).setValue(session.getUser().getAffiliation());
                            }
                        }
                    }
                }
            }
            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Add New Dataset", " - Enter metadata to create the dataset's citation. You can add more metadata about this dataset after it's created."));
        } else {
            throw new RuntimeException("On Dataset page without id or ownerid."); // improve error handling
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
            datasetVersionUI = new DatasetVersionUI(workingVersion);            
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, rBundle.getString("editDatasetMetadataSummary"), rBundle.getString("editDatasetMetadataDetail")));
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
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, rBundle.getString("datasetReleaseFailedSummary"), " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, rBundle.getString("datasetReleasedSummary"), rBundle.getString("datasetReleasedDetail"));
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
    }

    public String deleteDataset() {

        Command cmd;
        try {
            cmd = new DestroyDatasetCommand(dataset, session.getUser());
            commandEngine.submit(cmd);
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, rBundle.getString("datasetDeleteFailedSummary"), " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, rBundle.getString("datasetDeletedSummary"), rBundle.getString("datasetDeletedDetail"));
        FacesContext.getCurrentInstance().addMessage(null, message);
        return "/dataverse.xhtml?id=" + dataset.getOwner().getId() + "&faces-redirect=true";
    }

    public String deleteDatasetVersion() {
        Command cmd;
        try {
            cmd = new DeleteDatasetVersionCommand(session.getUser(), dataset);
            commandEngine.submit(cmd);
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, rBundle.getString("versionDeleteFailedSummary"), " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, rBundle.getString("datasetDeletedSummary"), rBundle.getString("datasetDeletedDetail"));
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
        boolean dontSave = false;
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        for (DatasetField dsf : workingVersion.getFlatDatasetFields()) {
            dsf.setValidationMessage(null); // clear out any existing validation message
            Set<ConstraintViolation<DatasetField>> constraintViolations = validator.validate(dsf);
            for (ConstraintViolation<DatasetField> constraintViolation : constraintViolations) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, rBundle.getString("validationErrorSummary"), constraintViolation.getMessage()));
                dsf.setValidationMessage(constraintViolation.getMessage());
                dontSave = true;
                break; // currently only support one message, so we can break out of the loop after the first constraint violation
            }
            for (DatasetFieldValue dsfv : dsf.getDatasetFieldValues()) {
                dsfv.setValidationMessage(null); // clear out any existing validation message
                Set<ConstraintViolation<DatasetFieldValue>> constraintViolations2 = validator.validate(dsfv);
                for (ConstraintViolation<DatasetFieldValue> constraintViolation : constraintViolations2) {
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, rBundle.getString("validationErrorSummary"), constraintViolation.getMessage()));
                    dsfv.setValidationMessage(constraintViolation.getMessage());
                    dontSave = true;
                    break; // currently only support one message, so we can break out of the loop after the first constraint violation                    
                }
            }
        }
        if (dontSave) {
            return "";
        }

        //TODO get real application-wide protocol/authority
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");

        /*
         * Save and/or ingest files, if there are any:
        
         * All the back end-specific ingest logic has been moved into 
         * the IngestServiceBean! -- L.A.
         * TODO: we still need to figure out how the ingestServiceBean is 
         * going to communicate the information about ingest errors back to 
         * the page, and what the page should be doing to alert the user. 
         * (we may not do any communication/exceptions/etc. here - relying
         * instead on the ingest/upload status properly set on each of the 
         * individual files, and adding a mechanism to the page for displaying
         * file-specific error reports - in pop-up windows maybe?)
         */
        //First Remove Any that have never been ingested;
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
                                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, rBundle.getString("dataFileDeleteFailedSummary"), " - " + ex.toString()));
                                logger.severe(ex.getMessage());
                            }
                        }
                    }
                }
            }
        }
        
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
                userNotificationService.sendNotification(session.getUser(), dataset.getCreateDate(), UserNotification.Type.CREATEDS, dataset.getLatestVersion().getId());
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
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, rBundle.getString("datasetSaveFailedSummary"), " - " + error.toString()));
            return null;
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, rBundle.getString("datasetSaveFailedSummary"), " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        newFiles.clear();
        editMode = null;

        // Call Ingest Service one more time, to 
        // queue the data ingest jobs for asynchronous execution: 
        ingestService.startIngestJobs(dataset, session.getUser());

        return "/dataset.xhtml?id=" + dataset.getId() + "&versionId=" + dataset.getLatestVersion().getId() + "&faces-redirect=true";
    }

    public void cancel() {
        // reset values
        dataset = datasetService.find(dataset.getId());
        ownerId = dataset.getOwner().getId();
        setVersionTabList(resetVersionTabList());
        newFiles.clear();
        editMode = null;
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

        try {
            dFile = ingestService.createDataFile(workingVersion, uFile.getInputstream(), uFile.getFileName(), uFile.getContentType());
        } catch (IOException ioex) {
            logger.warning("Failed to process and/or save the file " + uFile.getFileName() + "; " + ioex.getMessage());
            return;
        }
        
        newFiles.add(dFile);

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

    public void setVersionTabList(List<DatasetVersion> versionTabList) {
        this.versionTabList = versionTabList;
    }

    private List<DatasetVersion> selectedVersions;

    public List<DatasetVersion> getSelectedVersions() {
        return selectedVersions;
    }

    public void setSelectedVersions(List<DatasetVersion> selectedVersions) {
        this.selectedVersions = selectedVersions;
    }

    public DatasetVersionDifference getDatasetVersionDifference() {
        return datasetVersionDifference;
    }

    public void setDatasetVersionDifference(DatasetVersionDifference datasetVersionDifference) {
        this.datasetVersionDifference = datasetVersionDifference;
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
        int count = 0;
        int size = this.getDataset().getVersions().size();

        if (originalVersion == null) {
            for (DatasetVersion dsv : newVersion.getDataset().getVersions()) {
                if (newVersion.equals(dsv)) {
                    if ((count + 1) < size) {
                        setDatasetVersionDifference(new DatasetVersionDifference(newVersion, newVersion.getDataset().getVersions().get(count + 1)));
                        break;
                    }
                }
                count++;
            }
        } else {
            setDatasetVersionDifference(new DatasetVersionDifference(newVersion, originalVersion));
        }
    }

    private boolean canIssueUpdateCommand() {
        try {
            if (permissionServiceBean.on(dataset).canIssueCommand("UpdateDatasetCommand")) {
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
            return dataset.getVersions();
        } else {
            for (DatasetVersion version : dataset.getVersions()) {
                if (version.isReleased()) {
                    retList.add(version);
                }
            }
            return retList;
        }
    }
}
