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

        // FIXME: Remove manual "cp" step below.
        // cp src/test/java/edu/harvard/iq/dataverse/makedatacount/sushi_sample_logs.json /tmp
        String reportOnDisk = "/tmp/sushi_sample_logs.json";
        Response addUsageMetricsFromSushiReport = UtilIT.makeDataCountAddUsageMetricsFromSushiReport(datasetId.toString(), reportOnDisk);
        addUsageMetricsFromSushiReport.prettyPrint();
        addUsageMetricsFromSushiReport.then().assertThat()
                .statusCode(OK.getStatusCode());

        String invalidMetric = "junk";
        Response invalidMetricAttempt = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), invalidMetric, apiToken);
        invalidMetricAttempt.prettyPrint();
        invalidMetricAttempt.then().assertThat()
                .body("message", equalTo("MetricType must be one of these values: [viewsTotal, viewsUnique, downloadsTotal, downloadsUnique, citations]."))
                .statusCode(BAD_REQUEST.getStatusCode());

        String metricViewsTotal = "viewsTotal";
        Response getViewsTotal = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsTotal, apiToken);
        getViewsTotal.prettyPrint();
        getViewsTotal.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsTotal", equalTo(3));

        String metricDownloadsTotal = "downloadsTotal";
        Response getDownloadsTotal = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricDownloadsTotal, apiToken);
        getDownloadsTotal.prettyPrint();
        getDownloadsTotal.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.downloadsTotal", equalTo(2));
    }

    @Test
    public void testMakeDataCountDownloadCitation() {
        String idOrPersistentIdOfDataset = "doi:10.7910/DVN/HQZOOB";
        Response updateCitations = UtilIT.makeDataCountUpdateCitationsForDataset(idOrPersistentIdOfDataset);
        updateCitations.prettyPrint();
        updateCitations.then().assertThat()
                .statusCode(OK.getStatusCode());
        // As of this writing, number of citations is 2.
    }

}
