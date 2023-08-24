package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The following query has been helpful in discovering places where user ids
 * appear throughout the database. Here's a summary of how user deletion affects
 * these tables.
 *
 * - apitoken: Not a concern. Tokens are deleted.
 *
 * - authenticateduserlookup: Not a concern. Rows are deleted.
 *
 * - confirmemaildata: Not a concern. Rows are deleted.
 *
 * - datasetlock: Not a concern, locks are deleted.
 *
 * - datasetversionuser: Definitely a concern. This table is what feeds the
 * "Contributors" list under the "Version" tab on the dataset page. You can't
 * delete the user. You can merge the user but the name under "Contributors"
 * will change to the user you merged into. There is talk of implementing the
 * concept of disabling users to handle this.
 *
 * - dvobject (creator_id): Definitely a concern. You can't delete a user. You
 * have to merge instead.
 *
 * - dvobject (releaseuser_id): Definitely a concern. You can't delete a user.
 * You have to merge instead. It seems that for files, releaseuser_id is not
 * populated.
 *
 * - explicitgroup: Not a concern. Group membership is deleted.
 *
 * - fileaccessrequests: Not a concern. File requests are deleted.
 *
 * - guestbookresponse: Definitely a concern but it's possible to null out the
 * user id. You can't delete a user but you can merge instead. There is talk of
 * deactivate which would probably null out the id. In all cases the name and
 * email address in the rows are left alone.
 *
 * - oauth2tokendata: Not a concern. Rows are deleted.
 *
 * - savedsearch: Definitely a concern. You can't delete a user. You have to
 * merge.
 *
 * - userbannermessage: Not a concern. Rows are deleted.
 *
 * - usernotification (user_id): Not a concern. Deleted by a cascade.
 *
 * - usernotification (requestor_id): Not a big concern because of other
 * constraints. This is only populated by "submit for review" (so that the
 * curator has the name and email address of the author). All these
 * notifications would be deleted by a cascade but deleting the user itself is
 * prevented because the user recorded in the datasetversionuser table. (Both
 * "submit for review" and "return to author" add you to that table.) So the
 * bottom line is that the user can't be deleted. It has to be merged.
 *
 * - workflowcomment: Not a big concern because of other constraints. A workflow
 * comment is optionally added as part of "return to author" but this also
 * creates a row in the datasetversionuser table which means the user can't be
 * deleted. It has to be merged instead.
 *
 *
 * The tables that aren't captured above are actionlogrecord and roleassignment
 * because the relationship is to the identifier (username) rather than the id.
 * So we'll list them separately:
 *
 * - actionlogrecord: Not a concern. Delete can go through. On merge, they are
 * changed from one user identifier to another.
 *
 * - roleassignment: Not a concern. Delete can go through. On merge, they are
 * changed from one user identifier to another.
 */
/*
           table_name            |                        constraint_name                         
---------------------------------+----------------------------------------------------------------
 apitoken                        | fk_apitoken_authenticateduser_id
 authenticateduserlookup         | fk_authenticateduserlookup_authenticateduser_id
 confirmemaildata                | fk_confirmemaildata_authenticateduser_id
 datasetlock                     | fk_datasetlock_user_id
 datasetversionuser              | fk_datasetversionuser_authenticateduser_id
 dvobject                        | fk_dvobject_creator_id
 dvobject                        | fk_dvobject_releaseuser_id
 explicitgroup_authenticateduser | explicitgroup_authenticateduser_containedauthenticatedusers_id
 fileaccessrequests              | fk_fileaccessrequests_authenticated_user_id
 guestbookresponse               | fk_guestbookresponse_authenticateduser_id
 oauth2tokendata                 | fk_oauth2tokendata_user_id
 savedsearch                     | fk_savedsearch_creator_id
 userbannermessage               | fk_userbannermessage_user_id
 usernotification                | fk_usernotification_user_id
 usernotification                | fk_usernotification_requestor_id
 workflowcomment                 | fk_workflowcomment_authenticateduser_id
(16 rows)

-- https://stackoverflow.com/questions/5347050/postgresql-sql-script-to-get-a-list-of-all-tables-that-has-a-particular-column
select R.TABLE_NAME, R.CONSTRAINT_NAME
from INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE u
inner join INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS FK
    on U.CONSTRAINT_CATALOG = FK.UNIQUE_CONSTRAINT_CATALOG
    and U.CONSTRAINT_SCHEMA = FK.UNIQUE_CONSTRAINT_SCHEMA
    and U.CONSTRAINT_NAME = FK.UNIQUE_CONSTRAINT_NAME
inner join INFORMATION_SCHEMA.KEY_COLUMN_USAGE R
    ON R.CONSTRAINT_CATALOG = FK.CONSTRAINT_CATALOG
    AND R.CONSTRAINT_SCHEMA = FK.CONSTRAINT_SCHEMA
    AND R.CONSTRAINT_NAME = FK.CONSTRAINT_NAME
WHERE U.COLUMN_NAME = 'id'
--  AND U.TABLE_CATALOG = 'b'
--  AND U.TABLE_SCHEMA = 'c'
  AND U.TABLE_NAME = 'authenticateduser'
ORDER BY R.TABLE_NAME;
 */
public class DeleteUsersIT {

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testDeleteRolesAndUnpublishedDataverse() {

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

        Response getTraces1 = UtilIT.getUserTraces(usernameForCreateDV, superuserApiToken);
        getTraces1.prettyPrint();
        getTraces1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.user.identifier", equalTo("@" + usernameForCreateDV))
                // traces is {} when user hasn't left a trace
                .body("data.traces", equalTo(Collections.emptyMap()));

        Response createDataverse = UtilIT.createRandomDataverse(normalApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response getTraces2 = UtilIT.getUserTraces(usernameForCreateDV, superuserApiToken);
        getTraces2.prettyPrint();
        getTraces2.then().assertThat().statusCode(OK.getStatusCode());

        if (true) {
            return;
        }

        createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String usernameForAssignedRole = UtilIT.getUsernameFromResponse(createUser);
        String roleApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response assignRole = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.EDITOR.toString(),
                "@" + usernameForAssignedRole, superuserApiToken);

        // Shouldn't be able to delete user with a role
        Response deleteUserRole = UtilIT.deleteUser(usernameForAssignedRole);

        deleteUserRole.prettyPrint();
        deleteUserRole.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Could not delete Authenticated User @" + usernameForAssignedRole + " because the user is associated with role assignment record(s)."));

        // Now remove that role
        Response removeRoles1 = UtilIT.deleteUserRoles(usernameForAssignedRole, superuserApiToken);
        removeRoles1.prettyPrint();
        removeRoles1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Roles removed for user " + usernameForAssignedRole + "."));

        // Now the delete should work
        Response deleteUserRole2 = UtilIT.deleteUser(usernameForAssignedRole);
        deleteUserRole2.prettyPrint();
        deleteUserRole2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("AuthenticatedUser @" + usernameForAssignedRole + " deleted. "));

        // The owner of the dataverse that was just created is dataverseAdmin because it created the parent dataverse (root).
        Response getTraces3 = UtilIT.getUserTraces(usernameForCreateDV, superuserApiToken);
        getTraces3.prettyPrint();
        getTraces3.then().assertThat().statusCode(OK.getStatusCode());

        // Removing roles here but could equally just delete the dataverse.
        Response removeRoles2 = UtilIT.deleteUserRoles(usernameForCreateDV, superuserApiToken);
        removeRoles2.prettyPrint();
        removeRoles2.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Shouldn't be able to delete a user who has created a DV
        Response deleteUserCreateDV = UtilIT.deleteUser(usernameForCreateDV);
        deleteUserCreateDV.prettyPrint();
        deleteUserCreateDV.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Could not delete Authenticated User @" + usernameForCreateDV + " because the user has created Dataverse object(s)."));

        Response deleteDataverse = UtilIT.deleteDataverse(dataverseAlias, superuserApiToken);
        deleteDataverse.prettyPrint();
        deleteDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Should be able to delete user after dv is deleted
        Response deleteUserAfterDeleteDV = UtilIT.deleteUser(usernameForCreateDV);
        deleteUserAfterDeleteDV.prettyPrint();
        deleteUserAfterDeleteDV.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response deleteSuperuser = UtilIT.deleteUser(superuserUsername);
        deleteSuperuser.prettyPrint();
        assertEquals(200, deleteSuperuser.getStatusCode());

    }

    @Test
    public void testDeleteUserWithUnPublishedDataverse() {

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response removeRoles1 = UtilIT.deleteUserRoles(username, superuserApiToken);
        removeRoles1.prettyPrint();
        removeRoles1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Roles removed for user " + username + "."));

        Response deleteUser1 = UtilIT.deleteUser(username);
        deleteUser1.prettyPrint();
        deleteUser1.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Could not delete Authenticated User @" + username + " because the user has created Dataverse object(s)."));

        Response traces = UtilIT.getUserTraces(username, superuserApiToken);
        traces.prettyPrint();
        traces.then().assertThat().statusCode(OK.getStatusCode());

        // You can't delete. You have to merge.
        Response mergeAccounts = UtilIT.mergeAccounts(superuserUsername, username, superuserApiToken);
        mergeAccounts.prettyPrint();
        mergeAccounts.then().assertThat().statusCode(OK.getStatusCode());
    }

    /**
     * You can't delete an account with guestbook entries so you have to merge
     * it instead.
     */
    @Test
    public void testDeleteUserWithGuestbookEntries() throws IOException {

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String authorUsername = UtilIT.getUsernameFromResponse(createUser);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response downloader = UtilIT.createRandomUser();
        downloader.prettyPrint();
        String downloaderUsername = UtilIT.getUsernameFromResponse(downloader);
        String downloaderApiToken = UtilIT.getApiTokenFromResponse(downloader);

        Response createDataverse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        Path pathtoReadme = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "README.md");
        java.nio.file.Files.write(pathtoReadme, "In the beginning...".getBytes());

        Response uploadReadme = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme.toString(), authorApiToken);
        uploadReadme.prettyPrint();
        uploadReadme.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("README.md"));

        int fileId = JsonPath.from(uploadReadme.body().asString()).getInt("data.files[0].dataFile.id");

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, authorApiToken);
        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", authorApiToken);
        publishDataset.then().assertThat().statusCode(OK.getStatusCode());
        // This download creates a guestbook entry.
        Response downloadFile = UtilIT.downloadFile(fileId, downloaderApiToken);
        downloadFile.then().assertThat().statusCode(OK.getStatusCode());

        // We can't delete the downloader because a guestbook record (a download) has been created.
        Response deleteDownloaderFail = UtilIT.deleteUser(downloaderUsername);
        deleteDownloaderFail.prettyPrint();
        deleteDownloaderFail.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

        // Let's see why we can't download.
        Response getTraces = UtilIT.getUserTraces(downloaderUsername, superuserApiToken);
        getTraces.prettyPrint();
        getTraces.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.traces.guestbookEntries.count", equalTo(1));

        // We can't delete so we do a merge instead.
        Response mergeAccounts = UtilIT.mergeAccounts(superuserUsername, downloaderUsername, superuserApiToken);
        mergeAccounts.prettyPrint();
        mergeAccounts.then().assertThat().statusCode(OK.getStatusCode());

    }

    @Test
    public void testDatasetLocks() throws IOException {

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String authorUsername = UtilIT.getUsernameFromResponse(createUser);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response downloader = UtilIT.createRandomUser();
        downloader.prettyPrint();
        String downloaderUsername = UtilIT.getUsernameFromResponse(downloader);
        String downloaderApiToken = UtilIT.getApiTokenFromResponse(downloader);

        Response createDataverse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        Response lockDatasetResponse = UtilIT.lockDataset(datasetId.longValue(), "Ingest", superuserApiToken);
        lockDatasetResponse.prettyPrint();
        lockDatasetResponse.then().assertThat()
                .body("data.message", equalTo("dataset locked with lock type Ingest"))
                .statusCode(200);

        Response checkDatasetLocks = UtilIT.checkDatasetLocks(datasetId.longValue(), "Ingest", superuserApiToken);
        checkDatasetLocks.prettyPrint();
        checkDatasetLocks.then().assertThat()
                .body("data[0].lockType", equalTo("Ingest"))
                .statusCode(200);
        Response deleteUserWhoCreatedLock = UtilIT.deleteUser(superuserUsername);
        deleteUserWhoCreatedLock.prettyPrint();
        deleteUserWhoCreatedLock.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testDeleteUserWhoIsMemberOfGroup() throws IOException {

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String authorUsername = UtilIT.getUsernameFromResponse(createUser);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response downloader = UtilIT.createRandomUser();
        downloader.prettyPrint();
        String downloaderUsername = UtilIT.getUsernameFromResponse(downloader);
        String downloaderApiToken = UtilIT.getApiTokenFromResponse(downloader);

        Response createDataverse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createGroupMember = UtilIT.createRandomUser();
        createGroupMember.prettyPrint();
        String groupMemberUsername = UtilIT.getUsernameFromResponse(createGroupMember);
        String groupMemberApiToken = UtilIT.getApiTokenFromResponse(createGroupMember);

        String aliasInOwner = "groupFor" + dataverseAlias;
        String displayName = "Group for " + dataverseAlias;
        String user2identifier = "@" + groupMemberUsername;
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

        Response getTraces = UtilIT.getUserTraces(groupMemberUsername, superuserApiToken);
        getTraces.prettyPrint();
        getTraces.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteUserInGroup = UtilIT.deleteUser(groupMemberUsername);
        deleteUserInGroup.prettyPrint();
        deleteUserInGroup.then().assertThat()
                .statusCode(OK.getStatusCode());

    }

    @Test
    public void testDeleteUserWithFileAccessRequests() throws IOException {

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String authorUsername = UtilIT.getUsernameFromResponse(createUser);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response fileRequester = UtilIT.createRandomUser();
        fileRequester.prettyPrint();
        String fileRequesterUsername = UtilIT.getUsernameFromResponse(fileRequester);
        String fileRequesterApiToken = UtilIT.getApiTokenFromResponse(fileRequester);

        Response createDataverse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        Path pathtoReadme = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "README.md");
        java.nio.file.Files.write(pathtoReadme, "In the beginning...".getBytes());

        Response uploadReadme = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoReadme.toString(), authorApiToken);
        uploadReadme.prettyPrint();
        uploadReadme.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("README.md"));

        Integer fileId = JsonPath.from(uploadReadme.body().asString()).getInt("data.files[0].dataFile.id");

        Response restrictResponse = UtilIT.restrictFile(fileId.toString(), true, authorApiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat().statusCode(OK.getStatusCode());

        //Update Dataset to allow requests
        Response allowAccessRequestsResponse = UtilIT.allowAccessRequests(datasetPid, true, authorApiToken);
        allowAccessRequestsResponse.prettyPrint();
        allowAccessRequestsResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, authorApiToken);
        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", authorApiToken);
        publishDataset.then().assertThat().statusCode(OK.getStatusCode());

        Response requestFileAccessResponse = UtilIT.requestFileAccess(fileId.toString(), fileRequesterApiToken);
        requestFileAccessResponse.prettyPrint();
        requestFileAccessResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Let's see why we can't download.
        Response getTraces = UtilIT.getUserTraces(fileRequesterUsername, superuserApiToken);
        getTraces.prettyPrint();
        getTraces.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Even if users have outstanding file requests, they can be deleted.
        Response deleteDownloaderSuccess = UtilIT.deleteUser(fileRequesterUsername);
        deleteDownloaderSuccess.prettyPrint();
        deleteDownloaderSuccess.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testCuratorSendsCommentsToAuthor() throws InterruptedException {
        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createCurator1 = UtilIT.createRandomUser();
        createCurator1.prettyPrint();
        createCurator1.then().assertThat()
                .statusCode(OK.getStatusCode());
        String curator1Username = UtilIT.getUsernameFromResponse(createCurator1);
        String curator1ApiToken = UtilIT.getApiTokenFromResponse(createCurator1);

        Response createCurator2 = UtilIT.createRandomUser();
        createCurator2.prettyPrint();
        createCurator2.then().assertThat()
                .statusCode(OK.getStatusCode());
        String curator2Username = UtilIT.getUsernameFromResponse(createCurator2);
        String curator2ApiToken = UtilIT.getApiTokenFromResponse(createCurator2);

        Response createDataverseResponse = UtilIT.createRandomDataverse(curator1ApiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response makeCurator2Admin = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.ADMIN.toString(), "@" + curator2Username, curator1ApiToken);
        makeCurator2Admin.prettyPrint();
        makeCurator2Admin.then().assertThat()
                .body("data.assignee", equalTo("@" + curator2Username))
                .body("data._roleAlias", equalTo("admin"))
                .statusCode(OK.getStatusCode());

        Response createAuthor1 = UtilIT.createRandomUser();
        createAuthor1.prettyPrint();
        createAuthor1.then().assertThat()
                .statusCode(OK.getStatusCode());
        String author1Username = UtilIT.getUsernameFromResponse(createAuthor1);
        String author1ApiToken = UtilIT.getApiTokenFromResponse(createAuthor1);

        Response createAuthor2 = UtilIT.createRandomUser();
        createAuthor2.prettyPrint();
        createAuthor2.then().assertThat()
                .statusCode(OK.getStatusCode());
        String author2Username = UtilIT.getUsernameFromResponse(createAuthor2);
        String author2ApiToken = UtilIT.getApiTokenFromResponse(createAuthor2);

        Response grantAuthor1AddDataset = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.DS_CONTRIBUTOR.toString(), "@" + author1Username, curator1ApiToken);
        grantAuthor1AddDataset.prettyPrint();
        grantAuthor1AddDataset.then().assertThat()
                .body("data.assignee", equalTo("@" + author1Username))
                .body("data._roleAlias", equalTo("dsContributor"))
                .statusCode(OK.getStatusCode());

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, author1ApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        // FIXME: have the initial create return the DOI or Handle to obviate the need for this call.
        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(datasetId, author1ApiToken);
        getDatasetJsonBeforePublishing.prettyPrint();
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.identifier");

        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;
        System.out.println("datasetPersistentId: " + datasetPersistentId);

//        Response grantAuthor2ContributorOnDataset = UtilIT.grantRoleOnDataset(datasetPersistentId, DataverseRole.DS_CONTRIBUTOR.toString(), "@" + author2Username, curatorApiToken);
        // TODO: Tighten this down to something more realistic than ADMIN.
        Response grantAuthor2ContributorOnDataset = UtilIT.grantRoleOnDataset(datasetPersistentId, DataverseRole.ADMIN.toString(), "@" + author2Username, curator1ApiToken);
        grantAuthor2ContributorOnDataset.prettyPrint();
        grantAuthor2ContributorOnDataset.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.assignee", equalTo("@" + author2Username))
                .body("data._roleAlias", equalTo("admin"));

//        // Whoops, the author tries to publish but isn't allowed. The curator will take a look.
//        Response noPermToPublish = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", author1ApiToken);
//        noPermToPublish.prettyPrint();
//        noPermToPublish.then().assertThat()
//                .body("message", equalTo("User @" + author1Username + " is not permitted to perform requested action."))
//                .statusCode(UNAUTHORIZED.getStatusCode());
        Response submitForReview = UtilIT.submitDatasetForReview(datasetPersistentId, author2ApiToken);
        submitForReview.prettyPrint();
        submitForReview.then().assertThat()
                .statusCode(OK.getStatusCode());

        // curator2 returns dataset to author. This makes curator2 a contributor.
        String comments = "You forgot to upload any files.";
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("reasonForReturn", comments);
        Response returnToAuthor = UtilIT.returnDatasetToAuthor(datasetPersistentId, jsonObjectBuilder.build(), curator2ApiToken);
        returnToAuthor.prettyPrint();
        returnToAuthor.then().assertThat()
                .body("data.inReview", equalTo(false))
                .statusCode(OK.getStatusCode());

        Response getTracesForCurator2 = UtilIT.getUserTraces(curator2Username, superuserApiToken);
        getTracesForCurator2.prettyPrint();
        getTracesForCurator2.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response removeRolesFromCurator2 = UtilIT.deleteUserRoles(curator2Username, superuserApiToken);
        removeRolesFromCurator2.prettyPrint();
        removeRolesFromCurator2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Roles removed for user " + curator2Username + "."));

        // Because curator2 returned the dataset to the authors, curator2 is now a contributor
        // and cannot be deleted.
        Response deleteCurator2Fail = UtilIT.deleteUser(curator2Username);
        deleteCurator2Fail.prettyPrint();
        deleteCurator2Fail.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Could not delete Authenticated User @" + curator2Username
                        + " because the user has contributed to dataset version(s)."));

        // What should we do with curator2 instead of deleting? The only option is to merge
        // curator2 into some other account. Once implemented, we'll deactivate curator2's account
        // so that curator2 continues to be displayed as a contributor.
        //
        // TODO: deactivate curator2 here
        //
        // Show the error if you don't have permission.
        Response failToRemoveRole = UtilIT.deleteUserRoles(author2Username, curator2ApiToken);
        failToRemoveRole.prettyPrint();
        failToRemoveRole.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode())
                .body("message", equalTo("User @" + curator2Username + " is not permitted to perform requested action."));

        Response removeRolesFromAuthor2 = UtilIT.deleteUserRoles(author2Username, superuserApiToken);
        removeRolesFromAuthor2.prettyPrint();
        removeRolesFromAuthor2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Roles removed for user " + author2Username + "."));

        // Similarly, we can't delete author2 because author2 submitted
        // the dataset for review, which makes one a contributor.
        Response deleteAuthor2Fail = UtilIT.deleteUser(author2Username);
        deleteAuthor2Fail.prettyPrint();
        deleteAuthor2Fail.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Could not delete Authenticated User @" + author2Username
                        + " because the user has contributed to dataset version(s)."));

    }

}
