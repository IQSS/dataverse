
package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author skraffmi
 */
public class RolesIT {
    
    private static final Logger logger = Logger.getLogger(RolesIT.class.getCanonicalName());

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }
    
    @Test
    public void testCreateDeleteRoles() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();

        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.makeSuperUser(username);
        
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String pathToJsonFile = "scripts/api/data/role-test-addRole.json";
        Response addBuiltinRoleResponse = UtilIT.addBuiltInRole(pathToJsonFile);
        addBuiltinRoleResponse.prettyPrint();
        String body = addBuiltinRoleResponse.getBody().asString();
        String status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);
        
        //Try to delete from non-admin api - should fail.
        
        Response deleteBuiltinRoleResponseError = UtilIT.deleteDataverseRole("testRole", apiToken);
        deleteBuiltinRoleResponseError.prettyPrint();
        body = deleteBuiltinRoleResponseError.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);
        
        deleteBuiltinRoleResponseError.then().assertThat().body("message", equalTo("May not delete Built In Role Test Role."));

        
        Response deleteBuiltinRoleResponseSucceed = UtilIT.deleteBuiltInRole("testRole");
        deleteBuiltinRoleResponseSucceed.prettyPrint();
        body = deleteBuiltinRoleResponseSucceed.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);
        
        //add as dataverse role
        Response addDataverseRoleResponse = UtilIT.addDataverseRole(pathToJsonFile, dataverseAlias, apiToken);
        addDataverseRoleResponse.prettyPrint();
        body = addBuiltinRoleResponse.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);

        Response createNoPermsUser = UtilIT.createRandomUser();
        createNoPermsUser.prettyPrint();
        String noPermsapiToken = UtilIT.getApiTokenFromResponse(createNoPermsUser);

        Response noPermsResponse = UtilIT.viewDataverseRole("testRole", noPermsapiToken);
        noPermsResponse.prettyPrint();
        noPermsResponse.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response viewDataverseRoleResponse = UtilIT.viewDataverseRole("testRole", apiToken);
        viewDataverseRoleResponse.prettyPrint();
        body = viewDataverseRoleResponse.getBody().asString();
        String idString = JsonPath.from(body).getString("data.id");
        
        System.out.print("idString: " + idString);
        
        Response deleteDataverseRoleResponseBadAlias = UtilIT.deleteDataverseRole("badAlias", apiToken);
        deleteDataverseRoleResponseBadAlias.prettyPrint();
        body = deleteDataverseRoleResponseBadAlias.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);        
        deleteDataverseRoleResponseBadAlias.then().assertThat().body("message", equalTo("Dataverse Role with alias badAlias not found."));
        
        Long idBad = Long.parseLong(idString) + 10;
        Response deleteDataverseRoleResponseBadId = UtilIT.deleteDataverseRoleById(idBad.toString(), apiToken);
        deleteDataverseRoleResponseBadId.prettyPrint();
        body = deleteDataverseRoleResponseBadId.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);
        deleteDataverseRoleResponseBadId.then().assertThat().body("message", equalTo("Dataverse Role with ID " + idBad.toString() + " not found."));
        
        Response deleteDataverseRoleResponseSucceed = UtilIT.deleteDataverseRoleById(idString, apiToken);
        deleteDataverseRoleResponseSucceed.prettyPrint();
        body = deleteDataverseRoleResponseSucceed.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);

    }
    
}
