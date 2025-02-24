package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import edu.harvard.iq.dataverse.api.UtilIT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static jakarta.ws.rs.core.Response.Status.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@LocalJvmSettings
public class LocalContextsIT {

    private static final String RANDOM_API_KEY = "random_api_key_12345";

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_URL, value = "https://localcontexts.org/")
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_API_KEY, value = RANDOM_API_KEY)
    public void testGetDatasetLocalContexts() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        // Test getDatasetLocalContexts
        Response getLocalContextsDataResponse = UtilIT.searchLocalContexts(datasetId.toString(), apiToken);
        getLocalContextsDataResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("$", not(empty()));

        // Clean up
        UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        UtilIT.deleteDataverse(dataverseAlias, apiToken);
        UtilIT.deleteUser(username);
    }

    @Test
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_URL, value = "https://localcontexts.org/")
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_API_KEY, value = RANDOM_API_KEY)
    public void testGetProjectLocalContexts() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        // Test getProjectLocalContexts
        // Note: You might need to use a valid projectId here
        String projectId = "sample_project_id";
        Response getProjectLocalContextsResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/localcontexts/datasets/" + datasetId + "/" + projectId);

        getProjectLocalContextsResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("$", not(empty()));

        // Clean up
        UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        UtilIT.deleteDataverse(dataverseAlias, apiToken);
        UtilIT.deleteUser(username);
    }

    @Test
    public void testLocalContextsWithoutConfiguration() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        // Test getDatasetLocalContexts without configuration
        Response getLocalContextsDataResponse = UtilIT.searchLocalContexts(datasetId.toString(), apiToken);
        getLocalContextsDataResponse.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode())
                .body("message", equalTo("LocalContexts API configuration is missing."));

        // Clean up
        UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        UtilIT.deleteDataverse(dataverseAlias, apiToken);
        UtilIT.deleteUser(username);
    }
}