package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class ExternalToolServiceBeanTest {

    public ExternalToolServiceBeanTest() {
    }

    @Test
    public void testfindAll() {
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalTool externalTool = new ExternalTool("displayName", "description", "http://foo.com", "{}");
        ExternalToolHandler externalToolHandler4 = new ExternalToolHandler(externalTool, dataFile, apiToken);
        List<ExternalTool> externalTools = new ArrayList<>();
        externalTools.add(externalTool);
        List<ExternalToolHandler> externalToolHandlers = ExternalToolServiceBean.findExternalToolHandlersByFile(externalTools, dataFile, apiToken);
        assertEquals(dataFile.getId(), externalToolHandlers.get(0).getDataFile().getId());
    }

    @Test
    public void testParseAddExternalToolInput() throws IOException {
        assertEquals(null, ExternalToolServiceBean.parseAddExternalToolInput(null));
        assertEquals(null, ExternalToolServiceBean.parseAddExternalToolInput(""));
        assertEquals(null, ExternalToolServiceBean.parseAddExternalToolInput(Json.createObjectBuilder().build().toString()));
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("fileid", "{fileId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolInput(tool);
        assertEquals("AwesomeTool", externalTool.getDisplayName());
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, apiToken);
        String toolUrl = externalToolHandler.getToolUrlWithQueryParams();
        System.out.println("result: " + toolUrl);
        assertEquals("http://awesometool.com?fileid=42&key=7196b5ce-f200-4286-8809-03ffdbc255d7", toolUrl);
    }

    @Test
    public void testParseAddExternalToolInputNoFileId() throws IOException {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        // TODO: Consider failing fast right here where we parse the manifest for the tool and it doesn't use the {fileId} keyword.
        ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolInput(tool);
        assertEquals("AwesomeTool", externalTool.getDisplayName());
        DataFile nullDataFile = null;
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        Exception expectedException = null;
        try {
            ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, nullDataFile, apiToken);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("A DataFile is required.", expectedException.getMessage());
    }

}
