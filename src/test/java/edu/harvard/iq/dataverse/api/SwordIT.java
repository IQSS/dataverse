package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.api.datadeposit.SwordConfigurationImpl;
import java.util.List;
import java.util.logging.Logger;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * In all these tests you should never see something like "[long string exposing
 * internal user details] ...is missing permissions [AddDataset] on Object"
 * which is an indication that we have failed to put a permission check in place
 * and failed to write a human-readable response.
 *
 * @todo Add tests for "Member" role after "createAssignment" in Datasets.java
 * is real. "grantRoleOnDataset" in UtilIT.java is not used despite being in
 * https://github.com/IQSS/dataverse/pull/3111
 */
public class SwordIT {

    private static final Logger logger = Logger.getLogger(SwordIT.class.getCanonicalName());
    private static String superuser;
    private static final String rootDataverseAlias = "root";
    private static String apiTokenSuperuser;

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
        boolean testAgainstDev1 = false;
        if (testAgainstDev1) {
            RestAssured.baseURI = "https://dev1.dataverse.org";
        }
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        superuser = UtilIT.getUsernameFromResponse(createUser);
        apiTokenSuperuser = UtilIT.getApiTokenFromResponse(createUser);
        String apitoken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.makeSuperUser(superuser).then().assertThat().statusCode(OK.getStatusCode());
        Response checkRootDataverse = UtilIT.listDatasetsViaSword(rootDataverseAlias, apitoken);
        checkRootDataverse.prettyPrint();
        checkRootDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());
        boolean rootDataverseHasBeenReleased = checkRootDataverse.getBody().xmlPath().getBoolean("feed.dataverseHasBeenReleased");
        if (!rootDataverseHasBeenReleased) {
            logger.info("Many of these SWORD tests require that the root dataverse has been published. Publish the root dataverse and then re-run these tests.");
            System.exit(666);
        }

    }

    @Test
    public void testServiceDocument() {
        Response createUser = UtilIT.createRandomUser();

        String username = UtilIT.getUsernameFromResponse(createUser);
        String apitoken = UtilIT.getApiTokenFromResponse(createUser);

        Response serviceDocumentResponse = UtilIT.getServiceDocument(apitoken);
        serviceDocumentResponse.prettyPrint();

        SwordConfigurationImpl swordConfiguration = new SwordConfigurationImpl();

        serviceDocumentResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("service.version", equalTo(swordConfiguration.generatorVersion()));

        String collection = serviceDocumentResponse.getBody().xmlPath().get("service.workspace.collection").toString();
        System.out.println("collection: " + collection);
        /**
         * We assume that you have configured your installation to allow the
         * ":authenticated-users" groups to create datasets in the root
         * dataverse via the "fullContributor" role or similar.
         */
        assertTrue(serviceDocumentResponse.body().asString().contains("swordv2/collection/dataverse/root"));

        Response deleteUser1Response = UtilIT.deleteUser(username);
        deleteUser1Response.prettyPrint();
        boolean issue2825Resolved = false;
        if (issue2825Resolved) {
            /**
             * We can't delete this user because in some cases the user has
             * released the root dataverse:
             * https://github.com/IQSS/dataverse/issues/2825
             */
            assertEquals(200, deleteUser1Response.getStatusCode());
        }
        UtilIT.deleteUser(username);

    }
    
    @Test
    public void testSwordAuthUserLastApiUse(){
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apitoken = UtilIT.getApiTokenFromResponse(createUser);
        
        Response serviceDocumentResponse = UtilIT.getServiceDocument(apitoken);
        
        Response getUserAsJsonAgain = UtilIT.getAuthenticatedUser(username, apitoken);
        getUserAsJsonAgain.prettyPrint();
        getUserAsJsonAgain.then().assertThat()
                // Checking that it's 2017 or whatever. Not y3k compliant! 
                .body("data.lastApiUseTime", startsWith("2"))
                .statusCode(OK.getStatusCode());

        UtilIT.deleteUser(username);
    }

    @Test
    public void testServiceDocumentWithInvalidApiToken() {

        Response serviceDocumentResponse = UtilIT.getServiceDocument("invalidApiToken");
//        serviceDocumentResponse.prettyPrint();

        serviceDocumentResponse.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    public void testCreateDataverseCreateDatasetUploadFileDownloadFileEditTitle() {

        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        assertEquals(CREATED.getStatusCode(), createDataverseResponse.getStatusCode());

        String initialDatasetTitle = "My Working Title";

        Response randomUnprivUser = UtilIT.createRandomUser();
        String apiTokenNoPrivs = UtilIT.getApiTokenFromResponse(randomUnprivUser);
        String usernameNoPrivs = UtilIT.getUsernameFromResponse(randomUnprivUser);

        Response createDatasetShouldFail = UtilIT.createDatasetViaSwordApi(dataverseAlias, initialDatasetTitle, apiTokenNoPrivs);
        createDatasetShouldFail.prettyPrint();
        createDatasetShouldFail.then().assertThat()
                /**
                 * @todo It would be nice if this could be UNAUTHORIZED or
                 * FORBIDDEN rather than BAD_REQUEST.
                 */
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("error.summary", equalTo("user " + usernameNoPrivs + " " + usernameNoPrivs + " is not authorized to create a dataset in this dataverse."));

        /**
         * "Clients MUST NOT require a Collection Feed Document for deposit
         * operation." -- 6.2 Listing Collections
         * http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#protocoloperations_listingcollections
         */
        Response createDatasetResponse = UtilIT.createDatasetViaSwordApi(dataverseAlias, initialDatasetTitle, apiToken);
        createDatasetResponse.prettyPrint();
        assertEquals(CREATED.getStatusCode(), createDatasetResponse.getStatusCode());
        String persistentId = UtilIT.getDatasetPersistentIdFromSwordResponse(createDatasetResponse);
        logger.info("persistent id: " + persistentId);

        Response atomEntryUnAuth = UtilIT.getSwordAtomEntry(persistentId, apiTokenNoPrivs);
        atomEntryUnAuth.prettyPrint();
        atomEntryUnAuth.then().assertThat()
                /**
                 * @todo It would be nice if this could be UNAUTHORIZED or
                 * FORBIDDEN rather than BAD_REQUEST.
                 */
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("error.summary", equalTo("User " + usernameNoPrivs + " " + usernameNoPrivs + " is not authorized to retrieve entry for " + persistentId));

        Response atomEntry = UtilIT.getSwordAtomEntry(persistentId, apiToken);
        atomEntry.prettyPrint();
        atomEntry.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("entry.treatment", equalTo("no treatment information available"));

        Response listDatasetsUnAuth = UtilIT.listDatasetsViaSword(dataverseAlias, apiTokenNoPrivs);
        listDatasetsUnAuth.prettyPrint();
        listDatasetsUnAuth.then().assertThat()
                /**
                 * @todo It would be nice if this could be UNAUTHORIZED or
                 * FORBIDDEN rather than BAD_REQUEST.
                 */
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("error.summary", equalTo("user " + usernameNoPrivs + " " + usernameNoPrivs + " is not authorized to list datasets in dataverse " + dataverseAlias));

        Response listDatasetsResponse = UtilIT.listDatasetsViaSword(dataverseAlias, apiToken);
        listDatasetsResponse.prettyPrint();
        listDatasetsResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("feed.dataverseHasBeenReleased", equalTo("false"))
                .body("feed.entry[0].title", equalTo(initialDatasetTitle));

        Response uploadFileUnAuth = UtilIT.uploadRandomFile(persistentId, apiTokenNoPrivs);
        uploadFileUnAuth.prettyPrint();
        uploadFileUnAuth.then().assertThat()
                /**
                 * @todo It would be nice if this could be UNAUTHORIZED or
                 * FORBIDDEN rather than BAD_REQUEST.
                 */
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("error.summary", equalTo("user " + usernameNoPrivs + " " + usernameNoPrivs + " is not authorized to modify dataset with global ID " + persistentId));

        Response uploadFile1 = UtilIT.uploadRandomFile(persistentId, apiToken);
        uploadFile1.prettyPrint();
        assertEquals(CREATED.getStatusCode(), uploadFile1.getStatusCode());

        Response getDatasetJson = UtilIT.nativeGetUsingPersistentId(persistentId, apiToken);
        getDatasetJson.prettyPrint();
        getDatasetJson.then().assertThat()
                .body("data.latestVersion.files[0].dataFile.filename", equalTo("trees.png"))
                .body("data.latestVersion.files[0].dataFile.rootDataFileId", equalTo(-1))
                .body("data.latestVersion.files[0].dataFile.previousDataFileId", nullValue())
                .statusCode(OK.getStatusCode());

        Response swordStatementUnAuth = UtilIT.getSwordStatement(persistentId, apiTokenNoPrivs);
        swordStatementUnAuth.prettyPrint();
        swordStatementUnAuth.then().assertThat()
                /**
                 * @todo It would be nice if this could be UNAUTHORIZED or
                 * FORBIDDEN rather than BAD_REQUEST.
                 */
                .body("error.summary", equalTo("user " + usernameNoPrivs + " " + usernameNoPrivs + " is not authorized to view dataset with global ID " + persistentId))
                .statusCode(BAD_REQUEST.getStatusCode());

        Response swordStatement = UtilIT.getSwordStatement(persistentId, apiToken);
        swordStatement.prettyPrint();
        String title = UtilIT.getTitleFromSwordStatementResponse(swordStatement);
        assertEquals(initialDatasetTitle, title);
        Integer fileId = UtilIT.getFileIdFromSwordStatementResponse(swordStatement);
        assertNotNull(fileId);
        assertEquals(Integer.class, fileId.getClass());

        logger.info("Id of uploaded file: " + fileId);
        String filename = UtilIT.getFilenameFromSwordStatementResponse(swordStatement);
        assertNotNull(filename);
        assertEquals(String.class, filename.getClass());
        logger.info("Filename of uploaded file: " + filename);
        assertEquals("trees.png", filename);

        Response attemptToDownloadUnpublishedFileWithoutApiToken = UtilIT.downloadFile(fileId);
        attemptToDownloadUnpublishedFileWithoutApiToken.prettyPrint();
        attemptToDownloadUnpublishedFileWithoutApiToken.then().assertThat()
                .body("status", equalTo("ERROR"))
                .statusCode(FORBIDDEN.getStatusCode());

        Response attemptToDownloadUnpublishedFileUnauthApiToken = UtilIT.downloadFile(fileId, apiTokenNoPrivs);
        attemptToDownloadUnpublishedFileUnauthApiToken.prettyPrint();
        attemptToDownloadUnpublishedFileUnauthApiToken.then().assertThat()
                .body("status", equalTo("ERROR"))
                .statusCode(FORBIDDEN.getStatusCode());

        Response downloadUnpublishedFileWithValidApiToken = UtilIT.downloadFile(fileId, apiToken);
        assertEquals(OK.getStatusCode(), downloadUnpublishedFileWithValidApiToken.getStatusCode());
        logger.info("downloaded " + downloadUnpublishedFileWithValidApiToken.getContentType() + " (" + downloadUnpublishedFileWithValidApiToken.asByteArray().length + " bytes)");

        Response deleteFileUnAuth = UtilIT.deleteFile(fileId, apiTokenNoPrivs);
        deleteFileUnAuth.prettyPrint();
        deleteFileUnAuth.then().assertThat()
                /**
                 * @todo It would be nice if this could be UNAUTHORIZED or
                 * FORBIDDEN rather than BAD_REQUEST.
                 */
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("error.summary", equalTo("User " + usernameNoPrivs + " " + usernameNoPrivs + " is not authorized to modify " + dataverseAlias));

        Response deleteFile = UtilIT.deleteFile(fileId, apiToken);
        deleteFile.prettyPrint();
        deleteFile.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());

        Response downloadDeletedFileWithValidApiToken = UtilIT.downloadFile(fileId, apiToken);
        assertEquals(NOT_FOUND.getStatusCode(), downloadDeletedFileWithValidApiToken.getStatusCode());

        String newTitle = "My Awesome Dataset";
        Response updatedMetadataUnAuth = UtilIT.updateDatasetTitleViaSword(persistentId, newTitle, apiTokenNoPrivs);
        updatedMetadataUnAuth.prettyPrint();
        updatedMetadataUnAuth.then().assertThat()
                /**
                 * @todo It would be nice if this could be UNAUTHORIZED or
                 * FORBIDDEN rather than BAD_REQUEST.
                 */
                .statusCode(BAD_REQUEST.getStatusCode())
                /**
                 * @todo This error doesn't make a ton of sense. Something like
                 * "not authorized to modify dataset metadata" would be better.
                 */
                .body("error.summary", equalTo("User " + usernameNoPrivs + " " + usernameNoPrivs + " is not authorized to modify dataverse " + dataverseAlias));

        Response updatedMetadataResponse = UtilIT.updateDatasetTitleViaSword(persistentId, newTitle, apiToken);
        updatedMetadataResponse.prettyPrint();
        swordStatement = UtilIT.getSwordStatement(persistentId, apiToken);
        title = UtilIT.getTitleFromSwordStatementResponse(swordStatement);
        assertEquals(newTitle, title);
        logger.info("Title updated from \"" + initialDatasetTitle + "\" to \"" + newTitle + "\".");

        Response deleteDatasetUnAuth = UtilIT.deleteLatestDatasetVersionViaSwordApi(persistentId, apiTokenNoPrivs);
        deleteDatasetUnAuth.prettyPrint();
        deleteDatasetUnAuth.then().assertThat()
                /**
                 * @todo It would be nice if this could be UNAUTHORIZED or
                 * FORBIDDEN rather than BAD_REQUEST.
                 */
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("error.summary", equalTo("User " + usernameNoPrivs + " " + usernameNoPrivs + " is not authorized to modify " + dataverseAlias));

        Response deleteDatasetResponse = UtilIT.deleteLatestDatasetVersionViaSwordApi(persistentId, apiToken);
        deleteDatasetResponse.prettyPrint();
        deleteDatasetResponse.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());

        Response deleteDataverse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverse.prettyPrint();
        deleteDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        UtilIT.deleteUser(username);
        UtilIT.deleteUser(usernameNoPrivs);

    }

    /**
     * This test requires the root dataverse to have been published already. We
     * assume the default, out-of-the-box configuration that the answer to the
     * question "What should be the default role for someone adding datasets to
     * this dataverse?" is "Contributor" rather than "Curator". We are
     * permitting both dataverse and datasets to be created in the root
     * dataverse (:authenticated-users gets fullContributor at root).
     */
    @Test
    public void testCreateAndDeleteDatasetInRoot() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiTokenContributor = UtilIT.getApiTokenFromResponse(createUser);

        String datasetTitle = "Dataset In Root";

        Response randomUnprivUser = UtilIT.createRandomUser();
        String apiTokenNoPrivs = UtilIT.getApiTokenFromResponse(randomUnprivUser);
        String usernameNoPrivs = UtilIT.getUsernameFromResponse(randomUnprivUser);

        String persistentId = null;
        Integer datasetId = null;
        String protocol;
        String authority;
        String identifier = null;

        Response createDataset = UtilIT.createDatasetViaSwordApi(rootDataverseAlias, datasetTitle, apiTokenContributor);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode())
                .body("entry.treatment", equalTo("no treatment information available"));

        persistentId = UtilIT.getDatasetPersistentIdFromSwordResponse(createDataset);
        GlobalId globalId = new GlobalId(persistentId);
        protocol = globalId.getProtocol();
        authority = globalId.getAuthority();
        identifier = globalId.getIdentifier();

        Response listDatasetsAtRoot = UtilIT.listDatasetsViaSword(rootDataverseAlias, apiTokenContributor);
        listDatasetsAtRoot.prettyPrint();
        listDatasetsAtRoot.then().assertThat().statusCode(OK.getStatusCode());
        assertTrue(listDatasetsAtRoot.body().asString().contains(identifier));

        Response listDatasetsAtRootNoPrivs = UtilIT.listDatasetsViaSword(rootDataverseAlias, apiTokenNoPrivs);
        listDatasetsAtRootNoPrivs.prettyPrint();
        listDatasetsAtRootNoPrivs.then().assertThat()
                .statusCode(OK.getStatusCode())
                /**
                 * Because the root dataverse allows anyone to create datasets
                 * in it, we allow anyone to see via SWORD if the dataverse has
                 * been published...
                 */
                .body("feed.dataverseHasBeenReleased", equalTo("true"));
        /**
         * ... but not just anyone should be able to see your dataset, only
         * those with edit access.
         */
        assertFalse(listDatasetsAtRootNoPrivs.body().asString().contains(identifier));

        Response atomEntry = UtilIT.getSwordAtomEntry(persistentId, apiTokenContributor);
        atomEntry.prettyPrint();
        atomEntry.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("entry.id", endsWith(persistentId));

        Response uploadFile = UtilIT.uploadRandomFile(persistentId, apiTokenContributor);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat().statusCode(CREATED.getStatusCode());

        Response statementContainingFile = UtilIT.getSwordStatement(persistentId, apiTokenContributor);
        statementContainingFile.prettyPrint();
        statementContainingFile.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("feed.id", endsWith(persistentId))
                .body("feed.title", equalTo(datasetTitle))
                .body("feed.author.name", equalTo("Lastname, Firstname"))
                .body("feed.entry[0].summary", equalTo("Resource Part"))
                .body("feed.entry[0].id", endsWith("trees.png"));

        String firstAndOnlyFileIdAsString = statementContainingFile.getBody().xmlPath().get("feed.entry[0].id").toString().split("/")[10];
        Response deleteFile = UtilIT.deleteFile(Integer.parseInt(firstAndOnlyFileIdAsString), apiTokenContributor);
        deleteFile.prettyPrint();
        deleteFile.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());

        Response statementWithNoFiles = UtilIT.getSwordStatement(persistentId, apiTokenContributor);
        statementWithNoFiles.prettyPrint();
        statementWithNoFiles.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("feed.title", equalTo(datasetTitle));

        try {
            String attemptToGetFileId = statementWithNoFiles.getBody().xmlPath().get("feed.entry[0].id").toString().split("/")[10];
            System.out.println("attemptToGetFileId: " + attemptToGetFileId);
            assertNull(attemptToGetFileId);
        } catch (Exception ex) {
            System.out.println("We expect an exception here because we can no longer find the file because deleted it: " + ex);
            assertTrue(ex.getClass().getName().equals(ArrayIndexOutOfBoundsException.class.getName()));
        }

        String newTitle = "A New Hope";
        Response updateTitle = UtilIT.updateDatasetTitleViaSword(persistentId, newTitle, apiTokenContributor);
        updateTitle.prettyPrint();
        updateTitle.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("entry.treatment", equalTo("no treatment information available"));

        Response statementWithUpdatedTitle = UtilIT.getSwordStatement(persistentId, apiTokenContributor);
        statementWithUpdatedTitle.prettyPrint();
        statementWithUpdatedTitle.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("feed.title", equalTo(newTitle));

        Response nativeGetToGetId = UtilIT.nativeGetUsingPersistentId(persistentId, apiTokenContributor);
        nativeGetToGetId.then().assertThat()
                .statusCode(OK.getStatusCode());
        datasetId = JsonPath.from(nativeGetToGetId.body().asString()).getInt("data.id");

        Response listDatasetsAtRootAsSuperuser = UtilIT.listDatasetsViaSword(rootDataverseAlias, apiTokenSuperuser);
        listDatasetsAtRootAsSuperuser.prettyPrint();
        listDatasetsAtRootAsSuperuser.then().assertThat().statusCode(OK.getStatusCode());
        assertTrue(listDatasetsAtRootAsSuperuser.body().asString().contains(identifier));



        Response publishShouldFailForContributorViaSword = UtilIT.publishDatasetViaSword(persistentId, apiTokenContributor);
        publishShouldFailForContributorViaSword.prettyPrint();
        publishShouldFailForContributorViaSword.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("error.summary", equalTo("User " + username + " " + username + " is not authorized to modify dataverse " + rootDataverseAlias));

        Response publishShouldFailForContributorViaNative = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiTokenContributor);
        publishShouldFailForContributorViaNative.prettyPrint();
        publishShouldFailForContributorViaNative.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode())
                .body("message", equalTo("User @" + username + " is not permitted to perform requested action."));

        Response deleteDatasetResponse = UtilIT.deleteLatestDatasetVersionViaSwordApi(persistentId, apiTokenContributor);
        deleteDatasetResponse.prettyPrint();
        deleteDatasetResponse.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());

        UtilIT.deleteUser(username);
        UtilIT.deleteUser(usernameNoPrivs);

//        UtilIT.listDatasetsViaSword(rootDataverseAlias, apiTokenSuperuser).prettyPrint();
    }

    /**
     * This test requires the root dataverse to have been published already.
     */
    @Test
    public void testCreateDatasetPublishDestroy() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        String datasetTitle = "Publish or Perish";
        Response createDataset = UtilIT.createDatasetViaSwordApi(dataverseAlias, datasetTitle, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String persistentId = UtilIT.getDatasetPersistentIdFromSwordResponse(createDataset);

        Response attemptToPublishDatasetInUnpublishedDataverse = UtilIT.publishDatasetViaSword(persistentId, apiToken);
        attemptToPublishDatasetInUnpublishedDataverse.prettyPrint();
        attemptToPublishDatasetInUnpublishedDataverse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

        Response randomUnprivUser = UtilIT.createRandomUser();
        String apiTokenNoPrivs = UtilIT.getApiTokenFromResponse(randomUnprivUser);
        String usernameNoPrivs = UtilIT.getUsernameFromResponse(randomUnprivUser);

        Response publishDataverseUnAuth = UtilIT.publishDataverseViaSword(dataverseAlias, apiTokenNoPrivs);
        publishDataverseUnAuth.prettyPrint();
        publishDataverseUnAuth.then().assertThat()
                /**
                 * @todo It would be nice if this could be UNAUTHORIZED or
                 * FORBIDDEN rather than BAD_REQUEST.
                 */
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("error.summary", equalTo("User " + usernameNoPrivs + " " + usernameNoPrivs + " is not authorized to modify dataverse " + dataverseAlias));

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDatsetUnAuth = UtilIT.publishDatasetViaSword(persistentId, apiTokenNoPrivs);
        System.out.println("BEGIN");
        publishDatsetUnAuth.prettyPrint();
        System.out.println("END");
        publishDatsetUnAuth.then().assertThat()
                /**
                 * @todo It would be nice if this could be UNAUTHORIZED or
                 * FORBIDDEN rather than BAD_REQUEST.
                 */
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("error.summary", equalTo("User " + usernameNoPrivs + " " + usernameNoPrivs + " is not authorized to modify dataverse " + dataverseAlias));
        
        Response thisDataverseContents = UtilIT.showDataverseContents(dataverseAlias, apiTokenNoPrivs);
        thisDataverseContents.prettyPrint();
        thisDataverseContents.then().assertThat()
                .statusCode(OK.getStatusCode());
        logger.info("Without priviledges we do not expect to find \"" + persistentId + "\" from the persistent ID to be present for random user");
        assertFalse(thisDataverseContents.body().asString().contains(persistentId.toString()));

        
        Response publishDataset = UtilIT.publishDatasetViaSword(persistentId, apiToken);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response attemptToDeletePublishedDataset = UtilIT.deleteLatestDatasetVersionViaSwordApi(persistentId, apiToken);
        attemptToDeletePublishedDataset.prettyPrint();
        attemptToDeletePublishedDataset.then().assertThat()
                .statusCode(METHOD_NOT_ALLOWED.getStatusCode());

        String newTitle = "A New Hope";
        Response updateTitle = UtilIT.updateDatasetTitleViaSword(persistentId, newTitle, apiToken);
        updateTitle.prettyPrint();
        updateTitle.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("entry.treatment", equalTo("no treatment information available"));

        Response deletePostPublicationDraft = UtilIT.deleteLatestDatasetVersionViaSwordApi(persistentId, apiToken);
        deletePostPublicationDraft.prettyPrint();
        deletePostPublicationDraft.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());

        /**
         * @todo This can probably be removed now that
         * https://github.com/IQSS/dataverse/issues/1837 has been fixed.
         */
        Response reindexDatasetToFindDatabaseId = UtilIT.reindexDataset(persistentId);
        reindexDatasetToFindDatabaseId.prettyPrint();
        reindexDatasetToFindDatabaseId.then().assertThat()
                .statusCode(OK.getStatusCode());

        Integer datasetId = JsonPath.from(reindexDatasetToFindDatabaseId.asString()).getInt("data.id");

        /* get contents again after publication - should see id*/
        thisDataverseContents = UtilIT.showDataverseContents(dataverseAlias, apiToken);
        thisDataverseContents.prettyPrint();
        thisDataverseContents.then().assertThat()
                .statusCode(OK.getStatusCode());
        logger.info("We expect to find the numeric id of the dataset (\"" + datasetId + "\") in the response.");
        assertTrue(thisDataverseContents.body().asString().contains(datasetId.toString()));
        
        
        /**
         * @todo The "destroy" endpoint should accept a persistentId:
         * https://github.com/IQSS/dataverse/issues/1837
         */
        Response makeSuperuserRespone = UtilIT.makeSuperUser(username);
        makeSuperuserRespone.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response destroyDataset = UtilIT.destroyDataset(datasetId, apiToken);
        destroyDataset.prettyPrint();
        destroyDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response atomEntryDestroyed = UtilIT.getSwordAtomEntry(persistentId, apiToken);
        atomEntryDestroyed.prettyPrint();
        atomEntryDestroyed.then().statusCode(400);

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        UtilIT.deleteUser(username);
        UtilIT.deleteUser(usernameNoPrivs);

    }

    /**
     * This test requires the root dataverse to have been published already.
     *
     * Test the following issues:
     *
     * - https://github.com/IQSS/dataverse/issues/1784
     *
     * - https://github.com/IQSS/dataverse/issues/2222
     *
     * - https://github.com/IQSS/dataverse/issues/2464
     */
    @Test
    public void testDeleteFiles() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaSwordApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromSwordResponse(createDataset);

        Response uploadZip = UtilIT.uploadFile(datasetPersistentId, "3files.zip", apiToken);
        uploadZip.prettyPrint();
        assertEquals(CREATED.getStatusCode(), uploadZip.getStatusCode());
        Response statement1 = UtilIT.getSwordStatement(datasetPersistentId, apiToken);
        statement1.prettyPrint();
        String index0a = statement1.getBody().xmlPath().get("feed.entry[0].id").toString().split("/")[10];
        String index1a = statement1.getBody().xmlPath().get("feed.entry[1].id").toString().split("/")[10];
        String index2a = statement1.getBody().xmlPath().get("feed.entry[2].id").toString().split("/")[10];

        List<String> fileList = statement1.getBody().xmlPath().getList("feed.entry.id");
        logger.info("Dataset contains file ids: " + index0a + " " + index1a + " " + index2a + " (" + fileList.size() + ") files");

        Response deleteIndex0a = UtilIT.deleteFile(Integer.parseInt(index0a), apiToken);
//        deleteIndex0a.prettyPrint();
        deleteIndex0a.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());
        logger.info("Deleted file id " + index0a + " from draft of unpublished dataset.");

        Response statement2 = UtilIT.getSwordStatement(datasetPersistentId, apiToken);
        statement2.prettyPrint();
        String index0b = statement2.getBody().xmlPath().get("feed.entry[0].id").toString().split("/")[10];
        String index1b = statement2.getBody().xmlPath().get("feed.entry[1].id").toString().split("/")[10];
        try {
            String index2b = statement2.getBody().xmlPath().get("feed.entry[2].id").toString().split("/")[10];
        } catch (ArrayIndexOutOfBoundsException ex) {
            // expected since file has been deleted
        }
        logger.info("Dataset contains file ids: " + index0b + " " + index1b);
        List<String> twoFilesLeftInV2Draft = statement2.getBody().xmlPath().getList("feed.entry.id");
        logger.info("Number of files remaining in this draft:" + twoFilesLeftInV2Draft.size());
        assertEquals(2, twoFilesLeftInV2Draft.size());

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
//        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        logger.info("dataset has not yet been published:");
        Response atomEntryUnpublished = UtilIT.getSwordAtomEntry(datasetPersistentId, apiToken);
        atomEntryUnpublished.prettyPrint();

        Response publishDataset = UtilIT.publishDatasetViaSword(datasetPersistentId, apiToken);
//        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        logger.info("dataset has been published:");
        Response atomEntryPublishedV1 = UtilIT.getSwordAtomEntry(datasetPersistentId, apiToken);
        atomEntryPublishedV1.prettyPrint();

        Response deleteIndex0b = UtilIT.deleteFile(Integer.parseInt(index0b), apiToken);
//        deleteIndex0b.prettyPrint();
        deleteIndex0b.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());
        logger.info("Deleted file id " + index0b + " from published dataset (should create draft).");
        Response statement3 = UtilIT.getSwordStatement(datasetPersistentId, apiToken);
        statement3.prettyPrint();

        logger.info("draft created from published dataset because a file was deleted:");
        Response atomEntryDraftV2 = UtilIT.getSwordAtomEntry(datasetPersistentId, apiToken);
        atomEntryDraftV2.prettyPrint();
        String citation = atomEntryDraftV2.body().xmlPath().getString("entry.bibliographicCitation");
        logger.info("citation (should contain 'DRAFT'): " + citation);
        boolean draftStringFoundInCitation = citation.matches(".*DRAFT.*");
        assertEquals(true, draftStringFoundInCitation);

        List<String> oneFileLeftInV2Draft = statement3.getBody().xmlPath().getList("feed.entry.id");
        logger.info("Number of files remaining in this post version 1 draft:" + oneFileLeftInV2Draft.size());
        assertEquals(1, oneFileLeftInV2Draft.size());

        Response deleteIndex1b = UtilIT.deleteFile(Integer.parseInt(index1b), apiToken);
        deleteIndex1b.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());
        logger.info("Deleted file id " + index1b + " from draft version of a published dataset.");

        Response statement4 = UtilIT.getSwordStatement(datasetPersistentId, apiToken);
        statement4.prettyPrint();

        List<String> fileListEmpty = statement4.getBody().xmlPath().getList("feed.entry.id");
        logger.info("Number of files remaining:" + fileListEmpty.size());
        assertEquals(0, fileListEmpty.size());

        Response deleteDatasetDraft = UtilIT.deleteLatestDatasetVersionViaSwordApi(datasetPersistentId, apiToken);
        deleteDatasetDraft.prettyPrint();

        Response statement5 = UtilIT.getSwordStatement(datasetPersistentId, apiToken);
        statement5.prettyPrint();
        List<String> twoFilesinV1published = statement5.getBody().xmlPath().getList("feed.entry.id");
        logger.info("Number of files in V1 (draft has been deleted)" + twoFilesinV1published.size());
        assertEquals(2, twoFilesinV1published.size());

        /**
         * @todo The "destroy" endpoint should accept a persistentId:
         * https://github.com/IQSS/dataverse/issues/1837
         */
        Response reindexDatasetToFindDatabaseId = UtilIT.reindexDataset(datasetPersistentId);
        reindexDatasetToFindDatabaseId.prettyPrint();
        reindexDatasetToFindDatabaseId.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer datasetId3 = JsonPath.from(reindexDatasetToFindDatabaseId.asString()).getInt("data.id");
        Response makeSuperuserRespone = UtilIT.makeSuperUser(username);
        makeSuperuserRespone.then().assertThat()
                .statusCode(OK.getStatusCode());
        Response destroyDataset = UtilIT.destroyDataset(datasetId3, apiToken);
        destroyDataset.prettyPrint();
        destroyDataset.then().assertThat()
                .statusCode(OK.getStatusCode());
        logger.info("Dataset has been destroyed: " + datasetPersistentId + " id " + datasetId3 + " but let's double check we can't access it...");

        Response atomEntryDestroyed = UtilIT.getSwordAtomEntry(datasetPersistentId, apiToken);
        atomEntryDestroyed.prettyPrint();
        atomEntryDestroyed.then().statusCode(400);

        Response createDataset4 = UtilIT.createRandomDatasetViaSwordApi(dataverseAlias, apiToken);
        createDataset4.prettyPrint();
        String datasetPersistentId4 = UtilIT.getDatasetPersistentIdFromSwordResponse(createDataset4);

        Response uploadZipToDataset4 = UtilIT.uploadFile(datasetPersistentId4, "3files.zip", apiToken);
        uploadZipToDataset4.prettyPrint();
        assertEquals(CREATED.getStatusCode(), uploadZipToDataset4.getStatusCode());
        Response publishDataset4 = UtilIT.publishDatasetViaSword(datasetPersistentId4, apiToken);
//        publishDataset4.prettyPrint();
        publishDataset4.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response statement4a = UtilIT.getSwordStatement(datasetPersistentId4, apiToken);
        statement4a.prettyPrint();
        List<String> threePublishedFiles = statement4a.getBody().xmlPath().getList("feed.entry.id");
        logger.info("Number of files in lastest version (v1 published) of " + datasetPersistentId4 + threePublishedFiles.size());
        assertEquals(3, threePublishedFiles.size());
        String dataset4FileIndex0 = statement4a.getBody().xmlPath().get("feed.entry[0].id").toString().split("/")[10];
        String dataset4FileIndex1 = statement4a.getBody().xmlPath().get("feed.entry[1].id").toString().split("/")[10];
        String dataset4FileIndex2 = statement4a.getBody().xmlPath().get("feed.entry[2].id").toString().split("/")[10];
        /**
         * @todo Fix https://github.com/IQSS/dataverse/issues/2464 so that *any*
         * file can be deleted via SWORD and not just the first file (zero
         * index). Attempting to delete dataset4FileIndex1 or dataset4FileIndex2
         * will exercise the bug. Attempting to delete dataset4FileIndex0 will
         * not.
         */
        String fileToDeleteFromDataset4 = dataset4FileIndex1;

        Response deleteFileFromDataset4 = UtilIT.deleteFile(Integer.parseInt(fileToDeleteFromDataset4), apiToken);
        deleteFileFromDataset4.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());
        logger.info("Deleted file id " + fileToDeleteFromDataset4 + " from " + datasetPersistentId4 + " which should move it from published to draft.");
        Response statement4b = UtilIT.getSwordStatement(datasetPersistentId4, apiToken);
        statement4b.prettyPrint();

        boolean issue2464fixed = false;
        if (issue2464fixed) {
            List<String> datasetMovedToDraftWithTwoFilesLeft = statement4b.getBody().xmlPath().getList("feed.entry.id");
            logger.info("Number of files left in " + datasetPersistentId4 + ": " + datasetMovedToDraftWithTwoFilesLeft.size());
            assertEquals(2, datasetMovedToDraftWithTwoFilesLeft.size());
        } else {
            List<String> issue2464NotFixedYetSoAllThreeFilesRemain = statement4b.getBody().xmlPath().getList("feed.entry.id");
            logger.info("Number of files left in " + datasetPersistentId4 + ": " + issue2464NotFixedYetSoAllThreeFilesRemain.size());
        }

        /**
         * @todo The "destroy" endpoint should accept a persistentId:
         * https://github.com/IQSS/dataverse/issues/1837
         */
        Response reindexDataset4ToFindDatabaseId = UtilIT.reindexDataset(datasetPersistentId4);
        reindexDataset4ToFindDatabaseId.prettyPrint();
        reindexDataset4ToFindDatabaseId.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer datasetId4 = JsonPath.from(reindexDataset4ToFindDatabaseId.asString()).getInt("data.id");

        Response destroyDataset4 = UtilIT.destroyDataset(datasetId4, apiToken);
        destroyDataset4.prettyPrint();
        destroyDataset4.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response deleteDataverse3Response = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverse3Response.prettyPrint();
        assertEquals(200, deleteDataverse3Response.getStatusCode());

        UtilIT.deleteUser(username);

    }

    @AfterClass
    public static void tearDownClass() {
        UtilIT.deleteUser(superuser);
    }

}
