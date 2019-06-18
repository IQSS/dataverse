package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import org.junit.BeforeClass;
import org.junit.Test;

public class SiteMapIT {

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testSiteMap() {
        Response response = UtilIT.sitemapUpdate();
        response.prettyPrint();
        Response download = UtilIT.sitemapDownload();
        download.prettyPrint();
    }

}
