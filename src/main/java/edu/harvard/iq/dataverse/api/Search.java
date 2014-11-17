package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.FacetCategory;
import edu.harvard.iq.dataverse.FacetLabel;
import edu.harvard.iq.dataverse.SolrSearchResult;
import edu.harvard.iq.dataverse.SearchServiceBean;
import edu.harvard.iq.dataverse.SolrQueryResponse;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("search")
public class Search extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Search.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;
    @EJB
    DataverseServiceBean dataverseService;

    @GET
    public Response search(@QueryParam("key") String apiToken,
            @QueryParam("q") String query,
            @QueryParam("fq") final List<String> filterQueries,
            @QueryParam("sort") String sortField,
            @QueryParam("order") String sortOrder,
            @QueryParam("start") final int paginationStart,
            @QueryParam("showrelevance") boolean showRelevance) {
        if (query != null) {
            if (sortField == null) {
                // predictable default
                sortField = SearchFields.ID;
            }
            if (sortOrder == null) {
                // asc for alphabetical by default despite GitHub using desc by default: "The sort order if sort parameter is provided. One of asc or desc. Default: desc" -- http://developer.github.com/v3/search/
                sortOrder = "asc";
            }
            SolrQueryResponse solrQueryResponse;
            try {
                User dataverseUser = null;
                if (apiToken != null) {
                    dataverseUser = findUserByApiToken(apiToken);
                    if (dataverseUser == null) {
                        String message = "Unable to find a user with API token provided.";
                        return errorResponse(Response.Status.FORBIDDEN, message);
                    }
                }
                SearchServiceBean.PublishedToggle publishedToggle = SearchServiceBean.PublishedToggle.PUBLISHED;
                solrQueryResponse = searchService.search(dataverseUser, dataverseService.findRootDataverse(), query, filterQueries, sortField, sortOrder, paginationStart, publishedToggle);
            } catch (EJBException ex) {
                Throwable cause = ex;
                StringBuilder sb = new StringBuilder();
                sb.append(cause + " ");
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                    sb.append(cause.getClass().getCanonicalName() + " ");
                    sb.append(cause + " ");
                    // if you search for a colon you see RemoteSolrException: org.apache.solr.search.SyntaxError: Cannot parse ':'
                }
                String message = "Exception running search for [" + query + "] with filterQueries " + filterQueries + " and paginationStart [" + paginationStart + "]: " + sb.toString();
                logger.info(message);
                return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
            }

            JsonArrayBuilder itemsArrayBuilder = Json.createArrayBuilder();
            JsonArrayBuilder relevancePerResult = Json.createArrayBuilder();
            List<SolrSearchResult> solrSearchResults = solrQueryResponse.getSolrSearchResults();
            for (SolrSearchResult solrSearchResult : solrSearchResults) {
                itemsArrayBuilder.add(solrSearchResult.toJsonObject());
                relevancePerResult.add(solrSearchResult.getRelevance());
            }

            JsonObjectBuilder spelling_alternatives = Json.createObjectBuilder();
            for (Map.Entry<String, List<String>> entry : solrQueryResponse.getSpellingSuggestionsByToken().entrySet()) {
                spelling_alternatives.add(entry.getKey(), entry.getValue().toString());
            }

            JsonArrayBuilder facets = Json.createArrayBuilder();
            JsonObjectBuilder facetCategoryBuilder = Json.createObjectBuilder();
            for (FacetCategory facetCategory : solrQueryResponse.getFacetCategoryList()) {
                JsonObjectBuilder facetCategoryBuilderFriendlyPlusData = Json.createObjectBuilder();
                JsonArrayBuilder facetLabelBuilderData = Json.createArrayBuilder();
                for (FacetLabel facetLabel : facetCategory.getFacetLabel()) {
                    JsonObjectBuilder countBuilder = Json.createObjectBuilder();
                    countBuilder.add(facetLabel.getName(), facetLabel.getCount());
                    facetLabelBuilderData.add(countBuilder);
                }
                facetCategoryBuilderFriendlyPlusData.add("friendly", facetCategory.getFriendlyName());
                facetCategoryBuilderFriendlyPlusData.add("labels", facetLabelBuilderData);
                facetCategoryBuilder.add(facetCategory.getName(), facetCategoryBuilderFriendlyPlusData);
            }
            facets.add(facetCategoryBuilder);
            Map<String, String> datasetfieldFriendlyNamesBySolrField = solrQueryResponse.getDatasetfieldFriendlyNamesBySolrField();
//            logger.info("the hash: " + datasetfieldFriendlyNamesBySolrField);
            for (Map.Entry<String, String> entry : datasetfieldFriendlyNamesBySolrField.entrySet()) {
                String string = entry.getKey();
                String string1 = entry.getValue();
//                logger.info(string + ":" + string1);
            }

            List filterQueriesActual = solrQueryResponse.getFilterQueriesActual();
            JsonObjectBuilder value = Json.createObjectBuilder()
                    .add("q", query)
                    .add("fq_provided", filterQueries.toString())
                    .add("fq_actual", filterQueriesActual.toString())
                    .add("total_count", solrQueryResponse.getNumResultsFound())
                    .add("start", solrQueryResponse.getResultsStart())
                    .add("count_in_response", solrSearchResults.size())
                    .add("items", solrSearchResults.toString());
            if (showRelevance) {
                value.add("relevance", relevancePerResult.build());
            }
            if (false) {
                /*
                 * @todo: add booleans to enable these
                 * You can use SettingsServiceBeans for this
                 */
                value.add("spelling_alternatives", spelling_alternatives);
                value.add("facets", facets);
            }
            if (solrQueryResponse.getError() != null) {
                /**
                 * @todo You get here if you pass only ":" as a query, for
                 * example. Should we return more or better information?
                 */
                return errorResponse(Response.Status.BAD_REQUEST, solrQueryResponse.getError());
            }
            return okResponse(value);
        } else {
            return errorResponse(Response.Status.BAD_REQUEST, "q parameter is missing");
        }
    }

    /**
     * This method is for integration tests of search and should be disabled
     * with the boolean within it before release.
     */
    @GET
    @Path("test")
    public Response searchDebug(
            @QueryParam("key") String apiToken,
            @QueryParam("q") String query,
            @QueryParam("fq") final List<String> filterQueries) {

        boolean searchTestMethodDisabled = false;
        if (searchTestMethodDisabled) {
            return errorResponse(Response.Status.BAD_REQUEST, "disabled");
        }

        User user = findUserByApiToken(apiToken);
        if (user == null) {
            return errorResponse(Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiToken + "'");
        }

        SearchServiceBean.PublishedToggle publishedToggle = SearchServiceBean.PublishedToggle.PUBLISHED;
        Dataverse subtreeScope = dataverseService.findRootDataverse();

        String sortField = SearchFields.ID;
        String sortOrder = "asc";
        int paginationStart = 0;
        SolrQueryResponse solrQueryResponse = searchService.search(user, subtreeScope, query, filterQueries, sortField, sortOrder, paginationStart, publishedToggle);

        JsonArrayBuilder itemsArrayBuilder = Json.createArrayBuilder();
        List<SolrSearchResult> solrSearchResults = solrQueryResponse.getSolrSearchResults();
        for (SolrSearchResult solrSearchResult : solrSearchResults) {
            itemsArrayBuilder.add(solrSearchResult.getType() + ":" + solrSearchResult.getNameSort());
        }

        return okResponse(itemsArrayBuilder);
    }

}
