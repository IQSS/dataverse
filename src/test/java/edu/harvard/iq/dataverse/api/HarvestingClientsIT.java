package edu.harvard.iq.dataverse.api;

import java.util.logging.Logger;
import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import org.junit.Test;
import com.jayway.restassured.response.Response;
import static org.hamcrest.CoreMatchers.equalTo;
import static junit.framework.Assert.assertEquals;
import org.junit.BeforeClass;

/**
 * extremely minimal (for now) API tests for creating OAI clients.
 */
public class HarvestingClientsIT {

    private static final Logger logger = Logger.getLogger(HarvestingClientsIT.class.getCanonicalName());

    private static final String harvestClientsApi = "/api/harvest/clients/";
    private static final String harvestCollection = "root";
    private static final String harvestUrl = "https://demo.dataverse.org/oai";
    private static final String archiveUrl = "https://demo.dataverse.org";
    private static final String harvestMetadataFormat = "oai_dc";
    private static final String archiveDescription = "RestAssured harvesting client test";
    
    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    private void setupUsers() {
        Response cu0 = UtilIT.createRandomUser();
        normalUserAPIKey = UtilIT.getApiTokenFromResponse(cu0);
        Response cu1 = UtilIT.createRandomUser();
        String un1 = UtilIT.getUsernameFromResponse(cu1);
        Response u1a = UtilIT.makeSuperUser(un1);
        adminUserAPIKey = UtilIT.getApiTokenFromResponse(cu1);
    }

    private String normalUserAPIKey;
    private String adminUserAPIKey;

    @Test
    public void testCreateEditDeleteClient() {
        setupUsers();
        String nickName = UtilIT.getRandomString(6);
        

        String clientApiPath = String.format(harvestClientsApi+"%s", nickName);
        String clientJson = String.format("{\"dataverseAlias\":\"%s\","
                + "\"type\":\"oai\","
                + "\"harvestUrl\":\"%s\","
                + "\"archiveUrl\":\"%s\","
                + "\"metadataFormat\":\"%s\"}", 
                harvestCollection, harvestUrl, archiveUrl, harvestMetadataFormat);

        
        // Try to create a client as normal user, should fail:
        
        Response rCreate = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, normalUserAPIKey)
                .body(clientJson)
                .post(clientApiPath);
        assertEquals(401, rCreate.getStatusCode());

        
        // Try to create the same as admin user, should succeed:
        
        rCreate = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(clientJson)
                .post(clientApiPath);
        assertEquals(201, rCreate.getStatusCode());
        
        // Try to update the client we have just created:
        
        String updateJson = String.format("{\"archiveDescription\":\"%s\"}", archiveDescription);
        
        Response rUpdate = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(updateJson)
                .put(clientApiPath);
        assertEquals(200, rUpdate.getStatusCode());
        
        // Now let's retrieve the client we've just created and edited: 
                
        Response getClientResponse = given()
                .get(clientApiPath);
        
        logger.info("getClient.getStatusCode(): " + getClientResponse.getStatusCode());
        logger.info("getClient printresponse:  " + getClientResponse.prettyPrint());
        assertEquals(200, getClientResponse.getStatusCode());
        
        // ... and validate the values:
        
        getClientResponse.then().assertThat()
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
                .body("data.type", equalTo("oai"))
                .body("data.nickName", equalTo(nickName))
                .body("data.archiveDescription", equalTo(archiveDescription))
                .body("data.dataverseAlias", equalTo(harvestCollection))
                .body("data.harvestUrl", equalTo(harvestUrl))
                .body("data.archiveUrl", equalTo(archiveUrl))
                .body("data.metadataFormat", equalTo(harvestMetadataFormat));        
        
        // Try to delete the client as normal user  should fail: 
        
        Response rDelete = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, normalUserAPIKey)
                .delete(clientApiPath);
        logger.info("rDelete.getStatusCode(): " + rDelete.getStatusCode());
        assertEquals(401, rDelete.getStatusCode());
        
        // Try to delete as admin user  should work:
        
        rDelete = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .delete(clientApiPath);
        logger.info("rDelete.getStatusCode(): " + rDelete.getStatusCode());
        assertEquals(200, rDelete.getStatusCode());
    }
}
