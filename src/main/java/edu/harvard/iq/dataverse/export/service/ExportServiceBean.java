package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.export.service.ExportSystemException.InternalFailure;
import edu.harvard.iq.dataverse.export.service.ExportSystemException.InvalidRequest;
import edu.harvard.iq.dataverse.export.service.ExporterRegistryBean.Details;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.export.caps.bulk.BulkDatasetExporter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import org.apache.commons.io.output.CloseShieldOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Stateless
public class ExportServiceBean {

    private static final Logger logger = Logger.getLogger(ExportServiceBean.class.getCanonicalName());

    @EJB
    ExporterRegistryBean registry;
    
    /**
     * Self-reference used to re-enter this bean through its proxy.
     * Required wherever a transaction attribute must actually take effect: a plain {@code this.method()} call is
     * a self-invocation that bypasses the container's interceptor chain, silently ignoring {@code REQUIRES_NEW}.
     */
    @EJB
    ExportServiceBean self;
    
    @EJB
    DatasetVersionServiceBean versionService;
    
    // We must use (frowned upon) field injection here, as EJB requires a no-args constructor.
    // When the codebase transitions to use CDI only, this shall be changed to constructor injection.
    @SuppressWarnings("java:S6813")
    @Inject
    ExportCache cache;
    
    // We must use (frowned upon) field injection here, as EJB requires a no-args constructor.
    // When the codebase transitions to use CDI only, this shall be changed to constructor injection.
    @SuppressWarnings("java:S6813")
    @Inject
    ExportPipelineBean pipeline;
    
    // ++++ ++++ ++++ METHODS TO RETRIEVE EXPORTED DATA ++++ ++++ ++++
    
    /**
     * Retrieves a stream of the metadata export for the given dataset version in the specified format.
     * <p>
     * First checks for a fresh, cached export.
     * If none is available (usually because the dataset version is not able to be cached),
     * generates a fresh export by invoking the export pipeline and writing to a temporary location.
     * <p>
     * The caller is responsible for closing the returned {@link InputStream}.
     *
     * @param datasetVersion the dataset version to retrieve the export for; must not be null
     * @param formatName the name of the export format to retrieve; must not be null
     * @return an {@link InputStream} containing the export data for the requested format
     * @throws InternalFailure if the input stream for the metadata export cannot be retrieved due to underlying errors
     * @throws InvalidRequest if the request is malformed (unknown export format or version is null)
     */
    public InputStream getExport(DatasetVersion datasetVersion, String formatName) {
        // Note: we don't do validation here, as the lower layers will take care of it.
        try {
            ExportCacheKey key = new ExportCacheKey(datasetVersion, formatName);
            return pipeline.readFreshCachedExport(datasetVersion, key)
                           .orElse(pipeline.readFreshExport(datasetVersion, formatName));
        // ESEs are runtime exceptions, don't double-wrap
        } catch (ExportSystemException ex) {
            throw ex;
        // IOEs and any other runtime errors should not be wrapped in EJB-proxy errors, but our own, checked ones
        } catch (IOException | RuntimeException ex) {
            throw new InternalFailure("Export to String failed for dataset version " + datasetVersion.getId() +
                                      " to format " + formatName, ex);
        }
    }
    
    /**
     * Looks up a dataset version by id and returns its metadata export, in a transaction of its own.
     * <p>
     * The new transaction is essential rather than incidental:
     * a) the returned stream is consumed after the caller's transaction has ended, and
     * b) both production paths (to cache or temp file) need a live persistence context while they run.
     * What they return however, does not: a cached export is a storage object and a freshly produced one is a temp file.
     * Both remain readable long after the entity has been detached.
     * <p>
     * Must be invoked through an EJB bean proxy (either via {@link #self} or otherwise injected),
     * never as a plain self-invocation (the transaction annotation would be ignored).
     *
     * @param datasetVersionId the id of the dataset version to export
     * @param formatName the name of the export format to retrieve
     * @return an {@link InputStream} over the export; the caller is responsible for closing it
     * @throws InvalidRequest if no version with that id exists (it may have been destroyed meanwhile)
     * @throws InternalFailure if the export cannot be produced or read
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public InputStream getExportInNewTransaction(long datasetVersionId, String formatName) {
        DatasetVersion version = versionService.find(datasetVersionId);
        if (version == null) {
            throw new InvalidRequest("No dataset version found with id=" + datasetVersionId);
        }
        // Self-invocation on purpose: this must run inside the transaction opened above, not a further one.
        return getExport(version, formatName);
    }
    
    /**
     * Retrieves the latest published version of the given dataset as a String in the specified export format.
     * The export is read from the cache if available. Otherwise, it is generated on the fly.
     *
     * @param dataset the dataset whose latest released version is to be exported; must not be null
     * @param formatName the name of the export format to use; must not be null and registered
     * @return the latest published dataset content as a UTF-8 encoded String,
     *         or null if the dataset is null, no released version exists, or an I/O error occurs.
     * @apiNote TODO: While returning null is frowned upon in modern Java, it is necessary for backward compatibility.
     *                This method and any callers should be refactored to follow the "never return null on public API"
     *                principle going forward.
     * @throws InternalFailure if an error occurs during a non-cached, on-the-fly export
     * @throws InvalidRequest if the formatName is null or not registered
     */
    public String getLatestPublishedAsString(Dataset dataset, String formatName) {
        if (dataset == null) {
            return null;
        }
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        if (releasedVersion == null) {
            return null;
        }
        registry.requireExists(formatName);
        
        // Read the export from the cache or generate it if not present.
        ExportCacheKey key = new ExportCacheKey(releasedVersion, formatName);
        try (InputStream inputStream = pipeline.readFreshCachedExport(releasedVersion, key)
                                               .orElse(pipeline.readFreshExport(releasedVersion, formatName))) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // TODO: should this be escalatable via FailureEscalation?
            logger.log(Level.FINE, ex.getMessage(), ex);
            return null;
        } catch (ExportSystemException ex) {
            // Note: only IOEs were explicitly ignored, runtime errors could bubble up.
            throw ex;
        // Any other runtime errors should not be wrapped in EJB-proxy errors, but our own, checked ones
        } catch (RuntimeException ex) {
            throw new InternalFailure("Export to String failed for dataset " + dataset.getId() +
                                      " to format " + formatName, ex);
        }
    }
    
    /**
     * Writes a combined export of the given dataset versions to the {@code output} stream.
     * The format's {@link BulkDatasetExporter} capability decides how individual exports are framed and joined.
     * <p>
     * Each item is resolved lazily and in its own transaction, so at most one single-dataset export is
     * materialized at any time and memory stays independent of the batch size.
     * The caller (most notably a JAX-RS Response) owns {@code output} and is responsible for closing it.
     * <p>
     * It runs without a transaction of its own, and it holds no entities.
     * All Bytes are written while the response is streamed, which must not pin a database connection.
     * All entity work happens per item, behind the resolver, in {@link #getExportInNewTransaction(long, String)}.
     *
     * @apiNote The {@code targets} batch is not a consistent snapshot.
     *          <p>
     *          Items are re-fetched by id when their export is imminent, so a draft may have been edited,
     *          a version deaccessioned, permissions revoked, or (for superusers) a dataset destroyed
     *          since the caller vetted the list.
     *          <p>
     *          The window is small, and vanished or failing items appear as a per-item error rather than
     *          failing the whole batch. Callers requiring a true snapshot must not use this method.
     *
     * @param targets the dataset versions to include, in output order
     * @param formatName the export format; must be registered and support {@link BulkDatasetExporter}
     * @param output the stream to write the combined document to
     * @param correlationId short id echoed to the client and included in all log records of this batch
     * @throws InvalidRequest if the arguments are invalid, or the format is unknown or lacks the bulk capability
     * @throws InternalFailure if the bulk exporter fails
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void bulkExport(List<ExportTarget> targets, String formatName, OutputStream output, String correlationId) {
        if (targets == null || targets.isEmpty()) {
            throw new InvalidRequest("At least one export target is required");
        }
        if (output == null) {
            throw new InvalidRequest("Output stream must not be null");
        }
        if (correlationId == null || correlationId.isBlank()) {
            throw new InvalidRequest("Correlation id must not be null or empty");
        }
        // Throws InvalidRequest if the format is unknown or its exporter lacks the capability
        Details exporterDetail = registry.requireExistsAndSupports(formatName, BulkDatasetExporter.class);
        BulkDatasetExporter exporter = (BulkDatasetExporter) registry.get(exporterDetail);
        
        logger.log(Level.FINE, () -> "[" + correlationId + "] Bulk export of " + targets.size()
            + " items to format " + formatName + " started");
        
        // The idea:
        // 1. Collect resolvable entities for all the requested target dataset version in a "context".
        // 2. When resolving, use distinct transactions to avoid long-running transactions, blocking a DB connection.
        // 3. Make the resolving itself another layer of indirection by using a functional interface.
        //    Only once consumption starts, any of this will be executed, resulting in simple IO on a cache hit,
        //    computed cache-write-through on a miss, and on-the-fly generation for non-cacheable versions.
        //    Using a resolvable functional interface also enables re-reading the same resource if necessary.
        // 4. The export plugin receives the context and can choose how to consume it (see {@link BulkDatasetContext.Item})
        // 5. As the output stream is provided by the caller, it's their responsibility to close it.
        
        // Note: The resolver goes through the "self" proxy: a plain this.getExportInNewTransaction(...) would be a
        //       self-invocation, REQUIRES_NEW would be ignored, and every item would run detached.
        try (BulkExportPipeline context = new BulkExportPipeline(
            targets,
            // Note: Using a lambda and functional interface here to stall execution,
            //       saving on DB connections, memory, and open handles.
            versionId -> self.getExportInNewTransaction(versionId, formatName),
            correlationId)
        ) {
            // Close-shielded: a plugin must not close the response stream out from under the caller!
            exporter.exportBulk(context, CloseShieldOutputStream.wrap(output));
            output.flush();
        } catch (ExportSystemException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new InternalFailure("[" + correlationId + "] Bulk export to format " + formatName + " failed", ex);
        }
    }
    
    
    
    // ++++ ++++ ++++ METHODS FOR CACHE MANAGEMENT ++++ ++++ ++++
    
    /**
     * Returns the amount of storage used by the cache for the given dataset.
     * @param dataset the dataset
     * @return the amount of storage used by the cache for the given dataset
     * @throws IOException if an I/O error occurs while calculating the storage usage
     * @throws InvalidRequest if the dataset is null
     */
    public long usedCacheStorage(Dataset dataset) throws IOException {
        if (dataset == null) {
            throw new InvalidRequest("Dataset may not be null");
        }
        return cache.usedStorage(dataset);
    }
    
    // TODO: Add a service method to "purge" all cache entries for a dataset, also cleaning up any dangling data
    //       (for example in case a dataset is deaccessioned)
    
    /**
     * Clears all cached export formats for the given dataset.
     * Because all formats are removed, the dataset's * "last exported" timestamp is also set to null,
     * reflecting no cached exports remain.
     * <p>
     * TODO: When this service is extended to support caching and retrieving arbitrary dataset versions,
     *       it needs to be decided what "all" means: does "all" include all versions?
     *       Maybe replace the method with one that takes a list of versions.
     * TODO: The export timestamp should be moved to the individual versions.
     *       Not sure where else we may rely on this timestamp being on the dataset.
     *
     * @param dataset the dataset whose cached exports should all be cleared
     * @throws InternalFailure if an error occurs while clearing the cached format entries
     * @throws InvalidRequest if the dataset is null
     */
    public void clearAllCachedFormats(Dataset dataset) {
        // NOTE: Depending on the definition of "all", one may also use ExportCache.evictAll(),
        //       having the benefit of cleaning up leftover cache entries for which no exporter exists anymore.
        clearCachedFormats(dataset, List.of());
    }
    
    /**
     * Clears the cached formats for the given dataset.
     * Delegates to the version-specific overload by resolving the default version of the dataset.
     *
     * @param dataset the dataset for which cached formats should be cleared; must not be null
     * @param formatNames the list of format names to clear; may not be null, use an empty list to clear all formats
     * @throws InternalFailure if an error occurs while clearing the cached format entries
     * @throws InvalidRequest if the dataset is null
     */
    public void clearCachedFormats(Dataset dataset, List<String> formatNames) {
        if (dataset == null) {
            throw new InvalidRequest("Dataset may not be null");
        }
        // Let clearCachedFormats(DatasetVersion, List<String>) handle verifying the formatNames
        
        clearCachedFormats(defaultVersion(dataset), formatNames);
        // Only if we clear *all* formats, reset the "last exported" time stamp.
        // (Otherwise some formats still may exist in the cache.)
        // Keep in mind that this date will be crucial to determine results of cache staleness checks!
        if (formatNames.isEmpty())
            dataset.setLastExportTime(null);
    }
    
    /**
     * Clears the cached formats for the specified dataset version.
     * Validates that the dataset version is not null and that all provided format names exist in
     * the registry before clearing each cached format.
     * Eviction cascades up; any format given being a prerequisite for another will lead to eviction of both (recursively).
     *
     * @param datasetVersion the dataset version whose cached formats should be cleared; must not be null; empty means all formats
     * @param formatNames the list of format names to clear from the cache
     * @throws InternalFailure if an error occurs while clearing the cached format entries
     * @throws InvalidRequest if the dataset or associated data is null or any format name is invalid
     */
    public void clearCachedFormats(DatasetVersion datasetVersion, List<String> formatNames) {
        if (datasetVersion == null || datasetVersion.getDataset() == null) {
            throw new InvalidRequest("Dataset version or it's containing dataset may not be null");
        }
        
        // Do not proceed if this version is not cacheable by policy (drafts)
        // Keep in mind: if the policy changes, this fast exit may have unintended side effects!
        if (!isCacheable(datasetVersion)) {
            return;
        }
        // Will also enforce a non-null list
        registry.requireAllExist(formatNames);
        
        // If the list of format names is empty, retrieve all format names from the registry and evict all.
        if (formatNames.isEmpty()) {
            formatNames = registry.getDetails().stream().map(Details::formatName).toList();
        // If not empty, make sure to add transitive dependents to the evict list
        } else {
            formatNames = withTransitiveDependents(formatNames);
        }
        
        // Iterate over the list of format names and evict the cache for each format.
        // In case of errors, keep going but eventually fail by throwing an exception.
        List<String> failedFormats = new ArrayList<>();
        formatNames.forEach(format -> {
            ExportCacheKey key = new ExportCacheKey(datasetVersion, format);
            try {
                cache.evict(datasetVersion.getDataset(), key);
            // Any IOE or runtime exception (this includes ExportSystemExceptions) needs to be recorded,
            // leads to failure, and must not leave any escape hatches across the service boundary.
            } catch (IOException | RuntimeException e) {
                logger.log(Level.WARNING, e, () -> "Failed to evict cache of dataset version id=" + datasetVersion.getId() + " and format=" + format);
                failedFormats.add(format);
            }
        });
        if (!failedFormats.isEmpty()) {
            throw new InternalFailure("Failed to evict cache for formats=" + String.join(", ", failedFormats) + ", see logs for details");
        }
    }
    
    
    
    // ++++ ++++ ++++ METHODS TO TRIGGER DIFFERENT EXPORTS ++++ ++++ ++++
    
    /**
     * Exports the given dataset in all available supported formats and caches in dataset auxiliary storage.
     * <p>
     * This is a convenience wrapper that delegates to {@link #exportFormats(Dataset, List)} with an empty list,
     * causing every registered exporter to be invoked.
     * <p>
     * Note: Currently, only the latest released version of the dataset is exported.
     *       This may change in future versions.
     *
     * @param dataset the dataset whose metadata should be re-exported in all formats
     * @throws InternalFailure if an error occurs while exporting the dataset
     * @throws InvalidRequest if the dataset is null
     */
    public void exportAllFormats(Dataset dataset) {
        exportFormats(dataset, List.of());
    }
    
    /**
     * Exports the given dataset in a single specified format and caches in dataset auxiliary storage.
     * Delegate to the multi-format export method with a very short list.
     * Be aware that this may cause multiple exporters to be invoked in case the format is a prerequisite for others.
     *
     * @param dataset the dataset to export; must not be null
     * @param formatName the name of the export format to use; must not be null
     * @throws InternalFailure if an error occurs while exporting the dataset
     * @throws InvalidRequest if the dataset or format name null or invalid
     */
    public void exportFormat(Dataset dataset, String formatName) {
        // Check here to avoid NPE from List.of()
        if (formatName == null) {
            throw new InvalidRequest("Format name cannot be null");
        }
        exportFormats(dataset, List.of(formatName));
    }
    
    /**
     * Exports the given dataset selectively in the specified formats and caches in dataset auxiliary storage.
     * It resolves the dataset's {@link #defaultVersion} and delegates to the version-specific export method.
     * Upon successful completion of all exports, the dataset's last export time is updated to the current timestamp.
     * <p>
     * Be aware that this may cause more exporters to be invoked in case any format is a prerequisite for others.
     * If the list is empty, this method will export all available formats.
     *
     * @param dataset the dataset to export; must not be null
     * @param formatNames the list of format names to export in; an empty list means all formats
     * @throws InternalFailure if an error occurs while exporting the dataset
     * @throws InvalidRequest if the dataset or format name null or invalid
     */
    public void exportFormats(Dataset dataset, List<String> formatNames) {
        if (dataset == null) {
            throw new InvalidRequest("Dataset must not be null");
        }
        
        exportFormats(defaultVersion(dataset), formatNames);
        
        // Only if all formats were requested to be exported, update last export time on the dataset
        if (formatNames.isEmpty())
            dataset.setLastExportTime(Date.from(Instant.now()));
    }
    
    /**
     * Clears the cached exports for the specified formats (or all registered formats if the list is empty),
     * resolves all transitive dependent formats, orders the required exporters topologically to guarantee
     * that prerequisite formats are regenerated before their dependents, and then sequentially produces
     * and caches all the requested exported metadata formats.
     * <p>
     * If any of the requested formats has transitive dependents in the registry, those dependents are
     * automatically included in the export process so that they are regenerated with fresh prerequisite
     * data.
     *
     * @param datasetVersion the dataset version to export; must not be null
     * @param formatNames the names of the export formats to produce; if empty, all formats registered in
     *                    the registry will be exported
     * @throws InternalFailure if an error occurs while exporting the dataset version
     * @throws InvalidRequest if {@code datasetVersion} is null,
     *                                              does not fullfill {@link #isCacheable(DatasetVersion)}, or
     *                                              if any format name is invalid
     */
    public void exportFormats(DatasetVersion datasetVersion, List<String> formatNames) {
        if (datasetVersion == null) {
            throw new InvalidRequest("Dataset version must not be null");
        }
        if (!isCacheable(datasetVersion)) {
            throw new InvalidRequest("Dataset version is not cacheable, thus it cannot be exported to cache");
        }
        registry.requireAllExist(formatNames);
        
        // NOTE: Evict all formats at once before producing any new exports to improve cache consistency
        //       and force formats using the given ones as prerequisites to be renewed before use!
        clearCachedFormats(datasetVersion, formatNames);
        
        // If the list of format names is empty, retrieve all format names from the registry.
        if (formatNames.isEmpty()) {
            formatNames = registry.getDetails().stream().map(Details::formatName).toList();
        // Otherwise, make sure to add all formats relying on the requested ones, as they need to be regenerated, too.
        } else {
            formatNames = withTransitiveDependents(formatNames);
        }
        
        // Retrieve the exporters for all formats, then order the list topologically, ensuring dependencies get done first
        List<Exporter> exporters = formatNames.stream()
                                  .map(registry::get)
                                  .flatMap(Optional::stream) // safe: names were validated above!
                                  .sorted(registry.getTopologicalComparator())
                                  .toList();
        
        // THINK: What about the datacite export format? Any exporter may use it via the provider.
        //        Shouldn't all exports have this as an implicit dependency? Same goes for schema.org and ORE export!
        //        At the moment, the provider does a live conversion and does not read from a cached export, thus safe for now.
        
        // Now execute exports in sequential order
        // Note: If parallelization of exports is to be achieved, use a different data structure (like a queue) and
        //       group by number of dependencies. All exports at a certain depth must be done before proceeding to
        //       avoid race conditions.
        List<String> failedFormats = new ArrayList<>();
        for (Exporter exporter : exporters) {
            String formatName = exporter.getFormatName();
            ExportCacheKey key = new ExportCacheKey(datasetVersion, formatName);
            try {
                pipeline.produceAndCache(datasetVersion, key);
            // RuntimeEx also catches ExportSystemException and NPEs
            } catch (IOException | RuntimeException ex) {
                failedFormats.add(formatName);
                logger.log(Level.WARNING, ex, () -> "Export of " + formatName + " failed for dataset version" + datasetVersion);
            }
        }
        
        if (!failedFormats.isEmpty()) {
            throw new InternalFailure("Exporting formats=" +
                String.join(", ", failedFormats) + " failed, for details see logs");
        }
    }
    
    /**
     * Enrich a list of formats names with all of their transitive dependents (those formats that depend on them).
     * @param formatNames The list of formats to expand
     * @return Unmodifiable list containing both original format names and their transitive dependents
     */
    List<String> withTransitiveDependents(List<String> formatNames) {
        if (formatNames == null || formatNames.isEmpty()) {
            return List.of();
        }
        return formatNames.stream()
            // The flatMap replaces any stream element with the concatenated elements,
            // thus re-adding the format itself to the list keeps it around.
            .flatMap(format -> Stream.concat(
                Stream.of(format),
                registry.getTransitiveDependents(format).stream())
            )
            // Filter for duplicates (multiple formats may have the same dependents)
            .distinct()
            .toList();
    }
    
    /**
     * Cache policy: drafts are mutable and therefore never cached; released versions are cacheable.
     * Extend here (not at call sites) when caching of further version states (e.g. deaccessioned) needs an explicit decision.
     * Keep in mind: if the policy changes, this may have unintended side effects! Make sure to verify!
     */
    static boolean isCacheable(DatasetVersion version) {
        return !version.isDraft();
    }
    
    /**
     * Export policy: determines the default dataset version to use for export operations.
     * If the given dataset has been released, its released version is returned.
     * Otherwise, the dataset's latest version is returned.
     *
     * @param dataset the dataset from which the default version should be resolved
     * @return the released version if the dataset is released, otherwise the latest version (should be draft)
     */
    static DatasetVersion defaultVersion(Dataset dataset) {
        return dataset.isReleased() ? dataset.getReleasedVersion() : dataset.getLatestVersion();
    }
    
    /**
     * Factory method to create a data provider.
     * Intended for usage in tests, exposing a {@link ExportDataProvider} instance.
     * Exporters should not be used directly outside of tests.
     *
     * @param version the dataset version to back the data provider during export operations
     * @return an {@link ExportDataProvider} instance
     */
    public static ExportDataProvider createProvider(DatasetVersion version) {
        return new InternalExportDataProvider(version);
    }

}
