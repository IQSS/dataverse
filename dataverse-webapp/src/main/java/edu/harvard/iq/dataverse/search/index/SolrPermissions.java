package edu.harvard.iq.dataverse.search.index;

public class SolrPermissions {
    private SearchPermissions searchPermissions;
    private SolrPermission addDatasetPermissions;

    // -------------------- CONSTRUCTORS --------------------

    public SolrPermissions(SearchPermissions searchPermissions, SolrPermission addDatasetPermissions) {
        this.searchPermissions = searchPermissions;
        this.addDatasetPermissions = addDatasetPermissions;
    }

    // -------------------- GETTERS --------------------

    public SearchPermissions getSearchPermissions() {
        return searchPermissions;
    }

    public SolrPermission getAddDatasetPermissions() {
        return addDatasetPermissions;
    }
}
