package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.dataset.DatasetType;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DatasetTypesIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response getSoftwareType = UtilIT.getDatasetType(DatasetType.DATASET_TYPE_SOFTWARE);
        getSoftwareType.prettyPrint();

        String typeFound = JsonPath.from(getSoftwareType.getBody().asString()).getString("data.name");
        System.out.println("type found: " + typeFound);
        if (DatasetType.DATASET_TYPE_SOFTWARE.equals(typeFound)) {
            return;
        }

        System.out.println("The \"software\" type wasn't found. Create it.");
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());

        String jsonIn = Json.createObjectBuilder().add("name", DatasetType.DATASET_TYPE_SOFTWARE).build().toString();

        Response typeAdded = UtilIT.addDatasetType(jsonIn, apiToken);
        typeAdded.prettyPrint();
        typeAdded.then().assertThat().statusCode(OK.getStatusCode());
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
        String jsonIn = Json.createObjectBuilder().add("name", randomName).build().toString();

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
    public void testSoftwareCodemeta() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(username, true).then().assertThat().statusCode(OK.getStatusCode());

        Response getCitationBlock = UtilIT.getMetadataBlock("citation");
        getCitationBlock.prettyPrint();
        getCitationBlock.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.associatedDatasetTypes[0]", CoreMatchers.nullValue());

        //Avoid all-numeric names (which are not allowed)
        String randomName = "A" + UUID.randomUUID().toString().substring(0, 8);
        String jsonIn = Json.createObjectBuilder().add("name", randomName).build().toString();

        System.out.println("adding type with name " + randomName);
        Response typeAdded = UtilIT.addDatasetType(jsonIn, apiToken);
        typeAdded.prettyPrint();
        typeAdded.then().assertThat().statusCode(OK.getStatusCode());

        
        Long typeId = JsonPath.from(typeAdded.getBody().asString()).getLong("data.id");

        System.out.println("id of type: " + typeId);
        Response getTypeById = UtilIT.getDatasetType(typeId.toString());
        getTypeById.prettyPrint();
        getTypeById.then().assertThat().statusCode(OK.getStatusCode());
        
//        if (true) return;
        
        String updateToTheseTypes = Json.createArrayBuilder()
                // FIXME: put this back
                .add(randomName)
                .build().toString();

//        UpdateMetadataBlockDatasetTypeAssociations
//        Response associateGeospatialWithDatasetType1 = UtilIT.updateMetadataBlockDatasetTypeAssociations("journal", updateToTheseTypes, apiToken);
        Response associateGeospatialWithDatasetType1 = UtilIT.updateMetadataBlockDatasetTypeAssociations("geospatial", updateToTheseTypes, apiToken);
        associateGeospatialWithDatasetType1.prettyPrint();
        associateGeospatialWithDatasetType1.then().assertThat().
                statusCode(OK.getStatusCode())
                .body("data.associatedDatasetTypes.after[0]", CoreMatchers.is(randomName));
        if (true) return;

//        Response getGeospatialBlock = UtilIT.getMetadataBlock("journal");
        Response getGeospatialBlock = UtilIT.getMetadataBlock("geospatial");
        getGeospatialBlock.prettyPrint();
        getGeospatialBlock.then().assertThat()
                .statusCode(OK.getStatusCode())
//                .body("data.associatedDatasetTypes.before[0]", CoreMatchers.is(randomName))
//                .body("data.associatedDatasetTypes", CoreMatchers.containsString(randomName));
                .body("data.associatedDatasetTypes[0]", CoreMatchers.is(randomName));
    }

    @Test
    public void testSetMetadataBlocks() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response setMetadataBlocksResponse = UtilIT.setMetadataBlocks(dataverseAlias, Json.createArrayBuilder().add("citation").add("astrophysics"), apiToken);
        setMetadataBlocksResponse.prettyPrint();
        setMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());

        Response listBlocks = UtilIT.listMetadataBlocks(dataverseAlias, false, false, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat().statusCode(OK.getStatusCode());

//        setMetadataBlocksResponse = UtilIT.setMetadataBlocks(dataverseAlias, Json.createArrayBuilder().add("citation"), apiToken);
        setMetadataBlocksResponse = UtilIT.setMetadataBlocks(dataverseAlias, Json.createArrayBuilder(), apiToken);
        setMetadataBlocksResponse.prettyPrint();
        setMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());

        listBlocks = UtilIT.listMetadataBlocks(dataverseAlias, false, false, apiToken);
        listBlocks.prettyPrint();
        listBlocks.then().assertThat().statusCode(OK.getStatusCode());
        if (true) return;

        
        
        
        
        
        
        String[] testInputLevelNames = {"geographicCoverage", "country", "city", "notesText"};
        boolean[] testRequiredInputLevels = {false, true, false, false};
        boolean[] testIncludedInputLevels = {false, true, true, false};
        Response updateDataverseInputLevelsResponse = UtilIT.updateDataverseInputLevels(dataverseAlias, testInputLevelNames, testRequiredInputLevels, testIncludedInputLevels, apiToken);
        updateDataverseInputLevelsResponse.prettyPrint();
        updateDataverseInputLevelsResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Dataverse not found
        Response listMetadataBlocksResponse = null;
//        listMetadataBlocksResponse = UtilIT.listMetadataBlocks("-1", false, false, apiToken);
//        listMetadataBlocksResponse.prettyPrint();
//        listMetadataBlocksResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());
//        if (true) return;

        // Existent dataverse and no optional params
        String[] expectedAllMetadataBlockDisplayNames = {"Astronomy and Astrophysics Metadata", "Citation Metadata", "Geospatial Metadata"};

        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(dataverseAlias, false, false, apiToken);
        listMetadataBlocksResponse.prettyPrint();
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fields", equalTo(null))
                .body("data[1].fields", equalTo(null))
                .body("data[2].fields", equalTo(null))
                .body("data.size()", equalTo(3));

        String actualMetadataBlockDisplayName1 = listMetadataBlocksResponse.then().extract().path("data[0].displayName");
        String actualMetadataBlockDisplayName2 = listMetadataBlocksResponse.then().extract().path("data[1].displayName");
        String actualMetadataBlockDisplayName3 = listMetadataBlocksResponse.then().extract().path("data[2].displayName");
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName2);
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName3);
        assertNotEquals(actualMetadataBlockDisplayName2, actualMetadataBlockDisplayName3);
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName1));
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName2));
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName3));
        
        if (true) return;

        // Existent dataverse and onlyDisplayedOnCreate=true
        String[] expectedOnlyDisplayedOnCreateMetadataBlockDisplayNames = {"Citation Metadata", "Geospatial Metadata"};

        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(dataverseAlias, true, false, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fields", equalTo(null))
                .body("data[1].fields", equalTo(null))
                .body("data.size()", equalTo(2));

        actualMetadataBlockDisplayName1 = listMetadataBlocksResponse.then().extract().path("data[0].displayName");
        actualMetadataBlockDisplayName2 = listMetadataBlocksResponse.then().extract().path("data[1].displayName");
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName2);
        assertThat(expectedOnlyDisplayedOnCreateMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName1));
        assertThat(expectedOnlyDisplayedOnCreateMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName2));

        // Existent dataverse and returnDatasetFieldTypes=true
        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(dataverseAlias, false, true, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fields", not(equalTo(null)))
                .body("data[1].fields", not(equalTo(null)))
                .body("data[2].fields", not(equalTo(null)))
                .body("data.size()", equalTo(3));

        actualMetadataBlockDisplayName1 = listMetadataBlocksResponse.then().extract().path("data[0].displayName");
        actualMetadataBlockDisplayName2 = listMetadataBlocksResponse.then().extract().path("data[1].displayName");
        actualMetadataBlockDisplayName3 = listMetadataBlocksResponse.then().extract().path("data[2].displayName");
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName2);
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName3);
        assertNotEquals(actualMetadataBlockDisplayName2, actualMetadataBlockDisplayName3);
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName1));
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName2));
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName3));

        // Check dataset fields for the updated input levels are retrieved
        int geospatialMetadataBlockIndex = actualMetadataBlockDisplayName1.equals("Geospatial Metadata") ? 0 : actualMetadataBlockDisplayName2.equals("Geospatial Metadata") ? 1 : 2;

        // Since the included property of notesText is set to false, we should retrieve the total number of fields minus one
        int citationMetadataBlockIndex = geospatialMetadataBlockIndex == 0 ? 1 : 0;
        listMetadataBlocksResponse.then().assertThat()
                .body(String.format("data[%d].fields.size()", citationMetadataBlockIndex), equalTo(79));

        // Since the included property of geographicCoverage is set to false, we should retrieve the total number of fields minus one
        listMetadataBlocksResponse.then().assertThat()
                .body(String.format("data[%d].fields.size()", geospatialMetadataBlockIndex), equalTo(10));

        String actualGeospatialMetadataField1 = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.geographicCoverage.name", geospatialMetadataBlockIndex));
        String actualGeospatialMetadataField2 = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.country.name", geospatialMetadataBlockIndex));
        String actualGeospatialMetadataField3 = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.city.name", geospatialMetadataBlockIndex));

        assertNull(actualGeospatialMetadataField1);
        assertNotNull(actualGeospatialMetadataField2);
        assertNotNull(actualGeospatialMetadataField3);

        // Existent dataverse and onlyDisplayedOnCreate=true and returnDatasetFieldTypes=true
        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(dataverseAlias, true, true, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fields", not(equalTo(null)))
                .body("data[1].fields", not(equalTo(null)))
                .body("data.size()", equalTo(2));

        actualMetadataBlockDisplayName1 = listMetadataBlocksResponse.then().extract().path("data[0].displayName");
        actualMetadataBlockDisplayName2 = listMetadataBlocksResponse.then().extract().path("data[1].displayName");
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName2);
        assertThat(expectedOnlyDisplayedOnCreateMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName1));
        assertThat(expectedOnlyDisplayedOnCreateMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName2));

        // Check dataset fields for the updated input levels are retrieved
        geospatialMetadataBlockIndex = actualMetadataBlockDisplayName2.equals("Geospatial Metadata") ? 1 : 0;

        listMetadataBlocksResponse.then().assertThat()
                .body(String.format("data[%d].fields.size()", geospatialMetadataBlockIndex), equalTo(1));

        actualGeospatialMetadataField1 = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.geographicCoverage.name", geospatialMetadataBlockIndex));
        actualGeospatialMetadataField2 = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.country.name", geospatialMetadataBlockIndex));
        actualGeospatialMetadataField3 = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.city.name", geospatialMetadataBlockIndex));

        assertNull(actualGeospatialMetadataField1);
        assertNotNull(actualGeospatialMetadataField2);
        assertNull(actualGeospatialMetadataField3);

        citationMetadataBlockIndex = geospatialMetadataBlockIndex == 0 ? 1 : 0;

        // notesText has displayOnCreate=true but has include=false, so should not be retrieved
        String notesTextCitationMetadataField = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.notesText.name", citationMetadataBlockIndex));
        assertNull(notesTextCitationMetadataField);

        // producerName is a conditionally required field, so should not be retrieved
        String producerNameCitationMetadataField = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.producerName.name", citationMetadataBlockIndex));
        assertNull(producerNameCitationMetadataField);

        // author is a required field, so should be retrieved
        String authorCitationMetadataField = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.author.name", citationMetadataBlockIndex));
        assertNotNull(authorCitationMetadataField);

        // User has no permissions on the requested dataverse
        Response createSecondUserResponse = UtilIT.createRandomUser();
        String secondApiToken = UtilIT.getApiTokenFromResponse(createSecondUserResponse);

        createDataverseResponse = UtilIT.createRandomDataverse(secondApiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String secondDataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(secondDataverseAlias, true, true, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // List metadata blocks from Root
        listMetadataBlocksResponse = UtilIT.listMetadataBlocks("root", true, true, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].displayName", equalTo("Citation Metadata"))
                .body("data[0].fields", not(equalTo(null)))
                .body("data.size()", equalTo(1));
    }
    
}
