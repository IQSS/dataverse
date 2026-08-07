package edu.harvard.iq.dataverse.export.service;

import java.util.List;

/**
 * Represents an abstraction for determining whether a cached export needs to be invalidated and regenerated.
 * <p>
 * This sealed interface is intended to enforce a controlled hierarchy of classes that implement the cache
 * invalidation logic, ensuring behavior consistency across different implementations. If necessary, the contract
 * may be altered to allow more dynamic discovery of invalidators.
 */
public sealed interface ExportCacheInvalidator permits FileEmbargoExpiryInvalidator {
    
    /**
     * A collection of {@link ExportCacheInvalidator} instances.
     * This list is intended to centralize all invalidation mechanisms for export cache entries.
     * Any new implementations must be added here in addition to the "permits" on the interface seal.
     */
    List<ExportCacheInvalidator> invalidators = List.of(new FileEmbargoExpiryInvalidator());
    
    /** Should a cached export for this key be discarded and regenerated? */
    boolean isStale(ExportCacheKey key);
}
