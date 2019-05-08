package edu.harvard.iq.dataverse.externaltools;

import javax.json.JsonObject;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import edu.harvard.iq.dataverse.DataFileServiceBean;

public class ExternalToolTest {

    @Test
    public void testToJson() {
        System.out.println("toJson");
        String displayName = "myDisplayName";
        String description = "myDescription";
        ExternalTool.Type type = ExternalTool.Type.EXPLORE;
        String toolUrl = "http://example.com";
        String toolParameters = "{}";
        ExternalTool externalTool = new ExternalTool(displayName, description, type, toolUrl, toolParameters, DataFileServiceBean.MIME_TYPE_TSV_ALT);
        externalTool.setId(42l);
        JsonObject jsonObject = externalTool.toJson().build();
        System.out.println("result: " + jsonObject);
        assertEquals("testToJson() with ExternalTool.DISPLAY_NAME", "myDisplayName", jsonObject.getString(ExternalTool.DISPLAY_NAME));
        assertEquals("testToJson() with ExternalTool.DESCRIPTION", "myDescription", jsonObject.getString(ExternalTool.DESCRIPTION));
        assertEquals("testToJson() with ExternalTool.TYPE", "explore", jsonObject.getString(ExternalTool.TYPE));
        assertEquals("testToJson() with ExternalTool.TOOL_URL", "http://example.com", jsonObject.getString(ExternalTool.TOOL_URL));
        assertEquals("testToJson() with ExternalTool.TOOL_PARAMETERS", "{}", jsonObject.getString(ExternalTool.TOOL_PARAMETERS));
        assertEquals("testToJson() with ExternalTool.CONTENT_TYPE", DataFileServiceBean.MIME_TYPE_TSV_ALT, jsonObject.getString(ExternalTool.CONTENT_TYPE));
    }

}
