package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.json.Json;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class NetcdfIT {

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testNmclFromNetcdf() throws IOException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);

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
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        String pathToFile = "src/test/resources/netcdf/madis-raob";

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat().statusCode(OK.getStatusCode());

        long fileId = JsonPath.from(uploadFile.body().asString()).getLong("data.files[0].dataFile.id");
        String tag = "NcML";
        String version = "0.1";

        Response downloadNcml = UtilIT.downloadAuxFile(fileId, tag, version, apiToken);
        //downloadNcml.prettyPrint(); // long output
        downloadNcml.then().assertThat()
                .statusCode(OK.getStatusCode())
                .contentType("text/xml; name=\"madis-raob.ncml_0.1.xml\";charset=UTF-8");

        Response deleteNcml = UtilIT.deleteAuxFile(fileId, tag, version, apiToken);
        deleteNcml.prettyPrint();
        deleteNcml.then().assertThat().statusCode(OK.getStatusCode());

        Response downloadNcmlShouldFail = UtilIT.downloadAuxFile(fileId, tag, version, apiToken);
        downloadNcmlShouldFail.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response extractNcml = UtilIT.extractNcml(fileId, apiToken);
        extractNcml.prettyPrint();
        extractNcml.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response downloadNcmlShouldWork = UtilIT.downloadAuxFile(fileId, tag, version, apiToken);
        downloadNcmlShouldWork.then().assertThat()
                .statusCode(OK.getStatusCode());

    }

    @Test
    public void testNmclFromNetcdfErrorChecking() throws IOException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);

        Response createUserRandom = UtilIT.createRandomUser();
        createUserRandom.then().assertThat().statusCode(OK.getStatusCode());
        String apiTokenRandom = UtilIT.getApiTokenFromResponse(createUserRandom);

        String apiTokenNull = null;

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
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        String pathToFile = "src/test/resources/netcdf/madis-raob";

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat().statusCode(OK.getStatusCode());

        long fileId = JsonPath.from(uploadFile.body().asString()).getLong("data.files[0].dataFile.id");
        String tag = "NcML";
        String version = "0.1";

        Response downloadNcmlFail = UtilIT.downloadAuxFile(fileId, tag, version, apiTokenNull);
        downloadNcmlFail.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());

        Response downloadNcml = UtilIT.downloadAuxFile(fileId, tag, version, apiToken);
        downloadNcml.then().assertThat()
                .statusCode(OK.getStatusCode())
                .contentType("text/xml; name=\"madis-raob.ncml_0.1.xml\";charset=UTF-8");

        Response deleteNcml = UtilIT.deleteAuxFile(fileId, tag, version, apiToken);
        deleteNcml.prettyPrint();
        deleteNcml.then().assertThat().statusCode(OK.getStatusCode());

        Response downloadNcmlShouldFail = UtilIT.downloadAuxFile(fileId, tag, version, apiToken);
        downloadNcmlShouldFail.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());

        Response extractNcmlFailRandomUser = UtilIT.extractNcml(fileId, apiTokenRandom);
        extractNcmlFailRandomUser.prettyPrint();
        extractNcmlFailRandomUser.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response extractNcml = UtilIT.extractNcml(fileId, apiToken);
        extractNcml.prettyPrint();
        extractNcml.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.result", CoreMatchers.equalTo(true));

        Response downloadNcmlShouldWork = UtilIT.downloadAuxFile(fileId, tag, version, apiToken);
        downloadNcmlShouldWork.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response extractNcmlFailExistsAlready = UtilIT.extractNcml(fileId, apiToken);
        extractNcmlFailExistsAlready.prettyPrint();
        extractNcmlFailExistsAlready.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.result", CoreMatchers.equalTo(false));

        Path pathToTxt = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "file.txt");
        String contentOfTxt = "Just a text file. Don't expect NcML out!";
        java.nio.file.Files.write(pathToTxt, contentOfTxt.getBytes());

        Response uploadFileTxt = UtilIT.uploadFileViaNative(datasetId.toString(), pathToTxt.toString(), apiToken);
        uploadFileTxt.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("file.txt"));

        long fileIdTxt = JsonPath.from(uploadFileTxt.body().asString()).getLong("data.files[0].dataFile.id");

        Response extractNcmlFailText = UtilIT.extractNcml(fileIdTxt, apiToken);
        extractNcmlFailText.prettyPrint();
        extractNcmlFailText.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.result", CoreMatchers.equalTo(false));

    }

    @Test
    public void testExtraBoundingBoxFromNetcdf() throws IOException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response setMetadataBlocks = UtilIT.setMetadataBlocks(dataverseAlias, Json.createArrayBuilder().add("citation").add("geospatial"), apiToken);
        setMetadataBlocks.prettyPrint();
        setMetadataBlocks.then().assertThat().statusCode(OK.getStatusCode());

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        // From https://www.ncei.noaa.gov/data/international-comprehensive-ocean-atmosphere/v3/archive/nrt/ICOADS_R3.0.0_1662-10.nc
        // via https://data.noaa.gov/onestop/collections/details/9bd5c743-0684-4e70-817a-ed977117f80c?f=temporalResolution:1%20Minute%20-%20%3C%201%20Hour;dataFormats:NETCDF
        String pathToFile = "src/test/resources/netcdf/ICOADS_R3.0.0_1662-10.nc";

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat().statusCode(OK.getStatusCode());

        Response getJson = UtilIT.nativeGet(datasetId, apiToken);
        getJson.prettyPrint();
        getJson.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.latestVersion.metadataBlocks.geospatial.fields[0].value[0].westLongitude.value", equalTo("-16.320007"))
                .body("data.latestVersion.metadataBlocks.geospatial.fields[0].value[0].eastLongitude.value", equalTo("-6.220001"))
                .body("data.latestVersion.metadataBlocks.geospatial.fields[0].value[0].northLongitude.value", equalTo("49.62"))
                .body("data.latestVersion.metadataBlocks.geospatial.fields[0].value[0].southLongitude.value", equalTo("41.8"));
    }

}
