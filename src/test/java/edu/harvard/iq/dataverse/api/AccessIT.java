/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import jakarta.json.Json;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.zip.ZipEntry;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.collection.IsMapContaining;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.*;

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
    public static String persistentId; 

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
    
    public static int tabFile1SizeOriginal = 279;
    public static int tabFile1SizeConverted = 4;
    public static int tabFile1SizeConvertedWithVarHeader = 9; 
    
    private static String testZipFileWithFolders = "scripts/api/data/zip/test.zip";
    // these are the files inside the test zip file above:
    private static String testFileFromZipUploadWithFolders1 = "file1.txt"; // this file sits at the top level of the zip archive, no folders...
    private static String testFileFromZipUploadWithFolders2 = "folder1/file11.txt";
    private static String testFileFromZipUploadWithFolders3 = "folder2/file22.txt";
    
    private static int testFileFromZipUploadWithFoldersSize1 = 26; 
    private static int testFileFromZipUploadWithFoldersSize2 = 27; 
    private static int testFileFromZipUploadWithFoldersSize3 = 27; 
    
    private static String testFileFromZipUploadWithFoldersChecksum1 = "8f326944be21361ad8219bc3269bc9eb";
    private static String testFileFromZipUploadWithFoldersChecksum2 = "0fe4efd85229bad6e587fd3f1a6c8e05";
    private static String testFileFromZipUploadWithFoldersChecksum3 = "00433ccb20111f9d40f0e5ab6fa8396f";
    
    @BeforeAll
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
        persistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");

        Response allowAccessRequests = UtilIT.allowAccessRequests(datasetId.toString(), true, apiToken);
        allowAccessRequests.prettyPrint();
        allowAccessRequests.then().assertThat().statusCode(200);
        
        basicFileName = "004.txt";
        String basicPathToFile = "scripts/search/data/replace_test/" + basicFileName;
        Response basicAddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), basicPathToFile, apiToken);
        basicFileId = JsonPath.from(basicAddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
        tabFile1Name = "120745.dta";
        tabFile1NameConvert = tabFile1Name.substring(0, tabFile1Name.indexOf(".dta")) + ".tab";
        String tab1PathToFile = "scripts/search/data/tabular/" + tabFile1Name;
        Response tab1AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab1PathToFile, apiToken);
        tabFile1Id = JsonPath.from(tab1AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        //String origFilePid = JsonPath.from(addResponse.body().asString()).getString("data.files[0].dataFile.persistentId");
        
        tabFile2Name = "stata13-auto.dta";
        tabFile2NameConvert = tabFile2Name.substring(0, tabFile2Name.indexOf(".dta")) + ".tab";
        String tab2PathToFile = "scripts/search/data/tabular/" + tabFile2Name;

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + tabFile2Name);

        Response tab2AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab2PathToFile, apiToken);
        tabFile2Id = JsonPath.from(tab2AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
        tabFile3NameRestricted = "stata13-auto-withstrls.dta";
        tabFile3NameRestrictedConvert = tabFile3NameRestricted.substring(0, tabFile3NameRestricted.indexOf(".dta")) + ".tab";
        String tab3PathToFile = "scripts/search/data/tabular/" + tabFile3NameRestricted;

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + tabFile3NameRestricted);
        
        Response tab3AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab3PathToFile, apiToken);

        tabFile3IdRestricted = JsonPath.from(tab3AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + tabFile3NameRestricted);
        
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
        Response tab4AddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), tab4PathToFile, apiToken);
        tabFile4IdUnpublished = JsonPath.from(tab4AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + tabFile2Name);
                        
    }
    
    @AfterAll
    public static void tearDown() {   

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());

        Response deleteDatasetResponse = UtilIT.destroyDataset(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());

    }
    

    @Test
    public void testSaveAuxiliaryFileWithVersion() throws IOException {
        System.out.println("Add aux file with update");
        String mimeType = null;
        String pathToFile = "scripts/search/data/tabular/1char";
        String formatTag = "dpJSON";
        String formatVersion = "v1";

        Response uploadResponse = UtilIT.uploadAuxFile(tabFile3IdRestricted.longValue(), pathToFile, formatTag, formatVersion, mimeType, true, null, apiToken);
        uploadResponse.prettyPrint();
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());

        System.out.println("Downloading Aux file that was just added");
        Response downloadResponse = UtilIT.downloadAuxFile(tabFile3IdRestricted.longValue(), formatTag, formatVersion, apiToken);
        downloadResponse.then().assertThat().statusCode(OK.getStatusCode());
        String dataStr = downloadResponse.prettyPrint();
        assertEquals(dataStr, "a\n");
    }
    
    //This test does a lot of testing of non-original downloads as well
    @Test
    public void testDownloadSingleFile() {
        //Not logged in non-restricted
        Response anonDownloadOriginal = UtilIT.downloadFileOriginal(tabFile1Id);
        Response anonDownloadConverted = UtilIT.downloadFile(tabFile1Id);
        Response anonDownloadConvertedNullKey = UtilIT.downloadFile(tabFile1Id, null);

        // ... and download the same tabular data file, but without the variable name header added:
        Response anonDownloadTabularNoHeader = UtilIT.downloadTabularFileNoVarHeader(tabFile1Id);
        // ... and download the same tabular file, this time requesting the "format=tab" explicitly:
        Response anonDownloadTabularWithFormatName = UtilIT.downloadTabularFile(tabFile1Id);
        assertEquals(OK.getStatusCode(), anonDownloadOriginal.getStatusCode());
        assertEquals(OK.getStatusCode(), anonDownloadConverted.getStatusCode());
        assertEquals(OK.getStatusCode(), anonDownloadTabularNoHeader.getStatusCode());
        assertEquals(OK.getStatusCode(), anonDownloadTabularWithFormatName.getStatusCode());
        assertEquals(UNAUTHORIZED.getStatusCode(), anonDownloadConvertedNullKey.getStatusCode());
        
        int origSizeAnon = anonDownloadOriginal.getBody().asByteArray().length;
        int convertSizeAnon = anonDownloadConverted.getBody().asByteArray().length;
        int tabularSizeNoVarHeader = anonDownloadTabularNoHeader.getBody().asByteArray().length;
        int tabularSizeWithFormatName = anonDownloadTabularWithFormatName.getBody().asByteArray().length;
        System.out.println("origSize: "+origSizeAnon + " | convertSize: " + convertSizeAnon + " | convertNoHeaderSize: " + tabularSizeNoVarHeader);

        assertEquals(tabFile1SizeOriginal, origSizeAnon);
        assertEquals(tabFile1SizeConvertedWithVarHeader, convertSizeAnon);        
        assertEquals(tabFile1SizeConverted, tabularSizeNoVarHeader);
        assertEquals(tabFile1SizeConvertedWithVarHeader, tabularSizeWithFormatName);
        
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
        
        // Finally, verify that the multi-file bundle produced by the API  
        // is properly named (as of v6.7 this should be a pretty name based on 
        // the persistent Id of the dataset). 
        
        String contentDispositionHeader = anonDownloadConverted.getHeader("Content-disposition");
        System.out.println("Response header: "+contentDispositionHeader);
        
        Pattern regexPattern = Pattern.compile("attachment; filename=\"([a-z0-9\\.-]*\\.zip)\"");
        Matcher regexMatcher = regexPattern.matcher(contentDispositionHeader);
        boolean regexMatch = regexMatcher.find();
        assertTrue(regexMatch);
        
        String expectedPrettyName = persistentId.replaceAll("[:/]", "-").toLowerCase() + ".zip";
        System.out.println("expected \"pretty\" file name of the zipped multi-file bundle: " + expectedPrettyName);
        
        String fileBundleName = regexMatcher.group(1);
        System.out.println("file name found in the header: "+fileBundleName);
        
        assertEquals(fileBundleName, expectedPrettyName);
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
                if (entry.isDirectory()) {
                    // Dataverse zip bundles can contain folder entries!
                    // (we just skip them)
                    continue;
                }

                String name = entry.getName(); 


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
    
    @Test
    public void testRequestAccess() throws InterruptedException {
    
        String pathToJsonFile = "scripts/api/data/dataset-create-new.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetIdNew = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        
        basicFileName = "004.txt";
        String basicPathToFile = "scripts/search/data/replace_test/" + basicFileName;
        Response basicAddResponse = UtilIT.uploadFileViaNative(datasetIdNew.toString(), basicPathToFile, apiToken);
        Integer basicFileIdNew = JsonPath.from(basicAddResponse.body().asString()).getInt("data.files[0].dataFile.id");

        String tabFile3NameRestrictedNew = "stata13-auto-withstrls.dta";
        String tab3PathToFile = "scripts/search/data/tabular/" + tabFile3NameRestrictedNew;
        Response tab3AddResponse = UtilIT.uploadFileViaNative(datasetIdNew.toString(), tab3PathToFile, apiToken);
        Integer tabFile3IdRestrictedNew = JsonPath.from(tab3AddResponse.body().asString()).getInt("data.files[0].dataFile.id");

        assertTrue(UtilIT.sleepForLock(datasetIdNew.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + tab3PathToFile);
        
        Response restrictResponse = UtilIT.restrictFile(tabFile3IdRestrictedNew.toString(), true, apiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        String apiTokenRando = UtilIT.getApiTokenFromResponse(createUser);
        String apiIdentifierRando = UtilIT.getUsernameFromResponse(createUser);

        Response randoDownload = UtilIT.downloadFile(tabFile3IdRestrictedNew, apiTokenRando);
        assertEquals(403, randoDownload.getStatusCode());

        Response requestFileAccessResponse = UtilIT.requestFileAccess(tabFile3IdRestrictedNew.toString(), apiTokenRando);
        //Cannot request until we set the dataset to allow requests
        assertEquals(400, requestFileAccessResponse.getStatusCode());
        //Update Dataset to allow requests
        Response allowAccessRequestsResponse = UtilIT.allowAccessRequests(datasetIdNew.toString(), true, apiToken);
        assertEquals(200, allowAccessRequestsResponse.getStatusCode());
        //Must republish to get it to work
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetIdNew, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());

        requestFileAccessResponse = UtilIT.requestFileAccess(tabFile3IdRestrictedNew.toString(), apiTokenRando);
        assertEquals(200, requestFileAccessResponse.getStatusCode());

        Response listAccessRequestResponse = UtilIT.getAccessRequestList(tabFile3IdRestrictedNew.toString(), apiToken);
        listAccessRequestResponse.prettyPrint();
        assertEquals(200, listAccessRequestResponse.getStatusCode());
        System.out.println("List Access Request: " + listAccessRequestResponse.prettyPrint());

        listAccessRequestResponse = UtilIT.getAccessRequestList(tabFile3IdRestrictedNew.toString(), apiTokenRando);
        listAccessRequestResponse.prettyPrint();
        assertEquals(403, listAccessRequestResponse.getStatusCode());

        Response rejectFileAccessResponse = UtilIT.rejectFileAccessRequest(tabFile3IdRestrictedNew.toString(), "@" + apiIdentifierRando, apiToken);
        assertEquals(200, rejectFileAccessResponse.getStatusCode());

        requestFileAccessResponse = UtilIT.requestFileAccess(tabFile3IdRestrictedNew.toString(), apiTokenRando);
        //grant file access
        Response grantFileAccessResponse = UtilIT.grantFileAccess(tabFile3IdRestrictedNew.toString(), "@" + apiIdentifierRando, apiToken);
        assertEquals(200, grantFileAccessResponse.getStatusCode());
        
        //if you make a request while you have been granted access you should get a command exception
        requestFileAccessResponse = UtilIT.requestFileAccess(tabFile3IdRestrictedNew.toString(), apiTokenRando);
        assertEquals(400, requestFileAccessResponse.getStatusCode());
        
        //if you make a request of a public file you should also get a command exception
        requestFileAccessResponse = UtilIT.requestFileAccess(basicFileIdNew.toString(), apiTokenRando);
        assertEquals(400, requestFileAccessResponse.getStatusCode());
        

        //Now should be able to download
        randoDownload = UtilIT.downloadFile(tabFile3IdRestrictedNew, apiTokenRando);
        assertEquals(OK.getStatusCode(), randoDownload.getStatusCode());

        //revokeFileAccess        
        Response revokeFileAccessResponse = UtilIT.revokeFileAccess(tabFile3IdRestrictedNew.toString(), "@" + apiIdentifierRando, apiToken);
        assertEquals(200, revokeFileAccessResponse.getStatusCode());

        listAccessRequestResponse = UtilIT.getAccessRequestList(tabFile3IdRestrictedNew.toString(), apiToken);
        assertEquals(404, listAccessRequestResponse.getStatusCode());
    }

    // This is a round trip test of uploading a zipped archive, with some folder
    // structure, then downloading the resulting multiple files as a multi-file
    // zipped bundle - that should have the folder hierarchy preserved. 
    @Test
    public void testZipUploadAndDownload() throws IOException {
        
        System.out.println("Testing round trip zip upload-and-download");
        // Upload the zip file that has a mix of files with and without folders:
        Response uploadZipResponse = UtilIT.uploadFileViaNative(datasetId.toString(), testZipFileWithFolders, apiToken);
        String responseBodyAsString = uploadZipResponse.body().asString();
        assertEquals(OK.getStatusCode(), uploadZipResponse.getStatusCode());        
        
        JsonPath responseBodyAsJson = JsonPath.from(responseBodyAsString);
        
        Integer fileId1 = responseBodyAsJson.getInt("data.files[0].dataFile.id");
        Integer fileId2 = responseBodyAsJson.getInt("data.files[1].dataFile.id");
        Integer fileId3 = responseBodyAsJson.getInt("data.files[2].dataFile.id");
        
        assertNotNull(fileId1);
        assertNotNull(fileId2);
        assertNotNull(fileId3);
        
        String uploadedFileName1 = responseBodyAsJson.getString("data.files[0].dataFile.filename");
        assertEquals(uploadedFileName1, testFileFromZipUploadWithFolders1);
        
        String uploadedFileName2 = responseBodyAsJson.getString("data.files[1].dataFile.filename");
        assertEquals(uploadedFileName2, testFileFromZipUploadWithFolders2.substring(testFileFromZipUploadWithFolders2.lastIndexOf('/')+1));
        
        String uploadedFileName3 = responseBodyAsJson.getString("data.files[2].dataFile.filename");
        assertEquals(uploadedFileName3, testFileFromZipUploadWithFolders3.substring(testFileFromZipUploadWithFolders3.lastIndexOf('/')+1));
        
        int uploadedFileSize1 = responseBodyAsJson.getInt("data.files[0].dataFile.filesize");
        assertEquals(testFileFromZipUploadWithFoldersSize1, uploadedFileSize1);
        
        int uploadedFileSize2 = responseBodyAsJson.getInt("data.files[1].dataFile.filesize");
        assertEquals(testFileFromZipUploadWithFoldersSize2, uploadedFileSize2);
        
        int uploadedFileSize3 = responseBodyAsJson.getInt("data.files[2].dataFile.filesize");
        assertEquals(testFileFromZipUploadWithFoldersSize3, uploadedFileSize3);
        
        System.out.println("Successfully uploaded the zip file; all files added to the dataset.");
        
        // Try to download the 3 files as a zip bundle: 
        
        Response downloadAsZipResponse = UtilIT.downloadFiles(new Integer[]{fileId1, fileId2, fileId3}, apiToken);
        assertEquals(OK.getStatusCode(), downloadAsZipResponse.getStatusCode());
        HashMap<String,ByteArrayOutputStream> unzippedFiles = readZipResponse(downloadAsZipResponse.getBody().asInputStream());
        
        // Check that we did in fact get 3 zipped files (pluse the manifest file):
        assertEquals(4, unzippedFiles.size()); 
        
        System.out.println("Successfully downloaded the 3 test files as a zip bundle.");
        
        // Check that the zipped bundle contains the 3 files requested, with the folder names: 
        
        assertThat(unzippedFiles, IsMapContaining.hasKey(testFileFromZipUploadWithFolders1));
        assertThat(unzippedFiles, IsMapContaining.hasKey(testFileFromZipUploadWithFolders2));
        assertThat(unzippedFiles, IsMapContaining.hasKey(testFileFromZipUploadWithFolders3));
        
        System.out.println("File names and folders are properly preserved in the downloaded zip bundle.");
        
        // ... and check the file sizes: 
        
        assertEquals(testFileFromZipUploadWithFoldersSize1, unzippedFiles.get(testFileFromZipUploadWithFolders1).size());
        assertEquals(testFileFromZipUploadWithFoldersSize2, unzippedFiles.get(testFileFromZipUploadWithFolders2).size());
        assertEquals(testFileFromZipUploadWithFoldersSize3, unzippedFiles.get(testFileFromZipUploadWithFolders3).size());
        
        System.out.println("File sizes are correct in the download zip bundle.");
        
        // And finally, check the md5 checksums of the unzipped file streams: 
        
        assertEquals(testFileFromZipUploadWithFoldersChecksum1, FileUtil.calculateChecksum(unzippedFiles.get(testFileFromZipUploadWithFolders1).toByteArray(), DataFile.ChecksumType.MD5));
        assertEquals(testFileFromZipUploadWithFoldersChecksum2, FileUtil.calculateChecksum(unzippedFiles.get(testFileFromZipUploadWithFolders2).toByteArray(), DataFile.ChecksumType.MD5)); 
        assertEquals(testFileFromZipUploadWithFoldersChecksum3, FileUtil.calculateChecksum(unzippedFiles.get(testFileFromZipUploadWithFolders3).toByteArray(), DataFile.ChecksumType.MD5));
        
        System.out.println("MD5 checksums of the unzipped file streams are correct.");
        
        System.out.println("Zip upload-and-download round trip test: success!");
    }

    @Test
    public void testGetUserFileAccessRequested() {
        // Create new user
        Response createUserResponse = UtilIT.createRandomUser();
        createUserResponse.then().assertThat().statusCode(OK.getStatusCode());
        String newUserApiToken = UtilIT.getApiTokenFromResponse(createUserResponse);

        String dataFileId = Integer.toString(tabFile3IdRestricted);

        // Call with new user and unrequested access file
        Response getUserFileAccessRequestedResponse = UtilIT.getUserFileAccessRequested(dataFileId, newUserApiToken);
        getUserFileAccessRequestedResponse.then().assertThat().statusCode(OK.getStatusCode());

        boolean userFileAccessRequested = JsonPath.from(getUserFileAccessRequestedResponse.body().asString()).getBoolean("data");
        assertFalse(userFileAccessRequested);

        // Request file access for the new user
        Response requestFileAccessResponse = UtilIT.requestFileAccess(dataFileId, newUserApiToken);
        requestFileAccessResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Call with new user and requested access file
        getUserFileAccessRequestedResponse = UtilIT.getUserFileAccessRequested(dataFileId, newUserApiToken);
        getUserFileAccessRequestedResponse.then().assertThat().statusCode(OK.getStatusCode());

        userFileAccessRequested = JsonPath.from(getUserFileAccessRequestedResponse.body().asString()).getBoolean("data");
        assertTrue(userFileAccessRequested);
    }

    @Test
    public void testGetUserPermissionsOnFile() {
        // Call with valid file id
        Response getUserPermissionsOnFileResponse = UtilIT.getUserPermissionsOnFile(Integer.toString(basicFileId), apiToken);
        getUserPermissionsOnFileResponse.then().assertThat().statusCode(OK.getStatusCode());
        boolean canDownloadFile = JsonPath.from(getUserPermissionsOnFileResponse.body().asString()).getBoolean("data.canDownloadFile");
        assertTrue(canDownloadFile);
        boolean canEditOwnerDataset = JsonPath.from(getUserPermissionsOnFileResponse.body().asString()).getBoolean("data.canEditOwnerDataset");
        assertTrue(canEditOwnerDataset);
        boolean canManageFilePermissions = JsonPath.from(getUserPermissionsOnFileResponse.body().asString()).getBoolean("data.canManageFilePermissions");
        assertTrue(canManageFilePermissions);

        // Call with invalid file id
        Response getUserPermissionsOnFileInvalidIdResponse = UtilIT.getUserPermissionsOnFile("testInvalidId", apiToken);
        getUserPermissionsOnFileInvalidIdResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }
}
