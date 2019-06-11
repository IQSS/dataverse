package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.path.xml.XmlPath;
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
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class InReviewWorkflowIT {

    private static final Logger logger = Logger.getLogger(DatasetsIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

    }

    @Test
    public void testCuratorSendsCommentsToAuthor() throws InterruptedException {
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

        // The author submits the dataset for review.
        Response submitForReview = UtilIT.submitDatasetForReview(datasetPersistentId, authorApiToken);
        submitForReview.prettyPrint();
        submitForReview.then().assertThat()
                .body("data.inReview", equalTo(true))
                .statusCode(OK.getStatusCode());

        boolean returnEarlyToTestEditButton = false;
        if (returnEarlyToTestEditButton) {
            System.out.println("Curator username/password and API token: " + curatorUsername + " and " + curatorApiToken);
            System.out.println("Author username/password and API token: " + authorUsername + " and " + authorApiToken);
            return;
        }

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

        // Joe Random, a user with no perms on dataset, tries returning the dataset as if he's a curator and fails.
        Response returnToAuthorFail = UtilIT.returnDatasetToAuthor(datasetPersistentId, joeRandObj.build(), joeRandomApiToken);
        returnToAuthorFail.prettyPrint();
        returnToAuthorFail.then().assertThat()
                .body("message", equalTo("User @" + joeRandomUsername + " is not permitted to perform requested action."))
                .statusCode(UNAUTHORIZED.getStatusCode());

        // BEGIN https://github.com/IQSS/dataverse/issues/4139
        // The author tries to edit the title after submitting the dataset for review. This is not allowed because the dataset is locked.
        Response updateTitleResponseAuthor = UtilIT.updateDatasetTitleViaSword(datasetPersistentId, "New Title from Author", authorApiToken);
        updateTitleResponseAuthor.prettyPrint();
        updateTitleResponseAuthor.then().assertThat()
                .body("error.summary", equalTo("problem updating dataset: edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException: Dataset cannot be edited due to In Review dataset lock."))
                .statusCode(BAD_REQUEST.getStatusCode());

        // The curator tries to update the title while the dataset is in review via SWORD.
        Response updateTitleResponseCuratorViaSword = UtilIT.updateDatasetTitleViaSword(datasetPersistentId, "A Better Title", curatorApiToken);
        updateTitleResponseCuratorViaSword.prettyPrint();
        updateTitleResponseCuratorViaSword.then().assertThat()
                .statusCode(OK.getStatusCode());
        Response atomEntry = UtilIT.getSwordAtomEntry(datasetPersistentId, curatorApiToken);
        atomEntry.prettyPrint();
        atomEntry.then().assertThat()
                .statusCode(OK.getStatusCode());
        String citation = XmlPath.from(atomEntry.body().asString()).getString("bibliographicCitation");
        System.out.println("citation: " + citation);
        Assert.assertTrue(citation.contains("A Better Title"));

        // The author tries to update the title while the dataset is in review via native.
        String pathToJsonFile = "doc/sphinx-guides/source/_static/api/dataset-update-metadata.json";
        Response updateTitleResponseAuthorViaNative = UtilIT.updateDatasetMetadataViaNative(datasetPersistentId, pathToJsonFile, authorApiToken);
        updateTitleResponseAuthorViaNative.prettyPrint();
        updateTitleResponseAuthorViaNative.then().assertThat()
                .body("message", equalTo("Dataset cannot be edited due to In Review dataset lock."))
                .statusCode(FORBIDDEN.getStatusCode());
        Response atomEntryAuthorNative = UtilIT.getSwordAtomEntry(datasetPersistentId, authorApiToken);
        atomEntryAuthorNative.prettyPrint();
        atomEntryAuthorNative.then().assertThat()
                .statusCode(OK.getStatusCode());
        String citationAuthorNative = XmlPath.from(atomEntryAuthorNative.body().asString()).getString("bibliographicCitation");
        System.out.println("citation: " + citationAuthorNative);
        // The author was unable to change the title.
        Assert.assertTrue(citationAuthorNative.contains("A Better Title"));

        // The author remembers she forgot to add a file and tries to upload it while
        // the dataset is in review via native API but this fails.
        String pathToFile1 = "src/main/webapp/resources/images/cc0.png";
        Response authorAttemptsToAddFileWhileInReviewViaNative = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile1, authorApiToken);
        authorAttemptsToAddFileWhileInReviewViaNative.prettyPrint();
        authorAttemptsToAddFileWhileInReviewViaNative.then().assertThat()
                // TODO: It would be nice to reveal the message AddReplaceFileHelper puts in server.log:
                // "Dataset cannot be edited due to In Review dataset lock."
                .body("message", equalTo("Failed to add file to dataset."))
                .statusCode(BAD_REQUEST.getStatusCode());

        // The author tries adding a random file via SWORD and this also fails due
        // to the dataset being in review.
        Response authorAttemptsToAddFileWhileInReviewViaSword = UtilIT.uploadRandomFile(datasetPersistentId, authorApiToken);
        authorAttemptsToAddFileWhileInReviewViaSword.prettyPrint();
        authorAttemptsToAddFileWhileInReviewViaSword.then().assertThat()
                .body("error.summary", equalTo("Couldn't update dataset edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException: Dataset cannot be edited due to In Review dataset lock."))
                .statusCode(BAD_REQUEST.getStatusCode());

        // The curator adds the file himself while
        // the dataset is in review via native API.
        Response curatorAttemptsToAddFileWhileInReviewViaNative = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile1, curatorApiToken);
        curatorAttemptsToAddFileWhileInReviewViaNative.prettyPrint();
        curatorAttemptsToAddFileWhileInReviewViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());

        boolean testMultipleLocks = true;
        if (testMultipleLocks) {
            // The curator adds a second file himself while
            // the dataset is in review via SWORD.
            String pathToFileThatGoesThroughIngest = "scripts/search/data/tabular/50by1000.dta.zip";
            Response curatorAttemptsToAddFileWhileInReviewViaSword = UtilIT.uploadZipFileViaSword(datasetPersistentId, pathToFileThatGoesThroughIngest, curatorApiToken);
            curatorAttemptsToAddFileWhileInReviewViaSword.prettyPrint();
            curatorAttemptsToAddFileWhileInReviewViaSword.then().assertThat()
                    .statusCode(CREATED.getStatusCode());
            // Give file time to ingest. The lock needs to go away before any edits can happen.
            boolean exerciseConcurrentModificationException = false;
            if (exerciseConcurrentModificationException) {
                String comments = "How do we feel about concurrency?";
                JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                jsonObjectBuilder.add("reasonForReturn", comments);
                Response returnToAuthor = UtilIT.returnDatasetToAuthor(datasetPersistentId, jsonObjectBuilder.build(), curatorApiToken);
                returnToAuthor.prettyPrint();
            } else {
                // Increasing the sleep delay here, from 2 to 10 sec.; 
                // With the 2 sec. delay, it appears to have been working consistently
                // on the phoenix server (because it's fast, I'm guessing?) - but 
                // I kept seeing an error on my own build at this point once in a while, 
                // because the dataset is still locked when we try to edit it, 
                // a few lines down. -- L.A. Oct. 2018
                Thread.sleep(10000);
            }
        }

        // The author changes his mind and figures this is a teaching moment to
        // have the author upload the file herself. He deletes the files and will
        // ask her to upload files on her own.
        Response getFileIdRequest = UtilIT.nativeGet(datasetId, curatorApiToken);
        getFileIdRequest.prettyPrint();
        getFileIdRequest.then().assertThat()
                .statusCode(OK.getStatusCode());;
        int fileId1 = JsonPath.from(getFileIdRequest.getBody().asString()).getInt("data.latestVersion.files[0].dataFile.id");
        Response deleteFile1 = UtilIT.deleteFile(fileId1, curatorApiToken);
        deleteFile1.prettyPrint();
        deleteFile1.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());
        int fileId2 = JsonPath.from(getFileIdRequest.getBody().asString()).getInt("data.latestVersion.files[1].dataFile.id");
        Response deleteFile2 = UtilIT.deleteFile(fileId2, curatorApiToken);
        deleteFile2.prettyPrint();
        deleteFile2.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());

        // The curator tries to update the title while the dataset is in review via native.
        Response updateTitleResponseCuratorViaNative = UtilIT.updateDatasetMetadataViaNative(datasetPersistentId, pathToJsonFile, curatorApiToken);
        updateTitleResponseCuratorViaNative.prettyPrint();
        updateTitleResponseCuratorViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());
        Response atomEntryCuratorNative = UtilIT.getSwordAtomEntry(datasetPersistentId, curatorApiToken);
        atomEntryCuratorNative.prettyPrint();
        atomEntryCuratorNative.then().assertThat()
                .statusCode(OK.getStatusCode());
        String citationCuratorNative = XmlPath.from(atomEntryCuratorNative.body().asString()).getString("bibliographicCitation");
        System.out.println("citation: " + citationCuratorNative);
        Assert.assertTrue(citationCuratorNative.contains("newTitle"));
        // END https://github.com/IQSS/dataverse/issues/4139

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
                .body("data.notifications[0].type", equalTo("PUBLISHEDDS"))
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
