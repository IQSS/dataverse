package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.BundleUtil;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.text.MessageFormat;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class DataverseFeaturedItemsIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
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
        String dataverseAlias = createDataverseAndGetAlias(apiToken);
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
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, "updatedTitle1", "coffeeshop.png", 1);

        // Update featured item: remove image file
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, "updatedTitle1", 2, false, null, apiToken);
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, "updatedTitle1", null, 2);

        // Update featured item: set new image file
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, "updatedTitle1", 2, false, "src/test/resources/images/coffeeshop.png", apiToken);
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, "updatedTitle1", "coffeeshop.png", 2);

        // Update featured item: set malicious content which should be sanitized
        String unsafeContent = "<h1 class=\"rte-heading\">A title</h1><a target=\"_blank\" class=\"rte-link\" href=\"https://test.com\">link</a>";
        String sanitizedContent = "<h1 class=\"rte-heading\">A title</h1><a target=\"_blank\" class=\"rte-link\" href=\"https://test.com\" rel=\"noopener noreferrer nofollow\">link</a>";
        updateFeatureItemResponse = UtilIT.updateDataverseFeaturedItem(featuredItemId, unsafeContent, 2, false, "src/test/resources/images/coffeeshop.png", apiToken);
        verifyUpdatedFeaturedItem(updateFeatureItemResponse, sanitizedContent, "coffeeshop.png", 2);
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

    private void verifyUpdatedFeaturedItem(Response response, String expectedContent, String expectedImageFileName, int expectedDisplayOrder) {
        response.then().assertThat()
                .body("data.content", equalTo(expectedContent))
                .body("data.imageFileName", equalTo(expectedImageFileName))
                .body("data.displayOrder", equalTo(expectedDisplayOrder))
                .statusCode(OK.getStatusCode());
    }
}
