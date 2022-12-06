package edu.harvard.iq.dataverse.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.path.xml.element.Node;
import static edu.harvard.iq.dataverse.api.UtilIT.API_TOKEN_HTTP_HEADER;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import static org.junit.Assert.assertTrue;

/**
 * Tests for the Harvesting Server functionality
 * Note that these test BOTH the proprietary Dataverse rest APIs for creating 
 * and managing sets, AND the OAI-PMH functionality itself.
 */
public class HarvestingServerIT {

    private static final Logger logger = Logger.getLogger(HarvestingServerIT.class.getCanonicalName());

    private static String normalUserAPIKey;
    private static String adminUserAPIKey;
    private static String singleSetDatasetIdentifier;
    private static String singleSetDatasetPersistentId;

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
	// enable harvesting server
	//  Gave some thought to storing the original response, and resetting afterwards - but that appears to be more complexity than it's worth
	Response enableHarvestingServerResponse = UtilIT.setSetting(SettingsServiceBean.Key.OAIServerEnabled,"true");
        
        // Create users:
        setupUsers();
        
        // Create and publish some datasets: 
        setupDatasets();
        
    }

    @AfterClass
    public static void afterClass() {
	// disable harvesting server (default value)
	Response enableHarvestingServerResponse = UtilIT.setSetting(SettingsServiceBean.Key.OAIServerEnabled,"false");
    }

    private static void setupUsers() {
        Response cu0 = UtilIT.createRandomUser();
        normalUserAPIKey = UtilIT.getApiTokenFromResponse(cu0);
        Response cu1 = UtilIT.createRandomUser();
        String un1 = UtilIT.getUsernameFromResponse(cu1);
        Response u1a = UtilIT.makeSuperUser(un1);
        adminUserAPIKey = UtilIT.getApiTokenFromResponse(cu1);
    }
    
    private static void setupDatasets() {
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
        singleSetDatasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);

        // publish dataset:
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(singleSetDatasetPersistentId, "major", adminUserAPIKey);
        assertEquals(200, publishDataset.getStatusCode());

        singleSetDatasetIdentifier = singleSetDatasetPersistentId.substring(singleSetDatasetPersistentId.lastIndexOf('/') + 1);

        logger.info("identifier: " + singleSetDatasetIdentifier);
        
        // Publish command is executed asynchronously, i.e. it may 
        // still be running after we received the OK from the publish API. 
        // The oaiExport step also requires the metadata exports to be done and this
        // takes longer than just publish/reindex.
        // So wait for all of this to finish.
        UtilIT.sleepForReexport(singleSetDatasetPersistentId, adminUserAPIKey, 10);
    }

    private String jsonForTestSpec(String name, String def) {
        String r = String.format("{\"name\":\"%s\",\"definition\":\"%s\"}", name, def);//description is optional
        return r;
    }
    
    private String jsonForEditSpec(String name, String def, String desc) {
        String r = String.format("{\"name\":\"%s\",\"definition\":\"%s\",\"description\":\"%s\"}", name, def, desc);
        return r;
    }

    private XmlPath validateOaiVerbResponse(Response oaiResponse, String verb) {
        // confirm that the response is in fact XML:
        XmlPath responseXmlPath = oaiResponse.getBody().xmlPath();
        assertNotNull(responseXmlPath);
        
        String dateString = responseXmlPath.getString("OAI-PMH.responseDate");
        assertNotNull(dateString); // TODO: validate that it's well-formatted!
        logger.info("date string from the OAI output:"+dateString);
        assertEquals("http://localhost:8080/oai", responseXmlPath.getString("OAI-PMH.request"));
        assertEquals(verb, responseXmlPath.getString("OAI-PMH.request.@verb"));
        return responseXmlPath;
    }
    
    @Test 
    public void testOaiIdentify() {
        // Run Identify:
        Response identifyResponse = UtilIT.getOaiIdentify();
        assertEquals(OK.getStatusCode(), identifyResponse.getStatusCode());
        //logger.info("Identify response: "+identifyResponse.prettyPrint());

        // Validate the response: 
        
        XmlPath responseXmlPath = validateOaiVerbResponse(identifyResponse, "Identify");
        assertEquals("http://localhost:8080/oai", responseXmlPath.getString("OAI-PMH.Identify.baseURL"));
        // Confirm that the server is reporting the correct parameters that 
        // our server implementation should be using:
        assertEquals("2.0", responseXmlPath.getString("OAI-PMH.Identify.protocolVersion"));
        assertEquals("transient", responseXmlPath.getString("OAI-PMH.Identify.deletedRecord"));
        assertEquals("YYYY-MM-DDThh:mm:ssZ", responseXmlPath.getString("OAI-PMH.Identify.granularity"));
    }
    
    @Test
    public void testOaiListMetadataFormats() {
        // Run ListMeatadataFormats:
        Response listFormatsResponse = UtilIT.getOaiListMetadataFormats();
        assertEquals(OK.getStatusCode(), listFormatsResponse.getStatusCode());
        //logger.info("ListMetadataFormats response: "+listFormatsResponse.prettyPrint());

        // Validate the response: 
        
        XmlPath responseXmlPath = validateOaiVerbResponse(listFormatsResponse, "ListMetadataFormats");
        
        // Check the payload of the response atgainst the list of metadata formats
        // we are currently offering under OAI; will need to be explicitly 
        // modified if/when we add more harvestable formats.
        
        List listFormats = responseXmlPath.getList("OAI-PMH.ListMetadataFormats.metadataFormat");

        assertNotNull(listFormats);
        assertEquals(5, listFormats.size());
        
        // The metadata formats are reported in an unpredictable ordder. We
        // want to sort the prefix names for comparison purposes, and for that 
        // they need to be saved in a modifiable list: 
        List<String> metadataPrefixes = new ArrayList<>(); 
        
        for (int i = 0; i < listFormats.size(); i++) {
            metadataPrefixes.add(responseXmlPath.getString("OAI-PMH.ListMetadataFormats.metadataFormat["+i+"].metadataPrefix"));
        }
        Collections.sort(metadataPrefixes);
        
        assertEquals("[Datacite, dataverse_json, oai_datacite, oai_dc, oai_ddi]", metadataPrefixes.toString());
        

    }
    
    
    @Test
    public void testSetCreateAPIandOAIlistIdentifiers() {
        // Create the set with Dataverse /api/harvest/server API:
        String setName = UtilIT.getRandomString(6);
        String def = "*";

        // make sure the set does not exist
        String setPath = String.format("/api/harvest/server/oaisets/%s", setName);
        String createPath ="/api/harvest/server/oaisets/add";
        Response r0 = given()
                .get(setPath);
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
                .get(setPath);
        
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
        
        
        
        // try to delete as normal user, should fail
        Response r5 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, normalUserAPIKey)
                .delete(setPath);
        logger.info("r5.getStatusCode(): " + r5.getStatusCode());
        assertEquals(400, r5.getStatusCode());
        
        // try to delete as admin user, should work
        Response r6 = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .delete(setPath);
        logger.info("r6.getStatusCode(): " + r6.getStatusCode());
        assertEquals(200, r6.getStatusCode());

    }
    
    @Test
    public void testSetEdit() {
        //setupUsers();
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
    public void testSingleRecordOaiSet() throws InterruptedException {

        //setupUsers();

        

        // Let's try and create an OAI set with the "single set dataset" that 
        // was created as part of the initial setup:
        
        String setName = singleSetDatasetIdentifier;
        String setQuery = "dsPersistentId:" + singleSetDatasetIdentifier;
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
        Response getSet = given()
                .get(apiPath);
        
        logger.info("getSet.getStatusCode(): " + getSet.getStatusCode());
        logger.fine("getSet printresponse:  " + getSet.prettyPrint());
        assertEquals(200, getSet.getStatusCode());
        int i = 0;
        int maxWait=10;
        do {
            

            // Run ListIdentifiers on this newly-created set:
            Response listIdentifiersResponse = UtilIT.getOaiListIdentifiers(setName, "oai_dc");
            List ret = listIdentifiersResponse.getBody().xmlPath().getList("OAI-PMH.ListIdentifiers.header");

            assertEquals(OK.getStatusCode(), listIdentifiersResponse.getStatusCode());
            assertNotNull(ret);
            logger.info("setName: " + setName);
            if (logger.isLoggable(Level.FINE)) {
                logger.info("listIdentifiersResponse.prettyPrint:..... ");
                listIdentifiersResponse.prettyPrint();
            }
            if (ret.size() != 1) {
                i++;
            } else {
                // There should be 1 and only 1 record in the response:
                assertEquals(1, ret.size());
                // And the record should be the dataset we have just created:
                assertEquals(singleSetDatasetPersistentId, listIdentifiersResponse.getBody().xmlPath()
                        .getString("OAI-PMH.ListIdentifiers.header.identifier"));
                break;
            }
            Thread.sleep(1000L);
        } while (i<maxWait); 
        // OK, the code above that expects to have to wait for up to 10 seconds 
        // for the set to export is most likely utterly unnecessary (the potentially
        // expensive part of the operation - exporting the metadata of our dataset -
        // already happened during its publishing (we made sure to wait there). 
        // Exporting the set should not take any time - but I'll keep that code 
        // in place since it's not going to hurt. - L.A. 
        System.out.println("Waited " + i + " seconds for OIA export.");
        //Fail if we didn't find the exported record before the timeout
        assertTrue(i < maxWait);
        Response listRecordsResponse = UtilIT.getOaiListRecords(setName, "oai_dc");
        assertEquals(OK.getStatusCode(), listRecordsResponse.getStatusCode());
        List listRecords = listRecordsResponse.getBody().xmlPath().getList("OAI-PMH.ListRecords.record");

        assertNotNull(listRecords);
        assertEquals(1, listRecords.size());
        assertEquals(singleSetDatasetPersistentId, listRecordsResponse.getBody().xmlPath().getString("OAI-PMH.ListRecords.record[0].header.identifier"));

        // assert that Datacite format does not contain the XML prolog
        Response listRecordsResponseDatacite = UtilIT.getOaiListRecords(setName, "Datacite");
        assertEquals(OK.getStatusCode(), listRecordsResponseDatacite.getStatusCode());
        String body = listRecordsResponseDatacite.getBody().asString();
        assertFalse(body.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));

        // And now run GetRecord on the OAI record for the dataset:
        Response getRecordResponse = UtilIT.getOaiRecord(singleSetDatasetPersistentId, "oai_dc");
        
        System.out.println("GetRecord response in its entirety: "+getRecordResponse.getBody().prettyPrint());
        System.out.println("one more time:");
        getRecordResponse.prettyPrint();
        
        assertEquals(singleSetDatasetPersistentId, getRecordResponse.getBody().xmlPath().getString("OAI-PMH.GetRecord.record.header.identifier"));

        // TODO: 
        // check the actual metadata payload of the OAI record more carefully?
    }
    
    // This test will attempt to create a set with multiple records (enough 
    // to trigger a paged response with a continuation token) and test its
    // performance. 
    
    
    @Test
    public void testMultiRecordOaiSet() throws InterruptedException {
        
    }
}
