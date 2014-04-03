/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.util.MD5Checksum;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

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
    DataFileServiceBean datafileService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    TemplateServiceBean templateService;
    @EJB
    DatasetFieldServiceBean fieldService;
    @EJB
    VariableServiceBean variableService;
    @EJB
    IngestServiceBean ingestService;
    @Inject
    DataverseSession session;


    private Dataset dataset = new Dataset();
    private EditMode editMode;
    private Long ownerId;
    private int selectedTabIndex;
    private Map<UploadedFile, DataFile> newFiles = new HashMap();
    private DatasetVersion editVersion = new DatasetVersion();
    private DatasetVersionUI datasetVersionUI = new DatasetVersionUI();
    private List<DatasetField> deleteRecords = new ArrayList();


    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
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

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }

    public void init() {

        if (dataset.getId() != null) { // view mode for a dataset           
            dataset = datasetService.find(dataset.getId());
            editVersion = dataset.getLatestVersion();
            ownerId = dataset.getOwner().getId();
            editVersion.setDatasetFields(editVersion.initDatasetFields());
            datasetVersionUI = new DatasetVersionUI(editVersion);
        } else if (ownerId != null) {
            // create mode for a new child dataset
            editMode = EditMode.CREATE;
            dataset.setOwner(dataverseService.find(ownerId));
            dataset.setVersions(new ArrayList());
            editVersion.setDataset(dataset);
            editVersion.setFileMetadatas(new ArrayList());
            editVersion.setVersionState(VersionState.DRAFT);
            editVersion.setDatasetFields(editVersion.initDatasetFields());
            editVersion.setVersionNumber(new Long(1));
            datasetVersionUI = new DatasetVersionUI(editVersion);
            //TODO add call to initDepositFields if it's decided that they are indeed metadata
            //initDepositFields();
            dataset.getVersions().add(editVersion);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Add New Dataset", " - Enter metadata to create the dataset's citation. You can add more metadata about this dataset after it's created."));
        } else {
            throw new RuntimeException("On Dataset page without id or ownerid."); // improve error handling
        }
    }
    /*
     private void initDepositFields(){
     //Special Handling - fill depositor and deposit date if blank
     //add initial values for Depositor and Desposit Date
     for(DatasetFieldValue dsfv : editVersion.getDatasetFieldValues()){
     if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.depositor) && dsfv.isEmpty()) {
     if (session.getUser() != null && session.getUser().getLastName() != null && session.getUser().getFirstName() != null  ){
     dsfv.setStrValue(session.getUser().getLastName() + ", " + session.getUser().getFirstName());
     }
     }
     DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
     Date date = new Date();
     if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.dateOfDeposit) && dsfv.isEmpty()) {
     dsfv.setStrValue(dateFormat.format(date));
     }
            
     }            
     }
     */

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

    public void addGeneralRecord(Object recordType) {
        //The page provides the value record to be added        
        DatasetField dfvType = (DatasetField) recordType;
        DatasetField addNew = new DatasetField();
        addNew.setDatasetVersion(editVersion);
        addNew.setDatasetFieldType(dfvType.getDatasetFieldType());
        //If there are children create them and add to map list        
        if (dfvType.getDatasetFieldType().isHasChildren()) {
            addNew = addChildren(addNew);
        }
        // add parent value
        editVersion.getDatasetFields().add(addNew);
        //Refresh the UI to add the new fields to the blocks
        datasetVersionUI = new DatasetVersionUI(editVersion);
    }

    private DatasetField addChildren(DatasetField dsfvIn) {/*
         dsfvIn.setChildDatasetFieldValues(new ArrayList());
         for (DatasetFieldType dsfc : dsfvIn.getDatasetField().getChildDatasetFields()) {
         DatasetFieldValue cv = new DatasetFieldValue();
         cv.setParentDatasetFieldValue(dsfvIn);
         cv.setDatasetField(dsfc);
         cv.setDatasetVersion(editVersion);
         dsfvIn.getChildDatasetFieldValues().add(cv);
         editVersion.getDatasetFieldValues().add(cv);
         }*/

        return dsfvIn;
    }

    public void deleteGeneralRecord(Object toDeleteIn) {/*
         DatasetFieldValue toDelete = (DatasetFieldValue) toDeleteIn;
         //Delete children if any
         if (toDelete.getChildDatasetFieldValues() != null && !toDelete.getChildDatasetFieldValues().isEmpty()) {
         for (DatasetFieldValue dsfvDelete : toDelete.getChildDatasetFieldValues()) {
         editVersion.getDatasetFieldValues().remove(dsfvDelete);
         this.deleteRecords.add(dsfvDelete);
         }
         }
         editVersion.getDatasetFieldValues().remove(toDelete);
         this.deleteRecords.add(toDelete);
         datasetVersionUI = new DatasetVersionUI(editVersion);*/
    }

     public String releaseDataset() {
        dataset = datasetService.find(dataset.getId());
        dataset.setReleaseDate(new Timestamp(new Date().getTime()));
        dataset.setReleaseUser(session.getUser());
        dataset.getEditVersion().setReleaseTime(new Timestamp(new Date().getTime()));
        dataset.getEditVersion().setVersionState(VersionState.RELEASED);
        dataset = datasetService.CreateDatasetCommand(dataset);
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetReleased", "Your dataset is now public.");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return "/dataset.xhtml?id=" + dataset.getId() + "&faces-redirect=true";
    }

    public String save() {
        dataset.setOwner(dataverseService.find(ownerId));
        //TODO get real application-wide protocol/authority
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("5555");
        //Todo pre populate deposit date


        if (!(dataset.getVersions().get(0).getFileMetadatas() == null) && !dataset.getVersions().get(0).getFileMetadatas().isEmpty()) {
            int fmdIndex = 0;
            for (FileMetadata fmd : dataset.getVersions().get(0).getFileMetadatas()) {
                for (FileMetadata fmdTest : editVersion.getFileMetadatas()) {
                    if (fmd.equals(fmdTest)) {
                        dataset.getVersions().get(0).getFileMetadatas().get(fmdIndex).setDataFile(fmdTest.getDataFile());
                    }
                }
                fmdIndex++;
            }
        }

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
                for (UploadedFile uFile : newFiles.keySet()) {
                    DataFile dFile = newFiles.get(uFile);
                    String tempFileLocation = getFilesTempDirectory() + "/" + dFile.getFileSystemName();

                    boolean ingestedAsTabular = false;
                    boolean metadataExtracted = false;

                    datasetService.generateFileSystemName(dFile);

                    if (ingestService.ingestableAsTabular(dFile)) {

                        try {
                            ingestedAsTabular = ingestService.ingestAsTabular(tempFileLocation, dFile);
                            dFile.setContentType("text/tab-separated-values");
                        } catch (IOException iex) {
                            Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, null, iex);
                            ingestedAsTabular = false;
                        }
                    } else if (ingestService.fileMetadataExtractable(dFile)) {

                        try {
                            dFile.setContentType("application/fits");
                            metadataExtracted = ingestService.extractIndexableMetadata(tempFileLocation, dFile, editVersion);
                        } catch (IOException mex) {
                            Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, "Caught exception trying to extract indexable metadata from file " + dFile.getName(), mex);
                        }
                        if (metadataExtracted) {
                            Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Successfully extracted indexable metadata from file " + dFile.getName());
                        } else {
                            Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Failed to extract indexable metadata from file " + dFile.getName());
                        }
                    }

                    /* Try to save the file in its permanent location: 
                     * (unless it was already ingested and saved as tabular data) 
                     */
                    if (!ingestedAsTabular) {
                        try {

                            Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Will attempt to save the file as: " + dFile.getFileSystemLocation().toString());
                            Files.copy(uFile.getInputstream(), dFile.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);

                            MD5Checksum md5Checksum = new MD5Checksum();
                            try {
                                dFile.setmd5(md5Checksum.CalculateMD5(dFile.getFileSystemLocation().toString()));
                            } catch (Exception md5ex) {
                                Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Could not calculate MD5 signature for the new file " + dFile.getName());
                            }

                        } catch (IOException ioex) {
                            Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to save the file  " + dFile.getFileSystemLocation());
                        }
                    }

                    // Any necessary post-processing: 
                    
                    ingestService.performPostProcessingTasks(dFile);
                }
            }
        }

        try {
            dataset = datasetService.save(dataset);
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
        }
        newFiles.clear();
        editMode = null;

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

    public void handleFileUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        DataFile dFile = new DataFile(uFile.getFileName(), uFile.getContentType());
        FileMetadata fmd = new FileMetadata();
        dFile.setOwner(dataset);
        fmd.setDataFile(dFile);
        dFile.getFileMetadatas().add(fmd);
        fmd.setLabel(dFile.getName());
        fmd.setCategory(dFile.getContentType());
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

                Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Will attempt to save the file as: " + getFilesTempDirectory() + "/" + dFile.getFileSystemName());
                Files.copy(uFile.getInputstream(), Paths.get(getFilesTempDirectory(), dFile.getFileSystemName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ioex) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to save the file  " + dFile.getFileSystemName());
            }
        }
        newFiles.put(uFile, dFile);

    }

    public DatasetVersionUI getDatasetVersionUI() {
        return datasetVersionUI;
    }

    //boolean for adding "Replication for" to title
    private boolean replicationFor;

    public boolean isReplicationFor() {
        return replicationFor;
    }

    public void setReplicationFor(boolean replicationFor) {
        this.replicationFor = replicationFor;
    }    
    
}
