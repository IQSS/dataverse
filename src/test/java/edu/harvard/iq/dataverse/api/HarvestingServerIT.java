package edu.harvard.iq.dataverse.api;

import java.util.logging.Logger;
import com.jayway.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import static junit.framework.Assert.assertEquals;

import com.jayway.restassured.response.Response;
import static com.jayway.restassured.RestAssured.given;

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
}
