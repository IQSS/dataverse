package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import static edu.harvard.iq.dataverse.api.UtilIT.getRandomString;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.List;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import org.junit.Test;
import org.junit.BeforeClass;
import java.util.UUID;
import javax.validation.constraints.AssertTrue;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Ignore;

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
    public void testFilterAuthenticatedUsersForbidden() throws Exception {
        
        // --------------------------------------------
        // Forbidden: Try *without* an API token
        // --------------------------------------------
        Response anon = UtilIT.filterAuthenticatedUsers("", null, null, null);
        anon.prettyPrint();
        anon.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        // --------------------------------------------
        // Forbidden: Try with a regular user--*not a superuser*
        // --------------------------------------------
        Response createUserResponse = UtilIT.createRandomUser();
        createUserResponse.then().assertThat().statusCode(OK.getStatusCode());
        
        String nonSuperuserApiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        String nonSuperUsername = UtilIT.getUsernameFromResponse(createUserResponse);
        
        Response filterResponseBadToken = UtilIT.filterAuthenticatedUsers(nonSuperuserApiToken, null, null, null);
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
        Response filterReponse01 = UtilIT.filterAuthenticatedUsers(superuserApiToken, randUserNamePrefix, null, 100);
        filterReponse01.then().assertThat().statusCode(OK.getStatusCode());
        filterReponse01.prettyPrint();

        int numResults = 11;
        filterReponse01.then().assertThat()
                .body("data.userCount", equalTo(numResults))
                .body("data.selectedPage", equalTo(1))
                .body("data.pagination.pageCount", equalTo(1))
                .body("data.pagination.numResults", equalTo(numResults));
        
        String userIdentifer;
        for (int i=0; i < numResults; i++){
            userIdentifer = JsonPath.from(filterReponse01.getBody().asString()).getString("data.users[" + i + "].userIdentifier");
            assertEquals(randomUsernames.contains(userIdentifer), true);
        }

        List<Object> userList1 = JsonPath.from(filterReponse01.body().asString()).getList("data.users");
        assertEquals(userList1.size(), numResults);
        
        // --------------------------------------------
        // Search for the 11 new users, but only return 5 per page
        // --------------------------------------------        
        int numUsersReturned = 5;
        Response filterReponse02 = UtilIT.filterAuthenticatedUsers(superuserApiToken, randUserNamePrefix, 1, numUsersReturned);
        filterReponse02.then().assertThat().statusCode(OK.getStatusCode());
        filterReponse02.prettyPrint();

        filterReponse02.then().assertThat()
                .body("data.userCount", equalTo(numResults))
                .body("data.selectedPage", equalTo(1))
                .body("data.pagination.docsPerPage", equalTo(numUsersReturned))
                .body("data.pagination.pageCount", equalTo(3))
                .body("data.pagination.numResults", equalTo(numResults));
        
        String userIdentifer2;
        for (int i=0; i < numUsersReturned; i++){
            userIdentifer2 = JsonPath.from(filterReponse02.getBody().asString()).getString("data.users[" + i + "].userIdentifier");
            assertEquals(randomUsernames.contains(userIdentifer2), true);
        }
        
        List<Object> userList2 = JsonPath.from(filterReponse02.body().asString()).getList("data.users");
        assertEquals(userList2.size(), numUsersReturned);

        
        // --------------------------------------------
        // Search for the 11 new users, return 5 per page, and start on NON-EXISTENT 4th page -- should revert to 1st page
        // --------------------------------------------        
        Response filterReponse02a = UtilIT.filterAuthenticatedUsers(superuserApiToken, randUserNamePrefix, 4, numUsersReturned);
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
        Response filterReponse03 = UtilIT.filterAuthenticatedUsers(superuserApiToken, randUserNamePrefix, 3, 5);
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
        Response filterReponse04 = UtilIT.filterAuthenticatedUsers(superuserApiToken, "zzz" + randUserNamePrefix, 1, 50);
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
        Response filterReponse05 = UtilIT.filterAuthenticatedUsers(superuserApiToken, singleUsername, 1, 50);
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
        String emailOfUserToConvert = createUserToConvert.body().jsonPath().getString("data.authenticatedUser.email");
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
    
    @Test
    @Ignore
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

}
