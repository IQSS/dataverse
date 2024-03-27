/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;

/**
 * Used for showing a list of File Version differences
 * 
 * Stores a mix of DatasetVersion, DataFile, and FileMetadata
 * information.
 * 
 * @author rmp553
 */
//ToDo - used at all?
public class FileVersionInfo {
    
    private Long id;

    private String doi;

    private Long majorVersion;

    private Long minorVersion;

    private Long datasetId;

    private Long datasetVersionId;

    private DataFile dataFile;

    private FileMetadata fileMetadata;

    /**
     * Constructor
     * 
     */
    public FileVersionInfo(){
        
        
    }

    /**
     *  Set id
     *  @param id
     */
    public void setId(Long id){
        this.id = id;
    }

    /**
     *  Get for id
     *  @return Long
     */
    public Long getId(){
        return this.id;
    }
    

    /**
     *  Set doi
     *  @param doi
     */
    public void setDoi(String doi){
        this.doi = doi;
    }

    /**
     *  Get for doi
     *  @return String
     */
    public String getDoi(){
        return this.doi;
    }
    

    /**
     *  Set majorVersion
     *  @param majorVersion
     */
    public void setMajorVersion(Long majorVersion){
        this.majorVersion = majorVersion;
    }

    /**
     *  Get for majorVersion
     *  @return Long
     */
    public Long getMajorVersion(){
        return this.majorVersion;
    }
    

    /**
     *  Set minorVersion
     *  @param minorVersion
     */
    public void setMinorVersion(Long minorVersion){
        this.minorVersion = minorVersion;
    }

    /**
     *  Get for minorVersion
     *  @return Long
     */
    public Long getMinorVersion(){
        return this.minorVersion;
    }
    

    /**
     *  Set datasetId
     *  @param datasetId
     */
    public void setDatasetId(Long datasetId){
        this.datasetId = datasetId;
    }

    /**
     *  Get for datasetId
     *  @return Long
     */
    public Long getDatasetId(){
        return this.datasetId;
    }
    

    /**
     *  Set datasetVersionId
     *  @param datasetVersionId
     */
    public void setDatasetVersionId(Long datasetVersionId){
        this.datasetVersionId = datasetVersionId;
    }

    /**
     *  Get for datasetVersionId
     *  @return Long
     */
    public Long getDatasetVersionId(){
        return this.datasetVersionId;
    }
    

    /**
     *  Set dataFile
     *  @param dataFile
     */
    public void setDataFile(DataFile dataFile){
        this.dataFile = dataFile;
    }

    /**
     *  Get for dataFile
     *  @return DataFile
     */
    public DataFile getDataFile(){
        return this.dataFile;
    }
    

    /**
     *  Set fileMetadata
     *  @param fileMetadata
     */
    public void setFileMetadata(FileMetadata fileMetadata){
        this.fileMetadata = fileMetadata;
    }

    /**
     *  Get for fileMetadata
     *  @return FileMetadata
     */
    public FileMetadata getFileMetadata(){
        return this.fileMetadata;
    }
    

}
