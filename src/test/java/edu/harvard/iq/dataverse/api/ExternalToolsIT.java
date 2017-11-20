package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import java.io.IOException;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExternalToolsIT {

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testGetExternalTools() {
        Response getExternalTools = UtilIT.getExternalTools();
        getExternalTools.prettyPrint();
    }

    @Test
    public void getExternalToolsByFileId() {
        // FIXME: Don't hard code the file id. Make a dataset.
        long fileId = 12;
        // FIXME: Don't card code the API token.
        String apiToken = "564e49d6-e653-4216-9c6e-996130bb67d2";
        Response getExternalTools = UtilIT.getExternalToolsByFileId(fileId, apiToken);
        getExternalTools.prettyPrint();
        // "toolUrl": "https://beta.dataverse.org/custom/DifferentialPrivacyPrototype/UI/code/interface.html?fileid=12&key=564e49d6-e653-4216-9c6e-996130bb67d2",
    }

    @Test
    public void testAddExternalTool() throws IOException {
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
        Response addExternalTool = UtilIT.addExternalTool(job.build());
        addExternalTool.prettyPrint();
        addExternalTool.then().assertThat()
                .body("data.displayName", CoreMatchers.equalTo("AwesomeTool"))
                .statusCode(OK.getStatusCode());
    }

}
