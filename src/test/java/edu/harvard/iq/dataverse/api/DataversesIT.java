package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class DataversesIT {

    private static final Logger logger = Logger.getLogger(DataversesIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testAttemptToCreateDuplicateAlias() throws Exception {

        Response createUserResponse = UtilIT.createRandomUser();
//        createUserResponse.prettyPrint();
        createUserResponse.then().assertThat().statusCode(OK.getStatusCode());

        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        String username = UtilIT.getUsernameFromResponse(createUserResponse);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        if (createDataverseResponse.getStatusCode() != 201) {
            // purposefully using println here to the error shows under "Test Results" in Netbeans
            System.out.println("A workspace for testing (a dataverse) couldn't be created in the root dataverse. The output was:\n\n" + createDataverseResponse.body().asString());
            System.out.println("\nPlease ensure that users can created dataverses in the root in order for this test to run.");
        } else {
            createDataverseResponse.prettyPrint();
        }
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias1 = UtilIT.getAliasFromResponse(createDataverseResponse);
        String dataverseAlias2 = dataverseAlias1.toUpperCase();
        logger.info("Attempting to creating dataverse with alias '" + dataverseAlias2 + "' (uppercase version of existing '" + dataverseAlias1 + "' dataverse, should fail)...");
        Response attemptToCreateDataverseWithDuplicateAlias = UtilIT.createDataverse(dataverseAlias2, apiToken);
        attemptToCreateDataverseWithDuplicateAlias.prettyPrint();
        attemptToCreateDataverseWithDuplicateAlias.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

        logger.info("Deleting dataverse " + dataverseAlias1);
        Response deleteDataverse1Response = UtilIT.deleteDataverse(dataverseAlias1, apiToken);
        deleteDataverse1Response.prettyPrint();
        deleteDataverse1Response.then().assertThat().statusCode(OK.getStatusCode());

        logger.info("Checking response code for attempting to delete a non-existent dataverse.");
        Response attemptToDeleteDataverseThatShouldNotHaveBeenCreated = UtilIT.deleteDataverse(dataverseAlias2, apiToken);
        attemptToDeleteDataverseThatShouldNotHaveBeenCreated.prettyPrint();
        attemptToDeleteDataverseThatShouldNotHaveBeenCreated.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        Response deleteUser1Response = UtilIT.deleteUser(username);
        deleteUser1Response.prettyPrint();
        deleteUser1Response.then().assertThat().statusCode(OK.getStatusCode());

    }

    @Test
    public void testCreateDatasets() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        // create test dataverse
        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse1Response);

        Response attemptJsonMigrationAsNonSuperuser = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch2.json"), "MIGRATION");
        attemptJsonMigrationAsNonSuperuser.prettyPrint();
        attemptJsonMigrationAsNonSuperuser.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        // migrate dataset with global ID
        Response createDataset1Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch2.json"), "MIGRATION");
        createDataset1Response.prettyPrint();
        createDataset1Response.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        int datasetId1 = UtilIT.getDatasetIdFromResponse(createDataset1Response);

        // attempt to migrate dataset with duplicate global ID
        Response createDataset2Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch2.json"), "MIGRATION");
        createDataset2Response.prettyPrint();
        createDataset2Response.then().assertThat()
                .body("message", equalTo("A dataset with the identifier doi:10.15785/SBGRID/8888 already exists."))
                .statusCode(BAD_REQUEST.getStatusCode());

        // attempt to migrate dataset missing global ID param: authority
        Response createDataset3Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch3.json"), "MIGRATION");
        createDataset3Response.prettyPrint();
        createDataset3Response.then().assertThat()
                .body("message", equalTo("Protocol, authority and identifier must all be specified when migrating a dataset with an existing global identifier."))
                .statusCode(BAD_REQUEST.getStatusCode());

        // migrate dataset with no import type specified (should be reset to MIGRATION since all global ID params are present)
        Response createDataset4Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch4.json"), null);
        createDataset4Response.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        int datasetId2 = UtilIT.getDatasetIdFromResponse(createDataset4Response);

        // normal dataset creation (NEW, no existing DOI to migrate)
        Response createDataset5Response = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset5Response.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        int datasetId3 = UtilIT.getDatasetIdFromResponse(createDataset5Response);

        Response deleteDataset1Response = UtilIT.deleteDatasetViaNativeApi(datasetId1, apiToken);
        deleteDataset1Response.prettyPrint();
        deleteDataset1Response.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDataset2Response = UtilIT.deleteDatasetViaNativeApi(datasetId2, apiToken);
        deleteDataset2Response.prettyPrint();
        deleteDataset2Response.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDataset3Response = UtilIT.deleteDatasetViaNativeApi(datasetId3, apiToken);
        deleteDataset3Response.prettyPrint();
        deleteDataset3Response.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDataverse1Response = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverse1Response.prettyPrint();
        deleteDataverse1Response.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteUser1Response = UtilIT.deleteUser(username);
        deleteUser1Response.prettyPrint();
        deleteUser1Response.then().assertThat().statusCode(OK.getStatusCode());

    }

}
