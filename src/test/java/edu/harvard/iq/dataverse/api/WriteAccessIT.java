package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.BeforeClass;
import org.junit.Test;

/* 
TODO: use this class for RestAssuered tests of of WRITE (POST, PUT) Access API calls. -- L.A.
*/

public class WriteAccessIT {

    public WriteAccessIT() {
    }

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    
    @Test
    public void testSubmitDataSummaryViaAPI() {
        Response createUser = UtilIT.createRandomUser();
        JsonObject jsonObject = Json.createObjectBuilder().build();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);
        // TODO: Rather than hard coding a file id, create a dataset and upload a file.
        // Yes, this test will fail, unless there is an existing,
        // already uploaded file, *that also must be TABULAR*. -- L.A.
        long fileId = 12;
        // FIXME: What should the permisions be?
        UtilIT.makeSuperUser(username);
        Response response = UtilIT.submitDataSummaryForCaching(jsonObject, fileId, apiToken);
        response.prettyPrint();
    }
    
}
