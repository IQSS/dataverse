package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.OK;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

//TODO: These tests are fairly flawed as they don't actually add data to compare on.
//To improve these tests we should try adding data and see if the number DOESN'T
//go up to show that the caching worked
public class MetricsIT {

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
        UtilIT.clearMetricCache();
    }

    @AfterClass
    public static void cleanUpClass() {
        UtilIT.clearMetricCache();
    }

    @Test
    public void testGetDataversesToMonth() {
        String yyyymm = "2018-04";
//        yyyymm = null;
        Response response = UtilIT.metricsDataversesToMonth(yyyymm);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDataversesToMonth(yyyymm);
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(precache, postcache);
    }

    @Test
    public void testGetDatasetsToMonth() {
        String yyyymm = "2018-04";
//        yyyymm = null;
        Response response = UtilIT.metricsDatasetsToMonth(yyyymm);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDatasetsToMonth(yyyymm);
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(precache, postcache);
    }

    @Test
    public void testGetFilesToMonth() {
        String yyyymm = "2018-04";
//        yyyymm = null;
        Response response = UtilIT.metricsFilesToMonth(yyyymm);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsFilesToMonth(yyyymm);
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(precache, postcache);
    }

    @Test
    public void testGetDownloadsToMonth() {
        String yyyymm = "2018-04";
//        yyyymm = null;
        Response response = UtilIT.metricsDownloadsToMonth(yyyymm);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDownloadsToMonth(yyyymm);
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(precache, postcache);
    }
    
        
    @Test
    public void testGetDataversesPastDays() {
        String days = "30";

        Response response = UtilIT.metricsDataversesPastDays(days);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDataversesPastDays(days);
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(precache, postcache);
    }
    
    @Test
    public void testGetDatasetsPastDays() {
        String days = "30";

        Response response = UtilIT.metricsDatasetsPastDays(days);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDatasetsPastDays(days);
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(precache, postcache);
    }
    
    
    @Test
    public void testGetFilesPastDays() {
        String days = "30";

        Response response = UtilIT.metricsFilesPastDays(days);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsFilesPastDays(days);
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(precache, postcache);
    }
    
    @Test
    public void testGetDownloadsPastDays() {
        String days = "30";

        Response response = UtilIT.metricsDownloadsPastDays(days);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDownloadsPastDays(days);
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(precache, postcache);
    }    
    

    @Test
    public void testGetDataverseByCategory() {
        Response response = UtilIT.metricsDataversesByCategory();
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDataversesByCategory();
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(precache, postcache);
    }
    
    @Test
    public void testGetDataverseBySubject() {
        Response response = UtilIT.metricsDataversesBySubject();
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDataversesBySubject();
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(precache, postcache);
    }

    @Test
    public void testGetDatasetsBySubject() {
        Response response = UtilIT.metricsDatasetsBySubject();
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDatasetsBySubject();
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(precache, postcache);
    }

}
