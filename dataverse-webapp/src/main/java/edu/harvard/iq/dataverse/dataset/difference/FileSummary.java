package edu.harvard.iq.dataverse.dataset.difference;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType;

/**
 * Summary of a file with properties that can't
 * change between versions.
 * 
 * @author madryk
 */
public class FileSummary {

    private String fileId;
    private DataFile.ChecksumType fileChecksumType;
    private String fileChecksumValue;
    
    // -------------------- CONSTRUCTORS --------------------
    
    public FileSummary(String fileId, ChecksumType fileChecksumType, String fileChecksumValue) {
        this.fileId = fileId;
        this.fileChecksumType = fileChecksumType;
        this.fileChecksumValue = fileChecksumValue;
    }
    
    // -------------------- GETTERS --------------------
    
    public String getFileId() {
        return fileId;
    }
    public DataFile.ChecksumType getFileChecksumType() {
        return fileChecksumType;
    }
    public String getFileChecksumValue() {
        return fileChecksumValue;
    }
}