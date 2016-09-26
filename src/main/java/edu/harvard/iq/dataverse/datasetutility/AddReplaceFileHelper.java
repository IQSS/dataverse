/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.api.FileUpload;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;

/**
 *  Methods to add or replace a single file.
 * 
 * @author rmp553
 */
@SessionScoped
public class AddReplaceFileHelper implements java.io.Serializable {
    
    private static final Logger logger = Logger.getLogger(AddReplaceFileHelper.class.getCanonicalName());

    @EJB
    IngestServiceBean ingestService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataFileServiceBean fileService;        
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    EjbDataverseEngine commandEngine;

    // -----------------------------------
    // Instance variables directly added
    // -----------------------------------
    private Dataset dataset;                    // constructor
    private DataverseRequest dvRequest;         // constructor
    private InputStream newFileInputStream;     // step 20
    private String newFileName;                 // step 20
    private String newFileContentType;          // step 20
    // -- Optional  
    private DataFile fileToReplace;             // step 25
    
    
    // Instance variables derived from other input
    private User user;
    private DatasetVersion workingVersion;
    List<DataFile> newFileList;
    List<DataFile> filesToAdd;
   
    
    // For error handling
    private boolean errorFound;
    private List<String> errorMessages;
    
    
    public AddReplaceFileHelper(){
        throw new IllegalStateException("Must be called with a dataset and or user");
    }
    
    
    /** 
     * MAIN CONSTRUCTOR -- minimal requirements
     * 
     * @param dataset
     * @param dvRequest 
     */

    public AddReplaceFileHelper(DataverseRequest dvRequest){


        if (dvRequest == null){
            throw new NullPointerException("dvRequest cannot be null");
        }
        if (dvRequest.getUser() == null){
            throw new NullPointerException("dvRequest cannot have a null user");
        }

        initErrorHandling();
        
        // Initiate instance vars
        this.dataset = null;
        this.dvRequest = dvRequest;
        this.user = dvRequest.getUser();
        
    }
    
   
    /**
     * Initialize error handling vars
     */
    private void initErrorHandling(){

        this.errorFound = false;
        this.errorMessages = new ArrayList<>();
        
    }
         
 
    
    

    /**
     * Add error message
     * 
     * @param errMsg 
     */
    private void addError(String errMsg){
        
        if (errMsg == null){
            throw new NullPointerException("errMsg cannot be null");
        }
        this.errorFound = true;
 
        logger.fine(errMsg);
        this.errorMessages.add(errMsg);
    }
    

    private void addErrorSevere(String errMsg){
        
        if (errMsg == null){
            throw new NullPointerException("errMsg cannot be null");
        }
        this.errorFound = true;
 
        logger.severe(errMsg);
        this.errorMessages.add(errMsg);
    }

    
    /**
     * Was an error found?
     * 
     * @return 
     */
    public boolean hasError(){
        return this.errorFound;
        
    }
    
    /**
     * get error messages
     * 
     * @return 
     */
    public List<String> getErrorMessages(){
        return this.errorMessages;
    }   

    /**
     * get error messages as string 
     * 
     * @param joinString
     * @return 
     */
    public String getErrorMessagesAsString(String joinString){
        if (joinString==null){
            joinString = "\n";
        }
        return String.join(joinString, this.errorMessages);
    }   

    
     /**
     * 
     */
    public boolean step_01_loadDataset(Dataset selectedDataset){

        if (this.hasError()){
            return false;
        }

        if (selectedDataset == null){
            throw new NullPointerException("dataset cannot be null");
        }

        dataset = selectedDataset;
        
        return true;
    }

    
    /**
     * 
     */
    public boolean step_01_loadDatasetById(Long datasetId){
        
        if (this.hasError()){
            return false;
        }

        if (datasetId == null){
            throw new NullPointerException("datasetId cannot be null");
        }
        
        dataset = datasetService.find(datasetId);
        if (dataset == null){
            this.addError("There was no dataset found for id: " + datasetId);
            return false;
        }      
       
        return true;
    }
    
    
    
        
    
    /**
     *  Step 10 Verify User and Permissions
     * 
     * 
     * @return 
     */
    public boolean step_10_VerifyUserAndPermissions(){
        
        if (this.hasError()){
            return false;
        }
        
        if (!permissionService.request(dvRequest).on(dataset).has(Permission.EditDataset)){
           String errMsg = "You do not have permission to this dataset.";
           addError(errMsg);
           return false;
        }
        return true;

    }
    
    
    public boolean step_20_loadNewFile(String fileName, String fileContentType, InputStream fileInputStream){
        
        if (this.hasError()){
            return false;
        }
        
        if (fileName == null){
            String errMsg = "The fileName cannot be null.";
            this.addErrorSevere(errMsg);
            return false;
            
        }

        if (fileContentType == null){
            String errMsg = "The fileContentType cannot be null.";
            this.addErrorSevere(errMsg);
            return false;
            
        }

        if (fileName == null){
            String errMsg = "The fileName cannot be null.";
            this.addErrorSevere(errMsg);
            return false;
            
        }
        

        if (fileInputStream == null){
            String errMsg = "The fileInputStream cannot be null.";
            this.addErrorSevere(errMsg);
            return false;
        }
       
        newFileName = fileName;
        newFileContentType = fileContentType;
        newFileInputStream = fileInputStream;
        
        return true;
    }
    
    /**
     * Optional: old file to replace
     * 
     * @param oldFile
     * @return 
     */
    public boolean step_25_loadFileToReplace(DataFile oldFile){

        if (this.hasError()){
            return false;
        }
        
        if (oldFile == null){
            throw new NullPointerException("fileToReplace cannot be null");
        }
        
        if (oldFile.getOwner() != this.dataset){
            String errMsg = "This file does not belong to the datset";
            addError(errMsg);
            return false;
        }
        
        fileToReplace = oldFile;
        
        return true;
    }

    
    /**
     * Optional: old file to replace
     * 
     * @param oldFile
     * @return 
     */
    public boolean step_25_loadFileToReplaceById(Long dataFileId){
        
        if (this.hasError()){
            return false;
        }
        
        if (dataFileId == null){
            throw new NullPointerException("dataFileId cannot be null");
        }
        
        fileToReplace = fileService.find(dataFileId);
        if (fileToReplace == null){
            this.addError("There was no file found for id: " + dataFileId);
            return false;
        }      
        
        return true;
    }
    
    
    public boolean step_30_createNewFilesViaIngest(){
        
        if (this.hasError()){
            return false;
        }

        // Load the working version of the Dataset
        workingVersion = dataset.getEditVersion();
        
        try {
            newFileList = ingestService.createDataFiles(workingVersion,
                    this.newFileInputStream,
                    this.newFileName,
                    this.newFileContentType);
        } catch (IOException ex) {
            String errMsg = "There was an error when trying to add the new file.";
            this.addErrorSevere(errMsg);
            logger.severe(ex.toString());
            return false;
        }
        
        
        /**
         * This only happens:
         *  (1) the dataset was empty
         *  (2) the new file (or new file unzipped) did not ingest via "createDataFiles"
         */
        if (newFileList.isEmpty()){
            this.addErrorSevere("Sorry! An error occurred and the new file was not added.");
            return false;
        }
        
        return this.run_auto_step_35_checkForDuplicates();
                       
    }
    
    /**
     * This is always run after step 30
     * 
     * @return 
     */
    public boolean run_auto_step_35_checkForDuplicates(){
        
        if (this.hasError()){
            return false;
        }
        
        // Double checked -- this check also happens in step 30
        //
        if (newFileList.isEmpty()){
            this.addErrorSevere("Sorry! An error occurred and the new file was not added.");
            return false;
        }

        // Initialize new file list
        this.filesToAdd = new ArrayList();

        String warningMessage  = null;
        

        // -----------------------------------------------------------
        // Iterate through the recently ingest files
        // -----------------------------------------------------------
        for (DataFile df : newFileList){
             msg("Checking file: " + df.getFileMetadata().getLabel());

            // -----------------------------------------------------------
            // (1) Check for ingest warnings
            // -----------------------------------------------------------
            if (df.isIngestProblem()) {
                if (df.getIngestReportMessage() != null) {
                    // may collect multiple error messages
                    this.addError(df.getIngestReportMessage());
                }
                df.setIngestDone();
            }
          
            
            // -----------------------------------------------------------
            // (2) Check for duplicates
            // -----------------------------------------------------------                        
            if (DuplicateFileChecker.isDuplicateOriginalWay(workingVersion, df.getFileMetadata())){

                String dupeName = df.getFileMetadata().getLabel();
                removeLinkedFileFromDataset(dataset, df);
                this.addErrorSevere("This file has a duplicate already in the dataset: " + dupeName);                
            }else{
                filesToAdd.add(df);
            }
        }
        
        if (this.hasError()){
            filesToAdd.clear();
            return false;
        }
        
        return true;
    } // end run_auto_step_35_checkForDuplicates
    
    
    public boolean step_40_checkForConstraintViolations(){
                
        if (this.hasError()){
            return false;
        }
        
        if (filesToAdd.isEmpty()){
            // This error shouldn't happen if steps called in sequence....
            this.addErrorSevere("There are no files to add.  (This error shouldn't happen if steps called in sequence....)");                
            return false;
        }

        // -----------------------------------------------------------
        // Iterate through checking for constraint violations
        //  Gather all error messages
        // -----------------------------------------------------------                        
        Set<ConstraintViolation> constraintViolations = workingVersion.validate();    
        List<String> errMsgs = new ArrayList<>();
        for (ConstraintViolation violation : constraintViolations){
            this.addError(violation.getMessage());
        }
        
        return this.hasError();
    }
    
    
    public boolean step_50_addFilesViaIngestService(){
                       
        if (this.hasError()){
            return false;
        }
                
        if (filesToAdd.isEmpty()){
            // This error shouldn't happen if steps called in sequence....
            this.addErrorSevere("There are no files to add.  (This error shouldn't happen if steps called in sequence....)");                
            return false;
        }
        
        ingestService.addFiles(workingVersion, filesToAdd);

        return true;
    }
    
    
    /**
     * Create and run the update dataset command
     * 
     * @return 
     */
    public boolean step_70_run_update_dataset_command(){
        
        if (this.hasError()){
            return false;
        }

        Command<Dataset> update_cmd;
        update_cmd = new UpdateDatasetCommand(dataset, dvRequest);
        ((UpdateDatasetCommand) update_cmd).setValidateLenient(true);  
        
        try {            
            commandEngine.submit(update_cmd);
        } catch (CommandException ex) {
            this.addErrorSevere("Failed to update the dataset.  Please contact the administrator");
            logger.severe(ex.getMessage());
            return false;
        }catch (EJBException ex) {
            this.addErrorSevere("Failed to update the dataset.  Please contact the administrator");
            logger.severe(ex.getMessage());
            return false;
        } 
        return true;
    }

    
    public boolean step_80_notifyUser(){
        if (this.hasError()){
            return false;
        }
       
        // Create a notification!
       
        // skip for now 
        return true;
    }
    

    public boolean step_100_startIngestJobs(){
        if (this.hasError()){
            return false;
        }
        
        // clear old file list
        //
        filesToAdd.clear();

        
        // start the ingest!
        //
        ingestService.startIngestJobs(dataset, dvRequest.getAuthenticatedUser());
        
        return true;
    }

    
    private void msg(String m){
        System.out.println(m);
    }
    private void dashes(){
        msg("----------------");
    }
    private void msgt(String m){
        dashes(); msg(m); dashes();
    }
    
    
    /*
    DatasetPage sequence:
    
    (A) editFilesFragment.xhtml -> EditDataFilesPage.handleFileUpload
    (B) EditDataFilesPage.java -> handleFileUpload
        (1) UploadedFile uf  event.getFile() // UploadedFile
            --------
                UploadedFile interface:
                    public String getFileName()
                    public InputStream getInputstream() throws IOException;
                    public long getSize();
                    public byte[] getContents();
                    public String getContentType();
                    public void write(String string) throws Exception;
            --------
        (2) List<DataFile> dFileList = null;     
        try {
            // Note: A single file may be unzipped into multiple files
            dFileList = ingestService.createDataFiles(workingVersion, uFile.getInputstream(), uFile.getFileName(), uFile.getContentType());
        }
    
        (3) processUploadedFileList(dFileList);

    (C) EditDataFilesPage.java -> processUploadedFileList
        - iterate through list of DataFile objects -- which COULD happen with a single .zip
            - isDuplicate check
            - if good:
                - newFiles.add(dataFile);        // looks good
                - fileMetadatas.add(dataFile.getFileMetadata());
            - return null;    // looks good, return null
    (D) save()  // in the UI, user clicks the button.  API is automatic if no errors
        
        (1) Look for constraintViolations:
            // DatasetVersion workingVersion;
            Set<ConstraintViolation> constraintViolations = workingVersion.validate();
                if (!constraintViolations.isEmpty()) {
                 //JsfHelper.addFlashMessage(JH.localize("dataset.message.validationError"));
                 JH.addMessage(FacesMessage.SEVERITY_ERROR, JH.localize("dataset.message.validationError"));
                //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "See below for details."));
                return "";
            }
    
         (2) Use the ingestService for a final check
            // ask Leonid if this is needed for API
            // One last check before we save the files - go through the newly-uploaded 
            // ones and modify their names so that there are no duplicates. 
            // (but should we really be doing it here? - maybe a better approach to do it
            // in the ingest service bean, when the files get uploaded.)
            // Finally, save the files permanently: 
            ingestService.addFiles(workingVersion, newFiles);

         (3) Use the API to save the dataset
            - make new CreateDatasetCommand
                - check if dataset has a template
            - creates UserNotification message
    
    */  
    // Checks:
    //   - Does the md5 already exist in the dataset?
    //   - If it's a replace, has the name and/or extension changed?
    //   On failure, send back warning
    //
    // - All looks good
    // - Create a DataFile
    // - Create a FileMetadata
    // - Copy the Dataset version, making a new DRAFT
    //      - If it's replace, don't copy the file being replaced
    // - Add this new file.
    // ....
    
    
    /**
     * When a duplicate file is found after the initial ingest,
     * remove the file from the dataset because
     * createDataFiles has already linked it to the dataset:
     *  - first, through the filemetadata list
     *  - then through tht datafiles list
     * 
     * 
     * @param dataset
     * @param dataFileToRemove 
     */
    private boolean removeLinkedFileFromDataset(Dataset dataset, DataFile dataFileToRemove){
        
        if (dataset==null){
            this.addErrorSevere("dataset cannot be null in removeLinkedFileFromDataset");
            return false;
        }
        
        if (dataFileToRemove==null){
            this.addErrorSevere("dataFileToRemove cannot be null in removeLinkedFileFromDataset");
            return false;
        }
        
        // -----------------------------------------------------------
        // (1) Remove file from filemetadata list
        // -----------------------------------------------------------                        
        Iterator<FileMetadata> fmIt = dataset.getEditVersion().getFileMetadatas().iterator();
        msgt("Clear FileMetadatas");
        while (fmIt.hasNext()) {
            FileMetadata fm = fmIt.next();
            msg("Check: " + fm);
            if (fm.getId() == null && dataFileToRemove.getStorageIdentifier().equals(fm.getDataFile().getStorageIdentifier())) {
                msg("Got It! ");
                fmIt.remove();
                break;
            }
        }
        
        
        // -----------------------------------------------------------
        // (2) Remove file from datafiles list
        // -----------------------------------------------------------                        
        Iterator<DataFile> dfIt = dataset.getFiles().iterator();
        msgt("Clear Files");
        while (dfIt.hasNext()) {
            DataFile dfn = dfIt.next();
            msg("Check: " + dfn);
            if (dfn.getId() == null && dataFileToRemove.getStorageIdentifier().equals(dfn.getStorageIdentifier())) {
                msg("Got It! try to remove from iterator");
                
                dfIt.remove();
                msg("it work");
                
                break;
            }else{
                msg("...ok");
            }
        }
        return true;
    }
    
    
    
}
