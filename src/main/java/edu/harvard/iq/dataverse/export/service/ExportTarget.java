package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.DatasetVersion;

/**
 * Identifies one dataset version within a bulk export, carrying only immutable, already-resolved values.
 * <p>
 * Used to separate service boundaries by allowing to drop holds on live JPA entities.
 * Transforming them into thread-safe records holding immutable database references allows safe border crossing.
 * <p>
 * This is especially important for bulk exports, written after the JAX-RS resource method (and its associated transaction)
 * has returned, so any managed entity would be detached by then and lazy associations would fail.
 * The labels are captured here while a persistence context is still active.
 * The version is re-fetched by id in its own transaction when its export is actually needed.
 *
 * @param versionId     database id of the dataset version, used to re-fetch it during export
 * @param persistentId  the dataset's persistent identifier, as exposed to bulk exporter plugins
 * @param versionNumber the friendly version number, as exposed to bulk exporter plugins
 */
public record ExportTarget(long versionId, String persistentId, String versionNumber) {
    
    /**
     * Captures a target from a managed dataset version.
     * Must be called while a persistence context is active, as it touches the dataset association.
     */
    public static ExportTarget from(DatasetVersion version) {
        if (version == null || version.getId() == null || version.getDataset() == null) {
            throw new IllegalArgumentException("Dataset version, its id and its dataset must not be null");
        }
        return new ExportTarget(
            version.getId(),
            version.getDataset().getGlobalId().asString(),
            version.getSemanticVersion());
    }
}
