package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
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
    }

    @Test
    public void testGetDownloadsByMonth() {
        Response response = UtilIT.metricsDownloadsByMonth();
        response.prettyPrint();
    }

    @Test
    public void testGetDatasetsByMonth() {
        Response response = UtilIT.metricsDatasetsByMonth();
        response.prettyPrint();
    }

}
