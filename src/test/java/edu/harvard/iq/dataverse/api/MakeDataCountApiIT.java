package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
    public void testMakeDataCountGetMetric() throws IOException {

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

        String testFile1Src = "src/test/java/edu/harvard/iq/dataverse/makedatacount/sushi_sample_logs.json";
        String testFile1Tmp = "/tmp/sushi_sample_logs.json";
        FileUtils.copyFile(new File(testFile1Src), new File(testFile1Tmp));
        String reportOnDisk = testFile1Tmp;
        Response addUsageMetricsFromSushiReport = UtilIT.makeDataCountAddUsageMetricsFromSushiReport(datasetId.toString(), reportOnDisk);
        addUsageMetricsFromSushiReport.prettyPrint();
        addUsageMetricsFromSushiReport.then().assertThat()
                .statusCode(OK.getStatusCode());

        String countryCodeUs = "us";
        String countryCodeKr = "kr";
        String countryCodeCa = "ca";

        String invalidMetric = "junk";
        Response invalidMetricAttempt = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), invalidMetric, countryCodeUs, apiToken);
        invalidMetricAttempt.prettyPrint();
        invalidMetricAttempt.then().assertThat()
                .body("message", equalTo("MetricType must be one of these values: [viewsTotal, viewsTotalRegular, viewsTotalMachine, viewsUnique, viewsUniqueRegular, viewsUniqueMachine, downloadsTotal, downloadsTotalRegular, downloadsTotalMachine, downloadsUnique, downloadsUniqueRegular, downloadsUniqueMachine, citations]."))
                .statusCode(BAD_REQUEST.getStatusCode());

        String metricViewsTotal = "viewsTotal";
        String metricViewsTotalRegular = "viewsTotalRegular";
        String metricViewsTotalMachine = "viewsTotalMachine";
        String metricViewsUnique = "viewsUnique";
        String metricViewsUniqueRegular = "viewsUniqueRegular";
        String metricViewsUniqueMachine = "viewsUniqueMachine";
        String metricDownloadsTotal = "downloadsTotal";
        String metricDownloadsTotalRegular = "downloadsTotalRegular";
        String metricDownloadsTotalMachine = "downloadsTotalMachine";
        String metricDownloadsUnique = "downloadsUnique";
        String metricDownloadsUniqueRegular = "downloadsUniqueRegular";
        String metricDownloadsUniqueMachine = "downloadsUniqueMachine";

        Response getViewsTotal = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsTotal, countryCodeUs, apiToken);
        getViewsTotal.prettyPrint();
        getViewsTotal.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsTotal", equalTo(7));

        String monthYear = "2018-05";
        Response getViewsTotalUs = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsTotal, monthYear, countryCodeUs, apiToken);
        getViewsTotalUs.prettyPrint();
        getViewsTotalUs.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsTotal", equalTo(7));

        Response getViewsTotalKr = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsTotal, monthYear, countryCodeKr, apiToken);
        getViewsTotalKr.prettyPrint();
        getViewsTotalKr.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsTotal", equalTo(2));

        Response getViewsTotalRegular = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsTotalRegular, apiToken);
        getViewsTotalRegular.prettyPrint();
        getViewsTotalRegular.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsTotalRegular", equalTo(7));

        Response getViewsTotalMachine = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsTotalMachine, apiToken);
        getViewsTotalMachine.prettyPrint();
        getViewsTotalMachine.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsTotalMachine", equalTo(17));

        Response getViewsUniqueUs = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsUnique, monthYear, countryCodeUs, apiToken);
        getViewsUniqueUs.prettyPrint();
        getViewsUniqueUs.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsUnique", equalTo(5));

        Response getViewsUniqueRegular = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsUniqueRegular, apiToken);
        getViewsUniqueRegular.prettyPrint();
        getViewsUniqueRegular.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsUniqueRegular", equalTo(6));

        Response getViewsUniqueMachine = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsUniqueMachine, apiToken);
        getViewsUniqueMachine.prettyPrint();
        getViewsUniqueMachine.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsUniqueMachine", equalTo(9));

        Response getDownloadsTotalUs = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricDownloadsTotal, monthYear, countryCodeUs, apiToken);
        getDownloadsTotalUs.prettyPrint();
        getDownloadsTotalUs.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.downloadsTotal", equalTo(6));

        Response getDownloadsTotalRegular = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricDownloadsTotalRegular, apiToken);
        getDownloadsTotalRegular.prettyPrint();
        getDownloadsTotalRegular.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.downloadsTotalRegular", equalTo(9));

        Response getDownloadsTotalMachine = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricDownloadsTotalMachine, apiToken);
        getDownloadsTotalMachine.prettyPrint();
        getDownloadsTotalMachine.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.downloadsTotalMachine", equalTo(8));

        Response getDownloadsUniqueCa = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricDownloadsUnique, monthYear, countryCodeCa, apiToken);
        getDownloadsUniqueCa.prettyPrint();
        getDownloadsUniqueCa.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.downloadsUnique", equalTo(3));

        Response getDownloadsUniqueRegular = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricDownloadsUniqueRegular, apiToken);
        getDownloadsUniqueRegular.prettyPrint();
        getDownloadsUniqueRegular.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.downloadsUniqueRegular", equalTo(5));

        Response getDownloadsUniqueMachine = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricDownloadsUniqueMachine, apiToken);
        getDownloadsUniqueMachine.prettyPrint();
        getDownloadsUniqueMachine.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.downloadsUniqueMachine", equalTo(6));

    }

    /**
     * Ignore is set on this test because it requires database edits to pass.
     * There are currently two citions for doi:10.7910/DVN/HQZOOB but you have
     * to pick one of your datasets (10 in the example below) and update the
     * "identifier" and "authority" to match that doi like this:
     *
     * update dvobject set identifier = 'DVN/HQZOOB' where id = 10;
     *
     * update dvobject set authority = '10.7910' where id = 10;
     */
    @Ignore
    @Test
    public void testMakeDataCountDownloadCitation() {
        String idOrPersistentIdOfDataset = "doi:10.7910/DVN/HQZOOB";
        Response updateCitations = UtilIT.makeDataCountUpdateCitationsForDataset(idOrPersistentIdOfDataset);
        updateCitations.prettyPrint();
        updateCitations.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

}
