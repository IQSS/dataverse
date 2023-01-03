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
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import org.hamcrest.CoreMatchers;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import static junit.framework.Assert.assertEquals;
import static java.lang.Thread.sleep;
import javax.imageio.ImageIO;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import org.hamcrest.Matchers;
import org.junit.After;
import static org.junit.Assert.assertNotEquals;
import static java.lang.Thread.sleep;
import javax.json.JsonObjectBuilder;

public class SearchIT {

    private static final Logger logger = Logger.getLogger(SearchIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {

        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response makeSureTokenlessSearchIsEnabled = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiRequiresToken);
        makeSureTokenlessSearchIsEnabled.then().assertThat()
                .statusCode(OK.getStatusCode());

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

        String searchPart = "id:dataset_" + datasetId1 + "_draft";        
        assertTrue("Failed test if search exceeds max duration " + searchPart , UtilIT.sleepForSearch(searchPart, apiToken2, "", UtilIT.MAXIMUM_INGEST_LOCK_DURATION)); 
        
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

        Response publishedDataverseSearchableByAlias = UtilIT.search("dvAlias:" + dataverseAlias, nullToken);
        publishedDataverseSearchableByAlias.prettyPrint();
        publishedDataverseSearchableByAlias.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].name", CoreMatchers.is(dataverseAlias))
                .body("data.items[0].type", CoreMatchers.is("dataverse"))
                .body("data.items[0].identifier", CoreMatchers.is(dataverseAlias));

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

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

    }
    
    @Test
    public void testAdditionalDatasetContent6300() {

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

        Response datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());

        String identifier = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");

        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonBeforePublishing.prettyPrint();
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.authority");

        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;
        String pathToJsonFile = "doc/sphinx-guides/source/_static/api/dataset-add-metadata.json";
        Response addSubjectViaNative = UtilIT.addDatasetMetadataViaNative(datasetPersistentId, pathToJsonFile, apiToken);
        addSubjectViaNative.prettyPrint();
        addSubjectViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response searchResponse = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken);
        searchResponse.prettyPrint();
        /*["Astronomy and Astrophysics"]*/
        assertTrue(searchResponse.body().jsonPath().getString("data.items[0].subjects").contains("Astronomy and Astrophysics"));
        assertTrue(searchResponse.body().jsonPath().getString("data.items[0].versionState").equals("DRAFT"));
        /*                "versionState": "DRAFT",*/

        //We now need to publish to see version number
        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        searchResponse = UtilIT.search("id:dataset_" + datasetId, apiToken);
        searchResponse.prettyPrint();
        /*["Astronomy and Astrophysics"]*/
        assertTrue(searchResponse.body().jsonPath().getString("data.items[0].subjects").contains("Astronomy and Astrophysics"));
        assertTrue(searchResponse.body().jsonPath().getString("data.items[0].versionState").equals("RELEASED"));

        assertTrue(searchResponse.body().jsonPath().getString("data.items[0].majorVersion").equals("1"));
        assertTrue(searchResponse.body().jsonPath().getString("data.items[0].minorVersion").equals("0"));

        assertTrue(searchResponse.body().jsonPath().getString("data.items[0].authors").contains("Spruce, Sabrina"));

        assertTrue(searchResponse.body().jsonPath().getString("data.items[0].contacts[0].name").contains("Finch, Fiona"));
        assertTrue(searchResponse.body().jsonPath().getString("data.items[0].storageIdentifier").contains(identifier));

    }

    @Test
    public void testSearchDynamicMetadataFields() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        Response searchResponseAuthor = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken, "&metadata_fields=citation:author");
        searchResponseAuthor.prettyPrint();
        searchResponseAuthor.then().assertThat()
                .body("data.items[0].metadataBlocks.citation.displayName", CoreMatchers.equalTo("Citation Metadata"))
                .body("data.items[0].metadataBlocks.citation.fields[0].typeName", CoreMatchers.equalTo("author"))
                .body("data.items[0].metadataBlocks.citation.fields[0].value[0].authorName.value", CoreMatchers.equalTo("Finch, Fiona"))
                .body("data.items[0].metadataBlocks.citation.fields[0].value[0].authorAffiliation.value", CoreMatchers.equalTo("Birds Inc."))
                .statusCode(OK.getStatusCode());

        // "{field_name} could not be a sub field of a compound field
        Response subFieldsNotSupported = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken, "&metadata_fields=citation:authorAffiliation");
        subFieldsNotSupported.prettyPrint();
        subFieldsNotSupported.then().assertThat()
                .body("data.items[0].metadataBlocks.citation.displayName", CoreMatchers.equalTo("Citation Metadata"))
                // No fields returned. authorAffiliation is a subfield of author and not supported.
                .body("data.items[0].metadataBlocks.citation.fields", Matchers.empty())
                .statusCode(OK.getStatusCode());

        Response allFieldsFromCitation = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken, "&metadata_fields=citation:*");
        // Many more fields printed
        allFieldsFromCitation.prettyPrint();
        allFieldsFromCitation.then().assertThat()
                .body("data.items[0].metadataBlocks.citation.displayName", CoreMatchers.equalTo("Citation Metadata"))
                // Many fields returned, all of the citation block that has been filled in.
                .body("data.items[0].metadataBlocks.citation.fields.typeName.size", Matchers.equalTo(5))
                .statusCode(OK.getStatusCode());

    }
    
    
    /*
     * Note: this test does a lot of checking for permissions with / without privlidged api key.
     * Thumbnails access is the same with/without that access as of 4.9.4 --MAD
     * 
     * If permissions come into play for thumbnails, the deprecated UtilIT.getInputStreamFromUnirest
     * should be repaired to actually use api keys
     */
    @Test
    public void testDatasetThumbnail() {
        logger.info("BEGIN testDatasetThumbnail");

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
        
        File trees = new File("scripts/search/data/binary/trees.png");
        String treesAsBase64 = null;
        treesAsBase64 = ImageThumbConverter.generateImageThumbnailFromFileAsBase64(trees, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);

        if (treesAsBase64 == null) {
            Logger.getLogger(SearchIT.class.getName()).log(Level.SEVERE, "Failed to generate a base64 thumbnail from the file trees.png");
        }
        
        InputStream inputStream1creator = UtilIT.getInputStreamFromUnirest(thumbnailUrl, apiToken);
        assertNotEquals(treesAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream1creator));

        InputStream inputStream1guest = UtilIT.getInputStreamFromUnirest(thumbnailUrl, noSpecialAcessApiToken);
        assertNotEquals(treesAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream1guest));
 

        Response getThumbnailImage1 = UtilIT.getDatasetThumbnail(datasetPersistentId, apiToken); 
        getThumbnailImage1.prettyPrint();
        getThumbnailImage1.then().assertThat()
                .contentType("")
                .statusCode(NOT_FOUND.getStatusCode());

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
                .statusCode(NOT_FOUND.getStatusCode());

        Response uploadFile = UtilIT.uploadFile(datasetPersistentId, "trees.zip", apiToken);
        uploadFile.prettyPrint();

        Response getDatasetJson1 = UtilIT.nativeGetUsingPersistentId(datasetPersistentId, apiToken);
        Long dataFileId1 = JsonPath.from(getDatasetJson1.getBody().asString()).getLong("data.latestVersion.files[0].dataFile.id");
        System.out.println("datafileId: " + dataFileId1);
        getDatasetJson1.then().assertThat()
                .statusCode(200);

        logger.info("DataFile uploaded, should automatically become the thumbnail:");

        Response search2 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken);
        search2.prettyPrint();
        search2.then().assertThat()
                .body("data.items[0].name", CoreMatchers.equalTo("Darwin's Finches"))
                .body("data.items[0].fileCount", CoreMatchers.equalTo(1))
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

        
        Response getThumbnailImageA = UtilIT.getDatasetThumbnail(datasetPersistentId, apiToken); //
        getThumbnailImageA.prettyPrint();
        getThumbnailImageA.then().assertThat()
                .contentType("image/png")
                .statusCode(OK.getStatusCode());

        String trueOrWidthInPixels = "true";
        Response getFileThumbnailImageA = UtilIT.getFileThumbnail(dataFileId1.toString(), trueOrWidthInPixels, apiToken);
        getFileThumbnailImageA.then().assertThat()
                .contentType("image/png")
                .statusCode(OK.getStatusCode());

        try {
            BufferedImage bufferedImage = ImageIO.read(getFileThumbnailImageA.body().asInputStream());
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            System.out.println("width: " + width);
            System.out.println("height: " + height);
            int expectedWidth = 64;
            assertEquals(expectedWidth, width);
        } catch (IOException ex) {
            Logger.getLogger(SearchIT.class.getName()).log(Level.SEVERE, null, ex);
        }

        InputStream inputStream2creator = UtilIT.getInputStreamFromUnirest(thumbnailUrl, apiToken);
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
        assertNotEquals(treesAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream5creator));

        InputStream inputStream5guest = UtilIT.getInputStreamFromUnirest(thumbnailUrl, noSpecialAcessApiToken);
        assertNotEquals(treesAsBase64, UtilIT.inputStreamToDataUrlSchemeBase64Png(inputStream5guest));
        
        Response getThumbnailImageB = UtilIT.getDatasetThumbnail(datasetPersistentId, apiToken); //
        getThumbnailImageB.prettyPrint();
        getThumbnailImageB.then().assertThat()
                .contentType("")
                .statusCode(NOT_FOUND.getStatusCode());

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
                .body("data.total_count", CoreMatchers.equalTo(1));

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

    @Test
    public void testNestedSubtree() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        
        //(String alias, String category, String apiToken, String parentDV)
        Response createDataverseResponse2 = UtilIT.createSubDataverse("subDV" + UtilIT.getRandomIdentifier(), null, apiToken, dataverseAlias);
        createDataverseResponse2.prettyPrint();
        String dataverseAlias2 = UtilIT.getAliasFromResponse(createDataverseResponse2);

        String searchPart = "*"; 

        Response searchUnpublishedSubtree = UtilIT.search(searchPart, apiToken, "&subtree="+dataverseAlias);
        searchUnpublishedSubtree.prettyPrint();
        searchUnpublishedSubtree.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
        
        Response searchUnpublishedSubtree2 = UtilIT.search(searchPart, apiToken, "&subtree="+dataverseAlias2);
        searchUnpublishedSubtree2.prettyPrint();
        searchUnpublishedSubtree2.then().assertThat()
                .statusCode(OK.getStatusCode())
                // TODO: investigate if this is a bug that nothing was found.
                .body("data.total_count", CoreMatchers.equalTo(0));

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            /**
             * This sleep is here because dataverseAlias2 is showing with
             * discoverableBy of group_public first and then overwritten by
             * group_user224 (or whatever) later. This is backward from what we
             * expect. We expect group_user224 to be in discoverableBy first
             * while the dataverse is unpublished (only that users can see it)
             * and then we want discoverableBy to change to group_public when
             * the dataverse is published (everyone can see it).
             *
             * The theory on this bug, this timing issue, is that the indexing
             * from "create" is being queued up and happens after the indexing
             * from "publish".
             *
             * Please note that if you remove this sleep and run SearchIT in
             * isolation, it passes. To exercise this bug you have to run
             * multiple API tests at once such as SearchIT and DatasetsIT.
             */
        }

        Response publishDataverse2 = UtilIT.publishDataverseViaNativeApi(dataverseAlias2, apiToken);
        publishDataverse2.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response searchPublishedSubtree = UtilIT.search(searchPart, apiToken, "&subtree="+dataverseAlias);
        searchPublishedSubtree.prettyPrint();
        searchPublishedSubtree.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
        
        Response searchPublishedSubtree2 = UtilIT.search(searchPart, apiToken, "&subtree="+dataverseAlias2);
        searchPublishedSubtree2.prettyPrint();
        searchPublishedSubtree2.then().assertThat()
                .statusCode(OK.getStatusCode())
                // TODO: investigate if this is a bug that nothing was found.
                .body("data.total_count", CoreMatchers.equalTo(0));
        
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias2, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        System.out.println("id: " + datasetId);
        String datasetPid = JsonPath.from(createDatasetResponse.getBody().asString()).getString("data.persistentId");
        System.out.println("datasetPid: " + datasetPid);
        
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken);
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response searchPublishedSubtreeWDS = UtilIT.search(searchPart, apiToken, "&subtree="+dataverseAlias);
        searchPublishedSubtreeWDS.prettyPrint();
        searchPublishedSubtreeWDS.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(2));
        
        Response searchPublishedSubtreeWDS2 = UtilIT.search(searchPart, apiToken, "&subtree="+dataverseAlias2);
        searchPublishedSubtreeWDS2.prettyPrint();
        searchPublishedSubtreeWDS2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
                
    }
    
    //If this test fails it'll fail inconsistently as it tests underlying async role code
    //Hopefully it will not fail as we fixed the issue in https://github.com/IQSS/dataverse/issues/3471
    @Test
    public void testCuratorCardDataversePopulation() throws InterruptedException {

        Response createSuperUser = UtilIT.createRandomUser();
        createSuperUser.prettyPrint();
        assertEquals(200, createSuperUser.getStatusCode());
        String usernameSuper = UtilIT.getUsernameFromResponse(createSuperUser);
        String apiTokenSuper = UtilIT.getApiTokenFromResponse(createSuperUser);
        Response makeSuperUser = UtilIT.makeSuperUser(usernameSuper);
        assertEquals(200, makeSuperUser.getStatusCode());
        
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String parentDataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        
        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(parentDataverseAlias, apiToken);
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        String subDataverseAlias = "dv" + UtilIT.getRandomIdentifier();
        Response createSubDataverseResponse = UtilIT.createSubDataverse(subDataverseAlias, null, apiTokenSuper, parentDataverseAlias);
        createSubDataverseResponse.prettyPrint();
        //UtilIT.getAliasFromResponse(createSubDataverseResponse);
        
        Response grantRoleOnDataverseResponse = UtilIT.grantRoleOnDataverse(subDataverseAlias, "curator", "@" + username, apiTokenSuper); 
        grantRoleOnDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
                
        String searchPart = "*"; 
        
        Response searchPublishedSubtreeSuper = UtilIT.search(searchPart, apiTokenSuper, "&subtree="+parentDataverseAlias);
        assertTrue("Failed test if search exceeds max duration " + searchPart , UtilIT.sleepForSearch(searchPart, apiToken, "&subtree="+parentDataverseAlias, UtilIT.MAXIMUM_INGEST_LOCK_DURATION)); 
        searchPublishedSubtreeSuper.prettyPrint();
        searchPublishedSubtreeSuper.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
        
        Response searchPublishedSubtreeCurator = UtilIT.search(searchPart, apiToken, "&subtree="+parentDataverseAlias);
        searchPublishedSubtreeCurator.prettyPrint();
        searchPublishedSubtreeCurator.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
        
    }
    
    @Test
    public void testSubtreePermissions() {

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
        
        Response createDataverseResponse2 = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse2.prettyPrint();
        String dataverseAlias2 = UtilIT.getAliasFromResponse(createDataverseResponse2);

        Response createDatasetResponse2 = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias2, apiToken);
        createDatasetResponse2.prettyPrint();
        Integer datasetId2 = UtilIT.getDatasetIdFromResponse(createDatasetResponse2);
        System.out.println("id: " + datasetId2);
        String datasetPid2 = JsonPath.from(createDatasetResponse2.getBody().asString()).getString("data.persistentId");
        System.out.println("datasetPid: " + datasetPid2);

        Response datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response datasetAsJson2 = UtilIT.nativeGet(datasetId2, apiToken);
        datasetAsJson2.then().assertThat()
                .statusCode(OK.getStatusCode());

        String identifier = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");
        String identifier2 = JsonPath.from(datasetAsJson2.getBody().asString()).getString("data.identifier"); 

        String searchPart = "*"; 

        Response searchFakeSubtree = UtilIT.search(searchPart, apiToken, "&subtree=fake");
        searchFakeSubtree.prettyPrint();
        searchFakeSubtree.then().assertThat()
                .statusCode(400);
        
        Response searchFakeSubtreeNoAPI = UtilIT.search(searchPart, null, "&subtree=fake");
        searchFakeSubtreeNoAPI.prettyPrint();
        searchFakeSubtreeNoAPI.then().assertThat()
                .statusCode(400);

        Response searchUnpublishedSubtree = UtilIT.search(searchPart, apiToken, "&subtree="+dataverseAlias);
        searchUnpublishedSubtree.prettyPrint();
        searchUnpublishedSubtree.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
        
        Response searchUnpublishedSubtreeNoAPI = UtilIT.search(searchPart, null, "&subtree="+dataverseAlias);
        searchUnpublishedSubtreeNoAPI.prettyPrint();
        searchUnpublishedSubtreeNoAPI.then().assertThat()
                .statusCode(OK.getStatusCode())
                // TODO: investigate if this is a bug that nothing was found.
                .body("data.total_count", CoreMatchers.equalTo(0));
        
        Response searchUnpublishedSubtrees = UtilIT.search(searchPart, apiToken, "&subtree="+dataverseAlias +"&subtree="+dataverseAlias2);
        searchUnpublishedSubtrees.prettyPrint();
        searchUnpublishedSubtrees.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(2));
        
        Response searchUnpublishedSubtreesNoAPI = UtilIT.search(searchPart, null, "&subtree="+dataverseAlias +"&subtree="+dataverseAlias2);
        searchUnpublishedSubtreesNoAPI.prettyPrint();
        searchUnpublishedSubtreesNoAPI.then().assertThat()
                .statusCode(OK.getStatusCode())
                // TODO: investigate if this is a bug that nothing was found.
                .body("data.total_count", CoreMatchers.equalTo(0));

        Response searchUnpublishedRootSubtreeForDataset = UtilIT.search(identifier.replace("FK2/", ""), apiToken, "&subtree=root");
        searchUnpublishedRootSubtreeForDataset.prettyPrint();
        searchUnpublishedRootSubtreeForDataset.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));

        Response searchUnpublishedRootSubtreeForDatasetNoAPI = UtilIT.search(identifier.replace("FK2/", ""), null, "&subtree=root");
        searchUnpublishedRootSubtreeForDatasetNoAPI.prettyPrint();
        searchUnpublishedRootSubtreeForDatasetNoAPI.then().assertThat()
                .statusCode(OK.getStatusCode())
                // TODO: investigate if this is a bug that nothing was found.
                .body("data.total_count", CoreMatchers.equalTo(0));
        
        Response searchUnpublishedNoSubtreeForDataset = UtilIT.search(identifier.replace("FK2/", ""), apiToken, "");
        searchUnpublishedNoSubtreeForDataset.prettyPrint();
        searchUnpublishedNoSubtreeForDataset.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
        
        Response searchUnpublishedNoSubtreeForDatasetNoAPI = UtilIT.search(identifier.replace("FK2/", ""), null, "");
        searchUnpublishedNoSubtreeForDatasetNoAPI.prettyPrint();
        searchUnpublishedNoSubtreeForDatasetNoAPI.then().assertThat()
                .statusCode(OK.getStatusCode())
                // TODO: investigate if this is a bug that nothing was found.
                .body("data.total_count", CoreMatchers.equalTo(0));

        //PUBLISH
        
        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken);
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response publishDataverse2 = UtilIT.publishDataverseViaNativeApi(dataverseAlias2, apiToken);
        publishDataverse2.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDataset2 = UtilIT.publishDatasetViaNativeApi(datasetPid2, "major", apiToken);
        publishDataset2.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response searchPublishedSubtree = UtilIT.search(searchPart, apiToken, "&subtree="+dataverseAlias);
        searchPublishedSubtree.prettyPrint();
        searchPublishedSubtree.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
        
        Response searchPublishedSubtreeNoAPI = UtilIT.search(searchPart, null, "&subtree="+dataverseAlias);
        searchPublishedSubtreeNoAPI.prettyPrint();
        searchPublishedSubtreeNoAPI.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
        
        Response searchPublishedSubtrees = UtilIT.search(searchPart, apiToken, "&subtree="+dataverseAlias+"&subtree="+dataverseAlias2);
        searchPublishedSubtrees.prettyPrint();
        searchPublishedSubtrees.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(2));
        
        Response searchPublishedSubtreesNoAPI = UtilIT.search(searchPart, null, "&subtree="+dataverseAlias+"&subtree="+dataverseAlias2);
        searchPublishedSubtreesNoAPI.prettyPrint();
        searchPublishedSubtreesNoAPI.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(2));
        
        Response searchPublishedRootSubtreeForDataset = UtilIT.search(identifier.replace("FK2/", ""), apiToken, "&subtree=root");
        searchPublishedRootSubtreeForDataset.prettyPrint();
        searchPublishedRootSubtreeForDataset.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
        
        Response searchPublishedRootSubtreeForDatasetNoAPI = UtilIT.search(identifier.replace("FK2/", ""), null, "&subtree=root");
        searchPublishedRootSubtreeForDatasetNoAPI.prettyPrint();
        searchPublishedRootSubtreeForDatasetNoAPI.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
    }

    @Test
    public void testGeospatialSearch() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response setMetadataBlocks = UtilIT.setMetadataBlocks(dataverseAlias, Json.createArrayBuilder().add("citation").add("geospatial"), apiToken);
        setMetadataBlocks.prettyPrint();
        setMetadataBlocks.then().assertThat().statusCode(OK.getStatusCode());

        JsonObjectBuilder datasetJson = Json.createObjectBuilder()
                .add("datasetVersion", Json.createObjectBuilder()
                        .add("metadataBlocks", Json.createObjectBuilder()
                                .add("citation", Json.createObjectBuilder()
                                        .add("fields", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "title")
                                                        .add("value", "Dataverse HQ")
                                                        .add("typeClass", "primitive")
                                                        .add("multiple", false)
                                                )
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("authorName",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "Simpson, Homer")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "authorName"))
                                                                )
                                                        )
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("typeName", "author")
                                                )
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("datasetContactEmail",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "hsimpson@mailinator.com")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "datasetContactEmail"))
                                                                )
                                                        )
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("typeName", "datasetContact")
                                                )
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("dsDescriptionValue",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "Headquarters for Dataverse.")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "dsDescriptionValue"))
                                                                )
                                                        )
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("typeName", "dsDescription")
                                                )
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add("Other")
                                                        )
                                                        .add("typeClass", "controlledVocabulary")
                                                        .add("multiple", true)
                                                        .add("typeName", "subject")
                                                )
                                        )
                                )
                                .add("geospatial", Json.createObjectBuilder()
                                        .add("fields", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "geographicBoundingBox")
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        // The box is roughly on Cambridge, MA
                                                                        // See https://linestrings.com/bbox/#-71.187346,42.33661,-71.043056,42.409599
                                                                        .add("westLongitude",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "-71.187346")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "westLongitude")
                                                                        )
                                                                        .add("southLongitude",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "42.33661")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "southLongitude")
                                                                        )
                                                                        .add("eastLongitude",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "-71.043056")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "eastLongitude")
                                                                        )
                                                                        .add("northLongitude",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "42.409599")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "northLongitude")
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ));

        Response createDatasetResponse = UtilIT.createDataset(dataverseAlias, datasetJson, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        String datasetPid = JsonPath.from(createDatasetResponse.getBody().asString()).getString("data.persistentId");

        // Plymouth rock (41.9580775,-70.6621063) is within 50 km of Cambridge. Hit.
        Response search1 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken, "&show_entity_ids=true&geo_point=41.9580775,-70.6621063&geo_radius=50");
        search1.prettyPrint();
        search1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].entity_id", CoreMatchers.is(datasetId));

        // Plymouth rock (41.9580775,-70.6621063) is not within 1 km of Cambridge. Miss.
        Response search2 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken, "&geo_point=41.9580775,-70.6621063&geo_radius=1");
        search2.prettyPrint();
        search2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(0))
                .body("data.count_in_response", CoreMatchers.is(0));

    }

    @Test
    public void testGeospatialSearchInvalid() {

        Response noRadius = UtilIT.search("*", null, "&geo_point=40,60");
        noRadius.prettyPrint();
        noRadius.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo("If you supply geo_point you must also supply geo_radius."));

        Response noPoint = UtilIT.search("*", null, "&geo_radius=5");
        noPoint.prettyPrint();
        noPoint.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo("If you supply geo_radius you must also supply geo_point."));

        Response junkPoint = UtilIT.search("*", null, "&geo_point=junk&geo_radius=5");
        junkPoint.prettyPrint();
        junkPoint.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo("Must contain a single comma to separate latitude and longitude."));

        Response pointLatLongTooLarge = UtilIT.search("*", null, "&geo_point=999,999&geo_radius=5");
        pointLatLongTooLarge.prettyPrint();
        pointLatLongTooLarge.then().assertThat()
                // "Search Syntax Error: Error from server at http://localhost:8983/solr/collection1:
                // Can't parse point '999.0,999.0' because: Bad X value 999.0 is not in boundary Rect(minX=-180.0,maxX=180.0,minY=-90.0,maxY=90.0)"
                .statusCode(BAD_REQUEST.getStatusCode());

        Response junkRadius = UtilIT.search("*", null, "&geo_point=40,60&geo_radius=junk");
        junkRadius.prettyPrint();
        junkRadius.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo("Non-number radius supplied."));

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
    }

}
