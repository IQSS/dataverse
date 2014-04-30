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
import edu.harvard.iq.dataverse.engine.command.impl.ReleaseDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
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
import java.util.Iterator;
import java.util.List;
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
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DatasetPage")
public class DatasetPage implements java.io.Serializable {

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
    EjbDataverseEngine commandEngine;
    @Inject
    DataverseSession session;

    private Dataset dataset = new Dataset();
    private EditMode editMode;
    private Long ownerId;
    private Long versionId;
    private int selectedTabIndex;
    private List<DataFile> newFiles = new ArrayList();
    private DatasetVersion editVersion = new DatasetVersion();
    private DatasetVersion displayVersion;
    private DatasetVersionUI datasetVersionUI = new DatasetVersionUI();
    private List<DatasetField> deleteRecords = new ArrayList();
    private int releaseRadio = 1;
    private String datasetNextMajorVersion = "1.0";
    private String datasetNextMinorVersion = "";
    private String dropBoxSelection = "";

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

    public DatasetVersion getDisplayVersion() {
        return displayVersion;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public DatasetVersion getEditVersion() {
        return editVersion;
    }

    public void setEditVersion(DatasetVersion editVersion) {
        this.editVersion = editVersion;
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
                if (canIssueUpdateCommand()){
                   displayVersion = dataset.getLatestVersion(); 
                } else {
                   displayVersion = dataset.getReleasedVersion();
                }                
            } else {
                displayVersion = datasetVersionService.find(versionId);
            }  

            ownerId = dataset.getOwner().getId();
            //displayVersion.setDatasetFields(displayVersion.initDatasetFields());
            if (dataset.getReleasedVersion() != null) {
                datasetNextMajorVersion = new Integer(dataset.getReleasedVersion().getVersionNumber().intValue() + 1).toString() + ".0";
                datasetNextMinorVersion = new Integer(dataset.getReleasedVersion().getVersionNumber().intValue()).toString() + "."
                        + new Integer(dataset.getReleasedVersion().getMinorVersionNumber().intValue() + 1).toString();
            }
            datasetVersionUI = new DatasetVersionUI(displayVersion);
            if (!dataset.isReleased() || (dataset.isReleased() && displayVersion.equals(dataset.getLatestVersion()) && !displayVersion.isDraft())) {
                displayCitation = dataset.getCitation(false, displayVersion);
            } else if (dataset.isReleased() && displayVersion.isDraft()) {
                displayCitation = dataset.getCitation(false, displayVersion.getMostRecentlyReleasedVersion());
            } else if (dataset.isReleased() && !displayVersion.equals(dataset.getLatestVersion())) {
                displayCitation = dataset.getCitation(false, displayVersion.getLargestMinorRelease());
            } else {
                displayCitation = "";
            }
            
            setVersionTabList(resetVersionTabList());

        } else if (ownerId != null) {
            // create mode for a new child dataset
            editMode = EditMode.CREATE;
            editVersion = dataset.getLatestVersion();
            dataset.setOwner(dataverseService.find(ownerId));
            datasetVersionUI = new DatasetVersionUI(editVersion);
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
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Add New Dataset", " - Enter metadata to create the dataset's citation. You can add more metadata about this dataset after it's created."));
        } else {
            throw new RuntimeException("On Dataset page without id or ownerid."); // improve error handling
        }
    }

    public void edit(EditMode editMode) {
        this.editMode = editMode;
        if (editMode == EditMode.INFO) {
            editVersion = dataset.getEditVersion();
        } else if (editMode == EditMode.FILE) {
            editVersion = dataset.getEditVersion();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Upload + Edit Dataset Files", " - You can drag and drop your files from your desktop, directly into the upload widget."));
        } else if (editMode == EditMode.METADATA) {
            editVersion = dataset.getEditVersion();
            datasetVersionUI = new DatasetVersionUI(editVersion);
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

    private String releaseDataset(boolean minor) {
        Command<Dataset> cmd;
        try {
            if (editMode == EditMode.CREATE) {
                cmd = new ReleaseDatasetCommand(dataset, session.getUser(), minor);
            } else {
                cmd = new ReleaseDatasetCommand(dataset, session.getUser(), minor);
            }
            dataset = commandEngine.submit(cmd);
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Release Failed", " - " + ex.toString()));
            Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, null, ex);
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetReleased", "Your dataset is now public.");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return "/dataset.xhtml?id=" + dataset.getId() + "&faces-redirect=true";
    }

    public void refresh(ActionEvent e) {
        int i = 0;
        // Go through the list of the files on the page...
        // (I've had to switch from going through the files via dataset.getFiles(), to 
        // .getLatestVersion().getFileMetadatas() - because that's how the page is
        // accessing them. -- L.A.)
        //for (DataFile dataFile : dataset.getFiles()) {
        for (FileMetadata fileMetadata : dataset.getLatestVersion().getFileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();
            // and see if any are marked as "ingest-in-progress":
            if (dataFile.isIngestInProgress()) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Refreshing the status of the file " + fileMetadata.getLabel() + "...");
                // and if so, reload the file object from the database...
                dataFile = datafileService.find(dataFile.getId());
                if (!dataFile.isIngestInProgress()) {
                    Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "File " + fileMetadata.getLabel() + " finished ingesting.");
                    // and, if the status has changed - i.e., if the ingest has 
                    // completed, or failed, update the object in the list of 
                    // files visible to the page:
                    fileMetadata.setDataFile(dataFile);
                }
            }
            i++;
        }
    }

    public String save() {
        dataset.setOwner(dataverseService.find(ownerId));
        //TODO get real application-wide protocol/authority
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("5555");

        /*
         * Save and/or ingest files, if there are any:
         */
        if (newFiles != null && newFiles.size() > 0) {
            try {
                if (dataset.getFileSystemDirectory() != null && !Files.exists(dataset.getFileSystemDirectory())) {
                    /* Note that "createDirectories()" must be used - not 
                     * "createDirectory()", to make sure all the parent 
                     * directories that may not yet exist are created as well. 
                     */

                    Files.createDirectories(dataset.getFileSystemDirectory());
                }
            } catch (IOException dirEx) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, "Failed to create study directory " + dataset.getFileSystemDirectory().toString());
            }

            if (dataset.getFileSystemDirectory() != null && Files.exists(dataset.getFileSystemDirectory())) {
                for (DataFile dFile : newFiles) {
                    String tempFileLocation = getFilesTempDirectory() + "/" + dFile.getFileSystemName();
                    
                    // These are all brand new files, so they should all have 
                    // one filemetadata total. You do NOT want to use 
                    // getLatestFilemetadata() here - because it relies on 
                    // comparing the object IDs of the corresponding datasetversions...
                    // Which may not have been persisted yet. 
                    // -- L.A. 4.0 beta.
                    FileMetadata fileMetadata = dFile.getFileMetadatas().get(0);
                    String fileName = fileMetadata.getLabel(); 

                    //boolean ingestedAsTabular = false;
                    boolean metadataExtracted = false;

                    datasetService.generateFileSystemName(dFile);

                    if (ingestService.ingestableAsTabular(dFile)) {
                        /*
                         * Note that we don't try to ingest the file right away - 
                         * instead we mark it as "scheduled for ingest", then at 
                         * the end of the save process it will be queued for async. 
                         * ingest in the background. In the meantime, the file 
                         * will be ingested as a regular, non-tabular file, and 
                         * appear as such to the user, until the ingest job is
                         * finished with the Ingest Service.
                         */
                        dFile.SetIngestScheduled();
                    } else if (ingestService.fileMetadataExtractable(dFile)) {

                        try {
                            dFile.setContentType("application/fits");
                            metadataExtracted = ingestService.extractIndexableMetadata(tempFileLocation, dFile, editVersion);
                        } catch (IOException mex) {
                            Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, "Caught exception trying to extract indexable metadata from file " + fileName, mex);
                        }
                        if (metadataExtracted) {
                            Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Successfully extracted indexable metadata from file " + fileName);
                        } else {
                            Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Failed to extract indexable metadata from file " + fileName);
                        }
                    }

                    // Try to save the file in its permanent location: 
                    //if (!ingestedAsTabular) {
                    try {

                        Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Will attempt to save the file as: " + dFile.getFileSystemLocation().toString());
                        Files.copy(new FileInputStream(new File(tempFileLocation)), dFile.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);

                        MD5Checksum md5Checksum = new MD5Checksum();
                        try {
                            dFile.setmd5(md5Checksum.CalculateMD5(dFile.getFileSystemLocation().toString()));
                        } catch (Exception md5ex) {
                            Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Could not calculate MD5 signature for the new file " + fileName);
                        }

                    } catch (IOException ioex) {
                        Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to save the file  " + dFile.getFileSystemLocation());
                    }
                    //}

                    // Any necessary post-processing: 
                    ingestService.performPostProcessingTasks(dFile);
                }
            }
        }

        Command<Dataset> cmd;
        try {
            if (editMode == EditMode.CREATE) {
                cmd = new CreateDatasetCommand(dataset, session.getUser());
            } else {
                cmd = new UpdateDatasetCommand(dataset, session.getUser());
            }
            dataset = commandEngine.submit(cmd);
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
            logger.info("Couldn't save dataset: " + error.toString());
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + error.toString()));
            return null;
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + ex.toString()));
            Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, null, ex);
        }
        newFiles.clear();
        editMode = null;

        // Queue the ingest jobs for asynchronous execution: 
        for (DataFile dataFile : dataset.getFiles()) {
            if (dataFile.isIngestScheduled()) {
                dataFile.SetIngestInProgress();
                Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Attempting to queue the file " + dataFile.getFileMetadata().getLabel() + " for ingest.");
                ingestService.asyncIngestAsTabular(dataFile);
            }
        }

        return "/dataset.xhtml?id=" + dataset.getId() + "&faces-redirect=true";
    }

    private String getFilesTempDirectory() {
        String filesRootDirectory = System.getProperty("dataverse.files.directory");
        if (filesRootDirectory == null || filesRootDirectory.equals("")) {
            filesRootDirectory = "/tmp/files";
        }

        String filesTempDirectory = filesRootDirectory + "/temp";

        if (!Files.exists(Paths.get(filesTempDirectory))) {
            /* Note that "createDirectories()" must be used - not 
             * "createDirectory()", to make sure all the parent 
             * directories that may not yet exist are created as well. 
             */
            try {
                Files.createDirectories(Paths.get(filesTempDirectory));
            } catch (IOException ex) {
                return null;
            }
        }

        return filesTempDirectory;
    }

    public void cancel() {
        // reset values
        dataset = datasetService.find(dataset.getId());
        ownerId = dataset.getOwner().getId();
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

            Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "DropBox url: " + fileLink + ", filename: " + fileName + ", size: " + fileSize);

            DataFile dFile = null;

            // Make http call, download the file: 
            GetMethod dropBoxMethod = new GetMethod(fileLink);
            int status = 0;
            InputStream dropBoxStream = null;
            try {
                status = getClient().executeMethod(dropBoxMethod);
                if (status == 200) {
                    dropBoxStream = dropBoxMethod.getResponseBodyAsStream();
                    dFile = new DataFile("application/octet-stream");
                    dFile.setOwner(dataset);

                    // save the file, in the temporary location for now: 
                    datasetService.generateFileSystemName(dFile);
                    if (getFilesTempDirectory() != null) {
                        Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Will attempt to save the DropBox file as: " + getFilesTempDirectory() + "/" + dFile.getFileSystemName());
                        Files.copy(dropBoxStream, Paths.get(getFilesTempDirectory(), dFile.getFileSystemName()), StandardCopyOption.REPLACE_EXISTING);
                        File tempFile = Paths.get(getFilesTempDirectory(), dFile.getFileSystemName()).toFile();
                        if (tempFile.exists()) {
                            long writtenBytes = tempFile.length();
                            Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "File size, expected: " + fileSize + ", written: " + writtenBytes);
                        } else {
                            throw new IOException();
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to access DropBox url: " + fileLink + "!");
                continue;
            } finally {
                if (dropBoxMethod != null) {
                    dropBoxMethod.releaseConnection();
                }
                if (dropBoxStream != null) {
                    try {
                        dropBoxStream.close();
                    } catch (Exception ex) {
                    }
                }
            }

            // If we've made it this far, we must have downloaded the file
            // successfully, so let's finish processing it as a new DataFile
            // object: 
            FileMetadata fmd = new FileMetadata();
            fmd.setDataFile(dFile);
            dFile.getFileMetadatas().add(fmd);
            fmd.setLabel(fileName);
            fmd.setCategory(dFile.getContentType());
            if (editVersion.getFileMetadatas() == null) {
                editVersion.setFileMetadatas(new ArrayList());
            }
            editVersion.getFileMetadatas().add(fmd);
            fmd.setDatasetVersion(editVersion);
            dataset.getFiles().add(dFile);

            // When uploading files from dropBox, we don't get the benefit of 
            // having the browser recognize the mime type of the file. So we'll 
            // have to rely on our own utilities (Jhove, etc.) to try and determine
            // what it is. 
            String fileType = null;
            try {
                fileType = FileUtil.determineFileType(Paths.get(getFilesTempDirectory(), dFile.getFileSystemName()).toFile(), fileName);
                Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "File utility recognized the file as " + fileType);
                if (fileType != null && !fileType.equals("")) {
                    dFile.setContentType(fileType);
                }
            } catch (IOException ex) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to run the file utility mime type check on file " + fileName);
            }

            newFiles.add(dFile);
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        DataFile dFile = new DataFile(uFile.getContentType());

        FileMetadata fmd = new FileMetadata();
        fmd.setLabel(uFile.getFileName());
        fmd.setCategory(dFile.getContentType());

        dFile.setOwner(dataset);
        fmd.setDataFile(dFile);

        dFile.getFileMetadatas().add(fmd);
        
        if (editVersion.getFileMetadatas() == null) {
            editVersion.setFileMetadatas(new ArrayList());
        }
        editVersion.getFileMetadatas().add(fmd);
        fmd.setDatasetVersion(editVersion);
        dataset.getFiles().add(dFile);

        datasetService.generateFileSystemName(dFile);

        // save the file, in the temporary location for now: 
        if (getFilesTempDirectory() != null) {
            try {

                Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Will attempt to save the file as: " + getFilesTempDirectory() + "/" + dFile.getFileSystemName());
                Files.copy(uFile.getInputstream(), Paths.get(getFilesTempDirectory(), dFile.getFileSystemName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ioex) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to save the file  " + dFile.getFileSystemName());
                return;
            }
        }

        // Let's try our own utilities (Jhove, etc.) to determine the file type 
        // of the uploaded file. (we may or may not do better than the browser,
        // which may have already recognized the type correctly...)
        String fileType = null;
        try {
            fileType = FileUtil.determineFileType(Paths.get(getFilesTempDirectory(), dFile.getFileSystemName()).toFile(), fmd.getLabel());
            Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "File utility recognized the file as " + fileType);
            if (fileType != null && !fileType.equals("")) {
                // let's look at the browser's guess regarding the mime type
                // of the file: 
                String bgType = dFile.getContentType();
                Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Browser recognized the file as " + bgType);

                if (bgType == null || bgType.equals("") || bgType.equalsIgnoreCase("application/octet-stream")) {
                    dFile.setContentType(fileType);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to run the file utility mime type check on file " + fmd.getLabel());
        }

        newFiles.add(dFile);

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
    
    private boolean canIssueUpdateCommand(){
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
        List <DatasetVersion> retList = new ArrayList();

            if (canIssueUpdateCommand()) {
                return dataset.getVersions();
            } else {
                for(DatasetVersion version: dataset.getVersions()){
                    if (version.isReleased()){
                        retList.add(version);
                    }
                }
                return retList;

            }
    }
 
}
