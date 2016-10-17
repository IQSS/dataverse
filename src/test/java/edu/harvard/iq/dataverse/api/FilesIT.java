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
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
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
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        //createUser.prettyPrint();

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
        //createDatasetResponse.prettyPrint();
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
        //msgt("Here it is: " + addResponse.prettyPrint());
        String successMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        

      
        addResponse.then().assertThat()
                .body("message", equalTo(successMsg))
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
                .body("data.contentType", equalTo("image/png"))
                .body("data.filename", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());
        
    }

    
    @Test
    public void test_002_AddFileBadDatasetId() {
        msgt("test_002_AddFileNullFileId");
         // Create user
        String apiToken = "someToken";

        // Create Dataset
        String datasetId = "cat"; //createDatasetGetId(dataverseAlias, apiToken);
       
        
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative("cat", pathToFile, apiToken);
        //msgt("Here it is: " + addResponse.prettyPrint());

        // Adding a non-numeric id should result in a 404
        addResponse.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());
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
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        String successMsgAdd = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        
      
        addResponse.then().assertThat()
                .body("message", equalTo(successMsgAdd))
                .body("data.contentType", equalTo("image/png"))
                .body("data.filename", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());
        
        
        long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.id");

        msg("Orig file id: " + origFileId);
        assertNotNull(origFileId);    // If checkOut fails, display message
        
        // -------------------------
        // Publish dataverse and dataset
        // -------------------------
        Response publishDataversetResp = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataversetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
        
         
        // -------------------------
        // Replace file
        // -------------------------
        String pathToFile2 = "src/main/webapp/resources/images/cc0.png";
        Response replaceResp = UtilIT.replaceFile(origFileId, pathToFile2, apiToken);
        
        msgt(replaceResp.prettyPrint());
        
        String successMsg2 = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.replace");        

        replaceResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("message", equalTo(successMsg2))
                .body("data.filename", equalTo("cc0.png"))
                //.body("data.rootDataFileId", equalTo(origFileId))              
                ;

        long rootDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.rootDataFileId");
        long previousDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.previousDataFileId");
        long newDataFileId = JsonPath.from(replaceResp.body().asString()).getLong("data.id");
        
        assertEquals(origFileId, previousDataFileId);
        assertEquals(rootDataFileId, previousDataFileId);

        
        // -------------------------
        // Publish dataset (again)
        // -------------------------
        publishDatasetResp = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResp.then().assertThat()
                .statusCode(OK.getStatusCode());
          
        
        // -------------------------
        // Replace file (again)
        // -------------------------
        String pathToFile3 = "src/main/webapp/resources/images/favicondataverse.png";
        Response replaceResp2 = UtilIT.replaceFile(newDataFileId, pathToFile3, apiToken);
        
        msgt("2nd replace: " + replaceResp2.prettyPrint());
        
        replaceResp2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("status", equalTo(AbstractApiBean.STATUS_OK))
                .body("message", equalTo(successMsg2))
                .body("data.filename", equalTo("favicondataverse.png"))
                ;

        long rootDataFileId2 = JsonPath.from(replaceResp2.body().asString()).getLong("data.rootDataFileId");
        long previousDataFileId2 = JsonPath.from(replaceResp2.body().asString()).getLong("data.previousDataFileId");
        
        msgt("newDataFileId: " + newDataFileId);
        msgt("previousDataFileId2: " + previousDataFileId2);
        msgt("rootDataFileId2: " + rootDataFileId2);
        
        assertEquals(newDataFileId, previousDataFileId2);
        assertEquals(rootDataFileId2, origFileId);        
        
    }
    
    //@Test
    public void xtest_006_ReplaceFileGood() {

        // Create user
        String apiToken = createUserGetToken();

        // Create Dataverse
        String dataverseAlias = createDataverseGetAlias(apiToken);

        // Create Dataset
        Integer datasetId = createDatasetGetId(dataverseAlias, apiToken);
        
        // ---------------------
        // Add file
        // ---------------------
        Response getDatasetJsonBeforeFiles = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonBeforeFiles.prettyPrint();
        getDatasetJsonBeforeFiles.then().assertThat().statusCode(OK.getStatusCode());
        String protocol1 = JsonPath.from(getDatasetJsonBeforeFiles.getBody().asString()).getString("data.protocol");
        String authority1 = JsonPath.from(getDatasetJsonBeforeFiles.getBody().asString()).getString("data.authority");
        String identifier1 = JsonPath.from(getDatasetJsonBeforeFiles.getBody().asString()).getString("data.identifier");
        String dataset1PersistentId = protocol1 + ":" + authority1 + "/" + identifier1;

        Response uploadFileResponse = UtilIT.uploadRandomFile(dataset1PersistentId, apiToken);
        uploadFileResponse.prettyPrint();
        getDatasetJsonBeforeFiles.then().assertThat().statusCode(OK.getStatusCode());
        assertEquals(CREATED.getStatusCode(), uploadFileResponse.getStatusCode());

        Response getDatasetJsonWithFiles = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonWithFiles.prettyPrint();
        getDatasetJsonWithFiles.then().assertThat().statusCode(OK.getStatusCode());
        int fileId = JsonPath.from(getDatasetJsonWithFiles.getBody().asString()).getInt("data.latestVersion.files[0].dataFile.id");
        UtilIT.publishDataverseViaSword(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaSword(dataset1PersistentId, apiToken).then().assertThat().statusCode(OK.getStatusCode());

        // ---------------------
        // Replace file
        // ---------------------
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response replace = UtilIT.replaceFile(fileId, pathToFile, apiToken);
        replace.prettyPrint();
        
        String successMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        

        
        replace.then().assertThat()
                .body("message", equalTo("File successfully replaced!"))
                .statusCode(OK.getStatusCode());

        Response getDatasetJson = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJson.prettyPrint();
        getDatasetJson.then().assertThat()
                .body("data.latestVersion.files[0].dataFile.filename", equalTo("dataverseproject.png"))
                .body("data.latestVersion.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.latestVersion.files[0].dataFile.rootDataFileId", not(-1))
                .body("data.latestVersion.files[0].dataFile.previousDataFileId", equalTo(fileId))
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
