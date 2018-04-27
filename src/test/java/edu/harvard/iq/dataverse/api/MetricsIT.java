package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.OK;
import org.junit.BeforeClass;
import org.junit.Test;

public class MetricsIT {

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testGetDataverseByCategory() {
        Response response = UtilIT.metricsDataverseByCategory();
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testGetDataversesByMonth() {
        String yyyymm = "2018-04";
        Response response = UtilIT.metricsDataversesByMonth(yyyymm);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testGetDownloadsByMonth() {
        String yyyymm = "2018-04";
        Response response = UtilIT.metricsDownloadsByMonth(yyyymm);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testGetFilesByMonth() {
        String yyyymm = "2018-04";
        Response response = UtilIT.metricsFilesByMonth(yyyymm);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testGetDatasetsByMonth() {
        String yyyymm = "2018-04";
        Response response = UtilIT.metricsDatasetsByMonth(yyyymm);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testGetDatasetsBySubject() {
        Response response = UtilIT.metricsDatasetsBySubject();
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

}
