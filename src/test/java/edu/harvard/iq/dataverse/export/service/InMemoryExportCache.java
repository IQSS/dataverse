package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import io.gdcc.spi.export.ExportException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory {@link ExportCache} for unit tests.
 * <p>
 * Behaves like {@link StorageIOCache} from the pipeline's point of view:
 * <ul>
 *   <li>{@link #write} executes the writer for real; an entry appears only if the writer completes without throwing.</li>
 *   <li>{@link #evict} of a missing entry is silent.</li>
 *   <li>Streams returned by {@link #read} are tracked so tests can assert they were closed.</li>
 * </ul>
 * Failure injection via {@link #failReadsWith}, {@link #failWritesWith}, {@link #failEvictsWith}.
 * Not thread-safe; intended for single-threaded tests.
 */
final class InMemoryExportCache implements ExportCache {
    
    private final Map<String, byte[]> store = new LinkedHashMap<>();
    
    private final AtomicInteger reads = new AtomicInteger();
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicInteger writes = new AtomicInteger();
    private final AtomicInteger writerInvocations = new AtomicInteger();
    private final AtomicInteger evictions = new AtomicInteger();
    private final AtomicInteger openStreams = new AtomicInteger();
    
    private IOException readFailure;
    private IOException writeFailure;
    private IOException evictFailure;
    private IOException streamCloseFailure;
    private boolean swallowWrites;
    
    // ---- ExportCache -------------------------------------------------------------------------
    
    @Override
    public Optional<InputStream> read(Dataset dataset, ExportCacheKey key) throws IOException {
        reads.incrementAndGet();
        if (readFailure != null) {
            throw readFailure;
        }
        byte[] bytes = store.get(storeKey(dataset, key));
        if (bytes == null) {
            return Optional.empty();
        }
        hits.incrementAndGet();
        openStreams.incrementAndGet();
        return Optional.of(new TrackingInputStream(bytes));
    }
    
    @Override
    public void write(Dataset dataset, ExportCacheKey key, ExportStreamWriter writer) throws ExportException, IOException {
        writes.incrementAndGet();
        if (writeFailure != null) {
            throw writeFailure;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writerInvocations.incrementAndGet();
        // Any exception here propagates and leaves the store untouched - same contract as StorageIOCache.
        writer.writeTo(buffer);
        if (!swallowWrites) {
            store.put(storeKey(dataset, key), buffer.toByteArray());
        }
    }
    
    @Override
    public void evict(Dataset dataset, ExportCacheKey key) throws IOException {
        evictions.incrementAndGet();
        if (evictFailure != null) {
            throw evictFailure;
        }
        store.remove(storeKey(dataset, key));
    }
    
    @Override
    public void evictAll(Dataset dataset) throws IOException {
        if (evictFailure != null) {
            throw evictFailure;
        }
        String prefix = datasetPrefix(dataset);
        store.keySet().removeIf(k -> k.startsWith(prefix));
    }
    
    @Override
    public long usedStorage(Dataset dataset) {
        String prefix = datasetPrefix(dataset);
        return store.entrySet().stream()
            .filter(e -> e.getKey().startsWith(prefix))
            .mapToLong(e -> e.getValue().length)
            .sum();
    }
    
    // ---- Test helpers: seeding & inspection --------------------------------------------------
    
    /** Pre-seed an entry, e.g. to test cache hits or staleness handling without running an exporter. */
    void seed(Dataset dataset, ExportCacheKey key, String content) {
        store.put(storeKey(dataset, key), content.getBytes(StandardCharsets.UTF_8));
    }
    
    boolean contains(Dataset dataset, ExportCacheKey key) {
        return store.containsKey(storeKey(dataset, key));
    }
    
    Optional<String> contentOf(Dataset dataset, ExportCacheKey key) {
        return Optional.ofNullable(store.get(storeKey(dataset, key)))
            .map(b -> new String(b, StandardCharsets.UTF_8));
    }
    
    boolean isEmpty() {
        return store.isEmpty();
    }
    
    int size() {
        return store.size();
    }
    
    int reads()             { return reads.get(); }
    int hits()              { return hits.get(); }
    int writes()            { return writes.get(); }
    int writerInvocations() { return writerInvocations.get(); }
    int evictions()         { return evictions.get(); }
    
    /** Number of streams handed out by {@link #read} that have not been closed yet. */
    int openStreams() {
        return openStreams.get();
    }
    
    // ---- Test helpers: failure injection -----------------------------------------------------
    
    InMemoryExportCache failReadsWith(IOException e)   { this.readFailure = e;  return this; }
    InMemoryExportCache failWritesWith(IOException e)  { this.writeFailure = e; return this; }
    InMemoryExportCache failEvictsWith(IOException e)  { this.evictFailure = e; return this; }
    
    /** Streams handed out by {@link #read} are marked closed, then throw on {@code close()}. */
    InMemoryExportCache failStreamClosesWith(IOException e) { this.streamCloseFailure = e; return this; }
    
    /** Writers are executed, but nothing is stored: simulates a cache that silently loses data. */
    InMemoryExportCache swallowWrites() { this.swallowWrites = true; return this; }
    
    InMemoryExportCache clearFailures() {
        readFailure = writeFailure = evictFailure = null;
        return this;
    }
    
    // ---- Internals ---------------------------------------------------------------------------
    
    private static String datasetPrefix(Dataset dataset) {
        // Datasets in tests often have no id; fall back to identity so two distinct datasets never collide.
        String id = dataset.getId() != null ? dataset.getId().toString() : "@" + System.identityHashCode(dataset);
        return id + "/";
    }
    
    private static String storeKey(Dataset dataset, ExportCacheKey key) {
        return datasetPrefix(Objects.requireNonNull(dataset)) + Objects.requireNonNull(key).auxTag();
    }
    
    private final class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed;
        
        TrackingInputStream(byte[] buf) {
            super(buf);
        }
        
        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                openStreams.decrementAndGet();
            }
            super.close();
            if (streamCloseFailure != null) {
                throw streamCloseFailure;
            }
        }
    }
}