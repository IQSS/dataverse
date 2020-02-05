package edu.harvard.iq.dataverse.search.index;

/**
 * Class representation of solr document used to
 * determine if user making the search can see
 * associated dvobject.
 * <p>
 * Note that single dvobject can have multiple {@link PermissionsSolrDoc}.
 * For example dataset can have one doc for DRAFT version and one
 * for RELEASED version.
 */
public class PermissionsSolrDoc {

    private final long dvObjectId;

    private final String solrId;

    private final Long datasetVersionId;

    private final String nameOrTitle;

    private final SearchPermissions permissions;

    // -------------------- CONSTRUCTORS --------------------
    
    public PermissionsSolrDoc(long dvObjectId, String solrId, Long datasetVersionId, String nameOrTitle, SearchPermissions permissions) {
        this.dvObjectId = dvObjectId;
        this.solrId = solrId;
        this.datasetVersionId = datasetVersionId;
        this.nameOrTitle = nameOrTitle;
        this.permissions = permissions;
    }

    // -------------------- GETTERS --------------------
    
    /**
     * The database id of the DvObject.
     */
    public long getDvObjectId() {
        return dvObjectId;
    }

    /**
     * The Solr id of this particular Solr document associated with the
     * dvObjectId (might end in _draft, for example).
     */
    public String getSolrId() {
        return solrId;
    }

    /**
     * The dataset version id applies only to datasets and their children
     * (files) and will be null for dataverses.
     */
    public Long getDatasetVersionId() {
        return datasetVersionId;
    }
    
    /**
     * Dataverses and Files have names while Datasets have titles.
     */
    public String getNameOrTitle() {
        return nameOrTitle;
    }
    
    /**
     * A list of users and groups who should be able to search for this Solr
     * Document.
     */
    public SearchPermissions getPermissions() {
        return permissions;
    }
}
