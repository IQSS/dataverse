package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.api.datadeposit.SwordConfigurationImpl;
import java.util.List;
import java.util.logging.Logger;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

public class SwordIT {

    private static final Logger logger = Logger.getLogger(SwordIT.class.getCanonicalName());
    private static String username1;
    private static String apiToken1;
    private static String dataverseAlias1;
    private static String dataverseAlias2;
    private static String dataverseAlias3;
    private static String datasetPersistentId1;
    private static String datasetPersistentId2;
    private static String datasetPersistentId3;

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response createUser1 = UtilIT.createRandomUser();
//        createUser1.prettyPrint();
        username1 = UtilIT.getUsernameFromResponse(createUser1);
        apiToken1 = UtilIT.getApiTokenFromResponse(createUser1);
        Response makeSuperuser = UtilIT.makeSuperUser(username1);
        makeSuperuser.prettyPrint();
    }

    @Test
    public void testServiceDocument() {

        Response serviceDocumentResponse = UtilIT.getServiceDocument(apiToken1);
        serviceDocumentResponse.prettyPrint();

        SwordConfigurationImpl swordConfiguration = new SwordConfigurationImpl();

        serviceDocumentResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("service.version", equalTo(swordConfiguration.generatorVersion()));
    }

    @Test
    public void testServiceDocumentWithInvalidApiToken() {

        Response serviceDocumentResponse = UtilIT.getServiceDocument("invalidApiToken");
//        serviceDocumentResponse.prettyPrint();

        serviceDocumentResponse.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    public void testCreateDatasetUploadFileDownloadFileEditTitle() {

        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken1);
        createDataverse1Response.prettyPrint();
        dataverseAlias1 = UtilIT.getAliasFromResponse(createDataverse1Response);
        assertEquals(CREATED.getStatusCode(), createDataverse1Response.getStatusCode());

        String initialDatasetTitle = "My Working Title";
        /**
         * "Clients MUST NOT require a Collection Feed Document for deposit
         * operation." -- 6.2 Listing Collections
         * http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#protocoloperations_listingcollections
         */
        Response createDataset1Response = UtilIT.createDatasetViaSwordApi(dataverseAlias1, initialDatasetTitle, apiToken1);
        createDataset1Response.prettyPrint();
        assertEquals(CREATED.getStatusCode(), createDataset1Response.getStatusCode());
        datasetPersistentId1 = UtilIT.getDatasetPersistentIdFromResponse(createDataset1Response);
        logger.info("persistent id: " + datasetPersistentId1);

        Response atomEntry = UtilIT.getSwordAtomEntry(datasetPersistentId1, apiToken1);
        atomEntry.prettyPrint();
        atomEntry.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("entry.treatment", equalTo("no treatment information available"));

        Response listDatasetsResponse = UtilIT.listDatasetsViaSword(dataverseAlias1, apiToken1);
        listDatasetsResponse.prettyPrint();
        listDatasetsResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("feed.dataverseHasBeenReleased", equalTo("false"))
                .body("feed.entry[0].title", equalTo(initialDatasetTitle));

        Response uploadFile1 = UtilIT.uploadRandomFile(datasetPersistentId1, apiToken1);
        uploadFile1.prettyPrint();
        assertEquals(CREATED.getStatusCode(), uploadFile1.getStatusCode());

        Response swordStatement = UtilIT.getSwordStatement(datasetPersistentId1, apiToken1);
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
        assertEquals(FORBIDDEN.getStatusCode(), attemptToDownloadUnpublishedFileWithoutApiToken.getStatusCode());

        Response downloadUnpublishedFileWithValidApiToken = UtilIT.downloadFile(fileId, apiToken1);
        assertEquals(OK.getStatusCode(), downloadUnpublishedFileWithValidApiToken.getStatusCode());
        logger.info("downloaded " + downloadUnpublishedFileWithValidApiToken.getContentType() + " (" + downloadUnpublishedFileWithValidApiToken.asByteArray().length + " bytes)");

        Response deleteFile = UtilIT.deleteFile(fileId, apiToken1);
        deleteFile.prettyPrint();
        deleteFile.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());

        Response downloadDeletedFileWithValidApiToken = UtilIT.downloadFile(fileId, apiToken1);
        assertEquals(NOT_FOUND.getStatusCode(), downloadDeletedFileWithValidApiToken.getStatusCode());

        String newTitle = "My Awesome Dataset";
        Response updatedMetadataResponse = UtilIT.updateDatasetTitleViaSword(datasetPersistentId1, newTitle, apiToken1);
        updatedMetadataResponse.prettyPrint();
        swordStatement = UtilIT.getSwordStatement(datasetPersistentId1, apiToken1);
        title = UtilIT.getTitleFromSwordStatementResponse(swordStatement);
        assertEquals(newTitle, title);
        logger.info("Title updated from \"" + initialDatasetTitle + "\" to \"" + newTitle + "\".");

        Response deleteDatasetResponse = UtilIT.deleteLatestDatasetVersionViaSwordApi(datasetPersistentId1, apiToken1);
        deleteDatasetResponse.prettyPrint();
        assertEquals(204, deleteDatasetResponse.getStatusCode());

        Response deleteDataverse1Response = UtilIT.deleteDataverse(dataverseAlias1, apiToken1);
        deleteDataverse1Response.prettyPrint();
        assertEquals(200, deleteDataverse1Response.getStatusCode());

    }

    @Test
    public void testCreateDatasetPublishDestroy() {

        Response createDataverse = UtilIT.createRandomDataverse(apiToken1);
        createDataverse.prettyPrint();
        dataverseAlias2 = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaSwordApi(dataverseAlias2, apiToken1);
        createDataset.prettyPrint();
        datasetPersistentId2 = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        Response attemptToPublishDatasetInUnpublishedDataverse = UtilIT.publishDatasetViaSword(datasetPersistentId2, apiToken1);
        attemptToPublishDatasetInUnpublishedDataverse.prettyPrint();
        attemptToPublishDatasetInUnpublishedDataverse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

        String rootDataverseAlias = "root";
        Response publishRootDataverse = UtilIT.publishDataverseViaSword(rootDataverseAlias, apiToken1);
        publishRootDataverse.prettyPrint();

        Response listDatasetsResponse = UtilIT.listDatasetsViaSword(rootDataverseAlias, apiToken1);
        listDatasetsResponse.prettyPrint();
        listDatasetsResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("feed.dataverseHasBeenReleased", equalTo("true"));

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias2, apiToken1);
        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaSword(datasetPersistentId2, apiToken1);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response attemptToDeletePublishedDataset = UtilIT.deleteLatestDatasetVersionViaSwordApi(datasetPersistentId2, apiToken1);
        attemptToDeletePublishedDataset.prettyPrint();
        attemptToDeletePublishedDataset.then().assertThat()
                .statusCode(METHOD_NOT_ALLOWED.getStatusCode());

        Response reindexDatasetToFindDatabaseId = UtilIT.reindexDataset(datasetPersistentId2);
        reindexDatasetToFindDatabaseId.prettyPrint();
        reindexDatasetToFindDatabaseId.then().assertThat()
                .statusCode(OK.getStatusCode());

        Integer datasetId2 = JsonPath.from(reindexDatasetToFindDatabaseId.asString()).getInt("data.id");

        /**
         * @todo The "destroy" endpoint should accept a persistentId:
         * https://github.com/IQSS/dataverse/issues/1837
         */
        Response destroyDataset = UtilIT.destroyDataset(datasetId2, apiToken1);
        destroyDataset.prettyPrint();
        destroyDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response atomEntryDestroyed = UtilIT.getSwordAtomEntry(datasetPersistentId2, apiToken1);
        atomEntryDestroyed.prettyPrint();
        atomEntryDestroyed.then().statusCode(400);

        Response deleteDataverse2Response = UtilIT.deleteDataverse(dataverseAlias2, apiToken1);
        deleteDataverse2Response.prettyPrint();
        assertEquals(200, deleteDataverse2Response.getStatusCode());

    }

    /**
     * Test the following issues:
     *
     * - https://github.com/IQSS/dataverse/issues/1784
     *
     * - https://github.com/IQSS/dataverse/issues/2222
     */
    @Test
    public void testDeleteFiles() {

        Response createDataverse = UtilIT.createRandomDataverse(apiToken1);
        createDataverse.prettyPrint();
        dataverseAlias3 = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaSwordApi(dataverseAlias3, apiToken1);
        createDataset.prettyPrint();
        datasetPersistentId3 = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        Response uploadZip = UtilIT.uploadFile(datasetPersistentId3, "3files.zip", apiToken1);
        uploadZip.prettyPrint();
        assertEquals(CREATED.getStatusCode(), uploadZip.getStatusCode());
        Response statement1 = UtilIT.getSwordStatement(datasetPersistentId3, apiToken1);
        statement1.prettyPrint();
        String index0a = statement1.getBody().xmlPath().get("feed.entry[0].id").toString().split("/")[10];
        String index1a = statement1.getBody().xmlPath().get("feed.entry[1].id").toString().split("/")[10];
        String index2a = statement1.getBody().xmlPath().get("feed.entry[2].id").toString().split("/")[10];

        List<String> fileList = statement1.getBody().xmlPath().getList("feed.entry.id");
        logger.info("Dataset contains file ids: " + index0a + " " + index1a + " " + index2a + " (" + fileList.size() + ") files");

        Response deleteIndex0a = UtilIT.deleteFile(Integer.parseInt(index0a), apiToken1);
//        deleteIndex0a.prettyPrint();
        deleteIndex0a.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());
        logger.info("Deleted file id " + index0a + " from draft of unpublished dataset.");

        Response statement2 = UtilIT.getSwordStatement(datasetPersistentId3, apiToken1);
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

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias3, apiToken1);
//        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        logger.info("dataset has not yet been published:");
        Response atomEntryUnpublished = UtilIT.getSwordAtomEntry(datasetPersistentId3, apiToken1);
        atomEntryUnpublished.prettyPrint();

        Response publishDataset = UtilIT.publishDatasetViaSword(datasetPersistentId3, apiToken1);
//        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        logger.info("dataset has been published:");
        Response atomEntryPublishedV1 = UtilIT.getSwordAtomEntry(datasetPersistentId3, apiToken1);
        atomEntryPublishedV1.prettyPrint();

        Response deleteIndex0b = UtilIT.deleteFile(Integer.parseInt(index0b), apiToken1);
//        deleteIndex0b.prettyPrint();
        deleteIndex0b.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());
        logger.info("Deleted file id " + index0b + " from published dataset (should create draft).");
        Response statement3 = UtilIT.getSwordStatement(datasetPersistentId3, apiToken1);
        statement3.prettyPrint();

        logger.info("draft created from published dataset because a file was deleted:");
        Response atomEntryDraftV2 = UtilIT.getSwordAtomEntry(datasetPersistentId3, apiToken1);
        atomEntryDraftV2.prettyPrint();
        String citation = atomEntryDraftV2.body().xmlPath().getString("entry.bibliographicCitation");
        logger.info("citation (should contain 'DRAFT'): " + citation);
        boolean draftStringFoundInCitation = citation.matches(".*DRAFT.*");
        assertEquals(true, draftStringFoundInCitation);

        List<String> oneFileLeftInV2Draft = statement3.getBody().xmlPath().getList("feed.entry.id");
        logger.info("Number of files remaining in this post version 1 draft:" + oneFileLeftInV2Draft.size());
        assertEquals(1, oneFileLeftInV2Draft.size());

        Response deleteIndex1b = UtilIT.deleteFile(Integer.parseInt(index1b), apiToken1);
        deleteIndex1b.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());
        logger.info("Deleted file id " + index1b + " from draft version of a published dataset.");

        Response statement4 = UtilIT.getSwordStatement(datasetPersistentId3, apiToken1);
        statement4.prettyPrint();

        List<String> fileListEmpty = statement4.getBody().xmlPath().getList("feed.entry.id");
        logger.info("Number of files remaining:" + fileListEmpty.size());
        assertEquals(0, fileListEmpty.size());

        Response deleteDatasetDraft = UtilIT.deleteLatestDatasetVersionViaSwordApi(datasetPersistentId3, apiToken1);
        deleteDatasetDraft.prettyPrint();

        Response statement5 = UtilIT.getSwordStatement(datasetPersistentId3, apiToken1);
        statement5.prettyPrint();
        List<String> twoFilesinV1published = statement5.getBody().xmlPath().getList("feed.entry.id");
        logger.info("Number of files in V1 (draft has been deleted)" + twoFilesinV1published.size());
        assertEquals(2, twoFilesinV1published.size());

        /**
         * @todo The "destroy" endpoint should accept a persistentId:
         * https://github.com/IQSS/dataverse/issues/1837
         */
        Response reindexDatasetToFindDatabaseId = UtilIT.reindexDataset(datasetPersistentId3);
        reindexDatasetToFindDatabaseId.prettyPrint();
        reindexDatasetToFindDatabaseId.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer datasetId3 = JsonPath.from(reindexDatasetToFindDatabaseId.asString()).getInt("data.id");
        Response destroyDataset = UtilIT.destroyDataset(datasetId3, apiToken1);
        destroyDataset.prettyPrint();
        destroyDataset.then().assertThat()
                .statusCode(OK.getStatusCode());
        logger.info("Dataset has been destroyed: " + datasetPersistentId3 + " id " + datasetId3 + " but let's double check we can't access it...");

        Response atomEntryDestroyed = UtilIT.getSwordAtomEntry(datasetPersistentId3, apiToken1);
        atomEntryDestroyed.prettyPrint();
        atomEntryDestroyed.then().statusCode(400);

        Response deleteDataverse3Response = UtilIT.deleteDataverse(dataverseAlias3, apiToken1);
        deleteDataverse3Response.prettyPrint();
        assertEquals(200, deleteDataverse3Response.getStatusCode());

    }

    @AfterClass
    public static void tearDownClass() {
        boolean disabled = false;

        if (disabled) {
            return;
        }

        Response deleteUser1Response = UtilIT.deleteUser(username1);
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

    }

}
