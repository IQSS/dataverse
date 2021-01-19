package edu.harvard.iq.dataverse.datafile.pojo;

public enum FileIntegrityCheckResult {
    STORAGE_ERROR,
    NOT_EXIST,
    DIFFERENT_SIZE,
    DIFFERENT_CHECKSUM,
    
    OK,
    OK_SKIPPED_CHECKSUM_VERIFICATION;
    
    // -------------------- LOGIC --------------------
    
    public boolean isOK() {
        return this == OK || this == OK_SKIPPED_CHECKSUM_VERIFICATION;
    }
}
