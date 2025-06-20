package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

@Stateless
@Named
public class GetExternalSearchServiceBean extends AbstractExternalSearchServiceBean {

    protected static final Logger logger = Logger.getLogger(GetExternalSearchServiceBean.class.getCanonicalName());

    public GetExternalSearchServiceBean() {
        // Default constructor
    }

    @Override
    public String getServiceName() {
        return "getExternalSearch";
    }

    @Override
    public String getDisplayName() {
        return settingsService.getValueForKey(SettingsServiceBean.Key.GetExternalSearchName, "External Search (GET)");
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

        String externalSearchUrl = settingsService.getValueForKey(SettingsServiceBean.Key.GetExternalSearchUrl);
        if (externalSearchUrl == null || externalSearchUrl.isEmpty()) {
            throw new SearchException("External search URL is not configured", null);
        }

        // Prepare query parameters
        String queryParams = prepareQuery(query, paginationStart, numResultsPerPage, sortField, sortOrder,
                filterQueries, addHighlights, addFacets, onlyDataRelatedToMe, retrieveEntities, geoPoint, geoRadius);

        // Send GET request to external service
        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(externalSearchUrl).queryParam("params", queryParams)
                    .request(MediaType.APPLICATION_JSON).get();

            if (response.getStatus() != 200) {
                throw new SearchException("External search service returned status " + response.getStatus(), null);
            }

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
}