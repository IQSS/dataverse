package edu.harvard.iq.dataverse.api;

import static edu.harvard.iq.dataverse.api.ApiConstants.DS_VERSION_LATEST_PUBLISHED;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.util.StringUtil;
import static io.restassured.path.json.JsonPath.with;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.OK;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DatasetTypesIT {

    final static String INSTRUMENT = "instrument";

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());

        ensureDatasetTypeIsPresent(DatasetType.DATASET_TYPE_SOFTWARE, apiToken);
        ensureDatasetTypeIsPresent(DatasetType.DATASET_TYPE_REVIEW, apiToken);
        ensureDatasetTypeIsPresent(INSTRUMENT, apiToken);
    }

    @AfterAll
    public static void afterClass() {
        // Other tests make assertions about displayOnCreate so revert it back to how it was.
        UtilIT.setDisplayOnCreate("astroInstrument", false);
    }

    private static void ensureDatasetTypeIsPresent(String datasetType, String apiToken) {
        Response getDatasetType = UtilIT.getDatasetType(datasetType);
        getDatasetType.prettyPrint();
        String typeFound = JsonPath.from(getDatasetType.getBody().asString()).getString("data.name");
        System.out.println("type found: " + typeFound);
        if (datasetType.equals(typeFound)) {
            return;
        }
        System.out.println("The " + datasetType + "type wasn't found. Create it.");
        String displayName = capitalize(datasetType);
        String jsonIn = Json.createObjectBuilder()
                        .add("name", datasetType)
                        .add("displayName", displayName)
                        .build().toString();
        Response typeAdded = UtilIT.addDatasetType(jsonIn, apiToken);
        typeAdded.prettyPrint();
        typeAdded.then().assertThat().statusCode(OK.getStatusCode());
    }

    private static String capitalize(String stringIn) {
        return stringIn.substring(0, 1).toUpperCase() + stringIn.substring(1);
    }

    @Test
    public void testCreateSoftwareDatasetNative() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);

        String jsonIn = UtilIT.getDatasetJson("doc/sphinx-guides/source/_static/api/dataset-create-software.json");

        Response createSoftware = UtilIT.createDataset(dataverseAlias, jsonIn, apiToken);
        createSoftware.prettyPrint();

        createSoftware.then().assertThat().statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createSoftware);
        String datasetPid = JsonPath.from(createSoftware.getBody().asString()).getString("data.persistentId");

        Response getDatasetJson = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJson.prettyPrint();
        getDatasetJson.then().assertThat().statusCode(OK.getStatusCode());
        String datasetType = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.datasetType");
        System.out.println("datasetType: " + datasetType);
        assertEquals("software", datasetType);

        Response searchDraft = UtilIT.searchAndShowFacets("id:dataset_" + datasetId + "_draft", apiToken);
        searchDraft.prettyPrint();
        searchDraft.then().assertThat()
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                // No "Dataset Type" or count for "Software" because we hide the facet if there is only one type.
                .body("data.facets[0].datasetType.friendly", CoreMatchers.nullValue())
                .body("data.facets[0].datasetType.labels[0].Software", CoreMatchers.nullValue())
                .statusCode(OK.getStatusCode());

        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat().statusCode(CREATED.getStatusCode());

        String dataset2Pid = JsonPath.from(createDataset.getBody().asString()).getString("data.persistentId");

        UtilIT.publishDatasetViaNativeApi(dataset2Pid, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());
        //An explicit sleep is needed here because the searchAndShowFacets won't sleep for the query used here
        UtilIT.sleepForReindex(dataset2Pid, apiToken, 5);
        Response searchCollection = UtilIT.searchAndShowFacets("parentName:" + dataverseAlias, null);
        searchCollection.prettyPrint();
        searchCollection.then().assertThat()
                .body("data.total_count", CoreMatchers.is(2))
                .body("data.count_in_response", CoreMatchers.is(2))
                .body("data.facets[0].datasetType.friendly", CoreMatchers.is("Dataset Type"))
                .body("data.facets[0].datasetType.labels[0].Dataset", CoreMatchers.is(1))
                .body("data.facets[0].datasetType.labels[1].Software", CoreMatchers.is(1))
                .statusCode(OK.getStatusCode());

//        Response searchAsGuest = UtilIT.search(SearchFields.DATASET_TYPE + ":software", null);
//        searchAsGuest.prettyPrint();
//        searchAsGuest.then().assertThat()
//                .body("data.total_count", CoreMatchers.is(1))
//                .body("data.count_in_response", CoreMatchers.is(1))
//                .body("data.facets[0].datasetType.friendly", CoreMatchers.is("Dataset Type"))
//                .body("data.facets[0].datasetType.labels[0].software", CoreMatchers.is(1))
//                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testCreateDatasetSemantic() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);

        String jsonIn = UtilIT.getDatasetJson("doc/sphinx-guides/source/_static/api/dataset-create-software.jsonld");

        Response createSoftware = UtilIT.createDatasetSemantic(dataverseAlias, jsonIn, apiToken);
        createSoftware.prettyPrint();

        createSoftware.then().assertThat().statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createSoftware);
        String datasetPid = JsonPath.from(createSoftware.getBody().asString()).getString("data.persistentId");

        Response getDatasetJson = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJson.prettyPrint();
        getDatasetJson.then().assertThat().statusCode(OK.getStatusCode());
        String datasetType = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.datasetType");
        System.out.println("datasetType: " + datasetType);

        assertEquals("software", datasetType);

    }

    @Test
    public void testImportJson() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);

        String jsonIn = UtilIT.getDatasetJson("doc/sphinx-guides/source/_static/api/dataset-create-software.json");

        String randomString = UtilIT.getRandomString(6);

        Response importJson = UtilIT.importDatasetNativeJson(apiToken, dataverseAlias, jsonIn, "doi:10.5072/FK2/" + randomString, "no");
        importJson.prettyPrint();

        importJson.then().assertThat().statusCode(CREATED.getStatusCode());

        Integer datasetId = JsonPath.from(importJson.getBody().asString()).getInt("data.id");
        String datasetPid = JsonPath.from(importJson.getBody().asString()).getString("data.persistentId");

        Response getDatasetJson = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJson.prettyPrint();
        getDatasetJson.then().assertThat().statusCode(OK.getStatusCode());
        String datasetType = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.datasetType");
        System.out.println("datasetType: " + datasetType);
        assertEquals("software", datasetType);

    }

    @Test
    public void testGetDatasetTypes() {
        Response getTypes = UtilIT.getDatasetTypes();
        getTypes.prettyPrint();
        getTypes.then().assertThat()
                .statusCode(OK.getStatusCode())
                // non-null because types were added by a Flyway script
                .body("data", CoreMatchers.not(equalTo(null)));
    }

    @Test
    public void testGetDefaultDatasetType() {
        Response getType = UtilIT.getDatasetType(DatasetType.DEFAULT_DATASET_TYPE);
        getType.prettyPrint();
        getType.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.name", equalTo(DatasetType.DEFAULT_DATASET_TYPE));
    }

    @Test
    public void testDeleteDefaultDatasetType() {
        Response getType = UtilIT.getDatasetType(DatasetType.DEFAULT_DATASET_TYPE);
        getType.prettyPrint();
        getType.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.name", equalTo(DatasetType.DEFAULT_DATASET_TYPE));

        Long doomed = JsonPath.from(getType.getBody().asString()).getLong("data.id");

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());

        Response deleteType = UtilIT.deleteDatasetTypes(doomed, apiToken);
        deleteType.prettyPrint();
        deleteType.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    public void testAddAndDeleteDatasetType() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());

        Response badJson = UtilIT.addDatasetType("this isn't even JSON", apiToken);
        badJson.prettyPrint();
        badJson.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        String numbersOnlyIn = Json.createObjectBuilder().add("name", "12345").build().toString();
        Response numbersOnly = UtilIT.addDatasetType(numbersOnlyIn, apiToken);
        numbersOnly.prettyPrint();
        numbersOnly.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        //Avoid all-numeric names (which are not allowed)
        String randomName = "A" + UUID.randomUUID().toString().substring(0, 8);
        String displayName = capitalize(randomName);
        String jsonIn = Json.createObjectBuilder()
                        .add("name", randomName)
                        .add("displayName", displayName)
                        .build().toString();

        System.out.println("adding type with name " + randomName);
        Response typeAdded = UtilIT.addDatasetType(jsonIn, apiToken);
        typeAdded.prettyPrint();

        typeAdded.then().assertThat().statusCode(OK.getStatusCode());

        Long doomed = JsonPath.from(typeAdded.getBody().asString()).getLong("data.id");

        System.out.println("doomed: " + doomed);
        Response getTypeById = UtilIT.getDatasetType(doomed.toString());
        getTypeById.prettyPrint();
        getTypeById.then().assertThat().statusCode(OK.getStatusCode());

        System.out.println("deleting type with id " + doomed);
        Response typeDeleted = UtilIT.deleteDatasetTypes(doomed, apiToken);
        typeDeleted.prettyPrint();
        typeDeleted.then().assertThat().statusCode(OK.getStatusCode());

    }
    
    @Test
    public void testAddDatasetTypeWithMDBLicense(){
        
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("name", "testDatasetType");
        job.add("displayName", "testDatasetType");
        job.add("linkedMetadataBlocks", Json.createArrayBuilder().add("geospatial"));
        job.add("availableLicenses", Json.createArrayBuilder().add("CC0 1.0"));

        Response typeAdded = UtilIT.addDatasetType(job.build(), apiToken);
        typeAdded.prettyPrint();

        typeAdded.then().assertThat().statusCode(OK.getStatusCode());

        Long doomed = JsonPath.from(typeAdded.getBody().asString()).getLong("data.id");

        System.out.println("doomed: " + doomed);
        Response getTypeById = UtilIT.getDatasetType(doomed.toString());
        getTypeById.prettyPrint();
        getTypeById.then().assertThat().statusCode(OK.getStatusCode());

        System.out.println("deleting type with id " + doomed);
        Response typeDeleted = UtilIT.deleteDatasetTypes(doomed, apiToken);
        typeDeleted.prettyPrint();
        typeDeleted.then().assertThat().statusCode(OK.getStatusCode());
        
        //bad metadatablock name 
        job = Json.createObjectBuilder();
        job.add("name", "testDatasetType");
        job.add("linkedMetadataBlocks", Json.createArrayBuilder().add("geospatialXXX"));
        job.add("availableLicenses", Json.createArrayBuilder().add("CC0 1.0"));
        
        typeAdded = UtilIT.addDatasetType(job.build(), apiToken);
        typeAdded.prettyPrint();

        typeAdded.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", containsString("Metadata block not found:"));
        
        job = Json.createObjectBuilder();
        job.add("name", "testDatasetType");
        job.add("linkedMetadataBlocks", Json.createArrayBuilder().add("geospatial"));
        job.add("availableLicenses", Json.createArrayBuilder().add("CC0 12.0"));

        typeAdded = UtilIT.addDatasetType(job.build(), apiToken); 
        typeAdded.prettyPrint();
        typeAdded.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", containsString("License not found:"));

    }
    
    @Test
    public void testUpdateDatasetTypeWithLicense(){
        
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("name", "testDatasetType");
        job.add("displayName", "testDatasetType");

        Response typeAdded = UtilIT.addDatasetType(job.build(), apiToken);
        typeAdded.prettyPrint();

        typeAdded.then().assertThat().statusCode(OK.getStatusCode());
        
                String availableLicenseArray = """
            ["CC0 1.0"]
""";
        
        Response addCC0 = UtilIT.updateDatasetTypeAvailableLicense("testDatasetType", availableLicenseArray, apiToken);
        addCC0.prettyPrint();
        addCC0.then().assertThat().
                statusCode(OK.getStatusCode())
                .body("data.availableLicenses.after[0]", CoreMatchers.is("CC0 1.0"));
        
        
                        String badAvailableLicenseArray = """
            ["CC0 xx.0"]
""";

        addCC0 = UtilIT.updateDatasetTypeAvailableLicense("testDatasetType", badAvailableLicenseArray, apiToken);
        addCC0.prettyPrint();
        addCC0.then().assertThat().
                statusCode(BAD_REQUEST.getStatusCode())
                .body("message", containsString("License not found:"));
        
                        
        Long doomed = JsonPath.from(typeAdded.getBody().asString()).getLong("data.id");

        System.out.println("doomed: " + doomed);
        Response getTypeById = UtilIT.getDatasetType(doomed.toString());
        getTypeById.prettyPrint();
        getTypeById.then().assertThat().statusCode(OK.getStatusCode());

        System.out.println("deleting type with id " + doomed);
        Response typeDeleted = UtilIT.deleteDatasetTypes(doomed, apiToken);
        typeDeleted.prettyPrint();
        typeDeleted.then().assertThat().statusCode(OK.getStatusCode());

    }

    @Test
    public void testUpdateDatasetTypeLinksWithMetadataBlocks() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());

        System.out.println("listing root collection blocks with display on create: only citation");
        Response listBlocks = UtilIT.listMetadataBlocks(":root", true, false, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", nullValue());

        System.out.println("listing root collection blocks without display on create: only citation");
        listBlocks = UtilIT.listMetadataBlocks(":root", false, false, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", nullValue());

        //Avoid all-numeric names (which are not allowed)
        String randomName = "zzz" + UUID.randomUUID().toString().substring(0, 8);
        String displayName = capitalize(randomName);
        String jsonIn = Json.createObjectBuilder()
                        .add("name", randomName)
                        .add("displayName", displayName)
                        .build().toString();

        System.out.println("adding type with name " + randomName);
        Response typeAdded = UtilIT.addDatasetType(jsonIn, apiToken);
        typeAdded.prettyPrint();
        typeAdded.then().assertThat().statusCode(OK.getStatusCode());

        Long typeId = JsonPath.from(typeAdded.getBody().asString()).getLong("data.id");

        System.out.println("id of type: " + typeId);
        Response getTypeById = UtilIT.getDatasetType(typeId.toString());
        getTypeById.prettyPrint();
        getTypeById.then().assertThat().statusCode(OK.getStatusCode());

        String metadataBlockToLink = """
            ["geospatial"]
""";

        Response linkDatasetType1ToGeospatial = UtilIT.updateDatasetTypeLinksWithMetadataBlocks(randomName, metadataBlockToLink, apiToken);
        linkDatasetType1ToGeospatial.prettyPrint();
        linkDatasetType1ToGeospatial.then().assertThat().
                statusCode(OK.getStatusCode())
                .body("data.linkedMetadataBlocks.after[0]", CoreMatchers.is("geospatial"));

        getTypeById = UtilIT.getDatasetType(typeId.toString());
        getTypeById.prettyPrint();
        getTypeById.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.linkedMetadataBlocks[0]", CoreMatchers.is("geospatial"));

        System.out.println("listing root collection blocks with display on create");
        listBlocks = UtilIT.listMetadataBlocks(":root", true, false, randomName, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", is("geospatial"))
                .body("data[2].name", nullValue());

        System.out.println("listing root collection blocks without display on create");
        listBlocks = UtilIT.listMetadataBlocks(":root", false, false, randomName, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", is("geospatial"))
                .body("data[2].name", nullValue());

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);

        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());

        System.out.println("listing " + dataverseAlias + " collection blocks with display on create using dataset type " + randomName);
        listBlocks = UtilIT.listMetadataBlocks(dataverseAlias, true, false, randomName, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", is("geospatial"))
                .body("data[2].name", nullValue());

        System.out.println("listing " + dataverseAlias + " collection blocks without display on create using dataset type " + randomName);
        listBlocks = UtilIT.listMetadataBlocks(dataverseAlias, false, false, randomName, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", is("geospatial"))
                .body("data[2].name", nullValue());

        System.out.println("listing " + dataverseAlias + " collection blocks and inner dataset field types, without display on create and return dataset field types set to true using dataset type " + randomName);
        listBlocks = UtilIT.listMetadataBlocks(dataverseAlias, false, true, randomName, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", is("geospatial"))
                .body("data[0].fields.size()", is(35))
                .body("data[1].fields.size()", is(3));

        System.out.println("listing " + dataverseAlias + " collection blocks and inner dataset field types, with display on create and return dataset field types set to true using dataset type " + randomName);
        listBlocks = UtilIT.listMetadataBlocks(dataverseAlias, true, true, randomName, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", is("geospatial"))
                .body("data[0].fields.size()", is(10))
                .body("data[1].fields.size()", is(0)); // There are no fields required or with displayOnCreate=true in geospatial.tsv

        // We send an empty array to mean "delete or clear all"
        String emptyJsonArray = "[]";
        Response removeDatasetTypeLinks = UtilIT.updateDatasetTypeLinksWithMetadataBlocks(randomName, emptyJsonArray, apiToken);
        removeDatasetTypeLinks.prettyPrint();
        removeDatasetTypeLinks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.linkedMetadataBlocks.after[0]", CoreMatchers.nullValue());

        listBlocks = UtilIT.listMetadataBlocks(dataverseAlias, true, false, randomName, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .body("data[0].name", is("citation"));
        
        //Cleanup objects 

        
        Long doomed = JsonPath.from(typeAdded.getBody().asString()).getLong("data.id");

        System.out.println("doomed: " + doomed);

        getTypeById.prettyPrint();
        getTypeById.then().assertThat().statusCode(OK.getStatusCode());

        System.out.println("deleting type with id " + doomed);
        Response typeDeleted = UtilIT.deleteDatasetTypes(doomed, apiToken);
        typeDeleted.prettyPrint();
        typeDeleted.then().assertThat().statusCode(OK.getStatusCode());
        
        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
        
        
    }

    @Test
    public void testLinkInstrumentToAstro() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());

        String metadataBlockLink = """
            ["astrophysics"]
//""";

        String datasetType = "instrument";
        Response linkInstrumentToAstro = UtilIT.updateDatasetTypeLinksWithMetadataBlocks(datasetType, metadataBlockLink, apiToken);
        linkInstrumentToAstro.prettyPrint();
        linkInstrumentToAstro.then().assertThat().
                statusCode(OK.getStatusCode())
                .body("data.linkedMetadataBlocks.after[0]", CoreMatchers.is("astrophysics"));

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);

        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());

        // displayOnCreate will only be true for fields that are set this way in the database.
        // We set it here so we can make assertions below.
        UtilIT.setDisplayOnCreate("astroInstrument", true);

        Response listBlocks = null;
        System.out.println("listing root collection blocks with display on create using dataset type " + datasetType);
        listBlocks = UtilIT.listMetadataBlocks(":root", true, true, datasetType, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", is("astrophysics"))
                .body("data[2].name", nullValue())
                .body("data[0].fields.title.displayOnCreate", equalTo(true))
                .body("data[1].fields.astroInstrument.displayOnCreate", equalTo(true));

        System.out.println("listing root collection blocks with all fields (not display on create) using dataset type " + datasetType);
        listBlocks = UtilIT.listMetadataBlocks(":root", false, true, datasetType, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", is("astrophysics"))
                .body("data[2].name", nullValue())
                .body("data[0].fields.title.displayOnCreate", equalTo(true))
                .body("data[0].fields.subtitle.displayOnCreate", equalTo(false))
                .body("data[1].fields.astroInstrument.displayOnCreate", equalTo(true))
                .body("data[1].fields.astroObject.displayOnCreate", equalTo(false));

        System.out.println("listing " + dataverseAlias + " collection blocks with display on create using dataset type " + datasetType);
        listBlocks = UtilIT.listMetadataBlocks(dataverseAlias, true, true, datasetType, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", is("astrophysics"))
                .body("data[2].name", nullValue())
                .body("data[0].fields.title.displayOnCreate", equalTo(true))
                // subtitle is hidden because it is not "display on create"
                .body("data[0].fields.subtitle", nullValue())
                .body("data[1].fields.astroInstrument.displayOnCreate", equalTo(true))
                // astroObject is hidden because it is not "display on create"
                .body("data[1].fields.astroObject", nullValue());

        System.out.println("listing " + dataverseAlias + " collection blocks with all fields (not display on create) using dataset type " + datasetType);
        listBlocks = UtilIT.listMetadataBlocks(dataverseAlias, false, true, datasetType, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", is("citation"))
                .body("data[1].name", is("astrophysics"))
                .body("data[2].name", nullValue())
                .body("data[0].fields.title.displayOnCreate", equalTo(true))
                .body("data[0].fields.subtitle.displayOnCreate", equalTo(false))
                .body("data[1].fields.astroInstrument.displayOnCreate", equalTo(true))
                .body("data[1].fields.astroObject.displayOnCreate", equalTo(false));

    }
    
    @Test
    public void testCreateDatasetWithCustomType() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);
       
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());
   
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("name", "testDatasetType");
        job.add("displayName", "testDatasetType");
        job.add("linkedMetadataBlocks", Json.createArrayBuilder().add("geospatial"));
        job.add("availableLicenses", Json.createArrayBuilder().add("CC0 1.0"));
        
        Response typeAdded = UtilIT.addDatasetType(job.build(), apiToken);
        typeAdded.prettyPrint();

        typeAdded.then().assertThat().statusCode(OK.getStatusCode());
        Response getTypes = UtilIT.getDatasetTypes();   
        getTypes = UtilIT.getDatasetTypes();
        getTypes.prettyPrint();

        String pathToJsonFile = "scripts/api/data/dataset-create-new-with-type.json";
        
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        
        createDatasetResponse.prettyPrint();
        

        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        String datasetPid = JsonPath.from(createDatasetResponse.getBody().asString()).getString("data.persistentId");

        Response getDatasetJson = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetJson.prettyPrint();
        getDatasetJson.then().assertThat().statusCode(OK.getStatusCode());
        String datasetType = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.datasetType");
        System.out.println("datasetType: " + datasetType);
        assertEquals("testDatasetType", datasetType);
        String datasetVerstionState = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.latestVersion.versionState");
        assertEquals("DRAFT", datasetVerstionState);
        String datasetLicense = JsonPath.from(getDatasetJson.getBody().asString()).getString("data.latestVersion.license");
        //License is null because the one in the dataset json does not correspond to the 
        // available licenses in the dataset type.
        assertNull(datasetLicense);
        Response deleteDatasetResponse = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDatasetResponse.prettyPrint();
        assertEquals(200, deleteDatasetResponse.getStatusCode());
        
        Long doomed = JsonPath.from(typeAdded.getBody().asString()).getLong("data.id");

        System.out.println("doomed: " + doomed);
        Response getTypeById = UtilIT.getDatasetType(doomed.toString());
        getTypeById.prettyPrint();
        getTypeById.then().assertThat().statusCode(OK.getStatusCode());

        System.out.println("deleting type with id " + doomed);
        Response typeDeleted = UtilIT.deleteDatasetTypes(doomed, apiToken);
        typeDeleted.prettyPrint();
        typeDeleted.then().assertThat().statusCode(OK.getStatusCode());
        
        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());

    }

    /**
     * In this test, there are two users: one who publishes a dataset and
     * another who publishes a review of that dataset.
     */
    @Test
    public void testCreateReview() {
        Response createDatasetDepositor = UtilIT.createRandomUser();
        createDatasetDepositor.then().assertThat().statusCode(OK.getStatusCode());
        String apiTokenDepositor = UtilIT.getApiTokenFromResponse(createDatasetDepositor);

        Response createCollectionOfData = UtilIT.createRandomDataverse(apiTokenDepositor);
        createCollectionOfData.then().assertThat().statusCode(CREATED.getStatusCode());
        String collectionOfDataAlias = UtilIT.getAliasFromResponse(createCollectionOfData);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(collectionOfDataAlias, apiTokenDepositor);
        createDataset.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        String datasetPid = JsonPath.from(createDataset.getBody().asString()).getString("data.persistentId");

        UtilIT.publishDataverseViaNativeApi(collectionOfDataAlias, apiTokenDepositor).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiTokenDepositor);

        Response createReviewer = UtilIT.createRandomUser();
        createReviewer.then().assertThat().statusCode(OK.getStatusCode());
        String apiTokenReviewer = UtilIT.getApiTokenFromResponse(createReviewer);

        // We assume the reviewer wants their own collection for reviews.
        Response createCollectionOfReviews = UtilIT.createRandomDataverse(apiTokenReviewer);
        createCollectionOfReviews.then().assertThat().statusCode(CREATED.getStatusCode());
        String collectionOfReviewsAlias = UtilIT.getAliasFromResponse(createCollectionOfReviews);

        Response datasetMetadataResponse = UtilIT.nativeGet(datasetId, apiTokenReviewer);
        datasetMetadataResponse.then().assertThat().statusCode(OK.getStatusCode());
        datasetMetadataResponse.prettyPrint();
        JsonPath datasetMetadata = JsonPath.from(datasetMetadataResponse.body().asString());
        String datasetTitle = datasetMetadata.getString("data.latestVersion.metadataBlocks.citation.fields[0].value");
        String datasetPidUrl = datasetMetadata.getString("data.persistentUrl");
        String datasetPidProtocol = datasetMetadata.getString("data.protocol");
        String datasetPidAuthority = datasetMetadata.getString("data.authority");
        String datasetPidSeparator = datasetMetadata.getString("data.separator");
        String datasetPidIdentifier = datasetMetadata.getString("data.identifier");
        String datasetPidWithoutProtocol = datasetPidAuthority + datasetPidSeparator + datasetPidIdentifier;

        Response getCitation = UtilIT.getDatasetVersionCitation(datasetId, DS_VERSION_LATEST_PUBLISHED, false, apiTokenReviewer);
        getCitation.prettyPrint();
        getCitation.then().assertThat().statusCode(OK.getStatusCode());
        String datasetCitationHtml = JsonPath.from(getCitation.getBody().asString()).getString("data.message");
        String datasetCitationText = StringUtil.html2text(datasetCitationHtml);

        /**
         * We are added the HTML version of a Related Dataset. We like the HTML
         * version because both JSF and the SPA render the DOI link as a
         * clickable link.
         *
         * The tooltip for Related Dataset says "Information, such as a
         * persistent ID or citation, about a related dataset, such as previous
         * research on the Dataset's subject".
         *
         * We are aware that there is a custom metadata block called
         * "relatedDatasetsV2" at https://github.com/vera/related-datasets-cvoc
         * that we have been playing with. We especially like that relationships
         * can be expressed between the current object (a review) and the
         * related dataset. This is simlar to how "Related Publication" works.
         * See also discussion at
         * https://dataverse.zulipchat.com/#narrow/channel/379673-dev/topic/Improved.20.22Related.20datasets.22/near/534969036
         */
        JsonObjectBuilder jsonForCreatingReview = Json.createObjectBuilder()
                /**
                 * See above where this type is added to the installation and
                 * therefore available for use.
                 */
                .add("datasetType", DatasetType.DATASET_TYPE_REVIEW)
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
                                                        .add("value", "Review of " + datasetTitle)
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
                                                                                        .add("value", "This is a review of a dataset.")
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
                                                                .add(datasetCitationHtml)
                                                        )
                                                        .add("typeClass", "primitive")
                                                        .add("multiple", true)
                                                        .add("typeName", "relatedDatasets")
                                                )
                                        )
                                )
                        ));

        Response createReview = UtilIT.createDataset(collectionOfReviewsAlias, jsonForCreatingReview, apiTokenReviewer);
        createReview.prettyPrint();
        createReview.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer reviewId = UtilIT.getDatasetIdFromResponse(createReview);
        String reviewPid = JsonPath.from(createReview.getBody().asString()).getString("data.persistentId");

        Response getReviewMetadata = UtilIT.nativeGet(reviewId, apiTokenReviewer);
        getReviewMetadata.prettyPrint();
        getReviewMetadata.then().assertThat().statusCode(OK.getStatusCode());
        String datasetType = JsonPath.from(getReviewMetadata.getBody().asString()).getString("data.datasetType");
        assertEquals("review", datasetType);

        UtilIT.publishDataverseViaNativeApi(collectionOfReviewsAlias, apiTokenReviewer).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(reviewPid, "major", apiTokenReviewer).then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testInternationalization() {
        Response getDatasetType = UtilIT.getDatasetType("software");
        getDatasetType.prettyPrint();
        getDatasetType.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.name", is("software"))
                .body("data.displayName", is("Software"));

        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Accept-Language
        getDatasetType = UtilIT.getDatasetType("software", "en-US,en;q=0.5");
        getDatasetType.prettyPrint();
        getDatasetType.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.name", is("software"))
                .body("data.displayName", is("Software"));

        getDatasetType = UtilIT.getDatasetType("software", "en-US");
        getDatasetType.prettyPrint();
        getDatasetType.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.name", is("software"))
                .body("data.displayName", is("Software"));

        getDatasetType = UtilIT.getDatasetType("software", "");
        getDatasetType.prettyPrint();
        getDatasetType.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.name", is("software"))
                .body("data.displayName", is("Software"));

        boolean i18nIsConfigured = false;
        if (!i18nIsConfigured) {
            System.out.println("i18n is not configured; skipping test of non-English languages");
            return;
        }

        getDatasetType = UtilIT.getDatasetType("software", "fr-CA,fr;q=0.8,en-US;q=0.6,en;q=0.4");
        getDatasetType.prettyPrint();
        getDatasetType.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.name", is("software"))
                .body("data.displayName", is("Logiciel"));

        getDatasetType = UtilIT.getDatasetTypes("fr-CA,fr;q=0.8,en-US;q=0.6,en;q=0.4");
        getDatasetType.prettyPrint();
        getDatasetType.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Messy but the only way we've figured out ¯\_(ツ)_/¯
        List<Map<String, Object>> dataset = with(getDatasetType.body().asString()).param("dataset", "dataset")
                .getList("data.findAll { data -> data.name == dataset }");
        Map<String, Object> firstDataset = dataset.get(0);
        assertEquals("Ensemble de données", firstDataset.get("displayName"));

        List<Map<String, Object>> instrument = with(getDatasetType.body().asString()).param("instrument", "instrument")
                .getList("data.findAll { data -> data.name == instrument }");
        Map<String, Object> firstInstrument = instrument.get(0);
        // Instrument isn't translated in the French properties file; should fall back to English
        assertEquals("Instrument", firstInstrument.get("displayName"));
    }

}
