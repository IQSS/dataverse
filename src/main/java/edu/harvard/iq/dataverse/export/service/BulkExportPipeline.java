package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.util.StreamRegistry;
import io.gdcc.spi.export.caps.bulk.BulkDatasetContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.LongFunction;
import java.util.logging.Logger;

/**
 * Platform-side {@link BulkDatasetContext}: a lazy view over a batch of dataset versions.
 * <p>
 * <b>Cost of re-reading:</b> every call to {@link Item#writeTo(OutputStream)} or {@link Item#open()} resolves the
 * export afresh, which for a cached version is a storage read and for a draft is a full re-export. Exporters that
 * need the same bytes twice are better off buffering them themselves, where they can size the trade-off knowingly.
 * <p>
 * <b>Stream ownership:</b> streams handed out by {@link Item#open()} should be closed by the exporter, but need
 * not be: every stream opened through this context is tracked and closed when the context closes. Closing twice is
 * harmless. {@link Item#writeTo(OutputStream)} closes its stream itself and never accumulates resources.
 * <p>
 * <b>Threading:</b> stream bookkeeping is synchronized, so an exporter distributing items across threads will not
 * corrupt this context or leak streams. Individual streams returned by {@link Item#open()} are not thread-safe and
 * must not be shared between threads.
 *
 * @implNote The resolver deliberately yields an {@link InputStream} rather than a re-openable handle: today every
 *           export is fully materialized before the stream is returned, so re-reading simply means resolving again.
 *           Should cheap re-reads or prefetching ever be required, this is the seam to change. Hand in something
 *           re-openable (a {@code Path}, say) and memorize per item, at the price of temp-file lifetime management.
 */
final class BulkExportPipeline implements BulkDatasetContext, AutoCloseable {
    
    private static final Logger logger = Logger.getLogger(BulkExportPipeline.class.getCanonicalName());
    
    private final List<Item> items;
    private final LongFunction<InputStream> resolver;
    private final String correlationId;
    private final StreamRegistry streams;
    
    /**
     * @param targets       the batch, in the order requested by the API caller; must not be null
     * @param resolver      opens the single-dataset export for a given dataset version id, in its own transaction
     * @param correlationId included in all log records and error messages of this batch
     */
    BulkExportPipeline(List<ExportTarget> targets, LongFunction<InputStream> resolver, String correlationId) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.correlationId = Objects.requireNonNull(correlationId);
        this.items = Objects.requireNonNull(targets, "targets must not be null")
            .stream()
            .<Item>map(TargetItem::new)
            .toList();
        this.streams = new StreamRegistry("bulk-export " + correlationId);
    }
    
    @Override
    public int size() {
        return items.size();
    }
    
    @Override
    public Iterable<Item> items() {
        return items;
    }
    
    /**
     * Resolves one item's export and tracks it, so it is released even if the exporter never closes it.
     * The export is fully produced before this returns, so a failure surfaces here rather than halfway through
     * writing, leaving the exporter free to substitute an error stub for the item.
     */
    private InputStream resolve(ExportTarget target) throws IOException {
        InputStream stream;
        try {
            stream = resolver.apply(target.versionId());
        } catch (RuntimeException ex) {
            // Surface as IOException: that is what the SPI declares, so exporters can actually catch it.
            throw new IOException("[" + correlationId + "] Could not provide export for " + target.persistentId(), ex);
        }
        if (stream == null) {
            throw new IOException("[" + correlationId + "] No export available for " + target.persistentId());
        }
        
        // Track the stream state, so it is released even if the exporter never closes it.
        // Note: if necessary to improve performance, we can add BufferedInputStream wrapping here later.
        return streams.track(stream);
    }
    
    /** Releases every stream the exporter left open, including after abandoned iteration or a client disconnect. */
    @Override
    public void close() {
        streams.close();
    }
    
    private final class TargetItem implements Item {
        
        private final ExportTarget target;
        
        private TargetItem(ExportTarget target) {
            this.target = target;
        }
        
        @Override
        public String persistentId() {
            return target.persistentId();
        }
        
        @Override
        public String versionNumber() {
            return target.versionNumber();
        }
        
        @Override
        public long writeTo(OutputStream output) throws IOException {
            if (output == null) {
                throw new IllegalArgumentException("Target output stream must not be null");
            }
            // Reach out to the context and trigger the resolving chain, applying the getExport lambda
            try (InputStream in = resolve(target)) {
                // Unbuffered on purpose: transferTo copies through its own buffer, and the tracking wrapper delegates
                // it, so interposing another buffer here would only add a memory copy per byte.
                return in.transferTo(output);
            }
        }
        
        @Override
        public InputStream open() throws IOException {
            // Remember: the resulting stream is tracked for closing!
            return resolve(target);
        }
    }
}