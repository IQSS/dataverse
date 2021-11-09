package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import org.junit.BeforeClass;
import org.junit.Test;

public class ValidateIT {

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testValidate() throws IOException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response createUserNoPrivs = UtilIT.createRandomUser();
        createUserNoPrivs.then().assertThat().statusCode(OK.getStatusCode());
        String apiTokenNoPrivs = UtilIT.getApiTokenFromResponse(createUserNoPrivs);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);
        String badCharacter = "(\f)"; // form feed
//        badCharacter = "{\u0002}"; // start of text (can't reproduce problem)
//        badCharacter = "[\u000C]"; // form feed
//        badCharacter = "(...)";

        JsonObjectBuilder jsonUpdateObject = Json.createObjectBuilder().add("fields",
                Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("typeName", "title")
                                .add("value", "MyTitle " + badCharacter)
                        ));
        String jsonUpdateString = jsonUpdateObject.build().toString();
        Path jsonUpdatePath = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "update.json");
        java.nio.file.Files.write(jsonUpdatePath, jsonUpdateString.getBytes());
        Response addDataToBadData = UtilIT.updateFieldLevelDatasetMetadataViaNative(datasetPid, jsonUpdatePath.toString(), apiToken);
        addDataToBadData.prettyPrint();
        addDataToBadData.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

//        Response datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
//        datasetAsJson.prettyPrint();
//        String junkText = "Junk\flives\u0002here.";
//        String junkText = "Junk lives\u0002here.";
//        Path junkPath = Paths.get("/tmp/junk.txt");
//        Path junkPath = Paths.get("/tmp/junk-sot.txt");
//        java.nio.file.Files.write(junkPath, junkText.getBytes());
    }

}
