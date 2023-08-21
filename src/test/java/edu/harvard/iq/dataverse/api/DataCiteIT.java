package edu.harvard.iq.dataverse.api;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.jupiter.api.Test;

/**
 * These tests will only work if you are using "DataCite" rather than "EZID" for
 * your :DoiProvider and have done all the other related setup to switch from
 * EZID, including setting JVM options. Look for DataCite in the dev guide for
 * more tips. You should switch away from EZID for dev anyway because it's going
 * away: https://www.cdlib.org/cdlinfo/2017/08/04/ezid-doi-service-is-evolving/
 */
public class DataCiteIT {

    @Test
    public void testCreateAndPublishDataset() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String title = "myTitle";
        String description = "myDescription";
        Response createDataset = UtilIT.createDatasetViaSwordApi(dataverseAlias, title, description, apiToken);
        createDataset.prettyPrint();
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromSwordResponse(createDataset);

        Response uploadFile = UtilIT.uploadRandomFile(datasetPersistentId, apiToken);
        uploadFile.then().assertThat()
                .statusCode(201);

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        publishDataset.prettyPrint();
        assertEquals(200, publishDataset.getStatusCode());

    }

    @Test
    public void testCreateAndPublishDatasetHtmlInDescription() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // description has "BEGIN<br></br>END";
        String pathToJsonFile = "scripts/search/tests/data/dataset-finch2.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat()
                .statusCode(201);
        Long datasetId = JsonPath.from(createDatasetResponse.body().asString()).getLong("data.id");

        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(new Integer(datasetId.toString()), apiToken);
        getDatasetJsonBeforePublishing.prettyPrint();
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;
        getDatasetJsonBeforePublishing.then().assertThat()
                .body("data.latestVersion.metadataBlocks.citation.fields[0].value", equalTo("HTML & More"))
                .body("data.latestVersion.metadataBlocks.citation.fields[3].value[0].dsDescriptionValue.value", equalTo("BEGIN<br></br>END"))
                .statusCode(200);

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId.toString(), "major", apiToken);
        publishDataset.prettyPrint();
        assertEquals(200, publishDataset.getStatusCode());

    }

}
