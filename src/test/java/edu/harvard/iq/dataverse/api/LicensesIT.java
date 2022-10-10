package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import org.junit.Test;
import org.junit.BeforeClass;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;

public class LicensesIT {

    private static final Logger logger = Logger.getLogger(LicensesIT.class.getCanonicalName());

    @BeforeClass
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
        long licenseId = JsonPath.from(body).getLong("data[-1].id");
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
