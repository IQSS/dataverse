package edu.harvard.iq.dataverse.api;

import io.restassured.response.Response;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.hamcrest.Matchers.*;

import java.util.stream.Stream;

/**
 * Since testing for valid responses requires seting up a project at
 * localcontexts that has the PID of the test dataset and an api key, these
 * tests are just checking for various error conditions - if the LocalContexts
 * settings aren't set, if the LC server doesn't respond with a 200, if the user
 * can't edit the dataset.
 * 
 */
@LocalJvmSettings
public class LocalContextsIT {

    private static final String RANDOM_API_KEY = "random_api_key_12345";

    @ParameterizedTest
    @MethodSource("localContextsTestCases")
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_URL, value = "https://localcontexts.org/")
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_API_KEY, value = RANDOM_API_KEY)
    public void testGetDatasetLocalContexts(String testCase) {
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
                .statusCode(SERVICE_UNAVAILABLE.getStatusCode());

        // Test with a second user
        Response createSecondUser = UtilIT.createRandomUser();
        String secondUsername = UtilIT.getUsernameFromResponse(createSecondUser);
        String secondApiToken = UtilIT.getApiTokenFromResponse(createSecondUser);

        Response secondUserResponse;
        if (testCase.equals("searchLocalContexts")) {
            secondUserResponse = UtilIT.searchLocalContexts(datasetId.toString(), secondApiToken);
            secondUserResponse.then().assertThat()
                    .statusCode(FORBIDDEN.getStatusCode());
        } else {
            String projectId = "sample_project_id";
            secondUserResponse = UtilIT.getLocalContextsProject(datasetId.toString(), projectId, secondApiToken);
            secondUserResponse.then().assertThat()
                    .statusCode(SERVICE_UNAVAILABLE.getStatusCode());
        }

        // Clean up
        UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        UtilIT.deleteDataverse(dataverseAlias, apiToken);
        UtilIT.deleteUser(username);
        UtilIT.deleteUser(secondUsername);
    }

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