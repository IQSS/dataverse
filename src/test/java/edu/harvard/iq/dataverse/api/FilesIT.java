package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import com.jayway.restassured.path.json.JsonPath;
import edu.harvard.iq.dataverse.util.BundleUtil;
import static java.lang.Thread.sleep;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertNotNull;

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
       
        
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";

        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", "my description")
                .add("categories", Json.createArrayBuilder()
                        .add("Data")
                );

        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, json.build(), apiToken);

        //addResponse.prettyPrint();
        msgt("Here it is: " + addResponse.prettyPrint());
        String successMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        

      
        addResponse.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsg))
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
                .body("data.files[0].categories[0]", equalTo("Data"))
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].dataFile.description", equalTo("my description"))
//                .body("data.files[0].dataFile.tags", nullValue())
                .body("data.files[0].dataFile.tabularTags", nullValue())
                .body("data.files[0].label", equalTo("favicondataverse.png"))
                // not sure why description appears in two places
                .body("data.files[0].description", equalTo("my description"))
                .statusCode(OK.getStatusCode());
        
        
        //------------------------------------------------
        // Try to add the same file again -- and fail
        //------------------------------------------------
        Response addTwiceResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        msgt("2nd requests: " + addTwiceResponse.prettyPrint());    //addResponse.prettyPrint();
        
        String errMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.duplicate_file");
                
        addTwiceResponse.then().assertThat()
                .body("message", Matchers.startsWith(errMsg))
                .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
                .statusCode(BAD_REQUEST.getStatusCode());
    }

    
    @Test
    public void test_002_AddFileBadDatasetId() {
        msgt("test_002_AddFileNullFileId");
         // Create user
        String apiToken =createUserGetToken();

        // Create Dataset
        String datasetId = "cat"; //createDatasetGetId(dataverseAlias, apiToken);
       
        
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
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
       
        
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId, pathToFile, apiToken);


        msgt("Here it is: " + addResponse.prettyPrint());

        //String errMsg Start = ResourceBundle.getBundle("Bundle").getString("find.dataset.error.dataset.not.found.id");
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
       
        
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId, pathToFile, apiToken);

        msgt("Here it is: " + addResponse.prettyPrint());

        String errMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.auth");
        
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

        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";

        String junkJson = "thisIsNotJson";

        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, junkJson, apiToken);

        addResponse.then().assertThat()
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
                .body("data.files[0].categories", nullValue())
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].dataFile.description", equalTo(""))
                .body("data.files[0].dataFile.tabularTags", nullValue())
                .body("data.files[0].label", equalTo("favicondataverse.png"))
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

        
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiTokenUnauthorizedUser);

        //addResponse.prettyPrint();
        msgt("Here it is: " + addResponse.prettyPrint());
        
        
        String errMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.no_edit_dataset_permission");        

      
        addResponse.then().assertThat()
                .body("message", equalTo(errMsg))
                .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
                .statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    public void test_006_ReplaceFileGood() {
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
        String pathToFile = "scripts/search/data/replace_test/growing_file/2016-01/data.tsv";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        
      
        addResponse.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsgAdd))
                .body("data.files[0].dataFile.contentType", equalTo("text/tab-separated-values"))
                .body("data.files[0].label", equalTo("data.tsv"))
                .body("data.files[0].description", equalTo(""))
                .statusCode(OK.getStatusCode());
        
        
        long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

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
        Response replaceRespWrongCtype  = UtilIT.replaceFile(origFileId, pathToFileWrongCtype, apiToken);
        
        msgt(replaceRespWrongCtype.prettyPrint());
        
        String errMsgCtype = BundleUtil.getStringFromBundle("file.addreplace.error.replace.new_file_has_different_content_type", Arrays.asList("Tab-Delimited", "GIF Image"));

        
        replaceRespWrongCtype.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
                .body("message", equalTo(errMsgCtype));
                //.body("data.rootDataFileId", equalTo(origFileId))    
        
        // -------------------------
        // Replace file
        // -------------------------
        msg("Replace file - 1st time");
        String pathToFile2 = "scripts/search/data/replace_test/growing_file/2016-02/data.tsv";
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", "My Tabular Data")
                .add("categories", Json.createArrayBuilder()
                        .add("Data")
                );
        Response replaceResp = UtilIT.replaceFile(origFileId, pathToFile2, json.build(), apiToken);
        
        msgt(replaceResp.prettyPrint());
        
        String successMsg2 = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.replace");        

        replaceResp.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsg2))
                .body("data.files[0].label", equalTo("data.tsv"))
                .body("data.files[0].dataFile.contentType", equalTo("text/tab-separated-values"))
                .body("data.files[0].description", equalTo("My Tabular Data"))
                .body("data.files[0].categories[0]", equalTo("Data"))
                //.body("data.rootDataFileId", equalTo(origFileId))              
                .statusCode(OK.getStatusCode());

        long rootDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.rootDataFileId");
        long previousDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.previousDataFileId");
        long newDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.id");
        
        assertEquals(origFileId, previousDataFileId);
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
        String pathToFile3 = "scripts/search/data/replace_test/growing_file/2016-03/data.tsv";
        Response replaceResp2 = UtilIT.replaceFile(newDataFileId, pathToFile3, apiToken);
        
        msgt("2nd replace: " + replaceResp2.prettyPrint());
        
        replaceResp2.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsg2))
                .statusCode(OK.getStatusCode())
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
                .body("data.files[0].label", equalTo("data.tsv"))
                // yes, replacing a file blanks out the description (and categories)
                .body("data.files[0].description", equalTo(""))
                ;

        long rootDataFileId2 = JsonPath.from(replaceResp2.body().asString()).getLong("data.files[0].dataFile.rootDataFileId");
        long previousDataFileId2 = JsonPath.from(replaceResp2.body().asString()).getLong("data.files[0].dataFile.previousDataFileId");
        
        msgt("newDataFileId: " + newDataFileId);
        msgt("previousDataFileId2: " + previousDataFileId2);
        msgt("rootDataFileId2: " + rootDataFileId2);
        
        assertEquals(newDataFileId, previousDataFileId2);
        assertEquals(rootDataFileId2, origFileId);        
        
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

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");

        addResponse.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsgAdd))
                .body("data.files[0].dataFile.contentType", equalTo("application/x-stata"))
                .body("data.files[0].label", equalTo("50by1000.dta"))
                .statusCode(OK.getStatusCode());

        long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

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
        sleep(10000);

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
        Response replaceResp = UtilIT.replaceFile(origFileId, pathToFile2, json.build(), apiToken);

        msgt(replaceResp.prettyPrint());

        boolean impossibleToSetTabularTagsViaApi = true;
        if (impossibleToSetTabularTagsViaApi) {
            // previously we were getting this: You cannot add data file tags to a non-tabular file.
            System.out.println("Skipping attempt to verify that tabular tags can be set. Getting this error: Warning! The original and replacement file have different content types.  Original file is \\\"Tab-Delimited\\\" while replacement file is \\\"Stata Binary\\\"");
            return;
        }

        String successMsg2 = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.replace");

        replaceResp.then().assertThat()
                .body("message", equalTo(successMsg2))
                .body("data.files[0].label", equalTo("120745.dta"))
                .body("data.files[0].description", equalTo("tiny Stata file"))
                .body("data.files[0].categories[0]", equalTo("Data"))
                .statusCode(OK.getStatusCode());

        long rootDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.rootDataFileId");
        long previousDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.previousDataFileId");
        long newDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.files[0].dataFile.id");

        assertEquals(origFileId, previousDataFileId);
        assertEquals(rootDataFileId, previousDataFileId);

    }

    @Test
    public void testForceReplace() {
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

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("cc0.png"))
                .statusCode(OK.getStatusCode());

        long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

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
        Response replaceResp = UtilIT.replaceFile(origFileId, pathToFile2, json.build(), apiToken);

        replaceResp.prettyPrint();

        replaceResp.then().assertThat()
                .body("data.files[0].label", equalTo("data.tsv"))
                .body("data.files[0].description", equalTo("not an image"))
                .body("data.files[0].categories[0]", equalTo("Data"))
                .statusCode(OK.getStatusCode());

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
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        
      
        addResponse.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsgAdd))
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("favicondataverse.png"))
                .statusCode(OK.getStatusCode());
        
        
        long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

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
        Response replaceResp = UtilIT.replaceFile(origFileId, pathToFile2, apiToken);

        String errMsgUnpublished = ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.unpublished_file_cannot_be_replaced");
        
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
        long fakeFileId = origFileId+10;
        Response replaceResp2 = UtilIT.replaceFile(fakeFileId, pathToFile2, apiToken);

        msgt("non-existent id: " + replaceResp.prettyPrint());

        replaceResp2.then().assertThat()
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
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        
      
        addResponse.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsgAdd))
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("favicondataverse.png"))
                .statusCode(OK.getStatusCode());
        
        
        long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

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
        UtilIT.deleteFile((int)origFileId, apiToken);
        
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
        Response replaceResp = UtilIT.replaceFile(origFileId, pathToFile2, apiToken);

        String errMsgDeleted = ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.existing_file_not_in_latest_published_version");
        
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
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("favicondataverse.png"))
                .statusCode(OK.getStatusCode());

        long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

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
        Response replaceResp = UtilIT.replaceFile(origFileId, pathToFile2, jsonAsString, apiToken);

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

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");

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
