package edu.harvard.iq.dataverse.search;

import java.util.List;

public class DvObjectSolrDoc {

    private final String solrId;
    private final String nameOrTitle;
    private final List<String> permissions;

    public DvObjectSolrDoc(String solrId, String nameOrTitle, List<String> permissions) {
        this.solrId = solrId;
        this.nameOrTitle = nameOrTitle;
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "DvObjectSolrDoc{" + "solrId=" + solrId + ", nameOrTitle=" + nameOrTitle + ", permissions=" + permissions + '}';
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
