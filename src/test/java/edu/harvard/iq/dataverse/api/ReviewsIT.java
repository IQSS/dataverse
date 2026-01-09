package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.dataset.DatasetType;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ReviewsIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());

        ensureDatasetTypeIsPresent(DatasetType.DATASET_TYPE_REVIEW, apiToken);
    }

    private static void ensureDatasetTypeIsPresent(String datasetType, String apiToken) {
        Response getDatasetType = UtilIT.getDatasetType(datasetType);
        getDatasetType.prettyPrint();
        String typeFound = JsonPath.from(getDatasetType.getBody().asString()).getString("data.name");
        System.out.println("type found: " + typeFound);
        if (datasetType.equals(typeFound)) {
            return;
        }
        System.out.println("The " + datasetType + "type wasn't found. Create it.");
        String displayName = capitalize(datasetType);
        String jsonIn = Json.createObjectBuilder()
                .add("name", datasetType)
                .add("displayName", displayName)
                .build().toString();
        Response typeAdded = UtilIT.addDatasetType(jsonIn, apiToken);
        typeAdded.prettyPrint();
        typeAdded.then().assertThat().statusCode(OK.getStatusCode());
    }

    private static String capitalize(String stringIn) {
        return stringIn.substring(0, 1).toUpperCase() + stringIn.substring(1);
    }

    @Test
    public void testCreateReview() {

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

        Response setMetadataBlocks = UtilIT.setMetadataBlocks(dataverseAlias,
                Json.createArrayBuilder().add("citation").add("review"), apiToken);
        setMetadataBlocks.prettyPrint();
        setMetadataBlocks.then().assertThat().statusCode(OK.getStatusCode());

        String[] testInputLevelNames = { "itemReviewedUrl", "itemReviewedType" };
        boolean[] testRequiredInputLevels = { true, true };
        boolean[] testIncludedInputLevels = { true, true };
        Response updateDataverseInputLevelsResponse = UtilIT.updateDataverseInputLevels(dataverseAlias,
                testInputLevelNames, testRequiredInputLevels, testIncludedInputLevels, apiToken);
        updateDataverseInputLevelsResponse.prettyPrint();
        updateDataverseInputLevelsResponse.then().assertThat().statusCode(OK.getStatusCode());

        String itemReviewedTitle = "Percent of Children That Have Asthma";
        String itemReviewedUrl = "https://datacommons.org/tools/statvar#sv=Percent_Person_Children_WithAsthma";
        String reviewTitle = "Review of " + itemReviewedTitle;
        String authorName = "Wazowski, Mike";
        String authorEmail = "mwazowski@mailinator.com";
        JsonObjectBuilder jsonForCreatingReview = Json.createObjectBuilder()
                /**
                 * See above where this type is added to the installation and
                 * therefore available for use.
                 */
                .add("datasetType", DatasetType.DATASET_TYPE_REVIEW)
                .add("datasetVersion", Json.createObjectBuilder()
                        .add("license", Json.createObjectBuilder()
                                .add("name", "CC0 1.0")
                                .add("uri", "http://creativecommons.org/publicdomain/zero/1.0"))
                        .add("metadataBlocks", Json.createObjectBuilder()
                                .add("citation", Json.createObjectBuilder()
                                        .add("fields", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "title")
                                                        .add("value", reviewTitle)
                                                        .add("typeClass", "primitive")
                                                        .add("multiple", false))
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("authorName",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", authorName)
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName",
                                                                                                "authorName"))))
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("typeName", "author"))
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("datasetContactEmail",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", authorEmail)
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName",
                                                                                                "datasetContactEmail"))))
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("typeName", "datasetContact"))
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("dsDescriptionValue",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value",
                                                                                                "This is a review of a dataset.")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName",
                                                                                                "dsDescriptionValue"))))
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("typeName", "dsDescription"))
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add("Medicine, Health and Life Sciences"))
                                                        .add("typeClass", "controlledVocabulary")
                                                        .add("multiple", true)
                                                        .add("typeName", "subject"))
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createObjectBuilder()
                                                                .add("itemReviewedUrl",
                                                                        Json.createObjectBuilder()
                                                                                .add("value", itemReviewedUrl)
                                                                                .add("typeClass", "primitive")
                                                                                .add("multiple", false)
                                                                                .add("typeName", "itemReviewedUrl"))
                                                                .add("itemReviewedType",
                                                                        Json.createObjectBuilder()
                                                                                .add("value", "Dataset")
                                                                                .add("typeClass",
                                                                                        "controlledVocabulary")
                                                                                .add("multiple", false)
                                                                                .add("typeName", "itemReviewedType")))
                                                        .add("typeClass", "compound")
                                                        .add("multiple", false)
                                                        .add("typeName", "itemReviewed"))))));

        Response createReview = UtilIT.createDataset(dataverseAlias, jsonForCreatingReview, apiToken);
        createReview.prettyPrint();
        createReview.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer reviewId = UtilIT.getDatasetIdFromResponse(createReview);
        String reviewPid = JsonPath.from(createReview.getBody().asString()).getString("data.persistentId");

    }

}
