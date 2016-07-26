package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 *
 * @author bsilverstein
 *
 * Next steps:
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

        Response createUserToConfirm = UtilIT.createRandomUser();
        createUserToConfirm.prettyPrint();
        createUserToConfirm.then().assertThat()
                .statusCode(200);

        long userIdToConfirm = JsonPath.from(createUserToConfirm.body().asString()).getLong("data.authenticatedUser.id");
        String userToConfirmApiToken = JsonPath.from(createUserToConfirm.body().asString()).getString("data.apiToken");
        String usernameToConfirm = JsonPath.from(createUserToConfirm.body().asString()).getString("data.user.userName");

        Response createSuperuser = UtilIT.createRandomUser();
        createSuperuser.then().assertThat()
                .statusCode(200);
        String superuserUsername = JsonPath.from(createSuperuser.body().asString()).getString("data.user.userName");
        String superUserApiToken = JsonPath.from(createUserToConfirm.body().asString()).getString("data.apiToken");

        UtilIT.makeSuperUser(superuserUsername);
        createSuperuser.then().assertThat()
                .statusCode(200);

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

        String junkToken = "noSuchToken";
//        Response noSuchToken = given()
//                .post("/api/admin/confirmEmail/" + junkToken);
//        noSuchToken.prettyPrint();
//        noSuchToken.then().assertThat()
//                .statusCode(404)
//                .body("message", equalTo("Invalid token: " + junkToken));
        Response confirmEmailViaBrowserJunkToken = given()
                .get("/confirmemail.xhtml?token=" + junkToken);
        confirmEmailViaBrowserJunkToken.then().assertThat()
                /**
                 * @todo Make this a 404 rather than a 200. Then remove this
                 * commented code above (the old POST method).
                 */
                .statusCode(200);

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

}
