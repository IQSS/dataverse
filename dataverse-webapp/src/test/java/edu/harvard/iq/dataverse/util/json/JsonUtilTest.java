package edu.harvard.iq.dataverse.util.json;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonUtilTest {

    @Test
    public void testPrettyPrint() {
        JsonUtil jsonUtil = new JsonUtil();
        String nullString = null;
        assertEquals(null, JsonUtil.prettyPrint(nullString));
        assertEquals("", JsonUtil.prettyPrint(""));
        assertEquals("junk", JsonUtil.prettyPrint("junk"));
        assertEquals("{}", JsonUtil.prettyPrint("{}"));
        assertEquals("{\n" + "  \"foo\": \"bar\"\n" + "}", JsonUtil.prettyPrint("{\"foo\": \"bar\"}"));
    }

}
