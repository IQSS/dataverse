/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 * Adding single file replace to the EditDatafilesPage.
 * 
 * Phase 1: File successfully uploaded and unpersisted DataFile is in memory
 * Phase 2: Save the files
 * 
  http://localhost:8080/editdatafiles.xhtml?mode=SINGLE_REPLACE&datasetId=26&fid=726
 * This is messy, trying to contain some of it--give me APIs or more time, more time:)
 * 
 * @author rmp553
 */
public class FileReplacePageHelper {
    
    AddReplaceFileHelper replaceFileHelper;
            
    Dataset dataset;
    DataFile fileToReplace;
    
    private List<DataFile> newlyAddedFiles;
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
    
    /**
     * Handle native file replace
     * @param event 
     */
    public boolean handleNativeFileUpload(FileUploadEvent fileUploadEvent) {
        
        phase1Success = false;
        newlyAddedFiles = null;
        
        if (fileUploadEvent == null){
            throw new NullPointerException("fileUploadEvent cannot be null");
        }
        
        UploadedFile uFile = fileUploadEvent.getFile();

        
        try {
            replaceFileHelper.runReplaceFromUI_Phase1(fileToReplace.getId(),
                    uFile.getFileName(),
                    uFile.getContentType(),
                    uFile.getInputstream(),
                    null
            );
        } catch (IOException ex) {
            Logger.getLogger(FileReplacePageHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (replaceFileHelper.hasError()){
            msgt("upload error");
            msg(replaceFileHelper.getErrorMessagesAsString("\n"));
            return false;
        }else{
            phase1Success = true;
            //newlyAddedFiles = addReplaceFileHelper.getNewlyAddedFiles();
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
