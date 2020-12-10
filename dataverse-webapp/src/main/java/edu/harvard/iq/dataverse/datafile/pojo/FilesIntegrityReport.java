package edu.harvard.iq.dataverse.datafile.pojo;

import java.util.ArrayList;
import java.util.List;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

public class FilesIntegrityReport {

    private int checkedCount;
    
    private List<DataFile> suspicious;

    private String warning;
    
    public FilesIntegrityReport() {
        suspicious = new ArrayList<DataFile>();
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public void setCheckedCount(int checkedCount) {
        this.checkedCount = checkedCount;
    }

    public int getCheckedCount() {
        return checkedCount;
    }

    public void addSuspicious(DataFile dataFile) {
        suspicious.add(dataFile);
    }

    public List<DataFile> getSuspicious() {
        return suspicious;
    }

    public String getWarning() {
        return warning;
    }

    public String getSummaryInfo() {
        if (warning != null) {
            return warning;
        } else {
            String message = "Found " + checkedCount + " files in repository.";
            message += " Found " + suspicious.size() + " suspicious files.";
            return message;
        }

    }
}
