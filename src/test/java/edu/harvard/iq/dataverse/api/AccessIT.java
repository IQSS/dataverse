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
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 *
 * @author madunlap
 */
public class AccessIT {
    
    public static String username;
    public static String apiToken;
    public static String dataverseAlias;
    public static Integer datasetId;
    
    public static Integer basicFileId;
    public static Integer tabFile1Id;
    public static Integer tabFile2Id;
    public static Integer tabFile3IdRestricted;
    public static Integer tabFile4IdUnpublished;
    
    @BeforeClass
    public static void setUp() throws InterruptedException {
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
        Thread.sleep(1000); //Added because tests are failing during setup, test is probably going too fast. Especially between first and second file
        Response tab1AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab1PathToFile, apiToken);
        tabFile1Id = JsonPath.from(tab1AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        //String origFilePid = JsonPath.from(addResponse.body().asString()).getString("data.files[0].dataFile.persistentId");
        
        String tab2PathToFile = "scripts/search/data/tabular/stata14-auto-withstrls.dta";
        Thread.sleep(1000); //Added because tests are failing during setup, test is probably going too fast. Especially between first and second file
        Response tab2AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab2PathToFile, apiToken);
        tabFile2Id = JsonPath.from(tab2AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
        String tab3PathToFile = "scripts/search/data/tabular/stata13-auto.dta";
        Thread.sleep(1000); //Added because tests are failing during setup, test is probably going too fast. Especially between first and second file
        Response tab3AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab3PathToFile, apiToken);
        tabFile3IdRestricted = JsonPath.from(tab3AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        Thread.sleep(3000); //Dataverse needs more time...
        Response restrictResponse = UtilIT.restrictFile(tabFile3IdRestricted.toString(), true, apiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat()
        //        .body("data.message", equalTo("File stata13-auto.tab restricted."))
                .statusCode(OK.getStatusCode());
        
        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());
        
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());
        
        String tab4PathToFile = "scripts/search/data/tabular/120745.dta";
        Thread.sleep(1000); //Added because tests are failing during setup, test is probably going too fast. Especially between first and second file
        Response tab4AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab4PathToFile, apiToken);
        tabFile4IdUnpublished = JsonPath.from(tab4AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
    }
    
    @AfterClass
    public static void tearDown() {        
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());
        
        Response deleteDatasetResponse = UtilIT.destroyDataset(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());
        //Deleting dataset cleaning up the files

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
    }
    
    //This test does a lot of testing of non-original downloads as well
    @Test
    public void testDownloadSingleFile() {
        //Not logged in non-restricted
        Response anonDownloadOriginal = UtilIT.downloadFileOriginal(tabFile1Id);
        Response anonDownloadConverted = UtilIT.downloadFile(tabFile1Id);
        assertEquals(OK.getStatusCode(), anonDownloadOriginal.getStatusCode());
        assertEquals(OK.getStatusCode(), anonDownloadConverted.getStatusCode()); //just to ensure next test
        int origSizeAnon = anonDownloadOriginal.getBody().asByteArray().length;
        int convertSizeAnon = anonDownloadConverted.getBody().asByteArray().length;
        System.out.println("origSize: "+origSizeAnon + " | convertSize: " + convertSizeAnon);
        assertThat(origSizeAnon, is(not(convertSizeAnon)));
        
        //Not logged in restricted
        Response anonDownloadOriginalRestricted = UtilIT.downloadFileOriginal(tabFile3IdRestricted);
        Response anonDownloadConvertedRestricted = UtilIT.downloadFile(tabFile3IdRestricted);
        assertEquals(403, anonDownloadOriginalRestricted.getStatusCode());
        assertEquals(403, anonDownloadConvertedRestricted.getStatusCode());
        
        //Not logged in unpublished
        Response anonDownloadOriginalUnpublished = UtilIT.downloadFileOriginal(tabFile4IdUnpublished);
        Response anonDownloadConvertedUnpublished = UtilIT.downloadFile(tabFile4IdUnpublished);
        assertEquals(403, anonDownloadOriginalUnpublished.getStatusCode());
        assertEquals(403, anonDownloadConvertedUnpublished.getStatusCode());

        //Logged in non-restricted
        Response authDownloadOriginal = UtilIT.downloadFileOriginal(tabFile1Id, apiToken);
        Response authDownloadConverted = UtilIT.downloadFile(tabFile1Id, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadOriginal.getStatusCode());
        assertEquals(OK.getStatusCode(), authDownloadConverted.getStatusCode()); //just to ensure next test
        int origSizeAuth = authDownloadOriginal.getBody().asByteArray().length;
        int convertSizeAuth = authDownloadConverted.getBody().asByteArray().length;
        System.out.println("origSize: "+origSizeAuth + " | convertSize: " + convertSizeAuth);
        assertThat(origSizeAuth, is(not(convertSizeAuth)));  
        
        //Logged in restricted
        Response authDownloadOriginalRestricted = UtilIT.downloadFileOriginal(tabFile3IdRestricted, apiToken);
        Response authDownloadConvertedRestricted = UtilIT.downloadFile(tabFile3IdRestricted, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadOriginalRestricted.getStatusCode());
        assertEquals(OK.getStatusCode(), authDownloadConvertedRestricted.getStatusCode()); //just to ensure next test
        int origSizeAuthRestricted = authDownloadOriginalRestricted.getBody().asByteArray().length;
        int convertSizeAuthRestricted = authDownloadConvertedRestricted.getBody().asByteArray().length;
        System.out.println("origSize: "+origSizeAuthRestricted + " | convertSize: " + convertSizeAuthRestricted);
        assertThat(origSizeAuthRestricted, is(not(convertSizeAuthRestricted)));  
        
        //Logged in unpublished
        Response authDownloadOriginalUnpublished = UtilIT.downloadFileOriginal(tabFile4IdUnpublished, apiToken);
        Response authDownloadConvertedUnpublished = UtilIT.downloadFile(tabFile4IdUnpublished, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadOriginalUnpublished.getStatusCode());
        assertEquals(OK.getStatusCode(), authDownloadConvertedUnpublished.getStatusCode()); //just to ensure next test
        int origSizeAuthUnpublished = authDownloadOriginalUnpublished.getBody().asByteArray().length;
        int convertSizeAuthUnpublished = authDownloadConvertedUnpublished.getBody().asByteArray().length;
        System.out.println("origSize: "+origSizeAuthUnpublished+ " | convertSize: " + convertSizeAuthUnpublished);
        assertThat(origSizeAuthUnpublished, is(not(convertSizeAuthUnpublished)));  
    }
    
    @Test
    public void testDownloadMultipleFiles() {
    //Not logged in non-restricted
        Response anonDownloadOriginal = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile2Id});
        Response anonDownloadConverted = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile2Id});
        assertEquals(OK.getStatusCode(), anonDownloadOriginal.getStatusCode());
        assertEquals(OK.getStatusCode(), anonDownloadConverted.getStatusCode()); //just to ensure next test
        int origSizeAnon = anonDownloadOriginal.getBody().asByteArray().length;
        int convertSizeAnon = anonDownloadConverted.getBody().asByteArray().length;
        System.out.println("origSize: "+origSizeAnon + " | convertSize: " + convertSizeAnon);
        assertThat(origSizeAnon, is(not(convertSizeAnon)));
        
        //Not logged in restricted
        Response anonDownloadOriginalRestricted = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile3IdRestricted});
        Response anonDownloadConvertedRestricted = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile3IdRestricted});
        assertEquals(206, anonDownloadOriginalRestricted.getStatusCode()); //partial content, only accessible files
        assertEquals(206, anonDownloadConvertedRestricted.getStatusCode()); //partial content, only accessible files
        
        //Not logged in unpublished
        Response anonDownloadOriginalUnpublished = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile4IdUnpublished});
        Response anonDownloadConvertedUnpublished = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile4IdUnpublished});
        assertEquals(206, anonDownloadOriginalUnpublished.getStatusCode()); //partial content, only accessible files
        assertEquals(206, anonDownloadConvertedUnpublished.getStatusCode()); //partial content, only accessible files

        //Logged in non-restricted
        Response authDownloadOriginal = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile2Id}, apiToken);
        Response authDownloadConverted = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile2Id}, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadOriginal.getStatusCode());
        assertEquals(OK.getStatusCode(), authDownloadConverted.getStatusCode()); //just to ensure next test
        int origSizeAuth = authDownloadOriginal.getBody().asByteArray().length;
        int convertSizeAuth = authDownloadConverted.getBody().asByteArray().length;
        System.out.println("origSize: "+origSizeAuth + " | convertSize: " + convertSizeAuth);
        assertThat(origSizeAuth, is(not(convertSizeAuth)));  
        
        //Logged in restricted
        Response authDownloadOriginalRestricted = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile3IdRestricted}, apiToken);
        Response authDownloadConvertedRestricted = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile3IdRestricted}, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadOriginalRestricted.getStatusCode());
        assertEquals(OK.getStatusCode(), authDownloadConvertedRestricted.getStatusCode()); //just to ensure next test
        int origSizeAuthRestricted = authDownloadOriginalRestricted.getBody().asByteArray().length;
        int convertSizeAuthRestricted = authDownloadConvertedRestricted.getBody().asByteArray().length;
        System.out.println("origSize: "+origSizeAuthRestricted + " | convertSize: " + convertSizeAuthRestricted);
        assertThat(origSizeAuthRestricted, is(not(convertSizeAuthRestricted)));  
        
        //Logged in unpublished
        Response authDownloadOriginalUnpublished = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile4IdUnpublished}, apiToken);
        Response authDownloadConvertedUnpublished = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile4IdUnpublished}, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadOriginalUnpublished.getStatusCode());
        assertEquals(OK.getStatusCode(), authDownloadConvertedUnpublished.getStatusCode()); //just to ensure next test
        int origSizeAuthUnpublished = authDownloadOriginalUnpublished.getBody().asByteArray().length;
        int convertSizeAuthUnpublished = authDownloadConvertedUnpublished.getBody().asByteArray().length;
        System.out.println("origSize: "+origSizeAuthUnpublished+ " | convertSize: " + convertSizeAuthUnpublished);
        assertThat(origSizeAuthUnpublished, is(not(convertSizeAuthUnpublished)));  
    }
}
