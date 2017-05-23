package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
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
import edu.harvard.iq.dataverse.DataFile;
import static edu.harvard.iq.dataverse.api.UtilIT.API_TOKEN_HTTP_HEADER;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.UUID;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import org.apache.commons.lang.StringUtils;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.Assert;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.parsing.Parser;
import static com.jayway.restassured.path.json.JsonPath.with;
import com.jayway.restassured.path.xml.XmlPath;
import java.util.ArrayList;
import java.util.HashMap;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertTrue;

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
        System.out.println("identifier: " + identifier);
        assertEquals(6, identifier.length());

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
            assertEquals("", XmlPath.from(exportDatasetAsDdi.body().asString()).getString("codeBook.stdyDscr.stdyInfo.contact"));
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

        Response setSequentialNumberAsIdentifierGenerationStyle = UtilIT.setSetting(SettingsServiceBean.Key.ExcludeEmailFromExport, "true");
        setSequentialNumberAsIdentifierGenerationStyle.then().assertThat()
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
        assertTrue(StringUtils.isNumeric(identifier));

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
        assertEquals("Darwin's Finches - " + dataverseAlias + " Dataverse", title);
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

}
