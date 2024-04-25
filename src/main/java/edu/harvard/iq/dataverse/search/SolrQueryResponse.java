package edu.harvard.iq.dataverse.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.apache.solr.client.solrj.SolrQuery;
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
    private boolean solrTemporarilyUnavailable = false;

    public static String DATAVERSES_COUNT_KEY = "dataverses_count";
    public static String DATASETS_COUNT_KEY = "datasets_count";
    public static String FILES_COUNT_KEY = "files_count";
    public static String[] DVOBJECT_COUNT_KEYS = { DATAVERSES_COUNT_KEY, DATASETS_COUNT_KEY, FILES_COUNT_KEY};
    SolrQuery solrQuery;

    public SolrQueryResponse(SolrQuery solrQuery) {
        this.solrQuery = solrQuery;
    }


    
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
            countMap.put(fcnt.getName().toLowerCase().replace(" ", "_") + "_count", fcnt.getCount());
        }
    }
 
    public JsonObjectBuilder getPublicationStatusCountsAsJSON(){
        
        if (this.publicationStatusCounts == null){
            return null;
        }

       // requiredVars = If one of these is not returned in the query,
       //              add it with a count of 0 (zero)
       //   - e.g. You always want these variable to show up in the JSON,
       //       even if they're not returned via Solr
       //
       String[] requiredVars = { "in_review_count", "unpublished_count", "published_count", "draft_count", "deaccessioned_count"};

        for (String var : requiredVars){
            if (!publicationStatusCounts.containsKey(var)){
                publicationStatusCounts.put(var, new Long(0));
            }
        }
        return this.getMapCountsAsJSON(publicationStatusCounts);
    }
    
    public boolean isSolrTemporarilyUnavailable() {
        return solrTemporarilyUnavailable;
    }
    
    public void setSolrTemporarilyUnavailable(boolean solrTemporarilyUnavailable) {
        this.solrTemporarilyUnavailable = solrTemporarilyUnavailable;
    }
    
    public JsonObjectBuilder getDvObjectCountsAsJSON(){
        
        if (this.dvObjectCounts == null){
            return null;
        }
        
        //String[] requiredVars = { "dataverses_count", "datasets_count", "files_count"};
        for (String var : SolrQueryResponse.DVOBJECT_COUNT_KEYS){
            if (!dvObjectCounts.containsKey(var)){
                dvObjectCounts.put(var, new Long(0));
            }
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

    /**
     * Check if the error string has been set
     * @return 
     */
    public boolean hasError(){
        return error != null;
    }
    
    
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public SolrQuery getSolrQuery() {
        return solrQuery;
    }

}
