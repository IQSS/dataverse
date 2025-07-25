package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.util.BundleUtil;

/**
 * Integration tests for the Role Assignment History API endpoints.
 * 
 * @author [Your Name]
 */
public class RoleAssignmentHistoryIT {
    
    private static final Logger logger = Logger.getLogger(RoleAssignmentHistoryIT.class.getCanonicalName());

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }
    
    @Test
    public void testDataverseRoleAssignmentHistory() {
        // Create admin user
        Response createAdminUser = UtilIT.createRandomUser();
        String adminUsername = UtilIT.getUsernameFromResponse(createAdminUser);
        String adminApiToken = UtilIT.getApiTokenFromResponse(createAdminUser);
        UtilIT.setSuperuserStatus(adminUsername, true);
        
        // Create regular users
        Response createUser1 = UtilIT.createRandomUser();
        String username1 = UtilIT.getUsernameFromResponse(createUser1);
        
        Response createUser2 = UtilIT.createRandomUser();
        String username2 = UtilIT.getUsernameFromResponse(createUser2);
        
        // Create dataverse
        Response createDataverseResponse = UtilIT.createRandomDataverse(adminApiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        
        // Assign roles to users
        Response grantContributor = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.DS_CONTRIBUTOR, "@" + username1, adminApiToken);
        grantContributor.then().assertThat().statusCode(OK.getStatusCode());
        
        Response grantCurator = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.CURATOR, "@" + username2, adminApiToken);
        grantCurator.then().assertThat().statusCode(OK.getStatusCode());
        
        // Revoke role from user1
        grantContributor.prettyPrint();
        String idToDelete = JsonPath.from(grantContributor.getBody().asString()).getString("data.id");
        Response revokeContributor = UtilIT.revokeRoleOnDataverse(dataverseAlias, Long.parseLong(idToDelete), adminApiToken);
        revokeContributor.then().assertThat().statusCode(OK.getStatusCode());
        
        // Get role assignment history in JSON format
        Response historyJson = UtilIT.getDataverseRoleAssignmentHistory(dataverseAlias, false, adminApiToken);
        historyJson.then().assertThat().statusCode(OK.getStatusCode());
        
        historyJson.prettyPrint();
        
        // Verify JSON response structure
        List<Map<String, Object>> data = JsonPath.from(historyJson.getBody().asString()).getList("data");
        // History should contain 2 entries
        assertTrue(data.size() == 2);
        
        // Verify the first history entry (the one unrevoked assignment)
        Map<String, Object> firstEntry = data.get(0);
        // Entry should have definedOn field
        assertTrue(firstEntry.containsKey("definedOn"));
        assertTrue(firstEntry.containsKey("assigneeIdentifier"));
        assertEquals("@" + username2, firstEntry.get("assigneeIdentifier"));
        assertTrue(firstEntry.containsKey("roleName"));
        assertEquals(DataverseRole.CURATOR, firstEntry.get("roleName"));
        assertTrue(firstEntry.containsKey("assignedBy"));
        assertEquals("@" + adminUsername, firstEntry.get("assignedBy"));
        assertTrue(firstEntry.containsKey("assignedAt"));
        assertNotEquals(JsonValue.NULL, firstEntry.get("assignedAt"));
        assertTrue(firstEntry.containsKey("revokedBy"));
        assertEquals(JsonValue.NULL, firstEntry.get("revokedBy"));
        assertTrue(firstEntry.containsKey("revokedAt"));
        assertEquals(JsonValue.NULL, firstEntry.get("revokedAt"));
        
        // Verify the second history entry
        Map<String, Object> secondEntry = data.get(0);
        // Entry should have definedOn field
        assertTrue(secondEntry.containsKey("definedOn"));
        assertTrue(secondEntry.containsKey("assigneeIdentifier"));
        assertEquals("@" + username1, secondEntry.get("assigneeIdentifier"));
        assertTrue(secondEntry.containsKey("roleName"));
        assertEquals(DataverseRole.DS_CONTRIBUTOR, secondEntry.get("roleName"));
        assertTrue(secondEntry.containsKey("assignedBy"));
        assertEquals("@" + adminUsername, secondEntry.get("assignedBy"));
        assertTrue(secondEntry.containsKey("assignedAt"));
        assertNotEquals(JsonValue.NULL, secondEntry.get("assignedAt"));
        assertTrue(secondEntry.containsKey("revokedBy"));
        assertEquals("@" + adminUsername, secondEntry.get("revokedBy"));
        assertTrue(secondEntry.containsKey("revokedAt"));
        assertNotEquals(JsonValue.NULL, secondEntry.get("revokedAt"));
        
        // Get role assignment history in CSV format
        Response historyCsv = UtilIT.getDataverseRoleAssignmentHistory(dataverseAlias, false, adminApiToken);
        historyCsv.then().assertThat().statusCode(OK.getStatusCode());
        
        // Generate CSV response
        StringBuilder csvBuilder = AbstractApiBean.getHistoryCsvHeaderRow();
        // Verify CSV response
        String csvBody = historyCsv.getBody().asString();
        assertTrue(csvBody.startsWith(csvBuilder.toString()));
        String[] strings = csvBody.split("\n");
        assertTrue(strings[1].contains("@" + username2 + "," + DataverseRole.CURATOR + ",@" + adminUsername));
        assertTrue(strings[2].contains("@" + username2 + "," + DataverseRole.DS_CONTRIBUTOR + ",@" + adminUsername));
        
        // Clean up
        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, adminApiToken);
        deleteDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());
        
        Response deleteUser2Response = UtilIT.deleteUser(username2);
        deleteUser2Response.prettyPrint();
        assertEquals(200, deleteUser2Response.getStatusCode());
        
        Response deleteUser1Response = UtilIT.deleteUser(username1);
        deleteUser1Response.prettyPrint();
        assertEquals(200, deleteUser1Response.getStatusCode());
        
        Response deleteAdminUserResponse = UtilIT.deleteUser(adminUsername);
        deleteAdminUserResponse.prettyPrint();
        assertEquals(200, deleteAdminUserResponse.getStatusCode());
    }
}

