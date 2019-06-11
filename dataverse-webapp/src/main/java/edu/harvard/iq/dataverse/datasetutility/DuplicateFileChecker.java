/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *  Used for adding/replacing single files.
 * 
 *  Methods check if the files already exist in the *saved* DatasetVersion
 *  
 * @author rmp553
 */
public class DuplicateFileChecker {
    
    private static final Logger logger = Logger.getLogger(DuplicateFileChecker.class.getCanonicalName());
    private DatasetVersionServiceBean datasetVersionService;
    
    /**
     * Constructor
     * 
     * @param datasetVersionService 
     */
    public DuplicateFileChecker(DatasetVersionServiceBean datasetVersionService){

        if (datasetVersionService == null){
            throw new NullPointerException("datasetVersionService cannot be null");
        }
        
        this.datasetVersionService = datasetVersionService;
    } // end: constructor
    
    
    /**
     * Check the database to see if this file is already in the DatasetVersion
     * 
     * Note: This checks a SINGLE file against the database only.
     * 
     * @param checksum
     * @return 
     */
    public boolean isFileInSavedDatasetVersion(DatasetVersion datasetVersion, FileMetadata fileMetadata){
        
         if (datasetVersion == null){
             throw new NullPointerException("datasetVersion cannot be null");
         }
        
         if (fileMetadata == null){
             throw new NullPointerException("fileMetadata cannot be null");
         }
         return this.isFileInSavedDatasetVersion(datasetVersion, fileMetadata.getDataFile().getChecksumValue());
     }
    
    /**
     * See if this checksum already exists by a new query
     * 
     * @param checksum
     * @return 
     */
    public boolean isFileInSavedDatasetVersion(DatasetVersion datasetVersion, String checkSum){

        if (datasetVersion == null){
             throw new NullPointerException("datasetVersion cannot be null");
        }
        
        if (checkSum == null){
            throw new NullPointerException("checkSum cannot be null");
        }
        
        return datasetVersionService.doesChecksumExistInDatasetVersion(datasetVersion, checkSum);
        
    }
    
    /**
     * From dataset version:
     *  - Get the checksum of all the files 
     *  - Load them into a hash
     * 
     * Loads checksums from unsaved datasetversion--checks more 
     * 
     */
    public Map<String, Integer> getDatasetHashesFromDatabase(DatasetVersion datasetVersion){
     
        if (datasetVersion == null){
             throw new NullPointerException("datasetVersion cannot be null");
         }
        
        Map<String, Integer> checksumHashCounts = new HashMap<>();

        List<FileMetadata> fileMetadatas = new ArrayList<>(datasetVersion.getFileMetadatas());
        
        for (FileMetadata fm : fileMetadatas){            
            String checkSum = fm.getDataFile().getChecksumValue();
            if (checksumHashCounts.get(checkSum) != null){
                checksumHashCounts.put(checkSum, checksumHashCounts.get(checkSum).intValue() + 1);
            }else{
                checksumHashCounts.put(checkSum, 1);
            }   
        }
        return checksumHashCounts;
    }
    
    
    /** 
     * Original isDuplicate method from the DatasetPage and EditDatafilesPage
     * 
     * Note: this has efficiency issues in that the hash is re-created for every fileMetadata checked
     * Note: the only remaining component that uses this method is AddReplaceFileHelper; 
     * Currently it is only being used for file replace calls from the File page - which always 
     * operates on one file at a time; so performance is not much of an issue. -- L.A. 4.6.1
     * 
     * @param workingVersion
     * @param fileMetadata
     * @return 
     */
    public static boolean isDuplicateOriginalWay(DatasetVersion workingVersion, FileMetadata fileMetadata) {
        if (workingVersion == null){
            throw new NullPointerException("datasetVersion cannot be null");
        }

        String selectedCheckSum = fileMetadata.getDataFile().getChecksumValue();
        if (selectedCheckSum == null) {
            return false;
        }        
        
        Map<String, Integer> checkSumMap = new HashMap<String, Integer>();

        // TODO: 
        // think of a way to do this that doesn't involve populating this 
        // map for every file on the page? 
        // man not be that much of a problem, if we paginate and never display 
        // more than a certain number of files... Still, needs to be revisited
        // before the final 4.0. 
        // -- L.A. 4.0

        // make a "defensive copy" to avoid java.util.ConcurrentModificationException from being thrown
        // when uploading 100+ files
        List<FileMetadata> wvCopy = new ArrayList<>(workingVersion.getFileMetadatas());
        Iterator<FileMetadata> fmIt = wvCopy.iterator();

        while (fmIt.hasNext()) {
            FileMetadata fm = fmIt.next();
            String currentCheckSum = fm.getDataFile().getChecksumValue();
            if (currentCheckSum != null) {
                if (checkSumMap.get(currentCheckSum) != null) {
                    checkSumMap.put(currentCheckSum, checkSumMap.get(currentCheckSum).intValue() + 1);
                } else {
                    checkSumMap.put(currentCheckSum, 1);
                }
            }
        }
        return checkSumMap.get(selectedCheckSum) != null; // && checkSumMap.get(selectedCheckSum).intValue() > 1;
            
    }
    
}
