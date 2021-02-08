package edu.harvard.iq.dataverse.dataset.difference;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

/**
 * Class that contains old and new value of {@link FileTermsOfUse}
 * that is different in file between two {@link DatasetVersion}s
 * 
 * @author madryk
 */
public class TermsOfUseDiff extends ItemDiff<FileTermsOfUse> {

    private final DataFile originalFile;

    // -------------------- CONSTRUCTORS --------------------
    
    public TermsOfUseDiff(FileTermsOfUse oldValue, FileTermsOfUse newValue, DataFile originalFile) {
        super(oldValue, newValue);
        this.originalFile = originalFile;
    }

    // -------------------- GETTERS --------------------

    /**
     * Returns original data containing information about file.
     */
    public DataFile getOriginalFile() {
        return originalFile;
    }
}