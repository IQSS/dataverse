package edu.harvard.iq.dataverse.datafile.pojo;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilesIntegrityReport {

    private int checkedCount;
    private int skippedChecksumVerification;
    private Map<FileIntegrityCheckResult, Integer> failsCount = new HashMap<>();
    
    private List<FileIntegrityFail> suspicious = new ArrayList<>();


    // -------------------- GETTERS --------------------

    public int getCheckedCount() {
        return checkedCount;
    }

    public int getSkippedChecksumVerification() {
        return skippedChecksumVerification;
    }

    public List<FileIntegrityFail> getSuspicious() {
        return suspicious;
    }

    // -------------------- LOGIC --------------------

    public void addSuspicious(DataFile dataFile, FileIntegrityCheckResult checkResult) {
        suspicious.add(new FileIntegrityFail(dataFile, checkResult));
    }

    public void incrementFailCount(FileIntegrityCheckResult checkResult) {
        failsCount.put(checkResult, failsCount.getOrDefault(checkResult, 0) + 1);
    }

    public void incrementCheckedCount() {
        ++checkedCount;
    }

    public void incrementSkippedChecksumVerification() {
        ++skippedChecksumVerification;
    }

    public String getSummaryInfo() {
        return String.format("Found %d files in repository. Found %d suspicious files.", checkedCount, suspicious.size());
    }

    public int getFailCountFor(FileIntegrityCheckResult checkResult) {
        return failsCount.getOrDefault(checkResult, 0);
    }

    // -------------------- SETTERS --------------------

    public void setCheckedCount(int checkedCount) {
        this.checkedCount = checkedCount;
    }
}
