package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.BundleUtil;
import io.restassured.RestAssured;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class UsersIT {

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
       /* 
        Response removeAllowApiTokenLookupViaApi = UtilIT.deleteSetting(SettingsServiceBean.Key.AllowApiTokenLookupViaApi);
        removeAllowApiTokenLookupViaApi.then().assertThat()
                .statusCode(200);
*/
    }
    
    @Test
    public void testChangeAuthenticatedUserIdentifier() {
        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        String usernameOfUser = UtilIT.getUsernameFromResponse(createUser);
        String userApiToken = UtilIT.getApiTokenFromResponse(createUser);
        
        Response createUserForAlreadyExists = UtilIT.createRandomUser();
        createUserForAlreadyExists.prettyPrint();
        assertEquals(200, createUserForAlreadyExists.getStatusCode());
        String usernameOfUserAlreadyExists = UtilIT.getUsernameFromResponse(createUserForAlreadyExists);
        
        String newUsername = "newUser_" + UtilIT.getRandomString(4);
        Response changeAuthIdResponse = UtilIT.changeAuthenticatedUserIdentifier(usernameOfUser, newUsername, superuserApiToken);
        changeAuthIdResponse.prettyPrint();
        changeAuthIdResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        //No api token
        Response changeAuthIdResponseNoToken = UtilIT.changeAuthenticatedUserIdentifier(usernameOfUser, newUsername, null);
        changeAuthIdResponseNoToken.prettyPrint();
        changeAuthIdResponseNoToken.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());
        
        //Users own api token
        Response changeAuthIdResponseNormalToken = UtilIT.changeAuthenticatedUserIdentifier(usernameOfUser, newUsername, userApiToken);
        changeAuthIdResponseNormalToken.prettyPrint();
        changeAuthIdResponseNormalToken.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());

        //Try changing to already existing username
        Response changeAuthIdResponseBadAlreadyExists= UtilIT.changeAuthenticatedUserIdentifier(newUsername, usernameOfUserAlreadyExists, superuserApiToken);
        changeAuthIdResponseBadAlreadyExists.prettyPrint();
        changeAuthIdResponseBadAlreadyExists.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());
        
        String newUsernameBad = ""; //one character, should fail before bean validation even
        //Without second param url is not found.
        Response changeAuthIdResponseBad = UtilIT.changeAuthenticatedUserIdentifier(newUsername, newUsernameBad, superuserApiToken);
        changeAuthIdResponseBad.prettyPrint();
        changeAuthIdResponseBad.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());
        
        String newUsernameBad2 = "q"; //one character, should fail bean validation
        Response changeAuthIdResponseBad2 = UtilIT.changeAuthenticatedUserIdentifier(newUsername, newUsernameBad2, superuserApiToken);
        changeAuthIdResponseBad2.prettyPrint();
        changeAuthIdResponseBad2.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());
        
        //if this fails likely one of the converts that said they failed actually didn't!
        Response deleteUserToConvert = UtilIT.deleteUser(newUsername);
        assertEquals(200, deleteUserToConvert.getStatusCode());

        Response deleteSuperuser = UtilIT.deleteUser(superuserUsername);
        assertEquals(200, deleteSuperuser.getStatusCode());
    }
    
    @Test
    public void testMergeAccounts(){
        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String usernameConsumed = UtilIT.getUsernameFromResponse(createUser);
        String normalApiToken = UtilIT.getApiTokenFromResponse(createUser);
        
        
        Response createDataverse = UtilIT.createRandomDataverse(normalApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = JsonPath.from(createDataverse.body().asString()).getString("data.alias");
        
        Response createDataverseSuper = UtilIT.createRandomDataverse(superuserApiToken);
        createDataverseSuper.prettyPrint();
        createDataverseSuper.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAliasSuper = JsonPath.from(createDataverseSuper.body().asString()).getString("data.alias");

        String pathToJsonFile = "src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-hdl.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, normalApiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        Response datasetAsJson = UtilIT.nativeGet(datasetId, normalApiToken);
        datasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        String randomString = UtilIT.getRandomIdentifier();

        Response mergeAccounts = UtilIT.mergeAccounts(randomString, usernameConsumed, superuserApiToken);
        assertEquals(400, mergeAccounts.getStatusCode());
        mergeAccounts.prettyPrint();
        
        Response targetUser = UtilIT.createRandomUser();
        targetUser.prettyPrint();
        String targetname = UtilIT.getUsernameFromResponse(targetUser);
        String targetToken = UtilIT.getApiTokenFromResponse(targetUser);
        
        pathToJsonFile = "scripts/api/data/dataset-create-new.json";
        Response createDatasetResponseSuper = UtilIT.createDatasetViaNativeApi(dataverseAliasSuper, pathToJsonFile, superuserApiToken);
        createDatasetResponseSuper.prettyPrint();
        Integer datasetIdNew = JsonPath.from(createDatasetResponseSuper.body().asString()).getInt("data.id");

        String tabFile3NameRestrictedNew = "stata13-auto-withstrls.dta";
        String tab3PathToFile = "scripts/search/data/tabular/" + tabFile3NameRestrictedNew;

        Response tab3AddResponse = UtilIT.uploadFileViaNative(datasetIdNew.toString(), tab3PathToFile, superuserApiToken);
        Integer tabFile3IdRestrictedNew = JsonPath.from(tab3AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
        //Sleep while dataset locked for ingest
        assertTrue(UtilIT.sleepForLock(datasetIdNew.longValue(), "Ingest", superuserApiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + tabFile3NameRestrictedNew);

        Response restrictResponse = UtilIT.restrictFile(tabFile3IdRestrictedNew.toString(), true, superuserApiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat()
                .statusCode(OK.getStatusCode());


        //Update Dataset to allow requests
        Response allowAccessRequestsResponse = UtilIT.allowAccessRequests(datasetIdNew.toString(), true, superuserApiToken);
        assertEquals(200, allowAccessRequestsResponse.getStatusCode());
        
        Response publishDataverseResponseSuper =  UtilIT.publishDataverseViaNativeApi(dataverseAliasSuper, superuserApiToken);
        assertEquals(200, publishDataverseResponseSuper.getStatusCode());
        publishDataverseResponseSuper.prettyPrint();
        
        //Meanwhile add another file to be downloaded
        
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";

        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", "my description")
                .add("categories", Json.createArrayBuilder()
                        .add("Data")
                );

        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, json.build(), superuserApiToken);
        
        Integer downloadableFileId = JsonPath.from(addResponse.body().asString()).getInt("data.files[0].dataFile.id");
        
        //Must republish to get it to work
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetIdNew, "major", superuserApiToken);
        assertEquals(200, publishDataset.getStatusCode());

        Response requestFileAccessResponse = UtilIT.requestFileAccess(tabFile3IdRestrictedNew.toString(), normalApiToken);
        assertEquals(200, requestFileAccessResponse.getStatusCode());
        
        Response downloadThatFile = UtilIT.downloadFile(downloadableFileId, normalApiToken);
        assertEquals(200, downloadThatFile.getStatusCode());
        
        
        String aliasInOwner = "groupFor" + dataverseAlias;
        String displayName = "Group for " + dataverseAlias;
        String user2identifier = "@" + usernameConsumed;
        String target2identifier = "@" + targetname;
        Response createGroup = UtilIT.createGroup(dataverseAlias, aliasInOwner, displayName, superuserApiToken);
        createGroup.prettyPrint();
        createGroup.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        List<String> roleAssigneesToAdd = Arrays.asList(user2identifier, target2identifier);
        Response addToGroup = UtilIT.addToGroup(dataverseAlias, aliasInOwner, roleAssigneesToAdd, superuserApiToken);
        addToGroup.prettyPrint();
        addToGroup.then().assertThat()
                .statusCode(OK.getStatusCode());
        
              
        mergeAccounts = UtilIT.mergeAccounts(targetname, usernameConsumed, superuserApiToken);
        assertEquals(200, mergeAccounts.getStatusCode());
        mergeAccounts.prettyPrint();
        
        //No api token
        Response mergeResponseNoToken = UtilIT.mergeAccounts(targetname, usernameConsumed, null);
        mergeResponseNoToken.prettyPrint();
        mergeResponseNoToken.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());
        
        //Users own api token
        Response mergeResponseNormalToken = UtilIT.mergeAccounts(targetname, usernameConsumed, normalApiToken);
        mergeResponseNormalToken.prettyPrint();
        mergeResponseNormalToken.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());
        
        //After merging user see that old one is gone and new one exists
        Response getConsumedUserResponse =  UtilIT.getAuthenticatedUser(usernameConsumed, normalApiToken);
        assertEquals(400, getConsumedUserResponse.getStatusCode());
        
        Response getPersistedUserResponse =  UtilIT.getAuthenticatedUser(targetname, normalApiToken);
        assertEquals(200, getPersistedUserResponse.getStatusCode());
        
        //Make sure that you can publish the dataverse/dataset as the newly assigned user
        
        Response publishDataverseResponse =  UtilIT.publishDataverseViaNativeApi(dataverseAlias, targetToken);
        assertEquals(200, publishDataverseResponse.getStatusCode());
        publishDataverseResponse.prettyPrint();
        
        Response publishDatasetResponse =  UtilIT.publishDatasetViaNativeApi(datasetId, "major", targetToken);
        assertEquals(200, publishDatasetResponse.getStatusCode());
        publishDatasetResponse.prettyPrint();
        
        
        
    }
    
    /** Note: the below commands do not actually live in Users.java. They live in Admin.java */

    @Test
    public void convertNonBcryptUserFromBuiltinToShib() {

        Response createUserToConvert = UtilIT.createRandomUser();
        createUserToConvert.prettyPrint();
//
        long AuthenticatedUserIdOfBcryptUserToConvert = createUserToConvert.body().jsonPath().getLong("data.authenticatedUser.id");
        long BuiltinUserIdOfBcryptUserToConvert = createUserToConvert.body().jsonPath().getLong("data.user.id");
        String emailOfNonBcryptUserToConvert = createUserToConvert.body().jsonPath().getString("data.authenticatedUser.email");
        String usernameOfNonBcryptUserToConvert = UtilIT.getUsernameFromResponse(createUserToConvert);
        System.out.println("usernameOfBcryptUserToConvert: " + usernameOfNonBcryptUserToConvert);
        String newEmailAddressToUse = "builtin2shib." + UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";
//        String password = "sha-1Pass";
        String password = usernameOfNonBcryptUserToConvert;
        Response convertToSha1 = convertUserFromBcryptToSha1(BuiltinUserIdOfBcryptUserToConvert, password);
        convertToSha1.prettyPrint();
        convertToSha1.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response setAllowApiTokenLookupViaApi = UtilIT.setSetting(SettingsServiceBean.Key.AllowApiTokenLookupViaApi, "true");
        setAllowApiTokenLookupViaApi.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        password = "sha-1Pass";
        Response getApiTokenUsingUsername = UtilIT.getApiTokenUsingUsername(usernameOfNonBcryptUserToConvert, password);
        assertEquals(200, getApiTokenUsingUsername.getStatusCode());
        
        Response removeAllowApiTokenLookupViaApi = UtilIT.deleteSetting(SettingsServiceBean.Key.AllowApiTokenLookupViaApi);
        removeAllowApiTokenLookupViaApi.then().assertThat()
                .statusCode(200);

        String data = emailOfNonBcryptUserToConvert + ":" + password + ":" + newEmailAddressToUse;
        System.out.println("data: " + data);

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        String dataWithBadPassword = emailOfNonBcryptUserToConvert + ":" + "badPassword" + ":" + newEmailAddressToUse;
        Response makeShibUserWrongSha1Password = UtilIT.migrateBuiltinToShib(dataWithBadPassword, superuserApiToken);
        makeShibUserWrongSha1Password.prettyPrint();
        makeShibUserWrongSha1Password.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("[\"User doesn't know password.\"]"));

        Response makeShibUser = UtilIT.migrateBuiltinToShib(data, superuserApiToken);
        makeShibUser.prettyPrint();
        makeShibUser.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.affiliation", equalTo("TestShib Test IdP"));
        
        String newUsername = "newUser_" + UtilIT.getRandomString(4);
        System.out.println("newUsername for change shib user: " + newUsername);
        Response renameShib = UtilIT.changeAuthenticatedUserIdentifier(usernameOfNonBcryptUserToConvert, newUsername, superuserApiToken);
        renameShib.prettyPrint();
        renameShib.then().assertThat()
                .statusCode(OK.getStatusCode()); 

    }

    @Test
    public void testUsernameCaseSensitivity() {
        String randomUsername = UtilIT.getRandomIdentifier();
        String lowercaseUsername = randomUsername.toLowerCase();
        String uppercaseUsername = randomUsername.toUpperCase();
        String randomEmailForLowercaseuser = UtilIT.getRandomIdentifier() + "@mailinator.com";
        String randomEmailForUppercaseuser = UtilIT.getRandomIdentifier() + "@mailinator.com";

        // Create first user (username all lower case).
        Response createLowercaseUser = UtilIT.createUser(lowercaseUsername, randomEmailForLowercaseuser);
        createLowercaseUser.prettyPrint();
        createLowercaseUser.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Attempt to create second user (same username but all UPPER CASE).
        Response createUppercaseUser = UtilIT.createUser(uppercaseUsername, randomEmailForUppercaseuser);
        createUppercaseUser.prettyPrint();
        createUppercaseUser.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                /**
                 * Technically, it's the lowercase version that exists but the
                 * point gets across. There's currently no way to bubble up the
                 * exact username it's in conflict with, even if we wanted to.
                 */
                .body("message", equalTo("username '" + uppercaseUsername + "' already exists"));
        ;
    }
    
    @Test
    public void testAPITokenEndpoints() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        
        String userApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response getExpiration = UtilIT.getTokenExpiration("BAD-TOKEN-692134794");
        getExpiration.prettyPrint();
        getExpiration.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());
        
        getExpiration = UtilIT.getTokenExpiration(userApiToken);
        getExpiration.prettyPrint();
        getExpiration.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", containsString(userApiToken))
                .body("data.message", containsString("expires on"));

        // Recreate given a bad API token
        Response recreateToken = UtilIT.recreateToken("BAD-Token-blah-89234");
        recreateToken.prettyPrint();
        recreateToken.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());

        // Recreate given a valid API token
        recreateToken = UtilIT.recreateToken(userApiToken);
        recreateToken.prettyPrint();
        recreateToken.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", containsString("New token for"))
                .body("data.message", CoreMatchers.not(containsString("and expires on")));

        // Recreate given a valid API token and returning expiration
        createUser = UtilIT.createRandomUser();
        assertEquals(OK.getStatusCode(), createUser.getStatusCode());

        userApiToken =  UtilIT.getApiTokenFromResponse(createUser);

        recreateToken = UtilIT.recreateToken(userApiToken, true);
        recreateToken.prettyPrint();
        recreateToken.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", containsString("New token for"))
                .body("data.message", containsString("and expires on"));

        /*
        Add tests for Private URL
        */
        
        createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        
        Response createPrivateUrl = UtilIT.privateUrlCreate(datasetId, apiToken, false);
        createPrivateUrl.prettyPrint();
        assertEquals(OK.getStatusCode(), createPrivateUrl.getStatusCode());

        Response shouldExist = UtilIT.privateUrlGet(datasetId, apiToken);
        shouldExist.prettyPrint();
        assertEquals(OK.getStatusCode(), shouldExist.getStatusCode());

        String tokenForPrivateUrlUser = JsonPath.from(shouldExist.body().asString()).getString("data.token");
        
        getExpiration = UtilIT.getTokenExpiration(tokenForPrivateUrlUser);
        getExpiration.prettyPrint();
        getExpiration.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());

        createUser = UtilIT.createRandomUser();
        assertEquals(OK.getStatusCode(), createUser.getStatusCode());

        String userApiTokenForDelete = UtilIT.getApiTokenFromResponse(createUser);

        Response deleteToken = UtilIT.deleteToken(userApiTokenForDelete);
        deleteToken.prettyPrint();
        deleteToken.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", containsString(" deleted."));

        //Make sure it's deleted
        getExpiration = UtilIT.getTokenExpiration(userApiTokenForDelete);
        getExpiration.prettyPrint();
        getExpiration.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());
        
    }
    
    @Test
    public void testDeleteAuthenticatedUser() {

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String usernameForCreateDV = UtilIT.getUsernameFromResponse(createUser);
        String normalApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(normalApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String usernameForAssignedRole = UtilIT.getUsernameFromResponse(createUser);
        String roleApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response assignRole = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.EDITOR.toString(),
                "@" + usernameForAssignedRole, superuserApiToken);

        //Shouldn't be able to delete user with a role
        Response deleteUserRole = UtilIT.deleteUser(usernameForAssignedRole);

        deleteUserRole.prettyPrint();
        deleteUserRole.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Could not delete Authenticated User @" + usernameForAssignedRole + " because the user is associated with role assignment record(s)."));

        //Shouldn't be able to delete a user who has created a DV
        Response deleteUserCreateDV = UtilIT.deleteUser(usernameForCreateDV);

        deleteUserCreateDV.prettyPrint();
        deleteUserCreateDV.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Could not delete Authenticated User @" + usernameForCreateDV + " because the user has created Dataverse object(s); the user is associated with role assignment record(s)."));

        Response deleteDataverse = UtilIT.deleteDataverse(dataverseAlias, normalApiToken);
        deleteDataverse.prettyPrint();
        deleteDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response deleteUserAfterDeleteDV = UtilIT.deleteUser(usernameForCreateDV);
        //Should be able to delete user after dv is deleted
        deleteUserAfterDeleteDV.prettyPrint();
        deleteUserAfterDeleteDV.then().assertThat()
                .statusCode(OK.getStatusCode());

        deleteUserAfterDeleteDV = UtilIT.deleteUser(usernameForAssignedRole);
        //Should be able to delete user after dv is deleted role should be gone as well
        deleteUserAfterDeleteDV.prettyPrint();
        deleteUserAfterDeleteDV.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response deleteSuperuser = UtilIT.deleteUser(superuserUsername);
        assertEquals(200, deleteSuperuser.getStatusCode());

    }

    @Test
    public void testUserPermittedDataverses() {
        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        String usernameOfUser = UtilIT.getUsernameFromResponse(createUser);
        String userApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse1 = UtilIT.createRandomDataverse(superuserApiToken);
        createDataverse1.prettyPrint();
        createDataverse1.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias1 = UtilIT.getAliasFromResponse(createDataverse1);

        // create a second Dataverse and add a Group with permissions
        Response createDataverse2 = UtilIT.createRandomDataverse(superuserApiToken);
        createDataverse2.prettyPrint();
        createDataverse2.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias2 = UtilIT.getAliasFromResponse(createDataverse2);
        String aliasInOwner = "groupFor" + dataverseAlias2;
        String displayName = "Group for " + dataverseAlias2;
        Response createGroup = UtilIT.createGroup(dataverseAlias2, aliasInOwner, displayName, superuserApiToken);
        String groupIdentifier = JsonPath.from(createGroup.asString()).getString("data.identifier");
        Response grantRoleResponse = UtilIT.grantRoleOnDataverse(dataverseAlias2, DataverseRole.EDITOR.toString(), groupIdentifier, superuserApiToken);
        grantRoleResponse.prettyPrint();
        grantRoleResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response collectionsResp = UtilIT.getUserPermittedCollections(superuserUsername, userApiToken, "ViewUnpublishedDataset");
        collectionsResp.prettyPrint();
        assertEquals(403, collectionsResp.getStatusCode());
        collectionsResp = UtilIT.getUserPermittedCollections(usernameOfUser, userApiToken, "ViewUnpublishedDataset");
        collectionsResp.prettyPrint();
        assertEquals(200, collectionsResp.getStatusCode());
        collectionsResp = UtilIT.getUserPermittedCollections(usernameOfUser, "", "ViewUnpublishedDataset");
        assertEquals(401, collectionsResp.getStatusCode());
        collectionsResp = UtilIT.getUserPermittedCollections("fakeUser", superuserApiToken, "ViewUnpublishedDataset");
        assertEquals(500, collectionsResp.getStatusCode());
        collectionsResp = UtilIT.getUserPermittedCollections(usernameOfUser, superuserApiToken, "bad");
        assertEquals(500, collectionsResp.getStatusCode());

        // Testing adding an explicit permission/role to one dataverse
        collectionsResp = UtilIT.getUserPermittedCollections(usernameOfUser, userApiToken, "DownloadFile");
        collectionsResp.prettyPrint();
        collectionsResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.count", equalTo(0));

        Response assignRole = UtilIT.grantRoleOnDataverse(dataverseAlias1, DataverseRole.EDITOR.toString(),
                "@" + usernameOfUser, superuserApiToken);
        assignRole.prettyPrint();
        assertEquals(200, assignRole.getStatusCode());

        collectionsResp = UtilIT.getUserPermittedCollections(usernameOfUser, userApiToken, "DownloadFile");
        collectionsResp.prettyPrint();
        collectionsResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.count", equalTo(1));

        // Add user to group and test with both explicit and group permissions
        Response addToGroup = UtilIT.addToGroup(dataverseAlias2, aliasInOwner, List.of("@" + usernameOfUser), superuserApiToken);
        addToGroup.prettyPrint();
        addToGroup.then().assertThat()
                .statusCode(OK.getStatusCode());

        collectionsResp = UtilIT.getUserPermittedCollections(usernameOfUser, userApiToken, "DownloadFile");
        collectionsResp.prettyPrint();
        collectionsResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.count", equalTo(2));
    }

    // This test is disabled because it is only compatible with the containerized development environment and would cause the Jenkins job to fail.
    @Disabled
    public void testRegisterOIDCUser() {
        // Set Up - Get the admin access token from the OIDC provider
        Response adminOidcLoginResponse = UtilIT.performKeycloakROPCLogin("admin", "admin");
        adminOidcLoginResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("access_token", notNullValue());
        String adminOidcAccessToken = adminOidcLoginResponse.jsonPath().getString("access_token");

        // Set Up - Create random user in the OIDC provider without some necessary claims (email, firstName and lastName)
        String randomUsername = UUID.randomUUID().toString().substring(0, 8);

        String newKeycloakUserWithoutClaimsJson = "{"
                + "\"username\":\"" + randomUsername + "\","
                + "\"enabled\":true,"
                + "\"credentials\":["
                + "  {"
                + "    \"type\":\"password\","
                + "    \"value\":\"password\","
                + "    \"temporary\":false"
                + "  }"
                + "]"
                + "}";

        Response createKeycloakOidcUserResponse = UtilIT.createKeycloakUser(adminOidcAccessToken, newKeycloakUserWithoutClaimsJson);
        createKeycloakOidcUserResponse.then().assertThat().statusCode(CREATED.getStatusCode());

        Response newUserOidcLoginResponse = UtilIT.performKeycloakROPCLogin(randomUsername, "password");
        String userWithoutClaimsAccessToken = newUserOidcLoginResponse.jsonPath().getString("access_token");

        // Set Up - Create a second random user in the OIDC provider with all necessary claims (including email, firstName and lastName)
        randomUsername = UUID.randomUUID().toString().substring(0, 8);
        String email = randomUsername + "@dataverse.org";
        String firstName = "John";
        String lastName = "Doe";

        String newKeycloakUserWithClaimsJson = "{"
                + "\"username\":\"" + randomUsername + "\","
                + "\"enabled\":true,"
                + "\"email\":\"" + email + "\","
                + "\"firstName\":\"" + firstName + "\","
                + "\"lastName\":\"" + lastName + "\","
                + "\"credentials\":["
                + "  {"
                + "    \"type\":\"password\","
                + "    \"value\":\"password\","
                + "    \"temporary\":false"
                + "  }"
                + "]"
                + "}";

        Response createKeycloakOidcUserWithClaimsResponse = UtilIT.createKeycloakUser(adminOidcAccessToken, newKeycloakUserWithClaimsJson);
        createKeycloakOidcUserWithClaimsResponse.then().assertThat().statusCode(CREATED.getStatusCode());

        Response newUserWithClaimsOidcLoginResponse = UtilIT.performKeycloakROPCLogin(randomUsername, "password");
        String userWithClaimsAccessToken = newUserWithClaimsOidcLoginResponse.jsonPath().getString("access_token");

        // Should return error when empty token is passed
        Response registerOidcUserResponse = UtilIT.registerOidcUser(
                "{}",
                ""
        );
        registerOidcUserResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(BundleUtil.getStringFromBundle("users.api.errors.bearerTokenRequired")));

        // Should return error when a malformed User JSON is sent
        registerOidcUserResponse = UtilIT.registerOidcUser(
                "{{{user:abcde}",
                "Bearer testBearerToken"
        );
        registerOidcUserResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Error parsing the POSTed User json: Invalid token=CURLYOPEN at (line no=1, column no=2, offset=1). Expected tokens are: [STRING]"));

        // Should return error when the provided User JSON is valid but the provided Bearer token is invalid
        registerOidcUserResponse = UtilIT.registerOidcUser(
                "{"
                        + "\"termsAccepted\":true"
                        + "}",
                "Bearer testBearerToken"
        );
        registerOidcUserResponse.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode())
                .body("message", equalTo("Unauthorized bearer token."));

        // Should return an error when the termsAccepted field is missing in the User JSON
        registerOidcUserResponse = UtilIT.registerOidcUser(
                "{"
                        + "\"affiliation\":\"YourAffiliation\","
                        + "\"position\":\"YourPosition\""
                        + "}",
                "Bearer testBearerToken"
        );
        registerOidcUserResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Error parsing the POSTed User json: Field 'termsAccepted' is mandatory"));

        // Should return an error when the Bearer token is valid but required claims are missing in the IdP, needing completion from the request JSON
        registerOidcUserResponse = UtilIT.registerOidcUser(
                "{"
                        + "\"termsAccepted\":true"
                        + "}",
                "Bearer " + userWithoutClaimsAccessToken
        );
        registerOidcUserResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.invalidFields")))
                .body("fieldErrors.firstName", equalTo(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldRequired", List.of("firstName"))))
                .body("fieldErrors.lastName", equalTo(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldRequired", List.of("lastName"))))
                .body("fieldErrors.emailAddress", equalTo(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldRequired", List.of("emailAddress"))));

        // Should register user when the Bearer token is valid and the provided User JSON contains the missing claims in the IdP
        registerOidcUserResponse = UtilIT.registerOidcUser(
                "{"
                        + "\"firstName\":\"testFirstName\","
                        + "\"lastName\":\"testLastName\","
                        + "\"emailAddress\":\"" + UUID.randomUUID().toString().substring(0, 8) + "@dataverse.org\","
                        + "\"termsAccepted\":true"
                        + "}",
                "Bearer " + userWithoutClaimsAccessToken
        );
        registerOidcUserResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("User registered."));

        // Should return error when attempting to re-register with the same Bearer token but different User data
        String newUserJson = "{"
                + "\"firstName\":\"newFirstName\","
                + "\"lastName\":\"newLastName\","
                + "\"emailAddress\":\"newEmail@dataverse.com\","
                + "\"termsAccepted\":true"
                + "}";
        registerOidcUserResponse = UtilIT.registerOidcUser(
                newUserJson,
                "Bearer " + userWithoutClaimsAccessToken
        );
        registerOidcUserResponse.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo("User is already registered with this token."));

        // Should return an error when the Bearer token is valid and attempting to set JSON properties that conflict with existing claims in the IdP
        registerOidcUserResponse = UtilIT.registerOidcUser(
                "{"
                        + "\"firstName\":\"testFirstName\","
                        + "\"lastName\":\"testLastName\","
                        + "\"emailAddress\":\"" + UUID.randomUUID().toString().substring(0, 8) + "@dataverse.org\","
                        + "\"termsAccepted\":true"
                        + "}",
                "Bearer " + userWithClaimsAccessToken
        );
        registerOidcUserResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("fieldErrors.firstName", equalTo(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldAlreadyPresentInProvider", List.of("firstName"))))
                .body("fieldErrors.lastName", equalTo(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldAlreadyPresentInProvider", List.of("lastName"))))
                .body("fieldErrors.emailAddress", equalTo(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldAlreadyPresentInProvider", List.of("emailAddress"))));

        // Should register user when the Bearer token is valid and all required claims are present in the IdP, requiring only minimal data in the User JSON
        registerOidcUserResponse = UtilIT.registerOidcUser(
                "{"
                        + "\"termsAccepted\":true"
                        + "}",
                "Bearer " + userWithClaimsAccessToken
        );
        registerOidcUserResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("User registered."));
    }

    private Response convertUserFromBcryptToSha1(long idOfBcryptUserToConvert, String password) {
        JsonObjectBuilder data = Json.createObjectBuilder();
        data.add("builtinUserId", idOfBcryptUserToConvert);
        data.add("password", password);
        Response response = given()
                .contentType(ContentType.JSON)
                .body(data.build().toString())
                .post("/api/admin/convertUserFromBcryptToSha1");
        return response;
    }

}
