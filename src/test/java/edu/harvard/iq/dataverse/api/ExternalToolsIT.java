package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.OK;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;

public class ExternalToolsIT {

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testGetExternalTools() {
        Response getExternalTools = UtilIT.getExternalTools();
        getExternalTools.prettyPrint();
    }

    @Test
    public void testExternalToolsNonAdminEndpoint() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(username, true);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = JsonPath.from(createDataset.getBody().asString()).getInt("data.id");
        String datasetPid = JsonPath.from(createDataset.getBody().asString()).getString("data.persistentId");

        String toolManifest = """
{
   "displayName": "Dataset Configurator",
   "description": "Slices! Dices! <a href='https://docs.datasetconfigurator.com' target='_blank'>More info</a>.",
   "types": [
     "configure"
   ],
   "scope": "dataset",
   "toolUrl": "https://datasetconfigurator.com",
   "toolParameters": {
     "queryParameters": [
       {
         "datasetPid": "{datasetPid}"
       },
       {
         "localeCode": "{localeCode}"
       }
     ]
   }
 }
""";

        Response addExternalTool = UtilIT.addExternalTool(JsonUtil.getJsonObject(toolManifest), apiToken);
        addExternalTool.prettyPrint();
        addExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.displayName", CoreMatchers.equalTo("Dataset Configurator"));

        Long toolId = JsonPath.from(addExternalTool.getBody().asString()).getLong("data.id");
        Response getExternalToolsByDatasetId = UtilIT.getExternalToolForDatasetById(datasetId.toString(), "configure", apiToken, toolId.toString());
        getExternalToolsByDatasetId.prettyPrint();
        getExternalToolsByDatasetId.then().assertThat()
                .body("data.displayName", CoreMatchers.equalTo("Dataset Configurator"))
                .body("data.scope", CoreMatchers.equalTo("dataset"))
                .body("data.types[0]", CoreMatchers.equalTo("configure"))
                .body("data.toolUrlWithQueryParams", CoreMatchers.equalTo("https://datasetconfigurator.com?datasetPid=" + datasetPid))
                .statusCode(OK.getStatusCode());

        Response getExternalTools = UtilIT.getExternalTools(apiToken);
        getExternalTools.prettyPrint();
        getExternalTools.then().assertThat()
                .statusCode(OK.getStatusCode());
        Response getExternalTool = UtilIT.getExternalTool(toolId, apiToken);
        getExternalTool.prettyPrint();
        getExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode());

        // non superuser can only view tools
        UtilIT.setSuperuserStatus(username, false);
        getExternalTools = UtilIT.getExternalTools(apiToken);
        getExternalTools.then().assertThat()
                .statusCode(OK.getStatusCode());
        getExternalToolsByDatasetId = UtilIT.getExternalToolForDatasetById(datasetId.toString(), "configure", apiToken, toolId.toString());
        getExternalToolsByDatasetId.prettyPrint();
        getExternalToolsByDatasetId.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Add by non-superuser will fail
        addExternalTool = UtilIT.addExternalTool(JsonUtil.getJsonObject(toolManifest), apiToken);
        addExternalTool.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", CoreMatchers.equalTo("Superusers only."));

        //Delete by non-superuser will fail
        Response deleteExternalTool = UtilIT.deleteExternalTool(toolId, apiToken);
        deleteExternalTool.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", CoreMatchers.equalTo("Superusers only."));

        //Delete the tool added by this test...
        UtilIT.setSuperuserStatus(username, true);
        deleteExternalTool = UtilIT.deleteExternalTool(toolId, apiToken);
        deleteExternalTool.prettyPrint();
        deleteExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testFileLevelTool1() {

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

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToTabularFile);
        Integer tabularFileId = JsonPath.from(uploadTabularFile.getBody().asString()).getInt("data.files[0].dataFile.id");

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("explore"));
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
                .statusCode(OK.getStatusCode())
                .body("data.displayName", CoreMatchers.equalTo("AwesomeTool"));

        Long toolId = JsonPath.from(addExternalTool.getBody().asString()).getLong("data.id");

        Response getTool = UtilIT.getExternalTool(toolId);
        getTool.prettyPrint();
        getTool.then().assertThat()
                .body("data.scope", CoreMatchers.equalTo("file"))
                .statusCode(OK.getStatusCode());

        Response getExternalToolsForFileInvalidType = UtilIT.getExternalToolsForFile(tabularFileId.toString(), "invalidType", apiToken);
        getExternalToolsForFileInvalidType.prettyPrint();
        getExternalToolsForFileInvalidType.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo("Type must be one of these values: [explore, configure, preview, query]."));

       // Getting tool by tool Id to avoid issue where there are existing tools
        String toolIdString = toolId.toString();
        Response getExternalToolsForTabularFiles = UtilIT.getExternalToolForFileById(tabularFileId.toString(), "explore", apiToken, toolIdString);
        getExternalToolsForTabularFiles.prettyPrint();
        
        getExternalToolsForTabularFiles.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.displayName", CoreMatchers.equalTo("AwesomeTool"))
                .body("data.scope", CoreMatchers.equalTo("file"))
                .body("data.contentType", CoreMatchers.equalTo("text/tab-separated-values"))
                .body("data.toolUrlWithQueryParams", CoreMatchers.equalTo("http://awesometool.com?fileid=" + tabularFileId + "&key=" + apiToken));

        Response getExternalToolsForJuptyerNotebooks = UtilIT.getExternalToolsForFile(jupyterNotebookFileId.toString(), "explore", apiToken);
        getExternalToolsForJuptyerNotebooks.prettyPrint();
        getExternalToolsForJuptyerNotebooks.then().assertThat()
                .statusCode(OK.getStatusCode())
                // No tools for this file type.
                .body("data", Matchers.hasSize(0));
        
        //Delete the tool added by this test...
        Response deleteExternalTool = UtilIT.deleteExternalTool(toolId);
        deleteExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testDatasetLevelTool1() {

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
        job.add("types", Json.createArrayBuilder().add("explore"));
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
                .statusCode(OK.getStatusCode())
                .body("data.displayName", CoreMatchers.equalTo("DatasetTool1"));
        
        Long toolId = JsonPath.from(addExternalTool.getBody().asString()).getLong("data.id");

        Response getExternalToolsByDatasetIdInvalidType = UtilIT.getExternalToolsForDataset(datasetId.toString(), "invalidType", apiToken);
        getExternalToolsByDatasetIdInvalidType.prettyPrint();
        getExternalToolsByDatasetIdInvalidType.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo("Type must be one of these values: [explore, configure, preview, query]."));

        Response getExternalToolsByDatasetId = UtilIT.getExternalToolForDatasetById(datasetId.toString(), "explore", apiToken, toolId.toString());
        getExternalToolsByDatasetId.prettyPrint();
        getExternalToolsByDatasetId.then().assertThat()
                .body("data.displayName", CoreMatchers.equalTo("DatasetTool1"))
                .body("data.scope", CoreMatchers.equalTo("dataset"))
                .body("data.toolUrlWithQueryParams", CoreMatchers.equalTo("http://datasettool1.com?datasetPid=" + datasetPid + "&key=" + apiToken))
                .statusCode(OK.getStatusCode());
        
        //Delete the tool added by this test...
        Response deleteExternalTool = UtilIT.deleteExternalTool(toolId);
        deleteExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testDatasetLevelToolConfigure() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
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

        Integer datasetId = JsonPath.from(createDataset.getBody().asString()).getInt("data.id");
        String datasetPid = JsonPath.from(createDataset.getBody().asString()).getString("data.persistentId");

        String toolManifest = """
{
   "displayName": "Dataset Configurator",
   "description": "Slices! Dices! <a href='https://docs.datasetconfigurator.com' target='_blank'>More info</a>.",
   "types": [
     "configure"
   ],
   "scope": "dataset",
   "toolUrl": "https://datasetconfigurator.com",
   "toolParameters": {
     "queryParameters": [
       {
         "datasetPid": "{datasetPid}"
       },
       {
         "localeCode": "{localeCode}"
       }
     ]
   }
 }
""";

        Response addExternalTool = UtilIT.addExternalTool(JsonUtil.getJsonObject(toolManifest));
        addExternalTool.prettyPrint();
        addExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.displayName", CoreMatchers.equalTo("Dataset Configurator"));
        
        Long toolId = JsonPath.from(addExternalTool.getBody().asString()).getLong("data.id");
        Response getExternalToolsByDatasetId = UtilIT.getExternalToolForDatasetById(datasetId.toString(), "configure", apiToken, toolId.toString());
        getExternalToolsByDatasetId.prettyPrint();
        getExternalToolsByDatasetId.then().assertThat()
                .body("data.displayName", CoreMatchers.equalTo("Dataset Configurator"))
                .body("data.scope", CoreMatchers.equalTo("dataset"))
                .body("data.types[0]", CoreMatchers.equalTo("configure"))
                .body("data.toolUrlWithQueryParams", CoreMatchers.equalTo("https://datasetconfigurator.com?datasetPid=" + datasetPid))
                .statusCode(OK.getStatusCode());
        
        //Delete the tool added by this test...
        Response deleteExternalTool = UtilIT.deleteExternalTool(toolId);
        deleteExternalTool.then().assertThat()
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
        job.add("contentType", "application/pdf");
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
                .body("message", CoreMatchers.equalTo("One of the following reserved words is required: {fileId}, {filePid}."))
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
        job.add("contentType", "application/pdf");
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

    @Disabled
    @Test
    public void deleteTools() {

        // Delete all external tools before testing.
        Response getTools = UtilIT.getExternalTools();
        getTools.prettyPrint();
        getTools.then().assertThat()
                .statusCode(OK.getStatusCode());
        String body = getTools.getBody().asString();
        JsonReader bodyObject = Json.createReader(new StringReader(body));
        JsonArray tools = bodyObject.readObject().getJsonArray("data");
        /*
        for (int i = 0; i < tools.size(); i++) {
            JsonObject tool = tools.getJsonObject(i);
            int id = tool.getInt("id");
            Response deleteExternalTool = UtilIT.deleteExternalTool(id);
            deleteExternalTool.prettyPrint();
        }*/
    }

    // preview only
    @Disabled
    @Test
    public void createToolShellScript() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "View Code");
        job.add("description", "");
        job.add("types", Json.createArrayBuilder().add("preview"));
        job.add("scope", "file");
        job.add("hasPreviewMode", "true");
        job.add("contentType", "application/x-sh");
        job.add("toolUrl", "http://localhost:8000/dataverse-previewers/previewers/TextPreview.html");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("fileid", "{fileId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("siteUrl", "{siteUrl}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("datasetid", "{datasetId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("datasetversion", "{datasetVersion}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("locale", "{localeCode}")
                                .build())
                        .build())
                .build());
        Response addExternalTool = UtilIT.addExternalTool(job.build());
        addExternalTool.prettyPrint();
        addExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        long toolId = JsonPath.from(addExternalTool.getBody().asString()).getLong("data.id");
        
        //Delete the tool added by this test...
        Response deleteExternalTool = UtilIT.deleteExternalTool(toolId);
        deleteExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    // explore only
    @Disabled
    @Test
    public void createToolDataExplorer() {
    /*    
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "Data Explorer");
        job.add("description", "");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "file");
        job.add("contentType", "text/tab-separated-values");
        job.add("toolUrl", "https://scholarsportal.github.io/Dataverse-Data-Explorer-v2/");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("fileId", "{fileId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("siteUrl", "{siteUrl}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("dvLocale", "{localeCode}")
                                .build())
                        .build())
                .build());
        Response addExternalTool = UtilIT.addExternalTool(job.build());
        addExternalTool.prettyPrint();
        addExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        long toolId = JsonPath.from(addExternalTool.getBody().asString()).getLong("data.id");
        
        //Delete the tool added by this test...
        Response deleteExternalTool = UtilIT.deleteExternalTool(toolId);
        deleteExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode());
        */
    }

    // both preview and explore
    @Disabled
    @Test
    public void createToolSpreadsheetViewer() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "View Data");
        job.add("description", "");
        job.add("types", Json.createArrayBuilder()
                .add("preview")
                .add("explore")
        );
        job.add("scope", "file");
        job.add("hasPreviewMode", "true");
        job.add("contentType", "text/tab-separated-values");
        job.add("toolUrl", "http://localhost:8000/dataverse-previewers/previewers/SpreadsheetPreview.html");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("fileid", "{fileId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("siteUrl", "{siteUrl}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("datasetid", "{datasetId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("datasetversion", "{datasetVersion}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("locale", "{localeCode}")
                                .build())
                        .build())
                .build());
        Response addExternalTool = UtilIT.addExternalTool(job.build());
        addExternalTool.prettyPrint();
        addExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testFileLevelToolWithAuxFileReq() throws IOException {

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

        // Not really an HDF5 file. Just random bytes. But the file extension makes it detected as HDF5.
        Path pathToFalseHdf5 = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "false.hdf5");
        byte[] bytes = {1, 2, 3, 4, 5};
        java.nio.file.Files.write(pathToFalseHdf5, bytes);

        Response uploadFalseHdf5 = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFalseHdf5.toString(), apiToken);
        uploadFalseHdf5.prettyPrint();
        uploadFalseHdf5.then().assertThat()
                .statusCode(OK.getStatusCode());

        Integer falseHdf5 = JsonPath.from(uploadFalseHdf5.getBody().asString()).getInt("data.files[0].dataFile.id");

        String pathToTrueHdf5 = "src/test/resources/hdf/hdf5/vlen_string_dset";
        Response uploadTrueHdf5 = UtilIT.uploadFileViaNative(datasetId.toString(), pathToTrueHdf5, apiToken);
        uploadTrueHdf5.prettyPrint();
        uploadTrueHdf5.then().assertThat()
                .statusCode(OK.getStatusCode());

        Integer trueHdf5 = JsonPath.from(uploadTrueHdf5.getBody().asString()).getInt("data.files[0].dataFile.id");

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "HDF5 Tool");
        job.add("description", "Operates on HDF5 files");
        job.add("types", Json.createArrayBuilder().add("preview"));
        job.add("scope", "file");
        job.add("contentType", "application/x-hdf5");
        job.add("toolUrl", "/dataexplore/dataverse-previewers/previewers/v1.3/TextPreview.html");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("fileid", "{fileId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("siteUrl", "{siteUrl}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .build())
                .build());
        job.add("requirements", Json.createObjectBuilder()
                .add("auxFilesExist", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("formatTag", "NcML")
                                .add("formatVersion", "0.1")
                        )
                )
        );
        Response addExternalTool = UtilIT.addExternalTool(job.build());
        addExternalTool.prettyPrint();
        addExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.displayName", CoreMatchers.equalTo("HDF5 Tool"));

        Long toolId = JsonPath.from(addExternalTool.getBody().asString()).getLong("data.id");

        Response getTool = UtilIT.getExternalTool(toolId);
        getTool.prettyPrint();
        getTool.then().assertThat()
                .body("data.scope", CoreMatchers.equalTo("file"))
                .statusCode(OK.getStatusCode());

        // No tools for false HDF5 file. Aux file couldn't be extracted. Doesn't meet requirements.
        Response getToolsForFalseHdf5 = UtilIT.getExternalToolsForFile(falseHdf5.toString(), "preview", apiToken);
        getToolsForFalseHdf5.prettyPrint();
        getToolsForFalseHdf5.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data", Matchers.hasSize(0));

        // The tool shows for a true HDF5 file. The NcML aux file is available. Requirements met.
        Response getToolsForTrueHdf5 = UtilIT.getExternalToolForFileById(trueHdf5.toString(), "preview", apiToken, toolId.toString());
        getToolsForTrueHdf5.prettyPrint();
        getToolsForTrueHdf5.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.displayName", CoreMatchers.equalTo("HDF5 Tool"))
                .body("data.scope", CoreMatchers.equalTo("file"))
                .body("data.contentType", CoreMatchers.equalTo("application/x-hdf5"));
        
        //Delete the tool added by this test...
        Response deleteExternalTool = UtilIT.deleteExternalTool(toolId);
        deleteExternalTool.then().assertThat()
                .statusCode(OK.getStatusCode());
        
    }
    

@Test
public void testExternalToolUrlApi() {
    // Create a user
    Response createUser = UtilIT.createRandomUser();
    createUser.prettyPrint();
    createUser.then().assertThat()
            .statusCode(OK.getStatusCode());
    String username = UtilIT.getUsernameFromResponse(createUser);
    String apiToken = UtilIT.getApiTokenFromResponse(createUser);

    // Create a dataverse
    Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
    createDataverseResponse.prettyPrint();
    createDataverseResponse.then().assertThat()
            .statusCode(CREATED.getStatusCode());
    String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

    // Create a dataset
    Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
    createDataset.prettyPrint();
    createDataset.then().assertThat()
            .statusCode(CREATED.getStatusCode());
    Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
    String datasetPid = JsonPath.from(createDataset.getBody().asString()).getString("data.persistentId");

    // Upload a text file
    String pathToTextFile = "src/test/java/edu/harvard/iq/dataverse/util/testing-readme.txt";
    Response uploadTextFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToTextFile, apiToken);
    uploadTextFile.prettyPrint();
    uploadTextFile.then().assertThat()
            .statusCode(OK.getStatusCode());
    Integer textFileId = JsonPath.from(uploadTextFile.getBody().asString()).getInt("data.files[0].dataFile.id");
    
    // Create a dataset-level tool
    JsonObjectBuilder datasetToolJob = Json.createObjectBuilder();
    datasetToolJob.add("displayName", "Dataset API Tool");
    datasetToolJob.add("description", "Tests the dataset-level tool URL API");
    datasetToolJob.add("types", Json.createArrayBuilder().add("explore"));
    datasetToolJob.add("scope", "dataset");
    datasetToolJob.add("toolUrl", "http://example.org/dataset-tool");
    datasetToolJob.add("toolParameters", Json.createObjectBuilder()
            .add("queryParameters", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("datasetId", "{datasetId}")
                            .build())
                    .add(Json.createObjectBuilder()
                            .add("key", "{apiToken}")
                            .build())
                    .build())
            .build());
    datasetToolJob.add("allowedApiCalls", Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                    .add("name", "retrieveDatasetJson")
                    .add("httpMethod", "GET")
                    .add("urlTemplate", "/api/v1/datasets/{datasetId}")
                    .add("timeOut", 10)
                    .build())
            .build());
    
    Response addDatasetTool = UtilIT.addExternalTool(datasetToolJob.build());
    addDatasetTool.prettyPrint();
    addDatasetTool.then().assertThat()
            .statusCode(OK.getStatusCode())
            .body("data.displayName", CoreMatchers.equalTo("Dataset API Tool"));
    Long datasetToolId = JsonPath.from(addDatasetTool.getBody().asString()).getLong("data.id");
    
    // Create a file-level tool for text/plain
    JsonObjectBuilder fileToolJob = Json.createObjectBuilder();
    fileToolJob.add("displayName", "Text File Tool");
    fileToolJob.add("description", "Tests the file-level tool URL API");
    fileToolJob.add("types", Json.createArrayBuilder().add("explore"));
    fileToolJob.add("scope", "file");
    fileToolJob.add("contentType", "text/plain");
    fileToolJob.add("toolUrl", "http://example.org/text-tool");
    fileToolJob.add("toolParameters", Json.createObjectBuilder()
            .add("queryParameters", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("fileId", "{fileId}")
                            .build())
                    .add(Json.createObjectBuilder()
                            .add("key", "{apiToken}")
                            .build())
                    .build())
            .build());
    fileToolJob.add("allowedApiCalls", Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                    .add("name", "retrieveFileContents")
                    .add("httpMethod", "GET")
                    .add("urlTemplate", "/api/v1/access/datafile/{fileId}?gbrecs=true")
                    .add("timeOut", 3600)
                    .build())
            .build());
    
    Response addFileTool = UtilIT.addExternalTool(fileToolJob.build());
    addFileTool.prettyPrint();
    addFileTool.then().assertThat()
            .statusCode(OK.getStatusCode())
            .body("data.displayName", CoreMatchers.equalTo("Text File Tool"));
    Long fileToolId = JsonPath.from(addFileTool.getBody().asString()).getLong("data.id");
    
    // Test the dataset tool URL API
    Response datasetToolUrl = UtilIT.getDatasetToolUrl(datasetId.toString(), datasetToolId.toString(), apiToken, null);
    datasetToolUrl.prettyPrint();
    datasetToolUrl.then().assertThat()
            .statusCode(OK.getStatusCode())
            .body("status", CoreMatchers.equalTo("OK"))
            .body("data.toolUrl", CoreMatchers.startsWith("http://example.org/dataset-tool?datasetId=" + datasetId))
            .body("data.toolName", CoreMatchers.equalTo("Dataset API Tool"));
    
 // Extract the callback parameter
    String toolUrl = JsonPath.from(datasetToolUrl.getBody().asString()).getString("data.toolUrl");
    String callbackParam = toolUrl.substring(toolUrl.indexOf("callback=") + 9);
    if (callbackParam.contains("&")) {
        callbackParam = callbackParam.substring(0, callbackParam.indexOf("&"));
    }

    // Decode the callback URL
    byte[] decodedBytes = Base64.getDecoder().decode(callbackParam);
    String decodedCallback = new String(decodedBytes, StandardCharsets.UTF_8);
    System.out.println("Decoded callback URL: " + decodedCallback);

    // Verify the callback URL contains the dataset API endpoint
    assertTrue(decodedCallback.contains("toolparams/" + datasetToolId), 
            "Callback URL should contain a call to a/api/datasets/{id}/versions/{versionId}/toolparams/{toolId}");

    // Actually call the callback URL and verify the response
    Response callbackResponse = UtilIT.callCallbackUrl(decodedCallback);
    callbackResponse.prettyPrint();
    callbackResponse.then().assertThat().statusCode(OK.getStatusCode()).body("status", CoreMatchers.equalTo("OK"));

    // Verify the response contains the dataset API endpoint
    String callbackResponseBody = callbackResponse.getBody().asString();
    assertTrue(callbackResponseBody.contains("/api/v1/datasets/" + datasetId),
            "Callback response should contain the dataset API endpoint");

    // Verify the response contains the allowed API calls
    assertTrue(callbackResponseBody.contains("retrieveDatasetJson"),
            "Callback response should contain the allowed API call name");

    // Test the file tool URL API
    Response fileToolUrl = UtilIT.getFileToolUrl(textFileId.toString(), fileToolId.toString(), apiToken, null);
    fileToolUrl.prettyPrint();
    fileToolUrl.then().assertThat()
            .statusCode(OK.getStatusCode())
            .body("status", CoreMatchers.equalTo("OK"))
            .body("data.toolUrl", CoreMatchers.startsWith("http://example.org/text-tool?fileId=" + textFileId))
            .body("data.toolName", CoreMatchers.equalTo("Text File Tool"));
    
 // Extract the callback parameter from file tool URL
    String fileToolUrlString = JsonPath.from(fileToolUrl.getBody().asString()).getString("data.toolUrl");
    String fileCallbackParam = fileToolUrlString.substring(fileToolUrlString.indexOf("callback=") + 9);
    if (fileCallbackParam.contains("&")) {
        fileCallbackParam = fileCallbackParam.substring(0, fileCallbackParam.indexOf("&"));
    }

    // Decode the file tool callback URL
    byte[] fileDecodedBytes = Base64.getDecoder().decode(fileCallbackParam);
    String fileDecodedCallback = new String(fileDecodedBytes, StandardCharsets.UTF_8);
    System.out.println("Decoded file callback URL: " + fileDecodedCallback);

    // Verify the file callback URL contains the file API endpoint
    assertTrue(fileDecodedCallback.contains("toolparams/" + fileToolId), 
            "File callback URL should contain a call to api/files/{id}/toolparams/{toolId}");

    // Actually call the file callback URL and verify the response
    Response fileCallbackResponse = UtilIT.callCallbackUrl(fileDecodedCallback);
    fileCallbackResponse.prettyPrint();
    fileCallbackResponse.then().assertThat()
            .statusCode(OK.getStatusCode())
            .body("status", CoreMatchers.equalTo("OK"));

    // Verify the response contains the file API endpoint
    String fileCallbackResponseBody = fileCallbackResponse.getBody().asString();
    assertTrue(fileCallbackResponseBody.contains("/api/v1/access/datafile/" + textFileId),
            "File callback response should contain the file API endpoint");

    // Verify the response contains the allowed API calls
    assertTrue(fileCallbackResponseBody.contains("retrieveFileContents"),
            "File callback response should contain the allowed API call name");
    assertTrue(fileCallbackResponseBody.contains("gbrecs=true"),
            "File callback response should contain the query parameter");
    
    // Test with preview mode
    JsonObjectBuilder previewParams = Json.createObjectBuilder()
            .add("preview", true)
            .add("locale", "fr");
    
    Response fileToolUrlWithPreview = UtilIT.getFileToolUrl(
            textFileId.toString(), fileToolId.toString(), apiToken, previewParams.build());
    fileToolUrlWithPreview.prettyPrint();
    fileToolUrlWithPreview.then().assertThat()
            .statusCode(OK.getStatusCode())
            .body("status", CoreMatchers.equalTo("OK"))
            .body("data.preview", CoreMatchers.equalTo(true));
    
    // Clean up - delete the tools
    Response deleteDatasetTool = UtilIT.deleteExternalTool(datasetToolId);
    deleteDatasetTool.then().assertThat()
            .statusCode(OK.getStatusCode());
    
    Response deleteFileTool = UtilIT.deleteExternalTool(fileToolId);
    deleteFileTool.then().assertThat()
            .statusCode(OK.getStatusCode());
    
    // Clean up - delete dataset, dataverse, and user
    try {
        // Delete dataset
        Response deleteDataset = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDataset.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        // Delete dataverse
        Response deleteDataverse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        // Delete user
        Response deleteUser = UtilIT.deleteUser(username);
        deleteUser.then().assertThat()
                .statusCode(OK.getStatusCode());
    } catch (Exception e) {
        System.out.println("Error during cleanup: " + e.getMessage());
        e.printStackTrace();
        fail("Cleanup failed: " + e.getMessage());
    }
    
}

}
