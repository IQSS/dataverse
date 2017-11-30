package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.BeforeClass;
import org.junit.Test;

public class SummaryStatsIT {

    public SummaryStatsIT() {
    }

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testSomeMethod() {
        Response createUser = UtilIT.createRandomUser();
        JsonObject jsonObject = Json.createObjectBuilder().build();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);
        // TODO: Rather than hard coding a file id, create a dataset and upload a file.
        long fileId = 12;
        // FIXME: What should the permisions be?
        UtilIT.makeSuperUser(username);
        Response response = UtilIT.processPrepFile(jsonObject, fileId, apiToken);
        response.prettyPrint();
    }

}
