package edu.harvard.iq.dataverse.externaltools;

import jakarta.json.JsonObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.DataFileServiceBean;

import java.util.ArrayList;
import java.util.List;

public class ExternalToolTest {

    @Test
    public void testToJson() {
        System.out.println("toJson");
        String displayName = "myDisplayName";
        String toolName = "explorer";
        String description = "myDescription";
        ExternalTool.Type type = ExternalTool.Type.EXPLORE;
        List<ExternalToolType> externalToolTypes = new ArrayList<>();
        ExternalToolType externalToolType = new ExternalToolType();
        externalToolType.setType(ExternalTool.Type.EXPLORE);
        externalToolTypes.add(externalToolType);
        ExternalTool.Scope scope = ExternalTool.Scope.FILE;
        String toolUrl = "http://example.com";
        String toolParameters = "{}";
        ExternalTool externalTool = new ExternalTool(displayName, toolName, description, externalToolTypes, scope, toolUrl, toolParameters, DataFileServiceBean.MIME_TYPE_TSV_ALT);
        externalTool.setId(42l);
        JsonObject jsonObject = externalTool.toJson().build();
        System.out.println("result: " + jsonObject);
        assertEquals("myDisplayName", jsonObject.getString(ExternalTool.DISPLAY_NAME), "testToJson() with ExternalTool.DISPLAY_NAME");
        assertEquals("explorer", jsonObject.getString(ExternalTool.TOOL_NAME), "testToJson() with ExternalTool.TOOL_NAME");
        assertEquals("myDescription", jsonObject.getString(ExternalTool.DESCRIPTION), "testToJson() with ExternalTool.DESCRIPTION");
        assertEquals("explore", jsonObject.getJsonArray(ExternalTool.TYPES).getString(0), "testToJson() with ExternalTool.TYPES");
        assertEquals("http://example.com", jsonObject.getString(ExternalTool.TOOL_URL), "testToJson() with ExternalTool.TOOL_URL");
        assertEquals(0, jsonObject.getJsonObject(ExternalTool.TOOL_PARAMETERS).size(), "testToJson() with ExternalTool.TOOL_PARAMETERS should be empty");
        assertEquals(DataFileServiceBean.MIME_TYPE_TSV_ALT, jsonObject.getString(ExternalTool.CONTENT_TYPE), "testToJson() with ExternalTool.CONTENT_TYPE");
    }

    @Test
    public void testGetAllowedApiCalls() {
        System.out.println("testGetAllowedApiCalls");
        String displayName = "myDisplayName";
        String toolName = "explorer";
        String description = "myDescription";
        ExternalTool.Type type = ExternalTool.Type.EXPLORE;
        List<ExternalToolType> externalToolTypes = new ArrayList<>();
        ExternalToolType externalToolType = new ExternalToolType();
        externalToolType.setType(ExternalTool.Type.EXPLORE);
        externalToolTypes.add(externalToolType);
        ExternalTool.Scope scope = ExternalTool.Scope.FILE;
        String toolUrl = "http://example.com";
        String toolParameters = "{}";
        String allowedApiCalls = "[{\"name\":\"retrieveFileContents\",\"httpMethod\":\"GET\",\"urlTemplate\":\"/api/v1/access/datafile/{fileId}?gbrecs=true\",\"timeOut\":3600},{\"name\":\"downloadFile\",\"httpMethod\":\"GET\",\"urlTemplate\":\"/api/v1/access/datafile/{fileId}?gbrecs=false\",\"timeOut\":3600},{\"name\":\"getDatasetVersionMetadata\",\"httpMethod\":\"GET\",\"urlTemplate\":\"/api/v1/datasets/{datasetId}/versions/{datasetVersion}\",\"timeOut\":3600}]";
        ExternalTool externalTool = new ExternalTool(displayName, toolName, description, externalToolTypes, scope, toolUrl, toolParameters, DataFileServiceBean.MIME_TYPE_TSV_ALT, allowedApiCalls, null);
        externalTool.setId(42l);
        externalTool.setGuestbookResponseId(2L);
        JsonObject jsonObject = externalTool.toJson().build();
        System.out.println("result: " + jsonObject);
        assertTrue(jsonObject.toString().contains("gbrecs=true&gbrids=2&externalToolId=42"));
        assertTrue(jsonObject.toString().contains("gbrecs=false&gbrids=2&externalToolId=42"));
    }
}
