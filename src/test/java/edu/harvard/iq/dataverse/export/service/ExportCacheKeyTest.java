package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportCacheKeyTest {
    
    private static final String FORMAT = "dvn";
    private static final String DATASET_ID = "42";
    private static final Long DATASET_ID_LONG = 42L;
    private static final String FRIENDLY_VERSION = "1.0";
    
    /**
     * Creates a mocked DatasetVersion with the given dataset id and friendly version.
     */
    private static DatasetVersion mockVersion(Long datasetId, String friendlyVersion) {
        Dataset dataset = mock(Dataset.class);
        when(dataset.getId()).thenReturn(datasetId);
        
        DatasetVersion version = mock(DatasetVersion.class);
        when(version.getDataset()).thenReturn(dataset);
        when(version.getFriendlyVersionNumber()).thenReturn(friendlyVersion);
        return version;
    }
    
    @Nested
    class CanonicalConstructor {
        
        @Test
        void createsKeyWithValidArguments() {
            ExportCacheKey key = new ExportCacheKey(FORMAT, DATASET_ID, FRIENDLY_VERSION);
            
            assertAll(
                () -> assertEquals(FORMAT, key.formatName()),
                () -> assertEquals(DATASET_ID, key.datasetId()),
                () -> assertEquals(FRIENDLY_VERSION, key.friendlyVersion())
            );
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void rejectsInvalidFormatName(String invalid) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ExportCacheKey(invalid, DATASET_ID, FRIENDLY_VERSION));
            assertTrue(ex.getMessage().contains("formatName"));
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void rejectsInvalidDatasetId(String invalid) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ExportCacheKey(FORMAT, invalid, FRIENDLY_VERSION));
            assertTrue(ex.getMessage().contains("datasetId"));
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void rejectsInvalidFriendlyVersion(String invalid) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ExportCacheKey(FORMAT, DATASET_ID, invalid));
            assertTrue(ex.getMessage().contains("friendlyVersion"));
        }
    }
    
    @Nested
    class VersionConstructor {
        
        @Test
        void createsKeyFromValidVersion() {
            DatasetVersion version = mockVersion(DATASET_ID_LONG, FRIENDLY_VERSION);
            
            ExportCacheKey key = new ExportCacheKey(version, FORMAT);
            
            assertAll(
                () -> assertEquals(FORMAT, key.formatName()),
                () -> assertEquals("42", key.datasetId()),
                () -> assertEquals(FRIENDLY_VERSION, key.friendlyVersion())
            );
        }
        
        @Test
        void rejectsNullVersion() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ExportCacheKey(null, FORMAT));
            assertTrue(ex.getMessage().contains("version"));
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void rejectsInvalidFormatName(String invalid) {
            DatasetVersion version = mockVersion(DATASET_ID_LONG, FRIENDLY_VERSION);
            
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ExportCacheKey(version, invalid));
            assertTrue(ex.getMessage().contains("formatName"));
        }
        
        @Test
        void rejectsVersionWithNullDataset() {
            DatasetVersion version = mock(DatasetVersion.class);
            when(version.getFriendlyVersionNumber()).thenReturn(FRIENDLY_VERSION);
            when(version.getDataset()).thenReturn(null);
            
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ExportCacheKey(version, FORMAT));
            assertTrue(ex.getMessage().contains("dataset must not be null"));
        }
        
        @Test
        void rejectsDatasetWithNullId() {
            DatasetVersion version = mockVersion(null, FRIENDLY_VERSION);
            
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ExportCacheKey(version, FORMAT));
            assertTrue(ex.getMessage().contains("non-null ID"));
        }
        
        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -42L})
        void rejectsDatasetWithNonPositiveId(long invalidId) {
            DatasetVersion version = mockVersion(invalidId, FRIENDLY_VERSION);
            
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ExportCacheKey(version, FORMAT));
            assertTrue(ex.getMessage().contains("greater 0"));
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void rejectsVersionWithBlankFriendlyVersion(String invalid) {
            DatasetVersion version = mockVersion(DATASET_ID_LONG, invalid);
            
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ExportCacheKey(version, FORMAT));
            assertTrue(ex.getMessage().contains("friendlyVersion"));
        }
    }
    
    @Nested
    class AuxTag {
        
        @Test
        void buildsCanonicalVersionQualifiedTag() {
            ExportCacheKey key = new ExportCacheKey(FORMAT, DATASET_ID, FRIENDLY_VERSION);
            
            assertEquals("export_dvn_1.0.cached", key.auxTag());
        }
        
        @Test
        void tagUsesConfiguredPrefixAndSuffix() {
            ExportCacheKey key = new ExportCacheKey("csv", DATASET_ID, "2.3");
            
            String tag = key.auxTag();
            assertAll(
                () -> assertTrue(tag.startsWith(ExportCacheKey.TAG_PREFIX)),
                () -> assertTrue(tag.endsWith(ExportCacheKey.TAG_SUFFIX)),
                () -> assertEquals(ExportCacheKey.TAG_PREFIX + "csv_2.3" + ExportCacheKey.TAG_SUFFIX, tag)
            );
        }
    }
    
    @Nested
    class RecordSemantics {
        @Test
        void keyFromEntityEqualsManuallyConstructedKey() {
            DatasetVersion version = mockVersion(DATASET_ID_LONG, FRIENDLY_VERSION);
            
            assertEquals(
                new ExportCacheKey(FORMAT, DATASET_ID, FRIENDLY_VERSION),
                new ExportCacheKey(version, FORMAT)
            );
        }
    }
    
    @Test
    @DisplayName("Key survives serialization round-trip")
    void isSerializable() throws Exception {
        ExportCacheKey original = new ExportCacheKey(FORMAT, DATASET_ID, FRIENDLY_VERSION);
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        ExportCacheKey deserialized;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            deserialized = (ExportCacheKey) in.readObject();
        }
        
        assertEquals(original, deserialized);
    }
}