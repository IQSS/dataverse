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
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProvIT {
    
    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
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
        
        Response deleteProvJson = UtilIT.deleteProvJson(dataFileId.toString(), apiTokenForDepositor);
        deleteProvJson.prettyPrint();
        deleteProvJson.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        // TODO: Test that if provenance already exists in CPL (e.g. cplId in fileMetadata is not 0) upload returns error.
        //       There are currently no api endpoints to set up up this test.

        //Provenance FreeForm
        JsonObject provFreeFormGood = Json.createObjectBuilder()
                .add("text", "I inherited this file from my grandfather.")
                .build();
        Response uploadProvFreeForm = UtilIT.uploadProvFreeForm(dataFileId.toString(), provFreeFormGood, apiTokenForDepositor);
        uploadProvFreeForm.prettyPrint();
        uploadProvFreeForm.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response deleteProvFreeForm = UtilIT.deleteProvFreeForm(dataFileId.toString(), apiTokenForDepositor);
        deleteProvFreeForm.prettyPrint();
        deleteProvFreeForm.then().assertThat()
                .statusCode(OK.getStatusCode());

    }

}
