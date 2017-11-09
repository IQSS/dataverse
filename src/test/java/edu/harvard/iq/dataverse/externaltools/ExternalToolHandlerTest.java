package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import javax.json.JsonObject;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ExternalToolHandlerTest {

    @Test
    public void testToJson() {
        System.out.println("toJson");
        ExternalTool externalTool = new ExternalTool("displayName", "description", "toolUrl", "{}");
        externalTool.setId(42l);
        DataFile dataFile = new DataFile();
        ApiToken apiToken = new ApiToken();
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, apiToken);
        JsonObject json = externalToolHandler.toJson().build();
        System.out.println("JSON: " + json);
        assertEquals("displayName", json.getString("displayName"));

    }
}
