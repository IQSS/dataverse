package edu.harvard.iq.dataverse.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public abstract class AbstractExternalSearchServiceBean implements ConfigurableSearchService {

    protected static final Logger logger = Logger
            .getLogger(AbstractExternalSearchServiceBean.class.getCanonicalName());

    protected SettingsServiceBean settingsService;

    protected SearchService solrSearchService;

    @Override
    public void setSolrSearchService(SearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    @Override
    public void setSettingsService(SettingsServiceBean settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Creates a SolrQueryResponse object from the external search service response.
     * The external service is expected to return a JSON object with the following
     * structure: { "results": [ { "PID": "doi:10.3886/ICPSR09083.v1", "Distance":
     * 0.30227208137512207 },... ] }
     * "DOI" can be used instead of "PID" (preferred) 
     * 
     * @param responseString   - see above
     * @param dataverseRequest
     * @param retrieveEntities
     * @param addFacets
     * @param addHighlights
     * @return
     * @throws Exception
     */
    protected SolrQueryResponse postProcessResponse(String responseString, DataverseRequest dataverseRequest,
            boolean retrieveEntities, boolean addFacets, boolean addHighlights, boolean addCollections) throws Exception {

        JsonObject responseObject = JsonUtil.getJsonObject(responseString);
        JsonArray resultsArray = responseObject.getJsonArray("results");

        List<String> pids = new ArrayList<>();
        Map<String, Float> pidToDistanceMap = new HashMap<>();

        for (JsonValue value : resultsArray) {
            JsonObject result = (JsonObject) value;
            String pid = null;
            if (result.containsKey("PID")) {
                pid = result.getString("PID");
            }
            //Backward compatibility
            if(result.containsKey("DOI")) {
                pid = result.getString("DOI");
            } 

            float distance = result.getJsonNumber("Distance").bigDecimalValue().floatValue();

            pids.add(pid);
            pidToDistanceMap.put(pid, distance);
        }

        // Create a Solr query to fetch the entities
        String solrQuery = "identifier:("
                + String.join(" OR ", pids.stream().map(pid -> "\"" + pid + "\"").collect(Collectors.toList())) + ")";
        logger.fine("Query to solr: " + solrQuery);
        // Execute Solr query
        SolrQueryResponse solrResponse = solrSearchService.search(dataverseRequest, null, solrQuery,
                Collections.emptyList(), null, null, 0, false, pids.size(), retrieveEntities, null, null, addFacets,
                addHighlights, addCollections);

        // Reorder results based on distance, lowest values first
        List<SolrSearchResult> reorderedResults = solrResponse.getSolrSearchResults().stream()
                .filter(result -> pidToDistanceMap.containsKey(result.getIdentifier())).sorted((r1, r2) -> Float
                        .compare(pidToDistanceMap.get(r1.getIdentifier()), pidToDistanceMap.get(r2.getIdentifier())))
                .collect(Collectors.toList());

        // Add distance information to each SolrSearchResult
        reorderedResults.forEach(result -> {
            String pid = result.getIdentifier();
            if (pidToDistanceMap.containsKey(pid)) {
                result.setScore(pidToDistanceMap.get(pid));
            }
        });

        solrResponse.setSolrSearchResults(reorderedResults);
        solrResponse.setNumResultsFound((long) reorderedResults.size());

        return solrResponse;
    }
}
