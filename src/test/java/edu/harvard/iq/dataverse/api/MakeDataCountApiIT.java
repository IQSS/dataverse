package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.util.DateUtil;
import java.io.File;
import java.io.IOException;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import org.apache.commons.io.FileUtils;
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

        String invalidMetric = "junk";
        Response invalidMetricAttempt = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), invalidMetric, countryCodeUs, apiToken);
        invalidMetricAttempt.prettyPrint();
        invalidMetricAttempt.then().assertThat()
                .body("message", equalTo("MetricType must be one of these values: [viewsTotal, viewsUnique, downloadsTotal, downloadsUnique, citations]."))
                .statusCode(BAD_REQUEST.getStatusCode());

        String metricViewsTotal = "viewsTotal";
        String metricViewsUnique = "viewsUnique";
        String metricDownloadsTotal = "downloadsTotal";
        String metricDownloadsUnique = "downloadsUnique";

        String currentMonth = DateUtil.getCurrentYearDashMonth();
        Response getViewsCurrentMonth = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsTotal, countryCodeUs, apiToken);
        getViewsCurrentMonth.prettyPrint();
        getViewsCurrentMonth.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("No metrics available for dataset " + datasetId + " for " + currentMonth + " for country code " + countryCodeUs + "."));

        String monthYear = "2018-05";
        Response getViewsTotalUs = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsTotal, monthYear, countryCodeUs, apiToken);
        getViewsTotalUs.prettyPrint();
        getViewsTotalUs.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsTotal", equalTo(3));

        Response getViewsUniqueUs = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsUnique, monthYear, countryCodeUs, apiToken);
        getViewsUniqueUs.prettyPrint();
        getViewsUniqueUs.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsUnique", equalTo(2));

        Response getDownloadsTotalUs = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricDownloadsTotal, monthYear, countryCodeUs, apiToken);
        getDownloadsTotalUs.prettyPrint();
        getDownloadsTotalUs.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.downloadsTotal", equalTo(2));

        String countryCodeKr = "kr";

        Response getViewsTotalKr = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricViewsTotal, monthYear, countryCodeKr, apiToken);
        getViewsTotalKr.prettyPrint();
        getViewsTotalKr.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.viewsTotal", equalTo(2));

        String countryCodeCa = "ca";

        Response getDownloadsUniqueCa = UtilIT.makeDataCountGetMetricForDataset(datasetId.toString(), metricDownloadsUnique, monthYear, countryCodeCa, apiToken);
        getDownloadsUniqueCa.prettyPrint();
        getDownloadsUniqueCa.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.downloadsUnique", equalTo(1));

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
