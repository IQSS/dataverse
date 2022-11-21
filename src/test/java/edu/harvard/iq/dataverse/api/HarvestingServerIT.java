package edu.harvard.iq.dataverse.api;

import java.util.logging.Logger;
import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.path.json.JsonPath;
import static edu.harvard.iq.dataverse.api.UtilIT.API_TOKEN_HTTP_HEADER;
import javax.json.Json;
import javax.json.JsonArray;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.Ignore;
import java.util.List;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * extremely minimal API tests for creating OAI sets.
 */
public class HarvestingServerIT {

    private static final Logger logger = Logger.getLogger(HarvestingServerIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
	// enable harvesting server
	//  Gave some thought to storing the original response, and resetting afterwards - but that appears to be more complexity than it's worth
	Response enableHarvestingServerResponse = UtilIT.setSetting(SettingsServiceBean.Key.OAIServerEnabled,"true");
    }

    @AfterClass
    public static void afterClass() {
	// disable harvesting server (default value)
	Response enableHarvestingServerResponse = UtilIT.setSetting(SettingsServiceBean.Key.OAIServerEnabled,"false");
    }

    private void setupUsers() {
        Response cu0 = UtilIT.createRandomUser();
        normalUserAPIKey = UtilIT.getApiTokenFromResponse(cu0);
        Response cu1 = UtilIT.createRandomUser();
        String un1 = UtilIT.getUsernameFromResponse(cu1);
        Response u1a = UtilIT.makeSuperUser(un1);
        adminUserAPIKey = UtilIT.getApiTokenFromResponse(cu1);
    }

    private String jsonForTestSpec(String name, String def) {
        String r = String.format("{\"name\":\"%s\",\"definition\":\"%s\"}", name, def);//description is optional
        return r;
    }
    
    private String jsonForEditSpec(String name, String def, String desc) {
        String r = String.format("{\"name\":\"%s\",\"definition\":\"%s\",\"description\":\"%s\"}", name, def, desc);
        return r;
    }

    private String normalUserAPIKey;
    private String adminUserAPIKey;

    @Test
    public void testSetCreation() {
        setupUsers();
        String setName = UtilIT.getRandomString(6);
        String def = "*";

        // make sure the set does not exist
        String u0 = String.format("/api/harvest/server/oaisets/%s", setName);
        String createPath ="/api/harvest/server/oaisets/add";
        Response r0 = given()
                .get(u0);
        assertEquals(404, r0.getStatusCode());

        // try to create set as normal user, should fail
        Response r1 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, normalUserAPIKey)
                .body(jsonForTestSpec(setName, def))
                .post(createPath);
        assertEquals(400, r1.getStatusCode());

        // try to create set as admin user, should succeed
        Response r2 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForTestSpec(setName, def))
                .post(createPath);
        assertEquals(201, r2.getStatusCode());
        
        Response getSet = given()
                .get(u0);
        
        logger.info("getSet.getStatusCode(): " + getSet.getStatusCode());
        logger.info("getSet printresponse:  " + getSet.prettyPrint());
        assertEquals(200, getSet.getStatusCode());
        
        Response responseAll = given()
                .get("/api/harvest/server/oaisets");
        
        logger.info("responseAll.getStatusCode(): " + responseAll.getStatusCode());
        logger.info("responseAll printresponse:  " + responseAll.prettyPrint());
        assertEquals(200, responseAll.getStatusCode());

        // try to create set with same name as admin user, should fail
        Response r3 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForTestSpec(setName, def))
                .post(createPath);
        assertEquals(400, r3.getStatusCode());

        // try to export set as admin user, should succeed (under admin API, not checking that normal user will fail)
        Response r4 = UtilIT.exportOaiSet(setName);
        assertEquals(200, r4.getStatusCode());
        
        // try to delete as normal user  should fail
        Response r5 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, normalUserAPIKey)
                .delete(u0);
        logger.info("r5.getStatusCode(): " + r5.getStatusCode());
        assertEquals(400, r5.getStatusCode());
        
        // try to delete as admin user  should work
        Response r6 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .delete(u0);
        logger.info("r6.getStatusCode(): " + r6.getStatusCode());
        assertEquals(200, r6.getStatusCode());

    }
    
    @Test
    public void testSetEdit() {
        setupUsers();
        String setName = UtilIT.getRandomString(6);
        String def = "*";

        // make sure the set does not exist
        String u0 = String.format("/api/harvest/server/oaisets/%s", setName);
        String createPath ="/api/harvest/server/oaisets/add";
        Response r0 = given()
                .get(u0);
        assertEquals(404, r0.getStatusCode());


        // try to create set as admin user, should succeed
        Response r1 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForTestSpec(setName, def))
                .post(createPath);
        assertEquals(201, r1.getStatusCode());

        
        // try to edit as normal user  should fail
        Response r2 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, normalUserAPIKey)
                .body(jsonForEditSpec(setName, def,""))
                .put(u0);
        logger.info("r2.getStatusCode(): " + r2.getStatusCode());
        assertEquals(400, r2.getStatusCode());
        
        // try to edit as with blanks should fail
        Response r3 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForEditSpec(setName, "",""))
                .put(u0);
        logger.info("r3.getStatusCode(): " + r3.getStatusCode());
        assertEquals(400, r3.getStatusCode());
        
        // try to edit as with something should pass
        Response r4 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForEditSpec(setName, "newDef","newDesc"))
                .put(u0);
        logger.info("r4 Status code: " + r4.getStatusCode());
        logger.info("r4.prettyPrint(): " + r4.prettyPrint());
        assertEquals(OK.getStatusCode(), r4.getStatusCode());
        
        logger.info("u0: " + u0);
        // now delete it...
        Response r6 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .delete(u0);
        logger.info("r6.getStatusCode(): " + r6.getStatusCode());
        assertEquals(200, r6.getStatusCode());

    }

    // A more elaborate test - we'll create and publish a dataset, then create an
    // OAI set with that one dataset, and attempt to retrieve the OAI record
    // with GetRecord. 
    @Test
    public void testOaiFunctionality() throws InterruptedException {

        setupUsers();

        // create dataverse:
        Response createDataverseResponse = UtilIT.createRandomDataverse(adminUserAPIKey);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // publish dataverse:
        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, adminUserAPIKey);
        assertEquals(OK.getStatusCode(), publishDataverse.getStatusCode());

        // create dataset: 
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, adminUserAPIKey);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        // retrieve the global id: 
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);

        // publish dataset:
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", adminUserAPIKey);
        assertEquals(200, publishDataset.getStatusCode());

        String identifier = datasetPersistentId.substring(datasetPersistentId.lastIndexOf('/') + 1);

        logger.info("identifier: " + identifier);

        // Let's try and create an OAI set with the dataset we have just 
        // created and published:
        // - however, publish command is executed asynchronously, i.e. it may 
        // still be running after we received the OK from the publish API. 
        // So let's give it a couple of extra seconds to finish, to make sure 
        // the dataset is published, exported and indexed - because the OAI
        // set create API requires all of the above.
        Thread.sleep(3000L);
        String setName = identifier;
        String setQuery = "dsPersistentId:" + identifier;
        String apiPath = String.format("/api/harvest/server/oaisets/%s", setName);
        String createPath ="/api/harvest/server/oaisets/add";
        Response createSetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForTestSpec(setName, setQuery))
                .post(createPath);
        assertEquals(201, createSetResponse.getStatusCode());

        // TODO: a) look up the set via native harvest/server api; 
        //       b) look up the set via the OAI ListSets;
        // export set: 
        // (this is asynchronous - so we should probably wait a little)
        Response exportSetResponse = UtilIT.exportOaiSet(setName);
        assertEquals(200, exportSetResponse.getStatusCode());
        //SEK 09/04/2019 resonable wait time for export OAI? #6128
        Thread.sleep(5000L);
        
        Response getSet = given()
                .get(apiPath);
        
        logger.info("getSet.getStatusCode(): " + getSet.getStatusCode());
        logger.info("getSet printresponse:  " + getSet.prettyPrint());
        assertEquals(200, getSet.getStatusCode());

        // Run ListIdentifiers on this newly-created set:
        Response listIdentifiersResponse = UtilIT.getOaiListIdentifiers(setName, "oai_dc");
        List ret = listIdentifiersResponse.getBody().xmlPath().getList("OAI-PMH.ListIdentifiers.header");

        assertEquals(OK.getStatusCode(), listIdentifiersResponse.getStatusCode());
        assertNotNull(ret);
        logger.info("setName: " + setName);
        logger.info("listIdentifiersResponse.prettyPrint:..... ");
        listIdentifiersResponse.prettyPrint();
        // There should be 1 and only 1 record in the response:
        assertEquals(1, ret.size());
        // And the record should be the dataset we have just created:
        assertEquals(datasetPersistentId, listIdentifiersResponse.getBody().xmlPath().getString("OAI-PMH.ListIdentifiers.header.identifier"));

        Response listRecordsResponse = UtilIT.getOaiListRecords(setName, "oai_dc");
        assertEquals(OK.getStatusCode(), listRecordsResponse.getStatusCode());
        List listRecords = listRecordsResponse.getBody().xmlPath().getList("OAI-PMH.ListRecords.record");

        assertNotNull(listRecords);
        assertEquals(1, listRecords.size());
        assertEquals(datasetPersistentId, listRecordsResponse.getBody().xmlPath().getString("OAI-PMH.ListRecords.record[0].header.identifier"));

        // assert that Datacite format does not contain the XML prolog
        Response listRecordsResponseDatacite = UtilIT.getOaiListRecords(setName, "Datacite");
        assertEquals(OK.getStatusCode(), listRecordsResponseDatacite.getStatusCode());
        String body = listRecordsResponseDatacite.getBody().asString();
        assertFalse(body.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));

        // And now run GetRecord on the OAI record for the dataset:
        Response getRecordResponse = UtilIT.getOaiRecord(datasetPersistentId, "oai_dc");

        assertEquals(datasetPersistentId, getRecordResponse.getBody().xmlPath().getString("OAI-PMH.GetRecord.record.header.identifier"));

        // TODO: 
        // check the actual metadata payload of the OAI record more carefully?
    }
}
