package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import jakarta.json.JsonArray;
import org.hamcrest.CoreMatchers;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.hamcrest.Matchers;

import jakarta.json.JsonObjectBuilder;

import static jakarta.ws.rs.core.Response.Status.*;
import static java.lang.Thread.sleep;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchIT {

    private static final Logger logger = Logger.getLogger(SearchIT.class.getCanonicalName());

    @BeforeAll
    public static void setUpClass() {

        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response makeSureTokenlessSearchIsEnabled = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiRequiresToken);
        makeSureTokenlessSearchIsEnabled.then().assertThat()
                .statusCode(OK.getStatusCode());

    }

    @Test
    public void testSearchPermisions() {
        Response createUser1 = UtilIT.createRandomUser();
        String apiToken1 = UtilIT.getApiTokenFromResponse(createUser1);
        String affiliation = "testAffiliation";

        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken1, affiliation);
        createDataverse1Response.prettyPrint();
        assertEquals(201, createDataverse1Response.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse1Response);

        Response createDataset1Response = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken1);
        createDataset1Response.prettyPrint();
        createDataset1Response.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset1Response);

        Response shouldBeVisibleToUser1 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken1);
        shouldBeVisibleToUser1.prettyPrint();
        shouldBeVisibleToUser1.then().assertThat()
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].name", CoreMatchers.is("Darwin's Finches"))
                .statusCode(OK.getStatusCode());

        Response createUser2 = UtilIT.createRandomUser();
        String username2 = UtilIT.getUsernameFromResponse(createUser2);
        String apiToken2 = UtilIT.getApiTokenFromResponse(createUser2);

        Response shouldNotBeVisibleToUser2 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken2);
        shouldNotBeVisibleToUser2.prettyPrint();
        shouldNotBeVisibleToUser2.then().assertThat()
                .body("data.total_count", CoreMatchers.is(0))
                .statusCode(OK.getStatusCode());

        String nullToken = null;

        Response shouldNotBeVisibleToTokenLess = UtilIT.search("id:dataset_" + datasetId + "_draft", nullToken);
        shouldNotBeVisibleToTokenLess.prettyPrint();
        shouldNotBeVisibleToTokenLess.then().assertThat()
                .body("data.total_count", CoreMatchers.is(0))
                .statusCode(OK.getStatusCode());

        String roleToAssign = "admin";

        Response grantUser2AccessOnDataset = UtilIT.grantRoleOnDataverse(dataverseAlias, roleToAssign, "@" + username2, apiToken1);
        grantUser2AccessOnDataset.prettyPrint();
        assertEquals(200, grantUser2AccessOnDataset.getStatusCode());

        String searchPart = "id:dataset_" + datasetId + "_draft";
        assertTrue(UtilIT.sleepForSearch(searchPart, apiToken2, "", 1, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if search exceeds max duration " + searchPart);
        
        Response shouldBeVisibleToUser2 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken2);
        shouldBeVisibleToUser2.prettyPrint();
        shouldBeVisibleToUser2.then().assertThat()
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].name", CoreMatchers.is("Darwin's Finches"))
                .body("data.items[0].publicationStatuses", CoreMatchers.hasItems("Unpublished", "Draft"))
                .statusCode(OK.getStatusCode());

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken1);
        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken1);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response makeSureTokenlessSearchIsEnabled = UtilIT.deleteSetting(SettingsServiceBean.Key.SearchApiRequiresToken);
        makeSureTokenlessSearchIsEnabled.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishedPublicDataShouldBeVisibleToTokenless = UtilIT.search("id:dataset_" + datasetId, nullToken);
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
                .body("data.items[0].affiliation", CoreMatchers.is(affiliation))
                .body("data.items[0].parentDataverseName", CoreMatchers.is("Root"))
                .body("data.items[0].parentDataverseIdentifier", CoreMatchers.is("root"));

        Response disableTokenlessSearch = UtilIT.setSetting(SettingsServiceBean.Key.SearchApiRequiresToken, "true");
        disableTokenlessSearch.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response dataverse47behaviorOfTokensBeingRequired = UtilIT.search("id:dataset_" + datasetId, nullToken);
        dataverse47behaviorOfTokensBeingRequired.prettyPrint();
        dataverse47behaviorOfTokensBeingRequired.then().assertThat()
                .body("message", CoreMatchers.equalTo(AbstractApiBean.RESPONSE_MESSAGE_AUTHENTICATED_USER_REQUIRED))
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
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);

        Response searchResponse = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken);
        searchResponse.prettyPrint();
        assertFalse(searchResponse.body().jsonPath().getString("data.items[0].citation").contains("href"));
        assertTrue(searchResponse.body().jsonPath().getString("data.items[0].citationHtml").contains("href"));
        searchResponse.then().assertThat()
                .body("data.items[0].citation", CoreMatchers.not(Matchers.containsString("href")))
                .body("data.items[0].citationHtml", Matchers.containsString("href"))
                .statusCode(200);

        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response uploadImage = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadImage.prettyPrint();
        uploadImage.then().assertThat()
                .statusCode(200);

        Response publishResponse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishResponse.prettyPrint();
        publishResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        publishResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishResponse.prettyPrint();
        publishResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response updateTitleResponseAuthor = UtilIT.updateDatasetTitleViaSword(datasetPersistentId, "New Title", apiToken);
        updateTitleResponseAuthor.prettyPrint();
        updateTitleResponseAuthor.then().assertThat()
                .statusCode(OK.getStatusCode());

        // search descending will get the latest 100.
        // This could fail if more than 100 get created between our update and the search. Highly unlikely
        searchResponse = UtilIT.search("*&type=file&sort=date&order=desc&per_page=100&start=0&subtree=root" , apiToken);
        searchResponse.prettyPrint();

        int i=0;
        String parentCitation = "";
        String datasetName = "";
        // most likely ours is in index 0, but it's not a guaranty.
        while (i < 100) {
            String dataset_persistent_id = searchResponse.body().jsonPath().getString("data.items[" + i + "].dataset_persistent_id");
            if (datasetPersistentId.equalsIgnoreCase(dataset_persistent_id)) {
                parentCitation = searchResponse.body().jsonPath().getString("data.items[" + i + "].dataset_citation");
                datasetName = searchResponse.body().jsonPath().getString("data.items[" + i + "].dataset_name");
                break;
            }
            i++;
        }
        // see https://github.com/IQSS/dataverse/issues/10735
        // was showing the citation of the draft version and not the released parent
        assertFalse(parentCitation.contains("New Title"));
        assertTrue(parentCitation.contains(datasetName));
        assertFalse(parentCitation.contains("DRAFT"));
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
                .body("data.items[0].metadataBlocks.citation.fields", Matchers.hasSize(5))
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
                .body("message", CoreMatchers.containsString("File is larger than maximum size:"))
                /**
                 * @todo We want this to expect 400 (BAD_REQUEST), not 403
                 * (FORBIDDEN).
                 */
                //                .statusCode(400);
                .statusCode(FORBIDDEN.getStatusCode());

        String datasetLogo = "src/main/webapp/resources/images/cc0.png";
        File datasetLogoFile = new File(datasetLogo);
        String datasetLogoAsBase64 = ImageThumbConverter.generateImageThumbnailFromFileAsBase64(datasetLogoFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);

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
        UtilIT.sleepForReindex(String.valueOf(datasetId), apiToken, 5);
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
        UtilIT.sleepForReindex(String.valueOf(datasetId), apiToken, 5);
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
        assertTrue(UtilIT.sleepForSearch(searchPart, apiToken, "&subtree=" + dataverseAlias, 1, UtilIT.GENERAL_LONG_DURATION), "Missing subDV");
        
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
            Thread.sleep(4000);
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
        UtilIT.sleepForReindex(datasetPid, apiToken, 5);
        assertTrue(UtilIT.sleepForSearch(searchPart, apiToken, "&subtree=" + dataverseAlias, 2, UtilIT.GENERAL_LONG_DURATION), "Did not find 2 children");
        assertTrue(UtilIT.sleepForSearch(searchPart, apiToken, "&subtree=" + dataverseAlias2, 1, UtilIT.GENERAL_LONG_DURATION), "Did not find 1 child");
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

        Response grantRoleOnDataverseResponse = UtilIT.grantRoleOnDataverse(subDataverseAlias, "curator", "@" + username, apiTokenSuper); 
        grantRoleOnDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        String searchPart = "*"; 
        
        assertTrue(UtilIT.sleepForSearch(searchPart, apiToken, "&subtree="+parentDataverseAlias, 1, UtilIT.GENERAL_LONG_DURATION), "Failed test if search exceeds max duration " + searchPart);
        
        Response searchPublishedSubtreeSuper = UtilIT.search(searchPart, apiTokenSuper, "&subtree="+parentDataverseAlias);
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
        
        // Wait a little while for the index to pick up the datasets, otherwise timing issue with searching for it.
        UtilIT.sleepForReindex(datasetId2.toString(), apiToken, 3);

        String identifier = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");
        String identifier2 = JsonPath.from(datasetAsJson2.getBody().asString()).getString("data.identifier"); 

        String searchPart = "*"; 

        Response searchFakeSubtree = UtilIT.search(searchPart, apiToken, "&subtree=fake");
        searchFakeSubtree.prettyPrint();
        searchFakeSubtree.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());
        
        Response searchFakeSubtreeNoAPI = UtilIT.search(searchPart, null, "&subtree=fake");
        searchFakeSubtreeNoAPI.prettyPrint();
        searchFakeSubtreeNoAPI.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

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

        UtilIT.sleepForReindex(String.valueOf(datasetId), apiToken, 5);
        Response searchUnpublishedRootSubtreeForDataset = UtilIT.search(identifier.replace("FK2/", ""), apiToken, "&subtree=root");
        searchUnpublishedRootSubtreeForDataset.prettyPrint();
        searchUnpublishedRootSubtreeForDataset.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));

        UtilIT.sleepForReindex(String.valueOf(datasetId), apiToken, 5);
        Response searchUnpublishedRootSubtreeForDatasetNoAPI = UtilIT.search(identifier.replace("FK2/", ""), null, "&subtree=root");
        searchUnpublishedRootSubtreeForDatasetNoAPI.prettyPrint();
        searchUnpublishedRootSubtreeForDatasetNoAPI.then().assertThat()
                .statusCode(OK.getStatusCode())
                // TODO: investigate if this is a bug that nothing was found.
                .body("data.total_count", CoreMatchers.equalTo(0));

        UtilIT.sleepForReindex(String.valueOf(datasetId), apiToken, 5);
        Response searchUnpublishedNoSubtreeForDataset = UtilIT.search(identifier.replace("FK2/", ""), apiToken, "");
        searchUnpublishedNoSubtreeForDataset.prettyPrint();
        searchUnpublishedNoSubtreeForDataset.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));
        
        UtilIT.sleepForReindex(String.valueOf(datasetId), apiToken, 5);
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
        
        assertTrue(UtilIT.sleepForSearch(searchPart, null, "&subtree=" + dataverseAlias2, 1, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Missing dataset w/no apiKey");
        
        Response searchPublishedSubtreesNoAPI = UtilIT.search(searchPart, null, "&subtree="+dataverseAlias+"&subtree="+dataverseAlias2);
        searchPublishedSubtreesNoAPI.prettyPrint();
        searchPublishedSubtreesNoAPI.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(2));
        
        UtilIT.sleepForReindex(String.valueOf(datasetId), apiToken, 5);
        Response searchPublishedRootSubtreeForDataset = UtilIT.search(identifier.replace("FK2/", ""), apiToken, "&subtree=root");
        searchPublishedRootSubtreeForDataset.prettyPrint();
        searchPublishedRootSubtreeForDataset.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.equalTo(1));

        UtilIT.sleepForReindex(String.valueOf(datasetId), apiToken, 5);
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
                                                                        .add("southLatitude",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "42.33661")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "southLatitude")
                                                                        )
                                                                        .add("eastLongitude",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "-71.043056")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "eastLongitude")
                                                                        )
                                                                        .add("northLatitude",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "42.409599")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "northLatitude")
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

    @Test
    public void testRangeQueries() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Using the "astrophysics" block because it contains all field types relevant for range queries
        // (int, float and date)
        Response setMetadataBlocks = UtilIT.setMetadataBlocks(dataverseAlias, Json.createArrayBuilder().add("citation").add("astrophysics"), apiToken);
        setMetadataBlocks.prettyPrint();
        setMetadataBlocks.then().assertThat().statusCode(OK.getStatusCode());

        JsonObjectBuilder datasetJson = Json.createObjectBuilder()
                .add("datasetVersion", Json.createObjectBuilder()
                        .add("metadataBlocks", Json.createObjectBuilder()
                                .add("citation", Json.createObjectBuilder()
                                        .add("fields", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "title")
                                                        .add("value", "Test Astrophysics Dataset")
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
                                                                                        .add("value", "This is a test dataset.")
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
                                .add("astrophysics", Json.createObjectBuilder()
                                        .add("fields", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "coverage.Temporal")
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("coverage.Temporal.StartTime",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "2015-01-01")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "coverage.Temporal.StartTime")
                                                                        )
                                                                )
                                                        )
                                                )
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "coverage.ObjectCount")
                                                        .add("typeClass", "primitive")
                                                        .add("multiple", false)
                                                        .add("value", "9000")
                                                )
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "coverage.SkyFraction")
                                                        .add("typeClass", "primitive")
                                                        .add("multiple", false)
                                                        .add("value", "0.002")
                                                )
                                        )
                                )
                        ));

        Response createDatasetResponse = UtilIT.createDataset(dataverseAlias, datasetJson, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        String datasetPid = JsonPath.from(createDatasetResponse.getBody().asString()).getString("data.persistentId");

        // Integer range query: Hit
        Response search1 = UtilIT.search("id:dataset_" + datasetId + "_draft AND coverage.ObjectCount:[1000 TO 10000]", apiToken, "&show_entity_ids=true");
        search1.prettyPrint();
        search1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].entity_id", CoreMatchers.is(datasetId));

        // Integer range query: Miss
        Response search2 = UtilIT.search("id:dataset_" + datasetId + "_draft AND coverage.ObjectCount:[* TO 1000]", apiToken);
        search2.prettyPrint();
        search2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(0))
                .body("data.count_in_response", CoreMatchers.is(0));

        // Float range query: Hit
        Response search3 = UtilIT.search("id:dataset_" + datasetId + "_draft AND coverage.SkyFraction:[0 TO 0.5]", apiToken, "&show_entity_ids=true");
        search3.prettyPrint();
        search3.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].entity_id", CoreMatchers.is(datasetId));

        // Float range query: Miss
        Response search4 = UtilIT.search("id:dataset_" + datasetId + "_draft AND coverage.SkyFraction:[0.5 TO 1]", apiToken);
        search4.prettyPrint();
        search4.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(0))
                .body("data.count_in_response", CoreMatchers.is(0));

        // Date range query: Hit
        Response search5 = UtilIT.search("id:dataset_" + datasetId + "_draft AND coverage.Temporal.StartTime:2015", apiToken, "&show_entity_ids=true");
        search5.prettyPrint();
        search5.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].entity_id", CoreMatchers.is(datasetId));

        // Date range query: Miss
        Response search6 = UtilIT.search("id:dataset_" + datasetId + "_draft AND coverage.Temporal.StartTime:[2020 TO *]", apiToken);
        search6.prettyPrint();
        search6.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(0))
                .body("data.count_in_response", CoreMatchers.is(0));

        // Combining all three range queries: Hit
        Response search7 = UtilIT.search("id:dataset_" + datasetId + "_draft AND coverage.ObjectCount:[1000 TO 10000] AND coverage.SkyFraction:[0 TO 0.5] AND coverage.Temporal.StartTime:2015", apiToken, "&show_entity_ids=true");
        search7.prettyPrint();
        search7.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].entity_id", CoreMatchers.is(datasetId));

        // Combining all three range queries: Miss
        Response search8 = UtilIT.search("id:dataset_" + datasetId + "_draft AND coverage.ObjectCount:[* TO 1000] AND coverage.SkyFraction:[0.5 TO 1] AND coverage.Temporal.StartTime:[2020 TO *]", apiToken);
        search8.prettyPrint();
        search8.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(0))
                .body("data.count_in_response", CoreMatchers.is(0));

    }

    @Test
    public void testSearchWithInvalidDateField() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response setMetadataBlocks = UtilIT.setMetadataBlocks(dataverseAlias, Json.createArrayBuilder().add("citation"), apiToken);
        setMetadataBlocks.prettyPrint();
        setMetadataBlocks.then().assertThat().statusCode(OK.getStatusCode());

        // Adding a dataset with a date in the "timePeriodCoveredStart" field that doesn't match Solr's date format
        // (ISO-8601 format, e.g. YYYY-MM-DDThh:mm:ssZ, YYYYY-MM-DD, YYYY-MM, YYYY)
        // (See: https://solr.apache.org/guide/solr/latest/indexing-guide/date-formatting-math.html)
        // So the date currently cannot be indexed
        JsonObjectBuilder datasetJson = Json.createObjectBuilder()
                .add("datasetVersion", Json.createObjectBuilder()
                        .add("metadataBlocks", Json.createObjectBuilder()
                                .add("citation", Json.createObjectBuilder()
                                        .add("fields", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "title")
                                                        .add("value", "Test Dataset")
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
                                                                                        .add("value", "This is a test dataset.")
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
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "timePeriodCovered")
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("timePeriodCoveredStart",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "15-01-01")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "timePeriodCoveredStart")
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

        // When querying on the date field: miss (because the date field was skipped during indexing)
        Response search1 = UtilIT.search("id:dataset_" + datasetId + "_draft AND timePeriodCoveredStart:[2000 TO 2020]", apiToken);
        search1.prettyPrint();
        search1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(0))
                .body("data.count_in_response", CoreMatchers.is(0));

        // When querying not on the date field: the dataset can be found (only the date field was skipped during indexing, not the entire dataset)
        Response search2 = UtilIT.search("id:dataset_" + datasetId + "_draft", apiToken, "&show_entity_ids=true");
        search2.prettyPrint();
        search2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].entity_id", CoreMatchers.is(datasetId));

    }

    @AfterEach
    public void tearDownDataverse() {
        File treesThumb = new File("scripts/search/data/binary/trees.png.thumb48");
        treesThumb.delete();
        File cc0Thumb = new File("src/main/webapp/resources/images/cc0.png.thumb48");
        cc0Thumb.delete();
        File dataverseprojectThumb = new File("src/main/webapp/resources/images/dataverseproject.png.thumb48");
        dataverseprojectThumb.delete();
    }

    @AfterAll
    public static void cleanup() {
    }

    @Test
    public void testSearchFilesAndUrlImages() throws InterruptedException {
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
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response logoResponse = UtilIT.uploadDatasetLogo(datasetPid, pathToFile, apiToken);
        logoResponse.prettyPrint();
        logoResponse.then().assertThat()
                .statusCode(200);

        Response uploadImage = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadImage.prettyPrint();
        uploadImage.then().assertThat()
                .statusCode(200);
        pathToFile = "src/main/webapp/resources/js/mydata.js";
        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat()
                .statusCode(200);
        pathToFile = "src/test/resources/tab/test.tab";
        String searchableUniqueId = "testtab"+ UUID.randomUUID().toString().substring(0, 8); // so the search only returns 1 file
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", searchableUniqueId)
                .add("restrict", "true")
                .add("categories", Json.createArrayBuilder().add("Data"));
        Response uploadTabFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, json.build(), apiToken);
        uploadTabFile.prettyPrint();
        uploadTabFile.then().assertThat()
                .statusCode(200);
        // Ensure tabular file is ingested
        sleep(2000);
        // Set tabular tags
        String tabularFileId = uploadTabFile.getBody().jsonPath().getString("data.files[0].dataFile.id");
        List<String> testTabularTags = List.of("Survey", "Genomics");
        Response setFileTabularTagsResponse = UtilIT.setFileTabularTags(tabularFileId, apiToken, testTabularTags);
        setFileTabularTagsResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response searchResp = UtilIT.search("dataverseproject", apiToken);
        searchResp.prettyPrint();
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items[0].type", CoreMatchers.is("file"))
                .body("data.items[0].file_content_type", CoreMatchers.is("image/png"))
                .body("data.items[0].url", CoreMatchers.containsString("/api/access/datafile/"))
                .body("data.items[0].image_url", CoreMatchers.containsString("/api/access/datafile/"))
                .body("data.items[0].image_url", CoreMatchers.containsString("imageThumb=true"));

        searchResp = UtilIT.search(dataverseAlias, apiToken);
        searchResp.prettyPrint();
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items[0].type", CoreMatchers.is("dataverse"))
                .body("data.items[0].url", CoreMatchers.containsString("/dataverse/"))
                .body("data.items[0]", CoreMatchers.not(CoreMatchers.hasItem("image_url")));

        searchResp = UtilIT.search(datasetPid, apiToken);
        searchResp.prettyPrint();
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items[0].type", CoreMatchers.is("dataset"))
                .body("data.items[0].image_url", CoreMatchers.containsString("/logo"));

        searchResp = UtilIT.search("mydata", apiToken);
        searchResp.prettyPrint();
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items[0].type", CoreMatchers.is("file"))
                .body("data.items[0].url", CoreMatchers.containsString("/datafile/"))
                .body("data.items[0]", CoreMatchers.not(CoreMatchers.hasItem("image_url")));
        searchResp = UtilIT.search(searchableUniqueId, apiToken);
        searchResp.prettyPrint();
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items[0].type", CoreMatchers.is("file"))
                .body("data.items[0].url", CoreMatchers.containsString("/datafile/"))
                .body("data.items[0].variables", CoreMatchers.is(3))
                .body("data.items[0].observations", CoreMatchers.is(10))
                .body("data.items[0].restricted", CoreMatchers.is(true))
                .body("data.items[0].canDownloadFile", CoreMatchers.is(true))
                .body("data.items[0].tabularTags", CoreMatchers.hasItem("Genomics"))
                .body("data.items[0]", CoreMatchers.not(CoreMatchers.hasItem("image_url")));
    }

    @Test
    public void testShowTypeCounts() throws InterruptedException  {
        //Create 1 user and 1 Dataverse/Collection
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String affiliation = "testAffiliation";

        // test total_count_per_object_type is included with zero counts for each type
        Response searchResp = UtilIT.search(username, apiToken, "&show_type_counts=true");
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count_per_object_type.Dataverses", CoreMatchers.is(0))
                .body("data.total_count_per_object_type.Datasets", CoreMatchers.is(0))
                .body("data.total_count_per_object_type.Files", CoreMatchers.is(0));
        

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken, affiliation);
        assertEquals(201, createDataverseResponse.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        String dataverseWithDatasetsFilesAlias = dataverseAlias;

        // create 3 Datasets, each with 2 Datafiles
        for (int i = 0; i < 3; i++) {
            Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
            createDatasetResponse.then().assertThat()
                    .statusCode(CREATED.getStatusCode());
            String datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse).toString();

            // putting the dataverseAlias in the description of each file so the search q={dataverseAlias} will return dataverse, dataset, and files for this test only
            String jsonAsString = "{\"description\":\"" + dataverseAlias + "\",\"directoryLabel\":\"data/subdir1\",\"categories\":[\"Data\"], \"restrict\":\"false\"  }";

            String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
            Response uploadImage = UtilIT.uploadFileViaNative(datasetId, pathToFile, jsonAsString, apiToken);
            uploadImage.then().assertThat()
                    .statusCode(200);
            pathToFile = "src/main/webapp/resources/js/mydata.js";
            Response uploadFile = UtilIT.uploadFileViaNative(datasetId, pathToFile, jsonAsString, apiToken);
            uploadFile.then().assertThat()
                    .statusCode(200);

            // This call forces a wait for dataset indexing to finish and gives time for file uploads to complete
            UtilIT.search("id:dataset_" + datasetId, apiToken);
            UtilIT.sleepForReindex(datasetId, apiToken, 3);
        }

        // Test Search without show_type_counts
        searchResp = UtilIT.search(dataverseAlias, apiToken);
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count_per_object_type", CoreMatchers.equalTo(null));
        // Test Search with show_type_counts = FALSE
        searchResp = UtilIT.search(dataverseAlias, apiToken, "&show_type_counts=false");
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count_per_object_type", CoreMatchers.equalTo(null));
        // Test Search with show_type_counts = TRUE
        searchResp = UtilIT.search(dataverseAlias, apiToken, "&show_type_counts=true");
        searchResp.prettyPrint();
        
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count_per_object_type.Dataverses", CoreMatchers.is(1))
                .body("data.total_count_per_object_type.Datasets", CoreMatchers.is(3))
                .body("data.total_count_per_object_type.Files", CoreMatchers.is(6));
        
        
        
        // go through the same exercise with only a collection to verify that Dataasets and Files
        // are there with a count of 0
        
        createDataverseResponse = UtilIT.createRandomDataverse(apiToken, affiliation);
        assertEquals(201, createDataverseResponse.getStatusCode());
        dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        sleep(4000); //make sure new dataverse gets indexed

        // Test Search without show_type_counts
        searchResp = UtilIT.search(dataverseAlias, apiToken);
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count_per_object_type", CoreMatchers.equalTo(null));
        // Test Search with show_type_counts = FALSE
        searchResp = UtilIT.search(dataverseAlias, apiToken, "&show_type_counts=false");
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count_per_object_type", CoreMatchers.equalTo(null));
        // Test Search with show_type_counts = TRUE
        searchResp = UtilIT.search(dataverseAlias, apiToken, "&show_type_counts=true");
        searchResp.prettyPrint();
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.total_count_per_object_type.Dataverses", CoreMatchers.is(1))
                .body("data.total_count_per_object_type.Datasets", CoreMatchers.is(0))
                .body("data.total_count_per_object_type.Files", CoreMatchers.is(0));

        // Test Search with show_type_counts = TRUE getting only Dataverses
        searchResp = UtilIT.search(dataverseWithDatasetsFilesAlias, apiToken, "&show_facets=true&type=dataverse&show_type_counts=true");
        searchResp.prettyPrint();
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.count_in_response", CoreMatchers.is(1)) // 1 dataverse
                .body("data.total_count_per_object_type.Dataverses", CoreMatchers.is(1))
                .body("data.total_count_per_object_type.Datasets", CoreMatchers.is(3))
                .body("data.total_count_per_object_type.Files", CoreMatchers.is(6));
        // Test Search with show_type_counts = TRUE getting only Dataverses and Datasets
        searchResp = UtilIT.search(dataverseWithDatasetsFilesAlias, apiToken, "&type=dataverse&type=dataset&show_type_counts=true");
        searchResp.prettyPrint();
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.count_in_response", CoreMatchers.is(4)) // 1 dataverse + 3 datasets
                .body("data.total_count_per_object_type.Dataverses", CoreMatchers.is(1))
                .body("data.total_count_per_object_type.Datasets", CoreMatchers.is(3))
                .body("data.total_count_per_object_type.Files", CoreMatchers.is(6));
        // Test Search with show_type_counts = TRUE getting Dataverses, Datasets and Files
        searchResp = UtilIT.search(dataverseWithDatasetsFilesAlias, apiToken, "&type=dataverse&type=dataset&type=file&show_type_counts=true");
        searchResp.prettyPrint();
        searchResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.count_in_response", CoreMatchers.is(10)) // 1 dataverse + 3 datasets + 6 files
                .body("data.total_count_per_object_type.Dataverses", CoreMatchers.is(1))
                .body("data.total_count_per_object_type.Datasets", CoreMatchers.is(3))
                .body("data.total_count_per_object_type.Files", CoreMatchers.is(6));
    }

    @Test
    public void testTabularFiles() throws IOException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        Path pathToDataFile = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "data.csv");
        String contentOfCsv = ""
                + "name,pounds,species,treats\n"
                + "Midnight,15,dog,milkbones\n"
                + "Tiger,17,cat,cat grass\n"
                + "Panther,21,cat,cat nip\n";
        java.nio.file.Files.write(pathToDataFile, contentOfCsv.getBytes());

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToDataFile.toString(), apiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("data.csv"));

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToDataFile);

        Long fileId = JsonPath.from(uploadFile.body().asString()).getLong("data.files[0].dataFile.id");

        Response search = UtilIT.search("entityId:" + fileId, apiToken);
        search.prettyPrint();
        search.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items[0].name", is("data.tab"))
                .body("data.items[0].variables", is(4))
                .body("data.items[0].observations", is(3));
    }

    @Test
    public void testTotalCount() throws IOException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        Path pathToDataFile = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "data.csv");
        String contentOfCsv = ""
                + "name,pounds,species,treats\n"
                + "Midnight,15,dog,milkbones\n"
                + "Tiger,17,cat,cat grass\n"
                + "Panther,21,cat,cat nip\n";
        java.nio.file.Files.write(pathToDataFile, contentOfCsv.getBytes());

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToDataFile.toString(), apiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("data.csv"));

        Response searchResponse = UtilIT.search("&show_facets=true&sort=date&order=desc&show_type_counts=true&subtree=root&per_page=10&type=dataverse&type=dataset", apiToken);
        searchResponse.prettyPrint();
        searchResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testShowCollections() {
        Response createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        JsonPath createdDataverse = JsonPath.from(createDataverseResponse.body().asString());
        String dataverseName = createdDataverse.getString("data.name");
        String dataverseAlias = createdDataverse.getString("data.alias");
        Integer dataverseId = createdDataverse.getInt("data.id");

        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        JsonPath createdDataset = JsonPath.from(createDatasetResponse.body().asString());
        int datasetId = createdDataset.getInt("data.id");
        String datasetPid = createdDataset.getString("data.persistentId");

        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());

        // Test that the Dataverse collection that the dataset was created in is returned
        Response searchResponse = UtilIT.search("*", apiToken, "&subtree=" + dataverseAlias + "&type=dataset&show_collections=true");
        searchResponse.prettyPrint();
        searchResponse.then().assertThat()
                      .statusCode(OK.getStatusCode())
                      .body("data.count_in_response", CoreMatchers.is(1))
                      .body("data.items[0].collections[0].id", CoreMatchers.is(dataverseId))
                      .body("data.items[0].collections[0].name", CoreMatchers.is(dataverseName))
                      .body("data.items[0].collections[0].alias", CoreMatchers.is(dataverseAlias));

        Response createDataverse2Response = UtilIT.createRandomDataverse(apiToken);
        createDataverse2Response.prettyPrint();
        createDataverse2Response.then().assertThat().statusCode(CREATED.getStatusCode());
        JsonPath createDataverse2 = JsonPath.from(createDataverse2Response.body().asString());
        String dataverse2Name = createDataverse2.getString("data.name");
        String dataverse2Alias = createDataverse2.getString("data.alias");
        Integer dataverse2Id = createDataverse2.getInt("data.id");

        UtilIT.publishDataverseViaNativeApi(dataverse2Alias, apiToken).then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.linkDataset(datasetPid, dataverse2Alias, apiToken).then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.sleepForReindex(String.valueOf(datasetId), apiToken, 5);

        // Test that the Dataverse collection that the dataset was linked to is also returned
        searchResponse = UtilIT.search("*", apiToken, "&subtree=" + dataverseAlias + "&type=dataset&show_collections=true");
        searchResponse.prettyPrint();
        searchResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].collections[0].id", CoreMatchers.is(dataverseId))
                .body("data.items[0].collections[0].name", CoreMatchers.is(dataverseName))
                .body("data.items[0].collections[0].alias", CoreMatchers.is(dataverseAlias))
                .body("data.items[0].collections[1].id", CoreMatchers.is(dataverse2Id))
                .body("data.items[0].collections[1].name", CoreMatchers.is(dataverse2Name))
                .body("data.items[0].collections[1].alias", CoreMatchers.is(dataverse2Alias));;

    }

}
