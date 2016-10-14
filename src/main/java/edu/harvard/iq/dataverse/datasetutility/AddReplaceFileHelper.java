/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.validation.ConstraintViolation;

/**
 *  Methods to add or replace a single file.
 * 
 *  Usage example:
 *
 * // (1) Instantiate the class
 * 
 *  AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(dvRequest2,
 *                                               this.ingestService,
 *                                               this.datasetService,
 *                                               this.fileService,
 *                                               this.permissionSvc,
 *                                               this.commandEngine);
 * 
 * // (2) Run file "ADD"
 *
 *  addFileHelper.runAddFileByDatasetId(datasetId,
 *                               newFileName,
 *                               newFileContentType,
 *                               newFileInputStream);
 *  // (2a) Check for errors
 *  if (addFileHelper.hasError()){
 *      // get some errors
 *      System.out.println(addFileHelper.getErrorMessagesAsString("\n"));
 *  }
 * 
 *
 * // OR (3) Run file "REPLACE"
 *
 *  addFileHelper.runReplaceFile(datasetId,
 *                               newFileName,
 *                               newFileContentType,
 *                               newFileInputStream,
 *                               fileToReplaceId);
 *  // (2a) Check for errors
 *  if (addFileHelper.hasError()){
 *      // get some errors
 *      System.out.println(addFileHelper.getErrorMessagesAsString("\n"));
 *  }
 *
 * 
 * 
 * @author rmp553
 */
public class AddReplaceFileHelper{
    
    private static final Logger logger = Logger.getLogger(AddReplaceFileHelper.class.getCanonicalName());

    
    public static String FILE_ADD_OPERATION = "FILE_ADD_OPERATION";
    public static String FILE_REPLACE_OPERATION = "FILE_REPLACE_OPERATION";
    public static String FILE_REPLACE_FORCE_OPERATION = "FILE_REPLACE_FORCE_OPERATION";
    
            
    private String currentOperation;
    
    // -----------------------------------
    // All the needed EJBs, passed to the constructor
    // -----------------------------------
    private IngestServiceBean ingestService;
    private DatasetServiceBean datasetService;
    private DataFileServiceBean fileService;        
    private PermissionServiceBean permissionService;
    private EjbDataverseEngine commandEngine;
    
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
    List<DataFile> initialFileList; 
    List<DataFile> finalFileList;
    
    // Ingested file 
    private DataFile newlyAddedFile; 
    
    // For error handling
    private boolean errorFound;
    private List<String> errorMessages;
    
    
  //  public AddReplaceFileHelper(){
  //      throw new IllegalStateException("Must be called with a dataset and or user");
  //  }
    
    
    /** 
     * MAIN CONSTRUCTOR -- minimal requirements
     * 
     * @param dataset
     * @param dvRequest 
     */

    public AddReplaceFileHelper(DataverseRequest dvRequest, 
                            IngestServiceBean ingestService,                            
                            DatasetServiceBean datasetService,
                            DataFileServiceBean fileService,
                            PermissionServiceBean permissionService,
                            EjbDataverseEngine commandEngine){

        // ---------------------------------
        // make sure DataverseRequest isn't null and has a user
        // ---------------------------------
        if (dvRequest == null){
            throw new NullPointerException("dvRequest cannot be null");
        }
        if (dvRequest.getUser() == null){
            throw new NullPointerException("dvRequest cannot have a null user");
        }

        // ---------------------------------
        // make sure services aren't null
        // ---------------------------------
        if (ingestService == null){
            throw new NullPointerException("ingestService cannot be null");
        }
        if (datasetService == null){
            throw new NullPointerException("datasetService cannot be null");
        }
        if (fileService == null){
            throw new NullPointerException("fileService cannot be null");
        }
        if (permissionService == null){
            throw new NullPointerException("ingestService cannot be null");
        }
        if (commandEngine == null){
            throw new NullPointerException("commandEngine cannot be null");
        }

        // ---------------------------------
        
        this.ingestService = ingestService;
        this.datasetService = datasetService;
        this.fileService = fileService;
        this.permissionService = permissionService;
        this.commandEngine = commandEngine;
        
        
        
        initErrorHandling();
        
        // Initiate instance vars
        this.dataset = null;
        this.dvRequest = dvRequest;
        this.user = dvRequest.getUser();
        
    }
    
    public boolean runAddFileByDatasetId(Long datasetId, String newFileName, String newFileContentType, InputStream newFileInputStream){
        
        msgt(">> runAddFileByDatasetId");

        initErrorHandling();
        this.currentOperation = FILE_ADD_OPERATION;
        
        if (!this.step_001_loadDatasetById(datasetId)){
            return false;
        }
        
        return this.runAddFile(this.dataset, newFileName, newFileContentType, newFileInputStream);
    }
    
    
    /**
     * After the constructor, this method is called to add a file
     * 
     * @param dataset
     * @param newFileName
     * @param newFileContentType
     * @param newFileInputStream
     * @return 
     */
    public boolean runAddFile(Dataset dataset, String newFileName, String newFileContentType, InputStream newFileInputStream){
        msgt(">> runAddFile");
        if (this.hasError()){
            return false;
        }
        this.currentOperation = FILE_ADD_OPERATION;
        
        return this.runAddReplaceFile(dataset, newFileName, newFileContentType, newFileInputStream, null);
    }
    

    /**
     * After the constructor, this method is called to replace a file
     * 
     * @param dataset
     * @param newFileName
     * @param newFileContentType
     * @param newFileInputStream
     * @return 
     */
    public boolean runForceReplaceFile(Dataset dataset, String newFileName, String newFileContentType, InputStream newFileInputStream, Long oldFileId){
        
        msgt(">> runForceReplaceFile");
        this.currentOperation = FILE_REPLACE_FORCE_OPERATION;

        if (oldFileId==null){
            this.addErrorSevere(getBundleErr("existing_file_to_replace_id_is_null"));
            return false;
        }
        
        return this.runAddReplaceFile(dataset, newFileName, newFileContentType, newFileInputStream, oldFileId);
    }
    

    public boolean runForceReplaceFileByDatasetId(Long datasetId, String newFileName, String newFileContentType, InputStream newFileInputStream, Long oldFileId){
        
        msgt(">> runAddFileByDatasetId");

        initErrorHandling();
        this.currentOperation = FILE_REPLACE_FORCE_OPERATION;
        
        if (!this.step_001_loadDatasetById(datasetId)){
            return false;
        }
        if (oldFileId==null){
            this.addErrorSevere(getBundleErr("existing_file_to_replace_id_is_null"));
            return false;
        }
        
        return this.runAddReplaceFile(this.dataset, newFileName, newFileContentType, newFileInputStream, oldFileId);
    }

    
    public boolean runReplaceFileByDatasetId(Long datasetId, String newFileName, String newFileContentType, InputStream newFileInputStream, Long oldFileId){
        
        msgt(">> runAddFileByDatasetId");

        initErrorHandling();
        this.currentOperation = FILE_REPLACE_OPERATION;
        
        if (!this.step_001_loadDatasetById(datasetId)){
            return false;
        }
        if (oldFileId==null){
            this.addErrorSevere(getBundleErr("existing_file_to_replace_id_is_null"));
            return false;
        }
        
        return this.runReplaceFile(this.dataset, newFileName, newFileContentType, newFileInputStream, oldFileId);
    }
    
    /**
     * After the constructor, this method is called to replace a file
     * 
     * @param dataset
     * @param newFileName
     * @param newFileContentType
     * @param newFileInputStream
     * @return 
     */
    public boolean runReplaceFile(Dataset dataset, String newFileName, String newFileContentType, InputStream newFileInputStream, Long oldFileId){
        
        msgt(">> runReplaceFile");
        this.currentOperation = FILE_REPLACE_OPERATION;

        if (oldFileId==null){
            this.addErrorSevere(getBundleErr("existing_file_to_replace_id_is_null"));
            return false;
        }
        
        return this.runAddReplaceFile(dataset, newFileName, newFileContentType, newFileInputStream, oldFileId);
    }
    
    /**
     * Here we're going to run through the steps to ADD or REPLACE a file
     * 
     * The difference between ADD and REPLACE (add/delete) is:
     * 
     *  oldFileId - For ADD, set to null
     *  oldFileId - For REPLACE, set to id of file to replace 
     * 
     * This has now been broken into Phase 1 and Phase 2
     * 
     * The APIs will use this method and call Phase 1 & Phase 2 consecutively
     * 
     * The UI will call Phase 1 on initial upload and 
     *   then run Phase 2 if the user chooses to save the changes.
     * 
     * 
     * @return 
     */
    private boolean runAddReplaceFile(Dataset dataset,  
            String newFileName, String newFileContentType, InputStream newFileInputStream,
            Long oldFileId){
        
        // Run "Phase 1" - Initial ingest of file + error check
        // But don't save the dataset version yet
        //
        boolean phase1Success = runAddReplacePhase1(dataset,  
                                        newFileName,  
                                        newFileContentType,  
                                        newFileInputStream,
                                        oldFileId);
        
        if (!phase1Success){
            return false;
        }
        
       
        return runAddReplacePhase2();
        
    }

    /**
     * For the UI: File add/replace has been broken into 2 steps
     * 
     * Phase 1 (here): Add/replace the file and make sure there are no errors
     *          But don't update the Dataset (yet)
     * 
     * @return 
     */
    public boolean runAddReplacePhase1(Dataset dataset,  
            String newFileName, String newFileContentType, InputStream newFileInputStream,
            Long oldFileId){
        
        if (this.hasError()){
            return false;   // possible to have errors already...
        }

        initErrorHandling();
        

        msgt("step_001_loadDataset");
        if (!this.step_001_loadDataset(dataset)){
            return false;
        }
        
        msgt("step_010_VerifyUserAndPermissions");
        if (!this.step_010_VerifyUserAndPermissions()){
            return false;
            
        }

        msgt("step_020_loadNewFile");
        if (!this.step_020_loadNewFile(newFileName, newFileContentType, newFileInputStream)){
            return false;
            
        }

        // Replace only step!
        if (isFileReplaceOperation()){
            
            msgt("step_025_loadFileToReplaceById");
            if (!this.step_025_loadFileToReplaceById(oldFileId)){
                return false;
            }
        }
        
        msgt("step_030_createNewFilesViaIngest");
        if (!this.step_030_createNewFilesViaIngest()){
            return false;
            
        }

        msgt("step_050_checkForConstraintViolations");
        if (!this.step_050_checkForConstraintViolations()){
            return false;
            
        }
        return true;
    }
    
    
    /**
     * For the UI: File add/replace has been broken into 2 steps
     * 
     * Phase 2 (here): Phase 1 has run ok, Update the Dataset -- issue the commands!
     * 
     * @return 
     */
    public boolean runAddReplacePhase2(){
        
        if (this.hasError()){
            return false;   // possible to have errors already...
        }

        if ((finalFileList ==  null)||(finalFileList.isEmpty())){
            addError(getBundleErr("phase2_called_early_no_new_files"));
            return false;
        }
        
         msgt("step_060_addFilesViaIngestService");
        if (!this.step_060_addFilesViaIngestService()){
            return false;
            
        }
        
        if (this.isFileReplaceOperation()){
            msgt("step_080_run_update_dataset_command_for_replace");
            if (!this.step_080_run_update_dataset_command_for_replace()){
                return false;            
            }
            
        }else{
            msgt("step_070_run_update_dataset_command");
            if (!this.step_070_run_update_dataset_command()){
                return false;            
            }
        }
        
        msgt("step_090_notifyUser");
        if (!this.step_090_notifyUser()){
            return false;            
        }

        msgt("step_100_startIngestJobs");
        if (!this.step_100_startIngestJobs()){
            return false;            
        }

        return true;
    }
    
    
    /**
     *  Get for currentOperation
     *  @return String
     */
    public String getCurrentOperation(){
        return this.currentOperation;
    }

    
    /**
     * Is this a file FORCE replace operation?
     * 
     * Only overrides warnings of content type change
     * 
     * @return 
     */
    public boolean isForceFileOperation(){
        
        return this.currentOperation.equals(FILE_REPLACE_FORCE_OPERATION);
    }
    
    /**
     * Is this a file replace operation?
     * @return 
     */
    public boolean isFileReplaceOperation(){
    
        if (this.currentOperation.equals(FILE_REPLACE_OPERATION)){
            return true;
        }else if (this.currentOperation.equals(FILE_REPLACE_FORCE_OPERATION)){
            return true;
        }
        return false;
    }
    
    /**
     * Is this a file add operation?
     * 
     * @return 
     */
    public boolean isFileAddOperation(){
    
        return this.currentOperation.equals(FILE_ADD_OPERATION);
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
     * Convenience method for getting bundle properties
     * 
     * @param msgName
     * @return 
     */
    private String getBundleMsg(String msgName, boolean isErr){
        if (msgName == null){
            throw new NullPointerException("msgName cannot be null");
        }
        if (isErr){        
            return ResourceBundle.getBundle("Bundle").getString("file.addreplace.error." + msgName);
        }else{
            return ResourceBundle.getBundle("Bundle").getString("file.addreplace.success." + msgName);
        }
       
    }
    
    /**
     * Convenience method for getting bundle error message
     * 
     * @param msgName
     * @return 
     */
    private String getBundleErr(String msgName){
        return this.getBundleMsg(msgName, true);
    }
    
    /**
     * Convenience method for getting bundle success message
     * 
     * @param msgName
     * @return 
     */
    private String getBundleSuccess(String msgName){
        return this.getBundleMsg(msgName, false);
    }
    
    
     
    /**
     * 
     */
    private boolean step_001_loadDataset(Dataset selectedDataset){

        if (this.hasError()){
            return false;
        }

        if (selectedDataset == null){
            this.addErrorSevere(getBundleErr("dataset_is_null"));
            return false;
        }

        dataset = selectedDataset;
        
        return true;
    }
    
    /**
     * 
     */
    private boolean step_001_loadDatasetById(Long datasetId){
        
        if (this.hasError()){
            return false;
        }

        if (datasetId == null){
            this.addErrorSevere(getBundleErr("dataset_id_is_null"));
            return false;
        }
        
        Dataset yeDataset = datasetService.find(datasetId);
        if (yeDataset == null){
            this.addError(getBundleErr("dataset_id_not_found") + " " + datasetId);
            return false;
        }      
       
        return step_001_loadDataset(yeDataset);
    }
    
    
    
        
    
    /**
     *  Step 10 Verify User and Permissions
     * 
     * 
     * @return 
     */
    private boolean step_010_VerifyUserAndPermissions(){
        
        if (this.hasError()){
            return false;
        }
        
        msg("dataset:" + dataset.toString());
        msg("Permission.EditDataset:" + Permission.EditDataset.toString());
        msg("dvRequest:" + dvRequest.toString());
        msg("permissionService:" + permissionService.toString());
        
        if (!permissionService.request(dvRequest).on(dataset).has(Permission.EditDataset)){
           addError(getBundleErr("no_edit_dataset_permission"));
           return false;
        }
        return true;

    }
    
    
    private boolean step_020_loadNewFile(String fileName, String fileContentType, InputStream fileInputStream){
        
        if (this.hasError()){
            return false;
        }
        
        if (fileName == null){
            this.addErrorSevere(getBundleErr("filename_is_null"));
            return false;
            
        }

        if (fileContentType == null){
            this.addErrorSevere(getBundleErr("file_content_type_is_null"));
            return false;
            
        }
        
        if (fileInputStream == null){
            this.addErrorSevere(getBundleErr("file_input_stream_is_null"));
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
    private boolean step_025_loadFileToReplace(DataFile existingFile){

        if (this.hasError()){
            return false;
        }
        
        if (existingFile == null){
            this.addErrorSevere(getBundleErr("existing_file_to_replace_is_null"));
            return false;
        }       
        
        if (!existingFile.getOwner().equals(this.dataset)){
            addError(getBundleErr("existing_file_to_replace_not_in_dataset"));
            return false;
        }
        
        if (!existingFile.isReleased()){
            addError(getBundleErr("unpublished_file_cannot_be_replaced"));
            return false;            
        }
        
              
        fileToReplace = existingFile;
        
        return true;
    }

    
    /**
     * Optional: old file to replace
     * 
     * @param oldFile
     * @return 
     */
    private boolean step_025_loadFileToReplaceById(Long dataFileId){
        
        if (this.hasError()){
            return false;
        }
        
        // This shouldn't happen, the public replace method should throw
        //  a NullPointerException
        //
        if (dataFileId == null){
            this.addErrorSevere(getBundleErr("existing_file_to_replace_id_is_null"));
            return false;
        }
        
        DataFile existingFile = fileService.find(dataFileId);

        if (existingFile == null){
            this.addError(getBundleErr("existing_file_to_replace_not_found_by_id") + " " + dataFileId);
            return false;
        }      
        
        return step_025_loadFileToReplace(existingFile);
    }
    
    
    private boolean step_030_createNewFilesViaIngest(){
        
        if (this.hasError()){
            return false;
        }

        // Load the working version of the Dataset
        workingVersion = dataset.getEditVersion();
        
        if (!step_035_auto_isReplacementInLatestVersion()){
            return false;
        }
        
        try {
            initialFileList = ingestService.createDataFiles(workingVersion,
                    this.newFileInputStream,
                    this.newFileName,
                    this.newFileContentType);

        } catch (IOException ex) {
            this.addErrorSevere(getBundleErr("ingest_create_file_err"));
            logger.severe(ex.toString());
            this.runMajorCleanup(); 
            return false;
        }
        
        
        /**
         * This only happens:
         *  (1) the dataset was empty
         *  (2) the new file (or new file unzipped) did not ingest via "createDataFiles"
         */
        if (initialFileList.isEmpty()){
            this.addErrorSevere("initial_file_list_empty");
            this.runMajorCleanup();
            return false;
        }
        
        if (initialFileList.size() > 1){
            this.addError("initial_file_list_more_than_one");
            this.runMajorCleanup();
            return false;
            
        }
        
        if (!this.step_040_auto_checkForDuplicates()){
            return false;
        }
                       

        return this.step_045_auto_checkForFileReplaceDuplicate();
    }
    
    /**
     * Make sure the file to replace is in the workingVersion
     *  -- e.g. that it wasn't deleted from a previous Version
     * 
     * @return 
     */
    private boolean step_035_auto_isReplacementInLatestVersion(){
        
        if (this.hasError()){
            return false;
        }
        if (!this.isFileReplaceOperation()){
            return true;
        }
        
        boolean fileInLatestVersion = false;
        for (FileMetadata fm : workingVersion.getFileMetadatas()){
            if (fm.getDataFile().getId() != null){
                if (Objects.equals(fileToReplace.getId(),fm.getDataFile().getId())){
                    fileInLatestVersion = true;
                }
            }
        }
        if (!fileInLatestVersion){
            addError(getBundleErr("existing_file_not_in_latest_published_version"));
            this.runMajorCleanup(); 
            return false;                        
        }
        return true;
    }
    
    /**
     * Create a "final file list" 
     * 
     * This is always run after step 30 -- the ingest
     * 
     * @return 
     */
    private boolean step_040_auto_checkForDuplicates(){
        
        msgt("step_040_auto_checkForDuplicates");
        if (this.hasError()){
            return false;
        }
        
        // Double checked -- this check also happens in step 30
        //
        if (initialFileList.isEmpty()){
            this.addErrorSevere("initial_file_list_empty");
            return false;
        }

        // Initialize new file list
        this.finalFileList = new ArrayList();

        String warningMessage  = null;
        

        // -----------------------------------------------------------
        // Iterate through the recently ingest files
        // -----------------------------------------------------------
        for (DataFile df : initialFileList){
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
                //removeUnSavedFilesFromWorkingVersion();
                //removeLinkedFileFromDataset(dataset, df);
                //abandonOperationRemoveAllNewFilesFromDataset();
                this.addErrorSevere(getBundleErr("duplicate_file") + " " + dupeName);   
                //return false;
            }else{
                finalFileList.add(df);
            }
        }
        
        if (this.hasError()){
            // We're recovering from the duplicate check.
            msg("We're recovering from a duplicate check 1");
            runMajorCleanup();
            msg("We're recovering from a duplicate check 2");
            finalFileList.clear();           
            return false;
        }
        
        if (finalFileList.size() > 1){            
            this.addErrorSevere("There is more than 1 file to add.  (This error shouldn't happen b/c the initial file list should always have 1 item");
            return false;
        }
        
        
        if (finalFileList.isEmpty()){
            this.addErrorSevere("There are no files to add.  (This error shouldn't happen if steps called in sequence....step_040_auto_checkForDuplicates)");                
            return false;
        }
        
        
        return true;
    } // end step_040_auto_checkForDuplicates
    
    
    /**
     * This is always checked.   
     * 
     * For ADD: If there is not replacement file, then the check is considered a success
     * For REPLACE: The checksum is examined against the "finalFileList" list
     * 
     */
    private boolean step_045_auto_checkForFileReplaceDuplicate(){
        
        if (this.hasError()){
            return false;
        }

        // Not a FILE REPLACE operation -- skip this step!!
        //
        if (!isFileReplaceOperation()){
            return true;
        }

        
        if (finalFileList.isEmpty()){
            // This error shouldn't happen if steps called in sequence....
            this.addErrorSevere("There are no files to add.  (This error shouldn't happen if steps called in sequence....checkForFileReplaceDuplicate)");                
            return false;
        }
        
        
        if (this.fileToReplace == null){
            // This error shouldn't happen if steps called correctly
            this.addErrorSevere(getBundleErr("existing_file_to_replace_is_null") + " (This error shouldn't happen if steps called in sequence....checkForFileReplaceDuplicate)");
            return false;
        }
    
        for (DataFile df : finalFileList){
            
            if (Objects.equals(df.getChecksumValue(), fileToReplace.getChecksumValue())){
                this.addError(getBundleErr("replace.new_file_same_as_replacement"));                                
                break;
            }
            
            // This should be able to be overridden --force
            if (!isForceFileOperation()){
                
                // Warning that content type of the file has changed
                //
                if (!df.getContentType().equalsIgnoreCase(fileToReplace.getContentType())){
                    this.addError(getBundleErr("replace.new_file_has_different_content_type"));
                    //+ " The new file,\"" + df.getFileMetadata().getLabel() 
                    //        + "\" has content type [" + df.getContentType() + "] while the replacment file, \"" 
                    //        + fileToReplace.getFileMetadata().getLabel() + "\" has content type: [" + fileToReplace.getContentType() + "]");                               
                }
            }
        }
        
        if (hasError()){
            runMajorCleanup();
            return false;
        }
        
        return true;
        
    } // end step_045_auto_checkForFileReplaceDuplicate
    
    
    
    private boolean step_050_checkForConstraintViolations(){
                
        if (this.hasError()){
            return false;
        }
        
        if (finalFileList.isEmpty()){
            // This error shouldn't happen if steps called in sequence....
            this.addErrorSevere(getBundleErr("final_file_list_empty"));
            return false;
        }

        // -----------------------------------------------------------
        // Iterate through checking for constraint violations
        //  Gather all error messages
        // -----------------------------------------------------------   
        Set<ConstraintViolation> constraintViolations = workingVersion.validate();    

        // -----------------------------------------------------------   
        // No violations found
        // -----------------------------------------------------------   
        if (constraintViolations.isEmpty()){
            return true;
        }
        
        // -----------------------------------------------------------   
        // violations found: gather all error messages
        // -----------------------------------------------------------   
        List<String> errMsgs = new ArrayList<>();
        for (ConstraintViolation violation : constraintViolations){
            this.addError(violation.getMessage());
        }
        
        return this.hasError();
    }
    
    
    private boolean step_060_addFilesViaIngestService(){
                       
        if (this.hasError()){
            return false;
        }
                
        if (finalFileList.isEmpty()){
            // This error shouldn't happen if steps called in sequence....
            this.addErrorSevere(getBundleErr("final_file_list_empty"));                
            return false;
        }
        
        ingestService.addFiles(workingVersion, finalFileList);

        return true;
    }
    
    
    /**
     * Create and run the update dataset command
     * 
     * @return 
     */
    private boolean step_070_run_update_dataset_command(){
        
        if (this.hasError()){
            return false;
        }

        Command<Dataset> update_cmd;
        update_cmd = new UpdateDatasetCommand(dataset, dvRequest);
        ((UpdateDatasetCommand) update_cmd).setValidateLenient(true);  
        
        try {            
            // Submit the update dataset command 
            // and update the local dataset object
            //
            dataset = commandEngine.submit(update_cmd);
        } catch (CommandException ex) {
            this.addErrorSevere(getBundleErr("add.command_engine_error"));
            logger.severe(ex.getMessage());
            return false;
        }catch (EJBException ex) {
            this.addErrorSevere("add.ejb_exception");
            logger.severe(ex.getMessage());
            return false;
        } 
        return true;
    }

    
    /**
     * Go through the working DatasetVersion and remove the
     * FileMetadata of the file to replace
     * 
     * @return 
     */
    private boolean step_085_auto_remove_filemetadata_to_replace_from_working_version(){

        msgt("step_085_auto_remove_filemetadata_to_replace_from_working_version 1");

        if (!isFileReplaceOperation()){
            // Shouldn't happen!
            this.addErrorSevere(getBundleErr("only_replace_operation") + " (step_085_auto_remove_filemetadata_to_replace_from_working_version");
            return false;
        }
        msg("step_085_auto_remove_filemetadata_to_replace_from_working_version 2");

        if (this.hasError()){
            return false;
        }

        
        msgt("File to replace getId: " + fileToReplace.getId());

        Iterator<FileMetadata> fmIt = workingVersion.getFileMetadatas().iterator();
        msgt("Clear file to replace");
        int cnt = 0;
        while (fmIt.hasNext()) {
           cnt++;
          
           FileMetadata fm = fmIt.next();
           msg(cnt + ") next file: " + fm);
           msg("   getDataFile().getId(): " + fm.getDataFile().getId());
           if (fm.getDataFile().getId() != null){
               if (Objects.equals(fm.getDataFile().getId(), fileToReplace.getId())){
                   msg("Let's remove it!");
                   fmIt.remove();
                   return true;
               }
           }
        }
        msg("No matches found!");
        addErrorSevere(getBundleErr("failed_to_remove_old_file_from_dataset"));
        runMajorCleanup();
        return false;
    }
    

    private boolean runMajorCleanup(){
        
        // (1) remove unsaved files from the working version
        removeUnSavedFilesFromWorkingVersion();
        
        // ----------------------------------------------------
        // (2) if the working version is brand new, delete it
        //      It doesn't have an "id" so you can't use the DeleteDatasetVersionCommand
        // ----------------------------------------------------
        // Remove this working version from the dataset
        Iterator<DatasetVersion> versionIterator = dataset.getVersions().iterator();
        msgt("Clear Files");
        while (versionIterator.hasNext()) {
            DatasetVersion dsv = versionIterator.next();
            if (dsv.getId() == null){
                versionIterator.remove();
            }
        }
        
        return true;
        
    }
    
    /**
     * We are outta here!  Remove everything unsaved from the edit version!
     * 
     * @return 
     */
    private boolean removeUnSavedFilesFromWorkingVersion(){
        msgt("Clean up: removeUnSavedFilesFromWorkingVersion");
        
        // -----------------------------------------------------------
        // (1) Remove all new FileMetadata objects
        // -----------------------------------------------------------                        
        //Iterator<FileMetadata> fmIt = dataset.getEditVersion().getFileMetadatas().iterator();//  
        Iterator<FileMetadata> fmIt = workingVersion.getFileMetadatas().iterator(); //dataset.getEditVersion().getFileMetadatas().iterator();//  
        while (fmIt.hasNext()) {
            FileMetadata fm = fmIt.next();
            if (fm.getDataFile().getId() == null){
                fmIt.remove();
            }
        }
        
        // -----------------------------------------------------------
        // (2) Remove all new DataFile objects
        // -----------------------------------------------------------                        
        Iterator<DataFile> dfIt = dataset.getFiles().iterator();
        msgt("Clear Files");
        while (dfIt.hasNext()) {
            DataFile df = dfIt.next();
            if (df.getId() == null){
                dfIt.remove();
            }
        }
        return true;
        
    }
    
    
    private boolean step_080_run_update_dataset_command_for_replace(){

        if (!isFileReplaceOperation()){
            // Shouldn't happen!
            this.addErrorSevere(getBundleErr("only_replace_operation") + " (step_080_run_update_dataset_command_for_replace)");
            return false;
        }

        if (this.hasError()){
            return false;
        }

        // -----------------------------------------------------------
        // Remove the "fileToReplace" from the current working version
        // -----------------------------------------------------------
        if (!step_085_auto_remove_filemetadata_to_replace_from_working_version()){
            return false;
        }


        
        // -----------------------------------------------------------
        // Set the "root file ids" and "previous file ids"
        // THIS IS A KEY STEP - SPLIT IT OUT
        //  (1) Old file: Set the Root File Id on the original file and save it
        //  (2) New file: Set the previousFileId to the id of the original file
        //  (3) New file: Set the rootFileId to the rootFileId of the original file
        // -----------------------------------------------------------
 
        
        /*
            Check the root file id on fileToReplace, updating it if necessary
        */
        if (fileToReplace.getRootDataFileId().equals(DataFile.ROOT_DATAFILE_ID_DEFAULT)){

            fileToReplace.setRootDataFileId(fileToReplace.getId());
            fileToReplace = fileService.save(fileToReplace);
        }
        
        /*
            Go through the final file list, settting the rootFileId and previousFileId
        */
        for (DataFile df : finalFileList){            
            df.setPreviousDataFileId(fileToReplace.getId());
            
            df.setRootDataFileId(fileToReplace.getRootDataFileId());
            
        }
        
       
        Command<Dataset> update_cmd;
        update_cmd = new UpdateDatasetCommand(dataset, dvRequest);


        ((UpdateDatasetCommand) update_cmd).setValidateLenient(true);
        

        try {        
            // Submit the update dataset command 
            // and update the local dataset object
            //
              dataset = commandEngine.submit(update_cmd);
          } catch (CommandException ex) {
              this.addErrorSevere(getBundleErr("replace.command_engine_error"));
              logger.severe(ex.getMessage());
              return false;
          }catch (EJBException ex) {
              this.addErrorSevere(getBundleErr("replace.ejb_exception"));
              logger.severe(ex.getMessage());
              return false;
          } 

        return true;
    }
            
    /**
     * We want the version of the newly added file that has an id set
     * 
     * TODO: This is inefficient/expensive.  Need to redo it in a sane way
     *      - e.g. Query to find 
     *          (1) latest dataset version in draft
     *          (2) pick off files that are NOT released
     *          (3) iterate through only those files
     *      - or an alternate/better version
     * 
     * @param df 
     */
    private void setNewlyAddedFile(DataFile df){
        
        newlyAddedFile = df;
        
        for (FileMetadata fm : dataset.getEditVersion().getFileMetadatas()){
            
            // Find a file where the checksum value and identifiers are the same..
            //
            if (newlyAddedFile.getChecksumValue().equals(fm.getDataFile().getChecksumValue())){
                if (newlyAddedFile.getStorageIdentifier().equals(fm.getDataFile().getStorageIdentifier())){
                    newlyAddedFile = fm.getDataFile();
                    break;
                }
            }
        }
        
    }

        
    public DataFile getNewlyAddedFile(){
        
        return newlyAddedFile;
    }
    
    public String getSuccessResult(){
        if (newlyAddedFile == null){
            return "Bad ERROR: Newly created file not found";
        }
        return newlyAddedFile.asJSON();
        
    }
    
    public JsonObject getSuccessResultAsGsonObject(){
        if (newlyAddedFile == null){
            throw new NullPointerException("Bad error: newlyAddedFile is null!");
        }
        return newlyAddedFile.asGsonObject(false);
        
    }
    
    
    /**
     * Currently this is a placeholder if we decide to send
     * user notifications.
     * 
     */
    private boolean step_090_notifyUser(){
        if (this.hasError()){
            return false;
        }
       
        // Create a notification!
       
        // skip for now 
        return true;
    }
    

    private boolean step_100_startIngestJobs(){
        if (this.hasError()){
            return false;
        }
                
        // Should only be one file in the list
        for (DataFile df : finalFileList){
            setNewlyAddedFile(df);
            //df.getFileMetadata();
            break;
        }
        
        // clear old file list
        //
        finalFileList.clear();

        // TODO: Need to run ingwest async......
        //if (true){
            //return true;
        //}
        
        msg("pre ingest start");
        // start the ingest!
        //
               
        ingestService.startIngestJobs(dataset, dvRequest.getAuthenticatedUser());
        
        msg("post ingest start");
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
            this.addErrorSevere(getBundleErr("remove_linked_file.dataset"));
            return false;
        }
        
        if (dataFileToRemove==null){
            this.addErrorSevere(getBundleErr("remove_linked_file.file"));       
            return false;
        }
        
        // -----------------------------------------------------------
        // (1) Remove file from filemetadata list
        // -----------------------------------------------------------                        
        Iterator<FileMetadata> fmIt = workingVersion.getFileMetadatas().iterator();
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
                msg("it worked");
                
                break;
            }else{
                msg("...ok");
            }
        }
        return true;
    }
    
    
    
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
    
    

/*
    1) Recovery from adding same file and duplicate being found
        - draft ok
        - published verion - nope
*/