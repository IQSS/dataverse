package edu.harvard.iq.dataverse.externaltools;

import javax.json.JsonObject;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ExternalToolTest {

    @Test
    public void testToJson() {
        System.out.println("toJson");
        String displayName = "myDisplayName";
        String description = "myDescription";
        ExternalTool.Type type = ExternalTool.Type.EXPLORE;
        String toolUrl = "http://example.com";
        String toolParameters = "{}";
        ExternalTool externalTool = new ExternalTool(displayName, description, type, toolUrl, toolParameters);
        externalTool.setId(42l);
        JsonObject jsonObject = externalTool.toJson().build();
        System.out.println("result: " + jsonObject);
        assertEquals("myDisplayName", jsonObject.getString("displayName"));
        assertEquals("myDescription", jsonObject.getString("description"));
        assertEquals("explore", jsonObject.getString("type"));
        assertEquals("http://example.com", jsonObject.getString("toolUrl"));
        assertEquals("{}", jsonObject.getString("toolParameters"));
    }

}
