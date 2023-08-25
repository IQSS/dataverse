package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Make assertions about duplicate file names (and maybe in the future,
 * duplicate MD5s).
 */
public class DuplicateFilesIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void uploadTwoFilesWithSameNameSameDirectory() throws IOException {

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

        Path pathtoReadme1 = Paths.get(Files.createTempDirectory(null) + File.separator + "README.md");
        Files.write(pathtoReadme1, "File 1".getBytes());
        System.out.println("README: " + pathtoReadme1);

        Response uploadReadme1 = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme1.toString(), apiToken);
        uploadReadme1.prettyPrint();
        uploadReadme1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("README.md"));

        Path pathtoReadme2 = Paths.get(Files.createTempDirectory(null) + File.separator + "README.md");
        Files.write(pathtoReadme2, "File 2".getBytes());
        System.out.println("README: " + pathtoReadme2);

        Response uploadReadme2 = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme2.toString(), apiToken);
        uploadReadme2.prettyPrint();
        uploadReadme2.then().assertThat()
                .statusCode(OK.getStatusCode())
                // Note that "README.md" was changed to "README-1.md". This is a feature. It's what the GUI does.
                .body("data.files[0].label", equalTo("README-1.md"));

    }

    @Test
    public void uploadTwoFilesWithSameNameDifferentDirectories() throws IOException {

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

        Path pathtoReadme1 = Paths.get(Files.createTempDirectory(null) + File.separator + "README.md");
        Files.write(pathtoReadme1, "File 1".getBytes());
        System.out.println("README: " + pathtoReadme1);

        JsonObjectBuilder json1 = Json.createObjectBuilder()
                .add("description", "Description of the whole project.");

        Response uploadReadme1 = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme1.toString(), json1.build(), apiToken);
        uploadReadme1.prettyPrint();
        uploadReadme1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("README.md"));

        Path pathtoReadme2 = Paths.get(Files.createTempDirectory(null) + File.separator + "README.md");
        Files.write(pathtoReadme2, "File 2".getBytes());
        System.out.println("README: " + pathtoReadme2);

        JsonObjectBuilder json2 = Json.createObjectBuilder()
                .add("description", "Docs for the code.")
                .add("directoryLabel", "code");

        Response uploadReadme2 = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme2.toString(), json2.build(), apiToken);
        uploadReadme2.prettyPrint();
        uploadReadme2.then().assertThat()
                .statusCode(OK.getStatusCode())
                // The file name is still "README.md" (wasn't renamed)
                // because the previous file was in a different directory.
                .body("data.files[0].label", equalTo("README.md"));

    }

    @Test
    public void renameFileToSameName() throws IOException {

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

        Path pathtoReadme1 = Paths.get(Files.createTempDirectory(null) + File.separator + "README.md");
        Files.write(pathtoReadme1, "File 1".getBytes());
        System.out.println("README: " + pathtoReadme1);

        Response uploadReadme1 = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme1.toString(), apiToken);
        uploadReadme1.prettyPrint();
        uploadReadme1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("README.md"));

        Integer idOfReadme1 = JsonPath.from(uploadReadme1.getBody().asString()).getInt("data.files[0].dataFile.id");
        System.out.println("id: " + idOfReadme1);

        Path pathtoReadme2 = Paths.get(Files.createTempDirectory(null) + File.separator + "README2.md");
        Files.write(pathtoReadme2, "File 2".getBytes());
        System.out.println("README: " + pathtoReadme2);

        Response uploadReadme2 = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme2.toString(), apiToken);
        uploadReadme2.prettyPrint();
        uploadReadme2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("README2.md"));

        Long idOfReadme2 = JsonPath.from(uploadReadme2.getBody().asString()).getLong("data.files[0].dataFile.id");
        System.out.println("id: " + idOfReadme2);

        JsonObjectBuilder renameFile = Json.createObjectBuilder()
                .add("label", "README.md");
        Response renameFileResponse = UtilIT.updateFileMetadata(String.valueOf(idOfReadme2), renameFile.build().toString(), apiToken);
        renameFileResponse.prettyPrint();
        renameFileResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Filename already exists at README.md"));

        // This "registerParser" is to avoid this error: Expected response body to be verified as JSON, HTML or XML but content-type 'text/plain' is not supported out of the box.
        RestAssured.registerParser("text/plain", Parser.JSON);
        Response getMetadataResponse = UtilIT.getDataFileMetadataDraft(idOfReadme2, apiToken);
        getMetadataResponse.prettyPrint();
        getMetadataResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                // The rename was rejected. It's still README2.md.
                .body("label", equalTo("README2.md"));
    }

    @Test
    public void moveFileToDirectoryContainingSameFileName() throws IOException {

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

        Path pathtoReadme1 = Paths.get(Files.createTempDirectory(null) + File.separator + "README.md");
        Files.write(pathtoReadme1, "File 1".getBytes());
        System.out.println("README: " + pathtoReadme1);

        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", "Docs for the code.")
                .add("directoryLabel", "code");

        Response uploadReadme1 = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme1.toString(), json.build(), apiToken);
        uploadReadme1.prettyPrint();
        uploadReadme1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("README.md"));

        Integer idOfReadme1 = JsonPath.from(uploadReadme1.getBody().asString()).getInt("data.files[0].dataFile.id");
        System.out.println("id: " + idOfReadme1);

        Path pathtoReadme2 = Paths.get(Files.createTempDirectory(null) + File.separator + "README.md");
        Files.write(pathtoReadme2, "File 2".getBytes());
        System.out.println("README: " + pathtoReadme2);

        Response uploadReadme2 = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme2.toString(), apiToken);
        uploadReadme2.prettyPrint();
        uploadReadme2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("README.md"));

        Long idOfReadme2 = JsonPath.from(uploadReadme2.getBody().asString()).getLong("data.files[0].dataFile.id");
        System.out.println("id: " + idOfReadme2);

        JsonObjectBuilder moveFile = Json.createObjectBuilder()
                .add("directoryLabel", "code");
        Response moveFileResponse = UtilIT.updateFileMetadata(String.valueOf(idOfReadme2), moveFile.build().toString(), apiToken);
        moveFileResponse.prettyPrint();
        moveFileResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Filename already exists at code/README.md"));

    }

    /**
     * In this test we make sure that other changes in the absence of label and
     * directoryLabel go through, such as changing a file description.
     */
    @Test
    public void modifyFileDescription() throws IOException {

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

        Path pathtoReadme1 = Paths.get(Files.createTempDirectory(null) + File.separator + "README.md");
        Files.write(pathtoReadme1, "File 1".getBytes());
        System.out.println("README: " + pathtoReadme1);

        Response uploadReadme1 = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme1.toString(), apiToken);
        uploadReadme1.prettyPrint();
        uploadReadme1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("README.md"));

        Integer idOfReadme1 = JsonPath.from(uploadReadme1.getBody().asString()).getInt("data.files[0].dataFile.id");
        System.out.println("id: " + idOfReadme1);

        JsonObjectBuilder updateFileMetadata = Json.createObjectBuilder()
                .add("description", "This file is awesome.");
        Response updateFileMetadataResponse = UtilIT.updateFileMetadata(String.valueOf(idOfReadme1), updateFileMetadata.build().toString(), apiToken);
        updateFileMetadataResponse.prettyPrint();
        updateFileMetadataResponse.then().statusCode(OK.getStatusCode());

    }

    /**
     * In this test we make sure that that if you keep the label the same, you
     * are still able to change other metadata such as file description.
     */
    @Test
    public void modifyFileDescriptionSameLabel() throws IOException {

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

        Path pathtoReadme1 = Paths.get(Files.createTempDirectory(null) + File.separator + "README.md");
        Files.write(pathtoReadme1, "File 1".getBytes());
        System.out.println("README: " + pathtoReadme1);

        JsonObjectBuilder json1 = Json.createObjectBuilder()
                .add("directoryLabel", "code");

        Response uploadReadme1 = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme1.toString(), json1.build(), apiToken);
        uploadReadme1.prettyPrint();
        uploadReadme1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("README.md"));

        Integer idOfReadme1 = JsonPath.from(uploadReadme1.getBody().asString()).getInt("data.files[0].dataFile.id");
        System.out.println("id: " + idOfReadme1);

        JsonObjectBuilder updateFileMetadata = Json.createObjectBuilder()
                .add("label", "README.md")
                .add("directoryLabel", "code")
                .add("description", "This file is awesome.");
        Response updateFileMetadataResponse = UtilIT.updateFileMetadata(String.valueOf(idOfReadme1), updateFileMetadata.build().toString(), apiToken);
        updateFileMetadataResponse.prettyPrint();
        updateFileMetadataResponse.then().statusCode(OK.getStatusCode());

    }

    /**
     * What if the directory for the file exists and you pass the filename
     * (label) while changing the description? This should be allowed.
     */
    @Test
    public void existingDirectoryPassLabelChangeDescription() throws IOException {

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

        Path pathToFile = Paths.get(Files.createTempDirectory(null) + File.separator + "label");
        Files.write(pathToFile, "File 1".getBytes());
        System.out.println("file: " + pathToFile);

        JsonObjectBuilder json1 = Json.createObjectBuilder()
                .add("directory", "code");

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile.toString(), json1.build(), apiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("label"));

        Integer idOfFile = JsonPath.from(uploadFile.getBody().asString()).getInt("data.files[0].dataFile.id");
        System.out.println("id: " + idOfFile);

        JsonObjectBuilder updateFileMetadata = Json.createObjectBuilder()
                .add("label", "label")
                .add("description", "This file is awesome.");
        Response updateFileMetadataResponse = UtilIT.updateFileMetadata(String.valueOf(idOfFile), updateFileMetadata.build().toString(), apiToken);
        updateFileMetadataResponse.prettyPrint();
        updateFileMetadataResponse.then().statusCode(OK.getStatusCode());

    }

    /**
     * This test is for the following scenario.
     *
     * What if the database has null for the directoryLabel? What if you pass in
     * directory as “” (because you don’t realize you can just not pass it).
     * when it check and compares, the old directory is null. so will that mean
     * labelChange = true and it will fail even though you didn’t really change
     * the directory?
     *
     * While "labelChange" does end up being true,
     * IngestUtil.conflictsWithExistingFilenames returns false, so the change is
     * allowed to go through. The description is allowed to be changed and the
     * directoryLabel remains null even though the user passed in an empty
     * string, which is what we want.
     */
    @Test
    public void existingDirectoryNullPassEmptyStringChangeDescription() throws IOException {

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

        Path pathToFile = Paths.get(Files.createTempDirectory(null) + File.separator + "file1.txt");
        Files.write(pathToFile, "File 1".getBytes());
        System.out.println("file: " + pathToFile);

        JsonObjectBuilder json1 = Json.createObjectBuilder()
                .add("description", "This is my file.");

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile.toString(), json1.build(), apiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("file1.txt"));

        Integer idOfFile = JsonPath.from(uploadFile.getBody().asString()).getInt("data.files[0].dataFile.id");
        System.out.println("id: " + idOfFile);

        JsonObjectBuilder updateFileMetadata = Json.createObjectBuilder()
                // It doesn't make sense to pass "" as a directoryLabel.
                .add("directoryLabel", "")
                .add("description", "This file is awesome.");
        Response updateFileMetadataResponse = UtilIT.updateFileMetadata(String.valueOf(idOfFile), updateFileMetadata.build().toString(), apiToken);
        updateFileMetadataResponse.prettyPrint();
        updateFileMetadataResponse.then().statusCode(OK.getStatusCode());

        Response datasetJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetJson.prettyPrint();
        datasetJson.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.latestVersion.files[0].label", equalTo("file1.txt"))
                .body("data.latestVersion.files[0].description", equalTo("This file is awesome."))
                // Even though we tried to set directoryValue to "" above, it's correctly null in the database.
                .body("data.latestVersion.files[0].directoryLabel", nullValue());
    }

}
