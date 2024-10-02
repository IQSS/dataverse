package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.dataset.DatasetType;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.OK;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

}
