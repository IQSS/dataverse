package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keyset-stability integration tests for the SQL-backed dataset version tree
 * endpoint. The contract these tests defend:
 *
 * <ol>
 *   <li>For any page size, the concatenation of paged calls equals the
 *       single-shot listing of the same path/order/include — no skips, no
 *       duplicates, identical ordering.</li>
 *   <li>Each item appears exactly once across the paged walk.</li>
 *   <li>Folders precede files; within each block, ordering is by name
 *       (case-insensitive) with a stable id tie-break.</li>
 *   <li>The {@code access} marker on file items reflects the file's
 *       restricted/embargoed state correctly.</li>
 * </ol>
 *
 * The "oracle" in this IT is the single-shot listing (limit large enough to
 * return everything in one page). The paged walks are compared against it
 * page-for-page. This is the SQL keyset-paginator's regression net for
 * {@code DatasetVersionTreeService}.
 */
public class DatasetVersionTreeKeysetIT {

    private static final String DRAFT = ":draft";
    private static final String TEST_FILE = "src/test/java/edu/harvard/iq/dataverse/util/irclog.tsv";

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    /**
     * Builds a moderately deep, mixed-access fixture intended to stress the
     * keyset paginator:
     *
     * <pre>
     *   A.txt
     *   a.txt          (case-clash with A.txt at root)
     *   B.txt
     *   b.txt
     *   archive/2024/january.csv
     *   archive/2024/february.csv
     *   archive/2025/january.csv
     *   data/alpha.txt
     *   data/Beta.txt
     *   data/beta.txt  (case-clash inside folder)
     *   data/gamma.txt
     *   data/sub/leaf-01.txt
     *   data/sub/leaf-02.txt
     *   data/sub/leaf-03.txt
     *   data/sub/leaf-04.txt
     *   data/sub/leaf-05.txt
     *   docs/01.md
     *   docs/02.md
     *   docs/03.md
     *   docs/restricted-readme.md   (RESTRICTED)
     *   docs/embargoed-preview.md   (EMBARGOED)
     *   docs/another-restricted.md  (RESTRICTED)
     *   media/img-001.png
     *   media/img-002.png
     *   media/img-003.png
     * </pre>
     *
     * 25 files, 4 top-level folders (one with a nested subfolder), case
     * clashes at root and inside a folder, three sentinels (2 restricted,
     * 1 embargoed). The numbers are chosen so paging at limit=1, 2, 3, 5,
     * 10 all hit folder/file phase transitions.
     */
    private static int createKeysetFixture(String apiToken) {
        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.then().assertThat().statusCode(CREATED.getStatusCode());
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        // Root: case-clash pair plus straight pair.
        upload(datasetId, "A.txt", null, apiToken);
        upload(datasetId, "a.txt", null, apiToken);
        upload(datasetId, "B.txt", null, apiToken);
        upload(datasetId, "b.txt", null, apiToken);

        // archive/2024 and archive/2025 — two-deep nesting under one root folder.
        upload(datasetId, "january.csv", "archive/2024", apiToken);
        upload(datasetId, "february.csv", "archive/2024", apiToken);
        upload(datasetId, "january.csv", "archive/2025", apiToken);

        // data/* with a case-clash, plus a 5-leaf subfolder.
        upload(datasetId, "alpha.txt", "data", apiToken);
        upload(datasetId, "Beta.txt", "data", apiToken);
        upload(datasetId, "beta.txt", "data", apiToken);
        upload(datasetId, "gamma.txt", "data", apiToken);
        for (int i = 1; i <= 5; i++) {
            upload(datasetId, String.format("leaf-%02d.txt", i), "data/sub", apiToken);
        }

        // docs/* — and stash three file ids for the access-state mutations below.
        upload(datasetId, "01.md", "docs", apiToken);
        upload(datasetId, "02.md", "docs", apiToken);
        upload(datasetId, "03.md", "docs", apiToken);
        Integer restrictedId1 = uploadAndGetId(datasetId, "restricted-readme.md", "docs", apiToken);
        Integer embargoedId = uploadAndGetId(datasetId, "embargoed-preview.md", "docs", apiToken);
        Integer restrictedId2 = uploadAndGetId(datasetId, "another-restricted.md", "docs", apiToken);

        // media/*
        upload(datasetId, "img-001.png", "media", apiToken);
        upload(datasetId, "img-002.png", "media", apiToken);
        upload(datasetId, "img-003.png", "media", apiToken);

        // Apply the access-state sentinels.
        UtilIT.restrictFile(restrictedId1.toString(), true, apiToken)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.restrictFile(restrictedId2.toString(), true, apiToken)
                .then().assertThat().statusCode(OK.getStatusCode());
        // Embargo needs a future date.
        String future = LocalDate.now().plusYears(2).toString();
        UtilIT.createFileEmbargo(datasetId, embargoedId, future, apiToken)
                .then().assertThat().statusCode(OK.getStatusCode());

        return datasetId;
    }

    private static void upload(Integer datasetId, String label, String directoryLabel,
                               String apiToken) {
        uploadAndGetId(datasetId, label, directoryLabel, apiToken);
    }

    private static Integer uploadAndGetId(Integer datasetId, String label,
                                           String directoryLabel, String apiToken) {
        JsonObjectBuilder metadata = Json.createObjectBuilder().add("label", label);
        if (directoryLabel != null) {
            metadata.add("directoryLabel", directoryLabel);
        }
        Response response = UtilIT.uploadFileViaNative(datasetId.toString(), TEST_FILE,
                metadata.build(), apiToken);
        response.then().assertThat().statusCode(OK.getStatusCode());
        return JsonPath.from(response.asString()).getInt("data.files[0].dataFile.id");
    }

    // ---- Oracle / paged-walk plumbing -----------------------------------

    /**
     * Compact representation of a tree item — only the bits we want to assert
     * stay identical between oracle and paged walks. Hash/equals over the
     * tuple gives us a clean per-item comparison without dragging the whole
     * JSON shape through the diff.
     */
    record Item(String type, String name, String path, Long id, String access) {
        @SuppressWarnings("rawtypes")
        static Item from(Map raw) {
            String type = (String) raw.get("type");
            String name = (String) raw.get("name");
            String path = (String) raw.get("path");
            Long id = raw.get("id") == null ? null : ((Number) raw.get("id")).longValue();
            String access = (String) raw.get("access");
            return new Item(type, name, path, id, access);
        }
    }

    private static List<Item> singleShot(int datasetId, String path, String include,
                                          String order, String token) {
        // limit=1000 hits the endpoint's MAX_LIMIT clamp ceiling and is
        // well above this fixture's total node count, so the response
        // must fit on a single page (asserted just below).
        Response response = UtilIT.getVersionTree(datasetId, DRAFT, path, 1000, null,
                include, order, null, null, token);
        response.then().assertThat().statusCode(OK.getStatusCode());
        // No more pages should remain after a single shot at this fixture size.
        assertNull(JsonPath.from(response.asString()).getString("data.nextCursor"),
                "Single-shot call should fit on one page for this fixture");
        @SuppressWarnings("rawtypes")
        List<Map> raw = JsonPath.from(response.asString())
                .getList("data.items", Map.class);
        List<Item> out = new ArrayList<>(raw.size());
        for (Map r : raw) {
            out.add(Item.from(r));
        }
        return out;
    }

    private static List<Item> pagedWalk(int datasetId, String path, String include,
                                         String order, int pageLimit, String token) {
        List<Item> out = new ArrayList<>();
        String cursor = null;
        int safety = 0;
        do {
            Response response = UtilIT.getVersionTree(datasetId, DRAFT, path, pageLimit, cursor,
                    include, order, null, null, token);
            response.then().assertThat().statusCode(OK.getStatusCode());
            JsonPath body = JsonPath.from(response.asString());
            @SuppressWarnings("rawtypes")
            List<Map> raw = body.getList("data.items", Map.class);
            assertTrue(raw.size() <= pageLimit,
                    "Page size " + raw.size() + " exceeds requested limit " + pageLimit);
            for (Map r : raw) {
                out.add(Item.from(r));
            }
            cursor = body.getString("data.nextCursor");
            if (++safety > 1000) {
                throw new IllegalStateException("Paged walk did not terminate within 1000 iterations");
            }
        } while (cursor != null);
        return out;
    }

    private static void assertNoDuplicates(List<Item> items) {
        Set<String> seen = new HashSet<>();
        for (Item item : items) {
            String key = item.type() + ":" + item.path() + ":" + item.id();
            assertTrue(seen.add(key), "Duplicate item in paged walk: " + key);
        }
    }

    private static void assertFoldersBeforeFiles(List<Item> items) {
        boolean seenFile = false;
        for (Item item : items) {
            if ("file".equals(item.type())) {
                seenFile = true;
            } else if ("folder".equals(item.type()) && seenFile) {
                throw new AssertionError("Folder appeared after a file: " + item.path());
            }
        }
    }

    // ---- Tests -----------------------------------------------------------

    @Test
    public void rootListingIsKeysetStableAcrossPageSizes() {
        String token = UtilIT.createRandomUserGetToken();
        int datasetId = createKeysetFixture(token);

        List<Item> oracle = singleShot(datasetId, null, null, null, token);
        // Sanity: 4 top-level folders + 4 root files = 8 items.
        assertEquals(8, oracle.size(), "Expected 8 items at root");
        assertFoldersBeforeFiles(oracle);

        // limit=4 makes the folder listing end exactly at the page boundary
        // (4 folders, page full, files still unseen) — the regression case
        // where the walk used to stop with nextCursor=null and silently drop
        // every root file. limit=1 and limit=2 hit the same boundary on a
        // later page; limit=8 fits everything exactly on one page.
        for (int limit : new int[] {1, 2, 3, 4, 5, 7, 8, 10}) {
            List<Item> paged = pagedWalk(datasetId, null, null, null, limit, token);
            assertNoDuplicates(paged);
            assertEquals(oracle, paged,
                    "Paged walk at limit=" + limit + " diverged from single-shot oracle");
        }
    }

    @Test
    public void nestedFolderListingIsKeysetStableAcrossPageSizes() {
        String token = UtilIT.createRandomUserGetToken();
        int datasetId = createKeysetFixture(token);

        // data/ has 1 subfolder (sub) + 4 files (alpha, Beta, beta, gamma) = 5 items.
        List<Item> oracle = singleShot(datasetId, "data", null, null, token);
        assertEquals(5, oracle.size(), "Expected 5 items in data/");
        assertFoldersBeforeFiles(oracle);

        for (int limit : new int[] {1, 2, 3, 4}) {
            List<Item> paged = pagedWalk(datasetId, "data", null, null, limit, token);
            assertNoDuplicates(paged);
            assertEquals(oracle, paged,
                    "Paged walk of data/ at limit=" + limit + " diverged");
        }
    }

    @Test
    public void deeplyNestedFolderListingIsKeysetStable() {
        String token = UtilIT.createRandomUserGetToken();
        int datasetId = createKeysetFixture(token);

        // data/sub has 5 leaf files, no subfolders.
        List<Item> oracle = singleShot(datasetId, "data/sub", null, null, token);
        assertEquals(5, oracle.size());
        for (Item item : oracle) {
            assertEquals("file", item.type(), "data/sub should contain only files");
        }

        for (int limit : new int[] {1, 2, 3, 4}) {
            List<Item> paged = pagedWalk(datasetId, "data/sub", null, null, limit, token);
            assertNoDuplicates(paged);
            assertEquals(oracle, paged,
                    "Paged walk of data/sub at limit=" + limit + " diverged");
        }
    }

    @Test
    public void descendingOrderIsKeysetStable() {
        String token = UtilIT.createRandomUserGetToken();
        int datasetId = createKeysetFixture(token);

        List<Item> oracle = singleShot(datasetId, null, null, "NameZA", token);
        assertFoldersBeforeFiles(oracle);

        for (int limit : new int[] {1, 2, 3, 5}) {
            List<Item> paged = pagedWalk(datasetId, null, null, "NameZA", limit, token);
            assertNoDuplicates(paged);
            assertEquals(oracle, paged,
                    "NameZA paged walk at limit=" + limit + " diverged");
        }
    }

    @Test
    public void filesOnlyIncludeIsKeysetStable() {
        String token = UtilIT.createRandomUserGetToken();
        int datasetId = createKeysetFixture(token);

        List<Item> oracle = singleShot(datasetId, "docs", "files", null, token);
        // docs/ has 6 files, no subfolders.
        assertEquals(6, oracle.size());
        for (Item item : oracle) {
            assertEquals("file", item.type());
        }

        for (int limit : new int[] {1, 2, 3, 5}) {
            List<Item> paged = pagedWalk(datasetId, "docs", "files", null, limit, token);
            assertNoDuplicates(paged);
            assertEquals(oracle, paged,
                    "include=files paged walk at limit=" + limit + " diverged");
        }
    }

    @Test
    public void foldersOnlyIncludeIsKeysetStable() {
        String token = UtilIT.createRandomUserGetToken();
        int datasetId = createKeysetFixture(token);

        // archive/ has 2 subfolders (2024, 2025), no direct files.
        List<Item> oracle = singleShot(datasetId, "archive", "folders", null, token);
        assertEquals(2, oracle.size());
        for (Item item : oracle) {
            assertEquals("folder", item.type());
        }

        for (int limit : new int[] {1, 2}) {
            List<Item> paged = pagedWalk(datasetId, "archive", "folders", null, limit, token);
            assertNoDuplicates(paged);
            assertEquals(oracle, paged,
                    "include=folders paged walk at limit=" + limit + " diverged");
        }
    }

    @Test
    public void caseInsensitiveOrderingIsStable() {
        String token = UtilIT.createRandomUserGetToken();
        int datasetId = createKeysetFixture(token);

        // Root files include case-clash pairs (A.txt/a.txt, B.txt/b.txt).
        // Order by lower(name); the id tiebreak makes the ordering deterministic.
        List<Item> oracle = singleShot(datasetId, null, "files", null, token);
        // 4 root files.
        assertEquals(4, oracle.size());

        // The case-insensitive sort puts A/a together, then B/b together.
        // We only assert the lower-cased name sequence; the id tiebreak
        // resolution between A.txt and a.txt depends on insertion order.
        List<String> lowerNames = new ArrayList<>();
        for (Item item : oracle) {
            lowerNames.add(item.name().toLowerCase());
        }
        assertEquals(Arrays.asList("a.txt", "a.txt", "b.txt", "b.txt"), lowerNames);

        for (int limit : new int[] {1, 2, 3}) {
            List<Item> paged = pagedWalk(datasetId, null, "files", null, limit, token);
            assertEquals(oracle, paged,
                    "Case-insensitive paged walk at limit=" + limit + " diverged");
        }
    }

    @Test
    public void accessMarkersReflectRestrictedAndEmbargoedSentinels() {
        String token = UtilIT.createRandomUserGetToken();
        int datasetId = createKeysetFixture(token);

        // The 6 docs/* files include 2 restricted and 1 embargoed.
        List<Item> oracle = singleShot(datasetId, "docs", "files", null, token);
        Map<String, String> accessByName = new HashMap<>();
        for (Item item : oracle) {
            accessByName.put(item.name(), item.access());
        }

        assertEquals("public", accessByName.get("01.md"));
        assertEquals("public", accessByName.get("02.md"));
        assertEquals("public", accessByName.get("03.md"));
        assertEquals("restricted", accessByName.get("restricted-readme.md"));
        assertEquals("restricted", accessByName.get("another-restricted.md"));
        assertEquals("embargoed", accessByName.get("embargoed-preview.md"));
    }

    @Test
    public void folderCountsReflectRecursiveFilesAndDirectSubfolders() {
        String token = UtilIT.createRandomUserGetToken();
        int datasetId = createKeysetFixture(token);

        Response response = UtilIT.getVersionTree(datasetId, DRAFT, null, 1000, null,
                "folders", null, null, null, token);
        response.then().assertThat().statusCode(OK.getStatusCode());
        JsonPath body = JsonPath.from(response.asString());

        // archive/ should report 3 files (across 2024 and 2025) and 2 subfolders.
        assertEquals(3, body.getInt("data.items.find { it.name == 'archive' }.counts.files"));
        assertEquals(2, body.getInt("data.items.find { it.name == 'archive' }.counts.folders"));

        // data/ should report 9 files (4 in data/ + 5 in data/sub) and 1 subfolder.
        assertEquals(9, body.getInt("data.items.find { it.name == 'data' }.counts.files"));
        assertEquals(1, body.getInt("data.items.find { it.name == 'data' }.counts.folders"));

        // docs/ should report 6 files (3 plain + 2 restricted + 1 embargoed) and 0 subfolders.
        assertEquals(6, body.getInt("data.items.find { it.name == 'docs' }.counts.files"));
        assertEquals(0, body.getInt("data.items.find { it.name == 'docs' }.counts.folders"));

        // media/ should report 3 files and 0 subfolders.
        assertEquals(3, body.getInt("data.items.find { it.name == 'media' }.counts.files"));
        assertEquals(0, body.getInt("data.items.find { it.name == 'media' }.counts.folders"));
    }

    @Test
    public void cursorRoundtripIsStableAndOpaque() {
        String token = UtilIT.createRandomUserGetToken();
        int datasetId = createKeysetFixture(token);

        // Two parallel walks at different cursors landing on the same items
        // mid-stream. The cursor returned at page-N from limit=1 must point
        // at the same logical position the limit=2 walk lands on after N/2
        // steps.
        Response p1Limit1 = UtilIT.getVersionTree(datasetId, DRAFT, null, 1, null,
                null, null, null, null, token);
        String cursorAfter1 = JsonPath.from(p1Limit1.asString()).getString("data.nextCursor");
        assertNotNull(cursorAfter1);

        Response p2Limit1 = UtilIT.getVersionTree(datasetId, DRAFT, null, 1, cursorAfter1,
                null, null, null, null, token);
        String cursorAfter2 = JsonPath.from(p2Limit1.asString()).getString("data.nextCursor");
        assertNotNull(cursorAfter2);

        // A limit=2 walk's first page should consume the same two items.
        Response combined = UtilIT.getVersionTree(datasetId, DRAFT, null, 2, null,
                null, null, null, null, token);
        String cursorCombined = JsonPath.from(combined.asString()).getString("data.nextCursor");

        // Both cursors point past the same 2 items, so they should resume
        // the same way — the next page must be identical.
        Response resumeFromSerial = UtilIT.getVersionTree(datasetId, DRAFT, null, 5, cursorAfter2,
                null, null, null, null, token);
        Response resumeFromCombined = UtilIT.getVersionTree(datasetId, DRAFT, null, 5, cursorCombined,
                null, null, null, null, token);
        @SuppressWarnings("rawtypes")
        List<Map> serialItems = JsonPath.from(resumeFromSerial.asString())
                .getList("data.items", Map.class);
        @SuppressWarnings("rawtypes")
        List<Map> combinedItems = JsonPath.from(resumeFromCombined.asString())
                .getList("data.items", Map.class);

        List<Item> a = new ArrayList<>();
        for (Map raw : serialItems) {
            a.add(Item.from(raw));
        }
        List<Item> b = new ArrayList<>();
        for (Map raw : combinedItems) {
            b.add(Item.from(raw));
        }
        assertEquals(a, b,
                "Resuming from limit=1 walked-twice cursor and limit=2 single-step cursor "
                        + "must yield identical pages");
    }
}
