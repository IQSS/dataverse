package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
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

    private static final Logger logger = Logger.getLogger(ExternalSearchServiceBean.class.getCanonicalName());
    @EJB
    private SettingsServiceBean settingsService;

    private SearchService solrSearchService;

    @Override
    public String getServiceName() {
        return "externalSearch";
    }

    @Override
    public void setSolrSearchService(SearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    @Override
    public SolrQueryResponse search(
            DataverseRequest dataverseRequest,
            List<Dataverse> dataverses,
            String query,
            List<String> filterQueries,
            String sortField,
            String sortOrder,
            int paginationStart,
            boolean onlyDataRelatedToMe,
            int numResultsPerPage,
            boolean retrieveEntities,
            String geoPoint,
            String geoRadius,
            boolean addFacets,
            boolean addHighlights
    ) throws SearchException {
        
        String externalSearchUrl = settingsService.getValueForKey(SettingsServiceBean.Key.ExternalSearchUrl);
        if (externalSearchUrl == null || externalSearchUrl.isEmpty()) {
            throw new SearchException("External search URL is not configured", null);
        }
        
        // Prepare query parameters
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

        //ToDo: types and subtrees are already filterqueries at this point - should they be sent directly in the search parameters?
        //Metadata fields are only used in post processing after the solr search - whould they be passed through here?
        
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

        // Send GET request to external service
        Client client = ClientBuilder.newClient();
        Response response = client.target(externalSearchUrl)
                .queryParam("params", queryParams)
                .request(MediaType.APPLICATION_JSON)
                .get();
        
        /* POST alternative
        
        // Create JSON object with search parameters
        JsonObject searchParams = Json.createObjectBuilder()
                .add("query", query)
                .add("filterQueries", Json.createArrayBuilder(filterQueries))
                .add("sortField", sortField)
                .add("sortOrder", sortOrder)
                .add("paginationStart", paginationStart)
                .add("onlyDataRelatedToMe", onlyDataRelatedToMe)
                .add("numResultsPerPage", numResultsPerPage)
                .add("geoPoint", geoPoint)
                .add("geoRadius", geoRadius)
                .build();

        // Send POST request to external service
        Client client = ClientBuilder.newClient();
        Response response = client.target(externalSearchUrl)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(searchParams));
        */
        if (response.getStatus() != 200) {
            throw new SearchException("External search service returned status " + response.getStatus(), null);
        }

        // Parse response to get list of entityIds
        // Parse response to get list of entityIds
        JsonArray entityIdsJson = response.readEntity(JsonArray.class);
        List<Long> entityIds = entityIdsJson.stream()
                .mapToLong(jsonValue -> ((JsonNumber) jsonValue).longValue())
                .boxed()
                .collect(Collectors.toList());

        // Create a Solr query to fetch the entities
        String solrQuery = "id:(" + String.join(" OR ", entityIds.stream().map(Object::toString).collect(Collectors.toList())) + ")";

        // Execute Solr query
        SolrQueryResponse solrResponse = solrSearchService.search(
                dataverseRequest, null, solrQuery, Collections.emptyList(),
                null, null, 0, false, entityIds.size(), retrieveEntities,
                null, null, addFacets, addHighlights
        );

        // Reorder results to match the order of entityIds
        Map<Long, SolrSearchResult> resultMap = solrResponse.getSolrSearchResults().stream()
                .collect(Collectors.toMap(SolrSearchResult::getEntityId, r -> r));

        List<SolrSearchResult> reorderedResults = entityIds.stream()
                .map(resultMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        solrResponse.setSolrSearchResults(reorderedResults);
        solrResponse.setNumResultsFound((long) reorderedResults.size());

        return solrResponse;
    }
}