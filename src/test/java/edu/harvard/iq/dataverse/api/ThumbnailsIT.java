package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class ThumbnailsIT {

    @Test
    public void testDatasetThumbnail() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String logo = "src/main/webapp/resources/images/cc0.png";
        Response setDataverseLogo = UtilIT.setDataverseLogo(dataverseAlias, logo, apiToken);
        setDataverseLogo.prettyPrint();
        setDataverseLogo.then().assertThat()
                .body("message", CoreMatchers.equalTo("Setting the dataverse logo via API needs more work."))
                .statusCode(403);

    }
}
