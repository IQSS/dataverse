package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("datasets")
public class Datasets {

    private static final Logger logger = Logger.getLogger(Datasets.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;

    @GET
    public String get() {
        List<Dataset> datasets = datasetService.findAll();
        JsonArrayBuilder datasetsArrayBuilder = Json.createArrayBuilder();
        for (Dataset dataset : datasets) {
            logger.info("dataset: " + dataset.getTitle());
            JsonObjectBuilder datasetInfoBuilder = Json.createObjectBuilder()
                    .add("id", dataset.getId())
                    .add("name", dataset.getTitle())
                    .add("description", dataset.getDescription());
            datasetsArrayBuilder.add(datasetInfoBuilder);
        }
        JsonArray jsonArray = datasetsArrayBuilder.build();
        return Util.jsonArray2prettyString(jsonArray);
    }
}
