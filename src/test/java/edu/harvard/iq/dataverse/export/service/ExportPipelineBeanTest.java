package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.export.service.ExportCache.ExportStreamWriter;
import io.gdcc.spi.export.DatasetExportQuery;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportPipelineBeanTest {
    
    private static final long DATASET_ID = 42L;
    private static final String BASE = "base";
    private static final String DERIVED = "derived";
    
    @Mock ExporterRegistryBean registry;
    
    private Dataset dataset;
    private ExportCache cache;
    private CountingInvalidator invalidator;
    private ExportPipelineBean pipeline;
    
    @BeforeEach
    void setUp() {
        dataset = newDataset();
        invalidator = new CountingInvalidator(false);
        cache = mock(StorageIOCache.class);
        pipeline = pipelineWith(invalidator);
    }
    
    /** Mirrors container wiring: constructor for the invalidators, field injection for the EJB/CDI collaborators. */
    private ExportPipelineBean pipelineWith(ExportCacheInvalidator... invalidators) {
        ExportPipelineBean bean = new ExportPipelineBean(List.of(invalidators));
        bean.registry = registry;
        bean.cache = cache;
        return bean;
    }
    
    /** Shared source for the null-argument checks of both (version, key) entry points. */
    static Stream<Arguments> nullVersionAndKeyCombinations() {
        DatasetVersion version = releasedVersion(newDataset());
        ExportCacheKey key = new ExportCacheKey(version, BASE);
        return Stream.of(arguments(null, key), arguments(version, null), arguments(null, null));
    }
    
    @Nested
    class ReadFreshCachedExport {
        
        @ParameterizedTest(name = "[{index}] rejects null arguments")
        @MethodSource("edu.harvard.iq.dataverse.export.service.ExportPipelineBeanTest#nullVersionAndKeyCombinations")
        void rejectsNullArguments(DatasetVersion version, ExportCacheKey key) {
            assertThrows(IllegalArgumentException.class, () -> pipeline.readFreshCachedExport(version, key));
            verifyNoInteractions(cache);
        }
        
        @Test
        void draftsBypassTheCache() throws IOException {
            // Given
            DatasetVersion draft = draftVersion(dataset);
            
            // When
            Optional<InputStream> result = pipeline.readFreshCachedExport(draft, new ExportCacheKey(draft, BASE));
            
            // Then
            assertAll(
                () -> assertTrue(result.isEmpty()),
                () -> assertEquals(0, invalidator.calls()),
                () -> verifyNoInteractions(cache)
            );
        }
        
        @Test
        void cacheMissYieldsEmpty() throws IOException {
            // Given
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            
            when(cache.read(dataset, key)).thenReturn(Optional.empty());
            
            // When & Then
            assertTrue(pipeline.readFreshCachedExport(version, key).isEmpty());
            assertAll(
                () -> assertEquals(0, invalidator.calls()),
                () -> verify(cache, never()).evict(any(), any())
            );
        }
        
        @Test
        void freshEntryIsReturnedUntouched() throws IOException {
            // Given
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            
            ClosureTrackingInputStream cached = new ClosureTrackingInputStream("cached");
            when(cache.read(dataset, key)).thenReturn(Optional.of(cached));
            
            // When
            Optional<InputStream> result = pipeline.readFreshCachedExport(version, key);
            
             // Then
            assertAll(
                () -> assertSame(cached, result.orElseThrow()),
                () -> assertFalse(cached.isClosed()),
                () -> assertEquals(1, invalidator.calls()),
                () -> verify(cache, never()).evict(any(), any())
            );
        }
        
        @Test
        void noInvalidatorsMeansAlwaysFresh() throws IOException {
            // Given
            pipeline = pipelineWith();
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            
            ClosureTrackingInputStream cached = new ClosureTrackingInputStream("cached");
            when(cache.read(dataset, key)).thenReturn(Optional.of(cached));
            
            // When & Then
            assertSame(cached, pipeline.readFreshCachedExport(version, key).orElseThrow());
        }
        
        @Test
        void staleEntryIsEvictedAndReportedAsMiss() throws IOException {
            // Given
            pipeline = pipelineWith(new CountingInvalidator(false), new CountingInvalidator(true));
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            
            ClosureTrackingInputStream cached = new ClosureTrackingInputStream("stale");
            when(cache.read(dataset, key)).thenReturn(Optional.of(cached));
            
            // When
            Optional<InputStream> result = pipeline.readFreshCachedExport(version, key);
            
            // Then
            assertAll(
                () -> assertTrue(result.isEmpty()),
                () -> assertTrue(cached.isClosed()),
                () -> verify(cache).evict(dataset, key)
            );
        }
        
        @Test
        void evictionFailureClosesStreamAndPropagates() throws IOException {
            // Given
            pipeline = pipelineWith(new CountingInvalidator(true));
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            
            ClosureTrackingInputStream cached = new ClosureTrackingInputStream("stale");
            when(cache.read(dataset, key)).thenReturn(Optional.of(cached));
            doThrow(new IOException("evict failed")).when(cache).evict(dataset, key);
            
            // When & Then
            IOException ex = assertThrows(IOException.class, () -> pipeline.readFreshCachedExport(version, key));
            assertAll(
                () -> assertEquals("evict failed", ex.getMessage()),
                () -> assertTrue(cached.isClosed())
            );
        }
        
        @Test
        void invalidatorFailureNeverMasksOriginalException() throws IOException {
            // Given
            IllegalStateException failure = new IllegalStateException("invalidator broke");
            pipeline = pipelineWith(new FailingInvalidator(failure));
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            
            InputStream failingClose = mock(InputStream.class);
            doThrow(new IOException("close failed")).when(failingClose).close();
            when(cache.read(dataset, key)).thenReturn(Optional.of(failingClose));
            
            // When & Then
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> pipeline.readFreshCachedExport(version, key));
            assertAll(
                () -> assertSame(failure, ex),
                () -> assertEquals(1, ex.getSuppressed().length),
                () -> assertInstanceOf(IOException.class, ex.getSuppressed()[0]),
                () -> verify(cache, never()).evict(any(), any())
            );
        }
    }
    
    @Nested
    class ReadFreshTemporaryExport {
        
        @Test
        void rejectsNullVersion() {
            assertThrows(IllegalArgumentException.class, () -> pipeline.readFreshExport(null, BASE));
        }
        
        @Test
        void rejectsUnregisteredFormat() {
            doThrow(new IllegalArgumentException("unknown")).when(registry).requireExists("unknown");
            
            assertThrows(IllegalArgumentException.class, () -> pipeline.readFreshExport(draftVersion(dataset), "unknown"));
            verifyNoInteractions(cache);
        }
        
        @Test
        void producesFormatWithoutPrerequisite() throws IOException {
            registerExporterMock(BASE, null, writing("BASE"));
            
            try (InputStream in = pipeline.readFreshExport(draftVersion(dataset), BASE)) {
                assertEquals("BASE", readUtf8(in));
            }
            verifyNoInteractions(cache);
        }
        
        @Test
        void draftPrerequisitesAreProducedFresh() throws IOException {
            // Given
            registerExporterMock(BASE, null, writing("BASE"));
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            
            // When (& Then)
            try (InputStream in = pipeline.readFreshExport(draftVersion(dataset), DERIVED)) {
                assertEquals("DERIVED(BASE)", readUtf8(in));
            }
            assertAll(
                () -> assertEquals(0, invalidator.calls()),
                () -> verifyNoInteractions(cache)
            );
        }
        
        @Test
        void releasedPrerequisitesAreReadFromCache() throws IOException {
            // Given
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey baseKey = new ExportCacheKey(version, BASE);
            ClosureTrackingInputStream cached = new ClosureTrackingInputStream("CACHED");
            when(cache.read(dataset, baseKey)).thenReturn(Optional.of(cached));
            
            // When (& Then)
            try (InputStream in = pipeline.readFreshExport(version, DERIVED)) {
                assertEquals("DERIVED(CACHED)", readUtf8(in));
            }
            assertAll(
                () -> assertTrue(cached.isClosed()),
                () -> assertEquals(1, invalidator.calls()),
                () -> verify(cache, never()).write(any(), any(), any())
            );
        }
        
        @Test
        void releasedPrerequisitesAreWrittenThroughOnMiss() throws IOException {
            // Given
            registerExporterMock(BASE, null, writing("BASE"));
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey baseKey = new ExportCacheKey(version, BASE);
            
            ByteArrayOutputStream cacheContent = new ByteArrayOutputStream();
            doAnswer(invocation -> {
                invocation.<ExportStreamWriter>getArgument(2).writeTo(cacheContent);
                return null;
            }).when(cache).write(eq(dataset), eq(baseKey), any());
            when(cache.read(dataset, baseKey))
                // first call result
                .thenReturn(Optional.empty())
                // second call result, value created interactively
                .thenAnswer(invocation -> Optional.of(new ByteArrayInputStream(cacheContent.toByteArray())));
            
            // When & Then
            try (InputStream in = pipeline.readFreshExport(version, DERIVED)) {
                assertEquals("DERIVED(BASE)", readUtf8(in));
            }
            assertAll(
                () -> assertEquals("BASE", cacheContent.toString(UTF_8)),
                () -> verify(cache).write(eq(dataset), eq(baseKey), any()),
                () -> verify(cache, times(2)).read(dataset, baseKey)
            );
        }
        
        @Test
        void failsWhenPrerequisiteCannotBeReadBack() {
            // Given
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            // cache.write() is a no-op on the mock, cache.read() defaults to Optional.empty()
            DatasetVersion version = releasedVersion(dataset);
            
            // When & Then
            var ex = assertThrows(ExportException.class, () -> pipeline.readFreshExport(version, DERIVED));
            assertTrue(ex.getMessage().contains(BASE + " was produced but could not be read back"));
        }
        
        @Test
        void detectsPrerequisiteCycles() {
            // Given
            registerExporterMock(BASE, DERIVED, WRAPPING_PREREQUISITE);
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            
            // When & Then
            var ex =  assertThrows(IllegalArgumentException.class, () -> pipeline.readFreshExport(draftVersion(dataset), DERIVED));
            assertTrue(ex.getMessage().contains(DERIVED + " -> " + BASE + " -> " + DERIVED));
        }
        
        @Test
        void wrapsIllegalStateExceptionFromExporter() {
            // Given
            IllegalStateException cause = new IllegalStateException("field type mismatch");
            registerExporterMock(BASE, null, (provider, out) -> { throw cause; });
            
            // The wrapped message references the dataset's global id, so the fixture must provide one.
            Dataset identified = spy(dataset);
            doReturn(mock(GlobalId.class)).when(identified).getGlobalId();
            
            // When & Then
            var ex = assertThrows(ExportException.class, () -> pipeline.readFreshExport(draftVersion(identified), BASE));
            assertAll(
                () -> assertSame(cause, ex.getCause()),
                () -> assertTrue(ex.getMessage().contains("IllegalStateException caught"))
            );
        }
    }
    
    @Nested
    class ProducingAndCaching {
        
        @ParameterizedTest(name = "[{index}] rejects null arguments")
        @MethodSource("edu.harvard.iq.dataverse.export.service.ExportPipelineBeanTest#nullVersionAndKeyCombinations")
        void rejectsNullArguments(DatasetVersion version, ExportCacheKey key) {
            assertThrows(IllegalArgumentException.class, () -> pipeline.produceAndCache(version, key));
            verifyNoInteractions(cache);
        }
        
        @Test
        void delegatesProductionToCacheWriter() throws Exception {
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            registerExporterMock(BASE, null, writing("BASE"));
            
            pipeline.produceAndCache(version, key);
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            capturedWriter(key).writeTo(out);
            assertEquals("BASE", out.toString(UTF_8));
        }
        
        @Test
        void writerRejectsNullOutputStream() throws Exception {
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            
            pipeline.produceAndCache(version, key);
            
            ExportStreamWriter writer = capturedWriter(key);
            assertThrows(IllegalArgumentException.class, () -> writer.writeTo(null));
        }
        
        @Test
        void writerRejectsUnknownFormat() throws Exception {
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, "unknown");
            
            pipeline.produceAndCache(version, key);
            
            ExportStreamWriter writer = capturedWriter(key);
            IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> writer.writeTo(new ByteArrayOutputStream()));
            assertTrue(ex.getMessage().contains("unknown"));
        }
        
        private ExportStreamWriter capturedWriter(ExportCacheKey key) throws IOException {
            ArgumentCaptor<ExportStreamWriter> captor = ArgumentCaptor.forClass(ExportStreamWriter.class);
            verify(cache).write(eq(dataset), eq(key), captor.capture());
            return captor.getValue();
        }
    }
    
    // ++++ ++++ ++++ FIXTURES & HELPERS ++++ ++++ ++++
    
    /** Minimal exporter behaviour, keeping test bodies focused on the pipeline rather than Mockito plumbing. */
    @FunctionalInterface
    private interface ExportBody {
        void export(ExportDataProvider provider, OutputStream out) throws IOException;
    }
    
    private static final ExportBody WRAPPING_PREREQUISITE = (provider, out) -> {
        try (InputStream prerequisite = provider.getPrerequisiteInputStream(DatasetExportQuery.defaults()).orElseThrow()) {
            out.write(("DERIVED(" + readUtf8(prerequisite) + ")").getBytes(UTF_8));
        }
    };
    
    private static ExportBody writing(String content) {
        return (provider, out) -> out.write(content.getBytes(UTF_8));
    }
    
    /**
     * Registers a mocked exporter under {@code formatName}.
     * The exporter's own stubs are lenient on purpose, as several tests intentionally fail before the exporter
     * is ever driven to completion.
     */
    private void registerExporterMock(String formatName, String prerequisite, ExportBody body) {
        Exporter exporter = mock(Exporter.class);
        lenient().when(exporter.getPrerequisiteFormatName()).thenReturn(Optional.ofNullable(prerequisite));
        lenient().doAnswer(invocation -> {
            body.export(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(exporter).exportDataset(any(), any());
        when(registry.get(formatName)).thenReturn(Optional.of(exporter));
    }
    
    
    // TODO: aren't there mock factories around for this?
    
    private static Dataset newDataset() {
        Dataset dataset = new Dataset();
        dataset.setId(DATASET_ID);
        return dataset;
    }
    
    private static DatasetVersion draftVersion(Dataset dataset) {
        return version(dataset, DatasetVersion.VersionState.DRAFT);
    }
    
    private static DatasetVersion releasedVersion(Dataset dataset) {
        return version(dataset, DatasetVersion.VersionState.RELEASED);
    }
    
    private static DatasetVersion version(Dataset dataset, DatasetVersion.VersionState state) {
        DatasetVersion version = new DatasetVersion();
        version.setDataset(dataset);
        version.setVersionState(state);
        version.setVersionNumber(1L);
        version.setMinorVersionNumber(0L);
        return version;
    }
    
    private static String readUtf8(InputStream in) throws IOException {
        return new String(in.readAllBytes(), UTF_8);
    }
    
    /** Deterministic invalidator with a call counter. Any real invalidators have their own tests. */
    private static final class CountingInvalidator implements ExportCacheInvalidator {
        private final boolean stale;
        private int calls;
        
        CountingInvalidator(boolean stale) {
            this.stale = stale;
        }
        
        @Override
        public boolean isStale(DatasetVersion datasetVersion, ExportCacheKey key) {
            calls++;
            return stale;
        }
        
        int calls() {
            return calls;
        }
    }
    
    /** Invalidator always blowing up on use, allowing to exercise the pipeline's stream cleanup. */
    private record FailingInvalidator(RuntimeException failure) implements ExportCacheInvalidator {
        @Override
        public boolean isStale(DatasetVersion datasetVersion, ExportCacheKey key) {
            throw failure;
        }
    }
    
    /** Records whether the pipeline closed the stream it received from the cache. */
    private static final class ClosureTrackingInputStream extends ByteArrayInputStream {
        private boolean closed;
        
        ClosureTrackingInputStream(String content) {
            super(content.getBytes(UTF_8));
        }
        
        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
        
        boolean isClosed() {
            return closed;
        }
    }
}