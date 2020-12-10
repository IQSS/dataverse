package edu.harvard.iq.dataverse.timer;

import java.io.Serializable;

public class FilesIntegrityCheckTimerInfo implements Serializable {

    String serverId;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public FilesIntegrityCheckTimerInfo() {

    }

    public FilesIntegrityCheckTimerInfo(String serverId) {
        this.serverId = serverId;
    }

}