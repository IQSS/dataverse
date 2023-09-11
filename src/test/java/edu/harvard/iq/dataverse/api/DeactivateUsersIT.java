package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DeactivateUsersIT {

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testDeactivateUser() {

        Response createSuperuser = UtilIT.createRandomUser();
        createSuperuser.then().assertThat().statusCode(OK.getStatusCode());
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createDataverse = UtilIT.createRandomDataverse(superuserApiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, superuserApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response grantRoleBeforeDeactivate = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.ADMIN.toString(), "@" + username, superuserApiToken);
        grantRoleBeforeDeactivate.prettyPrint();
        grantRoleBeforeDeactivate.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.assignee", equalTo("@" + username))
                .body("data._roleAlias", equalTo("admin"));

        String aliasInOwner = "groupFor" + dataverseAlias;
        String displayName = "Group for " + dataverseAlias;
        String user2identifier = "@" + username;
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

        Response userTracesBeforeDeactivate = UtilIT.getUserTraces(username, superuserApiToken);
        userTracesBeforeDeactivate.prettyPrint();
        userTracesBeforeDeactivate.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.traces.roleAssignments.items[0].definitionPointName", equalTo(dataverseAlias))
                .body("data.traces.roleAssignments.items[0].definitionPointId", equalTo(dataverseId))
                .body("data.traces.explicitGroups.items[0].name", equalTo("Group for " + dataverseAlias));

        Response deactivateUser = UtilIT.deactivateUser(username);
        deactivateUser.prettyPrint();
        deactivateUser.then().assertThat().statusCode(OK.getStatusCode());

        Response getUser = UtilIT.getAuthenticatedUser(username, superuserApiToken);
        getUser.prettyPrint();
        getUser.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.deactivated", equalTo(true));

        Response findUser = UtilIT.filterAuthenticatedUsers(superuserApiToken, username, null, 100, null);
        findUser.prettyPrint();
        findUser.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.users[0].userIdentifier", equalTo(username))
                .body("data.users[0].deactivated", equalTo(true))
                .body("data.users[0].deactivatedTime", startsWith("2"));

        Response getUserDeactivated = UtilIT.getAuthenticatedUserByToken(apiToken);
        getUserDeactivated.prettyPrint();
        getUserDeactivated.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        Response userTracesAfterDeactivate = UtilIT.getUserTraces(username, superuserApiToken);
        userTracesAfterDeactivate.prettyPrint();
        userTracesAfterDeactivate.then().assertThat()
                .statusCode(OK.getStatusCode())
                /**
                 * Here we are showing the the following were deleted:
                 *
                 * - role assignments
                 *
                 * - membership in explict groups.
                 */
                .body("data.traces", equalTo(Collections.EMPTY_MAP));

        Response grantRoleAfterDeactivate = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.ADMIN.toString(), "@" + username, superuserApiToken);
        grantRoleAfterDeactivate.prettyPrint();
        grantRoleAfterDeactivate.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo("User " + username + " is deactivated and cannot be given a role."));

        Response addToGroupAfter = UtilIT.addToGroup(dataverseAlias, aliasInOwner, roleAssigneesToAdd, superuserApiToken);
        addToGroupAfter.prettyPrint();
        addToGroupAfter.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo("User " + username + " is deactivated and cannot be added to a group."));

        Response grantRoleOnDataset = UtilIT.grantRoleOnDataset(datasetPersistentId, DataverseRole.ADMIN.toString(), "@" + username, superuserApiToken);
        grantRoleOnDataset.prettyPrint();
        grantRoleOnDataset.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo("User " + username + " is deactivated and cannot be given a role."));

    }

    @Test
    public void testDeactivateUserById() {
        
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        
        Long userId = JsonPath.from(createUser.body().asString()).getLong("data.authenticatedUser.id");
        Response deactivateUser = UtilIT.deactivateUser(userId);
        deactivateUser.prettyPrint();
        deactivateUser.then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testMergeDeactivatedIntoNonDeactivatedUser() {

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUserMergeTarget = UtilIT.createRandomUser();
        createUserMergeTarget.prettyPrint();
        String usernameMergeTarget = UtilIT.getUsernameFromResponse(createUserMergeTarget);

        Response createUserToMerge = UtilIT.createRandomUser();
        createUserToMerge.prettyPrint();
        String usernameToMerge = UtilIT.getUsernameFromResponse(createUserToMerge);

        Response deactivateUser = UtilIT.deactivateUser(usernameToMerge);
        deactivateUser.prettyPrint();
        deactivateUser.then().assertThat().statusCode(OK.getStatusCode());

        // User accounts can only be merged if they are either both active or both deactivated.
        Response mergeAccounts = UtilIT.mergeAccounts(usernameMergeTarget, usernameToMerge, superuserApiToken);
        mergeAccounts.prettyPrint();
        mergeAccounts.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testMergeNonDeactivatedIntoDeactivatedUser() {

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUserMergeTarget = UtilIT.createRandomUser();
        createUserMergeTarget.prettyPrint();
        String usernameMergeTarget = UtilIT.getUsernameFromResponse(createUserMergeTarget);

        Response createUserToMerge = UtilIT.createRandomUser();
        createUserToMerge.prettyPrint();
        String usernameToMerge = UtilIT.getUsernameFromResponse(createUserToMerge);

        Response deactivateUser = UtilIT.deactivateUser(usernameMergeTarget);
        deactivateUser.prettyPrint();
        deactivateUser.then().assertThat().statusCode(OK.getStatusCode());

        // User accounts can only be merged if they are either both active or both deactivated.
        Response mergeAccounts = UtilIT.mergeAccounts(usernameMergeTarget, usernameToMerge, superuserApiToken);
        mergeAccounts.prettyPrint();
        mergeAccounts.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testMergeDeactivatedIntoDeactivatedUser() {

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUserMergeTarget = UtilIT.createRandomUser();
        createUserMergeTarget.prettyPrint();
        String usernameMergeTarget = UtilIT.getUsernameFromResponse(createUserMergeTarget);

        Response createUserToMerge = UtilIT.createRandomUser();
        createUserToMerge.prettyPrint();
        String usernameToMerge = UtilIT.getUsernameFromResponse(createUserToMerge);

        Response deactivatedUserMergeTarget = UtilIT.deactivateUser(usernameMergeTarget);
        deactivatedUserMergeTarget.prettyPrint();
        deactivatedUserMergeTarget.then().assertThat().statusCode(OK.getStatusCode());

        Response deactivatedUserToMerge = UtilIT.deactivateUser(usernameToMerge);
        deactivatedUserToMerge.prettyPrint();
        deactivatedUserToMerge.then().assertThat().statusCode(OK.getStatusCode());

        // User accounts can only be merged if they are either both active or both deactivated.
        Response mergeAccounts = UtilIT.mergeAccounts(usernameMergeTarget, usernameToMerge, superuserApiToken);
        mergeAccounts.prettyPrint();
        mergeAccounts.then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testMergeUserIntoSelf() {

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createUserToMerge = UtilIT.createRandomUser();
        createUserToMerge.prettyPrint();
        String usernameToMerge = UtilIT.getUsernameFromResponse(createUserToMerge);

        String usernameMergeTarget = usernameToMerge;

        Response mergeAccounts = UtilIT.mergeAccounts(usernameMergeTarget, usernameToMerge, superuserApiToken);
        mergeAccounts.prettyPrint();
        mergeAccounts.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testTurnDeactivatedUserIntoSuperuser() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);

        Response deactivateUser = UtilIT.deactivateUser(username);
        deactivateUser.prettyPrint();
        deactivateUser.then().assertThat().statusCode(OK.getStatusCode());

        Response toggleSuperuser = UtilIT.makeSuperUser(username);
        toggleSuperuser.prettyPrint();
        toggleSuperuser.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

    }

}
