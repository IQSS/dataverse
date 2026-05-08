package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Integration tests for the paginated dataset version tree endpoint:
 *
 *   GET /api/datasets/{id}/versions/{versionId}/tree
 *
 * The endpoint is the backend half of the tree-view selection-and-download
 * track (#6691) and is consumed by the dataverse-client-javascript SDK
 * helper {@code listDatasetTreeNode}. These tests exercise the contract from
 * the perspective of an HTTP client.
 */
public class DatasetsTreeIT {

    private static final String DRAFT_VERSION = ":draft";
    private static final String LATEST = ":latest";

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    /**
     * Builds a small dataset tree:
     * <pre>
     *   root.txt
     *   data/a.txt
     *   data/b.txt
     *   data/sub/c.txt
     *   docs/readme.md
     * </pre>
     * @return the dataset id
     */
    private static int createDatasetWithTree(String apiToken) {
        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        upload(datasetId, "src/test/java/edu/harvard/iq/dataverse/util/irclog.tsv",
                "root.txt", null, apiToken);
        upload(datasetId, "src/test/java/edu/harvard/iq/dataverse/util/irclog.tsv",
                "a.txt", "data", apiToken);
        upload(datasetId, "src/test/java/edu/harvard/iq/dataverse/util/irclog.tsv",
                "b.txt", "data", apiToken);
        upload(datasetId, "src/test/java/edu/harvard/iq/dataverse/util/irclog.tsv",
                "c.txt", "data/sub", apiToken);
        upload(datasetId, "src/test/java/edu/harvard/iq/dataverse/util/irclog.tsv",
                "readme.md", "docs", apiToken);

        return datasetId;
    }

    private static void upload(Integer datasetId, String pathToFile, String label,
                               String directoryLabel, String apiToken) {
        JsonObjectBuilder metadata = Json.createObjectBuilder().add("label", label);
        if (directoryLabel != null) {
            metadata.add("directoryLabel", directoryLabel);
        }
        Response response = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile,
                metadata.build(), apiToken);
        response.then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void rootListingReturnsImmediateChildrenFoldersFirst() {
        String apiToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(apiToken);

        Response response = UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, null, null, null, null, null, apiToken);
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.path", equalTo(""))
                .body("data.items", hasSize(3))
                .body("data.items[0].type", equalTo("folder"))
                .body("data.items[0].name", equalTo("data"))
                .body("data.items[0].counts.files", equalTo(3))
                .body("data.items[0].counts.folders", equalTo(1))
                .body("data.items[1].type", equalTo("folder"))
                .body("data.items[1].name", equalTo("docs"))
                .body("data.items[2].type", equalTo("file"))
                .body("data.items[2].name", equalTo("root.txt"))
                .body("data.items[2].downloadUrl", startsWith("/api/access/datafile/"))
                .body("data.nextCursor", nullValue())
                .body("data.order", equalTo("NameAZ"))
                .body("data.include", equalTo("all"));
    }

    @Test
    public void folderListingReturnsOnlyImmediateChildren() {
        String apiToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(apiToken);

        Response response = UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                "data", null, null, null, null, null, null, apiToken);
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.path", equalTo("data"))
                .body("data.items", hasSize(3))
                .body("data.items[0].type", equalTo("folder"))
                .body("data.items[0].name", equalTo("sub"))
                .body("data.items[1].type", equalTo("file"))
                .body("data.items[1].name", equalTo("a.txt"))
                .body("data.items[1].path", equalTo("data/a.txt"))
                .body("data.items[2].type", equalTo("file"))
                .body("data.items[2].name", equalTo("b.txt"));
    }

    @Test
    public void pathNormalisationStripsRedundantSlashes() {
        String apiToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(apiToken);

        Response response = UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                "/data//sub///", null, null, null, null, null, null, apiToken);
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.path", equalTo("data/sub"))
                .body("data.items", hasSize(1))
                .body("data.items[0].name", equalTo("c.txt"));
    }

    @Test
    public void cursorPaginationReturnsStableSliceAndExhausts() {
        String apiToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(apiToken);

        Response page1 = UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, 2, null, null, null, null, null, apiToken);
        page1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items", hasSize(2))
                .body("data.nextCursor", notNullValue());
        String cursor = JsonPath.from(page1.asString()).getString("data.nextCursor");

        Response page2 = UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, 2, cursor, null, null, null, null, apiToken);
        page2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items", hasSize(1))
                .body("data.nextCursor", nullValue());
    }

    @Test
    public void invalidCursorReturns400() {
        String apiToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(apiToken);

        UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, "not-a-real-cursor", null, null, null, null, apiToken)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void invalidOrderReturns400() {
        String apiToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(apiToken);

        UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, null, null, "Bogus", null, null, apiToken)
                .then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void includeFilterReturnsOnlyMatchingType() {
        String apiToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(apiToken);

        UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, null, "folders", null, null, null, apiToken)
                .then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items.type", hasItem("folder"))
                .body("data.items.findAll { it.type == 'file' }", hasSize(0));

        UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, null, "files", null, null, null, apiToken)
                .then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items.type", hasItem("file"))
                .body("data.items.findAll { it.type == 'folder' }", hasSize(0));
    }

    @Test
    public void descendingOrderReversesNameSort() {
        String apiToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(apiToken);

        Response response = UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, null, null, "NameZA", null, null, apiToken);
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.order", equalTo("NameZA"))
                // Folders still come first; within folders, descending is docs, data.
                .body("data.items[0].name", equalTo("docs"))
                .body("data.items[1].name", equalTo("data"))
                .body("data.items[2].name", equalTo("root.txt"));
    }

    @Test
    public void originalsTogglesDownloadUrlFormat() {
        String apiToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(apiToken);

        Response withoutOriginals = UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, null, "files", null, null, false, apiToken);
        withoutOriginals.then().assertThat()
                .body("data.items[0].downloadUrl",
                        equalTo("/api/access/datafile/" + JsonPath.from(withoutOriginals.asString())
                                .getInt("data.items[0].id")));

        Response withOriginals = UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, null, "files", null, null, true, apiToken);
        withOriginals.then().assertThat()
                .body("data.items[0].downloadUrl",
                        startsWith("/api/access/datafile/"))
                .body("data.items[0].downloadUrl", equalTo(
                        "/api/access/datafile/" + JsonPath.from(withOriginals.asString())
                                .getInt("data.items[0].id") + "?format=original"));
    }

    @Test
    public void unauthenticatedUserCannotReadDraftVersion() {
        String ownerToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(ownerToken);

        // No api token → unauthenticated. Drafts are not readable by anonymous.
        Response response = UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, null, null, null, null, null, null);
        // Either 401 (no auth) or 403 (auth-required filter); both are acceptable.
        int status = response.getStatusCode();
        assert status == UNAUTHORIZED.getStatusCode()
                || status == FORBIDDEN.getStatusCode()
                : "Expected 401 or 403, got " + status;
    }

    @Test
    public void otherAuthenticatedUserCannotReadOwnersDraft() {
        String ownerToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(ownerToken);

        String otherToken = UtilIT.createRandomUserGetToken();
        UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, null, null, null, null, null, otherToken)
                .then().assertThat()
                .statusCode(jakarta.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
        // 404 (not 403) is the standard Dataverse behaviour for a draft a
        // user cannot access; the dataset is treated as not visible at all.
    }

    @Test
    public void emptyDatasetReturnsEmptyItems() {
        String apiToken = UtilIT.createRandomUserGetToken();
        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, null, null, null, null, null, apiToken)
                .then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items", hasSize(0))
                .body("data.nextCursor", nullValue())
                .body("data.approximateCount", equalTo(0));
    }

    @Test
    public void draftVersionDoesNotEmitCacheableEtag() {
        String apiToken = UtilIT.createRandomUserGetToken();
        int datasetId = createDatasetWithTree(apiToken);

        Response response = UtilIT.getVersionTree(datasetId, DRAFT_VERSION,
                null, null, null, null, null, null, null, apiToken);
        response.then().assertThat().statusCode(OK.getStatusCode());
        // Drafts can change — no ETag, no immutable cache header.
        assertNull(response.getHeader("ETag"),
                "Draft versions must not emit a cacheable ETag");
    }

    @Test
    public void publishedVersionEmitsEtagAndHonoursIfNoneMatch() {
        Response createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.makeSuperUser(UtilIT.getUsernameFromResponse(createUser));

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken)
                .then().assertThat().statusCode(OK.getStatusCode());

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        upload(datasetId, "src/test/java/edu/harvard/iq/dataverse/util/irclog.tsv",
                "etag.txt", null, apiToken);
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken)
                .then().assertThat().statusCode(OK.getStatusCode());

        Response first = UtilIT.getVersionTree(datasetId, LATEST,
                null, null, null, null, null, null, null, apiToken);
        first.then().assertThat()
                .statusCode(OK.getStatusCode())
                .header("Cache-Control", equalTo("private, immutable"));
        String etag = first.getHeader("ETag");
        assertNotNull(etag, "Published versions must emit an ETag");
        assert etag.startsWith("\"") && etag.endsWith("\"") : "ETag must be quoted: " + etag;

        Response cached = io.restassured.RestAssured.given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken)
                .header("If-None-Match", etag)
                .get("/api/datasets/" + datasetId + "/versions/" + LATEST + "/tree");
        cached.then().assertThat()
                .statusCode(jakarta.ws.rs.core.Response.Status.NOT_MODIFIED.getStatusCode())
                .header("Cache-Control", equalTo("private, immutable"));

        // Different query params must yield a different ETag.
        Response differentQuery = UtilIT.getVersionTree(datasetId, LATEST,
                null, null, null, "files", null, null, null, apiToken);
        differentQuery.then().assertThat().statusCode(OK.getStatusCode());
        assertEquals(false, etag.equals(differentQuery.getHeader("ETag")),
                "ETag must change when include filter changes");
    }

    @Test
    public void publishedDatasetIsReadableViaLatest() {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String username = UtilIT.getUsernameFromResponse(createUser);
        UtilIT.makeSuperUser(username);

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken)
                .then().assertThat().statusCode(OK.getStatusCode());

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        upload(datasetId, "src/test/java/edu/harvard/iq/dataverse/util/irclog.tsv",
                "published.txt", null, apiToken);
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken)
                .then().assertThat().statusCode(OK.getStatusCode());

        Response response = UtilIT.getVersionTree(datasetId, LATEST,
                null, null, null, null, null, null, null, apiToken);
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.items", hasSize(1))
                .body("data.items[0].type", equalTo("file"))
                .body("data.items[0].name", equalTo("published.txt"));
    }
}
