package edu.harvard.iq.dataverse.api;

import static io.restassured.RestAssured.given;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class InfoIT {

    @BeforeAll
    public static void setUpClass() {
        UtilIT.deleteSetting(SettingsServiceBean.Key.MaxEmbargoDurationInMonths);
    }

    @AfterAll
    public static void afterClass() {
        UtilIT.deleteSetting(SettingsServiceBean.Key.MaxEmbargoDurationInMonths);
    }

    @Test
    public void testGetDatasetPublishPopupCustomText() {

        given().urlEncodingEnabled(false)
                .body("Hello world!")
                .put("/api/admin/settings/"
                        + SettingsServiceBean.Key.DatasetPublishPopupCustomText);

        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/settings/" + SettingsServiceBean.Key.DatasetPublishPopupCustomText);
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Hello world!"));

        given().urlEncodingEnabled(false)
                .delete("/api/admin/settings/"
                        + SettingsServiceBean.Key.DatasetPublishPopupCustomText);

        response = given().urlEncodingEnabled(false)
                .get("/api/info/settings/" + SettingsServiceBean.Key.DatasetPublishPopupCustomText);
        response.prettyPrint();
        response.then().assertThat().statusCode(NOT_FOUND.getStatusCode())
                .body("message", equalTo("Setting "
                        + SettingsServiceBean.Key.DatasetPublishPopupCustomText
                        + " not found"));
    }

    @Test
    public void testGetVersion() {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/version");
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.version", notNullValue());
    }

    @Test
    public void testGetServer() {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/server");
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.message", notNullValue());
    }
    
    @Test
    public void getTermsOfUse() {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/apiTermsOfUse");
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.message", notNullValue());
    }

    @Test
    public void getAllowsIncompleteMetadata() {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/settings/incompleteMetadataViaApi");
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data", notNullValue());
    }

    @Test
    public void getZipDownloadLimit() {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/zipDownloadLimit");
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data", notNullValue());
    }

    @Test
    public void getEmbargoEnabled() {
        String testEndpoint = "/api/info/embargoEnabled";
        // Embargo disabled
        Response response = given().urlEncodingEnabled(false).get(testEndpoint);
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data", equalTo(false));
        // Embargo enabled
        UtilIT.setSetting(SettingsServiceBean.Key.MaxEmbargoDurationInMonths, "12");
        response = given().urlEncodingEnabled(false).get(testEndpoint);
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data", equalTo(true));
    }
}
