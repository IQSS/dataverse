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
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.json.JsonObjectBuilder;
import javax.validation.ConstraintViolation;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.ocpsoft.common.util.Strings;

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
    private SystemConfig systemConfig;
    
    // -----------------------------------
    // Instance variables directly added
    // -----------------------------------
    private Dataset dataset;                    // constructor (for add, not replace)
    private DataverseRequest dvRequest;         // constructor
    private InputStream newFileInputStream;     // step 20
    private String newFileName;                 // step 20
    private String newFileContentType;          // step 20
    // -- Optional  
    private DataFile fileToReplace;             // step 25
    
    
    // -----------------------------------
    // Instance variables derived from other input
    // -----------------------------------
    private User user;
    private DatasetVersion workingVersion;
    List<DataFile> initialFileList; 
    List<DataFile> finalFileList;
    
    // -----------------------------------
    // Ingested files
    // -----------------------------------
    private List<DataFile> newlyAddedFiles; 
    private List<FileMetadata> newlyAddedFileMetadatas;
    // -----------------------------------
    // For error handling
    // -----------------------------------
    
    private boolean errorFound;
    private List<String> errorMessages;
    private Response.Status httpErrorCode; // optional
    
    // For Force Replace, this becomes a warning rather than an error
    //
    private boolean contentTypeWarningFound;
    private String contentTypeWarningString;
    
    public void resetFileHelper(){
        
        initErrorHandling();
        
        // operation
        currentOperation = null;
        
        // dataset level
        dataset = null;
        
        // file to replace
        fileToReplace = null;
        
        newFileInputStream = null;    
        newFileName = null;    
        newFileContentType = null;    
    
        // file lists
        initialFileList = null;
        finalFileList = null;
        
        // final files
        newlyAddedFiles = null;
        newlyAddedFileMetadatas = null;
        
    }
    
    /** 
     * MAIN CONSTRUCTOR -- minimal requirements
     * 
     * @param dataset
     * @param ingestService
     * @param datasetService
     * @param dvRequest 
     */
    public AddReplaceFileHelper(DataverseRequest dvRequest, 
                            IngestServiceBean ingestService,                            
                            DatasetServiceBean datasetService,
                            DataFileServiceBean fileService,
                            PermissionServiceBean permissionService,
                            EjbDataverseEngine commandEngine,
                            SystemConfig systemConfig){

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
        if (systemConfig == null) {
            throw new NullPointerException("systemConfig cannot be null");
        }

        // ---------------------------------
        
        this.ingestService = ingestService;
        this.datasetService = datasetService;
        this.fileService = fileService;
        this.permissionService = permissionService;
        this.commandEngine = commandEngine;
        this.systemConfig = systemConfig;
        
        
        
        initErrorHandling();
        
        // Initiate instance vars
        this.dataset = null;
        this.dvRequest = dvRequest;
        this.user = dvRequest.getUser();
        
    }
    
    /**
     * 
     * @param chosenDataset
     * @param newFileName
     * @param newFileContentType
     * @param newFileInputStream
     * @param optionalFileParams
     * @return 
     */
    public boolean runAddFileByDataset(Dataset chosenDataset, 
            String newFileName, 
            String newFileContentType, 
            InputStream newFileInputStream,
            OptionalFileParams optionalFileParams){
        
        msgt(">> runAddFileByDatasetId");

        initErrorHandling();
        
        this.currentOperation = FILE_ADD_OPERATION;
        
        if (!this.step_001_loadDataset(chosenDataset)){
            return false;
        }
        
        //return this.runAddFile(this.dataset, newFileName, newFileContentType, newFileInputStream, optionalFileParams);
        return this.runAddReplaceFile(dataset, newFileName, newFileContentType, newFileInputStream, optionalFileParams);

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
    /*
    public boolean runAddFile(Dataset dataset,
                            String newFileName, 
                            String newFileContentType, 
                            InputStream newFileInputStream, 
                            OptionalFileParams optionalFileParams){
        msgt(">> runAddFile");
        
        initErrorHandling();
        
        if (this.hasError()){
            return false;
        }
        this.currentOperation = FILE_ADD_OPERATION;
        
        return this.runAddReplaceFile(dataset, newFileName, newFileContentType, newFileInputStream, optionalFileParams);
    }*/
    

    /**
     * After the constructor, this method is called to replace a file
     * 
     * @param dataset
     * @param newFileName
     * @param newFileContentType
     * @param newFileInputStream
     * @return 
     */
    public boolean runForceReplaceFile(Long oldFileId,
                        String newFileName, 
                        String newFileContentType, 
                        InputStream newFileInputStream,
                        OptionalFileParams optionalFileParams){
        
        msgt(">> runForceReplaceFile");
        initErrorHandling();

        this.currentOperation = FILE_REPLACE_FORCE_OPERATION;

               
        if (oldFileId==null){
            this.addErrorSevere(getBundleErr("existing_file_to_replace_id_is_null"));
            return false;
        }
       
        // Loads local variable "fileToReplace"
        //
        if (!this.step_005_loadFileToReplaceById(oldFileId)){
            return false;
        }

        
        return this.runAddReplaceFile(fileToReplace.getOwner(), newFileName, newFileContentType, newFileInputStream, optionalFileParams);
    }
    


    
    public boolean runReplaceFile(Long oldFileId,
                            String newFileName, 
                            String newFileContentType, 
                            InputStream newFileInputStream,
                            OptionalFileParams optionalFileParams){
    
        msgt(">> runReplaceFile");

        initErrorHandling();
        this.currentOperation = FILE_REPLACE_OPERATION;
        
        if (oldFileId==null){
            this.addErrorSevere(getBundleErr("existing_file_to_replace_id_is_null"));
            return false;
        }
        
         
        // Loads local variable "fileToReplace"
        //
        if (!this.step_005_loadFileToReplaceById(oldFileId)){
            return false;
        }

        return this.runAddReplaceFile(fileToReplace.getOwner(), newFileName, newFileContentType, newFileInputStream, optionalFileParams);
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
            String newFileName, String newFileContentType, 
            InputStream newFileInputStream,
            OptionalFileParams optionalFileParams){
        
        // Run "Phase 1" - Initial ingest of file + error check
        // But don't save the dataset version yet
        //
        boolean phase1Success = runAddReplacePhase1(dataset,  
                                        newFileName,  
                                        newFileContentType,  
                                        newFileInputStream,
                                        optionalFileParams
                                        );
        
        if (!phase1Success){
            return false;
        }
        
       
        return runAddReplacePhase2();
        
    }

    /**
     * Note: UI replace is always a "force replace" which means
     *  the replacement file can have a different content type
     * 
     * @param oldFileId
     * @param newFileName
     * @param newFileContentType
     * @param newFileInputStream
     * @param optionalFileParams
     * @return 
     */
    public boolean runReplaceFromUI_Phase1(Long oldFileId,  
            String newFileName, 
            String newFileContentType,
            InputStream newFileInputStream,
            OptionalFileParams optionalFileParams){
        
        
        initErrorHandling();
        this.currentOperation = FILE_REPLACE_FORCE_OPERATION;
        
        if (oldFileId==null){
            this.addErrorSevere(getBundleErr("existing_file_to_replace_id_is_null"));
            return false;
        }
        
         
        // Loads local variable "fileToReplace"
        //
        if (!this.step_005_loadFileToReplaceById(oldFileId)){
            return false;
        }

        return this.runAddReplacePhase1(fileToReplace.getOwner(), 
                newFileName, 
                newFileContentType, 
                newFileInputStream, 
                optionalFileParams);

       
    }
    
    
    /**
     * For the UI: File add/replace has been broken into 2 steps
     * 
     * Phase 1 (here): Add/replace the file and make sure there are no errors
     *          But don't update the Dataset (yet)
     * 
     * @return 
     */
    private boolean runAddReplacePhase1(Dataset dataset,  
            String newFileName, 
            String newFileContentType,
            InputStream newFileInputStream,
            OptionalFileParams optionalFileParams){
        
        if (this.hasError()){
            return false;   // possible to have errors already...
        }

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
        
        msgt("step_030_createNewFilesViaIngest");
        if (!this.step_030_createNewFilesViaIngest()){
            return false;
            
        }

        msgt("step_050_checkForConstraintViolations");
        if (!this.step_050_checkForConstraintViolations()){
            return false;            
        }
        
        msgt("step_055_loadOptionalFileParams");
        if (!this.step_055_loadOptionalFileParams(optionalFileParams)){
            return false;            
        }
        
        return true;
    }
    
    
    public boolean runReplaceFromUI_Phase2(){
        return runAddReplacePhase2();
    }
    

    /**
     * Called from the UI backing bean
     * 
     * @param categoriesList
     * @return 
     */
    public boolean updateCategoriesFromUI(List<String> categoriesList){
        if (hasError()){
            logger.severe("Should not be calling this method");
            return false;
        }
        
        if ((finalFileList==null)||(finalFileList.size()==0)){
            throw new NullPointerException("finalFileList needs at least 1 file!!");
        }
        
        // don't need to make updates
        //
        if (categoriesList ==null){
            return true;           
        }
        
        // remove nulls, dupes, etc.
        //
        categoriesList = Util.removeDuplicatesNullsEmptyStrings(categoriesList);
        if (categoriesList.isEmpty()){
            return true;
        }
        
        for (DataFile df : finalFileList){
            
            df.getFileMetadata().setCategoriesByName(categoriesList);
        }
        
        return true;
    }
    
    /**
     * Called from the UI backing bean

     * @param label
     * @param description
     * @param restricted
     * @return 
     */
    public boolean updateLabelDescriptionRestrictedFromUI(String label, String description, Boolean restricted){
                
        if (hasError()){
            logger.severe("Should not be calling this method");
            return false;
        }
        
        if ((finalFileList==null)||(finalFileList.size()==0)){
            throw new NullPointerException("finalFileList needs at least 1 file!!");
        }
        
        
        for (DataFile df : finalFileList){
            
            // update description
            if (description != null){
                df.getFileMetadata().setDescription(description.trim());
            }        

            // update label
            if (label != null){
                df.getFileMetadata().setLabel(label.trim());
            }               
            
            // update restriction
            if (restricted == null){
                restricted = false;
            }
            
            df.getFileMetadata().setRestricted(restricted);
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
    private boolean runAddReplacePhase2(){
        
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
        this.httpErrorCode = null;
        
        
        contentTypeWarningFound = false;
        contentTypeWarningString = null;
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
    
    /**
     * Add Error mesage and, if it's known, the HTTP response code
     * 
     * @param badHttpResponse, e.g. Response.Status.FORBIDDEN
     * @param errMsg 
     */
    private void addError(Response.Status badHttpResponse, String errMsg){
        
        if (badHttpResponse == null){
            throw new NullPointerException("badHttpResponse cannot be null");
        }
        if (errMsg == null){
            throw new NullPointerException("errMsg cannot be null");
        }
      
        this.httpErrorCode = badHttpResponse;
        
        this.addError(errMsg);
                
        
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
     * For API use, return the HTTP error code
     * 
     * Default is BAD_REQUEST
     * 
     * @return 
     */
    public Response.Status getHttpErrorCode(){
       
        if (!hasError()){
            logger.severe("Do not call this method unless there is an error!  check '.hasError()'");
        }
        
        if (httpErrorCode == null){
            return Response.Status.BAD_REQUEST;
        }else{
            return httpErrorCode;
        }
    }
    
    
    /**
     * Convenience method for getting bundle properties
     * 
     * @param msgName
     * @return 
     * @deprecated This method is deprecated because you have to know to search
     * only part of a bundle key ("add_file_error") rather than the full bundle
     * key ("file.addreplace.error.add.add_file_error") leading you to believe
     * that the bundle key is not used.
     */
    @Deprecated
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
     *  Step 10 Verify User and Permissions
     * 
     * 
     * @return 
     */
    private boolean step_010_VerifyUserAndPermissions(){
        
        if (this.hasError()){
            return false;
        }
                
        return step_015_auto_check_permissions(dataset);

    }
    
    private boolean step_015_auto_check_permissions(Dataset datasetToCheck){
        
        if (this.hasError()){
            return false;
        }
        
        if (datasetToCheck == null){
            addError(getBundleErr("dataset_is_null"));
            return false;
        }
        
        // Make a temp. command
        //
        CreateDatasetCommand createDatasetCommand = new CreateDatasetCommand(datasetToCheck, dvRequest, false);
        
        // Can this user run the command?
        //
        if (!permissionService.isUserAllowedOn(dvRequest.getUser(), createDatasetCommand, datasetToCheck)) {
            addError(Response.Status.FORBIDDEN,getBundleErr("no_edit_dataset_permission"));
           return false;
        }
        
        return true;
        
    }
    
    
    private boolean step_020_loadNewFile(String fileName, String fileContentType, InputStream fileInputStream){
        
        if (this.hasError()){
            return false;
        }
        
        if (fileName == null){
            this.addErrorSevere(getBundleErr("filename_undetermined"));
            return false;
            
        }

        if (fileContentType == null){
            this.addErrorSevere(getBundleErr("file_content_type_undetermined"));
            return false;
            
        }
        
        if (fileInputStream == null){
            this.addErrorSevere(getBundleErr("file_upload_failed"));
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
    private boolean step_005_loadFileToReplaceById(Long dataFileId){
        
        if (this.hasError()){
            return false;
        }
        
        //  Check for Null
        //
        if (dataFileId == null){
            this.addErrorSevere(getBundleErr("existing_file_to_replace_id_is_null"));
            return false;
        }
        
        // Does the file exist?
        //
        DataFile existingFile = fileService.find(dataFileId);

        if (existingFile == null){           
            this.addError(BundleUtil.getStringFromBundle("file.addreplace.error.existing_file_to_replace_not_found_by_id", Collections.singletonList(dataFileId.toString())));
            return false;
        } 
        

        // Do we have permission to replace this file? e.g. Edit the file's dataset
        //
        if (!step_015_auto_check_permissions(existingFile.getOwner())){
            return false;
        };

        
        
        // Is the file published?
        //
        if (!existingFile.isReleased()){
            addError(getBundleErr("unpublished_file_cannot_be_replaced"));
            return false;            
        }
        
        // Is the file in the latest dataset version?
        //
        if (!step_007_auto_isReplacementInLatestVersion(existingFile)){
            return false;
        }
        
        fileToReplace = existingFile;
        
        return true;        

    }
    
    /**
     * Make sure the file to replace is in the workingVersion
     *  -- e.g. that it wasn't deleted from a previous Version
     * 
     * @return 
     */
    private boolean step_007_auto_isReplacementInLatestVersion(DataFile existingFile){
        
        if (existingFile == null){
            throw new NullPointerException("existingFile cannot be null!");
        }

        if (this.hasError()){
            return false;
        }
        
        
        DatasetVersion latestVersion = existingFile.getOwner().getLatestVersion();
        
        boolean fileInLatestVersion = false;
        for (FileMetadata fm : latestVersion.getFileMetadatas()){
            if (fm.getDataFile().getId() != null){
                if (Objects.equals(existingFile.getId(),fm.getDataFile().getId())){
                    fileInLatestVersion = true;
                }
            }
        }
        if (!fileInLatestVersion){
            addError(getBundleErr("existing_file_not_in_latest_published_version"));
            return false;                        
        }
        return true;
    }
    
    
    private boolean step_030_createNewFilesViaIngest(){
        
        if (this.hasError()){
            return false;
        }

        // Load the working version of the Dataset
        workingVersion = dataset.getEditVersion();
                
        try {
            initialFileList = FileUtil.createDataFiles(workingVersion,
                    this.newFileInputStream,
                    this.newFileName,
                    this.newFileContentType,
                    this.systemConfig);

        } catch (IOException ex) {
            if (!Strings.isNullOrEmpty(ex.getMessage())) {
                this.addErrorSevere(getBundleErr("ingest_create_file_err") + " " + ex.getMessage());
            } else {
                this.addErrorSevere(getBundleErr("ingest_create_file_err"));
            }
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
            this.addErrorSevere(getBundleErr("initial_file_list_empty"));
            this.runMajorCleanup();
            return false;
        }
        
        /**
         * REPLACE: File replacement is limited to a single file!!
         * 
         * ADD: When adding files, some types of individual files
         * are broken into several files--which is OK
         */
        if (isFileReplaceOperation()){
            if (initialFileList.size() > 1){
                this.addError(getBundleErr("initial_file_list_more_than_one"));
                this.runMajorCleanup();
                return false;

            }
        }
        
        if (this.step_040_auto_checkForDuplicates()){
            ingestService.addFilesToDataset(workingVersion, finalFileList);
            return true;
        }
                       

        /*
            commenting out. see the comment in the source of the method below.
        if (this.step_045_auto_checkForFileReplaceDuplicate()) {
            return true;
        }*/
        
        return false;
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
            this.addErrorSevere(getBundleErr("initial_file_list_empty"));
            return false;
        }

        // Initialize new file list
        this.finalFileList = new ArrayList();

        String warningMessage  = null;
        

        if (isFileReplaceOperation() && this.fileToReplace == null){
            // This error shouldn't happen if steps called correctly
            this.addErrorSevere(getBundleErr("existing_file_to_replace_is_null") + " (This error shouldn't happen if steps called in sequence....checkForFileReplaceDuplicate)");
            return false;
        }
        
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
            if (isFileReplaceOperation() && Objects.equals(df.getChecksumValue(), fileToReplace.getChecksumValue())){
                this.addErrorSevere(getBundleErr("replace.new_file_same_as_replacement"));                                
                break;
            } else if (DuplicateFileChecker.isDuplicateOriginalWay(workingVersion, df.getFileMetadata())){
                String dupeName = df.getFileMetadata().getLabel();
                //removeUnSavedFilesFromWorkingVersion();
                //removeLinkedFileFromDataset(dataset, df);
                //abandonOperationRemoveAllNewFilesFromDataset();
                this.addErrorSevere(getBundleErr("duplicate_file") + " " + dupeName);   
                //return false;
            } else {
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
        
       /**
         * REPLACE: File replacement is limited to a single file!!
         * 
         * ADD: When adding files, some types of individual files
         * are broken into several files--which is OK
         */
            
       /**
        *  Also: check that the file is being replaced with the same content type
        *  file. Treat this as a fatal error, unless this is a "force replace" 
        *  operation; then it should be treated as merely a warning.
        */
        if (isFileReplaceOperation()){
        
            if (finalFileList.size() > 1){     
                String errMsg = "(This shouldn't happen -- error should have been detected in 030_createNewFilesViaIngest)";
                this.addErrorSevere(getBundleErr("initial_file_list_more_than_one") + " " + errMsg);            
                return false;
            }
            
            // Has the content type of the file changed?
            //
            if (!finalFileList.get(0).getContentType().equalsIgnoreCase(fileToReplace.getContentType())){
            
                List<String> errParams = Arrays.asList(fileToReplace.getFriendlyType(),
                                                finalFileList.get(0).getFriendlyType());
                
                String contentTypeErr = BundleUtil.getStringFromBundle("file.addreplace.error.replace.new_file_has_different_content_type", 
                                errParams);
                                        
                if (isForceFileOperation()){
                    // for force replace, just give a warning
                    this.setContentTypeWarning(contentTypeErr);
                }else{
                    // not a force replace? it's an error
                    this.addError(contentTypeErr);
                    runMajorCleanup();
                    return false;
                }
            }
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
     * NOTE: this method was always called AFTER the main duplicate check; 
     * So we would never detect this condition - of the file being replaced with 
     * the same file... because it would always be caught as simply an attempt
     * to replace a file with a file alraedy in the dataset! 
     * So I commented it out, instead modifying the method above, step_040_auto_checkForDuplicates()
     * to do both - check (first) if a file is being replaced with the exact same file;
     * and check if a file, or files being uploaded are duplicates of files already 
     * in the dataset. AND the replacement content type too. -- L.A. Jan 16 2017
     * 
     */
    /*private boolean step_045_auto_checkForFileReplaceDuplicate(){
        
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

            // Has the content type of the file changed?
            //
            if (!df.getContentType().equalsIgnoreCase(fileToReplace.getContentType())){
            
                List<String> errParams = Arrays.asList(fileToReplace.getFriendlyType(),
                                                df.getFriendlyType());
                
                String contentTypeErr = BundleUtil.getStringFromBundle("file.addreplace.error.replace.new_file_has_different_content_type", 
                                errParams);
                                        
                if (isForceFileOperation()){
                    // for force replace, just give a warning
                    this.setContentTypeWarning(contentTypeErr);
                }else{
                    // not a force replace? it's an error
                    this.addError(contentTypeErr);
                }
            }

        }
        
        if (hasError()){
            runMajorCleanup();
            return false;
        }
        
        return true;
        
    } // end step_045_auto_checkForFileReplaceDuplicate
    */
    
    
    
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
    
    
    /**
     * Load optional file params such as description, tags, fileDataTags, etc..
     * 
     * @param optionalFileParams
     * @return 
     */
    private boolean step_055_loadOptionalFileParams(OptionalFileParams optionalFileParams){
        
        if (hasError()){
            return false;
        }

        // --------------------------------------------
        // OK, the object may be null
        // --------------------------------------------
        if (optionalFileParams == null){
            return true;
        }
        
            
        // --------------------------------------------
        // Iterate through files (should only be 1 for now)
        // Add tags, description, etc
        // --------------------------------------------
        for (DataFile df : finalFileList){
            try {
                optionalFileParams.addOptionalParams(df);
            } catch (DataFileTagException ex) {
                Logger.getLogger(AddReplaceFileHelper.class.getName()).log(Level.SEVERE, null, ex);
                addError(ex.getMessage());
                return false;
            }
        }
        
        
        return true;
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
            /**
             * @todo Add a test to exercise this error.
             */
            this.addErrorSevere(getBundleErr("add.add_file_error"));
            logger.severe(ex.getMessage());
            return false;
        }catch (EJBException ex) {
            /**
             * @todo Add a test to exercise this error.
             */
            this.addErrorSevere("add.add_file_error (see logs)");
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
            if (fm.getDataFile().getId() != null) {
                if (Objects.equals(fm.getDataFile().getId(), fileToReplace.getId())) {
                    msg("Let's remove it!");

                    // If this is a tabular data file with a UNF, we'll need 
                    // to recalculate the version UNF, once the file is removed: 
                    
                    boolean recalculateUNF = !StringUtils.isEmpty(fm.getDataFile().getUnf());

                    if (workingVersion.getId() != null) {
                        // If this is an existing draft (i.e., this draft version 
                        // is already saved in the dataset, we'll also need to remove this filemetadata 
                        // explicitly:
                        msg(" this is an existing draft version...");
                        fileService.removeFileMetadata(fm);

                        // remove the filemetadata from the list of filemetadatas
                        // attached to the datafile object as well, for a good 
                        // measure: 
                        fileToReplace.getFileMetadatas().remove(fm);
                        // (and yes, we can do .remove(fm) safely - if this released
                        // file is part of an existing draft, we know that the 
                        // filemetadata object also exists in the database, and thus
                        // has the id, and can be identified unambiguously. 
                    }

                    // and remove it from the list of filemetadatas attached
                    // to the version object, via the iterator:
                    fmIt.remove();

                    if (recalculateUNF) {
                        msg("recalculating the UNF");
                        ingestService.recalculateDatasetVersionUNF(workingVersion);
                        msg("UNF recalculated: "+workingVersion.getUNF());
                    }
                    
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
        //  (1) Old file: Set the Root File Id on the original file  
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

        // Call the update dataset command
        //
        return step_070_run_update_dataset_command();
        
       
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
    private void setNewlyAddedFiles(List<DataFile> datafiles){
        
        if (hasError()){
            return;
        }
            
        // Init. newly added file list
        newlyAddedFiles = new ArrayList<>();
        newlyAddedFileMetadatas = new ArrayList<>();
        
        // Loop of uglinesss...but expect 1 to 4 files in final file list
        List<FileMetadata> latestFileMetadatas = dataset.getEditVersion().getFileMetadatas();
        
        
        for (DataFile newlyAddedFile : finalFileList){
            
             for (FileMetadata fm : latestFileMetadatas){
                 if (newlyAddedFile.getChecksumValue().equals(fm.getDataFile().getChecksumValue())){
                    if (newlyAddedFile.getStorageIdentifier().equals(fm.getDataFile().getStorageIdentifier())){
                        newlyAddedFiles.add(fm.getDataFile());
                        newlyAddedFileMetadatas.add(fm);
                    }
                }
             }
        }
        /*
       
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
        */
        
    }

    /**
     * For a successful replace operation, return a the first newly added file
     * @return 
     */
    public DataFile getFirstNewlyAddedFile(){
        
        if ((newlyAddedFiles == null)||(newlyAddedFiles.size() == 0)){
            return null;
        }
        return newlyAddedFiles.get(0);
    }
        
    public List<DataFile> getNewlyAddedFiles(){
        
        return newlyAddedFiles;
    }
    
    public List<FileMetadata> getNewlyAddedFileMetadatas(){
        
        return newlyAddedFileMetadatas;
    }
    
    
    public String getSuccessResult() throws NoFilesException{
        if (hasError()){
            throw new NoFilesException("Don't call this method if an error exists!! First check 'hasError()'");
        }

        if (newlyAddedFiles == null){
            throw new NullPointerException("newlyAddedFiles is null!");
        }
        
        return getSuccessResultAsJsonObjectBuilder().toString();
        
    }
    
    public JsonObjectBuilder getSuccessResultAsJsonObjectBuilder() throws NoFilesException{
        
        if (hasError()){
            throw new NoFilesException("Don't call this method if an error exists!! First check 'hasError()'");
        }
        
        if (newlyAddedFiles == null){
            throw new NullPointerException("newlyAddedFiles is null!");
        }
        
        if (newlyAddedFiles.isEmpty()){
            throw new NoFilesException("newlyAddedFiles is empty!");
        }
        
        return JsonPrinter.jsonDataFileList(newlyAddedFiles);
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
       
        // skip for now, may be part of dataset update listening
        //
        return true;
    }
    

    private boolean step_100_startIngestJobs(){
        if (this.hasError()){
            return false;
        }
                
        // Should only be one file in the list
        setNewlyAddedFiles(finalFileList);
        
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
        logger.fine(m);
        //System.out.println(m);
    }
    private void dashes(){
        msg("----------------");
    }
    private void msgt(String m){
        dashes(); msg(m); dashes();
    }
    
    
    /**
     * Return file list before saving
     * 
     * Used for UI display
     * 
     * @return 
     */
    public List<DataFile> getFileListBeforeSave(){
        
        return this.finalFileList;
    }
    
    public Boolean isFinalFileListEmpty (){
        return this.finalFileList.isEmpty();
    }
    
    
    /**
     * Return file list before saving
     * 
     * Used for UI display
     * 
     * @return 
     */
    public List<FileMetadata> getNewFileMetadatasBeforeSave(){
        
        if (this.finalFileList.size() == 0){
            return null;
        }
        
        List<FileMetadata> fileMetadatas = new ArrayList<>();
        for (DataFile df : finalFileList){
            fileMetadatas.add(df.getFileMetadata());
        }
        
        return fileMetadatas;
        
    }
    
    public void setContentTypeWarning(String warningString){
        
        if ((warningString == null)||(warningString.isEmpty())){
            throw new NullPointerException("warningString cannot be null");
        }
        
        contentTypeWarningFound = true;
        contentTypeWarningString = warningString;
    }
    
    public boolean hasContentTypeWarning(){
        return this.contentTypeWarningFound;
    }
    
    public String getContentTypeWarningString(){
        if (!hasContentTypeWarning()){
            // not really a NullPointerException but want to blow up here without adding try/catch everywhere
            //
            throw new NullPointerException("Don't call this method without checking 'hasContentTypeWarning()'");
        }
        return contentTypeWarningString;
    }
    
} // end class
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