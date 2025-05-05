package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;

public class CustomizationIT {

    @AfterEach
    public void after() {
        UtilIT.deleteSetting(SettingsServiceBean.Key.WebAnalyticsCode);
    }
    @Test
    public void testGetCustomAnalytics() {
        String html = """
                <!-- Global Site Tag (gtag.js) - Google Analytics -->
                <script async="async" src="https://www.googletagmanager.com/gtag/js?id=YOUR-ACCOUNT-CODE"></script>
                <script>
                    //<![CDATA[
                    window.dataLayer = window.dataLayer || [];
                    function gtag(){dataLayer.push(arguments);}
                    gtag('js', new Date());
                                
                    gtag('config', 'YOUR-ACCOUNT-CODE');
                    //]]>
                </script>""";

        UtilIT.setSetting(SettingsServiceBean.Key.WebAnalyticsCode, html).prettyPrint();

        Response getResponse = UtilIT.getCustomAnalyticsHTML();
        getResponse.prettyPrint();
        getResponse.then().assertThat()
                .statusCode(200)
                .body(equalTo(html));

        UtilIT.deleteSetting(SettingsServiceBean.Key.WebAnalyticsCode).prettyPrint();

        getResponse = UtilIT.getCustomAnalyticsHTML();
        getResponse.prettyPrint();
        getResponse.then().assertThat()
                .statusCode(404);
    }
}
