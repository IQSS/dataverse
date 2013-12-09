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
        return Util.jsonObject2prettyString(dataverse2json(dataverse));
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
