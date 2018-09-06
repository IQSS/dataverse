/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import java.util.zip.ZipEntry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import org.hamcrest.collection.IsMapContaining;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author madunlap
 */

public class AccessIT {
    
    public static final String MANIFEST = "MANIFEST.TXT";
    
    public static String username;
    public static String apiToken;
    public static String dataverseAlias;
    public static Integer datasetId;
    
    public static Integer basicFileId;
    public static Integer tabFile1Id;
    public static Integer tabFile2Id;
    public static Integer tabFile3IdRestricted;
    public static Integer tabFile4IdUnpublished;
    
    public static String basicFileName;
    public static String tabFile1Name;
    public static String tabFile2Name;
    public static String tabFile3NameRestricted;
    public static String tabFile4NameUnpublished;
    
    public static String tabFile1NameConvert;
    public static String tabFile2NameConvert;
    public static String tabFile3NameRestrictedConvert;
    public static String tabFile4NameUnpublishedConvert;
    
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
        
        basicFileName = "004.txt";
        String basicPathToFile = "scripts/search/data/replace_test/" + basicFileName;
        Response basicAddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), basicPathToFile, apiToken);
        basicFileId = JsonPath.from(basicAddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
        tabFile1Name = "120745.dta";
        tabFile1NameConvert = tabFile1Name.substring(0, tabFile1Name.indexOf(".dta")) + ".tab";
        String tab1PathToFile = "scripts/search/data/tabular/" + tabFile1Name;
        Thread.sleep(1000); //Added because tests are failing during setup, test is probably going too fast. Especially between first and second file
        Response tab1AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab1PathToFile, apiToken);
        tabFile1Id = JsonPath.from(tab1AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        //String origFilePid = JsonPath.from(addResponse.body().asString()).getString("data.files[0].dataFile.persistentId");
        
        tabFile2Name = "stata13-auto.dta";
        tabFile2NameConvert = tabFile2Name.substring(0, tabFile2Name.indexOf(".dta")) + ".tab";
        String tab2PathToFile = "scripts/search/data/tabular/" + tabFile2Name;
        Thread.sleep(1000); //Added because tests are failing during setup, test is probably going too fast. Especially between first and second file
        Response tab2AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab2PathToFile, apiToken);
        tabFile2Id = JsonPath.from(tab2AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
        tabFile3NameRestricted = "stata13-auto-withstrls.dta";
        tabFile3NameRestrictedConvert = tabFile3NameRestricted.substring(0, tabFile3NameRestricted.indexOf(".dta")) + ".tab";
        String tab3PathToFile = "scripts/search/data/tabular/" + tabFile3NameRestricted;
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
        
        tabFile4NameUnpublished = "stata14-auto-withstrls.dta";
        tabFile4NameUnpublishedConvert = tabFile4NameUnpublished.substring(0, tabFile4NameUnpublished.indexOf(".dta")) + ".tab";
        String tab4PathToFile = "scripts/search/data/tabular/" + tabFile4NameUnpublished;
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
    public void testDownloadMultipleFiles_NonLoggedInOpen() throws IOException {
        Response anonDownloadOriginal = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile2Id});
        assertEquals(OK.getStatusCode(), anonDownloadOriginal.getStatusCode());
        int origSizeAnon = anonDownloadOriginal.getBody().asByteArray().length;
        HashMap<String,ByteArrayOutputStream> files1 = readZipResponse(anonDownloadOriginal.getBody().asInputStream());
        assertEquals(4, files1.size()); //size +1 for manifest
        assertThat(files1, IsMapContaining.hasKey(basicFileName));
        assertThat(files1, IsMapContaining.hasKey(tabFile1Name));
        assertThat(files1, IsMapContaining.hasKey(tabFile2Name));
        
        Response anonDownloadConverted = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile2Id});
        assertEquals(OK.getStatusCode(), anonDownloadConverted.getStatusCode());
        int convertSizeAnon = anonDownloadConverted.getBody().asByteArray().length;
        HashMap<String,ByteArrayOutputStream> files2 = readZipResponse(anonDownloadConverted.getBody().asInputStream());
        assertEquals(4, files2.size()); //size +1 for manifest
        assertThat(files2, IsMapContaining.hasKey(basicFileName));
        assertThat(files2, IsMapContaining.hasKey(tabFile1NameConvert));
        assertThat(files2, IsMapContaining.hasKey(tabFile2NameConvert));
        
        System.out.println("origSize: " + origSizeAnon + " | convertSize: " + convertSizeAnon);
        assertThat(origSizeAnon, is(not(convertSizeAnon)));  
    }
    
    @Test
    public void testDownloadMultipleFiles_NonLoggedInRestricted() throws IOException {    
        Response anonDownloadOriginalRestricted = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile3IdRestricted});
        assertEquals(200, anonDownloadOriginalRestricted.getStatusCode());
        HashMap<String,ByteArrayOutputStream> files1 = readZipResponse(anonDownloadOriginalRestricted.getBody().asInputStream());
        assertEquals(3, files1.size()); //size +1 for manifest, -1 for not included restricted file
        assertThat(files1, IsMapContaining.hasKey(basicFileName));
        assertThat(files1, IsMapContaining.hasKey(tabFile1Name));
        assertThat(files1, not(IsMapContaining.hasKey(tabFile3NameRestricted)));
        
        Response anonDownloadConvertedRestricted = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile3IdRestricted});
        assertEquals(200, anonDownloadConvertedRestricted.getStatusCode());
        HashMap<String,ByteArrayOutputStream> files2 = readZipResponse(anonDownloadConvertedRestricted.getBody().asInputStream());
        assertEquals(3, files2.size()); //size +1 for manifest, -1 for not included restricted file
        assertThat(files2, IsMapContaining.hasKey(basicFileName));
        assertThat(files2, IsMapContaining.hasKey(tabFile1NameConvert));
        assertThat(files2, not(IsMapContaining.hasKey(tabFile3NameRestricted)));
    
    }
    
    @Test
    public void testDownloadMultipleFiles_LoggedInOpen() throws IOException {
        Response authDownloadOriginal = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile2Id}, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadOriginal.getStatusCode());
        int origSizeAuth = authDownloadOriginal.getBody().asByteArray().length;
        HashMap<String,ByteArrayOutputStream> files1 = readZipResponse(authDownloadOriginal.getBody().asInputStream());
        assertEquals(4, files1.size()); //size +1 for manifest
        assertThat(files1, IsMapContaining.hasKey(basicFileName));
        assertThat(files1, IsMapContaining.hasKey(tabFile1Name));
        assertThat(files1, not(IsMapContaining.hasKey(tabFile3NameRestricted)));
        
        Response authDownloadConverted = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile2Id}, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadConverted.getStatusCode());
        int convertSizeAuth = authDownloadConverted.getBody().asByteArray().length;        
        HashMap<String,ByteArrayOutputStream> files2 = readZipResponse(authDownloadConverted.getBody().asInputStream());
        assertEquals(4, files2.size()); //size +1 for manifest
        assertThat(files2, IsMapContaining.hasKey(basicFileName));
        assertThat(files2, IsMapContaining.hasKey(tabFile1NameConvert));
        assertThat(files2, IsMapContaining.hasKey(tabFile2NameConvert));

        System.out.println("origSize: "+origSizeAuth + " | convertSize: " + convertSizeAuth);
        assertThat(origSizeAuth, is(not(convertSizeAuth)));  
    }
    
    @Test
    public void testDownloadMultipleFiles_LoggedInRestricted() throws IOException {    
        Response authDownloadOriginalRestricted = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile3IdRestricted}, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadOriginalRestricted.getStatusCode());
        int origSizeAuthRestricted = authDownloadOriginalRestricted.getBody().asByteArray().length;
        HashMap<String,ByteArrayOutputStream> files1 = readZipResponse(authDownloadOriginalRestricted.getBody().asInputStream());
        assertEquals(4, files1.size()); //size +1 for manifest, we have access to restricted
        assertThat(files1, IsMapContaining.hasKey(basicFileName));
        assertThat(files1, IsMapContaining.hasKey(tabFile1Name));
        assertThat(files1, IsMapContaining.hasKey(tabFile3NameRestricted));
        
        Response authDownloadConvertedRestricted = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile3IdRestricted}, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadConvertedRestricted.getStatusCode());
        int convertSizeAuthRestricted = authDownloadConvertedRestricted.getBody().asByteArray().length;
        HashMap<String,ByteArrayOutputStream> files2 = readZipResponse(authDownloadConvertedRestricted.getBody().asInputStream());
        assertEquals(4, files2.size()); //size +1 for manifest, we have access to restricted
        assertThat(files2, IsMapContaining.hasKey(basicFileName));
        assertThat(files2, IsMapContaining.hasKey(tabFile1NameConvert));
        assertThat(files2, not(IsMapContaining.hasKey(tabFile3NameRestricted)));
        
        System.out.println("origSize: "+origSizeAuthRestricted + " | convertSize: " + convertSizeAuthRestricted);
        assertThat(origSizeAuthRestricted, is(not(convertSizeAuthRestricted)));  
    }
        
    @Test
    public void testDownloadMultipleFiles_LoggedInUnpublished() throws IOException {  
        Response authDownloadOriginalUnpublished = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile4IdUnpublished}, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadOriginalUnpublished.getStatusCode());
        HashMap<String,ByteArrayOutputStream> files1 = readZipResponse(authDownloadOriginalUnpublished.getBody().asInputStream());
        assertEquals(4, files1.size()); //size +1 for manifest, we have access to unpublished
        
        Response authDownloadConvertedUnpublished = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile4IdUnpublished}, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadConvertedUnpublished.getStatusCode());
        HashMap<String,ByteArrayOutputStream> files2 = readZipResponse(authDownloadConvertedUnpublished.getBody().asInputStream());
        assertEquals(4, files2.size()); //size +1 for manifest, we have access to unpublished
    }
    
    //This test is fairly brittle. We can't see the contents of the zip but a partial
    //one is returned. So I am checking to see if the sizes of the two unpublished anon
    //downloads are small enough that we are confident the result was halted early.
    //
    @Test
    public void testDownloadMultipleFiles_LoggedAndNot_Unpublished() throws IOException {   
        int margin = 100; //When comparing sizes, there may be some difference not due to the file exclusion. A very rough estimate
        Response authDownloadOriginalUnpublished = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile4IdUnpublished}, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadOriginalUnpublished.getStatusCode());
        int origAuthSize = authDownloadOriginalUnpublished.getBody().asByteArray().length;
        HashMap<String,ByteArrayOutputStream> files1 = readZipResponse(authDownloadOriginalUnpublished.getBody().asInputStream());
        assertEquals(4, files1.size()); //size +1 for manifest, we have access to unpublished
        
        Response authDownloadConvertedUnpublished = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile4IdUnpublished}, apiToken);
        assertEquals(OK.getStatusCode(), authDownloadConvertedUnpublished.getStatusCode());
        int convertAuthSize = authDownloadConvertedUnpublished.getBody().asByteArray().length;
        HashMap<String,ByteArrayOutputStream> files2 = readZipResponse(authDownloadConvertedUnpublished.getBody().asInputStream());
        assertEquals(4, files2.size()); //size +1 for manifest, we have access to unpublished

        Response anonDownloadOriginalUnpublished = UtilIT.downloadFilesOriginal(new Integer[]{basicFileId,tabFile1Id,tabFile4IdUnpublished});
        assertEquals(404, anonDownloadOriginalUnpublished.getStatusCode());
        int origAnonSize = anonDownloadOriginalUnpublished.getBody().asByteArray().length;
        HashMap<String,ByteArrayOutputStream> files3 = readZipResponse(anonDownloadOriginalUnpublished.getBody().asInputStream());
        assertEquals(0, files3.size()); //A size of 0 indicates the zip creation was interrupted.
        assertTrue(origAnonSize < origAuthSize + margin);
        
        Response anonDownloadConvertedUnpublished = UtilIT.downloadFiles(new Integer[]{basicFileId,tabFile1Id,tabFile4IdUnpublished});
        assertEquals(404, anonDownloadConvertedUnpublished.getStatusCode());
        int convertAnonSize = anonDownloadConvertedUnpublished.getBody().asByteArray().length;
        HashMap<String,ByteArrayOutputStream> files4 = readZipResponse(anonDownloadConvertedUnpublished.getBody().asInputStream());
        assertEquals(0, files4.size()); //A size of 0 indicates the zip creation was interrupted.
        assertTrue(convertAnonSize < convertAuthSize + margin);
    }
    
    //Reads an inputstream zip from a response and returns a map of outputStreams to use
    //
    //Currently this is unable to read the incomplete zips created by Dataverse
    //when the creation of the zip is interrupted (non-existent file, etc).
    //Supporting this seems extremely messy and we have already disussed changing
    //how this is done in #4576 --MAD 4.9.2
    private HashMap<String,ByteArrayOutputStream> readZipResponse(InputStream iStream) throws IOException {
        byte[] buffer = new byte[2048];
        
        HashMap<String, ByteArrayOutputStream> fileStreams = new HashMap<>();
        ZipInputStream zStream = new ZipInputStream(iStream);
        try
        {

            ZipEntry entry;
            while((entry = zStream.getNextEntry())!=null)
            {
                String name = new File(entry.getName()).getName(); //to get name without path
//                String s = String.format("Entry: %s len %d added %TD",
//                                entry.getName(), entry.getSize(),
//                                new Date(entry.getTime()));
//                System.out.println(s);

                // Once we get the entry from the zStream, the zStream is
                // positioned read to read the raw data, and we keep
                // reading until read returns 0 or less.
                
                ByteArrayOutputStream output = null; 
                

                try
                {
                    output = new ByteArrayOutputStream();
                    int len = 0;
                    while ((len = zStream.read(buffer)) > 0)
                    {
                        output.write(buffer, 0, len);
                    }
                }
                finally
                {
                    // we must always close the output file
                    if(output!=null) {
                        fileStreams.put(name, output);
                        //fileStreams.add(output);
                        output.close();
                    }
                }
            }
        }
        finally
        {
            // we must always close the zip file.
            zStream.close();
        }

        return fileStreams;
    }
    

}
