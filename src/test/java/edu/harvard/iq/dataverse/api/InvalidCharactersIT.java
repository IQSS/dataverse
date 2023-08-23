package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InvalidCharactersIT {

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testInvalidCharacters() throws IOException {
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
        String badCharacter = "(\f)"; // form feed (also \u000C)
//        badCharacter = "{\u0002}"; // start of text, reported problem with exports.

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
        addDataToBadData.then().assertThat()
                .statusCode(OK.getStatusCode())
                // The \f has been removed.
                .body("data.metadataBlocks.citation.fields[0].value", Matchers.equalTo("MyTitle ()"));


    }

}
