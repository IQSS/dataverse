package edu.harvard.iq.dataverse.dataset.difference;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

/**
 * Difference in file metadata of replaced file between two
 * {@link DatasetVersion}s
 * 
 * @author skraffmiller
 * @author madryk
 */
public class DatasetReplaceFileItem {

    private FileSummary oldFileSummary;
    private FileSummary newFileSummary;
    
    private FileMetadataDifferenceItem metadataDifference;

    // -------------------- CONSTRUCTORS --------------------
    
    public DatasetReplaceFileItem(FileSummary oldFileSummary, FileSummary newFileSummary,
            FileMetadataDifferenceItem metadataDifference) {
        this.oldFileSummary = oldFileSummary;
        this.newFileSummary = newFileSummary;
        this.metadataDifference = metadataDifference;
    }

    // -------------------- GETTERS --------------------
    
    public FileSummary getOldFileSummary() {
        return oldFileSummary;
    }

    public FileSummary getNewFileSummary() {
        return newFileSummary;
    }

    public FileMetadataDifferenceItem getMetadataDifference() {
        return metadataDifference;
    }
}