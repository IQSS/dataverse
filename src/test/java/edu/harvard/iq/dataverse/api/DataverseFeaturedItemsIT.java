package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class DataverseFeaturedItemsIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testDeleteFeaturedItem() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String pathToTestFile = "src/test/resources/images/coffeeshop.png";
        Response createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, "test", "test", pathToTestFile);
        createFeatureItemResponse.then().assertThat().statusCode(OK.getStatusCode());

        JsonPath createdFeaturedItem = JsonPath.from(createFeatureItemResponse.body().asString());
        Long featuredItemId = createdFeaturedItem.getLong("data.id");

        // Should return not found when passing incorrect item id
        Response deleteFeatureItemResponse = UtilIT.deleteDataverseFeaturedItem(100000L, apiToken);
        deleteFeatureItemResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // Should return unauthorized when passing correct id and user does not have permissions
        Response createRandomUser = UtilIT.createRandomUser();
        String randomUserApiToken = UtilIT.getApiTokenFromResponse(createRandomUser);
        deleteFeatureItemResponse = UtilIT.deleteDataverseFeaturedItem(featuredItemId, randomUserApiToken);
        deleteFeatureItemResponse.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // Should delete featured item when passing correct id and user have permissions
        deleteFeatureItemResponse = UtilIT.deleteDataverseFeaturedItem(featuredItemId, apiToken);
        deleteFeatureItemResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response listFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, apiToken);
        listFeaturedItemsResponse.then()
                .body("data.size()", equalTo(0))
                .assertThat().statusCode(OK.getStatusCode());
    }
}
