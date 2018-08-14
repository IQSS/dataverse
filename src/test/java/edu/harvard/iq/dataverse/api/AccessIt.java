/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author madunlap
 */
public class AccessIt {
    
    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }
    
    //MAD: It looks like we throw all the actual api calls in UtilIT.java...
    
    //curl -X GET "localhost:8080/api/access/datafiles/3,3"

    @Test
    public void testDownloadSingleFile() {
//MAD: Change this hardcoded fileId
        Response anonDownload = UtilIT.downloadFile(5);//fileId.intValue());
        assertEquals(OK.getStatusCode(), anonDownload.getStatusCode());
    }
    
//MAD: This one is broken but this wget works... wget "localhost:8080/api/access/datafile/:persistentId/?persistentId=doi:10.5072/FK2/DJ7VDJ/JKFDJR&format=original"
    @Ignore
    @Test
    public void testDownloadSingleFileOriginal() {
//MAD: Change this hardcoded fileId
        Response anonDownload = UtilIT.downloadFileOriginal(106);//fileId.intValue());
        assertEquals(OK.getStatusCode(), anonDownload.getStatusCode());
    }
    
    @Test
    public void testDownloadMultipleFiles() {
//MAD: Change this hardcoded fileId
        Response anonDownload = UtilIT.downloadFiles(new Integer[]{5,3});//fileId.intValue());
        assertEquals(OK.getStatusCode(), anonDownload.getStatusCode());
    }
}
