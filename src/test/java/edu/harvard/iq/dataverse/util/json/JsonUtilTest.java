package edu.harvard.iq.dataverse.util.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class JsonUtilTest {

    @Test
    void testPrettyPrint() {
        String nullString = null;
        assertEquals(null, JsonUtil.prettyPrint(nullString));
        assertEquals("", JsonUtil.prettyPrint(""));
        assertEquals("junk", JsonUtil.prettyPrint("junk"));
        assertEquals("{\n}", JsonUtil.prettyPrint("{}"));
        assertEquals("[\n    \"junk\"\n]", JsonUtil.prettyPrint("[\"junk\"]"));
        assertEquals("{\n" + "    \"foo\": \"bar\"\n" + "}", JsonUtil.prettyPrint("{\"foo\": \"bar\"}"));
    }

}
