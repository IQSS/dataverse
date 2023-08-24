package edu.harvard.iq.dataverse.api;

import static io.restassured.RestAssured.given;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class InfoIT {

    @Test
    public void testGetDatasetPublishPopupCustomText() {

        given().urlEncodingEnabled(false)
                .body("Hello world!")
                .put("/api/admin/settings/"
                        + SettingsServiceBean.Key.DatasetPublishPopupCustomText);

        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/settings/" + SettingsServiceBean.Key.DatasetPublishPopupCustomText);
        response.prettyPrint();
        response.then().assertThat().statusCode(200)
                .body("data.message", equalTo("Hello world!"));

        given().urlEncodingEnabled(false)
                .delete("/api/admin/settings/"
                        + SettingsServiceBean.Key.DatasetPublishPopupCustomText);

        response = given().urlEncodingEnabled(false)
                .get("/api/info/settings/" + SettingsServiceBean.Key.DatasetPublishPopupCustomText);
        response.prettyPrint();
        response.then().assertThat().statusCode(404)
                .body("message", equalTo("Setting "
                        + SettingsServiceBean.Key.DatasetPublishPopupCustomText
                        + " not found"));
    }

    @Test
    public void testGetVersion() {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/version");
        response.prettyPrint();
        response.then().assertThat().statusCode(200)
                .body("data.version", notNullValue());
    }

    @Test
    public void testGetServer() {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/server");
        response.prettyPrint();
        response.then().assertThat().statusCode(200)
                .body("data.message", notNullValue());
    }
    
    @Test
    public void getTermsOfUse() {
        Response response = given().urlEncodingEnabled(false)
                .get("/api/info/apiTermsOfUse");
        response.prettyPrint();
        response.then().assertThat().statusCode(200)
                .body("data.message", notNullValue());
    }
}
