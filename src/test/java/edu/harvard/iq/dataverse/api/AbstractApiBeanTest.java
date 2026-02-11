package edu.harvard.iq.dataverse.api;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.util.testing.FeatureFlag;
import edu.harvard.iq.dataverse.util.testing.LocalFeatureFlags;
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

@LocalFeatureFlags
class AbstractApiBeanTest {

    private static final Logger logger = Logger.getLogger(AbstractApiBeanTest.class.getCanonicalName());

    AbstractApiBeanImpl sut;

    @BeforeEach
    void before() {
        sut = new AbstractApiBeanImpl();
    }

    @Test
    void testParseBooleanOrDie_ok() throws Exception {
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
    void testMessagesNoJsonObject() {
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
    
    @Test
    @FeatureFlag(flag = FeatureFlags.UNIFY_API_RESPONSE_MESSAGE_STYLE)
    void testUnifiedMessageStyle() {
        // given
        String message = "myMessage";
        
        // when
        Response response = sut.ok(message);
        
        // then
        JsonReader jsonReader = Json.createReader(new StringReader(response.getEntity().toString()));
        JsonObject jsonObject = jsonReader.readObject();
        assertEquals(message, jsonObject.getString(ApiConstants.MESSAGE_FIELD));
    }

    /**
     * dummy implementation
     */
    public class AbstractApiBeanImpl extends AbstractApiBean {

    }

}
