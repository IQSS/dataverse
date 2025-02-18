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
    private SearchService solrSearchService;
    @EJB
    private SettingsServiceBean settingsService;

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