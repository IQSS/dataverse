package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.BundleUtil;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.text.MessageFormat;

import static jakarta.ws.rs.core.Response.Status.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataverseFeaturedItemsIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testCreateFeaturedItemWithDvOdbject() {
        String apiToken = createUserAndGetApiToken();
        String dataverseAlias = createDataverseAndGetAlias(apiToken);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).prettyPrint();

        Response createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, null, "dataset", datasetPersistentId);
        createFeatureItemResponse.prettyPrint();
        createFeatureItemResponse.then().assertThat().statusCode(OK.getStatusCode());
        JsonPath createdFeaturedItem = JsonPath.from(createFeatureItemResponse.body().asString());
        String dvObjectIdentifier = createdFeaturedItem.getString("data.dvObjectIdentifier");
        assertEquals(datasetPersistentId, dvObjectIdentifier);

        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, null, "dataverse", dataverseAlias);
        createFeatureItemResponse.prettyPrint();
        createFeatureItemResponse.then().assertThat().statusCode(OK.getStatusCode());
        createdFeaturedItem = JsonPath.from(createFeatureItemResponse.body().asString());
        dvObjectIdentifier = createdFeaturedItem.getString("data.dvObjectIdentifier");
        assertEquals(dataverseAlias, dvObjectIdentifier);

        String pathToFile = "scripts/search/data/tabular/50by1000.dta";
        Response uploadFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFileResponse.prettyPrint();
        JsonPath uploadedFile = JsonPath.from(uploadFileResponse.body().asString());
        String fileId = String.valueOf(uploadedFile.getInt("data.files[0].dataFile.id"));
        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, null, "datafile", fileId);
        createFeatureItemResponse.prettyPrint();
        createFeatureItemResponse.then().assertThat().statusCode(OK.getStatusCode());
        createdFeaturedItem = JsonPath.from(createFeatureItemResponse.body().asString());
        dvObjectIdentifier = createdFeaturedItem.getString("data.dvObjectIdentifier");
        assertEquals(fileId, dvObjectIdentifier);
    }

    @Test
    public void testCreateFeaturedItemWithBadDvOdbjectIds() {
        String apiToken = createUserAndGetApiToken();
        String dataverseAlias = createDataverseAndGetAlias(apiToken);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse) + "BAD";
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).prettyPrint();
        String datasetPersistentIdBad = datasetPersistentId + "BAD";
        String dataverseAliasBad = dataverseAlias + "BAD";
        String fieldIdBad = "999";

        // Test with bad Dataset id should return bad request with not found message
        Response createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, null, "dataset", datasetPersistentIdBad);
        createFeatureItemResponse.prettyPrint();
        createFeatureItemResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(BundleUtil.getStringFromBundle("find.dvo.error.dvObjectNotFound", List.of(datasetPersistentIdBad))));

        // Test with bad dataverse alias should return bad request with not found message
        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, null, "dataverse", dataverseAliasBad);
        createFeatureItemResponse.prettyPrint();
        createFeatureItemResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(BundleUtil.getStringFromBundle("find.dvo.error.dvObjectNotFound", List.of(dataverseAliasBad))));

        String pathToFile = "scripts/search/data/tabular/50by1000.dta";
        Response uploadFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFileResponse.prettyPrint();
        JsonPath uploadedFile = JsonPath.from(uploadFileResponse.body().asString());

        // Test with bad Datafile id should return bad request with not found message
        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, null, "datafile", fieldIdBad);
        createFeatureItemResponse.prettyPrint();
        createFeatureItemResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(BundleUtil.getStringFromBundle("find.dvo.error.dvObjectNotFound", List.of(fieldIdBad))));
    }

    @Test
    public void testUnpublishedPublishedDatasetFeatureItem() {
        String apiToken = createUserAndGetApiToken();
        String dataverseAlias = createDataverseAndGetAlias(apiToken);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        String datasetId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);
        Response createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, null, "dataset", datasetId);
        createFeatureItemResponse.prettyPrint();
        createFeatureItemResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.notPublished", List.of("Dataset"))));
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).prettyPrint();
        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, null, "dataset", datasetId);
        createFeatureItemResponse.prettyPrint();
        createFeatureItemResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testRestrictedUnrestrictedDatafileFeatureItem() {
        String apiToken = createUserAndGetApiToken();
        String dataverseAlias = createDataverseAndGetAlias(apiToken);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        String datasetId = String.valueOf(UtilIT.getDatasetIdFromResponse(createDatasetResponse));
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).prettyPrint();

        String pathToFile = "scripts/search/data/tabular/50by1000.dta";
        Response uploadFileResponse = UtilIT.uploadFileViaNative(datasetId, pathToFile, apiToken);
        uploadFileResponse.prettyPrint();
        JsonPath uploadedFile = JsonPath.from(uploadFileResponse.body().asString());
        String fileId = String.valueOf(uploadedFile.getInt("data.files[0].dataFile.id"));
        assertTrue(UtilIT.sleepForLock(datasetId, "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration");

        UtilIT.restrictFile(fileId, true, apiToken).prettyPrint();

        Response createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, null, "datafile", fileId);
        createFeatureItemResponse.prettyPrint();
        createFeatureItemResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.restricted")));

        UtilIT.restrictFile(fileId, false, apiToken).prettyPrint();
        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, null, "datafile", fileId);
        createFeatureItemResponse.prettyPrint();
        createFeatureItemResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
    }
    @Test
    public void testDeleteFeaturedItem() {
        String apiToken = createUserAndGetApiToken();
        String dataverseAlias = createDataverseAndGetAlias(apiToken);
        Long featuredItemId = createFeaturedItemAndGetId(dataverseAlias, apiToken, "src/test/resources/images/coffeeshop.png");

        // Should return not found when passing incorrect item id
        Response deleteFeatureItemResponse = UtilIT.deleteDataverseFeaturedItem(100000L, apiToken);
        deleteFeatureItemResponse.then()
                .body("message", equalTo(MessageFormat.format(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.notFound"), 100000L)))
                .assertThat().statusCode(NOT_FOUND.getStatusCode());

        // Should return unauthorized when passing correct id and user does not have permissions
        String randomUserApiToken = createUserAndGetApiToken();
        deleteFeatureItemResponse = UtilIT.deleteDataverseFeaturedItem(featuredItemId, randomUserApiToken);
        deleteFeatureItemResponse.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // Should delete featured item when passing correct id and user has permissions
        deleteFeatureItemResponse = UtilIT.deleteDataverseFeaturedItem(featuredItemId, apiToken);
        deleteFeatureItemResponse.then()
                .body("data.message", equalTo(MessageFormat.format(BundleUtil.getStringFromBundle("dataverseFeaturedItems.delete.successful"), featuredItemId)))
                .assertThat().statusCode(OK.getStatusCode());

        Response listFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, apiToken);
        listFeaturedItemsResponse.then()
                .body("data.size()", equalTo(0))
                .assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testUpdateFeaturedItem() {
        String apiToken = createUserAndGetApiToken();
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        Long featuredItemId = createFeaturedItemAndGetId(dataverseAlias, apiToken, "src/test/resources/images/coffeeshop.png");

        // Should return not found when passing incorrect item id
        Response updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(100000L, "updatedTitle", 1, false, null, apiToken);
        updateFeatureItemResponse.then()
                .body("message", equalTo(MessageFormat.format(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.notFound"), 100000L)))
                .assertThat().statusCode(NOT_FOUND.getStatusCode());

        // Should return unauthorized when passing correct id and user does not have permissions
        String randomUserApiToken = createUserAndGetApiToken();
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, "updatedTitle", 1, false, null, randomUserApiToken);
        updateFeatureItemResponse.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // Update featured item: keep image file
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, "updatedTitle1", 1, true, null, apiToken);
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, "updatedTitle1", "coffeeshop.png", 1,"custom", null);

        // Update featured item: remove image file
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, "updatedTitle1", 2, false, null, apiToken);
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, "updatedTitle1", null, 2,"custom", null);

        // Update featured item: set new image file
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, "updatedTitle1", 2, false, "src/test/resources/images/coffeeshop.png", apiToken);
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, "updatedTitle1", "coffeeshop.png", 2,"custom", null);

        // Update featured item: set malicious content which should be sanitized
        String unsafeContent = "<h1 class=\"rte-heading\">A title</h1><a target=\"_blank\" class=\"rte-link\" href=\"https://test.com\">link</a>";
        String sanitizedContent = "<h1 class=\"rte-heading\">A title</h1><a target=\"_blank\" class=\"rte-link\" href=\"https://test.com\" rel=\"noopener noreferrer nofollow\">link</a>";
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, unsafeContent, 2, false, "src/test/resources/images/coffeeshop.png", apiToken);
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, sanitizedContent, "coffeeshop.png", 2,"custom", null);

        // Update featured item: set dataverse type
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, "updatedTitle2", 3, false, null, "dataverse", dataverseAlias, apiToken);
        updateFeatureItemResponse.prettyPrint();
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, null, null, 3, "dataverse", dataverseAlias);

        // Test mismatch between type and dvObject
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, "updatedTitle2", 3, false, null, "dataset", dataverseAlias, apiToken);
        updateFeatureItemResponse.prettyPrint();
        updateFeatureItemResponse.then()
                .body("message", equalTo(BundleUtil.getStringFromBundle("dataverse.update.featuredItems.error.typeAndDvObjectMismatch")))
                .assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testUpdateFeaturedItemUnicode() {
        String apiToken = createUserAndGetApiToken();
        String dataverseAlias = createDataverseAndGetAlias(apiToken);

        String coffeeShopEnglish = "src/test/resources/images/coffeeshop.png";
        String coffeeShopGreek = System.getProperty("java.io.tmpdir") + File.separator + "καφενείο.png";
        Path pathToCoffeeShopGreek = java.nio.file.Paths.get(coffeeShopGreek);
        System.out.println("path to coffee show in Greek: " + pathToCoffeeShopGreek);
        try {
            java.nio.file.Files.copy(java.nio.file.Paths.get(coffeeShopEnglish), pathToCoffeeShopGreek, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Logger.getLogger(DataverseFeaturedItemsIT.class.getName()).log(Level.SEVERE, null, ex);
        }

        Response createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, "test", 0, coffeeShopGreek);
        createFeatureItemResponse.prettyPrint();
        /**
         * TODO: Fix this REST Assured test. Sending unicode works fine in curl
         * (see scripts/issues/11429/add-featured-items.sh) and we suspect we
         * aren't sending Unicode properly through REST Assured (or Unicode
         * isn't supported, which isn't likely). For now we assert
         * "????????.png" but once we fix the test, "καφενείο.png" should be
         * asserted.
         */
        verifyUpdatedFeaturedItem(createFeatureItemResponse, "test", "????????.png", 0,"custom", null);

        long featuredItemId = JsonPath.from(createFeatureItemResponse.body().asString()).getLong("data.id");

        // test getting /api/access/dataverseFeaturedItemImage/
        Response itemImageResponse = UtilIT.getDataverseFeaturedItemImage(featuredItemId, apiToken);
        itemImageResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // update content
        Response updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, "updatedTitle1", 1, true, null, apiToken);
        updateFeatureItemResponse.prettyPrint();
        // TODO: Fix this REST Assured assertion too (see above).
        // The equivalent curl command: scripts/issues/11429/update-featured-item.sh
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, "updatedTitle1", "????????.png", 1,"custom", null);

        // remove image
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, "updatedTitle1", 2, false, null, apiToken);
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, "updatedTitle1", null, 2,"custom", null);

        // add non-unicode image
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, "updatedTitle1", 2, false, coffeeShopEnglish, apiToken);
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, "updatedTitle1", "coffeeshop.png", 2,"custom", null);

        updateFeatureItemResponse = UtilIT.deleteDataverseFeaturedItem(featuredItemId, apiToken);
        updateFeatureItemResponse.then().assertThat().statusCode(OK.getStatusCode());

        List<Long> ids = Arrays.asList(0L);
        List<String> contents = Arrays.asList("Greek filename");
        List<Integer> orders = Arrays.asList(0);
        List<Boolean> keepFiles = Arrays.asList(false);
        List<String> pathsToFiles = Arrays.asList(coffeeShopGreek);

        Response updateDataverseFeaturedItemsResponse = UtilIT.updateDataverseFeaturedItems(dataverseAlias, ids, contents, orders, keepFiles, pathsToFiles, apiToken);
        updateDataverseFeaturedItemsResponse.prettyPrint();
        updateDataverseFeaturedItemsResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].content", equalTo("Greek filename"))
                // TODO: Fix this REST Assured assertion too (see above).
                // The equivalent curl command: scripts/issues/11429/update-featured-items.sh
                .body("data[0].imageFileName", equalTo("????????.png"))
                .body("data[0].imageFileUrl", containsString("/api/access/dataverseFeaturedItemImage/"))
                .body("data[0].displayOrder", equalTo(0));
    }

    private String createUserAndGetApiToken() {
        Response createUserResponse = UtilIT.createRandomUser();
        return UtilIT.getApiTokenFromResponse(createUserResponse);
    }

    private String createDataverseAndGetAlias(String apiToken) {
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        return UtilIT.getAliasFromResponse(createDataverseResponse);
    }

    private Long createFeaturedItemAndGetId(String dataverseAlias, String apiToken, String pathToTestFile) {
        Response createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, "test", 0, pathToTestFile);
        createFeatureItemResponse.then().assertThat().statusCode(OK.getStatusCode());
        JsonPath createdFeaturedItem = JsonPath.from(createFeatureItemResponse.body().asString());
        return createdFeaturedItem.getLong("data.id");
    }

    private void verifyUpdatedFeaturedItem(Response response, String expectedContent, String expectedImageFileName, int expectedDisplayOrder, String type, String dvObject) {
        response.prettyPrint();
        response.then().assertThat()
                .body("data.content", equalTo(expectedContent))
                .body("data.imageFileName", equalTo(expectedImageFileName))
                .body("data.displayOrder", equalTo(expectedDisplayOrder))
                .body("data.type", equalTo(type))
                .body("data.dvObjectIdentifier", equalTo(dvObject))
                .statusCode(OK.getStatusCode());
    }
}
