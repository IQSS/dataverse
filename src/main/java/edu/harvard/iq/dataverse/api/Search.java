package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.SolrSearchResult;
import edu.harvard.iq.dataverse.SearchServiceBean;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
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
    public String search(@QueryParam("q") String query) {
        if (query != null) {
            List<SolrSearchResult> solrSearchResults = searchService.search(query);
            JsonArrayBuilder filesArrayBuilder = Json.createArrayBuilder();
            for (SolrSearchResult solrSearchResult : solrSearchResults) {
                filesArrayBuilder.add(solrSearchResult.toJsonObject());
            }
            JsonObject value = Json.createObjectBuilder()
                    .add("total_count", solrSearchResults.size())
                    .add("items", solrSearchResults.toString())
                    .add("itemsJson", filesArrayBuilder.build())
                    .build();
            logger.info("value: " + value);
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
