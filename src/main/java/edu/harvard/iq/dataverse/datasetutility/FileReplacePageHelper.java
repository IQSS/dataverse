/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import java.io.InputStream;
import java.util.List;


/**
 * Adding single file replace to the EditDatafilesPage.
 * 
 * Phase 1: File successfully uploaded and unpersisted DataFile is in memory
 * Phase 2: Save the files
 * 
  http://localhost:8080/editdatafiles.xhtml?mode=REPLACE&datasetId=26&fid=726
 * This is messy, trying to contain some of it--give me APIs or more time, more time:)
 * 
 * @author rmp553
 */
public class FileReplacePageHelper {
    
    AddReplaceFileHelper replaceFileHelper;
            
    Dataset dataset;
    DataFile fileToReplace;
    
    private boolean phase1Success;
    
    /** 
     * constructor
     * 
     * @param dataset
     * @param fileToReplace 
     */
    public FileReplacePageHelper(AddReplaceFileHelper replaceFileHelper, Dataset dataset, DataFile fileToReplace){
        
        if (replaceFileHelper == null){
            throw new NullPointerException("replaceFileHelper cannot be null");
        }
        if (dataset == null){
            throw new NullPointerException("dataset cannot be null");
        }
        if (fileToReplace == null){
            throw new NullPointerException("fileToReplace cannot be null");
        }
        
        this.replaceFileHelper = replaceFileHelper;
        this.dataset = dataset;
        this.fileToReplace = fileToReplace;
    }
    
    public DataFile getFileToReplace(){
        return fileToReplace;
    }
    
    public boolean hasContentTypeWarning(){
        if (!wasPhase1Successful()){
            // not really a NullPointerException but want to blow up here without adding try/catch everywhere
            //
            throw new NullPointerException("Do not call if Phase 1 unsuccessful!");
        }
      
        return replaceFileHelper.hasContentTypeWarning();
    }
    
    public String getContentTypeWarning(){
        if (!hasContentTypeWarning()){
            // not really a NullPointerException but want to blow up here without adding try/catch everywhere
            //
            throw new NullPointerException("Do not call if content type warning doesn't exist!");
        }
      
        return replaceFileHelper.getContentTypeWarningString();
    }
    
    
    public boolean resetReplaceFileHelper(){
        
        phase1Success = false;
        this.replaceFileHelper.resetFileHelper();
        return true;
    }
    
    
    /**
     * Handle native file replace
     * @param checkSum 
     * @param event 
     */
    public boolean handleNativeFileUpload(InputStream inputStream, String fullStorageId, String fileName, String fileContentType, String checkSumValue, ChecksumType checkSumType) {
                
        phase1Success = false;
        
        // Preliminary sanity check
        //
        if ((inputStream == null)&&(fullStorageId==null)){
            throw new NullPointerException("inputStream and storageId cannot both be null");
        }
        if (fileName == null){
            throw new NullPointerException("fileName cannot be null");
        }
        if (fileContentType == null){
            throw new NullPointerException("fileContentType cannot be null");
        }
        
        OptionalFileParams ofp = null;
        ofp = new OptionalFileParams();
        if(checkSumValue != null) {
            ofp.setCheckSum(checkSumValue, checkSumType);
        }
        // Run 1st phase of replace
        //
        replaceFileHelper.runReplaceFromUI_Phase1(fileToReplace.getId(),
                fileName,
                fileContentType,
                inputStream,
                fullStorageId,
                ofp
        );
        
        // Did it work?
        //
        if (replaceFileHelper.hasError()){
            msgt("upload error");
            msg(replaceFileHelper.getErrorMessagesAsString("\n"));
            return false;
        }else{
            phase1Success = true;
            
            msg("Look at that!  Phase 1 worked");
            return true;
        }
   
    } // handleFileUpload

    public boolean runSaveReplacementFile_Phase2() throws FileReplaceException{
                
        if (!wasPhase1Successful()){
            throw new FileReplaceException("Do not call if Phase 1 unsuccessful!");
        }
        if (replaceFileHelper == null){
            throw new NullPointerException("replaceFileHelper cannot be null!");
        }
        
        if (replaceFileHelper.runReplaceFromUI_Phase2()){
            msg("Look at that!  Phase 2 worked");
            return true;
        }else{
            msg(replaceFileHelper.getErrorMessagesAsString("\n"));
            return false;
        }
    }
    
    public String getErrorMessages(){
        if (!replaceFileHelper.hasError()){
            throw new NullPointerException("Only call this if an error exists!");
        }
        return replaceFileHelper.getErrorMessagesAsString("\n");
    }
    
    
    /**
     * For a successful replace operation, return a the first newly added file
     * @return 
     */
    public DataFile getFirstNewlyAddedFile() throws FileReplaceException{
    
        if (!wasPhase1Successful()){
            throw new FileReplaceException("Do not call if Phase 1 unsuccessful!");
        }
        if (replaceFileHelper == null){
            throw new NullPointerException("replaceFileHelper cannot be null!");
        }
        if (replaceFileHelper.hasError()){
            throw new FileReplaceException("Do not call if errors exist! " + replaceFileHelper.getErrorMessagesAsString("\n"));            
        }
        
        return replaceFileHelper.getFirstNewlyAddedFile();
    }
        
   
    public List<FileMetadata> getNewFileMetadatasBeforeSave(){
        
        if (wasPhase1Successful()){
            return replaceFileHelper.getNewFileMetadatasBeforeSave();
        }
        return null;
        
    }
    
    public AddReplaceFileHelper getAddReplaceFileHelper(){
        return replaceFileHelper;
    }
    
    /** 
     * 
     * Show file upload component if Phase 1 hasn't happened yet
     * 
     * @return 
     */
    public boolean showFileUploadComponent(){
        
        return !(wasPhase1Successful());
    }
    
    public boolean wasPhase1Successful(){
        
        return phase1Success;
    }
    
    private void msg(String s){
        System.out.println(s);
    }
    
   
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
    
}
