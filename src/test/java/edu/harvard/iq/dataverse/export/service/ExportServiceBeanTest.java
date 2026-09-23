package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.export.service.ExportSystemException.InternalFailure;
import edu.harvard.iq.dataverse.export.service.ExportSystemException.InvalidRequest;
import edu.harvard.iq.dataverse.export.service.ExporterRegistryBean.Details;
import edu.harvard.iq.dataverse.export.service.ExporterRegistryBean.ExporterDetails;
import io.gdcc.spi.export.Exporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * The service is a thin orchestration layer over registry, cache, and pipeline. All three are mocked:
 * the assertions therefore cover delegation, ordering, argument validation, and exception translation
 * at the EJB boundary, not export rendering itself
 * (see {@link ExportPipelineBeanTest} and {@link ExporterRegistryBeanTest} for that).
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceBeanTest {
    
    private static final long DATASET_ID = 42L;
    /* Fixture graph: BASE is prerequisite of DERIVED; STANDALONE has no relations. */
    private static final String BASE = "base";
    private static final String DERIVED = "derived";
    private static final String STANDALONE = "standalone";
    private static final String UNKNOWN = "unknown";
    private static final List<String> TOPOLOGICAL_ORDER = List.of(BASE, DERIVED, STANDALONE);
    
    @Mock ExporterRegistryBean registry;
    @Mock ExportCache cache;
    @Mock ExportPipelineBean pipeline;
    @Mock DatasetVersionServiceBean versionService;
    // TODO: make this an actual object and not a mock?
    @Mock Dataset dataset;
    
    private ExportServiceBean service;
    
    @BeforeEach
    void setUp() {
        // EJB requires field injection, which is why these fields are package-private.
        service = new ExportServiceBean();
        service.registry = registry;
        service.cache = cache;
        service.pipeline = pipeline;
        service.versionService = versionService;
        
        lenient().when(dataset.getId()).thenReturn(DATASET_ID);
    }
    
    // ++++ ++++ ++++ RETRIEVING EXPORTS ++++ ++++ ++++
    
    @Nested
    class GetExport {
        
        @Test
        void servesCachedExportWhenPresent() throws IOException {
            DatasetVersion version = releasedVersion();
            InputStream cached = utf8("cached");
            when(pipeline.readFreshCachedExport(version, keyOf(version, BASE))).thenReturn(Optional.of(cached));
            
            assertSame(cached, service.getExport(version, BASE));
        }
        
        @Test
        void fallsBackToFreshExportOnCacheMiss() throws IOException {
            DatasetVersion draft = draftVersion();
            InputStream fresh = utf8("fresh");
            when(pipeline.readFreshCachedExport(draft, keyOf(draft, BASE))).thenReturn(Optional.empty());
            when(pipeline.readFreshExport(draft, BASE)).thenReturn(fresh);
            
            assertSame(fresh, service.getExport(draft, BASE));
        }
        
        @Test
        void rethrowsExportSystemExceptionsUnwrapped() throws IOException {
            DatasetVersion version = releasedVersion();
            InvalidRequest failure = new InvalidRequest("unknown format");
            when(pipeline.readFreshCachedExport(any(), any())).thenThrow(failure);
            
            assertSame(failure, assertThrows(InvalidRequest.class, () -> service.getExport(version, BASE)));
        }
        
        @ParameterizedTest(name = "{0}")
        @MethodSource("wrappedFailures")
        void wrapsAnyOtherFailureInInternalFailure(Exception failure) throws IOException {
            DatasetVersion version = releasedVersion();
            when(pipeline.readFreshCachedExport(any(), any())).thenThrow(failure);
            
            InternalFailure ex = assertThrows(InternalFailure.class, () -> service.getExport(version, BASE));
            assertSame(failure, ex.getCause());
        }
        
        static Stream<Named<Exception>> wrappedFailures() {
            return Stream.of(
                Named.of("IOException", new IOException("disk gone")),
                Named.of("RuntimeException", new IllegalArgumentException("boom"))
            );
        }
    }
    
    /**
     * Only the lookup and the error contract are testable here: whether the container actually opens a new
     * transaction cannot be observed outside a EJB/CDI container deployment!
     */
    @Nested
    class GetExportInNewTransaction {
        
        private static final long VERSION_ID = 7L;
        
        @Test
        void resolvesVersionByIdThenExportsIt() throws IOException {
            DatasetVersion version = releasedVersion();
            version.setId(VERSION_ID);
            InputStream cached = utf8("cached");
            when(versionService.find(VERSION_ID)).thenReturn(version);
            when(pipeline.readFreshCachedExport(version, keyOf(version, BASE))).thenReturn(Optional.of(cached));
            
            assertSame(cached, service.getExportInNewTransaction(VERSION_ID, BASE));
        }
        
        @Test
        void rejectsVanishedVersionWithoutTouchingThePipeline() {
            when(versionService.find(VERSION_ID)).thenReturn(null);
            
            InvalidRequest ex = assertThrows(InvalidRequest.class,
                () -> service.getExportInNewTransaction(VERSION_ID, BASE));
            
            assertAll(
                () -> assertTrue(ex.getMessage().contains(String.valueOf(VERSION_ID))),
                () -> verifyNoInteractions(pipeline)
            );
        }
    }
    
    @Nested
    class GetLatestPublishedAsString {
        
        @Test
        void returnsNullForNullDataset() {
            assertNull(service.getLatestPublishedAsString(null, BASE));
        }
        
        @Test
        void returnsNullWithoutReleasedVersion() {
            when(dataset.getReleasedVersion()).thenReturn(null);
            
            assertNull(service.getLatestPublishedAsString(dataset, BASE));
        }
        
        @Test
        void rejectsUnknownFormat() {
            releasedDataset();
            doThrow(new InvalidRequest("unknown")).when(registry).requireExists(UNKNOWN);
            
            assertThrows(InvalidRequest.class, () -> service.getLatestPublishedAsString(dataset, UNKNOWN));
        }
        
        @Test
        void decodesCachedExportAsUtf8() throws IOException {
            DatasetVersion released = releasedDataset();
            when(pipeline.readFreshCachedExport(released, keyOf(released, BASE))).thenReturn(Optional.of(utf8("cachéd")));
            
            assertEquals("cachéd", service.getLatestPublishedAsString(dataset, BASE));
        }
        
        @Test
        void fallsBackToFreshExportOnCacheMiss() throws IOException {
            DatasetVersion released = releasedDataset();
            when(pipeline.readFreshCachedExport(released, keyOf(released, BASE))).thenReturn(Optional.empty());
            when(pipeline.readFreshExport(released, BASE)).thenReturn(utf8("fresh"));
            
            assertEquals("fresh", service.getLatestPublishedAsString(dataset, BASE));
        }
        
        @Test
        void ioFailuresAreReportedAsNull() throws IOException {
            releasedDataset();
            when(pipeline.readFreshCachedExport(any(), any())).thenThrow(new IOException("disk gone"));
            
            assertNull(service.getLatestPublishedAsString(dataset, BASE));
        }
        
        @Test
        void rethrowsExportSystemExceptionsUnwrapped() throws IOException {
            releasedDataset();
            InternalFailure failure = new InternalFailure("pipeline broke");
            when(pipeline.readFreshCachedExport(any(), any())).thenThrow(failure);
            
            assertSame(failure, assertThrows(InternalFailure.class, () -> service.getLatestPublishedAsString(dataset, BASE)));
        }
        
        @Test
        void wrapsRuntimeExceptionsInInternalFailure() throws IOException {
            releasedDataset();
            IllegalArgumentException boom = new IllegalArgumentException("boom");
            when(pipeline.readFreshCachedExport(any(), any())).thenThrow(boom);
            
            InternalFailure ex = assertThrows(InternalFailure.class, () -> service.getLatestPublishedAsString(dataset, BASE));
            assertSame(boom, ex.getCause());
        }
    }
    
    // ++++ ++++ ++++ CACHE MANAGEMENT ++++ ++++ ++++
    
    @Nested
    class UsedCacheStorage {
        
        @Test
        void rejectsNullDataset() {
            assertThrows(InvalidRequest.class, () -> service.usedCacheStorage(null));
        }
        
        @Test
        void delegatesToCache() throws IOException {
            when(cache.usedStorage(dataset)).thenReturn(1_024L);
            
            assertEquals(1_024L, service.usedCacheStorage(dataset));
        }
    }
    
    @Nested
    class ClearingCachedFormatsOfVersion {
        
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("invalidVersions")
        void rejectsVersionWithoutDataset(DatasetVersion version) {
            assertThrows(InvalidRequest.class, () -> service.clearCachedFormats(version, List.of()));
        }
        
        static Stream<DatasetVersion> invalidVersions() {
            return Stream.of(null, new DatasetVersion());
        }
        
        @Test
        void draftsAreSkippedSilently() {
            assertDoesNotThrow(() -> service.clearCachedFormats(draftVersion(), List.of(BASE)));
        }
        
        @Test
        void rejectsUnknownFormatsBeforeEvictingAnything() {
            DatasetVersion version = releasedVersion();
            doThrow(new InvalidRequest("unknown")).when(registry).requireAllExist(List.of(UNKNOWN));
            
            assertThrows(InvalidRequest.class, () -> service.clearCachedFormats(version, List.of(UNKNOWN)));
            verifyNoInteractions(cache);
        }
        
        @Test
        void emptyListEvictsEveryRegisteredFormat() throws IOException {
            DatasetVersion version = releasedVersion();
            stubRegisteredDetails(BASE, DERIVED, STANDALONE);
            
            service.clearCachedFormats(version, List.of());
            
            verify(cache).evict(dataset, keyOf(version, BASE));
            verify(cache).evict(dataset, keyOf(version, DERIVED));
            verify(cache).evict(dataset, keyOf(version, STANDALONE));
            verifyNoMoreInteractions(cache);
        }
        
        @Test
        void evictionCascadesToTransitiveDependents() throws IOException {
            DatasetVersion version = releasedVersion();
            when(registry.getTransitiveDependents(BASE)).thenReturn(Set.of(DERIVED));
            
            service.clearCachedFormats(version, List.of(BASE));
            
            verify(cache).evict(dataset, keyOf(version, BASE));
            verify(cache).evict(dataset, keyOf(version, DERIVED));
            verifyNoMoreInteractions(cache);
        }
        
        @Test
        void keepsEvictingAfterFailureThenReportsFailedFormatsOnly() throws IOException {
            DatasetVersion version = releasedVersion();
            ExportCacheKey baseKey = keyOf(version, BASE);
            doThrow(new IOException("gone")).when(cache).evict(dataset, baseKey);
            
            InternalFailure ex = assertThrows(InternalFailure.class,
                () -> service.clearCachedFormats(version, List.of(BASE, STANDALONE)));
            
            assertAll(
                () -> assertTrue(ex.getMessage().contains(BASE)),
                () -> assertFalse(ex.getMessage().contains(STANDALONE)),
                () -> verify(cache).evict(dataset, keyOf(version, STANDALONE))
            );
        }
    }
    
    @Nested
    class ClearingCachedFormatsOfDataset {
        
        @Test
        void rejectsNullDataset() {
            assertThrows(InvalidRequest.class, () -> service.clearCachedFormats((Dataset) null, List.of()));
            verifyNoInteractions(registry, cache);
        }
        
        @Test
        void clearingAllFormatsEvictsReleasedVersionAndResetsTimestamp() throws IOException {
            DatasetVersion released = releasedDataset();
            stubRegisteredDetails(BASE);
            
            service.clearAllCachedFormats(dataset);
            
            verify(cache).evict(dataset, keyOf(released, BASE));
            verify(dataset).setLastExportTime(null);
        }
        
        @Test
        void clearingSelectedFormatsKeepsTimestamp() throws IOException {
            DatasetVersion released = releasedDataset();
            
            service.clearCachedFormats(dataset, List.of(BASE));
            
            verify(cache).evict(dataset, keyOf(released, BASE));
            verify(dataset, never()).setLastExportTime(any());
        }
        
        @Test
        void unreleasedDatasetsResolveToDraftAndSkipEviction() {
            draftOnlyDataset();
            
            service.clearCachedFormats(dataset, List.of(BASE));
            
            verifyNoInteractions(registry, cache);
        }
    }
    
    // ++++ ++++ ++++ TRIGGERING EXPORTS ++++ ++++ ++++
    
    @Nested
    class ExportingFormatsOfVersion {
        
        @BeforeEach
        void registryOrdersTopologically() {
            // Lenient: validation-failure tests never reach the sorting step.
            lenient().when(registry.getTopologicalComparator())
                .thenReturn(Comparator.comparingInt(e -> TOPOLOGICAL_ORDER.indexOf(e.getFormatName())));
        }
        
        @Test
        void rejectsNullVersion() {
            assertThrows(InvalidRequest.class, () -> service.exportFormats((DatasetVersion) null, List.of()));
            verifyNoInteractions(cache, pipeline);
        }
        
        @Test
        void rejectsDraftsAsNotCacheable() {
            assertThrows(InvalidRequest.class, () -> service.exportFormats(draftVersion(), List.of()));
            verifyNoInteractions(cache, pipeline);
        }
        
        @Test
        void rejectsUnknownFormatsBeforeDoingAnyWork() {
            DatasetVersion version = releasedVersion();
            doThrow(new InvalidRequest("unknown")).when(registry).requireAllExist(List.of(UNKNOWN));
            
            assertThrows(InvalidRequest.class, () -> service.exportFormats(version, List.of(UNKNOWN)));
            verifyNoInteractions(cache, pipeline);
        }
        
        @Test
        void evictsAllRequestedFormatsBeforeProducingAny() throws IOException {
            DatasetVersion version = releasedVersion();
            registerExporters(BASE, DERIVED);
            
            service.exportFormats(version, List.of(BASE, DERIVED));
            
            InOrder inOrder = inOrder(cache, pipeline);
            inOrder.verify(cache).evict(dataset, keyOf(version, BASE));
            inOrder.verify(cache).evict(dataset, keyOf(version, DERIVED));
            inOrder.verify(pipeline).produceAndCache(version, keyOf(version, BASE));
            inOrder.verify(pipeline).produceAndCache(version, keyOf(version, DERIVED));
        }
        
        @Test
        void producesInTopologicalOrderRegardlessOfRequestOrder() throws IOException {
            DatasetVersion version = releasedVersion();
            registerExporters(BASE, DERIVED);
            
            service.exportFormats(version, List.of(DERIVED, BASE));
            
            InOrder inOrder = inOrder(pipeline);
            inOrder.verify(pipeline).produceAndCache(version, keyOf(version, BASE));
            inOrder.verify(pipeline).produceAndCache(version, keyOf(version, DERIVED));
        }
        
        @Test
        void emptyListExportsEveryRegisteredFormat() throws IOException {
            DatasetVersion version = releasedVersion();
            stubRegisteredDetails(BASE, DERIVED, STANDALONE);
            registerExporters(BASE, DERIVED, STANDALONE);
            
            service.exportFormats(version, List.of());
            
            verify(pipeline).produceAndCache(version, keyOf(version, BASE));
            verify(pipeline).produceAndCache(version, keyOf(version, DERIVED));
            verify(pipeline).produceAndCache(version, keyOf(version, STANDALONE));
            verifyNoMoreInteractions(pipeline);
        }
        
        @Test
        void dependentsOfRequestedFormatsAreRegeneratedToo() throws IOException {
            DatasetVersion version = releasedVersion();
            when(registry.getTransitiveDependents(BASE)).thenReturn(Set.of(DERIVED));
            registerExporters(BASE, DERIVED);
            
            service.exportFormats(version, List.of(BASE));
            
            verify(pipeline).produceAndCache(version, keyOf(version, DERIVED));
        }
        
        @Test
        void keepsExportingAfterFailureThenReportsFailedFormatsOnly() throws IOException {
            DatasetVersion version = releasedVersion();
            registerExporters(BASE, STANDALONE);
            ExportCacheKey baseKey = keyOf(version, BASE);
            doThrow(new IOException("disk full")).when(pipeline).produceAndCache(version, baseKey);
            
            InternalFailure ex = assertThrows(InternalFailure.class,
                () -> service.exportFormats(version, List.of(BASE, STANDALONE)));
            
            assertAll(
                () -> assertTrue(ex.getMessage().contains(BASE)),
                () -> assertFalse(ex.getMessage().contains(STANDALONE)),
                () -> verify(pipeline).produceAndCache(version, keyOf(version, STANDALONE))
            );
        }
    }
    
    @Nested
    class ExportingFormatsOfDataset {
        
        @BeforeEach
        void registryOrdersTopologically() {
            lenient().when(registry.getTopologicalComparator())
                .thenReturn(Comparator.comparingInt(e -> TOPOLOGICAL_ORDER.indexOf(e.getFormatName())));
        }
        
        @Test
        void rejectsNullDataset() {
            assertThrows(InvalidRequest.class, () -> service.exportFormats((Dataset) null, List.of()));
        }
        
        @Test
        void rejectsNullFormatName() {
            assertThrows(InvalidRequest.class, () -> service.exportFormat(dataset, null));
            verifyNoInteractions(registry, cache, pipeline);
        }
        
        @Test
        void exportAllFormatsUsesReleasedVersionAndStampsDataset() throws IOException {
            DatasetVersion released = releasedDataset();
            stubRegisteredDetails(BASE);
            registerExporters(BASE);
            Date before = new Date();
            
            service.exportAllFormats(dataset);
            
            ArgumentCaptor<Date> stamp = ArgumentCaptor.forClass(Date.class);
            verify(pipeline).produceAndCache(released, keyOf(released, BASE));
            verify(dataset).setLastExportTime(stamp.capture());
            assertFalse(stamp.getValue().before(before));
        }
        
        @Test
        void singleFormatIsExportedButNotStamped() throws IOException {
            DatasetVersion released = releasedDataset();
            registerExporters(STANDALONE);
            
            service.exportFormat(dataset, STANDALONE);
            
            verify(pipeline).produceAndCache(released, keyOf(released, STANDALONE));
            verify(dataset, never()).setLastExportTime(any());
        }
        
        @Test
        void unreleasedDatasetsAreRejectedAndNotStamped() {
            draftOnlyDataset();
            
            assertThrows(InvalidRequest.class, () -> service.exportFormat(dataset, BASE));
            verify(dataset, never()).setLastExportTime(any());
        }
        
        @Test
        void failedExportLeavesTimestampUntouched() throws IOException {
            DatasetVersion released = releasedDataset();
            registerExporters(BASE);
            ExportCacheKey baseKey = keyOf(released, BASE);
            doThrow(new IOException("disk full")).when(pipeline).produceAndCache(released, baseKey);
            
            assertThrows(InternalFailure.class, () -> service.exportFormat(dataset, BASE));
            verify(dataset, never()).setLastExportTime(any());
        }
    }
    
    // ++++ ++++ ++++ HELPERS & POLICIES ++++ ++++ ++++
    
    @Nested
    class WithTransitiveDependents {
        
        @ParameterizedTest
        @NullAndEmptySource
        void nullOrEmptyYieldsEmptyWithoutRegistryLookup(List<String> formatNames) {
            assertTrue(service.withTransitiveDependents(formatNames).isEmpty());
            verifyNoInteractions(registry);
        }
        
        @Test
        void addsDependentsOnceWhileKeepingRequestOrder() {
            when(registry.getTransitiveDependents(BASE)).thenReturn(Set.of(DERIVED));
            when(registry.getTransitiveDependents(DERIVED)).thenReturn(Set.of());
            when(registry.getTransitiveDependents(STANDALONE)).thenReturn(Set.of());
            
            List<String> result = service.withTransitiveDependents(List.of(DERIVED, BASE, STANDALONE));
            
            assertAll(
                () -> assertEquals(List.of(DERIVED, BASE, STANDALONE), result),
                () -> assertThrows(UnsupportedOperationException.class, () -> result.add("intruder"))
            );
        }
    }
    
    @Nested
    class Policies {
        
        @ParameterizedTest
        @EnumSource(VersionState.class)
        void onlyDraftsAreNotCacheable(VersionState state) {
            assertEquals(state != VersionState.DRAFT, ExportServiceBean.isCacheable(version(state)));
        }
        
        @Test
        void defaultVersionPrefersReleasedVersion() {
            DatasetVersion released = releasedDataset();
            assertSame(released, ExportServiceBean.defaultVersion(dataset));
        }
        
        @Test
        void defaultVersionFallsBackToLatestVersion() {
            DatasetVersion draft = draftOnlyDataset();
            assertSame(draft, ExportServiceBean.defaultVersion(dataset));
        }
    }
    
    // ++++ ++++ ++++ FIXTURES ++++ ++++ ++++
    
    private DatasetVersion releasedVersion() {
        return version(VersionState.RELEASED);
    }
    
    private DatasetVersion draftVersion() {
        return version(VersionState.DRAFT);
    }
    
    private DatasetVersion version(VersionState state) {
        DatasetVersion version = new DatasetVersion();
        version.setDataset(dataset);
        version.setVersionState(state);
        version.setVersionNumber(1L);
        version.setMinorVersionNumber(0L);
        return version;
    }
    
    /** Dataset mock behaves as released; lenient because not every code path consults both accessors. */
    private DatasetVersion releasedDataset() {
        DatasetVersion released = releasedVersion();
        lenient().when(dataset.isReleased()).thenReturn(true);
        lenient().when(dataset.getReleasedVersion()).thenReturn(released);
        return released;
    }
    
    /** Dataset mock behaves as never released, its latest version being a draft. */
    private DatasetVersion draftOnlyDataset() {
        DatasetVersion draft = draftVersion();
        lenient().when(dataset.isReleased()).thenReturn(false);
        lenient().when(dataset.getLatestVersion()).thenReturn(draft);
        return draft;
    }
    
    private static ExportCacheKey keyOf(DatasetVersion version, String formatName) {
        return new ExportCacheKey(version, formatName);
    }
    
    private static InputStream utf8(String content) {
        return new ByteArrayInputStream(content.getBytes(UTF_8));
    }
    
    /** Makes {@code registry.getDetails()} report the given formats, which the service reads as "all formats". */
    private void stubRegisteredDetails(String... formatNames) {
        List<Details> details = Arrays.stream(formatNames)
            .<Details>map(name -> new ExporterDetails(name.toUpperCase(Locale.ROOT), name, "application/" + name, true, true))
            .toList();
        when(registry.getDetails()).thenReturn(details);
    }
    
    /** Registers minimal exporter mocks: only format name and registry lookup are needed by the service. */
    private void registerExporters(String... formatNames) {
        for (String formatName : formatNames) {
            Exporter exporter = mock(Exporter.class);
            when(exporter.getFormatName()).thenReturn(formatName);
            when(registry.get(formatName)).thenReturn(Optional.of(exporter));
        }
    }
    
    private static Details detailsOf(String formatName) {
        return new ExporterDetails(formatName.toUpperCase(Locale.ROOT), formatName,
            "application/" + formatName, true, true);
    }
    
    // TODO: using this in multiple places now, add as test utility?
    /** Caller-owned response stream: records whether the service (or a plugin) flushed or closed it. */
    private static final class RecordingOutputStream extends ByteArrayOutputStream {
        private boolean closed;
        private boolean flushed;
        
        @Override
        public void flush() {
            flushed = true;
        }
        
        @Override
        public void close() {
            closed = true;
        }
    }
    
    // TODO: using this in multiple places now, add as test utility?
    /** Stands in for a disconnected client. */
    private static final class FailingOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            throw new IOException("broken pipe");
        }
        
        @Override
        public void flush() throws IOException {
            throw new IOException("broken pipe");
        }
    }
    
    // TODO: using this in multiple places now, add as test utility?
    /** Export stream that remembers being closed, to prove the context releases what a plugin abandons. */
    private static final class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed;
        
        private TrackingInputStream(String content) {
            super(content.getBytes(UTF_8));
        }
        
        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
    
}