package edu.harvard.iq.dataverse.api;

import io.restassured.response.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.hamcrest.Matchers.*;

import java.util.stream.Stream;

/**
 * Simple test of NOT_FOUND responses when LocalContexts is not configured
 * 
 */
public class LocalContextsIT {

    @ParameterizedTest
    @MethodSource("localContextsTestCases")
    public void testLocalContextsWithoutConfiguration(String testCase) {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        Response localContextsResponse;
        if (testCase.equals("searchLocalContexts")) {
            localContextsResponse = UtilIT.searchLocalContexts(datasetId.toString(), apiToken);
        } else {
            String projectId = "sample_project_id";
            localContextsResponse = UtilIT.getLocalContextsProject(datasetId.toString(), projectId, apiToken);
        }

        localContextsResponse.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode())
                .body("message", equalTo("LocalContexts API configuration is missing."));

        // Clean up
        UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        UtilIT.deleteDataverse(dataverseAlias, apiToken);
        UtilIT.deleteUser(username);
    }

    private static Stream<String> localContextsTestCases() {
        return Stream.of("searchLocalContexts", "getLocalContextsProject");
    }
}