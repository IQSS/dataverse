package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import javax.json.JsonArray;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import org.hamcrest.CoreMatchers;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import static junit.framework.Assert.assertEquals;
import static java.lang.Thread.sleep;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import org.hamcrest.Matchers;
import org.junit.After;

public class SearchIT {

    private static final Logger logger = Logger.getLogger(SearchIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {

        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response makeSureTokenlessSearchIsEnabled = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiRequiresToken);
        makeSureTokenlessSearchIsEnabled.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response removeSearchApiNonPublicAllowed = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        removeSearchApiNonPublicAllowed.prettyPrint();
        removeSearchApiNonPublicAllowed.then().assertThat()
                .statusCode(200);

        Response remove = UtilIT.deleteSetting(SettingsServiceBean.Key.ThumbnailSizeLimitImage);
        remove.then().assertThat()
                .statusCode(200);

    }

    @Test
    public void testSearchPermisions() throws InterruptedException {
        Response createUser1 = UtilIT.createRandomUser();
        String username1 = UtilIT.getUsernameFromResponse(createUser1);
        String apiToken1 = UtilIT.getApiTokenFromResponse(createUser1);

        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken1);
        createDataverse1Response.prettyPrint();
        assertEquals(201, createDataverse1Response.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse1Response);

        Response createDataset1Response = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken1);
        createDataset1Response.prettyPrint();
        createDataset1Response.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId1 = UtilIT.getDatasetIdFromResponse(createDataset1Response);

        Response enableNonPublicSearch = UtilIT.enableSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        enableNonPublicSearch.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response shouldBeVisibleToUser1 = UtilIT.search("id:dataset_" + datasetId1 + "_draft", apiToken1);
        shouldBeVisibleToUser1.prettyPrint();
        shouldBeVisibleToUser1.then().assertThat()
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].name", CoreMatchers.is("Darwin's Finches"))
                .statusCode(OK.getStatusCode());

        Response createUser2 = UtilIT.createRandomUser();
        String username2 = UtilIT.getUsernameFromResponse(createUser2);
        String apiToken2 = UtilIT.getApiTokenFromResponse(createUser2);

        Response shouldNotBeVisibleToUser2 = UtilIT.search("id:dataset_" + datasetId1 + "_draft", apiToken2);
        shouldNotBeVisibleToUser2.prettyPrint();
        shouldNotBeVisibleToUser2.then().assertThat()
                .body("data.total_count", CoreMatchers.is(0))
                .statusCode(OK.getStatusCode());

        String nullToken = null;

        Response shouldNotBeVisibleToTokenLess = UtilIT.search("id:dataset_" + datasetId1 + "_draft", nullToken);
        shouldNotBeVisibleToTokenLess.prettyPrint();
        shouldNotBeVisibleToTokenLess.then().assertThat()
                .body("data.total_count", CoreMatchers.is(0))
                .statusCode(OK.getStatusCode());

        String roleToAssign = "admin";

        Response grantUser2AccessOnDataset = UtilIT.grantRoleOnDataverse(dataverseAlias, roleToAssign, "@" + username2, apiToken1);
        grantUser2AccessOnDataset.prettyPrint();
        assertEquals(200, grantUser2AccessOnDataset.getStatusCode());
        sleep(500l);

        Response shouldBeVisibleToUser2 = UtilIT.search("id:dataset_" + datasetId1 + "_draft", apiToken2);
        shouldBeVisibleToUser2.prettyPrint();
        shouldBeVisibleToUser2.then().assertThat()
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].name", CoreMatchers.is("Darwin's Finches"))
                .statusCode(OK.getStatusCode());

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken1);
        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId1, "major", apiToken1);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response disableNonPublicSearch = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        disableNonPublicSearch.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response makeSureTokenlessSearchIsEnabled = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiRequiresToken);
        makeSureTokenlessSearchIsEnabled.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishedPublicDataShouldBeVisibleToTokenless = UtilIT.search("id:dataset_" + datasetId1, nullToken);
        publishedPublicDataShouldBeVisibleToTokenless.prettyPrint();
        publishedPublicDataShouldBeVisibleToTokenless.then().assertThat()
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].name", CoreMatchers.is("Darwin's Finches"))
                .statusCode(OK.getStatusCode());

        Response disableTokenlessSearch = UtilIT.setSetting(SettingsServiceBean.Key.SearchApiRequiresToken, "true");
        disableTokenlessSearch.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response dataverse47behaviorOfTokensBeingRequired = UtilIT.search("id:dataset_" + datasetId1, nullToken);
        dataverse47behaviorOfTokensBeingRequired.prettyPrint();
        dataverse47behaviorOfTokensBeingRequired.then().assertThat()
                .body("message", CoreMatchers.equalTo("Please provide a key query parameter (?key=XXX) or via the HTTP header X-Dataverse-key"))
                .statusCode(UNAUTHORIZED.getStatusCode());

        Response reEnableTokenlessSearch = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiRequiresToken);
        reEnableTokenlessSearch.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testSearchCitation() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        Response enableNonPublicSearch = UtilIT.enableSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        enableNonPublicSearch.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response searchResponse = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken);
        searchResponse.prettyPrint();
        assertFalse(searchResponse.body().jsonPath().getString("data.items[0].citation").contains("href"));
        assertTrue(searchResponse.body().jsonPath().getString("data.items[0].citationHtml").contains("href"));
        searchResponse.then().assertThat()
                .body("data.items[0].citation", CoreMatchers.not(Matchers.containsString("href")))
                .body("data.items[0].citationHtml", Matchers.containsString("href"))
                .statusCode(200);

        Response deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        deleteDatasetResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        deleteDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response disableNonPublicSearch = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        disableNonPublicSearch.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

    }

    @Test
    public void testDatasetThumbnail() {
        logger.info("BEGIN testDatasetThumbnail");

//        Response setSearchApiNonPublicAllowed = UtilIT.setSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed, "true");
//        setSearchApiNonPublicAllowed.prettyPrint();
//
//        assertEquals("foo", "foo");
//        if (true) {
//            return;
//        }
//
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        Response setSearchApiNonPublicAllowed = UtilIT.setSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed, "true");
        setSearchApiNonPublicAllowed.prettyPrint();
        setSearchApiNonPublicAllowed.then().assertThat()
                .statusCode(200);

        Response search1 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken);
        search1.prettyPrint();
        search1.then().assertThat()
                .body("data.items[0].name", CoreMatchers.equalTo("Darwin's Finches"))
                .body("data.items[0].thumbnailFilename", CoreMatchers.equalTo(null))
                .body("data.items[0].datasetThumbnailBase64image", CoreMatchers.equalTo(null))
                .statusCode(200);

        Response datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetAsJson.prettyPrint();
        String protocol = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;
        long datasetVersionId = JsonPath.from(datasetAsJson.getBody().asString()).getLong("data.latestVersion.id");

        Response createNoSpecialAccessUser = UtilIT.createRandomUser();
        createNoSpecialAccessUser.prettyPrint();
        String noSpecialAccessUsername = UtilIT.getUsernameFromResponse(createNoSpecialAccessUser);
        String noSpecialAcessApiToken = UtilIT.getApiTokenFromResponse(createNoSpecialAccessUser);

        logger.info("Dataset created, no thumbnail expected:");
        Response getThumbnail1 = UtilIT.getDatasetThumbnailMetadata(datasetId, apiToken);
        getThumbnail1.prettyPrint();
        JsonObject emptyObject = Json.createObjectBuilder().build();
        getThumbnail1.then().assertThat()
                //                .body("data", CoreMatchers.equalTo(emptyObject))
                .body("data.isUseGenericThumbnail", CoreMatchers.equalTo(false))
                .body("data.dataFileId", CoreMatchers.equalTo(null))
                .body("data.datasetLogoPresent", CoreMatchers.equalTo(false))
                .statusCode(200);

        String thumbnailUrl = RestAssured.baseURI + "/api/datasets/" + datasetId + "/thumbnail";
        InputStream inputStream1creator = UtilIT.getInputStreamFromUnirest(thumbnailUrl, apiToken);
        assertNull(inputStream1creator);

        InputStream inputStream1guest = UtilIT.getInputStreamFromUnirest(thumbnailUrl, noSpecialAcessApiToken);
        assertNull(inputStream1guest);

        Response getThumbnailImage1 = UtilIT.getDatasetThumbnail(datasetPersistentId, apiToken); //
        getThumbnailImage1.prettyPrint();
        getThumbnailImage1.then().assertThat()
                .contentType("")
                .statusCode(NO_CONTENT.getStatusCode());

        Response attemptToGetThumbnailCandidates = UtilIT.showDatasetThumbnailCandidates(datasetPersistentId, noSpecialAcessApiToken);
        attemptToGetThumbnailCandidates.prettyPrint();
        attemptToGetThumbnailCandidates.then().assertThat()
                .body("message", CoreMatchers.equalTo("You are not permitted to list dataset thumbnail candidates."))
                .statusCode(FORBIDDEN.getStatusCode());

        Response thumbnailCandidates1 = UtilIT.showDatasetThumbnailCandidates(datasetPersistentId, apiToken);
        thumbnailCandidates1.prettyPrint();
        JsonArray emptyArray = Json.createArrayBuilder().build();
        thumbnailCandidates1.then().assertThat()
                .body("data", CoreMatchers.equalTo(emptyArray))
                .statusCode(200);

        Response getThumbnailImageNoAccess1 = UtilIT.getDatasetThumbnail(datasetPersistentId, noSpecialAcessApiToken);
        getThumbnailImageNoAccess1.prettyPrint();
        getThumbnailImageNoAccess1.then().assertThat()
                .contentType("")
                .statusCode(NO_CONTENT.getStatusCode());

        Response uploadFile = UtilIT.uploadFile(datasetPersistentId, "trees.zip", apiToken);
        uploadFile.prettyPrint();

        Response getDatasetJson1 = UtilIT.nativeGetUsingPersistentId(datasetPersistentId, apiToken);
        Long dataFileId1 = JsonPath.from(getDatasetJson1.getBody().asString()).getLong("data.latestVersion.files[0].dataFile.id");
        System.out.println("datafileId: " + dataFileId1);
        getDatasetJson1.then().assertThat()
                .statusCode(200);

        logger.info("DataFile uploaded, should automatically become the thumbnail:");

        File trees = new File("scripts/search/data/binary/trees.png");
        String treesAsBase64 = null;
        treesAsBase64 = ImageThumbConverter.generateImageThumbnailFromFileAsBase64(trees, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);

        if (treesAsBase64 == null) {
            Logger.getLogger(SearchIT.class.getName()).log(Level.SEVERE, "Failed to generate a base64 thumbnail from the file trees.png");
        }

        Response search2 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken);
        search2.prettyPrint();
        search2.then().assertThat()
                .body("data.items[0].name", CoreMatchers.equalTo("Darwin's Finches"))
                .statusCode(200);

        //Unpublished datafiles no longer populate the dataset thumbnail
        Response getThumbnail2 = UtilIT.getDatasetThumbnailMetadata(datasetId, apiToken);
        System.out.println("getThumbnail2: ");
        getThumbnail2.prettyPrint();
        getThumbnail2.then().assertThat()
                //                .body("data.datasetThumbnail", CoreMatchers.equalTo("randomFromDataFile" + dataFileId1))
                .body("data.datasetThumbnailBase64image", CoreMatchers.equalTo(null))
                .body("data.isUseGenericThumbnail", CoreMatchers.equalTo(false))
                .body("data.dataFileId", CoreMatchers.equalTo(null))
                .body("data.datasetLogoPresent", CoreMatchers.equalTo(false))
                .statusCode(200);
        
        //We now need to publish for the dataset to get the thumbnail
        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response getThumbnail3 = UtilIT.getDatasetThumbnailMetadata(datasetId, apiToken);
        System.out.println("getThumbnail3: ");
        getThumbnail3.prettyPrint();
        getThumbnail3.then().assertThat()
                //                .body("data.datasetThumbnail", CoreMatchers.equalTo("randomFromDataFile" + dataFileId1))
                .body("data.datasetThumbnailBase64image", CoreMatchers.equalTo(treesAsBase64))
                .body("data.isUseGenericThumbnail", CoreMatchers.equalTo(false))
                // This dataFileId is null because of automatic thumbnail selection.
                .body("data.dataFileId", CoreMatchers.equalTo(dataFileId1.toString()))
                .body("data.datasetLogoPresent", CoreMatchers.equalTo(false))
                .statusCode(200);

        InputStream inputStream2creator = UtilIT.getInputStreamFromUnirest(thumbnailUrl, apiToken);
        assertNotNull(inputStream2creator);
        assertEquals(treesAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream2creator));

        InputStream inputStream2guest = UtilIT.getInputStreamFromUnirest(thumbnailUrl, noSpecialAcessApiToken);
        assertEquals(treesAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream2guest));

        String leadingStringToRemove = FileUtil.DATA_URI_SCHEME;
        System.out.println("before: " + treesAsBase64);
        String encodedImg = treesAsBase64.substring(leadingStringToRemove.length());
        System.out.println("after: " + encodedImg);
        byte[] decodedImg = null;
        try {

            decodedImg = Base64.getDecoder().decode(encodedImg.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
        }

        Response getThumbnailImage2 = UtilIT.getDatasetThumbnail(datasetPersistentId, apiToken);
        getThumbnailImage2.prettyPrint();
        getThumbnailImage2.then().assertThat()
                //                .body(CoreMatchers.equalTo(decodedImg))
                .contentType("image/png")
                /**
                 * @todo Why can't we assert the content here? Why do we have to
                 * use Unirest instead? How do you download the bytes of the
                 * image using REST Assured?
                 */
                //                .content(CoreMatchers.equalTo(decodedImg))
                .statusCode(200);

        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response uploadSecondImage = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadSecondImage.prettyPrint();
        uploadSecondImage.then().assertThat()
                .statusCode(200);

        Response getDatasetJson2 = UtilIT.nativeGetUsingPersistentId(datasetPersistentId, apiToken);
        //odd that [0] gets the second uploaded file... replace with a find for dataverseproject.png
        Long dataFileId2 = JsonPath.from(getDatasetJson2.getBody().asString()).getLong("data.latestVersion.files[0].dataFile.id");
        System.out.println("datafileId2: " + dataFileId2);
        getDatasetJson2.then().assertThat()
                .statusCode(200);

        File dataverseProjectLogo = new File(pathToFile);
        String dataverseProjectLogoAsBase64 = null;
        dataverseProjectLogoAsBase64 = ImageThumbConverter.generateImageThumbnailFromFileAsBase64(dataverseProjectLogo, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);

        if (dataverseProjectLogoAsBase64 == null) {
            Logger.getLogger(SearchIT.class.getName()).log(Level.SEVERE, "Failed to generate a base64 thumbnail from the file dataverseproject.png");
        }

        Response switchToSecondDataFileThumbnail = UtilIT.useThumbnailFromDataFile(datasetPersistentId, dataFileId2, apiToken);
        switchToSecondDataFileThumbnail.prettyPrint();
        switchToSecondDataFileThumbnail.then().assertThat()
                .body("data.message", CoreMatchers.equalTo("Thumbnail set to " + dataverseProjectLogoAsBase64))
                .statusCode(200);

        logger.info("Second DataFile has been uploaded and switched to as the thumbnail:");
        Response getThumbnail4 = UtilIT.getDatasetThumbnailMetadata(datasetId, apiToken);
        getThumbnail4.prettyPrint();
        getThumbnail4.then().assertThat()
                //                .body("data.datasetThumbnail", CoreMatchers.equalTo("dataverseproject.png"))
                .body("data.datasetThumbnailBase64image", CoreMatchers.equalTo(dataverseProjectLogoAsBase64))
                .body("data.isUseGenericThumbnail", CoreMatchers.equalTo(false))
                .body("data.dataFileId", CoreMatchers.equalTo(dataFileId2.toString()))
                .body("data.datasetLogoPresent", CoreMatchers.equalTo(false))
                .statusCode(200);

        InputStream inputStream3creator = UtilIT.getInputStreamFromUnirest(thumbnailUrl, apiToken);
        assertEquals(dataverseProjectLogoAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream3creator));

        InputStream inputStream3guest = UtilIT.getInputStreamFromUnirest(thumbnailUrl, noSpecialAcessApiToken);
        assertEquals(dataverseProjectLogoAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream3guest));

        Response search3 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken);
        search3.prettyPrint();
        search3.then().assertThat()
                .body("data.items[0].name", CoreMatchers.equalTo("Darwin's Finches"))
                .statusCode(200);

        Response thumbnailCandidates2 = UtilIT.showDatasetThumbnailCandidates(datasetPersistentId, apiToken);
        thumbnailCandidates2.prettyPrint();
        thumbnailCandidates2.then().assertThat()
                .body("data[0].base64image", CoreMatchers.equalTo(dataverseProjectLogoAsBase64))
                .body("data[0].dataFileId", CoreMatchers.equalTo(dataFileId2.intValue()))
                .body("data[1].base64image", CoreMatchers.equalTo(treesAsBase64))
                .body("data[1].dataFileId", CoreMatchers.equalTo(dataFileId1.intValue()))
                .statusCode(200);

        //Add Failing Test logo file too big
        //Size limit hardcoded in systemConfig.getUploadLogoSizeLimit
        String tooBigLogo = "src/test/resources/images/coffeeshop.png";
        Response overrideThumbnailFail = UtilIT.uploadDatasetLogo(datasetPersistentId, tooBigLogo, apiToken);

        overrideThumbnailFail.prettyPrint();
        overrideThumbnailFail.then().assertThat()
                .body("message", CoreMatchers.equalTo("File is larger than maximum size: 500000."))
                /**
                 * @todo We want this to expect 400 (BAD_REQUEST), not 403
                 * (FORBIDDEN).
                 */
                //                .statusCode(400);
                .statusCode(FORBIDDEN.getStatusCode());

        String datasetLogo = "src/main/webapp/resources/images/cc0.png";
        File datasetLogoFile = new File(datasetLogo);
        String datasetLogoAsBase64 = datasetLogoAsBase64 = ImageThumbConverter.generateImageThumbnailFromFileAsBase64(datasetLogoFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);

        if (datasetLogoAsBase64 == null) {
            Logger.getLogger(SearchIT.class.getName()).log(Level.SEVERE, "Failed to generate a base64 thumbnail from the file dataverseproject.png");
        }

        Response overrideThumbnail = UtilIT.uploadDatasetLogo(datasetPersistentId, datasetLogo, apiToken);
        overrideThumbnail.prettyPrint();
        overrideThumbnail.then().assertThat()
                .body("data.message", CoreMatchers.equalTo("Thumbnail is now " + datasetLogoAsBase64))
                .statusCode(200);

        logger.info("Dataset logo has been uploaded and becomes the thumbnail:");
        Response getThumbnail5 = UtilIT.getDatasetThumbnailMetadata(datasetId, apiToken);
        getThumbnail5.prettyPrint();
        getThumbnail5.then().assertThat()
                //                .body("data.datasetThumbnail", CoreMatchers.equalTo(null))
                .body("data.isUseGenericThumbnail", CoreMatchers.equalTo(false))
                .body("data.datasetThumbnailBase64image", CoreMatchers.equalTo(datasetLogoAsBase64))
                .body("data.datasetLogoPresent", CoreMatchers.equalTo(true))
                .statusCode(200);

        InputStream inputStream4creator = UtilIT.getInputStreamFromUnirest(thumbnailUrl, apiToken);
        assertEquals(datasetLogoAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream4creator));

        InputStream inputStream4guest = UtilIT.getInputStreamFromUnirest(thumbnailUrl, noSpecialAcessApiToken);
        assertEquals(datasetLogoAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream4guest));

        Response search4 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken);
        search4.prettyPrint();
        search4.then().assertThat()
                .body("data.items[0].name", CoreMatchers.equalTo("Darwin's Finches"))
                .statusCode(200);

        Response thumbnailCandidates3 = UtilIT.showDatasetThumbnailCandidates(datasetPersistentId, apiToken);
        thumbnailCandidates3.prettyPrint();
        logger.fine("datasetLogoAsBase64:          " + datasetLogoAsBase64);
        logger.fine("dataverseProjectLogoAsBase64: " + dataverseProjectLogoAsBase64);
        logger.fine("treesAsBase64:                " + treesAsBase64);
        thumbnailCandidates3.then().assertThat()
                .body("data[0].base64image", CoreMatchers.equalTo(datasetLogoAsBase64))
                .body("data[0].dataFileId", CoreMatchers.equalTo(null))
                .body("data[1].base64image", CoreMatchers.equalTo(dataverseProjectLogoAsBase64))
                .body("data[1].dataFileId", CoreMatchers.equalTo(dataFileId2.intValue()))
                .body("data[2].base64image", CoreMatchers.equalTo(treesAsBase64))
                .body("data[2].dataFileId", CoreMatchers.equalTo(dataFileId1.intValue()))
                .statusCode(200);

        Response deleteDatasetLogo = UtilIT.removeDatasetThumbnail(datasetPersistentId, apiToken);
        deleteDatasetLogo.prettyPrint();
        deleteDatasetLogo.then().assertThat()
                .body("data.message", CoreMatchers.equalTo("Dataset thumbnail removed."))
                .statusCode(200);

        logger.info("Deleting the dataset logo means that the thumbnail is not set. It should be the generic icon:");
        Response getThumbnail6 = UtilIT.getDatasetThumbnailMetadata(datasetId, apiToken);
        getThumbnail6.prettyPrint();
        getThumbnail6.then().assertThat()
                //                .body("data.datasetThumbnail", CoreMatchers.equalTo(null))
                .body("data.isUseGenericThumbnail", CoreMatchers.equalTo(true))
                .body("data.datasetLogoPresent", CoreMatchers.equalTo(false))
                .statusCode(200);

        InputStream inputStream5creator = UtilIT.getInputStreamFromUnirest(thumbnailUrl, apiToken);
        assertNull(inputStream5creator);

        InputStream inputStream5guest = UtilIT.getInputStreamFromUnirest(thumbnailUrl, noSpecialAcessApiToken);
        assertNull(inputStream5guest);

        Response search5 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken);
        search5.prettyPrint();
        search5.then().assertThat()
                .body("data.items[0].name", CoreMatchers.equalTo("Darwin's Finches"))
                .body("data.items[0].thumbnailFilename", CoreMatchers.equalTo(null))
                .body("data.items[0].datasetThumbnailBase64image", CoreMatchers.equalTo(null))
                .statusCode(200);

        Response thumbnailCandidates4 = UtilIT.showDatasetThumbnailCandidates(datasetPersistentId, apiToken);
        thumbnailCandidates4.prettyPrint();
        thumbnailCandidates4.then().assertThat()
                .body("data[0].base64image", CoreMatchers.equalTo(dataverseProjectLogoAsBase64))
                .body("data[0].dataFileId", CoreMatchers.equalTo(dataFileId2.intValue()))
                .body("data[1].base64image", CoreMatchers.equalTo(treesAsBase64))
                .body("data[1].dataFileId", CoreMatchers.equalTo(dataFileId1.intValue()))
                .statusCode(200);

        Response switchtoFirstDataFileThumbnail = UtilIT.useThumbnailFromDataFile(datasetPersistentId, dataFileId1, apiToken);
        switchtoFirstDataFileThumbnail.prettyPrint();
        switchtoFirstDataFileThumbnail.then().assertThat()
                .body("data.message", CoreMatchers.equalTo("Thumbnail set to " + treesAsBase64))
                .statusCode(200);

        Response getThumbnailImageNoSpecialAccess99 = UtilIT.getDatasetThumbnail(datasetPersistentId, noSpecialAcessApiToken);
//        getThumbnailImageNoSpecialAccess99.prettyPrint();
        getThumbnailImageNoSpecialAccess99.then().assertThat()
                .contentType("image/png")
                .statusCode(OK.getStatusCode());

        InputStream inputStream99creator = UtilIT.getInputStreamFromUnirest(thumbnailUrl, apiToken);
        assertEquals(treesAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream99creator));

        InputStream inputStream99guest = UtilIT.getInputStreamFromUnirest(thumbnailUrl, noSpecialAcessApiToken);
        assertEquals(treesAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream99guest));

        Response searchResponse = UtilIT.search("id:dataset_" + datasetId, noSpecialAcessApiToken);
        searchResponse.prettyPrint();
        searchResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response removeSearchApiNonPublicAllowed = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        removeSearchApiNonPublicAllowed.then().assertThat()
                .statusCode(200);

        /**
         * @todo What happens when you delete a dataset? Does the thumbnail
         * created based on the logo get deleted too? Should it?
         */
    }

    @Test
    public void testIdentifier() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        System.out.println("id: " + datasetId);
        String datasetPid = JsonPath.from(createDatasetResponse.getBody().asString()).getString("data.persistentId");
        System.out.println("datasetPid: " + datasetPid);

        Response datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());

        String identifier = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");
        System.out.println("identifier: " + identifier);

        String searchPart = identifier.replace("FK2/", "");
        Response searchUnpublished = UtilIT.search(searchPart, apiToken);
        searchUnpublished.prettyPrint();
        searchUnpublished.then().assertThat()
                .statusCode(OK.getStatusCode())
                // It's expected that you can't find it because it hasn't been published.
                .body("data.total_count", CoreMatchers.equalTo(0));

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken);
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        searchPart = identifier.replace("FK2/", "");
        Response searchTargeted = UtilIT.search("dsPersistentId:" + searchPart, apiToken);
        searchTargeted.prettyPrint();
        searchTargeted.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));

        Response searchUntargeted = UtilIT.search(searchPart, apiToken);
        searchUntargeted.prettyPrint();
        searchUntargeted.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));

    }

    @After
    public void tearDownDataverse() {
        File treesThumb = new File("scripts/search/data/binary/trees.png.thumb48");
        treesThumb.delete();
        File cc0Thumb = new File("src/main/webapp/resources/images/cc0.png.thumb48");
        cc0Thumb.delete();
        File dataverseprojectThumb = new File("src/main/webapp/resources/images/dataverseproject.png.thumb48");
        dataverseprojectThumb.delete();
    }

    @AfterClass
    public static void cleanup() {

        Response enableNonPublicSearch = UtilIT.enableSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        assertEquals(200, enableNonPublicSearch.getStatusCode());

        Response deleteSearchApiNonPublicAllowed = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        deleteSearchApiNonPublicAllowed.then().assertThat()
                .statusCode(200);

        Response getSearchApiNonPublicAllowed = UtilIT.getSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
//        getSearchApiNonPublicAllowed.prettyPrint();
        getSearchApiNonPublicAllowed.then().assertThat()
                .body("message", CoreMatchers.equalTo("Setting " + SettingsServiceBean.Key.SearchApiNonPublicAllowed + " not found"))
                .statusCode(404);
    }

}
