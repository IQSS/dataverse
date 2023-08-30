package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        long toolId = JsonPath.from(addExternalTool.getBody().asString()).getLong("data.id");

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

        Response getExternalToolsByDatasetIdInvalidType = UtilIT.getExternalToolsForDataset(datasetId.toString(), "invalidType", apiToken);
        getExternalToolsByDatasetIdInvalidType.prettyPrint();
        getExternalToolsByDatasetIdInvalidType.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo("Type must be one of these values: [explore, configure, preview, query]."));

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
        for (int i = 0; i < tools.size(); i++) {
            JsonObject tool = tools.getJsonObject(i);
            int id = tool.getInt("id");
            Response deleteExternalTool = UtilIT.deleteExternalTool(id);
            deleteExternalTool.prettyPrint();
        }
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
    }

    // explore only
    @Disabled
    @Test
    public void createToolDataExplorer() {
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

        long toolId = JsonPath.from(addExternalTool.getBody().asString()).getLong("data.id");

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
        Response getToolsForTrueHdf5 = UtilIT.getExternalToolsForFile(trueHdf5.toString(), "preview", apiToken);
        getToolsForTrueHdf5.prettyPrint();
        getToolsForTrueHdf5.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].displayName", CoreMatchers.equalTo("HDF5 Tool"))
                .body("data[0].scope", CoreMatchers.equalTo("file"))
                .body("data[0].contentType", CoreMatchers.equalTo("application/x-hdf5"));
    }

}
