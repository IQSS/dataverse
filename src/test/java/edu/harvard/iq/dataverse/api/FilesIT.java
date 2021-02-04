package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.path.xml.XmlPath;
import static edu.harvard.iq.dataverse.api.AccessIT.apiToken;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import static java.lang.Thread.sleep;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static junit.framework.Assert.assertEquals;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.CoreMatchers.nullValue;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FilesIT {

    private static final Logger logger = Logger.getLogger(FilesIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
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
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
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
                .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
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

        String errMsg = BundleUtil.getStringFromBundle("file.addreplace.error.auth");
        
        addResponse.then().assertThat()
                .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
                .body("message", equalTo(errMsg))
                .statusCode(FORBIDDEN.getStatusCode());
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

        addResponse.then().assertThat()
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
                .body("data.files[0].categories", nullValue())
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].dataFile.description", equalTo(""))
                .body("data.files[0].dataFile.tabularTags", nullValue())
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                // not sure why description appears in two places
                .body("data.files[0].description", equalTo(""))
                .statusCode(OK.getStatusCode());
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
                .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
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
                .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
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
        Response replaceResp = UtilIT.replaceFile(origFileId.toString(), pathToFile2, json.build(), apiToken);
        
        msgt(replaceResp.prettyPrint());
        
        String successMsg2 = BundleUtil.getStringFromBundle("file.addreplace.success.replace");

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
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
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
       assertTrue("Failed test if Ingest Lock exceeds max duration " + pathToFile , UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION));

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
        assertEquals(BAD_REQUEST.getStatusCode(), updateMetadataFailResponse.getStatusCode());  
        
        //Adding an additional fileMetadata update tests after this to ensure updating replaced files works
        msg("Update file metadata for new file");
        //"junk" passed below is to test that it is discarded
        System.out.print("params: " +  String.valueOf(newDfId) + " " + updateJsonString + " " + apiToken);
        Response updateMetadataResponse = UtilIT.updateFileMetadata(String.valueOf(newDfId), updateJsonString, apiToken);
        assertEquals(OK.getStatusCode(), updateMetadataResponse.getStatusCode());  
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

        String errMsgUnpublished = BundleUtil.getStringFromBundle("file.addreplace.error.unpublished_file_cannot_be_replaced");
        
        replaceResp.then().assertThat()
               .statusCode(BAD_REQUEST.getStatusCode())
               .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
               .body("message", Matchers.startsWith(errMsgUnpublished))
               ;
       
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
               .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
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
               .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
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

        replaceResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("status", equalTo(AbstractApiBean.STATUS_OK));

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

        //unrestrict file
        restrict = false;
        Response unrestrictResponse = UtilIT.restrictFile(origFileId.toString(), restrict, apiToken);
        unrestrictResponse.prettyPrint();
        unrestrictResponse.then().assertThat()
                .body("data.message", equalTo("File dataverseproject.png unrestricted."))
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

        String successMsgAdd = BundleUtil.getStringFromBundle("file.addreplace.success.add");

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());

        long fileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

        Response searchShouldFindNothingBecauseUnpublished = UtilIT.search("id:datafile_" + fileId + "_draft", apiToken);
        searchShouldFindNothingBecauseUnpublished.prettyPrint();
        searchShouldFindNothingBecauseUnpublished.then().assertThat()
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
        
        assertTrue("Failed test if Ingest Lock exceeds max duration " + pathToFile , UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION));
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
        assertTrue("Failed test if Ingest Lock exceeds max duration " + pathToFile , UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION));
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
        assertTrue("Failed test if Ingest Lock exceeds max duration " + pathToFile , UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, 5));
     //   sleep(10000);
     
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        String apiTokenRando = createUserGetToken();
        
        Response datasetStorageSizeResponseDraft = UtilIT.findDatasetDownloadSize(datasetId.toString(), ":draft", apiTokenRando);
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
        assertTrue("Failed test if Ingest Lock exceeds max duration " + pathToFile , UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION));
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
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
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
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
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
        String expectedLinkAhrefTopFolder = "/api/datasets/"+datasetId+"/dirindex/?version=:draft&folder=subfolder";
        
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
    
}
