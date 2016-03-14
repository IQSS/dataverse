package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static edu.harvard.iq.dataverse.api.UtilIT.API_TOKEN_HTTP_HEADER;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import org.junit.Test;
import org.junit.BeforeClass;
import static com.jayway.restassured.RestAssured.given;
import java.util.UUID;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;

public class AdminIT {

    private static String nonSuperuserUsername;
    private static String nonSuperuserApiToken;

    private static String superuserUsername;
    private static String superuserApiToken;

    private static Long idOfUserToConvert;
    private static String usernameOfUserToConvert;
    private static String emailOfUserToConvert;

    @BeforeClass
    public static void setUp() {

        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response createNonSuperuser = UtilIT.createRandomUser();
        nonSuperuserUsername = UtilIT.getUsernameFromResponse(createNonSuperuser);
        nonSuperuserApiToken = UtilIT.getApiTokenFromResponse(createNonSuperuser);

        Response createUserToConvert = UtilIT.createRandomUser();
        createUserToConvert.prettyPrint();

        idOfUserToConvert = createUserToConvert.body().jsonPath().getLong("data.user.id");
        emailOfUserToConvert = createUserToConvert.body().jsonPath().getString("data.user.email");
        usernameOfUserToConvert = UtilIT.getUsernameFromResponse(createUserToConvert);

        Response createSuperuser = UtilIT.createRandomUser();
        superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testListAuthenticatedUsers() throws Exception {
        Response anon = listAuthenticatedUsers("");
        anon.prettyPrint();
        anon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response nonSuperuser = listAuthenticatedUsers(nonSuperuserApiToken);
        nonSuperuser.prettyPrint();
        nonSuperuser.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response superuser = listAuthenticatedUsers(superuserApiToken);
        superuser.prettyPrint();
        superuser.then().assertThat().statusCode(OK.getStatusCode());

    }

    @Test
    public void testConvertShibUserToBuiltin() throws Exception {

        String password = usernameOfUserToConvert;
        String data = emailOfUserToConvert + ":" + password;

        Response builtinToShibAnon = migrateBuiltinToShib(data, "");
        builtinToShibAnon.prettyPrint();
        builtinToShibAnon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response makeShibUser = migrateBuiltinToShib(data, superuserApiToken);
        makeShibUser.prettyPrint();
        /**
         * @todo Expect a non-OK response if the Shib user has an invalid email
         * address: https://github.com/IQSS/dataverse/issues/2998
         */
        makeShibUser.then().assertThat().statusCode(OK.getStatusCode());

        Response shibToBuiltinAnon = migrateShibToBuiltin(Long.MAX_VALUE, "", "");
        shibToBuiltinAnon.prettyPrint();
        shibToBuiltinAnon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response nonSuperuser = migrateShibToBuiltin(Long.MAX_VALUE, "", "");
        nonSuperuser.prettyPrint();
        nonSuperuser.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response infoOfUserToConvert = getAuthenticatedUser(usernameOfUserToConvert, superuserApiToken);
        infoOfUserToConvert.prettyPrint();
        /**
         * https://github.com/IQSS/dataverse/issues/2418 is cropping up again in
         * that if we try to reuse this ID below it's the wrong ID (off by one).
         * Weird and troubling.
         */
        idOfUserToConvert = infoOfUserToConvert.body().jsonPath().getLong("data.id");

        String invalidEmailAddress = "invalidEmailAddress";
        Response invalidEmailFail = migrateShibToBuiltin(idOfUserToConvert, invalidEmailAddress, superuserApiToken);
        invalidEmailFail.prettyPrint();
        invalidEmailFail.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        String existingEmailAddress = "dataverse@mailinator.com";
        Response existingEmailFail = migrateShibToBuiltin(idOfUserToConvert, existingEmailAddress, superuserApiToken);
        existingEmailFail.prettyPrint();
        existingEmailFail.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        String newEmailAddress = UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";
        Response shouldWork = migrateShibToBuiltin(idOfUserToConvert, newEmailAddress, superuserApiToken);
        shouldWork.prettyPrint();
        shouldWork.then().assertThat().statusCode(OK.getStatusCode());

    }

    @AfterClass
    public static void tearDownClass() {
        boolean disabled = false;

        if (disabled) {
            return;
        }

        Response deleteNonSuperuser = UtilIT.deleteUser(nonSuperuserUsername);
        assertEquals(200, deleteNonSuperuser.getStatusCode());

        Response deleteUserToConvert = UtilIT.deleteUser(usernameOfUserToConvert);
        assertEquals(200, deleteUserToConvert.getStatusCode());

        Response deleteSuperuser = UtilIT.deleteUser(superuserUsername);
        assertEquals(200, deleteSuperuser.getStatusCode());

    }

    /**
     * @todo: Once all these methods have stabilized, move to UtilIT.java
     */
    static Response listAuthenticatedUsers(String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/authenticatedUsers");
        return response;
    }

    static Response getAuthenticatedUser(String userIdentifier, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/authenticatedUsers/" + userIdentifier);
        return response;
    }

    static Response migrateShibToBuiltin(Long userIdToConvert, String newEmailAddress, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(newEmailAddress)
                .put("/api/admin/authenticatedUsers/id/" + userIdToConvert + "/convertShibToBuiltIn");
        return response;
    }

    static Response migrateBuiltinToShib(String data, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(data)
                .put("/api/test/user/convert/builtin2shib");
        return response;
    }

}
