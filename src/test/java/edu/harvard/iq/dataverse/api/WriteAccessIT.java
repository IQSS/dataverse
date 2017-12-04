package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import javax.json.Json;
import javax.json.JsonObject;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class WriteAccessIT {

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testSubmitDataSummaryViaAPI() throws InterruptedException {
        Response createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);

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
        String datasetPersistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");

        String pathToFile = "scripts/search/data/tabular/50by1000.dta";
        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFile.prettyPrint();
        int dataFileId = JsonPath.from(uploadFile.getBody().asString()).getInt("data.files[0].dataFile.id");
        // Give tabular file time to ingest.
        Thread.sleep(2000);

        Response createUserNoPrivs = UtilIT.createRandomUser();
        String apiTokenNoPrivs = UtilIT.getApiTokenFromResponse(createUserNoPrivs);
        String usernameNoPriv = UtilIT.getUsernameFromResponse(createUserNoPrivs);

        JsonObject jsonObject = Json.createObjectBuilder()
                .add("foo", "bar")
                .add("datafileId", dataFileId)
                .add("doesItMatterWhatIsInThisJson", false)
                .build();
        Response responseNoPrivs = UtilIT.submitDataSummaryForCaching(jsonObject, dataFileId, apiTokenNoPrivs);
        responseNoPrivs.prettyPrint();
        responseNoPrivs.then().assertThat()
                .body("message", equalTo("User not authorized to edit the dataset."))
                .statusCode(FORBIDDEN.getStatusCode());

        long fileIdDoesNotExist = Long.MAX_VALUE;
        Response submitDataSumDoesNotExist = UtilIT.submitDataSummaryForCaching(jsonObject, fileIdDoesNotExist, apiToken);
        submitDataSumDoesNotExist.prettyPrint();
        submitDataSumDoesNotExist.then().assertThat()
                .body("message", equalTo("File not found based on id " + fileIdDoesNotExist + "."))
                .statusCode(BAD_REQUEST.getStatusCode());

        Response submitDataSum = UtilIT.submitDataSummaryForCaching(jsonObject, dataFileId, apiToken);
        submitDataSum.prettyPrint();
        submitDataSum.then().assertThat()
                .body("data.message", equalTo("Summary metadata has been saved."))
                .statusCode(OK.getStatusCode());

        Response getDataSummaryNoPrivs = UtilIT.getDataSummaryDiffPrivate(dataFileId, apiTokenNoPrivs);
        // Ideally, this would return JSON rather than HTML.
        getDataSummaryNoPrivs.prettyPrint();
        getDataSummaryNoPrivs.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());

        Response getDataSummaryDoesNotExist = UtilIT.getDataSummaryDiffPrivate(fileIdDoesNotExist, apiToken);
        getDataSummaryDoesNotExist.prettyPrint();
        getDataSummaryDoesNotExist.then().assertThat()
                // It's somewhat interesting that the GET returns NOT FOUND (404) while the POST returns BAD REQUEST (400).
                .statusCode(NOT_FOUND.getStatusCode());

        Response getDataSummary = UtilIT.getDataSummaryDiffPrivate(dataFileId, apiToken);
        getDataSummary.prettyPrint();
        getDataSummary.then().assertThat()
                .statusCode(OK.getStatusCode());
        JsonPath jsonDownloaded = getDataSummary.getBody().jsonPath();
        // Just testing to see that we got back the JSON we sent. It doesn't matter what's in it.
        Assert.assertEquals("bar", jsonDownloaded.get("foo"));
        Assert.assertEquals(dataFileId, jsonDownloaded.getInt("datafileId"));

    }

}
