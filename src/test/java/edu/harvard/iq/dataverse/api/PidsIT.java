package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.OK;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class PidsIT {

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    /**
     * In order to execute this test code you must be configured with DataCite
     * credentials.
     */
    @Ignore
    @Test
    public void testGetPid() {
        String pid = "";
        pid = "10.70122/FK2/9BXT5O"; // findable
        pid = "10.70122/FK2/DNEUDP"; // draft

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response getPid = UtilIT.getPid(pid, apiToken);
        getPid.prettyPrint();
    }

}
