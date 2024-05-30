package edu.harvard.iq.dataverse.util.json;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

public class JsonUtil {

    private static final Logger logger = Logger.getLogger(JsonUtil.class.getCanonicalName());

    private JsonUtil() {}

    /**
     * Make an attempt at pretty printing a String but will return the original
     * string if it isn't JSON or if there is any exception.
     */
    public static String prettyPrint(String jsonString) {
        try {
            if (jsonString.trim().startsWith("{")) {
                return prettyPrint(getJsonObject(jsonString));
            } else {
                return prettyPrint(getJsonArray(jsonString));
            }
        } catch (Exception ex) {
            logger.info("Returning original string due to exception: " + ex);
            return jsonString;
        }
    }

    public static String prettyPrint(JsonArray jsonArray) {
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.writeArray(jsonArray);
        }
        return stringWriter.toString();
    }

    public static String prettyPrint(JsonObject jsonObject) {
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
        }
        return stringWriter.toString();
    }

    /**
     * Return the contents of the string as a JSON object.
     * This method closes its resources when an exception occurs, but does
     * not catch any exceptions.
     * @param serializedJson the JSON object serialized as a {@code String}
     * @throws JsonException when parsing fails.
     * @see #getJsonObject(InputStream)
     * @see #getJsonObjectFromFile(String)
     * @see #getJsonArray(String)
     */
    public static JsonObject getJsonObject(String serializedJson) {
        try (StringReader rdr = new StringReader(serializedJson)) {
            try (JsonReader jsonReader = Json.createReader(rdr)) {
                return jsonReader.readObject();
            }
        }
    }

    /**
     * Return the contents of the {@link InputStream} as a JSON object.
     *
     * This method closes its resources when an exception occurs, but does
     * not catch any exceptions.
     * The caller of this method is responsible for closing the provided stream.
     * @param stream the input stream to read from
     * @throws JsonException when parsing fails.
     * @see #getJsonObject(String)
     * @see #getJsonObjectFromFile(String)
     */
    public static JsonObject getJsonObject(InputStream stream) {
        try (JsonReader jsonReader = Json.createReader(stream)) {
            return jsonReader.readObject();
        }
    }

    /**
     * Return the contents of the file as a JSON object.
     * This method closes its resources when an exception occurs, but does
     * not catch any exceptions.
     * @param fileName the name of the file to read from
     * @throws FileNotFoundException when the file cannot be opened for reading
     * @throws JsonException when parsing fails.
     * @see #getJsonObject(String)
     * @see #getJsonObject(InputStream)
     */
    public static JsonObject getJsonObjectFromFile(String fileName) throws IOException {
        try (FileReader rdr = new FileReader(fileName)) {
            try (JsonReader jsonReader = Json.createReader(rdr)) {
                return jsonReader.readObject();
            }
        }
    }

    /**
     * Return the contents of the string as a JSON array.
     * This method closes its resources when an exception occurs, but does
     * not catch any exceptions.
     * @param serializedJson the JSON array serialized as a {@code String}
     * @throws JsonException when parsing fails.
     * @see #getJsonObject(String)
     */
    public static JsonArray getJsonArray(String serializedJson) {
        try (StringReader rdr = new StringReader(serializedJson)) {
            try (JsonReader jsonReader = Json.createReader(rdr)) {
                return jsonReader.readArray();
            }
        }
    }
}
