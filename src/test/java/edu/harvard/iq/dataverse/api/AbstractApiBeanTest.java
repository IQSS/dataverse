package edu.harvard.iq.dataverse.api;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.core.Response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractApiBeanTest {

    private static final Logger logger = Logger.getLogger(AbstractApiBeanTest.class.getCanonicalName());

    AbstractApiBeanImpl sut;

    @BeforeEach
    public void before() {
        sut = new AbstractApiBeanImpl();
    }

    @Test
    public void testParseBooleanOrDie_ok() throws Exception {
        assertTrue(sut.parseBooleanOrDie("1"));
        assertTrue(sut.parseBooleanOrDie("yes"));
        assertTrue(sut.parseBooleanOrDie("true"));
        assertFalse(sut.parseBooleanOrDie("false"));
        assertFalse(sut.parseBooleanOrDie("0"));
        assertFalse(sut.parseBooleanOrDie("no"));
    }
    
    @Test
    void testParseBooleanOrDie_invalid() {
        assertThrows(Exception.class, () -> sut.parseBooleanOrDie("I'm not a boolean value!"));
    }

    @Test
    void testFailIfNull_ok() {
        assertDoesNotThrow(() -> sut.failIfNull(sut, ""));
    }

    @Test
    public void testMessagesNoJsonObject() {
        String message = "myMessage";
        Response response = sut.ok(message);
        JsonReader jsonReader = Json.createReader(new StringReader((String) response.getEntity().toString()));
        JsonObject jsonObject = jsonReader.readObject();
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jwf = Json.createWriterFactory(config);
        StringWriter sw = new StringWriter();
        try (JsonWriter jsonWriter = jwf.createWriter(sw)) {
            jsonWriter.writeObject(jsonObject);
        }
        logger.info(sw.toString());
        assertEquals(message, jsonObject.getJsonObject("data").getString("message"));
    }

    /**
     * dummy implementation
     */
    public class AbstractApiBeanImpl extends AbstractApiBean {

    }

}
