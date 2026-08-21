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
     * This method is added to supplement the classic exportAllFormats() in order
     * to allow the metadata export APIs to selectively re-export only the formats
     * specified. This is to finally allow an instance admin to avoid running
     * a complete, from-scratch reexport when only _some_, or just one of them
     * actually needs to be refreshed. On a large instance this can waste a
     * significant amount of time and CPU cycles. (new as of 6.12)
     * This method calls the cacheExport() method for every valid/supported
     * format name supplied, or for every Exporter available, if an empty List
     * is passed.
     * Only the latest published version is used for exports.
     * exportAllFormats() above is now a convenience wrapper, with the
     * implementation moved here.
     *
     * @param dataset
     * @param formatNames
     * @throws ExportException
     */
    public void exportFormats(Dataset dataset, List<String> formatNames) throws ExportException {
        if (dataset == null) {
            throw new ExportException("exportFormats called with null Dataset");
        }
        try {
            registry.requireAllExist(formatNames);
        } catch (IllegalArgumentException ex) {
            throw new ExportException("Invalid format names: " + ex.getMessage());
        }
        
        try {
            clearCachedFormats(dataset, formatNames);
        } catch (IOException ex) {
            Logger.getLogger(ExportService.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            DatasetVersion releasedVersion = dataset.getReleasedVersion();
            if (releasedVersion == null) {
                throw new ExportException("No released version for dataset " + dataset.getGlobalId().toString());
            }
            InternalExportDataProvider dataProvider = new InternalExportDataProvider(releasedVersion);
            
            for (Exporter e : exporterMap.values()) {
                String formatName = e.getFormatName();
                if (formatNames.isEmpty() || formatNames.contains(formatName)) {
                    if (e.getPrerequisiteFormatName().isPresent()) {
                        String prereqFormatName = e.getPrerequisiteFormatName().get();
                        try (InputStream preReqStream = getExport(dataset.getReleasedVersion(), prereqFormatName)) {
                            dataProvider.setPrerequisiteInputStream(preReqStream);
                            cacheExport(dataset, dataProvider, formatName, e);
                            dataProvider.setPrerequisiteInputStream(null);
                        } catch (IOException ioe) {
                            throw new ExportException("Could not get prerequisite " + e.getPrerequisiteFormatName() + " to create " + formatName + "export for dataset " + dataset.getId(), ioe);
                        }
                    } else {
                        cacheExport(dataset, dataProvider, formatName, e);
                    }
                }
            }
            // Finally, if we have been able to successfully export in all available
            // formats, we'll increment the "last exported" time stamp:
            if (formatNames.isEmpty()) {
                dataset.setLastExportTime(new Timestamp(new Date().getTime()));
            }
            
        } catch (ServiceConfigurationError serviceError) {
            throw new ExportException("Service configuration error during export. " + serviceError.getMessage());
        } catch (RuntimeException e) {
            logger.log(Level.FINE, e.getMessage(), e);
            throw new ExportException(
                "Unknown runtime exception exporting metadata. " + (e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    // This method finds the exporter for the format requested,
    // then produces the dataset metadata as a JsonObject, then calls
    // the "cacheExport()" method that will save the produced output
    // in a file in the dataset directory.
    public void exportFormat(Dataset dataset, String formatName) throws ExportException {
        try {

            Exporter e = exporterMap.get(formatName);
            if (e != null) {
                DatasetVersion releasedVersion = dataset.getReleasedVersion();
                if (releasedVersion == null) {
                    throw new ExportException(
                            "No published version found during export. " + dataset.getGlobalId().toString());
                }
                if(e.getPrerequisiteFormatName().isPresent()) {
                    String prereqFormatName = e.getPrerequisiteFormatName().get();
                    try (InputStream preReqStream = getExport(releasedVersion, prereqFormatName)) {
                        InternalExportDataProvider dataProvider = new InternalExportDataProvider(releasedVersion, preReqStream);
                        cacheExport(dataset, dataProvider, formatName, e);
                    } catch (IOException ioe) {
                        throw new ExportException ("Could not get prerequisite " + e.getPrerequisiteFormatName() + " to create " + formatName + "export for dataset " + dataset.getId(), ioe);
                    }
                } else {
                    InternalExportDataProvider dataProvider = new InternalExportDataProvider(releasedVersion);
                    cacheExport(dataset, dataProvider, formatName, e);
                }
                // As with exportAll, we should update the lastexporttime for the dataset
                dataset.setLastExportTime(new Timestamp(new Date().getTime()));
            } else {
                throw new ExportException("Exporter not found");
            }
        } catch (IllegalStateException e) {
            // IllegalStateException can potentially mean very different, and
            // unexpected things. An exporter attempting to get a single primitive
            // value from a fieldDTO that is in fact a Multiple and contains a
            // json vector (this has happened, for example, when the code in the
            // DDI exporter was not updated following a metadata fieldtype change),
            // will result in IllegalStateException.
            throw new ExportException("IllegalStateException caught when exporting " + formatName + " for dataset "
                    + dataset.getGlobalId().toString()
                    + "; may or may not be due to a mismatch between an exporter code and a metadata block update. "
                    + e.getMessage());
        }

    }
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
