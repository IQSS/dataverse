package edu.harvard.iq.dataverse.api;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.path.json.JsonPath;
import org.junit.Test;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;

/**
 * This class tests Harvesting Client functionality. 
 * Note that these methods test BOTH the proprietary Dataverse rest API for 
 * creating and managing harvesting clients, AND the underlining OAI-PMH 
 * harvesting functionality itself. I.e., we will use the Dataverse 
 * /api/harvest/clients/ api to run an actual harvest of a control set and
 * then validate the resulting harvested content. 
 */
public class HarvestingClientsIT {

    private static final Logger logger = Logger.getLogger(HarvestingClientsIT.class.getCanonicalName());

    private static final String HARVEST_CLIENTS_API = "/api/harvest/clients/";
    private static final String ROOT_COLLECTION = "root";
    private static final String HARVEST_URL = "https://demo.dataverse.org/oai";
    private static final String ARCHIVE_URL = "https://demo.dataverse.org";
    private static final String HARVEST_METADATA_FORMAT = "oai_dc";
    private static final String ARCHIVE_DESCRIPTION = "RestAssured harvesting client test";
    private static final String CONTROL_OAI_SET = "controlTestSet";
    private static final int DATASETS_IN_CONTROL_SET = 7;
    private static String normalUserAPIKey;
    private static String adminUserAPIKey;
    private static String harvestCollectionAlias; 
    
    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
        
        // Create the users, an admin and a non-admin:
        setupUsers(); 
        
        // Create a collection that we will use to harvest remote content into: 
        setupCollection();
        
    }

    private static void setupUsers() {
        Response cu0 = UtilIT.createRandomUser();
        normalUserAPIKey = UtilIT.getApiTokenFromResponse(cu0);
        Response cu1 = UtilIT.createRandomUser();
        String un1 = UtilIT.getUsernameFromResponse(cu1);
        Response u1a = UtilIT.makeSuperUser(un1);
        adminUserAPIKey = UtilIT.getApiTokenFromResponse(cu1);
    }
    
    private static void setupCollection() {
        Response createDataverseResponse = UtilIT.createRandomDataverse(adminUserAPIKey);
        createDataverseResponse.prettyPrint();
        assertEquals(CREATED.getStatusCode(), createDataverseResponse.getStatusCode());
        
        harvestCollectionAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // publish dataverse:
        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(harvestCollectionAlias, adminUserAPIKey);
        assertEquals(OK.getStatusCode(), publishDataverse.getStatusCode());
    }

    @Test
    public void testCreateEditDeleteClient() {
        // This method focuses on testing the native Dataverse harvesting client
        // API. 
        
        String nickName = UtilIT.getRandomString(6);
        

        String clientApiPath = String.format(HARVEST_CLIENTS_API+"%s", nickName);
        String clientJson = String.format("{\"dataverseAlias\":\"%s\","
                + "\"type\":\"oai\","
                + "\"harvestUrl\":\"%s\","
                + "\"archiveUrl\":\"%s\","
                + "\"metadataFormat\":\"%s\"}", 
                ROOT_COLLECTION, HARVEST_URL, ARCHIVE_URL, HARVEST_METADATA_FORMAT);

        
        // Try to create a client as normal user, should fail:
        
        Response rCreate = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, normalUserAPIKey)
                .body(clientJson)
                .post(clientApiPath);
        assertEquals(UNAUTHORIZED.getStatusCode(), rCreate.getStatusCode());

        
        // Try to create the same as admin user, should succeed:
        
        rCreate = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(clientJson)
                .post(clientApiPath);
        assertEquals(CREATED.getStatusCode(), rCreate.getStatusCode());
        
        // Try to update the client we have just created:
        
        String updateJson = String.format("{\"archiveDescription\":\"%s\"}", ARCHIVE_DESCRIPTION);
        
        Response rUpdate = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(updateJson)
                .put(clientApiPath);
        assertEquals(OK.getStatusCode(), rUpdate.getStatusCode());
        
        // Now let's retrieve the client we've just created and edited: 
                
        Response getClientResponse = given()
                .get(clientApiPath);
        
        logger.info("getClient.getStatusCode(): " + getClientResponse.getStatusCode());
        logger.info("getClient printresponse:  " + getClientResponse.prettyPrint());
        assertEquals(OK.getStatusCode(), getClientResponse.getStatusCode());
        
        // ... and validate the values:
        
        getClientResponse.then().assertThat()
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
                .body("data.type", equalTo("oai"))
                .body("data.nickName", equalTo(nickName))
                .body("data.archiveDescription", equalTo(ARCHIVE_DESCRIPTION))
                .body("data.dataverseAlias", equalTo(ROOT_COLLECTION))
                .body("data.harvestUrl", equalTo(HARVEST_URL))
                .body("data.archiveUrl", equalTo(ARCHIVE_URL))
                .body("data.metadataFormat", equalTo(HARVEST_METADATA_FORMAT));        
        
        // Try to delete the client as normal user  should fail: 
        
        Response rDelete = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, normalUserAPIKey)
                .delete(clientApiPath);
        logger.info("rDelete.getStatusCode(): " + rDelete.getStatusCode());
        assertEquals(UNAUTHORIZED.getStatusCode(), rDelete.getStatusCode());
        
        // Try to delete as admin user  should work:
        
        rDelete = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .delete(clientApiPath);
        logger.info("rDelete.getStatusCode(): " + rDelete.getStatusCode());
        assertEquals(OK.getStatusCode(), rDelete.getStatusCode());
    }
    
    @Test
    public void testHarvestingClientRun()  throws InterruptedException {
        // This test will create a client and attempt to perform an actual 
        // harvest and validate the resulting harvested content. 
        
        // Setup: create the client via native API
        // since this API is tested somewhat extensively in the previous 
        // method, we don't need to pay too much attention to this method, aside 
        // from confirming the expected HTTP status code.
        
        String nickName = UtilIT.getRandomString(6);

        String clientApiPath = String.format(HARVEST_CLIENTS_API+"%s", nickName);
        String clientJson = String.format("{\"dataverseAlias\":\"%s\","
                + "\"type\":\"oai\","
                + "\"harvestUrl\":\"%s\","
                + "\"archiveUrl\":\"%s\","
                + "\"set\":\"%s\","
                + "\"metadataFormat\":\"%s\"}", 
                harvestCollectionAlias, HARVEST_URL, ARCHIVE_URL, CONTROL_OAI_SET, HARVEST_METADATA_FORMAT);
                
        Response createResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(clientJson)
                .post(clientApiPath);
        assertEquals(CREATED.getStatusCode(), createResponse.getStatusCode());
        
        // API TEST 1. Run the harvest using the configuration (client) we have 
        // just created
        
        String runHarvestApiPath = String.format(HARVEST_CLIENTS_API+"%s/run", nickName);
        
        // TODO? - verify that a non-admin user cannot perform this operation (401)
        
        Response runResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .post(runHarvestApiPath);
        assertEquals(ACCEPTED.getStatusCode(), runResponse.getStatusCode());
        
        // API TEST 2. As indicated by the ACCEPTED status code above, harvesting
        // is an asynchronous operation that will be performed in the background.
        // Verify that this "in progress" status is properly reported while it's 
        // running, and that it completes in some reasonable amount of time. 
        
        int i = 0;
        int maxWait=20; // a very conservative interval; this harvest has no business taking this long
        do {
            // Give it an initial 1 sec. delay, to make sure the client state 
            // has been updated in the database, which can take some appreciable 
            // amount of time on a heavily-loaded server running a full suite of
            // tests:
            Thread.sleep(1000L);
            // keep checking the status of the client with the GET api:
            Response getClientResponse = given()
                .get(clientApiPath);
        
            assertEquals(OK.getStatusCode(), getClientResponse.getStatusCode());
            JsonPath responseJsonPath = getClientResponse.body().jsonPath();
            assertNotNull("Invalid JSON in GET client response", responseJsonPath);
            assertEquals(AbstractApiBean.STATUS_OK, responseJsonPath.getString("status")); 
            
            String clientStatus = responseJsonPath.getString("data.status");
            assertNotNull(clientStatus);
            
            if ("inProgress".equals(clientStatus) || "IN PROGRESS".equals(responseJsonPath.getString("data.lastResult"))) {
                // we'll sleep for another second
                i++;
            } else {
                logger.info("getClientResponse.prettyPrint: " 
                        + getClientResponse.prettyPrint());
                // Check the values in the response:
                // a) Confirm that the harvest has completed: 
                assertEquals("Unexpected client status: "+clientStatus, "inActive", clientStatus);
                
                // b) Confirm that it has actually succeeded:
                assertEquals("Last harvest not reported a success (took "+i+" seconds)", "SUCCESS", responseJsonPath.getString("data.lastResult"));
                String harvestTimeStamp = responseJsonPath.getString("data.lastHarvest");
                assertNotNull(harvestTimeStamp); 
                
                // c) Confirm that the other timestamps match: 
                assertEquals(harvestTimeStamp, responseJsonPath.getString("data.lastSuccessful"));
                assertEquals(harvestTimeStamp, responseJsonPath.getString("data.lastNonEmpty"));
                
                // d) Confirm that the correct number of datasets have been harvested:
                assertEquals(DATASETS_IN_CONTROL_SET, responseJsonPath.getInt("data.lastDatasetsHarvested"));
                
                // ok, it looks like the harvest has completed successfully.
                break;
            }
        } while (i<maxWait); 
        
        System.out.println("Waited " + i + " seconds for the harvest to complete.");
        
        // Fail if it hasn't completed in maxWait seconds
        assertTrue(i < maxWait);
        
        // TODO(?) use the native Dataverses/Datasets apis to verify that the expected
        // datasets have been harvested. This may or may not be necessary, seeing 
        // how we have already confirmed the number of successfully harvested 
        // datasets from the control set; somewhat hard to imagine a practical 
        // situation where that would not be enough (?).  
        
        // Cleanup: delete the client 
        
        Response deleteResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .delete(clientApiPath);
        System.out.println("deleteResponse.getStatusCode(): " + deleteResponse.getStatusCode());
        assertEquals(OK.getStatusCode(), deleteResponse.getStatusCode());
        
    }
}
