package edu.harvard.iq.dataverse.search;

import java.util.List;

public class FileView {

    private List<SolrSearchResult> solrSearchResults;
    private List<FacetCategory> facetCategoryList;
    private List<String> filterQueries;
    private String query;

    public FileView(List<SolrSearchResult> solrSearchResults, List<FacetCategory> facetCategoryList, List<String> filterQueries, String query) {
        this.solrSearchResults = solrSearchResults;
        this.facetCategoryList = facetCategoryList;
        this.filterQueries = filterQueries;
        this.query = query;
    }

    public List<SolrSearchResult> getSolrSearchResults() {
        return solrSearchResults;
    }

    public List<FacetCategory> getFacetCategoryList() {
        return facetCategoryList;
    }

    public List<String> getFilterQueries() {
        return filterQueries;
    }

    public String getQuery() {
        return query;
    }

}
