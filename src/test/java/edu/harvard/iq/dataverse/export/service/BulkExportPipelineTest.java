
package edu.harvard.iq.dataverse.export.service;

import io.gdcc.spi.export.caps.bulk.BulkDatasetContext.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongFunction;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BulkExportPipelineTest {
    
    private static final String CORRELATION_ID = "batch-42";
    private static final ExportTarget FIRST = new ExportTarget(1L, "doi:10.5072/FK2/AAAAAA", "1.0");
    private static final ExportTarget SECOND = new ExportTarget(2L, "doi:10.5072/FK2/BBBBBB", "2.1");
    
    private final List<RecordingStream> handedOut = new ArrayList<>();
    private final AtomicInteger resolverCalls = new AtomicInteger();
    
    private LongFunction<InputStream> resolver;
    
    @BeforeEach
    void setUp() {
        resolver = versionId -> {
            resolverCalls.incrementAndGet();
            RecordingStream stream = new RecordingStream(payloadFor(versionId));
            handedOut.add(stream);
            return stream;
        };
    }
    
    private BulkExportPipeline pipelineOf(ExportTarget... targets) {
        return new BulkExportPipeline(List.of(targets), resolver, CORRELATION_ID);
    }
    
    private static byte[] payloadFor(long versionId) {
        return ("export-of-version-" + versionId).getBytes(UTF_8);
    }
    
    private static List<Item> itemsOf(BulkExportPipeline pipeline) {
        return StreamSupport.stream(pipeline.items().spliterator(), false).toList();
    }
    
    @Nested
    class Construction {
        
        @Test
        void rejectsNullTargets() {
            assertThrows(NullPointerException.class, () -> new BulkExportPipeline(null, resolver, CORRELATION_ID));
        }
        
        @Test
        void rejectsNullResolver() {
            assertThrows(NullPointerException.class, () -> new BulkExportPipeline(List.of(FIRST), null, CORRELATION_ID));
        }
        
        @Test
        void rejectsNullCorrelationId() {
            assertThrows(NullPointerException.class, () -> new BulkExportPipeline(List.of(FIRST), resolver, null));
        }
        
        @Test
        void doesNotResolveEagerly() {
            pipelineOf(FIRST, SECOND);
            
            assertEquals(0, resolverCalls.get());
        }
    }
    
    @Nested
    class Items {
        
        @Test
        void sizeMatchesTargets() {
            try (BulkExportPipeline pipeline = pipelineOf(FIRST, SECOND)) {
                assertEquals(2, pipeline.size());
            }
        }
        
        @Test
        void emptyBatchYieldsNoItems() {
            try (BulkExportPipeline pipeline = pipelineOf()) {
                assertEquals(0, pipeline.size());
                assertTrue(itemsOf(pipeline).isEmpty());
            }
        }
        
        @Test
        void exposeLabelsInRequestedOrder() {
            try (BulkExportPipeline pipeline = pipelineOf(FIRST, SECOND)) {
                List<Item> items = itemsOf(pipeline);
                
                assertEquals(List.of(FIRST.persistentId(), SECOND.persistentId()),
                    items.stream().map(Item::persistentId).toList());
                assertEquals(List.of(FIRST.versionNumber(), SECOND.versionNumber()),
                    items.stream().map(Item::versionNumber).toList());
            }
        }
        
        @Test
        void areIterableMoreThanOnce() {
            try (BulkExportPipeline pipeline = pipelineOf(FIRST)) {
                assertEquals(itemsOf(pipeline).size(), itemsOf(pipeline).size());
            }
        }
    }
    
    @Nested
    class WriteTo {
        
        @Test
        void copiesExportAndReportsByteCount() throws IOException {
            try (BulkExportPipeline pipeline = pipelineOf(SECOND)) {
                ByteArrayOutputStream sink = new ByteArrayOutputStream();
                
                long written = itemsOf(pipeline).getFirst().writeTo(sink);
                
                assertArrayEqualsAsText(payloadFor(SECOND.versionId()), sink.toByteArray());
                assertEquals(payloadFor(SECOND.versionId()).length, written);
            }
        }
        
        @Test
        void closesSourceStreamItself() throws IOException {
            try (BulkExportPipeline pipeline = pipelineOf(FIRST)) {
                itemsOf(pipeline).getFirst().writeTo(new ByteArrayOutputStream());
                
                assertTrue(handedOut.getFirst().isClosed());
            }
        }
        
        @Test
        void resolvesAfreshOnEveryCall() throws IOException {
            try (BulkExportPipeline pipeline = pipelineOf(FIRST)) {
                Item item = itemsOf(pipeline).getFirst();
                
                item.writeTo(new ByteArrayOutputStream());
                item.writeTo(new ByteArrayOutputStream());
                
                assertEquals(2, resolverCalls.get());
            }
        }
        
        @Test
        void rejectsNullOutput() {
            try (BulkExportPipeline pipeline = pipelineOf(FIRST)) {
                Item item = itemsOf(pipeline).getFirst();
                
                assertThrows(IllegalArgumentException.class, () -> item.writeTo(null));
                assertEquals(0, resolverCalls.get());
            }
        }
    }
    
    @Nested
    class Open {
        
        @Test
        void yieldsTheExportedBytes() throws IOException {
            try (BulkExportPipeline pipeline = pipelineOf(FIRST)) {
                try (InputStream in = itemsOf(pipeline).getFirst().open()) {
                    assertArrayEqualsAsText(payloadFor(FIRST.versionId()), in.readAllBytes());
                }
            }
        }
        
        @Test
        void allowsRedundantClosingByTheExporter() throws IOException {
            try (BulkExportPipeline pipeline = pipelineOf(FIRST)) {
                InputStream in = itemsOf(pipeline).getFirst().open();
                in.close();
                
                assertDoesNotThrow(in::close);
            }
        }
    }
    
    @Nested
    class FailingResolution {
        
        @Test
        void wrapsResolverFailureInIoException() {
            RuntimeException cause = new IllegalStateException("no persistence context");
            resolver = versionId -> {
                throw cause;
            };
            
            try (BulkExportPipeline pipeline = pipelineOf(FIRST)) {
                Item item = itemsOf(pipeline).getFirst();
                
                IOException failure = assertThrows(IOException.class, item::open);
                assertEquals(cause, failure.getCause());
                assertTrue(failure.getMessage().contains(CORRELATION_ID));
                assertTrue(failure.getMessage().contains(FIRST.persistentId()));
            }
        }
        
        @Test
        void reportsMissingExportAsIoException() {
            resolver = versionId -> null;
            
            try (BulkExportPipeline pipeline = pipelineOf(FIRST)) {
                Item item = itemsOf(pipeline).getFirst();
                
                IOException failure = assertThrows(IOException.class,
                    () -> item.writeTo(new ByteArrayOutputStream()));
                assertTrue(failure.getMessage().contains(FIRST.persistentId()));
            }
        }
        
        @Test
        void leavesOtherItemsUsable() throws IOException {
            resolver = versionId -> {
                if (versionId == FIRST.versionId()) {
                    throw new IllegalStateException("boom");
                }
                return new RecordingStream(payloadFor(versionId));
            };
            
            try (BulkExportPipeline pipeline = pipelineOf(FIRST, SECOND)) {
                List<Item> items = itemsOf(pipeline);
                ByteArrayOutputStream sink = new ByteArrayOutputStream();
                
                assertInstanceOf(IOException.class,
                    assertThrows(IOException.class, () -> items.get(0).writeTo(new ByteArrayOutputStream())));
                assertEquals(payloadFor(SECOND.versionId()).length, items.get(1).writeTo(sink));
            }
        }
    }
    
    @Nested
    class Closing {
        
        @Test
        void releasesStreamsTheExporterAbandoned() throws IOException {
            BulkExportPipeline pipeline = pipelineOf(FIRST, SECOND);
            for (Item item : pipeline.items()) {
                item.open();
            }
            
            pipeline.close();
            
            assertEquals(2, handedOut.size());
            assertTrue(handedOut.stream().allMatch(RecordingStream::isClosed));
        }
        
        @Test
        void isIdempotent() {
            // Given
            BulkExportPipeline pipeline = pipelineOf(FIRST);
            pipeline.items().forEach(item -> {
                try (InputStream stream = item.open()) {
                    stream.read();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            
            // When & Then
            // Double close should not throw (idempotent)
            assertDoesNotThrow(pipeline::close);
            assertDoesNotThrow(pipeline::close);
            // For good measure, make sure the stream is closed, too (after we opened it before)
            assertTrue(handedOut.getFirst().isClosed());
        }
        
        @Test
        void succeedsWithoutAnyOpenStream() {
            BulkExportPipeline pipeline = pipelineOf(FIRST);
            
            assertDoesNotThrow(pipeline::close);
            assertFalse(handedOut.stream().anyMatch(stream -> !stream.isClosed()));
        }
    }
    
    private static void assertArrayEqualsAsText(byte[] expected, byte[] actual) {
        assertEquals(new String(expected, UTF_8), new String(actual, UTF_8));
    }
    
    /** Minimal in-memory export that remembers whether the pipeline (or the exporter) closed it. */
    private static final class RecordingStream extends ByteArrayInputStream {
        
        private boolean closed;
        
        private RecordingStream(byte[] payload) {
            super(payload);
        }
        
        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
        
        private boolean isClosed() {
            return closed;
        }
    }
}