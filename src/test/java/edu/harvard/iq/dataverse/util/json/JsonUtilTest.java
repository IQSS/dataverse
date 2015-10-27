package edu.harvard.iq.dataverse.util.json;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class JsonUtilTest {

    @Test
    public void testPrettyPrint() {
        assertEquals(null, JsonUtil.prettyPrint(null));
        assertEquals("", JsonUtil.prettyPrint(""));
        assertEquals("junk", JsonUtil.prettyPrint("junk"));
        assertEquals("{}", JsonUtil.prettyPrint("{}"));
        assertEquals("{\n" + "  \"foo\": \"bar\"\n" + "}", JsonUtil.prettyPrint("{\"foo\": \"bar\"}"));
    }

}
