package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import org.junit.Test;
import org.junit.BeforeClass;
import java.util.UUID;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;

public class AdminIT {

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testListAuthenticatedUsers() throws Exception {
        Response anon = UtilIT.listAuthenticatedUsers("");
        anon.prettyPrint();
        anon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response createNonSuperuser = UtilIT.createRandomUser();
        String nonSuperuserUsername = UtilIT.getUsernameFromResponse(createNonSuperuser);
        String nonSuperuserApiToken = UtilIT.getApiTokenFromResponse(createNonSuperuser);

        Response nonSuperuser = UtilIT.listAuthenticatedUsers(nonSuperuserApiToken);
        nonSuperuser.prettyPrint();
        nonSuperuser.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response superuser = UtilIT.listAuthenticatedUsers(superuserApiToken);
        superuser.prettyPrint();
        superuser.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteNonSuperuser = UtilIT.deleteUser(nonSuperuserUsername);
        assertEquals(200, deleteNonSuperuser.getStatusCode());

        Response deleteSuperuser = UtilIT.deleteUser(superuserUsername);
        assertEquals(200, deleteSuperuser.getStatusCode());

    }

    @Test
    public void testConvertShibUserToBuiltin() throws Exception {

        Response createUserToConvert = UtilIT.createRandomUser();
        createUserToConvert.prettyPrint();

        long idOfUserToConvert = createUserToConvert.body().jsonPath().getLong("data.authenticatedUser.id");
        String emailOfUserToConvert = createUserToConvert.body().jsonPath().getString("data.user.email");
        String usernameOfUserToConvert = UtilIT.getUsernameFromResponse(createUserToConvert);

        String password = usernameOfUserToConvert;
        String newEmailAddressToUse = "builtin2shib." + UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";
        String data = emailOfUserToConvert + ":" + password + ":" + newEmailAddressToUse;

        Response builtinToShibAnon = UtilIT.migrateBuiltinToShib(data, "");
        builtinToShibAnon.prettyPrint();
        builtinToShibAnon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response getAuthProviders = UtilIT.getAuthProviders(superuserApiToken);
        getAuthProviders.prettyPrint();
        if (!getAuthProviders.body().asString().contains(BuiltinAuthenticationProvider.PROVIDER_ID)) {
            System.out.println("Can't proceed with test without builtin provider.");
            return;
        }

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
        existingEmailFail.then().assertThat()
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("User id " + idOfUserToConvert
                        + " could not be converted from Shibboleth to BuiltIn. Details from Exception: java.lang.Exception: User id "
                        + idOfUserToConvert + " (@"
                        + usernameOfUserToConvert
                        + ") cannot be converted from remote to BuiltIn because the email address dataverse@mailinator.com is already in use by user id 1 (@dataverseAdmin). "))
                .statusCode(BAD_REQUEST.getStatusCode());

        String newEmailAddress = UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";
        Response shouldWork = UtilIT.migrateShibToBuiltin(idOfUserToConvert, newEmailAddress, superuserApiToken);
        shouldWork.prettyPrint();
        shouldWork.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteUserToConvert = UtilIT.deleteUser(usernameOfUserToConvert);
        assertEquals(200, deleteUserToConvert.getStatusCode());

        Response deleteSuperuser = UtilIT.deleteUser(superuserUsername);
        assertEquals(200, deleteSuperuser.getStatusCode());

    }

    @Test
    public void testConvertOAuthUserToBuiltin() throws Exception {

        System.out.println("BEGIN testConvertOAuthUserToBuiltin");

        Response createUserToConvert = UtilIT.createRandomUser();
        createUserToConvert.prettyPrint();

        long idOfUserToConvert = createUserToConvert.body().jsonPath().getLong("data.authenticatedUser.id");
        String emailOfUserToConvert = createUserToConvert.body().jsonPath().getString("data.user.email");
        String usernameOfUserToConvert = UtilIT.getUsernameFromResponse(createUserToConvert);

        String password = usernameOfUserToConvert;
        String newEmailAddressToUse = "builtin2shib." + UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";

        GitHubOAuth2AP github = new GitHubOAuth2AP(null, null);
        String providerIdToConvertTo = github.getId();
        String newPersistentUserIdInLookupTable = UUID.randomUUID().toString().substring(0, 8);
        String data = emailOfUserToConvert + ":" + password + ":" + newEmailAddressToUse + ":" + providerIdToConvertTo + ":" + newPersistentUserIdInLookupTable;

        System.out.println("data: " + data);
        Response builtinToOAuthAnon = UtilIT.migrateBuiltinToOAuth(data, "");
        builtinToOAuthAnon.prettyPrint();
        builtinToOAuthAnon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response getAuthProviders = UtilIT.getAuthProviders(superuserApiToken);
        getAuthProviders.prettyPrint();
        if (!getAuthProviders.body().asString().contains(BuiltinAuthenticationProvider.PROVIDER_ID)) {
            System.out.println("Can't proceed with test without builtin provider.");
            return;
        }

        Response makeOAuthUser = UtilIT.migrateBuiltinToOAuth(data, superuserApiToken);
        makeOAuthUser.prettyPrint();
        makeOAuthUser.then().assertThat()
                .statusCode(OK.getStatusCode())
                //                .body("data.affiliation", equalTo("TestShib Test IdP"))
                .body("data.'changing to this provider'", equalTo("github"))
                .body("data.'password supplied'", equalTo(password));

        /**
         * @todo Write more failing tests such as expecting a non-OK response if
         * the OAuth user has an invalid email address:
         * https://github.com/IQSS/dataverse/issues/2998
         */
        Response oauthToBuiltinAnon = UtilIT.migrateOAuthToBuiltin(Long.MAX_VALUE, "", "");
        oauthToBuiltinAnon.prettyPrint();
        oauthToBuiltinAnon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response nonSuperuser = UtilIT.migrateOAuthToBuiltin(Long.MAX_VALUE, "", "");
        nonSuperuser.prettyPrint();
        nonSuperuser.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response infoOfUserToConvert = UtilIT.getAuthenticatedUser(usernameOfUserToConvert, superuserApiToken);
        infoOfUserToConvert.prettyPrint();
        infoOfUserToConvert.then().assertThat()
                .body("data.id", equalTo(Long.valueOf(idOfUserToConvert).intValue()))
                .body("data.identifier", equalTo("@" + usernameOfUserToConvert))
                .body("data.persistentUserId", equalTo(newPersistentUserIdInLookupTable))
                .body("data.authenticationProviderId", equalTo("github"))
                .statusCode(OK.getStatusCode());

        String invalidEmailAddress = "invalidEmailAddress";
        Response invalidEmailFail = UtilIT.migrateOAuthToBuiltin(idOfUserToConvert, invalidEmailAddress, superuserApiToken);
        invalidEmailFail.prettyPrint();
        invalidEmailFail.then().assertThat()
                .body("status", equalTo("ERROR"))
                .statusCode(BAD_REQUEST.getStatusCode());

        String existingEmailAddress = "dataverse@mailinator.com";
        Response existingEmailFail = UtilIT.migrateOAuthToBuiltin(idOfUserToConvert, existingEmailAddress, superuserApiToken);
        existingEmailFail.prettyPrint();
        existingEmailFail.then().assertThat()
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("User id " + idOfUserToConvert
                        + " could not be converted from remote to BuiltIn. Details from Exception: java.lang.Exception: User id "
                        + idOfUserToConvert + " (@"
                        + usernameOfUserToConvert
                        + ") cannot be converted from remote to BuiltIn because the email address dataverse@mailinator.com is already in use by user id 1 (@dataverseAdmin). "))
                .statusCode(BAD_REQUEST.getStatusCode());

        String newEmailAddress = UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";
        Response shouldWork = UtilIT.migrateOAuthToBuiltin(idOfUserToConvert, newEmailAddress, superuserApiToken);
        shouldWork.prettyPrint();
        shouldWork.then().assertThat()
                .body("data.username", notNullValue())
                .body("data.email", equalTo(newEmailAddress))
                .statusCode(OK.getStatusCode());

        Response infoForUserConvertedToBuiltin = UtilIT.getAuthenticatedUser(usernameOfUserToConvert, superuserApiToken);
        infoForUserConvertedToBuiltin.prettyPrint();
        infoForUserConvertedToBuiltin.then().assertThat()
                .body("data.id", equalTo(Long.valueOf(idOfUserToConvert).intValue()))
                .body("data.identifier", equalTo("@" + usernameOfUserToConvert))
                .body("data.persistentUserId", equalTo(usernameOfUserToConvert))
                .body("data.authenticationProviderId", equalTo("builtin"))
                .body("data.email", equalTo(newEmailAddress))
                .statusCode(OK.getStatusCode());

        Response deleteUserToConvert = UtilIT.deleteUser(usernameOfUserToConvert);
        assertEquals(200, deleteUserToConvert.getStatusCode());

        Response deleteSuperuser = UtilIT.deleteUser(superuserUsername);
        assertEquals(200, deleteSuperuser.getStatusCode());

    }

    @Test
    public void testCreateNonBuiltinUserViaApi() {
        Response createUser = UtilIT.createRandomAuthenticatedUser(OrcidOAuth2AP.PROVIDER_ID_PRODUCTION);
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());

        String persistentUserId = createUser.body().jsonPath().getString("data.persistentUserId");

        Response deleteUserToConvert = UtilIT.deleteUser(persistentUserId);
        assertEquals(200, deleteUserToConvert.getStatusCode());
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

        UtilIT.deleteDataverse(dataverseAlias, apiToken).then().assertThat()
                .statusCode(OK.getStatusCode());

        Response deleteSuperuser = UtilIT.deleteUser(username);
        assertEquals(200, deleteSuperuser.getStatusCode());
    }

}
