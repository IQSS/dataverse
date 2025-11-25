package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.response.Response;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class WorkflowsIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        UtilIT.deleteWorkflowIpWhitelist();
    }

    @AfterAll
    public static void afterClass() {
    }

    @Test
    public void testIpWhitelist() {
        Response response = null;

        response = UtilIT.getWorkflowIpWhitelist();
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("127.0.0.1;::1"));

        String testIp = "192.168.0.1;192.168.0.2";
        
        response = UtilIT.setWorkflowIpWhitelist("junk");
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Request contains illegal IP addresses."));

        response = UtilIT.setWorkflowIpWhitelist(testIp);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        response = UtilIT.getWorkflowIpWhitelist();
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo(testIp));
        
        response = given().when().get("/api/admin/settings/" + SettingsServiceBean.Key.WorkflowsAdminIpWhitelist);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo(testIp));
    }

}
