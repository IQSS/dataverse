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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

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
                    .add(SearchFields.ID, "dataset_" + dataset.getId())
                    .add(SearchFields.ENTITY_ID, dataset.getId())
                    .add(SearchFields.TYPE, "datasets")
                    /**
                     * @todo: should we assign a dataset title to name like
                     * this?
                     */
                    .add("name", dataset.getTitle())
                    .add(SearchFields.AUTHOR_STRING, dataset.getAuthor())
                    .add(SearchFields.TITLE, dataset.getTitle())
                    .add(SearchFields.DESCRIPTION, dataset.getDescription());
            datasetsArrayBuilder.add(datasetInfoBuilder);
        }
        JsonArray jsonArray = datasetsArrayBuilder.build();
        return Util.jsonArray2prettyString(jsonArray);
    }

    // used to primarily to feed data into elasticsearch
    @GET
    @Path("{id}/{verb}")
    public Dataset get(@PathParam("id") Long id, @PathParam("verb") String verb) {
        logger.info("GET called");
        if (verb.equals("dump")) {
            Dataset dataset = datasetService.find(id);
            if (dataset != null) {
                logger.info("found " + dataset);
                // prevent HTTP Status 500 - Internal Server Error
                dataset.setFiles(null);
                // elasticsearch fails on "today" with
                // MapperParsingException[failed to parse date field [today],
                // tried both date format [dateOptionalTime], and timestamp number with locale []]
                dataset.setCitationDate(null);
                // too much information
                dataset.setOwner(null);
                return dataset;
            }
        }
        /**
         * @todo return an error instead of "204 No Content"?
         *
         */
        logger.info("GET attempted with dataset id " + id + " and verb " + verb);
        return null;
    }

    @POST
    public String add(Dataset dataset){
        datasetService.save(dataset);
        return "dataset " + dataset.getTitle() + " created\n";
    }
}
