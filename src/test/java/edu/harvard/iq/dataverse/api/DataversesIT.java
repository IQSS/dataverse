package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.Dataverse;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import javax.ws.rs.core.Response.Status;
import org.junit.BeforeClass;
import org.junit.Test;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;

public class DataversesIT {

    private static final Logger logger = Logger.getLogger(DataversesIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testAttemptToCreateDuplicateAlias() throws Exception {

        Response createUser = UtilIT.createRandomUser();
//        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken);
        if (createDataverse1Response.getStatusCode() != 201) {
            // purposefully using println here to the error shows under "Test Results" in Netbeans
            System.out.println("A workspace for testing (a dataverse) couldn't be created in the root dataverse. The output was:\n\n" + createDataverse1Response.body().asString());
            System.out.println("\nPlease ensure that users can created dataverses in the root in order for this test to run.");
        } else {
            createDataverse1Response.prettyPrint();
        }
        assertEquals(201, createDataverse1Response.getStatusCode());

        String dataverseAlias1 = UtilIT.getAliasFromResponse(createDataverse1Response);
        String dataverseAlias2 = dataverseAlias1.toUpperCase();
        logger.info("Attempting to creating dataverse with alias '" + dataverseAlias2 + "' (uppercase version of existing '" + dataverseAlias1 + "' dataverse, should fail)...");
        String category = null;
        Response attemptToCreateDataverseWithDuplicateAlias = UtilIT.createDataverse(dataverseAlias2, category, apiToken);
        attemptToCreateDataverseWithDuplicateAlias.prettyPrint();
        assertEquals(403, attemptToCreateDataverseWithDuplicateAlias.getStatusCode());

        logger.info("Deleting dataverse " + dataverseAlias1);
        Response deleteDataverse1Response = UtilIT.deleteDataverse(dataverseAlias1, apiToken);
        deleteDataverse1Response.prettyPrint();
        assertEquals(200, deleteDataverse1Response.getStatusCode());

        logger.info("Checking response code for attempting to delete a non-existent dataverse.");
        Response attemptToDeleteDataverseThatShouldNotHaveBeenCreated = UtilIT.deleteDataverse(dataverseAlias2, apiToken);
        attemptToDeleteDataverseThatShouldNotHaveBeenCreated.prettyPrint();
        assertEquals(404, attemptToDeleteDataverseThatShouldNotHaveBeenCreated.getStatusCode());

    }

    @Test
    public void testDataverseCategory() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseWithoutCategory = UtilIT.createRandomDataverse(apiToken);
        createDataverseWithoutCategory.prettyPrint();
        createDataverseWithoutCategory.then().assertThat()
                .body("data.dataverseType", equalTo("UNCATEGORIZED"))
                .statusCode(Status.CREATED.getStatusCode());

        String alias1 = UtilIT.getRandomIdentifier();
        String category1 = Dataverse.DataverseType.LABORATORY.toString();
        Response createDataverseWithCategory = UtilIT.createDataverse(alias1, category1, apiToken);
        createDataverseWithCategory.prettyPrint();
        createDataverseWithCategory.then().assertThat()
                .body("data.dataverseType", equalTo("LABORATORY"))
                .statusCode(Status.CREATED.getStatusCode());

        String alias2 = UtilIT.getRandomIdentifier();
        String madeUpCategory = "madeUpCategory";
        Response createDataverseWithInvalidCategory = UtilIT.createDataverse(alias2, madeUpCategory, apiToken);
        createDataverseWithInvalidCategory.prettyPrint();
        createDataverseWithInvalidCategory.then().assertThat()
                .body("data.dataverseType", equalTo("UNCATEGORIZED"))
                .statusCode(Status.CREATED.getStatusCode());

        String alias3 = UtilIT.getRandomIdentifier();
        String category3 = Dataverse.DataverseType.LABORATORY.toString().toLowerCase();
        Response createDataverseWithLowerCaseCategory = UtilIT.createDataverse(alias3, category3, apiToken);
        createDataverseWithLowerCaseCategory.prettyPrint();
        createDataverseWithLowerCaseCategory.then().assertThat()
                .body("data.dataverseType", equalTo("UNCATEGORIZED"))
                .statusCode(Status.CREATED.getStatusCode());

    }

    @Test
    public void testMinimalDataverse() throws FileNotFoundException {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        JsonObject dvJson;
        FileReader reader = new FileReader("doc/sphinx-guides/source/_static/api/dataverse-minimal.json");
        dvJson = Json.createReader(reader).readObject();
        Response create = UtilIT.createDataverse(dvJson, apiToken);
        create.prettyPrint();
        create.then().assertThat().statusCode(CREATED.getStatusCode());
        Response deleteDataverse = UtilIT.deleteDataverse("science", apiToken);
        deleteDataverse.prettyPrint();
        deleteDataverse.then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testNotEnoughJson() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response createFail = UtilIT.createDataverse(Json.createObjectBuilder().add("name", "notEnough").add("alias", "notEnough").build(), apiToken);
        createFail.prettyPrint();
        createFail.then().assertThat()
                /**
                 * @todo We really don't want Dataverse to throw a 500 error
                 * when not enough JSON is supplied to create a dataverse.
                 */
                .statusCode(INTERNAL_SERVER_ERROR.getStatusCode());
    }

}
