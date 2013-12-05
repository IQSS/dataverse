package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("browse")
public class Browse {

    private static final Logger logger = Logger.getLogger(Browse.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;

    @GET
    public String browse() throws FileNotFoundException {
        try {
            logger.info("indexing...");
            if (dataverseService == null){
                return "dataverseService is null\n";
            }
            List<Dataverse> dataverses = dataverseService.findAll();
            JsonArrayBuilder filesArrayBuilder = Json.createArrayBuilder();
            JsonArrayBuilder dataversesArrayBuilder = Json.createArrayBuilder();
            JsonArrayBuilder datasetsArrayBuilder = Json.createArrayBuilder();
            for (Dataverse dataverse : dataverses) {
                logger.info("dataverse: " + dataverse.getAlias());
                JsonObjectBuilder dataverseInfoBuilder = Json.createObjectBuilder().add("alias", dataverse.getAlias());
                dataversesArrayBuilder.add(dataverseInfoBuilder);
                Long ownerId = dataverse.getId();
                List<Dataset> datasets = datasetService.findByOwnerId(ownerId);
                for (Dataset dataset : datasets) {
                    logger.info("dataset: " + dataset.getTitle());
                    String datasetInfo = dataverse.getAlias() + "|" + dataset.getTitle();
                    JsonObjectBuilder datasetObjectBuilder = Json.createObjectBuilder().add("datasetInfo", datasetInfo);
                    datasetsArrayBuilder.add(datasetObjectBuilder);
                    List<DataFile> files = dataset.getFiles();
                    for (DataFile file : files) {
                        logger.info("file: " + file.getName());
                        String fileInfo = dataverse.getAlias() + "|" + dataset.getTitle() + "|" + file.getName();
                        JsonObjectBuilder fileInfoBuilder = Json.createObjectBuilder().add("fileInfo", fileInfo);
                        filesArrayBuilder.add(fileInfoBuilder);
                    }
                }
            }
            JsonObject jsonObject = Json.createObjectBuilder()
                    .add("dataverses_total_count", dataversesArrayBuilder.build().size())
                    .add("dataverses", dataversesArrayBuilder)
                    .add("datasets_total_count", datasetsArrayBuilder.build().size())
                    .add("datasets", datasetsArrayBuilder)
                    .add("files_total_count", filesArrayBuilder.build().size())
                    .add("files", filesArrayBuilder)
                    .build();
            return jsonObject2prettyString(jsonObject);
        } catch (NullPointerException ex) {
            StackTraceElement stacktrace = ex.getStackTrace()[0];
            if (stacktrace != null) {
                String javaFile = stacktrace.getFileName();
                String methodName = stacktrace.getMethodName();
                int lineNumber = stacktrace.getLineNumber();
                String error = "Indexing failed. " + ex.getClass().getCanonicalName() + " on line " + javaFile + ":" + lineNumber + " (method: " + methodName + ")";
                logger.info(error);
                JsonObject jsonObject = Json.createObjectBuilder()
                        .add("message", "Error")
                        .add("documentation_url", "http://thedata.org")
                        .add("errors", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("code", error)))
                        .build();
                logger.info("jsonObject: " + jsonObject);
                return jsonObject2prettyString(jsonObject);
            } else {
                return null;
            }
        }
    }

    private String jsonObject2prettyString(JsonObject jsonObject) {
        Map<String, String> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, "");
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);

        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
        }
        return stringWriter.toString();
    }

}
