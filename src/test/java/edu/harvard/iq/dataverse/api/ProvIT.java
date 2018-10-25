package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProvIT {
    
    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    
    @Test 
    public void testFreeformDraftActions() {
        Response createDepositor = UtilIT.createRandomUser();
        createDepositor.prettyPrint();
        createDepositor.then().assertThat()
                .statusCode(OK.getStatusCode());
        String usernameForDepositor = UtilIT.getUsernameFromResponse(createDepositor);
        String apiTokenForDepositor = UtilIT.getApiTokenFromResponse(createDepositor);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiTokenForDepositor);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        
                
        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenForDepositor);
        publishDataverse.prettyPrint();
        assertEquals(200, publishDataverse.getStatusCode());

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenForDepositor);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response authorAddsFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiTokenForDepositor);
        authorAddsFile.prettyPrint();
        authorAddsFile.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());
        
        Long dataFileId = JsonPath.from(authorAddsFile.getBody().asString()).getLong("data.files[0].dataFile.id");
        
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiTokenForDepositor);
        publishDataset.prettyPrint();
        assertEquals(200, publishDataset.getStatusCode());
        
        //Provenance FreeForm
        JsonObject provFreeFormGood = Json.createObjectBuilder()
                .add("text", "I inherited this file from my grandfather.")
                .build();
        Response uploadProvFreeForm = UtilIT.uploadProvFreeForm(dataFileId.toString(), provFreeFormGood, apiTokenForDepositor);
        uploadProvFreeForm.prettyPrint();
        uploadProvFreeForm.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response datasetVersions = UtilIT.getDatasetVersions(datasetId.toString(), apiTokenForDepositor);
        datasetVersions.prettyPrint();
        datasetVersions.then().assertThat()
                .body("data[0].versionState", equalTo("DRAFT"));
        
        
    }
    
    @Test
    public void testAddProvFile() {

        Response createDepositor = UtilIT.createRandomUser();
        createDepositor.prettyPrint();
        createDepositor.then().assertThat()
                .statusCode(OK.getStatusCode());
        String usernameForDepositor = UtilIT.getUsernameFromResponse(createDepositor);
        String apiTokenForDepositor = UtilIT.getApiTokenFromResponse(createDepositor);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiTokenForDepositor);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        
                
        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenForDepositor);
        publishDataverse.prettyPrint();
        assertEquals(200, publishDataverse.getStatusCode());

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenForDepositor);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response authorAddsFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiTokenForDepositor);
        authorAddsFile.prettyPrint();
        authorAddsFile.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());

        Long dataFileId = JsonPath.from(authorAddsFile.getBody().asString()).getLong("data.files[0].dataFile.id");

        //Provenance Json
        JsonArray provJsonBadDueToBeingAnArray = Json.createArrayBuilder().add("bad").build();
        JsonObject provJsonGood = Json.createObjectBuilder()
                .add("entity", Json.createObjectBuilder()
                    .add("d1", Json.createObjectBuilder()
                        .add("name", "first.txt")
                        .add("value", "#ddg.function")    
                        .add("scope", "fn"))
                    .add("d2", Json.createObjectBuilder()
                        .add("rdt:name", "second.txt")
                        .add("rdt:value", "#ddg.function")    
                        .add("rdt:scope", "something"))        
                    )
                    .build();
        
        
        //entity name not found in prov json
        String entityNameBad = "broken name";
        Response uploadProvJsonBadEntity = UtilIT.uploadProvJson(dataFileId.toString(), provJsonGood, apiTokenForDepositor, entityNameBad);
        uploadProvJsonBadEntity.prettyPrint();
        uploadProvJsonBadEntity.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());
        
        //valid entity name
        String entityName = "d1";
        Response uploadProvJson = UtilIT.uploadProvJson(dataFileId.toString(), provJsonGood, apiTokenForDepositor, entityName);
        uploadProvJson.prettyPrint();
        uploadProvJson.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Provenance FreeForm
        JsonObject provFreeFormGood = Json.createObjectBuilder()
                .add("text", "I inherited this file from my grandfather.")
                .build();
        Response uploadProvFreeForm = UtilIT.uploadProvFreeForm(dataFileId.toString(), provFreeFormGood, apiTokenForDepositor);
        uploadProvFreeForm.prettyPrint();
        uploadProvFreeForm.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiTokenForDepositor);
        publishDataset.prettyPrint();
        assertEquals(200, publishDataset.getStatusCode());
        
        //We want to publish a 2nd version to confirm metadata is being passed over between version
        //Note: this UI file is just being used as an arbitrary file for upload testing
        String pathToFile2 = "src/main/webapp/resources/images/dataverseproject_logo.png";
        Response authorAddsFile2 = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile2, apiTokenForDepositor);
        authorAddsFile2.prettyPrint();
        authorAddsFile2.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data.files[0].label", equalTo("dataverseproject_logo.png"))
                .statusCode(OK.getStatusCode());
        
        Response publishDataset2 = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiTokenForDepositor);
        publishDataset2.prettyPrint();
        assertEquals(200, publishDataset2.getStatusCode());
        
        //Confirm prov freeform metadata was passed to 2nd version
        Response getProvFreeForm = UtilIT.getProvFreeForm(dataFileId.toString(), apiTokenForDepositor);
        getProvFreeForm.prettyPrint();
        getProvFreeForm.then().assertThat()
                .body("data", notNullValue(String.class))
                .body("data.text", notNullValue(String.class));
        assertEquals(200, getProvFreeForm.getStatusCode());

        //also confirm json, tho the 2nd publish shouldn't matter        
        Response getProvJson = UtilIT.getProvJson(dataFileId.toString(), apiTokenForDepositor);
        getProvJson.prettyPrint();
        getProvJson.then().assertThat()
                .body("data", notNullValue(String.class))
                .body("data.json", notNullValue(String.class));
        assertEquals(200, getProvJson.getStatusCode());
        
        // TODO: Test that if provenance already exists in CPL (e.g. cplId in fileMetadata is not 0) upload returns error.
        //       There are currently no api endpoints to set up up this test.
        
        Response deleteProvJson = UtilIT.deleteProvJson(dataFileId.toString(), apiTokenForDepositor);
        deleteProvJson.prettyPrint();
        deleteProvJson.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode()); //cannot delete json of a published dataset

// Command removed, redundant        
//        Response deleteProvFreeForm = UtilIT.deleteProvFreeForm(dataFileId.toString(), apiTokenForDepositor);
//        deleteProvFreeForm.prettyPrint();
//        deleteProvFreeForm.then().assertThat()
//                .statusCode(OK.getStatusCode());
        
    }
}
