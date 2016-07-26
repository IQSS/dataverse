package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.UUID;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 *
 * @author bsilverstein
 *
 * Next steps:
 *
 * - Switch from POST to confirmemail.xhtml
 *
 * - Change first email to say, "please click to confirm your email." See
 * confirmUrl in ConfirmEmailInitResponse.
 *
 * - Show on user page if has been email confirmed (see mockups).
 *
 * - Call confirmEmailSvc.createToken when user is created in GUI.
 *
 * - Call confirmEmailSvc.createToken when user changes email address.
 *
 * - What effect should there be of not having a confirmed email? No emails are
 * sent? User can't create stuff?
 *
 * - Make getMinutesUntilConfirmEmailTokenExpires configurable. How long should
 * the default be? 24 hours?
 *
 */
public class ConfirmEmailIT {

    private static final Logger logger = Logger.getLogger(ConfirmEmailIT.class.getCanonicalName());

    private static final String builtinUserKey = "burrito";
    private static final String idKey = "id";
    private static final String usernameKey = "userName";
    private static final String emailKey = "email";

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testConfirm() {

        String email = null;
        /**
         * @todo Switch this to UtilIT.createRandomUser()
         */
        Response createUserToConfirm = createUser(getRandomUsername(), "firstName", "lastName", email);
        createUserToConfirm.prettyPrint();
        createUserToConfirm.then().assertThat()
                .statusCode(200);

        long userIdToConfirm = JsonPath.from(createUserToConfirm.body().asString()).getLong("data.authenticatedUser.id");
        String userToConfirmApiToken = JsonPath.from(createUserToConfirm.body().asString()).getString("data.apiToken");
        String usernameToConfirm = JsonPath.from(createUserToConfirm.body().asString()).getString("data.user.userName");

        Response createSuperuser = createUser(getRandomUsername(), "super", "user", email);
        createSuperuser.then().assertThat()
                .statusCode(200);
        String superuserUsername = JsonPath.from(createSuperuser.body().asString()).getString("data.user.userName");
        String superUserApiToken = JsonPath.from(createUserToConfirm.body().asString()).getString("data.apiToken");

        UtilIT.makeSuperUser(superuserUsername);
        createSuperuser.then().assertThat()
                .statusCode(200);

        String junkToken = "noSuchToken";
        Response noSuchToken = given()
                .post("/api/admin/confirmEmail/" + junkToken);
        noSuchToken.prettyPrint();
        noSuchToken.then().assertThat()
                .statusCode(404)
                .body("message", equalTo("Invalid token: " + junkToken));

        System.out.println("not confirmed yet");
        Response getUserWithoutConfirmedEmail = UtilIT.getAuthenticatedUser(usernameToConfirm, superUserApiToken);
        getUserWithoutConfirmedEmail.prettyPrint();
        getUserWithoutConfirmedEmail.then().assertThat()
                .statusCode(200)
                .body("data.emailLastConfirmed", nullValue());

        Response getToken = given()
                .get("/api/admin/confirmEmail/" + userIdToConfirm);
        getToken.prettyPrint();
        getToken.then().assertThat()
                .statusCode(200);
        String confirmEmailToken = JsonPath.from(getToken.body().asString()).getString("data.token");

        // This is simulating the user clicking the URL from their email client.
//        Response confirmEmail = given()
//                .post("/api/admin/confirmEmail/" + confirmEmailToken);
//        confirmEmail.prettyPrint();
//        confirmEmail.then().assertThat()
//                .statusCode(200);

        /**
         * @todo Switch over to this instead of the POST above, once it's
         * working.
         */
        Response confirmEmailViaBrowser = given()
                .get("/confirmemail.xhtml?token=" + confirmEmailToken);
        confirmEmailViaBrowser.then().assertThat()
                .statusCode(200);
        Response getUserWithConfirmedEmail = UtilIT.getAuthenticatedUser(usernameToConfirm, superUserApiToken);
        getUserWithConfirmedEmail.prettyPrint();
        getUserWithConfirmedEmail.then().assertThat()
                .statusCode(200)
                // Checking that it's 2016 or whatever. Not y3k compliant! 
                .body("data.emailLastConfirmed", startsWith("2"));
    }

    private Response createUser(String username, String firstName, String lastName, String email) {
        String userAsJson = getUserAsJsonString(username, firstName, lastName, email);
        String password = getPassword(userAsJson);
        Response response = given()
                .body(userAsJson)
                .contentType(ContentType.JSON)
                .post("/api/builtin-users?key=" + builtinUserKey + "&password=" + password);
        return response;
    }

    private static String getRandomUsername() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String getUserAsJsonString(String username, String firstName, String lastName, String email) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(usernameKey, username);
        builder.add("firstName", firstName);
        builder.add("lastName", lastName);
        if (email == null) {
            builder.add(emailKey, getEmailFromUserName(username));
        } else {
            builder.add(emailKey, email);
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
