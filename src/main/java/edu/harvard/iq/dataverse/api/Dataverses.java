package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
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

        return jsonArray2prettyString(jsonArray);
//        return "foo\n";
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

    private String jsonArray2prettyString(JsonArray jsonArray) {
        Map<String, String> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, "");
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);

        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.writeArray(jsonArray);
        }
        return stringWriter.toString();
    }

}
