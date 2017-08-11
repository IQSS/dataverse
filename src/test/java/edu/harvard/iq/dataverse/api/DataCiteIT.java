package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.response.Response;
import static junit.framework.Assert.assertEquals;
import org.junit.Test;

/**
 * These tests will only work if you are using "DataCite" rather than "EZID" for
 * your :DoiProvider and have done all the other related setup to switch from
 * EZID, including setting JVM options. Look for DataCite in the dev guide for
 * more tips.
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
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

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

        String title = "myTitle";
        // TODO: exercise https://github.com/IQSS/dataverse/issues/3845 by putting HTML in dataset description
        String description = "BEGIN<br></br>END";
        if (true) {
            return;
        }
        Response createDataset = UtilIT.createDatasetViaSwordApi(dataverseAlias, title, description, apiToken);
        createDataset.prettyPrint();
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        publishDataset.prettyPrint();
        assertEquals(200, publishDataset.getStatusCode());

    }

}
