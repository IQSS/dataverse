package edu.harvard.iq.dataverse.datafile.pojo;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

public class FileIntegrityFail {

    private DataFile integrityFailFile;
    private FileIntegrityCheckResult checkResult;

    // -------------------- CONSTRUCTORS --------------------

    public FileIntegrityFail(DataFile integrityFailFile, FileIntegrityCheckResult checkResult) {
        super();
        this.integrityFailFile = integrityFailFile;
        this.checkResult = checkResult;
    }

    // -------------------- GETTERS --------------------

    public DataFile getIntegrityFailFile() {
        return integrityFailFile;
    }
    public FileIntegrityCheckResult getCheckResult() {
        return checkResult;
    }
}
