package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LinkIT {

    private static final Logger logger = Logger.getLogger(LinkIT.class.getCanonicalName());

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testLinkedDataset() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createSuperUser = UtilIT.createRandomUser();
        createSuperUser.prettyPrint();
        createSuperUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperUser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperUser);
        Response makeSuperuser = UtilIT.makeSuperUser(superuserUsername);
        makeSuperuser.prettyPrint();
        makeSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createDataverse1 = UtilIT.createRandomDataverse(apiToken);
        createDataverse1.prettyPrint();
        createDataverse1.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverse1Alias = UtilIT.getAliasFromResponse(createDataverse1);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverse1Alias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        Response createDataverse2 = UtilIT.createRandomDataverse(apiToken);
        createDataverse2.prettyPrint();
        createDataverse2.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverse2Alias = UtilIT.getAliasFromResponse(createDataverse2);
        Integer dataverse2Id = UtilIT.getDatasetIdFromResponse(createDataverse2);
        String dataverse2Name = JsonPath.from(createDataverse2.asString()).getString("data.name");

        UtilIT.publishDataverseViaNativeApi(dataverse1Alias, apiToken).then().assertThat()
                .statusCode(OK.getStatusCode());

        // You can't link an unpublished dataset.
        Response tryToLinkUnpublishedDataset = UtilIT.linkDataset(datasetPid, dataverse2Alias, superuserApiToken);
        tryToLinkUnpublishedDataset.prettyPrint();
        tryToLinkUnpublishedDataset.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo("Can't link a dataset that has not been published or is not harvested"));

        UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken).then().assertThat()
                .statusCode(OK.getStatusCode());

        UtilIT.publishDataverseViaNativeApi(dataverse2Alias, apiToken).then().assertThat()
                .statusCode(OK.getStatusCode());

        // A dataset cannot be linked to its parent dataverse.
        Response tryToLinkToParentDataverse = UtilIT.linkDataset(datasetPid, dataverse1Alias, superuserApiToken);
        tryToLinkToParentDataverse.prettyPrint();
        tryToLinkToParentDataverse.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo("Can't link a dataset to its dataverse"));

        // Link dataset to non-parent dataverse (allowed).
        Response linkDataset = UtilIT.linkDataset(datasetPid, dataverse2Alias, superuserApiToken);
        linkDataset.prettyPrint();
        linkDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        // A dataset cannot be linked to the same dataverse again.
        Response tryToLinkAgain = UtilIT.linkDataset(datasetPid, dataverse2Alias, superuserApiToken);
        tryToLinkAgain.prettyPrint();
        tryToLinkAgain.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo("Can't link a dataset that has already been linked to this dataverse"));
    }

    @Test
    public void testCreateDeleteDataverseLink() {
        Response createUser = UtilIT.createRandomUser();

        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response superuserResponse = UtilIT.makeSuperUser(username);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverseResponse);

        Response createDataverseResponse2 = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse2.prettyPrint();
        String dataverseAlias2 = UtilIT.getAliasFromResponse(createDataverseResponse2);

        Response createLinkingDataverseResponse = UtilIT.createDataverseLink(dataverseAlias, dataverseAlias2, apiToken);
        createLinkingDataverseResponse.prettyPrint();
        createLinkingDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Dataverse " + dataverseAlias + " linked successfully to " + dataverseAlias2));

        Response tryLinkingAgain = UtilIT.createDataverseLink(dataverseAlias, dataverseAlias2, apiToken);
        tryLinkingAgain.prettyPrint();
        tryLinkingAgain.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo(dataverseAlias + " has already been linked to " + dataverseAlias2 + "."));

        Response deleteLinkingDataverseResponse = UtilIT.deleteDataverseLink(dataverseAlias, dataverseAlias2, apiToken);
        deleteLinkingDataverseResponse.prettyPrint();
        deleteLinkingDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Link from Dataverse " + dataverseAlias + " to linked Dataverse " + dataverseAlias2 + " deleted"));
    }

    @Test
    public void testDeepLinks() {
        Response createUser = UtilIT.createRandomUser();

        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response superuserResponse = UtilIT.makeSuperUser(username);

        Response createLevel1a = UtilIT.createSubDataverse(UtilIT.getRandomDvAlias() + "-level1a", null, apiToken, ":root");
        createLevel1a.prettyPrint();
        String level1a = UtilIT.getAliasFromResponse(createLevel1a);

        Response createLevel1b = UtilIT.createSubDataverse(UtilIT.getRandomDvAlias() + "-level1b", null, apiToken, ":root");
        createLevel1b.prettyPrint();
        String level1b = UtilIT.getAliasFromResponse(createLevel1b);

        Response linkLevel1toLevel1 = UtilIT.createDataverseLink(level1a, level1b, apiToken);
        linkLevel1toLevel1.prettyPrint();
        linkLevel1toLevel1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Dataverse " + level1a + " linked successfully to " + level1b));

        assertTrue(UtilIT.sleepForSearch("*", apiToken, "&subtree="+level1b, 1, UtilIT.GENERAL_LONG_DURATION), "Zero counts in level1b");
        
        Response searchLevel1toLevel1 = UtilIT.search("*", apiToken, "&subtree=" + level1b);
        searchLevel1toLevel1.prettyPrint();
        searchLevel1toLevel1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", equalTo(1))
                .body("data.items[0].name", equalTo(level1a));

        Response createLevel2a = UtilIT.createSubDataverse(UtilIT.getRandomDvAlias() + "-level2a", null, apiToken, level1a);
        createLevel2a.prettyPrint();
        String level2a = UtilIT.getAliasFromResponse(createLevel2a);

        Response createLevel2b = UtilIT.createSubDataverse(UtilIT.getRandomDvAlias() + "-level2b", null, apiToken, level1b);
        createLevel2b.prettyPrint();
        String level2b = UtilIT.getAliasFromResponse(createLevel2b);

        Response linkLevel2toLevel2 = UtilIT.createDataverseLink(level2a, level2b, apiToken);
        linkLevel2toLevel2.prettyPrint();
        linkLevel2toLevel2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Dataverse " + level2a + " linked successfully to " + level2b));

        assertTrue(UtilIT.sleepForSearch("*", apiToken, "&subtree=" + level2b, 1, UtilIT.GENERAL_LONG_DURATION), "Never found linked dataverse: " + level2b);
        
        Response searchLevel2toLevel2 = UtilIT.search("*", apiToken, "&subtree=" + level2b);
        searchLevel2toLevel2.prettyPrint();
        searchLevel2toLevel2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", equalTo(1))
                .body("data.items[0].name", equalTo(level2a));

    }

    @Test
    public void testListLinks() {

        Response createUser1 = UtilIT.createRandomUser();
        createUser1.prettyPrint();
        createUser1.then().assertThat()
                .statusCode(OK.getStatusCode());
        String apiToken1 = UtilIT.getApiTokenFromResponse(createUser1);

        Response createUser2 = UtilIT.createRandomUser();
        createUser2.prettyPrint();
        createUser2.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username2 = UtilIT.getUsernameFromResponse(createUser2);
        String apiToken2 = UtilIT.getApiTokenFromResponse(createUser2);

        // Create and publish dataverse1 which both user1 and user2 have admin access to
        Response createDataverse1 = UtilIT.createRandomDataverse(apiToken1);
        createDataverse1.prettyPrint();
        createDataverse1.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverse1Alias = UtilIT.getAliasFromResponse(createDataverse1);

        UtilIT.publishDataverseViaNativeApi(dataverse1Alias, apiToken1).then().assertThat()
                .statusCode(OK.getStatusCode());

        Response grantUser2AccessOnDataverse = UtilIT.grantRoleOnDataverse(dataverse1Alias, "admin", "@" + username2, apiToken1);
        grantUser2AccessOnDataverse.prettyPrint();
        grantUser2AccessOnDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Create and publish dataset in dataverse1
        // Which means that both user1 and user2 have permission to view it
        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverse1Alias, apiToken1);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken1);
        publishDatasetResponse.prettyPrint();
        publishDatasetResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Create another unpublished dataverse2 as user2, and don't grant user1 any permissions on it
        // Which means that user1 doesn't have permission to view this dataverse before it is published
        Response createDataverse2 = UtilIT.createRandomDataverse(apiToken2);
        createDataverse2.prettyPrint();
        createDataverse2.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverse2Alias = UtilIT.getAliasFromResponse(createDataverse2);
        Integer dataverse2Id = UtilIT.getDatasetIdFromResponse(createDataverse2);

        // User1 doesn't have permission to link the dataset to the unpublished dataverse2
        Response tryToLinkToUnpublishedDataverseResponse = UtilIT.linkDataset(datasetPid, dataverse2Alias, apiToken1);
        tryToLinkToUnpublishedDataverseResponse.prettyPrint();
        tryToLinkToUnpublishedDataverseResponse.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());

        // But user2 does have permission to link the dataset to his own unpublished dataverse2
        Response linkDatasetToUnpublishedDataverseResponse = UtilIT.linkDataset(datasetPid, dataverse2Alias, apiToken2);
        linkDatasetToUnpublishedDataverseResponse.prettyPrint();
        linkDatasetToUnpublishedDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // User1 has permission to list the links of the dataset, but cannot see the link to the unpublished dataverse2
        Response linkDatasetsResponse = UtilIT.getDatasetLinks(datasetPid, apiToken1);
        linkDatasetsResponse.prettyPrint();
        linkDatasetsResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        JsonObject linkDatasets = Json.createReader(new StringReader(linkDatasetsResponse.asString())).readObject();
        JsonArray linksList = linkDatasets.getJsonObject("data").getJsonArray("linked-dataverses");
        assertEquals(0, linksList.size());

        // User2 has permission to list the links of the dataset and can see the link to the unpublished dataverse2
        Response linkDatasetsResponse2 = UtilIT.getDatasetLinks(datasetPid, apiToken2);
        linkDatasetsResponse2.prettyPrint();
        linkDatasetsResponse2.then().assertThat()
                .statusCode(OK.getStatusCode());
        JsonObject linkDatasets2 = Json.createReader(new StringReader(linkDatasetsResponse2.asString())).readObject();
        JsonArray linksList2 = linkDatasets2.getJsonObject("data").getJsonArray("linked-dataverses");
        assertEquals(1, linksList2.size());
        assertEquals(dataverse2Id, linksList2.getJsonObject(0).getInt("id"));

        UtilIT.publishDataverseViaNativeApi(dataverse2Alias, apiToken2).then().assertThat()
                .statusCode(OK.getStatusCode());

        // After publishing dataverse2, user1 can now also see the link
        Response linkDatasetsResponse3 = UtilIT.getDatasetLinks(datasetPid, apiToken1);
        linkDatasetsResponse3.prettyPrint();
        linkDatasetsResponse3.then().assertThat()
                .statusCode(OK.getStatusCode());
        JsonObject linkDatasets3 = Json.createReader(new StringReader(linkDatasetsResponse3.asString())).readObject();
        JsonArray linksList3 = linkDatasets3.getJsonObject("data").getJsonArray("linked-dataverses");
        assertEquals(1, linksList3.size());
        assertEquals(dataverse2Id, linksList3.getJsonObject(0).getInt("id"));

        // Create another dataset, but don't publish it
        Response createDataset2 = UtilIT.createRandomDatasetViaNativeApi(dataverse1Alias, apiToken1);
        createDataset2.prettyPrint();
        createDataset2.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataset2Pid = JsonPath.from(createDataset2.asString()).getString("data.persistentId");

        // Create user3 without permissions on the unpublished dataset
        Response createUser3 = UtilIT.createRandomUser();
        createUser3.prettyPrint();
        createUser3.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username3 = UtilIT.getUsernameFromResponse(createUser3);
        String apiToken3 = UtilIT.getApiTokenFromResponse(createUser3);

        // User3 cannot list the links of the unpublished dataset
        Response linkDatasetsResponse4 = UtilIT.getDatasetLinks(dataset2Pid, apiToken3);
        linkDatasetsResponse4.prettyPrint();
        linkDatasetsResponse4.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());

        // Grant user3 "member" role on dataverse1, which allows viewing unpublished datasets
        Response grantUser3AccessOnDataverse = UtilIT.grantRoleOnDataverse(dataverse1Alias, "member", "@" + username3, apiToken1);
        grantUser3AccessOnDataverse.prettyPrint();
        grantUser3AccessOnDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // User 3 can now also list the links
        Response linkDatasetsResponse5 = UtilIT.getDatasetLinks(dataset2Pid, apiToken3);
        linkDatasetsResponse5.prettyPrint();
        linkDatasetsResponse5.then().assertThat()
                .statusCode(OK.getStatusCode());
        JsonObject linkDatasets5 = Json.createReader(new StringReader(linkDatasetsResponse5.asString())).readObject();
        JsonArray linksList5 = linkDatasets5.getJsonObject("data").getJsonArray("linked-dataverses");
        assertEquals(0, linksList5.size());

        // Non-authenticated user cannot list the links of the unpublished dataset
        Response linkDatasetsResponse6 = UtilIT.getDatasetLinks(dataset2Pid, null);
        linkDatasetsResponse6.prettyPrint();
        linkDatasetsResponse6.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());

        // Publish dataset2
        Response publishDataset2Response = UtilIT.publishDatasetViaNativeApi(dataset2Pid, "major", apiToken1);
        publishDataset2Response.prettyPrint();
        publishDataset2Response.then().assertThat()
                .statusCode(OK.getStatusCode());

        // After publishing the dataset, non-authenticated user can now also list the links
        Response linkDatasetsResponse7 = UtilIT.getDatasetLinks(dataset2Pid, null);
        linkDatasetsResponse7.prettyPrint();
        linkDatasetsResponse7.then().assertThat()
                .statusCode(OK.getStatusCode());
        JsonObject linkDatasets7 = Json.createReader(new StringReader(linkDatasetsResponse7.asString())).readObject();
        JsonArray linksList7 = linkDatasets7.getJsonObject("data").getJsonArray("linked-dataverses");
        assertEquals(0, linksList7.size());

    }

}
