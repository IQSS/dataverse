package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.auto.service.AutoService;

@Stateless
@Named
@AutoService(value = SearchService.class)
public class ExternalSearchServiceBean implements SearchService {

    protected static final Logger logger = Logger.getLogger(ExternalSearchServiceBean.class.getCanonicalName());
    @EJB
    protected SettingsServiceBean settingsService;

    private SearchService solrSearchService;

    public ExternalSearchServiceBean() {
        // No-arg constructor required for @AutoService
    }
    
    @Override
    public String getServiceName() {
        return "externalSearch";
    }
    
    public String getDisplayName() {
        return settingsService.getValueForKey(SettingsServiceBean.Key.ExternalSearchName);
    }

    @Override
    public void setSolrSearchService(SearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    @Override
    public SolrQueryResponse search(DataverseRequest dataverseRequest, List<Dataverse> dataverses, String query,
            List<String> filterQueries, String sortField, String sortOrder, int paginationStart,
            boolean onlyDataRelatedToMe, int numResultsPerPage, boolean retrieveEntities, String geoPoint,
            String geoRadius, boolean addFacets, boolean addHighlights) throws SearchException {

        String externalSearchUrl = settingsService.getValueForKey(SettingsServiceBean.Key.ExternalSearchUrl);
        if (externalSearchUrl == null || externalSearchUrl.isEmpty()) {
            throw new SearchException("External search URL is not configured", null);
        }

        // Prepare query parameters
        String queryParams = prepareQuery(query, paginationStart, numResultsPerPage, sortField, sortOrder,
                filterQueries, addHighlights, addFacets, onlyDataRelatedToMe, retrieveEntities, geoPoint, geoRadius);

        // Send GET request to external service
        Client client = ClientBuilder.newClient();
        Response response = client.target(externalSearchUrl).queryParam("params", queryParams)
                .request(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            throw new SearchException("External search service returned status " + response.getStatus(), null);
        }
        try {
            // Parse response and process results
            String responseString = response.readEntity(String.class);
            logger.finest("External search returned: " + responseString);
            return postProcessResponse(responseString, dataverseRequest, retrieveEntities, addFacets, addHighlights);
        } catch (Exception e) {
            throw new SearchException("Error parsing external search service response", e);
        }
    }

    private String prepareQuery(String query, int paginationStart, int numResultsPerPage, String sortField,
            String sortOrder, List<String> filterQueries, boolean addHighlights, boolean addFacets,
            boolean onlyDataRelatedToMe, boolean retrieveEntities, String geoPoint, String geoRadius) {
        StringBuilder queryParams = new StringBuilder();
        queryParams.append("q=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        queryParams.append("&start=").append(paginationStart);
        queryParams.append("&per_page=").append(numResultsPerPage);

        if (sortField != null && !sortField.isEmpty()) {
            queryParams.append("&sort=").append(URLEncoder.encode(sortField, StandardCharsets.UTF_8));
        }
        if (sortOrder != null && !sortOrder.isEmpty()) {
            queryParams.append("&order=").append(URLEncoder.encode(sortOrder, StandardCharsets.UTF_8));
        }

        // Add filter queries with numbers
        for (int i = 0; i < filterQueries.size(); i++) {
            queryParams.append("&fq").append(i).append("=").append(filterQueries.get(i));
        }

        queryParams.append("&show_relevance=").append(addHighlights);
        queryParams.append("&show_facets=").append(addFacets);
        queryParams.append("&show_entity_ids=true");
        queryParams.append("&show_api_urls=true");
        queryParams.append("&show_my_data=").append(onlyDataRelatedToMe);
        queryParams.append("&query_entities=").append(retrieveEntities);

        if (geoPoint != null && !geoPoint.isEmpty()) {
            queryParams.append("&geo_point=").append(URLEncoder.encode(geoPoint, StandardCharsets.UTF_8));
        }
        if (geoRadius != null && !geoRadius.isEmpty()) {
            queryParams.append("&geo_radius=").append(URLEncoder.encode(geoRadius, StandardCharsets.UTF_8));
        }

        queryParams.append("&show_type_counts=true");

        return queryParams.toString();
    }

    /**
     Creates a SolrQueryResponse object from the external search service response. The external service is expected to return a JSON object
     with the following structure:
     {
      "results": [
        {
          "DOI": "doi:10.3886/ICPSR09083.v1",
          "Distance": 0.30227208137512207
        },...
      ]
     }
     
     * @param responseString - see above
     * @param dataverseRequest
     * @param retrieveEntities
     * @param addFacets
     * @param addHighlights
     * @return
     * @throws Exception
     */
    protected SolrQueryResponse postProcessResponse(String responseString, DataverseRequest dataverseRequest,
            boolean retrieveEntities, boolean addFacets, boolean addHighlights) throws Exception {

        JsonObject responseObject = JsonUtil.getJsonObject(responseString);
        JsonArray resultsArray = responseObject.getJsonArray("results");

        List<String> dois = new ArrayList<>();
        Map<String, Float> doiToDistanceMap = new HashMap<>();

        for (JsonValue value : resultsArray) {
            JsonObject result = (JsonObject) value;
            String doi = result.getString("DOI");
            float distance = result.getJsonNumber("Distance").bigDecimalValue().floatValue();
            
            dois.add(doi);
            doiToDistanceMap.put(doi, distance);
        }

        // Create a Solr query to fetch the entities
        String solrQuery = "identifier:("
                + String.join(" OR ", dois.stream().map(doi -> "\"" + doi + "\"").collect(Collectors.toList())) + ")";
        logger.fine("Query to solr: " + solrQuery);
        // Execute Solr query
        SolrQueryResponse solrResponse = solrSearchService.search(dataverseRequest, null, solrQuery,
                Collections.emptyList(), null, null, 0, false, dois.size(), retrieveEntities, null, null,
                addFacets, addHighlights);

        // Reorder results based on distance, lowest values first
        List<SolrSearchResult> reorderedResults = solrResponse.getSolrSearchResults().stream()
            .filter(result -> doiToDistanceMap.containsKey(result.getIdentifier()))
            .sorted((r1, r2) -> Float.compare(doiToDistanceMap.get(r1.getIdentifier()), doiToDistanceMap.get(r2.getIdentifier())))
            .collect(Collectors.toList());

        // Add distance information to each SolrSearchResult
        reorderedResults.forEach(result -> {
            String doi = result.getIdentifier();
            if (doiToDistanceMap.containsKey(doi)) {
                result.setScore(doiToDistanceMap.get(doi));
            }
        });

        solrResponse.setSolrSearchResults(reorderedResults);
        solrResponse.setNumResultsFound((long) reorderedResults.size());

        return solrResponse;
    }
}