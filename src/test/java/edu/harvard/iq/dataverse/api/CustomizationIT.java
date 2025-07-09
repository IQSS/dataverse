package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.*;

public class CustomizationIT {

    @AfterEach
    public void after() {
        UtilIT.deleteSetting(SettingsServiceBean.Key.WebAnalyticsCode);
    }

    @Test
    public void testGetCustomAnalytics() {
        // String setting = "/usr/local/glassfish4/glassfish/domains/domain1/docroot/index.html";
        String setting = "./appserver/glassfish/domains/domain1/docroot/index.html";
        UtilIT.setSetting(SettingsServiceBean.Key.WebAnalyticsCode, setting).prettyPrint();

        Response getResponse = UtilIT.getCustomizationFile("analytics");
        getResponse.prettyPrint();
        getResponse.then().assertThat()
                .statusCode(200)
                .body(containsString("<!doctype html>"))
                .body(containsString("<p class=\"signoff\">/opt/payara</p>"));//TESTING PWD NOT TO BE CHECKED IN
        assert (getResponse.getHeaders().get("Content-Type").getValue().startsWith("text/html"));

        UtilIT.deleteSetting(SettingsServiceBean.Key.WebAnalyticsCode).prettyPrint();

        getResponse = UtilIT.getCustomizationFile("analytics");
        getResponse.prettyPrint();
        getResponse.then().assertThat()
                .statusCode(404)
                .body(containsString("not found"));
    }

    @Test
    public void testGetCustomLogo() {
        // String setting = "/usr/local/glassfish4/glassfish/domains/domain1/docroot/img/logo.png";
        String setting = "./appserver/glassfish/domains/domain1/docroot/img/logo.png";
        UtilIT.setSetting(SettingsServiceBean.Key.LogoCustomizationFile, setting).prettyPrint();

        Response getResponse = UtilIT.getCustomizationFile("logo");
        getResponse.prettyPrint();
        getResponse.then().assertThat()
                .statusCode(200)
                .body(containsString("PNG"));

        assert (getResponse.getHeaders().get("Content-Type").getValue().startsWith("image/png"));

        UtilIT.deleteSetting(SettingsServiceBean.Key.LogoCustomizationFile).prettyPrint();

        getResponse = UtilIT.getCustomizationFile("logo");
        getResponse.prettyPrint();
        getResponse.then().assertThat()
                .statusCode(404)
                .body(containsString("not found"));
    }
}
