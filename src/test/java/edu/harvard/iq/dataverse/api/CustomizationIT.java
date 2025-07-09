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
        if (Files.exists(Paths.get("docker-dev-volumes"))) {
            docroot = "./appserver/glassfish/domains/domain1/docroot/";
        } else {
            // /usr/local/payara6/glassfish/domains/domain1/config
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
        getResponse.prettyPrint();
        getResponse.then().assertThat()
                .body(containsString("<!doctype html>"))
                .statusCode(200);

        assert (getResponse.getHeaders().get("Content-Type").getValue().startsWith("text/html"));

        UtilIT.deleteSetting(SettingsServiceBean.Key.WebAnalyticsCode).prettyPrint();

        getResponse = UtilIT.getCustomizationFile("analytics");
        getResponse.prettyPrint();
        getResponse.then().assertThat()
                .statusCode(404)
                .body(containsString("not found."));
    }

    @Test
    public void testGetCustomLogo() {
        String setting = docroot + "img/logo.png";
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
                .body(containsString("not found."));
    }
}
