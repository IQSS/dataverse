package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.export.service.ExportCache.ExportStreamWriter;
import io.gdcc.spi.export.ExportException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageIOCacheTest {
    
    private static final ExportCacheKey KEY = new ExportCacheKey("ddi", "42", "1.0");
    private static final String TAG = KEY.auxTag();
    private static final byte[] PAYLOAD = "<codeBook/>".getBytes(StandardCharsets.UTF_8);
    
    @Mock
    private StorageIO<Dataset> storage;
    // MockedStatic necessary to mock static methods! (We can't inject a mock for the method call)
    private MockedStatic<DataAccess> dataAccess;
    private Dataset dataset;
    private StorageIOCache cache;
    
    @BeforeEach
    void setUp() {
        dataset = new Dataset();
        dataset.setId(42L);
        cache = new StorageIOCache();
        // Actual mocking behavior must be set up individually before any tests are run, as we may not create
        // stubs when not using them (Mockito's strict stubbing policy applies!).
        dataAccess = mockStatic(DataAccess.class);
    }
    
    @AfterEach
    void tearDown() {
        dataAccess.close();
    }
    
    // Wires the mocked storage into the static lookup. Called only by tests that actually reach storage (strict stubs).
    private void givenStorageResolves() {
        dataAccess.when(() -> DataAccess.getStorageIO(dataset)).thenReturn(storage);
    }
    
    // Simple interface to allow method lambdas as test parameters
    @FunctionalInterface
    interface CacheOperation {
        void run(StorageIOCache cache, Dataset dataset) throws Exception;
    }
    
    static Stream<Arguments> allOperations() {
        return Stream.of(
            arguments("read", (CacheOperation) (c, d) -> c.read(d, KEY)),
            arguments("write", (CacheOperation) (c, d) -> c.write(d, KEY, out -> out.write(PAYLOAD))),
            arguments("evict", (CacheOperation) (c, d) -> c.evict(d, KEY)),
            arguments("evictAll", (CacheOperation) StorageIOCache::evictAll),
            arguments("usedStorage", (CacheOperation) StorageIOCache::usedStorage)
        );
    }
    
    @ParameterizedTest(name = "{0}() propagates storage lookup failure")
    @MethodSource("allOperations")
    void storageLookupFailurePropagates(String name, CacheOperation operation) {
        dataAccess.when(() -> DataAccess.getStorageIO(dataset)).thenThrow(new IOException("no driver"));
        assertThrows(IOException.class, () -> operation.run(cache, dataset));
    }
    
    @Nested
    class Reads {
        
        @BeforeEach
        void setupMocks() {
            givenStorageResolves();
        }
        
        @Test
        void returnsStreamOnCacheHit() throws IOException {
            InputStream cached = InputStream.nullInputStream();
            when(storage.isAuxObjectCached(TAG)).thenReturn(true);
            when(storage.getAuxFileAsInputStream(TAG)).thenReturn(cached);
            
            Optional<InputStream> result = cache.read(dataset, KEY);
            
            assertSame(cached, result.orElseThrow(), "the storage stream is handed through untouched");
        }
        
        @Test
        void returnsEmptyOnCacheMissWithoutOpeningStream() throws IOException {
            when(storage.isAuxObjectCached(TAG)).thenReturn(false);
            
            assertTrue(cache.read(dataset, KEY).isEmpty());
            verify(storage, never()).getAuxFileAsInputStream(anyString());
        }
        
        @Test
        void treatsFailingExistenceCheckAsMiss() throws IOException {
            when(storage.isAuxObjectCached(TAG)).thenThrow(new IOException("bucket unreachable"));
            
            assertTrue(cache.read(dataset, KEY).isEmpty());
            verify(storage, never()).getAuxFileAsInputStream(anyString());
        }
        
        @Test
        void treatsFailingOpenAsMiss() throws IOException {
            when(storage.isAuxObjectCached(TAG)).thenReturn(true);
            when(storage.getAuxFileAsInputStream(TAG)).thenThrow(new IOException("vanished"));
            
            assertTrue(cache.read(dataset, KEY).isEmpty());
        }
    }
    
    @Nested
    class Writes {
        
        @Test
        void persistsFullyRenderedExportUnderVersionedTagAndRemovesTempFile() throws Exception {
            givenStorageResolves();
            AtomicReference<Path> tempFile = new AtomicReference<>();
            AtomicReference<byte[]> persisted = new AtomicReference<>();
            doAnswer(invocation -> {
                Path path = invocation.getArgument(0);
                tempFile.set(path);
                persisted.set(Files.readAllBytes(path));
                return null;
            }).when(storage).savePathAsAux(any(Path.class), eq(TAG));
            
            cache.write(dataset, KEY, out -> out.write(PAYLOAD));
            
            assertAll(
                () -> assertArrayEquals(PAYLOAD, persisted.get(), "storage receives the complete export"),
                () -> assertFalse(Files.exists(tempFile.get()), "temp file is cleaned up afterwards")
            );
        }
        
        @Test
        void leavesStorageUntouchedWhenRendererThrowsExportException() {
            ExportException failure = new ExportException("renderer broke");
            ExportStreamWriter failingWriter = out -> { throw failure; };
            
            ExportException thrown = assertThrows(ExportException.class, () -> cache.write(dataset, KEY, failingWriter));
            
            assertSame(failure, thrown);
            dataAccess.verifyNoInteractions();
        }
        
        @Test
        void leavesStorageUntouchedWhenRendererThrowsIOException() {
            ExportStreamWriter failingWriter = out -> { throw new IOException("disk full"); };
            
            assertThrows(IOException.class, () -> cache.write(dataset, KEY, failingWriter));
            dataAccess.verifyNoInteractions();
        }
        
        @Test
        void propagatesPersistFailureButStillRemovesTempFile() throws IOException {
            givenStorageResolves();
            AtomicReference<Path> tempFile = new AtomicReference<>();
            doAnswer(invocation -> {
                tempFile.set(invocation.getArgument(0));
                throw new IOException("bucket unavailable");
            }).when(storage).savePathAsAux(any(Path.class), eq(TAG));
            
            assertThrows(IOException.class, () -> cache.write(dataset, KEY, out -> out.write(PAYLOAD)));
            assertFalse(Files.exists(tempFile.get()), "temp file must not leak on storage failure");
        }
    }
    
    @Nested
    class Evicts {
        
        @BeforeEach
        void resolveStorage() {
            givenStorageResolves();
        }
        
        @Test
        void deletesTheVersionedTag() throws IOException {
            cache.evict(dataset, KEY);
            
            verify(storage).deleteAuxObject(TAG);
        }
        
        @Test
        void swallowsDeleteFailures() throws IOException {
            doThrow(new IOException("gone already")).when(storage).deleteAuxObject(TAG);
            
            assertDoesNotThrow(() -> cache.evict(dataset, KEY));
        }
    }
    
    @Nested
    class EvictAll {
        
        @BeforeEach
        void resolveStorage() {
            givenStorageResolves();
        }
        
        @Test
        void deletesVersionedAndLegacyExportEntriesOnly() throws IOException {
            when(storage.listAuxObjects()).thenReturn(List.of(
                "export_ddi_1.0.cached", "export_ddi.cached", "thumbnail_64.png", "export_notes.txt"));
            
            cache.evictAll(dataset);
            
            assertAll(
                () -> verify(storage).deleteAuxObject("export_ddi_1.0.cached"),
                () -> verify(storage).deleteAuxObject("export_ddi.cached"),
                () -> verify(storage, times(2)).deleteAuxObject(anyString())
            );
        }
        
        @Test
        void continuesAfterIndividualDeleteFailure() throws IOException {
            when(storage.listAuxObjects()).thenReturn(List.of("export_a_1.0.cached", "export_b_1.0.cached"));
            doThrow(new IOException("locked")).when(storage).deleteAuxObject("export_a_1.0.cached");
            
            assertDoesNotThrow(() -> cache.evictAll(dataset));
            verify(storage).deleteAuxObject("export_b_1.0.cached");
        }
    }
    
    @Nested
    class UsedStorage {
        
        @BeforeEach
        void resolveStorage() {
            givenStorageResolves();
        }
        
        @Test
        void sumsSizesOfExportEntriesOnly() throws IOException {
            // Note the inclusion of the legacy file name used before introduction of ExportCacheKey!
            when(storage.listAuxObjects()).thenReturn(List.of("export_ddi_1.0.cached", "export_ddi.cached", "thumbnail_64.png"));
            when(storage.getAuxObjectSize("export_ddi_1.0.cached")).thenReturn(1_000L);
            when(storage.getAuxObjectSize("export_ddi.cached")).thenReturn(24L);
            
            assertEquals(1_024L, cache.usedStorage(dataset));
            verify(storage, never()).getAuxObjectSize("thumbnail_64.png");
        }
        
        @ParameterizedTest(name = "ignores non-export tag ''{0}''")
        @ValueSource(strings = {"thumbnail_64.png", "export_notes.txt", "cached", "legacy_export_ddi.cached"})
        void ignoresNonExportTags(String tag) throws IOException {
            when(storage.listAuxObjects()).thenReturn(List.of(tag));
            
            assertEquals(0L, cache.usedStorage(dataset));
            verify(storage, never()).getAuxObjectSize(anyString());
        }
        
        @Test
        void propagatesSizeLookupFailure() throws IOException {
            when(storage.listAuxObjects()).thenReturn(List.of(TAG));
            when(storage.getAuxObjectSize(TAG)).thenThrow(new IOException("stat failed"));
            
            assertThrows(IOException.class, () -> cache.usedStorage(dataset));
        }
    }
}