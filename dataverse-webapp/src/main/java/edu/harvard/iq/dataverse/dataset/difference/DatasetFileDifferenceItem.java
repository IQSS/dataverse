package edu.harvard.iq.dataverse.dataset.difference;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

/**
 * Difference in file metadata between two
 * {@link DatasetVersion}s
 * 
 * @author skraffmiller
 * @author madryk
 */
public class DatasetFileDifferenceItem {
    
    private FileSummary fileSummary;
    private FileMetadataDifferenceItem difference;
    
    // -------------------- CONSTRUCTORS --------------------
    
    public DatasetFileDifferenceItem(FileSummary fileSummary, FileMetadataDifferenceItem difference) {
        this.fileSummary = fileSummary;
        this.difference = difference;
    }

    // -------------------- GETTERS --------------------
    
    public FileSummary getFileSummary() {
        return fileSummary;
    }

    public FileMetadataDifferenceItem getDifference() {
        return difference;
    }
}