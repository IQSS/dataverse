/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.response.Response;
import static com.jayway.restassured.RestAssured.*;
import org.junit.Test;
import static org.junit.Assert.*;

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
        Response result = given().urlEncodingEnabled(false).get("/api/system/settings/:DatasetPublishPopupCustomText");
        result.prettyPrint();
        assertEquals(200, result.getStatusCode());
    }
}
