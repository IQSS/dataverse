package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Test;
import io.restassured.response.Response;

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
