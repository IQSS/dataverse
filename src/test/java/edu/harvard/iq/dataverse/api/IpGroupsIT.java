package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class IpGroupsIT {

    private static final Logger logger = Logger.getLogger(IpGroupsIT.class.getCanonicalName());

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testDownloadFile() {

        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        System.out.println("dataset id: " + datasetId);

        Response userWithNoRoles = UtilIT.createRandomUser();
        String userWithNoRolesApiToken = UtilIT.getApiTokenFromResponse(userWithNoRoles);
        String pathToFile = "src/main/webapp/resources/images/favicondataverse.png";
        Response addResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);

        addResponse.then().assertThat()
                .body("data.files[0].dataFile.contentType", equalTo("image/png"))
                .body("data.files[0].label", equalTo("favicondataverse.png"))
                .statusCode(OK.getStatusCode());

        Long fileId = JsonPath.from(addResponse.body().asString()).getLong("data.files[0].dataFile.id");

        boolean restrict = true;
        Response restrictResponse = UtilIT.restrictFile(fileId.toString(), restrict, apiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat()
                .body("data.message", equalTo("File favicondataverse.png restricted."))
                .statusCode(OK.getStatusCode());

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        // BEGIN: group sanity check
        // All this user2 stuff is a sanity check that groups for at all. We test an "explicit" group.
        Response createUser2 = UtilIT.createRandomUser();
        createUser2.prettyPrint();
        String username2 = UtilIT.getUsernameFromResponse(createUser2);
        String apiToken2 = UtilIT.getApiTokenFromResponse(createUser2);

        Response downloadFileUser2Fail = UtilIT.downloadFile(fileId.intValue(), apiToken2);
        assertEquals(FORBIDDEN.getStatusCode(), downloadFileUser2Fail.getStatusCode());

        String aliasInOwner = "groupFor" + dataverseAlias;
        String displayName = "Group for " + dataverseAlias;
        String user2identifier = "@" + username2;
        Response createGroup = UtilIT.createGroup(dataverseAlias, aliasInOwner, displayName, apiToken);
        createGroup.prettyPrint();
        createGroup.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String groupIdentifier = JsonPath.from(createGroup.asString()).getString("data.identifier");

        List<String> roleAssigneesToAdd = new ArrayList<>();
        roleAssigneesToAdd.add(user2identifier);
        Response addToGroup = UtilIT.addToGroup(dataverseAlias, aliasInOwner, roleAssigneesToAdd, apiToken);
        addToGroup.prettyPrint();
        addToGroup.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response grantRoleResponse = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.FILE_DOWNLOADER.toString(), groupIdentifier, apiToken);
        grantRoleResponse.prettyPrint();
        grantRoleResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response downloadFileUser2Works = UtilIT.downloadFile(fileId.intValue(), apiToken2);
        assertEquals(OK.getStatusCode(), downloadFileUser2Works.getStatusCode());
        // END: group sanity check

        System.out.println("file id: " + fileId);
        Response downloadFile = UtilIT.downloadFile(fileId.intValue(), apiToken);
        assertEquals(OK.getStatusCode(), downloadFile.getStatusCode());

        Response downloadFileNoPrivs = UtilIT.downloadFile(fileId.intValue(), userWithNoRolesApiToken);
        assertEquals(FORBIDDEN.getStatusCode(), downloadFileNoPrivs.getStatusCode());

        JsonObjectBuilder ipGroupAllJson = Json.createObjectBuilder();
        String uniqueIdentifierForIpGroup = "ipGroup" + UtilIT.getRandomIdentifier();
        ipGroupAllJson.add("alias", uniqueIdentifierForIpGroup);
        ipGroupAllJson.add("name", "An IP Group that matches all IP addresses and has a unique identifier.");
        ipGroupAllJson.add("ranges", Json.createArrayBuilder()
                .add(Json.createArrayBuilder()
                        .add("0.0.0.0")
                        .add("255.255.255.255")
                ));
        Response createIpGroupAll = UtilIT.createIpGroup(ipGroupAllJson.build());
        createIpGroupAll.prettyPrint();
        createIpGroupAll.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String ipGroupIdentifierString = "&ip/" + uniqueIdentifierForIpGroup;
        Response grantIpAll = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.FILE_DOWNLOADER.toString(), ipGroupIdentifierString, apiToken);
        grantIpAll.prettyPrint();
        grantIpAll.then().assertThat()
                .body("data.assignee", equalTo(ipGroupIdentifierString))
                .body("data._roleAlias", equalTo("fileDownloader"))
                .statusCode(OK.getStatusCode());

        Response downloadFileBasedOnIPGroup = UtilIT.downloadFile(fileId.intValue(), userWithNoRolesApiToken);
        // Should get an OK response (able to download file) based on IP Group membership. Has API token but no individual role.
        assertEquals(OK.getStatusCode(), downloadFileBasedOnIPGroup.getStatusCode());

        Response anonDownload = UtilIT.downloadFile(fileId.intValue());
        // Should get an OK response (able to download file) based on IP Group membership. No API token.
        assertEquals(OK.getStatusCode(), anonDownload.getStatusCode());

    }

}
