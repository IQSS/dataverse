package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SavedSearchIT {

    @BeforeAll
    public static void setUpClass() {

    }

    @AfterAll
    public static void afterClass() {

    }

    @Test
    public void testSavedSearches() {

        Response createAdminUser = UtilIT.createRandomUser();
        String adminUsername = UtilIT.getUsernameFromResponse(createAdminUser);
        String adminApiToken = UtilIT.getApiTokenFromResponse(createAdminUser);
        UtilIT.makeSuperUser(adminUsername);

        Response createDataverseResponse = UtilIT.createRandomDataverse(adminApiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverseResponse);

        //dataset-finch1-nolicense.json
        Response createDatasetResponse1 = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, adminApiToken);
        createDatasetResponse1.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse1);

        Response createDatasetResponse2 = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, adminApiToken);
        createDatasetResponse2.prettyPrint();
        Integer datasetId2 = UtilIT.getDatasetIdFromResponse(createDatasetResponse2);

        // missing body
        Response resp = RestAssured.given()
                .contentType("application/json")
                .post("/api/admin/savedsearches");
        resp.prettyPrint();
        resp.then().assertThat()
                .statusCode(INTERNAL_SERVER_ERROR.getStatusCode());

        // creatorId null
        resp = RestAssured.given()
                .body(createSavedSearchJson("*", null, dataverseId, "subject_ss:Medicine, Health and Life Sciences"))
                .contentType("application/json")
                .post("/api/admin/savedsearches");
        resp.prettyPrint();
        resp.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

        // creatorId string
        resp = RestAssured.given()
                .body(createSavedSearchJson("*", "1", dataverseId.toString(), "subject_ss:Medicine, Health and Life Sciences"))
                .contentType("application/json")
                .post("/api/admin/savedsearches");
        resp.prettyPrint();
        resp.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

        // creatorId not found
        resp = RestAssured.given()
                .body(createSavedSearchJson("*", 9999, dataverseId, "subject_ss:Medicine, Health and Life Sciences"))
                .contentType("application/json")
                .post("/api/admin/savedsearches");
        resp.prettyPrint();
        resp.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());

        // definitionPointId null
        resp = RestAssured.given()
                .body(createSavedSearchJson("*", 1, null, "subject_ss:Medicine, Health and Life Sciences"))
                .contentType("application/json")
                .post("/api/admin/savedsearches");
        resp.prettyPrint();
        resp.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

        // definitionPointId string
        resp = RestAssured.given()
                .body(createSavedSearchJson("*", "1", "9999", "subject_ss:Medicine, Health and Life Sciences"))
                .contentType("application/json")
                .post("/api/admin/savedsearches");
        resp.prettyPrint();
        resp.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

        // definitionPointId not found
        resp = RestAssured.given()
                .body(createSavedSearchJson("*", 1, 9999, "subject_ss:Medicine, Health and Life Sciences"))
                .contentType("application/json")
                .post("/api/admin/savedsearches");
        resp.prettyPrint();
        resp.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());

        // missing filter
        resp = RestAssured.given()
                .body(createSavedSearchJson("*", 1, dataverseId))
                .contentType("application/json")
                .post("/api/admin/savedsearches");
        resp.prettyPrint();
        resp.then().assertThat()
                .statusCode(OK.getStatusCode());

        // create a saved search as superuser : OK
        resp = RestAssured.given()
                .body(createSavedSearchJson("*", 1, dataverseId, "subject_ss:Medicine, Health and Life Sciences"))
                .contentType("application/json")
                .post("/api/admin/savedsearches");
        resp.prettyPrint();
        resp.then().assertThat()
                .statusCode(OK.getStatusCode());

        JsonPath path = JsonPath.from(resp.body().asString());
        Integer createdSavedSearchId = path.getInt("data.id");

        // get list as non superuser : OK
        Response getListReponse = RestAssured.given()
                .get("/api/admin/savedsearches/list");
        getListReponse.prettyPrint();
        getListReponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        JsonPath path2 = JsonPath.from(getListReponse.body().asString());
        List<Object> listBeforeDelete = path2.getList("data.savedSearches");

        // makelinks/all as non superuser : OK
        Response makelinksAll = RestAssured.given()
                .put("/api/admin/savedsearches/makelinks/all");
        makelinksAll.prettyPrint();
        makelinksAll.then().assertThat()
                .statusCode(OK.getStatusCode());

        //delete a saved search as non superuser : OK
        Response deleteReponse = RestAssured.given()
                .delete("/api/admin/savedsearches/" + createdSavedSearchId);
        deleteReponse.prettyPrint();
        deleteReponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // check list count minus 1
        getListReponse = RestAssured.given()
                .get("/api/admin/savedsearches/list");
        getListReponse.prettyPrint();
        JsonPath path3 = JsonPath.from(getListReponse.body().asString());
        List<Object> listAfterDelete = path3.getList("data.savedSearches");
        assertEquals(listBeforeDelete.size() - 1, listAfterDelete.size());
    }

    public String createSavedSearchJson(String query, Integer creatorId, Integer definitionPointId, String... filterQueries) {

        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (String filterQuery : filterQueries) {
            arr.add(filterQuery);
        }

        JsonObjectBuilder json = Json.createObjectBuilder();
        if(query != null) json.add("query", query);
        if(creatorId != null) json.add("creatorId", creatorId);
        if(definitionPointId != null) json.add("definitionPointId", definitionPointId);
        if(filterQueries.length > 0) json.add("filterQueries", arr);
        return json.build().toString();
    }

    public String createSavedSearchJson(String query, String creatorId, String definitionPointId, String... filterQueries) {

        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (String filterQuery : filterQueries) {
            arr.add(filterQuery);
        }

        JsonObjectBuilder json = Json.createObjectBuilder();
        if(query != null) json.add("query", query);
        if(creatorId != null) json.add("creatorId", creatorId);
        if(definitionPointId != null) json.add("definitionPointId", definitionPointId);
        if(filterQueries.length > 0) json.add("filterQueries", arr);
        return json.build().toString();
    }
}
