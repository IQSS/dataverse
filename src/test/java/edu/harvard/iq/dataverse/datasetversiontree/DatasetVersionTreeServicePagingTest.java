package edu.harvard.iq.dataverse.datasetversiontree;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.FileItem;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.FolderItem;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.Include;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.Order;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.TreePage;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.TreeQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Paging logic of {@link DatasetVersionTreeService} against a mocked
 * EntityManager: the SQL is captured and answered from canned rows, so the
 * page assembly, cursors and counts can be checked without a database.
 */
@ExtendWith(MockitoExtension.class)
class DatasetVersionTreeServicePagingTest {

    @Mock
    EntityManager em;

    @InjectMocks
    DatasetVersionTreeService svc = new DatasetVersionTreeService();

    private final List<String> executedSql = new ArrayList<>();
    private final List<List<Object>> boundParams = new ArrayList<>();
    private List<Object[]> folderRows = List.<Object[]>of();
    private List<Object[]> fileRows = List.<Object[]>of();
    private long folderCount;
    private long fileCount;

    @BeforeEach
    void wireQueries() {
        lenient().when(em.createNativeQuery(anyString())).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            executedSql.add(sql);
            List<Object> params = new ArrayList<>();
            boundParams.add(params);
            Query q = mock(Query.class);
            lenient().when(q.setParameter(anyInt(), any())).thenAnswer(p -> {
                params.add(p.getArgument(1));
                return q;
            });
            if (sql.startsWith("SELECT COUNT(DISTINCT")) {
                lenient().when(q.getSingleResult()).thenReturn(folderCount);
            } else if (sql.startsWith("SELECT COUNT(*)")) {
                lenient().when(q.getSingleResult()).thenReturn(fileCount);
            } else if (sql.contains("GROUP BY")) {
                lenient().when(q.getResultList()).thenAnswer(a -> page(folderRows, sql, params, 2));
            } else {
                lenient().when(q.getResultList()).thenAnswer(a -> page(fileRows, sql, params, 3));
            }
            return q;
        });
    }

    /** Applies the keyset predicate (rows after the cursor key) and the LIMIT, as the database would. */
    private static List<Object[]> page(List<Object[]> rows, String sql, List<Object> params, int afterOffset) {
        int limit = (Integer) params.get(params.size() - 1);
        List<Object[]> candidates = rows;
        if (sql.contains("AND (lower(")) {
            String after = ((String) params.get(params.size() - 1 - afterOffset)).toLowerCase();
            candidates = rows.stream()
                    .filter(r -> ((String) r[0]).toLowerCase().compareTo(after) > 0)
                    .collect(Collectors.toList());
        }
        return candidates.subList(0, Math.min(limit, candidates.size()));
    }

    private static Object[] folder(String name, long files, long folders) {
        return new Object[]{name, files, folders, 100L * files, 0L, 0L, 0L};
    }

    private static Object[] file(String label, long id, String checksumType) {
        return new Object[]{label, id, 42L, "text/plain", "public", checksumType, "abc"};
    }

    private static DatasetVersion version() {
        DatasetVersion v = new DatasetVersion();
        v.setId(7L);
        return v;
    }

    private TreePage list(String path, Integer limit, String cursor, Include include, boolean originals) {
        return svc.listChildren(version(), new TreeQuery(path, limit, cursor, include, Order.NAME_AZ, originals));
    }

    private List<String> names(TreePage page) {
        return page.items.stream().map(i -> i.name).collect(Collectors.toList());
    }

    @Test
    void singlePageListsFoldersThenFilesWithoutCountQueries() {
        folderRows = List.<Object[]>of(folder("data", 3, 1), folder("docs", 2, 0));
        fileRows = List.<Object[]>of(file("a.txt", 1, "MD5"), file("b.txt", 2, null));

        TreePage page = list(null, 10, null, Include.ALL, false);

        assertEquals(List.of("data", "docs", "a.txt", "b.txt"), names(page));
        assertNull(page.nextCursor);
        assertEquals(4, page.approximateCount);
        assertEquals(2, executedSql.size(), "no count queries on a complete page");
        FolderItem data = (FolderItem) page.items.get(0);
        assertEquals("data", data.path);
        assertEquals(300L, data.bytes);
        FileItem a = (FileItem) page.items.get(2);
        assertEquals("a.txt", a.path);
        assertEquals("MD5", a.checksumType);
        assertEquals("/api/access/datafile/1", a.downloadUrl);
        assertNull(((FileItem) page.items.get(3)).checksumType);
    }

    @Test
    void truncatedFolderListingMintsFolderCursorAndCountsTheRest() {
        folderRows = List.<Object[]>of(folder("a", 1, 0), folder("b", 1, 0), folder("c", 1, 0));
        fileRows = List.<Object[]>of(file("z.txt", 9, "SHA1"));
        folderCount = 3;
        fileCount = 1;

        TreePage page = list("", 2, null, Include.ALL, false);

        assertEquals(List.of("a", "b"), names(page));
        assertNotNull(page.nextCursor);
        assertEquals(4, page.approximateCount);
        assertTrue(executedSql.stream().anyMatch(s -> s.startsWith("SELECT COUNT(DISTINCT")));
        assertTrue(executedSql.stream().anyMatch(s -> s.startsWith("SELECT COUNT(*)")));

        TreePage next = list("", 2, page.nextCursor, Include.ALL, false);
        assertEquals(List.of("c", "z.txt"), names(next));
        assertNull(next.nextCursor);
        assertEquals(4, next.approximateCount, "snapshot carried in the cursor");
    }

    @Test
    void foldersFillingThePageExactlyStillYieldACursorWhenFilesRemain() {
        folderRows = List.<Object[]>of(folder("a", 1, 0), folder("b", 1, 0));
        fileRows = List.<Object[]>of(file("z.txt", 9, "SHA1"));
        fileCount = 1;

        TreePage page = list("", 2, null, Include.ALL, false);

        assertEquals(List.of("a", "b"), names(page));
        assertNotNull(page.nextCursor, "files exist behind the boundary");

        TreePage next = list("", 2, page.nextCursor, Include.ALL, false);
        assertEquals(List.of("z.txt"), names(next));
        assertNull(next.nextCursor);
    }

    @Test
    void includeFiltersSkipTheOtherQuery() {
        folderRows = List.<Object[]>of(folder("a", 1, 0));
        fileRows = List.<Object[]>of(file("z.txt", 9, "SHA1"));
        folderCount = 1;
        fileCount = 1;

        assertEquals(List.of("z.txt"), names(list("", 10, null, Include.FILES, false)));
        assertFalse(executedSql.stream().anyMatch(s -> s.contains("GROUP BY")));

        executedSql.clear();
        assertEquals(List.of("a"), names(list("", 10, null, Include.FOLDERS, false)));
        assertFalse(executedSql.stream().anyMatch(s -> s.contains("fm.label")));
    }

    @Test
    void nestedPathEscapesLikeWildcardsAndPrefixesItemPaths() {
        folderRows = List.<Object[]>of(folder("sub", 1, 0));
        fileRows = List.<Object[]>of(file("f.txt", 3, "SHA256"));

        TreePage page = list("a_b/c%", 10, null, Include.ALL, true);

        assertEquals("a_b/c%/sub", page.items.get(0).path);
        assertEquals("a_b/c%/f.txt", page.items.get(1).path);
        assertEquals("/api/access/datafile/3?format=original", ((FileItem) page.items.get(1)).downloadUrl);
        assertEquals("SHA-256", ((FileItem) page.items.get(1)).checksumType);
        assertTrue(executedSql.get(0).contains("ESCAPE"));
        assertEquals("a\\_b/c\\%/%", boundParams.get(0).get(1));
    }

    @Test
    void keysetClausesUseTheCursorKeys() {
        folderRows = List.<Object[]>of(folder("b", 1, 0));
        fileRows = List.<Object[]>of(file("y.txt", 2, "MD5"));
        String folderCursor = DatasetVersionTreeService.encodeCursor(
                DatasetVersionTreeService.TreeCursor.folders("a").withApproximateCount(3));
        String fileCursor = DatasetVersionTreeService.encodeCursor(
                DatasetVersionTreeService.TreeCursor.files("x.txt", 1).withApproximateCount(3));

        list("", 10, folderCursor, Include.ALL, false);
        assertTrue(boundParams.get(0).contains("a"));

        executedSql.clear();
        boundParams.clear();
        list("", 10, fileCursor, Include.ALL, false);
        assertFalse(executedSql.get(0).contains("GROUP BY"), "file-phase cursor skips folders");
        assertTrue(boundParams.get(0).contains("x.txt"));
        assertTrue(boundParams.get(0).contains(1L));
    }
}
