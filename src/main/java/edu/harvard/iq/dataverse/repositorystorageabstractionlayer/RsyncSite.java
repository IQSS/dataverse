package edu.harvard.iq.dataverse.repositorystorageabstractionlayer;

import java.io.File;

public class RsyncSite {

    private final String name;
    private final String fqdn;
    private final String fullRemotePathToDirectory;
    private final String rsyncDownloadcommand;

    public RsyncSite(String name, String fqdn, String fullRemotePathToDirectory) {
        this.name = name;
        this.fqdn = fqdn;
        this.fullRemotePathToDirectory = fullRemotePathToDirectory;
        // TODO: Stop hard-coding "rsync" here one a field has been added to the StorageSite object (or related object).
        this.rsyncDownloadcommand = "rsync -av rsync://" + this.fqdn + "" + File.separator + this.fullRemotePathToDirectory;
    }

    public String getName() {
        return name;
    }

    public String getFqdn() {
        return fqdn;
    }

    public String getRsyncDownloadcommand() {
        return rsyncDownloadcommand;
    }

    public String getFullRemotePathToDirectory() {
        return fullRemotePathToDirectory;
    }

}
