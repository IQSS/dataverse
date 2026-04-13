package edu.harvard.iq.dataverse.api;

import static edu.harvard.iq.dataverse.api.ApiConstants.DS_VERSION_LATEST_PUBLISHED;

import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * This test class has not been added to the API test suite at
 * tests/integration-tests.txt because it relies on the review.tsv which is not
 * loaded out of the box. When we start loading review.tsv for new installations
 * of Dataverse (and stop putting it under "experiemental" on the list of
 * metadata blocks in the guides), we'll add this test class to the API test
 * suite.
 * 
 * To run these tests, manually load review.tsv (or temporarily set
 * loadReviewTsv to true below) and update Solr. Be advised that there are
 * other places in the API test suite that make assertions on the number of
 * metadata blocks. Again, some day we might ship Dataverse with review.tsv
 * already loaded.
 */
public class ReviewsIT {

    private static String apiTokenSuperuser;

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String usernameSuperuser = UtilIT.getUsernameFromResponse(createUser);
        apiTokenSuperuser = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(usernameSuperuser, true).then().assertThat().statusCode(OK.getStatusCode());

        byte[] reviewTsv = null;
        try {
            reviewTsv = Files.readAllBytes(Paths.get("scripts/api/data/metadatablocks/review.tsv"));
        } catch (IOException e) {
        }

        // See warnings above. If you enable this, don't forget to update Solr.
        boolean loadReviewTsv = false;
        if (loadReviewTsv) {
            Response response = UtilIT.loadMetadataBlock(apiTokenSuperuser, reviewTsv);
            response.prettyPrint();
            assertEquals(200, response.getStatusCode());
            response.then().assertThat().statusCode(OK.getStatusCode());
        }

        String datasetDescription = "A study, experiment, set of observations, or publication that is uploaded by a user. A dataset can comprise a single file or multiple files.";
        ensureDatasetTypeIsPresent(DatasetType.DATASET_TYPE_DATASET, "Dataset", datasetDescription, apiTokenSuperuser);

        String reviewDescription = null;
        try {
            reviewDescription = JsonUtil.getJsonObjectFromFile("scripts/api/data/datasetTypes/review.json").getString("description");
        } catch (IOException e) {
        }
        ensureDatasetTypeIsPresent(DatasetType.DATASET_TYPE_REVIEW, "Review", reviewDescription, apiTokenSuperuser);
    }

    private static void ensureDatasetTypeIsPresent(String name, String displayName, String description,
            String apiToken) {
        Response getDatasetType = UtilIT.getDatasetType(name);
        getDatasetType.prettyPrint();
        String nameFound = JsonPath.from(getDatasetType.getBody().asString()).getString("data.name");
        String displayNameFound = JsonPath.from(getDatasetType.getBody().asString()).getString("data.displayName");
        String descriptionFound = JsonPath.from(getDatasetType.getBody().asString()).getString("data.description");
        System.out.println("Found: name=" + nameFound + ". Display name=" + displayNameFound + ". Description="
                + descriptionFound);
        if (name.equals(nameFound)) {
            System.out.println(name + "=" + nameFound + ". Exists. No need to create. Returning.");
            return;
        } else {
            System.out.println(name + " wasn't found. Create it.");
        }
        String jsonIn = NullSafeJsonBuilder.jsonObjectBuilder()
                .add("name", name)
                .add("displayName", displayName)
                .add("description", description)
                .add("linkedMetadataBlocks", Json.createArrayBuilder()
                        .add("review")
                )
                .build().toString();
        // System.out.println(JsonUtil.prettyPrint(jsonIn));
        Response typeAdded = UtilIT.addDatasetType(jsonIn, apiToken);
        typeAdded.prettyPrint();
        typeAdded.then().assertThat().statusCode(OK.getStatusCode());
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

        Response setAllowedDatasetTypes = UtilIT.setCollectionAttribute(dataverseAlias, "allowedDatasetTypes",
                "review", apiTokenSuperuser);
        setAllowedDatasetTypes.prettyPrint();
        setAllowedDatasetTypes.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.allowedDatasetTypes[0].name", is("review"))
                .body("data.allowedDatasetTypes[0].displayName", is("Review"))
                .body("data.allowedDatasetTypes[0].description", is("A review of a dataset compiled by the expert community."));

        String itemReviewedTitle = "Percent of Children That Have Asthma";
        String itemReviewedUrl = "https://datacommons.org/tools/statvar#sv=Percent_Person_Children_WithAsthma";
        // This citation came from https://www.mybib.com
        String itemReviewedCitation = "\"Statistical Variable Explorer - Data Commons.\" Datacommons.org, 2026, datacommons.org/tools/statvar#sv=Percent_Person_Children_WithAsthma. Accessed 9 Mar. 2026.";
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
                                                                                .add("typeName", "itemReviewedType"))
                                                                .add("itemReviewedCitation",
                                                                        Json.createObjectBuilder()
                                                                                .add("value", itemReviewedCitation)
                                                                                .add("typeClass", "primitive")
                                                                                .add("multiple", false)
                                                                                .add("typeName", "itemReviewedCitation")))
                                                        .add("typeClass", "compound")
                                                        .add("multiple", false)
                                                        .add("typeName", "itemReviewed"))))));

        Response createReview = UtilIT.createDataset(dataverseAlias, jsonForCreatingReview, apiToken);
        createReview.prettyPrint();
        createReview.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer reviewId = UtilIT.getDatasetIdFromResponse(createReview);
        String reviewPid = JsonPath.from(createReview.getBody().asString()).getString("data.persistentId");

    }

    /**
     * In this test, we check if temReviewedUrl and itemReviewedType are required. (They are subfields of itemReviewed.) In review.tsv they are set to required.
     */
    @Test
    public void testCreateReviewRequiredFields() {

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

        Response setAllowedDatasetTypes = UtilIT.setCollectionAttribute(dataverseAlias, "allowedDatasetTypes",
                "review", apiTokenSuperuser);
        setAllowedDatasetTypes.prettyPrint();
        setAllowedDatasetTypes.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.allowedDatasetTypes[0].name", is("review"))
                .body("data.allowedDatasetTypes[0].displayName", is("Review"))
                .body("data.allowedDatasetTypes[0].description",
                        is("A review of a dataset compiled by the expert community."));

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
                                                        .add("typeName", "subject"))))));

        Response createReview = UtilIT.createDataset(dataverseAlias, jsonForCreatingReview, apiToken);
        createReview.prettyPrint();
        // FIXME: The review was created but it shouldn't have been because
        // required fields were not supplied. In review.tsv various fields
        // are required. See https://github.com/IQSS/dataverse/issues/12196
        createReview.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer reviewId = UtilIT.getDatasetIdFromResponse(createReview);
        String reviewPid = JsonPath.from(createReview.getBody().asString()).getString("data.persistentId");

    }

    @Test
    public void testLocalReviews() {

        Response createUserDatasetAuthor = UtilIT.createRandomUser();
        createUserDatasetAuthor.prettyPrint();
        createUserDatasetAuthor.then().assertThat()
                .statusCode(OK.getStatusCode());
        String usernameDatasetAuthor = UtilIT.getUsernameFromResponse(createUserDatasetAuthor);
        String apiTokenDatasetAuthor = UtilIT.getApiTokenFromResponse(createUserDatasetAuthor);

        Response createCollectionOfDatasets = UtilIT.createRandomDataverse(apiTokenDatasetAuthor);
        createCollectionOfDatasets.prettyPrint();
        createCollectionOfDatasets.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String collectionAliasDatasets = UtilIT.getAliasFromResponse(createCollectionOfDatasets);
        String datasetJson = """
                {
                  "http://purl.org/dc/terms/title": "Pediatric Asthma",
                  "http://purl.org/dc/terms/creator": {
                      "https://dataverse.org/schema/citation/authorName": "Sullivan, James"
                  },
                  "https://dataverse.org/schema/citation/datasetContact": {
                    "https://dataverse.org/schema/citation/datasetContactEmail": "sully@mailinator.com"
                  },
                  "https://dataverse.org/schema/citation/dsDescription": {
                    "https://dataverse.org/schema/citation/dsDescriptionValue": "A dataset about pediatric asthma."
                  },
                  "http://purl.org/dc/terms/subject": "Medicine, Health and Life Sciences"
                }
                """;

        Response createDataset = UtilIT.createDatasetSemantic(collectionAliasDatasets, datasetJson,
                apiTokenDatasetAuthor);
        createDataset.prettyPrint();
        createDataset.then().assertThat().statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        Response setLicensetoCC0 = UtilIT.updateLicense(datasetId.toString(), "{ \"name\": \"CC0 1.0\" }",
                apiTokenDatasetAuthor);
        setLicensetoCC0.prettyPrint();
        setLicensetoCC0.then().assertThat().statusCode(OK.getStatusCode());

        Response publishCollection = UtilIT.publishDataverseViaNativeApi(collectionAliasDatasets,
                apiTokenDatasetAuthor);
        // publishCollection.prettyPrint();
        publishCollection.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiTokenDatasetAuthor);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUserReviewer = UtilIT.createRandomUser();
        createUserReviewer.prettyPrint();
        createUserReviewer.then().assertThat()
                .statusCode(OK.getStatusCode());
        String usernameReviewer = UtilIT.getUsernameFromResponse(createUserReviewer);
        String apiTokenReviewer = UtilIT.getApiTokenFromResponse(createUserReviewer);

        Response getDataset = UtilIT.nativeGetUsingPersistentId(datasetPid, apiTokenReviewer);
        getDataset.prettyPrint();
        getDataset.then().assertThat().statusCode(OK.getStatusCode());

        String datasetPersistentUrl = JsonPath.from(getDataset.body().asString()).getString("data.persistentUrl");
        String datasetTitle = JsonPath.from(getDataset.body().asString())
                .getString("data.latestVersion.metadataBlocks.citation.fields[0].value");

        Response getCitation = UtilIT.getDatasetVersionCitation(datasetId, DS_VERSION_LATEST_PUBLISHED, false,
                apiTokenReviewer);
        getCitation.prettyPrint();
        getCitation.then().assertThat().statusCode(OK.getStatusCode());
        String datasetCitationHtml = JsonPath.from(getCitation.getBody().asString()).getString("data.message");
        String datasetCitationText = StringUtil.html2text(datasetCitationHtml);

        Response createCollectionOfReviews = UtilIT.createRandomDataverse(apiTokenReviewer);
        createCollectionOfReviews.prettyPrint();
        createCollectionOfReviews.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String collectionAliasReviews = UtilIT.getAliasFromResponse(createCollectionOfReviews);

        Response setAllowedDatasetTypes = UtilIT.setCollectionAttribute(collectionAliasReviews, "allowedDatasetTypes",
                "review", apiTokenSuperuser);
        setAllowedDatasetTypes.prettyPrint();
        setAllowedDatasetTypes.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.allowedDatasetTypes[0].name", is("review"))
                .body("data.allowedDatasetTypes[0].displayName", is("Review"))
                .body("data.allowedDatasetTypes[0].description",
                        is("A review of a dataset compiled by the expert community."));

        String itemReviewedTitle = datasetTitle;
        String itemReviewedUrl = datasetPersistentUrl;
        String itemReviewedCitation = datasetCitationHtml;
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
                                                                                .add("typeName", "itemReviewedType"))
                                                                .add("itemReviewedCitation",
                                                                        Json.createObjectBuilder()
                                                                                .add("value", itemReviewedCitation)
                                                                                .add("typeClass", "primitive")
                                                                                .add("multiple", false)
                                                                                .add("typeName",
                                                                                        "itemReviewedCitation")))
                                                        .add("typeClass", "compound")
                                                        .add("multiple", false)
                                                        .add("typeName", "itemReviewed"))))));

        Response createReview = UtilIT.createDataset(collectionAliasReviews, jsonForCreatingReview, apiTokenReviewer);
        createReview.prettyPrint();
        createReview.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer reviewId = UtilIT.getDatasetIdFromResponse(createReview);
        String reviewPid = JsonPath.from(createReview.getBody().asString()).getString("data.persistentId");

        Response publishCollectionReviews = UtilIT.publishDataverseViaNativeApi(collectionAliasReviews,
                apiTokenReviewer);
        publishCollectionReviews.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishReview = UtilIT.publishDatasetViaNativeApi(reviewId, "major", apiTokenReviewer);
        publishReview.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Putting PID as URL in quotes to avoid hits we don't want
        Response searchForReviews = UtilIT.search("itemReviewedUrl:\"" + itemReviewedUrl + "\"", null);
        searchForReviews.prettyPrint();
        searchForReviews.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items[0].name", is(reviewTitle));

        Response getReviews = UtilIT.getReviews(datasetPid);
        getReviews.prettyPrint();
        getReviews.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.reviews[0].title", is(reviewTitle))
                .body("data.reviews[0].persistentId", is(reviewPid))
                .body("data.reviews[0].id", is(reviewId));
    }

}
