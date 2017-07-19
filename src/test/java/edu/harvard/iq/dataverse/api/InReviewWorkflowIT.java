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
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
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

//        boolean returnEarlyToTest = true;
//        if (returnEarlyToTest) {
//            System.out.println("Curator username/password and API token: " + curatorUsername + " and " + curatorApiToken);
//            System.out.println("Author username/password and API token: " + authorUsername + " and " + authorApiToken);
//            return;
//        }
        // The author submits the dataset for review.
        Response submitForReview = UtilIT.submitDatasetForReview(datasetPersistentId, authorApiToken);
        submitForReview.prettyPrint();
        submitForReview.then().assertThat()
                .body("data.inReview", equalTo(true))
                .statusCode(OK.getStatusCode());

        Response submitForReviewAlreadySubmitted = UtilIT.submitDatasetForReview(datasetPersistentId, authorApiToken);
        submitForReviewAlreadySubmitted.prettyPrint();
        submitForReviewAlreadySubmitted.then().assertThat()
                .body("message", equalTo("You cannot submit this dataset for review because it is already in review."))
                .statusCode(FORBIDDEN.getStatusCode());

        Response authorsChecksForCommentsPrematurely = UtilIT.getNotifications(authorApiToken);
        authorsChecksForCommentsPrematurely.prettyPrint();
        authorsChecksForCommentsPrematurely.then().assertThat()
                .body("data.notifications[0].type", equalTo("CREATEACC"))
                // The author thinks, "What's taking the curator so long to review my data?!?"
                .body("data.notifications[1]", equalTo(null))
                .statusCode(OK.getStatusCode());

        String joeRandomComments = "Joe Random says you'll never graduate.";
        JsonObjectBuilder joeRandObj = Json.createObjectBuilder();
        joeRandObj.add("reasonForReturn", joeRandomComments);

        Response curatorChecksNotificationsAndFindsWorkToDo = UtilIT.getNotifications(curatorApiToken);
        curatorChecksNotificationsAndFindsWorkToDo.prettyPrint();
        curatorChecksNotificationsAndFindsWorkToDo.then().assertThat()
                .body("data.notifications[0].type", equalTo("SUBMITTEDDS"))
                .body("data.notifications[0].reasonForReturn", equalTo(null))
                .body("data.notifications[1].type", equalTo("CREATEACC"))
                .body("data.notifications[1].reasonForReturn", equalTo(null))
                .statusCode(OK.getStatusCode());

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
        jsonObjectBuilder.add("reasonForReturn", noComments);
        Response returnToAuthorNoComment = UtilIT.returnDatasetToAuthor(datasetPersistentId, jsonObjectBuilder.build(), curatorApiToken);
        returnToAuthorNoComment.prettyPrint();
        returnToAuthorNoComment.then().assertThat()
                .body("comments", equalTo(null))
                .statusCode(BAD_REQUEST.getStatusCode());

        // Successfully return dataset to author for reason: "You forgot to upload any files."
        String comments = "You forgot to upload any files.";
        jsonObjectBuilder.add("reasonForReturn", comments);
        Response returnToAuthor = UtilIT.returnDatasetToAuthor(datasetPersistentId, jsonObjectBuilder.build(), curatorApiToken);
        returnToAuthor.prettyPrint();
        returnToAuthor.then().assertThat()
                .body("data.inReview", equalTo(false))
                .statusCode(OK.getStatusCode());

        jsonObjectBuilder.add("reasonForReturn", comments);
        Response returnToAuthorAlreadyReturned = UtilIT.returnDatasetToAuthor(datasetPersistentId, jsonObjectBuilder.build(), curatorApiToken);
        returnToAuthorAlreadyReturned.prettyPrint();
        returnToAuthorAlreadyReturned.then().assertThat()
                .body("message", equalTo("This dataset cannot be return to the author(s) because the latest version is not In Review. The author(s) needs to click Submit for Review first."))
                .statusCode(FORBIDDEN.getStatusCode());
        //FIXME when/if reasons for return are returned to notifications page and the API is 
        // updated appropriately, these tests will have to be updated.
        Response authorChecksForCommentsAgain = UtilIT.getNotifications(authorApiToken);
        authorChecksForCommentsAgain.prettyPrint();
        authorChecksForCommentsAgain.then().assertThat()
                .body("data.notifications[0].type", equalTo("RETURNEDDS"))
                // The author thinks, "This why we have curators!"
                //.body("data.notifications[0].reasonsForReturn[0].message", equalTo("You forgot to upload any files."))
                .body("data.notifications[1].type", equalTo("CREATEACC"))
                //.body("data.notifications[1].reasonsForReturn", equalTo(null))
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
                //.body("data.notifications[0].reasonsForReturn[0].message", equalTo("You forgot to upload any files."))
                .body("data.notifications[1].type", equalTo("SUBMITTEDDS"))
                // Yes, it's a little weird that the first "SUBMITTEDDS" notification now shows the return reason when it showed nothing before. For now we are simply always showing all the reasons for return. They start to stack up. That way you can see the history.
                //.body("data.notifications[1].reasonsForReturn[0].message", equalTo("You forgot to upload any files."))
                .body("data.notifications[2].type", equalTo("CREATEACC"))
                //.body("data.notifications[2].reasonsForReturn", equalTo(null))
                .statusCode(OK.getStatusCode());

        String reasonForReturn2 = "A README is required.";
        jsonObjectBuilder.add("reasonForReturn", reasonForReturn2);
        Response returntoAuthor2 = UtilIT.returnDatasetToAuthor(datasetPersistentId, jsonObjectBuilder.build(), curatorApiToken);
        returntoAuthor2.prettyPrint();
        returntoAuthor2.then().assertThat()
                .body("data.inReview", equalTo(false))
                .statusCode(OK.getStatusCode());

        Response authorChecksForComments3 = UtilIT.getNotifications(authorApiToken);
        authorChecksForComments3.prettyPrint();
        authorChecksForComments3.then().assertThat()
                .body("data.notifications[0].type", equalTo("RETURNEDDS"))
                // .body("data.notifications[0].reasonsForReturn[0].message", equalTo("You forgot to upload any files."))
                //.body("data.notifications[0].reasonsForReturn[1].message", equalTo("A README is required."))
                .body("data.notifications[1].type", equalTo("RETURNEDDS"))
                // Yes, it's a little weird that the reason for return on the first "RETURNEDDS" changed. We're showing the history.
                // .body("data.notifications[1].reasonsForReturn[0].message", equalTo("You forgot to upload any files."))
                // .body("data.notifications[1].reasonsForReturn[1].message", equalTo("A README is required."))
                .body("data.notifications[2].type", equalTo("CREATEACC"))
                // .body("data.notifications[2].reasonsForReturn", equalTo(null))
                .statusCode(OK.getStatusCode());

        String pathToReadme = "README.md";
        Response authorUploadsReadme = UtilIT.uploadFileViaNative(datasetId.toString(), pathToReadme, authorApiToken);
        authorUploadsReadme.prettyPrint();
        authorUploadsReadme.then().assertThat()
                .body("status", equalTo("OK"))
                .body("data.files[0].label", equalTo("README.md"))
                .statusCode(OK.getStatusCode());

        // The author re-submits.
        Response submit3 = UtilIT.submitDatasetForReview(datasetPersistentId, authorApiToken);
        submit3.prettyPrint();
        submit3.then().assertThat()
                .body("data.inReview", equalTo(true))
                .statusCode(OK.getStatusCode());

        // The curator checks to see if the author has resubmitted yet.
        Response curatorHopesTheReadmeIsThereNow = UtilIT.getNotifications(curatorApiToken);
        curatorHopesTheReadmeIsThereNow.prettyPrint();
        curatorHopesTheReadmeIsThereNow.then().assertThat()
                // TODO: Test this issue from the UI as well: https://github.com/IQSS/dataverse/issues/2526
                .body("data.notifications[0].type", equalTo("SUBMITTEDDS"))
                // .body("data.notifications[0].reasonsForReturn[0].message", equalTo("You forgot to upload any files."))
                // .body("data.notifications[0].reasonsForReturn[1].message", equalTo("A README is required."))
                .body("data.notifications[1].type", equalTo("SUBMITTEDDS"))
                //  .body("data.notifications[1].reasonsForReturn[0].message", equalTo("You forgot to upload any files."))
                //   .body("data.notifications[1].reasonsForReturn[1].message", equalTo("A README is required."))
                .body("data.notifications[2].type", equalTo("SUBMITTEDDS"))
                // Yes, it's a little weird that the first "SUBMITTEDDS" notification now shows the return reason when it showed nothing before. We're showing the history.
                //   .body("data.notifications[2].reasonsForReturn[0].message", equalTo("You forgot to upload any files."))
                //   .body("data.notifications[2].reasonsForReturn[1].message", equalTo("A README is required."))
                .body("data.notifications[3].type", equalTo("CREATEACC"))
                //   .body("data.notifications[3].reasonsForReturn", equalTo(null))
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
                // FIXME: Why is this ASSIGNROLE and not "your dataset has been published"?
                .body("data.notifications[0].type", equalTo("ASSIGNROLE"))
                .body("data.notifications[1].type", equalTo("RETURNEDDS"))
                // .body("data.notifications[1].reasonsForReturn[0].message", equalTo("You forgot to upload any files."))
                //  .body("data.notifications[1].reasonsForReturn[1].message", equalTo("A README is required."))
                .body("data.notifications[2].type", equalTo("RETURNEDDS"))
                // Yes, it's a little weird that the reason for return on the first "RETURNEDDS" changed. For now we are always showing the most recent reason for return.
                //  .body("data.notifications[2].reasonsForReturn[0].message", equalTo("You forgot to upload any files."))
                //.body("data.notifications[2].reasonsForReturn[1].message", equalTo("A README is required."))
                    .body("data.notifications[3].type", equalTo("CREATEACC"))
                //   .body("data.notifications[3].reasonsForReturn", equalTo(null))
                .statusCode(OK.getStatusCode());

        // These println's are here in case you want to log into the GUI to see what notifications look like.
        System.out.println("Curator username/password: " + curatorUsername);
        System.out.println("Author username/password: " + authorUsername);

    }

}
