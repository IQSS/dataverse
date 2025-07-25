package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
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

/**
 * Integration tests for the Role Assignment History API endpoints. 
 * 
 * Note: These tests require the role-assignment-history FeatureFlag to be true and are not run in normal builds
 * 
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
        Response historyCsv = UtilIT.getDataverseRoleAssignmentHistory(dataverseAlias, true, adminApiToken);
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

    @Test
    public void testDatasetRoleAssignmentHistory() {
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

        // Create dataverse and dataset
        Response createDataverseResponse = UtilIT.createRandomDataverse(adminApiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, adminApiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        // Assign roles to users on dataset
        Response grantEditor = UtilIT.grantRoleOnDataset(datasetId.toString(), DataverseRole.EDITOR, "@" + username1, adminApiToken);
        grantEditor.then().assertThat().statusCode(OK.getStatusCode());

        Response grantFileDownloader = UtilIT.grantRoleOnDataset(datasetId.toString(), DataverseRole.FILE_DOWNLOADER, "@" + username2, adminApiToken);
        grantFileDownloader.then().assertThat().statusCode(OK.getStatusCode());

        // Revoke role from user1
        grantEditor.prettyPrint();
        Long idToDelete = JsonPath.from(grantEditor.getBody().asString()).getLong("data.id");
        Response revokeEditor = UtilIT.revokeRoleOnDataset(datasetId.toString(), idToDelete, adminApiToken);
        revokeEditor.then().assertThat().statusCode(OK.getStatusCode());

        // Get role assignment history in JSON format
        Response historyJson = UtilIT.getDatasetRoleAssignmentHistory(datasetId, false, adminApiToken);
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
        assertEquals(datasetId.toString(), firstEntry.get("definedOn"));
        assertTrue(firstEntry.containsKey("assigneeIdentifier"));
        assertEquals("@" + username2, firstEntry.get("assigneeIdentifier"));
        assertTrue(firstEntry.containsKey("roleName"));
        assertEquals(DataverseRole.FILE_DOWNLOADER, firstEntry.get("roleName"));
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
        assertEquals(DataverseRole.EDITOR, secondEntry.get("roleName"));
        assertTrue(secondEntry.containsKey("assignedBy"));
        assertEquals("@" + adminUsername, secondEntry.get("assignedBy"));
        assertTrue(secondEntry.containsKey("assignedAt"));
        assertNotEquals(JsonValue.NULL, secondEntry.get("assignedAt"));
        assertTrue(secondEntry.containsKey("revokedBy"));
        assertEquals("@" + adminUsername, secondEntry.get("revokedBy"));
        assertTrue(secondEntry.containsKey("revokedAt"));
        assertNotEquals(JsonValue.NULL, secondEntry.get("revokedAt"));

        // Get role assignment history in CSV format
        Response historyCsv = UtilIT.getDatasetRoleAssignmentHistory(datasetId, true, adminApiToken);
        historyCsv.then().assertThat().statusCode(OK.getStatusCode());

        // Generate CSV response
        StringBuilder csvBuilder = AbstractApiBean.getHistoryCsvHeaderRow();
        // Verify CSV response
        String csvBody = historyCsv.getBody().asString();
        assertTrue(csvBody.startsWith(csvBuilder.toString()));
        String[] strings = csvBody.split("\n");
        assertTrue(strings[1].contains("@" + username2 + "," + DataverseRole.FILE_DOWNLOADER + ",@" + adminUsername));
        assertTrue(strings[2].contains("@" + username1 + "," + DataverseRole.EDITOR + ",@" + adminUsername));

        // Clean up
        Response deleteDatasetResponse = UtilIT.destroyDataset(datasetId, adminApiToken);
        deleteDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

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

    /*
     * This test is primarily to check the unique functionality for files where grants on multiple files within the same minute will be combined into one entry.
     */
    @Test
    public void testFileRoleAssignmentHistory() {
        // Create admin user
        Response createAdminUser = UtilIT.createRandomUser();
        String adminUsername = UtilIT.getUsernameFromResponse(createAdminUser);
        String adminApiToken = UtilIT.getApiTokenFromResponse(createAdminUser);
        UtilIT.setSuperuserStatus(adminUsername, true);

        // Create regular user
        Response createUser1 = UtilIT.createRandomUser();
        String username1 = UtilIT.getUsernameFromResponse(createUser1);

        // Create dataverse and dataset
        Response createDataverseResponse = UtilIT.createRandomDataverse(adminApiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, adminApiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        // Upload two files
        String pathToFile1 = "src/main/webapp/resources/images/dataverseproject.png";
        Response addFile1Response = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile1, adminApiToken);
        addFile1Response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("dataverseproject.png"));
        Long file1Id = JsonPath.from(addFile1Response.body().asString()).getLong("data.files[0].dataFile.id");

        String pathToFile2 = "src/main/webapp/resources/images/cc0.png";
        Response addFile2Response = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile2, adminApiToken);
        addFile2Response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("cc0.png"));
        Long file2Id = JsonPath.from(addFile2Response.body().asString()).getLong("data.files[0].dataFile.id");

        // Restrict both files
        Response restrictFile1Response = UtilIT.restrictFile(file1Id.toString(), true, adminApiToken);
        restrictFile1Response.then().assertThat().statusCode(OK.getStatusCode());

        Response restrictFile2Response = UtilIT.restrictFile(file2Id.toString(), true, adminApiToken);
        restrictFile2Response.then().assertThat().statusCode(OK.getStatusCode());

        // Assign FileDownloader role to both users on both files
        String fileIds = file1Id + ", " + file2Id;
        Response grantFileDownloader1 = UtilIT.grantFileAccess(file1Id.toString(), "@" + username1, adminApiToken);
        grantFileDownloader1.then().assertThat().statusCode(OK.getStatusCode());

        Response grantFileDownloader2 = UtilIT.grantFileAccess(file2Id.toString(), "@" + username1, adminApiToken);
        grantFileDownloader2.then().assertThat().statusCode(OK.getStatusCode());

        // Get role assignment history in JSON format
        Response historyJson = UtilIT.getDatasetFilesRoleAssignmentHistory(datasetId, false, adminApiToken);
        historyJson.then().assertThat().statusCode(OK.getStatusCode());

        historyJson.prettyPrint();

        // Verify JSON response structure
        List<Map<String, Object>> data = JsonPath.from(historyJson.getBody().asString()).getList("data");
        // History should usually contain 1 entry as both files should be listed on one line if the roles were assigned in the same minute
        // When that isn't the case, their assignedAt dates must be different.
        assertTrue(data.size() > 0);

        if (data.size() == 1) {
            // Verify the first history entry (the one unrevoked assignment)
            Map<String, Object> firstEntry = data.get(0);
            // Entry should have definedOn field with both file IDs
            assertTrue(firstEntry.containsKey("definedOn"));
            assertEquals(fileIds, firstEntry.get("definedOn"));
            assertTrue(firstEntry.containsKey("assigneeIdentifier"));
            assertEquals("@" + username1, firstEntry.get("assigneeIdentifier"));
            assertTrue(firstEntry.containsKey("roleName"));
            assertEquals(DataverseRole.FILE_DOWNLOADER, firstEntry.get("roleName"));
            assertTrue(firstEntry.containsKey("assignedBy"));
            assertEquals("@" + adminUsername, firstEntry.get("assignedBy"));
            assertTrue(firstEntry.containsKey("assignedAt"));
            assertNotEquals(JsonValue.NULL, firstEntry.get("assignedAt"));
            assertTrue(firstEntry.containsKey("revokedBy"));
            assertEquals(JsonValue.NULL, firstEntry.get("revokedBy"));
            assertTrue(firstEntry.containsKey("revokedAt"));
            assertEquals(JsonValue.NULL, firstEntry.get("revokedAt"));
        } else {
            Map<String, Object> firstEntry = data.get(0);
            // Entry should have definedOn field with the first file ID
            assertTrue(firstEntry.containsKey("definedOn"));
            assertEquals(file1Id.toString(), firstEntry.get("definedOn"));
            assertTrue(firstEntry.containsKey("assigneeIdentifier"));
            assertEquals("@" + username1, firstEntry.get("assigneeIdentifier"));
            assertTrue(firstEntry.containsKey("roleName"));
            assertEquals(DataverseRole.FILE_DOWNLOADER, firstEntry.get("roleName"));
            assertTrue(firstEntry.containsKey("assignedBy"));
            assertEquals("@" + adminUsername, firstEntry.get("assignedBy"));
            assertTrue(firstEntry.containsKey("assignedAt"));
            assertNotEquals(JsonValue.NULL, firstEntry.get("assignedAt"));
            assertTrue(firstEntry.containsKey("revokedBy"));
            assertEquals(JsonValue.NULL, firstEntry.get("revokedBy"));
            assertTrue(firstEntry.containsKey("revokedAt"));
            assertEquals(JsonValue.NULL, firstEntry.get("revokedAt"));
            Map<String, Object> secondEntry = data.get(1);
            // Entry should have definedOn field with the second file ID
            assertTrue(secondEntry.containsKey("definedOn"));
            assertEquals(file2Id.toString(), secondEntry.get("definedOn"));
            assertTrue(secondEntry.containsKey("assigneeIdentifier"));
            assertEquals("@" + username1, secondEntry.get("assigneeIdentifier"));
            assertTrue(secondEntry.containsKey("roleName"));
            assertEquals(DataverseRole.FILE_DOWNLOADER, secondEntry.get("roleName"));
            assertTrue(secondEntry.containsKey("assignedBy"));
            assertEquals("@" + adminUsername, secondEntry.get("assignedBy"));
            assertTrue(secondEntry.containsKey("assignedAt"));
            assertNotEquals(JsonValue.NULL, secondEntry.get("assignedAt"));
            assertTrue(secondEntry.containsKey("revokedBy"));
            assertEquals(JsonValue.NULL, secondEntry.get("revokedBy"));
            assertTrue(secondEntry.containsKey("revokedAt"));
            assertEquals(JsonValue.NULL, firstEntry.get("revokedAt"));
            // For two lines the assignedAt dates should be different
            assertNotEquals(firstEntry.get("assignedAt"), secondEntry.get("assignedAt"));
        }

        // Clean up
        Response deleteDatasetResponse = UtilIT.destroyDataset(datasetId, adminApiToken);
        deleteDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, adminApiToken);
        deleteDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteUser1Response = UtilIT.deleteUser(username1);
        deleteUser1Response.prettyPrint();
        assertEquals(200, deleteUser1Response.getStatusCode());

        Response deleteAdminUserResponse = UtilIT.deleteUser(adminUsername);
        deleteAdminUserResponse.prettyPrint();
        assertEquals(200, deleteAdminUserResponse.getStatusCode());
    }
}
