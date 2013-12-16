package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.FacetCategory;
import edu.harvard.iq.dataverse.FacetLabel;
import edu.harvard.iq.dataverse.SolrSearchResult;
import edu.harvard.iq.dataverse.SearchServiceBean;
import edu.harvard.iq.dataverse.SolrQueryResponse;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("search")
public class Search {

    private static final Logger logger = Logger.getLogger(Search.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;

    @GET
//    public JsonObject search(@QueryParam("q") String query) {
    public String search(@QueryParam("q") String query, @QueryParam("fq") final List<String> filterQueries) {
        if (query != null) {
            SolrQueryResponse solrQueryResponse = searchService.search(query, filterQueries);

            JsonArrayBuilder filesArrayBuilder = Json.createArrayBuilder();
            List<SolrSearchResult> solrSearchResults = solrQueryResponse.getSolrSearchResults();
            for (SolrSearchResult solrSearchResult : solrSearchResults) {
                filesArrayBuilder.add(solrSearchResult.toJsonObject());

            }

            JsonObjectBuilder spelling_alternatives = Json.createObjectBuilder();
            for (Map.Entry<String, List<String>> entry : solrQueryResponse.getSpellingSuggestionsByToken().entrySet()) {
                spelling_alternatives.add(entry.getKey(), entry.getValue().toString());
            }

            JsonArrayBuilder facets = Json.createArrayBuilder();
            JsonObjectBuilder facetCategoryBuilder = Json.createObjectBuilder();
            for (FacetCategory facetCategory : solrQueryResponse.getFacetCategoryList()) {
                JsonArrayBuilder facetLabelBuilder = Json.createArrayBuilder();
                for (FacetLabel facetLabel : facetCategory.getFacetLabel()) {
                    JsonObjectBuilder countBuilder = Json.createObjectBuilder();
                    countBuilder.add(facetLabel.getName(), facetLabel.getCount());
                    facetLabelBuilder.add(countBuilder);
                }
                facetCategoryBuilder.add(facetCategory.getName(), facetLabelBuilder);
            }
            facets.add(facetCategoryBuilder);

            JsonObject value = Json.createObjectBuilder()
                    .add("total_count", solrSearchResults.size())
                    .add("items", solrSearchResults.toString())
                    .add("spelling_alternatives", spelling_alternatives)
                    .add("itemsJson", filesArrayBuilder.build())
                    .add("facets", facets)
                    .build();
//            logger.info("value: " + value);
            return Util.jsonObject2prettyString(value);
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
