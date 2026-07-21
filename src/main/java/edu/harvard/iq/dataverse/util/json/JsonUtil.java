package edu.harvard.iq.dataverse.util.json;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonException;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonArrayBuilder;

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
     * @see #getJsonObjectFromInputStream(InputStream)
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
    public static JsonObject getJsonObjectFromInputStream(InputStream stream) {
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
     * @see #getJsonObjectFromInputStream(InputStream)
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
    
    
    /**
     * Parses a serialized JSON string and returns it as a JsonValue.
     * The returned JsonValue can be a JsonObject, JsonArray, or another type
     * based on the structure of the provided serialized JSON string.
     * This method closes its resources but does not catch any exceptions.
     *
     * @param serializedJson The JSON content serialized as a String
     * @return The parsed content as a JsonValue which could be a JsonObject, JsonArray, or another JsonValue type
     * @throws JsonException If an error occurs during parsing (null, invalid JSON, not trimmed, etc.)
     */
    public static JsonValue getJsonValue(String serializedJson) {
        if (serializedJson == null) {
            throw new JsonException("The serialized JSON string cannot be null.");
        }
        
        try (StringReader rdr = new StringReader(serializedJson)) {
            try (JsonReader jsonReader = Json.createReader(rdr)) {
                JsonValue jsonValue = jsonReader.read();
                if (jsonValue.getValueType() == JsonValue.ValueType.OBJECT) {
                    return jsonValue.asJsonObject();
                } else if (jsonValue.getValueType() == JsonValue.ValueType.ARRAY) {
                    return jsonValue.asJsonArray();
                } else {
                    return jsonValue;
                }
            }
        }
    }

    /**
     * A factory to create Jakarta JSON-P Builders such as {@code JsonObjectBuilder} and {@code JsonArrayBuilder}.
     * This is a thread-safe, static, and final instance to manage JSON builder creation.
     *
     * <p>Using a one-time initialized factory avoids a classpath-scan on every invocation of
     * {@code JsonUtil.createArrayBuilder()} or {@code JsonUtil.createObjectBuilder()}, creating non-neglible performance issues.
     * </p>
     *
     * <p>See also <a href="https://github.com/jakartaee/jsonp-api/issues/26">JSON-P #26</a>,
     * <a href="https://github.com/eclipse-ee4j/yasson/issues/698">Yasson #698</a> and others.</p>
     */
    private static final JsonProvider provider = JsonProvider.provider();

    /**
     * Create a new {@link JsonObjectBuilder} from a cached provider instance.
     * {@link Json#createObjectBuilder()} drop-in replacement, avoiding classpath-rescan on invocation.
     * @return the JSON Object Builder
     */
    public static JsonObjectBuilder createObjectBuilder() {
        return provider.createObjectBuilder();
    }

    /**
     * Create a new {@link JsonObjectBuilder}, initialized with the specified object from a cached provider instance.
     * {@link Json#createObjectBuilder()} drop-in replacement, avoiding classpath-rescan on invocation.
     * @return the JSON Object Builder
     */
    public static JsonObjectBuilder createObjectBuilder(JsonObject object) {
        return provider.createObjectBuilder(object);
    }

    /**
     * Create a new {@link JsonObjectBuilder}, initialized with the data from specified map from a cached provider instance.
     * {@link Json#createObjectBuilder()} drop-in replacement, avoiding classpath-rescan on invocation.
     * @return the JSON Object Builder
     */
    public static JsonObjectBuilder createObjectBuilder(Map<String, ?> map) {
        return provider.createObjectBuilder(map);
    }

    /**
     * Create a new {@link JsonArrayBuilder} from a cached provider instance.
     * {@link Json#createArrayBuilder()} drop-in replacement, avoiding classpath-rescan on invocation.
     * @return the JSON Array Builder
     */
    public static JsonArrayBuilder createArrayBuilder() {
        return provider.createArrayBuilder();
    }

    /**
     * Create a new {@link JsonArrayBuilder}, initialized with the specified array from a cached provider instance.
     * {@link Json#createArrayBuilder()} drop-in replacement, avoiding classpath-rescan on invocation.
     * @return the JSON Array Builder
     */
    public static JsonArrayBuilder createArrayBuilder(JsonArray array) {
        return provider.createArrayBuilder(array);
    }

    /**
     * Create a new {@link JsonArrayBuilder}, initialized with the content of specified collection from a cached provider instance.
     * {@link Json#createArrayBuilder()} drop-in replacement, avoiding classpath-rescan on invocation.
     * @return the JSON Array Builder
     */
    public static JsonArrayBuilder createArrayBuilder(Collection<?> collection) {
        return provider.createArrayBuilder(collection);
    }
}
