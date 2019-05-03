package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import java.io.StringReader;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MoveIT {

    private static final Logger logger = Logger.getLogger(MoveIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testMoveDataset() {

        Response createCurator = UtilIT.createRandomUser();
        createCurator.prettyPrint();
        createCurator.then().assertThat()
                .statusCode(OK.getStatusCode());
        String curatorUsername = UtilIT.getUsernameFromResponse(createCurator);
        String curatorApiToken = UtilIT.getApiTokenFromResponse(createCurator);

        Response createCuratorDataverse1 = UtilIT.createRandomDataverse(curatorApiToken);
        createCuratorDataverse1.prettyPrint();
        createCuratorDataverse1.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String curatorDataverseAlias1 = UtilIT.getAliasFromResponse(createCuratorDataverse1);

        Response createAuthor = UtilIT.createRandomUser();
        createAuthor.prettyPrint();
        createAuthor.then().assertThat()
                .statusCode(OK.getStatusCode());
        String authorUsername = UtilIT.getUsernameFromResponse(createAuthor);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createAuthor);

        // Whoops, the curator forgot to give the author permission to create a dataset.
        Response noPermToCreateDataset = UtilIT.createRandomDatasetViaNativeApi(curatorDataverseAlias1, authorApiToken);
        noPermToCreateDataset.prettyPrint();
        noPermToCreateDataset.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode())
                .body("message", equalTo("User @" + authorUsername + " is not permitted to perform requested action."));

        Response grantAuthorAddDataset = UtilIT.grantRoleOnDataverse(curatorDataverseAlias1, DataverseRole.DS_CONTRIBUTOR.toString(), "@" + authorUsername, curatorApiToken);
        grantAuthorAddDataset.prettyPrint();
        grantAuthorAddDataset.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.assignee", equalTo("@" + authorUsername))
                .body("data._roleAlias", equalTo("dsContributor"));

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(curatorDataverseAlias1, authorApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        String nullApiToken = null;
        Response moveDatasetFailGuest = UtilIT.moveDataset(datasetId.toString(), curatorDataverseAlias1, nullApiToken);
        moveDatasetFailGuest.prettyPrint();
        moveDatasetFailGuest.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode())
                .body("message", equalTo("User :guest is not permitted to perform requested action."));

        Response moveDatasetFailAlreadyThere = UtilIT.moveDataset(datasetId.toString(), curatorDataverseAlias1, curatorApiToken);
        moveDatasetFailAlreadyThere.prettyPrint();
        moveDatasetFailAlreadyThere.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo("This dataset is already in this dataverse."));

        Response createAuthorDataverse1 = UtilIT.createRandomDataverse(curatorApiToken);
        createAuthorDataverse1.prettyPrint();
        createAuthorDataverse1.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String authorDataverseAlias1 = UtilIT.getAliasFromResponse(createAuthorDataverse1);

        Response moveDatasetFail = UtilIT.moveDataset(datasetId.toString(), authorDataverseAlias1, authorApiToken);
        moveDatasetFail.prettyPrint();
        moveDatasetFail.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode())
                .body("message", equalTo("User @" + authorUsername + " is not permitted to perform requested action."));

        Response createSuperuser = UtilIT.createRandomUser();
        createSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String superusername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response makeSuperuser = UtilIT.makeSuperUser(superusername);
        makeSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response moveDataset1 = UtilIT.moveDataset(datasetId.toString(), authorDataverseAlias1, superuserApiToken);
        moveDataset1.prettyPrint();
        moveDataset1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Dataset moved successfully"));

        Response moveDataset2 = UtilIT.moveDataset(datasetId.toString(), curatorDataverseAlias1, superuserApiToken);
        moveDataset2.prettyPrint();
        moveDataset2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Dataset moved successfully"));

        Response createCuratorDataverse2 = UtilIT.createRandomDataverse(curatorApiToken);
        createCuratorDataverse2.prettyPrint();
        createCuratorDataverse2.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String curatorDataverseAlias2 = UtilIT.getAliasFromResponse(createCuratorDataverse2);

        Response moveDatasetFailNoPermToPublishDv = UtilIT.moveDataset(datasetId.toString(), curatorDataverseAlias2, authorApiToken);
        moveDatasetFailNoPermToPublishDv.prettyPrint();
        moveDatasetFailNoPermToPublishDv.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode())
                .body("message", equalTo("User @" + authorUsername + " is not permitted to perform requested action."));

        Response moveDataset3 = UtilIT.moveDataset(datasetId.toString(), curatorDataverseAlias2, curatorApiToken);
        moveDataset3.prettyPrint();
        moveDataset3.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Dataset moved successfully"));

    }

    @Test
    public void testMoveDatasetThief() {

        Response createAuthor = UtilIT.createRandomUser();
        createAuthor.prettyPrint();
        createAuthor.then().assertThat()
                .statusCode(OK.getStatusCode());
        String authorUsername = UtilIT.getUsernameFromResponse(createAuthor);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createAuthor);

        Response createThief = UtilIT.createRandomUser();
        createThief.prettyPrint();
        createThief.then().assertThat()
                .statusCode(OK.getStatusCode());
        String thiefUsername = UtilIT.getUsernameFromResponse(createThief);
        String thiefApiToken = UtilIT.getApiTokenFromResponse(createThief);

        Response createAuthorDataverse = UtilIT.createRandomDataverse(authorApiToken);
        createAuthorDataverse.prettyPrint();
        createAuthorDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String authorDataverseAlias = UtilIT.getAliasFromResponse(createAuthorDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(authorDataverseAlias, authorApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        // Can the thief steal the dataset?
        Response createThiefDataverse = UtilIT.createRandomDataverse(thiefApiToken);
        createThiefDataverse.prettyPrint();
        createThiefDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String thiefDataverseAlias = UtilIT.getAliasFromResponse(createThiefDataverse);

        Response thiefAttemptToStealDataset = UtilIT.moveDataset(datasetId.toString(), thiefDataverseAlias, thiefApiToken);
        thiefAttemptToStealDataset.prettyPrint();
        thiefAttemptToStealDataset.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode())
                .body("message", equalTo("User @" + thiefUsername + " is not permitted to perform requested action."));

    }

    @Test
    public void testMoveLinkedDataset() {

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

        UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken).then().assertThat()
                .statusCode(OK.getStatusCode());

        Response moveDatasetFailTargetDataverseNotPublished = UtilIT.moveDataset(datasetId.toString(), dataverse2Alias, apiToken);
        moveDatasetFailTargetDataverseNotPublished.prettyPrint();
        moveDatasetFailTargetDataverseNotPublished.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo("A published dataset may not be moved to an unpublished dataverse. You can retry the move after publishing " + dataverse2Name + "."));

        UtilIT.publishDataverseViaNativeApi(dataverse2Alias, apiToken).then().assertThat()
                .statusCode(OK.getStatusCode());

        // Link dataset to second dataverse.
        Response linkDataset = UtilIT.linkDataset(datasetPid, dataverse2Alias, superuserApiToken);
        linkDataset.prettyPrint();
        linkDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response getLinksBefore = UtilIT.getDatasetLinks(datasetPid, superuserApiToken);
        getLinksBefore.prettyPrint();
        getLinksBefore.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response listDatasetsBeforeSource = UtilIT.listDatasetsViaSword(dataverse1Alias, apiToken);
        listDatasetsBeforeSource.prettyPrint();
        listDatasetsBeforeSource.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("feed.entry[0].id", CoreMatchers.endsWith(datasetPid));

        Response listDatasetsBeforeDestination = UtilIT.listDatasetsViaSword(dataverse2Alias, apiToken);
        listDatasetsBeforeDestination.prettyPrint();
        listDatasetsBeforeDestination.then().assertThat()
                // TODO: Add assertion that no dataset exists.
                .statusCode(OK.getStatusCode());

        Response attemptToMoveLinkedDataset = UtilIT.moveDataset(datasetId.toString(), dataverse2Alias, superuserApiToken);
        attemptToMoveLinkedDataset.prettyPrint();
        attemptToMoveLinkedDataset.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo("Use the query parameter forceMove=true to complete the move. This dataset is linked to the new host dataverse or one of its parents. This move would remove all links to this dataset. "));

        JsonObject linksBeforeData = Json.createReader(new StringReader(getLinksBefore.asString())).readObject();
        Assert.assertEquals("OK", linksBeforeData.getString("status"));
        Assert.assertEquals(dataverse2Alias + " (id " + dataverse2Id + ")", linksBeforeData.getJsonObject("data").getJsonArray("dataverses that link to dataset id " + datasetId).getString(0));

        boolean forceMove = true;
        Response forceMoveLinkedDataset = UtilIT.moveDataset(datasetId.toString(), dataverse2Alias, forceMove, superuserApiToken);
        forceMoveLinkedDataset.prettyPrint();
        forceMoveLinkedDataset.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Dataset moved successfully"));

        Response listDatasetsAfterSource = UtilIT.listDatasetsViaSword(dataverse1Alias, apiToken);
        listDatasetsAfterSource.prettyPrint();
        listDatasetsAfterSource.then().assertThat()
                // TODO: Add assertion that no dataset exists.
                .statusCode(OK.getStatusCode());

        Response listDatasetsAfterDestination = UtilIT.listDatasetsViaSword(dataverse2Alias, apiToken);
        listDatasetsAfterDestination.prettyPrint();
        listDatasetsAfterDestination.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("feed.entry[0].id", CoreMatchers.endsWith(datasetPid));

        Response getLinksAfter = UtilIT.getDatasetLinks(datasetPid, superuserApiToken);
        getLinksAfter.prettyPrint();
        getLinksAfter.then().assertThat()
                .statusCode(OK.getStatusCode());

        JsonObject linksAfterData = Json.createReader(new StringReader(getLinksAfter.asString())).readObject();
        Assert.assertEquals("OK", linksAfterData.getString("status"));
        Assert.assertEquals(0, linksAfterData.getJsonObject("data").getJsonArray("dataverses that link to dataset id " + datasetId).size());

    }

}
