package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogoutIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testLogout() {
        // This branch enables the api-session-auth feature flag (required by
        // the reusable-components React bundle), so /api/logout is active and
        // validates the session. UtilIT.logout() sends no session cookie, so
        // the endpoint rejects it with 400 ("No valid session cookie was
        // sent"). (With the flag off, upstream's default, it returns 500.)
        Response logoutResponse = UtilIT.logout();
        assertEquals(BAD_REQUEST.getStatusCode(), logoutResponse.getStatusCode());
    }
}
