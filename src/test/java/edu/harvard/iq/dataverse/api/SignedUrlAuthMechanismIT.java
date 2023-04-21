package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

import static com.jayway.restassured.RestAssured.get;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SignedUrlAuthMechanismIT {

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testSignedUrlAuthMechanism() {
        // Test user setup
        Response createUserResponse = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUserResponse);
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        UtilIT.makeSuperUser(username);

        // Test dataset setup
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);

        // Valid Signed URL behavior
        String apiPath = String.format("/api/v1/datasets/:persistentId/?persistentId=%s", datasetPersistentId);
        Response createSignedUrlResponse = UtilIT.createSignedUrl(apiToken, apiPath, username);
        String signedUrl = UtilIT.getSignedUrlFromResponse(createSignedUrlResponse);
        Response signedUrlResponse = get(signedUrl);
        assertEquals(OK.getStatusCode(), signedUrlResponse.getStatusCode());

        // Invalid Signed URL behavior
        String invalidSignedUrlPath = String.format("/api/v1/datasets/:persistentId/?persistentId=%s&until=2999-01-01T23:59:29.855&user=dataverseAdmin&method=GET&token=invalidToken", datasetPersistentId);
        Response invalidSignedUrlResponse = get(invalidSignedUrlPath);
        assertEquals(UNAUTHORIZED.getStatusCode(), invalidSignedUrlResponse.getStatusCode());
    }
}
