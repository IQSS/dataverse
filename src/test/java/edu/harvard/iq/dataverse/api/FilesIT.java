package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.List;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.api.auth.ApiKeyAuthMechanism;
import jakarta.json.JsonObject;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import io.restassured.path.json.JsonPath;

import static edu.harvard.iq.dataverse.api.ApiConstants.*;
import static io.restassured.path.json.JsonPath.with;
import io.restassured.path.xml.XmlPath;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.File;
import java.io.IOException;

import static java.lang.Thread.sleep;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import static jakarta.ws.rs.core.Response.Status.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Year;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class FilesIT {

    private static final Logger logger = Logger.getLogger(FilesIT.class.getCanonicalName());

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response removePublicInstall = UtilIT.deleteSetting(SettingsServiceBean.Key.PublicInstall);
        removePublicInstall.then().assertThat().statusCode(200);

    }

    @AfterAll
    public static void tearDownClass() {
        UtilIT.deleteSetting(SettingsServiceBean.Key.PublicInstall);
    }

    /**
     * Create user and get apiToken
     * 
     * @return 
     */
    private String createUserGetToken(){
        Response createUser = UtilIT.createRandomUser();
        msg(createUser.toString());
        msg(createUser.prettyPrint());
        createUser.then().assertThat().statusCode(OK.getStatusCode());

        msg(createUser.prettyPrint());

        
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        System.out.println(apiToken);
        return apiToken;
    }
    
    
    private String createDataverseGetAlias(String apiToken){
        
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        //createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        return dataverseAlias;
    }
    
    
    private Integer createDatasetGetId(String dataverseAlias, String apiToken){
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        
        return datasetId;
        
    }
    
    @Test
    public void test_001_AddFileGood() {
        msgt("test_001_AddFileGood");
         // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);
       
        
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";

        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", "my description")
                .add("directoryLabel", "data/subdir1")
                .add("categories", Json.createArrayBuilder()
                        .add("Data")
                );

        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, json.build(), apiToken);

        //addResponse.prettyPrint();
        msgt("Here it is: " + addResponse.prettyPrint());
        String successMsg = BundleUtil.getStringFromBundle("file.addreplace.success.add");
      
        addResponse.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                .body("status", equalTo(ApiConstants.STATUS_OK))
                .body("data.files[0].categories[0]", equalTo("Data"))
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].dataFile.description", equalTo("my description"))
                .body("data.files[0].directoryLabel", equalTo("data/subdir1"))
                .body("data.files[0].dataFile.tabularTags", nullValue())
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                // not sure why description appears in two places
                .body("data.files[0].description", equalTo("my description"))
                .statusCode(OK.getStatusCode());
        
        
        //------------------------------------------------
        // Try to add the same file again -- and get warning
        //------------------------------------------------
        Response addTwiceResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        msgt("2nd requests: " + addTwiceResponse.prettyPrint());    //addResponse.prettyPrint();
        
        String dupeName = "dataverseproject.png";

        String errMsg = BundleUtil.getStringFromBundle("file.addreplace.warning.duplicate_file",
                Arrays.asList(dupeName));
        String errMsgFromResponse = JsonPath.from(addTwiceResponse.body().asString()).getString("message");
        addTwiceResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        assertTrue(errMsgFromResponse.contains(errMsg));
    }

    
    @Test
    public void test_002_AddFileBadDatasetId() {
        msgt("test_002_AddFileNullFileId");
         // Create user
        String apiToken =createUserGetToken();

        // Create Dataset
        String datasetId = "cat"; //createDatasetGetId(dataverseAlias, apiToken);
       
        
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response addResponse = UtilIT.uploadFileViaNative("cat", pathToFile, apiToken);
        //msgt("Here it is: " + addResponse.prettyPrint());

        // Adding a non-numeric id should result in a 404
        addResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());
    }
    
    
    @Test
    public void test_003_AddFileNonExistentDatasetId() {
        msgt("test_003_AddFileNonExistentDatasetId");

        // Create user
        String apiToken = createUserGetToken();

        // Create Dataset
        String datasetId = "9999"; //createDatasetGetId(dataverseAlias, apiToken);
       
        
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId, pathToFile, apiToken);


        msgt("Here it is: " + addResponse.prettyPrint());

        //String errMsg Start = BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.id");
        String errMsg = BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.id", Collections.singletonList(datasetId));
                
         addResponse.then().assertThat()
                .body("status", equalTo(ApiConstants.STATUS_ERROR))
                .body("message", equalTo(errMsg))
                .statusCode(NOT_FOUND.getStatusCode());
    }
    
    @Test
    public void test_004_AddFileBadToken() {
        msgt("test_004_AddFileBadToken");

        // Create user
        String apiToken = "Bad Medicine";

        // Create Dataset - should pick up permissions error first
        String datasetId = "1"; //createDatasetGetId(dataverseAlias, apiToken);
       
        
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId, pathToFile, apiToken);

        msgt("Here it is: " + addResponse.prettyPrint());


        addResponse.then().assertThat()
                .body("status", equalTo(ApiConstants.STATUS_ERROR))
                .body("message", equalTo(ApiKeyAuthMechanism.RESPONSE_MESSAGE_BAD_API_KEY))
                .statusCode(UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void testAddFileBadJson() {
        msgt("testAddFileBadJson");

        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";

        String junkJson = "thisIsNotJson";

        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, junkJson, apiToken);

        String parseError = BundleUtil.getStringFromBundle("file.addreplace.error.parsing");
        
        addResponse.then().assertThat()
        .statusCode(BAD_REQUEST.getStatusCode())
        .body("status", equalTo(ApiConstants.STATUS_ERROR))
        .body("message", equalTo(parseError));
    }
    
    @Test
    public void test_005_AddFileBadPermissions() {
        msgt("test_005_AddFileBadPerms");

         // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);
       
        // Create another user
        String apiTokenUnauthorizedUser = createUserGetToken();

        
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiTokenUnauthorizedUser);

        //addResponse.prettyPrint();
        msgt("Here it is: " + addResponse.prettyPrint());
        
        
        String errMsg = BundleUtil.getStringFromBundle("file.addreplace.error.no_edit_dataset_permission");

      
        addResponse.then().assertThat()
                .body("message", equalTo(errMsg))
                .body("status", equalTo(ApiConstants.STATUS_ERROR))
                .statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    public void test_006_ReplaceFileGood() throws InterruptedException {
        
        msgt("test_006_ReplaceFileGood");

        // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        // -------------------------
        // Add initial file
        // -------------------------
        msg("Add initial file");
        String pathToFile = "scripts/search/data/replace_test/003.txt";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = BundleUtil.getStringFromBundle("file.addreplace.success.add");
      
        addResponse.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsgAdd))
                .body("data.files[0].dataFile.contentType", startsWith("text/plain"))
                .body("data.files[0].label", equalTo("003.txt"))
                .body("data.files[0].description", equalTo(""))
                .statusCode(OK.getStatusCode());
        
        
        Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");
        String origFilePid = JsonPath.from(addResponse.body().asString()).getString("data.files[0].dataFile.persistentId");

        msg("Orig file id: " + origFileId);
        assertNotNull(origFileId);    // If checkOut fails, display message
        
        // -------------------------
        // Publish dataverse and dataset
        // -------------------------
        msg("Publish dataverse and dataset");
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        
         
        // -------------------------
        // Replace file - BAD/warning b/c different content-type
        // -------------------------
        msg("Replace file - BAD/warning b/c different content-type");

        String pathToFileWrongCtype = "src/main/webapp/resources/images/ajax-loading.gif";
         msgt("origFilePid: " + origFilePid);
        Response replaceRespWrongCtype  = UtilIT.replaceFile(origFileId.toString(), pathToFileWrongCtype, apiToken);
        
        msgt(replaceRespWrongCtype.prettyPrint());
        
        String errMsgCtype = BundleUtil.getStringFromBundle("file.addreplace.error.replace.new_file_has_different_content_type", Arrays.asList("Plain Text", "GIF Image"));


        replaceRespWrongCtype.prettyPrint();
        replaceRespWrongCtype.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("status", equalTo(ApiConstants.STATUS_ERROR))
                .body("message", equalTo(errMsgCtype));
                //.body("data.rootDataFileId", equalTo(origFileId))    
        
        // -------------------------
        // Replace file
        // -------------------------
        msg("Replace file - 1st time");
        String pathToFile2 = "scripts/search/data/replace_test/004.txt";
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", "My Text File")
                .add("directoryLabel", "data/subdir1")
                .add("categories", Json.createArrayBuilder()
                        .add("Data")
                );
        
        /*
         * ToDo: When the dataset is still locked, the replaceFile call below returns an
         * 'OK' status with an empty 'data' array The sleepForLock avoids that so this
         * test tests the normal replace functionality directly, but a new test to check
         * that, when the dataset is locked, the call fails instead of returning OK
         * would be useful (along with making the replace call do that)
         */
        /*
         * ToDo: make sleep time shorter for this? Add sleepForLock before subsequent
         * calls as well? (Or is it only needed here because it is still locked from the
         * publish call above?)
         */

        UtilIT.sleepForLock(datasetId, null, apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION);
        Response replaceResp = UtilIT.replaceFile(origFileId.toString(), pathToFile2, json.build(), apiToken);
        
        msgt(replaceResp.prettyPrint());
        
        replaceResp.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsg2))
                .body("data.files[0].label", equalTo("004.txt"))
                .body("data.files[0].dataFile.contentType", startsWith("text/plain"))
                .body("data.files[0].description", equalTo("My Text File"))
                .body("data.files[0].directoryLabel", equalTo("data/subdir1"))
                .body("data.files[0].categories[0]", equalTo("Data"))
                //.body("data.rootDataFileId", equalTo(origFileId))              
                .statusCode(OK.getStatusCode());

        long rootDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.rootDataFileId");
        long previousDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.previousDataFileId");
        Long newDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.id");
        
        assertEquals(origFileId.longValue(), previousDataFileId);
        assertEquals(rootDataFileId, previousDataFileId);

        
        // -------------------------
        // Publish dataset (again)
        // -------------------------
        msg("Publish dataset (again)");
        publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
          
        
        // -------------------------
        // Replace file (again)
        // -------------------------
        msg("Replace file (again)");
        String pathToFile3 = "scripts/search/data/replace_test/005.txt";
        JsonObjectBuilder json2 = Json.createObjectBuilder();
        Response replaceResp2 = UtilIT.replaceFile(newDataFileId.toString(), pathToFile3, json2.build(), apiToken);
        
        msgt("2nd replace: " + replaceResp2.prettyPrint());
        
        replaceResp2.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsg2))
                .statusCode(OK.getStatusCode())
                .body("status", equalTo(ApiConstants.STATUS_OK))
                .body("data.files[0].label", equalTo("005.txt"))
                // yes, replacing a file blanks out the description (and categories)
                .body("data.files[0].description", equalTo(""))
                ;

        Long rootDataFileId2 = JsonPath.from(replaceResp2.body().asString()).getLong("data.files[0].dataFile.rootDataFileId");
        Long previousDataFileId2 = JsonPath.from(replaceResp2.body().asString()).getLong("data.files[0].dataFile.previousDataFileId");
        
        msgt("newDataFileId: " + newDataFileId);
        msgt("previousDataFileId2: " + previousDataFileId2);
        msgt("rootDataFileId2: " + rootDataFileId2);
        
        assertEquals(newDataFileId.longValue(), previousDataFileId2.longValue());
        assertEquals(rootDataFileId2.longValue(), origFileId.longValue());
        
    }

    @Test
    public void test_006_ReplaceFileGoodTabular() throws InterruptedException {
        msgt("test_006_ReplaceFileGoodTabular");

        // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        // -------------------------
        // Add initial file
        // -------------------------
        msg("Add initial file");
        String pathToFile = "scripts/search/data/tabular/50by1000.dta";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        // As of 9d319bd on develop, we were seeing a 500 error when we pretty print the output and
        // "IllegalArgumentException: Cannot lock a dataset for a null user" in server.log
        addResponse.prettyPrint();

        addResponse.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsgAdd))
                .body("data.files[0].dataFile.contentType", equalTo("application/x-stata"))
                .body("data.files[0].label", equalTo("50by1000.dta"))
                .statusCode(OK.getStatusCode());

        Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

        msg("Orig file id: " + origFileId);
        assertNotNull(origFileId);    // If checkOut fails, display message

        // -------------------------
        // Publish dataverse and dataset
        // -------------------------
        msg("Publish dataverse and dataset");
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        // give file time to ingest
       // sleep(10000);
       assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile);

        Response ddi = UtilIT.getFileMetadata(origFileId.toString(), "ddi", apiToken);
//        ddi.prettyPrint();
        ddi.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("50by1000.tab"))
                .body("codeBook.dataDscr.var[0].@name", equalTo("var1"))
                // Yes, it's odd that we go from "var1" to "var3" to "var2" to "var5"
                .body("codeBook.dataDscr.var[1].@name", equalTo("var3"))
                .body("codeBook.dataDscr.var[2].@name", equalTo("var2"))
                .body("codeBook.dataDscr.var[3].@name", equalTo("var5"));
        String varWithVfirst = XmlPath.from(ddi.asString()).getString("codeBook.dataDscr.var[0].@ID");
        String varWithVsecond = XmlPath.from(ddi.asString()).getString("codeBook.dataDscr.var[1].@ID");
        String variables = String.join(",", varWithVfirst, varWithVsecond).replaceAll("v", "");
        Response subset = UtilIT.subset(origFileId.toString(), variables, apiToken);
//        subset.prettyPrint();
        subset.then().assertThat()
                .statusCode(OK.getStatusCode());
        String firstLine = subset.asString().split("\n")[0];
        // We only get two variables rather than all of them. Yes it's weird that it's var3 instead of var2.
        assertEquals("var1\tvar3", firstLine);

        Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.prettyPrint();
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        // -------------------------
        // Replace file
        // -------------------------
        msg("Replace file - 1st time");
        String pathToFile2 = "scripts/search/data/tabular/120745.dta";
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("forceReplace", true)
                .add("description", "tiny Stata file")
                .add("categories", Json.createArrayBuilder()
                        .add("Data")
                )
                .add("dataFileTags", Json.createArrayBuilder()
                        .add("Survey")
                );
        Response replaceResp = UtilIT.replaceFile(origFileId.toString(), pathToFile2, json.build(), apiToken);

        msgt(replaceResp.prettyPrint());

        boolean impossibleToSetTabularTagsViaApi = true;
        if (impossibleToSetTabularTagsViaApi) {
            // previously we were getting this: You cannot add data file tags to a non-tabular file.
            System.out.println("Skipping attempt to verify that tabular tags can be set. Getting this error: Warning! The original and replacement file have different content types.  Original file is \\\"Tab-Delimited\\\" while replacement file is \\\"Stata Binary\\\"");
            return;
        }

        String successMsg2 = BundleUtil.getStringFromBundle("file.addreplace.success.replace");

        replaceResp.then().assertThat()
                .body("message", equalTo(successMsg2))
                .body("data.files[0].label", equalTo("120745.dta"))
                .body("data.files[0].description", equalTo("tiny Stata file"))
                .body("data.files[0].categories[0]", equalTo("Data"))
                .statusCode(OK.getStatusCode());

        long rootDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.rootDataFileId");
        long previousDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.previousDataFileId");
        long newDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.id");

        assertEquals(origFileId.longValue(), previousDataFileId);
        assertEquals(rootDataFileId, previousDataFileId);

    }

    //This test first tests forceReplace, and after that ensures that updates only work on the latest file
    @Test
    public void testForceReplaceAndUpdate() {
        msgt("testForceReplace");

        // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        // -------------------------
        // Add initial file
        // -------------------------
        msg("Add initial file");
        String pathToFile = "src/main/webapp/resources/images/cc0.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = BundleUtil.getStringFromBundle("file.addreplace.success.add");

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("cc0.png"))
                .statusCode(OK.getStatusCode());

        Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

        msg("Orig file id: " + origFileId);
        assertNotNull(origFileId);    // If checkOut fails, display message

        // -------------------------
        // Publish dataverse and dataset
        // -------------------------
        msg("Publish dataverse and dataset");
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.prettyPrint();
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        // -------------------------
        // Replace file
        // -------------------------
        msg("Replace file - 1st time");
        String pathToFile2 = "scripts/search/data/replace_test/growing_file/2016-01/data.tsv";
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("forceReplace", true)
                .add("description", "not an image")
                .add("categories", Json.createArrayBuilder()
                        .add("Data")
                );
        
        UtilIT.sleepForLock(datasetId, null, apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION);
        
        Response replaceResp = UtilIT.replaceFile(origFileId.toString(), pathToFile2, json.build(), apiToken);

        replaceResp.prettyPrint();

        replaceResp.then().assertThat()
                .body("data.files[0].label", equalTo("data.tsv"))
                .body("data.files[0].description", equalTo("not an image"))
                .body("data.files[0].categories[0]", equalTo("Data"))
                .statusCode(OK.getStatusCode());
        
        Long newDfId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.id");
        System.out.print("newDfId: " + newDfId);
        //Adding an additional fileMetadata update tests after this to ensure updating replaced files works
        msg("Update file metadata for old file, will error");
        String updateDescription = "New description.";
        String updateCategory = "New category";
        //"junk" passed below is to test that it is discarded
        String updateJsonString = "{\"description\":\""+updateDescription+"\",\"categories\":[\""+updateCategory+"\"],\"forceReplace\":false ,\"junk\":\"junk\"}";
        Response updateMetadataFailResponse = UtilIT.updateFileMetadata(origFileId.toString(), updateJsonString, apiToken);
        updateMetadataFailResponse.prettyPrint();
        updateMetadataFailResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        UtilIT.sleepForLock(datasetId, null, apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION);

        //Adding an additional fileMetadata update tests after this to ensure updating replaced files works
        msg("Update file metadata for new file");
        //"junk" passed below is to test that it is discarded
        System.out.print("params: " +  String.valueOf(newDfId) + " " + updateJsonString + " " + apiToken);
        Response updateMetadataResponse = UtilIT.updateFileMetadata(String.valueOf(newDfId), updateJsonString, apiToken);
        updateMetadataResponse.prettyPrint();
        updateMetadataResponse.then().assertThat().statusCode(OK.getStatusCode());
        //String updateMetadataResponseString = updateMetadataResponse.body().asString();
        Response getUpdatedMetadataResponse = UtilIT.getDataFileMetadataDraft(newDfId, apiToken);
        String getUpMetadataResponseString = getUpdatedMetadataResponse.body().asString();
        msg("Draft (should be updated):");
        msg(getUpMetadataResponseString);
        assertEquals(updateDescription, JsonPath.from(getUpMetadataResponseString).getString("description"));
        assertEquals(updateCategory, JsonPath.from(getUpMetadataResponseString).getString("categories[0]"));
        assertNull(JsonPath.from(getUpMetadataResponseString).getString("provFreeform")); //unupdated fields are not persisted
        
        //what if we delete? Should get bad request since file is not part of current version

        publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.prettyPrint();
        assertEquals(OK.getStatusCode(), publishDatasetResp.getStatusCode()); 
        
        Response deleteFile = UtilIT.deleteFile(newDfId.intValue(), apiToken);
        deleteFile.prettyPrint();
        assertEquals(NO_CONTENT.getStatusCode(), deleteFile.getStatusCode()); 
        publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.prettyPrint();
        assertEquals(OK.getStatusCode(), publishDatasetResp.getStatusCode()); 
        msg("Update file metadata for deleted file, will error");
        String delDescription = "Deleted description.";
        String deletedCategory = "Deleted category";
        //"junk" passed below is to test that it is discarded
        String deletedJsonString = "{\"description\":\""+delDescription+"\",\"categories\":[\""+deletedCategory+"\"],\"forceReplace\":false ,\"junk\":\"junk\"}";
        Response updateMetadataFailResponseDeleted = UtilIT.updateFileMetadata(newDfId.toString(), deletedJsonString, apiToken);
        updateMetadataFailResponseDeleted.prettyPrint();
        assertEquals(BAD_REQUEST.getStatusCode(), updateMetadataFailResponseDeleted.getStatusCode()); 
        


    }
    
    @Test
    public void test_007_ReplaceFileUnpublishedAndBadIds() {
        msgt("test_007_ReplaceFileBadIds");

        // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        // -------------------------
        // Add initial file
        // -------------------------
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = BundleUtil.getStringFromBundle("file.addreplace.success.add");
      
        addResponse.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsgAdd))
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());
        
        
        Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

        msg("Orig file id: " + origFileId);
        assertNotNull(origFileId);    // If checkOut fails, display message
        
        // -------------------------
        // Publish dataverse
        // -------------------------
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        
        // -------------------------
        // Replace file in unpublished dataset -- e.g. file not published
        // -------------------------
        String pathToFile2 = "src/main/webapp/resources/images/cc0.png";
        Response replaceResp = UtilIT.replaceFile(origFileId.toString(), pathToFile2, apiToken);

        replaceResp.then().assertThat()
        .body("data.files[0].dataFile.contentType", equalTo("image/png"))
        .body("data.files[0].label", equalTo("cc0.png"))
        .statusCode(OK.getStatusCode());
        
        // -------------------------
        // Publish dataset
        // -------------------------
        Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        
         
        // -------------------------
        // Replace file with non-existent Id
        // -------------------------
        pathToFile2 = "src/main/webapp/resources/images/cc0.png";
        Long fakeFileId = origFileId+10;
        Response replaceResp2 = UtilIT.replaceFile(fakeFileId.toString(), pathToFile2, apiToken);

        msgt("non-existent id: " + replaceResp.prettyPrint());

        replaceResp2.then().assertThat()
                // TODO: Some day, change this from BAD_REQUEST to NOT_FOUND and expect the standard error message.
               .statusCode(BAD_REQUEST.getStatusCode())
               .body("status", equalTo(ApiConstants.STATUS_ERROR))
               .body("message", Matchers.equalTo(BundleUtil.getStringFromBundle("file.addreplace.error.existing_file_to_replace_not_found_by_id", Arrays.asList(fakeFileId + ""))))
               ;

        
    }
    
    
    @Test
    public void test_008_ReplaceFileAlreadyDeleted() {
        msgt("test_008_ReplaceFileAlreadyDeleted");

        // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        // -------------------------
        // Add initial file
        // -------------------------
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = BundleUtil.getStringFromBundle("file.addreplace.success.add");
      
        addResponse.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsgAdd))
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());
        
        
        Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

        msg("Orig file id: " + origFileId);
        assertNotNull(origFileId);    // If checkOut fails, display message
        
        // -------------------------
        // Publish dataverse
        // -------------------------
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

                
        // -------------------------
        // Publish dataset
        // -------------------------
        Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
                
        // -------------------------
        // Delete file
        // -------------------------
        Response deleteFileResp = UtilIT.deleteFile(origFileId.intValue(), apiToken);
        deleteFileResp.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());
        // -------------------------
        // Re-Publish dataset
        // -------------------------
        publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        
        // -------------------------
        // Replace file in unpublished dataset -- e.g. file not published
        // -------------------------
        String pathToFile2 = "src/main/webapp/resources/images/cc0.png";
        Response replaceResp = UtilIT.replaceFile(origFileId.toString(), pathToFile2, apiToken);

        String errMsgDeleted = BundleUtil.getStringFromBundle("file.addreplace.error.existing_file_not_in_latest_published_version");
        
        msgt("replace resp: " + replaceResp.prettyPrint());
        
        replaceResp.then().assertThat()
               .statusCode(BAD_REQUEST.getStatusCode())
               .body("status", equalTo(ApiConstants.STATUS_ERROR))
               .body("message", Matchers.startsWith(errMsgDeleted))
               ;       
        
    }

    @Test
    public void testReplaceFileBadJson() {
        msgt("test_008_ReplaceFileAlreadyDeleted");

        // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        // -------------------------
        // Add initial file
        // -------------------------
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = BundleUtil.getStringFromBundle("file.addreplace.success.add");

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());

        Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

        msg("Orig file id: " + origFileId);
        assertNotNull(origFileId);    // If checkOut fails, display message

        // -------------------------
        // Publish dataverse
        // -------------------------
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        // -------------------------
        // Publish dataset
        // -------------------------
        Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        String pathToFile2 = "src/main/webapp/resources/images/cc0.png";
        String jsonAsString = "notJson";
        Response replaceResp = UtilIT.replaceFile(origFileId.toString(), pathToFile2, jsonAsString, apiToken);

        msgt("replace resp: " + replaceResp.prettyPrint());
        String parseError = BundleUtil.getStringFromBundle("file.addreplace.error.parsing");
        replaceResp.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("status", equalTo(ApiConstants.STATUS_ERROR))
                .body("message", equalTo(parseError));

    }

    @Test
    public void testAddTinyFile() {
        msgt("testAddTinyFile");

        // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        // -------------------------
        // Add initial file
        // -------------------------
        msg("Add initial file");
        String pathToFile = "scripts/search/data/tabular/1char";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = BundleUtil.getStringFromBundle("file.addreplace.success.add");

        addResponse.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsgAdd))
                .body("data.files[0].dataFile.contentType", equalTo("text/plain; charset=US-ASCII"))
                .body("data.files[0].label", equalTo("1char"))
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testRestrictFile() {

        //get publicInstall setting so we can change it back
        Response publicInstallResponse = UtilIT.getSetting(SettingsServiceBean.Key.PublicInstall);
        //TODO: fix this its a little hacky
        String publicInstall = String.valueOf(publicInstallResponse.getBody().asString().contains("true"));

        // make sure this is not a public installation
        UtilIT.setSetting(SettingsServiceBean.Key.PublicInstall, "false");

        // Set restrict to true
        boolean restrict = true;
        // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        // -------------------------
        // Add initial file
        // -------------------------
        System.out.println("Add initial file");
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = BundleUtil.getStringFromBundle("file.addreplace.success.add");

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());

        Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");
        String origFilePid = JsonPath.from(addResponse.body().asString()).getString("data.files[0].dataFile.persistentId");

        System.out.println("Orig file id: " + origFileId);
        assertNotNull(origFileId);    // If checkOut fails, display message

        //restrict file good
        Response restrictResponse = UtilIT.restrictFile(origFileId.toString(), restrict, apiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat()
                .body("data.message", equalTo("File dataverseproject.png restricted."))
                .statusCode(OK.getStatusCode());

        //restrict already restricted file bad
        Response restrictResponseBad = UtilIT.restrictFile(origFileId.toString(), restrict, apiToken);
        restrictResponseBad.prettyPrint();
        restrictResponseBad.then().assertThat()
                .body("message", equalTo("Problem trying to update restriction status on dataverseproject.png: File dataverseproject.png is already restricted"))
                .statusCode(BAD_REQUEST.getStatusCode());

        //unrestrict file using json with missing "restrict"
        String restrictJson = "{}";
        Response unrestrictResponse = UtilIT.restrictFile(origFileId.toString(), restrictJson, apiToken);
        unrestrictResponse.prettyPrint();
        unrestrictResponse.then().assertThat()
                .body("message", equalTo("Error parsing Json: 'restrict' is required."))
                .statusCode(BAD_REQUEST.getStatusCode());

        //unrestrict file using json
        restrictJson = "{\"restrict\":false}";
        unrestrictResponse = UtilIT.restrictFile(origFileId.toString(), restrictJson, apiToken);
        unrestrictResponse.prettyPrint();
        unrestrictResponse.then().assertThat()
                .body("data.message", equalTo("File dataverseproject.png unrestricted."))
                .statusCode(OK.getStatusCode());

        //restrict file using json with enableAccessRequest false and missing TOA
        restrictJson = "{\"restrict\":true, \"enableAccessRequest\":false}";
        restrictResponse = UtilIT.restrictFile(origFileId.toString(), restrictJson, apiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat()
                .body("message", equalTo(BundleUtil.getStringFromBundle("dataset.message.toua.invalid")))
                .statusCode(CONFLICT.getStatusCode());
        //restrict file using json
        restrictJson = "{\"restrict\":true, \"enableAccessRequest\":false, \"termsOfAccess\":\"Testing terms of access\"}";
        restrictResponse = UtilIT.restrictFile(origFileId.toString(), restrictJson, apiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat()
                .body("data.message", equalTo("File dataverseproject.png restricted. Access Request is disabled. Terms of Access for restricted files: Testing terms of access"))
                .statusCode(OK.getStatusCode());

        //reset public install
        UtilIT.setSetting(SettingsServiceBean.Key.PublicInstall, publicInstall);

    }
    
    @Test
    public void testRestrictAddedFile() {
        msgt("testRestrictAddedFile");
        
        //get publicInstall setting so we can change it back
        Response publicInstallResponse = UtilIT.getSetting(SettingsServiceBean.Key.PublicInstall);
        String publicInstall = String.valueOf(publicInstallResponse.getBody().asString().contains("true"));
        
        // make sure this is not a public installation
        UtilIT.setSetting(SettingsServiceBean.Key.PublicInstall, "false");
        
        // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);
        
        // -------------------------
        // Add initial file
        // -------------------------
        msg("Add initial file");
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";

        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", "my description")
                .add("categories", Json.createArrayBuilder()
                    .add("Data")
                    )
                .add("restrict", "true");

        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, json.build(), apiToken);

        //addResponse.prettyPrint();
        msgt("Here it is: " + addResponse.prettyPrint());
        String successMsg = BundleUtil.getStringFromBundle("file.addreplace.success.add");
        
        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode()); 

        //reset public install
        UtilIT.setSetting(SettingsServiceBean.Key.PublicInstall, publicInstall);
        
    }

    @Test
    public void testAccessFacet() {
        msgt("testRestrictFile");

        UtilIT.setSetting(SettingsServiceBean.Key.PublicInstall, "true");

        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        // -------------------------
        // Add initial file
        // -------------------------
        msg("Add initial file");
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        
        // Wait a little while for the index to pick up the file, otherwise timing issue with searching for it.
        UtilIT.sleepForReindex(datasetId.toString(), apiToken, 4);

        String successMsgAdd = BundleUtil.getStringFromBundle("file.addreplace.success.add");

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());

        long fileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

        Response searchShouldFindBecauseAuthorApiTokenSupplied = UtilIT.search("id:datafile_" + fileId + "_draft", apiToken);
        searchShouldFindBecauseAuthorApiTokenSupplied.prettyPrint();
        searchShouldFindBecauseAuthorApiTokenSupplied.then().assertThat()
                .body("data.total_count", equalTo(1))
                .statusCode(OK.getStatusCode());

        Response searchResponse = UtilIT.searchAndShowFacets("id:datafile_" + fileId + "_draft", apiToken);
        searchResponse.prettyPrint();
        searchResponse.then().assertThat()
                // Now we can search unpublished data. Just for testing!                
                // FIXME - SEK (9/20/17) the checksum type test was failing previously - commenting out for now 
                .body("data.total_count", equalTo(1))
                .body("data.items[0].name", equalTo("dataverseproject.png"))
 //               .body("data.items[0].checksum.type", equalTo("SHA-1"))
                .body("data.facets", CoreMatchers.not(equalTo(null)))
                // No "fileAccess" facet because :PublicInstall is set to true.
                .body("data.facets[0].publicationStatus", CoreMatchers.not(equalTo(null)))
                .statusCode(OK.getStatusCode());

        //reset public install
        UtilIT.setSetting(SettingsServiceBean.Key.PublicInstall, "false");

    }

    @Test
    public void test_AddFileBadUploadFormat() {
        /*
        SEK 3/26/2018 removing test for now
        having the upload method set to rsync was causing failure of create dataset 
        see CreateDatasetCommand around line 242 - test for rsyncSupportEnabled
        Per Phil this test should only be run where DCM is present
        */
        boolean runTest = false;
        if (runTest) {
            Response setUploadMethods = UtilIT.setSetting(SettingsServiceBean.Key.UploadMethods, SystemConfig.FileUploadMethods.RSYNC.toString());

            msgt("test_AddFileBadUploadFormat");
            // Create user
            String apiToken = createUserGetToken();

            // Create Dataverse
            String dataverseAlias = createDataverseGetAlias(apiToken);

            // Create Dataset
            Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

            String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
            Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
            //msgt("Here it is: " + addResponse.prettyPrint());

            //Trying to upload with rsync should fail
            addResponse.then().assertThat()
                    .statusCode(METHOD_NOT_ALLOWED.getStatusCode());

            Response removeUploadMethods = UtilIT.deleteSetting(SettingsServiceBean.Key.UploadMethods);

        }

    }
    
    @Test
    public void testUningestFileViaApi() throws InterruptedException {
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

        // -------------------------
        // Add initial file
        // -------------------------
        String pathToFile = "scripts/search/data/tabular/50by1000.dta";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        addResponse.prettyPrint();

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("application/x-stata"))
                .body("data.files[0].label", equalTo("50by1000.dta"))
                .statusCode(OK.getStatusCode());

        long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");
        assertNotNull(origFileId);    // If checkOut fails, display message
       // sleep(10000);
        
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile);
        Response uningestFileResponse = UtilIT.uningestFile(origFileId, apiToken);
        assertEquals(200, uningestFileResponse.getStatusCode());       
    }
    
    @Test
    public void testFileMetaDataGetUpdateRoundTrip() throws InterruptedException {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(OK.getStatusCode(), createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);   

        assertEquals(OK.getStatusCode(), makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        
        // Add initial file
        String pathToFile = "scripts/search/data/dv-birds1.tsv";
        String description = "A description.";
        String category = "A category";
        String provFreeForm = "provenance is great";
        String label = "acoollabel.tab";
        String jsonString = "{\"description\":\""+description+"\",\"label\":\""+label+"\",\"provFreeForm\":\""+provFreeForm+"\",\"categories\":[{\"name\":\""+category+"\"}],\"forceReplace\":false}";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, jsonString, apiToken);
        Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");
        
        //sleep(2000); //ensure tsv is consumed
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile);
        msg("Publish dataverse and dataset");
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        //msg(publishDatasetResp.body().asString());
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        //Confirm metadata
        Response getMetadataResponse = UtilIT.getDataFileMetadata(origFileId, apiToken);
        String metadataResponseString = getMetadataResponse.body().asString();
        msg(metadataResponseString);
        assertEquals(OK.getStatusCode(), getMetadataResponse.getStatusCode());  
        assertEquals(description, JsonPath.from(metadataResponseString).getString("description"));
        assertEquals(label, JsonPath.from(metadataResponseString).getString("label"));
        assertEquals(provFreeForm, JsonPath.from(metadataResponseString).getString("provFreeForm"));
        assertEquals(category, JsonPath.from(metadataResponseString).getString("categories[0]"));
        assertNull(JsonPath.from(metadataResponseString).getString("dataFileTags"));
        
        //Update fileMetadata and get to confirm again
        msg("Update file metadata");
        String updateDescription = "New description.";
        String updateCategory = "New category";
        String updateDataFileTag = "Survey";
        String updateLabel = "newName.tab";
        //"junk" passed below is to test that it is discarded
        String updateJsonString = "{\"description\":\""+updateDescription+"\",\"label\":\""+updateLabel+"\",\"categories\":[\""+updateCategory+"\"],\"dataFileTags\":[\""+updateDataFileTag+"\"],\"forceReplace\":false ,\"junk\":\"junk\"}";
        Response updateMetadataResponse = UtilIT.updateFileMetadata(origFileId.toString(), updateJsonString, apiToken);
        assertEquals(OK.getStatusCode(), updateMetadataResponse.getStatusCode());  
        //String updateMetadataResponseString = updateMetadataResponse.body().asString();
        Response getUpdatedMetadataResponse = UtilIT.getDataFileMetadataDraft(origFileId, apiToken);
        String getUpMetadataResponseString = getUpdatedMetadataResponse.body().asString();
        msg("Draft (should be updated):");
        msg(getUpMetadataResponseString);
        assertEquals(updateDescription, JsonPath.from(getUpMetadataResponseString).getString("description"));
        assertEquals(updateLabel, JsonPath.from(getUpMetadataResponseString).getString("label"));
        assertEquals(updateCategory, JsonPath.from(getUpMetadataResponseString).getString("categories[0]"));
        assertNull(JsonPath.from(getUpMetadataResponseString).getString("provFreeform")); //unupdated fields are not persisted
        assertEquals(updateDataFileTag, JsonPath.from(getUpMetadataResponseString).getString("dataFileTags[0]"));

//We haven't published so the non-draft call should still give the pre-edit metadata
        Response getOldMetadataResponse = UtilIT.getDataFileMetadata(origFileId, apiToken);
        String getOldMetadataResponseString = getOldMetadataResponse.body().asString();
        msg("Old Published (shouldn't be updated):");
        msg(getOldMetadataResponseString);
        assertEquals(label, JsonPath.from(getOldMetadataResponseString).getString("label"));
        assertEquals(description, JsonPath.from(getOldMetadataResponseString).getString("description"));
        assertEquals(category, JsonPath.from(getOldMetadataResponseString).getString("categories[0]"));
        assertEquals(updateDataFileTag, JsonPath.from(getOldMetadataResponseString).getString("dataFileTags[0]")); //tags are not versioned, so the old version will have the tags
        
        //extra test for invalid data for tags returning a pretty error
        String updateInvalidJsonString = "{\"dataFileTags\":false}";
        Response updateInvalidMetadataResponse = UtilIT.updateFileMetadata(origFileId.toString(), updateInvalidJsonString, apiToken);
        assertEquals(BAD_REQUEST.getStatusCode(), updateInvalidMetadataResponse.getStatusCode());  
        
        //adding a publish here to test the error seen in #11208
        UtilIT.sleepForLock(datasetId, null, apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION);
        publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.prettyPrint();
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        String updateDescription2 = "update after publish";
        String updateJsonString2 = "{\"description\":\"" + updateDescription2 + "\",\"label\":\"" + updateLabel + "\",\"categories\":[\"" + updateCategory + "\"],\"dataFileTags\":[\"" + updateDataFileTag + "\"],\"forceReplace\":false ,\"junk\":\"junk\"}";
        Response updateMetadataResponse2 = UtilIT.updateFileMetadata(origFileId.toString(), updateJsonString2, apiToken);

        updateMetadataResponse2.prettyPrint();

        updateMetadataResponse2.then().assertThat()
                .statusCode(OK.getStatusCode());
        getUpdatedMetadataResponse = UtilIT.getDataFileMetadataDraft(origFileId, apiToken);
        String getUpdateMetadataResponseString = getUpdatedMetadataResponse.body().asString();
        msg(getUpdateMetadataResponseString);
        assertEquals(updateDescription2, JsonPath.from(getUpdateMetadataResponseString).getString("description"));

    }
    
    // This method tests the "/api/dataverses/.../storagesize API - but 
    // it's included in this class since it deals with files. 
    @Test
    public void testDataSizeInDataverse() throws InterruptedException {

        // Create user
        String apiToken = createUserGetToken();
        System.out.println("api token: "+apiToken);
        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);
        System.out.println("dataverseAlias: "+dataverseAlias);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        // Add first file:
        System.out.println("Add initial file");
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());
        
        
        // Add another file (ingestable):
        pathToFile = "scripts/search/data/tabular/50by1000.dta";
        addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        addResponse.prettyPrint();

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("application/x-stata"))
                .body("data.files[0].label", equalTo("50by1000.dta"))
                .statusCode(OK.getStatusCode());
        
        // wait for it to ingest... 
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, 5), "Failed test if Ingest Lock exceeds max duration " + pathToFile);
     //   sleep(10000);
     
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        String apiTokenRando = createUserGetToken();
        
        Response datasetStorageSizeResponseDraft = UtilIT.findDatasetDownloadSize(datasetId.toString(), DS_VERSION_DRAFT, apiTokenRando);
        datasetStorageSizeResponseDraft.prettyPrint();
        assertEquals(UNAUTHORIZED.getStatusCode(), datasetStorageSizeResponseDraft.getStatusCode());  
        Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        //msg(publishDatasetResp.body().asString());
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        // This is the magic number - the number of bytes in the 2 files uploaded
        // above, plus the size of the tab-delimited file generated by the ingest
        // of the stata file:
        
        int magicSizeNumber = 176134; 
        String magicControlString = MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.datasize"), magicSizeNumber);
        
        Response dvStorageSizeResponse = UtilIT.findDataverseStorageSize(dataverseAlias, apiToken);
        dvStorageSizeResponse.prettyPrint();
                
        assertEquals(magicControlString, JsonPath.from(dvStorageSizeResponse.body().asString()).getString("data.message"));
        
        magicControlString = MessageFormat.format(BundleUtil.getStringFromBundle("datasets.api.datasize.storage"), magicSizeNumber);
        
        //no perms

        Response datasetStorageSizeResponse = UtilIT.findDatasetStorageSize(datasetId.toString(), apiTokenRando);
        datasetStorageSizeResponse.prettyPrint();
        assertEquals(UNAUTHORIZED.getStatusCode(), datasetStorageSizeResponse.getStatusCode());  
                
        //has perms
        datasetStorageSizeResponse = UtilIT.findDatasetStorageSize(datasetId.toString(), apiToken);
        datasetStorageSizeResponse.prettyPrint();

        assertEquals(magicControlString, JsonPath.from(datasetStorageSizeResponse.body().asString()).getString("data.message"));
        
        magicControlString = MessageFormat.format(BundleUtil.getStringFromBundle("datasets.api.datasize.download"), magicSizeNumber);
        
        Response datasetDownloadSizeResponse = UtilIT.findDatasetDownloadSize(datasetId.toString());
        datasetDownloadSizeResponse.prettyPrint();
                
        assertEquals(magicControlString, JsonPath.from(datasetDownloadSizeResponse.body().asString()).getString("data.message"));
        
    }

    @Test
    public void GetFileVersionDifferences() {
        // Create superuser and regular user
        Response createUser = UtilIT.createRandomUser();
        String superUserUsername = UtilIT.getUsernameFromResponse(createUser);
        String superUserApiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.makeSuperUser(superUserUsername);
        createUser = UtilIT.createRandomUser();
        String regularUsername = UtilIT.getUsernameFromResponse(createUser);
        String regularApiToken = UtilIT.getApiTokenFromResponse(createUser);

        // Create dataverse and dataset. Upload 1 file
        String dataverseAlias = createDataverseGetAlias(superUserApiToken);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, superUserApiToken);
        Integer datasetId = createDatasetGetId(dataverseAlias, superUserApiToken);
        String pathToFile = "scripts/search/data/binary/trees.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, superUserApiToken);
        addResponse.prettyPrint();
        String dataFileId = addResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");

        // Superuser can see the draft version
        Response getFileDataResponse = UtilIT.getFileVersionDifferences(dataFileId, superUserApiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data[0].datasetVersion", equalTo("DRAFT"))
                .body("data[0].fileDifferenceSummary.file", equalTo("Added"))
                .statusCode(OK.getStatusCode());

        // Regular user can not see the draft version
        getFileDataResponse = UtilIT.getFileVersionDifferences(dataFileId, regularApiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("status", equalTo("ERROR"))
                .body("message", containsString("is not permitted to perform requested action."))
                .statusCode(UNAUTHORIZED.getStatusCode());

        // Publish the dataset with 1 file
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", superUserApiToken);

        // Regular user can see latest version now
        getFileDataResponse = UtilIT.getFileVersionDifferences(dataFileId, regularApiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data[0].datasetVersion", equalTo("1.0"))
                .body("data[0].fileDifferenceSummary.file", equalTo("Added"))
                .statusCode(OK.getStatusCode());

        // Add another file to create a new draft
        pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, superUserApiToken);
        addResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");

        // Regular user can only see the published version
        getFileDataResponse = UtilIT.getFileVersionDifferences(dataFileId, regularApiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data[0].datasetVersion", equalTo("1.0"))
                .body("data[0].fileDifferenceSummary.file", equalTo("Added"))
                .statusCode(OK.getStatusCode());

        // Give permission to view the draft version
        Response assignRole = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.CURATOR.toString(),
                "@" + regularUsername, superUserApiToken);
        assertEquals(200, assignRole.getStatusCode());

        // Regular user can see the draft and released versions now
        getFileDataResponse = UtilIT.getFileVersionDifferences(dataFileId, regularApiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data[0].datasetVersion", equalTo("DRAFT"))
                .body("data[1].datasetVersion", equalTo("1.0"))
                .body("data[1].fileDifferenceSummary.file", equalTo("Added"))
                .statusCode(OK.getStatusCode());

        // test replace file
        pathToFile = "src/test/resources/images/coffeeshop.png";
        Response replaceFileResponse = UtilIT.replaceFile(dataFileId, pathToFile, superUserApiToken);
        replaceFileResponse.prettyPrint();
        replaceFileResponse.then().assertThat()
                .body("status", equalTo("OK"));
        String replacedDataFileId = replaceFileResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");
        getFileDataResponse = UtilIT.getFileVersionDifferences(replacedDataFileId, regularApiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data[0].datasetVersion", equalTo("DRAFT"))
                .body("data[0].fileDifferenceSummary.file", equalTo("Replaced"))
                .body("data[0].datafileId", equalTo(Integer.parseInt(replacedDataFileId)))
                .body("data[1].datasetVersion", equalTo("1.0"))
                .body("data[1].fileDifferenceSummary.file", equalTo("Added"))
                .body("data[1].datafileId", equalTo(Integer.parseInt(dataFileId)))
                .statusCode(OK.getStatusCode());
        getFileDataResponse = UtilIT.getFileVersionDifferences(dataFileId, regularApiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data[0].datasetVersion", equalTo("DRAFT"))
                .body("data[0].fileDifferenceSummary.file", equalTo("Replaced"))
                .body("data[0].datafileId", equalTo(Integer.parseInt(replacedDataFileId)))
                .body("data[1].datasetVersion", equalTo("1.0"))
                .body("data[1].fileDifferenceSummary.file", equalTo("Added"))
                .body("data[1].datafileId", equalTo(Integer.parseInt(dataFileId)))
                .statusCode(OK.getStatusCode());

        // The following tests cover cases where the dataset version is deaccessioned
        Response deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, "1.0", "Test reason", null, superUserApiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());
        getFileDataResponse = UtilIT.getFileVersionDifferences(replacedDataFileId, regularApiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data[1].datasetVersion", equalTo("1.0"))
                .body("data[1].fileDifferenceSummary.deaccessionedReason", equalTo("Test reason"))
                .body("data[1].fileDifferenceSummary.file", equalTo("Added"))
                .statusCode(OK.getStatusCode());
    }
    @Test
    public void testGetFileInfo() {
        Response createUser = UtilIT.createRandomUser();
        String superUserUsername = UtilIT.getUsernameFromResponse(createUser);
        String superUserApiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.makeSuperUser(superUserUsername);
        String dataverseAlias = createDataverseGetAlias(superUserApiToken);
        Integer datasetId = createDatasetGetId(dataverseAlias, superUserApiToken);

        createUser = UtilIT.createRandomUser();
        String regularUsername = UtilIT.getUsernameFromResponse(createUser);
        String regularApiToken = UtilIT.getApiTokenFromResponse(createUser);

        msg("Add a non-tabular file");
        String pathToFile = "scripts/search/data/binary/trees.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, superUserApiToken);

        // The following tests cover cases where no version ID is specified in the endpoint
        // Superuser should get to see draft file data
        String dataFileId = addResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");
        Response getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken);
        String newFileName = "trees.png";
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileName))
                .body("data.dataFile.filename", equalTo(newFileName))
                .body("data.dataFile.contentType", equalTo("image/png"))
                .body("data.dataFile.filesize", equalTo(8361))
                .statusCode(OK.getStatusCode());

        // Regular user should not get to see draft file data
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken);
        getFileDataResponse.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());

        // Publish dataverse and dataset
        Response publishDataverseResp = UtilIT.publishDataverseViaSword(dataverseAlias, superUserApiToken);
        publishDataverseResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", superUserApiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Regular user should get to see published file data
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken);
        getFileDataResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.label", equalTo(newFileName));

        // The following tests cover cases where a version ID is specified in the endpoint
        // Superuser should not get to see draft file data when no draft version exists
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, DS_VERSION_DRAFT);
        getFileDataResponse.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());

        // Regular user should get to see file data from specific version filtering by tag
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, "1.0");
        getFileDataResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.label", equalTo(newFileName));

        // Update the file metadata
        String newFileNameFirstUpdate = "trees_2.png";
        JsonObjectBuilder updateFileMetadata = Json.createObjectBuilder()
                .add("label", newFileNameFirstUpdate);
        Response updateFileMetadataResponse = UtilIT.updateFileMetadata(dataFileId, updateFileMetadata.build().toString(), superUserApiToken);
        updateFileMetadataResponse.then().statusCode(OK.getStatusCode());

        // Superuser should get to see draft file data
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, DS_VERSION_DRAFT);
        getFileDataResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Regular user should not get to see draft file data
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, DS_VERSION_DRAFT);
        getFileDataResponse.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());

        // Publish dataset once again
        publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", superUserApiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Update the file metadata once again
        String newFileNameSecondUpdate = "trees_3.png";
        updateFileMetadata = Json.createObjectBuilder()
                .add("label", newFileNameSecondUpdate);
        updateFileMetadataResponse = UtilIT.updateFileMetadata(dataFileId, updateFileMetadata.build().toString(), superUserApiToken);
        updateFileMetadataResponse.then().statusCode(OK.getStatusCode());

        // Regular user should get to see latest published file data
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, DS_VERSION_LATEST_PUBLISHED);
        getFileDataResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.label", equalTo(newFileNameFirstUpdate));

        // Regular user should get to see latest published file data if latest is requested
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, DS_VERSION_LATEST);
        getFileDataResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.label", equalTo(newFileNameFirstUpdate));

        // Superuser should get to see draft file data if latest is requested
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, DS_VERSION_LATEST);
        getFileDataResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.label", equalTo(newFileNameSecondUpdate));

        // Publish dataset once again
        publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", superUserApiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Regular user should get to see file data by specific version number
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, "2.0");
        getFileDataResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.label", equalTo(newFileNameFirstUpdate));

        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, "3.0");
        getFileDataResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.label", equalTo(newFileNameSecondUpdate));

        // The following tests cover cases where the dataset version is deaccessioned
        
        Response deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, "3.0", "Test reason", null, superUserApiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Superuser should get to see file data if the latest version is deaccessioned filtering by latest and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, DS_VERSION_LATEST, true, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameSecondUpdate))
                .statusCode(OK.getStatusCode());

        // Superuser should get to see file data if the latest version is deaccessioned filtering by latest published and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, DS_VERSION_LATEST_PUBLISHED, true, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameSecondUpdate))
                .statusCode(OK.getStatusCode());

        // Superuser should get to see version 2.0 file data if the latest version is deaccessioned filtering by latest and includeDeaccessioned is false
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, DS_VERSION_LATEST, false, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameFirstUpdate))
                .statusCode(OK.getStatusCode());

        // Superuser should get to see version 2.0 file data if the latest version is deaccessioned filtering by latest published and includeDeaccessioned is false
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, DS_VERSION_LATEST_PUBLISHED, false, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameFirstUpdate))
                .statusCode(OK.getStatusCode());

        // Superuser should get to see file data from specific deaccessioned version filtering by tag and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, "3.0", true, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameSecondUpdate))
                .statusCode(OK.getStatusCode());

        // Superuser should not get to see file data from specific deaccessioned version filtering by tag and includeDeaccessioned is false
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, "3.0", false, false);
        getFileDataResponse.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());

        // Regular user should get to see version 2.0 file data if the latest version is deaccessioned filtering by latest and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, DS_VERSION_LATEST, true, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameFirstUpdate))
                .statusCode(OK.getStatusCode());

        // Regular user should get to see version 2.0 file data if the latest version is deaccessioned filtering by latest published and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, DS_VERSION_LATEST_PUBLISHED, true, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameFirstUpdate))
                .statusCode(OK.getStatusCode());

        // Regular user should get to see version 2.0 file data if the latest version is deaccessioned filtering by latest published and includeDeaccessioned is false
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, DS_VERSION_LATEST_PUBLISHED, false, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameFirstUpdate))
                .statusCode(OK.getStatusCode());

        // Regular user should not get to see file data from specific deaccessioned version filtering by tag and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, "3.0", true, false);
        getFileDataResponse.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());

        // Regular user should not get to see file data from specific deaccessioned version filtering by tag and includeDeaccessioned is false
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, "3.0", false, false);
        getFileDataResponse.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());

        // Update the file metadata
        String newFileNameThirdUpdate = "trees_4.png";
        updateFileMetadata = Json.createObjectBuilder()
                .add("label", newFileNameThirdUpdate);
        updateFileMetadataResponse = UtilIT.updateFileMetadata(dataFileId, updateFileMetadata.build().toString(), superUserApiToken);
        updateFileMetadataResponse.then().statusCode(OK.getStatusCode());

        // Superuser should get to see draft file data if draft exists filtering by latest and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, DS_VERSION_LATEST, true, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameThirdUpdate))
                .statusCode(OK.getStatusCode());

        // Superuser should get to see latest published file data if draft exists filtering by latest published and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, superUserApiToken, DS_VERSION_LATEST_PUBLISHED, true, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameSecondUpdate))
                .statusCode(OK.getStatusCode());

        // Regular user should get to see version 2.0 file data if the latest version is deaccessioned and draft exists filtering by latest and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, DS_VERSION_LATEST, true, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameFirstUpdate))
                .statusCode(OK.getStatusCode());

        // Regular user should get to see version 2.0 file data if the latest version is deaccessioned and draft exists filtering by latest published and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, DS_VERSION_LATEST_PUBLISHED, true, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameFirstUpdate))
                .statusCode(OK.getStatusCode());

        // Publish dataset once again
        publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", superUserApiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Regular user should get to see file data if the latest version is not deaccessioned filtering by latest and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, DS_VERSION_LATEST, true, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameThirdUpdate))
                .statusCode(OK.getStatusCode());

        // Regular user should get to see file data if the latest version is not deaccessioned filtering by latest published and includeDeaccessioned is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, DS_VERSION_LATEST_PUBLISHED, true, false);
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo(newFileNameThirdUpdate))
                .statusCode(OK.getStatusCode());

        // The following tests cover cases where the user requests to include the dataset version information in the response
        // User should get to see dataset version info in the response if returnDatasetVersion is true
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, "1.0", false, true);
        getFileDataResponse.then().assertThat()
                .body("data.datasetVersion.versionState", equalTo("RELEASED"))
                .statusCode(OK.getStatusCode());

        // User should not get to see dataset version info in the response if returnDatasetVersion is false
        getFileDataResponse = UtilIT.getFileData(dataFileId, regularApiToken, "1.0", false, false);
        getFileDataResponse.then().assertThat()
                .body("data.datasetVersion", equalTo(null))
                .statusCode(OK.getStatusCode());

        // Cleanup
        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetId, superUserApiToken);
        destroyDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, superUserApiToken);
        deleteDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(superUserUsername);
        deleteUserResponse.then().assertThat().statusCode(OK.getStatusCode());

        deleteUserResponse = UtilIT.deleteUser(regularUsername);
        deleteUserResponse.then().assertThat().statusCode(OK.getStatusCode());
    }
    
    @Test 
    public void testGetFileOwners() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        String dataverseAlias = createDataverseGetAlias(apiToken);
       
        
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);

        createUser = UtilIT.createRandomUser();
        String apiTokenRegular = UtilIT.getApiTokenFromResponse(createUser);

        msg("Add a non-tabular file");
        String pathToFile = "scripts/search/data/binary/trees.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String dataFileId = addResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");
        msgt("datafile id: " + dataFileId);

        addResponse.prettyPrint();

        Response getFileDataResponse = UtilIT.getFileWithOwners(dataFileId, apiToken, true);

        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("data.label", equalTo("trees.png"))
                .body("data.dataFile.filename", equalTo("trees.png"))
                .body("data.dataFile.contentType", equalTo("image/png"))
                .body("data.dataFile.filesize", equalTo(8361))
                .statusCode(OK.getStatusCode());
        
        getFileDataResponse.then().assertThat().body("data.dataFile.isPartOf.identifier", equalTo(datasetId));
        getFileDataResponse.then().assertThat().body("data.dataFile.isPartOf.persistentIdentifier", equalTo(datasetPid));

        // -------------------------
        // Publish dataverse and dataset
        // -------------------------
        msg("Publish dataverse and dataset");
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        //regular user should get to see file data
        getFileDataResponse = UtilIT.getFileData(dataFileId, apiTokenRegular);
        getFileDataResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        //cleanup

        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetId, apiToken);
        assertEquals(200, destroyDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        assertEquals(200, deleteUserResponse.getStatusCode());

        
    }
    
    @Test
    public void testValidateDDI_issue6027() throws InterruptedException {
        msgt("testValidateDDI_issue6027");
        String apiToken = createUserGetToken();
        String dataverseAlias = createDataverseGetAlias(apiToken);
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);

        msg("Add tabular file");
        String pathToFile = "scripts/search/data/tabular/stata13-auto-withstrls.dta";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        addResponse.prettyPrint();

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("application/x-stata-13"))
                .body("data.files[0].label", equalTo("stata13-auto-withstrls.dta"))
                .statusCode(OK.getStatusCode());

        Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");
        assertNotNull(origFileId);    // If checkOut fails, display message

        // -------------------------
        // Publish dataverse and dataset
        // -------------------------
        msg("Publish dataverse and dataset");
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());

        // give file time to ingest
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile);
       // sleep(10000);

        Response ddi = UtilIT.getFileMetadata(origFileId.toString(), "ddi", apiToken);
        ddi.prettyPrint();
        ddi.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("stata13-auto-withstrls.tab"))
                .body("codeBook.dataDscr.var[0].@name", equalTo("make"));

    }
    
    /*
        A very simple test for shape file package processing. 
    */    
    @Test
    public void test_ProcessShapeFilePackage() {
        msgt("test_ProcessShapeFilePackage");
         // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);
       
        // This archive contains 4 files that constitute a valid 
        // shape file. We want to check that these files were properly 
        // recognized and re-zipped as a shape package, preserving the 
        // folder structure found in the uploaded zip. 
        String pathToFile = "scripts/search/data/shape/shapefile.zip";
        
        String suppliedDescription = "file extracted from a shape bundle";
        String extractedFolderName = "subfolder";
        String extractedShapeName = "boston_public_schools_2012_z1l.zip"; 
        String extractedShapeType = "application/zipped-shapefile";

        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", suppliedDescription);

        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, json.build(), apiToken);

        msgt("Server response: " + addResponse.prettyPrint());
      
        // We are checking the following: 
        // - that the upload succeeded;
        // - that a shape file with the name specified above has been repackaged and added
        //   to the dataset as a single file;
        // - that the mime type has been properly identified;
        // - that the description supplied via the API has been added; 
        // - that the subfolder found inside the uploaded zip file has been properly
        //   preserved in the FileMetadata. 
        // 
        // Feel free to expand the checks further - we can also verify the 
        // checksum, the size of the resulting file, add more files to the uploaded
        // zip archive etc. etc. - but this should be a good start. 
        // -- L.A. 2020/09
        addResponse.then().assertThat()
                .body("status", equalTo(ApiConstants.STATUS_OK))
                .body("data.files[0].dataFile.contentType", equalTo(extractedShapeType))
                .body("data.files[0].label", equalTo(extractedShapeName))
                .body("data.files[0].directoryLabel", equalTo(extractedFolderName))
                .body("data.files[0].description", equalTo(suppliedDescription))
                .statusCode(OK.getStatusCode());
    }
    
    /*
        First test for the new "crawlable file access" API (#7084)
    */    
    @Test
    public void test_CrawlableAccessToDatasetFiles() {
        msgt("test_test_CrawlableAccessToDatasetFiles");
         // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        String datasetId = createDatasetGetId(dataverseAlias, apiToken).toString();
        
        msgt("dataset id: "+datasetId);
       
        String testFileName = "dataverseproject.png";
        String pathToFile = "src/main/webapp/resources/images/" + testFileName;
        String description = "test file 1";
        String folderName = "subfolder";

        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", description)
                .add("directoryLabel", folderName);

        Response addResponse = UtilIT.uploadFileViaNative(datasetId, pathToFile, json.build(), apiToken);

        msgt("Server response: " + addResponse.prettyPrint());
      
        addResponse.then().assertThat()
                .body("status", equalTo(ApiConstants.STATUS_OK))
                .body("data.files[0].label", equalTo(testFileName))
                .body("data.files[0].directoryLabel", equalTo(folderName))
                .body("data.files[0].description", equalTo(description))
                .statusCode(OK.getStatusCode());
        
        String dataFileId = addResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");
        //msgt("datafile id: "+dataFileId);
        
        // TODO: (potentially?)
        // maybe upload a few more files, in more folders, 
        // and try an actual recursive crawl of a full tree - ?
                
        // Make some calls to the "/dirindex API:
        // (note that this API outputs HTML!)
        
        // Expected values in the output: 
        String expectedTitleTopFolder = "Index of folder /";
        String expectedLinkTopFolder = folderName + "/";
        String expectedLinkAhrefTopFolder = "/api/datasets/"+datasetId+"/dirindex/?version=" + DS_VERSION_DRAFT + "&folder=subfolder";
        
        String expectedTitleSubFolder = "Index of folder /" + folderName;
        String expectedLinkAhrefSubFolder = "/api/access/datafile/" + folderName + "/" + dataFileId;
        
        // ... with no folder specified: 
        // (with just the one file above, this should show one folder only - "subfolder", and no files)
        Response fileAccessResponse = UtilIT.getCrawlableFileAccess(datasetId, "", apiToken);
        fileAccessResponse.then().assertThat().statusCode(OK.getStatusCode()).contentType("text/html");

        String htmlTitle = fileAccessResponse.getBody().htmlPath().getString("html.head.title");
        assertEquals(expectedTitleTopFolder, htmlTitle);
        
        String htmlCrawlLink = fileAccessResponse.getBody().htmlPath().getString("html.body.table.tr[2].td[0]");
        //msgt("html crawl link: "+htmlCrawlLink);
        assertEquals(expectedLinkTopFolder, htmlCrawlLink);
        
        String htmlCrawlLinkAhref = fileAccessResponse.getBody().htmlPath().get("html.body.table.tr[2].td[0].a.@href").toString();
        //msgt("html crawl link href: "+htmlCrawlLinkAhref);
        assertEquals(expectedLinkAhrefTopFolder, htmlCrawlLinkAhref);

        
        // ... and with the folder name "subfolder" specified: 
        // (should result in being shown one access link to the file above, no folders)
        fileAccessResponse = UtilIT.getCrawlableFileAccess(datasetId.toString(), folderName, apiToken);
        fileAccessResponse.then().assertThat().statusCode(OK.getStatusCode()).contentType("text/html");

        htmlTitle = fileAccessResponse.getBody().htmlPath().getString("html.head.title");
        assertEquals(expectedTitleSubFolder, htmlTitle);
        
        htmlCrawlLink = fileAccessResponse.getBody().htmlPath().getString("html.body.table.tr[2].td[0]");
        //msgt("html crawl link: "+htmlCrawlLink);
        // this should be the name of the test file above:
        assertEquals(testFileName, htmlCrawlLink);
        
        htmlCrawlLinkAhref = fileAccessResponse.getBody().htmlPath().get("html.body.table.tr[2].td[0].a.@href").toString();
        //msgt("html crawl link href: "+htmlCrawlLinkAhref);
        assertEquals(expectedLinkAhrefSubFolder, htmlCrawlLinkAhref);

    }
    
    private void msg(String m){
        System.out.println(m);
    }
    private void dashes(){
        msg("----------------");
    }
    private void msgt(String m){
        dashes(); msg(m); dashes();
    }

    @Test
    public void testRange() throws IOException {

        Response createUser = UtilIT.createRandomUser();
//        createUser.prettyPrint();
        String authorUsername = UtilIT.getUsernameFromResponse(createUser);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(authorApiToken);
//        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
//        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        Path pathToTxt = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "file.txt");
        String contentOfTxt = ""
                + "first is the worst\n"
                + "second is the best\n"
                + "third is the one with the hairy chest\n";
        java.nio.file.Files.write(pathToTxt, contentOfTxt.getBytes());

        Response uploadFileTxt = UtilIT.uploadFileViaNative(datasetId.toString(), pathToTxt.toString(), authorApiToken);
//        uploadFileTxt.prettyPrint();
        uploadFileTxt.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("file.txt"));

        Integer fileIdTxt = JsonPath.from(uploadFileTxt.body().asString()).getInt("data.files[0].dataFile.id");

        // Download the whole file.
        Response downloadTxtNoArgs = UtilIT.downloadFile(fileIdTxt, null, null, null, authorApiToken);
        downloadTxtNoArgs.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body(equalTo("first is the worst\n"
                        + "second is the best\n"
                        + "third is the one with the hairy chest\n"));

        // Download the first 10 bytes.
        Response downloadTxtFirst10 = UtilIT.downloadFile(fileIdTxt, "0-9", null, null, authorApiToken);
        downloadTxtFirst10.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("first is t"));

        // Download the last 6 bytes.
        Response downloadTxtLast6 = UtilIT.downloadFile(fileIdTxt, "-6", null, null, authorApiToken);
        downloadTxtLast6.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("chest\n"));

        // Download some bytes from the middle.
        Response downloadTxtMiddle = UtilIT.downloadFile(fileIdTxt, "09-19", null, null, authorApiToken);
        downloadTxtMiddle.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("the worst\ns"));

        // Skip the first 10 bytes and download the rest.
        Response downloadTxtSkipFirst10 = UtilIT.downloadFile(fileIdTxt, "9-", null, null, authorApiToken);
        downloadTxtSkipFirst10.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("the worst\n"
                        + "second is the best\n"
                        + "third is the one with the hairy chest\n"));

        Path pathToCsv = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "data.csv");
        String contentOfCsv = ""
                + "name,pounds,species\n"
                + "Marshall,40,dog\n"
                + "Tiger,17,cat\n"
                + "Panther,21,cat\n";
        java.nio.file.Files.write(pathToCsv, contentOfCsv.getBytes());

        Response uploadFileCsv = UtilIT.uploadFileViaNative(datasetId.toString(), pathToCsv.toString(), authorApiToken);
//        uploadFileCsv.prettyPrint();
        uploadFileCsv.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("data.csv"));

        Integer fileIdCsv = JsonPath.from(uploadFileCsv.body().asString()).getInt("data.files[0].dataFile.id");

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", authorApiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToCsv);

        // Just the tabular file, not the original, no byte range. Vanilla.
        Response downloadFileNoArgs = UtilIT.downloadFile(fileIdCsv, null, null, null, authorApiToken);
        downloadFileNoArgs.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body(equalTo("name\tpounds\tspecies\n"
                        + "\"Marshall\"\t40\t\"dog\"\n"
                        + "\"Tiger\"\t17\t\"cat\"\n"
                        + "\"Panther\"\t21\t\"cat\"\n"));

        // first 10 bytes of tabular format
        Response downloadTabFirstTen = UtilIT.downloadFile(fileIdCsv, "0-9", null, null, authorApiToken);
        downloadTabFirstTen.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("name\tpound"));

        // first 30 bytes of tabular format
        Response downloadTabFirst30 = UtilIT.downloadFile(fileIdCsv, "0-29", null, null, authorApiToken);
        downloadTabFirst30.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("name\tpounds\tspecies\n"
                        + "\"Marshall\""));

        // last 16 bytes of tabular format
        Response downloadTabLast16 = UtilIT.downloadFile(fileIdCsv, "-16", null, null, authorApiToken);
        downloadTabLast16.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("nther\"\t21\t\"cat\"\n"));

        Response downloadTabMiddleBytesHeader = UtilIT.downloadFile(fileIdCsv, "1-7", null, null, authorApiToken);
        downloadTabMiddleBytesHeader.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("ame\tpou"));

        Response downloadTabMiddleBytesBody = UtilIT.downloadFile(fileIdCsv, "31-43", null, null, authorApiToken);
        downloadTabMiddleBytesBody.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("40\t\"dog\"\n"
                        + "\"Tig"));

        // Original version of tabular file (CSV in this case).
        Response downloadOrig = UtilIT.downloadFile(fileIdCsv, null, "original", null, authorApiToken);
        downloadOrig.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body(equalTo("name,pounds,species\n"
                        + "Marshall,40,dog\n"
                        + "Tiger,17,cat\n"
                        + "Panther,21,cat\n"));

        // first ten bytes
        Response downloadOrigFirstTen = UtilIT.downloadFile(fileIdCsv, "0-9", "original", null, authorApiToken);
        downloadOrigFirstTen.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("name,pound"));

        // last ten bytes
        Response downloadOrigLastTen = UtilIT.downloadFile(fileIdCsv, "-10", "original", null, authorApiToken);
        downloadOrigLastTen.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("er,21,cat\n"));

        // middle bytes
        Response downloadOrigMiddle = UtilIT.downloadFile(fileIdCsv, "29-39", "original", null, authorApiToken);
        downloadOrigMiddle.then().assertThat()
                .statusCode(PARTIAL_CONTENT.getStatusCode())
                .body(equalTo("40,dog\nTige"));

        String pathToZipWithImage = "scripts/search/data/binary/trees.zip";
        Response uploadFileZipWithImage = UtilIT.uploadFileViaNative(datasetId.toString(), pathToZipWithImage, authorApiToken);
//        uploadFileZipWithImage.prettyPrint();
        uploadFileZipWithImage.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("trees.png"));

        Integer fileIdPng = JsonPath.from(uploadFileZipWithImage.body().asString()).getInt("data.files[0].dataFile.id");

        String trueOrWidthInPixels = "true";
        Response getFileThumbnailImageA = UtilIT.getFileThumbnail(fileIdPng.toString(), trueOrWidthInPixels, authorApiToken);
        getFileThumbnailImageA.then().assertThat()
                .contentType("image/png")
                .statusCode(OK.getStatusCode());

        // Yes, you can get a range of bytes from a thumbnail.
        String imageThumbPixels = "true";
        Response downloadThumbnail = UtilIT.downloadFile(fileIdPng, "0-149", null, imageThumbPixels, authorApiToken);
//        downloadThumbnail.prettyPrint();
        downloadThumbnail.then().assertThat().statusCode(PARTIAL_CONTENT.getStatusCode());

        Response multipleRangesNotSupported = UtilIT.downloadFile(fileIdTxt, "0-9,20-29", null, null, authorApiToken);
        // "Error due to Range header: Only one range is allowed."
        multipleRangesNotSupported.prettyPrint();
        multipleRangesNotSupported.then().assertThat().statusCode(REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode());

        Response startLargerThanEndError = UtilIT.downloadFile(fileIdTxt, "20-10", null, null, authorApiToken);
        // "Error due to Range header: Start is larger than end or size of file."
        startLargerThanEndError.prettyPrint();
        startLargerThanEndError.then().assertThat().statusCode(REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode());

        Response rangeBeyondFileSize = UtilIT.downloadFile(fileIdTxt, "88888-99999", null, null, authorApiToken);
        // "Error due to Range header: Start is larger than end or size of file."
        rangeBeyondFileSize.prettyPrint();
        rangeBeyondFileSize.then().assertThat().statusCode(REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode());

//        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, authorApiToken);
//        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());
//        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", authorApiToken);
//        publishDataset.then().assertThat().statusCode(OK.getStatusCode());

    }

    @Test
    public void testAddFileToDatasetSkipTabIngest() throws IOException, InterruptedException {

        Response createUser = UtilIT.createRandomUser();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        assertEquals(201, createDataverseResponse.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        assertEquals(201, createDatasetResponse.getStatusCode());
        Integer datasetIdInt = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        String pathToFile = "src/test/resources/sav/dct.sav";
        String jsonAsString = "{\"description\":\"My description.\",\"directoryLabel\":\"data/subdir1\",\"categories\":[\"Data\"], \"restrict\":\"false\", \"tabIngest\":\"false\"}";
        Response r = UtilIT.uploadFileViaNative(datasetIdInt.toString(), pathToFile, jsonAsString, apiToken);
        logger.info(r.prettyPrint());
        assertEquals(200, r.getStatusCode());

        assertTrue(UtilIT.sleepForLock(datasetIdInt, "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile);

        Long dataFileId = JsonPath.from(r.body().asString()).getLong("data.files[0].dataFile.id");
        Response fileMeta = UtilIT.getDataFileMetadataDraft(dataFileId, apiToken);
        String label = JsonPath.from(fileMeta.body().asString()).getString("label");
        assertEquals("dct.sav", label);

        pathToFile = "src/test/resources/sav/frequency-test.sav";
        jsonAsString = "{\"description\":\"My description.\",\"directoryLabel\":\"data/subdir1\",\"categories\":[\"Data\"], \"restrict\":\"false\"  }";
        Response rTabIngest = UtilIT.uploadFileViaNative(datasetIdInt.toString(), pathToFile, jsonAsString, apiToken);
        logger.info(rTabIngest.prettyPrint());
        assertEquals(200, rTabIngest.getStatusCode());

        assertTrue(UtilIT.sleepForLock(datasetIdInt, "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile);

        Long ingDataFileId = JsonPath.from(rTabIngest.body().asString()).getLong("data.files[0].dataFile.id");
        Response ingFileMeta = UtilIT.getDataFileMetadataDraft(ingDataFileId, apiToken);
        String ingLabel = JsonPath.from(ingFileMeta.body().asString()).getString("label");
        assertEquals("frequency-test.tab", ingLabel);

        //cleanup
        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetIdInt, apiToken);
        assertEquals(200, destroyDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        assertEquals(200, deleteUserResponse.getStatusCode());

    }

    @Test
    public void testDeleteFile() {
        msgt("testDeleteFile");
        // Create user
        String apiToken = createUserGetToken();

        // Create user with no permission
        String apiTokenNoPerms = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        // Upload file 1
        String pathToFile1 = "src/main/webapp/resources/images/dataverseproject.png";
        JsonObjectBuilder json1 = Json.createObjectBuilder()
                .add("description", "my description1")
                .add("directoryLabel", "data/subdir1")
                .add("categories", Json.createArrayBuilder().add("Data"));
        Response uploadResponse1 = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile1, json1.build(), apiToken);
        uploadResponse1.then().assertThat().statusCode(OK.getStatusCode());

        Integer fileId1 = JsonPath.from(uploadResponse1.body().asString()).getInt("data.files[0].dataFile.id");

        // Check file uploaded
        Response downloadResponse1 = UtilIT.downloadFile(fileId1, null, null, null, apiToken);
        downloadResponse1.then().assertThat().statusCode(OK.getStatusCode());

        // Delete file 1
        Response deleteResponseFail = UtilIT.deleteFileApi(fileId1, apiTokenNoPerms);
        deleteResponseFail.prettyPrint();
        deleteResponseFail.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        Response deleteResponse1 = UtilIT.deleteFileApi(fileId1, apiToken);
        deleteResponse1.then().assertThat().statusCode(OK.getStatusCode());

        // Check file 1 deleted for good because it was in a draft
        Response downloadResponse1notFound = UtilIT.downloadFile(fileId1, null, null, null, apiToken);
        downloadResponse1notFound.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // Upload file 2
        String pathToFile2 = "src/main/webapp/resources/images/cc0.png";
        JsonObjectBuilder json2 = Json.createObjectBuilder()
                .add("description", "my description2")
                .add("directoryLabel", "data/subdir1")
                .add("categories", Json.createArrayBuilder().add("Data"));
        Response uploadResponse2 = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile2, json2.build(), apiToken);
        uploadResponse2.then().assertThat().statusCode(OK.getStatusCode());

        Integer fileId2 = JsonPath.from(uploadResponse2.body().asString()).getInt("data.files[0].dataFile.id");

        // Upload file 3
        String pathToFile3 = "src/main/webapp/resources/images/orcid_16x16.png";
        JsonObjectBuilder json3 = Json.createObjectBuilder()
                .add("description", "my description3")
                .add("directoryLabel", "data/subdir1")
                .add("categories", Json.createArrayBuilder().add("Data"));
        Response uploadResponse3 = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile3, json3.build(), apiToken);
        uploadResponse3.then().assertThat().statusCode(OK.getStatusCode());

        Integer fileId3 = JsonPath.from(uploadResponse3.body().asString()).getInt("data.files[0].dataFile.id");

        // Publish collection and dataset
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());

        Response deleteResponse2 = UtilIT.deleteFileApi(fileId2, apiToken);
        deleteResponse2.then().assertThat().statusCode(OK.getStatusCode());

        // Check file 2 deleted from post v1.0 draft
        Response postv1draft = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_DRAFT, apiToken);
        postv1draft.prettyPrint();
        postv1draft.then().assertThat()
                .body("data.files.size()", equalTo(1))
                .statusCode(OK.getStatusCode());

        // Check file 2 still in v1.0
        Response v1 = UtilIT.getDatasetVersion(datasetPid, "1.0", apiToken);
        v1.prettyPrint();
        v1.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Map<String, Object> v1files1 = with(v1.body().asString()).param("fileToFind", "cc0.png")
                .getJsonObject("data.files.find { files -> files.label == fileToFind }");
        assertEquals("cc0.png", v1files1.get("label"));

        // Check file 2 still downloadable (published in in v1.0)
        Response downloadResponse2 = UtilIT.downloadFile(fileId2, null, null, null, apiToken);
        downloadResponse2.then().assertThat().statusCode(OK.getStatusCode());

        // Check file 3 still in post v1.0 draft
        Response postv1draft2 = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_DRAFT, apiToken);
        postv1draft2.prettyPrint();
        postv1draft2.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Map<String, Object> v1files2 = with(postv1draft2.body().asString()).param("fileToFind", "orcid_16x16.png")
                .getJsonObject("data.files.find { files -> files.label == fileToFind }");
        assertEquals("orcid_16x16.png", v1files2.get("label"));

        // Delete file 3, the current version is still draft
        Response deleteResponse3 = UtilIT.deleteFileApi(fileId3, apiToken);
        deleteResponse3.then().assertThat().statusCode(OK.getStatusCode());

        // Check file 3 deleted from post v1.0 draft
        Response postv1draft3 = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_DRAFT, apiToken);
        postv1draft3.prettyPrint();
        postv1draft3.then().assertThat()
                .body("data.files[0]", equalTo(null))
                .statusCode(OK.getStatusCode());
    }
    
    // The following specifically tests file-level PIDs configuration in 
    // individual collections (#8889/#9614)
    @Test
    public void testFilePIDsBehavior() {
        // Create user
        Response createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);
        Response toggleSuperuser = UtilIT.makeSuperUser(username);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());
        try {
            UtilIT.enableSetting(SettingsServiceBean.Key.FilePIDsEnabled);

            // Create Dataverse
            String collectionAlias = createDataverseGetAlias(apiToken);

            // Create Initial Dataset with 1 file:
            Integer datasetId = createDatasetGetId(collectionAlias, apiToken);
            String pathToFile = "scripts/search/data/replace_test/003.txt";
            Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

            addResponse.then().assertThat().body("data.files[0].dataFile.contentType", equalTo("text/plain"))
                    .body("data.files[0].label", equalTo("003.txt")).statusCode(OK.getStatusCode());

            Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

            // -------------------------
            // Publish dataverse and dataset
            // -------------------------
            msg("Publish dataverse and dataset");
            Response publishCollectionResp = UtilIT.publishDataverseViaSword(collectionAlias, apiToken);
            publishCollectionResp.then().assertThat().statusCode(OK.getStatusCode());

            Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
            publishDatasetResp.then().assertThat().statusCode(OK.getStatusCode());

            // The file in this dataset should have been assigned a PID when it was
            // published:
            Response fileInfoResponse = UtilIT.getFileData(origFileId.toString(), apiToken);
            fileInfoResponse.then().assertThat().statusCode(OK.getStatusCode());
            String fileInfoResponseString = fileInfoResponse.body().asString();
            msg(fileInfoResponseString);

            String origFilePersistentId = JsonPath.from(fileInfoResponseString).getString("data.dataFile.persistentId");
            assertNotNull(
                    "The file did not get a persistent identifier assigned (check that file PIDs are enabled instance-wide!)",
                    origFilePersistentId);

            // Now change the file PIDs registration configuration for the collection:
            UtilIT.enableSetting(SettingsServiceBean.Key.AllowEnablingFilePIDsPerCollection);
            Response changeAttributeResp = UtilIT.setCollectionAttribute(collectionAlias, "filePIDsEnabled", "false",
                    apiToken);

            // ... And do the whole thing with creating another dataset with a file:

            datasetId = createDatasetGetId(collectionAlias, apiToken);
            addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
            addResponse.then().assertThat().statusCode(OK.getStatusCode());
            Long newFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

            // And publish this dataset:
            msg("Publish second dataset");

            publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
            publishDatasetResp.then().assertThat().statusCode(OK.getStatusCode());

            // And confirm that the file didn't get a PID:

            fileInfoResponse = UtilIT.getFileData(newFileId.toString(), apiToken);
            fileInfoResponse.then().assertThat().statusCode(OK.getStatusCode());
            fileInfoResponseString = fileInfoResponse.body().asString();
            msg(fileInfoResponseString);

            assertEquals("", JsonPath.from(fileInfoResponseString).getString("data.dataFile.persistentId"),
                "The file was NOT supposed to be issued a PID");
        } finally {
            UtilIT.deleteSetting(SettingsServiceBean.Key.FilePIDsEnabled);
            UtilIT.deleteSetting(SettingsServiceBean.Key.AllowEnablingFilePIDsPerCollection);
        }
    }

    @Test
    public void testGetFileDownloadCount() throws InterruptedException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        // Upload test file
        String pathToTestFile = "src/test/resources/images/coffeeshop.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Publish collection and dataset
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());

        // Download test file
        int testFileId = JsonPath.from(uploadResponse.body().asString()).getInt("data.files[0].dataFile.id");

        Response downloadResponse = UtilIT.downloadFile(testFileId, apiToken);
        downloadResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Ensure download count is updated
        sleep(2000);

        // Get download count and assert it is 1
        Response getFileDownloadCountResponse = UtilIT.getFileDownloadCount(Integer.toString(testFileId), apiToken);
        getFileDownloadCountResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("1"));

        // Call with invalid file id
        Response getFileDownloadCountInvalidIdResponse = UtilIT.getFileDownloadCount("testInvalidId", apiToken);
        getFileDownloadCountInvalidIdResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testGetFileDataTables() throws InterruptedException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        // Upload non-tabular file
        String pathToNonTabularTestFile = "src/test/resources/images/coffeeshop.png";
        Response uploadNonTabularFileResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToNonTabularTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadNonTabularFileResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Assert that getting data tables for non-tabular file fails
        int testNonTabularFileId = JsonPath.from(uploadNonTabularFileResponse.body().asString()).getInt("data.files[0].dataFile.id");
        Response getFileDataTablesForNonTabularFileResponse = UtilIT.getFileDataTables(Integer.toString(testNonTabularFileId), apiToken);
        getFileDataTablesForNonTabularFileResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        // Upload tabular file
        String pathToTabularTestFile = "src/test/resources/tab/test.tab";
        Response uploadTabularFileResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToTabularTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadTabularFileResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Ensure tabular file is ingested
        sleep(2000);

        String testTabularFileId = Integer.toString(JsonPath.from(uploadTabularFileResponse.body().asString()).getInt("data.files[0].dataFile.id"));

        // Get file data tables for the tabular file and assert data is obtained
        Response getFileDataTablesForTabularFileResponse = UtilIT.getFileDataTables(testTabularFileId, apiToken);
        getFileDataTablesForTabularFileResponse.then().assertThat().statusCode(OK.getStatusCode());
        int dataTablesNumber = JsonPath.from(getFileDataTablesForTabularFileResponse.body().asString()).getList("data").size();
        assertTrue(dataTablesNumber > 0);

        // Get file data tables for a restricted tabular file as the owner and assert data is obtained
        Response restrictFileResponse = UtilIT.restrictFile(testTabularFileId, true, apiToken);
        restrictFileResponse.then().assertThat().statusCode(OK.getStatusCode());
        getFileDataTablesForTabularFileResponse = UtilIT.getFileDataTables(testTabularFileId, apiToken);
        getFileDataTablesForTabularFileResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Get file data tables for a restricted tabular file as other user and assert forbidden error is thrown
        Response createRandomUser = UtilIT.createRandomUser();
        createRandomUser.then().assertThat().statusCode(OK.getStatusCode());
        String randomUserApiToken = UtilIT.getApiTokenFromResponse(createRandomUser);
        getFileDataTablesForTabularFileResponse = UtilIT.getFileDataTables(testTabularFileId, randomUserApiToken);
        getFileDataTablesForTabularFileResponse.then().assertThat().statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    public void testSetFileCategories() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        // Upload test file
        String pathToTestFile = "src/test/resources/images/coffeeshop.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());

        String dataFileId = uploadResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");

        // Set categories
        String testCategory0 = "testCategory0";
        List<String> testCategories = List.of(testCategory0);
        Response setFileCategoriesResponse = UtilIT.setFileCategories(dataFileId, apiToken, testCategories);
        setFileCategoriesResponse.then().assertThat().statusCode(OK.getStatusCode());
        // Get file data and check for new categories
        Response getFileDataResponse = UtilIT.getFileData(dataFileId, apiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("data.categories", hasItem(testCategory0))
                .statusCode(OK.getStatusCode());
        // Set categories
        String testCategory1 = "testCategory1";
        String testCategory2 = "testCategory2";
        testCategories = List.of(testCategory1, testCategory2);
        setFileCategoriesResponse = UtilIT.setFileCategories(dataFileId, apiToken, testCategories);
        setFileCategoriesResponse.then().assertThat().statusCode(OK.getStatusCode());
        // Get file data and check for new categories + original category
        getFileDataResponse = UtilIT.getFileData(dataFileId, apiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("data.categories", hasItem(testCategory0))
                .body("data.categories", hasItem(testCategory1))
                .body("data.categories", hasItem(testCategory2))
                .statusCode(OK.getStatusCode());
        // test replace categories
        testCategories = List.of(testCategory1, testCategory2);
        setFileCategoriesResponse = UtilIT.setFileCategories(dataFileId, apiToken, testCategories, true);
        setFileCategoriesResponse.then().assertThat().statusCode(OK.getStatusCode());
        // Get file data and check for new categories only
        getFileDataResponse = UtilIT.getFileData(dataFileId, apiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("data.categories", not(hasItem(testCategory0)))
                .body("data.categories", hasItem(testCategory1))
                .body("data.categories", hasItem(testCategory2))
                .statusCode(OK.getStatusCode());
        // Test clear all categories by passing empty list
        setFileCategoriesResponse = UtilIT.setFileCategories(dataFileId, apiToken, Lists.emptyList(), true);
        setFileCategoriesResponse.then().assertThat().statusCode(OK.getStatusCode());
        getFileDataResponse = UtilIT.getFileData(dataFileId, apiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("data.dataFile", not(hasItem("categories")))
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testSetFileTabularTags() throws InterruptedException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        // Upload tabular file
        String pathToTabularTestFile = "src/test/resources/tab/test.tab";
        Response uploadTabularFileResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToTabularTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadTabularFileResponse.then().assertThat().statusCode(OK.getStatusCode());

        String tabularFileId = uploadTabularFileResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");

        // Ensure tabular file is ingested
        sleep(2000);

        // Set tabular tags
        String testTabularTag1 = "Survey";
        String testTabularTag2 = "Genomics";
        // We repeat one to test that it is not duplicated
        String testTabularTag3 = "Genomics";
        List<String> testTabularTags = List.of(testTabularTag1, testTabularTag2, testTabularTag3);
        Response setFileTabularTagsResponse = UtilIT.setFileTabularTags(tabularFileId, apiToken, testTabularTags);
        setFileTabularTagsResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Get file data and check for new tabular tags
        Response getFileDataResponse = UtilIT.getFileData(tabularFileId, apiToken);
        getFileDataResponse.then().assertThat()
                .body("data.dataFile.tabularTags", hasItem(testTabularTag1))
                .body("data.dataFile.tabularTags", hasItem(testTabularTag2))
                .statusCode(OK.getStatusCode());

        int actualTabularTagsCount = getFileDataResponse.jsonPath().getList("data.dataFile.tabularTags").size();
        assertEquals(2, actualTabularTagsCount);

        // Set invalid tabular tag
        String testInvalidTabularTag = "Invalid";
        setFileTabularTagsResponse = UtilIT.setFileTabularTags(tabularFileId, apiToken, List.of(testInvalidTabularTag));
        setFileTabularTagsResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        // Get file data and check tabular tags are unaltered
        getFileDataResponse = UtilIT.getFileData(tabularFileId, apiToken);
        getFileDataResponse.then().assertThat()
                .body("data.dataFile.tabularTags", hasItem(testTabularTag1))
                .body("data.dataFile.tabularTags", hasItem(testTabularTag2))
                .statusCode(OK.getStatusCode());

        actualTabularTagsCount = getFileDataResponse.jsonPath().getList("data.dataFile.tabularTags").size();
        assertEquals(2, actualTabularTagsCount);

        // Should receive an error when calling the endpoint for a non-tabular file
        String pathToTestFile = "src/test/resources/images/coffeeshop.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());

        String nonTabularFileId = uploadResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");

        setFileTabularTagsResponse = UtilIT.setFileTabularTags(nonTabularFileId, apiToken, List.of(testInvalidTabularTag));
        setFileTabularTagsResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        // Test set with replaceData = true to show that the list is replaced and not added to
        setFileTabularTagsResponse = UtilIT.setFileTabularTags(tabularFileId, apiToken, List.of("Geospatial"), true);
        setFileTabularTagsResponse.then().assertThat().statusCode(OK.getStatusCode());
        getFileDataResponse = UtilIT.getFileData(tabularFileId, apiToken);
        actualTabularTagsCount = getFileDataResponse.jsonPath().getList("data.dataFile.tabularTags").size();
        assertEquals(1, actualTabularTagsCount);
        // Test clear all tags by passing empty list
        setFileTabularTagsResponse = UtilIT.setFileTabularTags(tabularFileId, apiToken, Lists.emptyList(), true);
        setFileTabularTagsResponse.then().assertThat().statusCode(OK.getStatusCode());
        getFileDataResponse = UtilIT.getFileData(tabularFileId, apiToken);
        getFileDataResponse.prettyPrint();
        getFileDataResponse.then().assertThat()
                .body("data.dataFile", not(hasItem("tabularTags")))
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testGetHasBeenDeleted() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        // Upload test file
        String pathToTestFile = "src/test/resources/images/coffeeshop.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());

        String dataFileId = uploadResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");

        // Publish dataverse and dataset
        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Assert that the file has not been deleted
        Response getHasBeenDeletedResponse = UtilIT.getHasBeenDeleted(dataFileId, apiToken);
        getHasBeenDeletedResponse.then().assertThat().statusCode(OK.getStatusCode());
        boolean fileHasBeenDeleted = JsonPath.from(getHasBeenDeletedResponse.body().asString()).getBoolean("data");
        assertFalse(fileHasBeenDeleted);

        // Delete test file
        Response deleteFileInDatasetResponse = UtilIT.deleteFileInDataset(Integer.parseInt(dataFileId), apiToken);
        deleteFileInDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Assert that the file has been deleted
        getHasBeenDeletedResponse = UtilIT.getHasBeenDeleted(dataFileId, apiToken);
        getHasBeenDeletedResponse.then().assertThat().statusCode(OK.getStatusCode());
        fileHasBeenDeleted = JsonPath.from(getHasBeenDeletedResponse.body().asString()).getBoolean("data");
        assertTrue(fileHasBeenDeleted);
    }
    
    @Test
    public void testCollectionStorageQuotas() {
        // A minimal storage quota functionality test: 
        // - We create a collection and define a storage quota
        // - We configure Dataverse to enforce it 
        // - We confirm that we can upload a file with the size under the quota
        // - We confirm that we cannot upload a file once the quota is reached
        // - We disable the quota on the collection via the API
        
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        
        System.out.println("dataset id: "+datasetId);
        
        Response checkQuotaResponse = UtilIT.checkCollectionQuota(dataverseAlias, apiToken);
        checkQuotaResponse.then().assertThat().statusCode(OK.getStatusCode());
        // This brand new collection shouldn't have any quota defined yet: 
        assertEquals(BundleUtil.getStringFromBundle("dataverse.storage.quota.notdefined"), JsonPath.from(checkQuotaResponse.body().asString()).getString("data.message"));
        
        // Set quota to 1K:
        Response setQuotaResponse = UtilIT.setCollectionQuota(dataverseAlias, 1024, apiToken);
        setQuotaResponse.then().assertThat().statusCode(OK.getStatusCode());
        assertEquals(BundleUtil.getStringFromBundle("dataverse.storage.quota.updated"), JsonPath.from(setQuotaResponse.body().asString()).getString("data.message"));
        
        // Check again:
        checkQuotaResponse = UtilIT.checkCollectionQuota(dataverseAlias, apiToken);
        checkQuotaResponse.then().assertThat().statusCode(OK.getStatusCode());
        String expectedApiMessage = BundleUtil.getStringFromBundle("dataverse.storage.quota.allocation", Arrays.asList("1,024"));
        assertEquals(expectedApiMessage, JsonPath.from(checkQuotaResponse.body().asString()).getString("data.message"));

        System.out.println(expectedApiMessage);
        
        UtilIT.enableSetting(SettingsServiceBean.Key.UseStorageQuotas);
                
        String pathToFile306bytes = "src/test/resources/FileRecordJobIT.properties"; 
        String pathToFile1787bytes = "src/test/resources/datacite.xml";

        // Upload a small file: 
        
        Response uploadResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToFile306bytes, Json.createObjectBuilder().build(), apiToken);
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());
        
        // Check the recorded storage use: 
        
        Response checkStorageUseResponse = UtilIT.checkCollectionStorageUse(dataverseAlias, apiToken);
        checkStorageUseResponse.then().assertThat().statusCode(OK.getStatusCode());
        expectedApiMessage = BundleUtil.getStringFromBundle("dataverse.storage.use", Arrays.asList("306"));
        assertEquals(expectedApiMessage, JsonPath.from(checkStorageUseResponse.body().asString()).getString("data.message"));

        System.out.println(expectedApiMessage);
        
        // Attempt to upload the second file - this should get us over the quota, 
        // so it should be rejected:
        
        uploadResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToFile1787bytes, Json.createObjectBuilder().build(), apiToken);
        uploadResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
        // We should get this error message made up from 2 Bundle strings:
        expectedApiMessage = BundleUtil.getStringFromBundle("file.addreplace.error.ingest_create_file_err");
        expectedApiMessage = expectedApiMessage + " " + BundleUtil.getStringFromBundle("file.addreplace.error.quota_exceeded", Arrays.asList("1.7 KB", "718 B"));
        assertEquals(expectedApiMessage, JsonPath.from(uploadResponse.body().asString()).getString("message"));
        
        System.out.println(expectedApiMessage);
        
        // Check Storage Use again - should be unchanged: 
        
        checkStorageUseResponse = UtilIT.checkCollectionStorageUse(dataverseAlias, apiToken);
        checkStorageUseResponse.then().assertThat().statusCode(OK.getStatusCode());
        expectedApiMessage = BundleUtil.getStringFromBundle("dataverse.storage.use", Arrays.asList("306"));
        assertEquals(expectedApiMessage, JsonPath.from(checkStorageUseResponse.body().asString()).getString("data.message"));

        // Disable the quota on the collection; try again:
        
        Response disableQuotaResponse = UtilIT.disableCollectionQuota(dataverseAlias, apiToken);
        disableQuotaResponse.then().assertThat().statusCode(OK.getStatusCode());
        expectedApiMessage = BundleUtil.getStringFromBundle("dataverse.storage.quota.deleted");
        assertEquals(expectedApiMessage, JsonPath.from(disableQuotaResponse.body().asString()).getString("data.message"));

        // Check again: 
        
        checkQuotaResponse = UtilIT.checkCollectionQuota(dataverseAlias, apiToken);
        checkQuotaResponse.then().assertThat().statusCode(OK.getStatusCode());
        // ... should say "no quota", again: 
        assertEquals(BundleUtil.getStringFromBundle("dataverse.storage.quota.notdefined"), JsonPath.from(checkQuotaResponse.body().asString()).getString("data.message"));
        
        // And try to upload the larger file again:
        
        uploadResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToFile1787bytes, Json.createObjectBuilder().build(), apiToken);
        // ... should work this time around:
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());
            
        // Let's confirm that the total storage use has been properly implemented:

        //try {sleep(1000);}catch(InterruptedException ie){}
        
        checkStorageUseResponse = UtilIT.checkCollectionStorageUse(dataverseAlias, apiToken);
        checkStorageUseResponse.then().assertThat().statusCode(OK.getStatusCode());
        expectedApiMessage = BundleUtil.getStringFromBundle("dataverse.storage.use", Arrays.asList("2,093"));
        assertEquals(expectedApiMessage, JsonPath.from(checkStorageUseResponse.body().asString()).getString("data.message"));

        System.out.println(expectedApiMessage);
        
        // @todo: a test for the storage use hierarchy? - create a couple of 
        // sub-collections, upload a file into a dataset in the farthest branch 
        // collection, make sure the usage has been incremented all the way up 
        // to the root? 
        
        UtilIT.deleteSetting(SettingsServiceBean.Key.UseStorageQuotas);
    }
    
    @Test
    public void testIngestWithAndWithoutVariableHeader() throws NoSuchAlgorithmException {
        msgt("testIngestWithAndWithoutVariableHeader");
        
        // The compact Stata file we'll be using for this test: 
        // (this file is provided by Stata inc. - it's genuine quality)
        String pathToFile = "scripts/search/data/tabular/stata13-auto.dta";
        // The pre-calculated MD5 signature of the *complete* tab-delimited 
        // file as seen by the final Access API user (i.e., with the variable 
        // header line in it):
        String tabularFileMD5 = "f298c2567cc8eb544e36ad83edf6f595";
        // Expected byte sizes of the generated tab-delimited file as stored, 
        // with and without the header:
        int tabularFileSizeWoutHeader = 4026; 
        int tabularFileSizeWithHeader = 4113; 

        String apiToken = createUserGetToken();
        String dataverseAlias = createDataverseGetAlias(apiToken);
        Integer datasetIdA = createDatasetGetId(dataverseAlias, apiToken);
        
        // Before we do anything else, make sure that the instance is configured 
        // the "old" way, i.e., to store ingested files without the headers:
        UtilIT.deleteSetting(SettingsServiceBean.Key.StoreIngestedTabularFilesWithVarHeaders);
        
        Response addResponse = UtilIT.uploadFileViaNative(datasetIdA.toString(), pathToFile, apiToken);
        addResponse.prettyPrint();

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("application/x-stata-13"))
                .body("data.files[0].label", equalTo("stata13-auto.dta"))
                .statusCode(OK.getStatusCode());

        Long fileIdA = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");
        assertNotNull(fileIdA);

        // Give file time to ingest
        assertTrue(UtilIT.sleepForLock(datasetIdA.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile + "(A)");

        // Check the metadata to confirm that the file has ingested: 

        Response fileDataResponse = UtilIT.getFileData(fileIdA.toString(), apiToken);
        fileDataResponse.prettyPrint();
        fileDataResponse.then().assertThat()
                .body("data.dataFile.filename", equalTo("stata13-auto.tab"))
                .body("data.dataFile.contentType", equalTo("text/tab-separated-values"))
                .body("data.dataFile.filesize", equalTo(tabularFileSizeWoutHeader))
                .statusCode(OK.getStatusCode());
        

        // Download the file, verify the checksum: 

        Response fileDownloadResponse = UtilIT.downloadFile(fileIdA.intValue(), apiToken);
        fileDownloadResponse.then().assertThat()
                .statusCode(OK.getStatusCode()); 
        
        byte[] fileDownloadBytes = fileDownloadResponse.body().asByteArray(); 
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(fileDownloadBytes);
        byte[] rawDigestBytes = messageDigest.digest();
        String tabularFileMD5calculated = FileUtil.checksumDigestToString(rawDigestBytes);
        
        msgt("md5 of the downloaded file (saved without the variable name header): "+tabularFileMD5calculated);
        
        assertEquals(tabularFileMD5, tabularFileMD5calculated);

        // Repeat the whole thing, in another dataset (because we will be uploading 
        // an identical file), but with the "store with the header setting enabled): 
        
        UtilIT.enableSetting(SettingsServiceBean.Key.StoreIngestedTabularFilesWithVarHeaders);
        
        Integer datasetIdB = createDatasetGetId(dataverseAlias, apiToken);
        
        addResponse = UtilIT.uploadFileViaNative(datasetIdB.toString(), pathToFile, apiToken);
        addResponse.prettyPrint();

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("application/x-stata-13"))
                .body("data.files[0].label", equalTo("stata13-auto.dta"))
                .statusCode(OK.getStatusCode());

        Long fileIdB = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");
        assertNotNull(fileIdB);

        // Give file time to ingest
        assertTrue(UtilIT.sleepForLock(datasetIdB.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile + "(B)");
        
        // Check the metadata to confirm that the file has ingested: 

        fileDataResponse = UtilIT.getFileData(fileIdB.toString(), apiToken);
        fileDataResponse.prettyPrint();
        fileDataResponse.then().assertThat()
                .body("data.dataFile.filename", equalTo("stata13-auto.tab"))
                .body("data.dataFile.contentType", equalTo("text/tab-separated-values"))
                .body("data.dataFile.filesize", equalTo(tabularFileSizeWithHeader))
                .statusCode(OK.getStatusCode());
        

        // Download the file, verify the checksum, again

        fileDownloadResponse = UtilIT.downloadFile(fileIdB.intValue(), apiToken);
        fileDownloadResponse.then().assertThat()
                .statusCode(OK.getStatusCode()); 
        
        fileDownloadBytes = fileDownloadResponse.body().asByteArray(); 
        messageDigest.reset();
        messageDigest.update(fileDownloadBytes);
        rawDigestBytes = messageDigest.digest();
        tabularFileMD5calculated = FileUtil.checksumDigestToString(rawDigestBytes);
        
        msgt("md5 of the downloaded file (saved with the variable name header): "+tabularFileMD5calculated);
        
        assertEquals(tabularFileMD5, tabularFileMD5calculated);

        // In other words, whether the file was saved with, or without the header, 
        // as downloaded by the user, the end result must be the same in both cases!
        // In other words, whether that first line with the variable names is already
        // in the physical file, or added by Dataverse on the fly, the downloaded
        // content must be identical. 
        
        UtilIT.deleteSetting(SettingsServiceBean.Key.StoreIngestedTabularFilesWithVarHeaders);
        
        // @todo: cleanup? 
    }
    

    @Test
    public void testFileCitationByVersion() throws IOException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        String datasetPid = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");

        String pathToTestFile = "src/test/resources/images/coffeeshop.png";
        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadFile.then().assertThat().statusCode(OK.getStatusCode());

        Integer fileId = JsonPath.from(uploadFile.body().asString()).getInt("data.files[0].dataFile.id");

        String pidAsUrl = "https://doi.org/" + datasetPid.split("doi:")[1];
        int currentYear = Year.now().getValue();

        Response draftUnauthNoApitoken = UtilIT.getFileCitation(fileId, DS_VERSION_DRAFT, null);
        draftUnauthNoApitoken.prettyPrint();
        draftUnauthNoApitoken.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        Response createNoPermsUser = UtilIT.createRandomUser();
        createNoPermsUser.then().assertThat().statusCode(OK.getStatusCode());
        String noPermsApiToken = UtilIT.getApiTokenFromResponse(createNoPermsUser);

        Response draftUnauthNoPermsApiToken = UtilIT.getFileCitation(fileId, DS_VERSION_DRAFT, noPermsApiToken);
        draftUnauthNoPermsApiToken.prettyPrint();
        draftUnauthNoPermsApiToken.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        Response getFileCitationDraft = UtilIT.getFileCitation(fileId, DS_VERSION_DRAFT, apiToken);
        getFileCitationDraft.prettyPrint();
        getFileCitationDraft.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Finch, Fiona, " + currentYear + ", \"Darwin's Finches\", <a href=\"" + pidAsUrl + "\" target=\"_blank\">" + pidAsUrl + "</a>, Root, DRAFT VERSION; coffeeshop.png [fileName]"));

        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response publishedNoApiTokenNeeded = UtilIT.getFileCitation(fileId, "1.0", null);
        publishedNoApiTokenNeeded.then().assertThat().statusCode(OK.getStatusCode());

        Response publishedNoPermsApiTokenAllowed = UtilIT.getFileCitation(fileId, "1.0", noPermsApiToken);
        publishedNoPermsApiTokenAllowed.then().assertThat().statusCode(OK.getStatusCode());

        String updateJsonString = """
{
    "label": "foo.png"
}
""";

        Response updateMetadataResponse = UtilIT.updateFileMetadata(fileId.toString(), updateJsonString, apiToken);
        updateMetadataResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), updateMetadataResponse.getStatusCode());

        Response getFileCitationPostV1Draft = UtilIT.getFileCitation(fileId, DS_VERSION_DRAFT, apiToken);
        getFileCitationPostV1Draft.prettyPrint();
        getFileCitationPostV1Draft.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Finch, Fiona, " + currentYear + ", \"Darwin's Finches\", <a href=\"" + pidAsUrl + "\" target=\"_blank\">" + pidAsUrl + "</a>, Root, DRAFT VERSION; foo.png [fileName]"));

        Response getFileCitationV1OldFilename = UtilIT.getFileCitation(fileId, "1.0", apiToken);
        getFileCitationV1OldFilename.prettyPrint();
        getFileCitationV1OldFilename.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Finch, Fiona, " + currentYear + ", \"Darwin's Finches\", <a href=\"" + pidAsUrl + "\" target=\"_blank\">" + pidAsUrl + "</a>, Root, V1; coffeeshop.png [fileName]"));

        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken)
                .then().assertThat().statusCode(OK.getStatusCode());

        Response deaccessionDataset = UtilIT.deaccessionDataset(datasetId, "1.0", "just because", "http://example.com", apiToken);
        deaccessionDataset.prettyPrint();
        deaccessionDataset.then().assertThat().statusCode(OK.getStatusCode());

        Response getFileCitationV1PostDeaccessionAuthorDefault = UtilIT.getFileCitation(fileId, "1.0", apiToken);
        getFileCitationV1PostDeaccessionAuthorDefault.prettyPrint();
        getFileCitationV1PostDeaccessionAuthorDefault.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());
        
        Response getFileCitationV1PostDeaccessionAuthorIncludeDeaccessioned = UtilIT.getFileCitation(fileId, "1.0", true, apiToken);
        getFileCitationV1PostDeaccessionAuthorIncludeDeaccessioned.prettyPrint();
        getFileCitationV1PostDeaccessionAuthorIncludeDeaccessioned.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Finch, Fiona, " + currentYear + ", \"Darwin's Finches\", <a href=\"" + pidAsUrl + "\" target=\"_blank\">" + pidAsUrl + "</a>, Root, V1, DEACCESSIONED VERSION; coffeeshop.png [fileName]"));

        Response getFileCitationV1PostDeaccessionNoApiToken = UtilIT.getFileCitation(fileId, "1.0", null);
        getFileCitationV1PostDeaccessionNoApiToken.prettyPrint();
        getFileCitationV1PostDeaccessionNoApiToken.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode())
                .body("message", equalTo("Dataset version cannot be found or unauthorized."));

        Response getFileCitationV1PostDeaccessionNoPermsUser = UtilIT.getFileCitation(fileId, "1.0", noPermsApiToken);
        getFileCitationV1PostDeaccessionNoPermsUser.prettyPrint();
        getFileCitationV1PostDeaccessionNoPermsUser.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode())
                .body("message", equalTo("Dataset version cannot be found or unauthorized."));

    }

    @Test
    public void testUploadFilesWithLimits() throws JsonParseException {
        Response createUser = UtilIT.createRandomUser();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String adminApiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        // Update the dataverse with a datasetFileCountLimit of 1
        JsonObject data = JsonUtil.getJsonObject(createDataverseResponse.getBody().asString());
        JsonParser parser = new JsonParser();
        Dataverse dv = parser.parseDataverse(data.getJsonObject("data"));
        dv.setDatasetFileCountLimit(1);
        Response updateDataverseResponse = UtilIT.updateDataverse(dataverseAlias, dv, adminApiToken);
        updateDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.effectiveDatasetFileCountLimit", equalTo(1))
                .body("data.datasetFileCountLimit", equalTo(1));

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        String datasetPersistenceId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");
        createDatasetResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        // -------------------------
        // Add initial file
        // -------------------------
        String pathToFile = "scripts/search/data/tabular/50by1000.dta";
        Response uploadFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFileResponse.prettyPrint();
        uploadFileResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        String fileId = String.valueOf(JsonPath.from(uploadFileResponse.body().asString()).getInt("data.files[0].dataFile.id"));
        UtilIT.sleepForLock(datasetId, null, apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION);

        // upload a second file should fail since the limit is 1 file per dataset
        pathToFile = "scripts/search/data/tabular/open-source-at-harvard118.dta";
        uploadFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFileResponse.prettyPrint();
        uploadFileResponse.then().assertThat()
                .body("message", containsString(BundleUtil.getStringFromBundle("file.add.count_exceeds_limit", Collections.singletonList("1"))))
                .statusCode(BAD_REQUEST.getStatusCode());

        // Add 1 to file limit and upload a second file
        dv.setDatasetFileCountLimit(2);
        updateDataverseResponse = UtilIT.updateDataverse(dataverseAlias, dv, adminApiToken);
        updateDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.effectiveDatasetFileCountLimit", equalTo(2))
                .body("data.datasetFileCountLimit", equalTo(2));
        uploadFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFileResponse.prettyPrint();
        uploadFileResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        UtilIT.sleepForLock(datasetId, null, apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION);

        // Set limit back to 1 even though the number of files is 2
        dv.setDatasetFileCountLimit(1);
        updateDataverseResponse = UtilIT.updateDataverse(dataverseAlias, dv, adminApiToken);
        updateDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.effectiveDatasetFileCountLimit", equalTo(1))
                .body("data.datasetFileCountLimit", equalTo(1));

        Response getDatasetResponse = UtilIT.getDatasetVersion(datasetPersistenceId, DS_VERSION_DRAFT, apiToken);
        getDatasetResponse.prettyPrint();
        getDatasetResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.effectiveDatasetFileCountLimit", equalTo(1))
                .body("data.datasetFileUploadsAvailable", equalTo(0));

        // Replace a file should be allowed
        pathToFile = "scripts/search/data/tabular/120745.dta";
        Response replaceFileResponse = UtilIT.replaceFile(fileId, pathToFile, apiToken);
        replaceFileResponse.prettyPrint();
        replaceFileResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Superuser file uploads can exceed the limit!
        pathToFile = "scripts/search/data/tabular/stata13-auto.dta";
        uploadFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFileResponse.prettyPrint();
        uploadFileResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());
        uploadFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, adminApiToken);
        uploadFileResponse.prettyPrint();
        uploadFileResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Test changing the limit by a non-superuser
        dv.setDatasetFileCountLimit(100);
        updateDataverseResponse = UtilIT.updateDataverse(dataverseAlias, dv, apiToken);
        updateDataverseResponse.prettyPrint();
        updateDataverseResponse.then().assertThat()
                .body("message", containsString(BundleUtil.getStringFromBundle("file.dataset.error.set.file.count.limit")))
                .statusCode(FORBIDDEN.getStatusCode());
    }
}
