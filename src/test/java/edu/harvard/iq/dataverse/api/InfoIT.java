package edu.harvard.iq.dataverse.api;

import static io.restassured.RestAssured.given;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
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
import org.junit.jupiter.api.Disabled;
import org.skyscreamer.jsonassert.JSONAssert;

public class InfoIT {

    @BeforeAll
    public static void setUpClass() {
        UtilIT.deleteSetting(SettingsServiceBean.Key.MaxEmbargoDurationInMonths);
        UtilIT.deleteSetting(SettingsServiceBean.Key.DatasetPublishPopupCustomText);
        UtilIT.deleteSetting(SettingsServiceBean.Key.ApplicationTermsOfUse);
        UtilIT.deleteSetting(SettingsServiceBean.Key.ApplicationTermsOfUse, "fr");
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
    public void testGetAppTermsOfUse() {
        Response getTermsUnset = UtilIT.getAppTermsOfUse();
        getTermsUnset.prettyPrint();
        getTermsUnset.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.message", equalTo(BundleUtil.getStringFromBundle("system.app.terms")));

        String terms = "Be excellent to each other.";

        Response setTerms = UtilIT.setSetting(SettingsServiceBean.Key.ApplicationTermsOfUse, terms);
        setTerms.prettyPrint();
        setTerms.then().assertThat().statusCode(OK.getStatusCode());

        Response getTermsSet = UtilIT.getAppTermsOfUse();
        getTermsSet.prettyPrint();
        getTermsSet.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.message", equalTo(terms));

        Response getTermsEn = UtilIT.getAppTermsOfUse("en");
        getTermsEn.prettyPrint();
        getTermsEn.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.message", equalTo(terms));
    }

    /**
     * Disabled because internationalization isn't set up.
     */
    @Disabled
    @Test
    public void testGetAppTermsOfUseFrench() {
        Response getTermsUnsetFr = UtilIT.getAppTermsOfUse("fr");
        getTermsUnsetFr.prettyPrint();
        getTermsUnsetFr.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.message", equalTo(BundleUtil.getStringFromBundle("system.app.terms")));

        Response setTermsFr = UtilIT.setSetting(SettingsServiceBean.Key.ApplicationTermsOfUse, "Appelez-moi.", "fr");
        setTermsFr.prettyPrint();
        setTermsFr.then().assertThat().statusCode(OK.getStatusCode());

        Response getTermsFr = UtilIT.getAppTermsOfUse("fr");
        getTermsFr.prettyPrint();
        getTermsFr.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Appelez-moi."));
    }

    @Test
    public void testGetApiTermsOfUse() {
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
