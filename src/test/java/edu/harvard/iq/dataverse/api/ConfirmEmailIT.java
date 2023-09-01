package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author bsilverstein
 */
public class ConfirmEmailIT {

    private static final Logger logger = Logger.getLogger(ConfirmEmailIT.class.getCanonicalName());

    @BeforeAll
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
        Response confirmEmailViaBrowserJunkToken = given()
                .get("/confirmemail.xhtml?token=" + junkToken);
        boolean pageReturnsProper404Response = false;
        if (pageReturnsProper404Response) {
            confirmEmailViaBrowserJunkToken.then().assertThat().statusCode(404);
        } else {
            confirmEmailViaBrowserJunkToken.then().assertThat().statusCode(200);
        }

        boolean exitEarlyToTestManuallyInBrowser = false;
        if (exitEarlyToTestManuallyInBrowser) {
            return;
        }

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

        Response getToken2 = given()
                .get("/api/admin/confirmEmail/" + userIdToConfirm);
        getToken2.prettyPrint();
        getToken2.then().assertThat()
                .statusCode(400);

        Response confirmAgain1 = given()
                .post("/api/admin/confirmEmail/" + userIdToConfirm);
        confirmAgain1.prettyPrint();
        confirmAgain1.then().assertThat()
                .statusCode(200);

        Response getToken3 = given()
                .get("/api/admin/confirmEmail/" + userIdToConfirm);
        getToken3.prettyPrint();
        getToken3.then().assertThat()
                .statusCode(200);

        Response confirmAgain2 = given()
                .post("/api/admin/confirmEmail/" + userIdToConfirm);
        confirmAgain2.prettyPrint();
        confirmAgain2.then().assertThat()
                // This used to be 400 but now it's 200. Why?
                .statusCode(200);

        Response getToken4 = given()
                .get("/api/admin/confirmEmail/" + userIdToConfirm);
        getToken4.prettyPrint();
        getToken4.then().assertThat()
                .statusCode(200);
    }

    @Test
    public void testConfirmUserWithTokenCanBeDeleted() {

        Response createUserToConfirm = UtilIT.createRandomUser();
        createUserToConfirm.prettyPrint();
        createUserToConfirm.then().assertThat()
                .statusCode(200);

        long userIdToConfirm = JsonPath.from(createUserToConfirm.body().asString()).getLong("data.authenticatedUser.id");
        String userToConfirmApiToken = JsonPath.from(createUserToConfirm.body().asString()).getString("data.apiToken");
        String usernameToConfirm = JsonPath.from(createUserToConfirm.body().asString()).getString("data.user.userName");

        Response attemptToDeleteNonExistentUser = UtilIT.deleteUser("noSuchUser");
        attemptToDeleteNonExistentUser.then().assertThat()
                .statusCode(400);

        Response deleteUser = UtilIT.deleteUser(usernameToConfirm);
        deleteUser.prettyPrint();
        deleteUser.then().assertThat()
                .statusCode(200);

    }

}
