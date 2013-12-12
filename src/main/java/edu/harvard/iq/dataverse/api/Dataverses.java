package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

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
            dataversesArrayBuilder.add(dataverse2json(dataverse));
        }
        JsonArray jsonArray = dataversesArrayBuilder.build();
        return Util.jsonArray2prettyString(jsonArray);
    }

    @GET
    @Path("{id}")
    public String get(@PathParam("id") Long id) {
        Dataverse dataverse = dataverseService.find(id);
        if (dataverse != null) {
            return Util.jsonObject2prettyString(dataverse2json(dataverse));
        } else {
            /**
             * @todo inconsistent with /{id}/dump which simply returns nothing
             * and "204 No Content"
             */
            return Util.message2ApiError("Dataverse id " + id + " not found");
        }
    }

    // used to primarily to feed data into elasticsearch
    @GET
    @Path("{id}/{verb}")
    public Dataverse get(@PathParam("id") Long id, @PathParam("verb") String verb) {
        if (verb.equals("dump")) {
            Dataverse dataverse = dataverseService.find(id);
            if (dataverse != null) {
                return dataverse;
            }
        }
        /**
         * @todo return an error instead of "204 No Content"?
         *
         */
        logger.info("GET attempted with dataverse id " + id + " and verb " + verb);
        return null;
    }

    @POST
    public String add(Dataverse dataverse) {
        dataverseService.save(dataverse);
        return "dataverse " + dataverse.getAlias() + " created\n";
    }

    public JsonObject dataverse2json(Dataverse dataverse) {
        JsonObjectBuilder dataverseInfoBuilder = Json.createObjectBuilder()
                .add(SearchFields.ID, "dataverse_" + dataverse.getId())
                .add(SearchFields.ENTITY_ID, dataverse.getId())
                .add(SearchFields.TYPE, "dataverses")
                .add(SearchFields.NAME, dataverse.getName())
                .add(SearchFields.DESCRIPTION, dataverse.getDescription())
                .add(SearchFields.AFFILIATION, dataverse.getAffiliation());
        return dataverseInfoBuilder.build();
    }
}
