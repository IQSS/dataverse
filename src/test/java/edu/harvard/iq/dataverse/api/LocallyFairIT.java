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

import java.util.ArrayList;
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

    private List<String> dataverseAliases = new ArrayList<>();
    private List<String> datasetPids = new ArrayList<>();
    private List<String> usernames = new ArrayList<>();
    private String adminToken;

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
        if (adminToken == null) {
            adminToken = getSuperuserToken();
        }
        for (String datasetPid : datasetPids) {
            UtilIT.destroyDataset(datasetPid, adminToken);
        }
        for (String dataverseAlias : dataverseAliases) {
            UtilIT.deleteDataverse(dataverseAlias, adminToken);
        }
        for (String username : usernames) {
            UtilIT.deleteUser(username);
        }
        dataverseAliases.clear();
        datasetPids.clear();
        usernames.clear();
        adminToken = null;
    }

    private String getSuperuserToken() {
        Response createResponse = UtilIT.createRandomUser();
        String adminApiToken = UtilIT.getApiTokenFromResponse(createResponse);
        String username = UtilIT.getUsernameFromResponse(createResponse);
        usernames.add(username);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(Status.OK.getStatusCode());
        this.adminToken = adminApiToken;
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
        dataverseAliases.add(dataverseAlias);
        Response userResponse = UtilIT.createRandomUser();
        String username = "@" + UtilIT.getUsernameFromResponse(userResponse);
        usernames.add(UtilIT.getUsernameFromResponse(userResponse));
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
        Response userResponse2 = UtilIT.createRandomUser();
        String userToken2 = UtilIT.getApiTokenFromResponse(userResponse2);
        String username2 = "@" + UtilIT.getUsernameFromResponse(userResponse2);
        usernames.add(UtilIT.getUsernameFromResponse(userResponse2));
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
        dataverseAliases.add(dvAlias);

        Response dvResponse = UtilIT.exportDataverse(dvAlias, superUserToken);
        Integer dataverseId =UtilIT.getDataverseIdFromResponse(dvResponse);
                //dvResponse.jsonPath().getInt("data.id");

        // Create Users
        Response directUserResponse = UtilIT.createRandomUser();
        String directUserToken = UtilIT.getApiTokenFromResponse(directUserResponse);
        String directUsername = "@" + UtilIT.getUsernameFromResponse(directUserResponse);
        usernames.add(UtilIT.getUsernameFromResponse(directUserResponse));

        Response groupUserResponse = UtilIT.createRandomUser();
        String groupUserToken = UtilIT.getApiTokenFromResponse(groupUserResponse);
        String groupUsername = "@" + UtilIT.getUsernameFromResponse(groupUserResponse);
        usernames.add(UtilIT.getUsernameFromResponse(groupUserResponse));

        Response unauthorizedUserResponse = UtilIT.createRandomUser();
        String unauthorizedUserToken = UtilIT.getApiTokenFromResponse(unauthorizedUserResponse);
        usernames.add(UtilIT.getUsernameFromResponse(unauthorizedUserResponse));

        // Create Group
        String groupAlias = "testGroup" + UtilIT.getRandomString(4);
        UtilIT.createGroup(dvAlias, groupAlias, "Test Group", superUserToken).then().assertThat().statusCode(Status.CREATED.getStatusCode());
        String groupIdentifier = "&explicit/" + dataverseId + "-" + groupAlias;
        UtilIT.addToGroup(dvAlias, groupAlias, Arrays.asList(groupUsername), superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

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
        dataverseAliases.add(dvAlias);

        // Create Dataset
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dvAlias, superUserToken);
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);
        datasetPids.add(datasetPid);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        // Upload File
        Response uploadFileResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), "scripts/search/data/binary/trees.zip", superUserToken);
        Integer fileId = UtilIT.getDataFileIdFromResponse(uploadFileResponse);

        // Publish all
        UtilIT.publishDataverseViaNativeApi(dvAlias, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(datasetPid, "major", superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Restrict Dataverse
        Response authorizedUserResponse = UtilIT.createRandomUser();
        String authorizedUserToken = UtilIT.getApiTokenFromResponse(authorizedUserResponse);
        String authorizedUsername = "@" + UtilIT.getUsernameFromResponse(authorizedUserResponse);
        usernames.add(UtilIT.getUsernameFromResponse(authorizedUserResponse));
        addLocallyFairRoleAssignee(dvAlias, authorizedUsername, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        Response unauthorizedUserResponse = UtilIT.createRandomUser();
        String unauthorizedUserToken = UtilIT.getApiTokenFromResponse(unauthorizedUserResponse);
        usernames.add(UtilIT.getUsernameFromResponse(unauthorizedUserResponse));

        // 1. Check Dataverse
        UtilIT.getDataverseWithOwners(dvAlias, authorizedUserToken, false).then().assertThat().statusCode(Status.OK.getStatusCode());
        UtilIT.getDataverseWithOwners(dvAlias, unauthorizedUserToken, false).then().assertThat().statusCode(Status.NOT_FOUND.getStatusCode());

        // 2. Check Dataset
        UtilIT.nativeGetUsingPersistentId(datasetPid, authorizedUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());
        UtilIT.nativeGetUsingPersistentId(datasetPid, unauthorizedUserToken).then().assertThat().statusCode(Status.NOT_FOUND.getStatusCode());

        // 3. Check Datafile
        UtilIT.getFileData(fileId.toString(), authorizedUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());
        UtilIT.getFileData(fileId.toString(), unauthorizedUserToken).then().assertThat().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Test that locally fair content doesn't appear in search results for non-authorized users and does for those who can see it.
     */
    @Test
    public void testLocallyFairSearchVisibility() {
        String superUserToken = getSuperuserToken();
        String dvAlias = UtilIT.createRandomCollectionGetAlias(superUserToken);
        dataverseAliases.add(dvAlias);
        String dvName = JsonPath.from(UtilIT.getDataverseWithOwners(dvAlias, superUserToken, false).body().asString()).getString("data.name");

        // Restrict Dataverse
        Response authorizedUserResponse = UtilIT.createRandomUser();
        String authorizedUserToken = UtilIT.getApiTokenFromResponse(authorizedUserResponse);
        String authorizedUsername = "@" + UtilIT.getUsernameFromResponse(authorizedUserResponse);
        usernames.add(UtilIT.getUsernameFromResponse(authorizedUserResponse));
        addLocallyFairRoleAssignee(dvAlias, authorizedUsername, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Publish
        UtilIT.publishDataverseViaNativeApi(dvAlias, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Wait for index
        UtilIT.sleepForSearch(dvName, superUserToken, "", 1, 5);

        // Unauthorized search
        Response unauthorizedUserResponse = UtilIT.createRandomUser();
        String unauthorizedUserToken = UtilIT.getApiTokenFromResponse(unauthorizedUserResponse);
        usernames.add(UtilIT.getUsernameFromResponse(unauthorizedUserResponse));
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
        dataverseAliases.add(parentDv);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(parentDv, superUserToken);
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);
        datasetPids.add(datasetPid);

        // Publish normally
        UtilIT.publishDataverseViaNativeApi(parentDv, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(datasetPid, "major", superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Wait for search
        UtilIT.sleepForSearch("\"" + datasetPid + "\"", null, "", 1, 5);

        // Verify publicly visible
        UtilIT.search("\"" + datasetPid + "\"", null).then().assertThat().statusCode(Status.OK.getStatusCode())
                .body("data.total_count", equalTo(1));

        // Restrict parent
        Response authorizedUserResponse = UtilIT.createRandomUser();
        String authorizedUserToken = UtilIT.getApiTokenFromResponse(authorizedUserResponse);
        String authorizedUsername = "@" + UtilIT.getUsernameFromResponse(authorizedUserResponse);
        usernames.add(UtilIT.getUsernameFromResponse(authorizedUserResponse));
        addLocallyFairRoleAssignee(parentDv, authorizedUsername, superUserToken).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Reindex dataset
        UtilIT.reindexDataset(datasetPid).then().assertThat().statusCode(Status.OK.getStatusCode());

        // Wait for reindex to propagate (should disappear for anonymous)
        boolean disappeared = false;
        for (int i = 0; i < 10; i++) {
            Response searchResp = UtilIT.search("\"" + datasetPid + "\"", null);
            if (searchResp.jsonPath().getInt("data.total_count") == 0) {
                disappeared = true;
                break;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
        assertTrue(disappeared, "Dataset should have disappeared from search for anonymous users");

        // Verify authorized user can still see it in search
        UtilIT.search("\"" + datasetPid + "\"", authorizedUserToken).then().assertThat().statusCode(Status.OK.getStatusCode())
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
