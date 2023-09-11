package edu.harvard.iq.dataverse.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.restassured.response.Response;
import io.restassured.path.xml.XmlPath;
import io.restassured.path.xml.element.Node;

import java.util.ArrayList;
import java.util.Collections;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private static List<String> extraDatasetsIdentifiers = new ArrayList<>();

    @BeforeAll
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

    @AfterAll
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
        
        // ... And let's create 4 more datasets for a multi-dataset experiment:
        
        for (int i = 0; i < 4; i++) {
            // create dataset: 
            createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, adminUserAPIKey);
            createDatasetResponse.prettyPrint();
            datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

            // retrieve the global id: 
            String thisDatasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);

            // publish dataset:
            publishDataset = UtilIT.publishDatasetViaNativeApi(thisDatasetPersistentId, "major", adminUserAPIKey);
            assertEquals(200, publishDataset.getStatusCode());

            UtilIT.sleepForReexport(thisDatasetPersistentId, adminUserAPIKey, 10);
            
            extraDatasetsIdentifiers.add(thisDatasetPersistentId.substring(thisDatasetPersistentId.lastIndexOf('/') + 1));
        }
        
        
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
        logger.info(verb+" response: "+oaiResponse.prettyPrint());
        // confirm that the response is in fact XML:
        XmlPath responseXmlPath = oaiResponse.getBody().xmlPath();
        assertNotNull(responseXmlPath);
        
        String dateString = responseXmlPath.getString("OAI-PMH.responseDate");
        assertNotNull(dateString); 
        // TODO: validate the formatting of the date string in the record
        // header, above. (could be slightly tricky - since this formatting
        // is likely locale-specific)
        logger.fine("date string from the OAI output:"+dateString);
        //assertEquals("http://localhost:8080/oai", responseXmlPath.getString("OAI-PMH.request"));
        assertEquals(verb, responseXmlPath.getString("OAI-PMH.request.@verb"));
        return responseXmlPath;
    }
    
    @Test 
    public void testOaiIdentify() {
        // Run Identify:
        Response identifyResponse = UtilIT.getOaiIdentify();
        assertEquals(OK.getStatusCode(), identifyResponse.getStatusCode());

        // Validate the response: 
        
        XmlPath responseXmlPath = validateOaiVerbResponse(identifyResponse, "Identify");
        //assertEquals("http://localhost:8080/oai", responseXmlPath.getString("OAI-PMH.Identify.baseURL"));
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
    public void testNativeSetAPI() {
        String setName = UtilIT.getRandomString(6);
        String def = "*";
        
        // This test focuses on the Create/List/Edit functionality of the 
        // Dataverse OAI Sets API (/api/harvest/server):
 
        // API Test 1. Make sure the set does not exist yet
        String setPath = String.format("/api/harvest/server/oaisets/%s", setName);
        String createPath ="/api/harvest/server/oaisets/add";
        Response getSetResponse = given()
                .get(setPath);
        assertEquals(404, getSetResponse.getStatusCode());

        // API Test 2. Try to create set as normal user, should fail
        Response createSetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, normalUserAPIKey)
                .body(jsonForTestSpec(setName, def))
                .post(createPath);
        assertEquals(400, createSetResponse.getStatusCode());

        // API Test 3. Try to create set as admin user, should succeed
        createSetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForTestSpec(setName, def))
                .post(createPath);
        assertEquals(201, createSetResponse.getStatusCode());
        
        // API Test 4. Retrieve the set we've just created, validate the response
        getSetResponse = given().get(setPath);
        
        System.out.println("getSetResponse.getStatusCode(): " + getSetResponse.getStatusCode());
        System.out.println("getSetResponse, full:  " + getSetResponse.prettyPrint());
        assertEquals(200, getSetResponse.getStatusCode());
        
        getSetResponse.then().assertThat()
                .body("status", equalTo(ApiConstants.STATUS_OK))
                .body("data.definition", equalTo("*"))
                .body("data.description", equalTo(""))
                .body("data.name", equalTo(setName));
        
        
        // API Test 5. Retrieve all sets, check that our new set is listed 
        Response responseAll = given()
                .get("/api/harvest/server/oaisets");
        
        System.out.println("responseAll.getStatusCode(): " + responseAll.getStatusCode());
        System.out.println("responseAll full:  " + responseAll.prettyPrint());
        assertEquals(200, responseAll.getStatusCode());
        assertTrue(responseAll.body().jsonPath().getList("data.oaisets").size() > 0);
        assertTrue(responseAll.body().jsonPath().getList("data.oaisets.name", String.class).contains(setName));
        
        // API Test 6. Try to create a set with the same name, should fail
        createSetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForTestSpec(setName, def))
                .post(createPath);
        assertEquals(400, createSetResponse.getStatusCode());

        // API Test 7. Try to export set as admin user, should succeed. Set export
        // is under /api/admin, no need to try to access it as a non-admin user
        Response r4 = UtilIT.exportOaiSet(setName);
        assertEquals(200, r4.getStatusCode());
                
        // API TEST 8. Try to delete the set as normal user, should fail
        Response deleteResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, normalUserAPIKey)
                .delete(setPath);
        logger.info("deleteResponse.getStatusCode(): " + deleteResponse.getStatusCode());
        assertEquals(400, deleteResponse.getStatusCode());
        
        // API TEST 9. Delete as admin user, should work
        deleteResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .delete(setPath);
        logger.info("deleteResponse.getStatusCode(): " + deleteResponse.getStatusCode());
        assertEquals(200, deleteResponse.getStatusCode());

    }
    
    @Test
    public void testSetEditAPIandOAIlistSets() {
        // This test focuses on testing the Edit functionality of the Dataverse
        // OAI Set API and the ListSets method of the Dataverse OAI server.
        
        // Initial setup: crete a test set. 
        // Since the Create and List (POST and GET) functionality of the API 
        // is tested extensively in the previous test, we will not be paying 
        // as much attention to these methods, aside from confirming the 
        // expected HTTP result codes. 
        
        String setName = UtilIT.getRandomString(6);
        String setDef = "*";

        // Make sure the set does not exist
        String setPath = String.format("/api/harvest/server/oaisets/%s", setName);
        String createPath ="/api/harvest/server/oaisets/add";
        Response getSetResponse = given()
                .get(setPath);
        assertEquals(404, getSetResponse.getStatusCode());


        // Create the set as admin user
        Response createSetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForTestSpec(setName, setDef))
                .post(createPath);
        assertEquals(201, createSetResponse.getStatusCode());

        // I. Test the Modify/Edit (POST method) functionality of the 
        // Dataverse OAI Sets API
        
        String newDefinition = "title:New";
        String newDescription = "updated";
        
        // API Test 1. Try to modify the set as normal user, should fail
        Response editSetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, normalUserAPIKey)
                .body(jsonForEditSpec(setName, setDef, ""))
                .put(setPath);
        logger.info("non-admin user editSetResponse.getStatusCode(): " + editSetResponse.getStatusCode());
        assertEquals(400, editSetResponse.getStatusCode());
        
        // API Test 2. Try to modify as admin, but with invalid (empty) values, 
        // should fail
        editSetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForEditSpec(setName, "",""))
                .put(setPath);
        logger.info("invalid values editSetResponse.getStatusCode(): " + editSetResponse.getStatusCode());
        assertEquals(400, editSetResponse.getStatusCode());
        
        // API Test 3. Try to modify as admin, with sensible values
        editSetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForEditSpec(setName, newDefinition, newDescription))
                .put(setPath);
        logger.info("admin user editSetResponse status code: " + editSetResponse.getStatusCode());
        logger.info("admin user editSetResponse.prettyPrint(): " + editSetResponse.prettyPrint());
        assertEquals(OK.getStatusCode(), editSetResponse.getStatusCode());
        
        // API Test 4. List the set, confirm that the new values are shown
        getSetResponse = given().get(setPath);
        
        System.out.println("getSetResponse.getStatusCode(): " + getSetResponse.getStatusCode());
        System.out.println("getSetResponse, full:  " + getSetResponse.prettyPrint());
        assertEquals(200, getSetResponse.getStatusCode());
        
        getSetResponse.then().assertThat()
                .body("status", equalTo(ApiConstants.STATUS_OK))
                .body("data.definition", equalTo(newDefinition))
                .body("data.description", equalTo(newDescription))
                .body("data.name", equalTo(setName));

        // II. Test the ListSets functionality of the OAI server 
        
        Response listSetsResponse = UtilIT.getOaiListSets();
        
        // 1. Validate the service section of the OAI response: 
        
        XmlPath responseXmlPath = validateOaiVerbResponse(listSetsResponse, "ListSets");
        
        // 2. Validate the payload of the response, by confirming that the set 
        // we created and modified, above, is being listed by the OAI server 
        // and its xml record is properly formatted
        
        List<Node> listSets = responseXmlPath.getList("OAI-PMH.ListSets.set.list().findAll{it.setName=='"+setName+"'}", Node.class);
        
        // 2a. Confirm that our set is listed:
        assertNotNull(listSets, "Unexpected response from ListSets");
        assertEquals(1, listSets.size(), "Newly-created set isn't properly listed by the OAI server");
        // 2b. Confirm that the set entry contains the updated description: 
        assertEquals(newDescription, listSets.get(0).getPath("setDescription.metadata.element.field", String.class), "Incorrect description in the ListSets entry");
        
        // ok, the xml record looks good! 

        // Cleanup. Delete the set with the DELETE API
        Response deleteSetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .delete(setPath);
        assertEquals(200, deleteSetResponse.getStatusCode());

    }

    // A more elaborate test - we will create and export an 
    // OAI set with a single dataset, and attempt to retrieve 
    // it and validate the OAI server responses of the corresponding 
    // ListIdentifiers, ListRecords and GetRecord methods. 
    @Test
    public void testSingleRecordOaiSet() throws InterruptedException {
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

        // The GET method of the oai set API, as well as the OAI ListSets
        // method are tested extensively in another method in this class, so 
        // we'll skip looking too closely into those here. 
        
        // A quick test that the new set is listed under native API
        Response getSet = given()
                .get(apiPath);
        assertEquals(200, getSet.getStatusCode());
        
        // Export the set. 
        
        Response exportSetResponse = UtilIT.exportOaiSet(setName);
        assertEquals(200, exportSetResponse.getStatusCode());
                
        // Strictly speaking, exporting an OAI set is an asynchronous operation. 
        // So the code below was written to expect to have to wait for up to 10 
        // additional seconds for it to complete. In retrospect, this is 
        // most likely unnecessary (because the only potentially expensive part 
        // of the process is the metadata export, and in this case that must have
        // already happened - when the dataset was published (that operation
        // now has its own wait mechanism). But I'll keep this extra code in 
        // place since it's not going to hurt. - L.A. 
        
        Thread.sleep(1000L); // initial sleep interval
        int i = 0;
        int maxWait=10;
        do {

            // OAI Test 1. Run ListIdentifiers on this newly-created set:
            Response listIdentifiersResponse = UtilIT.getOaiListIdentifiers(setName, "oai_dc");
            assertEquals(OK.getStatusCode(), listIdentifiersResponse.getStatusCode());
            
            // Validate the service section of the OAI response: 
            XmlPath responseXmlPath = validateOaiVerbResponse(listIdentifiersResponse, "ListIdentifiers");
            
            List ret = responseXmlPath.getList("OAI-PMH.ListIdentifiers.header");
                        
            if (ret == null || ret.isEmpty()) {
                // OK, we'll sleep for another second
                i++;
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.info("listIdentifiersResponse.prettyPrint: " + listIdentifiersResponse.prettyPrint());
                }
                // Validate the payload of the ListIdentifiers response:
                // a) There should be 1 and only 1 item listed:
                assertEquals(1, ret.size());
                // b) The one record in it should be the dataset we have just created:
                assertEquals(singleSetDatasetPersistentId, responseXmlPath
                        .getString("OAI-PMH.ListIdentifiers.header.identifier"));
                assertEquals(setName, responseXmlPath
                        .getString("OAI-PMH.ListIdentifiers.header.setSpec"));
                assertNotNull(responseXmlPath.getString("OAI-PMH.ListIdentifiers.header.dateStamp"));
                // TODO: validate the formatting of the date string here as well.
                
                // ok, ListIdentifiers response looks valid.
                break;
            }
            Thread.sleep(1000L);
        } while (i<maxWait); 
        
        System.out.println("Waited " + i + " seconds for OIA export.");
        //Fail if we didn't find the exported record before the timeout
        assertTrue(i < maxWait);
        
        
        // OAI Test 2. Run ListRecords, request oai_dc:
        Response listRecordsResponse = UtilIT.getOaiListRecords(setName, "oai_dc");
        assertEquals(OK.getStatusCode(), listRecordsResponse.getStatusCode());
        
        // Validate the service section of the OAI response: 
        
        XmlPath responseXmlPath = validateOaiVerbResponse(listRecordsResponse, "ListRecords");
        
        // Validate the payload of the response: 
        // (the header portion must be identical to that of ListIdentifiers above, 
        // plus the response must contain a metadata section with a valid oai_dc 
        // record)
        
        List listRecords = responseXmlPath.getList("OAI-PMH.ListRecords.record");

        // Same deal, there must be 1 record only in the set:
        assertNotNull(listRecords);
        assertEquals(1, listRecords.size());
        // a) header section:
        assertEquals(singleSetDatasetPersistentId, responseXmlPath.getString("OAI-PMH.ListRecords.record.header.identifier"));
        assertEquals(setName, responseXmlPath
                .getString("OAI-PMH.ListRecords.record.header.setSpec"));
        assertNotNull(responseXmlPath.getString("OAI-PMH.ListRecords.record.header.dateStamp"));
        // b) metadata section: 
        // in the metadata section we are showing the resolver url form of the doi:
        String persistentIdUrl = singleSetDatasetPersistentId.replace("doi:", "https://doi.org/");
        assertEquals(persistentIdUrl, responseXmlPath.getString("OAI-PMH.ListRecords.record.metadata.dc.identifier"));
        assertEquals("Darwin's Finches", responseXmlPath.getString("OAI-PMH.ListRecords.record.metadata.dc.title"));
        assertEquals("Finch, Fiona", responseXmlPath.getString("OAI-PMH.ListRecords.record.metadata.dc.creator"));        
        assertEquals("Darwin's finches (also known as the Galápagos finches) are a group of about fifteen species of passerine birds.", 
                responseXmlPath.getString("OAI-PMH.ListRecords.record.metadata.dc.description"));
        assertEquals("Medicine, Health and Life Sciences", 
                responseXmlPath.getString("OAI-PMH.ListRecords.record.metadata.dc.subject"));
        // ok, looks legit!
        
        // OAI Test 3.
        // Assert that Datacite format does not contain the XML prolog
        // (this is a reference to a resolved issue; generally, harvestable XML
        // exports must NOT contain the "<?xml ..." headers - but there is now
        // efficient code in the XOAI library that checks for, and strips it, 
        // if necessary. - L.A.)
        Response listRecordsResponseDatacite = UtilIT.getOaiListRecords(setName, "Datacite");
        assertEquals(OK.getStatusCode(), listRecordsResponseDatacite.getStatusCode());
        String body = listRecordsResponseDatacite.getBody().asString();
        assertFalse(body.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));

        // OAI Test 4. run and validate GetRecord response
        
        Response getRecordResponse = UtilIT.getOaiRecord(singleSetDatasetPersistentId, "oai_dc");
        System.out.println("GetRecord response in its entirety: "+getRecordResponse.getBody().prettyPrint());
         
        // Validate the service section of the OAI response: 
        responseXmlPath = validateOaiVerbResponse(getRecordResponse, "GetRecord");
        
        // Validate the payload of the response:
        
        // Note that for a set with a single record the output of ListRecrods is
        // essentially identical to that of GetRecord!
        // (we'll test a multi-record set in a different method)
        // a) header section:
        assertEquals(singleSetDatasetPersistentId, responseXmlPath.getString("OAI-PMH.GetRecord.record.header.identifier"));
        assertEquals(setName, responseXmlPath
                .getString("OAI-PMH.GetRecord.record.header.setSpec"));
        assertNotNull(responseXmlPath.getString("OAI-PMH.GetRecord.record.header.dateStamp"));
        // b) metadata section: 
        assertEquals(persistentIdUrl, responseXmlPath.getString("OAI-PMH.GetRecord.record.metadata.dc.identifier"));
        assertEquals("Darwin's Finches", responseXmlPath.getString("OAI-PMH.GetRecord.record.metadata.dc.title"));
        assertEquals("Finch, Fiona", responseXmlPath.getString("OAI-PMH.GetRecord.record.metadata.dc.creator"));        
        assertEquals("Darwin's finches (also known as the Galápagos finches) are a group of about fifteen species of passerine birds.", 
                responseXmlPath.getString("OAI-PMH.GetRecord.record.metadata.dc.description"));
        assertEquals("Medicine, Health and Life Sciences", responseXmlPath.getString("OAI-PMH.GetRecord.record.metadata.dc.subject"));
        
        // ok, looks legit!

    }
    
    // This test will attempt to create a set with multiple records (enough 
    // to trigger a paged respons) and test the resumption token functionality). 
    // Note that this test requires the OAI service to be configured with some
    // non-default settings (the paging limits for ListIdentifiers and ListRecords
    // must be set to 2, in order to be able to trigger this paging behavior without
    // having to create and export too many datasets).
    // So you will need to do this:
    //    asadmin create-jvm-options "-Ddataverse.oai.server.maxidentifiers=2"
    //    asadmin create-jvm-options "-Ddataverse.oai.server.maxrecords=2"
    
    
    @Test
    public void testMultiRecordOaiSet() throws InterruptedException {
        // Setup: Let's create a control OAI set with the 5 datasets created 
        // in the class init: 

        String setName = UtilIT.getRandomString(6);
        String setQuery = "(dsPersistentId:" + singleSetDatasetIdentifier;
        for (String persistentId : extraDatasetsIdentifiers) {
            setQuery = setQuery.concat(" OR dsPersistentId:" + persistentId);
        }
        setQuery = setQuery.concat(")");

        String createPath = "/api/harvest/server/oaisets/add";

        Response createSetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForTestSpec(setName, setQuery))
                .post(createPath);
        assertEquals(201, createSetResponse.getStatusCode());

        // Dataverse OAI Sets API is tested extensively in other methods here, 
        // so no need to test in any more details than confirming the OK result
        // above 
        Response exportSetResponse = UtilIT.exportOaiSet(setName);
        assertEquals(200, exportSetResponse.getStatusCode());
        Thread.sleep(1000L);

        // OAI Test 1. Run ListIdentifiers on the set we've just created:
        Response listIdentifiersResponse = UtilIT.getOaiListIdentifiers(setName, "oai_dc");
        assertEquals(OK.getStatusCode(), listIdentifiersResponse.getStatusCode());

        // Validate the service section of the OAI response: 
        XmlPath responseXmlPath = validateOaiVerbResponse(listIdentifiersResponse, "ListIdentifiers");

        List<String> ret = responseXmlPath.getList("OAI-PMH.ListIdentifiers.header.identifier");
        assertNotNull(ret);

        if (logger.isLoggable(Level.FINE)) {
            logger.info("listIdentifiersResponse.prettyPrint: "+listIdentifiersResponse.prettyPrint());
        }

        // Validate the payload of the ListIdentifiers response:
        // 1a) There should be 2 items listed:
        assertEquals(2, ret.size(), "Wrong number of items on the first ListIdentifiers page");
        
        // 1b) The response contains a resumptionToken for the next page of items:
        String resumptionToken = responseXmlPath.getString("OAI-PMH.ListIdentifiers.resumptionToken");
        assertNotNull(resumptionToken, "No resumption token in the ListIdentifiers response (has the jvm option dataverse.oai.server.maxidentifiers been configured?)");
        
        // 1c) The total number of items in the set (5) is listed correctly:
        assertEquals(5, responseXmlPath.getInt("OAI-PMH.ListIdentifiers.resumptionToken.@completeListSize"));
        
        // 1d) ... and the offset (cursor) is at the right position (0): 
        assertEquals(0, responseXmlPath.getInt("OAI-PMH.ListIdentifiers.resumptionToken.@cursor"));

        // The formatting of individual item records in the ListIdentifiers response
        // is tested extensively in the previous test method, so we are not 
        // looking at them in such detail here; but we should record the 
        // identifiers listed, so that we can confirm that all the set is 
        // served as expected. 
        
        Set<String> persistentIdsInListIdentifiers = new HashSet<>();
        
        for (String persistentId : ret) {
            persistentIdsInListIdentifiers.add(persistentId.substring(persistentId.lastIndexOf('/') + 1));
        }

        // ok, let's move on to the next ListIdentifiers page: 
        // (we repeat the exact same checks as the above; minus the different
        // expected offset)
        
        // OAI Test 2. Run ListIdentifiers with the resumptionToken obtained 
        // in the previous step:
        
        listIdentifiersResponse = UtilIT.getOaiListIdentifiersWithResumptionToken(resumptionToken);
        assertEquals(OK.getStatusCode(), listIdentifiersResponse.getStatusCode());

        // Validate the service section of the OAI response: 
        responseXmlPath = validateOaiVerbResponse(listIdentifiersResponse, "ListIdentifiers");

        ret = responseXmlPath.getList("OAI-PMH.ListIdentifiers.header.identifier");
        assertNotNull(ret);

        if (logger.isLoggable(Level.FINE)) {
            logger.info("listIdentifiersResponse.prettyPrint: "+listIdentifiersResponse.prettyPrint());
        }
        
        // Validate the payload of the ListIdentifiers response:
        // 2a) There should still be 2 items listed:
        assertEquals(2, ret.size(), "Wrong number of items on the second ListIdentifiers page");
        
        // 2b) The response should contain a resumptionToken for the next page of items:
        resumptionToken = responseXmlPath.getString("OAI-PMH.ListIdentifiers.resumptionToken");
        assertNotNull(resumptionToken, "No resumption token in the ListIdentifiers response");
        
        // 2c) The total number of items in the set (5) is listed correctly:
        assertEquals(5, responseXmlPath.getInt("OAI-PMH.ListIdentifiers.resumptionToken.@completeListSize"));
        
        // 2d) ... and the offset (cursor) is at the right position (2): 
        assertEquals(2, responseXmlPath.getInt("OAI-PMH.ListIdentifiers.resumptionToken.@cursor"));
        
        // Record the identifiers listed on this results page:
        
        for (String persistentId : ret) {
            persistentIdsInListIdentifiers.add(persistentId.substring(persistentId.lastIndexOf('/') + 1));
        }
        
        // And now the next and the final ListIdentifiers page.
        // This time around we should get an *empty* resumptionToken (indicating
        // that there are no more results):
        
        // OAI Test 3. Run ListIdentifiers with the final resumptionToken
        
        listIdentifiersResponse = UtilIT.getOaiListIdentifiersWithResumptionToken(resumptionToken);
        assertEquals(OK.getStatusCode(), listIdentifiersResponse.getStatusCode());

        // Validate the service section of the OAI response: 
        responseXmlPath = validateOaiVerbResponse(listIdentifiersResponse, "ListIdentifiers");

        ret = responseXmlPath.getList("OAI-PMH.ListIdentifiers.header.identifier");
        assertNotNull(ret);

        if (logger.isLoggable(Level.FINE)) {
            logger.info("listIdentifiersResponse.prettyPrint: "+listIdentifiersResponse.prettyPrint());
        }
        
        // Validate the payload of the ListIdentifiers response:
        // 3a) There should be only 1 item listed:
        assertEquals(1, ret.size(), "Wrong number of items on the final ListIdentifiers page");
        
        // 3b) The response contains a resumptionToken for the next page of items:
        resumptionToken = responseXmlPath.getString("OAI-PMH.ListIdentifiers.resumptionToken");
        assertNotNull(resumptionToken, "No resumption token in the final ListIdentifiers response");
        assertEquals("", resumptionToken, "Non-empty resumption token in the final ListIdentifiers response");
        
        // 3c) The total number of items in the set (5) is still listed correctly:
        assertEquals(5, responseXmlPath.getInt("OAI-PMH.ListIdentifiers.resumptionToken.@completeListSize"));
        
        // 3d) ... and the offset (cursor) is at the right position (4): 
        assertEquals(4, responseXmlPath.getInt("OAI-PMH.ListIdentifiers.resumptionToken.@cursor"));
        
        // Record the last identifier listed on this final page:
        persistentIdsInListIdentifiers.add(ret.get(0).substring(ret.get(0).lastIndexOf('/') + 1));
        
        // Finally, let's confirm that the expected 5 datasets have been listed
        // as part of this Set: 
        
        boolean allDatasetsListed = true; 
        
        allDatasetsListed = persistentIdsInListIdentifiers.contains(singleSetDatasetIdentifier);
        for (String persistentId : extraDatasetsIdentifiers) {
            allDatasetsListed = allDatasetsListed && persistentIdsInListIdentifiers.contains(persistentId); 
        }
        
        assertTrue(allDatasetsListed, "Control datasets not properly listed in the paged ListIdentifiers response");
        
        // OK, it is safe to assume ListIdentifiers works as it should in page mode.
        
        // We will now repeat the exact same tests for ListRecords (again, no 
        // need to pay close attention to the formatting of the individual records, 
        // since that's tested in the previous test method, since our focus is
        // testing the paging/resumptionToken functionality)
        
        // OAI Test 4. Run ListRecords on the set we've just created:
        Response listRecordsResponse = UtilIT.getOaiListRecords(setName, "oai_dc");
        assertEquals(OK.getStatusCode(), listRecordsResponse.getStatusCode());

        // Validate the service section of the OAI response: 
        responseXmlPath = validateOaiVerbResponse(listRecordsResponse, "ListRecords");

        ret = responseXmlPath.getList("OAI-PMH.ListRecords.record.header.identifier");
        assertNotNull(ret);

        if (logger.isLoggable(Level.FINE)) {
            logger.info("listRecordsResponse.prettyPrint: "+listRecordsResponse.prettyPrint());
        }
        
        // Validate the payload of the ListRecords response:
        // 4a) There should be 2 items listed:
        assertEquals(2, ret.size(), "Wrong number of items on the first ListRecords page");
        
        // 4b) The response contains a resumptionToken for the next page of items:
        resumptionToken = responseXmlPath.getString("OAI-PMH.ListRecords.resumptionToken");
        assertNotNull(resumptionToken, "No resumption token in the ListRecords response (has the jvm option dataverse.oai.server.maxrecords been configured?)");
        
        // 4c) The total number of items in the set (5) is listed correctly:
        assertEquals(5, responseXmlPath.getInt("OAI-PMH.ListRecords.resumptionToken.@completeListSize"));
        
        // 4d) ... and the offset (cursor) is at the right position (0): 
        assertEquals(0, responseXmlPath.getInt("OAI-PMH.ListRecords.resumptionToken.@cursor"));
        
        Set<String> persistentIdsInListRecords = new HashSet<>();
        
        for (String persistentId : ret) {
            persistentIdsInListRecords.add(persistentId.substring(persistentId.lastIndexOf('/') + 1));
        }

        // ok, let's move on to the next ListRecords page: 
        // (we repeat the exact same checks as the above; minus the different
        // expected offset)
        
        // OAI Test 5. Run ListRecords with the resumptionToken obtained 
        // in the previous step:
        
        listRecordsResponse = UtilIT.getOaiListRecordsWithResumptionToken(resumptionToken);
        assertEquals(OK.getStatusCode(), listRecordsResponse.getStatusCode());

        // Validate the service section of the OAI response: 
        responseXmlPath = validateOaiVerbResponse(listRecordsResponse, "ListRecords");

        ret = responseXmlPath.getList("OAI-PMH.ListRecords.record.header.identifier");
        assertNotNull(ret);

        if (logger.isLoggable(Level.FINE)) {
            logger.info("listRecordsResponse.prettyPrint: "+listRecordsResponse.prettyPrint());
        }
        
        // Validate the payload of the ListRecords response:
        // 4a) There should still be 2 items listed:
        assertEquals(2, ret.size(), "Wrong number of items on the second ListRecords page");
        
        // 4b) The response should contain a resumptionToken for the next page of items:
        resumptionToken = responseXmlPath.getString("OAI-PMH.ListRecords.resumptionToken");
        assertNotNull(resumptionToken, "No resumption token in the ListRecords response");
        
        // 4c) The total number of items in the set (5) is listed correctly:
        assertEquals(5, responseXmlPath.getInt("OAI-PMH.ListRecords.resumptionToken.@completeListSize"));
        
        // 4d) ... and the offset (cursor) is at the right position (2): 
        assertEquals(2, responseXmlPath.getInt("OAI-PMH.ListRecords.resumptionToken.@cursor"));
        
        // Record the identifiers listed on this results page:
        
        for (String persistentId : ret) {
            persistentIdsInListRecords.add(persistentId.substring(persistentId.lastIndexOf('/') + 1));
        }
        
        // And now the next and the final ListRecords page.
        // This time around we should get an *empty* resumptionToken (indicating
        // that there are no more results):
        
        // OAI Test 6. Run ListRecords with the final resumptionToken
        
        listRecordsResponse = UtilIT.getOaiListRecordsWithResumptionToken(resumptionToken);
        assertEquals(OK.getStatusCode(), listRecordsResponse.getStatusCode());

        // Validate the service section of the OAI response: 
        responseXmlPath = validateOaiVerbResponse(listRecordsResponse, "ListRecords");

        ret = responseXmlPath.getList("OAI-PMH.ListRecords.record.header.identifier");
        assertNotNull(ret);

        if (logger.isLoggable(Level.FINE)) {
            logger.info("listRecordsResponse.prettyPrint: "+listRecordsResponse.prettyPrint());
        }
        
        // Validate the payload of the ListRecords response:
        // 6a) There should be only 1 item listed:
        assertEquals(1, ret.size(), "Wrong number of items on the final ListRecords page");
        
        // 6b) The response contains a resumptionToken for the next page of items:
        resumptionToken = responseXmlPath.getString("OAI-PMH.ListRecords.resumptionToken");
        assertNotNull(resumptionToken, "No resumption token in the final ListRecords response");
        assertEquals("", resumptionToken, "Non-empty resumption token in the final ListRecords response");
        
        // 6c) The total number of items in the set (5) is still listed correctly:
        assertEquals(5, responseXmlPath.getInt("OAI-PMH.ListRecords.resumptionToken.@completeListSize"));
        
        // 6d) ... and the offset (cursor) is at the right position (4): 
        assertEquals(4, responseXmlPath.getInt("OAI-PMH.ListRecords.resumptionToken.@cursor"));
        
        // Record the last identifier listed on this final page:
        persistentIdsInListRecords.add(ret.get(0).substring(ret.get(0).lastIndexOf('/') + 1));
        
        // Finally, let's confirm that the expected 5 datasets have been listed
        // as part of this Set: 
        
        allDatasetsListed = true; 
        
        allDatasetsListed = persistentIdsInListRecords.contains(singleSetDatasetIdentifier);
        for (String persistentId : extraDatasetsIdentifiers) {
            allDatasetsListed = allDatasetsListed && persistentIdsInListRecords.contains(persistentId); 
        }
        
        assertTrue(allDatasetsListed, "Control datasets not properly listed in the paged ListRecords response");
        
        // OK, it is safe to assume ListRecords works as it should in page mode
        // as well. 
        
        // And finally, let's delete the set
        String setPath = String.format("/api/harvest/server/oaisets/%s", setName);
        Response deleteResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .delete(setPath);
        logger.info("deleteResponse.getStatusCode(): " + deleteResponse.getStatusCode());
        assertEquals(200, deleteResponse.getStatusCode(), "Failed to delete the control multi-record set");
    }
    
    // TODO: 
    // What else can we test? 
    // Some ideas: 
    // - Test handling of deleted dataset records
    // - Test "from" and "until" time parameters
    // - Validate full verb response records against XML schema
    //   (for each supported metadata format, possibly?)
}
