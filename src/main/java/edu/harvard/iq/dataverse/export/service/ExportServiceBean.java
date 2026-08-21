package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.export.XMLExporter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class ExportServiceBean {

    private static final Logger logger = Logger.getLogger(ExportServiceBean.class.getCanonicalName());

    @EJB
    ExporterRegistryBean registry;
    
    // We must use (frowned upon) field injection here, as EJB requires a no-args constructor.
    // When the codebase transitions to use CDI only, this shall be changed to constructor injection.
    @SuppressWarnings("java:S6813")
    @Inject
    ExportCache cache;
    
    @EJB
    ExportPipelineBean pipeline;
    
    // METHODS TO RETRIEVE EXPORTED DATA
    
    public InputStream getExport(DatasetVersion datasetVersion, String formatName) throws ExportException, IOException {

        Dataset dataset = datasetVersion.getDataset();
        InputStream exportInputStream = null;

        if (datasetVersion.isDraft()) {
            // For drafts we create the export on the fly rather than caching.
            Exporter exporter = exporterMap.get(formatName);
            if (exporter != null) {
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    // getPrerequisiteFormatName logic copied from exportFormat()
                    if (exporter.getPrerequisiteFormatName().isPresent()) {
                        String prereqFormatName = exporter.getPrerequisiteFormatName().get();
                        try (InputStream preReqStream = getExport(datasetVersion, prereqFormatName)) {
                            InternalExportDataProvider dataProvider = new InternalExportDataProvider(datasetVersion, preReqStream);
                            exporter.exportDataset(dataProvider, outputStream);
                        } catch (IOException ioe) {
                            throw new ExportException("Could not get prerequisite " + prereqFormatName + " to create " + formatName + " export for dataset " + dataset.getId(), ioe);
                        }
                    } else {
                        InternalExportDataProvider dataProvider = new InternalExportDataProvider(datasetVersion);
                        exporter.exportDataset(dataProvider, outputStream);
                    }
                    return new ByteArrayInputStream(outputStream.toByteArray());
                }
            }
        } else {
            // for non-drafts (published versions) we try to locate an already existing, cached export
            exportInputStream = getCachedExportFormat(dataset, formatName);
        }

        if (exportInputStream != null) {
            return exportInputStream;
        }

        // if it doesn't exist, we'll try to run the export:
        exportFormat(dataset, formatName);

        // and then try again:
        exportInputStream = getCachedExportFormat(dataset, formatName);

        if (exportInputStream != null) {
            return exportInputStream;
        }

        // if there is no cached export still - we have to give up and throw
        // an exception!
        throw new ExportException("Failed to export the dataset as " + formatName);

    }

    public String getLatestPublishedAsString(Dataset dataset, String formatName) {
        if (dataset == null) {
            return null;
        }
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        if (releasedVersion == null) {
            return null;
        }
        InputStream inputStream = null;
        InputStreamReader inp = null;
        try {
            inputStream = getExport(releasedVersion, formatName);
            if (inputStream != null) {
                inp = new InputStreamReader(inputStream, "UTF8");
                BufferedReader br = new BufferedReader(inp);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append('\n');
                }
                br.close();
                inp.close();
                inputStream.close();
                return sb.toString();
            }
        } catch (IOException ex) {
            logger.log(Level.FINE, ex.getMessage(), ex);
            return null;
        } finally {
            IOUtils.closeQuietly(inp);
            IOUtils.closeQuietly(inputStream);
        }
        return null;

    }
    
    
    
    // ++++ ++++ ++++ METHODS FOR CACHE MANAGEMENT ++++ ++++ ++++
    
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
     * @throws IOException if an I/O error occurs while clearing the cached format entries
     */
    public void clearAllCachedFormats(Dataset dataset) throws IOException {
        clearCachedFormats(dataset, List.of());
        // Only if we clear *all* formats, reset the "last exported" time stamp.
        // (Otherwise some formats still may exist in the cache.)
        dataset.setLastExportTime(null);
    }
    
    /**
     * Clears the cached formats for the given dataset.
     * Delegates to the version-specific overload by resolving the default version of the dataset.
     *
     * @param dataset the dataset for which cached formats should be cleared; must not be null
     * @param formatNames the list of format names to clear; may be null to clear all formats
     * @throws ExportException if the dataset is null
     */
    public void clearCachedFormats(Dataset dataset, List<String> formatNames) throws ExportException {
        if (dataset == null) {
            throw new ExportException("Dataset may not be null");
        }
        // Let clearCachedFormats(DatasetVersion, List<String>) handle verifying the formatNames
        
        clearCachedFormats(defaultVersion(dataset), formatNames);
    }
    
    /**
     * Clears the cached formats for the specified dataset version.
     * Validates that the dataset version is not null and that all provided format names exist in
     * the registry before clearing each cached format.
     *
     * @param datasetVersion the dataset version whose cached formats should be cleared; must not be null
     * @param formatNames the list of format names to clear from the cache
     * @throws ExportException if the dataset version is null or any format name is invalid
     */
    public void clearCachedFormats(DatasetVersion datasetVersion, List<String> formatNames) {
        if (datasetVersion == null) {
            throw new ExportException("Dataset version may not be null");
        }
        try {
            registry.requireAllExist(formatNames);
        } catch (IllegalArgumentException ex) {
            throw new ExportException("Invalid format names: " + ex.getMessage());
        }
        
        formatNames.forEach(formatName -> clearCachedFormat(datasetVersion, formatName));
    }
    
    void clearCachedFormat(DatasetVersion datasetVersion, String formatName) throws ExportException {
        // Note: If this is ever changed to a "public" method, it will require parameter validation!
        //       (Which may duplicate checks when coming from other methods)
        
        // Build the cache key and evict it from the cache.
        // NOTE: If the given version wasn't cacheable in the first place (as per isCacheable()),
        //       eviction should just succeed instead of failing (nothing was ever there, but this
        //       was the service's choice, not the cache's!).
        ExportCacheKey key = new ExportCacheKey(datasetVersion, formatName);
        try {
            cache.evict(key);
        } catch (IOException ex) {
            throw new ExportException("Failed to clear cached format: " + ex.getMessage());
        }
    }
    
    
    
    // ++++ ++++ ++++ METHODS TO TRIGGER DIFFERENT EXPORTS ++++ ++++ ++++
    
    /**
     * Exports the given dataset in all available supported formats.
     * <p>
     * This is a convenience wrapper that delegates to {@link #exportFormats(Dataset, List)} with an empty list,
     * causing every registered exporter to be invoked.
     * <p>
     * Note: Currently, only the latest released version of the dataset is exported.
     *       This may change in future versions.
     *
     * @param dataset the dataset whose metadata should be re-exported in all formats
     * @throws ExportException if any exporter fails to produce its output
     */
    public void exportAllFormats(Dataset dataset) throws ExportException {
        exportFormats(dataset, List.of());
    }
    
    /**
     * Exports the given dataset in a single specified format.
     * Delegate to the multi-format export method with a very short list.
     * Be aware that this may cause multiple exporters to be invoked in case the format is a prerequisite for others.
     *
     * @param dataset the dataset to export; must not be null
     * @param formatName the name of the export format to use; must not be null
     * @throws ExportException if the format name is null or if the underlying export operation fails
     */
    public void exportFormat(Dataset dataset, String formatName) throws ExportException {
        // Check here to avoid NPE from List.of()
        if (formatName == null) {
            throw new ExportException("Format name cannot be null");
        }
        exportFormats(dataset, List.of(formatName));
    }
    
    /**
     * Exports the given dataset selectively in the specified formats by resolving the dataset's {@link #defaultVersion}
     * and delegating to the version-specific export method. Upon successful completion of all exports, the dataset's
     * last export time is updated to the current timestamp.
     * <p>
     * Be aware that this may cause more exporters to be invoked in case any format is a prerequisite for others.
     * If the list is empty, this method will export all available formats.
     *
     * @param dataset the dataset to export; must not be null
     * @param formatNames the list of format names to export in; an empty list means all formats
     * @throws ExportException if the dataset is null or if any export operation fails
     */
    public void exportFormats(Dataset dataset, List<String> formatNames) throws ExportException {
        if (dataset == null) {
            throw new ExportException("Dataset must not be null");
        }
        
        exportFormats(defaultVersion(dataset), formatNames);
        
        // All exports done successfully, update last export time on the dataset
        // TODO: Is it correct to update the last export time even if only some formats were exported?
        dataset.setLastExportTime(Date.from(Instant.now()));
    }
    
    /**
     * Clears the cached exports for the specified formats (or all registered formats if the list is empty),
     * resolves all transitive dependent formats, orders the required exporters topologically to guarantee
     * that prerequisite formats are regenerated before their dependents, and then sequentially produces
     * and caches the requested exports.
     * <p>
     * If any of the requested formats has transitive dependents in the registry, those dependents are
     * automatically included in the export process so that they are regenerated with fresh prerequisite
     * data.
     *
     * @param datasetVersion the dataset version to export; must not be null
     * @param formatNames the names of the export formats to produce; if empty, all formats registered in
     *                    the registry will be exported
     * @throws ExportException if datasetVersion is null or does not fullfill {@link #isCacheable(DatasetVersion)},
     *                         if any format name is invalid, or
     *                         if one or more exports fail during execution
     */
    public void exportFormats(DatasetVersion datasetVersion, List<String> formatNames) throws ExportException {
        if (datasetVersion == null) {
            throw new ExportException("Dataset version must not be null");
        }
        if (!isCacheable(datasetVersion)) {
            throw new ExportException("Dataset version is not cacheable, thus it cannot be exported to cache");
        }
        try {
            registry.requireAllExist(formatNames);
        } catch (IllegalArgumentException e) {
            throw new ExportException("One or more format names are invalid: " + e.getMessage());
        }
        
        // NOTE: Evict all formats at once before producing any new exports to improve cache consistency
        //       and force prerequisite formats to be renewed before use!
        clearCachedFormats(datasetVersion, formatNames);
        
        // If the list of format names is empty, retrieve all format names from the registry and evict all.
        if (formatNames.isEmpty()) {
            formatNames = registry.getDetails().stream().map(ExporterRegistryBean.Details::formatName).toList();
        // Otherwise, make sure to add all formats relying on the requested ones, as they need to be regenerated, too.
        } else {
            formatNames = formatNames.stream()
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
        boolean allSucceeded = true;
        for (Exporter exporter : exporters) {
            String formatName = exporter.getFormatName();
            ExportCacheKey key = new ExportCacheKey(datasetVersion, formatName);
            try {
                pipeline.produceAndCache(datasetVersion, key);
            // RuntimeEx also catches ExportException and NPEs
            } catch (IOException | RuntimeException ex) {
                allSucceeded = false;
                logger.log(Level.WARNING, ex, () -> "Export of " + formatName + " failed for dataset version" + datasetVersion);
            }
        }
        
        if (!allSucceeded) {
            throw new ExportException("One or more exports failed, for details see logs");
        }
    }
    
    /**
     * Cache policy: drafts are mutable and therefore never cached; released versions are cacheable.
     * Extend here (not at call sites) when caching of further version states (e.g. deaccessioned) needs an explicit decision.
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

}
