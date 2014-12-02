package edu.harvard.iq.dataverse.search;

import java.util.List;

public class DvObjectSolrDoc {

    /**
     * The database id of the DvObject.
     */
    private final String dvObjectId;
    /**
     * The Solr id of this particular Solr document associated with the
     * dvObjectId (might end in _draft, for example).
     */
    private final String solrId;
    /**
     * Dataverses and Files have names while Datasets have titles.
     */
    private final String nameOrTitle;
    /**
     * A list of users and groups who should be able to search for this Solr
     * Document.
     */
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
