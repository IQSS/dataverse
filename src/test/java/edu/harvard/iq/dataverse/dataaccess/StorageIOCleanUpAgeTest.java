package edu.harvard.iq.dataverse.dataaccess;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The age guard that keeps cleanUp from removing an upload that has finished but has
 * not been registered as a DataFile yet.
 */
class StorageIOCleanUpAgeTest {

    private static final Duration ONE_WEEK = Duration.ofDays(7);

    @Test
    void testIsOlderThan_unknownTimestampIsNeverRemoved() {
        assertFalse(StorageIO.isOlderThan(null, ONE_WEEK));
    }

    @Test
    void testIsOlderThan_justUploadedIsKept() {
        assertFalse(StorageIO.isOlderThan(Instant.now(), ONE_WEEK));
    }

    @Test
    void testIsOlderThan_withinGracePeriodIsKept() {
        assertFalse(StorageIO.isOlderThan(Instant.now().minus(Duration.ofDays(6)), ONE_WEEK));
    }

    @Test
    void testIsOlderThan_justInsideGracePeriodIsKept() {
        assertFalse(StorageIO.isOlderThan(Instant.now().minus(Duration.ofDays(7)).plusSeconds(30), ONE_WEEK));
    }

    @Test
    void testIsOlderThan_pastGracePeriodIsRemovable() {
        assertTrue(StorageIO.isOlderThan(Instant.now().minus(Duration.ofDays(8)), ONE_WEEK));
    }

    @Test
    void testIsOlderThan_longAbandonedIsRemovable() {
        assertTrue(StorageIO.isOlderThan(Instant.now().minus(Duration.ofDays(365)), ONE_WEEK));
    }

    @Test
    void testIsOlderThan_zeroGracePeriodRemovesAnythingWithATimestamp() {
        assertTrue(StorageIO.isOlderThan(Instant.now().minusSeconds(1), Duration.ZERO));
    }

    private static Map<String, Instant> stored(Object... nameThenAge) {
        Map<String, Instant> m = new HashMap<>();
        for (int i = 0; i < nameThenAge.length; i += 2) {
            Duration age = (Duration) nameThenAge[i + 1];
            m.put((String) nameThenAge[i], age == null ? null : Instant.now().minus(age));
        }
        return m;
    }

    private static final Predicate<String> ORPHANS = name -> name.startsWith("orphan");

    @Test
    void testSelectForCleanUp_takesOnlyOldOrphans() {
        Map<String, Instant> stored = stored(
                "orphan-old", Duration.ofDays(30),
                "orphan-fresh", Duration.ofHours(1),
                "referenced-old", Duration.ofDays(30));

        List<String> selected = StorageIO.selectForCleanUp(stored, ORPHANS, ONE_WEEK);

        assertEquals(List.of("orphan-old"), selected);
    }

    @Test
    void testSelectForCleanUp_skipsUnknownTimestamps() {
        List<String> selected = StorageIO.selectForCleanUp(stored("orphan-a", null), ORPHANS, ONE_WEEK);

        assertTrue(selected.isEmpty());
    }

    @Test
    void testSelectForCleanUp_emptyStoreSelectsNothing() {
        assertTrue(StorageIO.selectForCleanUp(Map.of(), ORPHANS, ONE_WEEK).isEmpty());
    }

    @Test
    void testSelectForCleanUp_zeroGraceStillHonoursTheFilter() {
        Map<String, Instant> stored = stored(
                "orphan-a", Duration.ofSeconds(5),
                "referenced-b", Duration.ofSeconds(5));

        List<String> selected = StorageIO.selectForCleanUp(stored, ORPHANS, Duration.ZERO);

        assertEquals(List.of("orphan-a"), selected);
    }

    @Test
    void testSelectForCleanUp_resultIsNotMeantToBeModified() {
        List<String> selected = StorageIO.selectForCleanUp(stored("orphan-a", Duration.ofDays(30)), ORPHANS, ONE_WEEK);

        assertThrows(UnsupportedOperationException.class, () -> selected.add("x"));
    }
}
