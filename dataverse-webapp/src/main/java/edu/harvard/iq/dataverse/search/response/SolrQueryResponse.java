package edu.harvard.iq.dataverse.search.response;

import org.apache.solr.client.solrj.SolrQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrQueryResponse {

    private List<SolrSearchResult> solrSearchResults;
    private Long numResultsFound;
    private Long resultsStart;
    private Map<String, List<String>> spellingSuggestionsByToken;
    private List<FacetCategory> facetCategoryList;
    private Map<String, String> datasetfieldFriendlyNamesBySolrField = new HashMap<>();
    private Map<String, String> staticSolrFieldFriendlyNamesBySolrField = new HashMap<>();
    private List<String> filterQueriesActual = new ArrayList<String>();
    private DvObjectCounts dvObjectCounts = DvObjectCounts.emptyDvObjectCounts();
    private PublicationStatusCounts publicationStatusCounts = PublicationStatusCounts.emptyPublicationStatusCounts();

    SolrQuery solrQuery;

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

    public Map<String, String> getDatasetfieldFriendlyNamesBySolrField() {
        return datasetfieldFriendlyNamesBySolrField;
    }

    void setDatasetfieldFriendlyNamesBySolrField(Map<String, String> datasetfieldFriendlyNamesBySolrField) {
        this.datasetfieldFriendlyNamesBySolrField = datasetfieldFriendlyNamesBySolrField;
    }

    public Map<String, String> getStaticSolrFieldFriendlyNamesBySolrField() {
        return staticSolrFieldFriendlyNamesBySolrField;
    }

    void setStaticSolrFieldFriendlyNamesBySolrField(Map<String, String> staticSolrFieldFriendlyNamesBySolrField) {
        this.staticSolrFieldFriendlyNamesBySolrField = staticSolrFieldFriendlyNamesBySolrField;
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
