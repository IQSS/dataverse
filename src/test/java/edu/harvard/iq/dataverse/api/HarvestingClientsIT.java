package edu.harvard.iq.dataverse.api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.Status.ACCEPTED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

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
    private static final String CONTROL_OAI_SET = "controlTestSet2";
    private static final int DATASETS_IN_CONTROL_SET = 8;
    private static final String DATACITE_ARCHIVE_URL = "https://oai.datacite.org";
    private static final String DATACITE_OAI_URL = DATACITE_ARCHIVE_URL + "/oai";
    private static String normalUserAPIKey;
    private static String adminUserAPIKey;
    private static String harvestCollectionAlias;
    String clientApiPath = null;
    List<String> globalIdList = new ArrayList();
    
    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
        
        // Create the users, an admin and a non-admin:
        setupUsers(); 
        
        // Create a collection that we will use to harvest remote content into: 
        setupCollection();
        
    }
    @AfterEach
    public void cleanup() throws InterruptedException {
        if (clientApiPath != null) {
            Response deleteResponse = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                    .delete(clientApiPath);
            clientApiPath = null;
            System.out.println("deleteResponse.getStatusCode(): " + deleteResponse.getStatusCode());

            int i = 0;
            int maxWait = 20;
            String query = "dsPersistentId:" + globalIdList.stream().map(s -> "\""+s+"\"").collect(Collectors.joining(","));
            do {
                if (UtilIT.search(query, normalUserAPIKey).prettyPrint().contains("count_in_response\": 0")) {
                    break;
                }
                Thread.sleep(1000L);
            } while (i++ < maxWait);
        }
        globalIdList.clear();
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
    public void testCreateEditDeleteClient() throws InterruptedException {
        // This method focuses on testing the native Dataverse harvesting client
        // API. 
        
        String nickName = "h" + UtilIT.getRandomString(6);
        

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
                .body("status", equalTo(ApiConstants.STATUS_OK))
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
    public void testHarvestingClientRun_AllowHarvestingMissingCVV_False()  throws InterruptedException {
        harvestingClientRun(false, false);
    }
    @Test
    public void testHarvestingClientRun_AllowHarvestingMissingCVV_True()  throws InterruptedException {
        harvestingClientRun(true, false);
    }

    @Test
    public void testHarvestingClientRun_AllowHarvestingMissingCVV_True_WithSourceName()  throws InterruptedException {
        harvestingClientRun(true, true);
    }

    private void harvestingClientRun(boolean allowHarvestingMissingCVV, boolean testingSourceName)  throws InterruptedException {
        int expectedNumberOfSetsHarvested = allowHarvestingMissingCVV ? DATASETS_IN_CONTROL_SET : DATASETS_IN_CONTROL_SET - 1;

        // This test will create a client and attempt to perform an actual 
        // harvest and validate the resulting harvested content. 
        
        // Setup: create the client via native API
        // since this API is tested somewhat extensively in the previous 
        // method, we don't need to pay too much attention to this method, aside 
        // from confirming the expected HTTP status code.
        
        String nickName = "h" + UtilIT.getRandomString(6);
        String sourceName = testingSourceName ? "AnotherSourceName" : "";

        clientApiPath = String.format(HARVEST_CLIENTS_API+"%s", nickName);
        String clientJson = String.format("{\"dataverseAlias\":\"%s\","
                + "\"type\":\"oai\","
                + "\"sourceName\":\"%s\","
                + "\"harvestUrl\":\"%s\","
                + "\"archiveUrl\":\"%s\","
                + "\"set\":\"%s\","
                + "\"allowHarvestingMissingCVV\":%s,"
                + "\"metadataFormat\":\"%s\"}", 
                harvestCollectionAlias, sourceName, HARVEST_URL, ARCHIVE_URL, CONTROL_OAI_SET, allowHarvestingMissingCVV, HARVEST_METADATA_FORMAT);
        
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
            // Give it an initial 2 sec. delay, to make sure the client state
            // has been updated in the database, which can take some appreciable 
            // amount of time on a heavily-loaded server running a full suite of
            // tests:
            Thread.sleep(2000L);
            // keep checking the status of the client with the GET api:
            Response getClientResponse = given()
                .get(clientApiPath);
        
            assertEquals(OK.getStatusCode(), getClientResponse.getStatusCode());
            JsonPath responseJsonPath = getClientResponse.body().jsonPath();
            assertNotNull(responseJsonPath, "Invalid JSON in GET client response");
            assertEquals(ApiConstants.STATUS_OK, responseJsonPath.getString("status"));
            
            String clientStatus = responseJsonPath.getString("data.status");
            assertNotNull(clientStatus);
            
            if ("inProgress".equals(clientStatus) || "IN PROGRESS".equals(responseJsonPath.getString("data.lastResult"))) {
                // we'll sleep for 2 more seconds
                i++;
            } else {
                logger.info("getClientResponse.prettyPrint: " 
                        + getClientResponse.prettyPrint());
                // Check the values in the response:
                // a) Confirm that the harvest has completed: 
                assertEquals("inActive", clientStatus, "Unexpected client status: "+clientStatus);
                
                // b) Confirm that it has actually succeeded:
                assertTrue(responseJsonPath.getString("data.lastResult").contains("Completed"), "Last harvest not reported a success (took "+i+" seconds)");
                String harvestTimeStamp = responseJsonPath.getString("data.lastHarvest");
                assertNotNull(harvestTimeStamp); 
                
                // c) Confirm that the other timestamps match: 
                assertEquals(harvestTimeStamp, responseJsonPath.getString("data.lastSuccessful"));
                assertEquals(harvestTimeStamp, responseJsonPath.getString("data.lastNonEmpty"));
                
                // d) Confirm that the correct number of datasets have been harvested:
                assertEquals(expectedNumberOfSetsHarvested, responseJsonPath.getInt("data.lastDatasetsHarvested"));
                
                // ok, it looks like the harvest has completed successfully.
                break;
            }
        } while (i<maxWait); 
        
        System.out.println("Waited " + i + " seconds for the harvest to complete.");

        // Let's give the asynchronous indexing an extra sec. to finish:
        Thread.sleep(1000L); 
        // Requires the index-harvested-metadata-source Flag feature to be enabled to search on the nickName
        // Otherwise, the search must be performed with metadataSource:Harvested
        Response searchHarvestedDatasets = UtilIT.search("metadataSource:" + (testingSourceName ? sourceName : nickName), normalUserAPIKey);
        searchHarvestedDatasets.then().assertThat().statusCode(OK.getStatusCode());
        searchHarvestedDatasets.prettyPrint();
        // Get all global ids for cleanup
        JsonPath jsonPath = searchHarvestedDatasets.getBody().jsonPath();
        int sz = jsonPath.getInt("data.items.size()");
        for(int idx = 0; idx < sz; idx++) {
            globalIdList.add(jsonPath.getString("data.items["+idx+"].global_id"));
        }
        // verify count after collecting global ids
        assertEquals(expectedNumberOfSetsHarvested, jsonPath.getInt("data.total_count"));

        // ensure the publisher name is present in the harvested dataset citation
        Response harvestedDataverse = given().get(ARCHIVE_URL + "/api/dataverses/1");  
        String harvestedDataverseName = harvestedDataverse.getBody().jsonPath().getString("data.name");        
        assertTrue(jsonPath.getString("data.items[0].citation").contains(harvestedDataverseName));
        
        // Fail if it hasn't completed in maxWait seconds
        assertTrue(i < maxWait);
    }
    
    /*
     * Being able to harvest from DataCite (issue #10909, pr #11011) is an 
     * important enough feature to warrant a dedicated test. 
     * Just like the other tests above, this will rely on an external OAI 
     * server, which is somewhat problematic inherently. However, of all the 
     * external servers and services, DataCite can be safely considered to be 
     * more reliable than most. 
     * The test is super straightforward, with the goal of harvesting one 
     * specific IQSS dataset (doi:10.7910/DVN/TJCLKP) from 
     * https://oai.datacite.org/oai. As part of testing the overal functionality
     * of being able to work with the quirks of the DataCite OAI service, it 
     * tests 2 new features:
     *    "useOaiIdentifiersAsPids": true,
     *    "useListRecords": true,
     * (both have useful applications in other scenarios, i.e. when harvesting
     * from other sources, not just from DataCite!)
     * 
    */
    @Test
    public void testHarvestingFromDatacite()  throws InterruptedException {
        String nickName = "philTJCLKP" + UtilIT.getRandomString(6);

        // The magical string used as the name of our "pseudo set" is the
        // native DataCite search API query that finds our dataset, base64-encoded. 
        // i.e., 
        //    native API: https://api.datacite.org/dois?query=doi:10.7910/DVN/TJCLKP
        // encoded: 
        //   echo "doi:10.7910/DVN/TJCLKP" | base64
        //   ZG9pOjEwLjc5MTAvRFZOL1RKQ0xLUAo=
        String pseudoSetName = "~ZG9pOjEwLjc5MTAvRFZOL1RKQ0xLUAo=";
        
        clientApiPath = String.format(HARVEST_CLIENTS_API+"%s", nickName);
        String clientJson = String.format("{\"dataverseAlias\":\"root\","
                + "\"type\":\"oai\","
                + "\"harvestUrl\":\"%s\","
                + "\"archiveUrl\":\"%s\","
                + "\"set\":\"%s\","
                + "\"useOaiIdentifiersAsPids\": true,"
                + "\"useListRecords\": true,"
                + "\"metadataFormat\":\"%s\"}", 
                DATACITE_OAI_URL, DATACITE_ARCHIVE_URL, pseudoSetName, HARVEST_METADATA_FORMAT);
        
        Response createResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(clientJson)
                .post(clientApiPath);
        assertEquals(CREATED.getStatusCode(), createResponse.getStatusCode());
        
        // API TEST 1. Run the harvest using the configuration (client) we have 
        // just created
        
        String runHarvestApiPath = String.format(HARVEST_CLIENTS_API+"%s/run", nickName);
                
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
            // Give it an initial 2 sec. delay, to make sure the client state
            // has been updated in the database, which can take some appreciable 
            // amount of time on a heavily-loaded server running a full suite of
            // tests:
            Thread.sleep(2000L);
            // keep checking the status of the client with the GET api:
            Response getClientResponse = given()
                .get(clientApiPath);
        
            assertEquals(OK.getStatusCode(), getClientResponse.getStatusCode());
            JsonPath responseJsonPath = getClientResponse.body().jsonPath();
            assertNotNull(responseJsonPath, "Invalid JSON in GET client response");
            assertEquals(ApiConstants.STATUS_OK, responseJsonPath.getString("status"));
            
            String clientStatus = responseJsonPath.getString("data.status");
            assertNotNull(clientStatus);
            
            if ("inProgress".equals(clientStatus) || "IN PROGRESS".equals(responseJsonPath.getString("data.lastResult"))) {
                // we'll sleep for 2 more seconds
                i++;
            } else {
                logger.info("getClientResponse.prettyPrint: " 
                        + getClientResponse.prettyPrint());
                // Check the values in the response:
                // a) Confirm that the harvest has completed: 
                assertEquals("inActive", clientStatus, "Unexpected client status: "+clientStatus);
                
                // b) Confirm that it has actually succeeded:
                assertEquals("Completed", responseJsonPath.getString("data.lastResult"), "Last harvest not reported a success (took "+i+" seconds)");
                String harvestTimeStamp = responseJsonPath.getString("data.lastHarvest");
                assertNotNull(harvestTimeStamp); 
                
                // c) Confirm that the other timestamps match: 
                assertEquals(harvestTimeStamp, responseJsonPath.getString("data.lastSuccessful"));
                assertEquals(harvestTimeStamp, responseJsonPath.getString("data.lastNonEmpty"));
                
                // d) Confirm that the expected 1 dataset has been harvested, with no failures:
                assertEquals(1, responseJsonPath.getInt("data.lastDatasetsHarvested"));
                assertEquals(0, responseJsonPath.getInt("data.lastDatasetsFailed"));
                assertEquals(0, responseJsonPath.getInt("data.lastDatasetsDeleted"));
                
                // ok, it looks like the harvest has completed successfully.
                break;
            }
        } while (i<maxWait); 
        
        System.out.println("Waited " + i + " seconds for the harvest to complete.");
        // Fail if it hasn't completed in maxWait seconds
        assertTrue(i < maxWait);
        
        // @todo: maybe call native API and check specifically on 
        // /api/datasets/:persistentId?persistentId=doi:10.7910/DVN/TJCLKP
        // to verify that it has been properly imporated. 
        
        // No need to delete the client (and the harvested dataset with it) here, 
        // it will be deleted by the @AfterEach cleanup() method
    }
}
