
package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import java.util.logging.Logger;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author skraffmi
 */
public class RolesIT {
    
    private static final Logger logger = Logger.getLogger(AdminIT.class.getCanonicalName());

    @BeforeClass
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
        
        //        deleteTitleViaNative.then().assertThat().body("message", equalTo("Error parsing dataset update: Empty value for field: Title "));

        
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
        
        Response deleteDataverseRoleResponseSucceed = UtilIT.deleteDataverseRole("testRole", apiToken);
        deleteDataverseRoleResponseSucceed.prettyPrint();
        body = deleteDataverseRoleResponseSucceed.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);

    }
    
}
