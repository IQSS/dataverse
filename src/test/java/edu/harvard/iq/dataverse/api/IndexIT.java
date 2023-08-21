package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import static edu.harvard.iq.dataverse.api.UtilIT.API_TOKEN_HTTP_HEADER;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.ArrayList;
import java.util.logging.Logger;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.jupiter.api.AfterEach;

public class IndexIT {

    private static final Logger logger = Logger.getLogger(IndexIT.class.getCanonicalName());

    @BeforeAll
    public static void setUpClass() {

        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response makeSureTokenlessSearchIsEnabled = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiRequiresToken);
        makeSureTokenlessSearchIsEnabled.then().assertThat()
                .statusCode(OK.getStatusCode());

    }

  
    @Test
    public void testIndexStatus() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        Response getDatasetJsonNoFiles = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonNoFiles.prettyPrint();
        String protocol1 = JsonPath.from(getDatasetJsonNoFiles.getBody().asString()).getString("data.protocol");
        String authority1 = JsonPath.from(getDatasetJsonNoFiles.getBody().asString()).getString("data.authority");
        String identifier1 = JsonPath.from(getDatasetJsonNoFiles.getBody().asString()).getString("data.identifier");
        String dataset1PersistentId = protocol1 + ":" + authority1 + "/" + identifier1;

        Response uploadMd5File = UtilIT.uploadRandomFile(dataset1PersistentId, apiToken);
        uploadMd5File.prettyPrint();
        assertEquals(CREATED.getStatusCode(), uploadMd5File.getStatusCode());
   
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .queryParam("sync","true")
                .get("/api/admin/index/status");
        response.prettyPrint();
        ArrayList emptyList = new ArrayList<>();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.contentInDatabaseButStaleInOrMissingFromIndex.dataverses", CoreMatchers.equalTo(emptyList))
                .body("data.contentInDatabaseButStaleInOrMissingFromIndex.datasets", CoreMatchers.equalTo(emptyList))
                .body("data.contentInIndexButNotDatabase.dataverses", CoreMatchers.equalTo(emptyList))
                .body("data.contentInIndexButNotDatabase.datasets", CoreMatchers.equalTo(emptyList))
                .body("data.contentInIndexButNotDatabase.files", CoreMatchers.equalTo(emptyList))
                .body("data.permissionsInDatabaseButStaleInOrMissingFromIndex.dvobjects", CoreMatchers.equalTo(emptyList))
                .body("data.permissionsInIndexButNotDatabase.permissions", CoreMatchers.equalTo(emptyList));
        
        Response getDatasetJsonAfterMd5File = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonAfterMd5File.prettyPrint();
        getDatasetJsonAfterMd5File.then().assertThat()
                .body("data.latestVersion.files[0].dataFile.md5", equalTo("0386269a5acb2c57b4eade587ff4db64"))
                .body("data.latestVersion.files[0].dataFile.checksum.type", equalTo("MD5"))
                .body("data.latestVersion.files[0].dataFile.checksum.value", equalTo("0386269a5acb2c57b4eade587ff4db64"));

        int fileId = JsonPath.from(getDatasetJsonAfterMd5File.getBody().asString()).getInt("data.latestVersion.files[0].dataFile.id");
        Response deleteFile = UtilIT.deleteFile(fileId, apiToken);
        deleteFile.prettyPrint();
        deleteFile.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());

        Response deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
     
        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
      
        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
      
    }
   
    @AfterEach
    public void tearDownDataverse() {
        }

    @AfterAll
    public static void cleanup() {
    }

}
