
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.logging.Logger;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    public void testGetUserSelectableRoles() {
        Response createAdminUser = UtilIT.createRandomUser();

        String adminUsername = UtilIT.getUsernameFromResponse(createAdminUser);
        String adminApiToken = UtilIT.getApiTokenFromResponse(createAdminUser);
        UtilIT.makeSuperUser(adminUsername);

        Response createUser = UtilIT.createRandomUser();

        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        // Non-superuser with no assigned roles: return all roles as fallback.

        Response getUserSelectableRolesResponse = UtilIT.getUserSelectableRoles(apiToken);

        getUserSelectableRolesResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(8));

        // Non-superuser with assigned role: return assigned role.

        Response createDataverseResponse = UtilIT.createRandomDataverse(adminApiToken);
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response grantUserAddDataset = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.DS_CONTRIBUTOR, "@" + username, adminApiToken);

        grantUserAddDataset.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.assignee", equalTo("@" + username))
                .body("data._roleAlias", equalTo("dsContributor"));

        getUserSelectableRolesResponse = UtilIT.getUserSelectableRoles(apiToken);
        getUserSelectableRolesResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(1))
                .body("data[0].alias", equalTo(DataverseRole.DS_CONTRIBUTOR));

        // Superuser: return all roles.

        getUserSelectableRolesResponse = UtilIT.getUserSelectableRoles(adminApiToken);

        getUserSelectableRolesResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(8));
    }
}
