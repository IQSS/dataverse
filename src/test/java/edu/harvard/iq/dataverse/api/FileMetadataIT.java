package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class FileMetadataIT {

    private static ClassLoader classLoader = FileMetadataIT.class.getClassLoader();

    // test properties
    private static String testName;
    private static String token;
    private static final String builtinUserKey = "burrito";
    private static final String keyString = "X-Dataverse-key";

    // dataset properties
    private static int dsId;
    private static int dsIdFirst;

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @BeforeEach
    public void setUpDataverse() {
        try {
            // create random test name
            // "abc" added so the name/alias isn't an integer, a requirement for dataverse creation.
            // Longer term, consider switching to UtilIT.getRandomDvAlias (and rewriting these tests).
            testName = "abc" + UUID.randomUUID().toString().substring(0, 8);
            // create user and set token
            token = given()
                    .body("{"
                            + "   \"userName\": \"" + testName + "\","
                            + "   \"firstName\": \"" + testName + "\","
                            + "   \"lastName\": \"" + testName + "\","
                            + "   \"email\": \"" + testName + "@mailinator.com\""
                            + "}")
                    .contentType(ContentType.JSON)
                    .request()
                    .post("/api/builtin-users/secret/" + builtinUserKey)
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.apiToken");
            System.out.println("TOKEN: " + token);
            // create dataverse
            given().body("{"
                    + "    \"name\": \"" + testName + "\","
                    + "    \"alias\": \"" + testName + "\","
                    + "    \"affiliation\": \"Test-Driven University\","
                    + "    \"dataverseContacts\": ["
                    + "        {"
                    + "            \"contactEmail\": \"test@example.com\""
                    + "        },"
                    + "        {"
                    + "            \"contactEmail\": \"test@example.org\""
                    + "        }"
                    + "    ],"
                    + "    \"permissionRoot\": true,"
                    + "    \"description\": \"test Description.\""
                    + "}")
                    .contentType(ContentType.JSON).request()
                    .post("/api/dataverses/:root?key=" + token)
                    .then().assertThat().statusCode(201);
            System.out.println("DATAVERSE: " + RestAssured.baseURI + "/dataverse/" + testName);
        } catch (Exception e) {
            fail("Error setting up test dataverse: " + e.getMessage(), e);
        }
    }

    @AfterAll
    public static void tearDownClass() {
        RestAssured.reset();
    }

    @AfterEach
    public void tearDownDataverse() {
        try {
            // delete dataset
            given().header(keyString, token)
                    .delete("/api/datasets/" + dsIdFirst)
                    .then().assertThat().statusCode(200);
            given().header(keyString, token)
                    .delete("/api/datasets/" + dsId)
                    .then().assertThat().statusCode(200);
            // delete dataverse
            given().header(keyString, token)
                    .delete("/api/dataverses/" + testName)
                    .then().assertThat().statusCode(200);
            // delete user
            given().header(keyString, token)
                    .delete("/api/admin/authenticatedUsers/" + testName + "/")
                    .then().assertThat().statusCode(200);
        } catch (Exception e) {
            System.out.println("Error tearing down test Dataverse: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Create a dataset with files and directoryLabels using json.
     */
    @Test
    public void testJsonParserWithDirectoryLabels() {
        try {
            //SEK 4/14/2020 need to be super user to add a dataset with files
            UtilIT.makeSuperUser(testName).then().assertThat().statusCode(OK.getStatusCode());

            // try to create a dataset with directory labels that contain both leading and trailing file separators
            //Should work now
            Response shouldNotFailDueToLeadingAndTrailingSeparators = given()
                    .header(keyString, token)
                    .body(IOUtils.toString(classLoader.getResourceAsStream(
                            "json/complete-dataset-with-files-invalid-directory-labels.json")))
                    .contentType("application/json")
                    .post("/api/dataverses/" + testName + "/datasets");
            shouldNotFailDueToLeadingAndTrailingSeparators.prettyPrint();
            dsIdFirst = shouldNotFailDueToLeadingAndTrailingSeparators.then().assertThat()
                    // Note that the JSON under test actually exercises leading too but only the first (trailing) is exercised here.
                    .statusCode(201).extract().jsonPath().getInt("data.id");

            // create dataset and set id
            System.out.println("Creating dataset....");
            dsId = given()
                    .header(keyString, token)
                    .body(IOUtils.toString(classLoader.getResourceAsStream("json/complete-dataset-with-files.json")))
                    .contentType("application/json")
                    .post("/api/dataverses/" + testName + "/datasets")
                    .then().assertThat().statusCode(201)
                    .extract().jsonPath().getInt("data.id");
            System.out.println("Dataset created with id " + dsId);
            // check that both directory labels were persisted
            String dirLabel1 = given()
                    .header(keyString, token)
                    .get("/api/datasets/" + dsId)
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.latestVersion.files[0].directoryLabel");
            System.out.println("directoryLabel 1: " + dirLabel1);
            assertEquals("data/subdir1", dirLabel1);

            Response response = given()
                    .header(keyString, token)
                    .get("/api/datasets/" + dsId);
            response.prettyPrint();
            response.then().assertThat()
                    .body("data.latestVersion.files[1].directoryLabel", equalTo("data/subdir2"))
                    .body("data.latestVersion.files[1].categories[0]", equalTo("Data"))
                    .statusCode(200);

        } catch (Exception e) {
            fail("Error testJsonParserWithDirectoryLabels: " + e.getMessage(), e);
        }
    }

}
