/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import java.util.List;
import java.util.logging.Logger;

/**
 * Given a file id or object, return a a list of this file's versions
 * 
 * @author rmp553
 */
public class FileVersionRetriever {
   
    private static final Logger logger = Logger.getLogger(FileVersionRetriever.class.getCanonicalName());

    DataFileServiceBean dataFileService;

    DataFile chosenFile;
    List<DataFile> fileList;
    
    /**
     * Constructor by chosenFileId
     * 
     * @param fileService
     * @param chosenFileId 
     */
    public FileVersionRetriever(DataFileServiceBean fileService, Long chosenFileId){
        if (fileService == null){
            throw new NullPointerException("fileService cannot be null");
        }
        if (chosenFileId == null){
            throw new NullPointerException("chosenFileId cannot be null");
        }
        dataFileService = fileService;
        chosenFile = dataFileService.find(chosenFileId);
        
        /*
        if (chosenFile == null){
            throw new NullPointerException("No DataFile found for id: "  + chosenFileId);
        }*/
    }

    /**
     * Constructor by chosenFile
     * 
     * @param fileService
     * @param chosenFile 
     */
    public FileVersionRetriever(DataFileServiceBean fileService, DataFile selectedFile){
        if (fileService == null){
            throw new NullPointerException("fileService cannot be null");
        }
        if (selectedFile == null){
            throw new NullPointerException("selectedFile cannot be null");
        }
        dataFileService = fileService;
        chosenFile = selectedFile;
    }
    
    /**
     * (1) Get all of the Dataset Versions
     * 
     * 
     */
    private void buildFileVersionHistory(){
        
        
        //dataFileService.
        
        
    }
    
    
   
}
