package edu.harvard.iq.dataverse.search;

import java.util.List;

public class DvObjectSolrDoc {

    private final String dvObjectId;
    private final String solrId;
    private final String nameOrTitle;
    private final List<String> permissions;

    public DvObjectSolrDoc(String dvObjectId, String solrId, String nameOrTitle, List<String> permissions) {
        this.dvObjectId = dvObjectId;
        this.solrId = solrId;
        this.nameOrTitle = nameOrTitle;
        this.permissions = permissions;
    }

    // this could be a Long
    public String getDvObjectId() {
        return dvObjectId;
    }

    public String getSolrId() {
        return solrId;
    }

    public String getNameOrTitle() {
        return nameOrTitle;
    }

    public List<String> getPermissions() {
        return permissions;
    }

}
