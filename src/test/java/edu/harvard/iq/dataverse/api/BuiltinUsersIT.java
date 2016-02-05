package edu.harvard.iq.dataverse.api;

import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.UUID;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import static junit.framework.Assert.assertEquals;
import org.junit.Test;

public class BuiltinUsersIT {

    private static final Logger logger = Logger.getLogger(BuiltinUsersIT.class.getCanonicalName());

    private static final String builtinUserKey = "burrito";
    private static final String idKey = "id";
    private static final String usernameKey = "userName";
    private static final String emailKey = "email";

    @Test
    public void testUserId() {

        Response createUserResponse = createUser(getRandomUsername(), "firstName", "lastName", null);
        createUserResponse.prettyPrint();
        assertEquals(200, createUserResponse.getStatusCode());

        JsonPath createdUser = JsonPath.from(createUserResponse.body().asString());
        int userIdFromJsonCreateResponse = createdUser.getInt("data.user." + idKey);
        String username = createdUser.getString("data.user." + usernameKey);

        Response getUserResponse = getUserFromDatabase(username);
        getUserResponse.prettyPrint();
        assertEquals(200, getUserResponse.getStatusCode());

        JsonPath getUserJson = JsonPath.from(getUserResponse.body().asString());
        int userIdFromDatabase = getUserJson.getInt("data.id");

        Response deleteUserResponse = deleteUser(username);
        assertEquals(200, deleteUserResponse.getStatusCode());
        deleteUserResponse.prettyPrint();

        logger.info(userIdFromDatabase + " was the id from the database");
        logger.info(userIdFromJsonCreateResponse + " was the id from JSON response on create");
        /**
         * This test is expected to pass on a clean, fresh database but for an
         * unknown reason it fails when you load it up with a production
         * database from dataverse.harvard.edu. Why? This is what
         * https://github.com/IQSS/dataverse/issues/2418 is about.
         */
        assertEquals(userIdFromDatabase, userIdFromJsonCreateResponse);
    }

    @Test
    public void testUsernameAsEmailAddress() {
        String emailAddress = getRandomUsername() + "@mailinator.com";
        Response createUserResponse = createUser(emailAddress, "firstName", "lastName", emailAddress);
        createUserResponse.prettyPrint();
        assertEquals(200, createUserResponse.getStatusCode());
    }

    @Test
    public void testUsernameTooLong() {
        String longEmailAddress = "abcdefghijklmnopqrstuvwxyz"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "@mailinator.com";
        Response createUserResponse = createUser(longEmailAddress, "firstName", "lastName", longEmailAddress);
        createUserResponse.prettyPrint();
        assertEquals(400, createUserResponse.getStatusCode());
    }

    @Test
    public void testMultipleInvalidFields() {
        String longEmailAddress = "abcdefghijklmnopqrstuvwxyz"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "@mailinator.com";
        Response createUserResponse = createUser(longEmailAddress, "", "lastName", longEmailAddress + "junk");
        createUserResponse.prettyPrint();
        assertEquals(400, createUserResponse.getStatusCode());
    }

    private Response createUser(String username, String firstName, String lastName, String emailAddress) {
        String userAsJson = getUserAsJsonString(username, firstName, lastName, emailAddress);
        String password = getPassword(userAsJson);
        Response response = given()
                .body(userAsJson)
                .contentType(ContentType.JSON)
                .post("/api/builtin-users?key=" + builtinUserKey + "&password=" + password);
        return response;
    }

    private Response getUserFromDatabase(String username) {
        Response getUserResponse = given()
                .get("/api/admin/authenticatedUsers/" + username + "/");
        return getUserResponse;
    }

    private static Response deleteUser(String username) {
        Response deleteUserResponse = given()
                .delete("/api/admin/authenticatedUsers/" + username + "/");
        return deleteUserResponse;
    }

    private static String getRandomUsername() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String getUserAsJsonString(String username, String firstName, String lastName, String emailAddress) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(usernameKey, username);
        builder.add("firstName", firstName);
        builder.add("lastName", lastName);
        if (emailAddress != null) {
            builder.add(emailKey, emailAddress);
        } else {
            builder.add(emailKey, getEmailFromUserName(username));
        }
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

}
