package edu.harvard.iq.dataverse.search;

import java.util.List;

public class FileView {

    List<SolrSearchResult> solrSearchResults;

    public FileView(List<SolrSearchResult> solrSearchResults, List<FacetCategory> facetCategoryList) {
        this.solrSearchResults = solrSearchResults;
        this.facetCategoryList = facetCategoryList;
    }
    private List<FacetCategory> facetCategoryList;

    public List<SolrSearchResult> getSolrSearchResults() {
        return solrSearchResults;
    }

    public List<FacetCategory> getFacetCategoryList() {
        return facetCategoryList;
    }

}
