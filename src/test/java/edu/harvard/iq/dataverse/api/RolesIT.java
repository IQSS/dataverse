
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
        
        
        Response deleteBuiltinRoleResponseSucceed = UtilIT.deleteBuiltInRole("testRole");
        deleteBuiltinRoleResponseSucceed.prettyPrint();
        body = deleteBuiltinRoleResponseSucceed.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);
        
/*
        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
*/
    }
    
}
