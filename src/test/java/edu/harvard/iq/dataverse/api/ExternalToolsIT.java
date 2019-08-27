package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.io.IOException;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
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
    public void testFileLevelTool1() {

        // Delete all external tools before testing.
        Response getTools = UtilIT.getExternalTools();
        getTools.prettyPrint();
        getTools.then().assertThat()
                .statusCode(OK.getStatusCode());
        String body = getTools.getBody().asString();
        JsonReader bodyObject = Json.createReader(new StringReader(body));
        JsonArray tools = bodyObject.readObject().getJsonArray("data");
        for (int i = 0; i < tools.size(); i++) {
            JsonObject tool = tools.getJsonObject(i);
            int id = tool.getInt("id");
            Response deleteExternalTool = UtilIT.deleteExternalTool(id);
            deleteExternalTool.prettyPrint();
        }

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        String pathToJupyterNotebok = "src/test/java/edu/harvard/iq/dataverse/util/irc-metrics.ipynb";
        Response uploadJupyterNotebook = UtilIT.uploadFileViaNative(datasetId.toString(), pathToJupyterNotebok, apiToken);
        uploadJupyterNotebook.prettyPrint();
        uploadJupyterNotebook.then().assertThat()
                .statusCode(OK.getStatusCode());

        Integer jupyterNotebookFileId = JsonPath.from(uploadJupyterNotebook.getBody().asString()).getInt("data.files[0].dataFile.id");

        String pathToTabularFile = "src/test/java/edu/harvard/iq/dataverse/util/irclog.tsv";
        Response uploadTabularFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToTabularFile, apiToken);
        uploadTabularFile.prettyPrint();
        uploadTabularFile.then().assertThat()
                .statusCode(OK.getStatusCode());

        Integer tabularFileId = JsonPath.from(uploadTabularFile.getBody().asString()).getInt("data.files[0].dataFile.id");

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("scope", "file");
        job.add("contentType", "text/tab-separated-values");
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

        long toolId = JsonPath.from(addExternalTool.getBody().asString()).getLong("data.id");

        Response getTool = UtilIT.getExternalTool(toolId);
        getTool.prettyPrint();
        getTool.then().assertThat()
                .body("data.scope", CoreMatchers.equalTo("file"))
                .statusCode(OK.getStatusCode());

        Response getExternalToolsForTabularFiles = UtilIT.getExternalToolsForFile(tabularFileId.toString(), "explore", apiToken);
        getExternalToolsForTabularFiles.prettyPrint();
        getExternalToolsForTabularFiles.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].displayName", CoreMatchers.equalTo("AwesomeTool"))
                .body("data[0].scope", CoreMatchers.equalTo("file"))
                .body("data[0].contentType", CoreMatchers.equalTo("text/tab-separated-values"))
                .body("data[0].toolUrlWithQueryParams", CoreMatchers.equalTo("http://awesometool.com?fileid=" + tabularFileId + "&key=" + apiToken));

        Response getExternalToolsForJuptyerNotebooks = UtilIT.getExternalToolsForFile(jupyterNotebookFileId.toString(), "explore", apiToken);
        getExternalToolsForJuptyerNotebooks.prettyPrint();
        getExternalToolsForJuptyerNotebooks.then().assertThat()
                .statusCode(OK.getStatusCode())
                // No tools for this file type.
                .body("data", Matchers.hasSize(0));
    }

    @Test
    public void testDatasetLevelTool1() {

        // Delete all external tools before testing.
        Response getTools = UtilIT.getExternalTools();
        getTools.prettyPrint();
        getTools.then().assertThat()
                .statusCode(OK.getStatusCode());
        String body = getTools.getBody().asString();
        JsonReader bodyObject = Json.createReader(new StringReader(body));
        JsonArray tools = bodyObject.readObject().getJsonArray("data");
        for (int i = 0; i < tools.size(); i++) {
            JsonObject tool = tools.getJsonObject(i);
            int id = tool.getInt("id");
            Response deleteExternalTool = UtilIT.deleteExternalTool(id);
            deleteExternalTool.prettyPrint();
        }

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

//        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        Integer datasetId = JsonPath.from(createDataset.getBody().asString()).getInt("data.id");
        String datasetPid = JsonPath.from(createDataset.getBody().asString()).getString("data.persistentId");

        String pathToFile = "src/test/java/edu/harvard/iq/dataverse/util/irclog.tsv";
        UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        Response getFileIdRequest = UtilIT.nativeGet(datasetId, apiToken);
        getFileIdRequest.prettyPrint();
        getFileIdRequest.then().assertThat()
                .statusCode(OK.getStatusCode());;

        int fileId = JsonPath.from(getFileIdRequest.getBody().asString()).getInt("data.latestVersion.files[0].dataFile.id");

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "DatasetTool1");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("scope", "dataset");
        job.add("toolUrl", "http://datasettool1.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("datasetPid", "{datasetPid}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .build())
                .build());
        Response addExternalTool = UtilIT.addExternalTool(job.build());
        addExternalTool.prettyPrint();
        addExternalTool.then().assertThat()
                .body("data.displayName", CoreMatchers.equalTo("DatasetTool1"))
                .statusCode(OK.getStatusCode());

        Response getExternalToolsByDatasetId = UtilIT.getExternalToolsForDataset(datasetId.toString(), "explore", apiToken);
        getExternalToolsByDatasetId.prettyPrint();
        getExternalToolsByDatasetId.then().assertThat()
                .body("data[0].displayName", CoreMatchers.equalTo("DatasetTool1"))
                .body("data[0].scope", CoreMatchers.equalTo("dataset"))
                .body("data[0].toolUrlWithQueryParams", CoreMatchers.equalTo("http://datasettool1.com?datasetPid=" + datasetPid + "&key=" + apiToken))
                .statusCode(OK.getStatusCode());

    }

    @Test
    public void testAddFilelToolNoFileId() throws IOException {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("scope", "file");
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
    public void testAddDatasetToolNoDatasetId() throws IOException {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("scope", "dataset");
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
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo("One of the following reserved words is required: {datasetId}, {datasetPid}."));
    }

    @Test
    public void testAddExternalToolNonReservedWord() throws IOException {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("scope", "file");
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
