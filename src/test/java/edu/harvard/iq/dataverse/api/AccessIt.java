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
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author madunlap
 */
public class AccessIt {
    
    public static String username;
    public static String apiToken;
    public static String dataverseAlias;
    public static Integer datasetId;
    
    public static int basicFileId;
    public static int tabFile1Id;
    public static int tabFile2Id;
    
    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
        
        //Creating dataverse and dataset
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        username = UtilIT.getUsernameFromResponse(createUser);
        apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String pathToJsonFile = "scripts/api/data/dataset-create-new.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDatasetResponse.prettyPrint();
        datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        
        String basicPathToFile = "scripts/search/data/replace_test/004.txt";
        Response basicAddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), basicPathToFile, apiToken);
        basicFileId = JsonPath.from(basicAddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
        String tab1PathToFile = "scripts/search/data/tabular/stata13-auto-withstrls.dta";
        Response tab1AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab1PathToFile, apiToken);
        tabFile1Id = JsonPath.from(tab1AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        //String origFilePid = JsonPath.from(addResponse.body().asString()).getString("data.files[0].dataFile.persistentId");
        
        String tab2PathToFile = "scripts/search/data/tabular/stata14-auto-withstrls.dta";
        Response tab2AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab2PathToFile, apiToken);
        tabFile2Id = JsonPath.from(tab2AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());
        
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());
    }
    
    @AfterClass
    public static void tearDown() {
        
        Response deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
        
        //MAD: Pretty sure delete dataset is cleaning up the files
        
//        Response deleteFileResponse = UtilIT.deleteFile(basicFileId, apiToken);
//        deleteFileResponse.prettyPrint();
//        assertEquals(NO_CONTENT, deleteFileResponse.getStatusCode());
//        
//        Response deleteFileResponse2 = UtilIT.deleteFile(tabFileId, apiToken);
//        deleteFileResponse2.prettyPrint();
//        assertEquals(NO_CONTENT, deleteFileResponse2.getStatusCode());
    }

    @Test
    public void testDownloadSingleFile() {
        Response anonDownload = UtilIT.downloadFile(tabFile1Id);
        assertEquals(OK.getStatusCode(), anonDownload.getStatusCode());
    }
    
    @Test
    public void testDownloadSingleFileOriginal() {
        Response anonDownloadOriginal = UtilIT.downloadFileOriginal(tabFile1Id);
        Response anonDownloadConverted = UtilIT.downloadFile(tabFile1Id);

        assertEquals(OK.getStatusCode(), anonDownloadOriginal.getStatusCode());
        assertEquals(OK.getStatusCode(), anonDownloadConverted.getStatusCode()); //just to ensure next test
        int origSize = anonDownloadOriginal.getBody().asByteArray().length;
        int convertSize = anonDownloadConverted.getBody().asByteArray().length;
        //Confirm that files are not the same after specifying type
        //Comparing size is somewhat brittle but is valid for the chosen file
        //MAD: I think I can find something better still...
        assertThat(origSize, is(not(convertSize)));  
    }
    
    @Test
    public void testDownloadMultipleFiles() {
        Response anonDownload = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile2Id});
        //Response anonDownload = UtilIT.downloadFilesOriginal(new Integer[]{221,221});
        assertEquals(OK.getStatusCode(), anonDownload.getStatusCode());
    }
    
    @Test
    public void testDownloadMultipleFilesOriginal() {
        //Response anonDownloadConverted = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile2Id});
        //Response anonDownloadOriginal = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile2Id});
        Response anonDownloadConverted = UtilIT.downloadFiles(new Integer[]{308,309});
        Response anonDownloadOriginal = UtilIT.downloadFilesOriginal(new Integer[]{308,309});
        assertEquals(OK.getStatusCode(), anonDownloadOriginal.getStatusCode());
        assertEquals(OK.getStatusCode(), anonDownloadConverted.getStatusCode()); //just to ensure next test
        int origSize = anonDownloadOriginal.getBody().asByteArray().length;
        int convertSize = anonDownloadConverted.getBody().asByteArray().length;
        System.out.println("origSize: "+origSize + " | convertSize: " + convertSize);
        //Confirm that files are not the same after specifying type
        //Comparing size is somewhat brittle but is valid for the chosen file
        assertThat(origSize, is(not(convertSize)));  
    }
}
