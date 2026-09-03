package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.Matchers.*;

/**
 * @author Vera Clemens (ZB MED)
 */
public class DatasetRelationTypesIT {

    private static String apiTokenSuperuser;
    private static String apiTokenNormalUser;

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        Response createSuperuser = UtilIT.createRandomUser();
        createSuperuser.then().assertThat().statusCode(OK.getStatusCode());
        String usernameSuperuser = UtilIT.getUsernameFromResponse(createSuperuser);
        apiTokenSuperuser = UtilIT.getApiTokenFromResponse(createSuperuser);
        UtilIT.setSuperuserStatus(usernameSuperuser, true).then().assertThat().statusCode(OK.getStatusCode());

        Response createNormalUser = UtilIT.createRandomUser();
        createNormalUser.then().assertThat().statusCode(OK.getStatusCode());
        apiTokenNormalUser = UtilIT.getApiTokenFromResponse(createNormalUser);
    }

    @Test
    public void testRelationTypeCRUD() {
        String name = "testRelation" + UtilIT.getRandomString(6);
        String displayName = "Test Relation " + UtilIT.getRandomString(6);
        String description = "Test description";

        // Create relation type without inverse
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("name", name)
                .add("displayName", displayName)
                .add("description", description);

        UtilIT.addDatasetRelationType(builder.build().toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.setDefaultDatasetRelationType(name, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.getDefaultDatasetRelationType(apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.name", equalTo(name))
                .body("data.default", equalTo(true));

        // Get relation type
        UtilIT.getDatasetRelationType(name, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.name", equalTo(name))
                .body("data.displayName", equalTo(displayName))
                .body("data.description", equalTo(description))
                .body("data.default", equalTo(true));

        String name2 = "testRelation2" + UtilIT.getRandomString(6);
        String displayName2 = "Test Relation 2 " + UtilIT.getRandomString(6);
        String description2 = "Test description2";

        // Create relation type which is inverse of itself
        JsonObjectBuilder builder2 = Json.createObjectBuilder()
                .add("name", name2)
                .add("displayName", displayName2)
                .add("description", description2)
                .add("inverse", Json.createObjectBuilder().add("name", name2));

        UtilIT.addDatasetRelationType(builder2.build().toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Get relation type
        UtilIT.getDatasetRelationType(name2, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.name", equalTo(name2))
                .body("data.displayName", equalTo(displayName2))
                .body("data.description", equalTo(description2))
                .body("data.inverse.name", equalTo(name2))
                .body("data.inverse.displayName", equalTo(displayName2))
                .body("data.inverse.description", equalTo(description2));

        String name3 = "testRelation3" + UtilIT.getRandomString(6);
        String displayName3 = "Test Relation 3 " + UtilIT.getRandomString(6);
        String description3 = "Test description3";
        String inverseName3 = "testInverseRelation3" + UtilIT.getRandomString(6);
        String inverseDisplayName3 = "Test Inverse Relation 3 " + UtilIT.getRandomString(6);
        String inverseDescription3 = "Test inverse description3";

        // Create relation type with an inverse relation type
        JsonObjectBuilder builder3 = Json.createObjectBuilder()
                .add("name", name3)
                .add("displayName", displayName3)
                .add("description", description3)
                .add("inverse", Json.createObjectBuilder().add("name", inverseName3)
                                                                 .add("displayName", inverseDisplayName3)
                                                                 .add("description", inverseDescription3));

        UtilIT.addDatasetRelationType(builder3.build().toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Get relation types
        UtilIT.getDatasetRelationType(name3, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.name", equalTo(name3))
                .body("data.displayName", equalTo(displayName3))
                .body("data.description", equalTo(description3))
                .body("data.inverse.name", equalTo(inverseName3))
                .body("data.inverse.displayName", equalTo(inverseDisplayName3))
                .body("data.inverse.description", equalTo(inverseDescription3));

        UtilIT.getDatasetRelationType(inverseName3, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.name", equalTo(inverseName3))
                .body("data.displayName", equalTo(inverseDisplayName3))
                .body("data.description", equalTo(inverseDescription3))
                .body("data.inverse.name", equalTo(name3))
                .body("data.inverse.displayName", equalTo(displayName3))
                .body("data.inverse.description", equalTo(description3));

        // List all relation types
        UtilIT.listDatasetRelationTypes(apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.name", hasItem(name))
                .body("data.name", hasItem(name2))
                .body("data.name", hasItem(name3))
                .body("data.name", hasItem(inverseName3));

        // The default type cannot be deleted.
        UtilIT.deleteDatasetRelationType(name, apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        // Delete relation types after selecting another default.
        UtilIT.setDefaultDatasetRelationType(name2, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.deleteDatasetRelationType(name, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.setDefaultDatasetRelationType(name3, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.deleteDatasetRelationType(name2, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.setDefaultDatasetRelationType(inverseName3, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.deleteDatasetRelationType(name3, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.getDefaultDatasetRelationType(apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.name", equalTo(inverseName3))
                .body("data.default", equalTo(true));

        // Verify deleted
        UtilIT.getDatasetRelationType(name, apiTokenSuperuser)
                .then().assertThat().statusCode(NOT_FOUND.getStatusCode());
        UtilIT.getDatasetRelationType(name2, apiTokenSuperuser)
                .then().assertThat().statusCode(NOT_FOUND.getStatusCode());
        UtilIT.getDatasetRelationType(name3, apiTokenSuperuser)
                .then().assertThat().statusCode(NOT_FOUND.getStatusCode());
        UtilIT.getDatasetRelationType(inverseName3, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testCreateRelationTypeValidation() {
        // Missing name
        JsonObjectBuilder noName = Json.createObjectBuilder()
                .add("displayName", "No Name");
        UtilIT.addDatasetRelationType(noName.build().toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        // Missing displayName
        JsonObjectBuilder noDisplayName = Json.createObjectBuilder()
                .add("name", "noDisplayName");
        UtilIT.addDatasetRelationType(noDisplayName.build().toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
        
        // Empty name
        JsonObjectBuilder emptyName = Json.createObjectBuilder()
                .add("name", "")
                .add("displayName", "Empty Name");
        UtilIT.addDatasetRelationType(emptyName.build().toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testRelationTypePermissions() {
        String name = "permTest" + UtilIT.getRandomString(6);
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("name", name)
                .add("displayName", "Perm Test" + UtilIT.getRandomString(6));
        String jsonIn = builder.build().toString();

        // Create as normal user fails
        UtilIT.addDatasetRelationType(jsonIn, apiTokenNormalUser)
                .then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        // Create as superuser succeeds
        UtilIT.addDatasetRelationType(jsonIn, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        String defaultName = "permDefault" + UtilIT.getRandomString(6);
        UtilIT.addDatasetRelationType(Json.createObjectBuilder()
                        .add("name", defaultName)
                        .add("displayName", "Perm Default " + UtilIT.getRandomString(6))
                        .build().toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.setDefaultDatasetRelationType(name, apiTokenNormalUser)
                .then().assertThat().statusCode(FORBIDDEN.getStatusCode());
        UtilIT.setDefaultDatasetRelationType(defaultName, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Delete as normal user fails
        UtilIT.deleteDatasetRelationType(name, apiTokenNormalUser)
                .then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        // Delete as superuser succeeds
        UtilIT.deleteDatasetRelationType(name, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testDeleteRelationTypeFailureWhenInUse() {
        String typeName = "inUseTest" + UtilIT.getRandomString(6);
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("name", typeName)
                .add("displayName", "In Use Test" + UtilIT.getRandomString(6));

        UtilIT.addDatasetRelationType(builder.build().toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Create datasets and a relation of this type
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        String relationJson = Json.createObjectBuilder()
                .add("relatedDatasetPid", pidB)
                .add("relationType", Json.createObjectBuilder().add("name", typeName))
                .build().toString();

        UtilIT.addDatasetRelation(pidA, relationJson, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Try to delete the relation type while it's in use
        // Expected behavior: should fail because of existing relations
        UtilIT.deleteDatasetRelationType(typeName, apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        // Cleanup: remove relation first
        // Need the relation ID
        Response listResponse = UtilIT.listDatasetRelations(pidA, apiTokenSuperuser);
        int relationId = listResponse.jsonPath().getInt("data[0].id");
        UtilIT.deleteDatasetRelation(pidA, relationId, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        String defaultName = "defaultAfterInUse" + UtilIT.getRandomString(6);
        UtilIT.addDatasetRelationType(Json.createObjectBuilder()
                        .add("name", defaultName)
                        .add("displayName", "Default After In Use " + UtilIT.getRandomString(6))
                        .build().toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.setDefaultDatasetRelationType(defaultName, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Now deleting the relation type should work
        UtilIT.deleteDatasetRelationType(typeName, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
    }
}
