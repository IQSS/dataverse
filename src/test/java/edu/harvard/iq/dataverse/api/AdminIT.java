package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;



import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminIT {

    private static final Logger logger = Logger.getLogger(AdminIT.class.getCanonicalName());

    private final String testNonSuperuserApiToken = createTestNonSuperuserApiToken();

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testListAuthenticatedUsers() throws Exception {
        Response anon = UtilIT.listAuthenticatedUsers(testNonSuperuserApiToken);
        anon.prettyPrint();
        anon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response nonSuperuser = UtilIT.listAuthenticatedUsers(testNonSuperuserApiToken);
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

        Response createNonSuperuser = UtilIT.createRandomUser();
        String nonSuperuserUsername = UtilIT.getUsernameFromResponse(createNonSuperuser);

        Response deleteNonSuperuser = UtilIT.deleteUser(nonSuperuserUsername);
        assertEquals(200, deleteNonSuperuser.getStatusCode());

        Response deleteSuperuser = UtilIT.deleteUser(superuserUsername);
        assertEquals(200, deleteSuperuser.getStatusCode());
    }
    
    @Test
    public void testFilterAuthenticatedUsersForbidden() throws Exception {
        
        // --------------------------------------------
        // Forbidden: Try *without* an API token
        // --------------------------------------------
        Response anon = UtilIT.filterAuthenticatedUsers("", null, null, null, null);
        anon.prettyPrint();
        anon.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // --------------------------------------------
        // Forbidden: Try with a regular user--*not a superuser*
        // --------------------------------------------
        Response createUserResponse = UtilIT.createRandomUser();
        createUserResponse.then().assertThat().statusCode(OK.getStatusCode());
        
        String nonSuperuserApiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        String nonSuperUsername = UtilIT.getUsernameFromResponse(createUserResponse);
        
        Response filterResponseBadToken = UtilIT.filterAuthenticatedUsers(nonSuperuserApiToken, null, null, null, null);
        filterResponseBadToken.then().assertThat().statusCode(FORBIDDEN.getStatusCode());
         
        // delete user
        Response deleteNonSuperuser = UtilIT.deleteUser(nonSuperUsername);
        assertEquals(200, deleteNonSuperuser.getStatusCode());
    }
    
    /**
     * Run multiple test against API endpoint to search authenticated users
     * @throws Exception 
     */
    @Test
    public void testFilterAuthenticatedUsers() throws Exception {

        Response createUserResponse;
        
        // --------------------------------------------
        // Make 11 random users
        // --------------------------------------------
        String randUserNamePrefix = "r" + UtilIT.getRandomString(4) + "_";

        List<String> randomUsernames = new ArrayList<String>();
        for (int i = 0; i < 11; i++){
            
            createUserResponse = UtilIT.createRandomUser(randUserNamePrefix);
            createUserResponse.then().assertThat().statusCode(OK.getStatusCode());
            String newUserName = UtilIT.getUsernameFromResponse(createUserResponse);
            randomUsernames.add(newUserName);
            
        }
        
        // --------------------------------------------
        // Create superuser
        // --------------------------------------------
        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        
        // --------------------------------------------
        // Search for the 11 new users and verify results
        // --------------------------------------------        
        Response filterReponse01 = UtilIT.filterAuthenticatedUsers(superuserApiToken, randUserNamePrefix, null, 100, null);
        filterReponse01.then().assertThat().statusCode(OK.getStatusCode());
        filterReponse01.prettyPrint();

        int numResults = 11;
        filterReponse01.then().assertThat()
                .body("data.userCount", equalTo(numResults))
                .body("data.selectedPage", equalTo(1))
                .body("data.pagination.pageCount", equalTo(1))
                .body("data.pagination.numResults", equalTo(numResults));
        
        String userIdentifier;
        for (int i=0; i < numResults; i++){
            userIdentifier = JsonPath.from(filterReponse01.getBody().asString()).getString("data.users[" + i + "].userIdentifier");
            assertTrue(randomUsernames.contains(userIdentifier));
        }

        List<Object> userList1 = JsonPath.from(filterReponse01.body().asString()).getList("data.users");
        assertEquals(userList1.size(), numResults);
        
        // --------------------------------------------
        // Search for the 11 new users, but only return 5 per page
        // --------------------------------------------        
        int numUsersReturned = 5;
        Response filterReponse02 = UtilIT.filterAuthenticatedUsers(superuserApiToken, randUserNamePrefix, 1, numUsersReturned, null);
        filterReponse02.then().assertThat().statusCode(OK.getStatusCode());
        filterReponse02.prettyPrint();

        filterReponse02.then().assertThat()
                .body("data.userCount", equalTo(numResults))
                .body("data.selectedPage", equalTo(1))
                .body("data.pagination.docsPerPage", equalTo(numUsersReturned))
                .body("data.pagination.pageCount", equalTo(3))
                .body("data.pagination.numResults", equalTo(numResults));
        
        String userIdentifier2;
        for (int i=0; i < numUsersReturned; i++){
            userIdentifier2 = JsonPath.from(filterReponse02.getBody().asString()).getString("data.users[" + i + "].userIdentifier");
            assertTrue(randomUsernames.contains(userIdentifier2));
        }
        
        List<Object> userList2 = JsonPath.from(filterReponse02.body().asString()).getList("data.users");
        assertEquals(userList2.size(), numUsersReturned);

        
        // --------------------------------------------
        // Search for the 11 new users, return 5 per page, and start on NON-EXISTENT 4th page -- should revert to 1st page
        // --------------------------------------------        
        Response filterReponse02a = UtilIT.filterAuthenticatedUsers(superuserApiToken, randUserNamePrefix, 4, numUsersReturned, null);
        filterReponse02a.then().assertThat().statusCode(OK.getStatusCode());
        filterReponse02a.prettyPrint();

        filterReponse02a.then().assertThat()
                .body("data.userCount", equalTo(numResults))
                .body("data.selectedPage", equalTo(1))
                .body("data.pagination.docsPerPage", equalTo(numUsersReturned))
                .body("data.pagination.pageCount", equalTo(3))
                .body("data.pagination.numResults", equalTo(numResults));

        List<Object> userList2a = JsonPath.from(filterReponse02a.body().asString()).getList("data.users");
        assertEquals(userList2a.size(), numUsersReturned);
        
        // --------------------------------------------
        // Search for the 11 new users, return 5 per page, start on 3rd page
        // --------------------------------------------     
        Response filterReponse03 = UtilIT.filterAuthenticatedUsers(superuserApiToken, randUserNamePrefix, 3, 5, null);
        filterReponse03.then().assertThat().statusCode(OK.getStatusCode());
        filterReponse03.prettyPrint();
        
        filterReponse03.then().assertThat()
                .body("data.userCount", equalTo(numResults))
                .body("data.selectedPage", equalTo(3))
                .body("data.pagination.docsPerPage", equalTo(5))
                .body("data.pagination.hasNextPageNumber", equalTo(false))
                .body("data.pagination.pageCount", equalTo(3))
                .body("data.pagination.numResults", equalTo(numResults));
       
        List<Object> userList3 = JsonPath.from(filterReponse03.body().asString()).getList("data.users");
        assertEquals(userList3.size(), 1);

        // --------------------------------------------
        // Run search that returns no users
        // --------------------------------------------     
        Response filterReponse04 = UtilIT.filterAuthenticatedUsers(superuserApiToken, "zzz" + randUserNamePrefix, 1, 50, null);
        filterReponse04.then().assertThat().statusCode(OK.getStatusCode());
        filterReponse04.prettyPrint();
        
        filterReponse04.then().assertThat()
                .body("data.userCount", equalTo(0))
                .body("data.selectedPage", equalTo(1));

        List<Object> userList4 = JsonPath.from(filterReponse04.body().asString()).getList("data.users");
        assertEquals(userList4.size(), 0);

        
        // --------------------------------------------
        // Run search that returns 1 user
        // --------------------------------------------     
        String singleUsername = randomUsernames.get(0);
        Response filterReponse05 = UtilIT.filterAuthenticatedUsers(superuserApiToken, singleUsername, 1, 50, null);
        filterReponse05.then().assertThat().statusCode(OK.getStatusCode());
        filterReponse05.prettyPrint();
        
        filterReponse05.then().assertThat()
                .body("data.userCount", equalTo(1))
                .body("data.selectedPage", equalTo(1));

        List<Object> userList5 = JsonPath.from(filterReponse05.body().asString()).getList("data.users");
        assertEquals(userList5.size(), 1);

        // --------------------------------------------
        // Delete  random users
        // --------------------------------------------
        Response deleteUserResponse;
        for (String aUsername : randomUsernames){
            
            deleteUserResponse = UtilIT.deleteUser(aUsername);
            assertEquals(200, deleteUserResponse.getStatusCode());
        
        }
        
        // --------------------------------------------
        // Delete superuser
        // --------------------------------------------
        deleteUserResponse = UtilIT.deleteUser(superuserUsername);
        assertEquals(200, deleteUserResponse.getStatusCode());
               
        
    }
    
    @Test
    public void testConvertShibUserToBuiltin() throws Exception {

        Response createUserToConvert = UtilIT.createRandomUser();
        createUserToConvert.prettyPrint();

        long idOfUserToConvert = createUserToConvert.body().jsonPath().getLong("data.authenticatedUser.id");
        String emailOfUserToConvert = createUserToConvert.body().jsonPath().getString("data.authenticatedUser.email");
        String usernameOfUserToConvert = UtilIT.getUsernameFromResponse(createUserToConvert);

        String password = usernameOfUserToConvert;
        String newEmailAddressToUse = "builtin2shib." + UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";
        String data = emailOfUserToConvert + ":" + password + ":" + newEmailAddressToUse;

        Response builtinToShibAnon = UtilIT.migrateBuiltinToShib(data, testNonSuperuserApiToken);
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
        Response shibToBuiltinAnon = UtilIT.migrateShibToBuiltin(Long.MAX_VALUE, "", testNonSuperuserApiToken);
        shibToBuiltinAnon.prettyPrint();
        shibToBuiltinAnon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response nonSuperuser = UtilIT.migrateShibToBuiltin(Long.MAX_VALUE, "", testNonSuperuserApiToken);
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

    /**
     * Here we are asserting that deactivated users cannot be converted into
     * shib users.
     */
    @Test
    public void testConvertDeactivateUserToShib() {

        Response createUserToConvert = UtilIT.createRandomUser();
        createUserToConvert.then().assertThat().statusCode(OK.getStatusCode());
        createUserToConvert.prettyPrint();

        long idOfUserToConvert = createUserToConvert.body().jsonPath().getLong("data.authenticatedUser.id");
        String emailOfUserToConvert = createUserToConvert.body().jsonPath().getString("data.authenticatedUser.email");
        String usernameOfUserToConvert = UtilIT.getUsernameFromResponse(createUserToConvert);

        Response deactivateUser = UtilIT.deactivateUser(usernameOfUserToConvert);
        deactivateUser.prettyPrint();
        deactivateUser.then().assertThat().statusCode(OK.getStatusCode());

        String password = usernameOfUserToConvert;
        String newEmailAddressToUse = "builtin2shib." + UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";
        String data = emailOfUserToConvert + ":" + password + ":" + newEmailAddressToUse;

        Response builtinToShibAnon = UtilIT.migrateBuiltinToShib(data, testNonSuperuserApiToken);
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
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("[\"builtin account has been deactivated\"]"));

        Response userIsStillBuiltin = UtilIT.getAuthenticatedUser(usernameOfUserToConvert, superuserApiToken);
        userIsStillBuiltin.prettyPrint();
        userIsStillBuiltin.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.id", equalTo(Long.valueOf(idOfUserToConvert).intValue()))
                .body("data.identifier", equalTo("@" + usernameOfUserToConvert))
                .body("data.authenticationProviderId", equalTo("builtin"));

    }

    @Test
    public void testConvertOAuthUserToBuiltin() throws Exception {

        System.out.println("BEGIN testConvertOAuthUserToBuiltin");

        Response createUserToConvert = UtilIT.createRandomUser();
        createUserToConvert.prettyPrint();

        long idOfUserToConvert = createUserToConvert.body().jsonPath().getLong("data.authenticatedUser.id");
        String emailOfUserToConvert = createUserToConvert.body().jsonPath().getString("data.authenticatedUser.email");
        String usernameOfUserToConvert = UtilIT.getUsernameFromResponse(createUserToConvert);

        String password = usernameOfUserToConvert;
        String newEmailAddressToUse = "builtin2shib." + UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";

        GitHubOAuth2AP github = new GitHubOAuth2AP(null, null);
        String providerIdToConvertTo = github.getId();
        String newPersistentUserIdInLookupTable = UUID.randomUUID().toString().substring(0, 8);
        String data = emailOfUserToConvert + ":" + password + ":" + newEmailAddressToUse + ":" + providerIdToConvertTo + ":" + newPersistentUserIdInLookupTable;

        System.out.println("data: " + data);
        Response builtinToOAuthAnon = UtilIT.migrateBuiltinToOAuth(data, testNonSuperuserApiToken);
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
        Response nonSuperuser = UtilIT.migrateOAuthToBuiltin(Long.MAX_VALUE, "", testNonSuperuserApiToken);
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
        Response createUser = UtilIT.createRandomAuthenticatedUser(OrcidOAuth2AP.PROVIDER_ID);
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

    @Test
    public void testRecalculateDataFileHash() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();

        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = JsonPath.from(createDataverse.body().asString()).getString("data.alias");

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        Response datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());

        String pathToFile = "scripts/search/data/tabular/50by1000.dta";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        Long origFileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        UtilIT.makeSuperUser(superuserUsername);

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", superuserApiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + origFileId);

        //Bad file id         
        Response computeDataFileHashResponse = UtilIT.computeDataFileHashValue("BadFileId", DataFile.ChecksumType.MD5.toString(), superuserApiToken);

        computeDataFileHashResponse.then().assertThat()
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("Could not find file with the id: BadFileId"))
                .statusCode(BAD_REQUEST.getStatusCode());

        //Bad Algorithm
        computeDataFileHashResponse = UtilIT.computeDataFileHashValue(origFileId.toString(), "Blank", superuserApiToken);

        computeDataFileHashResponse.then().assertThat()
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("Unknown algorithm: Blank"))
                .statusCode(BAD_REQUEST.getStatusCode());
        
        //Not a Super user
        computeDataFileHashResponse = UtilIT.computeDataFileHashValue(origFileId.toString(), DataFile.ChecksumType.MD5.toString(), apiToken);

        computeDataFileHashResponse.then().assertThat()
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("must be superuser"))
                .statusCode(UNAUTHORIZED.getStatusCode());


        computeDataFileHashResponse = UtilIT.computeDataFileHashValue(origFileId.toString(), DataFile.ChecksumType.MD5.toString(), superuserApiToken);
        computeDataFileHashResponse.prettyPrint();

        computeDataFileHashResponse.then().assertThat()
                .body("data.message", equalTo("Datafile rehashing complete. " + origFileId.toString() + "  successfully rehashed. New hash value is: 003b8c67fbdfa6df31c0e43e65b93f0e"))
                .statusCode(OK.getStatusCode());
        
        //Not a Super user
        Response validationResponse = UtilIT.validateDataFileHashValue(origFileId.toString(), apiToken);

        validationResponse.then().assertThat()
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("must be superuser"))
                .statusCode(UNAUTHORIZED.getStatusCode());
        
        //Bad File Id
        validationResponse = UtilIT.validateDataFileHashValue("BadFileId", superuserApiToken);

        validationResponse.then().assertThat()
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("Could not find file with the id: BadFileId"))
                .statusCode(BAD_REQUEST.getStatusCode());

        validationResponse = UtilIT.validateDataFileHashValue(origFileId.toString(), superuserApiToken);
        validationResponse.prettyPrint();
        validationResponse.then().assertThat()
                .body("data.message", equalTo("Datafile validation complete for " + origFileId.toString() + ". The hash value is: 003b8c67fbdfa6df31c0e43e65b93f0e"))
                .statusCode(OK.getStatusCode());

        //  String checkSumVal = 
        Response pubdv = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        Response publishDSViaNative = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDSViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());

    }
    
    @Test
    @Disabled
    public void testMigrateHDLToDOI() {
        /*
        This test is set to ignore because it requires a setup that will
        mint both handles and doi identifiers        
        Can re-enable when if test environments are running handle servers.        
        SEK 09/27/2018
        */
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();

        // change this to register new dataset witha handle
        UtilIT.setSetting(SettingsServiceBean.Key.Protocol, "hdl");
        UtilIT.setSetting(SettingsServiceBean.Key.Authority, "20.500.12050");
        UtilIT.setSetting(SettingsServiceBean.Key.Shoulder, "");
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = JsonPath.from(createDataverse.body().asString()).getString("data.alias");

        String pathToJsonFile = "src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-hdl.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        Response datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response pubdv = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        Response publishDSViaNative = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDSViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response migrateIdentifierResponseStillHDL = UtilIT.migrateDatasetIdentifierFromHDLToPId(datasetId.toString(), apiToken);
        migrateIdentifierResponseStillHDL.then().assertThat()
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("May not migrate while installation protocol set to \"hdl\". Protocol must be \"doi\""))
                .statusCode(BAD_REQUEST.getStatusCode());

        UtilIT.setSetting(SettingsServiceBean.Key.Protocol, "doi");
        UtilIT.setSetting(SettingsServiceBean.Key.Authority, "10.5072");
        UtilIT.setSetting(SettingsServiceBean.Key.Shoulder, "FK2/");
        Response migrateIdentifierResponseMustBeSuperUser = UtilIT.migrateDatasetIdentifierFromHDLToPId(datasetId.toString(), apiToken);
        migrateIdentifierResponseMustBeSuperUser.then().assertThat()
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("Forbidden. You must be a superuser."))
                .statusCode(UNAUTHORIZED.getStatusCode());
        
        Response createSuperuser = UtilIT.createRandomUser();
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        UtilIT.makeSuperUser(superuserUsername);
        
        Response migrateIdentifierResponse = UtilIT.migrateDatasetIdentifierFromHDLToPId(datasetId.toString(), superuserApiToken);
        migrateIdentifierResponse.prettyPrint();
        migrateIdentifierResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    /**
     * Disabled because once there are new fields in the database that Solr
     * doesn't know about, dataset creation could be prevented, or at least
     * subsequent search operations could fail because the dataset can't be
     * indexed.
     */
    @Disabled
    @Test
    public void testLoadMetadataBlock_NoErrorPath() {
        Response createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        byte[] updatedContent = null;
        try {
            updatedContent = Files.readAllBytes(Paths.get("scripts/api/data/metadatablocks/citation.tsv"));
        } catch (IOException e) {
            logger.warning(e.getMessage());
            assertEquals(0,1);
        }

        Response response = UtilIT.loadMetadataBlock(apiToken, updatedContent);
        assertEquals(200, response.getStatusCode());
        response.then().assertThat().statusCode(OK.getStatusCode());

        String body = response.getBody().asString();
        String status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);

        Map<String, List<Map<String, String>>> data = JsonPath.from(body).getMap("data");
        assertEquals(1, data.size());
        List<Map<String, String>> addedElements = data.get("added");
        //Note -test depends on the number of elements in the production citation block, so any changes to the # of elements there can break this test
        assertEquals(323, addedElements.size());

        Map<String, Integer> statistics = new HashMap<>();
        for (Map<String, String> unit : addedElements) {
            assertEquals(2, unit.size());
            assertTrue(unit.containsKey("name"));
            assertTrue(unit.containsKey("type"));
            String type = unit.get("type");
            if (!statistics.containsKey(type))
                statistics.put(type, 0);
            statistics.put(type, statistics.get(type) + 1);
        }

        assertEquals(3, statistics.size());
        assertEquals(1, (int) statistics.get("MetadataBlock"));
        assertEquals(78, (int) statistics.get("DatasetField"));
        assertEquals(244, (int) statistics.get("Controlled Vocabulary"));
    }

    /**
     * Disabled because once there are new fields in the database that Solr
     * doesn't know about, dataset creation could be prevented, or at least
     * subsequent search operations could fail because the dataset can't be
     * indexed.
     */
    @Disabled
    @Test
    public void testLoadMetadataBlock_ErrorHandling() {
        Response createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        byte[] updatedContent = null;
        try {
            updatedContent = Files.readAllBytes(Paths.get("src/test/resources/tsv/test.tsv"));
        } catch (IOException e) {
            logger.warning(e.getMessage());
            assertEquals(0,1);
        }
        Response response = UtilIT.loadMetadataBlock(apiToken, updatedContent);
        assertEquals(500, response.getStatusCode());
        response.then().assertThat().statusCode(INTERNAL_SERVER_ERROR.getStatusCode());

        String body = response.getBody().asString();
        String status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);

        String message = JsonPath.from(body).getString("message");
        assertEquals(
          "Error parsing metadata block in DATASETFIELD part, line #5: missing 'watermark' column (#5)",
          message
        );
    }
    @Test
    public void testClearThumbnailFailureFlag(){
        Response nonExistentFile = UtilIT.clearThumbnailFailureFlag(Long.MAX_VALUE);
        nonExistentFile.prettyPrint();
        nonExistentFile.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
        
        Response clearAllFlags = UtilIT.clearThumbnailFailureFlags();
        clearAllFlags.prettyPrint();
        clearAllFlags.then().assertThat().statusCode(OK.getStatusCode());
    }
    
    @Test
    public void testBannerMessages(){

        //We check for existing banner messages and get the number of existing messages
        Response getBannerMessageResponse = UtilIT.getBannerMessages();
        getBannerMessageResponse.prettyPrint();
        getBannerMessageResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer numBannerMessages =
                JsonPath.from(getBannerMessageResponse.getBody().asString()).getInt("data.size()");

        //We add a banner message with an error in the json file
        String pathToJsonFile = "scripts/api/data/bannerMessageError.json";       
        Response addBannerMessageErrorResponse  = UtilIT.addBannerMessage(pathToJsonFile);
        addBannerMessageErrorResponse.prettyPrint();
        addBannerMessageErrorResponse.then().assertThat()
                        .statusCode(BAD_REQUEST.getStatusCode())
                        .body("status", equalTo("ERROR"));
        
        //We add a banner message with a correct json file
        pathToJsonFile = "scripts/api/data/bannerMessageTest.json";
        Response addBannerMessageResponse = UtilIT.addBannerMessage(pathToJsonFile);
        addBannerMessageResponse.prettyPrint();
        addBannerMessageResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("status", equalTo("OK"))
                .body("data.message", equalTo("Banner Message added successfully."));
        Long addedBanner = Long.valueOf(
                        JsonPath.from(addBannerMessageResponse.getBody().asString()).getLong("data.id"));                
        
        //We get the banner messages and check that the number of messages has increased by 1
        getBannerMessageResponse = UtilIT.getBannerMessages();
        getBannerMessageResponse.prettyPrint();
        getBannerMessageResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(numBannerMessages + 1));

        //We delete the banner message
        Response deleteBannerMessageResponse = UtilIT.deleteBannerMessage(addedBanner);
        deleteBannerMessageResponse.prettyPrint();
        deleteBannerMessageResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("status", equalTo("OK"));
        
    }

    /**
     * For a successful download from /tmp, see BagIT. Here we are doing error
     * checking.
     */
    @Test
    public void testDownloadTmpFile() throws IOException {

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response tryToDownloadAsNonSuperuser = UtilIT.downloadTmpFile("/tmp/foo", apiToken);
        tryToDownloadAsNonSuperuser.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response toggleSuperuser = UtilIT.makeSuperUser(username);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response tryToDownloadEtcPasswd = UtilIT.downloadTmpFile("/etc/passwd", apiToken);
        tryToDownloadEtcPasswd.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("Path must begin with '/tmp' but after normalization was '/etc/passwd'."));
    }

    @Test
    public void testFindMissingFiles() {
        Response createUserResponse = UtilIT.createRandomUser();
        createUserResponse.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUserResponse);
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        UtilIT.setSuperuserStatus(username, true);

        String dataverseAlias = ":root";
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        String datasetPersistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");

        // Upload file
        Response uploadResponse = UtilIT.uploadRandomFile(datasetPersistentId, apiToken);
        uploadResponse.then().assertThat().statusCode(CREATED.getStatusCode());

        // Audit files
        Response resp = UtilIT.auditFiles(apiToken, null, 100L, null);
        resp.prettyPrint();
        JsonArray emptyArray = Json.createArrayBuilder().build();
        resp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.lastId", equalTo(100));

        // Audit files with invalid parameters
        resp = UtilIT.auditFiles(apiToken, 100L, 0L, null);
        resp.prettyPrint();
        resp.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("Invalid Parameters: lastId must be equal to or greater than firstId"));

        // Audit files with list of dataset identifiers parameter
        resp = UtilIT.auditFiles(apiToken, 1L, null, "bad/id, " + datasetPersistentId);
        resp.prettyPrint();
        resp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.failures[0].datasetIdentifier", equalTo("bad/id"))
                .body("data.failures[0].reason", equalTo("Not Found"));
    }

    private String createTestNonSuperuserApiToken() {
        Response createUserResponse = UtilIT.createRandomUser();
        createUserResponse.then().assertThat().statusCode(OK.getStatusCode());
        return UtilIT.getApiTokenFromResponse(createUserResponse);
    }

    @ParameterizedTest
    @ValueSource(booleans={true,false})
    public void testSetSuperUserStatus(Boolean status) {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        Response toggleSuperuser = UtilIT.setSuperuserStatus(username, status);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());
    }
}
