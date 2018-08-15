/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.ResourceBundle;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author madunlap
 */
public class AccessIt {
    
    public static int basicFileId;
    public static int tabFileId;
    
    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
        
        //Creating dataverse and dataset
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String pathToJsonFile = "scripts/api/data/dataset-create-new.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        
        String basicPathToFile = "scripts/search/data/replace_test/004.txt";
        Response basicAddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), basicPathToFile, apiToken);
        basicFileId = JsonPath.from(basicAddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
        String tabPathToFile = "scripts/search/data/tabular/stata13-auto-withstrls.dta";
        Response tabAddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tabPathToFile, apiToken);
        tabFileId = JsonPath.from(tabAddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        //String origFilePid = JsonPath.from(addResponse.body().asString()).getString("data.files[0].dataFile.persistentId");
        
        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());
        
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());
    }

    @Test
    public void testDownloadSingleFile() {
        Response anonDownload = UtilIT.downloadFile(tabFileId);
        assertEquals(OK.getStatusCode(), anonDownload.getStatusCode());
    }
    
    @Test
    public void testDownloadSingleFileOriginal() {
        Response anonDownloadOriginal = UtilIT.downloadFileOriginal(tabFileId);
        Response anonDownloadConverted = UtilIT.downloadFile(tabFileId);

        assertEquals(OK.getStatusCode(), anonDownloadOriginal.getStatusCode());
        assertEquals(OK.getStatusCode(), anonDownloadConverted.getStatusCode()); //just to ensure next test
        int origSize = anonDownloadOriginal.getBody().asByteArray().length;
        int convertSize = anonDownloadConverted.getBody().asByteArray().length;
        //Confirm that files are not the same after specifying type
        //Comparing size is somewhat brittle but is valid for the chosen file
        assertThat(origSize, is(not(convertSize)));  
    }
    
    @Test
    public void testDownloadMultipleFiles() {
        Response anonDownload = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFileId});
        assertEquals(OK.getStatusCode(), anonDownload.getStatusCode());
    }
}
