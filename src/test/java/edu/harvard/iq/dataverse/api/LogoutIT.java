package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogoutIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testLogout() {
        // Test failure because feature flag is turned off
        Response logoutResponse = UtilIT.logout();
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), logoutResponse.getStatusCode());
    }
}
