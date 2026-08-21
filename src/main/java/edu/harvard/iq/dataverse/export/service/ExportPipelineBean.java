package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.util.SecureTempFiles;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Stateless EJB that orchestrates the end-to-end export pipeline for dataset versions.
 * <p>
 * This bean acts as the central coordinator between the export cache, the exporter registry,
 * and the individual format-specific exporters. Its responsibilities include:
 * <ul>
 *   <li>Serving cached exports after verifying their freshness against all registered
 *       {@link ExportCacheInvalidator} instances. A stale entry is evicted and reported as
 *       a cache miss, ensuring that no consumer (prerequisite resolution or direct retrieval)
 *       ever receives outdated bytes.</li>
 *   <li>Producing a new export by looking up the appropriate {@link Exporter} in the
 *       {@link ExporterRegistryBean}, resolving any declared prerequisite format recursively,
 *       and writing the result into the cache atomically.</li>
 *   <li>Detecting and rejecting circular prerequisite chains via an in-flight format set
 *       passed through the recursive resolution calls.</li>
 * </ul>
 * <p>
 * All data production paths (draft, cached, bulk) funnel through this bean, which means
 * that every export is subjected to the same staleness validation, prerequisite resolution,
 * and error-wrapping logic.
 * <p>
 * Field injection is used for the {@link ExportCache} dependency because EJB mandates a
 * no-args constructor; this is expected to be replaced with constructor injection when the
 * codebase transitions to CDI-only dependency management.
 *
 * @see ExporterRegistryBean
 * @see ExportCache
 * @see ExportCacheInvalidator
 * @see ExportServiceBean
 */
@Stateless
class ExportPipelineBean {
    
    @EJB
    ExporterRegistryBean registry;
    
    // We must use (frowned upon) field injection here, as EJB requires a no-args constructor.
    // When the codebase transitions to use CDI only, this shall be changed to constructor injection.
    @SuppressWarnings("java:S6813")
    @Inject
    ExportCache cache;
    
    /**
     * A collection of {@link ExportCacheInvalidator} instances.
     * This list is intended to centralize all invalidation mechanisms for export cache entries.
     * Any new implementations must be added here in addition to the "permits" on the interface seal.
     * <p>
     * Note: Once we allow plugins to provide their own invalidation logic, we must load them.
     * This static, non-CDI list shall then be replaced by a registry pattern following implementation.
     */
    static final List<ExportCacheInvalidator> invalidators = List.of(new FileEmbargoExpiryInvalidator());
    
    /**
     * Attempts to read a cached export for the given dataset version and cache key, verifying freshness through
     * registered invalidators before returning the stream.
     * <p>
     * If the dataset version is not cacheable, this method returns {@link Optional#empty()}
     * immediately without consulting the cache.
     * <p>
     * When a cached entry is found, all registered invalidators are consulted.
     * If any invalidator reports the entry as stale, a cache miss is signaled.
     *
     * @param datasetVersion the dataset version whose cached export is to be read; must not be null
     * @param key            the cache key identifying the target export format and cache location; must not be null
     * @return an {@link Optional} containing an open {@link InputStream} to the cached export data, or
     *         {@link Optional#empty()} if the version is not cacheable, no entry exists, or the entry was determined to be stale and evicted
     * @throws IllegalArgumentException if {@code datasetVersion} or {@code key} is null
     * @throws IOException if an I/O error occurs while closing a stale stream or evicting the cache entry
     */
    Optional<InputStream> readFreshCachedExport(DatasetVersion datasetVersion, ExportCacheKey key) throws IOException {
        if (datasetVersion == null || key == null) {
            throw new IllegalArgumentException("Dataset version and export cache key must not be null");
        }
        
        // Short-circuit if the version is not cacheable anyway
        if (!ExportServiceBean.isCacheable(datasetVersion)) {
            return Optional.empty();
        }
        
        Optional<InputStream> cached = cache.read(datasetVersion.getDataset(), key);
        
        if (cached.isPresent()) {
            try {
                // Apply all invalidators to see if the cache entry may be stale
                // TODO: In case we ever have longer prerequisite format chains, this naive appraoch will need refinement.
                //       The staleness checks may be expensive and repeated execution is not helpful.
                //       For now, this pipeline is *stateless*, so changing the procedure needs careful consideration.
                if (invalidators.stream().anyMatch(inv -> inv.isStale(datasetVersion, key))) {
                    // If this in fact is stale, evict, close the stream, and report back cache miss
                    cache.evict(datasetVersion.getDataset(), key);
                    cached.get().close(); // First evict, then close, in case closing throws.
                    return Optional.empty();
                }
            } catch (IOException | RuntimeException ex) {
                // Avoid leaking the stream, but never let the close failure mask the original exception
                try {
                    cached.get().close();
                } catch (IOException closeEx) {
                    ex.addSuppressed(closeEx);
                }
                throw ex;
            }
        }
        
        return cached;
    }
    
    /**
     * No caching variant to produce an export for the given dataset version in the requested format.
     * The produces metadata export will reside as a temporary file on disk, auto-deleted after consumption.
     * <p>
     * The requested format name must be registered in the export registry.
     * If the exporter declares a prerequisite format, it is resolved recursively before the export is produced.
     * Circular prerequisite chains are detected and rejected.
     * <p>
     * If the given dataset version does not satisfy {@link ExportServiceBean#isCacheable(DatasetVersion)},
     * the export and any prerequisite data formats will be generated on-the-fly.
     * (Prerequisite formats will have their own temporary files, destroyed after consumption)
     * <p>
     * If the dataset version is cacheable, it will still be written to a temporary file, but any prequisites
     * will be read from the cache. If the prerequisites are not yet cached, they are going to be cached here.
     * <p>
     * The caller is responsible for closing the returned input stream.
     *
     * @param datasetVersion the dataset version whose metadata will be exported
     * @param formatName     the name of the export format to produce; must be a registered format
     * @throws IllegalArgumentException if the dataset version or output stream is null,
     *                                  if no exporter is registered for the format, or
     *                                  if a prerequisite cycle is detected
     * @throws ExportException if the prerequisite format resolution fails, or
     *                         if the exporter throws an {@link IllegalStateException}
     */
    InputStream readFreshExport(DatasetVersion datasetVersion, String formatName) throws IOException {
        if (datasetVersion == null) {
            throw new IllegalArgumentException("datasetVersion must not be null");
        }
        registry.requireExists(formatName);
        
        return produceToTempFile(formatName, datasetVersion, new LinkedHashSet<>());
    }
    
    /**
     * Produces an export for the given dataset version and writes the result through to the export cache.
     *
     * @param datasetVersion the dataset version whose metadata will be exported
     * @param key            the cache key identifying the target export format and cache location
     * @throws IllegalArgumentException argument validation fails
     * @throws ExportException if an error occurs during export in {@link #produce(String, DatasetVersion, OutputStream, Set)}
     * @throws IOException if an I/O error occurs while writing the export to the cache
     */
    void produceAndCache(DatasetVersion datasetVersion, ExportCacheKey key) throws IOException {
        if (datasetVersion == null || key == null) {
            throw new IllegalArgumentException("Neither dataset version nor cache key may be null");
        }
        
        cache.write(
            datasetVersion.getDataset(),
            key,
            // The trick here: by creating a lambda, use the input from the functional interface the cache provides.
            //                 This way, the cache owns all the I/O going on.
            out -> produce(key.formatName(), datasetVersion, out, new LinkedHashSet<>())
        );
    }
    
    /**
     * Produces a single export for the given dataset version by delegating to the registered exporter for the
     * requested format, writing the result to the supplied output stream.
     * <p>
     * If the exporter declares a prerequisite format, this method resolves that prerequisite recursively via
     * {@link #resolvePrerequisite(String, DatasetVersion, Set)}, before invoking the exporter's export logic.
     * The in-flight set is used to detect circular prerequisite chains and throws an {@link ExportException} if a cycle is found.
     * <p>
     * The requested format name is added to the in-flight set at entry and removed in a "finally" block, ensuring the
     * set is left in its original state regardless of whether the export succeeds or fails.
     *
     * @param formatName  the name of the export format to produce
     * @param version     the dataset version whose metadata will be exported
     * @param out         the output stream to write the produced export to; the caller is
     *                    responsible for closing it
     * @param inFlight    a set of format names currently being produced along the prerequisite
     *                    resolution chain; used to detect and reject circular dependencies
     * @throws IllegalArgumentException if no exporter is registered for the format,
     *                                  if a prerequisite cycle is detected, or
     *                                  if the output stream is null
     * @throws ExportException if the exporter throws an {@link IllegalStateException} or
     *                         if prerequisite format resolution fails
     *
     */
    private void produce(String formatName, DatasetVersion version, OutputStream out, Set<String> inFlight) {
        // version is null checked before, inFlight is injected by the caller. This is a private method, no additional checks necessary.
        if (out == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        
        // Try retrieving the exporter for the requested format
        Exporter exporter = registry.get(formatName).orElseThrow(() -> new IllegalArgumentException("No such exporter available for format " + formatName));
        
        // Add current requested format to the set of formats requested before for this dataset version.
        if (!inFlight.add(formatName)) {
            throw new IllegalArgumentException("Prerequisite cycle detected while exporting: " +
                String.join(" -> ", inFlight) +
                " -> " + formatName);
        }
        
        try {
            // Case A: No prerequisite format needed
            Optional<String> prereqFormatName = exporter.getPrerequisiteFormatName();
            if (prereqFormatName.isEmpty()) {
                exporter.exportDataset(new InternalExportDataProvider(version), out);
                return;
            }
            
            // Case B: Prerequisite format needed, recursively resolve, then export
            try (InputStream prereqStream = resolvePrerequisite(prereqFormatName.get(), version, inFlight)) {
                exporter.exportDataset(new InternalExportDataProvider(version, prereqStream), out);
            } catch (IOException ioe) {
                throw new ExportException("Could not provide prerequisite " + prereqFormatName.get() +
                    " to create " + formatName + " export for dataset " +
                    version.getDataset().getId(), ioe);
            }
        } catch (IllegalStateException ise) {
            /* @landreev 2023-04-23:
             * IllegalStateException can potentially mean very different, and unexpected things.
             * An exporter attempting to get a single primitive value from a fieldDTO that is, in fact, a multiple and
             * contains a JSON vector will result in an IllegalStateException.
             * This has happened, for example, when the code in the DDI exporter was not updated following a
             * metadata field type change.
             * Wrap it here so ALL data production paths (draft, cached, bulk) report it usefully.
             */
            throw new ExportException("IllegalStateException caught when exporting "
                + formatName + " for dataset "
                + version.getDataset().getGlobalId().toString()
                + "; may or may not be due to a mismatch between exporter code "
                + "and a metadata block update. " + ise.getMessage(), ise);
        } finally {
            inFlight.remove(formatName);
        }
    }
    
    /**
     * Provides the prerequisite export for a derived format.
     * <p>
     * In case a complete chain of prereq formats are needed, a recursive stack is used to iterate through it,
     * calling {@link #produce(String, DatasetVersion, OutputStream, Set)} on the prereq format.
     * <p>
     * For cacheable versions the cached entry is used if present and fresh.
     * On a miss the prerequisite is produced and written through to the cache.
     * (The bytes a derived export was built from are the same bytes subsequently served for the prerequisite format).
     * <p>
     * Non-cacheable versions (drafts) are always produced fresh, see cache policy at {@link ExportServiceBean#isCacheable(DatasetVersion)}.
     *
     * @param prereqFormatName  the name of the export format to produce
     * @param version the dataset version whose metadata will be exported
     * @param inFlight a set of format names currently being produced along the prerequisite
     *                 resolution chain; used to detect and reject circular dependencies
     * @return open stream to the exported metadata, which the caller must close
     */
    private InputStream resolvePrerequisite(String prereqFormatName, DatasetVersion version, Set<String> inFlight) throws IOException {
        // Note: Intentionally no checks for null parameters or writability of the set here.
        //       This is an internal method, and any calls are in this class, which hopefully provides enough control.
        
        // Non-cacheable versions are always created fresh
        if (!ExportServiceBean.isCacheable(version)) {
            return produceToTempFile(prereqFormatName, version, inFlight);
        }
        
        // If cacheable, try to read from the cache (will also trigger full invalidator chain!)
        ExportCacheKey key = new ExportCacheKey(version, prereqFormatName);
        Optional<InputStream> cached = readFreshCachedExport(version, key);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // If not in cache, produce and cache, return resulting data stream
        // TODO: This write-then-read is not atomic, which might lead to a race condition, also we already try to run exports in topological order.
        //       Consider adding a ExportCache.writeThenRead() function which ensures atomicity in the implementation.
        //       Alternatively, lock-by-key may be used inside the ExportCache.
        cache.write(version.getDataset(), key, out -> produce(prereqFormatName, version, out, inFlight));
        return cache
            .read(version.getDataset(), key)
            .orElseThrow(() -> new ExportException("Prerequisite " + prereqFormatName + " was produced but could not be read back"));
    }
    
    /**
     * Produces an export for the given (non-cacheable) dataset version by writing the result to a secure temporary file,
     * then returns an input stream over that file. This especially avoids huge blips in memory usage for drafts.
     * <p>
     * The temporary file is created with owner-only permissions and opened with {@link StandardOpenOption#DELETE_ON_CLOSE},
     * so the file is automatically removed when the caller closes the returned stream.
     * <p>
     * If an exception is thrown before the stream is handed back, the temporary file is deleted immediately to avoid
     * leaving orphaned files on disk.
     * <p>
     * TODO: Using temporary files will leave things behind when the JVM crashes.
     *       If we ever think this may become a problem (given that java.io.tmp dir should be cleaned up by the OS),
     *       we can always add something to an @Startup EJB.
     *
     * @return an open {@link InputStream} to the temporary file containing the produced export data;
     *         the caller is responsible for closing it, which also deletes the temporary file
     */
    private InputStream produceToTempFile(String formatName, DatasetVersion version, Set<String> inFlight) throws IOException {
        Path tempFile = SecureTempFiles.createOwnerOnlyTempFile("dataverse-export-draft-", ".tmp");
        try {
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
                // Note: Any prerequisites are recursively produced on demand, in addition to the original target format.
                //       If the dataset version can be cached, a read attempt for prerequisites will be made.
                produce(formatName, version, out, inFlight);
            }
            // The returned stream deletes the file on close.
            // Note: The only caller (produce(), Case B) already closes it via try-with-resources.
            return Files.newInputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE);
        } catch (IOException | RuntimeException e) {
            // Export failed before the stream existed: nobody will ever close it, delete now.
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException del) {
                e.addSuppressed(del);
            }
            throw e;
        }
    }
    
}
