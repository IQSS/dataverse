package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.DatasetVersion;

/**
 * Represents an abstraction for determining whether a cached export needs to be invalidated and regenerated.
 * <p>
 * If at a later point export plugins are to be enabled to provide their own invalidation logic,
 * this interface may be moved into the Exporter SPI codebase.
 */
public interface ExportCacheInvalidator {
    /**
     * Should a cached export for this key be discarded and regenerated?
     * @implNote Keep in mind that (in its current form) implementations will not actually retrieve the cached content.
     *           The contract will need to be adjusted if this is deemed necessary!
     * @param datasetVersion the dataset version for which the export is being generated
     * @param key the cache key associated with the export
     * @throws IllegalArgumentException if any parameters are null or implementation expectations are not met
     */
    boolean isStale(DatasetVersion datasetVersion, ExportCacheKey key);
}
