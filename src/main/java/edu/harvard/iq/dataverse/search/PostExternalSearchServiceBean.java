package edu.harvard.iq.dataverse.search;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import com.google.auto.service.AutoService;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

@Stateless
@Named
@AutoService(value = SearchService.class)
public class PostExternalSearchServiceBean extends ExternalSearchServiceBean {

    @Override
    public String getServiceName() {
        return "postExternalSearch";
    }

    private JsonObject prepareQuery(String query, int paginationStart, int numResultsPerPage, String sortField,
            String sortOrder, List<String> filterQueries, boolean addHighlights, boolean addFacets,
            boolean onlyDataRelatedToMe, boolean retrieveEntities, String geoPoint, String geoRadius) {

        // Create JSON object with search parameters JsonObject searchParams =
        return NullSafeJsonBuilder.jsonObjectBuilder().add("query", query)
                .add("filterQueries", Json.createArrayBuilder(filterQueries)).add("sortField", sortField)
                .add("sortOrder", sortOrder).add("paginationStart", paginationStart)
                .add("onlyDataRelatedToMe", onlyDataRelatedToMe).add("numResultsPerPage", numResultsPerPage)
                .add("geoPoint", geoPoint).add("geoRadius", geoRadius).build();
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
        JsonObject queryJson = prepareQuery(query, paginationStart, numResultsPerPage, sortField, sortOrder,
                filterQueries, addHighlights, addFacets, onlyDataRelatedToMe, retrieveEntities, geoPoint, geoRadius);

        // Send POST request to external service
        Client client = ClientBuilder.newClient();
        Response response = client.target(externalSearchUrl).request(MediaType.APPLICATION_JSON)
                .post(Entity.json(queryJson));

        if (response.getStatus() != 200) {
            throw new SearchException("External search service returned status " + response.getStatus(), null);
        }
        try {
            // Parse response and process results
            String responseString = response.readEntity(String.class);
            logger.fine("External search returned: " + responseString);
            return postProcessResponse(responseString, dataverseRequest, retrieveEntities, addFacets, addHighlights);
        } catch (Exception e) {
            throw new SearchException("Error parsing external search service response", e);
        }
    }
}