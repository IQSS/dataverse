package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.*;

public class CustomizationIT {

    static String docroot;
    @BeforeAll
    public static void setup() {
        // setup docroot for running test either in docker or in Jenkins
        if (Files.exists(Paths.get("docker-dev-volumes"))) {
            docroot = "./appserver/glassfish/domains/domain1/docroot/";
        } else {
            docroot = "../docroot/";
        }
    }
    @AfterEach
    public void after() {
        UtilIT.deleteSetting(SettingsServiceBean.Key.WebAnalyticsCode);
    }

    @Test
    public void testGetCustomAnalytics() {
        String setting = docroot + "index.html";
        UtilIT.setSetting(SettingsServiceBean.Key.WebAnalyticsCode, setting).prettyPrint();

        Response getResponse = UtilIT.getCustomizationFile("analytics");
        getResponse.then().assertThat()
                .statusCode(200)
                .body(containsString("<!doctype html>"));

        assert (getResponse.getHeaders().get("Content-Type").getValue().startsWith("text/html"));

        UtilIT.deleteSetting(SettingsServiceBean.Key.WebAnalyticsCode).prettyPrint();

        getResponse = UtilIT.getCustomizationFile("analytics");
        getResponse.then().assertThat()
                .statusCode(404)
                .body(containsString("not found."));
    }

    @Test
    public void testGetCustomLogo() {
        String setting = docroot + "img/logo.png";
        UtilIT.setSetting(SettingsServiceBean.Key.LogoCustomizationFile, setting).prettyPrint();

        Response getResponse = UtilIT.getCustomizationFile("logo");
        getResponse.then().assertThat()
                .statusCode(200)
                .body(containsString("PNG"));

        assert (getResponse.getHeaders().get("Content-Type").getValue().startsWith("image/png"));

        UtilIT.deleteSetting(SettingsServiceBean.Key.LogoCustomizationFile).prettyPrint();

        getResponse = UtilIT.getCustomizationFile("logo");
        getResponse.prettyPrint();
        getResponse.then().assertThat()
                .statusCode(404)
                .body(containsString("not found."));
    }

    @Test
    public void testGetCustomUnknown() {
        Response getResponse = UtilIT.getCustomizationFile("unknownType");
        getResponse.then().assertThat()
                .statusCode(400)
                .body(containsString("Customization type unknown or missing. Must be one of the following: [homePage, header"));
    }
}
