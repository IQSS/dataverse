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
                {
                  "datasetVersion": {
                    "license": {
                      "name": "CC0 1.0",
                      "uri": "http://creativecommons.org/publicdomain/zero/1.0"
                    },
                    "metadataBlocks": {
                      "citation": {
                        "fields": [
                          {
                            "value": "Review of Darwin's Finches",
                            "typeClass": "primitive",
                            "multiple": false,
                            "typeName": "title"
                          },
                          {
                            "value": [
                              {
                                "authorName": {
                                  "value": "Simpson, Homer",
                                  "typeClass": "primitive",
                                  "multiple": false,
                                  "typeName": "authorName"
                                }
                              }
                            ],
                            "typeClass": "compound",
                            "multiple": true,
                            "typeName": "author"
                          },
                          {
                            "value": [
                                { "datasetContactEmail" : {
                                    "typeClass": "primitive",
                                    "multiple": false,
                                    "typeName": "datasetContactEmail",
                                    "value" : "hsimpson@mailinator.com"
                                },
                                "datasetContactName" : {
                                    "typeClass": "primitive",
                                    "multiple": false,
                                    "typeName": "datasetContactName",
                                    "value": "Simpson, Homer"
                                }
                            }],
                            "typeClass": "compound",
                            "multiple": true,
                            "typeName": "datasetContact"
                          },
                          {
                            "value": [ {
                               "dsDescriptionValue":{
                                "value":   "This is a review of the Darwin's Finches dataset.",
                                "multiple":false,
                               "typeClass": "primitive",
                               "typeName": "dsDescriptionValue"
                            }}],
                            "typeClass": "compound",
                            "multiple": true,
                            "typeName": "dsDescription"
                          },
                          {
                            "value": [
                              "Other"
                            ],
                            "typeClass": "controlledVocabulary",
                            "multiple": true,
                            "typeName": "subject"
                          }
                        ],
                        "displayName": "Citation Metadata"
                      }
                    }
                  }
                }
                """;
        JsonObject jsonObject = JsonUtil.getJsonObject(jsonIn);

        Response createReview = createReview(parentCollection, jsonObject, apiToken);
        createReview.prettyPrint();
        createReview.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Response listReviews = listReviews(apiToken);
        // TODO: get this to print some reviews!
        listReviews.prettyPrint();
        listReviews.then().assertThat()
                .statusCode(OK.getStatusCode());

    }

    // TODO move methods to UtilIT
    private Response createReview(String parentCollection, JsonObject jsonObject, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonObject.toString())
                .contentType(ContentType.JSON)
                .post("/api/reviews?parentCollection=" + parentCollection);
        return response;
    }

    private Response listReviews(String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/reviews");
        return response;
    }

    // TODO delete when local methods are moved to UtilIT
    public static final String API_TOKEN_HTTP_HEADER = "X-Dataverse-key";

}
