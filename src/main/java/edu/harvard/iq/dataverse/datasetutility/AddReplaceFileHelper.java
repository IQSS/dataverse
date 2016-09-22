/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.Dataset;

/**
 *  Methods to add or replace a single file.
 * 
 * @author rmp553
 */
public class AddReplaceFileHelper {
    

    private Dataset dataset;
    
    public AddReplaceFileHelper(Dataset dataset){
        
        if (dataset == null){
            throw new NullPointerException("dataset cannot be null");
        }
        
        this.dataset = dataset;
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
    
    
    
    
}
