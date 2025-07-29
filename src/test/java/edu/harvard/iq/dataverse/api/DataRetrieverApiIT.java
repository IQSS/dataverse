package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.api.auth.ApiKeyAuthMechanism;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.util.BundleUtil;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataRetrieverApiIT {

    private static final String ERR_MSG_FORMAT = "{\n    \"success\": false,\n    \"error_message\": \"%s\"\n}";

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testRetrieveMyDataAsJsonString() {
        // Call with bad API token
        ArrayList<Long> emptyRoleIdsList = new ArrayList<>();
        Response badApiTokenResponse = UtilIT.retrieveMyDataAsJsonString("bad-token", "dummy-user-identifier", emptyRoleIdsList);
        badApiTokenResponse.then().assertThat().body("status", equalTo(ApiConstants.STATUS_ERROR)).body("message", equalTo(ApiKeyAuthMechanism.RESPONSE_MESSAGE_BAD_API_KEY)).statusCode(UNAUTHORIZED.getStatusCode());

        // Call as superuser with invalid user identifier
        Response createUserResponse = UtilIT.createRandomUser();
        Response makeSuperUserResponse = UtilIT.makeSuperUser(UtilIT.getUsernameFromResponse(createUserResponse));
        assertEquals(OK.getStatusCode(), makeSuperUserResponse.getStatusCode());
        String superUserApiToken = UtilIT.getApiTokenFromResponse(createUserResponse);

        String badUserIdentifier = "bad-identifier";
        Response invalidUserIdentifierResponse = UtilIT.retrieveMyDataAsJsonString(superUserApiToken, badUserIdentifier, emptyRoleIdsList);
        assertEquals(prettyPrintError("dataretrieverAPI.user.not.found", Arrays.asList(badUserIdentifier)), invalidUserIdentifierResponse.prettyPrint());
        assertEquals(OK.getStatusCode(), invalidUserIdentifierResponse.getStatusCode());

        // Call as superuser with valid user identifier and no roles
        Response createSecondUserResponse = UtilIT.createRandomUser();
        String userIdentifier = UtilIT.getUsernameFromResponse(createSecondUserResponse);
        Response validUserIdentifierResponse = UtilIT.retrieveMyDataAsJsonString(superUserApiToken, userIdentifier, emptyRoleIdsList);
        assertEquals(prettyPrintError("myDataFinder.error.result.no.role", null), validUserIdentifierResponse.prettyPrint());
        assertEquals(OK.getStatusCode(), validUserIdentifierResponse.getStatusCode());

        // Call as normal user with one valid role and no results
        Response createNormalUserResponse = UtilIT.createRandomUser();
        String normalUserUsername = UtilIT.getUsernameFromResponse(createNormalUserResponse);
        String normalUserApiToken = UtilIT.getApiTokenFromResponse(createNormalUserResponse);
        Response noResultwithOneRoleResponse = UtilIT.retrieveMyDataAsJsonString(normalUserApiToken, "", new ArrayList<>(Arrays.asList(5L)));
        assertEquals(prettyPrintError("myDataFinder.error.result.role.empty", Arrays.asList("Dataset Creator")), noResultwithOneRoleResponse.prettyPrint());
        assertEquals(OK.getStatusCode(), noResultwithOneRoleResponse.getStatusCode());

        // Call as normal user with multiple valid roles and no results
        Response noResultWithMultipleRoleResponse = UtilIT.retrieveMyDataAsJsonString(normalUserApiToken, "", new ArrayList<>(Arrays.asList(5L, 6L)));
        assertEquals(prettyPrintError("myDataFinder.error.result.roles.empty", Arrays.asList("Dataset Creator, Contributor")), noResultWithMultipleRoleResponse.prettyPrint());
        assertEquals(OK.getStatusCode(), noResultWithMultipleRoleResponse.getStatusCode());

        // Call as normal user with one valid dataset role and one dataset result
        Response createDataverseResponse = UtilIT.createRandomDataverse(normalUserApiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, normalUserApiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        UtilIT.sleepForReindex(datasetId.toString(), normalUserApiToken, 4);
        Response oneDatasetResponse = UtilIT.retrieveMyDataAsJsonString(normalUserApiToken, "", new ArrayList<>(Arrays.asList(6L)));
        assertEquals(OK.getStatusCode(), oneDatasetResponse.getStatusCode());
        JsonPath jsonPathOneDataset = oneDatasetResponse.getBody().jsonPath();
        assertEquals(1, jsonPathOneDataset.getInt("data.total_count"));
        assertEquals(datasetId, jsonPathOneDataset.getInt("data.items[0].entity_id"));

        // Call as normal user with one valid dataverse role and one dataverse result
        UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.DS_CONTRIBUTOR.toString(),
                "@" + normalUserUsername, superUserApiToken);
        Response oneDataverseResponse = UtilIT.retrieveMyDataAsJsonString(normalUserApiToken, "", new ArrayList<>(Arrays.asList(5L)));
        assertEquals(OK.getStatusCode(), oneDataverseResponse.getStatusCode());
        JsonPath jsonPathOneDataverse = oneDataverseResponse.getBody().jsonPath();
        assertEquals(1, jsonPathOneDataverse.getInt("data.total_count"));
        assertEquals(dataverseAlias, jsonPathOneDataverse.getString("data.items[0].name"));

        // Clean up
        Response deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, normalUserApiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, normalUserApiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(normalUserUsername);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
    }

    // Test getting a list of collections that the user can add datasets to
    @Test
    public void testRetrieveMyDataCollections() throws InterruptedException {
        int rootCount = 1; // everyone has access to this dataverse
        List<Object> items;
        Response createDataverseResponse;
        Response retrieveMyCollectionListResponse;
        // Create Superuser
        Response createUserResponse = UtilIT.createRandomUser();
        Response makeSuperUserResponse = UtilIT.makeSuperUser(UtilIT.getUsernameFromResponse(createUserResponse));
        assertEquals(OK.getStatusCode(), makeSuperUserResponse.getStatusCode());
        String superUserUsername = UtilIT.getUsernameFromResponse(createUserResponse);
        String superUserApiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        // Create User1
        createUserResponse = UtilIT.createRandomUser();
        assertEquals(OK.getStatusCode(), makeSuperUserResponse.getStatusCode());
        String User1Username = UtilIT.getUsernameFromResponse(createUserResponse);
        String User1ApiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        // Create User2
        createUserResponse = UtilIT.createRandomUser();
        String User2Username = UtilIT.getUsernameFromResponse(createUserResponse);
        String User2ApiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        // Create User3
        createUserResponse = UtilIT.createRandomUser();
        String User3Username = UtilIT.getUsernameFromResponse(createUserResponse);
        String User3ApiToken = UtilIT.getApiTokenFromResponse(createUserResponse);

        // User1 creates 15 Dataverses and adds a role to each allowing User2 access
        List<String> dataverses = new ArrayList<>();
        int user1DataverseCount = 15;
        for (int i = 0; i < user1DataverseCount; i++) {
            createDataverseResponse = UtilIT.createRandomDataverse(User1ApiToken);
            String alias = UtilIT.getAliasFromResponse(createDataverseResponse);
            dataverses.add(alias);
            UtilIT.grantRoleOnDataverse(alias, DataverseRole.CURATOR.toString(),
                    "@" + User2Username, User1ApiToken);
        }
        // User2 adds their own Dataverse
        int user2DataverseCount = 1;
        createDataverseResponse = UtilIT.createRandomDataverse(User2ApiToken);
        String alias = UtilIT.getAliasFromResponse(createDataverseResponse);
        dataverses.add(alias);

        // Sleep for indexing
        Thread.sleep(4000);

        // User1 gets the list of Dataverses/Collections it has access to
        retrieveMyCollectionListResponse = UtilIT.retrieveMyCollectionList(User1ApiToken, null);
        retrieveMyCollectionListResponse.prettyPrint();
        // The count should show the list size to be User1's + Root Dataverse count
        items = retrieveMyCollectionListResponse.getBody().jsonPath().getList("items");
        assertEquals(rootCount + user1DataverseCount, items.size());

        // User2 gets the list of Dataverses/Collections it has access to
        retrieveMyCollectionListResponse = UtilIT.retrieveMyCollectionList(User2ApiToken, null);
        retrieveMyCollectionListResponse.prettyPrint();
        // The count should show the list size to be User1's + User2's + Root Dataverse count
        items = retrieveMyCollectionListResponse.getBody().jsonPath().getList("items");
        assertEquals(rootCount + user1DataverseCount + user2DataverseCount, items.size());

        // User3 gets the list of Dataverses/Collections it has access to
        retrieveMyCollectionListResponse = UtilIT.retrieveMyCollectionList(User3ApiToken, null);
        retrieveMyCollectionListResponse.prettyPrint();
        // The count should show the list size to be only Root Dataverse count
        items = retrieveMyCollectionListResponse.getBody().jsonPath().getList("items");
        assertEquals(rootCount, items.size());

        // Superuser gets the list of Dataverses/Collections it has access to
        retrieveMyCollectionListResponse = UtilIT.retrieveMyCollectionList(superUserApiToken, null);
        retrieveMyCollectionListResponse.prettyPrint();
        // The count should show the list size to be only Root Dataverse count
        items = retrieveMyCollectionListResponse.getBody().jsonPath().getList("items");
        assertEquals(rootCount, items.size());

        // Superuser gets the list of Dataverses/Collections User1 has access to
        retrieveMyCollectionListResponse = UtilIT.retrieveMyCollectionList(superUserApiToken, User1Username);
        retrieveMyCollectionListResponse.prettyPrint();
        // The count should show the list size to be User1's + Root Dataverse count
        items = retrieveMyCollectionListResponse.getBody().jsonPath().getList("items");
        assertEquals(rootCount + user1DataverseCount, items.size());

        // Superuser gets the list of Dataverses/Collections User2 has access to
        retrieveMyCollectionListResponse = UtilIT.retrieveMyCollectionList(superUserApiToken, User2Username);
        retrieveMyCollectionListResponse.prettyPrint();
        // The count should show the list size to be User1's + User2's + Root Dataverse count
        items = retrieveMyCollectionListResponse.getBody().jsonPath().getList("items");
        assertEquals(rootCount + user1DataverseCount + user2DataverseCount, items.size());

        // Superuser gets the list of Dataverses/Collections for bad username
        retrieveMyCollectionListResponse = UtilIT.retrieveMyCollectionList(superUserApiToken, "badUserName");
        retrieveMyCollectionListResponse.prettyPrint();
        retrieveMyCollectionListResponse.then().assertThat()
                .body("success", equalTo(false))
                .body("error_message", startsWith("No user found for:"))
                .statusCode(OK.getStatusCode());

        // Clean up
        dataverses.forEach(dv -> {
            Response deleteDataverseResponse = UtilIT.deleteDataverse(dv, superUserApiToken);
            assertEquals(200, deleteDataverseResponse.getStatusCode());
        });
        Response deleteUserResponse = UtilIT.deleteUser(User1Username);
        deleteUserResponse.prettyPrint();
        deleteUserResponse = UtilIT.deleteUser(User2Username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
        deleteUserResponse = UtilIT.deleteUser(User3Username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
        deleteUserResponse = UtilIT.deleteUser(superUserUsername);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
    }

    @Test
    public void testRetrieveMyDataAsJsonStringSortOrder() {
        // Create superuser
        Response createSuperUserResponse = UtilIT.createRandomUser();
        String superUserIdentifier = UtilIT.getUsernameFromResponse(createSuperUserResponse);
        String superUserApiToken = UtilIT.getApiTokenFromResponse(createSuperUserResponse);
        Response makeSuperUserResponse = UtilIT.setSuperuserStatus(superUserIdentifier, true);
        assertEquals(OK.getStatusCode(), makeSuperUserResponse.getStatusCode());

        // Create regular user
        Response createUserResponse = UtilIT.createRandomUser();
        String userApiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        String userIdentifier = UtilIT.getUsernameFromResponse(createUserResponse);

        // Call as regular user with no result
        Response myDataEmptyResponse = UtilIT.retrieveMyDataAsJsonString(userApiToken, "", new ArrayList<>(Arrays.asList(6L)));
        assertEquals(prettyPrintError("myDataFinder.error.result.role.empty", Arrays.asList("Contributor")), myDataEmptyResponse.prettyPrint());
        assertEquals(OK.getStatusCode(), myDataEmptyResponse.getStatusCode());

        // Create and publish a dataverse
        Response createDataverseResponse = UtilIT.createRandomDataverse(superUserApiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, superUserApiToken);
        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());

        // Allow user to create datasets in dataverse
        Response grantRole = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.DS_CONTRIBUTOR, "@" + userIdentifier, superUserApiToken);
        grantRole.prettyPrint();
        assertEquals(OK.getStatusCode(), grantRole.getStatusCode());

        // As user, create two datasets and submit them for review
        Response createDatasetOneResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, userApiToken);
        createDatasetOneResponse.prettyPrint();
        Integer datasetOneId = UtilIT.getDatasetIdFromResponse(createDatasetOneResponse);
        String datasetOnePid = UtilIT.getDatasetPersistentIdFromResponse(createDatasetOneResponse);
        UtilIT.sleepForReindex(datasetOneId.toString(), userApiToken, 4);

        Response createDatasetTwoResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, userApiToken);
        createDatasetTwoResponse.prettyPrint();
        Integer datasetTwoId = UtilIT.getDatasetIdFromResponse(createDatasetTwoResponse);
        String datasetTwoPid = UtilIT.getDatasetPersistentIdFromResponse(createDatasetTwoResponse);
        UtilIT.sleepForReindex(datasetTwoId.toString(), userApiToken, 4);

        // Request datasets belonging to user
        Response twoDatasetsInReviewResponse = UtilIT.retrieveMyDataAsJsonString(userApiToken, "", new ArrayList<>(Arrays.asList(6L)));
        twoDatasetsInReviewResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), twoDatasetsInReviewResponse.getStatusCode());
        JsonPath jsonPathTwoDatasetsInReview = twoDatasetsInReviewResponse.getBody().jsonPath();
        assertEquals(2, jsonPathTwoDatasetsInReview.getInt("data.total_count"));
        // Expect newest dataset (dataset 2) first
        assertEquals(datasetTwoId, jsonPathTwoDatasetsInReview.getInt("data.items[0].entity_id"));
        assertEquals("DRAFT", jsonPathTwoDatasetsInReview.getString("data.items[0].versionState"));
        assertEquals(datasetOneId, jsonPathTwoDatasetsInReview.getInt("data.items[1].entity_id"));
        assertEquals("DRAFT", jsonPathTwoDatasetsInReview.getString("data.items[1].versionState"));

        // Publish dataset 1
        Response publishDatasetOne = UtilIT.publishDatasetViaNativeApi(datasetOneId, "major", superUserApiToken);
        publishDatasetOne.prettyPrint();
        publishDatasetOne.then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.sleepForReindex(datasetOneId.toString(), userApiToken, 4);

        // Publish dataset 2
        Response publishDatasetTwo = UtilIT.publishDatasetViaNativeApi(datasetTwoId, "major", superUserApiToken);
        publishDatasetTwo.prettyPrint();
        publishDatasetTwo.then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.sleepForReindex(datasetTwoId.toString(), userApiToken, 4);

        // Request datasets belonging to user
        Response twoPublishedDatasetsResponse = UtilIT.retrieveMyDataAsJsonString(userApiToken, "", new ArrayList<>(Arrays.asList(6L)));
        twoPublishedDatasetsResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), twoPublishedDatasetsResponse.getStatusCode());
        JsonPath jsonPathTwoPublishedDatasets = twoPublishedDatasetsResponse.getBody().jsonPath();
        assertEquals(2, jsonPathTwoPublishedDatasets.getInt("data.total_count"));
        // Expect newest dataset (dataset 2) first
        assertEquals(datasetTwoId, jsonPathTwoPublishedDatasets.getInt("data.items[0].entity_id"));
        assertEquals("RELEASED", jsonPathTwoPublishedDatasets.getString("data.items[0].versionState"));
        assertEquals(datasetOneId, jsonPathTwoPublishedDatasets.getInt("data.items[1].entity_id"));
        assertEquals("RELEASED", jsonPathTwoPublishedDatasets.getString("data.items[1].versionState"));

        // Create new draft version of dataset 1 by updating metadata
        String pathToJsonFilePostPub= "doc/sphinx-guides/source/_static/api/dataset-add-metadata-after-pub.json";
        Response addDataToPublishedVersion = UtilIT.addDatasetMetadataViaNative(datasetOnePid, pathToJsonFilePostPub, userApiToken);
        addDataToPublishedVersion.prettyPrint();
        addDataToPublishedVersion.then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.sleepForReindex(datasetOneId.toString(), userApiToken, 4);

        // Request datasets belonging to user
        Response twoPublishedDatasetsOneDraftResponse = UtilIT.retrieveMyDataAsJsonString(userApiToken, "", new ArrayList<>(Arrays.asList(6L)));
        twoPublishedDatasetsOneDraftResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), twoPublishedDatasetsOneDraftResponse.getStatusCode());
        JsonPath jsonPathTwoPublishedDatasetsOneDraft = twoPublishedDatasetsOneDraftResponse.getBody().jsonPath();
        assertEquals(3, jsonPathTwoPublishedDatasetsOneDraft.getInt("data.total_count"));

        // Expect newest dataset version (draft of dataset 1) first
        assertEquals(datasetOneId, jsonPathTwoPublishedDatasetsOneDraft.getInt("data.items[0].entity_id"));
        assertEquals("DRAFT", jsonPathTwoPublishedDatasetsOneDraft.getString("data.items[0].versionState"));
        // ...followed by dataset 2 (created after dataset 1)
        assertEquals(datasetTwoId, jsonPathTwoPublishedDatasetsOneDraft.getInt("data.items[1].entity_id"));
        assertEquals("RELEASED", jsonPathTwoPublishedDatasetsOneDraft.getString("data.items[1].versionState"));
        // ...followed by dataset 1 (oldest, created before dataset 2)
        assertEquals(datasetOneId, jsonPathTwoPublishedDatasetsOneDraft.getInt("data.items[2].entity_id"));
        assertEquals("RELEASED", jsonPathTwoPublishedDatasetsOneDraft.getString("data.items[2].versionState"));

        // Create new draft version of dataset 2 by uploading a file
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response uploadImage = UtilIT.uploadFileViaNative(datasetTwoId.toString(), pathToFile, userApiToken);
        uploadImage.prettyPrint();
        uploadImage.then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.sleepForReindex(datasetTwoId.toString(), userApiToken, 4);

        // Request datasets belonging to user
        Response twoPublishedDatasetsTwoDraftsResponse = UtilIT.retrieveMyDataAsJsonString(userApiToken, "", new ArrayList<>(Arrays.asList(6L)));
        twoPublishedDatasetsTwoDraftsResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), twoPublishedDatasetsTwoDraftsResponse.getStatusCode());
        JsonPath jsonPathTwoPublishedDatasetsTwoDrafts = twoPublishedDatasetsTwoDraftsResponse.getBody().jsonPath();
        assertEquals(4, jsonPathTwoPublishedDatasetsTwoDrafts.getInt("data.total_count"));

        // Expect newest dataset version (draft of dataset 2) first
        assertEquals(datasetTwoId, jsonPathTwoPublishedDatasetsTwoDrafts.getInt("data.items[0].entity_id"));
        assertEquals("DRAFT", jsonPathTwoPublishedDatasetsTwoDrafts.getString("data.items[0].versionState"));
        assertEquals(datasetOneId, jsonPathTwoPublishedDatasetsTwoDrafts.getInt("data.items[1].entity_id"));
        assertEquals("DRAFT", jsonPathTwoPublishedDatasetsTwoDrafts.getString("data.items[1].versionState"));
        assertEquals(datasetTwoId, jsonPathTwoPublishedDatasetsTwoDrafts.getInt("data.items[2].entity_id"));
        assertEquals("RELEASED", jsonPathTwoPublishedDatasetsTwoDrafts.getString("data.items[2].versionState"));
        assertEquals(datasetOneId, jsonPathTwoPublishedDatasetsTwoDrafts.getInt("data.items[3].entity_id"));
        assertEquals("RELEASED", jsonPathTwoPublishedDatasetsTwoDrafts.getString("data.items[3].versionState"));

        // Publish minor version of dataset 1
        Response publishDatasetOneMinor = UtilIT.publishDatasetViaNativeApi(datasetOneId, "minor", superUserApiToken);
        publishDatasetOneMinor.prettyPrint();
        publishDatasetOneMinor.then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.sleepForReindex(datasetOneId.toString(), userApiToken, 4);

        // Request datasets belonging to user
        Response oneMinorOneMajorOneDraftDatasetResponse = UtilIT.retrieveMyDataAsJsonString(userApiToken, "", new ArrayList<>(Arrays.asList(6L)));
        oneMinorOneMajorOneDraftDatasetResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), oneMinorOneMajorOneDraftDatasetResponse.getStatusCode());
        JsonPath jsonPathOneMinorOneMajorOneDraftDataset = oneMinorOneMajorOneDraftDatasetResponse.getBody().jsonPath();
        assertEquals(3, jsonPathOneMinorOneMajorOneDraftDataset.getInt("data.total_count"));

        // Expect minor version of dataset 1 to be sorted last (based on release date of major version)
        assertEquals(datasetTwoId, jsonPathOneMinorOneMajorOneDraftDataset.getInt("data.items[0].entity_id"));
        assertEquals("DRAFT", jsonPathOneMinorOneMajorOneDraftDataset.getString("data.items[0].versionState"));

        assertEquals(datasetTwoId, jsonPathOneMinorOneMajorOneDraftDataset.getInt("data.items[1].entity_id"));
        assertEquals("RELEASED", jsonPathOneMinorOneMajorOneDraftDataset.getString("data.items[1].versionState"));

        assertEquals(datasetOneId, jsonPathOneMinorOneMajorOneDraftDataset.getInt("data.items[2].entity_id"));
        assertEquals("RELEASED", jsonPathOneMinorOneMajorOneDraftDataset.getString("data.items[2].versionState"));
        assertEquals(1, jsonPathOneMinorOneMajorOneDraftDataset.getInt("data.items[2].majorVersion"));
        assertEquals(1, jsonPathOneMinorOneMajorOneDraftDataset.getInt("data.items[2].minorVersion"));

        // Clean up
        Response deleteDatasetOneResponse = UtilIT.destroyDataset(datasetOneId, superUserApiToken);
        deleteDatasetOneResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), deleteDatasetOneResponse.getStatusCode());
        Response deleteDatasetTwoResponse = UtilIT.destroyDataset(datasetTwoId, superUserApiToken);
        deleteDatasetTwoResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), deleteDatasetTwoResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, superUserApiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(userIdentifier);
        deleteUserResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), deleteUserResponse.getStatusCode());

        Response deleteSuperUserResponse = UtilIT.deleteUser(superUserIdentifier);
        deleteSuperUserResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), deleteSuperUserResponse.getStatusCode());
    }

    private static String prettyPrintError(String resourceBundleKey, List<String> params) {
        final String errorMessage;
        if (params == null || params.isEmpty()) {
            errorMessage = BundleUtil.getStringFromBundle(resourceBundleKey);
        } else {
            errorMessage = BundleUtil.getStringFromBundle(resourceBundleKey, params);
        }
        return String.format(ERR_MSG_FORMAT, errorMessage.replaceAll("\"", "\\\\\""));
    }
}
