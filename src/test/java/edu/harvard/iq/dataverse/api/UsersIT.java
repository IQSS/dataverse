package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class UsersIT {

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
        
        Response removeAllowApiTokenLookupViaApi = UtilIT.deleteSetting(SettingsServiceBean.Key.AllowApiTokenLookupViaApi);
        removeAllowApiTokenLookupViaApi.then().assertThat()
                .statusCode(200);

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
        try {
            Thread.sleep(1000); //Added because tests are failing during setup, test is probably going too fast. Especially between first and second file
        } catch (InterruptedException ex) {
            Logger.getLogger(UsersIT.class.getName()).log(Level.SEVERE, null, ex);
        }
        Response tab3AddResponse = UtilIT.uploadFileViaNative(datasetIdNew.toString(), tab3PathToFile, superuserApiToken);
        Integer tabFile3IdRestrictedNew = JsonPath.from(tab3AddResponse.body().asString()).getInt("data.files[0].dataFile.id");
        try {
            Thread.sleep(3000); //Dataverse needs more time...
        } catch (InterruptedException ex) {
            Logger.getLogger(UsersIT.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";

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
        Response createGroup = UtilIT.createGroup(dataverseAlias, aliasInOwner, displayName, superuserApiToken);
        createGroup.prettyPrint();
        createGroup.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String groupIdentifier = JsonPath.from(createGroup.asString()).getString("data.identifier");

        List<String> roleAssigneesToAdd = new ArrayList<>();
        roleAssigneesToAdd.add(user2identifier);
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
