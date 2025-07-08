package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
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
        String setting = "./appserver/glassfish/domains/domain1/docroot/index.html";
        UtilIT.setSetting(SettingsServiceBean.Key.WebAnalyticsCode, setting).prettyPrint();

        Response getResponse = UtilIT.getCustomizationFile("analytics");
        getResponse.prettyPrint();
        getResponse.then().assertThat()
                .statusCode(200)
                .body(containsString("<!doctype html>"));

        UtilIT.deleteSetting(SettingsServiceBean.Key.WebAnalyticsCode).prettyPrint();

        getResponse = UtilIT.getCustomizationFile("analytics");
        getResponse.prettyPrint();
        getResponse.then().assertThat()
                .statusCode(404)
                .body(containsString("not found"));
    }
}
