package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.export.service.ExportSystemException.InternalFailure;
import edu.harvard.iq.dataverse.export.service.ExportSystemException.InvalidRequest;
import io.gdcc.spi.export.DatasetExportQuery;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.Exporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the pipeline through its three entry points only. The cache is a real (in-memory) implementation,
 * so assertions are about observable outcomes: what ends up cached, what was evicted, and whether streams
 * were closed. The registry stays a mock, as it is a pure lookup table.
 */
@ExtendWith(MockitoExtension.class)
class ExportPipelineBeanTest {
    
    private static final long DATASET_ID = 42L;
    private static final String BASE = "base";
    private static final String DERIVED = "derived";
    private static final String UNKNOWN = "unknown";
    
    @Mock ExporterRegistryBean registry;
    
    private Dataset dataset;
    private InMemoryExportCache cache;
    private CountingInvalidator invalidator;
    private ExportPipelineBean pipeline;
    
    @BeforeEach
    void setUp() {
        dataset = newDataset();
        cache = new InMemoryExportCache();
        invalidator = new CountingInvalidator(false);
        pipeline = pipelineWith(invalidator);
    }
    
    private ExportPipelineBean pipelineWith(ExportCacheInvalidator... invalidators) {
        return new ExportPipelineBean(registry, cache, List.of(invalidators));
    }
    
    /** Shared source for the null-argument checks of both (version, key) entry points. */
    static Stream<Arguments> nullVersionAndKeyCombinations() {
        DatasetVersion version = releasedVersion(newDataset());
        ExportCacheKey key = new ExportCacheKey(version, BASE);
        return Stream.of(arguments(null, key), arguments(version, null), arguments(null, null));
    }
    
    // ++++ ++++ ++++ readFreshCachedExport ++++ ++++ ++++
    
    @Nested
    class ReadFreshCachedExport {
        
        @ParameterizedTest(name = "[{index}] rejects null arguments")
        @MethodSource("edu.harvard.iq.dataverse.export.service.ExportPipelineBeanTest#nullVersionAndKeyCombinations")
        void rejectsNullArguments(DatasetVersion version, ExportCacheKey key) {
            assertThrows(InvalidRequest.class, () -> pipeline.readFreshCachedExport(version, key));
            assertEquals(0, cache.reads());
        }
        
        @Test
        void draftsBypassTheCache() throws IOException {
            DatasetVersion draft = draftVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(draft, BASE);
            cache.seed(dataset, key, "should never be served for a draft");
            
            Optional<InputStream> result = pipeline.readFreshCachedExport(draft, key);
            
            assertAll(
                () -> assertTrue(result.isEmpty()),
                () -> assertEquals(0, invalidator.calls()),
                () -> assertEquals(0, cache.reads())
            );
        }
        
        @Test
        void cacheMissYieldsEmpty() throws IOException {
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            
            Optional<InputStream> result = pipeline.readFreshCachedExport(version, key);
            
            assertAll(
                () -> assertTrue(result.isEmpty()),
                () -> assertEquals(1, cache.reads()),
                () -> assertEquals(0, cache.hits()),
                () -> assertEquals(0, invalidator.calls()),
                () -> assertEquals(0, cache.evictions())
            );
        }
        
        @Test
        void freshEntryIsHandedOutOpenAndUnconsumed() throws IOException {
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            cache.seed(dataset, key, "cached");
            
            try (InputStream in = pipeline.readFreshCachedExport(version, key).orElseThrow()) {
                // The pipeline must neither close nor consume the stream before the caller sees it.
                assertEquals(1, cache.openStreams());
                assertEquals("cached", readUtf8(in));
            }
            assertAll(
                () -> assertEquals(1, invalidator.calls()),
                () -> assertEquals(0, cache.evictions()),
                () -> assertTrue(cache.contains(dataset, key)),
                () -> assertEquals(0, cache.openStreams())
            );
        }
        
        @Test
        void noInvalidatorsMeansAlwaysFresh() throws IOException {
            pipeline = pipelineWith();
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            cache.seed(dataset, key, "cached");
            
            try (InputStream in = pipeline.readFreshCachedExport(version, key).orElseThrow()) {
                assertEquals("cached", readUtf8(in));
            }
        }
        
        @Test
        void staleEntryIsEvictedAndReportedAsMiss() throws IOException {
            pipeline = pipelineWith(new CountingInvalidator(false), new CountingInvalidator(true));
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            cache.seed(dataset, key, "stale");
            
            Optional<InputStream> result = pipeline.readFreshCachedExport(version, key);
            
            assertAll(
                () -> assertTrue(result.isEmpty()),
                () -> assertFalse(cache.contains(dataset, key)),
                () -> assertEquals(1, cache.evictions()),
                () -> assertEquals(0, cache.openStreams())
            );
        }
        
        @Test
        void evictionFailureClosesStreamAndPropagates() {
            pipeline = pipelineWith(new CountingInvalidator(true));
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            cache.seed(dataset, key, "stale");
            cache.failEvictsWith(new IOException("evict failed"));
            
            IOException ex = assertThrows(IOException.class, () -> pipeline.readFreshCachedExport(version, key));
            
            assertAll(
                () -> assertEquals("evict failed", ex.getMessage()),
                () -> assertEquals(0, cache.openStreams()),
                () -> assertTrue(cache.contains(dataset, key), "failed eviction must not pretend the entry is gone")
            );
        }
        
        @Test
        void invalidatorFailureNeverMasksOriginalException() {
            IllegalStateException failure = new IllegalStateException("invalidator broke");
            pipeline = pipelineWith(new FailingInvalidator(failure));
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            cache.seed(dataset, key, "cached");
            cache.failStreamClosesWith(new IOException("close failed"));
            
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> pipeline.readFreshCachedExport(version, key));
            
            assertAll(
                () -> assertSame(failure, ex),
                () -> assertEquals(1, ex.getSuppressed().length),
                () -> assertInstanceOf(IOException.class, ex.getSuppressed()[0]),
                () -> assertEquals(0, cache.openStreams()),
                () -> assertEquals(0, cache.evictions())
            );
        }
    }
    
    // ++++ ++++ ++++ readFreshExport ++++ ++++ ++++
    
    @Nested
    class ReadFreshTemporaryExport {
        
        @Test
        void rejectsNullVersion() {
            assertThrows(InvalidRequest.class, () -> pipeline.readFreshExport(null, BASE));
        }
        
        @Test
        void rejectsUnregisteredFormatBeforeDoingAnyWork() {
            doThrow(new InvalidRequest("no such format")).when(registry).requireExists(UNKNOWN);
            
            assertThrows(InvalidRequest.class, () -> pipeline.readFreshExport(draftVersion(dataset), UNKNOWN));
            
            assertAll(
                () -> verify(registry).requireExists(UNKNOWN),
                () -> assertEquals(0, cache.reads())
            );
        }
        
        @Test
        void producesFormatWithoutPrerequisite() throws IOException {
            registerExporterMock(BASE, null, writing("BASE"));
            
            try (InputStream in = pipeline.readFreshExport(draftVersion(dataset), BASE)) {
                assertEquals("BASE", readUtf8(in));
            }
            assertAll(
                () -> assertEquals(0, cache.reads()),
                () -> assertEquals(0, cache.writes())
            );
        }
        
        @Test
        void draftPrerequisitesAreProducedFreshWithoutTouchingTheCache() throws IOException {
            registerExporterMock(BASE, null, writing("BASE"));
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            
            try (InputStream in = pipeline.readFreshExport(draftVersion(dataset), DERIVED)) {
                assertEquals("DERIVED(BASE)", readUtf8(in));
            }
            assertAll(
                () -> assertEquals(0, invalidator.calls()),
                () -> assertEquals(0, cache.reads()),
                () -> assertEquals(0, cache.writes())
            );
        }
        
        @Test
        void releasedPrerequisitesAreReadFromCache() throws IOException {
            // Deliberately no BASE exporter: if the pipeline tried to produce it, registry.get(BASE) would be empty and fail.
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey baseKey = new ExportCacheKey(version, BASE);
            cache.seed(dataset, baseKey, "CACHED");
            
            try (InputStream in = pipeline.readFreshExport(version, DERIVED)) {
                assertEquals("DERIVED(CACHED)", readUtf8(in));
            }
            assertAll(
                () -> assertEquals(1, invalidator.calls()),
                () -> assertEquals(0, cache.writes()),
                () -> assertEquals(0, cache.openStreams(), "prerequisite stream must be closed after use"),
                () -> assertEquals(1, cache.size(), "the derived (temporary) export must not be cached")
            );
        }
        
        @Test
        void releasedPrerequisitesAreWrittenThroughOnMiss() throws IOException {
            registerExporterMock(BASE, null, writing("BASE"));
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey baseKey = new ExportCacheKey(version, BASE);
            ExportCacheKey derivedKey = new ExportCacheKey(version, DERIVED);
            
            try (InputStream in = pipeline.readFreshExport(version, DERIVED)) {
                assertEquals("DERIVED(BASE)", readUtf8(in));
            }
            assertAll(
                () -> assertEquals(Optional.of("BASE"), cache.contentOf(dataset, baseKey)),
                () -> assertFalse(cache.contains(dataset, derivedKey), "the derived (temporary) export must not be cached"),
                () -> assertEquals(1, cache.writes()),
                () -> assertEquals(0, cache.openStreams())
            );
        }
        
        @Test
        void failsWhenPrerequisiteCannotBeReadBack() {
            registerExporterMock(BASE, null, writing("BASE"));
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            cache.swallowWrites();
            
            var ex = assertThrows(InternalFailure.class, () -> pipeline.readFreshExport(releasedVersion(dataset), DERIVED));
            
            assertAll(
                () -> assertTrue(ex.getMessage().contains(BASE + " was produced but could not be read back")),
                () -> assertEquals(1, cache.writerInvocations(), "the prerequisite was actually produced")
            );
        }
        
        @Test
        void detectsPrerequisiteCycles() {
            registerExporterMock(BASE, DERIVED, WRAPPING_PREREQUISITE);
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            
            var ex = assertThrows(InternalFailure.class, () -> pipeline.readFreshExport(draftVersion(dataset), DERIVED));
            assertTrue(ex.getMessage().contains(DERIVED + " -> " + BASE + " -> " + DERIVED));
        }
        
        @Test
        void cycleDetectionLeavesCacheUntouchedForReleasedVersions() {
            registerExporterMock(BASE, DERIVED, WRAPPING_PREREQUISITE);
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            
            assertThrows(InternalFailure.class, () -> pipeline.readFreshExport(releasedVersion(dataset), DERIVED));
            assertTrue(cache.isEmpty(), "no partial entry may survive a failed prerequisite chain");
        }
        
        @Test
        void wrapsIllegalStateExceptionFromExporter() {
            IllegalStateException cause = new IllegalStateException("field type mismatch");
            registerExporterMock(BASE, null, (provider, out) -> {
                throw cause;
            });
            Dataset identified = withGlobalId(dataset);
            
            var ex = assertThrows(InternalFailure.class, () -> pipeline.readFreshExport(draftVersion(identified), BASE));
            
            assertAll(
                () -> assertSame(cause, ex.getCause()),
                () -> assertTrue(ex.getMessage().contains("IllegalStateException caught"))
            );
        }
        
        @Test
        void otherRuntimeExceptionsPropagateUnwrapped() {
            // Only IllegalStateException is wrapped inside the pipeline; anything else surfaces as-is
            // and is wrapped at the service boundary (ExportServiceBean).
            IllegalArgumentException boom = new IllegalArgumentException("boom");
            registerExporterMock(BASE, null, (provider, out) -> { throw boom; });
            
            var ex = assertThrows(IllegalArgumentException.class, () -> pipeline.readFreshExport(draftVersion(dataset), BASE));
            assertSame(boom, ex);
        }
    }
    
    // ++++ ++++ ++++ produceAndCache ++++ ++++ ++++
    
    @Nested
    class ProducingAndCaching {
        
        @ParameterizedTest(name = "[{index}] rejects null arguments")
        @MethodSource("edu.harvard.iq.dataverse.export.service.ExportPipelineBeanTest#nullVersionAndKeyCombinations")
        void rejectsNullArguments(DatasetVersion version, ExportCacheKey key) {
            assertThrows(InvalidRequest.class, () -> pipeline.produceAndCache(version, key));
            assertEquals(0, cache.writes());
        }
        
        @Test
        void producesAndStoresFormatWithoutPrerequisite() throws IOException {
            registerExporterMock(BASE, null, writing("BASE"));
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey key = new ExportCacheKey(version, BASE);
            
            pipeline.produceAndCache(version, key);
            
            assertAll(
                () -> assertEquals(Optional.of("BASE"), cache.contentOf(dataset, key)),
                () -> assertEquals(1, cache.size()),
                () -> assertEquals(1, cache.writes())
            );
        }
        
        @Test
        void unknownFormatIsRejectedAndNothingIsCached() {
            when(registry.get(UNKNOWN)).thenReturn(Optional.empty());
            DatasetVersion version = releasedVersion(dataset);
            
            var ex = assertThrows(InvalidRequest.class, () -> pipeline.produceAndCache(version, new ExportCacheKey(version, UNKNOWN)));
            
            assertAll(
                () -> assertTrue(ex.getMessage().contains(UNKNOWN)),
                () -> assertTrue(cache.isEmpty())
            );
        }
        
        @Test
        void unregisteredPrerequisiteIsRejectedAndNothingIsCached() {
            registerExporterMock(DERIVED, "missing-prereq", WRAPPING_PREREQUISITE);
            when(registry.get("missing-prereq")).thenReturn(Optional.empty());
            DatasetVersion version = releasedVersion(dataset);
            
            var ex = assertThrows(InvalidRequest.class, () -> pipeline.produceAndCache(version, new ExportCacheKey(version, DERIVED)));
            
            assertAll(
                () -> assertTrue(ex.getMessage().contains("missing-prereq")),
                () -> assertTrue(cache.isEmpty())
            );
        }
        
        @Test
        void exporterFailureLeavesCacheUntouched() {
            IllegalArgumentException boom = new IllegalArgumentException("boom");
            registerExporterMock(BASE, null, (provider, out) -> { throw boom; });
            DatasetVersion version = releasedVersion(dataset);
            
            var ex = assertThrows(IllegalArgumentException.class, () -> pipeline.produceAndCache(version, new ExportCacheKey(version, BASE)));
            
            assertAll(
                () -> assertSame(boom, ex),
                () -> assertTrue(cache.isEmpty())
            );
        }
        
        @Test
        void illegalStateExceptionIsWrappedOnTheCachePathToo() {
            IllegalStateException cause = new IllegalStateException("field type mismatch");
            registerExporterMock(BASE, null, (provider, out) -> { throw cause; });
            Dataset identified = withGlobalId(dataset);
            DatasetVersion version = releasedVersion(identified);
            
            var ex = assertThrows(InternalFailure.class, () -> pipeline.produceAndCache(version, new ExportCacheKey(version, BASE)));
            
            assertAll(
                () -> assertSame(cause, ex.getCause()),
                () -> assertTrue(cache.isEmpty())
            );
        }
        
        @Test
        void prerequisiteIsProducedAndCachedAlongside() throws IOException {
            registerExporterMock(BASE, null, writing("BASE"));
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey baseKey = new ExportCacheKey(version, BASE);
            ExportCacheKey derivedKey = new ExportCacheKey(version, DERIVED);
            
            pipeline.produceAndCache(version, derivedKey);
            
            assertAll(
                () -> assertEquals(Optional.of("DERIVED(BASE)"), cache.contentOf(dataset, derivedKey)),
                () -> assertEquals(Optional.of("BASE"), cache.contentOf(dataset, baseKey)),
                () -> assertEquals(2, cache.writes()),
                () -> assertEquals(0, cache.openStreams())
            );
        }
        
        @Test
        void cachedPrerequisiteIsReusedWithoutRunningItsExporter() throws IOException {
            // Deliberately no BASE exporter registered.
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey baseKey = new ExportCacheKey(version, BASE);
            ExportCacheKey derivedKey = new ExportCacheKey(version, DERIVED);
            cache.seed(dataset, baseKey, "CACHED");
            
            pipeline.produceAndCache(version, derivedKey);
            
            assertAll(
                () -> assertEquals(Optional.of("DERIVED(CACHED)"), cache.contentOf(dataset, derivedKey)),
                () -> assertEquals(1, cache.writes()),
                () -> verify(registry, never()).get(BASE),
                () -> assertEquals(0, cache.openStreams())
            );
        }
        
        @Test
        void stalePrerequisiteIsRegeneratedBeforeUse() throws IOException {
            pipeline = pipelineWith(new CountingInvalidator(true));
            registerExporterMock(BASE, null, writing("BASE"));
            registerExporterMock(DERIVED, BASE, WRAPPING_PREREQUISITE);
            DatasetVersion version = releasedVersion(dataset);
            ExportCacheKey baseKey = new ExportCacheKey(version, BASE);
            ExportCacheKey derivedKey = new ExportCacheKey(version, DERIVED);
            cache.seed(dataset, baseKey, "OLD");
            
            pipeline.produceAndCache(version, derivedKey);
            
            assertAll(
                () -> assertEquals(Optional.of("DERIVED(BASE)"), cache.contentOf(dataset, derivedKey), "stale bytes must never feed a derived export"),
                () -> assertEquals(Optional.of("BASE"), cache.contentOf(dataset, baseKey)),
                () -> assertEquals(1, cache.evictions()),
                () -> assertEquals(2, cache.writes())
            );
        }
        
        @Test
        void cacheWriteFailurePropagatesBeforeAnyExporterRuns() {
            cache.failWritesWith(new IOException("disk full"));
            DatasetVersion version = releasedVersion(dataset);
            
            IOException ex = assertThrows(IOException.class, () -> pipeline.produceAndCache(version, new ExportCacheKey(version, BASE)));
            
            assertAll(
                () -> assertEquals("disk full", ex.getMessage()),
                () -> assertEquals(0, cache.writerInvocations())
            );
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
     * is ever driven to completion. The registry lookup itself stays strict: every registered format must be used.
     */
    private Exporter registerExporterMock(String formatName, String prerequisite, ExportBody body) {
        Exporter exporter = mock(Exporter.class);
        lenient().when(exporter.getPrerequisiteFormatName()).thenReturn(Optional.ofNullable(prerequisite));
        lenient().doAnswer(invocation -> {
            body.export(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(exporter).exportDataset(any(), any());
        when(registry.get(formatName)).thenReturn(Optional.of(exporter));
        return exporter;
    }
    
    /** The ISE-wrapping message references the dataset's global id, so that fixture must provide one. */
    private static Dataset withGlobalId(Dataset dataset) {
        Dataset identified = spy(dataset);
        doReturn(mock(GlobalId.class)).when(identified).getGlobalId();
        return identified;
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
}