package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.startsWith;

/**
 *  NOTE: This test WILL NOT pass if your installation is not configured for Amazon S3 storage.
 *  For S3 storage, you must set two jvm options: storage-driver-id and s3-bucket-name
 *  Refer to the guides or to https://github.com/IQSS/dataverse/issues/3921#issuecomment-319973245
 * @author bsilverstein
 */
public class S3AccessIT {
    
    private static final Logger logger = Logger.getLogger(S3AccessIT.class.getCanonicalName());

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
        
    }
    
    @Test
    public void testAddDataFileS3Prefix() {
        //create user who will make a dataverse/dataset
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        createDatasetResponse.prettyPrint();
        
        //upload a tabular file via native, check storage id prefix for s3
        String pathToFile = "scripts/search/data/tabular/1char";
        Response addFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        addFileResponse.prettyPrint();
        addFileResponse.then().assertThat()
                .body("data.files[0].dataFile.storageIdentifier", startsWith("s3://"));
        
        //clean up test dvobjects and user
        Response deleteDataset = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDataset.prettyPrint();
        deleteDataset.then().assertThat()
                .statusCode(200);

        Response deleteDataverse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverse.prettyPrint();
        deleteDataverse.then().assertThat()
                .statusCode(200);
        
        Response deleteUser = UtilIT.deleteUser(username);
        deleteUser.prettyPrint();
        deleteUser.then().assertThat()
                .statusCode(200);
    }
}
