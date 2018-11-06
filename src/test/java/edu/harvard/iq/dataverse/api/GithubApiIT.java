package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class GithubApiIT {

    private static final Logger logger = Logger.getLogger(GithubApiIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testGithubDeposit() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apitoken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apitoken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apitoken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPersistentId = JsonPath.from(createDataset.getBody().asString()).getString("data.persistentId");

        Response githubUrlShouldBeAbsent = UtilIT.getGithubUrl(datasetPersistentId, apitoken);
        githubUrlShouldBeAbsent.prettyPrint();
        githubUrlShouldBeAbsent.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.githubUrl", equalTo(null));

        String githubUrl = "https://github.com/IQSS/metrics.dataverse.org";
        Response setGithubUrl = UtilIT.setGithubUrl(datasetPersistentId, githubUrl, apitoken);
        setGithubUrl.then().assertThat()
                .statusCode(OK.getStatusCode());
        setGithubUrl.prettyPrint();

        Response getGithubUrl = UtilIT.getGithubUrl(datasetPersistentId, apitoken);
        getGithubUrl.prettyPrint();
        getGithubUrl.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.githubUrl", equalTo(githubUrl));

        Response importGithubRepo = UtilIT.importGithubRepo(datasetPersistentId, apitoken);
        importGithubRepo.prettyPrint();
        importGithubRepo.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response getDatasetAsJson = UtilIT.nativeGet(datasetId, apitoken);
        getDatasetAsJson.prettyPrint();
        getDatasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.latestVersion.files[0].label", equalTo("metrics.dataverse.org.zip"))
                // FIXME: Populate file description with metadata from GitHub.
                .body("data.latestVersion.files[0].description", equalTo(""))
                .body("data.latestVersion.files[0].dataFile.filename", equalTo("metrics.dataverse.org.zip"))
                .body("data.latestVersion.files[0].dataFile.contentType", equalTo("application/zip"));
    }

}
