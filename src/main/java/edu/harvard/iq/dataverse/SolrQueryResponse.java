package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SolrQueryResponse {

    private static final Logger logger = Logger.getLogger(SolrQueryResponse.class.getCanonicalName());

    private List<SolrSearchResult> solrSearchResults;
    private Long numResultsFound;
    private Long resultsStart;
    private Map<String, List<String>> spellingSuggestionsByToken;
    private List<FacetCategory> facetCategoryList;
    private List<FacetCategory> typeFacetCategories;
    Map<String, String> datasetfieldFriendlyNamesBySolrField = new HashMap<>();
    private Map<String, String> staticSolrFieldFriendlyNamesBySolrField;
    private List<String> filterQueriesActual = new ArrayList<String>();
    private String error;

    public List<SolrSearchResult> getSolrSearchResults() {
        return solrSearchResults;
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

    public List<FacetCategory> getTypeFacetCategories() {
        return typeFacetCategories;
    }

    public void setTypeFacetCategories(List<FacetCategory> typeFacetCategories) {
        this.typeFacetCategories = typeFacetCategories;
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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}
