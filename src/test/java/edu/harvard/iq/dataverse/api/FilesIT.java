package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import com.jayway.restassured.path.json.JsonPath;
import java.util.ResourceBundle;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
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
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        //addResponse.prettyPrint();
        msgt("Here it is: " + addResponse.prettyPrint());
        String successMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        

      
        addResponse.then().assertThat()
                .body("message", equalTo(successMsg))
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
                .body("files[0].contentType", equalTo("image/png"))
                .body("files[0].filename", equalTo("dataverseproject.png"))
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


        //msgt("Here it is: " + addResponse.prettyPrint());

        String errMsgStart = ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.dataset_id_not_found");
        
         addResponse.then().assertThat()
                .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
                .body("message", Matchers.startsWith(errMsgStart))
                .statusCode(BAD_REQUEST.getStatusCode());
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
    public void test_005_AddFileBadPermissions() {
        msgt("test_005_AddFileBadPerms");

        // To do!!!
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
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        
      
        addResponse.then().assertThat()
                .body("message", equalTo(successMsgAdd))
                .body("files[0].contentType", equalTo("image/png"))
                .body("files[0].filename", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());
        
        
        long origFileId = JsonPath.from(addResponse.body().asString()).getLong("files[0].id");

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
        
        String errMsgCtype = ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.replace.new_file_has_different_content_type");        

        
        replaceRespWrongCtype.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
                .body("message", equalTo(errMsgCtype));
                //.body("data.rootDataFileId", equalTo(origFileId))    
        
        // -------------------------
        // Replace file
        // -------------------------
        msg("Replace file - 1st time");
        String pathToFile2 = "src/main/webapp/resources/images/cc0.png";
        Response replaceResp = UtilIT.replaceFile(origFileId, pathToFile2, apiToken);
        
        msgt(replaceResp.prettyPrint());
        
        String successMsg2 = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.replace");        

        replaceResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("message", equalTo(successMsg2))
                .body("files[0].filename", equalTo("cc0.png"))
                //.body("data.rootDataFileId", equalTo(origFileId))              
                ;

        long rootDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("files[0].rootDataFileId");
        long previousDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("files[0].previousDataFileId");
        long newDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("files[0].id");
        
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
        String pathToFile3 = "src/main/webapp/resources/images/favicondataverse.png";
        Response replaceResp2 = UtilIT.replaceFile(newDataFileId, pathToFile3, apiToken);
        
        msgt("2nd replace: " + replaceResp2.prettyPrint());
        
        replaceResp2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
                .body("message", equalTo(successMsg2))
                .body("files[0].filename", equalTo("favicondataverse.png"))
                ;

        long rootDataFileId2 = JsonPath.from(replaceResp2.body().asString()).getLong("files[0].rootDataFileId");
        long previousDataFileId2 = JsonPath.from(replaceResp2.body().asString()).getLong("files[0].previousDataFileId");
        
        msgt("newDataFileId: " + newDataFileId);
        msgt("previousDataFileId2: " + previousDataFileId2);
        msgt("rootDataFileId2: " + rootDataFileId2);
        
        assertEquals(newDataFileId, previousDataFileId2);
        assertEquals(rootDataFileId2, origFileId);        
        
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
                .body("message", equalTo(successMsgAdd))
                .body("files[0].contentType", equalTo("image/png"))
                .body("files[0].filename", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());
        
        
        long origFileId = JsonPath.from(addResponse.body().asString()).getLong("files[0].id");

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
        Response replaceResp2 = UtilIT.replaceFile(origFileId+10, pathToFile2, apiToken);

        msgt("non-existent id: " + replaceResp.prettyPrint());

        String errMsg1 = ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.existing_file_to_replace_not_found_by_id");        

        replaceResp2.then().assertThat()
               .statusCode(BAD_REQUEST.getStatusCode())
               .body("status", equalTo(AbstractApiBean.STATUS_ERROR))
               .body("message", Matchers.startsWith(errMsg1))
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
                .body("message", equalTo(successMsgAdd))
                .body("files[0].contentType", equalTo("image/png"))
                .body("files[0].filename", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());
        
        
        long origFileId = JsonPath.from(addResponse.body().asString()).getLong("files[0].id");

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
