package edu.harvard.iq.dataverse.datafile.pojo;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

/**
 * Class that holds info about rsync after using RequestRsyncScriptCommand.
 */
public class RsyncInfo {

    private String rsyncScript;
    private String rsyncScriptFileName;

    // -------------------- CONSTRUCTORS --------------------

    public RsyncInfo(String rsyncScript, String rsyncScriptFileName) {
        this.rsyncScript = rsyncScript;
        this.rsyncScriptFileName = rsyncScriptFileName;
    }

    // -------------------- GETTERS --------------------

    /**
     * @return rsync in the form of string.
     */
    public String getRsyncScript() {
        return rsyncScript;
    }

    /**
     * Rsync script file that is usually constructed by {@link edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtil#getScriptName(DatasetVersion)}
     */
    public String getRsyncScriptFileName() {
        return rsyncScriptFileName;
    }
}
