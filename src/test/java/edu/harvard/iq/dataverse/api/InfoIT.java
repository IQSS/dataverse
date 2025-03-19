package edu.harvard.iq.dataverse.api;

import static io.restassured.RestAssured.given;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import org.skyscreamer.jsonassert.JSONAssert;

public class InfoIT {

    @BeforeAll
    public static void setUpClass() {
        UtilIT.deleteSetting(SettingsServiceBean.Key.MaxEmbargoDurationInMonths);
        UtilIT.deleteSetting(SettingsServiceBean.Key.DatasetPublishPopupCustomText);
    }

    @AfterAll
    public static void afterClass() {
        UtilIT.deleteSetting(SettingsServiceBean.Key.MaxEmbargoDurationInMonths);
        UtilIT.deleteSetting(SettingsServiceBean.Key.DatasetPublishPopupCustomText);
    }

    @Test
    public void testGetDatasetPublishPopupCustomText() {
        testSettingEndpoint(SettingsServiceBean.Key.DatasetPublishPopupCustomText, "Hello world!");
    }

    @Test
    public void testGetMaxEmbargoDurationInMonths() {
        testSettingEndpoint(SettingsServiceBean.Key.MaxEmbargoDurationInMonths, "12");
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
    public void testGetTermsOfUse() {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/apiTermsOfUse");
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.message", notNullValue());
    }

    @Test
    public void testGetAllowsIncompleteMetadata() {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/settings/incompleteMetadataViaApi");
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data", notNullValue());
    }

    @Test
    public void testGetZipDownloadLimit() {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/zipDownloadLimit");
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data", notNullValue());
    }

    @Test
    public void testGetExportFormats() throws IOException {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/exportFormats");
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode());

        String actual = response.getBody().asString();
        String expected =
                java.nio.file.Files.readString(
                        Paths.get("src/test/resources/json/export-formats.json"),
                        StandardCharsets.UTF_8);
        JSONAssert.assertEquals(expected, actual, true);
        
    }


    private void testSettingEndpoint(SettingsServiceBean.Key settingKey, String testSettingValue) {
        String endpoint =  "/api/info/settings/" + settingKey;
        // Setting not found
        Response response = given().urlEncodingEnabled(false).get(endpoint);
        response.prettyPrint();
        response.then().assertThat().statusCode(NOT_FOUND.getStatusCode())
                .body("message", equalTo("Setting " + settingKey + " not found"));
        // Setting exists
        UtilIT.setSetting(settingKey, testSettingValue);
        response = given().urlEncodingEnabled(false).get(endpoint);
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.message", equalTo(testSettingValue));
    }
}
