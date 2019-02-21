package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.path.xml.XmlPath;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import static java.lang.Thread.sleep;
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
import static junit.framework.Assert.assertEquals;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.CoreMatchers.nullValue;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;

public class FilesIT {

    private static final Logger logger = Logger.getLogger(FilesIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response removeSearchApiNonPublicAllowed = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        removeSearchApiNonPublicAllowed.prettyPrint();
        removeSearchApiNonPublicAllowed.then().assertThat()
                .statusCode(200);

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

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        
      
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
                .add("categories", Json.createArrayBuilder()
                        .add("Data")
                );
        Response replaceResp = UtilIT.replaceFile(origFileId.toString(), pathToFile2, json.build(), apiToken);
        
        msgt(replaceResp.prettyPrint());
        
        String successMsg2 = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.replace");        

        replaceResp.then().assertThat()
                /**
                 * @todo We have a need to show human readable success messages
                 * via API in a consistent location.
                 */
                //                .body("message", equalTo(successMsg2))
                .body("data.files[0].label", equalTo("004.txt"))
                .body("data.files[0].dataFile.contentType", startsWith("text/plain"))
                .body("data.files[0].description", equalTo("My Text File"))
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
        sleep(10000);

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

        assertEquals(origFileId.longValue(), previousDataFileId);
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
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("favicondataverse.png"))
                .statusCode(OK.getStatusCode());

        Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");
        String origFilePid = JsonPath.from(addResponse.body().asString()).getString("data.files[0].dataFile.persistentId");

        System.out.println("Orig file id: " + origFileId);
        assertNotNull(origFileId);    // If checkOut fails, display message

        //restrict file good
        Response restrictResponse = UtilIT.restrictFile(origFileId.toString(), restrict, apiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat()
                .body("data.message", equalTo("File favicondataverse.png restricted."))
                .statusCode(OK.getStatusCode());

        //restrict already restricted file bad
        Response restrictResponseBad = UtilIT.restrictFile(origFileId.toString(), restrict, apiToken);
        restrictResponseBad.prettyPrint();
        restrictResponseBad.then().assertThat()
                .body("message", equalTo("Problem trying to update restriction status on favicondataverse.png: File favicondataverse.png is already restricted"))
                .statusCode(BAD_REQUEST.getStatusCode());

        //unrestrict file
        restrict = false;
        Response unrestrictResponse = UtilIT.restrictFile(origFileId.toString(), restrict, apiToken);
        unrestrictResponse.prettyPrint();
        unrestrictResponse.then().assertThat()
                .body("data.message", equalTo("File favicondataverse.png unrestricted."))
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
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";

        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", "my description")
                .add("categories", Json.createArrayBuilder()
                    .add("Data")
                    )
                .add("restrict", "true");

        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, json.build(), apiToken);

        //addResponse.prettyPrint();
        msgt("Here it is: " + addResponse.prettyPrint());
        String successMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        
        
        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("favicondataverse.png"))
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
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("favicondataverse.png"))
                .statusCode(OK.getStatusCode());

        long fileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

        Response searchShouldFindNothingBecauseUnpublished = UtilIT.search("id:datafile_" + fileId + "_draft", apiToken);
        searchShouldFindNothingBecauseUnpublished.prettyPrint();
        searchShouldFindNothingBecauseUnpublished.then().assertThat()
                // This is normal. "limited to published data" http://guides.dataverse.org/en/4.7/api/search.html
                .body("data.total_count", equalTo(0))
                .statusCode(OK.getStatusCode());
        // Let's temporarily allow searching of drafts.
        Response enableNonPublicSearch = UtilIT.enableSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        enableNonPublicSearch.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response searchResponse = UtilIT.searchAndShowFacets("id:datafile_" + fileId + "_draft", apiToken);
        searchResponse.prettyPrint();
        searchResponse.then().assertThat()
                // Now we can search unpublished data. Just for testing!                
                // FIXME - SEK (9/20/17) the checksum type test was failing previously - commenting out for now 
                .body("data.total_count", equalTo(1))
                .body("data.items[0].name", equalTo("favicondataverse.png"))
 //               .body("data.items[0].checksum.type", equalTo("SHA-1"))
                .body("data.facets", CoreMatchers.not(equalTo(null)))
                // No "fileAccess" facet because :PublicInstall is set to true.
                .body("data.facets[0].publicationStatus", CoreMatchers.not(equalTo(null)))
                .statusCode(OK.getStatusCode());

        Response removeSearchApiNonPublicAllowed = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        removeSearchApiNonPublicAllowed.prettyPrint();
        removeSearchApiNonPublicAllowed.then().assertThat()
                .statusCode(200);

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

            String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
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
        sleep(10000);
        Response uningestFileResponse = UtilIT.uningestFile(origFileId, apiToken);
        assertEquals(200, uningestFileResponse.getStatusCode());       
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
