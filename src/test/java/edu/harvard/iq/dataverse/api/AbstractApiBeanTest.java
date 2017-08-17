package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.MockResponse;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.core.Response;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class AbstractApiBeanTest {

    private static final Logger logger = Logger.getLogger(AbstractApiBeanTest.class.getCanonicalName());

    AbstractApiBeanImpl sut;

    @Before
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

    @Test(expected = Exception.class)
    public void testParseBooleanOrDie_invalid() throws Exception {
        sut.parseBooleanOrDie("I'm not a boolean value!");
    }

    @Test
    public void testFailIfNull_ok() throws Exception {
        sut.failIfNull(sut, "");
    }

    @Test
    public void testAllowCors() {
        Response r = sut.allowCors(new MockResponse(200));
        assertEquals("*", r.getHeaderString("Access-Control-Allow-Origin"));
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
