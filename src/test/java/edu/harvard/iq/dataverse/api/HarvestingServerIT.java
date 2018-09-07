package edu.harvard.iq.dataverse.api;

import java.util.logging.Logger;
import com.jayway.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import static junit.framework.Assert.assertEquals;

import com.jayway.restassured.response.Response;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.path.json.JsonPath;
import javax.json.Json;
import javax.json.JsonArray;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.Ignore;
import static junit.framework.Assert.assertEquals;
import static com.jayway.restassured.RestAssured.given;
import static junit.framework.Assert.assertEquals;

/**
 * extremely minimal API tests for creating OAI sets.
 */
public class HarvestingServerIT
{
	private static final Logger logger = Logger.getLogger(HarvestingServerIT.class.getCanonicalName());

	@BeforeClass
	public static void setUpClass()
	{
		RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
	}

	@AfterClass
	public static void afterClass()
	{
		//intentional no-op until there's cleanup to be done
	}

	private void setupUsers()
	{
		Response cu0 = UtilIT.createRandomUser();
		normalUserAPIKey = UtilIT.getApiTokenFromResponse( cu0 );
		Response cu1 = UtilIT.createRandomUser();
		String un1 = UtilIT.getUsernameFromResponse( cu1 );
		Response u1a = UtilIT.makeSuperUser( un1 );
		adminUserAPIKey = UtilIT.getApiTokenFromResponse( cu1 );
	}

	private String jsonForTestSpec(String name, String def)
	{
		String r = String.format("{\"name\":\"%s\",\"definition\":\"%s\"}",name,def);//description is optional
		return r;
	}

	private String normalUserAPIKey;
	private String adminUserAPIKey;

	@Test
	public void testSetCreation()
	{
		setupUsers();
		String setName = UtilIT.getRandomString(6);
		String def="*";

		// make sure the set does not exist
		String u0 = String.format("/api/harvest/server/oaisets/%s",setName);
		Response r0 = given()
			.get( u0 );
		assertEquals( 404, r0.getStatusCode() );
			
		// try to create set as normal user, should fail
		
		Response r1 = given()
			.header( UtilIT.API_TOKEN_HTTP_HEADER,normalUserAPIKey)
			.body( jsonForTestSpec( setName,def) )
			.post( u0 );
		assertEquals( 400, r1.getStatusCode() );
		
		// try to create set as admin user, should succeed
		Response r2 = given()
			.header( UtilIT.API_TOKEN_HTTP_HEADER,adminUserAPIKey)
			.body( jsonForTestSpec( setName,def) )
			.post( u0 );
		assertEquals( 201, r2.getStatusCode() );

		// try to create set with same name as admin user, should fail
		Response r3 = given()
			.header( UtilIT.API_TOKEN_HTTP_HEADER,adminUserAPIKey)
			.body( jsonForTestSpec( setName,def) )
			.post( u0 );
		assertEquals( 400, r3.getStatusCode() );

		// try to export set as admin user, should succeed (under admin API, not checking that normal user will fail)
		String u1 = String.format("/api/admin/metadata/exportOAI/%s",setName);
		Response r4 = given()
			.put( u1 );
		assertEquals( 200 , r4.getStatusCode() );

		// TODO - get an answer to the question of if it's worth cleaning up (users, sets) or not

	}
        
    // A more elaborate test - we'll create and publish a dataset, then create an
    // OAI set with that one dataset, and attempt to retrieve the OAI record
    // with GetRecord. 
    @Test
    public void testGetRecord() throws InterruptedException {

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
        String datasetPersistentId = UtilIT.getPersistentDatasetIdFromResponse(createDatasetResponse);
        
        // publish dataset:
        
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", adminUserAPIKey);
        assertEquals(200, publishDataset.getStatusCode());
        
        String identifier = datasetPersistentId.substring(datasetPersistentId.lastIndexOf('/')+1);
        
        logger.info("identifier: "+identifier);
        
        // Let's try and create an OAI set with the dataset we have just 
        // created and published: 
        
        String setName = identifier;
        String setQuery="dsPersistentId:"+identifier;
        String apiPath = String.format("/api/harvest/server/oaisets/%s",setName);
                
                
        Response createSetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, adminUserAPIKey)
                .body(jsonForTestSpec(setName, setQuery))
                .post(apiPath);
        assertEquals(201, createSetResponse.getStatusCode());
        
        // TODO: a) look up the set via native harvest/server api; 
        //       b) look up the set via the OAI ListSets;
        
        // export set: 
        // (this is asynchronous - so we should probably wait a little)
        
        apiPath = String.format("/api/admin/metadata/exportOAI/%s", setName);
        Response exportSetResponse = given()
                .put(apiPath);
        assertEquals(200, exportSetResponse.getStatusCode());
        
        Thread.sleep(5000L);
        
        // And now run GetRecord, since we know the persistent id of the dataset:
        
        apiPath = String.format("/oai?verb=GetRecord&identifier=%s&metadataPrefix=oai_dc", datasetPersistentId);
        logger.info("GetRecord url: "+apiPath);
        Response getRecordResponse = given()
                .get(apiPath);
        //assertEquals(200, getRecordResponse.getStatusCode());
        logger.info(getRecordResponse.getBody().asString());
        // And confirm that we got the correct record back:
        
        assertEquals(datasetPersistentId, getRecordResponse.getBody().xmlPath().getString("OAI-PMH.GetRecord.record.header.identifier"));
        
        // TODO: 
        // check the metadata payload of the OAI record?
        
        // TODO: 
        // run ListIdentifiers on the set created above; verify that it returns 
        // one record, and that it is for the correct dataset.
    }
}
