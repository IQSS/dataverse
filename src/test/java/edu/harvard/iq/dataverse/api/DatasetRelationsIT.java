package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vera Clemens (ZB MED)
 */
public class DatasetRelationsIT {

    private static String apiTokenSuperuser;

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String usernameSuperuser = UtilIT.getUsernameFromResponse(createUser);
        apiTokenSuperuser = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(usernameSuperuser, true).then().assertThat().statusCode(OK.getStatusCode());

        // Ensure relation types exist
        String relationTypeJson = JsonUtil.createObjectBuilder()
                .add("name", "isRelatedTo")
                .add("displayName", "Is related to")
                .add("inverse", JsonUtil.createObjectBuilder().add("name", "isRelatedTo"))
                .build().toString();
        UtilIT.addDatasetRelationType(relationTypeJson, apiTokenSuperuser);
        
        String relationTypeJson2 = JsonUtil.createObjectBuilder()
                .add("name", "isSupplementTo")
                .add("displayName", "Is supplement to")
                .add("inverse", JsonUtil.createObjectBuilder().add("name", "isSupplementedBy").add("displayName", "Is supplemented by"))
                .build().toString();
        UtilIT.addDatasetRelationType(relationTypeJson2, apiTokenSuperuser);

        String relationTypeJson3 = JsonUtil.createObjectBuilder()
                .add("name", "isCitedBy")
                .add("displayName", "Is cited by")
                .add("inverse", JsonUtil.createObjectBuilder().add("name", "cites").add("displayName", "Cites"))
                .build().toString();
        UtilIT.addDatasetRelationType(relationTypeJson3, apiTokenSuperuser);

        String relationTypeWithoutInverseJson = JsonUtil.createObjectBuilder()
                .add("name", "isDerivedFromWithoutInverse")
                .add("displayName", "Is derived from without inverse")
                .build().toString();
        UtilIT.addDatasetRelationType(relationTypeWithoutInverseJson, apiTokenSuperuser);

        UtilIT.setDefaultDatasetRelationType("isRelatedTo", apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testListDatasetRelationsFilteringByRelationTypeAndVersion() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Dataset A - will have relations in multiple versions
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Dataset B
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // Dataset C
        Response createDatasetC = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidC = UtilIT.getDatasetPersistentIdFromResponse(createDatasetC);

        // Version 1.0 of Dataset A:
        // Relation 1: A -> B (isRelatedTo)
        JsonArray relationsV1 = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relationsV1.toString(), apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Version 2.0 of Dataset A:
        // Relation 1: A -> B (isRelatedTo) - kept from V1
        // Relation 2: A -> B (isSupplementTo)
        // Relation 2: A -> C (isSupplementTo)
        JsonArray relationsV2 = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isSupplementTo")))
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidC)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isSupplementTo")))
                .build();
        // This will create a draft for v2.0
        UtilIT.replaceDatasetRelations(pidA, relationsV2.toString(), apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        // Publish v2.0
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Current state:
        // V1.0: 1 relation (isRelatedTo)
        // v2.0: 3 relations (1x isRelatedTo, 2x isSupplementTo)

        // 1. Filter by Version AND Type (V1.0, isRelatedTo) -> Expect 1
        UtilIT.listDatasetRelations(pidA, "1.0", List.of("isRelatedTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1));

        // 2. Filter by Version AND Type (V1.0, isSupplementTo) -> Expect 0
        UtilIT.listDatasetRelations(pidA, "1.0", List.of("isSupplementTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0));

        // 3. Filter by Version AND Type (v2.0, isSupplementTo) -> Expect 2
        UtilIT.listDatasetRelations(pidA, "2.0", List.of("isSupplementTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2));

        // 4. Filter by Version only (V1.0) -> Expect 1
        UtilIT.listDatasetRelations(pidA, "1.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1));

        // 5. Filter by Version only (v2.0) -> Expect 3
        UtilIT.listDatasetRelations(pidA, "2.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(3));

        // 6. Filter by Type only (isSupplementTo) -> Expect 2 (from latest published v2.0)
        UtilIT.listDatasetRelations(pidA, null, List.of("isSupplementTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2));

        // 7. Filter by Type only (isRelatedTo) -> Expect 1 (from latest published v2.0)
        UtilIT.listDatasetRelations(pidA, null, List.of("isRelatedTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1));

        // 8. Filter by Types only (isRelatedTo, isSupplementTo) -> Expect 3 (from latest published v2.0)
        UtilIT.listDatasetRelations(pidA, null, Arrays.asList("isRelatedTo", "isSupplementTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(3));

        // A superuser can replace relations on a selected published version
        UtilIT.replaceDatasetRelations(pidA, "[]", "1.0", apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.listDatasetRelations(pidA, "1.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0));

        UtilIT.listDatasetRelations(pidA, "2.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(3));

        // 9. No filters (latest version) -> Expect 3
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(3));
    }

    @Test
    public void testListDatasetRelationsFilteringByDatasetType() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Ensure 'software' type exists in the system
        Response getDatasetType = UtilIT.getDatasetType("software");
        String typeFound = JsonPath.from(getDatasetType.getBody().asString()).getString("data.name");
        if (!("software".equals(typeFound))) {
            JsonObject softwareTypeJson = JsonUtil.createObjectBuilder()
                    .add("name", "software")
                    .add("displayName", "Software")
                    .add("description", "Software Dataset Type")
                    .build();
            UtilIT.addDatasetType(softwareTypeJson.toString(), apiTokenSuperuser);
        }

        // We need to ensure 'software' type is allowed in this collection
        Response setAllowed = UtilIT.setCollectionAttribute(dataverseAlias, "allowedDatasetTypes", "dataset,software", apiTokenSuperuser);
        setAllowed.then().assertThat().statusCode(OK.getStatusCode());

        // Dataset A (Source) - type 'dataset' (default)
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Dataset B (Target 1) - type 'dataset' (default)
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // Dataset C (Target 2) - type 'software'
        String softwareJson = UtilIT.getDatasetJson("doc/sphinx-guides/source/_static/api/dataset-create-software.json");
        Response createDatasetC = UtilIT.createDataset(dataverseAlias, softwareJson, apiTokenSuperuser);
        createDatasetC.then().assertThat().statusCode(201);
        String pidC = UtilIT.getDatasetPersistentIdFromResponse(createDatasetC);

        // Create relations: A -> B (isRelatedTo), A -> C (isRelatedTo), A -> External (isRelatedTo)
        JsonArray relations = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidC)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .add(JsonUtil.createObjectBuilder()
                        .add("externalIdentifier", "doi:10.1234/external")
                        .add("identifierScheme", "DOI")
                        .add("datasetType", "Software")
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo"))
                        .add("relationSource", "external"))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // No type filter -> Expect 3
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(3));

        // Filter by datasetType=dataset -> Expect 1 (Dataset B)
        UtilIT.listDatasetRelations(pidA, null, null, Arrays.asList("dataset"), null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items[0].relatedDatasetPid", equalTo(pidB));

        // Filter by datasetType=software -> Expect 1 (Dataset C)
        UtilIT.listDatasetRelations(pidA, null, null, Arrays.asList("software"), null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items[0].relatedDatasetPid", equalTo(pidC));

        // Filter by both -> Expect 2 (B and C)
        UtilIT.listDatasetRelations(pidA, null, null, Arrays.asList("dataset", "software"), null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2))
                .body("data.items.relatedDatasetPid", hasItems(pidB, pidC));

        // Filter by a non-existent type -> Expect 0
        UtilIT.listDatasetRelations(pidA, null, null, Arrays.asList("workflow"), null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0));

        // Expect correct dataset-type facets
        UtilIT.listDatasetRelations(pidA, null, null, null, null, null, null, apiTokenSuperuser, true)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.facets.datasetType", hasSize(2))
                .body("data.facets.datasetType[0].name", equalTo("dataset"))
                .body("data.facets.datasetType[0].count", equalTo(1))
                .body("data.facets.datasetType[1].name", equalTo("software"))
                .body("data.facets.datasetType[1].count", equalTo(1));

        // Expect correct relation-type facets (applying the active dataset-type filter)
        UtilIT.listDatasetRelations(pidA, null, null, List.of("software"), null, null, null, apiTokenSuperuser, true)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.facets.relationType", hasSize(1))
                .body("data.facets.relationType[0].name", equalTo("isRelatedTo"))
                .body("data.facets.relationType[0].count", equalTo(1));
    }

    @Test
    public void testListDatasetRelationsFilteringBySource() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Dataset A
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Dataset B
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // Create 1 internal and 1 external relation
        JsonArray relations = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .add(JsonUtil.createObjectBuilder()
                        .add("externalIdentifier", "https://example.org/1")
                        .add("identifierScheme", "URL")
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // No filter -> Expect 2
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2));

        // Filter by source=internal -> Expect 1
        UtilIT.listDatasetRelations(pidA, null, null, null, List.of("internal"), null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items[0].relatedDatasetPid", equalTo(pidB));

        // Filter by source=internal and type=dataset -> Expect 1
        UtilIT.listDatasetRelations(pidA, null, null, List.of("dataset"), List.of("internal"), null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items[0].relatedDatasetPid", equalTo(pidB));

        // Filter by source=external -> Expect 1
        UtilIT.listDatasetRelations(pidA, null, null, null, List.of("external"), null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items[0].externalIdentifier", equalTo("https://example.org/1"));

        // Filter by both -> Expect 2
        UtilIT.listDatasetRelations(pidA, null, null, null, Arrays.asList("internal", "external"), null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2));
    }

    @Test
    public void testListDatasetRelationsOrdering() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Create Dataset A
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Create Dataset B
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // Create Dataset C
        Response createDatasetC = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidC = UtilIT.getDatasetPersistentIdFromResponse(createDatasetC);

        // Define relation B -> A (defined on B)
        JsonArray relationsB = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidA)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .build();
        UtilIT.replaceDatasetRelations(pidB, relationsB.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Define relation A -> C (defined on A)
        JsonArray relationsA = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidC)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relationsA.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // List relations for A
        // Expecting:
        // 1. A -> C (defined on A)
        // 2. B -> A (defined on B)

        Response listResponse = UtilIT.listDatasetRelations(pidA, apiTokenSuperuser);
        listResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.items", hasSize(2));

        // We want relations defined ON dataset A to come first:
        // Relation A -> C is defined on A (definitionPointPid should be pidA)
        // Relation B -> A is defined on B (definitionPointPid should be pidB)

        List<String> definitionPointPids = listResponse.jsonPath().getList("data.items.definitionPointPid");
        assertEquals(pidA, definitionPointPids.get(0));
        assertEquals(pidB, definitionPointPids.get(1));
    }

    @Test
    public void testListDatasetRelationDeduplication() {
        // Create Dataset A
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Create Dataset B
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);
        Integer datasetBId = UtilIT.getDatasetIdFromResponse(createDatasetB);
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        assertTrue(UtilIT.sleepForSearch("id:dataset_" + datasetBId + " AND relatedDatasetCount:0", apiTokenSuperuser, "", 1, UtilIT.GENERAL_LONG_DURATION));
        UtilIT.search("id:dataset_" + datasetBId, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.items[0].relatedDatasetCount", equalTo(0));

        // Define relation A (draft) -> B
        JsonArray relations = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isCitedBy")))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Publish A v1.0
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Publishing A makes its internal relation visible to B, so B's Solr count must have been reindexed
        assertTrue(UtilIT.sleepForSearch("id:dataset_" + datasetBId + " AND relatedDatasetCount:1", apiTokenSuperuser, "", 1, UtilIT.GENERAL_LONG_DURATION));
        UtilIT.search("id:dataset_" + datasetBId, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.items[0].relatedDatasetCount", equalTo(1));

        // Define the SAME relation in inverse direction: B (draft) -> A
        JsonArray relationsInverse = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidA)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "cites")))
                .build();
        UtilIT.replaceDatasetRelations(pidB, relationsInverse.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Publish B v1.0
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Create A v2 (draft) and add one more relation
        // (The existing relation from A v1 still exists)
        String externalUrl = "https://example.org/dataset/12345";
        JsonObject relationNew = JsonUtil.createObjectBuilder()
                .add("externalIdentifier", externalUrl)
                .add("identifierScheme", "URL")
                .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo"))
                .build();
        UtilIT.addDatasetRelation(pidA, relationNew.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // The relation "A is cited by B" is now present 3 times: in A v1, B v1, A draft
        // Plus the additional relation in A draft: "A is related to https://example.org/dataset/12345"

        // Verify only those two relations are listed for A (draft) (no duplicates)
        UtilIT.listDatasetRelations(pidA, ":draft", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2))
                .body("data.items", hasSize(2))
                .body("data.items.relatedDatasetPid", hasItem(pidB))
                .body("data.items.externalIdentifier", hasItem(externalUrl));

        // Verify relation-type facets
        UtilIT.listDatasetRelations(pidA, ":draft", null, null, null, null, null, apiTokenSuperuser, true)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.facets.relationType", hasSize(2))
                .body("data.facets.relationType[0].name", equalTo("isCitedBy"))
                .body("data.facets.relationType[0].count", equalTo(1))
                .body("data.facets.relationType[1].name", equalTo("isRelatedTo"))
                .body("data.facets.relationType[1].count", equalTo(1));
    }

    @Test
    public void testListDatasetRelationsVersionIsolation() {
        // Create Dataset A, published v1.0
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Create Dataset B, draft version
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // Add relation from Dataset B (draft) to Dataset A (v1.0) at dataset B (draft)
        JsonArray relations = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidA)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .build();

        UtilIT.replaceDatasetRelations(pidB, relations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Verify relation is listed when requesting relations for Dataset B (draft)
        UtilIT.listDatasetRelations(pidB, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items", hasSize(1))
                .body("data.items[0].relatedDatasetPid", equalTo(pidA))
                .body("data.items[0].relationType.name", equalTo("isRelatedTo"));

        // Verify relation is not listed when requesting relations for Dataset A (v1)
        // since Dataset B, where the relation was defined, is still in Draft status
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0))
                .body("data.items", hasSize(0));

        // Publish Dataset B (=> v1.0)
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Verify relation is now listed when requesting relations for Dataset A
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items", hasSize(1))
                .body("data.items[0].relatedDatasetPid", equalTo(pidB))
                .body("data.items[0].relationType.name", equalTo("isRelatedTo"));

        // Verify relation is listed when requesting relations for Dataset B v1.0
        UtilIT.listDatasetRelations(pidB, "1.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items", hasSize(1))
                .body("data.items[0].relatedDatasetPid", equalTo(pidA))
                .body("data.items[0].relationType.name", equalTo("isRelatedTo"));

        // Remove the relation in new version of Dataset B (draft)
        UtilIT.replaceDatasetRelations(pidB, "[]", apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Verify relation is still listed when requesting relations for Dataset A
        // Because Dataset B is still in draft mode
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items", hasSize(1))
                .body("data.items[0].relatedDatasetPid", equalTo(pidB))
                .body("data.items[0].relationType.name", equalTo("isRelatedTo"));

        // Verify relation is still listed when requesting relations for Dataset B without a token
        // Because without a token, users cannot see Dataset B's draft
        UtilIT.listDatasetRelations(pidB)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items", hasSize(1))
                .body("data.items[0].relatedDatasetPid", equalTo(pidA))
                .body("data.items[0].relationType.name", equalTo("isRelatedTo"));

        // Verify relation is not listed when requesting relations for Dataset B's draft specifically
        UtilIT.listDatasetRelations(pidB, ":draft", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0))
                .body("data.items", hasSize(0));

        // Publish Dataset B (=> v2.0)
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Verify relation is NO LONGER listed when requesting relations for Dataset A
        // Because Dataset B is now at v2 and v2 has no relation
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0))
                .body("data.items", hasSize(0));

        // Verify relation is STILL listed when requesting relations for Dataset B v1 specifically
        UtilIT.listDatasetRelations(pidB, "1.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items", hasSize(1))
                .body("data.items[0].relatedDatasetPid", equalTo(pidA))
                .body("data.items[0].relationType.name", equalTo("isRelatedTo"));

        // Verify relation is NOT listed when requesting relations for Dataset B v2
        UtilIT.listDatasetRelations(pidB, "2.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0))
                .body("data.items", hasSize(0));

        // Verify relation is NOT listed when requesting relations for Dataset B
        UtilIT.listDatasetRelations(pidB, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0))
                .body("data.items", hasSize(0));
    }

    @Test
    public void testListExternalDatasetRelations() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Add external relation to Dataset A (draft)
        String externalUrl = "https://example.org/dataset/12345";
        JsonArray relations = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("externalIdentifier", externalUrl)
                        .add("identifierScheme", "URL")
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .build();

        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Verify external relation is listed
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items", hasSize(1))
                .body("data.items[0].externalIdentifier", equalTo(externalUrl))
                .body("data.items[0].identifierScheme", equalTo("URL"))
                .body("data.items[0].relationType.name", equalTo("isRelatedTo"));

        // Add external relation with datasetType
        String externalUrlWithDocType = "https://example.org/dataset/67890";
        JsonArray relationsWithDocType = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("externalIdentifier", externalUrlWithDocType)
                        .add("identifierScheme", "URL")
                        .add("relatedDatasetType", JsonUtil.createObjectBuilder().add("displayName", "Document"))
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .build();

        UtilIT.replaceDatasetRelations(pidA, relationsWithDocType.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Verify external relation with datasetType is listed
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items", hasSize(1))
                .body("data.items[0].externalIdentifier", equalTo(externalUrlWithDocType))
                .body("data.items[0].relatedDatasetType.displayName", equalTo("Document"));

        // An external free-text dataset type is included in the dataset-type facet.
        UtilIT.listDatasetRelations(pidA, null, null, null, null, null, null, apiTokenSuperuser, true)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.facets.datasetType", hasSize(1))
                .body("data.facets.datasetType[0].displayName", equalTo("Document"))
                .body("data.facets.datasetType[0].count", equalTo(1));

        // The free-text facet value can also be used with the datasetType filter.
        UtilIT.listDatasetRelations(pidA, null, null, List.of("Document"), null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items[0].externalIdentifier", equalTo(externalUrlWithDocType));

        // Add both internal and external relations
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        JsonArray mixedRelations = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .add(JsonUtil.createObjectBuilder()
                        .add("externalIdentifier", "doi:10.1234/5678")
                        .add("identifierScheme", "DOI")
                        .add("relatedDatasetType", JsonUtil.createObjectBuilder().add("displayName", "Dataset"))
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .build();

        UtilIT.replaceDatasetRelations(pidA, mixedRelations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2))
                .body("data.items", hasSize(2))
                .body("data.items[1].relatedDatasetPid", equalTo(pidB))
                .body("data.items[1].relatedDatasetType.name", equalTo("dataset"))
                .body("data.items[1].relatedDatasetType.displayName", equalTo("Dataset"))
                .body("data.items[0].externalIdentifier", equalTo("doi:10.1234/5678"))
                .body("data.items[0].identifierScheme", equalTo("DOI"))
                .body("data.items[0].relatedDatasetType.name", equalTo(null))
                .body("data.items[0].relatedDatasetType.displayName", equalTo("Dataset"));

        // An external type matching an internal type's display name shares its facet value
        UtilIT.listDatasetRelations(pidA, null, null, null, null, null, null, apiTokenSuperuser, true)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.facets.datasetType", hasSize(1))
                .body("data.facets.datasetType[0].name", equalTo("dataset"))
                .body("data.facets.datasetType[0].displayName", equalTo("Dataset"))
                .body("data.facets.datasetType[0].count", equalTo(2));

        UtilIT.listDatasetRelations(pidA, null, null, List.of("dataset"), null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2));
    }

    @Test
    public void testDestroyDatasetRemovesIncomingRelations() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);
        int idB = UtilIT.getDatasetIdFromResponse(createDatasetB);

        JsonArray relations = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1));

        UtilIT.destroyDataset(idB, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0))
                .body("data.items", hasSize(0));
    }

    @Test
    public void testInternalRelationsWithoutInverseType() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        String pidA = UtilIT.getDatasetPersistentIdFromResponse(UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser));
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser));

        // Create relation A -> B using a relation type that has no inverse
        JsonObject relation = JsonUtil.createObjectBuilder()
                .add("relatedDatasetPid", pidB)
                .add("relationType", JsonUtil.createObjectBuilder().add("name", "isDerivedFromWithoutInverse"))
                .build();
        Response createRelation = UtilIT.addDatasetRelation(pidA, relation.toString(), apiTokenSuperuser);
        createRelation.then().assertThat().statusCode(OK.getStatusCode());

        int relationId = createRelation.jsonPath().getInt("data.id");

        // Retrieving the relation for dataset A lists the relation type
        UtilIT.getDatasetRelation(pidA, relationId, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.datasetPid", equalTo(pidA))
                .body("data.relatedDatasetPid", equalTo(pidB))
                .body("data.relationType.name", equalTo("isDerivedFromWithoutInverse"));

        // Retrieving the relation for dataset B lists no relation type (because the type has no inverse)
        UtilIT.getDatasetRelation(pidB, relationId, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.datasetPid", equalTo(pidB))
                .body("data.relatedDatasetPid", equalTo(pidA))
                .body("data.relationType.name", equalTo(null));

        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.listDatasetRelations(pidB, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items[0].datasetPid", equalTo(pidB))
                .body("data.items[0].relatedDatasetPid", equalTo(pidA))
                .body("data.items[0].relationType.name", equalTo(null));

        UtilIT.listDatasetRelations(pidB, null, List.of("isDerivedFromWithoutInverse"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0));
    }

    @Test
    public void testGetDatasetRelationCounts() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Create main Dataset A
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Create other datasets to relate to
        String[] pids = new String[8];
        for (int i = 0; i < 8; i++) {
            Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
            pids[i] = UtilIT.getDatasetPersistentIdFromResponse(createDataset);
        }

        // Setup relations:
        // isCitedBy: 3 relations
        // isRelatedTo: 3 relations (including one that omits its type)
        // isSupplementTo: 2 relations
        // (Alphabetical: isCitedBy < isRelatedTo < isSupplementTo)
        // Expected order:
        // 1. isCitedBy (count: 3)
        // 2. isRelatedTo (count: 3)
        // 3. isSupplementTo (count: 2)

        JsonArray relations = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder().add("relatedDatasetPid", pids[0]).add("relationType", JsonUtil.createObjectBuilder().add("name", "isCitedBy")))
                .add(JsonUtil.createObjectBuilder().add("relatedDatasetPid", pids[1]).add("relationType", JsonUtil.createObjectBuilder().add("name", "isCitedBy")))
                .add(JsonUtil.createObjectBuilder().add("relatedDatasetPid", pids[2]).add("relationType", JsonUtil.createObjectBuilder().add("name", "isCitedBy")))
                .add(JsonUtil.createObjectBuilder().add("relatedDatasetPid", pids[3]).add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .add(JsonUtil.createObjectBuilder().add("relatedDatasetPid", pids[4]).add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .add(JsonUtil.createObjectBuilder().add("relatedDatasetPid", pids[5]).add("relationType", JsonUtil.createObjectBuilder().add("name", "isSupplementTo")))
                .add(JsonUtil.createObjectBuilder().add("relatedDatasetPid", pids[6]).add("relationType", JsonUtil.createObjectBuilder().add("name", "isSupplementTo")))
                .add(JsonUtil.createObjectBuilder().add("relatedDatasetPid", pids[7]))
                .build();

        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Check facets and sorting
        UtilIT.listDatasetRelations(pidA, null, null, null, null, null, null, apiTokenSuperuser, true)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.facets.relationType", hasSize(3))
                // 1st: isCitedBy (count 3)
                .body("data.facets.relationType[0].name", equalTo("isCitedBy"))
                .body("data.facets.relationType[0].count", equalTo(3))
                // 2nd: isRelatedTo (count 3)
                .body("data.facets.relationType[1].name", equalTo("isRelatedTo"))
                .body("data.facets.relationType[1].count", equalTo(3))
                // 3rd: isSupplementTo (count 2)
                .body("data.facets.relationType[2].name", equalTo("isSupplementTo"))
                .body("data.facets.relationType[2].count", equalTo(2))
                .body("data.facets.datasetType", hasSize(1))
                .body("data.facets.datasetType[0].name", equalTo("dataset"))
                .body("data.facets.datasetType[0].count", equalTo(8));

        // The dataset-type facet applies the active relation-type filter
        UtilIT.listDatasetRelations(pidA, null, List.of("isCitedBy"), null, null, null, null, apiTokenSuperuser, true)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(3))
                .body("data.facets.datasetType", hasSize(1))
                .body("data.facets.datasetType[0].name", equalTo("dataset"))
                .body("data.facets.datasetType[0].count", equalTo(3));

        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(8));
    }

    @Test
    public void testDatasetRelationCRUD() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);
        Integer datasetBId = UtilIT.getDatasetIdFromResponse(createDatasetB);
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // POST /api/datasets/{identifier}/relations
        String relationJson = JsonUtil.createObjectBuilder()
                .add("relatedDatasetPid", pidB)
                .add("relationType", JsonUtil.createObjectBuilder().add("name", "isSupplementTo"))
                .build().toString();

        Response postResponse = UtilIT.addDatasetRelation(pidA, relationJson, apiTokenSuperuser);
        postResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.id", notNullValue());

        int relationId = postResponse.jsonPath().getInt("data.id");

        // GET /api/datasets/{identifier}/relations/{id}
        // GET relation for dataset A -> should be isSupplementTo
        UtilIT.getDatasetRelation(pidA, relationId, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode())
                .body("data.id", equalTo(relationId))
                .body("data.relatedDatasetPid", equalTo(pidB))
                .body("data.relationType.name", equalTo("isSupplementTo"));

        // GET relation for dataset B -> should be isSupplementedBy (inverted)
        UtilIT.getDatasetRelation(pidB, relationId, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.relationType.name", equalTo("isSupplementedBy"));

        // POST /api/datasets/{identifier}/relations
        relationJson = JsonUtil.createObjectBuilder()
                .add("relatedDatasetPid", pidB)
                .build().toString();

        postResponse = UtilIT.addDatasetRelation(pidA, relationJson, apiTokenSuperuser);
        postResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("data.id", notNullValue());

        relationId = postResponse.jsonPath().getInt("data.id");

        // GET /api/datasets/{identifier}/relations/{id}
        // GET relation for dataset A -> should use the default type
        UtilIT.getDatasetRelation(pidA, relationId, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode())
                .body("data.id", equalTo(relationId))
                .body("data.relatedDatasetPid", equalTo(pidB))
                .body("data.relationType.name", equalTo("isRelatedTo"));

        // GET relation for dataset B -> should use the default type's inverse
        UtilIT.getDatasetRelation(pidB, relationId, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.relationType.name", equalTo("isRelatedTo"));

        // DELETE /api/datasets/{identifier}/relations/{id}
        UtilIT.deleteDatasetRelation(pidA, relationId, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Verify GET after DELETE is 404
        UtilIT.getDatasetRelation(pidA, relationId, apiTokenSuperuser)
                .then().assertThat().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void testDatasetRelationCRUDErrorPaths() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // GET or DELETE on non-existent ID should be 404
        UtilIT.getDatasetRelation(pidA, 999999, apiTokenSuperuser)
                .then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        UtilIT.deleteDatasetRelation(pidA, 999999, apiTokenSuperuser)
                .then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // First create a relation
        String relationJson = JsonUtil.createObjectBuilder()
                .add("relatedDatasetPid", pidB)
                .add("relationType", JsonUtil.createObjectBuilder().add("name", "isSupplementTo"))
                .build().toString();

        long relationId = UtilIT.addDatasetRelation(pidA, relationJson, apiTokenSuperuser)
                .then().extract().jsonPath().getInt("data.id");

        // Try to create relation with non-existent relation type
        // Should be 400
        String invalidRelationJson = JsonUtil.createObjectBuilder()
                .add("relatedDatasetPid", pidB)
                .add("relationType", JsonUtil.createObjectBuilder().add("name", "iDontExist"))
                .build().toString();

        UtilIT.addDatasetRelation(pidA, invalidRelationJson.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        // Create a third dataset C
        Response createDatasetC = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidC = UtilIT.getDatasetPersistentIdFromResponse(createDatasetC);

        // Try to GET relation with pidC (which is not involved in the relation)
        // Should be 404
        UtilIT.getDatasetRelation(pidC, relationId, apiTokenSuperuser)
                .then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // Try to add the already existing relation to B again
        // Should count as a duplicate and therefore be 400
        JsonObject dupRelation = JsonUtil.createObjectBuilder()
                .add("relatedDatasetPid", pidB)
                .add("relationType", JsonUtil.createObjectBuilder().add("name", "isSupplementTo"))
                .build();

        UtilIT.addDatasetRelation(pidA, dupRelation.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        // Try to DELETE relation with pidC
        // Should be 404
        UtilIT.deleteDatasetRelation(pidC, relationId, apiTokenSuperuser)
                .then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // Try to DELETE relation with pidB
        // Should be 404
        UtilIT.deleteDatasetRelation(pidB, relationId, apiTokenSuperuser)
                .then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // Try to DELETE relation with pidA
        // Should work as the relation was defined on pidA
        UtilIT.getDatasetRelation(pidB, relationId, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testDatasetRelationCRUDAuthErrorPaths() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // Create a normal user
        Response createUser = UtilIT.createRandomUser();
        String apiTokenUser = UtilIT.getApiTokenFromResponse(createUser);

        // Without edit rights on the dataset, post and delete should fail
        String relationJson = JsonUtil.createObjectBuilder()
                .add("relatedDatasetPid", pidB)
                .add("relationType", JsonUtil.createObjectBuilder().add("name", "isSupplementTo"))
                .build().toString();

        UtilIT.addDatasetRelation(pidA, relationJson, apiTokenUser)
                .then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // Create relation as superuser
        long relationId = UtilIT.addDatasetRelation(pidA, relationJson, apiTokenSuperuser)
                .then().extract().jsonPath().getInt("data.id");

        // Try to delete as normal user
        UtilIT.deleteDatasetRelation(pidA, relationId, apiTokenUser)
                .then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // If not authenticated and the relation isn't published, get should fail
        UtilIT.getDatasetRelation(pidA, relationId, null)
                .then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        // Even with a token, if user has no view rights on unpublished dataset, it should fail
        UtilIT.getDatasetRelation(pidA, relationId, apiTokenUser)
                .then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        // Publish dataset A and B
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Normal users cannot delete relations defined on a released version
        UtilIT.deleteDatasetRelation(pidA, relationId, apiTokenUser)
                .then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // After publication, GET should work even without token
        UtilIT.getDatasetRelation(pidA, relationId, null)
                .then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testReplaceDatasetRelationsErrorPaths() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser));
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser));

        JsonObject typedRelation = JsonUtil.createObjectBuilder()
                .add("relatedDatasetPid", pidB)
                .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo"))
                .build();
        UtilIT.replaceDatasetRelations(pidA, JsonUtil.createArrayBuilder().add(typedRelation).build().toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        JsonObject untypedRelation = JsonUtil.createObjectBuilder()
                .add("relatedDatasetPid", pidB)
                .build();
        JsonArray typedThenUntyped = JsonUtil.createArrayBuilder()
                .add(typedRelation)
                .add(untypedRelation)
                .build();
        UtilIT.replaceDatasetRelations(pidA, typedThenUntyped.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        JsonArray untypedThenTyped = JsonUtil.createArrayBuilder()
                .add(untypedRelation)
                .add(typedRelation)
                .build();
        UtilIT.replaceDatasetRelations(pidA, untypedThenTyped.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        JsonArray invalidRelatedDataset = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder().add("relatedDatasetPid", "doi:10.5072/FK2/DOESNOTEXIST"))
                .build();
        UtilIT.replaceDatasetRelations(pidA, invalidRelatedDataset.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        JsonObject invalidRelationType = JsonUtil.createObjectBuilder()
                .add("relatedDatasetPid", pidB)
                .add("relationType", JsonUtil.createObjectBuilder().add("name", "iDontExist"))
                .build();
        UtilIT.replaceDatasetRelations(pidA, JsonUtil.createArrayBuilder().add(invalidRelationType).build().toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data.items[0].relatedDatasetPid", equalTo(pidB))
                .body("data.items[0].relationType.name", equalTo("isRelatedTo"));
    }

    @Test
    public void testDatasetRelationsViaVersionApis() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Create dataset with relations via POST api/dataverses/%s/datasets
        String externalUrl1 = "https://example.org/dataset/1";
        JsonObject datasetData = JsonUtil.createObjectBuilder()
                .add("datasetVersion", JsonUtil.createObjectBuilder()
                        .add("license", JsonUtil.createObjectBuilder()
                                .add("name", "CC0 1.0")
                                .add("uri", "http://creativecommons.org/publicdomain/zero/1.0"))
                        .add("metadataBlocks", JsonUtil.createObjectBuilder()
                                .add("citation", JsonUtil.createObjectBuilder()
                                        .add("fields", JsonUtil.createArrayBuilder()
                                                .add(JsonUtil.createObjectBuilder()
                                                        .add("typeName", "title")
                                                        .add("multiple", false)
                                                        .add("typeClass", "primitive")
                                                        .add("value", "Dataset with Relations"))
                                                .add(JsonUtil.createObjectBuilder()
                                                        .add("typeName", "author")
                                                        .add("multiple", true)
                                                        .add("typeClass", "compound")
                                                        .add("value", JsonUtil.createArrayBuilder()
                                                                .add(JsonUtil.createObjectBuilder()
                                                                        .add("authorName", JsonUtil.createObjectBuilder()
                                                                                .add("typeName", "authorName")
                                                                                .add("multiple", false)
                                                                                .add("typeClass", "primitive")
                                                                                .add("value", "Lastname, Firstname")))))
                                                .add(JsonUtil.createObjectBuilder()
                                                        .add("typeName", "datasetContact")
                                                        .add("multiple", true)
                                                        .add("typeClass", "compound")
                                                        .add("value", JsonUtil.createArrayBuilder()
                                                                .add(JsonUtil.createObjectBuilder()
                                                                        .add("datasetContactEmail", JsonUtil.createObjectBuilder()
                                                                                .add("typeName", "datasetContactEmail")
                                                                                .add("multiple", false)
                                                                                .add("typeClass", "primitive")
                                                                                .add("value", "test@example.edu")))))
                                                .add(JsonUtil.createObjectBuilder()
                                                        .add("typeName", "dsDescription")
                                                        .add("multiple", true)
                                                        .add("typeClass", "compound")
                                                        .add("value", JsonUtil.createArrayBuilder()
                                                                .add(JsonUtil.createObjectBuilder()
                                                                        .add("dsDescriptionValue", JsonUtil.createObjectBuilder()
                                                                                .add("typeName", "dsDescriptionValue")
                                                                                .add("multiple", false)
                                                                                .add("typeClass", "primitive")
                                                                                .add("value", "Description text")))))
                                                .add(JsonUtil.createObjectBuilder()
                                                        .add("typeName", "subject")
                                                        .add("multiple", true)
                                                        .add("typeClass", "controlledVocabulary")
                                                        .add("value", JsonUtil.createArrayBuilder().add("Agricultural Sciences"))))))
                        .add("relations", JsonUtil.createArrayBuilder()
                                .add(JsonUtil.createObjectBuilder()
                                        .add("externalIdentifier", externalUrl1)
                                        .add("identifierScheme", "URL")
                                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))))
                .build();

        Response createDataset = UtilIT.createDataset(dataverseAlias, datasetData.toString(), apiTokenSuperuser);
        createDataset.then().assertThat().statusCode(CREATED.getStatusCode());
        String pid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        // Read dataset and verify relations are present via GET api/datasets/:persistentId/versions/:draft
        UtilIT.getDatasetVersion(pid, ":draft", apiTokenSuperuser, false, false, false, false)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.relations", hasSize(1))
                .body("data.relations[0].externalIdentifier", equalTo(externalUrl1))
                .body("data.relations[0].relationType.name", equalTo("isRelatedTo"));

        // Native JSON imports use the same relation entity parsing as dataset creation
        String importedPid = "doi:10.5072/FK2/" + UtilIT.getRandomString(6);
        Response importedDataset = UtilIT.importDatasetNativeJson(apiTokenSuperuser, dataverseAlias,
                datasetData.toString(), importedPid, "no");
        importedDataset.then().assertThat().statusCode(CREATED.getStatusCode());
        UtilIT.getDatasetVersion(importedPid, ":draft", apiTokenSuperuser, false, false, false, false)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.relations", hasSize(1))
                .body("data.relations[0].externalIdentifier", equalTo(externalUrl1))
                .body("data.relations[0].relationType.name", equalTo("isRelatedTo"));

        // Update dataset with NEW relations via PUT api/datasets/:persistentId/versions/:draft
        String externalUrl2 = "https://example.org/dataset/2";

        // Reuse the existing data and replace relations
        JsonObject updatedVersionData = JsonUtil.createObjectBuilder(datasetData.getJsonObject("datasetVersion"))
                .add("relations", JsonUtil.createArrayBuilder()
                        .add(JsonUtil.createObjectBuilder()
                                .add("externalIdentifier", externalUrl2)
                                .add("identifierScheme", "URL")
                                .add("relationType", JsonUtil.createObjectBuilder().add("name", "isSupplementTo"))))
                .build();

        UtilIT.updateDatasetMetadataViaNative(pid, updatedVersionData, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Replacing a draft with the same relations again must not create duplicates
        UtilIT.updateDatasetMetadataViaNative(pid, updatedVersionData, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Try to upload invalid relations
        // Should be 400
        JsonObject unknownRelationTypeVersion = JsonUtil.createObjectBuilder(updatedVersionData)
                .add("relations", JsonUtil.createArrayBuilder()
                        .add(JsonUtil.createObjectBuilder()
                                .add("externalIdentifier", externalUrl2)
                                .add("identifierScheme", "URL")
                                .add("relationType", JsonUtil.createObjectBuilder().add("name", "unknownRelationType"))))
                .build();
        UtilIT.updateDatasetMetadataViaNative(pid, unknownRelationTypeVersion, apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        JsonObject unknownRelatedDatasetVersion = JsonUtil.createObjectBuilder(updatedVersionData)
                .add("relations", JsonUtil.createArrayBuilder()
                        .add(JsonUtil.createObjectBuilder()
                                .add("relatedDatasetPid", "doi:10.5072/FK2/DOESNOTEXIST")))
                .build();

        // Change metadata in the same request as the invalid relation
        // The entire update should roll back
        JsonObject citation = updatedVersionData.getJsonObject("metadataBlocks").getJsonObject("citation");
        jakarta.json.JsonArrayBuilder changedFields = JsonUtil.createArrayBuilder();
        for (JsonValue fieldValue : citation.getJsonArray("fields")) {
            JsonObject field = (JsonObject) fieldValue;
            if ("title".equals(field.getString("typeName"))) {
                changedFields.add(JsonUtil.createObjectBuilder(field).add("value", "This title must not persist"));
            } else {
                changedFields.add(field);
            }
        }
        JsonObject changedMetadataBlocks = JsonUtil.createObjectBuilder(updatedVersionData.getJsonObject("metadataBlocks"))
                .add("citation", JsonUtil.createObjectBuilder(citation).add("fields", changedFields))
                .build();
        JsonObject invalidRelationAndMetadata = JsonUtil.createObjectBuilder(unknownRelatedDatasetVersion)
                .add("metadataBlocks", changedMetadataBlocks)
                .build();
        UtilIT.updateDatasetMetadataViaNative(pid, invalidRelationAndMetadata, apiTokenSuperuser)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());

        // Read again and verify relations are updated
        UtilIT.getDatasetVersion(pid, ":draft", apiTokenSuperuser, false, false, false, false)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.relations", hasSize(1))
                .body("data.relations[0].externalIdentifier", equalTo(externalUrl2))
                .body("data.metadataBlocks.citation.fields.find { it.typeName == 'title' }.value", equalTo("Dataset with Relations"))
                .body("data.relations[0].relationType.name", equalTo("isSupplementTo"));

        UtilIT.publishDatasetViaNativeApi(pid, "major", apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        JsonObject versionWithoutRelations = JsonUtil.createObjectBuilder(updatedVersionData)
                .remove("relations")
                .build();
        UtilIT.updateDatasetMetadataViaNative(pid, versionWithoutRelations, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Omitting relations while creating a new draft preserves those from the prior release
        UtilIT.getDatasetVersion(pid, ":draft", apiTokenSuperuser, false, false, false, false)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.relations", hasSize(1))
                .body("data.relations[0].externalIdentifier", equalTo(externalUrl2))
                .body("data.relations[0].relationType.name", equalTo("isSupplementTo"));

        // Harvestable native JSON exports include relations
        UtilIT.exportDataset(pid, "dataverse_json", true, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("datasetVersion.relations", hasSize(1))
                .body("datasetVersion.relations[0].externalIdentifier", equalTo(externalUrl2))
                .body("datasetVersion.relations[0].relationType.name", equalTo("isSupplementTo"));
    }

    @Test
    public void testVersionSummariesIncludeRelationChanges() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser));
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser));
        String externalIdentifier = "https://example.org/dataset/relations-summary";

        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        JsonArray relations = JsonUtil.createArrayBuilder()
                .add(JsonUtil.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .add(JsonUtil.createObjectBuilder()
                        .add("externalIdentifier", externalIdentifier)
                        .add("identifierScheme", "URL")
                        .add("relationType", JsonUtil.createObjectBuilder().add("name", "isRelatedTo")))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.compareDatasetVersions(pidA, "1.0", "2.0", apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.relationsAdded", hasSize(2))
                .body("data.relationsAdded.relatedDatasetPid", hasItem(pidB))
                .body("data.relationsAdded.externalIdentifier", hasItem(externalIdentifier));
        UtilIT.summaryDatasetVersionDifferences(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data[0].summary.relations.added", equalTo(2))
                .body("data[0].summary.relations.removed", equalTo(0));

        UtilIT.replaceDatasetRelations(pidA, "[]", apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.compareDatasetVersions(pidA, "2.0", "3.0", apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.relationsRemoved", hasSize(2))
                .body("data.relationsRemoved.relatedDatasetPid", hasItem(pidB))
                .body("data.relationsRemoved.externalIdentifier", hasItem(externalIdentifier));
        UtilIT.summaryDatasetVersionDifferences(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data[0].summary.relations.removed", equalTo(2));
    }
}
