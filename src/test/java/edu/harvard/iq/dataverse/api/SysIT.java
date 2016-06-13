/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 *
 * @author jacksonokuhn
 */
public class SysIT {

    /**
     * Test of getDatasetPublishPopupCustomText method, of class Sys.
     */
    @Test
    public void testGetDatasetPublishPopupCustomText() {

        System.out.println("checking getDatasetPublishPopupCustomText");

        given().urlEncodingEnabled(false)
                .body("Hello world!")
                .put("/api/admin/settings/"
                        + SettingsServiceBean.Key.DatasetPublishPopupCustomText);

        Response response = given().urlEncodingEnabled(false)
                .get("/api/system/settings/" + SettingsServiceBean.Key.DatasetPublishPopupCustomText);
        response.prettyPrint();
        response.then().assertThat().statusCode(200)
                .body("data.message", equalTo("Hello world!"));

        given().urlEncodingEnabled(false)
                .delete("/api/admin/settings/"
                        + SettingsServiceBean.Key.DatasetPublishPopupCustomText);

        response = given().urlEncodingEnabled(false)
                .get("/api/system/settings/" + SettingsServiceBean.Key.DatasetPublishPopupCustomText);
        response.prettyPrint();
        response.then().assertThat().statusCode(404)
                .body("message", equalTo("Setting "
                        + SettingsServiceBean.Key.DatasetPublishPopupCustomText
                        + " not found"));
    }
}
