package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class InReviewWorkflowIT {

    private static final Logger logger = Logger.getLogger(DatasetsIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

    }

    @Test
    public void testCuratorSendsCommentsToAuthor() {
        Response createCurator = UtilIT.createRandomUser();
        createCurator.prettyPrint();
        createCurator.then().assertThat()
                .statusCode(OK.getStatusCode());
        String curatorUsername = UtilIT.getUsernameFromResponse(createCurator);
        String curatorApiToken = UtilIT.getApiTokenFromResponse(createCurator);

        Response createDataverseResponse = UtilIT.createRandomDataverse(curatorApiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createAuthor = UtilIT.createRandomUser();
        createAuthor.prettyPrint();
        createAuthor.then().assertThat()
                .statusCode(OK.getStatusCode());
        String authorUsername = UtilIT.getUsernameFromResponse(createAuthor);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createAuthor);

        // Whoops, the curator forgot to give the author permission to create a dataset.
        Response noPermToCreateDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        noPermToCreateDataset.prettyPrint();
        noPermToCreateDataset.then().assertThat()
                .body("message", equalTo("User @" + authorUsername + " is not permitted to perform requested action."))
                .statusCode(UNAUTHORIZED.getStatusCode());

        Response grantAuthorAddDataset = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.DS_CONTRIBUTOR.toString(), "@" + authorUsername, curatorApiToken);
        grantAuthorAddDataset.prettyPrint();
        grantAuthorAddDataset.then().assertThat()
                .body("data.assignee", equalTo("@" + authorUsername))
                .body("data._roleAlias", equalTo("dsContributor"))
                .statusCode(OK.getStatusCode());

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        // FIXME: have the initial create return the DOI or Handle to obviate the need for this call.
        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(datasetId, authorApiToken);
        getDatasetJsonBeforePublishing.prettyPrint();
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.identifier");

        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;
        System.out.println("datasetPersistentId: " + datasetPersistentId);

        // Whoops, the author tries to publish but isn't allowed. The curator will take a look.
        Response noPermToPublish = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", authorApiToken);
        noPermToPublish.prettyPrint();
        noPermToPublish.then().assertThat()
                .body("message", equalTo("User @" + authorUsername + " is not permitted to perform requested action."))
                .statusCode(UNAUTHORIZED.getStatusCode());

        Response createJoeRandom = UtilIT.createRandomUser();
        createJoeRandom.prettyPrint();
        createJoeRandom.then().assertThat()
                .statusCode(OK.getStatusCode());
        String joeRandomUsername = UtilIT.getUsernameFromResponse(createJoeRandom);
        String joeRandomApiToken = UtilIT.getApiTokenFromResponse(createJoeRandom);

        Response submitForReviewFail = UtilIT.submitDatasetForReview(datasetPersistentId, joeRandomApiToken);
        submitForReviewFail.prettyPrint();
        submitForReviewFail.then().assertThat()
                .body("message", equalTo("User @" + joeRandomUsername + " is not permitted to perform requested action."))
                .statusCode(UNAUTHORIZED.getStatusCode());

        Response submitForReview = UtilIT.submitDatasetForReview(datasetPersistentId, authorApiToken);
        submitForReview.prettyPrint();
        submitForReview.then().assertThat()
                .body("data.inReview", equalTo(true))
                .statusCode(OK.getStatusCode());

        Response authorsChecksForCommentsPrematurely = UtilIT.getNotifications(authorApiToken);
        authorsChecksForCommentsPrematurely.prettyPrint();
        authorsChecksForCommentsPrematurely.then().assertThat()
                .body("data.notifications[0].type", equalTo("CREATEACC"))
                // The author thinks, "What's taking the curator so long to review my data?!?"
                .body("data.notifications[1]", equalTo(null))
                .statusCode(OK.getStatusCode());

        String joeRandomComments = "Joe Random says you'll never graduate.";
        JsonObjectBuilder joeRandObj = Json.createObjectBuilder();
        joeRandObj.add("comments", joeRandomComments);

        // Joe Random - a real jerk - tries returning a dataset unrightfully with some mean comments.
        // Despite being somewhere he already shouldn't be, he is not authorized to return it, thankfully.
        Response returnToAuthorFail = UtilIT.returnDatasetToAuthor(datasetPersistentId, joeRandObj.build(), joeRandomApiToken);
        returnToAuthorFail.prettyPrint();
        returnToAuthorFail.then().assertThat()
                .body("message", equalTo("User @" + joeRandomUsername + " is not permitted to perform requested action."))
                .statusCode(UNAUTHORIZED.getStatusCode());

        // TODO: test where curator neglecting to leave a comment. Should fail with "reason for return" required.
        String noComments = "";
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("comments", noComments);
        Response returnToAuthorNoComment = UtilIT.returnDatasetToAuthor(datasetPersistentId, jsonObjectBuilder.build(), curatorApiToken);
        returnToAuthorNoComment.prettyPrint();
        returnToAuthorNoComment.then().assertThat()
                .body("comments", equalTo(null))
                .statusCode(BAD_REQUEST.getStatusCode());

        // TODO: For now, show that a "reason for return" of over 200 characters is saved. Once the limit is in place, change the test to fail.
        String longComment = "Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque sed foo.";
        jsonObjectBuilder.add("comments", longComment);
        Response returnToAuthorWithLongComment = UtilIT.returnDatasetToAuthor(datasetPersistentId, jsonObjectBuilder.build(), curatorApiToken);
        returnToAuthorWithLongComment.prettyPrint();
        returnToAuthorWithLongComment.then().assertThat()
                // With no char limit in place, the long comment can be used when returning a dataset. Change this assertion once limit exists.
                .body("data.inReview", equalTo(false))
                .statusCode(OK.getStatusCode());

        // The author submits once again (no real changes made)
        Response submitForReviewAgain = UtilIT.submitDatasetForReview(datasetPersistentId, authorApiToken);
        submitForReviewAgain.prettyPrint();
        submitForReviewAgain.then().assertThat()
                .body("data.inReview", equalTo(true))
                .statusCode(OK.getStatusCode());
        
        // Successfully return dataset to author for reason: "You forgot to upload any files."
        String comments = "You forgot to upload any files.";
        jsonObjectBuilder.add("comments", comments);
        Response returnToAuthor = UtilIT.returnDatasetToAuthor(datasetPersistentId, jsonObjectBuilder.build(), curatorApiToken);
        returnToAuthor.prettyPrint();
        returnToAuthor.then().assertThat()
                .body("data.inReview", equalTo(false))
                .statusCode(OK.getStatusCode());

        Response authorChecksForCommentsAgain = UtilIT.getNotifications(authorApiToken);
        authorChecksForCommentsAgain.prettyPrint();
        authorChecksForCommentsAgain.then().assertThat()
                .body("data.notifications[0].type", equalTo("RETURNEDDS"))
                // The author thinks, "This why we have curators!"
                // FIXME: change to expect curator to also see the reason for return
                .body("data.notifications[0].comments", equalTo("You forgot to upload any files."))
                .body("data.notifications[1].type", equalTo("RETURNEDDS"))
                .body("data.notifications[1].comments", equalTo("You forgot to upload any files."))
                .statusCode(OK.getStatusCode());

        // The author upload the file she forgot.
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response authorAddsFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, authorApiToken);
        authorAddsFile.prettyPrint();
        authorAddsFile.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data.files[0].label", equalTo("dataverseproject.png"))
                .statusCode(OK.getStatusCode());

        // The author re-submits.
        Response resubmitForReview = UtilIT.submitDatasetForReview(datasetPersistentId, authorApiToken);
        resubmitForReview.prettyPrint();
        resubmitForReview.then().assertThat()
                .body("data.inReview", equalTo(true))
                .statusCode(OK.getStatusCode());

        // The curator checks to see if the author has resubmitted yet.
        Response curatorChecksNotifications = UtilIT.getNotifications(curatorApiToken);
        curatorChecksNotifications.prettyPrint();
        curatorChecksNotifications.then().assertThat()
                // TODO: Test this issue from the UI as well: https://github.com/IQSS/dataverse/issues/2526
                .body("data.notifications[0].type", equalTo("SUBMITTEDDS"))
                .body("data.notifications[0].comments", equalTo("You forgot to upload any files."))
                .body("data.notifications[1].type", equalTo("SUBMITTEDDS"))
                .body("data.notifications[1].comments", equalTo("You forgot to upload any files."))
                .body("data.notifications[2].type", equalTo("SUBMITTEDDS"))
                .body("data.notifications[2].comments", equalTo("You forgot to upload any files."))
                .body("data.notifications[3].type", equalTo("CREATEACC"))
                .body("data.notifications[3].comments", equalTo(null))
                .statusCode(OK.getStatusCode());

        // The curator publishes the dataverse.
        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, curatorApiToken);
        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // The curator publishes the dataset.
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", curatorApiToken);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response authorsChecksForCommentsPostPublication = UtilIT.getNotifications(authorApiToken);
        authorsChecksForCommentsPostPublication.prettyPrint();
        authorsChecksForCommentsPostPublication.then().assertThat()
                .body("data.notifications[0].type", equalTo("ASSIGNROLE"))
                .body("data.notifications[1].type", equalTo("RETURNEDDS"))
                // The reason for return is deleted on publish. It's water under the bridge.
                .body("data.notifications[1].comments", equalTo(null))
                .body("data.notifications[2].type", equalTo("RETURNEDDS"))
                .body("data.notifications[2].comments", equalTo(null))
                .body("data.notifications[3].type", equalTo("CREATEACC"))
                .body("data.notifications[3].comments", equalTo(null))
                .statusCode(OK.getStatusCode());

        // These println's are here in case you want to log into the GUI to see what notifications look like.
        System.out.println("Curator username/password: " + curatorUsername);
        System.out.println("Author username/password: " + authorUsername);

    }

}
