package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import org.junit.Test;
import org.junit.BeforeClass;
import java.util.UUID;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.hamcrest.CoreMatchers.equalTo;
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

        idOfUserToConvert = createUserToConvert.body().jsonPath().getLong("data.authenticatedUser.id");
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
        Response anon = UtilIT.listAuthenticatedUsers("");
        anon.prettyPrint();
        anon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response nonSuperuser = UtilIT.listAuthenticatedUsers(nonSuperuserApiToken);
        nonSuperuser.prettyPrint();
        nonSuperuser.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response superuser = UtilIT.listAuthenticatedUsers(superuserApiToken);
        superuser.prettyPrint();
        superuser.then().assertThat().statusCode(OK.getStatusCode());

    }

    @Test
    public void testConvertShibUserToBuiltin() throws Exception {

        String password = usernameOfUserToConvert;
        String newEmailAddressToUse = "builtin2shib." + UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";
        String data = emailOfUserToConvert + ":" + password + ":" + newEmailAddressToUse;

        Response builtinToShibAnon = UtilIT.migrateBuiltinToShib(data, "");
        builtinToShibAnon.prettyPrint();
        builtinToShibAnon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response makeShibUser = UtilIT.migrateBuiltinToShib(data, superuserApiToken);
        makeShibUser.prettyPrint();
        makeShibUser.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.affiliation", equalTo("TestShib Test IdP")
                );

        /**
         * @todo Write more failing tests such as expecting a non-OK response if
         * the Shib user has an invalid email address:
         * https://github.com/IQSS/dataverse/issues/2998
         */
        Response shibToBuiltinAnon = UtilIT.migrateShibToBuiltin(Long.MAX_VALUE, "", "");
        shibToBuiltinAnon.prettyPrint();
        shibToBuiltinAnon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response nonSuperuser = UtilIT.migrateShibToBuiltin(Long.MAX_VALUE, "", "");
        nonSuperuser.prettyPrint();
        nonSuperuser.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response infoOfUserToConvert = UtilIT.getAuthenticatedUser(usernameOfUserToConvert, superuserApiToken);
        infoOfUserToConvert.prettyPrint();

        String invalidEmailAddress = "invalidEmailAddress";
        Response invalidEmailFail = UtilIT.migrateShibToBuiltin(idOfUserToConvert, invalidEmailAddress, superuserApiToken);
        invalidEmailFail.prettyPrint();
        invalidEmailFail.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        String existingEmailAddress = "dataverse@mailinator.com";
        Response existingEmailFail = UtilIT.migrateShibToBuiltin(idOfUserToConvert, existingEmailAddress, superuserApiToken);
        existingEmailFail.prettyPrint();
        existingEmailFail.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        String newEmailAddress = UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";
        Response shouldWork = UtilIT.migrateShibToBuiltin(idOfUserToConvert, newEmailAddress, superuserApiToken);
        shouldWork.prettyPrint();
        shouldWork.then().assertThat().statusCode(OK.getStatusCode());

    }

    @Test
    public void testFindPermissonsOn() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = JsonPath.from(createDataverse.body().asString()).getString("data.alias");

        Response findPerms = UtilIT.findPermissionsOn(dataverseAlias, apiToken);
        findPerms.prettyPrint();
        findPerms.then().assertThat()
                .body("data.user", equalTo("@" + username))
                .statusCode(OK.getStatusCode());

        Response findRoleAssignee = UtilIT.findRoleAssignee("@" + username, apiToken);
        findRoleAssignee.prettyPrint();
        findRoleAssignee.then().assertThat()
                .body("data.title", equalTo(username + " " + username))
                .statusCode(OK.getStatusCode());

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

}
