package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import static io.restassured.RestAssured.given;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.util.json.JsonUtil;

import static jakarta.ws.rs.core.Response.Status.*;

public class ReviewsIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void createReview() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createCollection = UtilIT.createRandomDataverse(apiToken);
        createCollection.prettyPrint();
        createCollection.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String parentCollection = UtilIT.getAliasFromResponse(createCollection);

        String jsonIn = """
                {}
                """;
        JsonObject jsonObject = JsonUtil.getJsonObject(jsonIn);

        Response createReview = createReview(parentCollection, jsonObject, apiToken);
        createReview.prettyPrint();
        createReview.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    // TODO move this to UtilIT
    private Response createReview(String parentCollection, JsonObject jsonObject, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonObject.toString())
                .contentType(ContentType.JSON)
                .post("/api/dataverses/" + parentCollection + "/reviews");
        return response;
    }

    // TODO delete when local methods are moved to UtilIT
    public static final String API_TOKEN_HTTP_HEADER = "X-Dataverse-key";

}
