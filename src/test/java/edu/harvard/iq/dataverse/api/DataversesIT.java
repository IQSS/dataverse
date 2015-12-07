package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.UUID;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static com.jayway.restassured.RestAssured.given;
import static junit.framework.Assert.assertEquals;

public class DataversesIT {

    private static final Logger logger = Logger.getLogger(DataversesIT.class.getCanonicalName());

    private static final String builtinUserKey = "burrito";
    private static final String keyString = "X-Dataverse-key";
    private static String EMPTY_STRING = "";
    private static final String idKey = "id";
    private static final String apiTokenKey = "apiToken";
    private static final String usernameKey = "userName";
    private static final String emailKey = "email";

    private static String username1;
    private static String apiToken1;
    private static String dataverseAlias1;
    private static String dataverseAlias2;

    @BeforeClass
    public static void setUpClass() {

        String specifiedUri = System.getProperty("dataverse.test.baseurl");
        if (specifiedUri != null) {
            RestAssured.baseURI = specifiedUri;
        } else {
            RestAssured.baseURI = "http://localhost:8080";
        }
        logger.info("Base URL for tests: " + specifiedUri);

        Response createUserResponse = createUser(getRandomUsername(), "firstName", "lastName");
        createUserResponse.prettyPrint();
        assertEquals(200, createUserResponse.getStatusCode());

        JsonPath createdUser1 = JsonPath.from(createUserResponse.body().asString());
        apiToken1 = createdUser1.getString("data." + apiTokenKey);
        username1 = createdUser1.getString("data.user." + usernameKey);

    }

    @AfterClass
    public static void tearDownClass() {
        boolean disabled = false;

        if (disabled) {
            return;
        }

        Response deleteDataverse1Response = deleteDataverse(dataverseAlias1, apiToken1);
//        deleteDataverse1Response.prettyPrint();
        Response deleteDataverse2Response = deleteDataverse(dataverseAlias2, apiToken1);
//        deleteDataverse2Response.prettyPrint();

        Response deleteUser1Response = deleteUser(username1);
        deleteUser1Response.prettyPrint();
        assertEquals(200, deleteUser1Response.getStatusCode());

    }

    @Test
    public void testAttemptToCreateDuplicateAlias() throws Exception {
        dataverseAlias1 = getRandomIdentifier();
        logger.info("Creating dataverse with alias '" + dataverseAlias1 + "'...");
        Response createDataverse1Response = createDataverse(dataverseAlias1, apiToken1);
        createDataverse1Response.prettyPrint();
        dataverseAlias2 = dataverseAlias1.toUpperCase();
        logger.info("Attempting to creating dataverse with alias '" + dataverseAlias2 + "' (uppercase version of existing '" + dataverseAlias1 + "' dataverse, should fail)...");
        Response createDataverse2Response = createDataverse(dataverseAlias2, apiToken1);
        createDataverse2Response.prettyPrint();
        assertEquals(400, createDataverse2Response.getStatusCode());
    }

    private static Response createUser(String username, String firstName, String lastName) {
        logger.info("Creating test user...");
        String userAsJson = getUserAsJsonString(username, firstName, lastName);
        String password = getPassword(userAsJson);
        Response response = given()
                .body(userAsJson)
                .contentType(ContentType.JSON)
                .post("/api/builtin-users?key=" + builtinUserKey + "&password=" + password);
        return response;
    }

    private static String getUserAsJsonString(String username, String firstName, String lastName) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(usernameKey, username);
        builder.add("firstName", firstName);
        builder.add("lastName", lastName);
        builder.add(emailKey, getEmailFromUserName(username));
        String userAsJson = builder.build().toString();
        logger.fine("User to create: " + userAsJson);
        return userAsJson;
    }

    private static String getPassword(String jsonStr) {
        String password = JsonPath.from(jsonStr).get(usernameKey);
        return password;
    }

    private static String getEmailFromUserName(String username) {
        return username + "@mailinator.com";
    }

    private static Response deleteUser(String username) {
        Response deleteUserResponse = given()
                .delete("/api/admin/authenticatedUsers/" + username + "/");
        return deleteUserResponse;
    }

    private static Response createDataverse(String alias, String apiToken) {
        JsonArrayBuilder contactArrayBuilder = Json.createArrayBuilder();
        contactArrayBuilder.add(Json.createObjectBuilder().add("contactEmail", getEmailFromUserName(getRandomIdentifier())));
        JsonArrayBuilder subjectArrayBuilder = Json.createArrayBuilder();
        subjectArrayBuilder.add("Other");
        JsonObject dvData = Json.createObjectBuilder()
                .add("alias", alias)
                .add("name", alias)
                .add("dataverseContacts", contactArrayBuilder)
                .add("dataverseSubjects", subjectArrayBuilder)
                .build();
        Response createDataverseResponse = given()
                .body(dvData.toString()).contentType(ContentType.JSON)
                .when().post("/api/dataverses/:root?key=" + apiToken);
        return createDataverseResponse;
    }

    private static String getRandomUsername() {
        return "user" + getRandomIdentifier().substring(0, 8);
    }

    private static String getRandomIdentifier() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static Response deleteDataverse(String doomed, String apiToken) {
        return given().delete("/api/dataverses/" + doomed + "?key=" + apiToken);
    }

}
