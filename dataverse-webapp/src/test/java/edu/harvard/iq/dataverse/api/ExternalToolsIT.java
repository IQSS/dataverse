package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.io.IOException;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
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
        Response getExternalTools = UtilIT.getExternalToolsByFileId(fileId);
        getExternalTools.prettyPrint();
    }

    @Test
    public void testAddExternalTool() throws IOException {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explorer");
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

    @Test
    public void testAddExternalToolNoFileId() throws IOException {
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
        Response addExternalTool = UtilIT.addExternalTool(job.build());
        addExternalTool.prettyPrint();
        addExternalTool.then().assertThat()
                .body("message", CoreMatchers.equalTo("Required reserved word not found: {fileId}"))
                .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testDeleteExternalTool() {
        // FIXME: Don't hard code a tool id here.
        long externalToolId = 14l;
        Response addExternalTool = UtilIT.deleteExternalTool(externalToolId);
        addExternalTool.prettyPrint();
    }

    @Test
    public void testAddExternalToolNonReservedWord() throws IOException {
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
                        .add(Json.createObjectBuilder()
                                .add("mode", "mode1")
                                .build())
                        .build())
                .build());
        Response addExternalTool = UtilIT.addExternalTool(job.build());
        addExternalTool.prettyPrint();
        addExternalTool.then().assertThat()
                .body("message", CoreMatchers.equalTo("Unknown reserved word: mode1"))
                .statusCode(BAD_REQUEST.getStatusCode());
    }

}
