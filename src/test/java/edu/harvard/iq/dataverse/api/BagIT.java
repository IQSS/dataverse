package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.engine.command.impl.LocalSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BagIT {

    @BeforeClass
    public static void setUpClass() {

        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response setArchiverClassName = UtilIT.setSetting(SettingsServiceBean.Key.ArchiverClassName, LocalSubmitToArchiveCommand.class.getCanonicalName());
        setArchiverClassName.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response setArchiverSettings = UtilIT.setSetting(SettingsServiceBean.Key.ArchiverSettings, ":BagItLocalPath, :BagGeneratorThreads");
        setArchiverSettings.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response setBagItLocalPath = UtilIT.setSetting(":BagItLocalPath", "/tmp");
        setBagItLocalPath.then().assertThat()
                .statusCode(OK.getStatusCode());

    }

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

    @AfterClass
    public static void tearDownClass() {

        // Not checking if delete happened. Hopefully, it did.
        UtilIT.deleteSetting(SettingsServiceBean.Key.ArchiverClassName);
        UtilIT.deleteSetting(SettingsServiceBean.Key.ArchiverSettings);
        UtilIT.deleteSetting(":BagItLocalPath");

    }

}
