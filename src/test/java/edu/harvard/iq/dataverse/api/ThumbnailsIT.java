package edu.harvard.iq.dataverse.api;

import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

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

        String tooBigLogo = "src/test/resources/images/coffeeshop.png";
        Response tooBigAndFails = UtilIT.setDataverseLogo(dataverseAlias, tooBigLogo, apiToken);
        tooBigAndFails.prettyPrint();
        tooBigAndFails.then().assertThat()
                //                .body("message", CoreMatchers.equalTo("File is larger than maximum size: 500000."))
                //                .statusCode(400);
                .body("message", CoreMatchers.equalTo("Setting the dataverse logo via API needs more work."))
                .statusCode(403);

        String logo = "src/main/webapp/resources/images/cc0.png";
        Response setDataverseLogo = UtilIT.setDataverseLogo(dataverseAlias, logo, apiToken);
        setDataverseLogo.prettyPrint();
        setDataverseLogo.then().assertThat()
                .body("message", CoreMatchers.equalTo("Setting the dataverse logo via API needs more work."))
                .statusCode(403);

    }
}
