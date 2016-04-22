package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static junit.framework.Assert.assertEquals;

public class DataversesIT {

    private static final Logger logger = Logger.getLogger(DataversesIT.class.getCanonicalName());

    private static String username1;
    private static String apiToken1;
    private static String dataverseAlias1;
    private static String dataverseAlias2;
    private static String dataverseAlias3;
    private static int datasetId1;
    private static int datasetId2;
    private static int datasetId3;

    @BeforeClass
    public static void setUpClass() {

        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response createUserResponse = UtilIT.createRandomUser();
//        createUserResponse.prettyPrint();
        assertEquals(200, createUserResponse.getStatusCode());

        apiToken1 = UtilIT.getApiTokenFromResponse(createUserResponse);
        username1 = UtilIT.getUsernameFromResponse(createUserResponse);

    }

    @Test
    public void testAttemptToCreateDuplicateAlias() throws Exception {

        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken1);
        if (createDataverse1Response.getStatusCode() != 201) {
            // purposefully using println here to the error shows under "Test Results" in Netbeans
            System.out.println("A workspace for testing (a dataverse) couldn't be created in the root dataverse. The output was:\n\n" + createDataverse1Response.body().asString());
            System.out.println("\nPlease ensure that users can created dataverses in the root in order for this test to run.");
        } else {
            createDataverse1Response.prettyPrint();
        }
        assertEquals(201, createDataverse1Response.getStatusCode());

        dataverseAlias1 = UtilIT.getAliasFromResponse(createDataverse1Response);
        dataverseAlias2 = dataverseAlias1.toUpperCase();
        logger.info("Attempting to creating dataverse with alias '" + dataverseAlias2 + "' (uppercase version of existing '" + dataverseAlias1 + "' dataverse, should fail)...");
        Response attemptToCreateDataverseWithDuplicateAlias = UtilIT.createDataverse(dataverseAlias2, apiToken1);
        attemptToCreateDataverseWithDuplicateAlias.prettyPrint();
        assertEquals(400, attemptToCreateDataverseWithDuplicateAlias.getStatusCode());

        logger.info("Deleting dataverse " + dataverseAlias1);
        Response deleteDataverse1Response = UtilIT.deleteDataverse(dataverseAlias1, apiToken1);
        deleteDataverse1Response.prettyPrint();
        assertEquals(200, deleteDataverse1Response.getStatusCode());

        logger.info("Checking response code for attempting to delete a non-existent dataverse.");
        Response attemptToDeleteDataverseThatShouldNotHaveBeenCreated = UtilIT.deleteDataverse(dataverseAlias2, apiToken1);
        attemptToDeleteDataverseThatShouldNotHaveBeenCreated.prettyPrint();
        assertEquals(404, attemptToDeleteDataverseThatShouldNotHaveBeenCreated.getStatusCode());

    }

    @Test
    public void testCreateDatasets() {

        // create test dataverse
        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken1);
        dataverseAlias3 = UtilIT.getAliasFromResponse(createDataverse1Response);

        // migrate dataset with global ID
        Response createDataset1Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias3, apiToken1,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch2.json"), "MIGRATION");
        assertEquals(201, createDataset1Response.getStatusCode());
        datasetId1 = UtilIT.getDatasetIdFromResponse(createDataset1Response);

        // attempt to migrate dataset with duplicate global ID
        Response createDataset2Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias3, apiToken1,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch2.json"), "MIGRATION");
        assertEquals(400, createDataset2Response.getStatusCode());

        // attempt to migrate dataset missing global ID param: authority
        Response createDataset3Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias3, apiToken1,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch3.json"), "MIGRATION");
        assertEquals(400, createDataset3Response.getStatusCode());

        // migrate dataset with no import type specified (should be reset to MIGRATION since all global ID params are present)
        Response createDataset4Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias3, apiToken1,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch4.json"), null);
        assertEquals(201, createDataset4Response.getStatusCode());
        datasetId2 = UtilIT.getDatasetIdFromResponse(createDataset4Response);

        // normal dataset creation (NEW, no existing DOI to migrate)
        Response createDataset5Response = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias3, apiToken1);
        assertEquals(201, createDataset5Response.getStatusCode());
        datasetId3 = UtilIT.getDatasetIdFromResponse(createDataset5Response);

    }

    @AfterClass
    public static void tearDownClass() {
        boolean disabled = false;

        if (disabled) {
            return;
        }

        Response deleteDataset1Response = UtilIT.deleteDatasetViaNativeApi(datasetId1, apiToken1);
        deleteDataset1Response.prettyPrint();
        assertEquals(200, deleteDataset1Response.getStatusCode());

        Response deleteDataset2Response = UtilIT.deleteDatasetViaNativeApi(datasetId2, apiToken1);
        deleteDataset2Response.prettyPrint();
        assertEquals(200, deleteDataset2Response.getStatusCode());

        Response deleteDataset3Response = UtilIT.deleteDatasetViaNativeApi(datasetId3, apiToken1);
        deleteDataset3Response.prettyPrint();
        assertEquals(200, deleteDataset3Response.getStatusCode());

        Response deleteDataverse1Response = UtilIT.deleteDataverse(dataverseAlias3, apiToken1);
        deleteDataverse1Response.prettyPrint();
        assertEquals(200, deleteDataverse1Response.getStatusCode());

        Response deleteUser1Response = UtilIT.deleteUser(username1);
        deleteUser1Response.prettyPrint();
        assertEquals(200, deleteUser1Response.getStatusCode());

    }

}
