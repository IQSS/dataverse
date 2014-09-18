package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.FacetCategory;
import edu.harvard.iq.dataverse.FacetLabel;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.SolrSearchResult;
import edu.harvard.iq.dataverse.SearchServiceBean;
import edu.harvard.iq.dataverse.SolrQueryResponse;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;

@Path("search")
public class Search extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Search.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DataverseUserServiceBean dataverseUserService;

    @GET
//    public JsonObject search(@QueryParam("q") String query) {
    public String search(@QueryParam("key") String apiKey,
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
                DataverseUser dataverseUser = null;
                if (apiKey != null) {
                    String usernameProvided = apiKey;
                    dataverseUser = dataverseUserService.findByUserName(usernameProvided);
                    if (dataverseUser == null) {
                        return error("Couldn't find username: " + usernameProvided);
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
                return Util.message2ApiError(message);
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
                /**
                 * @todo: add booleans to enable these
                 */
                value.add("spelling_alternatives", spelling_alternatives);
                value.add("facets", facets);
            }
            if (solrQueryResponse.getError() != null) {
                value.add("error", solrQueryResponse.getError());
            }
            return Util.jsonObject2prettyString(value.build());
        } else {
            /**
             * @todo use Util.message2ApiError() instead
             */
            JsonObject value = Json.createObjectBuilder()
                    .add("message", "Validation Failed")
                    .add("documentation_url", "http://thedata.org")
                    .add("errors", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("field", "q")
                                    .add("code", "missing")))
                    .build();
            logger.info("value: " + value);
            return value.toString();
        }
    }
}
