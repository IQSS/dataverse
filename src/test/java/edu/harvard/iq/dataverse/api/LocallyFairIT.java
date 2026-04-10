package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Locally FAIR mechanism.
 */
public class LocallyFairIT {

    private static final Logger logger = Logger.getLogger(LocallyFairIT.class.getCanonicalName());

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @AfterAll
    public static void tearDownClass() {
    }

    private String getSuperuserToken() {
        Response createResponse = UtilIT.createRandomUser();
        String adminApiToken = UtilIT.getApiTokenFromResponse(createResponse);
        String username = UtilIT.getUsernameFromResponse(createResponse);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(Status.OK.getStatusCode());
        return adminApiToken;
    }

    /**
     * Test CRUD of a collection's locally fair assignees.
     * This checks that users can be added, listed, and removed from the locally FAIR list.
     */
    @Test
    public void testLocallyFairAssigneesCRUD() {
        String superUserToken = getSuperuserToken();
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(superUserToken);
        Response userResponse = UtilIT.createRandomUser();
        String username = "@" + UtilIT.getUsernameFromResponse(userResponse);
        String userToken = UtilIT.getApiTokenFromResponse(userResponse);

        // 1. Add locally fair assignee
        addLocallyFairRoleAssignee(dataverseAlias, username, superUserToken)
                .then().assertThat().statusCode(Status.OK.getStatusCode())
                .body("data.locallyFairRoleAssignees", hasItem(username));

        // 2. List locally fair assignees
        listLocallyFairRoleAssignees(dataverseAlias, superUserToken)
                .then().assertThat().statusCode(Status.OK.getStatusCode())
                .body("data", hasItem(username));

        // 3. Set locally fair assignees (replaces)
        String userToken2 = UtilIT.createRandomUserGetToken();
        String username2 = "@" + UtilIT.getUsernameFromResponse(UtilIT.getAuthenticatedUserByToken(userToken2));
        setLocallyFairRoleAssignees(dataverseAlias, Arrays.asList(username2), superUserToken)
                .then().assertThat().statusCode(Status.OK.getStatusCode())
                .body("data.locallyFairRoleAssignees", hasItem(username2))
                .body("data.locallyFairRoleAssignees", not(hasItem(username)));

        // 4. Delete locally fair assignee
        deleteLocallyFairRoleAssignee(dataverseAlias, username2, superUserToken)
                .then().assertThat().statusCode(Status.OK.getStatusCode())
                .body("data.locallyFairRoleAssignees", not(hasItem(username2)));

        // 5. Test Forbidden for non-superuser
        listLocallyFairRoleAssignees(dataverseAlias, userToken)
                .then().assertThat().statusCode(Status.FORBIDDEN.getStatusCode());
    }

    /**
     * Test that a user listed directly and via a group can access locally fair content.
     * Also checks that a user NOT listed/in a group cannot access it.
     */
    @Test
    public void testLocallyFairAccessPermissions() {
        String superUserToken = getSuperuserToken();
        String dvAlias = UtilIT.createRandomCollectionGetAlias(superUserToken);

        // Create Users
        String directUserToken = UtilIT.createRandomUserGetToken();
        String directUsername = "@" + UtilIT.getUsernameFromResponse(UtilIT.getAuthenticatedUserByToken(directUserToken));

        String groupUserToken = UtilIT.createRandomUserGetToken();
        String groupUsername = "@" + UtilIT.getUsernameFromResponse(UtilIT.getAuthenticatedUserByToken(groupUserToken));

        String unauthorizedUserToken = UtilIT.createRandomUserGetToken();

        // Create Group
        String groupAlias = "testGroup" + UtilIT.getRandomString(4);
        UtilIT.createGroup(dvAlias, groupAlias, "Test Group", superUserToken).then().assertThat().statusCode(Status.CREATED.getStatusCode());
        String groupIdentifier = "&explicit/" + dvAlias + "/" + groupAlias;
        UtilIT.addToGroup(dvAlias, groupIdentifier, Arrays.asList(groupUsername), superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Restrict Dataverse
        setLocallyFairRoleAssignees(dvAlias, Arrays.asList(directUsername, groupIdentifier), superUserToken)
                .then().assertThat().statusCode(Status.OK.getStatusCode());

        // Publish Dataverse
        UtilIT.publishDataverseViaNativeApi(dvAlias, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Verify Access
        // Direct User
        UtilIT.getDataverseWithOwners(dvAlias, directUserToken, false).then().assertThat().statusCode(Status.OK.getStatusCode());
        // Group User
        UtilIT.getDataverseWithOwners(dvAlias, groupUserToken, false).then().assertThat().statusCode(Status.OK.getStatusCode());
        // Unauthorized User
        UtilIT.getDataverseWithOwners(dvAlias, unauthorizedUserToken, false).then().assertThat().statusCode(Status.NOT_FOUND.getStatusCode());
        // Anonymous User
        UtilIT.getDataverseWithOwners(dvAlias, null, false).then().assertThat().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Test that the Locally FAIR mechanism works with collections, datasets, and datafiles.
     * Verifies 404 for unauthorized users on all object types.
     */
    @Test
    public void testLocallyFairAcrossAllObjectTypes() {
        String superUserToken = getSuperuserToken();
        String dvAlias = UtilIT.createRandomCollectionGetAlias(superUserToken);

        // Create Dataset
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dvAlias, superUserToken);
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        // Upload File
        Response uploadFileResponse = UtilIT.uploadRandomFile(datasetPid, superUserToken);
        Integer fileId = UtilIT.getDataFileIdFromResponse(uploadFileResponse);

        // Publish all
        UtilIT.publishDatasetViaNativeApi(datasetPid, "major", superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());
        UtilIT.publishDataverseViaNativeApi(dvAlias, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Restrict Dataverse
        String authorizedUserToken = UtilIT.createRandomUserGetToken();
        String authorizedUsername = "@" + UtilIT.getUsernameFromResponse(UtilIT.getAuthenticatedUserByToken(authorizedUserToken));
        addLocallyFairRoleAssignee(dvAlias, authorizedUsername, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        String unauthorizedUserToken = UtilIT.createRandomUserGetToken();

        // 1. Check Dataverse
        UtilIT.getDataverseWithOwners(dvAlias, authorizedUserToken, false).then().assertThat().statusCode(Status.OK.getStatusCode());
        UtilIT.getDataverseWithOwners(dvAlias, unauthorizedUserToken, false).then().assertThat().statusCode(Status.NOT_FOUND.getStatusCode());

        // 2. Check Dataset
        UtilIT.nativeGetUsingPersistentId(datasetPid, authorizedUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());
        UtilIT.nativeGetUsingPersistentId(datasetPid, unauthorizedUserToken).then().assertThat().statusCode(Status.NOT_FOUND.getStatusCode());

        // 3. Check Datafile
        UtilIT.getFileMetadata(fileId.toString(), null, authorizedUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());
        UtilIT.getFileMetadata(fileId.toString(), null, unauthorizedUserToken).then().assertThat().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Test that locally fair content doesn't appear in search results for non-authorized users and does for those who can see it.
     */
    @Test
    public void testLocallyFairSearchVisibility() {
        String superUserToken = getSuperuserToken();
        String dvAlias = UtilIT.createRandomCollectionGetAlias(superUserToken);
        String dvName = JsonPath.from(UtilIT.getDataverseWithOwners(dvAlias, superUserToken, false).body().asString()).getString("data.name");

        // Restrict Dataverse
        String authorizedUserToken = UtilIT.createRandomUserGetToken();
        String authorizedUsername = "@" + UtilIT.getUsernameFromResponse(UtilIT.getAuthenticatedUserByToken(authorizedUserToken));
        addLocallyFairRoleAssignee(dvAlias, authorizedUsername, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Publish
        UtilIT.publishDataverseViaNativeApi(dvAlias, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Wait for index
        UtilIT.sleepForSearch(dvName, superUserToken, null, 1, 30);

        // Unauthorized search
        String unauthorizedUserToken = UtilIT.createRandomUserGetToken();
        UtilIT.search("name:\"" + dvName + "\"", unauthorizedUserToken).then().assertThat().statusCode(Status.OK.getStatusCode())
                .body("data.total_count", equalTo(0));

        // Authorized search
        UtilIT.search("name:\"" + dvName + "\"", authorizedUserToken).then().assertThat().statusCode(Status.OK.getStatusCode())
                .body("data.total_count", equalTo(1))
                .body("data.items[0].name", equalTo(dvName));
    }

    /**
     * Test reindexing behavior when a parent collection becomes restricted.
     * A dataset published normally should become hidden after its parent is restricted and it is reindexed.
     */
    @Test
    public void testReindexingMakesDatasetLocallyFair() {
        String superUserToken = getSuperuserToken();
        String parentDv = UtilIT.createRandomCollectionGetAlias(superUserToken);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(parentDv, superUserToken);
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);
        String datasetName = JsonPath.from(UtilIT.nativeGetUsingPersistentId(datasetPid, superUserToken).body().asString()).getString("data.latestVersion.metadataBlocks.citation.fields[0].value");

        // Publish normally
        UtilIT.publishDataverseViaNativeApi(parentDv, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(datasetPid, "major", superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Wait for search
        UtilIT.sleepForSearch(datasetName, null, null, 1, 30);

        // Verify publicly visible
        UtilIT.search("name:\"" + datasetName + "\"", null).then().assertThat().statusCode(Status.OK.getStatusCode())
                .body("data.total_count", equalTo(1));

        // Restrict parent
        String authorizedUserToken = UtilIT.createRandomUserGetToken();
        String authorizedUsername = "@" + UtilIT.getUsernameFromResponse(UtilIT.getAuthenticatedUserByToken(authorizedUserToken));
        addLocallyFairRoleAssignee(parentDv, authorizedUsername, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Reindex dataset
        UtilIT.reindexDataset(datasetPid).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Wait for reindex to propagate (should disappear for anonymous)
        boolean disappeared = false;
        for (int i = 0; i < 10; i++) {
            Response searchResp = UtilIT.search("name:\"" + datasetName + "\"", null);
            if (searchResp.jsonPath().getInt("data.total_count") == 0) {
                disappeared = true;
                break;
            }
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
        }
        assertTrue(disappeared, "Dataset should have disappeared from search for anonymous users");

        // Verify authorized user can still see it in search
        UtilIT.search("name:\"" + datasetName + "\"", authorizedUserToken).then().assertThat().statusCode(Status.OK.getStatusCode())
                .body("data.total_count", equalTo(1));
    }

    private Response listLocallyFairRoleAssignees(String dvIdtf, String apiToken) {
        return given().header("X-Dataverse-key", apiToken)
                .get("/api/dataverses/" + dvIdtf + "/locallyFairRoleAssignees");
    }

    private Response setLocallyFairRoleAssignees(String dvIdtf, List<String> roleAssigneeIdentifiers, String apiToken) {
        return given().header("X-Dataverse-key", apiToken)
                .contentType("application/json")
                .body(roleAssigneeIdentifiers)
                .put("/api/dataverses/" + dvIdtf + "/locallyFairRoleAssignees");
    }

    private Response addLocallyFairRoleAssignee(String dvIdtf, String roleAssigneeIdentifier, String apiToken) {
        return given().header("X-Dataverse-key", apiToken)
                .put("/api/dataverses/" + dvIdtf + "/locallyFairRoleAssignees/" + roleAssigneeIdentifier);
    }

    private Response deleteLocallyFairRoleAssignee(String dvIdtf, String roleAssigneeIdentifier, String apiToken) {
        return given().header("X-Dataverse-key", apiToken)
                .delete("/api/dataverses/" + dvIdtf + "/locallyFairRoleAssignees/" + roleAssigneeIdentifier);
    }
}
