package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import org.junit.Test;

public class BagIT {

    /**
     * Note: to run this test you have to configure an archiver, such as the
     * local archiver.
     */
    @Test
    public void testBagItExport() {

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response toggleSuperuser = UtilIT.makeSuperUser(username);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken);
        publishDataset.then().assertThat().statusCode(OK.getStatusCode());

        Response archiveDataset = UtilIT.archiveDataset(datasetPid, "1.0", apiToken);
        archiveDataset.prettyPrint();
        archiveDataset.then().assertThat().statusCode(OK.getStatusCode());

    }
}
