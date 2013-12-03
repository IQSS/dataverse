package edu.harvard.iq.dataverse.api;

import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("search")
public class Search {

    private static final Logger logger = Logger.getLogger(Search.class.getCanonicalName());

    @GET
    public JsonObject search(@QueryParam("q") String query) {
        if (query != null) {
            JsonObject value = Json.createObjectBuilder()
                    .add("message", "Query: " + query).build();
            logger.info("value: " + value);
            return value;
        } else {
            JsonObject value = Json.createObjectBuilder()
                    .add("message", "Validation Failed")
                    .add("documentation_url", "http://thedata.org")
                    .add("errors", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("field", "q")
                                    .add("code", "missing")))
                    .build();
            logger.info("value: " + value);
            return value;
        }
    }
}
