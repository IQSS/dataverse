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
