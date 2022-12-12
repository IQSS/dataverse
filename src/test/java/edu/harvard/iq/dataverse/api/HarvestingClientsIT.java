package edu.harvard.iq.dataverse.api;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
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

    private static final String harvestClientsApi = "/api/harvest/clients/";
    private static final String rootCollection = "root";
    private static final String harvestUrl = "https://demo.dataverse.org/oai";
    private static final String archiveUrl = "https://demo.dataverse.org";
    private static final String harvestMetadataFormat = "oai_dc";
    private static final String archiveDescription = "RestAssured harvesting client test";
    private static final String controlOaiSet = "controlTestSet";
    private static final int datasetsInControlSet = 7;
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
        //setupUsers();
        String nickName = UtilIT.getRandomString(6);
        

        String clientApiPath = String.format(harvestClientsApi+"%s", nickName);
        String clientJson = String.format("{\"dataverseAlias\":\"%s\","
                + "\"type\":\"oai\","
                + "\"harvestUrl\":\"%s\","
                + "\"archiveUrl\":\"%s\","
                + "\"metadataFormat\":\"%s\"}", 
                rootCollection, harvestUrl, archiveUrl, harvestMetadataFormat);

        
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
        
        String updateJson = String.format("{\"archiveDescription\":\"%s\"}", archiveDescription);
        
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
                .body("data.archiveDescription", equalTo(archiveDescription))
                .body("data.dataverseAlias", equalTo(rootCollection))
                .body("data.harvestUrl", equalTo(harvestUrl))
                .body("data.archiveUrl", equalTo(archiveUrl))
                .body("data.metadataFormat", equalTo(harvestMetadataFormat));        
        
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
        
        // Setup: create the client via the API
        // since this API is tested somewhat extensively in the previous 
        // method, we don't need to pay too much attention to this method, aside 
        // from confirming the expected HTTP status code.
        
        String nickName = UtilIT.getRandomString(6);

        String clientApiPath = String.format(harvestClientsApi+"%s", nickName);
        String clientJson = String.format("{\"dataverseAlias\":\"%s\","
                + "\"type\":\"oai\","
                + "\"harvestUrl\":\"%s\","
                + "\"archiveUrl\":\"%s\","
                + "\"set\":\"%s\","
                + "\"metadataFormat\":\"%s\"}", 
                harvestCollectionAlias, harvestUrl, archiveUrl, controlOaiSet, harvestMetadataFormat);
                
        Response createResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(clientJson)
                .post(clientApiPath);
        assertEquals(CREATED.getStatusCode(), createResponse.getStatusCode());
        
        // API TEST 1. Run the harvest using the configuration (client) we have 
        // just created
        
        String runHarvestApiPath = String.format(harvestClientsApi+"%s/run", nickName);
        
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
            // keep checking the status of the client with the GET api:
            Response getClientResponse = given()
                .get(clientApiPath);
        
            assertEquals(OK.getStatusCode(), getClientResponse.getStatusCode());
            assertEquals(AbstractApiBean.STATUS_OK, getClientResponse.body().jsonPath().getString("status")); 
            
            if (logger.isLoggable(Level.FINE)) {
                logger.info("listIdentifiersResponse.prettyPrint: " 
                        + getClientResponse.prettyPrint());
            }
            
            String clientStatus = getClientResponse.body().jsonPath().getString("data.status");
            assertNotNull(clientStatus);
            
            if ("inProgress".equals(clientStatus)) {
                // we'll sleep for another second
                i++;
            } else {
                // Check the values in the response:
                // a) Confirm that the harvest has completed: 
                assertEquals("Unexpected client status: "+clientStatus, "inActive", clientStatus);
                
                // b) Confirm that it has actually succeeded:
                assertEquals("Last harvest not reported a success", "SUCCESS", getClientResponse.body().jsonPath().getString("data.lastResult"));
                String harvestTimeStamp = getClientResponse.body().jsonPath().getString("data.lastHarvest");
                assertNotNull(harvestTimeStamp); 
                
                // c) Confirm that the other timestamps match: 
                assertEquals(harvestTimeStamp, getClientResponse.body().jsonPath().getString("data.lastSuccessful"));
                assertEquals(harvestTimeStamp, getClientResponse.body().jsonPath().getString("data.lastNonEmpty"));
                
                // d) Confirm that the correct number of datasets have been harvested:
                assertEquals(datasetsInControlSet, getClientResponse.body().jsonPath().getInt("data.lastDatasetsHarvested"));
                
                // ok, it looks like the harvest has completed successfully.
                break;
            }
            Thread.sleep(1000L);
        } while (i<maxWait); 
        
        System.out.println("Waited " + i + " seconds for the harvest to complete.");
        
        // Fail if it hasn't completed in maxWait seconds
        assertTrue(i < maxWait);
        
        // TODO: use the native Dataverses/Datasets apis to verify that the expected
        // datasets have been harvested. 
        
        // Cleanup: delete the client 
        
        Response deleteResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .delete(clientApiPath);
        System.out.println("deleteResponse.getStatusCode(): " + deleteResponse.getStatusCode());
        assertEquals(OK.getStatusCode(), deleteResponse.getStatusCode());
        
    }
}
