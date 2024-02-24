package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.exposedsettings.Setting;
import edu.harvard.iq.dataverse.api.exposedsettings.SettingGroup;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.*;

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
    public void testGetExposedSettings() {
        // Call for single setting
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/exposedSettings/" + SettingGroup.GROUP_NAME_API + "/" + Setting.SETTING_NAME_API_ALLOW_INCOMPLETE_METADATA);
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data." + Setting.SETTING_NAME_API_ALLOW_INCOMPLETE_METADATA, equalTo(false));

        // Call for setting root group and lookup mode base
        response = given().urlEncodingEnabled(false)
                .queryParam("lookupMode", Info.ExposedSettingsLookupMode.base)
                .get("/api/info/exposedSettings");
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data." + Setting.SETTING_NAME_FQDN, notNullValue())
                .body("data." + Setting.SETTING_NAME_IS_PUBLIC_INSTALL, equalTo(false))
                .body("data.settingSubgroups", hasItems(SettingGroup.GROUP_NAME_API, SettingGroup.GROUP_NAME_DATASET, SettingGroup.GROUP_NAME_DATAFILE));

        // Call for setting root group and lookup mode sub
        response = given().urlEncodingEnabled(false)
                .queryParam("lookupMode", Info.ExposedSettingsLookupMode.sub)
                .get("/api/info/exposedSettings");
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data." + Setting.SETTING_NAME_FQDN, notNullValue())
                .body("data." + Setting.SETTING_NAME_IS_PUBLIC_INSTALL, equalTo(false))
                .body("data.settingSubgroups." + SettingGroup.GROUP_NAME_API + "." + Setting.SETTING_NAME_API_TERMS_OF_USE, equalTo(BundleUtil.getStringFromBundle("system.api.terms")))
                .body("data.settingSubgroups." + SettingGroup.GROUP_NAME_API + "." + Setting.SETTING_NAME_API_ALLOW_INCOMPLETE_METADATA, equalTo(false))
                .body("data.settingSubgroups." + SettingGroup.GROUP_NAME_DATASET + "." + Setting.SETTING_NAME_DATASET_ALLOWED_CURATION_LABELS, equalTo(null))
                .body("data.settingSubgroups." + SettingGroup.GROUP_NAME_DATASET + "." + Setting.SETTING_NAME_DATASET_PUBLISH_POPUP_CUSTOM_TEXT, equalTo(null))
                .body("data.settingSubgroups." + SettingGroup.GROUP_NAME_DATASET + "." + Setting.SETTING_NAME_DATASET_ZIP_DOWNLOAD_LIMIT, equalTo((int) SystemConfig.defaultZipDownloadLimit))
                .body("data.settingSubgroups." + SettingGroup.GROUP_NAME_DATAFILE + "." + Setting.SETTING_NAME_DATAFILE_MAX_EMBARGO_DURATION_IN_MONTHS, equalTo(null));

        // Call for setting subgroup with default lookup mode base
        response = given().urlEncodingEnabled(false)
                .get("/api/info/exposedSettings/" + SettingGroup.GROUP_NAME_DATASET);
        response.prettyPrint();
        response.then().assertThat().statusCode(OK.getStatusCode())
                .body("data." + Setting.SETTING_NAME_DATASET_ALLOWED_CURATION_LABELS, equalTo(null))
                .body("data." + Setting.SETTING_NAME_DATASET_PUBLISH_POPUP_CUSTOM_TEXT, equalTo(null))
                .body("data." + Setting.SETTING_NAME_DATASET_ZIP_DOWNLOAD_LIMIT, equalTo((int) SystemConfig.defaultZipDownloadLimit));

        // Call for invalid setting item
        response = given().urlEncodingEnabled(false)
                .get("/api/info/exposedSettings/invalidSettingItem");
        response.prettyPrint();
        response.then().assertThat().statusCode(NOT_FOUND.getStatusCode())
                .body("message", equalTo(BundleUtil.getStringFromBundle("info.api.exposedSettings.notFound")));
    }

    private void testSettingEndpoint(SettingsServiceBean.Key settingKey, String testSettingValue) {
        String endpoint = "/api/info/settings/" + settingKey;
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
