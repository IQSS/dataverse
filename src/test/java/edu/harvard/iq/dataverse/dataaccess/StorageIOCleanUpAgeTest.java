package edu.harvard.iq.dataverse.dataaccess;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
