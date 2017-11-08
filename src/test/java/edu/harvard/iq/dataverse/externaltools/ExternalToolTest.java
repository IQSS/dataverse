package edu.harvard.iq.dataverse.externaltools;

import javax.json.JsonObjectBuilder;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ExternalToolTest {

    // TODO: Write test for toJson.
    @Test
    public void testToJson() {
        System.out.println("toJson");
        ExternalTool externalTool = new ExternalTool();
        try {
            JsonObjectBuilder json = externalTool.toJson();
            System.out.println("JSON: " + json);
        } catch (Exception ex) {
            assertEquals(null, ex.getMessage());
        }
    }

}
