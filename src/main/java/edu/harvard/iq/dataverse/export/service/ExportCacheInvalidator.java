package edu.harvard.iq.dataverse.export.service;

/**
 * Represents an abstraction for determining whether a cached export needs to be invalidated and regenerated.
 * <p>
 * This sealed interface is intended to enforce a controlled hierarchy of classes that implement the cache
 * invalidation logic, ensuring behavior consistency across different implementations. If necessary, the contract
 * may be altered to allow more dynamic discovery of invalidators.
 * <p>
 * If at a later point we want to enable export plugins to provide their own invalidation logic,
 * this interface shall be unsealed and moved into the Exporter SPI codebase.
 */
public sealed interface ExportCacheInvalidator permits FileEmbargoExpiryInvalidator {
    /** Should a cached export for this key be discarded and regenerated? */
    boolean isStale(ExportCacheKey key);
}
