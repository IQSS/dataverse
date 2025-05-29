package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileTag;
import edu.harvard.iq.dataverse.DatasetVersionFilesServiceBean;
import edu.harvard.iq.dataverse.FileSearchCriteria;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.dataaccess.AbstractRemoteOverlayAccessIO;
import edu.harvard.iq.dataverse.dataaccess.GlobusOverlayAccessIOTest;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.datavariable.VariableMetadataDDIParser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JSONLDUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.path.json.JsonPath;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.JsonArrayBuilder;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.DatasetVersion.DEACCESSION_LINK_MAX_LENGTH;
import static edu.harvard.iq.dataverse.api.ApiConstants.*;
import static edu.harvard.iq.dataverse.api.UtilIT.API_TOKEN_HTTP_HEADER;
import static edu.harvard.iq.dataverse.api.UtilIT.equalToCI;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.with;
import static jakarta.ws.rs.core.Response.Status.*;
import static java.lang.Thread.sleep;
import java.time.Year;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.*;

public class DatasetsIT {

    private static final Logger logger = Logger.getLogger(DatasetsIT.class.getCanonicalName());
    
    @BeforeAll
    public static void setUpClass() {
        
        
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response removeIdentifierGenerationStyle = UtilIT.deleteSetting(SettingsServiceBean.Key.IdentifierGenerationStyle);
        removeIdentifierGenerationStyle.then().assertThat()
                .statusCode(200);

        Response removeExcludeEmail = UtilIT.deleteSetting(SettingsServiceBean.Key.ExcludeEmailFromExport);
        removeExcludeEmail.then().assertThat()
                .statusCode(200);

        Response removeAnonymizedFieldTypeNames = UtilIT.deleteSetting(SettingsServiceBean.Key.AnonymizedFieldTypeNames);
        removeAnonymizedFieldTypeNames.then().assertThat()
                .statusCode(200);

        UtilIT.deleteSetting(SettingsServiceBean.Key.MaxEmbargoDurationInMonths);

        /* With Dual mode, we can no longer mess with upload methods since native is now required for anything to work
               
        Response removeDcmUrl = UtilIT.deleteSetting(SettingsServiceBean.Key.DataCaptureModuleUrl);
        removeDcmUrl.then().assertThat()
                .statusCode(200);

        Response removeUploadMethods = UtilIT.deleteSetting(SettingsServiceBean.Key.UploadMethods);
        removeUploadMethods.then().assertThat()
                .statusCode(200);
         */
    }


    @AfterAll
    public static void afterClass() {

        Response removeIdentifierGenerationStyle = UtilIT.deleteSetting(SettingsServiceBean.Key.IdentifierGenerationStyle);
        removeIdentifierGenerationStyle.then().assertThat()
                .statusCode(200);

        Response removeExcludeEmail = UtilIT.deleteSetting(SettingsServiceBean.Key.ExcludeEmailFromExport);
        removeExcludeEmail.then().assertThat()
                .statusCode(200);

        Response removeAnonymizedFieldTypeNames = UtilIT.deleteSetting(SettingsServiceBean.Key.AnonymizedFieldTypeNames);
        removeAnonymizedFieldTypeNames.then().assertThat()
                .statusCode(200);

        UtilIT.deleteSetting(SettingsServiceBean.Key.MaxEmbargoDurationInMonths);

        /* See above
        Response removeDcmUrl = UtilIT.deleteSetting(SettingsServiceBean.Key.DataCaptureModuleUrl);
        removeDcmUrl.then().assertThat()
                .statusCode(200);

        Response removeUploadMethods = UtilIT.deleteSetting(SettingsServiceBean.Key.UploadMethods);
        removeUploadMethods.then().assertThat()
                .statusCode(200);
         */
    }
    
    @Test
    public void testCollectionSchema(){
        
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        
        Response getCollectionSchemaResponse =  UtilIT.getCollectionSchema(dataverseAlias, apiToken);
        getCollectionSchemaResponse.prettyPrint();
        getCollectionSchemaResponse.then().assertThat()
                .statusCode(200);

        JsonObject expectedSchema = null;
        try {
            expectedSchema = JsonUtil.getJsonObjectFromFile("doc/sphinx-guides/source/_static/api/dataset-schema.json");
        } catch (IOException ex) {
        }

        assertEquals(JsonUtil.prettyPrint(expectedSchema), JsonUtil.prettyPrint(getCollectionSchemaResponse.body().asString()));
        
        String expectedJson = UtilIT.getDatasetJson("scripts/search/tests/data/dataset-finch1.json");
        
        Response validateDatasetJsonResponse = UtilIT.validateDatasetJson(dataverseAlias, expectedJson, apiToken);
        validateDatasetJsonResponse.prettyPrint();
        validateDatasetJsonResponse.then().assertThat()
                .statusCode(200);
        
        
        String pathToJsonFile = "scripts/search/tests/data/datasetMissingReqFields.json"; 
        
        String jsonIn = UtilIT.getDatasetJson(pathToJsonFile);
        
        Response validateBadDatasetJsonResponse = UtilIT.validateDatasetJson(dataverseAlias, jsonIn, apiToken);
        validateBadDatasetJsonResponse.prettyPrint();
        validateBadDatasetJsonResponse.then().assertThat()
                .statusCode(200);

        
        validateBadDatasetJsonResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body(containsString("failed validation"));
        
        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());
        
    }

    @Test
    public void testDatasetSchemaValidation() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response getCollectionSchemaResponse =  UtilIT.getCollectionSchema(dataverseAlias, apiToken);
        getCollectionSchemaResponse.prettyPrint();
        getCollectionSchemaResponse.then().assertThat()
                .statusCode(200);

        JsonObject expectedSchema = null;
        try {
            expectedSchema = JsonUtil.getJsonObjectFromFile("doc/sphinx-guides/source/_static/api/dataset-schema.json");
        } catch (IOException ex) {
        }

        assertEquals(JsonUtil.prettyPrint(expectedSchema), JsonUtil.prettyPrint(getCollectionSchemaResponse.body().asString()));

        // add a language that is not in the Controlled vocabulary
        testDatasetSchemaValidationHelper(dataverseAlias, apiToken,
                "\"aar\"",
                "\"aar\",\"badlang\"",
                BundleUtil.getStringFromBundle("schema.validation.exception.dataset.cvv.missing", List.of("fields", "language", "badlang"))
        );

        // change multiple to true on value that is a not a List
        testDatasetSchemaValidationHelper(dataverseAlias, apiToken,
                "multiple\": false,\n" +
                        "            \"typeName\": \"title",
                "multiple\": true,\n" +
                        "            \"typeName\": \"title",
                BundleUtil.getStringFromBundle("schema.validation.exception.notlist.multiple", List.of("fields", "title"))
        );

        // change multiple to false on value that is a List
        testDatasetSchemaValidationHelper(dataverseAlias, apiToken,
                "typeName\": \"language\",\n" +
                        "            \"multiple\": true",
                "typeName\": \"language\",\n" +
                        "            \"multiple\": false",
                BundleUtil.getStringFromBundle("schema.validation.exception.list.notmultiple", List.of("fields", "language"))
        );

        // add a mismatched typeName
        testDatasetSchemaValidationHelper(dataverseAlias, apiToken,
                "\"typeName\": \"datasetContactName\",",
                "\"typeName\": \"datasetContactNme\",",
                BundleUtil.getStringFromBundle("schema.validation.exception.compound.mismatch", List.of("datasetContactName", "datasetContactNme"))
        );

        // add a typeName which is not allowed
        testDatasetSchemaValidationHelper(dataverseAlias, apiToken,
                "\"datasetContactEmail\": {\n" +
                        "                  \"typeClass\": \"primitive\",\n" +
                        "                  \"multiple\": false,\n" +
                        "                  \"typeName\": \"datasetContactEmail\",",
                "\"datasetContactNotAllowed\": {\n" +
                        "                  \"typeClass\": \"primitive\",\n" +
                        "                  \"multiple\": false,\n" +
                        "                  \"typeName\": \"datasetContactNotAllowed\",",
                BundleUtil.getStringFromBundle("schema.validation.exception.dataset.invalidType", List.of("datasetContact", "datasetContactNotAllowed", "datasetContactName, datasetContactAffiliation, datasetContactEmail"))
        );

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());
    }
    private void testDatasetSchemaValidationHelper(String dataverseAlias, String apiToken, String origString, String replacementString, String expectedError) {
        String json = UtilIT.getDatasetJson("scripts/search/tests/data/dataset-finch3.json");
        json = json.replace(origString, replacementString);
        Response validateDatasetJsonResponse = UtilIT.validateDatasetJson(dataverseAlias, json, apiToken);
        validateDatasetJsonResponse.prettyPrint();
        validateDatasetJsonResponse.then().assertThat()
                .statusCode(200)
                .body(containsString(expectedError));
    }

    @Test
    public void testCreateDataset() {

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
        assertEquals(10, identifier.length());

        Response deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());
        
        // Now, let's allow anyone with a Dataverse account (any "random user") 
        // to create datasets in this dataverse: 
        
        Response grantRole = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.DS_CONTRIBUTOR, AuthenticatedUsers.get().getIdentifier(), apiToken);
        grantRole.prettyPrint();
        assertEquals(OK.getStatusCode(), grantRole.getStatusCode());
        // Test duplicate grant
        grantRole = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.DS_CONTRIBUTOR, AuthenticatedUsers.get().getIdentifier(), apiToken);
        grantRole.prettyPrint();
        grantRole.then().assertThat()
                .body("message", containsString(BundleUtil.getStringFromBundle("datasets.api.grant.role.assignee.has.role.error")))
                .statusCode(FORBIDDEN.getStatusCode());

        // Create another random user: 
        
        Response createRandomUser = UtilIT.createRandomUser();
        createRandomUser.prettyPrint();
        String randomUsername = UtilIT.getUsernameFromResponse(createRandomUser);
        String randomUserApiToken = UtilIT.getApiTokenFromResponse(createRandomUser);
        
        // This random user should be able to create a dataset in the dataverse 
        // above, because we've set it up so, right? - Not exactly: the dataverse
        // hasn't been published yet! So if this random user tries to create 
        // a dataset now, it should fail: 
        /* - this test removed because the perms for create dataset have been reverted
        createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, randomUserApiToken);
        createDatasetResponse.prettyPrint();
        assertEquals(UNAUTHORIZED.getStatusCode(), createDatasetResponse.getStatusCode());
        */
        // Now, let's publish this dataverse...
        
        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(OK.getStatusCode(), publishDataverse.getStatusCode());

        // Return a short sleep here
        //without it we have seen some database deadlocks SEK 09/13/2019

        try {
            Thread.sleep(1000l);
        } catch (InterruptedException iex) {}

        // ... And now that it's published, try to create a dataset again, 
        // as the "random", not specifically authorized user: 
        // (this time around, it should work!)
        
        createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, randomUserApiToken);
        createDatasetResponse.prettyPrint();
        datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());

        // OK, let's delete this dataset as well, and then delete the dataverse...
        
        deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());
        
        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

    }

    @Test
    public void testAddUpdateDatasetViaNativeAPI() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        JsonArrayBuilder metadataBlocks = Json.createArrayBuilder();
        metadataBlocks.add("citation");
        metadataBlocks.add("journal");
        metadataBlocks.add("socialscience");
        Response setMetadataBlocksResponse = UtilIT.setMetadataBlocks(dataverseAlias, metadataBlocks, apiToken);
        setMetadataBlocksResponse.prettyPrint();
        setMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        Response datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());

        String identifier = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");

        //Test Add Data

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

        RestAssured.registerParser("text/plain", Parser.JSON);
        Response exportDatasetAsJson = UtilIT.exportDataset(datasetPersistentId, "dataverse_json", apiToken);
        exportDatasetAsJson.prettyPrint();

        pathToJsonFile = "doc/sphinx-guides/source/_static/api/dataset-add-subject-metadata.json";
        addSubjectViaNative = UtilIT.addDatasetMetadataViaNative(datasetPersistentId, pathToJsonFile, apiToken);
        addSubjectViaNative.prettyPrint();
        addSubjectViaNative.then().assertThat()
                .statusCode(OK.getStatusCode()).body(containsString("Mathematical Sciences"));

        String pathToJsonFileSingle = "doc/sphinx-guides/source/_static/api/dataset-simple-update-metadata.json";
        Response addSubjectSingleViaNative = UtilIT.updateFieldLevelDatasetMetadataViaNative(datasetPersistentId, pathToJsonFileSingle, apiToken);
        String responseString = addSubjectSingleViaNative.prettyPrint();
        addSubjectSingleViaNative.then().assertThat()
                .statusCode(OK.getStatusCode()).body(containsString("Mathematical Sciences")).body(containsString("Social Sciences"));

        String pathToJsonFileSingleCvoc = "doc/sphinx-guides/source/_static/api/dataset-add-single-cvoc-field-metadata.json";
        Response addSingleCvocViaNative = UtilIT.updateFieldLevelDatasetMetadataViaNative(datasetPersistentId, pathToJsonFileSingleCvoc, apiToken);
        addSingleCvocViaNative.prettyPrint();
        addSingleCvocViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());

        String pathToJsonFileSingleCompound = "doc/sphinx-guides/source/_static/api/dataset-add-single-compound-field-metadata.json";
        Response addSingleCompoundViaNative = UtilIT.updateFieldLevelDatasetMetadataViaNative(datasetPersistentId, pathToJsonFileSingleCompound, apiToken);
        addSingleCompoundViaNative.prettyPrint();
        addSingleCompoundViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());

        //Trying to blank out required field should fail...
        String pathToJsonFileBadData = "doc/sphinx-guides/source/_static/api/dataset-update-with-blank-metadata.json";
        Response deleteTitleViaNative = UtilIT.updateFieldLevelDatasetMetadataViaNative(datasetPersistentId, pathToJsonFileBadData, apiToken);
        deleteTitleViaNative.prettyPrint();
        String emptyRequiredFieldError = BundleUtil.getStringFromBundle("datasetFieldValidator.error.emptyRequiredSingleValueForField", List.of("Title"));
        deleteTitleViaNative.then().assertThat().body("message", equalTo(BundleUtil.getStringFromBundle("updateDatasetFieldsCommand.api.processDatasetUpdate.parseError", List.of(emptyRequiredFieldError))));


        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());
        UtilIT.sleepForLock(datasetPersistentId, "finalizePublication", apiToken, UtilIT.MAXIMUM_PUBLISH_LOCK_DURATION);
        //post publish update
        String pathToJsonFilePostPub= "doc/sphinx-guides/source/_static/api/dataset-add-metadata-after-pub.json";
        Response addDataToPublishedVersion = UtilIT.addDatasetMetadataViaNative(datasetPersistentId, pathToJsonFilePostPub, apiToken);
        addDataToPublishedVersion.prettyPrint();
        addDataToPublishedVersion.then().assertThat().statusCode(OK.getStatusCode());

        publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);

        //post publish update
        String pathToJsonFileBadDataSubtitle = "doc/sphinx-guides/source/_static/api/dataset-edit-metadata-subtitle.json";
        Response addDataToBadData = UtilIT.updateFieldLevelDatasetMetadataViaNative(datasetPersistentId, pathToJsonFileBadDataSubtitle, apiToken);
        addDataToBadData.prettyPrint();

        addDataToBadData.then().assertThat()
                .body("message", equalToCI("Error parsing dataset update: Invalid value submitted for Subtitle. It should be a single value."))
                .statusCode(400);

        addSubjectViaNative = UtilIT.addDatasetMetadataViaNative(datasetPersistentId, pathToJsonFile, apiToken);
        addSubjectViaNative.prettyPrint();
        addSubjectViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());

        String pathToJsonDeleteFile = "doc/sphinx-guides/source/_static/api/dataset-delete-subject-metadata.json";
        addSubjectViaNative = UtilIT.deleteDatasetMetadataViaNative(datasetPersistentId, pathToJsonDeleteFile, apiToken);
        addSubjectViaNative.prettyPrint();
        addSubjectViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());

        pathToJsonDeleteFile = "doc/sphinx-guides/source/_static/api/dataset-delete-author-metadata.json";
        addSubjectViaNative = UtilIT.deleteDatasetMetadataViaNative(datasetPersistentId, pathToJsonDeleteFile, apiToken);
        addSubjectViaNative.prettyPrint();
        addSubjectViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());

        pathToJsonDeleteFile = "doc/sphinx-guides/source/_static/api/dataset-delete-author-no-match.json";
        addSubjectViaNative = UtilIT.deleteDatasetMetadataViaNative(datasetPersistentId, pathToJsonDeleteFile, apiToken);
        addSubjectViaNative.prettyPrint();
        addSubjectViaNative.then().assertThat().body("message", equalTo("Delete metadata failed: Author: Spruce, Sabrina not found."))
                .statusCode(400);

        publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());
        //6078
        String pathToJsonFileEditPostPub = "doc/sphinx-guides/source/_static/api/dataset-edit-metadata-after-pub.json";
        Response editPublishedVersion = UtilIT.updateFieldLevelDatasetMetadataViaNative(datasetPersistentId, pathToJsonFileEditPostPub, apiToken);
        editPublishedVersion.prettyPrint();
        editPublishedVersion.then().assertThat().statusCode(OK.getStatusCode());

        publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        //"Delete metadata failed: " + updateField.getDatasetFieldType().getDisplayName() + ": " + displayValue + " not found."

        // Test controlled vocabulary optional field removal

        // Step 1 - Set controlled vocabulary field

        String jsonString = """
        {
          "fields": [
            {
              "typeName": "author",
              "value": [
                {
                  "authorName": {
                    "typeName": "authorName",
                    "value": "Belicheck, Bill"
                  },
                  "authorAffiliation": {
                    "typeName": "authorIdentifierScheme",
                    "value": "ORCID"
                  }
                }
              ]
            }
          ]
        }
        """;

        Response updateMetadataAddAuthorWithOptionalCvv = UtilIT.editVersionMetadataFromJsonStr(datasetPersistentId, jsonString, apiToken);
        updateMetadataAddAuthorWithOptionalCvv.then().assertThat()
                .body("data.metadataBlocks.citation.fields[2].value[0].authorIdentifierScheme.value", equalTo("ORCID"))
                .statusCode(OK.getStatusCode());

        // Step 2 - Remove controlled vocabulary field

        jsonString = """
        {
          "fields": [
            {
              "typeName": "author",
              "value": [
                {
                  "authorName": {
                    "typeName": "authorName",
                    "value": "Belicheck, Bill"
                  },
                  "authorAffiliation": {
                    "typeName": "authorIdentifierScheme",
                    "value": ""
                  }
                }
              ]
            }
          ]
        }
        """;

        Response updateMetadataRemoveOptionalCvv = UtilIT.editVersionMetadataFromJsonStr(datasetPersistentId, jsonString, apiToken);
        updateMetadataRemoveOptionalCvv.then().assertThat()
                .body("data.metadataBlocks.citation.fields[2].value[0].authorIdentifierScheme", equalTo(null))
                .statusCode(OK.getStatusCode());

        // Test optional compound field entire removal

        // Step 1 - Set optional compound field

        jsonString = """
        {
          "fields": [
            {
              "typeName": "distributor",
              "multiple": true,
              "typeClass": "compound",
              "value": [
                {
                  "distributorName": {
                    "typeName": "distributorName",
                    "multiple": false,
                    "typeClass": "primitive",
                    "value": "LastDistributor1, FirstDistributor1"
                  },
                  "distributorAffiliation": {
                    "typeName": "distributorAffiliation",
                    "multiple": false,
                    "typeClass": "primitive",
                    "value": "DistributorAffiliation1"
                  }
                }
              ]
            }
          ]
        }
        """;

        Response updateMetadataAddDistributor = UtilIT.editVersionMetadataFromJsonStr(datasetPersistentId, jsonString, apiToken);
        updateMetadataAddDistributor.then().assertThat()
                .body("data.metadataBlocks.citation.fields[7].typeName", equalTo("distributor"))
                .body("data.metadataBlocks.citation.fields[7].value[0].distributorAffiliation.value", equalTo("DistributorAffiliation1"))
                .statusCode(OK.getStatusCode());

        // Step 2 - Remove optional compound field

        jsonString = """
        {
          "fields": [
            {
              "typeName": "distributor",
              "multiple": true,
              "typeClass": "compound",
              "value": [
                {
                  "distributorName": {
                    "typeName": "distributorName",
                    "multiple": false,
                    "typeClass": "primitive",
                    "value": ""
                  },
                  "distributorAffiliation": {
                    "typeName": "distributorAffiliation",
                    "multiple": false,
                    "typeClass": "primitive",
                    "value": ""
                  }
                }
              ]
            }
          ]
        }
        """;

        Response updateMetadataRemoveDistributor = UtilIT.editVersionMetadataFromJsonStr(datasetPersistentId, jsonString, apiToken);
        updateMetadataRemoveDistributor.then().assertThat()
                .body("data.metadataBlocks.citation.fields[7].typeName", not(equalTo("distributor")))
                .statusCode(OK.getStatusCode());

        // Test multiple field removal

        // Step 1 - Set optional multiple field

        jsonString = """
        {
          "fields": [
            {
              "typeName": "alternativeTitle",
              "multiple": true,
              "typeClass": "primitive",
              "value": ["Alternative1","Alternative2"]
            }
          ]
        }
        """;

        Response updateMetadataAddAlternativeTitles = UtilIT.editVersionMetadataFromJsonStr(datasetPersistentId, jsonString, apiToken);
        updateMetadataAddAlternativeTitles.then().assertThat()
                .body("data.metadataBlocks.citation.fields[2].typeName", equalTo("alternativeTitle"))
                .statusCode(OK.getStatusCode());

        // Step 2 - Remove optional multiple field

        jsonString = """
        {
          "fields": [
            {
              "typeName": "alternativeTitle",
              "multiple": true,
              "typeClass": "primitive",
              "value": []
            }
          ]
        }
        """;

        Response updateMetadataRemoveAlternativeTitles = UtilIT.editVersionMetadataFromJsonStr(datasetPersistentId, jsonString, apiToken);
        updateMetadataRemoveAlternativeTitles.prettyPrint();
        updateMetadataRemoveAlternativeTitles.then().assertThat()
                .body("data.metadataBlocks.citation.fields[2].typeName", not(equalTo("alternativeTitle")))
                .statusCode(OK.getStatusCode());

        // Test sourceLastUpdateTime optional query parameter
        String sourceLastUpdateTime =  updateMetadataRemoveAlternativeTitles.then().extract().path("data.lastUpdateTime");
        assertNotNull(sourceLastUpdateTime);
        String oldTimestamp = "2025-04-25T13:58:28Z";

        // Case 1 - Pass outdated internal version number

        Response updateMetadataWithOutdatedInternalVersionNumber = UtilIT.editVersionMetadataFromJsonStr(datasetPersistentId, jsonString, apiToken, oldTimestamp);
        updateMetadataWithOutdatedInternalVersionNumber.then().assertThat()
                .body("message", equalTo(BundleUtil.getStringFromBundle("abstractApiBean.error.internalVersionTimestampIsOutdated", Collections.singletonList(oldTimestamp))))
                .statusCode(BAD_REQUEST.getStatusCode());

        // Case 2 - Pass latest internal version number

        Response updateMetadataWithLatestInternalVersionNumber = UtilIT.editVersionMetadataFromJsonStr(datasetPersistentId, jsonString, apiToken, sourceLastUpdateTime);
        updateMetadataWithLatestInternalVersionNumber.then().assertThat()
                .statusCode(OK.getStatusCode());
    }
    
    @Test
    public void testAddEmptyDatasetViaNativeAPI() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);        
        
        String pathToJsonFile = "scripts/search/tests/data/emptyDataset.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDatasetResponse.prettyPrint();

        createDatasetResponse.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", startsWith("Validation Failed: "));  
        
        pathToJsonFile = "scripts/search/tests/data/datasetMissingReqFields.json";        
        createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDatasetResponse.prettyPrint();
        
        createDatasetResponse.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", startsWith("Validation Failed: "));
 
    }

    /**
     * This test requires the root dataverse to be published to pass.
     */
    @Test
    public void testCreatePublishDestroyDataset() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());
        
        Response createNoAccessUser = UtilIT.createRandomUser();
        createNoAccessUser.prettyPrint();
        String apiTokenNoAccess= UtilIT.getApiTokenFromResponse(createNoAccessUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonBeforePublishing.prettyPrint();
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;
        
        Response datasetAsJsonNoAccess = UtilIT.nativeGet(datasetId, apiTokenNoAccess);
        datasetAsJsonNoAccess.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());        
 
        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());
        Response attemptToPublishZeroDotOne = UtilIT.publishDatasetViaNativeApiDeprecated(datasetPersistentId, "minor", apiToken);
        logger.info("Attempting to publish a minor (\"zero-dot-one\") version");
        attemptToPublishZeroDotOne.prettyPrint();
        attemptToPublishZeroDotOne.then().assertThat()
                .body("message", equalTo("Cannot publish as minor version. Re-try as major release."))
                .statusCode(403);
        
        logger.info("Attempting to publish a major version");
        // Return random sleep  9/13/2019
        // Without it we've seen some DB deadlocks
        // 3 second sleep, to allow the indexing to finish:

        try {
            Thread.sleep(3000l);
        } catch (InterruptedException iex) {}

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());

        Response getDatasetJsonAfterPublishing = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonAfterPublishing.prettyPrint();
        getDatasetJsonAfterPublishing.then().assertThat()
                .body("data.latestVersion.versionNumber", equalTo(1))
                .body("data.latestVersion.versionMinorNumber", equalTo(0))
                // FIXME: make this less brittle by removing "2" and "0". See also test below.
                .body("data.latestVersion.metadataBlocks.citation.fields[2].value[0].datasetContactEmail.value", equalTo("finch@mailinator.com"))
                .statusCode(OK.getStatusCode());
        
        Response datasetAsJsonNoAccessPostPublish = UtilIT.nativeGet(datasetId, apiTokenNoAccess);
        datasetAsJsonNoAccessPostPublish.then().assertThat()
                .statusCode(OK.getStatusCode());        
        assertTrue(datasetAsJsonNoAccessPostPublish.body().asString().contains(identifier));

        List<JsonObject> datasetContactsFromNativeGet = with(getDatasetJsonAfterPublishing.body().asString()).param("datasetContact", "datasetContact")
                .getJsonObject("data.latestVersion.metadataBlocks.citation.fields.findAll { fields -> fields.typeName == datasetContact }");
        Map firstDatasetContactFromNativeGet = datasetContactsFromNativeGet.get(0);
        assertTrue(firstDatasetContactFromNativeGet.toString().contains("finch@mailinator.com"));

        RestAssured.registerParser("text/plain", Parser.JSON);
        Response exportDatasetAsJson = UtilIT.exportDataset(datasetPersistentId, "dataverse_json", apiToken);
        exportDatasetAsJson.prettyPrint();
        exportDatasetAsJson.then().assertThat()
                .body("datasetVersion.metadataBlocks.citation.fields[2].value[0].datasetContactEmail.value", equalTo("finch@mailinator.com"))
                .statusCode(OK.getStatusCode());
        RestAssured.unregisterParser("text/plain");

        // FIXME: It would be awesome if we could just get a JSON object back instead. :(
        Map<String, Object> datasetContactFromExport = with(exportDatasetAsJson.body().asString()).param("datasetContact", "datasetContact")
                .getJsonObject("datasetVersion.metadataBlocks.citation.fields.find { fields -> fields.typeName == datasetContact }");
        logger.info("datasetContactFromExport: " + datasetContactFromExport);

        assertEquals("datasetContact", datasetContactFromExport.get("typeName"));
        List valuesArray = (ArrayList) datasetContactFromExport.get("value");
        // FIXME: it's brittle to rely on the first value but what else can we do, given our API?
        Map<String, Object> firstValue = (Map<String, Object>) valuesArray.get(0);
//        System.out.println("firstValue: " + firstValue);
        Map firstValueMap = (HashMap) firstValue.get("datasetContactEmail");
//        System.out.println("firstValueMap: " + firstValueMap);
        assertEquals("finch@mailinator.com", firstValueMap.get("value"));
        assertTrue(datasetContactFromExport.toString().contains("finch@mailinator.com"));
        assertTrue(firstValue.toString().contains("finch@mailinator.com"));

        Response getDatasetVersion = UtilIT.getDatasetVersion(datasetPersistentId, DS_VERSION_LATEST_PUBLISHED, apiToken);
        getDatasetVersion.prettyPrint();
        getDatasetVersion.then().assertThat()
                .body("data.datasetId", equalTo(datasetId))
                .body("data.datasetPersistentId", equalTo(datasetPersistentId))
                .statusCode(OK.getStatusCode());

        Response citationBlock = UtilIT.getMetadataBlockFromDatasetVersion(datasetPersistentId, null, null, apiToken);
        citationBlock.prettyPrint();
        citationBlock.then().assertThat()
                .body("data.fields[2].value[0].datasetContactEmail.value", equalTo("finch@mailinator.com"))
                .statusCode(OK.getStatusCode());

        Response exportFail = UtilIT.exportDataset(datasetPersistentId, "noSuchExporter", apiToken);
        exportFail.prettyPrint();
        exportFail.then().assertThat()
                .body("message", equalTo("Export Failed"))
                .statusCode(FORBIDDEN.getStatusCode());

        Response exportDatasetAsDublinCore = UtilIT.exportDataset(datasetPersistentId, "oai_dc", apiToken);
        exportDatasetAsDublinCore.prettyPrint();
        exportDatasetAsDublinCore.then().assertThat()
                .body("oai_dc.title", is("Darwin's Finches"))
                .statusCode(OK.getStatusCode());

        Response exportDatasetAsDdi = UtilIT.exportDataset(datasetPersistentId, "ddi", apiToken);
        exportDatasetAsDdi.prettyPrint();
        exportDatasetAsDdi.then().assertThat()
                .statusCode(OK.getStatusCode());

        /**
         * The Native API allows you to create a dataset contact with an email
         * but no name. The email should appear in the DDI export. SWORD may
         * have the same behavior. Untested. This smells like a bug.
         */
        boolean nameRequiredForContactToAppear = true;
        if (nameRequiredForContactToAppear) {
            assertEquals("Finch, Fiona", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.citation.distStmt.contact"));
        } else {
            assertEquals("finch@mailinator.com", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.citation.distStmt.contact.@email"));
        }
        assertEquals(datasetPersistentId, XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.docDscr.citation.titlStmt.IDNo"));

        // Test includeDeaccessioned option
        Response deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, "Test deaccession reason.", null, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // includeDeaccessioned false
        getDatasetVersion = UtilIT.getDatasetVersion(datasetPersistentId, DS_VERSION_LATEST_PUBLISHED, apiToken, false, false);
        getDatasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // includeDeaccessioned true
        getDatasetVersion = UtilIT.getDatasetVersion(datasetPersistentId, DS_VERSION_LATEST_PUBLISHED, apiToken, false, true);
        getDatasetVersion.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDatasetResponse = UtilIT.destroyDataset(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());

        // Start of test of deleting a file from a deaccessioned version.

        // Create Dataset for deaccession test.
        Response deaccessionTestDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        deaccessionTestDataset.prettyPrint();
        deaccessionTestDataset.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer deaccessionTestDatasetId = UtilIT.getDatasetIdFromResponse(deaccessionTestDataset);
        
        // File upload for deaccession test.
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";   
        Response uploadResponse = UtilIT.uploadFileViaNative(deaccessionTestDatasetId.toString(), pathToFile, apiToken);
        uploadResponse.prettyPrint();
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());
        Integer deaccessionTestFileId = JsonPath.from(uploadResponse.body().asString()).getInt("data.files[0].dataFile.id");

        // Publish Dataset for deaccession test.
        Response deaccessionTestPublishResponse = UtilIT.publishDatasetViaNativeApi(deaccessionTestDatasetId, "major", apiToken);
        deaccessionTestPublishResponse.prettyPrint();

        // Deaccession Dataset for deaccession test.
        Response deaccessionTestDatasetResponse = UtilIT.deaccessionDataset(deaccessionTestDatasetId, DS_VERSION_LATEST_PUBLISHED, "Test deaccession reason.", null, apiToken);
        deaccessionTestDatasetResponse.prettyPrint();
        deaccessionTestDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Version check for deaccession test - Deaccessioned.
        Response deaccessionTestVersions = UtilIT.getDatasetVersions(deaccessionTestDatasetId.toString(), apiToken);
        deaccessionTestVersions.prettyPrint();
        deaccessionTestVersions.then().assertThat()
                .body("data[0].latestVersionPublishingState", equalTo("DEACCESSIONED"))
                .statusCode(OK.getStatusCode());

        // File deletion / Draft creation due diligence check for deaccession test.
        Response deaccessionTestDeleteFile =  UtilIT.deleteFileInDataset(deaccessionTestFileId, apiToken);
        deaccessionTestDeleteFile.prettyPrint();
        deaccessionTestDeleteFile
                .then().assertThat()
                .statusCode(OK.getStatusCode());
                
        // Version check for deaccession test - Draft.
        deaccessionTestVersions = UtilIT.getDatasetVersions(deaccessionTestDatasetId.toString(), apiToken);
        deaccessionTestVersions.prettyPrint();
        deaccessionTestVersions.then().assertThat()
                .body("data[0].latestVersionPublishingState", equalTo("DRAFT"))
                .statusCode(OK.getStatusCode());

        // Deleting Dataset for deaccession test.
        Response deaccessionTestDelete = UtilIT.destroyDataset(deaccessionTestDatasetId, apiToken);
        deaccessionTestDelete.prettyPrint();
        deaccessionTestDelete.then()
                .assertThat()
                .statusCode(OK.getStatusCode());
        
        //  End of deaccession test.

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

    }

    @Test
    public void testHideMetadataBlocksInDatasetVersionsAPI() {

        // Create user
        String apiToken = UtilIT.createRandomUserGetToken();

        // Create user with no permission
        String apiTokenNoPerms = UtilIT.createRandomUserGetToken();

        // Create Collection
        String collectionAlias = UtilIT.createRandomCollectionGetAlias(apiToken);

        // Create Dataset
        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(collectionAlias, apiToken);
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        // Now check that the metadata is NOT shown, when we ask the versions api to dos o.
        boolean excludeMetadata = true;
        Response unpublishedDraft = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_DRAFT, apiToken, true,excludeMetadata, false);
        unpublishedDraft.prettyPrint();
        unpublishedDraft.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.metadataBlocks", equalTo(null));

        // Now check that the metadata is shown, when we ask the versions api to dos o.
        excludeMetadata = false;
        unpublishedDraft = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_DRAFT, apiToken,true, excludeMetadata, false);
        unpublishedDraft.prettyPrint();
        unpublishedDraft.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.metadataBlocks", notNullValue() );
    }
    /**
     * The apis (/api/datasets/{id}/versions and /api/datasets/{id}/versions/{vid}
     * are already called from other RestAssured tests, in this class and also in FilesIT. 
     * But this test is dedicated to this api specifically, and focuses on the 
     * functionality added to it in 6.1. 
    */
    @Test
    public void testDatasetVersionsAPI() {
        
        // Create user
        String apiToken = UtilIT.createRandomUserGetToken();

        // Create user with no permission
        String apiTokenNoPerms = UtilIT.createRandomUserGetToken();

        // Create Collection
        String collectionAlias = UtilIT.createRandomCollectionGetAlias(apiToken);

        // Create Dataset
        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(collectionAlias, apiToken);
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        // Upload file
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());
        
        // Check that the file we just uploaded is shown by the versions api:
        Response unpublishedDraft = UtilIT.getDatasetVersion(datasetPid, ":draft", apiToken);
        unpublishedDraft.prettyPrint();
        unpublishedDraft.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        // Now check that the file is NOT shown, when we ask the versions api to 
        // skip files: 
        boolean excludeFiles = true; 
        unpublishedDraft = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_DRAFT, apiToken, excludeFiles, false);
        unpublishedDraft.prettyPrint();
        unpublishedDraft.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files", equalTo(null));

        unpublishedDraft = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_DRAFT, apiTokenNoPerms, excludeFiles, false);
        unpublishedDraft.prettyPrint();
        unpublishedDraft.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());
        
        excludeFiles = false; 
        unpublishedDraft = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_DRAFT, apiToken, excludeFiles, false);
        unpublishedDraft.prettyPrint();
        unpublishedDraft.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files.size()", equalTo(1));

        unpublishedDraft = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_DRAFT, apiTokenNoPerms, excludeFiles, false);
        unpublishedDraft.prettyPrint();
        unpublishedDraft.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());


        // Publish collection and dataset
        UtilIT.publishDataverseViaNativeApi(collectionAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());

        //Set of tests on non-deaccesioned dataset 
        String specificVersion = "1.0";        
        boolean includeDeaccessioned = false;
        Response datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.versionState", equalTo("RELEASED"))
                .body("data.latestVersionPublishingState", equalTo("RELEASED"));

        // Upload another file: 
        String pathToFile2 = "src/main/webapp/resources/images/cc0.png";
        Response uploadResponse2 = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile2, apiToken);
        uploadResponse2.prettyPrint();
        uploadResponse2.then().assertThat().statusCode(OK.getStatusCode());

        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.versionState", equalTo("DRAFT"))
                .body("data.latestVersionPublishingState", equalTo("DRAFT"));

        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.versionState", equalTo("RELEASED"))
                .body("data.latestVersionPublishingState", equalTo("DRAFT"));
       
        // We should now have a published version, and a draft. 
        
        // Call /versions api, *with the owner api token*, make sure both 
        // versions are listed; also check that the correct numbers of files 
        // are shown in each version (2 in the draft, 1 in the published). 
        Response versionsResponse = UtilIT.getDatasetVersions(datasetPid, apiToken);
        versionsResponse.prettyPrint();
        versionsResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(2))
                .body("data[0].files.size()", equalTo(2))
                .body("data[1].files.size()", equalTo(1));


        // Now call this api with the new (as of 6.1) pagination parameters
        Integer offset = 0;
        Integer howmany = 1;
        versionsResponse = UtilIT.getDatasetVersions(datasetPid, apiToken, offset, howmany);
        // (the above should return only one version, the draft)
        versionsResponse.prettyPrint();
        versionsResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(1))
                .body("data.versionState[0]", equalTo("DRAFT"))
                .body("data[0].files.size()", equalTo(2));
                
        // And now call it with an un-privileged token, to make sure only one 
        // (the published) version is shown:    
        versionsResponse = UtilIT.getDatasetVersions(datasetPid, apiTokenNoPerms);
        versionsResponse.prettyPrint();
        versionsResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.versionState[0]", not("DRAFT"))
                .body("data.size()", equalTo(1));
        
        // And now call the "short", no-files version of the same api
        excludeFiles = true;
        versionsResponse = UtilIT.getDatasetVersions(datasetPid, apiTokenNoPerms, excludeFiles);
        versionsResponse.prettyPrint();
        versionsResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].files", equalTo(null));


        

        
        
        excludeFiles = true;
        //Latest published authorized token
        //Latest published requested, draft exists and user has access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("RELEASED"))
            .body("data.files", equalTo(null));

        //Latest published unauthorized token
        //Latest published requested, draft exists but user doesn't have access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("RELEASED"))
            .body("data.files", equalTo(null));

        //Latest authorized token
        //Latest requested, draft exists and user has access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DRAFT"))
            .body("data.files", equalTo(null));

        //Latest unauthorized token
        //Latest requested, draft exists but user doesn't have access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("RELEASED"))
            .body("data.files", equalTo(null));

        //Specific version authorized token
        //Specific version requested, draft exists and user has access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("RELEASED"))
            .body("data.files", equalTo(null));

        //Specific version unauthorized token
        //Specific version requested, draft exists but user doesn't have access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("RELEASED"))
            .body("data.files", equalTo(null));

        excludeFiles = false;
        
        //Latest published authorized token
        //Latest published requested, draft exists and user has access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("RELEASED"))
            .body("data.files.size()", equalTo(1));

        //Latest published unauthorized token
        //Latest published requested, draft exists but user doesn't have access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("RELEASED"))
            .body("data.files.size()", equalTo(1));

        //Latest authorized token, user is authenticated should get the Draft version
        //Latest requested, draft exists and user has access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DRAFT"))
            .body("data.files.size()", equalTo(2));

        //Latest unauthorized token, user has no permissions should get the latest Published version
        //Latest requested, draft exists but user doesn't have access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("RELEASED"))
            .body("data.files.size()", equalTo(1));

        //Specific version authorized token
        //Specific version requested, draft exists and user has access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("RELEASED"))
            .body("data.files.size()", equalTo(1));

        //Specific version unauthorized token
        //Specific version requested, draft exists but user doesn't have access to draft
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("RELEASED"))
            .body("data.files.size()", equalTo(1));

        //We deaccession the dataset
        Response deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, "Test deaccession reason.", null, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        //Set of tests on deaccesioned dataset, only 3/9 should return OK message

        includeDeaccessioned = true;
        excludeFiles = false;

        //Latest published authorized token with deaccessioned dataset
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DEACCESSIONED"))
            .body("data.files.size()", equalTo(1));

        //Latest published requesting files, one version is DEACCESSIONED the second is DRAFT so shouldn't get any datasets
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        //Latest authorized token should get the DRAFT version
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DRAFT"))
            .body("data.files.size()", equalTo(2));

        //Latest unauthorized token requesting files, one version is DEACCESSIONED the second is DRAFT so shouldn't get any datasets
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        //Specific version authorized token
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DEACCESSIONED"))
            .body("data.files.size()", equalTo(1));

        //Specific version unauthorized token requesting files, one version is DEACCESSIONED the second is DRAFT so shouldn't get any datasets.
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        excludeFiles = true;

        //Latest published exclude files authorized token with deaccessioned dataset
        //Latest published requested, that version was deaccessioned but a draft exist and the user has access to it.
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DEACCESSIONED"))
            .body("data.files", equalTo(null));

        //Latest published exclude files, should get the DEACCESSIONED version
        //Latest published requested, that version was deaccessioned but a draft exist but the user doesn't have access to it.
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DEACCESSIONED"))
            .body("data.files", equalTo(null));

        //Latest authorized token should get the DRAFT version with no files
        //Latest requested there is a draft and the user has access to it.
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DRAFT"))
            .body("data.files", equalTo(null));

        //Latest unauthorized token excluding files, one version is DEACCESSIONED the second is DRAFT so shouldn't get any datasets
        //Latest requested and latest version is deaccessioned and the user doesn't have access to the draft.
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DEACCESSIONED"))
            .body("data.files", equalTo(null));

        //Specific version authorized token
        //Specific version requested (deaccesioned), the latest version is on draft amd the user has access to it.
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DEACCESSIONED"))
            .body("data.files", equalTo(null));

        //Specific version unauthorized token requesting files, one version is DEACCESSIONED the second is DRAFT so shouldn't get any datasets.
        //Specific version requested (deaccesioned), the latest version is on draft but the user doesn't have access to it.
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DEACCESSIONED"))
            .body("data.files", equalTo(null));

        //Set of test when we have a deaccessioned dataset but we don't include deaccessioned
        includeDeaccessioned = false;
        excludeFiles = false;

        //Latest published authorized token with deaccessioned dataset not included
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        //Latest published unauthorized token with deaccessioned dataset not included
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        //Latest authorized token should get the DRAFT version
        //Latest version requested, the user has access to the draft.
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DRAFT"))
            .body("data.files.size()", equalTo(2));

        //Latest unauthorized token one version is DEACCESSIONED the second is DRAFT so shouldn't get any datasets
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        //Specific version authorized token, the version is DEACCESSIONED so shouldn't get any datasets
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        //Specific version unauthorized token, the version is DEACCESSIONED so shouldn't get any datasets
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        excludeFiles = true;

        //Latest published authorized token with deaccessioned dataset not included
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        //Latest published unauthorized token with deaccessioned dataset not included
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST_PUBLISHED, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        //Latest authorized token should get the DRAFT version
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(OK.getStatusCode())
            .body("data.versionState", equalTo("DRAFT"))
            .body("data.files", equalTo(null));

        //Latest unauthorized token one version is DEACCESSIONED the second is DRAFT so shouldn't get any datasets
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_LATEST, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        //Specific version authorized token, the version is DEACCESSIONED so shouldn't get any datasets
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiToken, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        //Specific version unauthorized token, the version is DEACCESSIONED so shouldn't get any datasets
        datasetVersion = UtilIT.getDatasetVersion(datasetPid, specificVersion, apiTokenNoPerms, excludeFiles, includeDeaccessioned);
        datasetVersion.prettyPrint();
        datasetVersion.then().assertThat().statusCode(NOT_FOUND.getStatusCode());
       
    }

    
    /**
     * This test requires the root dataverse to be published to pass.
     */
    @Test
    public void testExport() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String pathToJsonFile = "scripts/api/data/dataset-create-new.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonBeforePublishing.prettyPrint();
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());
        Response attemptToPublishZeroDotOne = UtilIT.publishDatasetViaNativeApiDeprecated(datasetPersistentId, "minor", apiToken);
        attemptToPublishZeroDotOne.prettyPrint();
        attemptToPublishZeroDotOne.then().assertThat()
                .body("message", equalTo("Cannot publish as minor version. Re-try as major release."))
                .statusCode(403);

        logger.info("In testExport; attempting to publish, as major version");
        //Return random sleep  9/13/2019
        // 3 second sleep, to allow the indexing to finish: 
        // Without it we've seen som DB dealocks

        try {
            Thread.sleep(3000l);
        } catch (InterruptedException iex) {}

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());

        Response getDatasetJsonAfterPublishing = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonAfterPublishing.prettyPrint();
        getDatasetJsonAfterPublishing.then().assertThat()
                .body("data.latestVersion.versionNumber", equalTo(1))
                .body("data.latestVersion.versionMinorNumber", equalTo(0))
                // FIXME: make this less brittle by removing "2" and "0". See also test below.
                .body("data.latestVersion.metadataBlocks.citation.fields[2].value[0].datasetContactEmail.value", equalTo("sammi@sample.com"))
                .statusCode(OK.getStatusCode());

        List<JsonObject> datasetContactsFromNativeGet = with(getDatasetJsonAfterPublishing.body().asString()).param("datasetContact", "datasetContact")
                .getJsonObject("data.latestVersion.metadataBlocks.citation.fields.findAll { fields -> fields.typeName == datasetContact }");
        Map firstDatasetContactFromNativeGet = datasetContactsFromNativeGet.get(0);
        assertTrue(firstDatasetContactFromNativeGet.toString().contains("sammi@sample.com"));

        RestAssured.registerParser("text/plain", Parser.JSON);
        Response exportDatasetAsJson = UtilIT.exportDataset(datasetPersistentId, "dataverse_json", apiToken);
        exportDatasetAsJson.prettyPrint();
        exportDatasetAsJson.then().assertThat()
                .body("datasetVersion.metadataBlocks.citation.fields[2].value[0].datasetContactEmail.value", equalTo("sammi@sample.com"))
                .statusCode(OK.getStatusCode());
        RestAssured.unregisterParser("text/plain");

        // FIXME: It would be awesome if we could just get a JSON object back instead. :(
        Map<String, Object> datasetContactFromExport = with(exportDatasetAsJson.body().asString()).param("datasetContact", "datasetContact")
                .getJsonObject("datasetVersion.metadataBlocks.citation.fields.find { fields -> fields.typeName == datasetContact }");
        logger.info("datasetContactFromExport: " + datasetContactFromExport);

        assertEquals("datasetContact", datasetContactFromExport.get("typeName"));
        List valuesArray = (ArrayList) datasetContactFromExport.get("value");
        // FIXME: it's brittle to rely on the first value but what else can we do, given our API?
        Map<String, Object> firstValue = (Map<String, Object>) valuesArray.get(0);
//        System.out.println("firstValue: " + firstValue);
        Map firstValueMap = (HashMap) firstValue.get("datasetContactEmail");
//        System.out.println("firstValueMap: " + firstValueMap);
        assertEquals("sammi@sample.com", firstValueMap.get("value"));
        assertTrue(datasetContactFromExport.toString().contains("sammi@sample.com"));
        assertTrue(firstValue.toString().contains("sammi@sample.com"));

        Response citationBlock = UtilIT.getMetadataBlockFromDatasetVersion(datasetPersistentId, null, null, apiToken);
        citationBlock.prettyPrint();
        citationBlock.then().assertThat()
                .body("data.fields[2].value[0].datasetContactEmail.value", equalTo("sammi@sample.com"))
                .statusCode(OK.getStatusCode());

        Response exportFail = UtilIT.exportDataset(datasetPersistentId, "noSuchExporter", apiToken);
        exportFail.prettyPrint();
        exportFail.then().assertThat()
                .body("message", equalTo("Export Failed"))
                .statusCode(FORBIDDEN.getStatusCode());

        Response exportDatasetAsDublinCore = UtilIT.exportDataset(datasetPersistentId, "oai_dc", apiToken);
        exportDatasetAsDublinCore.prettyPrint();
        exportDatasetAsDublinCore.then().assertThat()
                .body("oai_dc.title", is("Dataset One"))
                .statusCode(OK.getStatusCode());

        Response exportDatasetAsDdi = UtilIT.exportDataset(datasetPersistentId, "ddi", apiToken);
        exportDatasetAsDdi.prettyPrint();
        exportDatasetAsDdi.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals(null, XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.stdyInfo.contact.@email"));
        assertEquals(datasetPersistentId, XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.docDscr.citation.titlStmt.IDNo"));

        Response reexportAllFormats = UtilIT.reexportDatasetAllFormats(datasetPersistentId);
        reexportAllFormats.prettyPrint();
        reexportAllFormats.then().assertThat().statusCode(OK.getStatusCode());

        Response reexportAllFormatsUsingId = UtilIT.reexportDatasetAllFormats(datasetId.toString());
        reexportAllFormatsUsingId.prettyPrint();
        reexportAllFormatsUsingId.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDatasetResponse = UtilIT.destroyDataset(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

    }

    /**
     * This test requires the root dataverse to be published to pass.
     */
    @Test
    public void testExcludeEmail() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String pathToJsonFile = "scripts/api/data/dataset-create-new.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonBeforePublishing.prettyPrint();
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());
        Response attemptToPublishZeroDotOne = UtilIT.publishDatasetViaNativeApiDeprecated(datasetPersistentId, "minor", apiToken);
        attemptToPublishZeroDotOne.prettyPrint();
        attemptToPublishZeroDotOne.then().assertThat()
                .body("message", equalTo("Cannot publish as minor version. Re-try as major release."))
                .statusCode(403);

        Response setToExcludeEmailFromExport = UtilIT.setSetting(SettingsServiceBean.Key.ExcludeEmailFromExport, "true");
        setToExcludeEmailFromExport.then().assertThat()
                .statusCode(OK.getStatusCode());
        // return random sleep  9/13/2019
        // 3 second sleep, to allow the indexing to finish:

        try {
            Thread.sleep(3000l);
        } catch (InterruptedException iex) {}

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());

        Response getDatasetJsonAfterPublishing = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonAfterPublishing.prettyPrint();
        getDatasetJsonAfterPublishing.then().assertThat()
                .body("data.latestVersion.versionNumber", equalTo(1))
                .body("data.latestVersion.versionMinorNumber", equalTo(0))
                .statusCode(OK.getStatusCode());

        Response exportDatasetAsDdi = UtilIT.exportDataset(datasetPersistentId, "ddi", apiToken);
        exportDatasetAsDdi.prettyPrint();
        exportDatasetAsDdi.then().assertThat()
                .statusCode(OK.getStatusCode());

        assertEquals("Dataverse, Admin", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.citation.distStmt.contact"));
        // no "sammi@sample.com" to be found https://github.com/IQSS/dataverse/issues/3443
        assertEquals(null, XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.citation.distStmt.contact.@email"));
        assertEquals("Sample Datasets, inc.", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.citation.distStmt.contact.@affiliation"));
        assertEquals(datasetPersistentId, XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.docDscr.citation.titlStmt.IDNo"));

        List<JsonObject> datasetContactsFromNativeGet = with(getDatasetJsonAfterPublishing.body().asString()).param("datasetContact", "datasetContact")
                .getJsonObject("data.latestVersion.metadataBlocks.citation.fields.findAll { fields -> fields.typeName == datasetContact }");
        // TODO: Assert that email can't be found.
        assertEquals(1, datasetContactsFromNativeGet.size());

        // TODO: Write test for DDI too.
        Response exportDatasetAsJson = UtilIT.exportDataset(datasetPersistentId, "dataverse_json", apiToken);
        exportDatasetAsJson.prettyPrint();
        exportDatasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());
        RestAssured.unregisterParser("text/plain");

        List<JsonObject> datasetContactsFromExport = with(exportDatasetAsJson.body().asString()).param("datasetContact", "datasetContact")
                .getJsonObject("datasetVersion.metadataBlocks.citation.fields.findAll { fields -> fields.typeName == datasetContact }");
        // TODO: Assert that email can't be found.
        assertEquals(1, datasetContactsFromExport.size());

        Response citationBlock = UtilIT.getMetadataBlockFromDatasetVersion(datasetPersistentId, null, null, apiToken);
        citationBlock.prettyPrint();
        citationBlock.then().assertThat()
                .statusCode(OK.getStatusCode());

        List<JsonObject> datasetContactsFromCitationBlock = with(citationBlock.body().asString()).param("datasetContact", "datasetContact")
                .getJsonObject("data.fields.findAll { fields -> fields.typeName == datasetContact }");
        // TODO: Assert that email can't be found.
        assertEquals(1, datasetContactsFromCitationBlock.size());

        Response deleteDatasetResponse = UtilIT.destroyDataset(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

        Response removeExcludeEmail = UtilIT.deleteSetting(SettingsServiceBean.Key.ExcludeEmailFromExport);
        removeExcludeEmail.then().assertThat()
                .statusCode(200);

    }

    @Disabled
    /*The identifier generation style is no longer a global, dynamically changeable setting. To make this test work after PR #10234,
     * will require configuring a PidProvider that uses this style and creating a collection/dataset that uses that provider.
     */
    @Test
    public void testStoredProcGeneratedAsIdentifierGenerationStyle() {
        // Please note that this test only works if the stored procedure
        // named generateIdentifierFromStoredProcedure() has been created in the 
        // database (see the documentation for the "IdentifierGenerationStyle" option
        // in the Configuration section of the Installation guide).
        // Furthermore, the test below expects the identifier generated by the stored
        // procedure to be a numeric string. The "sequential numerical values" procedure, 
        // documented as the first example in the guide above, will satisfy the test. 
        // If your installation is using a stored procedure that generates the identifiers
        // in any other format, the test below will fail. 
        // (The test is mainly used by the Dataverse Project's automated API test system 
        // to verify the integrity of this framework for generating the identifiers on the 
        // database side). 
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response setStoredProcGeneratedAsIdentifierGenerationStyle = UtilIT.setSetting(SettingsServiceBean.Key.IdentifierGenerationStyle, "storedProcGenerated");
        setStoredProcGeneratedAsIdentifierGenerationStyle.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        Response datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());

        String identifier = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");
        logger.info("identifier: " + identifier);
        String numericPart = identifier.replace("FK2/", ""); //remove shoulder from identifier
        assertTrue(StringUtils.isNumeric(numericPart));
        //Return random sleep  9/13/2019        

        try {
            Thread.sleep(3000l);
        } catch (Exception ex) {logger.warning("failed to execute sleep 3 sec.");}

        
        Response deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

        Response remove = UtilIT.deleteSetting(SettingsServiceBean.Key.IdentifierGenerationStyle);
        remove.then().assertThat()
                .statusCode(200);

    }

    /**
     * This test requires the root dataverse to be published to pass.
     */
    @Test
    public void testPrivateUrl() {

        Response createUser = UtilIT.createRandomUser();
//        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response failToCreateWhenDatasetIdNotFound = UtilIT.privateUrlCreate(Integer.MAX_VALUE, apiToken, false);
        failToCreateWhenDatasetIdNotFound.prettyPrint();
        assertEquals(NOT_FOUND.getStatusCode(), failToCreateWhenDatasetIdNotFound.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        logger.info("dataset id: " + datasetId);

        Response createContributorResponse = UtilIT.createRandomUser();
        String contributorUsername = UtilIT.getUsernameFromResponse(createContributorResponse);
        String contributorApiToken = UtilIT.getApiTokenFromResponse(createContributorResponse);
        UtilIT.getRoleAssignmentsOnDataverse(dataverseAlias, apiToken).prettyPrint();
        Response grantRoleShouldFail = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.EDITOR.toString(), "doesNotExist", apiToken);
        grantRoleShouldFail.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Assignee not found"));
        /**
         * editor (a.k.a. Contributor) has "ViewUnpublishedDataset",
         * "EditDataset", "DownloadFile", and "DeleteDatasetDraft" per
         * scripts/api/data/role-editor.json
         */
        Response grantRole = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.EDITOR.toString(), "@" + contributorUsername, apiToken);
        grantRole.prettyPrint();
        assertEquals(OK.getStatusCode(), grantRole.getStatusCode());
        UtilIT.getRoleAssignmentsOnDataverse(dataverseAlias, apiToken).prettyPrint();
        Response contributorDoesNotHavePermissionToCreatePrivateUrl = UtilIT.privateUrlCreate(datasetId, contributorApiToken, false);
        contributorDoesNotHavePermissionToCreatePrivateUrl.prettyPrint();
        assertEquals(UNAUTHORIZED.getStatusCode(), contributorDoesNotHavePermissionToCreatePrivateUrl.getStatusCode());

        Response getDatasetJson = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJson.prettyPrint();
        String protocol1 = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.protocol");
        String authority1 = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.authority");
        String identifier1 = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.identifier");
        String dataset1PersistentId = protocol1 + ":" + authority1 + "/" + identifier1;

        Response uploadFileResponse = UtilIT.uploadRandomFile(dataset1PersistentId, apiToken);
        uploadFileResponse.prettyPrint();
        assertEquals(CREATED.getStatusCode(), uploadFileResponse.getStatusCode());

        Response badApiKeyEmptyString = UtilIT.privateUrlGet(datasetId, "");
        badApiKeyEmptyString.prettyPrint();
        assertEquals(UNAUTHORIZED.getStatusCode(), badApiKeyEmptyString.getStatusCode());
        Response badApiKeyDoesNotExist = UtilIT.privateUrlGet(datasetId, "junk");
        badApiKeyDoesNotExist.prettyPrint();
        assertEquals(UNAUTHORIZED.getStatusCode(), badApiKeyDoesNotExist.getStatusCode());
        Response badDatasetId = UtilIT.privateUrlGet(Integer.MAX_VALUE, apiToken);
        badDatasetId.prettyPrint();
        assertEquals(NOT_FOUND.getStatusCode(), badDatasetId.getStatusCode());
        Response pristine = UtilIT.privateUrlGet(datasetId, apiToken);
        pristine.prettyPrint();
        assertEquals(NOT_FOUND.getStatusCode(), pristine.getStatusCode());

        Response createPrivateUrl = UtilIT.privateUrlCreate(datasetId, apiToken, false);
        createPrivateUrl.prettyPrint();
        assertEquals(OK.getStatusCode(), createPrivateUrl.getStatusCode());

        Response userWithNoRoles = UtilIT.createRandomUser();
        String userWithNoRolesApiToken = UtilIT.getApiTokenFromResponse(userWithNoRoles);
        Response unAuth = UtilIT.privateUrlGet(datasetId, userWithNoRolesApiToken);
        unAuth.prettyPrint();
        assertEquals(UNAUTHORIZED.getStatusCode(), unAuth.getStatusCode());
        Response shouldExist = UtilIT.privateUrlGet(datasetId, apiToken);
        shouldExist.prettyPrint();
        assertEquals(OK.getStatusCode(), shouldExist.getStatusCode());

        String tokenForPrivateUrlUser = JsonPath.from(shouldExist.body().asString()).getString("data.token");
        logger.info("privateUrlToken: " + tokenForPrivateUrlUser);

        String urlWithToken = JsonPath.from(shouldExist.body().asString()).getString("data.link");
        logger.info("URL with token: " + urlWithToken);

        assertEquals(tokenForPrivateUrlUser, urlWithToken.substring(urlWithToken.length() - UUID.randomUUID().toString().length()));

        /**
         * If you're getting a crazy error like this...
         *
         * javax.net.ssl.SSLHandshakeException:
         * sun.security.validator.ValidatorException: PKIX path building failed:
         * sun.security.provider.certpath.SunCertPathBuilderException: unable to
         * find valid certification path to requested target
         *
         * ... you might do well to set "siteUrl" to localhost:8080 like this:
         *
         * asadmin create-jvm-options
         * "-Ddataverse.siteUrl=http\://localhost\:8080"
         */
        
        /* 
         * Attempt to follow the private link url; as a user not otherwise 
         * authorized to view the draft - and make sure they get the dataset page:
         * 
         * MAKE SURE TO READ the note below, about jsessions and cookies!
        */
        
        Response getDatasetAsUserWhoClicksPrivateUrl = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get(urlWithToken);
        String title = getDatasetAsUserWhoClicksPrivateUrl.getBody().htmlPath().getString("html.head.title");
        assertEquals("Darwin's Finches - " + dataverseAlias, title);
        assertEquals(OK.getStatusCode(), getDatasetAsUserWhoClicksPrivateUrl.getStatusCode());

        /*
         * NOTE, this is what happens when we attempt to access the dataset via the 
         * private url, as implemented above: 
         * 
         * The private url page authorizes the user to view the dataset 
         * by issuing a new jsession, and issuing a 302 redirect to the dataset 
         * page WITH THE JSESSIONID ADDED TO THE URL - as in 
         * dataset.xhtml?persistentId=xxx&jsessionid=yyy
         * RestAssured's .get() method follows redirects by default - so in the 
         * end the above works and we get the correct dataset. 
         * But note that this relies on the jsessionid in the url. We've 
         * experimented with disabling url-supplied jsessions (in PR #5316); 
         * then the above stopped working - because now jsession is supplied 
         * AS A COOKIE, which the RestAssured code above does not use, so 
         * the dataset page refuses to show the dataset to the user. (So the 
         * assertEquals code above fails, because the page title is not "Darwin's Finches", 
         * but "Login Page")
         * Below is an implementation of the test above that uses the jsession 
         * cookie, instead of relying on the jsessionid in the URL: 
         
        // This should redirect us to the actual dataset page, and 
        // give us a valid session cookie: 
        
        Response getDatasetAsUserWhoClicksPrivateUrl = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .redirects().follow(false)
                .get(urlWithToken);
        // (note that we have purposefully asked not to follow redirects 
        // automatically; this way we can test that we are being redirected
        // to the right place, that we've been given the session cookie, etc.
                
        assertEquals(FOUND.getStatusCode(), getDatasetAsUserWhoClicksPrivateUrl.getStatusCode());
        // Yes, javax.ws.rs.core.Response.Status.FOUND is 302!
        String title = getDatasetAsUserWhoClicksPrivateUrl.getBody().htmlPath().getString("html.head.title");
        assertEquals("Document moved", title);
        
        String redirectLink = getDatasetAsUserWhoClicksPrivateUrl.getBody().htmlPath().getString("html.body.a.@href");
        assertNotNull(redirectLink);
        assertTrue(redirectLink.contains("dataset.xhtml"));
        
        String jsessionid = getDatasetAsUserWhoClicksPrivateUrl.cookie("jsessionid");
        assertNotNull(jsessionid);
        
        // ... and now we can try and access the dataset, with another HTTP GET, 
        // sending the jsession cookie along:
        
        try { 
            redirectLink = URLDecoder.decode(redirectLink, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // do nothing - try to redirect to the url as is? 
        }
        
        logger.info("redirecting to "+redirectLink+", using jsession "+jsessionid);
        
        getDatasetAsUserWhoClicksPrivateUrl = given()
                .cookies("JSESSIONID", jsessionid)
                .get(redirectLink);
        
        assertEquals(OK.getStatusCode(), getDatasetAsUserWhoClicksPrivateUrl.getStatusCode());
        title = getDatasetAsUserWhoClicksPrivateUrl.getBody().htmlPath().getString("html.head.title");
        assertEquals("Darwin's Finches - " + dataverseAlias, title);
         
        */
        
        Response junkPrivateUrlToken = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/privateurl.xhtml?token=" + "junk");
        assertEquals("404 Not Found", junkPrivateUrlToken.getBody().htmlPath().getString("html.head.title").substring(0, 13));

        long roleAssignmentIdFromCreate = JsonPath.from(createPrivateUrl.body().asString()).getLong("data.roleAssignment.id");
        logger.info("roleAssignmentIdFromCreate: " + roleAssignmentIdFromCreate);

        Response badAnonLinkTokenEmptyString = UtilIT.nativeGet(datasetId, "");
        badAnonLinkTokenEmptyString.prettyPrint();
        assertEquals(UNAUTHORIZED.getStatusCode(), badAnonLinkTokenEmptyString.getStatusCode());

        Response getWithPrivateUrlToken = UtilIT.nativeGet(datasetId, tokenForPrivateUrlUser);
        assertEquals(OK.getStatusCode(), getWithPrivateUrlToken.getStatusCode());
//        getWithPrivateUrlToken.prettyPrint();
        logger.info("http://localhost:8080/privateurl.xhtml?token=" + tokenForPrivateUrlUser);
        Response swordStatement = UtilIT.getSwordStatement(dataset1PersistentId, apiToken);
        assertEquals(OK.getStatusCode(), swordStatement.getStatusCode());
        Integer fileId = UtilIT.getFileIdFromSwordStatementResponse(swordStatement);
        Response downloadFile = UtilIT.downloadFile(fileId, tokenForPrivateUrlUser);
        assertEquals(OK.getStatusCode(), downloadFile.getStatusCode());
        Response downloadFileBadToken = UtilIT.downloadFile(fileId, "junk");
        assertEquals(UNAUTHORIZED.getStatusCode(), downloadFileBadToken.getStatusCode());
        Response notPermittedToListRoleAssignment = UtilIT.getRoleAssignmentsOnDataset(datasetId.toString(), null, userWithNoRolesApiToken);
        assertEquals(UNAUTHORIZED.getStatusCode(), notPermittedToListRoleAssignment.getStatusCode());
        Response roleAssignments = UtilIT.getRoleAssignmentsOnDataset(datasetId.toString(), null, apiToken);
        roleAssignments.prettyPrint();
        assertEquals(OK.getStatusCode(), roleAssignments.getStatusCode());
        List<JsonObject> assignments = with(roleAssignments.body().asString()).param("member", "member").getJsonObject("data.findAll { data -> data._roleAlias == member }");
        assertEquals(1, assignments.size());
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(datasetId);
        assertEquals("Preview URL Enabled", privateUrlUser.getDisplayInfo().getTitle());
        List<JsonObject> assigneeShouldExistForPrivateUrlUser = with(roleAssignments.body().asString()).param("assigneeString", privateUrlUser.getIdentifier()).getJsonObject("data.findAll { data -> data.assignee == assigneeString }");
        logger.info(assigneeShouldExistForPrivateUrlUser + " found for " + privateUrlUser.getIdentifier());
        assertEquals(1, assigneeShouldExistForPrivateUrlUser.size());
        Map roleAssignment = assignments.get(0);
        int roleAssignmentId = (int) roleAssignment.get("id");
        logger.info("role assignment id: " + roleAssignmentId);
        assertEquals(roleAssignmentIdFromCreate, roleAssignmentId);
        Response revoke = UtilIT.revokeRole(dataverseAlias, roleAssignmentId, apiToken);
        revoke.prettyPrint();
        assertEquals(OK.getStatusCode(), revoke.getStatusCode());

        Response shouldNoLongerExist = UtilIT.privateUrlGet(datasetId, apiToken);
        shouldNoLongerExist.prettyPrint();
        assertEquals(NOT_FOUND.getStatusCode(), shouldNoLongerExist.getStatusCode());

        Response createPrivateUrlUnauth = UtilIT.privateUrlCreate(datasetId, userWithNoRolesApiToken, false);
        createPrivateUrlUnauth.prettyPrint();
        assertEquals(UNAUTHORIZED.getStatusCode(), createPrivateUrlUnauth.getStatusCode());

        Response createPrivateUrlAgain = UtilIT.privateUrlCreate(datasetId, apiToken, false);
        createPrivateUrlAgain.prettyPrint();
        assertEquals(OK.getStatusCode(), createPrivateUrlAgain.getStatusCode());

        Response shouldNotDeletePrivateUrl = UtilIT.privateUrlDelete(datasetId, userWithNoRolesApiToken);
        shouldNotDeletePrivateUrl.prettyPrint();
        assertEquals(UNAUTHORIZED.getStatusCode(), shouldNotDeletePrivateUrl.getStatusCode());

        Response deletePrivateUrlResponse = UtilIT.privateUrlDelete(datasetId, apiToken);
        deletePrivateUrlResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), deletePrivateUrlResponse.getStatusCode());

        Response tryToDeleteAlreadyDeletedPrivateUrl = UtilIT.privateUrlDelete(datasetId, apiToken);
        tryToDeleteAlreadyDeletedPrivateUrl.prettyPrint();
        assertEquals(NOT_FOUND.getStatusCode(), tryToDeleteAlreadyDeletedPrivateUrl.getStatusCode());

        Response createPrivateUrlOnceAgain = UtilIT.privateUrlCreate(datasetId, apiToken, false);
        createPrivateUrlOnceAgain.prettyPrint();
        assertEquals(OK.getStatusCode(), createPrivateUrlOnceAgain.getStatusCode());

        Response tryToCreatePrivateUrlWhenExisting = UtilIT.privateUrlCreate(datasetId, apiToken, false);
        tryToCreatePrivateUrlWhenExisting.prettyPrint();
        assertEquals(FORBIDDEN.getStatusCode(), tryToCreatePrivateUrlWhenExisting.getStatusCode());

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(OK.getStatusCode(), publishDataverse.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaSword(dataset1PersistentId, apiToken);
        assertEquals(OK.getStatusCode(), publishDataset.getStatusCode());
        Response privateUrlTokenShouldBeDeletedOnPublish = UtilIT.privateUrlGet(datasetId, apiToken);
        privateUrlTokenShouldBeDeletedOnPublish.prettyPrint();
        assertEquals(NOT_FOUND.getStatusCode(), privateUrlTokenShouldBeDeletedOnPublish.getStatusCode());

        Response getRoleAssignmentsOnDatasetShouldFailUnauthorized = UtilIT.getRoleAssignmentsOnDataset(datasetId.toString(), null, userWithNoRolesApiToken);
        assertEquals(UNAUTHORIZED.getStatusCode(), getRoleAssignmentsOnDatasetShouldFailUnauthorized.getStatusCode());
        Response publishingShouldHaveRemovedRoleAssignmentForPrivateUrlUser = UtilIT.getRoleAssignmentsOnDataset(datasetId.toString(), null, apiToken);
        publishingShouldHaveRemovedRoleAssignmentForPrivateUrlUser.prettyPrint();
        List<JsonObject> noAssignmentsForPrivateUrlUser = with(publishingShouldHaveRemovedRoleAssignmentForPrivateUrlUser.body().asString()).param("member", "member").getJsonObject("data.findAll { data -> data._roleAlias == member }");
        assertEquals(0, noAssignmentsForPrivateUrlUser.size());

        Response tryToCreatePrivateUrlToPublishedVersion = UtilIT.privateUrlCreate(datasetId, apiToken, false);
        tryToCreatePrivateUrlToPublishedVersion.prettyPrint();
        assertEquals(FORBIDDEN.getStatusCode(), tryToCreatePrivateUrlToPublishedVersion.getStatusCode());

        String newTitle = "I am changing the title";
        Response updatedMetadataResponse = UtilIT.updateDatasetTitleViaSword(dataset1PersistentId, newTitle, apiToken);
        updatedMetadataResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), updatedMetadataResponse.getStatusCode());

        Response createPrivateUrlForPostVersionOneDraft = UtilIT.privateUrlCreate(datasetId, apiToken, false);
        createPrivateUrlForPostVersionOneDraft.prettyPrint();
        assertEquals(OK.getStatusCode(), createPrivateUrlForPostVersionOneDraft.getStatusCode());

        // A Contributor has DeleteDatasetDraft
        Response deleteDraftVersionAsContributor = UtilIT.deleteDatasetVersionViaNativeApi(datasetId, DS_VERSION_DRAFT, contributorApiToken);
        deleteDraftVersionAsContributor.prettyPrint();
        deleteDraftVersionAsContributor.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Draft version of dataset " + datasetId + " deleted"));

        Response privateUrlRoleAssignmentShouldBeGoneAfterDraftDeleted = UtilIT.getRoleAssignmentsOnDataset(datasetId.toString(), null, apiToken);
        privateUrlRoleAssignmentShouldBeGoneAfterDraftDeleted.prettyPrint();
        assertFalse(privateUrlRoleAssignmentShouldBeGoneAfterDraftDeleted.body().asString().contains(privateUrlUser.getIdentifier()));

        String newTitleAgain = "I am changing the title again";
        Response draftCreatedAgainPostPub = UtilIT.updateDatasetTitleViaSword(dataset1PersistentId, newTitleAgain, apiToken);
        draftCreatedAgainPostPub.prettyPrint();
        assertEquals(OK.getStatusCode(), draftCreatedAgainPostPub.getStatusCode());

        /**
         * Making sure the Private URL is deleted when a dataset is destroyed is
         * less of an issue now that a Private URL is now effectively only a
         * specialized role assignment which is already known to be deleted when
         * a dataset is destroy. Still, we'll keep this test in here in case we
         * switch Private URL back to being its own table in the future.
         */
        Response createPrivateUrlToMakeSureItIsDeletedWithDestructionOfDataset = UtilIT.privateUrlCreate(datasetId, apiToken, false);
        createPrivateUrlToMakeSureItIsDeletedWithDestructionOfDataset.prettyPrint();
        assertEquals(OK.getStatusCode(), createPrivateUrlToMakeSureItIsDeletedWithDestructionOfDataset.getStatusCode());

        /**
         * @todo What about deaccessioning? We can't test deaccessioning via API
         * until https://github.com/IQSS/dataverse/issues/778 is worked on. If
         * you deaccession a dataset, is the Private URL deleted? Probably not
         * because in order to create a Private URL the dataset version must be
         * a draft and for that draft to be deaccessioned it must be published
         * first and publishing a version will delete the Private URL. So, we
         * shouldn't need to worry about cleaning up Private URLs in the case of
         * deaccessioning.
         */
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetId, apiToken);
        destroyDatasetResponse.prettyPrint();
        assertEquals(200, destroyDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
        /**
         * @todo Should the Search API work with the Private URL token?
         */
    }
    
    @Test
    public void testAddRoles(){
        
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
        assertEquals(10, identifier.length());
        
        String protocol1 = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.protocol");
        String authority1 = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.authority");
        String identifier1 = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol1 + ":" + authority1 + "/" + identifier1;

       
        
        // Create another random user: 
        
        Response createRandomUser = UtilIT.createRandomUser();
        createRandomUser.prettyPrint();
        String randomUsername = UtilIT.getUsernameFromResponse(createRandomUser);
        String randomUserApiToken = UtilIT.getApiTokenFromResponse(createRandomUser);
        

        //Give that random user permission
        //(String definitionPoint, String role, String roleAssignee, String apiToken)
        //Can't give yourself permission
        Response giveRandoPermission = UtilIT.grantRoleOnDataset(datasetPersistentId, "fileDownloader", "@" + randomUsername, randomUserApiToken);
                giveRandoPermission.prettyPrint();
        assertEquals(401, giveRandoPermission.getStatusCode());
        
        giveRandoPermission = UtilIT.grantRoleOnDataset(datasetPersistentId, "fileDownloader", "@" + randomUsername, apiToken);
                giveRandoPermission.prettyPrint();
        assertEquals(200, giveRandoPermission.getStatusCode());

        //Asserting same role creation is covered
        validateAssignExistingRole(datasetPersistentId,randomUsername,apiToken, "fileDownloader");

        // Create another random user to become curator:

        Response createCuratorUser = UtilIT.createRandomUser();
        createCuratorUser.prettyPrint();
        String curatorUsername = UtilIT.getUsernameFromResponse(createCuratorUser);
        String curatorUserApiToken = UtilIT.getApiTokenFromResponse(createCuratorUser);

        Response giveCuratorPermission = UtilIT.grantRoleOnDataset(datasetPersistentId, "curator", "@" + curatorUsername, apiToken);
        giveCuratorPermission.prettyPrint();
        assertEquals(200, giveCuratorPermission.getStatusCode());

        // Test if privilege escalation is possible: curator should not be able to assign admin rights
        Response giveTooMuchPermission = UtilIT.grantRoleOnDataset(datasetPersistentId, "admin", "@" + curatorUsername, curatorUserApiToken);
        giveTooMuchPermission.prettyPrint();
        assertEquals(401, giveTooMuchPermission.getStatusCode());

        giveTooMuchPermission = UtilIT.grantRoleOnDataset(datasetPersistentId, "admin", "@" + randomUsername, curatorUserApiToken);
        giveTooMuchPermission.prettyPrint();
        assertEquals(401, giveTooMuchPermission.getStatusCode());
        
        String idToDelete = JsonPath.from(giveRandoPermission.getBody().asString()).getString("data.id");                

        giveRandoPermission = UtilIT.grantRoleOnDataset(datasetPersistentId, "designatedHitter", "@" + randomUsername, apiToken);
                giveRandoPermission.prettyPrint();
                        giveRandoPermission.then().assertThat()
                .contentType(ContentType.JSON)
                .body("message", containsString("Cannot find role named 'designatedHitter' in dataverse "))
                .statusCode(400);
        assertEquals(400, giveRandoPermission.getStatusCode());
        
        //Try to delete Role with Id saved above
        //Fails for lack of perms
        Response deleteGrantedAccess = UtilIT.revokeRoleOnDataset(datasetPersistentId, new Long(idToDelete), randomUserApiToken);
        deleteGrantedAccess.prettyPrint();
        assertEquals(401, deleteGrantedAccess.getStatusCode());
        
        //Should be able to delete with proper apiToken
        deleteGrantedAccess = UtilIT.revokeRoleOnDataset(datasetPersistentId, new Long(idToDelete), apiToken);
        deleteGrantedAccess.prettyPrint();
        assertEquals(200, deleteGrantedAccess.getStatusCode());
        
        Response deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());
        
        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

        deleteUserResponse = UtilIT.deleteUser(randomUsername);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

        deleteUserResponse = UtilIT.deleteUser(curatorUsername);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
        
    }

    @Test
    public void testListRoleAssignments() {
        Response createAdminUser = UtilIT.createRandomUser();
        String adminUsername = UtilIT.getUsernameFromResponse(createAdminUser);
        String adminApiToken = UtilIT.getApiTokenFromResponse(createAdminUser);
        UtilIT.makeSuperUser(adminUsername);

        Response createDataverseResponse = UtilIT.createRandomDataverse(adminApiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Now, let's allow anyone with a Dataverse account (any "random user")
        // to create datasets in this dataverse:

        Response grantRole = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.DS_CONTRIBUTOR, AuthenticatedUsers.get().getIdentifier(), adminApiToken);
        grantRole.prettyPrint();
        assertEquals(OK.getStatusCode(), grantRole.getStatusCode());

        Response createContributorUser = UtilIT.createRandomUser();
        String contributorUsername = UtilIT.getUsernameFromResponse(createContributorUser);
        String contributorApiToken = UtilIT.getApiTokenFromResponse(createContributorUser);

        // First, we test listing role assignments on a dataverse which requires "ManageDataversePermissions"

        Response notPermittedToListRoleAssignmentOnDataverse = UtilIT.getRoleAssignmentsOnDataverse(dataverseAlias, contributorApiToken);
        assertEquals(UNAUTHORIZED.getStatusCode(), notPermittedToListRoleAssignmentOnDataverse.getStatusCode());

        Response roleAssignmentsOnDataverse = UtilIT.getRoleAssignmentsOnDataverse(dataverseAlias, adminApiToken);
        roleAssignmentsOnDataverse.prettyPrint();
        assertEquals(OK.getStatusCode(), roleAssignmentsOnDataverse.getStatusCode());

        // Second, we test listing role assignments on a dataset which requires "ManageDatasetPermissions"

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, contributorApiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        logger.info("dataset id: " + datasetId);

        Response datasetAsJson = UtilIT.nativeGet(datasetId, adminApiToken);
        datasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());

        String identifier = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");
        assertEquals(10, identifier.length());

        String protocol1 = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.protocol");
        String authority1 = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.authority");
        String identifier1 = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol1 + ":" + authority1 + "/" + identifier1;

        Response notPermittedToListRoleAssignmentOnDataset = UtilIT.getRoleAssignmentsOnDataset(datasetId.toString(), null, contributorApiToken);
        assertEquals(UNAUTHORIZED.getStatusCode(), notPermittedToListRoleAssignmentOnDataset.getStatusCode());

        // We assign the curator role to the contributor user
        // (includes "ManageDatasetPermissions" which are required for listing role assignments of a dataset, but not
        // "ManageDataversePermissions")

        Response giveRandoPermission = UtilIT.grantRoleOnDataset(datasetPersistentId, "curator", "@" + contributorUsername, adminApiToken);
        giveRandoPermission.prettyPrint();
        assertEquals(200, giveRandoPermission.getStatusCode());

        // Contributor user should now be able to list dataset role assignments as well

        Response roleAssignmentsOnDataset = UtilIT.getRoleAssignmentsOnDataset(datasetId.toString(), null, contributorApiToken);
        roleAssignmentsOnDataset.prettyPrint();
        assertEquals(OK.getStatusCode(), roleAssignmentsOnDataset.getStatusCode());

        // ...but not dataverse role assignments

        notPermittedToListRoleAssignmentOnDataverse = UtilIT.getRoleAssignmentsOnDataverse(dataverseAlias, contributorApiToken);
        assertEquals(UNAUTHORIZED.getStatusCode(), notPermittedToListRoleAssignmentOnDataverse.getStatusCode());
    }

    private static void validateAssignExistingRole(String datasetPersistentId, String randomUsername, String apiToken, String role) {
        final Response failedGrantPermission = UtilIT.grantRoleOnDataset(datasetPersistentId, role, "@" + randomUsername, apiToken);
        failedGrantPermission.prettyPrint();
        failedGrantPermission.then().assertThat()
                .body("message", containsString(BundleUtil.getStringFromBundle("datasets.api.grant.role.assignee.has.role.error")))
                .statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    public void testFileChecksum() {

        Response createUser = UtilIT.createRandomUser();
//        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        logger.info("dataset id: " + datasetId);

        Response getDatasetJsonNoFiles = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonNoFiles.prettyPrint();
        String protocol1 = JsonPath.from(getDatasetJsonNoFiles.getBody().asString()).getString("data.protocol");
        String authority1 = JsonPath.from(getDatasetJsonNoFiles.getBody().asString()).getString("data.authority");
        String identifier1 = JsonPath.from(getDatasetJsonNoFiles.getBody().asString()).getString("data.identifier");
        String dataset1PersistentId = protocol1 + ":" + authority1 + "/" + identifier1;

        Response makeSureSettingIsDefault = UtilIT.deleteSetting(SettingsServiceBean.Key.FileFixityChecksumAlgorithm);
        makeSureSettingIsDefault.prettyPrint();
        makeSureSettingIsDefault.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Setting :FileFixityChecksumAlgorithm deleted."));
        Response getDefaultSetting = UtilIT.getSetting(SettingsServiceBean.Key.FileFixityChecksumAlgorithm);
        getDefaultSetting.prettyPrint();
        getDefaultSetting.then().assertThat()
                .body("message", equalTo("Setting :FileFixityChecksumAlgorithm not found"));

        Response uploadMd5File = UtilIT.uploadRandomFile(dataset1PersistentId, apiToken);
        uploadMd5File.prettyPrint();
        assertEquals(CREATED.getStatusCode(), uploadMd5File.getStatusCode());
        Response getDatasetJsonAfterMd5File = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonAfterMd5File.prettyPrint();
        getDatasetJsonAfterMd5File.then().assertThat()
                .body("data.latestVersion.files[0].dataFile.md5", equalTo("0386269a5acb2c57b4eade587ff4db64"))
                .body("data.latestVersion.files[0].dataFile.checksum.type", equalTo("MD5"))
                .body("data.latestVersion.files[0].dataFile.checksum.value", equalTo("0386269a5acb2c57b4eade587ff4db64"));

        int fileId = JsonPath.from(getDatasetJsonAfterMd5File.getBody().asString()).getInt("data.latestVersion.files[0].dataFile.id");
        Response deleteFile = UtilIT.deleteFile(fileId, apiToken);
        deleteFile.prettyPrint();
        deleteFile.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());

        Response setToSha1 = UtilIT.setSetting(SettingsServiceBean.Key.FileFixityChecksumAlgorithm, DataFile.ChecksumType.SHA1.toString());
        setToSha1.prettyPrint();
        setToSha1.then().assertThat()
                .statusCode(OK.getStatusCode());
        Response getNonDefaultSetting = UtilIT.getSetting(SettingsServiceBean.Key.FileFixityChecksumAlgorithm);
        getNonDefaultSetting.prettyPrint();
        getNonDefaultSetting.then().assertThat()
                .body("data.message", equalTo("SHA-1"))
                .statusCode(OK.getStatusCode());

        Response uploadSha1File = UtilIT.uploadRandomFile(dataset1PersistentId, apiToken);
        uploadSha1File.prettyPrint();
        assertEquals(CREATED.getStatusCode(), uploadSha1File.getStatusCode());
        Response getDatasetJsonAfterSha1File = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonAfterSha1File.prettyPrint();
        getDatasetJsonAfterSha1File.then().assertThat()
                .body("data.latestVersion.files[0].dataFile.md5", nullValue())
                .body("data.latestVersion.files[0].dataFile.checksum.type", equalTo("SHA-1"))
                .body("data.latestVersion.files[0].dataFile.checksum.value", equalTo("17ea9225aa0e96ae6ff61c256237d6add6c197d1"))
                .statusCode(OK.getStatusCode());

    }

    @Test
    public void testDeleteDatasetWhileFileIngesting() {

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        String persistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");
        logger.info("Dataset created with id " + datasetId + " and persistent id " + persistentId);

        String pathToFileThatGoesThroughIngest = "scripts/search/data/tabular/50by1000.dta";
        Response uploadIngestableFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFileThatGoesThroughIngest, apiToken);
        uploadIngestableFile.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response deleteDataset = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDataset.prettyPrint();
        deleteDataset.then().assertThat()
                .body("message", equalTo("Dataset cannot be edited due to dataset lock."))
                .statusCode(FORBIDDEN.getStatusCode());

    }
    
    @Test
    public void testGetDatasetOwners() {

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        String persistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");
        logger.info("Dataset created with id " + datasetId + " and persistent id " + persistentId);

        Response getDatasetWithOwners = UtilIT.getDatasetWithOwners(persistentId, apiToken, true);
        getDatasetWithOwners.prettyPrint();
        getDatasetWithOwners.then().assertThat().body("data.isPartOf.identifier", equalTo(dataverseAlias));
        getDatasetWithOwners.then().assertThat().body("data.isPartOf.isReleased", equalTo(false));
        getDatasetWithOwners.then().assertThat().body("data.isPartOf.isPartOf.identifier", equalTo("root"));
        getDatasetWithOwners.then().assertThat().body("data.isPartOf.isPartOf.isReleased", equalTo(true));

        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetId, apiToken);
        assertEquals(200, destroyDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        assertEquals(200, deleteUserResponse.getStatusCode());        
    }

    /**
     * In order for this test to pass you must have the Data Capture Module (
     * https://github.com/sbgrid/data-capture-module ) running. We assume that
     * most developers haven't set up the DCM so the test is disabled.
     */
    @Test
    public void testCreateDatasetWithDcmDependency() {

        boolean disabled = true;

        if (disabled) {
            return;
        }

        // TODO: Test this with a value like "junk" rather than a valid URL which would give `java.net.MalformedURLException: no protocol`.
        // The DCM Vagrant box runs on port 8888: https://github.com/sbgrid/data-capture-module/blob/master/Vagrantfile
        String dcmVagrantUrl = "http://localhost:8888";
        // The DCM mock runs on port 5000: https://github.com/sbgrid/data-capture-module/blob/master/doc/mock.md
        String dcmMockUrl = "http://localhost:5000";
        String dcmUrl = dcmMockUrl;
        Response setDcmUrl = UtilIT.setSetting(SettingsServiceBean.Key.DataCaptureModuleUrl, dcmUrl);
        setDcmUrl.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response setUploadMethods = UtilIT.setSetting(SettingsServiceBean.Key.UploadMethods, SystemConfig.FileUploadMethods.RSYNC.toString());
        setUploadMethods.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response urlConfigured = given()
                //                .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/settings/" + SettingsServiceBean.Key.DataCaptureModuleUrl.toString());
        if (urlConfigured.getStatusCode() != 200) {
            fail(SettingsServiceBean.Key.DataCaptureModuleUrl + " has not been not configured. This test cannot run without it.");
        }

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        long userId = JsonPath.from(createUser.body().asString()).getLong("data.authenticatedUser.id");

        /**
         * @todo Query system to see which file upload mechanisms are available.
         */
//        String dataverseAlias = "dv" + UtilIT.getRandomIdentifier();
//        List<String> fileUploadMechanismsEnabled = Arrays.asList(Dataset.FileUploadMechanism.RSYNC.toString());
//        Response createDataverseResponse = UtilIT.createDataverse(dataverseAlias, fileUploadMechanismsEnabled, apiToken);
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                //                .body("data.alias", equalTo(dataverseAlias))
                //                .body("data.fileUploadMechanismsEnabled[0]", equalTo(Dataset.FileUploadMechanism.RSYNC.toString()))
                .statusCode(201);

        /**
         * @todo Make this configurable at runtime similar to
         * UtilIT.getRestAssuredBaseUri
         */
//        Response createDatasetResponse = UtilIT.createDatasetWithDcmDependency(dataverseAlias, apiToken);
//        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        Response getDatasetJson = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJson.prettyPrint();
        String protocol1 = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.protocol");
        String authority1 = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.authority");
        String identifier1 = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol1 + ":" + authority1 + "/" + identifier1;

        Response getDatasetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/" + datasetId);
        getDatasetResponse.prettyPrint();
        getDatasetResponse.then().assertThat()
                .statusCode(200);

//        final List<Map<String, ?>> dataTypeField = JsonPath.with(getDatasetResponse.body().asString())
//                .get("data.latestVersion.metadataBlocks.citation.fields.findAll { it.typeName == 'dataType' }");
//        logger.fine("dataTypeField: " + dataTypeField);
//        assertThat(dataTypeField.size(), equalTo(1));
//        assertEquals("dataType", dataTypeField.get(0).get("typeName"));
//        assertEquals("controlledVocabulary", dataTypeField.get(0).get("typeClass"));
//        assertEquals("X-Ray Diffraction", dataTypeField.get(0).get("value"));
//        assertTrue(dataTypeField.get(0).get("multiple").equals(false));
        String nullTokenToIndicateGuest = null;
        Response getRsyncScriptPermErrorGuest = UtilIT.getRsyncScript(datasetPersistentId, nullTokenToIndicateGuest);
        getRsyncScriptPermErrorGuest.prettyPrint();
        getRsyncScriptPermErrorGuest.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode())
                .contentType(ContentType.JSON)
                .body("message", equalTo(AbstractApiBean.RESPONSE_MESSAGE_AUTHENTICATED_USER_REQUIRED));

        Response createNoPermsUser = UtilIT.createRandomUser();
        String noPermsUsername = UtilIT.getUsernameFromResponse(createNoPermsUser);
        String noPermsApiToken = UtilIT.getApiTokenFromResponse(createNoPermsUser);

        Response getRsyncScriptPermErrorNonGuest = UtilIT.getRsyncScript(datasetPersistentId, noPermsApiToken);
        getRsyncScriptPermErrorNonGuest.then().assertThat()
                .contentType(ContentType.JSON)
                .body("message", equalTo("User @" + noPermsUsername + " is not permitted to perform requested action."))
                .statusCode(UNAUTHORIZED.getStatusCode());

        boolean stopEarlyBecauseYouDoNotHaveDcmInstalled = true;
        if (stopEarlyBecauseYouDoNotHaveDcmInstalled) {
            return;
        }
        
        boolean stopEarlyToVerifyTheScriptWasCreated = false;
        if (stopEarlyToVerifyTheScriptWasCreated) {
            logger.info("On the DCM, does /deposit/gen/upload-" + datasetId + ".bash exist? It should! Creating the dataset should be enough to create it.");
            return;
        }
        Response getRsyncScript = UtilIT.getRsyncScript(datasetPersistentId, apiToken);
        getRsyncScript.prettyPrint();
        System.out.println("content type: " + getRsyncScript.getContentType()); // text/html;charset=ISO-8859-1
        getRsyncScript.then().assertThat()
                .contentType(ContentType.TEXT)
                .statusCode(200);
        String rsyncScript = getRsyncScript.body().asString();
        System.out.println("script:\n" + rsyncScript);
        assertTrue(rsyncScript.startsWith("#!"));
        assertTrue(rsyncScript.contains("placeholder")); // The DCM mock script has the word "placeholder" in it.

        Response removeUploadMethods = UtilIT.deleteSetting(SettingsServiceBean.Key.UploadMethods);
        removeUploadMethods.then().assertThat()
                .statusCode(200);

        Response createUser2 = UtilIT.createRandomUser();
        String username2 = UtilIT.getUsernameFromResponse(createUser2);
        String apiToken2 = UtilIT.getApiTokenFromResponse(createUser2);

        Response createDataverseResponse2 = UtilIT.createRandomDataverse(apiToken2);
        createDataverseResponse2.prettyPrint();
        String dataverseAlias2 = UtilIT.getAliasFromResponse(createDataverseResponse2);

        Response createDatasetResponse2 = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias2, apiToken2);
        createDatasetResponse2.prettyPrint();
        Integer datasetId2 = JsonPath.from(createDatasetResponse2.body().asString()).getInt("data.id");
        System.out.println("dataset id: " + datasetId);

        Response attemptToGetRsyncScriptForNonRsyncDataset = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken2)
                .get("/api/datasets/" + datasetId2 + "/dataCaptureModule/rsync");
        attemptToGetRsyncScriptForNonRsyncDataset.prettyPrint();
        attemptToGetRsyncScriptForNonRsyncDataset.then().assertThat()
                .body("message", equalTo(":UploadMethods does not contain dcm/rsync+ssh."))
                .statusCode(METHOD_NOT_ALLOWED.getStatusCode());

    }

    /**
     * This test is just a test of notifications so a fully implemented dcm is
     * not necessary
     */
    @Test
    public void testDcmChecksumValidationMessages() throws IOException, InterruptedException {
        
        /*SEK 3/28/2018 This test needs more work
            Currently it is failing at around line 1114
            Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
            the CreateDatasetCommand is not getting the rsync script so the dataset is not being created
            so the whole test is failing
        */
        
        boolean disabled = true;

        if (disabled) {
            return;
        }

        // The DCM Vagrant box runs on port 8888: https://github.com/sbgrid/data-capture-module/blob/master/Vagrantfile
        String dcmVagrantUrl = "http://localhost:8888";
        // The DCM mock runs on port 5000: https://github.com/sbgrid/data-capture-module/blob/master/doc/mock.md
        String dcmMockUrl = "http://localhost:5000";
        String dcmUrl = dcmMockUrl;

        Response setDcmUrl = UtilIT.setSetting(SettingsServiceBean.Key.DataCaptureModuleUrl, dcmUrl);
        setDcmUrl.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response setUploadMethods = UtilIT.setSetting(SettingsServiceBean.Key.UploadMethods, SystemConfig.FileUploadMethods.RSYNC.toString());
        setUploadMethods.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response urlConfigured = given()
                .get("/api/admin/settings/" + SettingsServiceBean.Key.DataCaptureModuleUrl.toString());
        if (urlConfigured.getStatusCode() != 200) {
            fail(SettingsServiceBean.Key.DataCaptureModuleUrl + " has not been not configured. This test cannot run without it.");
        }

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        long userId = JsonPath.from(createUser.body().asString()).getLong("data.authenticatedUser.id");

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(201);

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        Response getDatasetJson = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJson.prettyPrint();
        Response getDatasetResponse = given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/" + datasetId);
        getDatasetResponse.prettyPrint();
        getDatasetResponse.then().assertThat()
                .statusCode(200);

        boolean stopEarlyToVerifyTheScriptWasCreated = false;
        if (stopEarlyToVerifyTheScriptWasCreated) {
            logger.info("On the DCM, does /deposit/gen/upload-" + datasetId + ".bash exist? It should! Creating the dataset should be enough to create it.");
            return;
        }

        Response createUser2 = UtilIT.createRandomUser();
        String apiToken2 = UtilIT.getApiTokenFromResponse(createUser2);

        Response createDataverseResponse2 = UtilIT.createRandomDataverse(apiToken2);
        createDataverseResponse2.prettyPrint();
        String dataverseAlias2 = UtilIT.getAliasFromResponse(createDataverseResponse2);

        Response createDatasetResponse2 = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias2, apiToken2);
        createDatasetResponse2.prettyPrint();
        Integer datasetId2 = JsonPath.from(createDatasetResponse2.body().asString()).getInt("data.id");

        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonBeforePublishing.prettyPrint();
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.identifier");
        logger.info("identifier: " + identifier);
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;
        logger.info("datasetPersistentId: " + datasetPersistentId);

        /**
         * Here we are pretending to be the Data Capture Module reporting on if
         * checksum validation success or failure. Don't notify the user on
         * success (too chatty) but do notify on failure.
         *
         * @todo On success a process should be kicked off to crawl the files so
         * they are imported into Dataverse. Once the crawling and importing is
         * complete, notify the user.
         *
         * @todo What authentication should be used here? The API token of the
         * user? (If so, pass the token in the initial upload request payload.)
         * This is suboptimal because of the security risk of having the Data
         * Capture Module store the API token. Or should Dataverse be able to be
         * configured so that it only will receive these messages from trusted
         * IP addresses? Should there be a shared secret that's used for *all*
         * requests from the Data Capture Module to Dataverse?
         */
        /*
        Can't find dataset - give bad dataset ID
         */
        JsonObjectBuilder wrongDataset = Json.createObjectBuilder();
        String fakeDatasetId = "78921457982457921";
        wrongDataset.add("status", "validation passed");
        Response createSuperuser = UtilIT.createRandomUser();
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        UtilIT.makeSuperUser(superuserUsername);
        Response datasetNotFound = UtilIT.dataCaptureModuleChecksumValidation(fakeDatasetId, wrongDataset.build(), superuserApiToken);
        datasetNotFound.prettyPrint();

        datasetNotFound.then().assertThat()
                .statusCode(404)
                .body("message", equalTo("Dataset with ID " + fakeDatasetId + " not found."));

        JsonObjectBuilder badNews = Json.createObjectBuilder();
        // Status options are documented at https://github.com/sbgrid/data-capture-module/blob/master/doc/api.md#post-upload
        badNews.add("status", "validation failed");
        Response uploadFailed = UtilIT.dataCaptureModuleChecksumValidation(datasetPersistentId, badNews.build(), superuserApiToken);
        uploadFailed.prettyPrint();

        uploadFailed.then().assertThat()
                /**
                 * @todo Double check that we're ok with 200 here. We're saying
                 * "Ok, the bad news was delivered." We had a success of
                 * informing the user of bad news.
                 */
                .statusCode(200)
                .body("data.message", equalTo("User notified about checksum validation failure."));

        Response authorsGetsBadNews = UtilIT.getNotifications(apiToken);
        authorsGetsBadNews.prettyPrint();
        authorsGetsBadNews.then().assertThat()
                .body("data.notifications[0].type", equalTo("CHECKSUMFAIL"))
                .statusCode(OK.getStatusCode());

        Response removeUploadMethods = UtilIT.deleteSetting(SettingsServiceBean.Key.UploadMethods);
        removeUploadMethods.then().assertThat()
                .statusCode(200);

        String uploadFolder = identifier.split("FK2/")[1];
        logger.info("uploadFolder: " + uploadFolder);

        /**
         * The "extra testing" involves having this REST Assured test do two
         * jobs done by the rsync script and the DCM. The rsync script creates
         * "files.sha" and (if checksum validation succeeds) the DCM moves the
         * files and the "files.sha" file into the uploadFolder.
         *
         * The whole test was disabled in ae6b0a7 so we are changing
         * doExtraTesting to true.
         */
        boolean doExtraTesting = true;

        if (doExtraTesting) {

            String SEP = java.io.File.separator;
            // Set this to where you keep your files in dev. It might be nice to have an API to query to get this location from Dataverse.
            // TODO: Think more about if dsDir should end with "/FK2" or not.
            String dsDir = "/usr/local/glassfish4/glassfish/domains/domain1/files/10.5072";
            String dsDirPlusIdentifier = dsDir + SEP + identifier;
            logger.info("dsDirPlusIdentifier: " + dsDirPlusIdentifier);
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(dsDirPlusIdentifier));
            String dsDirPlusIdentifierPlusUploadFolder = dsDir + SEP + identifier + SEP + uploadFolder;
            logger.info("dsDirPlusIdentifierPlusUploadFolder: " + dsDirPlusIdentifierPlusUploadFolder);
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(dsDirPlusIdentifierPlusUploadFolder));
            String checksumFilename = "files.sha";
            String filename1 = "file1.txt";
            String fileContent1 = "big data!";
            java.nio.file.Files.write(java.nio.file.Paths.get(dsDir + SEP + identifier + SEP + uploadFolder + SEP + checksumFilename), fileContent1.getBytes());
//            // This is actually the SHA-1 of a zero byte file. It doesn't seem to matter what you send to the DCM?
            String checksumFileContent = "da39a3ee5e6b4b0d3255bfef95601890afd80709 " + filename1;
            java.nio.file.Files.createFile(java.nio.file.Paths.get(dsDir + SEP + identifier + SEP + uploadFolder + SEP + filename1));
            java.nio.file.Files.write(java.nio.file.Paths.get(dsDir + SEP + identifier + SEP + uploadFolder + SEP + checksumFilename), checksumFileContent.getBytes());

        }
        int totalSize = 1234567890;
        /**
         * @todo How can we test what the checksum validation notification looks
         * like in the GUI? There is no API for retrieving notifications.
         *
         * @todo How can we test that the email notification looks ok?
         */
        JsonObjectBuilder goodNews = Json.createObjectBuilder();
        goodNews.add("status", "validation passed");
        goodNews.add("uploadFolder", uploadFolder);
        goodNews.add("totalSize", totalSize);
        Response uploadSuccessful = UtilIT.dataCaptureModuleChecksumValidation(datasetPersistentId, goodNews.build(), superuserApiToken);
        /**
         * Errors here are expected unless you've set doExtraTesting to true to
         * do the jobs of the rsync script and the DCM to create the files.sha
         * file and put it and the data files in place. You might see stuff
         * like: "message": "Uploaded files have passed checksum validation but
         * something went wrong while attempting to put the files into
         * Dataverse. Status code was 400 and message was 'Dataset directory is
         * invalid.'."
         */
        uploadSuccessful.prettyPrint();

        if (doExtraTesting) {

            uploadSuccessful.then().assertThat()
                    .statusCode(200)
                    .body("data.message", equalTo("FileSystemImportJob in progress"));

            if (doExtraTesting) {

                long millisecondsNeededForFileSystemImportJobToFinish = 1000;
                Thread.sleep(millisecondsNeededForFileSystemImportJobToFinish);
                Response datasetAsJson2 = UtilIT.nativeGet(datasetId, apiToken);
                datasetAsJson2.prettyPrint();
                datasetAsJson2.then().assertThat()
                        .statusCode(OK.getStatusCode())
                        .body("data.latestVersion.files[0].dataFile.filename", equalTo(uploadFolder))
                        .body("data.latestVersion.files[0].dataFile.contentType", equalTo("application/vnd.dataverse.file-package"))
                        .body("data.latestVersion.files[0].dataFile.filesize", equalTo(totalSize))
                        .body("data.latestVersion.files[0].dataFile.checksum.type", equalTo("SHA-1"));
            }
        }
        logger.info("username/password: " + username);
    }
    
    @Test
    public void testCreateDeleteDatasetLink() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        
        Response superuserResponse = UtilIT.makeSuperUser(username);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);
        
        // This should fail, because we are attempting to link the dataset 
        // to its own dataverse:
        Response publishTargetDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishTargetDataverse.prettyPrint();
        publishTargetDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDatasetForLinking = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetForLinking.prettyPrint();
        publishDatasetForLinking.then().assertThat()
                .statusCode(OK.getStatusCode());
                        
        Response createLinkingDatasetResponse = UtilIT.createDatasetLink(datasetId.longValue(), dataverseAlias, apiToken);
        createLinkingDatasetResponse.prettyPrint();
        createLinkingDatasetResponse.then().assertThat()
                .body("message", equalTo("Can't link a dataset to its dataverse"))
                .statusCode(FORBIDDEN.getStatusCode());
        
        // OK, let's create a different random dataverse:
        createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        publishTargetDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDatasetForLinking.prettyPrint();
        publishTargetDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        // And link the dataset to this new dataverse:
        createLinkingDatasetResponse = UtilIT.createDatasetLink(datasetId.longValue(), dataverseAlias, apiToken);
        createLinkingDatasetResponse.prettyPrint();
        createLinkingDatasetResponse.then().assertThat()
                .body("data.message", equalTo("Dataset " + datasetId +" linked successfully to " + dataverseAlias))
                .statusCode(200);

        // Create a new user that doesn't have permission to delete the link
        Response createUser2 = UtilIT.createRandomUser();
        createUser2.prettyPrint();
        String username2 = UtilIT.getUsernameFromResponse(createUser2);
        String apiToken2 = UtilIT.getApiTokenFromResponse(createUser2);
        // Try to delete the link without PublishDataset permissions
        Response deleteLinkingDatasetResponse = UtilIT.deleteDatasetLink(datasetId.longValue(), dataverseAlias, apiToken2);
        deleteLinkingDatasetResponse.prettyPrint();
        deleteLinkingDatasetResponse.then().assertThat()
                .body("message", equalTo("User @" + username2 + " is not permitted to perform requested action."))
                .statusCode(UNAUTHORIZED.getStatusCode());

        // Add the Curator role to this user to show that they can delete the link later. (Timing issues if you try to delete right after giving permission)
        Response givePermissionResponse = UtilIT.grantRoleOnDataset(datasetPersistentId, "curator", "@" + username2, apiToken);
        givePermissionResponse.prettyPrint();
        givePermissionResponse.then().assertThat()
                .statusCode(200);

        // And now test deleting it as superuser:
        deleteLinkingDatasetResponse = UtilIT.deleteDatasetLink(datasetId.longValue(), dataverseAlias, apiToken);
        deleteLinkingDatasetResponse.prettyPrint();

        deleteLinkingDatasetResponse.then().assertThat()
                .body("data.message", equalTo("Link from Dataset " + datasetId + " to linked Dataverse " + dataverseAlias + " deleted"))
                .statusCode(200);

        // And re-link the dataset to this new dataverse:
        createLinkingDatasetResponse = UtilIT.createDatasetLink(datasetId.longValue(), dataverseAlias, apiToken);
        createLinkingDatasetResponse.prettyPrint();
        createLinkingDatasetResponse.then().assertThat()
                .body("data.message", equalTo("Dataset " + datasetId +" linked successfully to " + dataverseAlias))
                .statusCode(200);

        // And now test deleting it as user2 with new role as curator (Publish permissions):
        deleteLinkingDatasetResponse = UtilIT.deleteDatasetLink(datasetId.longValue(), dataverseAlias, apiToken2);
        deleteLinkingDatasetResponse.prettyPrint();

        deleteLinkingDatasetResponse.then().assertThat()
                .body("data.message", equalTo("Link from Dataset " + datasetId + " to linked Dataverse " + dataverseAlias + " deleted"))
                .statusCode(200);
    }
    
    @Test
    @Disabled
    public void testApiErrors() {

        /*
        Issue 8859
        This test excerises the issue where the update apis fail when the dataset is out of compliance with
        the requirement that datasets containing restricted files must allow reuest access or have terms of access.
        It's impossible to get a dataset into this state via the version 5.11.1 or later so if you needc to
        run this test you must set it up manually by setting requestAccess on TermsOfUseAccess to false 
        with an update query.
        Update the decalarations below with values from your test environment
         */
        String datasetPid = "doi:10.5072/FK2/7LDIUU";
        String apiToken = "1fab5406-0f03-47de-9a5b-d36d1d683a50";
        Long datasetId = 902L;

        String newDescription = "{\"citation:dsDescription\": {\"citation:dsDescriptionValue\": \"New description\"},  \"@context\":{\"citation\": \"https://dataverse.org/schema/citation/\"}}";
        Response response = UtilIT.updateDatasetJsonLDMetadata(datasetId.intValue(), apiToken, newDescription, false);
        response.then().assertThat().statusCode(CONFLICT.getStatusCode());
        assertTrue(response.prettyPrint().contains("You must enable request access or add terms of access in datasets with restricted files"));


        String pathToJsonFile = "doc/sphinx-guides/source/_static/api/dataset-update-metadata.json";
        Response updateTitle = UtilIT.updateDatasetMetadataViaNative(datasetPid, pathToJsonFile, apiToken);
        updateTitle.prettyPrint();
        assertEquals(CONFLICT.getStatusCode(), updateTitle.getStatusCode());
        assertTrue(updateTitle.prettyPrint().contains("You must enable request access or add terms of access in datasets with restricted files"));

        String basicFileName = "004.txt";

        String basicPathToFile = "scripts/search/data/replace_test/" + basicFileName;

        Response basicAddResponse = UtilIT.uploadFileViaNative(datasetId.toString(), basicPathToFile, apiToken);
        basicAddResponse.prettyPrint();
        assertEquals(CONFLICT.getStatusCode(), basicAddResponse.getStatusCode());
        assertTrue(basicAddResponse.prettyPrint().contains("You must enable request access or add terms of access in datasets with restricted files"));

        Response deleteFile = UtilIT.deleteFile(907, apiToken);
        deleteFile.prettyPrint();
        assertEquals(BAD_REQUEST.getStatusCode(), deleteFile.getStatusCode());
        
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken);
        publishDataset.prettyPrint();
        assertEquals(409, publishDataset.getStatusCode());
        assertTrue(publishDataset.prettyPrint().contains("You must enable request access or add terms of access in datasets with restricted files"));

    }

    
    @Test
    public void testDatasetLocksApi() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        
        Response superuserResponse = UtilIT.makeSuperUser(username);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        String persistentIdentifier = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);
        
        // This should return an empty list, as the dataset should have no locks just yet:
        Response checkDatasetLocks = UtilIT.checkDatasetLocks(datasetId.longValue(), null, apiToken);
        checkDatasetLocks.prettyPrint();
        JsonArray emptyArray = Json.createArrayBuilder().build();
        checkDatasetLocks.then().assertThat()
                .body("data", equalTo(emptyArray))
                .statusCode(200);
        
        // Lock the dataset with an ingest lock: 
        Response lockDatasetResponse = UtilIT.lockDataset(datasetId.longValue(), "Ingest", apiToken);
        lockDatasetResponse.prettyPrint();
        lockDatasetResponse.then().assertThat()
                .body("data.message", equalTo("dataset locked with lock type Ingest"))
                .statusCode(200);
        
        // Check again: 
        // This should return an empty list, as the dataset should have no locks just yet:
        checkDatasetLocks = UtilIT.checkDatasetLocks(datasetId.longValue(), "Ingest", apiToken);
        checkDatasetLocks.prettyPrint();
        checkDatasetLocks.then().assertThat()
                .body("data[0].lockType", equalTo("Ingest"))
                .statusCode(200);
        
        // Try to lock the dataset with the same type lock, AGAIN 
        // (this should fail, of course!)
        lockDatasetResponse = UtilIT.lockDataset(datasetId.longValue(), "Ingest", apiToken);
        lockDatasetResponse.prettyPrint();
        lockDatasetResponse.then().assertThat()
                .body("message", equalTo("dataset already locked with lock type Ingest"))
                .statusCode(FORBIDDEN.getStatusCode());
        
        // Let's also test the new (as of 5.10) API that lists the locks 
        // present across all datasets. 
        
        // First, we'll try listing ALL locks currently in the system, and make sure that the ingest lock 
        // for this dataset is on the list:
        checkDatasetLocks = UtilIT.listAllLocks(apiToken);
        checkDatasetLocks.prettyPrint();
        checkDatasetLocks.then().assertThat()
                .statusCode(200);
        
        boolean lockListedCorrectly = false;
        List<Map<String, String>> listedLockEntries = checkDatasetLocks.body().jsonPath().getList("data");
        for (int i = 0; i < listedLockEntries.size(); i++) {
            if ("Ingest".equals(listedLockEntries.get(i).get("lockType"))
                    && username.equals(listedLockEntries.get(i).get("user"))
                    && persistentIdentifier.equals(listedLockEntries.get(i).get("dataset"))) {
                lockListedCorrectly = true; 
                break;
            } 
        }
        assertTrue(lockListedCorrectly, "Lock missing from the output of /api/datasets/locks");
        
        // Try the same, but with an api token of a random, non-super user 
        // (this should get rejected):
        createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String wrongApiToken = UtilIT.getApiTokenFromResponse(createUser);
        checkDatasetLocks = UtilIT.listAllLocks(wrongApiToken);
        checkDatasetLocks.prettyPrint();
        checkDatasetLocks.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());
        
        // Try to narrow the listing down to the lock of type=Ingest specifically; 
        // verify that the lock in question is still being listed:
        checkDatasetLocks = UtilIT.listLocksByType("Ingest", apiToken);
        checkDatasetLocks.prettyPrint();
        // We'll again assume that it's possible that the API is going to list 
        // *multiple* locks; i.e. that there are other datasets with the lock 
        // of type "Ingest" on them. So we'll go through the list and look for the
        // lock for this specific dataset again. 
        lockListedCorrectly = false;
        listedLockEntries = checkDatasetLocks.body().jsonPath().getList("data");
        for (int i = 0; i < listedLockEntries.size(); i++) {
            if ("Ingest".equals(listedLockEntries.get(i).get("lockType"))
                    && username.equals(listedLockEntries.get(i).get("user"))
                    && persistentIdentifier.equals(listedLockEntries.get(i).get("dataset"))) {
                lockListedCorrectly = true; 
                break;
            } 
        }
        assertTrue(lockListedCorrectly, "Lock missing from the output of /api/datasets/locks?type=Ingest");

        
        // Try to list locks of an invalid type:
        checkDatasetLocks = UtilIT.listLocksByType("BadLockType", apiToken);
        checkDatasetLocks.prettyPrint();
        checkDatasetLocks.then().assertThat()
                .body("message", startsWith("Invalid lock type value: BadLockType"))
                .statusCode(BAD_REQUEST.getStatusCode());
       
        // List the locks owned by the current user; verify that the lock above 
        // is still listed:
        checkDatasetLocks = UtilIT.listLocksByUser(username, apiToken);
        checkDatasetLocks.prettyPrint();
        // Safe to assume there should be only one:
        checkDatasetLocks.then().assertThat()
                .body("data[0].lockType", equalTo("Ingest"))
                .body("data[0].user", equalTo(username))
                .body("data[0].dataset", equalTo(persistentIdentifier))
                .statusCode(200);
        
        // Further narrow down the listing to both the type AND user: 
        checkDatasetLocks = UtilIT.listLocksByTypeAndUser("Ingest", username, apiToken);
        checkDatasetLocks.prettyPrint();
        // Even safer to assume there should be only one:
        checkDatasetLocks.then().assertThat()
                .statusCode(200)
                .body("data[0].lockType", equalTo("Ingest"))
                .body("data[0].user", equalTo(username))
                .body("data[0].dataset", equalTo(persistentIdentifier));
                
        
        // Finally, try asking for the locks owned by this user AND of type "InReview". 
        // This should produce an empty list:
        checkDatasetLocks = UtilIT.listLocksByTypeAndUser("InReview", username, apiToken);
        checkDatasetLocks.prettyPrint();
        checkDatasetLocks.then().assertThat()
                .statusCode(200)
                .body("data", equalTo(emptyArray));
        
        // And now test deleting the lock:
        Response unlockDatasetResponse = UtilIT.unlockDataset(datasetId.longValue(), "Ingest", apiToken);
        unlockDatasetResponse.prettyPrint();
        
        unlockDatasetResponse.then().assertThat()
                .body("data.message", equalTo("lock type Ingest removed"))
                .statusCode(200);
        
        // ... and check for the lock on the dataset again, this time by specific lock type: 
        // (should return an empty list, now that we have unlocked it)
        checkDatasetLocks = UtilIT.checkDatasetLocks(datasetId.longValue(), "Ingest", apiToken);
        checkDatasetLocks.prettyPrint();
        checkDatasetLocks.then().assertThat()
                .body("data", equalTo(emptyArray))
                .statusCode(200);
    }
    
    /**
     * This test requires the root dataverse to be published to pass.
     */
    @Test
    @Disabled
    public void testUpdatePIDMetadataAPI() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonBeforePublishing.prettyPrint();
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());
        //Return random sleep  9/13/2019
        //without it we've seen DB deadlocks
        try {
            Thread.sleep(3000l);
        } catch (InterruptedException iex){}
      
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());

        Response getDatasetJsonAfterPublishing = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonAfterPublishing.prettyPrint();
        getDatasetJsonAfterPublishing.then().assertThat()
                .body("data.latestVersion.versionNumber", equalTo(1))
                .body("data.latestVersion.versionMinorNumber", equalTo(0))
                .body("data.latestVersion.metadataBlocks.citation.fields[2].value[0].datasetContactEmail.value", equalTo("finch@mailinator.com"))
                .statusCode(OK.getStatusCode());

        String pathToJsonFilePostPub = "doc/sphinx-guides/source/_static/api/dataset-add-metadata-after-pub.json";
        Response addDataToPublishedVersion = UtilIT.addDatasetMetadataViaNative(datasetPersistentId, pathToJsonFilePostPub, apiToken);
        addDataToPublishedVersion.prettyPrint();
        addDataToPublishedVersion.then().assertThat().statusCode(OK.getStatusCode());

        Response updatePIDMetadata = UtilIT.updateDatasetPIDMetadata(datasetPersistentId, apiToken);
        updatePIDMetadata.prettyPrint();
        updatePIDMetadata.then().assertThat()
                .statusCode(OK.getStatusCode());
        logger.info("datasetPersistentId: " + datasetPersistentId);

    }
    
    @Test
    public void testUpdateDatasetVersionWithFiles() throws InterruptedException {
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

       String pathToJsonFile = "src/test/resources/json/update-dataset-version-with-files.json";

        Response updateMetadataAddFilesViaNative = UtilIT.updateDatasetMetadataViaNative(datasetPersistentId, pathToJsonFile, authorApiToken);
        updateMetadataAddFilesViaNative.prettyPrint();
        updateMetadataAddFilesViaNative.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

        // These println's are here in case you want to log into the GUI to see what notifications look like.
        System.out.println("Curator username/password: " + curatorUsername);
        System.out.println("Author username/password: " + authorUsername);

    }
    
    @Test
    public void testLinkingDatasets() {

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

        // Link dataset to second dataverse.
        //should fail if dataset is not published
        Response linkDataset = UtilIT.linkDataset(datasetPid, dataverse2Alias, superuserApiToken);
        linkDataset.prettyPrint();
        linkDataset.then().assertThat()
                .body("message", equalTo(BundleUtil.getStringFromBundle("dataset.link.not.available")))
                .statusCode(FORBIDDEN.getStatusCode());

        UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken).then().assertThat()
                .statusCode(OK.getStatusCode());

        //Once published you should be able to link it
        linkDataset = UtilIT.linkDataset(datasetPid, dataverse2Alias, superuserApiToken);
        linkDataset.prettyPrint();
        linkDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Link another to test the list of linked datasets
        Response createDataverse3 = UtilIT.createRandomDataverse(apiToken);
        createDataverse3.prettyPrint();
        createDataverse3.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverse3Alias = UtilIT.getAliasFromResponse(createDataverse3);
        Integer dataverse3Id = UtilIT.getDatasetIdFromResponse(createDataverse3);
        linkDataset = UtilIT.linkDataset(datasetPid, dataverse3Alias, superuserApiToken);
        linkDataset.prettyPrint();
        linkDataset.then().assertThat()
                .statusCode(OK.getStatusCode());
        // get the list in Json format
        Response linkDatasetsResponse = UtilIT.getDatasetLinks(datasetPid, superuserApiToken);
        linkDatasetsResponse.prettyPrint();
        linkDatasetsResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        JsonObject linkDatasets = Json.createReader(new StringReader(linkDatasetsResponse.asString())).readObject();
        JsonArray lst = linkDatasets.getJsonObject("data").getJsonArray("linked-dataverses");
        List<Integer> ids = List.of(dataverse2Id, dataverse3Id);
        List<Integer> uniqueids = new ArrayList<>();
        assertEquals(ids.size(), lst.size());
        for (int i = 0; i < lst.size(); i++) {
            int id = lst.getJsonObject(i).getInt("id");
            assertTrue(ids.contains(id));
            assertFalse(uniqueids.contains(id));
            uniqueids.add(id);
        }

//Experimental code for trying to trick test into thinking the dataset has been harvested
/*
createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverse1Alias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId2 = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid2 = JsonPath.from(createDataset.asString()).getString("data.persistentId");
        
        linkDataset = UtilIT.linkDataset(datasetPid2, dataverse2Alias, superuserApiToken);
        linkDataset.prettyPrint();
        linkDataset.then().assertThat()
                .body("message", equalTo( BundleUtil.getStringFromBundle("dataset.link.not.available")))
                .statusCode(FORBIDDEN.getStatusCode());
                EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        // Do stuff...
        entityManager.createNativeQuery("UPDATE dataset SET harvestingclient_id=1 WHERE id="+datasetId2).executeUpdate();
        entityManager.getTransaction().commit();
        entityManager.close();

        
        UtilIT.linkDataset(datasetId2.toString(), dataverse2Alias, superuserApiToken);
        linkDataset.prettyPrint();
        linkDataset.then().assertThat()
                .statusCode(OK.getStatusCode());
         */
    }

    /**
     * This tests the "DDI export" and verifies that variable metadata is included for an unrestricted file.
     */
    @Test
    public void testUnrestrictedFileExportDdi() throws IOException {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String authorUsername = UtilIT.getUsernameFromResponse(createUser);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        Path pathToFile = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "data.csv");
        String contentOfCsv = ""
                + "name,pounds,species\n"
                + "Marshall,40,dog\n"
                + "Tiger,17,cat\n"
                + "Panther,21,cat\n";
        java.nio.file.Files.write(pathToFile, contentOfCsv.getBytes());

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile.toString(), authorApiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("data.csv"));

        String fileId = JsonPath.from(uploadFile.body().asString()).getString("data.files[0].dataFile.id");

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", authorApiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile);

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, authorApiToken);
        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", authorApiToken);
        publishDataset.then().assertThat().statusCode(OK.getStatusCode());

        // We're testing export here, which is at dataset level.
        // Guest/public version
        Response exportByGuest = UtilIT.exportDataset(datasetPid, "ddi");
        exportByGuest.prettyPrint();
        exportByGuest.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("data.tab"))
                .body("codeBook.fileDscr.fileTxt.dimensns.caseQnty", equalTo("3"))
                .body("codeBook.fileDscr.fileTxt.dimensns.varQnty", equalTo("3"))
                .body("codeBook.dataDscr", CoreMatchers.not(equalTo(null)))
                .body("codeBook.dataDscr.var[0].@name", equalTo("name"))
                .body("codeBook.dataDscr.var[1].@name", equalTo("pounds"))
                // This is an example of a summary stat (max) that should be visible.
                .body("codeBook.dataDscr.var[1].sumStat.find { it.@type == 'max' }", equalTo("40.0"))
                .body("codeBook.dataDscr.var[2].@name", equalTo("species"));
    }
        
    /**
     * In this test we are restricting a file and testing "export DDI" at the
     * dataset level as well as getting the DDI at the file level.
     *
     * Export at the dataset level is always the public version.
     *
     * At the file level, you can still get summary statistics (and friends,
     * "dataDscr") if you have access to download the file. If you don't have
     * access, you get an error.
     */
    @Test
    public void testRestrictFileExportDdi() throws IOException {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String authorUsername = UtilIT.getUsernameFromResponse(createUser);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        Path pathToFile = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "data.csv");
        String contentOfCsv = ""
                + "name,pounds,species\n"
                + "Marshall,40,dog\n"
                + "Tiger,17,cat\n"
                + "Panther,21,cat\n";
        java.nio.file.Files.write(pathToFile, contentOfCsv.getBytes());

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile.toString(), authorApiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("data.csv"));

        String fileId = JsonPath.from(uploadFile.body().asString()).getString("data.files[0].dataFile.id");

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", authorApiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile);

        Response restrictFile = UtilIT.restrictFile(fileId, true, authorApiToken);
        restrictFile.prettyPrint();
        restrictFile.then().assertThat().statusCode(OK.getStatusCode());

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, authorApiToken);
        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", authorApiToken);
        publishDataset.then().assertThat().statusCode(OK.getStatusCode());

        // We're testing export here, which is at dataset level.
        // Guest/public version
        Response exportByGuest = UtilIT.exportDataset(datasetPid, "ddi");
        exportByGuest.prettyPrint();
        exportByGuest.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("data.tab"));

        // Here we are asserting that dataDscr is empty. TODO: Do this in REST Assured.
        String dataDscrForGuest = XmlPath.from(exportByGuest.asString()).getString("codeBook.dataDscr");
        assertEquals("", dataDscrForGuest);

        // Author export (has access)
        Response exportByAuthor = UtilIT.exportDataset(datasetPid, "ddi", authorApiToken);
        exportByAuthor.prettyPrint();
        exportByAuthor.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("data.tab"));

        // Here we are asserting that dataDscr is empty. TODO: Do this in REST Assured.
        String dataDscrForAuthor = XmlPath.from(exportByAuthor.asString()).getString("codeBook.dataDscr");
        assertEquals("", dataDscrForAuthor);

        // Now we are testing file-level retrieval.
        // The author has access to a restricted file and gets all the metadata.
        Response fileMetadataDdiAuthor = UtilIT.getFileMetadata(fileId, "ddi", authorApiToken);
        fileMetadataDdiAuthor.prettyPrint();
        fileMetadataDdiAuthor.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("data.tab"))
                //                .body("codeBook", containsString("dataDscr"))
                // The names of all these variables (name, pounds, species) should be visible.
                .body("codeBook.dataDscr", CoreMatchers.not(equalTo(null)))
                //                .body("codeBook.dataDscr", equalTo(null))
                .body("codeBook.dataDscr.var[0].@name", equalTo("name"))
                .body("codeBook.dataDscr.var[1].@name", equalTo("pounds"))
                // This is an example of a summary stat (max) that should be visible.
                .body("codeBook.dataDscr.var[1].sumStat.find { it.@type == 'max' }", equalTo("40.0"))
                .body("codeBook.dataDscr.var[2].@name", equalTo("species"));

        Response createUserNoAuth = UtilIT.createRandomUser();
        createUserNoAuth.prettyPrint();
        String usernameNoAuth = UtilIT.getUsernameFromResponse(createUserNoAuth);
        String apiTokenNoAuth = UtilIT.getApiTokenFromResponse(createUserNoAuth);

        // Users with no access to the restricted file are blocked.
        Response fileMetadataDdiUserNoAuth = UtilIT.getFileMetadata(fileId, "ddi", apiTokenNoAuth);
        fileMetadataDdiUserNoAuth.prettyPrint();
        fileMetadataDdiUserNoAuth.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("You do not have permission to download this file."));

        // Guest users (not logged in) are also blocked.
        Response fileMetadataDdiGuest = UtilIT.getFileMetadata(fileId, "ddi");
        fileMetadataDdiGuest.prettyPrint();
        fileMetadataDdiGuest.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("You do not have permission to download this file."));
    }

    @Test
    public void testSemanticMetadataAPIs() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Create a dataset using native api
        // (this test requires that we create a dataset without the license set; note the extra boolean arg.:)
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken, true);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        // Get the metadata with the semantic api
        Response response = UtilIT.getDatasetJsonLDMetadata(datasetId, apiToken);
        response.then().assertThat().statusCode(OK.getStatusCode());
        // Compare the metadata with an expected value - the metadatablock entries
        // should be the same but there will be additional fields with values related to
        // the dataset's creation (e.g. new id)
        String jsonLDString = getData(response.getBody().asString());
        JsonObject jo = null;
        try {
            jo = JSONLDUtil.decontextualizeJsonLD(jsonLDString);
        } catch (NoSuchMethodError e) {
            logger.info(ExceptionUtils.getStackTrace(e));
        }
        

        String expectedJsonLD = UtilIT.getDatasetJson("scripts/search/tests/data/dataset-finch1.jsonld");
        jo = Json.createObjectBuilder(jo).remove("@id").remove("http://schema.org/dateModified").build();
        String jsonLD = jo.toString();

        // ToDo: Are the static pars as expected
        JSONAssert.assertEquals(expectedJsonLD, jsonLD, false);
        // Now change the title
        response = UtilIT.updateDatasetJsonLDMetadata(datasetId, apiToken,
                "{\"title\": \"New Title\", \"@context\":{\"title\": \"http://purl.org/dc/terms/title\"}}", true);
        response.then().assertThat().statusCode(OK.getStatusCode());

        response = UtilIT.getDatasetJsonLDMetadata(datasetId, apiToken);
        response.then().assertThat().statusCode(OK.getStatusCode());

        // Check that the semantic api returns the new title
        jsonLDString = getData(response.getBody().asString());
        JsonObject jsonLDObject = JSONLDUtil.decontextualizeJsonLD(jsonLDString);
        assertEquals("New Title", jsonLDObject.getString("http://purl.org/dc/terms/title"));

        // Add an additional description (which is multi-valued and compound)
        // Also add new terms of use (single value so would fail with replace false if a
        // value existed)
        String newDescription = "{\"citation:dsDescription\": {\"citation:dsDescriptionValue\": \"New description\"}, \"https://dataverse.org/schema/core#termsOfUse\": \"New terms\", \"@context\":{\"citation\": \"https://dataverse.org/schema/citation/\"}}";
        response = UtilIT.updateDatasetJsonLDMetadata(datasetId, apiToken, newDescription, false);
        response.then().assertThat().statusCode(OK.getStatusCode());

        response = UtilIT.getDatasetJsonLDMetadata(datasetId, apiToken);
        response.then().assertThat().statusCode(OK.getStatusCode());

        // Look for a second description
        jsonLDString = getData(response.getBody().asString());
        jsonLDObject = JSONLDUtil.decontextualizeJsonLD(jsonLDString);
        assertEquals("New description",
                ((JsonObject) jsonLDObject.getJsonArray("https://dataverse.org/schema/citation/dsDescription").get(1))
                        .getString("https://dataverse.org/schema/citation/dsDescriptionValue"));

        // Can't add terms of use with replace=false and a value already set (single
        // valued field)
        String badTerms = "{\"https://dataverse.org/schema/core#termsOfUse\": \"Bad terms\"}}";
        response = UtilIT.updateDatasetJsonLDMetadata(datasetId, apiToken, badTerms, false);
        response.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        
        //We publish the dataset and dataverse      
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());    
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());
        
        //We check the version is published
        response = UtilIT.getDatasetJsonLDMetadata(datasetId, apiToken);
        response.prettyPrint();
        jsonLDString = getData(response.getBody().asString());
        jsonLDObject = JSONLDUtil.decontextualizeJsonLD(jsonLDString);
        String publishedVersion = jsonLDObject.getString("http://schema.org/version");
        assertNotEquals("DRAFT", publishedVersion);

        // Upload a file so a draft version is created
        String pathToFile = "src/main/webapp/resources/images/cc0.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadResponse.prettyPrint();
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());
        int fileID = uploadResponse.jsonPath().getInt("data.files[0].dataFile.id");
        
        //We check the authenticated user gets DRAFT
        response = UtilIT.getDatasetJsonLDMetadata(datasetId, apiToken);
        response.prettyPrint(); 
        jsonLDString = getData(response.getBody().asString());
        jsonLDObject = JSONLDUtil.decontextualizeJsonLD(jsonLDString);
        assertEquals("DRAFT", jsonLDObject.getString("http://schema.org/version"));
        
        // Create user with no permission and check they get published version
        String apiTokenNoPerms = UtilIT.createRandomUserGetToken();
        response = UtilIT.getDatasetJsonLDMetadata(datasetId, apiTokenNoPerms);
        response.prettyPrint();
        jsonLDString = getData(response.getBody().asString());
        jsonLDObject = JSONLDUtil.decontextualizeJsonLD(jsonLDString);
        assertNotEquals("DRAFT", jsonLDObject.getString("http://schema.org/version"));
        
        // Delete the file
        Response deleteFileResponse = UtilIT.deleteFileInDataset(fileID, apiToken);
        deleteFileResponse.prettyPrint();
        deleteFileResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Delete the terms of use
        response = UtilIT.deleteDatasetJsonLDMetadata(datasetId, apiToken,
                "{\"https://dataverse.org/schema/core#termsOfUse\": \"New terms\"}");
        response.then().assertThat().statusCode(OK.getStatusCode());

        response = UtilIT.getDatasetJsonLDMetadata(datasetId, apiToken);
        response.then().assertThat().statusCode(OK.getStatusCode());

        // Verify that they're gone
        jsonLDString = getData(response.getBody().asString());
        jsonLDObject = JSONLDUtil.decontextualizeJsonLD(jsonLDString);
        assertTrue(!jsonLDObject.containsKey("https://dataverse.org/schema/core#termsOfUse"));

        //Delete the DRAFT dataset
        Response deleteDraftResponse = UtilIT.deleteDatasetVersionViaNativeApi(datasetId, DS_VERSION_DRAFT, apiToken);
        deleteDraftResponse.prettyPrint();
        deleteDraftResponse.then().assertThat().statusCode(OK.getStatusCode());

        //We set the user as superuser so we can delete the published dataset
        Response superUserResponse = UtilIT.makeSuperUser(username);
        superUserResponse.prettyPrint();
        deleteDraftResponse.then().assertThat().statusCode(OK.getStatusCode());

        //Delete the published dataset
        Response deletePublishedResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deletePublishedResponse.prettyPrint();
        deleteDraftResponse.then().assertThat().statusCode(OK.getStatusCode());

        //Delete the dataverse
        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        //Delete the user
        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

    }
    
    @Test
    public void testReCreateDataset() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Create a dataset using native API
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        // Get the semantic metadata
        Response response = UtilIT.getDatasetJsonLDMetadata(datasetId, apiToken);
        response.then().assertThat().statusCode(OK.getStatusCode());
        response.prettyPeek();
        String expectedString = getData(response.getBody().asString());
        
        // Delete the dataset via native API
        Response deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());

        
        logger.info("SENDING to reCreate Dataset: " + expectedString);
        // Now use the migrate API to recreate the dataset
        // Now use the migrate API to recreate the dataset
        response = UtilIT.recreateDatasetJsonLD(apiToken, dataverseAlias, expectedString);
        response.prettyPeek();
        String body = response.getBody().asString();
        response.then().assertThat().statusCode(CREATED.getStatusCode());

        try (StringReader rdr = new StringReader(body)) {
            datasetId = Json.createReader(rdr).readObject().getJsonObject("data").getInt("id");
        }
        // Get the jsonLD metadata for what we recreated
        response = UtilIT.getDatasetJsonLDMetadata(datasetId, apiToken);
        response.then().assertThat().statusCode(OK.getStatusCode());

        String jsonLDString = getData(response.getBody().asString());
        JsonObject jsonLD = JSONLDUtil.decontextualizeJsonLD(jsonLDString);

        JsonObject expectedJsonLD = JSONLDUtil.decontextualizeJsonLD(expectedString);
        expectedJsonLD = Json.createObjectBuilder(expectedJsonLD).remove("@id").remove("http://schema.org/dateModified")
                .build();
        // ToDo: Assert that the semantic api response is the same (everything in the
        // expected version is in the new one - deleting the @id and dateModified means
        // those won't be compared (with last param = false)
        JSONAssert.assertEquals(expectedJsonLD.toString(), jsonLD.toString(), false);

        // Now cleanup
        deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
    }

    @Test
    public void testCurationLabelAPIs() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response setCurationLabelSets = UtilIT.setSetting(SettingsServiceBean.Key.AllowedCurationLabels, "{\"StandardProcess\":[\"Author contacted\", \"Privacy Review\", \"Awaiting paper publication\", \"Final Approval\"],\"AlternateProcess\":[\"State 1\",\"State 2\",\"State 3\"]}");
        setCurationLabelSets.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        
        //Set curation label set on dataverse
        //Valid option, bad user
        Response setDataverseCurationLabelSetResponse = UtilIT.setDataverseCurationLabelSet(dataverseAlias, apiToken, "AlternateProcess");
        setDataverseCurationLabelSetResponse.then().assertThat().statusCode(FORBIDDEN.getStatusCode());
        
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        //Non-existent option
        Response setDataverseCurationLabelSetResponse2 = UtilIT.setDataverseCurationLabelSet(dataverseAlias, apiToken, "OddProcess");
        setDataverseCurationLabelSetResponse2.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
        //Valid option, superuser
        Response setDataverseCurationLabelSetResponse3 = UtilIT.setDataverseCurationLabelSet(dataverseAlias, apiToken, "AlternateProcess");
        setDataverseCurationLabelSetResponse3.then().assertThat().statusCode(OK.getStatusCode());

        
        // Create a dataset using native api
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        // Get the curation label set in use
        Response response = UtilIT.getDatasetCurationLabelSet(datasetId, apiToken);
        response.then().assertThat().statusCode(OK.getStatusCode());
        //Verify that the set name is what was set on the dataverse
        String labelSetName = getData(response.getBody().asString());
        // full should be {"message":"AlternateProcess"}
        assertTrue(labelSetName.contains("AlternateProcess"));
        
        // Now set a label
        //Option from the wrong set
        Response response2 = UtilIT.setDatasetCurationLabel(datasetId, apiToken, "Author contacted");
        response2.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
        // Valid option
        Response response3 = UtilIT.setDatasetCurationLabel(datasetId, apiToken, "State 1");
        response3.then().assertThat().statusCode(OK.getStatusCode());
    }

    private JsonObject getDataAsJsonObject(String body) {
        try (StringReader rdr = new StringReader(body)) {
            return Json.createReader(rdr).readObject().getJsonObject("data");
        }
    }
    private String getData(String body) {
            return getDataAsJsonObject(body).toString();
    }

    @Test
    public void testFilesUnchangedAfterDatasetMetadataUpdate() throws IOException {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");
        Integer datasetId = JsonPath.from(createDataset.asString()).getInt("data.id");

        Path pathtoScript = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "run.sh");
        java.nio.file.Files.write(pathtoScript, "#!/bin/bash\necho hello".getBytes());

        JsonObjectBuilder json1 = Json.createObjectBuilder()
                .add("description", "A script to reproduce results.")
                .add("directoryLabel", "code");

        Response uploadReadme1 = UtilIT.uploadFileViaNative(datasetId.toString(), pathtoScript.toString(), json1.build(), apiToken);
        uploadReadme1.prettyPrint();
        uploadReadme1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("run.sh"))
                .body("data.files[0].directoryLabel", equalTo("code"));

        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());

        Response getDatasetJsonBeforeUpdate = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonBeforeUpdate.prettyPrint();
        getDatasetJsonBeforeUpdate.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.latestVersion.files[0].label", equalTo("run.sh"))
                .body("data.latestVersion.files[0].directoryLabel", equalTo("code"));
        
        String pathToJsonFile = "doc/sphinx-guides/source/_static/api/dataset-update-metadata.json";
        Response updateTitle = UtilIT.updateDatasetMetadataViaNative(datasetPid, pathToJsonFile, apiToken);
        updateTitle.prettyPrint();
        updateTitle.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        Response getDatasetJsonAfterUpdate = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonAfterUpdate.prettyPrint();
        getDatasetJsonAfterUpdate.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.latestVersion.files[0].label", equalTo("run.sh"))
                .body("data.latestVersion.files[0].directoryLabel", equalTo("code"));
        
    }


    
    @Test
    public void testCuratePublishedDatasetVersionCommand() throws IOException {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);
        
        
        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");
        Integer datasetId = JsonPath.from(createDataset.asString()).getInt("data.id");

        Path pathtoScript = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "run.sh");
        java.nio.file.Files.write(pathtoScript, "#!/bin/bash\necho hello".getBytes());

        JsonObjectBuilder json1 = Json.createObjectBuilder()
                .add("description", "A script to reproduce results.")
                .add("directoryLabel", "code");

        
        

        String pathToFileThatGoesThroughIngest = "src/test/resources/sav/dct.sav";
        Response uploadIngestableFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFileThatGoesThroughIngest, apiToken);
        uploadIngestableFile.then().assertThat()
                .statusCode(OK.getStatusCode());
        uploadIngestableFile.prettyPrint();

        String origFileId = JsonPath.from(uploadIngestableFile.body().asString()).getString("data.files[0].dataFile.id");

        System.out.println("Orig file id " + origFileId);

        logger.fine("Orig file id: " + origFileId);
        assertNotNull(origFileId);
        assertNotEquals("",origFileId);

        // Give file time to ingest
        
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFileThatGoesThroughIngest);
        
        Response origXml = UtilIT.getFileMetadata(origFileId, null, apiToken);
        assertEquals(200, origXml.getStatusCode());


        String stringOrigXml = origXml.getBody().prettyPrint();

        InputStream variableData = origXml.body().asInputStream();

        Map<Long, VariableMetadata> mapVarToVarMet = new HashMap<Long, VariableMetadata>();
        Map<Long,VarGroup> varGroupMap = new HashMap<Long, VarGroup>();
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader xmlr = factory.createXMLStreamReader(variableData);
            VariableMetadataDDIParser vmdp = new VariableMetadataDDIParser();

            vmdp.processDataDscr(xmlr, mapVarToVarMet, varGroupMap);

        } catch (XMLStreamException e) {
            logger.warning(e.getMessage());
            assertEquals(0,1);
        }


        //Test here
        String updatedContent = "";
        try {
            updatedContent = new String(Files.readAllBytes(Paths.get("src/test/resources/xml/dct.xml")));
        } catch (IOException e) {
            logger.warning(e.getMessage());
            assertEquals(0,1);
        }
        Long replV1168 = 0L;
        Long replV1169 = 0L;
        Long replV1170 = 0L;
        int numberVariables = 0;
        for (VariableMetadata var : mapVarToVarMet.values()) {
            if (var.getLabel().equals("gender")) {
                replV1170 = var.getDataVariable().getId();
                numberVariables = numberVariables +1;
            } else if (var.getLabel().equals("weight")) {
                replV1168 = var.getDataVariable().getId();
                numberVariables = numberVariables +1;
            } else if (var.getLabel().equals("age_rollup")) {
                replV1169 = var.getDataVariable().getId();
                numberVariables = numberVariables +1;
            }
        }
        assertEquals(3, numberVariables);

        updatedContent = updatedContent.replaceAll("v1168", "v" + replV1168 );
        updatedContent = updatedContent.replaceAll("v1169", "v" + replV1169 );
        updatedContent = updatedContent.replaceAll("v1170", "v" + replV1170 );

        //edit draft vesrsion
        Response editDDIResponse = UtilIT.editDDI(updatedContent, origFileId, apiToken);

        editDDIResponse.prettyPrint();
        assertEquals(200, editDDIResponse.getStatusCode());



        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());

        Response getDatasetJsonBeforeUpdate = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonBeforeUpdate.prettyPrint();
        getDatasetJsonBeforeUpdate.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.latestVersion.files[0].label", equalTo("dct.tab"));
        
        String pathToJsonFile = "doc/sphinx-guides/source/_static/api/dataset-update-metadata.json";
        Response updateTitle = UtilIT.updateDatasetMetadataViaNative(datasetPid, pathToJsonFile, apiToken);
        updateTitle.prettyPrint();
        updateTitle.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        // shouldn't be able to update current unless you're a super user

        UtilIT.publishDatasetViaNativeApi(datasetId, "updatecurrent", apiToken).then().assertThat().statusCode(FORBIDDEN.getStatusCode());
        
        Response makeSuperUser = UtilIT.setSuperuserStatus(username, true);
                
        //should work after making super user
        
        UtilIT.publishDatasetViaNativeApi(datasetId, "updatecurrent", apiToken).then().assertThat().statusCode(OK.getStatusCode());
        
        //Check that the dataset contains the updated metadata (which includes the name Spruce)
        Response getDatasetJsonAfterUpdate = UtilIT.nativeGet(datasetId, apiToken);
        assertTrue(getDatasetJsonAfterUpdate.prettyPrint().contains("Spruce"));
        getDatasetJsonAfterUpdate.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        //Check that the draft version is gone
        Response getDraft1 = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_DRAFT, apiToken);
        getDraft1.then().assertThat()
        .statusCode(NOT_FOUND.getStatusCode());

        
        //Also test a terms change
        String jsonLDTerms = "{\"https://dataverse.org/schema/core#fileTermsOfAccess\":{\"https://dataverse.org/schema/core#dataAccessPlace\":\"Somewhere\"}}";
        Response updateTerms = UtilIT.updateDatasetJsonLDMetadata(datasetId, apiToken, jsonLDTerms, true);
        updateTerms.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        //Run Update-Current Version again
        
        UtilIT.publishDatasetViaNativeApi(datasetId, "updatecurrent", apiToken).then().assertThat().statusCode(OK.getStatusCode());

        
        //Verify the new term is there
        Response jsonLDResponse = UtilIT.getDatasetJsonLDMetadata(datasetId, apiToken);
        assertTrue(jsonLDResponse.prettyPrint().contains("Somewhere"));
        jsonLDResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        //And that the draft is gone
        Response getDraft2 = UtilIT.getDatasetVersion(datasetPid, DS_VERSION_DRAFT, apiToken);
        getDraft2.then().assertThat()
        .statusCode(NOT_FOUND.getStatusCode());
       
        
    }
    
    /**
     * In this test we are restricting a file and testing that terms of accees
     * or request access is required
     *
     * Export at the dataset level is always the public version.
     *
     */
    @Test
    public void testRestrictFileTermsOfUseAndAccess() throws IOException {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String authorUsername = UtilIT.getUsernameFromResponse(createUser);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");

        Path pathToFile = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "data.csv");
        String contentOfCsv = ""
                + "name,pounds,species\n"
                + "Marshall,40,dog\n"
                + "Tiger,17,cat\n"
                + "Panther,21,cat\n";
        java.nio.file.Files.write(pathToFile, contentOfCsv.getBytes());

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile.toString(), authorApiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("data.csv"));

        String fileId = JsonPath.from(uploadFile.body().asString()).getString("data.files[0].dataFile.id");

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", authorApiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile);

        Response restrictFile = UtilIT.restrictFile(fileId, true, authorApiToken);
        restrictFile.prettyPrint();
        restrictFile.then().assertThat().statusCode(OK.getStatusCode());

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, authorApiToken);
        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", authorApiToken);
        publishDataset.then().assertThat().statusCode(OK.getStatusCode());
        
        
        //not allowed to remove request access if there are retricted files
        
        Response disallowRequestAccess = UtilIT.allowAccessRequests(datasetPid, false, authorApiToken);
        disallowRequestAccess.prettyPrint();
        disallowRequestAccess.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
        
    }
    
    /**
     * In this test we are removing request access and showing that
     * files cannot be restricted
     * Not testing legacy datasets that are out of compliance 
     * cannot be published or upload files 
     * or have metadata updated -
     */
    @Test
    public void testRestrictFilesWORequestAccess() throws IOException {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String authorUsername = UtilIT.getUsernameFromResponse(createUser);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");
        
        //should be allowed to remove Request access before restricting file
        Response disallowRequestAccess = UtilIT.allowAccessRequests(datasetPid, false, authorApiToken);
        disallowRequestAccess.prettyPrint();
        disallowRequestAccess.then().assertThat().statusCode(OK.getStatusCode());

        Path pathToFile = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "data.csv");
        String contentOfCsv = ""
                + "name,pounds,species\n"
                + "Marshall,40,dog\n"
                + "Tiger,17,cat\n"
                + "Panther,21,cat\n";
        java.nio.file.Files.write(pathToFile, contentOfCsv.getBytes());

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile.toString(), authorApiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("data.csv"));

        String fileId = JsonPath.from(uploadFile.body().asString()).getString("data.files[0].dataFile.id");

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", authorApiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile);

        Response restrictFile = UtilIT.restrictFile(fileId, true, authorApiToken);
        restrictFile.prettyPrint();
        //shouldn't be able to restrict a file
        restrictFile.then().assertThat().statusCode(CONFLICT.getStatusCode());
        
        // OK, let's delete this dataset as well, and then delete the dataverse...
        
        Response deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, authorApiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());
        
        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, authorApiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(authorUsername);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

    }

    /**
     * In this test we do CRUD of archivalStatus (Note this and other archiving
     * related tests are part of
     * https://github.com/harvard-lts/hdc-integration-tests)
     *
     * This test requires the root dataverse to be published to pass.
     */
    @Test
    public void testArchivalStatusAPI() throws IOException {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createNoAccessUser = UtilIT.createRandomUser();
        createNoAccessUser.prettyPrint();
        String apiTokenNoAccess = UtilIT.getApiTokenFromResponse(createNoAccessUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonBeforePublishing.prettyPrint();
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString())
                .getString("data.authority");
        String identifier = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString())
                .getString("data.identifier");
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());

        logger.info("Attempting to publish a major version");
        // Return random sleep 9/13/2019
        // Without it we've seen some DB deadlocks
        // 3 second sleep, to allow the indexing to finish:

        try {
            Thread.sleep(3000l);
        } catch (InterruptedException iex) {
        }

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());

        String pathToJsonFileSingle = "doc/sphinx-guides/source/_static/api/dataset-simple-update-metadata.json";
        Response addSubjectSingleViaNative = UtilIT.updateFieldLevelDatasetMetadataViaNative(datasetPersistentId, pathToJsonFileSingle, apiToken);
        addSubjectSingleViaNative.prettyPrint();
        addSubjectSingleViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());

        
        // Now change the title
//        Response response = UtilIT.updateDatasetJsonLDMetadata(datasetId, apiToken,
//                "{\"title\": \"New Title\", \"@context\":{\"title\": \"http://purl.org/dc/terms/title\"}}", true);
//        response.then().assertThat().statusCode(OK.getStatusCode());

        int status = Status.CONFLICT.getStatusCode();
        while (status == Status.CONFLICT.getStatusCode()) {

            Response publishV2 = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
            status = publishV2.thenReturn().statusCode();
        }
        assertEquals(OK.getStatusCode(), status);

        if (!UtilIT.sleepForReindex(datasetPersistentId, apiToken, 3)) {
            logger.info("Still indexing after 3 seconds");
        }

        //Verify the status is empty
        Response nullStatus = UtilIT.getDatasetVersionArchivalStatus(datasetId, "1.0", apiToken);
        nullStatus.prettyPrint();
        nullStatus.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        //Set it
        Response setStatus = UtilIT.setDatasetVersionArchivalStatus(datasetId, "1.0", apiToken, "pending",
                "almost there");
        setStatus.then().assertThat().statusCode(OK.getStatusCode());

        //Get it
        Response getStatus = UtilIT.getDatasetVersionArchivalStatus(datasetId, "1.0", apiToken);
        getStatus.then().assertThat().body("data.status", equalTo("pending")).body("data.message",
                equalTo("almost there"));

        //Delete it
        Response deleteStatus = UtilIT.deleteDatasetVersionArchivalStatus(datasetId, "1.0", apiToken);
        deleteStatus.then().assertThat().statusCode(OK.getStatusCode());

        //Make sure it's gone
        Response nullStatus2 = UtilIT.getDatasetVersionArchivalStatus(datasetId, "1.0", apiToken);
        nullStatus2.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

    }

    @Test
    public void testGetDatasetSummaryFieldNames() {
        Response summaryFieldNamesResponse = UtilIT.getDatasetSummaryFieldNames();
        summaryFieldNamesResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                // check for any order
                .body("data", hasItems("dsDescription", "subject", "keyword", "publication", "notesText"))
                // check for exact order
                .body("data", contains("dsDescription", "subject", "keyword", "publication", "notesText"));
    }

    @Test
    public void getPrivateUrlDatasetVersion() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Non-anonymized test
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        UtilIT.privateUrlCreate(datasetId, apiToken, false).then().assertThat().statusCode(OK.getStatusCode());
        Response privateUrlGet = UtilIT.privateUrlGet(datasetId, apiToken);
        privateUrlGet.then().assertThat().statusCode(OK.getStatusCode());
        String tokenForPrivateUrlUser = JsonPath.from(privateUrlGet.body().asString()).getString("data.token");

        // We verify that the response contains the dataset associated to the private URL token
        Response getPrivateUrlDatasetVersionResponse = UtilIT.getPrivateUrlDatasetVersion(tokenForPrivateUrlUser);
        getPrivateUrlDatasetVersionResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.datasetId", equalTo(datasetId));

        // Test anonymized
        Response setAnonymizedFieldsSettingResponse = UtilIT.setSetting(SettingsServiceBean.Key.AnonymizedFieldTypeNames, "author");
        setAnonymizedFieldsSettingResponse.then().assertThat().statusCode(OK.getStatusCode());

        createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        UtilIT.privateUrlCreate(datasetId, apiToken, true).then().assertThat().statusCode(OK.getStatusCode());
        privateUrlGet = UtilIT.privateUrlGet(datasetId, apiToken);
        privateUrlGet.then().assertThat().statusCode(OK.getStatusCode());
        tokenForPrivateUrlUser = JsonPath.from(privateUrlGet.body().asString()).getString("data.token");

        Response getPrivateUrlDatasetVersionAnonymizedResponse = UtilIT.getPrivateUrlDatasetVersion(tokenForPrivateUrlUser);
        getPrivateUrlDatasetVersionAnonymizedResponse.prettyPrint();

        // We verify that the response is anonymized for the author field
        getPrivateUrlDatasetVersionAnonymizedResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.datasetId", equalTo(datasetId))
                .body("data.metadataBlocks.citation.fields[1].value", equalTo(BundleUtil.getStringFromBundle("dataset.anonymized.withheld")))
                .body("data.metadataBlocks.citation.fields[1].typeClass", equalTo("primitive"))
                .body("data.metadataBlocks.citation.fields[1].multiple", equalTo(false));

        // Similar to the check above but doesn't rely on fields[1]
        List<JsonObject> authors = with(getPrivateUrlDatasetVersionAnonymizedResponse.body().asString()).param("fieldToFind", "author")
                .getJsonObject("data.metadataBlocks.citation.fields.findAll { fields -> fields.typeName == fieldToFind }");
        Map firstAuthor = authors.get(0);
        String value = (String) firstAuthor.get("value");
        assertEquals(BundleUtil.getStringFromBundle("dataset.anonymized.withheld"), value);

        UtilIT.deleteSetting(SettingsServiceBean.Key.AnonymizedFieldTypeNames);

        // Test invalid token
        getPrivateUrlDatasetVersionAnonymizedResponse = UtilIT.getPrivateUrlDatasetVersion("invalidToken");
        getPrivateUrlDatasetVersionAnonymizedResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void getPrivateUrlDatasetVersionCitation() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        UtilIT.privateUrlCreate(datasetId, apiToken, false).then().assertThat().statusCode(OK.getStatusCode());
        Response privateUrlGet = UtilIT.privateUrlGet(datasetId, apiToken);
        String tokenForPrivateUrlUser = JsonPath.from(privateUrlGet.body().asString()).getString("data.token");

        Response getPrivateUrlDatasetVersionCitation = UtilIT.getPrivateUrlDatasetVersionCitation(tokenForPrivateUrlUser);
        getPrivateUrlDatasetVersionCitation.prettyPrint();

        getPrivateUrlDatasetVersionCitation.then().assertThat()
                .statusCode(OK.getStatusCode())
                // We check that the returned message contains information expected for the citation string
                .body("data.message", containsString("DRAFT VERSION"));
    }

    @Test
    public void getDatasetVersionCitation() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        Response getDatasetVersionCitationResponse = UtilIT.getDatasetVersionCitation(datasetId, DS_VERSION_DRAFT, false, apiToken);
        getDatasetVersionCitationResponse.prettyPrint();

        getDatasetVersionCitationResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                // We check that the returned message contains information expected for the citation string
                .body("data.message", containsString("DRAFT VERSION"));

        // Test Deaccessioned
        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, "Test deaccession reason.", null, apiToken);
        deaccessionDatasetResponse.prettyPrint();
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode())
                .assertThat().body("data.message", containsString(String.valueOf(datasetId)));

        // includeDeaccessioned false
        Response getDatasetVersionCitationNotDeaccessioned = UtilIT.getDatasetVersionCitation(datasetId, DS_VERSION_LATEST_PUBLISHED, false, apiToken);
        getDatasetVersionCitationNotDeaccessioned.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // includeDeaccessioned true
        Response getDatasetVersionCitationDeaccessioned =  UtilIT.getDatasetVersionCitation(datasetId, DS_VERSION_LATEST_PUBLISHED, true, apiToken);
        getDatasetVersionCitationDeaccessioned.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", containsString("DEACCESSIONED VERSION"));

        publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResponse.prettyPrint();
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        String persistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");

        deaccessionDatasetResponse = UtilIT.deaccessionDataset(persistentId, DS_VERSION_LATEST_PUBLISHED, "Test deaccession reason.", null, apiToken);
        deaccessionDatasetResponse.prettyPrint();
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode())
                .assertThat().body("data.message", containsString(String.valueOf(persistentId)));
    }

    @Test
    public void testCitationDate() throws IOException {

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);
        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.getBody().asString()).getString("data.persistentId");
        String pidIdentifierOnly = datasetPid.substring(datasetPid.length() - 6);

        Path pathToAddDateOfDepositJson = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "dateOfDeposit.json");
        String dateOfDeposit = """
{
  "fields": [
    {
      "typeName": "dateOfDeposit",
      "value": "1999-12-31"
    }
  ]
}
""";
        java.nio.file.Files.write(pathToAddDateOfDepositJson, dateOfDeposit.getBytes());

        Response addDateOfDeposit = UtilIT.addDatasetMetadataViaNative(datasetPid, pathToAddDateOfDepositJson.toString(), apiToken);
        addDateOfDeposit.prettyPrint();
        addDateOfDeposit.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.metadataBlocks.citation.fields[5].value", equalTo("1999-12-31"));

        Response setCitationDate = UtilIT.setDatasetCitationDateField(datasetPid, "dateOfDeposit", apiToken);
        setCitationDate.prettyPrint();
        setCitationDate.then().assertThat().statusCode(OK.getStatusCode());

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String currentYear = Year.now().getValue() + "";

        Response getExportFormats = UtilIT.getExportFormats();
        getExportFormats.prettyPrint();
        getExportFormats.then().assertThat().statusCode(OK.getStatusCode());

        // It would be nice to be able to use .getJsonObject(). See https://github.com/rest-assured/rest-assured/pull/1257
        Map<String, Object> exporters = JsonPath.from(getExportFormats.body().asString()).getMap("data");

        if (exporters.containsKey("croissant")) {
            Response exportDraftCroissant = UtilIT.exportDataset(datasetPid, "croissant", false, DS_VERSION_DRAFT, apiToken);
            exportDraftCroissant.prettyPrint();
            exportDraftCroissant.then().assertThat()
                    .statusCode(OK.getStatusCode())
                    .body("version", equalTo("DRAFT"));
        }

        Response exportDraftDatacite = UtilIT.exportDataset(datasetPid, "Datacite", false, DS_VERSION_DRAFT, apiToken);
        exportDraftDatacite.prettyPrint();
        exportDraftDatacite.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("resource.dates.date", CoreMatchers.equalTo("1999-12-31"))
                .body("resource.version", equalTo("DRAFT"));

        Response exportDraftNativeJson = UtilIT.exportDataset(datasetPid, "dataverse_json", false, DS_VERSION_DRAFT, apiToken);
        exportDraftNativeJson.prettyPrint();
        exportDraftNativeJson.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("datasetVersion.versionState", equalTo("DRAFT"))
                .body("datasetVersion.latestVersionPublishingState", equalTo("DRAFT"))
                .body("datasetVersion.citation", equalTo("Finch, Fiona, 1999, \"Darwin's Finches\", https://doi.org/10.5072/FK2/" + pidIdentifierOnly + ", Root, DRAFT VERSION"));

        Response exportDraftDcterms = UtilIT.exportDataset(datasetPid, "dcterms", false, DS_VERSION_DRAFT, apiToken);
        exportDraftDcterms.prettyPrint();
        exportDraftDcterms.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("metadata.dateSubmitted", CoreMatchers.equalTo("1999-12-31"));

        Response exportDraftDdi = UtilIT.exportDataset(datasetPid, "ddi", false, DS_VERSION_DRAFT, apiToken);
        exportDraftDdi.prettyPrint();
        exportDraftDdi.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.docDscr.citation.titlStmt.titl", CoreMatchers.equalTo("Darwin's Finches"))
                // TODO figure out how to say that distDate is absent
                //                .body("codeBook.docDscr.citation.distStmt.distDate", CoreMatchers.equalTo(null))
                // TODO figure out how to say that version is like this: <version type="DRAFT"/> (e.g. no content, no "1" between tags)
                //                .body("codeBook.docDscr.citation.verStmt.version", Matchers.empty())
                .body("codeBook.docDscr.citation.verStmt.version.@date", CoreMatchers.equalTo(null))
                .body("codeBook.docDscr.citation.verStmt.version.@type", CoreMatchers.equalTo("DRAFT"));

        Response exportDraftHtml = UtilIT.exportDataset(datasetPid, "html", false, DS_VERSION_DRAFT, apiToken);
        exportDraftHtml.prettyPrint();
        exportDraftHtml.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("html.head.title", equalTo("Darwin's Finches"));

        // aka OpenAire
        Response exportDraftOaiDatacite = UtilIT.exportDataset(datasetPid, "oai_datacite", false, DS_VERSION_DRAFT, apiToken);
        exportDraftOaiDatacite.prettyPrint();
        exportDraftOaiDatacite.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("resource.dates.date", CoreMatchers.equalTo("1999-12-31"))
                .body("resource.publicationYear", CoreMatchers.equalTo("1999"));

        Response exportDraftOaiDc = UtilIT.exportDataset(datasetPid, "oai_dc", false, DS_VERSION_DRAFT, apiToken);
        exportDraftOaiDc.prettyPrint();
        exportDraftOaiDc.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("oai_dc.type", equalTo("Dataset"))
                .body("oai_dc.date", equalTo("1999-12-31"));

        Response exportDraftOaiDdi = UtilIT.exportDataset(datasetPid, "oai_ddi", false, DS_VERSION_DRAFT, apiToken);
        exportDraftOaiDdi.prettyPrint();
        exportDraftOaiDdi.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.docDscr.citation.titlStmt.titl", CoreMatchers.equalTo("Darwin's Finches"))
                // TODO figure out how to say that distDate is absent
                //                .body("codeBook.docDscr.citation.distStmt.distDate", CoreMatchers.equalTo(null))
                // TODO figure out how to say that version is like this: <version type="DRAFT"/> (e.g. no content, no "1" between tags)
                //                .body("codeBook.docDscr.citation.verStmt.version", Matchers.empty())
                .body("codeBook.docDscr.citation.verStmt.version.@date", CoreMatchers.equalTo(null))
                .body("codeBook.docDscr.citation.verStmt.version.@type", CoreMatchers.equalTo("DRAFT"));

        Response exportDraftOaiOre = UtilIT.exportDataset(datasetPid, "OAI_ORE", false, DS_VERSION_DRAFT, apiToken);
        exportDraftOaiOre.prettyPrint();
        exportDraftOaiOre.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("'dcterms:modified'", equalTo(today))
                .body("'ore:describes'.dateOfDeposit", equalTo("1999-12-31"))
                .body("'ore:describes'.'schema:dateModified'", endsWith(currentYear))
                .body("'ore:describes'.'schema:creativeWorkStatus'", equalTo("DRAFT"));

        Response exportDraftSchemaDotOrg = UtilIT.exportDataset(datasetPid, "schema.org", false, DS_VERSION_DRAFT, apiToken);
        exportDraftSchemaDotOrg.prettyPrint();
        exportDraftSchemaDotOrg.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("version", equalTo("DRAFT"))
                .body("datePublished", equalTo(null))
                .body("dateModified", equalTo(""));

        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());

        Response getCitationAfter = UtilIT.getDatasetVersionCitation(datasetId, DS_VERSION_LATEST_PUBLISHED, true, apiToken);
        getCitationAfter.prettyPrint();

        String doi = datasetPid.substring(4);

        // Note that the year 1999 appears in the citation because we
        // set the citation date field to a field that has that year.
        String expectedCitation = "Finch, Fiona, 1999, \"Darwin's Finches\", <a href=\"https://doi.org/" + doi + "\" target=\"_blank\">https://doi.org/" + doi + "</a>, Root, V1";

        getCitationAfter.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", is(expectedCitation));

        Response exportDatacite = UtilIT.exportDataset(datasetPid, "Datacite");
        exportDatacite.prettyPrint();
        exportDatacite.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("resource.dates.date[0].@dateType", CoreMatchers.equalTo("Submitted"))
                .body("resource.dates.date[0]", CoreMatchers.equalTo("1999-12-31"))
                .body("resource.dates.date[1].@dateType", CoreMatchers.equalTo("Available"))
                .body("resource.dates.date[1]", CoreMatchers.equalTo(today))
                .body("resource.version", equalTo("1.0"));

        Response exportNativeJson = UtilIT.exportDataset(datasetPid, "dataverse_json");
        exportNativeJson.prettyPrint();
        exportNativeJson.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("datasetVersion.versionNumber", equalTo(1))
                .body("datasetVersion.versionMinorNumber", equalTo(0))
                .body("datasetVersion.versionState", equalTo("RELEASED"))
                .body("datasetVersion.latestVersionPublishingState", equalTo("RELEASED"))
                .body("datasetVersion.citation", equalTo("Finch, Fiona, 1999, \"Darwin's Finches\", https://doi.org/10.5072/FK2/" + pidIdentifierOnly + ", Root, V1"));

        Response exportDcterms = UtilIT.exportDataset(datasetPid, "dcterms");
        exportDcterms.prettyPrint();
        exportDcterms.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("metadata.date", CoreMatchers.equalTo(today))
                .body("metadata.dateSubmitted", CoreMatchers.equalTo("1999-12-31"));

        Response exportDdi = UtilIT.exportDataset(datasetPid, "ddi");
        exportDdi.prettyPrint();
        exportDdi.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.docDscr.citation.titlStmt.titl", CoreMatchers.equalTo("Darwin's Finches"))
                .body("codeBook.docDscr.citation.distStmt.distDate", CoreMatchers.equalTo(today))
                .body("codeBook.docDscr.citation.verStmt.version", CoreMatchers.equalTo("1"))
                .body("codeBook.docDscr.citation.verStmt.version.@date", CoreMatchers.equalTo(today))
                .body("codeBook.docDscr.citation.verStmt.version.@type", CoreMatchers.equalTo("RELEASED"));

        Response exportHtml = UtilIT.exportDataset(datasetPid, "html");
        exportHtml.prettyPrint();
        exportHtml.then().assertThat()
                .statusCode(OK.getStatusCode())
                // HTML is too hard to parse. Just confirm we're getting some content we expect.
                .body("html.head.title", equalTo("Darwin's Finches"));

        // aka OpenAire
        Response exportOaiDatacite = UtilIT.exportDataset(datasetPid, "oai_datacite");
        exportOaiDatacite.prettyPrint();
        exportOaiDatacite.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("resource.dates.date[0].@dateType", CoreMatchers.equalTo("Submitted"))
                .body("resource.dates.date[0]", CoreMatchers.equalTo("1999-12-31"))
                .body("resource.dates.date[1].@dateType", CoreMatchers.equalTo("Updated"))
                .body("resource.dates.date[1]", CoreMatchers.equalTo(today))
                .body("resource.publicationYear", CoreMatchers.equalTo("2025"));

        Response exportDatasetOaiDc = UtilIT.exportDataset(datasetPid, "oai_dc", apiToken, true);
        exportDatasetOaiDc.prettyPrint();
        exportDatasetOaiDc.then().assertThat()
                .body("oai_dc.type", equalTo("Dataset"))
                .body("oai_dc.date", equalTo("1999-12-31"))
                .statusCode(OK.getStatusCode());

        Response exportOaiDDi = UtilIT.exportDataset(datasetPid, "oai_ddi");
        exportOaiDDi.prettyPrint();
        exportOaiDDi.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.docDscr.citation.titlStmt.titl", CoreMatchers.equalTo("Darwin's Finches"))
                .body("codeBook.docDscr.citation.distStmt.distDate", CoreMatchers.equalTo(today))
                .body("codeBook.docDscr.citation.verStmt.version", CoreMatchers.equalTo("1"))
                .body("codeBook.docDscr.citation.verStmt.version.@date", CoreMatchers.equalTo(today))
                .body("codeBook.docDscr.citation.verStmt.version.@type", CoreMatchers.equalTo("RELEASED"));

        Response exportOaiOre = UtilIT.exportDataset(datasetPid, "OAI_ORE");
        exportOaiOre.prettyPrint();
        exportOaiOre.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("'dcterms:modified'", equalTo(today))
                .body("'ore:describes'.dateOfDeposit", equalTo("1999-12-31"))
                .body("'ore:describes'.'schema:dateModified'", startsWith(currentYear))
                .body("'ore:describes'.'schema:datePublished'", equalTo(today))
                .body("'ore:describes'.'schema:creativeWorkStatus'", equalTo("RELEASED"));

        Response exportSchemaDotOrg = UtilIT.exportDataset(datasetPid, "schema.org");
        exportSchemaDotOrg.prettyPrint();
        exportSchemaDotOrg.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("version", equalTo("1"))
                .body("datePublished", equalTo(today))
                .body("dateModified", equalTo(today));

        Response clearDateField = UtilIT.clearDatasetCitationDateField(datasetPid, apiToken);
        clearDateField.prettyPrint();
        clearDateField.then().assertThat().statusCode(OK.getStatusCode());

        // Clearing not enough. You have to reexport because the previous date is cached.
        Response rexport = UtilIT.reexportDatasetAllFormats(datasetPid);
        rexport.prettyPrint();
        rexport.then().assertThat().statusCode(OK.getStatusCode());

        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Response exportPostClear = UtilIT.exportDataset(datasetPid, "oai_dc", apiToken, true);
        exportPostClear.prettyPrint();
        exportPostClear.then().assertThat()
                .body("oai_dc.type", equalTo("Dataset"))
                .body("oai_dc.date", equalTo(todayDate))
                .statusCode(OK.getStatusCode());

        String newTitle = "A Post-1.0 Version";
        Response updateTitle = UtilIT.updateDatasetTitleViaSword(datasetPid, newTitle, apiToken);
        updateTitle.prettyPrint();
        updateTitle.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("entry.treatment", equalTo("no treatment information available"));

        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());

        Response exportDatacitePreviouslyPublishedVersion = UtilIT.exportDataset(datasetPid, "Datacite", false, "1.0", apiToken);
        exportDatacitePreviouslyPublishedVersion.prettyPrint();
        exportDatacitePreviouslyPublishedVersion.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Non-draft version requested (1.0) but for published versions only the latest (:latest-published) is supported."));

        Response exportDataciteJunkVersion = UtilIT.exportDataset(datasetPid, "Datacite", false, "junk", apiToken);
        exportDataciteJunkVersion.prettyPrint();
        exportDataciteJunkVersion.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Unable to look up dataset based on version. Try :latest-published or :draft."));

        Response deaccessionV20 = UtilIT.deaccessionDataset(datasetPid, "2.0", "retracted", null, apiToken);
        deaccessionV20.prettyPrint();
        deaccessionV20.then().assertThat().statusCode(OK.getStatusCode());

        Response exportDataciteV20Deaccessioned = UtilIT.exportDataset(datasetPid, "Datacite", false, "2.0", apiToken);
        exportDataciteV20Deaccessioned.prettyPrint();
        exportDataciteV20Deaccessioned.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Unable to look up dataset based on version. Try :latest-published or :draft."));

        Response exportDataciteV10NotDeaccessioned = UtilIT.exportDataset(datasetPid, "Datacite", false, "1.0", apiToken);
        exportDataciteV10NotDeaccessioned.prettyPrint();
        exportDataciteV10NotDeaccessioned.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("resource.dates.date[0].@dateType", CoreMatchers.equalTo("Submitted"))
                .body("resource.dates.date[0]", CoreMatchers.equalTo("1999-12-31"))
                .body("resource.dates.date[1].@dateType", CoreMatchers.equalTo("Available"))
                .body("resource.dates.date[1]", CoreMatchers.equalTo(today))
                .body("resource.version", equalTo("1.0"));

        Response exportDataciteLatestPublishedNotDeaccessioned = UtilIT.exportDataset(datasetPid, "Datacite", false, DS_VERSION_LATEST_PUBLISHED, apiToken);
        exportDataciteLatestPublishedNotDeaccessioned.prettyPrint();
        exportDataciteLatestPublishedNotDeaccessioned.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("resource.dates.date[0].@dateType", CoreMatchers.equalTo("Submitted"))
                .body("resource.dates.date[0]", CoreMatchers.equalTo("1999-12-31"))
                .body("resource.dates.date[1].@dateType", CoreMatchers.equalTo("Available"))
                .body("resource.dates.date[1]", CoreMatchers.equalTo(today))
                .body("resource.version", equalTo("1.0"));

        Response deaccessionV10 = UtilIT.deaccessionDataset(datasetPid, "1.0", "retracted", null, apiToken);
        deaccessionV10.prettyPrint();
        deaccessionV10.then().assertThat().statusCode(OK.getStatusCode());

        Response exportDataciteV10IsDeaccessioned = UtilIT.exportDataset(datasetPid, "Datacite", false, "1.0", apiToken);
        exportDataciteV10IsDeaccessioned.prettyPrint();
        exportDataciteV10IsDeaccessioned.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Unable to look up dataset based on version. Try :latest-published or :draft."));

        Response exportDataciteLatestPublishedIsDeaccessioned = UtilIT.exportDataset(datasetPid, "Datacite", false, DS_VERSION_LATEST_PUBLISHED, apiToken);
        exportDataciteLatestPublishedIsDeaccessioned.prettyPrint();
        exportDataciteLatestPublishedIsDeaccessioned.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Unable to look up dataset based on version. Try :latest-published or :draft."));
    }

    @Test
    public void testDataCiteExport() throws IOException {

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);

        JsonObjectBuilder datasetJson = Json.createObjectBuilder()
                .add("datasetVersion", Json.createObjectBuilder()
                        .add("license", Json.createObjectBuilder()
                                .add("name", "CC0 1.0")
                                .add("uri", "http://creativecommons.org/publicdomain/zero/1.0")
                        )
                        .add("metadataBlocks", Json.createObjectBuilder()
                                .add("citation", Json.createObjectBuilder()
                                        .add("fields", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "title")
                                                        .add("value", "Test dataset")
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
                                                                                        .add("typeName", "authorName")
                                                                        )
                                                                        .add("authorAffiliation",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "https://ror.org/03vek6s52")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "authorAffiliation")
                                                                        )
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
                                                                                        .add("value", "Just a test dataset.")
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
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("authorName",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "https://ror.org/01cwqze88") // NIH
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "grantNumberAgency")
                                                                        )
                                                                        .add("authorAffiliation",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "12345")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "grantNumberValue")
                                                                        )
                                                                )
                                                        )
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("typeName", "grantNumber")
                                                )
                                        )
                                )
                        ));

        Response createDatasetResponse = UtilIT.createDataset(dataverseAlias, datasetJson, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        String datasetPid = JsonPath.from(createDatasetResponse.getBody().asString()).getString("data.persistentId");

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat().statusCode(OK.getStatusCode());

        Response exportDatasetAsDataCite = UtilIT.exportDataset(datasetPid, "Datacite", apiToken, true);
        exportDatasetAsDataCite.prettyPrint();
        exportDatasetAsDataCite.then().assertThat()
                .body("resource.creators.creator[0].creatorName", equalTo("Simpson, Homer"))
                // see below for additional affiliation assertions, which can vary
                .body("resource.creators.creator[0].affiliation.@schemeURI", equalTo("https://ror.org"))
                .body("resource.creators.creator[0].affiliation.@affiliationIdentifierScheme", equalTo("ROR"))
                // see below for additional fundingReference assertions, which can vary
                .body("resource.fundingReferences.fundingReference[0].awardNumber", equalTo("12345"))
                .statusCode(OK.getStatusCode());

        // Out of the box :CVocConf is not set. If you set it with
        // https://github.com/gdcc/dataverse-external-vocab-support/blob/de011d239254ff7d651212c565f8604808dcd7e9/examples/config/grantNumberAgencyRor.json
        // you can expect different results.
        boolean authorsOrcidAndRorEnabled = false;
        if (authorsOrcidAndRorEnabled) {
            exportDatasetAsDataCite.then().assertThat()
                    .body("resource.creators.creator[0].affiliation", equalTo("Harvard University"))
                    // Once https://github.com/IQSS/dataverse/pull/11175 is merged the equalTo bellow
                    // should be "https://ror.org/03vek6s52" instead of "Harvard University".
                    .body("resource.creators.creator[0].affiliation.@affiliationIdentifier", equalTo("Harvard University"));
        } else {
            exportDatasetAsDataCite.then().assertThat()
                    .body("resource.creators.creator[0].affiliation", equalTo("https://ror.org/03vek6s52"))
                    .body("resource.creators.creator[0].affiliation.@affiliationIdentifier", equalTo("https://ror.org/03vek6s52"));
        }

        // Out of the box :CVocConf is not set. If you set it with
        // https://github.com/gdcc/dataverse-external-vocab-support/blob/de011d239254ff7d651212c565f8604808dcd7e9/examples/config/grantNumberAgencyRor.json
        // you can expect different results.
        boolean grantNumberAgencyRorEnabled = false;
        if (grantNumberAgencyRorEnabled) {
            exportDatasetAsDataCite.then().assertThat()
                    .body("resource.fundingReferences.fundingReference[0].funderName", equalTo("National Institutes of Health"))
                    .body("resource.fundingReferences.fundingReference[0].funderIdentifier.@funderIdentifierType", equalTo("ROR"))
                    .body("resource.fundingReferences.fundingReference[0].funderIdentifier.@schemeURI", equalTo("https://ror.org"))
                    .body("resource.fundingReferences.fundingReference[0].funderIdentifier", equalTo("https://ror.org/01cwqze88"));
        } else {
            exportDatasetAsDataCite.then().assertThat()
                    .body("resource.fundingReferences.fundingReference[0].funderName", equalTo("https://ror.org/01cwqze88"))
                    .body("resource.fundingReferences.fundingReference[0].awardNumber", equalTo("12345"));
        }
    }

    @Test
    public void getVersionFiles() throws IOException, InterruptedException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String datasetPersistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        String testFileName1 = "test_1.txt";
        String testFileName2 = "test_2.txt";
        String testFileName3 = "test_3.txt";
        String testFileName4 = "test_4.txt";
        String testFileName5 = "test_5.png";

        UtilIT.createAndUploadTestFile(datasetPersistentId, testFileName1, new byte[50], apiToken);
        UtilIT.createAndUploadTestFile(datasetPersistentId, testFileName2, new byte[200], apiToken);
        UtilIT.createAndUploadTestFile(datasetPersistentId, testFileName3, new byte[100], apiToken);
        UtilIT.createAndUploadTestFile(datasetPersistentId, testFileName5, new byte[300], apiToken);
        UtilIT.createAndUploadTestFile(datasetPersistentId, testFileName4, new byte[400], apiToken);

        String testDatasetVersion = ":latest";

        // Test pagination and NameAZ order criteria (the default criteria)
        int testPageSize = 2;

        // Test page 1
        Response getVersionFilesResponsePaginated = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, testPageSize, null, null, null, null, null, null, null, false, apiToken);

        getVersionFilesResponsePaginated.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName1))
                .body("data[1].label", equalTo(testFileName2))
                .body("totalCount", equalTo(5));

        int fileMetadatasCount = getVersionFilesResponsePaginated.jsonPath().getList("data").size();
        assertEquals(testPageSize, fileMetadatasCount);

        String testFileId1 = JsonPath.from(getVersionFilesResponsePaginated.body().asString()).getString("data[0].dataFile.id");
        String testFileId2 = JsonPath.from(getVersionFilesResponsePaginated.body().asString()).getString("data[1].dataFile.id");

        // Test page 2
        getVersionFilesResponsePaginated = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, testPageSize, testPageSize, null, null, null, null, null, null, false, apiToken);

        getVersionFilesResponsePaginated.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName3))
                .body("data[1].label", equalTo(testFileName4))
                .body("totalCount", equalTo(5));

        fileMetadatasCount = getVersionFilesResponsePaginated.jsonPath().getList("data").size();
        assertEquals(testPageSize, fileMetadatasCount);

        // Test page 3 (last)
        getVersionFilesResponsePaginated = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, testPageSize, testPageSize * 2, null, null, null, null, null, null, false, apiToken);

        getVersionFilesResponsePaginated.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName5));

        fileMetadatasCount = getVersionFilesResponsePaginated.jsonPath().getList("data").size();
        assertEquals(1, fileMetadatasCount);

        // Test NameZA order criteria
        Response getVersionFilesResponseNameZACriteria = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, null, null, null, null, DatasetVersionFilesServiceBean.FileOrderCriteria.NameZA.toString(), false, apiToken);

        getVersionFilesResponseNameZACriteria.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName5))
                .body("data[1].label", equalTo(testFileName4))
                .body("data[2].label", equalTo(testFileName3))
                .body("data[3].label", equalTo(testFileName2))
                .body("data[4].label", equalTo(testFileName1));

        // Test Newest order criteria
        Response getVersionFilesResponseNewestCriteria = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, null, null, null, null, DatasetVersionFilesServiceBean.FileOrderCriteria.Newest.toString(), false, apiToken);

        getVersionFilesResponseNewestCriteria.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName4))
                .body("data[1].label", equalTo(testFileName5))
                .body("data[2].label", equalTo(testFileName3))
                .body("data[3].label", equalTo(testFileName2))
                .body("data[4].label", equalTo(testFileName1));

        // Test Oldest order criteria
        Response getVersionFilesResponseOldestCriteria = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, null, null, null, null, DatasetVersionFilesServiceBean.FileOrderCriteria.Oldest.toString(), false, apiToken);

        getVersionFilesResponseOldestCriteria.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName1))
                .body("data[1].label", equalTo(testFileName2))
                .body("data[2].label", equalTo(testFileName3))
                .body("data[3].label", equalTo(testFileName5))
                .body("data[4].label", equalTo(testFileName4));

        // Test Size order criteria
        Response getVersionFilesResponseSizeCriteria = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, null, null, null, null, DatasetVersionFilesServiceBean.FileOrderCriteria.Size.toString(), false, apiToken);

        getVersionFilesResponseSizeCriteria.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName1))
                .body("data[1].label", equalTo(testFileName3))
                .body("data[2].label", equalTo(testFileName2))
                .body("data[3].label", equalTo(testFileName5))
                .body("data[4].label", equalTo(testFileName4));

        // Test Type order criteria
        Response getVersionFilesResponseTypeCriteria = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, null, null, null, null, DatasetVersionFilesServiceBean.FileOrderCriteria.Type.toString(), false, apiToken);

        getVersionFilesResponseTypeCriteria.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName5))
                .body("data[1].label", equalTo(testFileName1))
                .body("data[2].label", equalTo(testFileName2))
                .body("data[3].label", equalTo(testFileName3))
                .body("data[4].label", equalTo(testFileName4));

        // Test invalid order criteria
        String invalidOrderCriteria = "invalidOrderCriteria";
        Response getVersionFilesResponseInvalidOrderCriteria = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, null, null, null, null, invalidOrderCriteria, false, apiToken);
        getVersionFilesResponseInvalidOrderCriteria.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Invalid order criteria: " + invalidOrderCriteria));

        // Test Content Type
        Response getVersionFilesResponseContentType = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, "image/png", null, null, null, null, null, false, apiToken);

        getVersionFilesResponseContentType.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName5));

        fileMetadatasCount = getVersionFilesResponseContentType.jsonPath().getList("data").size();
        assertEquals(1, fileMetadatasCount);

        // Test Category Name
        String testCategory = "testCategory";
        Response setFileCategoriesResponse = UtilIT.setFileCategories(testFileId1, apiToken, List.of(testCategory));
        setFileCategoriesResponse.then().assertThat().statusCode(OK.getStatusCode());
        setFileCategoriesResponse = UtilIT.setFileCategories(testFileId2, apiToken, List.of(testCategory));
        setFileCategoriesResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response getVersionFilesResponseCategoryName = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, null, testCategory, null, null, null, false, apiToken);

        getVersionFilesResponseCategoryName.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName1))
                .body("data[1].label", equalTo(testFileName2));

        fileMetadatasCount = getVersionFilesResponseCategoryName.jsonPath().getList("data").size();
        assertEquals(2, fileMetadatasCount);

        // Test Access Status Restricted
        Response restrictFileResponse = UtilIT.restrictFile(String.valueOf(testFileId1), true, apiToken);
        restrictFileResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response getVersionFilesResponseRestricted = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, FileSearchCriteria.FileAccessStatus.Restricted.toString(), null, null, null, null, false, apiToken);

        getVersionFilesResponseRestricted.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName1));

        fileMetadatasCount = getVersionFilesResponseRestricted.jsonPath().getList("data").size();
        assertEquals(1, fileMetadatasCount);

        // Test Access Status Embargoed
        UtilIT.setSetting(SettingsServiceBean.Key.MaxEmbargoDurationInMonths, "12");
        String activeEmbargoDate = LocalDate.now().plusMonths(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Create embargo for test file 1 (Embargoed and Restricted)
        Response createActiveFileEmbargoResponse = UtilIT.createFileEmbargo(datasetId, Integer.parseInt(testFileId1), activeEmbargoDate, apiToken);

        createActiveFileEmbargoResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Create embargo for test file 2 (Embargoed and Public)
        createActiveFileEmbargoResponse = UtilIT.createFileEmbargo(datasetId, Integer.parseInt(testFileId2), activeEmbargoDate, apiToken);

        createActiveFileEmbargoResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response getVersionFilesResponseEmbargoedThenPublic = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, FileSearchCriteria.FileAccessStatus.EmbargoedThenPublic.toString(), null, null, null, null, false, apiToken);

        getVersionFilesResponseEmbargoedThenPublic.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName2));

        fileMetadatasCount = getVersionFilesResponseEmbargoedThenPublic.jsonPath().getList("data").size();
        assertEquals(1, fileMetadatasCount);

        Response getVersionFilesResponseEmbargoedThenRestricted = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, FileSearchCriteria.FileAccessStatus.EmbargoedThenRestricted.toString(), null, null, null, null, false, apiToken);

        getVersionFilesResponseEmbargoedThenRestricted.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName1));

        fileMetadatasCount = getVersionFilesResponseEmbargoedThenRestricted.jsonPath().getList("data").size();
        assertEquals(1, fileMetadatasCount);


        // Test Access Status Retention
        UtilIT.setSetting(SettingsServiceBean.Key.MinRetentionDurationInMonths, "-1");
        String retentionEndDate = LocalDate.now().plusMonths(240).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Create retention for test file 2 (Retention and Public)
        Response createFileRetentionResponse = UtilIT.createFileRetention(datasetId, Integer.parseInt(testFileId2), retentionEndDate, apiToken);
        createFileRetentionResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response getVersionFilesResponseRetentionPeriodExpired = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, FileSearchCriteria.FileAccessStatus.RetentionPeriodExpired.toString(), null, null, null, null, false, apiToken);
        getVersionFilesResponseRetentionPeriodExpired.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0));

        // Test Access Status Public
        Response getVersionFilesResponsePublic = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, FileSearchCriteria.FileAccessStatus.Public.toString(), null, null, null, null, false, apiToken);

        getVersionFilesResponsePublic.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName3))
                .body("data[1].label", equalTo(testFileName4))
                .body("data[2].label", equalTo(testFileName5));

        fileMetadatasCount = getVersionFilesResponsePublic.jsonPath().getList("data").size();
        assertEquals(3, fileMetadatasCount);

        // Test invalid access status
        String invalidStatus = "invalidStatus";
        Response getVersionFilesResponseInvalidStatus = UtilIT.getVersionFiles(datasetId, testDatasetVersion, null, null, null, invalidStatus, null, null, null, null, false, apiToken);
        getVersionFilesResponseInvalidStatus.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(BundleUtil.getStringFromBundle("datasets.api.version.files.invalid.access.status", List.of(invalidStatus))));

        // Test Search Text
        Response getVersionFilesResponseSearchText = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST, null, null, null, null, null, null, "test_1", null, false, apiToken);

        getVersionFilesResponseSearchText.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName1));

        fileMetadatasCount = getVersionFilesResponseSearchText.jsonPath().getList("data").size();
        assertEquals(1, fileMetadatasCount);

        // Test Deaccessioned
        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, "Test deaccession reason.", null, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // includeDeaccessioned false
        Response getVersionFilesResponseNoDeaccessioned = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST_PUBLISHED, null, null, null, null, null, null, null, null, false, apiToken);
        getVersionFilesResponseNoDeaccessioned.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // includeDeaccessioned true
        Response getVersionFilesResponseDeaccessioned = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST_PUBLISHED, null, null, null, null, null, null, null, null, true, apiToken);
        getVersionFilesResponseDeaccessioned.then().assertThat().statusCode(OK.getStatusCode());

        getVersionFilesResponseDeaccessioned.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo(testFileName1))
                .body("data[1].label", equalTo(testFileName2))
                .body("data[2].label", equalTo(testFileName3))
                .body("data[3].label", equalTo(testFileName4))
                .body("data[4].label", equalTo(testFileName5));

        // Test Tabular Tag Name
        String pathToTabularTestFile = "src/test/resources/tab/test.tab";
        Response uploadTabularFileResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToTabularTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadTabularFileResponse.then().assertThat().statusCode(OK.getStatusCode());

        String tabularFileId = uploadTabularFileResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");

        // Ensure tabular file is ingested
        sleep(2000);

        String tabularTagName = "Survey";
        Response setFileTabularTagsResponse = UtilIT.setFileTabularTags(tabularFileId, apiToken, List.of(tabularTagName));
        setFileTabularTagsResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response getVersionFilesResponseTabularTagName = UtilIT.getVersionFiles(datasetId, testDatasetVersion, null, null, null, null, null, tabularTagName, null, null, false, apiToken);

        getVersionFilesResponseTabularTagName.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].label", equalTo("test.tab"));

        fileMetadatasCount = getVersionFilesResponseTabularTagName.jsonPath().getList("data").size();
        assertEquals(1, fileMetadatasCount);

        // Test that the dataset files for a deaccessioned dataset cannot be accessed by a guest
        // By latest published version
        Response getDatasetVersionResponse = UtilIT.getVersionFiles(datasetId, DS_VERSION_LATEST_PUBLISHED, null, null, null, null, null, null, null, null, true, null);
        getDatasetVersionResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());
        // By specific version 1.0
        getDatasetVersionResponse = UtilIT.getVersionFiles(datasetId, "1.0", null, null, null, null, null, null, null, null, true, null);
        getDatasetVersionResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void getVersionFileCounts() throws IOException, InterruptedException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String datasetPersistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        // Creating test files
        String testFileName1 = "test_1.txt";
        String testFileName2 = "test_2.txt";
        String testFileName3 = "test_3.png";

        UtilIT.createAndUploadTestFile(datasetPersistentId, testFileName1, new byte[50], apiToken);
        UtilIT.createAndUploadTestFile(datasetPersistentId, testFileName2, new byte[200], apiToken);
        UtilIT.createAndUploadTestFile(datasetPersistentId, testFileName3, new byte[100], apiToken);

        // Creating a categorized test file
        String pathToTestFile = "src/test/resources/images/coffeeshop.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());
        String dataFileId = uploadResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");
        String testCategory = "testCategory";
        Response setFileCategoriesResponse = UtilIT.setFileCategories(dataFileId, apiToken, List.of(testCategory));
        setFileCategoriesResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Setting embargo for file (Embargo and Public)
        UtilIT.setSetting(SettingsServiceBean.Key.MaxEmbargoDurationInMonths, "12");
        String activeEmbargoDate = LocalDate.now().plusMonths(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Response createFileEmbargoResponse = UtilIT.createFileEmbargo(datasetId, Integer.parseInt(dataFileId), activeEmbargoDate, apiToken);
        createFileEmbargoResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Getting the file counts and assert each count
        Response getVersionFileCountsResponse = UtilIT.getVersionFileCounts(datasetId, DS_VERSION_LATEST, null, null, null, null, null, false, apiToken);

        getVersionFileCountsResponse.then().assertThat().statusCode(OK.getStatusCode());

        JsonPath responseJsonPath = getVersionFileCountsResponse.jsonPath();
        LinkedHashMap<String, Integer> responseCountPerContentTypeMap = responseJsonPath.get("data.perContentType");
        LinkedHashMap<String, Integer> responseCountPerCategoryNameMap = responseJsonPath.get("data.perCategoryName");
        LinkedHashMap<String, Integer> responseCountPerTabularTagNameMap = responseJsonPath.get("data.perTabularTagName");
        LinkedHashMap<String, Integer> responseCountPerAccessStatusMap = responseJsonPath.get("data.perAccessStatus");

        assertEquals(4, (Integer) responseJsonPath.get("data.total"));
        assertEquals(2, responseCountPerContentTypeMap.get("image/png"));
        assertEquals(2, responseCountPerContentTypeMap.get("text/plain"));
        assertEquals(2, responseCountPerContentTypeMap.size());
        assertEquals(1, responseCountPerCategoryNameMap.get(testCategory));
        assertEquals(0, responseCountPerTabularTagNameMap.size());
        assertEquals(2, responseCountPerAccessStatusMap.size());
        assertEquals(3, responseCountPerAccessStatusMap.get(FileSearchCriteria.FileAccessStatus.Public.toString()));
        assertEquals(1, responseCountPerAccessStatusMap.get(FileSearchCriteria.FileAccessStatus.EmbargoedThenPublic.toString()));

        // Test content type criteria
        getVersionFileCountsResponse = UtilIT.getVersionFileCounts(datasetId, DS_VERSION_LATEST, "image/png", null, null, null, null, false, apiToken);
        getVersionFileCountsResponse.then().assertThat().statusCode(OK.getStatusCode());

        responseJsonPath = getVersionFileCountsResponse.jsonPath();
        responseCountPerContentTypeMap = responseJsonPath.get("data.perContentType");
        responseCountPerCategoryNameMap = responseJsonPath.get("data.perCategoryName");
        responseCountPerTabularTagNameMap = responseJsonPath.get("data.perTabularTagName");
        responseCountPerAccessStatusMap = responseJsonPath.get("data.perAccessStatus");

        assertEquals(2, (Integer) responseJsonPath.get("data.total"));
        assertEquals(2, responseCountPerContentTypeMap.get("image/png"));
        assertEquals(1, responseCountPerContentTypeMap.size());
        assertEquals(1, responseCountPerCategoryNameMap.size());
        assertEquals(1, responseCountPerCategoryNameMap.get(testCategory));
        assertEquals(0, responseCountPerTabularTagNameMap.size());
        assertEquals(2, responseCountPerAccessStatusMap.size());
        assertEquals(1, responseCountPerAccessStatusMap.get(FileSearchCriteria.FileAccessStatus.Public.toString()));
        assertEquals(1, responseCountPerAccessStatusMap.get(FileSearchCriteria.FileAccessStatus.EmbargoedThenPublic.toString()));

        // Test access status criteria
        getVersionFileCountsResponse = UtilIT.getVersionFileCounts(datasetId, DS_VERSION_LATEST, null, FileSearchCriteria.FileAccessStatus.Public.toString(), null, null, null, false, apiToken);

        getVersionFileCountsResponse.then().assertThat().statusCode(OK.getStatusCode());

        responseJsonPath = getVersionFileCountsResponse.jsonPath();
        responseCountPerContentTypeMap = responseJsonPath.get("data.perContentType");
        responseCountPerCategoryNameMap = responseJsonPath.get("data.perCategoryName");
        responseCountPerTabularTagNameMap = responseJsonPath.get("data.perTabularTagName");
        responseCountPerAccessStatusMap = responseJsonPath.get("data.perAccessStatus");

        assertEquals(3, (Integer) responseJsonPath.get("data.total"));
        assertEquals(1, responseCountPerContentTypeMap.get("image/png"));
        assertEquals(2, responseCountPerContentTypeMap.get("text/plain"));
        assertEquals(2, responseCountPerContentTypeMap.size());
        assertEquals(0, responseCountPerCategoryNameMap.size());
        assertEquals(0, responseCountPerTabularTagNameMap.size());
        assertEquals(1, responseCountPerAccessStatusMap.size());
        assertEquals(3, responseCountPerAccessStatusMap.get(FileSearchCriteria.FileAccessStatus.Public.toString()));

        // Test invalid access status
        String invalidStatus = "invalidStatus";
        Response getVersionFilesResponseInvalidStatus = UtilIT.getVersionFileCounts(datasetId, DS_VERSION_LATEST, null, invalidStatus, null, null, null, false, apiToken);
        getVersionFilesResponseInvalidStatus.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(BundleUtil.getStringFromBundle("datasets.api.version.files.invalid.access.status", List.of(invalidStatus))));

        // Test category name criteria
        getVersionFileCountsResponse = UtilIT.getVersionFileCounts(datasetId, DS_VERSION_LATEST, null, null, testCategory, null, null, false, apiToken);

        getVersionFileCountsResponse.then().assertThat().statusCode(OK.getStatusCode());

        responseJsonPath = getVersionFileCountsResponse.jsonPath();
        responseCountPerContentTypeMap = responseJsonPath.get("data.perContentType");
        responseCountPerCategoryNameMap = responseJsonPath.get("data.perCategoryName");
        responseCountPerTabularTagNameMap = responseJsonPath.get("data.perTabularTagName");
        responseCountPerAccessStatusMap = responseJsonPath.get("data.perAccessStatus");

        assertEquals(1, (Integer) responseJsonPath.get("data.total"));
        assertEquals(1, responseCountPerContentTypeMap.get("image/png"));
        assertEquals(1, responseCountPerContentTypeMap.size());
        assertEquals(1, responseCountPerCategoryNameMap.size());
        assertEquals(1, responseCountPerCategoryNameMap.get(testCategory));
        assertEquals(0, responseCountPerTabularTagNameMap.size());
        assertEquals(1, responseCountPerAccessStatusMap.size());
        assertEquals(1, responseCountPerAccessStatusMap.get(FileSearchCriteria.FileAccessStatus.EmbargoedThenPublic.toString()));

        // Test search text criteria
        getVersionFileCountsResponse = UtilIT.getVersionFileCounts(datasetId, DS_VERSION_LATEST, null, null, null, null, "test", false, apiToken);

        getVersionFileCountsResponse.then().assertThat().statusCode(OK.getStatusCode());

        responseJsonPath = getVersionFileCountsResponse.jsonPath();
        responseCountPerContentTypeMap = responseJsonPath.get("data.perContentType");
        responseCountPerCategoryNameMap = responseJsonPath.get("data.perCategoryName");
        responseCountPerTabularTagNameMap = responseJsonPath.get("data.perTabularTagName");
        responseCountPerAccessStatusMap = responseJsonPath.get("data.perAccessStatus");

        assertEquals(3, (Integer) responseJsonPath.get("data.total"));
        assertEquals(1, responseCountPerContentTypeMap.get("image/png"));
        assertEquals(2, responseCountPerContentTypeMap.get("text/plain"));
        assertEquals(2, responseCountPerContentTypeMap.size());
        assertEquals(0, responseCountPerCategoryNameMap.size());
        assertEquals(0, responseCountPerTabularTagNameMap.size());
        assertEquals(1, responseCountPerAccessStatusMap.size());
        assertEquals(3, responseCountPerAccessStatusMap.get(FileSearchCriteria.FileAccessStatus.Public.toString()));

        // Test tabular tag name criteria
        String pathToTabularTestFile = "src/test/resources/tab/test.tab";
        Response uploadTabularFileResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToTabularTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadTabularFileResponse.then().assertThat().statusCode(OK.getStatusCode());

        String tabularFileId = uploadTabularFileResponse.getBody().jsonPath().getString("data.files[0].dataFile.id");

        // Ensure tabular file is ingested
        sleep(2000);

        String tabularTagName = "Survey";
        Response setFileTabularTagsResponse = UtilIT.setFileTabularTags(tabularFileId, apiToken, List.of(tabularTagName));
        setFileTabularTagsResponse.then().assertThat().statusCode(OK.getStatusCode());

        getVersionFileCountsResponse = UtilIT.getVersionFileCounts(datasetId, DS_VERSION_LATEST, null, null, null, tabularTagName, null, false, apiToken);

        getVersionFileCountsResponse.then().assertThat().statusCode(OK.getStatusCode());

        responseJsonPath = getVersionFileCountsResponse.jsonPath();
        responseCountPerContentTypeMap = responseJsonPath.get("data.perContentType");
        responseCountPerCategoryNameMap = responseJsonPath.get("data.perCategoryName");
        responseCountPerTabularTagNameMap = responseJsonPath.get("data.perTabularTagName");
        responseCountPerAccessStatusMap = responseJsonPath.get("data.perAccessStatus");

        assertEquals(1, (Integer) responseJsonPath.get("data.total"));
        assertEquals(1, responseCountPerContentTypeMap.get("text/tab-separated-values"));
        assertEquals(1, responseCountPerContentTypeMap.size());
        assertEquals(0, responseCountPerCategoryNameMap.size());
        assertEquals(1, responseCountPerTabularTagNameMap.size());
        assertEquals(1, responseCountPerTabularTagNameMap.get(tabularTagName));
        assertEquals(1, responseCountPerAccessStatusMap.size());
        assertEquals(1, responseCountPerAccessStatusMap.get(FileSearchCriteria.FileAccessStatus.Public.toString()));

        // Test Deaccessioned
        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, "Test deaccession reason.", null, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // includeDeaccessioned false
        Response getVersionFileCountsResponseNoDeaccessioned = UtilIT.getVersionFileCounts(datasetId, DS_VERSION_LATEST_PUBLISHED, null, null, null, null, null, false, apiToken);
        getVersionFileCountsResponseNoDeaccessioned.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // includeDeaccessioned true
        Response getVersionFileCountsResponseDeaccessioned = UtilIT.getVersionFileCounts(datasetId, DS_VERSION_LATEST_PUBLISHED, null, null, null, null, null, true, apiToken);
        getVersionFileCountsResponseDeaccessioned.then().assertThat().statusCode(OK.getStatusCode());

        responseJsonPath = getVersionFileCountsResponseDeaccessioned.jsonPath();
        assertEquals(5, (Integer) responseJsonPath.get("data.total"));

        // Test that the dataset file counts for a deaccessioned dataset cannot be accessed by a guest
        // By latest published version
        Response getDatasetVersionResponse = UtilIT.getVersionFileCounts(datasetId, DS_VERSION_LATEST_PUBLISHED, null, null, null, null, null, true, null);
        getDatasetVersionResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());
        // By specific version 1.0
        getDatasetVersionResponse = UtilIT.getVersionFileCounts(datasetId, "1.0", null, null, null, null, null, true, null);
        getDatasetVersionResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void deaccessionDataset() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        String testDeaccessionReason = "Test deaccession reason.";
        String testDeaccessionForwardURL = "http://demo.dataverse.org";

        // Test that draft and latest version constants are not allowed and a bad request error is received
        String expectedInvalidVersionIdentifierError = BundleUtil.getStringFromBundle("datasets.api.deaccessionDataset.invalid.version.identifier.error", List.of(DS_VERSION_LATEST_PUBLISHED));

        Response deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_DRAFT, testDeaccessionReason, testDeaccessionForwardURL, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(expectedInvalidVersionIdentifierError));

        deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST, testDeaccessionReason, testDeaccessionForwardURL, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(expectedInvalidVersionIdentifierError));

        // Test that a not found error occurs when there is no published version available
        deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, testDeaccessionReason, testDeaccessionForwardURL, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // Publish test dataset
        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Test that a bad request error is received when the forward URL exceeds DEACCESSION_LINK_MAX_LENGTH
        String testInvalidDeaccessionForwardURL = RandomStringUtils.randomAlphabetic(DEACCESSION_LINK_MAX_LENGTH + 1);

        deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, testDeaccessionReason, testInvalidDeaccessionForwardURL, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode())
                .body("message", containsString(testInvalidDeaccessionForwardURL));

        // Test that the dataset is successfully deaccessioned when published and valid deaccession params are sent
        deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, testDeaccessionReason, testDeaccessionForwardURL, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Test that a not found error occurs when the only published version has already been deaccessioned
        deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, testDeaccessionReason, testDeaccessionForwardURL, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // Test that a dataset can be deaccessioned without forward URL
        createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, testDeaccessionReason, null, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void getDownloadSize() throws IOException, InterruptedException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String datasetPersistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        // Creating test text files
        String testFileName1 = "test_1.txt";
        String testFileName2 = "test_2.txt";

        int testFileSize1 = 50;
        int testFileSize2 = 200;

        UtilIT.createAndUploadTestFile(datasetPersistentId, testFileName1, new byte[testFileSize1], apiToken);
        UtilIT.createAndUploadTestFile(datasetPersistentId, testFileName2, new byte[testFileSize2], apiToken);

        int expectedTextFilesStorageSize = testFileSize1 + testFileSize2;

        // Get the total size when there are no tabular files
        Response getDownloadSizeResponse = UtilIT.getDownloadSize(datasetId, DS_VERSION_LATEST, null, null, null, null, null, DatasetVersionFilesServiceBean.FileDownloadSizeMode.All.toString(), false, apiToken);
        getDownloadSizeResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.storageSize", equalTo(expectedTextFilesStorageSize));

        // Upload test tabular file
        String pathToTabularTestFile = "src/test/resources/tab/test.tab";
        Response uploadTabularFileResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToTabularTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadTabularFileResponse.then().assertThat().statusCode(OK.getStatusCode());

        int tabularOriginalSize = 157;

        // Ensure tabular file is ingested
        Thread.sleep(2000);

        // Get the total size ignoring the original tabular file sizes
        getDownloadSizeResponse = UtilIT.getDownloadSize(datasetId, DS_VERSION_LATEST, null, null, null, null, null, DatasetVersionFilesServiceBean.FileDownloadSizeMode.Archival.toString(), false, apiToken);
        getDownloadSizeResponse.then().assertThat().statusCode(OK.getStatusCode());

        int actualSizeIgnoringOriginalTabularSizes = Integer.parseInt(getDownloadSizeResponse.getBody().jsonPath().getString("data.storageSize"));

        // Assert that the size has been incremented with the last uploaded file
        assertTrue(actualSizeIgnoringOriginalTabularSizes > expectedTextFilesStorageSize);

        // Get the total size including only original sizes and ignoring archival sizes for tabular files
        int expectedSizeIncludingOnlyOriginalForTabular = tabularOriginalSize + expectedTextFilesStorageSize;

        getDownloadSizeResponse = UtilIT.getDownloadSize(datasetId, DS_VERSION_LATEST, null, null, null, null, null, DatasetVersionFilesServiceBean.FileDownloadSizeMode.Original.toString(), false, apiToken);
        getDownloadSizeResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.storageSize", equalTo(expectedSizeIncludingOnlyOriginalForTabular));

        // Get the total size including both the original and archival tabular file sizes
        int tabularArchivalSize = actualSizeIgnoringOriginalTabularSizes - expectedTextFilesStorageSize;
        int expectedSizeIncludingAllSizes = tabularArchivalSize + tabularOriginalSize + expectedTextFilesStorageSize;

        getDownloadSizeResponse = UtilIT.getDownloadSize(datasetId, DS_VERSION_LATEST, null, null, null, null, null, DatasetVersionFilesServiceBean.FileDownloadSizeMode.All.toString(), false, apiToken);
        getDownloadSizeResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.storageSize", equalTo(expectedSizeIncludingAllSizes));

        // Get the total size sending invalid file download size mode
        String invalidMode = "invalidMode";
        getDownloadSizeResponse = UtilIT.getDownloadSize(datasetId, DS_VERSION_LATEST, null, null, null, null, null, invalidMode, false, apiToken);
        getDownloadSizeResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Invalid mode: " + invalidMode));

        // Upload second test tabular file (same source as before)
        uploadTabularFileResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToTabularTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadTabularFileResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Ensure tabular file is ingested
        Thread.sleep(2000);

        // Get the total size including only original sizes and ignoring archival sizes for tabular files
        expectedSizeIncludingOnlyOriginalForTabular = tabularOriginalSize + expectedSizeIncludingOnlyOriginalForTabular;

        getDownloadSizeResponse = UtilIT.getDownloadSize(datasetId, DS_VERSION_LATEST, null, null, null, null, null, DatasetVersionFilesServiceBean.FileDownloadSizeMode.Original.toString(), false, apiToken);
        getDownloadSizeResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.storageSize", equalTo(expectedSizeIncludingOnlyOriginalForTabular));

        // Get the total size including both the original and archival tabular file sizes
        expectedSizeIncludingAllSizes = tabularArchivalSize + tabularOriginalSize + expectedSizeIncludingAllSizes;

        getDownloadSizeResponse = UtilIT.getDownloadSize(datasetId, DS_VERSION_LATEST, null, null, null, null, null, DatasetVersionFilesServiceBean.FileDownloadSizeMode.All.toString(), false, apiToken);
        getDownloadSizeResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.storageSize", equalTo(expectedSizeIncludingAllSizes));

        // Get the total size including both the original and archival tabular file sizes with search criteria
        getDownloadSizeResponse = UtilIT.getDownloadSize(datasetId, DS_VERSION_LATEST, "text/plain", FileSearchCriteria.FileAccessStatus.Public.toString(), null, null, "test_", DatasetVersionFilesServiceBean.FileDownloadSizeMode.All.toString(), false, apiToken);
        // We exclude tabular sizes from the expected result since the search criteria filters by content type "text/plain" and search text "test_"
        int expectedSizeIncludingAllSizesAndApplyingCriteria = testFileSize1 + testFileSize2;
        getDownloadSizeResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.storageSize", equalTo(expectedSizeIncludingAllSizesAndApplyingCriteria));
        // Test Deaccessioned
        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, "Test deaccession reason.", null, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // includeDeaccessioned false
        Response getVersionFileCountsResponseNoDeaccessioned = UtilIT.getDownloadSize(datasetId, DS_VERSION_LATEST_PUBLISHED, null, null, null, null, null, DatasetVersionFilesServiceBean.FileDownloadSizeMode.All.toString(), false, apiToken);
        getVersionFileCountsResponseNoDeaccessioned.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // includeDeaccessioned true
        Response getVersionFileCountsResponseDeaccessioned = UtilIT.getDownloadSize(datasetId, DS_VERSION_LATEST_PUBLISHED, null, null, null, null, null, DatasetVersionFilesServiceBean.FileDownloadSizeMode.All.toString(), true, apiToken);
        getVersionFileCountsResponseDeaccessioned.then().assertThat().statusCode(OK.getStatusCode());

        // Test that the dataset file counts for a deaccessioned dataset cannot be accessed by a guest
        // By latest published version
        Response getVersionFileCountsGuestUserResponse = UtilIT.getDownloadSize(datasetId, DS_VERSION_LATEST_PUBLISHED, null, null, null, null, null, DatasetVersionFilesServiceBean.FileDownloadSizeMode.All.toString(), true, null);
        getVersionFileCountsGuestUserResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());
        // By specific version 1.0
        getVersionFileCountsGuestUserResponse = UtilIT.getDownloadSize(datasetId, "1.0", null, null, null, null, null, DatasetVersionFilesServiceBean.FileDownloadSizeMode.All.toString(), true, null);
        getVersionFileCountsGuestUserResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetDownloadCount() {
        Response createUser1 = UtilIT.createRandomUser();
        createUser1.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser1);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String datasetPersistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        Response uploadFileResponse = UtilIT.uploadFileViaNative(String.valueOf(datasetId), "scripts/search/data/replace_test/004.txt", apiToken);
        uploadFileResponse.prettyPrint();
        Integer fileId = Integer.parseInt(JsonPath.from(uploadFileResponse.body().asString()).getString("data.files[0].dataFile.id"));
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);

        Response createUser2 = UtilIT.createRandomUser();
        createUser2.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken2 = UtilIT.getApiTokenFromResponse(createUser2);
        UtilIT.downloadFile(fileId, apiToken2);

        UtilIT.setSetting(":MDCStartDate", "2019-10-01");
        Response countResponse = UtilIT.getDownloadCountByDatasetId(datasetId, apiToken2, null);
        countResponse.prettyPrint();
        countResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("downloadCount", equalTo(0))
                .body("MDCStartDate", equalTo("2019-10-01"));
        countResponse = UtilIT.getDownloadCountByDatasetId(datasetId, apiToken2, true);
        countResponse.prettyPrint();
        countResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("downloadCount", equalTo(1));
    }

    @Test
    public void testGetUserPermissionsOnDataset() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        // Call with valid dataset id
        Response getUserPermissionsOnDatasetResponse = UtilIT.getUserPermissionsOnDataset(Integer.toString(datasetId), apiToken);
        getUserPermissionsOnDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());
        boolean canViewUnpublishedDataset = JsonPath.from(getUserPermissionsOnDatasetResponse.body().asString()).getBoolean("data.canViewUnpublishedDataset");
        assertTrue(canViewUnpublishedDataset);
        boolean canEditDataset = JsonPath.from(getUserPermissionsOnDatasetResponse.body().asString()).getBoolean("data.canEditDataset");
        assertTrue(canEditDataset);
        boolean canPublishDataset = JsonPath.from(getUserPermissionsOnDatasetResponse.body().asString()).getBoolean("data.canPublishDataset");
        assertTrue(canPublishDataset);
        boolean canManageDatasetPermissions = JsonPath.from(getUserPermissionsOnDatasetResponse.body().asString()).getBoolean("data.canManageDatasetPermissions");
        assertTrue(canManageDatasetPermissions);
        boolean canDeleteDatasetDraft = JsonPath.from(getUserPermissionsOnDatasetResponse.body().asString()).getBoolean("data.canDeleteDatasetDraft");
        assertTrue(canDeleteDatasetDraft);

        // Call with invalid dataset id
        Response getUserPermissionsOnDatasetInvalidIdResponse = UtilIT.getUserPermissionsOnDataset("testInvalidId", apiToken);
        getUserPermissionsOnDatasetInvalidIdResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    //Requires that a Globus remote store be set up as with the parameters in the GlobusOverlayAccessIOTest class
    //Tests whether the API call succeeds and has some of the expected parameters
    @Test
    @Disabled
    public void testGetGlobusUploadParameters() {
        //Creates managed and remote Globus stores
        GlobusOverlayAccessIOTest.setUp();

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response setDriver = UtilIT.setDatasetStorageDriver(datasetId, System.getProperty("dataverse.files.globusr.label"), apiToken);
        assertEquals(200, setDriver.getStatusCode());

        Response getUploadParams = UtilIT.getDatasetGlobusUploadParameters(datasetId, "en_us", apiToken);
        assertEquals(200, getUploadParams.getStatusCode());
        JsonObject data = JsonUtil.getJsonObject(getUploadParams.getBody().asString());
        JsonObject queryParams = data.getJsonObject("queryParameters");
        assertEquals("en_us", queryParams.getString("dvLocale"));
        assertEquals("false", queryParams.getString("managed"));
        //Assumes only one reference endpoint with a basepath is configured
        assertTrue(queryParams.getJsonArray("referenceEndpointsWithPaths").get(0).toString().indexOf(System.getProperty("dataverse.files.globusr." + AbstractRemoteOverlayAccessIO.REFERENCE_ENDPOINTS_WITH_BASEPATHS)) > -1);
        JsonArray signedUrls = data.getJsonArray("signedUrls");
        boolean found = false;
        for (int i = 0; i < signedUrls.size(); i++) {
            JsonObject signedUrl = signedUrls.getJsonObject(i);
            if (signedUrl.getString("name").equals("requestGlobusReferencePaths")) {
                found=true;
                break;
            }
        }
        assertTrue(found);
        //Removes managed and remote Globus stores
        GlobusOverlayAccessIOTest.tearDown();
    }

    @Test
    public void testGetCanDownloadAtLeastOneFile() {
        Response createUserResponse = UtilIT.createRandomUser();
        createUserResponse.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        String datasetPersistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");

        // Upload file
        String pathToTestFile = "src/test/resources/images/coffeeshop.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToTestFile, Json.createObjectBuilder().build(), apiToken);
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());

        String fileId = JsonPath.from(uploadResponse.body().asString()).getString("data.files[0].dataFile.id");

        // Publish dataset version
        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Create a second user to call the getCanDownloadAtLeastOneFile method
        Response createSecondUserResponse = UtilIT.createRandomUser();
        createSecondUserResponse.then().assertThat().statusCode(OK.getStatusCode());
        String secondUserApiToken = UtilIT.getApiTokenFromResponse(createSecondUserResponse);
        String secondUserUsername = UtilIT.getUsernameFromResponse(createSecondUserResponse);

        // Call when a file is released
        Response canDownloadAtLeastOneFileResponse = UtilIT.getCanDownloadAtLeastOneFile(Integer.toString(datasetId), DS_VERSION_LATEST, secondUserApiToken);
        canDownloadAtLeastOneFileResponse.then().assertThat().statusCode(OK.getStatusCode());
        boolean canDownloadAtLeastOneFile = JsonPath.from(canDownloadAtLeastOneFileResponse.body().asString()).getBoolean("data");
        assertTrue(canDownloadAtLeastOneFile);

        // Restrict file
        Response restrictFileResponse = UtilIT.restrictFile(fileId, true, apiToken);
        restrictFileResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Publish dataset version
        publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Call when a file is restricted and the user does not have access
        canDownloadAtLeastOneFileResponse = UtilIT.getCanDownloadAtLeastOneFile(Integer.toString(datasetId), DS_VERSION_LATEST, secondUserApiToken);
        canDownloadAtLeastOneFileResponse.then().assertThat().statusCode(OK.getStatusCode());
        canDownloadAtLeastOneFile = JsonPath.from(canDownloadAtLeastOneFileResponse.body().asString()).getBoolean("data");
        assertFalse(canDownloadAtLeastOneFile);

        // Grant restricted file access to the user
        Response grantFileAccessResponse = UtilIT.grantFileAccess(fileId, "@" + secondUserUsername, apiToken);
        grantFileAccessResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Call when a file is restricted and the user has access
        canDownloadAtLeastOneFileResponse = UtilIT.getCanDownloadAtLeastOneFile(Integer.toString(datasetId), DS_VERSION_LATEST, secondUserApiToken);
        canDownloadAtLeastOneFileResponse.then().assertThat().statusCode(OK.getStatusCode());
        canDownloadAtLeastOneFile = JsonPath.from(canDownloadAtLeastOneFileResponse.body().asString()).getBoolean("data");
        assertTrue(canDownloadAtLeastOneFile);

        // Create a third user to call the getCanDownloadAtLeastOneFile method
        Response createThirdUserResponse = UtilIT.createRandomUser();
        createThirdUserResponse.then().assertThat().statusCode(OK.getStatusCode());
        String thirdUserApiToken = UtilIT.getApiTokenFromResponse(createThirdUserResponse);
        String thirdUserUsername = UtilIT.getUsernameFromResponse(createThirdUserResponse);

        // Call when a file is restricted and the user does not have access
        canDownloadAtLeastOneFileResponse = UtilIT.getCanDownloadAtLeastOneFile(Integer.toString(datasetId), DS_VERSION_LATEST, thirdUserApiToken);
        canDownloadAtLeastOneFileResponse.then().assertThat().statusCode(OK.getStatusCode());
        canDownloadAtLeastOneFile = JsonPath.from(canDownloadAtLeastOneFileResponse.body().asString()).getBoolean("data");
        assertFalse(canDownloadAtLeastOneFile);

        // Grant fileDownloader role on the dataset to the user
        Response grantDatasetFileDownloaderRoleOnDatasetResponse = UtilIT.grantRoleOnDataset(datasetPersistentId, "fileDownloader", "@" + thirdUserUsername, apiToken);
        grantDatasetFileDownloaderRoleOnDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Call when a file is restricted and the user has fileDownloader role on the dataset
        canDownloadAtLeastOneFileResponse = UtilIT.getCanDownloadAtLeastOneFile(Integer.toString(datasetId), DS_VERSION_LATEST, thirdUserApiToken);
        canDownloadAtLeastOneFileResponse.then().assertThat().statusCode(OK.getStatusCode());
        canDownloadAtLeastOneFile = JsonPath.from(canDownloadAtLeastOneFileResponse.body().asString()).getBoolean("data");
        assertTrue(canDownloadAtLeastOneFile);

        // Create a fourth user to call the getCanDownloadAtLeastOneFile method
        Response createFourthUserResponse = UtilIT.createRandomUser();
        createFourthUserResponse.then().assertThat().statusCode(OK.getStatusCode());
        String fourthUserApiToken = UtilIT.getApiTokenFromResponse(createFourthUserResponse);
        String fourthUserUsername = UtilIT.getUsernameFromResponse(createFourthUserResponse);

        // Call when a file is restricted and the user does not have access
        canDownloadAtLeastOneFileResponse = UtilIT.getCanDownloadAtLeastOneFile(Integer.toString(datasetId), DS_VERSION_LATEST, fourthUserApiToken);
        canDownloadAtLeastOneFileResponse.then().assertThat().statusCode(OK.getStatusCode());
        canDownloadAtLeastOneFile = JsonPath.from(canDownloadAtLeastOneFileResponse.body().asString()).getBoolean("data");
        assertFalse(canDownloadAtLeastOneFile);

        // Grant fileDownloader role on the collection to the user
        Response grantDatasetFileDownloaderRoleOnCollectionResponse = UtilIT.grantRoleOnDataverse(dataverseAlias, "fileDownloader", "@" + fourthUserUsername, apiToken);
        grantDatasetFileDownloaderRoleOnCollectionResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Call when a file is restricted and the user has fileDownloader role on the collection
        canDownloadAtLeastOneFileResponse = UtilIT.getCanDownloadAtLeastOneFile(Integer.toString(datasetId), DS_VERSION_LATEST, fourthUserApiToken);
        canDownloadAtLeastOneFileResponse.then().assertThat().statusCode(OK.getStatusCode());
        canDownloadAtLeastOneFile = JsonPath.from(canDownloadAtLeastOneFileResponse.body().asString()).getBoolean("data");
        assertTrue(canDownloadAtLeastOneFile);

        // Call with invalid dataset id
        Response getUserPermissionsOnDatasetInvalidIdResponse = UtilIT.getCanDownloadAtLeastOneFile("testInvalidId", DS_VERSION_LATEST, secondUserApiToken);
        getUserPermissionsOnDatasetInvalidIdResponse.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testCompareDatasetVersionsAPI() throws InterruptedException {

        Response createUser = UtilIT.createRandomUser();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(datasetId, apiToken);
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;
        // used for all added files
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", "my description")
                .add("directoryLabel", "/data/subdir1/")
                .add("categories", Json.createArrayBuilder()
                        .add("Data")
                );
        JsonObject jsonObj = json.build();
        String pathToFile = "src/main/webapp/resources/images/dataverse-icon-1200.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(String.valueOf(datasetId), pathToFile, jsonObj, apiToken);
        uploadResponse.prettyPrint();
        uploadResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer modifyFileId = UtilIT.getDataFileIdFromResponse(uploadResponse);
        pathToFile = "src/main/webapp/resources/images/dataverseproject_logo.jpg";
        uploadResponse = UtilIT.uploadFileViaNative(String.valueOf(datasetId), pathToFile, jsonObj, apiToken);
        uploadResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer deleteFileId = UtilIT.getDataFileIdFromResponse(uploadResponse);

        pathToFile = "src/main/webapp/resources/images/fav/favicon-16x16.png";
        uploadResponse = UtilIT.uploadFileViaNative(String.valueOf(datasetId), pathToFile, jsonObj, apiToken);
        uploadResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer replaceFileId = UtilIT.getDataFileIdFromResponse(uploadResponse);

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());

        // post publish update to create DRAFT version
        String pathToJsonFilePostPub = "doc/sphinx-guides/source/_static/api/dataset-add-metadata-after-pub.json";
        Response addDataToPublishedVersion = UtilIT.addDatasetMetadataViaNative(datasetPersistentId, pathToJsonFilePostPub, apiToken);
        addDataToPublishedVersion.then().assertThat().statusCode(OK.getStatusCode());

        // Test adding a file
        pathToFile = "src/test/resources/tab/test.tab";
        Response uploadTabularFileResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToFile, jsonObj, apiToken);
        uploadTabularFileResponse.prettyPrint();
        uploadTabularFileResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer addedFileId = UtilIT.getDataFileIdFromResponse(uploadTabularFileResponse);

        // Ensure tabular file is ingested
        sleep(2000);

        String tabularTagName = "Survey";
        Response setFileTabularTagsResponse = UtilIT.setFileTabularTags(String.valueOf(addedFileId), apiToken, List.of(tabularTagName));
        setFileTabularTagsResponse.prettyPrint();
        setFileTabularTagsResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Test removing a file
        uploadResponse = UtilIT.deleteFile(deleteFileId, apiToken);
        uploadResponse.prettyPrint();
        uploadResponse.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());

        // Test Replacing a file
        Response replaceResponse = UtilIT.replaceFile(String.valueOf(replaceFileId), "src/main/webapp/resources/images/fav/favicon-32x32.png", jsonObj, apiToken);
        replaceResponse.prettyPrint();
        replaceResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Test modify by restricting the file
        Response restrictResponse = UtilIT.restrictFile(modifyFileId.toString(), true, apiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Also test a terms of access change
        String jsonLDTerms = "{\"https://dataverse.org/schema/core#fileTermsOfAccess\":{\"https://dataverse.org/schema/core#dataAccessPlace\":\"Somewhere\"}}";
        Response updateTerms = UtilIT.updateDatasetJsonLDMetadata(datasetId, apiToken, jsonLDTerms, true);
        updateTerms.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response compareResponse = UtilIT.compareDatasetVersions(datasetPersistentId, ":latest-published", ":draft", apiToken);
        compareResponse.prettyPrint();
        compareResponse.then().assertThat()
                .body("data.oldVersion.versionNumber", CoreMatchers.equalTo("1.0"))
                .body("data.oldVersion.versionState", CoreMatchers.equalTo("RELEASED"))
                .body("data.newVersion.versionNumber", CoreMatchers.equalTo("DRAFT"))
                .body("data.newVersion.versionState", CoreMatchers.equalTo("DRAFT"))
                .body("data.metadataChanges[0].blockName", CoreMatchers.equalTo("Citation Metadata"))
                .body("data.metadataChanges[0].changed[0].fieldName", CoreMatchers.equalTo("Author"))
                .body("data.metadataChanges[0].changed[0].oldValue", CoreMatchers.containsString("Finch, Fiona; (Birds Inc.)"))
                .body("data.metadataChanges[1].blockName", CoreMatchers.equalTo("Life Sciences Metadata"))
                .body("data.metadataChanges[1].changed[0].fieldName", CoreMatchers.equalTo("Design Type"))
                .body("data.metadataChanges[1].changed[0].oldValue", CoreMatchers.containsString(""))
                .body("data.metadataChanges[1].changed[0].newValue", CoreMatchers.containsString("Nested Case Control Design"))
                .body("data.metadataChanges[1].changed[0].newValue", CoreMatchers.containsString("Parallel Group Design"))
                .body("data.filesAdded[0].fileName", CoreMatchers.equalTo("test.tab"))
                .body("data.filesAdded[0].filePath", CoreMatchers.equalTo("data/subdir1"))
                .body("data.filesAdded[0].description", CoreMatchers.equalTo("my description"))
                .body("data.filesAdded[0].tags[0]", CoreMatchers.equalTo("Survey"))
                .body("data.filesRemoved[0].fileName", CoreMatchers.equalTo("dataverseproject_logo.jpg"))
                .body("data.fileChanges[0].fileName", CoreMatchers.equalTo("dataverse-icon-1200.png"))
                .body("data.fileChanges[0].changed[0].newValue", CoreMatchers.equalTo("true"))
                .body("data.filesReplaced[0].oldFile.fileName", CoreMatchers.equalTo("favicon-16x16.png"))
                .body("data.filesReplaced[0].newFile.fileName", CoreMatchers.equalTo("favicon-32x32.png"))
                .body("data.TermsOfAccess", CoreMatchers.notNullValue())
                .statusCode(OK.getStatusCode());

        compareResponse = UtilIT.compareDatasetVersions(datasetPersistentId, ":draft", ":latest-published", apiToken);
        compareResponse.prettyPrint();
        compareResponse.then().assertThat()
                .body("message", CoreMatchers.equalTo(BundleUtil.getStringFromBundle("dataset.version.compare.incorrect.order")))
                .statusCode(BAD_REQUEST.getStatusCode());
        
        
        Response deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, "Test deaccession reason.", null, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());
        
        compareResponse = UtilIT.compareDatasetVersions(datasetPersistentId, ":latest-published", ":draft", apiToken, false);
        compareResponse.prettyPrint();
        compareResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        compareResponse = UtilIT.compareDatasetVersions(datasetPersistentId,  ":latest-published", ":draft", apiToken, true);
        compareResponse.prettyPrint();
        compareResponse.then().assertThat().statusCode(OK.getStatusCode());

        
        
    }
    
    @Test
    public void testSummaryDatasetVersionsDifferencesAPI() throws InterruptedException {

        Response createUser = UtilIT.createRandomUser();
        assertEquals(200, createUser.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        Response getDatasetJsonBeforePublishing = UtilIT.nativeGet(datasetId, apiToken);
        String protocol = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.protocol");
        String authority = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.authority");
        String identifier = JsonPath.from(getDatasetJsonBeforePublishing.getBody().asString()).getString("data.identifier");
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;
        // used for all added files
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("description", "my description")
                .add("directoryLabel", "/data/subdir1/")
                .add("categories", Json.createArrayBuilder()
                        .add("Data")
                );
        JsonObject jsonObj = json.build();
        String pathToFile = "src/main/webapp/resources/images/dataverse-icon-1200.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(String.valueOf(datasetId), pathToFile, jsonObj, apiToken);
        uploadResponse.prettyPrint();
        uploadResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer modifyFileId = UtilIT.getDataFileIdFromResponse(uploadResponse);
        pathToFile = "src/main/webapp/resources/images/dataverseproject_logo.jpg";
        uploadResponse = UtilIT.uploadFileViaNative(String.valueOf(datasetId), pathToFile, jsonObj, apiToken);
        uploadResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer deleteFileId = UtilIT.getDataFileIdFromResponse(uploadResponse);

        pathToFile = "src/main/webapp/resources/images/fav/favicon-16x16.png";
        uploadResponse = UtilIT.uploadFileViaNative(String.valueOf(datasetId), pathToFile, jsonObj, apiToken);
        uploadResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer replaceFileId = UtilIT.getDataFileIdFromResponse(uploadResponse);

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());

        // post publish update to create DRAFT version
        String pathToJsonFilePostPub = "doc/sphinx-guides/source/_static/api/dataset-add-metadata-after-pub.json";
        Response addDataToPublishedVersion = UtilIT.addDatasetMetadataViaNative(datasetPersistentId, pathToJsonFilePostPub, apiToken);
        addDataToPublishedVersion.then().assertThat().statusCode(OK.getStatusCode());

        // Test adding a file
        pathToFile = "src/test/resources/tab/test.tab";
        Response uploadTabularFileResponse = UtilIT.uploadFileViaNative(Integer.toString(datasetId), pathToFile, jsonObj, apiToken);
        uploadTabularFileResponse.prettyPrint();
        uploadTabularFileResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        Integer addedFileId = UtilIT.getDataFileIdFromResponse(uploadTabularFileResponse);

        // Ensure tabular file is ingested
        sleep(2000);

        String tabularTagName = "Survey";
        Response setFileTabularTagsResponse = UtilIT.setFileTabularTags(String.valueOf(addedFileId), apiToken, List.of(tabularTagName));
        setFileTabularTagsResponse.prettyPrint();
        setFileTabularTagsResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Test removing a file
        uploadResponse = UtilIT.deleteFile(deleteFileId, apiToken);
        uploadResponse.prettyPrint();
        uploadResponse.then().assertThat()
                .statusCode(NO_CONTENT.getStatusCode());

        // Test Replacing a file
        Response replaceResponse = UtilIT.replaceFile(String.valueOf(replaceFileId), "src/main/webapp/resources/images/fav/favicon-32x32.png", jsonObj, apiToken);
        replaceResponse.prettyPrint();
        replaceResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Test modify by restricting the file
        Response restrictResponse = UtilIT.restrictFile(modifyFileId.toString(), true, apiToken);
        restrictResponse.prettyPrint();
        restrictResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Also test a terms of access change
        String jsonLDTerms = "{\"https://dataverse.org/schema/core#fileTermsOfAccess\":{\"https://dataverse.org/schema/core#dataAccessPlace\":\"Somewhere\"}}";
        Response updateTerms = UtilIT.updateDatasetJsonLDMetadata(datasetId, apiToken, jsonLDTerms, true);
        updateTerms.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        

        Response compareResponse = UtilIT.summaryDatasetVersionDifferences(datasetPersistentId, apiToken);
        compareResponse.prettyPrint(); 

        compareResponse.then().assertThat()
                .body("data[1].versionNumber", equalTo("1.0"))
                .body("data[1].summary", equalTo("firstPublished"))
                .body("data[0].versionNumber", equalTo("DRAFT"))
                .body("data[0].summary.'Citation Metadata'.Author.added", equalTo(2))
                .body("data[0].summary.'Citation Metadata'.Subject.added", equalTo(2))
                .body("data[0].summary.'Additional Citation Metadata'.changed", equalTo(0))
                .body("data[0].summary.'Additional Citation Metadata'.added", equalTo(2))
                .body("data[0].summary.'Life Sciences Metadata'.added", equalTo(2))
                .body("data[0].summary.'Life Sciences Metadata'.deleted", equalTo(0))
                .body("data[0].summary.files.added", equalTo(1))
                .body("data[0].summary.files.changedFileMetaData", equalTo(2))
                .statusCode(OK.getStatusCode());

        //user with no privileges will only see the published version
        
        Response createUsernoPriv = UtilIT.createRandomUser();
        assertEquals(200, createUsernoPriv.getStatusCode());
        String apiTokenNoPriv = UtilIT.getApiTokenFromResponse(createUsernoPriv);
        
        Response compareResponse2 = UtilIT.summaryDatasetVersionDifferences(datasetPersistentId, apiTokenNoPriv);
        compareResponse2.prettyPrint();
        compareResponse2.then().assertThat()
                .body("data[0].versionNumber", CoreMatchers.equalTo("1.0"))
                .body("data[0].summary", CoreMatchers.equalTo("firstPublished"))
                .statusCode(OK.getStatusCode());
        
        Response deaccessionDatasetResponse = UtilIT.deaccessionDataset(datasetId, DS_VERSION_LATEST_PUBLISHED, "Test deaccession reason.", null, apiToken);
        deaccessionDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());
        
        compareResponse = UtilIT.summaryDatasetVersionDifferences(datasetPersistentId, apiToken);
        compareResponse.prettyPrint(); 
        
        compareResponse.then().assertThat()
                .body("data[1].versionNumber", equalTo("1.0"))
                .body("data[1].summary.deaccessioned.reason", equalTo("Test deaccession reason."))
                .body("data[0].versionNumber", equalTo("DRAFT"))
                .body("data[0].summary.", equalTo("previousVersionDeaccessioned"))
                .statusCode(OK.getStatusCode());
        
        
    }
    
    @Test
    public void testRequireFilesToPublishDatasets() {
        // Create superuser and regular user
        Response createUserResponse = UtilIT.createRandomUser();
        createUserResponse.then().assertThat().statusCode(OK.getStatusCode());
        String usernameAdmin = UtilIT.getUsernameFromResponse(createUserResponse);
        String apiTokenAdmin = UtilIT.getApiTokenFromResponse(createUserResponse);
        Response makeSuperUser = UtilIT.makeSuperUser(usernameAdmin);
        assertEquals(200, makeSuperUser.getStatusCode());

        createUserResponse = UtilIT.createRandomUser();
        createUserResponse.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);

        // Create and publish a top level Dataverse (under root) with a requireFilesToPublishDataset set to true
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String ownerAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        // Only admin can set this attribute
        Response setDataverseAttributeResponse = UtilIT.setCollectionAttribute(ownerAlias, "requireFilesToPublishDataset", "true", apiToken);
        setDataverseAttributeResponse.prettyPrint();
        setDataverseAttributeResponse.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());
        setDataverseAttributeResponse = UtilIT.setCollectionAttribute(ownerAlias, "requireFilesToPublishDataset", "true", apiTokenAdmin);
        setDataverseAttributeResponse.prettyPrint();
        setDataverseAttributeResponse.then().assertThat().statusCode(OK.getStatusCode());
        setDataverseAttributeResponse.then().assertThat().body("data.effectiveRequiresFilesToPublishDataset", equalTo(true));
        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(ownerAlias, apiTokenAdmin);
        publishDataverseResponse.prettyPrint();
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Create and publish a new Dataverse under the above Dataverse with requireFilesToPublishDataset not set (default null)
        String alias = "dv2-" + UtilIT.getRandomIdentifier();
        createDataverseResponse = UtilIT.createSubDataverse(alias, null, apiToken, ownerAlias);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(alias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Create a Dataset under the 2nd level Dataverse
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(alias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer id = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        // Try to publish with no files (minimum is 1 file from the top level Dataverse)
        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(String.valueOf(id), "major", apiToken);
        publishDatasetResponse.prettyPrint();
        publishDatasetResponse.then().assertThat().statusCode(FORBIDDEN.getStatusCode());
        publishDatasetResponse.then().assertThat().body("message", containsString(
                BundleUtil.getStringFromBundle("dataset.mayNotPublish.FilesRequired")
        ));

        // Upload 1 file and try to publish again
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(String.valueOf(id), pathToFile, apiToken);
        uploadResponse.then().assertThat().statusCode(OK.getStatusCode());

        publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(String.valueOf(id), "major", apiToken);
        publishDatasetResponse.prettyPrint();
        publishDatasetResponse.then().assertThat().statusCode(OK.getStatusCode());
    }
    
    @Test
    public void testDeleteFiles() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        // Add files to the dataset
        String pathToFile1 = "scripts/api/data/licenses/licenseCC0-1.0.json";
        String pathToFile2 = "scripts/api/data/licenses/licenseCC-BY-4.0.json";
        String pathToFile3 = "scripts/api/data/licenses/licenseCC-BY-NC-4.0.json";
        String pathToFile4 = "scripts/api/data/licenses/licenseCC-BY-NC-ND-4.0.json";
        String pathToFile5 = "scripts/api/data/licenses/licenseCC-BY-ND-4.0.json";

        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("description", "File 1");
        Response addFile1Response = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile1, json.build(), apiToken);
        Long file1Id = JsonPath.from(addFile1Response.body().asString()).getLong("data.files[0].dataFile.id");

        json.add("description", "File 2");
        Response addFile2Response = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile2, json.build(), apiToken);
        Long file2Id = JsonPath.from(addFile2Response.body().asString()).getLong("data.files[0].dataFile.id");

        json.add("description", "File 3");
        Response addFile3Response = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile3, json.build(), apiToken);
        Long file3Id = JsonPath.from(addFile3Response.body().asString()).getLong("data.files[0].dataFile.id");

        json.add("description", "File 4");
        Response addFile4Response = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile4, json.build(), apiToken);
        Long file4Id = JsonPath.from(addFile4Response.body().asString()).getLong("data.files[0].dataFile.id");

        json.add("description", "File 5");
        Response addFile5Response = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile5, json.build(), apiToken);
        Long file5Id = JsonPath.from(addFile5Response.body().asString()).getLong("data.files[0].dataFile.id");

        // Delete files 1 and 2
        JsonArrayBuilder fileIdsToDelete = Json.createArrayBuilder();
        fileIdsToDelete.add(file1Id);
        fileIdsToDelete.add(file2Id);

        Response deleteFilesResponse = UtilIT.deleteDatasetFiles(datasetId.toString(), fileIdsToDelete.build(), apiToken);
        deleteFilesResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", startsWith("2"));

        // Verify files were deleted
        Response getDatasetResponse = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.latestVersion.files.findAll { it.dataFile.id == " + file1Id + " }.size()", equalTo(0))
                .body("data.latestVersion.files.findAll { it.dataFile.id == " + file2Id + " }.size()", equalTo(0))
                .body("data.latestVersion.files.findAll { it.dataFile.id == " + file3Id + " }.size()", equalTo(1))
                .body("data.latestVersion.files.findAll { it.dataFile.id == " + file4Id + " }.size()", equalTo(1))
                .body("data.latestVersion.files.findAll { it.dataFile.id == " + file5Id + " }.size()", equalTo(1));

        
        // Test deleting after dataset publication
        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Publish the dataset
        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Delete files 3 and 4 from the published dataset
        fileIdsToDelete = Json.createArrayBuilder();
        fileIdsToDelete.add(file3Id);
        fileIdsToDelete.add(file4Id);

        deleteFilesResponse = UtilIT.deleteDatasetFiles(datasetId.toString(), fileIdsToDelete.build(), apiToken);
        deleteFilesResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", startsWith("2"));

        // Verify files were deleted
        getDatasetResponse = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.latestVersion.files.findAll { it.dataFile.id == " + file3Id + " }.size()", equalTo(0))
                .body("data.latestVersion.files.findAll { it.dataFile.id == " + file4Id + " }.size()", equalTo(0))
                .body("data.latestVersion.files.findAll { it.dataFile.id == " + file5Id + " }.size()", equalTo(1));

        // Test error conditions

        // Try to delete a non-existent file
        fileIdsToDelete = Json.createArrayBuilder();
        fileIdsToDelete.add(999999L);

        deleteFilesResponse = UtilIT.deleteDatasetFiles(datasetId.toString(), fileIdsToDelete.build(), apiToken);
        deleteFilesResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", containsString("No files"));

        // Try to delete files from a non-existent dataset
        deleteFilesResponse = UtilIT.deleteDatasetFiles("999999", fileIdsToDelete.build(), apiToken);
        deleteFilesResponse.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());

        // Try to delete files without proper permissions
        // Create a second user
        Response createSecondUser = UtilIT.createRandomUser();
        String unauthorizedUsername = UtilIT.getUsernameFromResponse(createSecondUser);
        String unauthorizedUserApiToken = UtilIT.getApiTokenFromResponse(createSecondUser);

        //Reset to a valid file id
        fileIdsToDelete = Json.createArrayBuilder();
        fileIdsToDelete.add(file5Id);
        deleteFilesResponse = UtilIT.deleteDatasetFiles(datasetId.toString(), fileIdsToDelete.build(), unauthorizedUserApiToken);
        deleteFilesResponse.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());

        // Make the user a superuser to destroy dataset
        Response makeSuperUserResponse = UtilIT.setSuperuserStatus(username, true);
        makeSuperUserResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        // Clean up
        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetId, apiToken);
        assertEquals(200, destroyDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUnauthorizedUserResponse = UtilIT.deleteUser(unauthorizedUsername);
        assertEquals(200, deleteUnauthorizedUserResponse.getStatusCode());
        
        Response deleteUserResponse = UtilIT.deleteUser(username);
        assertEquals(200, deleteUserResponse.getStatusCode());
    }
    
    @Test
    public void testUpdateMultipleFileMetadata() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        // Create and publish a top level Dataverse (under root)
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String ownerAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        
        // Create a dataset with multiple files
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(ownerAlias, apiToken);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);

        String pathToFile1 = "scripts/api/data/licenses/licenseCC0-1.0.json";
        String pathToFile2 = "scripts/api/data/licenses/licenseCC-BY-4.0.json";
        String pathToFile3 = "scripts/search/ds.tsv";

        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("description", "File 1");
        Response addFile1Response = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile1, json.build(), apiToken);
        Integer file1Id = UtilIT.getDataFileIdFromResponse(addFile1Response);

        json = Json.createObjectBuilder();
        json.add("description", "File 2");
        Response addFile2Response = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile2, json.build(), apiToken);
        Integer file2Id = UtilIT.getDataFileIdFromResponse(addFile2Response);

        json = Json.createObjectBuilder();
        json.add("description", "File 3");
        Response addFile3Response = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile3, json.build(), apiToken);
        Integer file3Id = UtilIT.getDataFileIdFromResponse(addFile3Response);

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration for " + pathToFile3);
        
        // Prepare JSON for updating file metadata
        JsonArrayBuilder filesArrayBuilder = Json.createArrayBuilder();
        filesArrayBuilder.add(Json.createObjectBuilder()
                .add("dataFileId", file1Id)
                .add("label", "Updated File 1")
                .add("directoryLabel", "dir1/")
                .add("description", "Updated description for File 1")
                .add("categories", Json.createArrayBuilder().add("Category 1").add("Category 2"))
                .add("provFreeForm", "Updated provenance for File 1")
                .add("restrict", true));

        filesArrayBuilder.add(Json.createObjectBuilder()
                .add("dataFileId", file2Id)
                .add("label", "Updated File 2")
                .add("directoryLabel", "dir2/")
                .add("description", "Updated description for File 2")
                .add("categories", Json.createArrayBuilder().add("Category 3"))
                .add("provFreeForm", "Updated provenance for File 2"));

        // Test updating file metadata
        Response updateResponse = UtilIT.updateDatasetFilesMetadata(datasetId.toString(), filesArrayBuilder.build(), apiToken);
        updateResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Verify the changes
        Response getDatasetResponse = UtilIT.getDatasetVersion(datasetPersistentId, ":draft", apiToken);
        getDatasetResponse.then().assertThat()
        .statusCode(OK.getStatusCode());
        JsonObject data = getDataAsJsonObject(getDatasetResponse.getBody().asString());
        JsonArray files = data.getJsonArray("files");
        JsonUtil.prettyPrint(files);
        for (JsonValue fileValue : files) {
            JsonObject file = (JsonObject) fileValue;
            JsonObject dataFile = file.getJsonObject("dataFile");
            if (dataFile.getInt("id") == file1Id) {
                assertEquals("Updated File 1", file.getString("label"));
                assertEquals("dir1", file.getString("directoryLabel"));
                assertEquals("Updated description for File 1", dataFile.getString("description"));
                assertTrue(dataFile.getJsonArray("categories").contains(Json.createValue("Category 1")));
                assertTrue(dataFile.getJsonArray("categories").contains(Json.createValue("Category 2")));
                assertTrue(file.getBoolean("restricted"));
                
                // Check provFreeForm for file1
                Response provResponse = UtilIT.getProvFreeForm(file1Id.toString(), apiToken);
                provResponse.then().assertThat()
                    .statusCode(OK.getStatusCode())
                    .body("data.text", equalTo("Updated provenance for File 1"));
            } else if (dataFile.getInt("id") == file2Id) {
                assertEquals("Updated File 2", file.getString("label"));
                assertEquals("dir2", file.getString("directoryLabel"));
                assertEquals("Updated description for File 2", dataFile.getString("description"));
                assertTrue(dataFile.getJsonArray("categories").contains(Json.createValue("Category 3")));
                
                // Check provFreeForm for file2
                Response provResponse = UtilIT.getProvFreeForm(file2Id.toString(), apiToken);
                provResponse.then().assertThat()
                    .statusCode(OK.getStatusCode())
                    .body("data.text", equalTo("Updated provenance for File 2"));
            }
        }

        // Test updating the same file with the same restrict value
        JsonArrayBuilder sameRestrictValueArrayBuilder = Json.createArrayBuilder();
        sameRestrictValueArrayBuilder.add(Json.createObjectBuilder()
                .add("dataFileId", file1Id)
                .add("restrict", true));

        Response sameRestrictUpdateResponse = UtilIT.updateDatasetFilesMetadata(datasetPersistentId, sameRestrictValueArrayBuilder.build(), apiToken);
        sameRestrictUpdateResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", containsString("is already restricted"));

        // Test updating a file not in the dataset
        JsonArrayBuilder invalidFilesArrayBuilder = Json.createArrayBuilder();
        invalidFilesArrayBuilder.add(Json.createObjectBuilder()
                .add("dataFileId", 999999)
                .add("label", "Invalid File"));

        Response invalidUpdateResponse = UtilIT.updateDatasetFilesMetadata(datasetId.toString(), invalidFilesArrayBuilder.build(), apiToken);
        invalidUpdateResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

        // Test updating after dataset publication
        Response publishDataverseResponse = UtilIT.publishDataverseViaNativeApi(ownerAlias, apiToken);
        publishDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());
        
        Response publishDatasetResponse = UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        publishDatasetResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        JsonArrayBuilder postPublishFilesArrayBuilder = Json.createArrayBuilder();
        postPublishFilesArrayBuilder.add(Json.createObjectBuilder()
                .add("dataFileId", file3Id)
                .add("label", "Updated File 3 After Publication")
                .add("description", "Updated description for File 3 after publication"));

        Response postPublishUpdateResponse = UtilIT.updateDatasetFilesMetadata(datasetId.toString(), postPublishFilesArrayBuilder.build(), apiToken);
        postPublishUpdateResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Verify the changes after publication
        Response getUpdatedDatasetResponse = UtilIT.getDatasetVersion(datasetPersistentId, ":latest", apiToken);
        JsonObject updatedData = getDataAsJsonObject(getUpdatedDatasetResponse.getBody().asString());
        JsonArray updatedFiles = updatedData.getJsonArray("files");

        for (JsonValue fileValue : updatedFiles) {
            JsonObject file = (JsonObject) fileValue;
            JsonObject dataFile = file.getJsonObject("dataFile");
            if (dataFile.getInt("id") == file3Id) {
                assertEquals("Updated File 3 After Publication", file.getString("label"));
                assertEquals("Updated description for File 3 after publication", dataFile.getString("description"));
            }
        }

     // Test adding dataFileTags to a non-tabular file (should fail)
        JsonArrayBuilder nonTabularTagsArrayBuilder = Json.createArrayBuilder();
        nonTabularTagsArrayBuilder.add(Json.createObjectBuilder()
                .add("dataFileId", file1Id)
                .add("dataFileTags", Json.createArrayBuilder().add("Survey")));

        Response nonTabularTagsResponse = UtilIT.updateDatasetFilesMetadata(datasetId.toString(), nonTabularTagsArrayBuilder.build(), apiToken);
        nonTabularTagsResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", containsString(BundleUtil.getStringFromBundle("file.metadata.datafiletag.not_tabular")));

        // Test adding valid dataFileTags to a tabular file (file3 is ds.tsv, which is tabular)
        JsonArrayBuilder validTagsArrayBuilder = Json.createArrayBuilder();
        validTagsArrayBuilder.add(Json.createObjectBuilder()
                .add("dataFileId", file3Id)
                .add("dataFileTags", Json.createArrayBuilder().add(DataFileTag.TagType.Survey.toString()).add(DataFileTag.TagType.Survey.toString())));

        Response validTagsResponse = UtilIT.updateDatasetFilesMetadata(datasetId.toString(), validTagsArrayBuilder.build(), apiToken);
        validTagsResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Verify the valid tags were added
        Response getUpdatedFile3Response = UtilIT.getDatasetVersion(datasetPersistentId, ":latest", apiToken);
        JsonObject updatedFile3Data = getDataAsJsonObject(getUpdatedFile3Response.getBody().asString());
        JsonArray updatedFile3Files = updatedFile3Data.getJsonArray("files");

        boolean foundValidTags = false;
        for (JsonValue fileValue : updatedFile3Files) {
            JsonObject file = (JsonObject) fileValue;
            JsonObject dataFile = file.getJsonObject("dataFile");
            if (dataFile.getInt("id") == file3Id) {
                JsonArray tabularTags = dataFile.getJsonArray("tabularTags");
                if (tabularTags != null && tabularTags.contains(Json.createValue(DataFileTag.TagType.Survey.toString())) && tabularTags.contains(Json.createValue(DataFileTag.TagType.Survey.toString()))) {
                    foundValidTags = true;
                    break;
                }
            }
        }
        assertTrue(foundValidTags);

        // Test adding an invalid dataFileTag to a tabular file
        JsonArrayBuilder invalidTagsArrayBuilder = Json.createArrayBuilder();
        invalidTagsArrayBuilder.add(Json.createObjectBuilder()
                .add("dataFileId", file3Id)
                .add("dataFileTags", Json.createArrayBuilder().add("InvalidTag")));

        Response invalidTagsResponse = UtilIT.updateDatasetFilesMetadata(datasetId.toString(), invalidTagsArrayBuilder.build(), apiToken);
        invalidTagsResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", containsString(BundleUtil.getStringFromBundle("file.addreplace.error.invalid_datafile_tag")));
        
        // Create a second user
        Response createSecondUser = UtilIT.createRandomUser();
        String secondUsername = UtilIT.getUsernameFromResponse(createSecondUser);
        String secondApiToken = UtilIT.getApiTokenFromResponse(createSecondUser);

        // Attempt to update file metadata with the second user
        JsonArrayBuilder unauthorizedFilesArrayBuilder = Json.createArrayBuilder();
        unauthorizedFilesArrayBuilder.add(Json.createObjectBuilder()
                .add("dataFileId", file3Id)
                .add("label", "Unauthorized Update")
                .add("description", "This update should not be allowed"));

        Response unauthorizedUpdateResponse = UtilIT.updateDatasetFilesMetadata(datasetId.toString(), unauthorizedFilesArrayBuilder.build(), secondApiToken);
        unauthorizedUpdateResponse.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("message", containsString("You do not have permission to edit this dataset"));

        // Make the user a superuser to destroy dataset
        Response makeSuperUserResponse = UtilIT.setSuperuserStatus(username, true);
        makeSuperUserResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        // Clean up
        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetId, apiToken);
        destroyDatasetResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        // Delete the dataverse
        Response deleteDataverseResponse = UtilIT.deleteDataverse(ownerAlias, apiToken);
        deleteDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        // Delete the second user
        Response deleteSecondUserResponse = UtilIT.deleteUser(secondUsername);
        deleteSecondUserResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
    }
}
