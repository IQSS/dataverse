package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import com.jayway.restassured.path.json.JsonPath;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

public class FilesIT {

    private static final Logger logger = Logger.getLogger(FilesIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }


    @Test
    public void testFileReplace() {

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
//        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
//        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        Response getDatasetJsonBeforeFiles = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonBeforeFiles.prettyPrint();
        getDatasetJsonBeforeFiles.then().assertThat().statusCode(OK.getStatusCode());
        String protocol1 = JsonPath.from(getDatasetJsonBeforeFiles.getBody().asString()).getString("data.protocol");
        String authority1 = JsonPath.from(getDatasetJsonBeforeFiles.getBody().asString()).getString("data.authority");
        String identifier1 = JsonPath.from(getDatasetJsonBeforeFiles.getBody().asString()).getString("data.identifier");
        String dataset1PersistentId = protocol1 + ":" + authority1 + "/" + identifier1;

        Response uploadFileResponse = UtilIT.uploadRandomFile(dataset1PersistentId, apiToken);
        uploadFileResponse.prettyPrint();
        getDatasetJsonBeforeFiles.then().assertThat().statusCode(OK.getStatusCode());
        assertEquals(CREATED.getStatusCode(), uploadFileResponse.getStatusCode());

        Response getDatasetJsonWithFiles = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonWithFiles.prettyPrint();
        getDatasetJsonWithFiles.then().assertThat().statusCode(OK.getStatusCode());
        int fileId = JsonPath.from(getDatasetJsonWithFiles.getBody().asString()).getInt("data.latestVersion.files[0].dataFile.id");
        UtilIT.publishDataverseViaSword(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaSword(dataset1PersistentId, apiToken).then().assertThat().statusCode(OK.getStatusCode());

        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response replace = UtilIT.replaceFile(datasetId, fileId, pathToFile, apiToken);
        replace.prettyPrint();
        replace.then().assertThat()
                .body("message", equalTo("File successfully replaced!"))
                .statusCode(OK.getStatusCode());

        Response getDatasetJson = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJson.prettyPrint();
        getDatasetJson.then().assertThat()
                .body("data.latestVersion.files[0].dataFile.filename", equalTo("dataverseproject.png"))
                .body("data.latestVersion.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.latestVersion.files[0].dataFile.rootDataFileId", not(-1))
                .body("data.latestVersion.files[0].dataFile.previousDataFileId", equalTo(fileId))
                .statusCode(OK.getStatusCode());

    }

    @Test
    public void test01_ReplaceGood() {

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
//        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);

//        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        String expectedContentType = "image/png";
        String expectedLabel = "dataverseproject.png";
        Response add = UtilIT.uploadFileViaNative(datasetId, pathToFile, apiToken);

        add.prettyPrint();
        add.then().assertThat()
                .body("message", equalTo("File successfully added!"))
                .body("data.filename", equalTo(expectedLabel))
                .body("data.contentType", equalTo(expectedContentType))
                .statusCode(OK.getStatusCode());

        Response getDatasetJson = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJson.prettyPrint();
        getDatasetJson.then().assertThat()
                .body("data.latestVersion.files[0].dataFile.filename", equalTo(expectedLabel))
                .body("data.latestVersion.files[0].dataFile.contentType", equalTo(expectedContentType))
                .body("data.latestVersion.files[0].dataFile.rootDataFileId", equalTo(-1))
                .body("data.latestVersion.files[0].dataFile.previousDataFileId", nullValue())
                .statusCode(OK.getStatusCode());
    }

}
