package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FeedbackApiIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testSupportRequest() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("fromEmail", "from@mailinator.com");
        job.add("subject", "Help!");
        job.add("body", "I need help.");

        Response response = UtilIT.submitFeedback(job);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fromEmail", CoreMatchers.equalTo("from@mailinator.com"));
    }

    @Test
    public void testSubmitFeedbackOnRootDataverse() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        long rootDataverseId = 1;
        job.add("id", rootDataverseId);
        job.add("fromEmail", "from@mailinator.com");
        job.add("toEmail", "to@mailinator.com");
        job.add("subject", "collaboration");
        job.add("body", "Are you interested writing a grant based on this research?");

        Response response = UtilIT.submitFeedback(job);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testSubmitFeedbackOnDataset() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String pathToJsonFile = "scripts/api/data/dataset-create-new-all-default-fields.json";
        Response createDataset = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);

        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        long datasetId = JsonPath.from(createDataset.body().asString()).getLong("data.id");
        String pid = JsonPath.from(createDataset.body().asString()).getString("data.persistentId");

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("id", datasetId);
        job.add("fromEmail", "from@mailinator.com");
        job.add("toEmail", "to@mailinator.com");
        job.add("subject", "collaboration");
        job.add("body", "Are you interested writing a grant based on this research?");

        Response response = UtilIT.submitFeedback(job);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].toEmail", CoreMatchers.equalTo("ContactEmail1@mailinator.com"));
    }

}
