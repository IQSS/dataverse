package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;

import static com.jayway.restassured.RestAssured.given;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.junit.Ignore;
import com.jayway.restassured.path.json.JsonPath;

import java.util.List;
import java.util.Map;
import javax.json.JsonObject;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;

import edu.harvard.iq.dataverse.DataFile;

import static edu.harvard.iq.dataverse.api.UtilIT.API_TOKEN_HTTP_HEADER;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.jayway.restassured.parsing.Parser;

import static com.jayway.restassured.path.json.JsonPath.with;

import com.jayway.restassured.path.xml.XmlPath;

import static edu.harvard.iq.dataverse.api.UtilIT.equalToCI;

import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.datavariable.VariableMetadataDDIParser;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JSONLDUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static org.junit.Assert.assertEquals;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;

import org.junit.AfterClass;
import org.junit.Assert;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;


public class DatasetsIT {

    private static final Logger logger = Logger.getLogger(DatasetsIT.class.getCanonicalName());
    
    

    @BeforeClass
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
        
        /* With Dual mode, we can no longer mess with upload methods since native is now required for anything to work
               
        Response removeDcmUrl = UtilIT.deleteSetting(SettingsServiceBean.Key.DataCaptureModuleUrl);
        removeDcmUrl.then().assertThat()
                .statusCode(200);

        Response removeUploadMethods = UtilIT.deleteSetting(SettingsServiceBean.Key.UploadMethods);
        removeUploadMethods.then().assertThat()
                .statusCode(200);
         */
    }

    @AfterClass
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
        


        //Trying to blank out required field should fail...
        String pathToJsonFileBadData = "doc/sphinx-guides/source/_static/api/dataset-update-with-blank-metadata.json";
        Response deleteTitleViaNative = UtilIT.updateFieldLevelDatasetMetadataViaNative(datasetPersistentId, pathToJsonFileBadData, apiToken);
        deleteTitleViaNative.prettyPrint();
        deleteTitleViaNative.then().assertThat().body("message", equalTo("Error parsing dataset update: Empty value for field: Title "));


        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());
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

        Response getDatasetVersion = UtilIT.getDatasetVersion(datasetPersistentId, ":latest-published", apiToken);
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
                // FIXME: Get this working. See https://github.com/rest-assured/rest-assured/wiki/Usage#example-3---complex-parsing-and-validation
                //                .body("oai_dc:dc.find { it == 'dc:title' }.item", hasItems("Darwin's Finches"))
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
                // FIXME: Get this working. See https://github.com/rest-assured/rest-assured/wiki/Usage#example-3---complex-parsing-and-validation
                //                .body("oai_dc:dc.find { it == 'dc:title' }.item", hasItems("Darwin's Finches"))
                .statusCode(OK.getStatusCode());

        Response exportDatasetAsDdi = UtilIT.exportDataset(datasetPersistentId, "ddi", apiToken);
        exportDatasetAsDdi.prettyPrint();
        exportDatasetAsDdi.then().assertThat()
                .statusCode(OK.getStatusCode());

        // This is now returning [] instead of sammi@sample.com. Not sure why.
        // :ExcludeEmailFromExport is absent so the email should be shown.
        assertEquals("[]", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.stdyInfo.contact.@email"));
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
        assertEquals("[]", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.citation.distStmt.contact.@email"));
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
        assertEquals("Private URL Enabled", privateUrlUser.getDisplayInfo().getTitle());
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
        Response deleteDraftVersionAsContributor = UtilIT.deleteDatasetVersionViaNativeApi(datasetId, ":draft", contributorApiToken);
        deleteDraftVersionAsContributor.prettyPrint();
        deleteDraftVersionAsContributor.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Draft version of dataset " + datasetId + " deleted"));

        Response privateUrlRoleAssignmentShouldBeGoneAfterDraftDeleted = UtilIT.getRoleAssignmentsOnDataset(datasetId.toString(), null, apiToken);
        privateUrlRoleAssignmentShouldBeGoneAfterDraftDeleted.prettyPrint();
        assertEquals(false, privateUrlRoleAssignmentShouldBeGoneAfterDraftDeleted.body().asString().contains(privateUrlUser.getIdentifier()));

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
        
        // And now test deleting it:
        Response deleteLinkingDatasetResponse = UtilIT.deleteDatasetLink(datasetId.longValue(), dataverseAlias, apiToken);
        deleteLinkingDatasetResponse.prettyPrint();
        
        deleteLinkingDatasetResponse.then().assertThat()
                .body("data.message", equalTo("Link from Dataset " + datasetId + " to linked Dataverse " + dataverseAlias + " deleted"))
                .statusCode(200);
    }
    
    @Test
    @Ignore
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
        assertTrue("Lock missing from the output of /api/datasets/locks", lockListedCorrectly);        
        
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
        assertTrue("Lock missing from the output of /api/datasets/locks?type=Ingest", lockListedCorrectly);        

        
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
    @Ignore
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

        assertTrue("Failed test if Ingest Lock exceeds max duration " + pathToFile, UtilIT.sleepForLock(datasetId.longValue(), "Ingest", authorApiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION));

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

        assertTrue("Failed test if Ingest Lock exceeds max duration " + pathToFile, UtilIT.sleepForLock(datasetId.longValue(), "Ingest", authorApiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION));

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
        Assert.assertEquals("", dataDscrForGuest);

        // Author export (has access)
        Response exportByAuthor = UtilIT.exportDataset(datasetPid, "ddi", authorApiToken);
        exportByAuthor.prettyPrint();
        exportByAuthor.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("data.tab"));

        // Here we are asserting that dataDscr is empty. TODO: Do this in REST Assured.
        String dataDscrForAuthor = XmlPath.from(exportByAuthor.asString()).getString("codeBook.dataDscr");
        Assert.assertEquals("", dataDscrForAuthor);

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

        // Cleanup - delete dataset, dataverse, user...
        Response deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
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

    private String getData(String body) {
        try (StringReader rdr = new StringReader(body)) {
            return Json.createReader(rdr).readObject().getJsonObject("data").toString();
        }
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
        
        assertTrue("Failed test if Ingest Lock exceeds max duration " + pathToFileThatGoesThroughIngest , UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION));
        
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
        
        Response makeSuperUser = UtilIT.makeSuperUser(username);
                
        //should work after making super user
        
        UtilIT.publishDatasetViaNativeApi(datasetId, "updatecurrent", apiToken).then().assertThat().statusCode(OK.getStatusCode());
        
        Response getDatasetJsonAfterUpdate = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJsonAfterUpdate.prettyPrint();
        getDatasetJsonAfterUpdate.then().assertThat()
                .statusCode(OK.getStatusCode());
        
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

        assertTrue("Failed test if Ingest Lock exceeds max duration " + pathToFile, UtilIT.sleepForLock(datasetId.longValue(), "Ingest", authorApiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION));

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

        assertTrue("Failed test if Ingest Lock exceeds max duration " + pathToFile, UtilIT.sleepForLock(datasetId.longValue(), "Ingest", authorApiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION));

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
        nullStatus.then().assertThat().statusCode(NO_CONTENT.getStatusCode());

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
        nullStatus2.then().assertThat().statusCode(NO_CONTENT.getStatusCode());

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
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        int datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        Response getDatasetVersionCitationResponse = UtilIT.getDatasetVersionCitation(datasetId, ":draft", apiToken);
        getDatasetVersionCitationResponse.prettyPrint();

        getDatasetVersionCitationResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                // We check that the returned message contains information expected for the citation string
                .body("data.message", containsString("DRAFT VERSION"));
    }
}
