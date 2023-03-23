package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.api.auth.ApiKeyAuthMechanism;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataRetrieverApiIT {

    @BeforeClass
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
        assertEquals("{\"success\":false,\"error_message\":\"No user found for: \\\"" + badUserIdentifier + "\\\"\"}", invalidUserIdentifierResponse.prettyPrint());
        assertEquals(OK.getStatusCode(), invalidUserIdentifierResponse.getStatusCode());

        // Call as superuser with valid user identifier
        Response createSecondUserResponse = UtilIT.createRandomUser();
        String userIdentifier = UtilIT.getUsernameFromResponse(createSecondUserResponse);
        Response validUserIdentifierResponse = UtilIT.retrieveMyDataAsJsonString(superUserApiToken, userIdentifier, emptyRoleIdsList);
        assertEquals("{\"success\":false,\"error_message\":\"Sorry, you have no assigned roles.\"}", validUserIdentifierResponse.prettyPrint());
        assertEquals(OK.getStatusCode(), validUserIdentifierResponse.getStatusCode());
    }
}
