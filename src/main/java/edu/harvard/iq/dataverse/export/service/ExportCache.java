package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import io.gdcc.spi.export.ExportException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Storage abstraction for cached metadata exports. Implementations own all
 * knowledge about where and under which names cached exports live; the export
 * pipeline only ever deals in {@link ExportCacheKey}s and streams.
 */
public sealed interface ExportCache permits StorageIOCache {
    
    /**
     * Looks up a cached export.
     * @return the cached export stream, or empty if none is cached. Note: the caller is responsible for closing the stream.
     * @throws IOException on actual storage failures (not on a cache miss)
     */
    Optional<InputStream> read(ExportCacheKey key) throws IOException;
    
    /**
     * Produces and stores an export. The {@code writer} callback receives the output stream to write to.
     * Any implementations guarantee that a partially written export is never made visible under the cache key
     * (i.e., a failed write leaves either the previous entry or no entry).
     */
    void write(ExportCacheKey key, ExportStreamWriter writer) throws ExportException, IOException;
    
    /** Removes a cached export. Absence of the entry is not an error. */
    void evict(ExportCacheKey key) throws IOException;
    
    /**
     * Removes all cached exports for a dataset, across all versions and formats, including legacy (pre-versioning) entries.
     * Intended for publish/deaccession hooks and the admin "reexport" API.
     */
    void evictAll(Dataset dataset) throws IOException;
    
    /** Callback that renders an export into the store-provided stream. */
    @FunctionalInterface
    interface ExportStreamWriter {
        void writeTo(OutputStream out) throws ExportException, IOException;
    }
}
