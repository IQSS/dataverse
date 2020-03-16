package edu.harvard.iq.dataverse.dataset.difference;

import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.apache.commons.lang.StringUtils;

/**
 * Difference in terms of use of file between two
 * {@link DatasetVersion}s
 * 
 * @author skraffmiller
 * @author madryk
 */
public class DatasetFileTermDifferenceItem {

    private FileSummary fileSummary;
    private String fileName;
    private FileTermsOfUse oldTerms;
    private FileTermsOfUse newTerms;
    
    // -------------------- CONSTRUCTORS --------------------
    
    public DatasetFileTermDifferenceItem(FileSummary fileSummary, FileTermsOfUse oldTerms, FileTermsOfUse newTerms) {
        this.fileSummary = fileSummary;
        this.oldTerms = oldTerms;
        this.newTerms = newTerms;
        this.fileName = StringUtils.EMPTY;
    }

    public DatasetFileTermDifferenceItem(FileSummary fileSummary, String fileName, FileTermsOfUse oldTerms, FileTermsOfUse newTerms) {
        this.fileSummary = fileSummary;
        this.fileName = fileName;
        this.oldTerms = oldTerms;
        this.newTerms = newTerms;
    }

    // -------------------- GETTERS --------------------
    
    public FileSummary getFileSummary() {
        return fileSummary;
    }

    public FileTermsOfUse getOldTerms() {
        return oldTerms;
    }

    public FileTermsOfUse getNewTerms() {
        return newTerms;
    }

    public String getFileName() {
        return fileName;
    }
}