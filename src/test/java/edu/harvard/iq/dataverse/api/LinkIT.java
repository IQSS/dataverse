package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.logging.Logger;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class LinkIT {

    private static final Logger logger = Logger.getLogger(LinkIT.class.getCanonicalName());

    @BeforeClass
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

        Response searchLevel2toLevel2 = UtilIT.search("*", apiToken, "&subtree=" + level2b);
        searchLevel2toLevel2.prettyPrint();
        searchLevel2toLevel2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", equalTo(1))
                .body("data.items[0].name", equalTo(level2a));

    }

}
