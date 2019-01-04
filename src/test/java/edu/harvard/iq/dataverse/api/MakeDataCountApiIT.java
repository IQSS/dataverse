package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class MakeDataCountApiIT {

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testMakeDataCountSendDataToHub() {
        Response sendDataToHub = UtilIT.makeDataCountSendDataToHub();
        sendDataToHub.prettyPrint();
        sendDataToHub.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testMakeDataCountGetMetric() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
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
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        

        Response addDummyData = UtilIT.makeDataCountAddDummyData(datasetId.toString());
        addDummyData.prettyPrint();
        addDummyData.then().assertThat()
                .statusCode(OK.getStatusCode()); 
        

        String invalidMetric = "junk";
        Response invalidMetricAttempt = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), invalidMetric, apiToken);
        invalidMetricAttempt.prettyPrint();
        invalidMetricAttempt.then().assertThat()
                .body("message", equalTo("MetricType must be one of these values: [viewsTotal, viewsUnique, downloadsTotal, downloadsUnique, citations]."))
                .statusCode(BAD_REQUEST.getStatusCode());

        String metric = "viewsTotal";
        Response getCitations = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metric, apiToken);
        getCitations.prettyPrint();
        getCitations.then().assertThat()
                .body("data.description", equalTo("VIEWS_TOTAL metric for dataset " + datasetId))
                .statusCode(OK.getStatusCode());
    }

}
