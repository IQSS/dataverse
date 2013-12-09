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
                    .add(SearchFields.ID, "dataverse_" + dataverse.getId())
                    .add(SearchFields.ENTITY_ID, dataverse.getId())
                    .add(SearchFields.TYPE, "dataverses")
                    .add(SearchFields.NAME, dataverse.getName())
                    .add(SearchFields.DESCRIPTION, dataverse.getDescription())
                    .add(SearchFields.AFFILIATION, dataverse.getAffiliation());
            dataversesArrayBuilder.add(dataverseInfoBuilder);
        }
        JsonArray jsonArray = dataversesArrayBuilder.build();
        return Util.jsonArray2prettyString(jsonArray);
    }
}
