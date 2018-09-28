package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
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
import edu.harvard.iq.dataverse.DataFile;
import static edu.harvard.iq.dataverse.api.UtilIT.API_TOKEN_HTTP_HEADER;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.UUID;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import org.apache.commons.lang.StringUtils;
import com.jayway.restassured.parsing.Parser;
import static com.jayway.restassured.path.json.JsonPath.with;
import com.jayway.restassured.path.xml.XmlPath;
import static edu.harvard.iq.dataverse.api.UtilIT.equalToCI;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

        Response removeDcmUrl = UtilIT.deleteSetting(SettingsServiceBean.Key.DataCaptureModuleUrl);
        removeDcmUrl.then().assertThat()
                .statusCode(200);

        Response removeUploadMethods = UtilIT.deleteSetting(SettingsServiceBean.Key.UploadMethods);
        removeUploadMethods.then().assertThat()
                .statusCode(200);
    }

    @AfterClass
    public static void afterClass() {

        Response removeIdentifierGenerationStyle = UtilIT.deleteSetting(SettingsServiceBean.Key.IdentifierGenerationStyle);
        removeIdentifierGenerationStyle.then().assertThat()
                .statusCode(200);

        Response removeExcludeEmail = UtilIT.deleteSetting(SettingsServiceBean.Key.ExcludeEmailFromExport);
        removeExcludeEmail.then().assertThat()
                .statusCode(200);

        Response removeDcmUrl = UtilIT.deleteSetting(SettingsServiceBean.Key.DataCaptureModuleUrl);
        removeDcmUrl.then().assertThat()
                .statusCode(200);

        Response removeUploadMethods = UtilIT.deleteSetting(SettingsServiceBean.Key.UploadMethods);
        removeUploadMethods.then().assertThat()
                .statusCode(200);
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
                .statusCode(OK.getStatusCode());
        

        String pathToJsonFileSingle = "doc/sphinx-guides/source/_static/api/dataset-simple-update-metadata.json";
        Response addSubjectSingleViaNative = UtilIT.updateFieldLevelDatasetMetadataViaNative(datasetPersistentId, pathToJsonFileSingle, apiToken);
        addSubjectSingleViaNative.prettyPrint();
        addSubjectSingleViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());


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
        
        //"Delete metadata failed: " + updateField.getDatasetFieldType().getDisplayName() + ": " + displayValue + " not found."
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
        attemptToPublishZeroDotOne.prettyPrint();
        attemptToPublishZeroDotOne.then().assertThat()
                .body("message", equalTo("Cannot publish as minor version. Re-try as major release."))
                .statusCode(403);

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
        System.out.println("datasetContactFromExport: " + datasetContactFromExport);

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
            assertEquals("Finch, Fiona", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.stdyInfo.contact"));
        } else {
            assertEquals("finch@mailinator.com", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.stdyInfo.contact.@email"));
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
        System.out.println("datasetContactFromExport: " + datasetContactFromExport);

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

        assertEquals("sammi@sample.com", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.stdyInfo.contact.@email"));
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

        assertEquals("Dataverse, Admin", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.stdyInfo.contact"));
        // no "sammi@sample.com" to be found https://github.com/IQSS/dataverse/issues/3443
        assertEquals("[]", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.stdyInfo.contact.@email"));
        assertEquals("Sample Datasets, inc.", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.stdyInfo.contact.@affiliation"));
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
    public void testSequentialNumberAsIdentifierGenerationStyle() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response setSequentialNumberAsIdentifierGenerationStyle = UtilIT.setSetting(SettingsServiceBean.Key.IdentifierGenerationStyle, "sequentialNumber");
        setSequentialNumberAsIdentifierGenerationStyle.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        Response datasetAsJson = UtilIT.nativeGet(datasetId, apiToken);
        datasetAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());

        String identifier = JsonPath.from(datasetAsJson.getBody().asString()).getString("data.identifier");
        System.out.println("identifier: " + identifier);
        String numericPart = identifier.replace("FK2/", ""); //remove shoulder from identifier
        assertTrue(StringUtils.isNumeric(numericPart));

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

        Response failToCreateWhenDatasetIdNotFound = UtilIT.privateUrlCreate(Integer.MAX_VALUE, apiToken);
        failToCreateWhenDatasetIdNotFound.prettyPrint();
        assertEquals(NOT_FOUND.getStatusCode(), failToCreateWhenDatasetIdNotFound.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        System.out.println("dataset id: " + datasetId);

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
        Response contributorDoesNotHavePermissionToCreatePrivateUrl = UtilIT.privateUrlCreate(datasetId, contributorApiToken);
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

        Response createPrivateUrl = UtilIT.privateUrlCreate(datasetId, apiToken);
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
        Response getDatasetAsUserWhoClicksPrivateUrl = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get(urlWithToken);
        String title = getDatasetAsUserWhoClicksPrivateUrl.getBody().htmlPath().getString("html.head.title");
        assertEquals("Darwin's Finches - " + dataverseAlias, title);
        assertEquals(OK.getStatusCode(), getDatasetAsUserWhoClicksPrivateUrl.getStatusCode());

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
        assertEquals(FORBIDDEN.getStatusCode(), downloadFileBadToken.getStatusCode());
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

        Response createPrivateUrlUnauth = UtilIT.privateUrlCreate(datasetId, userWithNoRolesApiToken);
        createPrivateUrlUnauth.prettyPrint();
        assertEquals(UNAUTHORIZED.getStatusCode(), createPrivateUrlUnauth.getStatusCode());

        Response createPrivateUrlAgain = UtilIT.privateUrlCreate(datasetId, apiToken);
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

        Response createPrivateUrlOnceAgain = UtilIT.privateUrlCreate(datasetId, apiToken);
        createPrivateUrlOnceAgain.prettyPrint();
        assertEquals(OK.getStatusCode(), createPrivateUrlOnceAgain.getStatusCode());

        Response tryToCreatePrivateUrlWhenExisting = UtilIT.privateUrlCreate(datasetId, apiToken);
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

        Response tryToCreatePrivateUrlToPublishedVersion = UtilIT.privateUrlCreate(datasetId, apiToken);
        tryToCreatePrivateUrlToPublishedVersion.prettyPrint();
        assertEquals(FORBIDDEN.getStatusCode(), tryToCreatePrivateUrlToPublishedVersion.getStatusCode());

        String newTitle = "I am changing the title";
        Response updatedMetadataResponse = UtilIT.updateDatasetTitleViaSword(dataset1PersistentId, newTitle, apiToken);
        updatedMetadataResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), updatedMetadataResponse.getStatusCode());

        Response createPrivateUrlForPostVersionOneDraft = UtilIT.privateUrlCreate(datasetId, apiToken);
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
        Response createPrivateUrlToMakeSureItIsDeletedWithDestructionOfDataset = UtilIT.privateUrlCreate(datasetId, apiToken);
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
        System.out.println("dataset id: " + datasetId);

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
                .contentType(ContentType.JSON)
                .body("message", equalTo("User :guest is not permitted to perform requested action."))
                .statusCode(UNAUTHORIZED.getStatusCode());

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
        String datasetPersistentId = protocol + ":" + authority + "/" + identifier;

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

        String uploadFolder = identifier;

        /**
         * The "extra testing" involves having this REST Assured test do two
         * jobs done by the rsync script and the DCM. The rsync script creates
         * "files.sha" and (if checksum validation succeeds) the DCM moves the
         * files and the "files.sha" file into the uploadFolder.
         */
        boolean doExtraTesting = false;

        if (doExtraTesting) {

            String SEP = java.io.File.separator;
            // Set this to where you keep your files in dev. It might be nice to have an API to query to get this location from Dataverse.
            String dsDir = "/Users/pdurbin/dataverse/files/10.5072/FK2";
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(dsDir + SEP + identifier));
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(dsDir + SEP + identifier + SEP + uploadFolder));
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
                    .body("data.message", equalTo("FileSystemImportJob in progress"))
                    .statusCode(200);

            if (doExtraTesting) {

                long millisecondsNeededForFileSystemImportJobToFinish = 1000;
                Thread.sleep(millisecondsNeededForFileSystemImportJobToFinish);
                Response datasetAsJson2 = UtilIT.nativeGet(datasetId, apiToken);
                datasetAsJson2.prettyPrint();
                datasetAsJson2.then().assertThat()
                        .body("data.latestVersion.files[0].dataFile.filename", equalTo(identifier))
                        .body("data.latestVersion.files[0].dataFile.contentType", equalTo("application/vnd.dataverse.file-package"))
                        .body("data.latestVersion.files[0].dataFile.filesize", equalTo(totalSize))
                        .body("data.latestVersion.files[0].dataFile.checksum.type", equalTo("SHA-1"))
                        .statusCode(OK.getStatusCode());
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
    
}
