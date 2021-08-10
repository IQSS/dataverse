package edu.harvard.iq.dataverse.search.response;

import org.apache.solr.client.solrj.SolrQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SolrQueryResponse {

    private List<SolrSearchResult> solrSearchResults;
    private Long numResultsFound;
    private Long resultsStart;
    private Map<String, List<String>> spellingSuggestionsByToken;
    private List<FacetCategory> facetCategoryList;
    private List<FilterQuery> filterQueries = new ArrayList<>();
    private List<String> filterQueriesActual = new ArrayList<>();
    private DvObjectCounts dvObjectCounts = DvObjectCounts.emptyDvObjectCounts();
    private PublicationStatusCounts publicationStatusCounts = PublicationStatusCounts.emptyPublicationStatusCounts();

    private SolrQuery solrQuery;

    public SolrQueryResponse(SolrQuery solrQuery) {
        this.solrQuery = solrQuery;
    }


    public List<SolrSearchResult> getSolrSearchResults() {
        return solrSearchResults;
    }

    public void setPublicationStatusCounts(PublicationStatusCounts publicationStatusCounts) {
        this.publicationStatusCounts = publicationStatusCounts;;
    }

    public PublicationStatusCounts getPublicationStatusCounts() {
        return this.publicationStatusCounts;
    }

    public void setDvObjectCounts(DvObjectCounts dvObjectCounts) {
        this.dvObjectCounts = dvObjectCounts;

    }

    public DvObjectCounts getDvObjectCounts() {
        return this.dvObjectCounts;
    }


    public void setSolrSearchResults(List<SolrSearchResult> solrSearchResults) {
        this.solrSearchResults = solrSearchResults;
    }

    public Map<String, List<String>> getSpellingSuggestionsByToken() {
        return spellingSuggestionsByToken;
    }

    public Long getNumResultsFound() {
        return numResultsFound;
    }

    public void setNumResultsFound(Long numResultsFound) {
        this.numResultsFound = numResultsFound;
    }

    public Long getResultsStart() {
        return resultsStart;
    }

    public void setResultsStart(Long resultsStart) {
        this.resultsStart = resultsStart;
    }

    public void setSpellingSuggestionsByToken(Map<String, List<String>> spellingSuggestionsByToken) {
        this.spellingSuggestionsByToken = spellingSuggestionsByToken;
    }

    public List<FacetCategory> getFacetCategoryList() {
        return facetCategoryList;
    }

    public void setFacetCategoryList(List<FacetCategory> facetCategoryList) {
        this.facetCategoryList = facetCategoryList;
    }

    public List<FilterQuery> getFilterQueries() {
        return filterQueries;
    }

    public void addFilterQuery(FilterQuery filterQuery) {
        filterQueries.add(filterQuery);
    }

    public void setFilterQueries(List<FilterQuery> filterQueries) {
        this.filterQueries = filterQueries;
    }

    public List<String> getFilterQueriesActual() {
        return filterQueriesActual;
    }

    public void setFilterQueriesActual(List<String> filterQueriesActual) {
        this.filterQueriesActual = filterQueriesActual;
    }

    public SolrQuery getSolrQuery() {
        return solrQuery;
    }
}
