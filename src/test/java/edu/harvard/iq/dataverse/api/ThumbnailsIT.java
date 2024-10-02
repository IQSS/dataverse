package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.BundleUtil;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void testBadTiffThumbnailFailure() {
        String goodTiff = "src/test/resources/images/good.tiff";
        String badTiff = "src/test/resources/images/bad.tiff";
        // setup dataverse and dataset
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.makeSuperUser(username);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        String pathToJsonFile = "src/test/resources/json/complete-dataset-with-files.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        Response datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetAsJson.prettyPrint();
        String protocol = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;

        // check thumbnails are empty

        Response thumbnailCandidatesResponse = UtilIT.showDatasetThumbnailCandidates(datasetPersistentId, apiToken);
        thumbnailCandidatesResponse.prettyPrint();
        thumbnailCandidatesResponse.then().assertThat().statusCode(OK.getStatusCode());
        List<Object> images = JsonPath.from(thumbnailCandidatesResponse.getBody().asString()).getList("data");
        assertTrue(images.size() == 0);

        // upload image files ( 1 good and 1 bad )

        Response uploadResponse = UtilIT.uploadFileViaNative(datasetId.toString(), goodTiff, apiToken);
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());
        uploadResponse = UtilIT.uploadFileViaNative(datasetId.toString(), badTiff, apiToken);
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());

        // check thumbnails only contains 1 good image
        thumbnailCandidatesResponse = UtilIT.showDatasetThumbnailCandidates(datasetPersistentId, apiToken);
        thumbnailCandidatesResponse.prettyPrint();
        thumbnailCandidatesResponse.then().assertThat().statusCode(OK.getStatusCode());
        images = JsonPath.from(thumbnailCandidatesResponse.getBody().asString()).getList("data");
        assertTrue(images.size() == 1);

        // test set logo with badTiff and tiff to large (goodTiff)
        Response uploadLogoResponse = UtilIT.uploadDatasetLogo(datasetPersistentId, badTiff, apiToken);
        uploadLogoResponse.prettyPrint();
        uploadLogoResponse.then().assertThat().statusCode(FORBIDDEN.getStatusCode());
        uploadLogoResponse.then().assertThat().body("message", equalTo(
            BundleUtil.getStringFromBundle("datasets.api.thumbnail.nonDatasetFailed")));

        uploadLogoResponse = UtilIT.uploadDatasetLogo(datasetPersistentId, goodTiff, apiToken);
        uploadLogoResponse.prettyPrint();
        uploadLogoResponse.then().assertThat().statusCode(FORBIDDEN.getStatusCode());
        uploadLogoResponse.then().assertThat().body("message", containsString(
            BundleUtil.getStringFromBundle("datasets.api.thumbnail.fileToLarge", List.of(""))));

    }
}
