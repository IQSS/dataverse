package edu.harvard.iq.dataverse.repositorystorageabstractionlayer;

import java.io.File;

public class RsyncSite {

    private final String name;
    private final String fqdn;
    private final String country;
    private final String fullRemotePathToDirectory;
    private final String rsyncDownloadcommand;

    public RsyncSite(String name, String fqdn, String country, String fullRemotePathToDirectory) {
        this.name = name;
        this.fqdn = fqdn;
        this.country = country;
        this.fullRemotePathToDirectory = fullRemotePathToDirectory;
        this.rsyncDownloadcommand = "rsync -av rsync://" + this.fqdn + "" + File.separator + this.fullRemotePathToDirectory;
    }

    public String getName() {
        return name;
    }

    public String getFqdn() {
        return fqdn;
    }

    public String getCountry() {
        return country;
    }

    public String getRsyncDownloadcommand() {
        return rsyncDownloadcommand;
    }

    public String getFullRemotePathToDirectory() {
        return fullRemotePathToDirectory;
    }

}
