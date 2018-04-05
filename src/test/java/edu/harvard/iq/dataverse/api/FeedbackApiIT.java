package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.response.Response;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;

public class FeedbackApiIT {

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
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

        Response response = given()
                .body(job.build().toString())
                .contentType("application/json")
                .post("/api/admin/feedback");
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(200)
                .body("data.body", CoreMatchers.equalTo("The message below was sent from the Contact button at " + RestAssured.baseURI + "/dataverse/root\n\nAre you interested writing a grant based on this research?"));
    }

}
