package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.Embargo;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.export.DDIExporter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit test using real entities: the invalidator only reads plain getters, so mocks would add noise.
 * Covered: argument validation, the (deliberately) DDI-only format gate, the version-state gate and the
 * strict "embargo ended after the last export and before today" window. Export rendering is out of scope.
 * <p>
 * Cache keys are built via the canonical record constructor: the invalidator only consults the format name,
 * and decoupling the key from the entity fixtures keeps the key's own validation out of the way.
 */
class FileEmbargoExpiryInvalidatorTest {
    
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final LocalDate TODAY = LocalDate.now(ZONE);
    /** Days between the fixture's last export and today; the CSV offsets below are relative to this. */
    private static final int EXPORT_AGE_DAYS = 10;
    private static final String NON_DDI = "dataverse_json";
    private static final ExportCacheKey DDI_KEY = keyFor(DDIExporter.PROVIDER_NAME);
    
    private final FileEmbargoExpiryInvalidator invalidator = new FileEmbargoExpiryInvalidator();
    
    @Nested
    class ArgumentValidation {
        
        @Test
        void rejectsNullVersion() {
            assertThrows(IllegalArgumentException.class, () -> invalidator.isStale(null, DDI_KEY));
        }
        
        @Test
        void rejectsNullKey() {
            DatasetVersion version = version(VersionState.RELEASED, toDate(TODAY.minusDays(EXPORT_AGE_DAYS)));
            assertThrows(IllegalArgumentException.class, () -> invalidator.isStale(version, null));
        }
        
        @Test
        void rejectsVersionWithoutDatasetForDdi() {
            DatasetVersion orphan = version(VersionState.RELEASED, toDate(TODAY.minusDays(EXPORT_AGE_DAYS)));
            orphan.setDataset(null);
            
            assertThrows(IllegalArgumentException.class, () -> invalidator.isStale(orphan, DDI_KEY));
        }
    }
    
    @Nested
    class FormatGate {
        
        /** Format names are compared case-sensitively, hence "DDI" is treated as a foreign format. */
        @ParameterizedTest
        @ValueSource(strings = {NON_DDI, "oai_dc", "DDI"})
        void nonDdiFormatsAreNeverStaleEvenWithExpiredEmbargo(String formatName) {
            DatasetVersion version = version(VersionState.RELEASED, toDate(TODAY.minusDays(EXPORT_AGE_DAYS)), expiredEmbargo());
            assertFalse(invalidator.isStale(version, keyFor(formatName)));
        }
        
        @Test
        void nonDdiFormatsShortCircuitBeforeInspectingTheVersion() {
            DatasetVersion orphan = version(VersionState.RELEASED, toDate(TODAY.minusDays(EXPORT_AGE_DAYS)));
            orphan.setDataset(null);
            
            assertFalse(invalidator.isStale(orphan, keyFor(NON_DDI)));
        }
    }
    
    @Nested
    class VersionStateGate {
        
        @ParameterizedTest
        @EnumSource(value = VersionState.class, names = {"RELEASED", "ARCHIVED"})
        void publishedVersionsAreInspected(VersionState state) {
            DatasetVersion version = version(state, toDate(TODAY.minusDays(EXPORT_AGE_DAYS)), expiredEmbargo());
            assertTrue(invalidator.isStale(version, DDI_KEY));
        }
        
        @ParameterizedTest
        @EnumSource(value = VersionState.class, names = {"RELEASED", "ARCHIVED"}, mode = EnumSource.Mode.EXCLUDE)
        void otherVersionsAreNeverStale(VersionState state) {
            DatasetVersion version = version(state, toDate(TODAY.minusDays(EXPORT_AGE_DAYS)), expiredEmbargo());
            assertFalse(invalidator.isStale(version, DDI_KEY));
        }
    }
    
    @Nested
    class EmbargoEvaluation {
        
        @Test
        void neverExportedVersionIsNotStale() {
            DatasetVersion version = version(VersionState.RELEASED, null, expiredEmbargo());
            assertFalse(invalidator.isStale(version, DDI_KEY));
        }
        
        @Test
        void versionWithoutFilesIsNotStale() {
            assertFalse(invalidator.isStale(version(VersionState.RELEASED, toDate(TODAY.minusDays(EXPORT_AGE_DAYS))), DDI_KEY));
        }
        
        @Test
        void filesWithoutEmbargoAreNotStale() {
            assertFalse(invalidator.isStale(version(VersionState.RELEASED, toDate(TODAY.minusDays(EXPORT_AGE_DAYS)), null, null), DDI_KEY));
        }
        
        @ParameterizedTest(name = "embargo ends {0} day(s) before today -> stale = {1}")
        @CsvSource({
            "15, false",  // ended before the last export: already reflected in the cache
            "10, false",  // ended on the export day: not strictly after the export
            " 5, true",   // ended between export and today
            " 1, true",   // ended yesterday
            " 0, false",  // ends today: not strictly before today
            "-5, false"   // still active
        })
        void onlyEmbargoesEndingStrictlyBetweenExportAndTodayAreStale(int daysBeforeToday, boolean expected) {
            DatasetVersion version = version(VersionState.RELEASED, toDate(TODAY.minusDays(EXPORT_AGE_DAYS)), embargoEnding(TODAY.minusDays(daysBeforeToday)));
            assertEquals(expected, invalidator.isStale(version, DDI_KEY));
        }
        
        @Test
        void expiredEmbargoOnAnyFileMarksVersionStale() {
            DatasetVersion version = version(VersionState.RELEASED, toDate(TODAY.minusDays(EXPORT_AGE_DAYS)),
                null, embargoEnding(TODAY.plusDays(30)), expiredEmbargo());
            assertTrue(invalidator.isStale(version, DDI_KEY));
        }
        
        @Test
        void embargoSharedByMultipleFilesIsStillDetected() {
            Embargo shared = expiredEmbargo();
            assertTrue(invalidator.isStale(version(VersionState.RELEASED, toDate(TODAY.minusDays(EXPORT_AGE_DAYS)), shared, shared), DDI_KEY));
        }
    }
    
    // ++++ ++++ ++++ FIXTURES ++++ ++++ ++++
    
    private static final AtomicLong EMBARGO_IDS = new AtomicLong();
    
    private static DatasetVersion version(VersionState state, Date lastExportTime, Embargo... embargoes) {
        Dataset dataset = new Dataset();
        dataset.setLastExportTime(lastExportTime);
        
        DatasetVersion version = new DatasetVersion();
        version.setDataset(dataset);
        version.setVersionState(state);
        version.setFileMetadatas(new ArrayList<>());
        for (Embargo embargo : embargoes) {
            version.getFileMetadatas().add(fileWith(embargo, version));
        }
        return version;
    }
    
    private static FileMetadata fileWith(Embargo embargo, DatasetVersion version) {
        DataFile file = new DataFile();
        file.setEmbargo(embargo);
        FileMetadata fm = new FileMetadata();
        fm.setDataFile(file);
        fm.setDatasetVersion(version);
        return fm;
    }
    
    /** Distinct ids matter: the invalidator skips embargoes whose id it has already seen. */
    private static Embargo embargoEnding(LocalDate dateAvailable) {
        Embargo embargo = new Embargo(dateAvailable, "test");
        embargo.setId(EMBARGO_IDS.incrementAndGet());
        return embargo;
    }
    
    /** Embargo that ended halfway between the last export and today. */
    private static Embargo expiredEmbargo() {
        return embargoEnding(TODAY.minusDays(EXPORT_AGE_DAYS / 2));
    }
    
    /** Dataset id and version are arbitrary: the invalidator only reads the format name from the key. */
    private static ExportCacheKey keyFor(String formatName) {
        return new ExportCacheKey(formatName, "42", "1.0");
    }
    
    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZONE).toInstant());
    }
}