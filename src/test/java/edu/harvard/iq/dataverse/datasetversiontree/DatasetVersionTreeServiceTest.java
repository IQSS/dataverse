package edu.harvard.iq.dataverse.datasetversiontree;

import edu.harvard.iq.dataverse.DatasetVersion;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The parts of {@link DatasetVersionTreeService} that never reach the
 * database: path normalisation, enum parsing, cursor encoding. The SQL
 * paginator is covered by {@code DatasetVersionTreeKeysetIT}.
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
    void normalizesPathLikeTheWriteSideSanitizer() {
        // The read side mirrors what FileMetadata#setDirectoryLabel stores
        // (StringUtil.sanitizeFileDirectory): backslash runs become slashes
        // and leading '/', '-', '.', ' ' are stripped — a stored label's
        // first segment can never start with those.
        assertEquals("data/sub", DatasetVersionTreeService.normalizePath("data\\sub"));
        assertEquals("hidden", DatasetVersionTreeService.normalizePath(".hidden"));
    }

    @Test
    void normalizesPathPreservesFolderTails() {
        // Inner segments may end in '.', ' ' or '-'; only trailing slashes are dropped.
        assertEquals("data.", DatasetVersionTreeService.normalizePath("data."));
        assertEquals("data. ", DatasetVersionTreeService.normalizePath("data. "));
        assertEquals("data", DatasetVersionTreeService.normalizePath("data/"));
        assertEquals("data.", DatasetVersionTreeService.normalizePath("data./"));
    }

    @Test
    void normalizesPathIsIdempotent() {
        // The endpoint normalizes once up front and the service normalizes
        // again; the two results must be identical. Junk/slash alternations
        // are the trap: a single-pass strip would leave "./data" as "/data"
        // on the first run and "data" on the second.
        for (String raw : new String[]{"./data", "/.data", "data./", "/data//sub/", ".hidden"}) {
            String once = DatasetVersionTreeService.normalizePath(raw);
            assertEquals(once, DatasetVersionTreeService.normalizePath(once),
                    "not idempotent for: " + raw);
        }
        assertEquals("data", DatasetVersionTreeService.normalizePath("./data"));
    }

    @Test
    void junkOnlyPathsAreRejectedNotAliasedToRoot() {
        // A non-blank path that normalizes to nothing names nothing storable;
        // mapping it to "" would silently return the ROOT listing for
        // requests like path=".." — a phantom folder holding the whole root.
        assertThrows(DatasetVersionTreeService.InvalidQueryException.class,
                () -> DatasetVersionTreeService.normalizePath(".."));
        assertThrows(DatasetVersionTreeService.InvalidQueryException.class,
                () -> DatasetVersionTreeService.normalizePath("-"));
        assertThrows(DatasetVersionTreeService.InvalidQueryException.class,
                () -> DatasetVersionTreeService.normalizePath(". "));
        assertThrows(DatasetVersionTreeService.InvalidQueryException.class,
                () -> DatasetVersionTreeService.normalizePath("../."));
        // ...while pure root spellings stay valid.
        assertEquals("", DatasetVersionTreeService.normalizePath("//"));
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
                () -> svc.listChildren(version, new DatasetVersionTreeService.TreeQuery(null, null, "not-a-real-cursor",
                        DatasetVersionTreeService.Include.ALL,
                        DatasetVersionTreeService.Order.NAME_AZ, false)));
    }

    @Test
    void malformedCursorPayloadsRejectedAsInvalidNotServerError() {
        // Structurally-valid Base64+JSON whose members have the wrong types
        // or shape must surface as InvalidQueryException (the 400 path), not
        // leak a ClassCastException / ArithmeticException out of
        // decodeCursor as a 500.
        for (String json : List.of(
                "{\"p\":\"FOLDERS\",\"k\":[1],\"c\":5}",         // number where the folder name belongs
                "{\"p\":\"FILES\",\"k\":[\"a\",\"b\"],\"c\":5}", // string where the file id belongs
                "{\"p\":\"FILES\",\"k\":[\"a\",1.5],\"c\":5}",   // fractional file id
                "{\"p\":\"FILES\",\"k\":\"a\",\"c\":5}",         // k is not an array
                "{\"p\":\"FILES\",\"k\":[],\"c\":5}",            // files phase needs its keyset pair
                "{\"p\":\"FOLDERS\",\"k\":[],\"c\":5}",          // folders phase needs its key
                "{\"p\":\"NOPE\",\"k\":[],\"c\":5}",             // unknown phase marker
                "{\"p\":\"FOLDERS\",\"k\":[\"data\"]}",          // count snapshot missing — never minted by this server
                "{\"p\":\"FOLDERS\",\"k\":[\"data\"],\"c\":-3}", // negative count snapshot
                "[1,2]"                                          // not a JSON object at all
        )) {
            String cursor = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
            assertThrows(DatasetVersionTreeService.InvalidQueryException.class,
                    () -> DatasetVersionTreeService.decodeCursor(cursor),
                    "should reject: " + json);
        }
    }

    @Test
    void cursorRoundTrips() {
        // Cursors are minted key-first and stamped with the walk's count
        // snapshot just before encoding, exactly as listChildren does it.
        var folders = DatasetVersionTreeService.TreeCursor.folders("Data \"2024\" – café")
                .withApproximateCount(42);
        assertEquals(folders,
                DatasetVersionTreeService.decodeCursor(DatasetVersionTreeService.encodeCursor(folders)));

        var files = DatasetVersionTreeService.TreeCursor.files("a \\\"quoted\\\" name.txt", 7L)
                .withApproximateCount(42);
        assertEquals(files,
                DatasetVersionTreeService.decodeCursor(DatasetVersionTreeService.encodeCursor(files)));
    }
}
