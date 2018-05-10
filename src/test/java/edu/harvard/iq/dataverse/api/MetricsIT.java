package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.OK;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

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
    public void testGetDataversesByMonth() {
        String yyyymm = "2018-04";
//        yyyymm = null;
        Response response = UtilIT.metricsDataversesByMonth(yyyymm);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDataversesByMonth(yyyymm); 
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        assertEquals(precache, postcache);
    }

    @Test
    public void testGetDatasetsByMonth() {
        String yyyymm = "2018-04";
//        yyyymm = null;
        Response response = UtilIT.metricsDatasetsByMonth(yyyymm);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDatasetsByMonth(yyyymm);
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        assertEquals(precache, postcache);
    }

    @Test
    public void testGetFilesByMonth() {
        String yyyymm = "2018-04";
//        yyyymm = null;
        Response response = UtilIT.metricsFilesByMonth(yyyymm);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsFilesByMonth(yyyymm);
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        assertEquals(precache, postcache);
    }

    @Test
    public void testGetDownloadsByMonth() {
        String yyyymm = "2018-04";
//        yyyymm = null;
        Response response = UtilIT.metricsDownloadsByMonth(yyyymm);
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDownloadsByMonth(yyyymm);
        String postcache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        assertEquals(precache, postcache);
    }

    @Test
    public void testGetDataverseByCategory() {
        Response response = UtilIT.metricsDataverseByCategory();
        String precache = response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        //Run each query twice and compare results to tests caching
        response = UtilIT.metricsDataverseByCategory();
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
