package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.apache.solr.client.solrj.response.FacetField;

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
    private Map<String, Long> dvObjectCounts = new HashMap<>();
    private Map<String, Long> publicationStatusCounts = new HashMap<>();

    public List<SolrSearchResult> getSolrSearchResults() {
        return solrSearchResults;
    }

    public void setPublicationStatusCounts(FacetField facetField){        
        setFacetFieldCounts(facetField, this.publicationStatusCounts);
    }
    
    public Map<String, Long> getPublicationStatusCounts(){
        return this.publicationStatusCounts;
    }
    
    public void setDvObjectCounts(FacetField facetField){
        setFacetFieldCounts(facetField, this.dvObjectCounts);
      
    }
    
    public Map<String, Long> getDvObjectCounts(){
        return this.dvObjectCounts;
    }
    
        
    private void setFacetFieldCounts(FacetField facetField,  Map<String, Long> countMap){
        if ((facetField ==null)||(countMap==null)){
            return;
        }
        
        for (FacetField.Count fcnt :  facetField.getValues()){
            countMap.put(fcnt.getName().toLowerCase() + "_count", fcnt.getCount());
        }
    }
 
    public JsonObjectBuilder getPublicationStatusCountsAsJSON(){
        
        if (this.publicationStatusCounts == null){
            return null;
        }
        return this.getMapCountsAsJSON(publicationStatusCounts);
    }
       
    
    public JsonObjectBuilder getDvObjectCountsAsJSON(){
        
        if (this.dvObjectCounts == null){
            return null;
        }
        return this.getMapCountsAsJSON(dvObjectCounts);
    }
    
    public JsonObjectBuilder getMapCountsAsJSON(Map<String, Long> countMap){
        
        if (countMap == null){
            return null;
        }
        JsonObjectBuilder jsonData = Json.createObjectBuilder();
        
        for (Map.Entry<String, Long>  entry : countMap.entrySet()) {
            jsonData.add(entry.getKey(), entry.getValue());
        }
        return jsonData;
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
