package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import java.util.logging.Logger;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
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

        Response moveDatasetFailAlreadyThere = UtilIT.moveDataset(datasetId.toString(), curatorDataverseAlias1, curatorApiToken);
        moveDatasetFailAlreadyThere.prettyPrint();
        moveDatasetFailAlreadyThere.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", equalTo("Dataset already in this Dataverse "));

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

}
