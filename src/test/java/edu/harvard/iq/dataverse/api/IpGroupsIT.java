package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.logging.Logger;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class IpGroupsIT {

    private static final Logger logger = Logger.getLogger(IpGroupsIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response removeSearchApiNonPublicAllowed = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        removeSearchApiNonPublicAllowed.prettyPrint();
        removeSearchApiNonPublicAllowed.then().assertThat()
                .statusCode(200);

    }

    @Test
    public void testDownloadFile() {

        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        System.out.println("dataset id: " + datasetId);

        Response userWithNoRoles = UtilIT.createRandomUser();
        String userWithNoRolesApiToken = UtilIT.getApiTokenFromResponse(userWithNoRoles);
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("favicondataverse.png"))
                .statusCode(OK.getStatusCode());

        Long fileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

        boolean restrict = true;
        Response restrictResponse = UtilIT.restrictFile(fileId, restrict, apiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat()
                .body("data.message", equalTo("File favicondataverse.png restricted."))
                .statusCode(OK.getStatusCode());

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        System.out.println("file id: " + fileId);
        Response downloadFile = UtilIT.downloadFile(fileId.intValue(), apiToken);
        assertEquals(OK.getStatusCode(), downloadFile.getStatusCode());

        Response downloadFileNoPrivs = UtilIT.downloadFile(fileId.intValue(), userWithNoRolesApiToken);
        assertEquals(FORBIDDEN.getStatusCode(), downloadFileNoPrivs.getStatusCode());

        // FIXME: This the 127.0.0.1 IP group. Add it programatically.
        String ipGroupLocalhost = "&ip/localhost";
        Response grantIpLocalhost = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.FILE_DOWNLOADER.toString(), ipGroupLocalhost, apiToken);
        grantIpLocalhost.prettyPrint();
        grantIpLocalhost.then().assertThat()
                .body("data.assignee", equalTo(ipGroupLocalhost))
                .body("data._roleAlias", equalTo("fileDownloader"))
                .statusCode(OK.getStatusCode());

        // FIXME: This is the 0.0.0.0 IP group. Add it programatically.
        String ipGroupAll = "&ip/ipGroupAll";
        Response grantIpAll = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.FILE_DOWNLOADER.toString(), ipGroupAll, apiToken);
        grantIpAll.prettyPrint();
        grantIpAll.then().assertThat()
                .body("data.assignee", equalTo(ipGroupAll))
                .body("data._roleAlias", equalTo("fileDownloader"))
                .statusCode(OK.getStatusCode());

        Response downloadFileBasedOnIPGroup = UtilIT.downloadFile(fileId.intValue(), userWithNoRolesApiToken);
        // FIXME: Get this working. Should get an OK response (able to download file) based on IP Group membership.
        assertEquals(OK.getStatusCode(), downloadFileBasedOnIPGroup.getStatusCode());

    }

}
