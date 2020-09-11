package edu.harvard.iq.dataverse.util.json;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.logging.Logger;
import javax.json.*;
import javax.json.stream.JsonGenerator;

public class JsonUtil {

    private static final Logger logger = Logger.getLogger(JsonUtil.class.getCanonicalName());
    private static JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));

    /**
     * Make an attempt at pretty printing a String but will return the original
     * string if it isn't JSON or if there is any exception.
     * Null- and empty-safe.
     */
    public static String prettyPrint(String jsonString) {
        if (jsonString == null || "".equals(jsonString)) return jsonString;
        try {
            JsonReader reader = Json.createReader(new StringReader(jsonString));
            JsonStructure struct = reader.read();
            return prettyPrint(struct);
        } catch (Exception ex) {
            logger.info("Returning original string due to exception: " + ex);
            return jsonString;
        }
    }
    
    /**
     * Prettyprint any Jakarta EE JSON-P structure (object, array, value, ...).
     * @param jsonStructure
     * @return Pretty JSON string
     */
    public static String prettyPrint(JsonStructure jsonStructure) {
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.write(jsonStructure);
        }
        return stringWriter.toString();
    }

}
