package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("dataverses")
public class Dataverses {

    private static final Logger logger = Logger.getLogger(Dataverses.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;

    @GET
    public String get() {
        List<Dataverse> dataverses = dataverseService.findAll();
        JsonArrayBuilder dataversesArrayBuilder = Json.createArrayBuilder();
        for (Dataverse dataverse : dataverses) {
            logger.info("dataverse: " + dataverse.getAlias());
            JsonObjectBuilder dataverseInfoBuilder = Json.createObjectBuilder()
                    .add("id", dataverse.getId())
                    .add("name", dataverse.getName())
                    .add("description", dataverse.getDescription())
                    /**
                     * @todo: change "category" to "affiliation"
                     */
                    .add("category", dataverse.getAffiliation());
            dataversesArrayBuilder.add(dataverseInfoBuilder);
            Long id = dataverse.getId();
        }
//        JsonObject jsonObject = Json.createObjectBuilder()
//                .add("dataverses_total_count", dataversesArrayBuilder.build().size())
//                .add("dataverses", dataversesArrayBuilder)
//                .build();
        JsonArray jsonArray = dataversesArrayBuilder.build();
//        return jsonObject2prettyString((JsonObject) jsonObject);

        return Util.jsonArray2prettyString(jsonArray);
    }
}
