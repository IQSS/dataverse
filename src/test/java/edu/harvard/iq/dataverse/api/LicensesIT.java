package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LicensesIT {

    private static final Logger logger = Logger.getLogger(LicensesIT.class.getCanonicalName());

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testLicenses(){

        //Setup two users - one a superuser
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createUser2 = UtilIT.createRandomUser();
        createUser2.prettyPrint();
        createUser2.then().assertThat()
                .statusCode(OK.getStatusCode());
        String adminName = UtilIT.getUsernameFromResponse(createUser2);
        String adminApiToken = UtilIT.getApiTokenFromResponse(createUser2);
        
        UtilIT.makeSuperUser(adminName).then().assertThat().statusCode(OK.getStatusCode());

        //Try adding a license as a normal user
        String pathToJsonFile = "src/test/resources/json/license.json";
        Response addLicenseResponse = UtilIT.addLicense(pathToJsonFile, apiToken);
        addLicenseResponse.prettyPrint();
        String body = addLicenseResponse.getBody().asString();
        String status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);

      //Succeed in adding it as a superuser
        Response adminAddLicenseResponse = UtilIT.addLicense(pathToJsonFile, adminApiToken);
        adminAddLicenseResponse.prettyPrint();
        body = adminAddLicenseResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);

        // Extract the license ID from the location header
        String locationHeader = adminAddLicenseResponse.getHeader("Location");
        long addedLicenseId = Long.parseLong(locationHeader.substring(locationHeader.lastIndexOf("/") + 1));

        // New test section to verify the added license
        Response getLicenseResponse = UtilIT.getLicenseById(addedLicenseId);
        getLicenseResponse.prettyPrint();
        getLicenseResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data", notNullValue());

        // Read the content of the license.json file
        String expectedLicenseJson;
        try {
            expectedLicenseJson = new String(Files.readAllBytes(Paths.get(pathToJsonFile)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read license file", e);
        }
        JsonPath expectedJson = new JsonPath(expectedLicenseJson);

        // Compare the returned license data with the expected JSON
        JsonPath actualJson = getLicenseResponse.jsonPath();
        assertEquals(expectedJson.getString("name"), actualJson.getString("data.name"));
        assertEquals(expectedJson.getString("uri"), actualJson.getString("data.uri"));
        assertEquals(expectedJson.getString("shortDescription"), actualJson.getString("data.shortDescription"));
        assertEquals(expectedJson.getString("iconUrl"), actualJson.getString("data.iconUrl"));
        assertEquals(expectedJson.getBoolean("active"), actualJson.getBoolean("data.active"));
        assertEquals(expectedJson.getInt("sortOrder"), actualJson.getInt("data.sortOrder"));
        assertEquals(expectedJson.getString("rightsIdentifier"), actualJson.getString("data.rightsIdentifier"));
        assertEquals(expectedJson.getString("rightsIdentifierScheme"), actualJson.getString("data.rightsIdentifierScheme"));
        assertEquals(expectedJson.getString("schemeUri"), actualJson.getString("data.schemeUri"));
        assertEquals(expectedJson.getString("languageCode"), actualJson.getString("data.languageCode"));

        //Fail to add a license with incorrect json (tries to define it's id which is assigned by the server)
        pathToJsonFile = "src/test/resources/json/licenseError.json";
        Response addLicenseErrorResponse = UtilIT.addLicense(pathToJsonFile, adminApiToken);
        addLicenseErrorResponse.prettyPrint();
        body = addLicenseErrorResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);
        
        //Get the license list
        Response getLicensesResponse = UtilIT.getLicenses();
        getLicensesResponse.prettyPrint();
        body = getLicensesResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        //Last added license; with the highest id
        long licenseId = (long) JsonPath.from(body).<Integer>getList("data.id").stream().max((x, y) -> Integer.compare(x, y)).get();
        //Assumes the first license is active, which should be true on a test server 
        long activeLicenseId = JsonPath.from(body).getLong("data[0].id");
        assertEquals("OK", status);
        
        //Get the last license by it's id
        Response getLicenseByIdResponse = UtilIT.getLicenseById(licenseId);
        getLicenseByIdResponse.prettyPrint();
        body = getLicenseByIdResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);
        
        //Fail trying to get the next license (which doesn't exist)
        Response getLicenseErrorResponse = UtilIT.getLicenseById(licenseId + 1L);
        getLicenseErrorResponse.prettyPrint();
        body = getLicenseErrorResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);
        
        //Fail setting the license we added, which is inactive, license as the default
        Response setDefaultLicenseByIdResponse = UtilIT.setDefaultLicenseById(licenseId, adminApiToken);
        setDefaultLicenseByIdResponse.prettyPrint();
        body = setDefaultLicenseByIdResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);
        
        //Set active and try again
        Response setActivceLicenseByIdResponse = UtilIT.setLicenseActiveById(licenseId, true, adminApiToken);
        setActivceLicenseByIdResponse.prettyPrint();
        body = setActivceLicenseByIdResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);
        
        Response setDefaultLicenseByIdAgainResponse = UtilIT.setDefaultLicenseById(licenseId, adminApiToken);
        setDefaultLicenseByIdAgainResponse.prettyPrint();
        body = setDefaultLicenseByIdAgainResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);

        //Fail deleting our test license as it is the default
        Response deleteDefaultLicenseByIdResponse = UtilIT.deleteLicenseById(licenseId, adminApiToken);
        deleteDefaultLicenseByIdResponse.prettyPrint();
        body = deleteDefaultLicenseByIdResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);

        //Set some other active license as the default
        Response resetDefaultLicenseByIdResponse = UtilIT.setDefaultLicenseById(activeLicenseId, adminApiToken);
        resetDefaultLicenseByIdResponse.prettyPrint();
        body = resetDefaultLicenseByIdResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);
        
        //Fail trying to set null sort order
        Response setSortOrderErrorResponse = UtilIT.setLicenseSortOrderById(activeLicenseId, null, adminApiToken);
        setSortOrderErrorResponse.prettyPrint();
        body = setSortOrderErrorResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);
        
        //Succeed in setting sort order
        Response setSortOrderResponse = UtilIT.setLicenseSortOrderById(activeLicenseId, 2l, adminApiToken);
        setSortOrderResponse.prettyPrint();
        body = setSortOrderResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);

        //Succeed in deleting our test license
        Response deleteLicenseByIdResponse = UtilIT.deleteLicenseById(licenseId, adminApiToken);
        deleteLicenseByIdResponse.prettyPrint();
        body = deleteLicenseByIdResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);
        
        //Try to delete a non-existent license
        Response deleteLicenseErrorResponse = UtilIT.deleteLicenseById(licenseId + 1L, adminApiToken);
        deleteLicenseErrorResponse.prettyPrint();
        body = deleteLicenseErrorResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);
        
    }
}
