package edu.harvard.iq.dataverse.datasetversiontree;

import edu.harvard.iq.dataverse.DatasetVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pure-function unit tests for {@link DatasetVersionTreeService}.
 *
 * <p>The full paginator (SQL-keyset) is exercised end-to-end by
 * {@code DatasetVersionTreeKeysetIT}: an in-memory paginator runs as
 * the oracle on the same fixture as the real SQL queries, and the two
 * are asserted page-by-page identical. That's the regression net for
 * sort order, folder counts, cursor stability, restricted/embargoed
 * /deaccessioned semantics, and the {@code originals=true} download-url
 * flip.
 *
 * <p>What stays here are the helpers that don't reach the database —
 * path normalisation, enum parsing, and the cursor's {@code 400}
 * behaviour (decoded before any DB access).
 */
class DatasetVersionTreeServiceTest {

    private final DatasetVersionTreeService svc = new DatasetVersionTreeService();

    @Test
    void normalizesPath() {
        assertEquals("data/sub", DatasetVersionTreeService.normalizePath("/data//sub///"));
        assertEquals("", DatasetVersionTreeService.normalizePath("/"));
        assertEquals("", DatasetVersionTreeService.normalizePath(null));
    }

    @Test
    void orderFromQueryRejectsBogus() {
        assertThrows(DatasetVersionTreeService.InvalidQueryException.class,
                () -> DatasetVersionTreeService.Order.fromQuery("Bogus"));
    }

    @Test
    void orderFromQueryAcceptsKnownValues() {
        assertEquals(DatasetVersionTreeService.Order.NAME_AZ,
                DatasetVersionTreeService.Order.fromQuery("NameAZ"));
        assertEquals(DatasetVersionTreeService.Order.NAME_ZA,
                DatasetVersionTreeService.Order.fromQuery("NameZA"));
        assertEquals(DatasetVersionTreeService.Order.NAME_AZ,
                DatasetVersionTreeService.Order.fromQuery(null));
    }

    @Test
    void includeFromQueryAcceptsKnownValuesCaseInsensitive() {
        assertEquals(DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Include.fromQuery(null));
        assertEquals(DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Include.fromQuery("all"));
        assertEquals(DatasetVersionTreeService.Include.FOLDERS,
                DatasetVersionTreeService.Include.fromQuery("FOLDERS"));
        assertEquals(DatasetVersionTreeService.Include.FILES,
                DatasetVersionTreeService.Include.fromQuery("FiLeS"));
    }

    @Test
    void includeFromQueryRejectsBogus() {
        assertThrows(DatasetVersionTreeService.InvalidQueryException.class,
                () -> DatasetVersionTreeService.Include.fromQuery("everything"));
    }

    @Test
    void invalidCursorRejected() {
        // Cursor is decoded before any DB access, so we don't need an
        // EntityManager (or a populated DatasetVersion) to verify the
        // 400 path.
        DatasetVersion version = new DatasetVersion();
        assertThrows(DatasetVersionTreeService.InvalidQueryException.class,
                () -> svc.listChildren(version, null, null, "not-a-real-cursor",
                        DatasetVersionTreeService.Include.ALL,
                        DatasetVersionTreeService.Order.NAME_AZ, false));
    }
}
