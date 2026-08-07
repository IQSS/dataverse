package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;

import java.util.Objects;

/**
 * This record encapsulates information related to the dataset, the version of the dataset,
 * and the format name used for the export, enabling precise identification
 * of cache entries for export operations.
 */
public record ExportCacheKey(Dataset dataset, DatasetVersion version, String formatName) {
    
    /**
     * Constructs an ExportCacheKey instance with the specified dataset, dataset version, and format name.
     * @param dataset the dataset associated with this cache key; must not be null
     * @param version the dataset version associated with this cache key; must not be null
     * @param formatName the format name used for export operations; must not be null or blank
     * @throws NullPointerException if the dataset, version, or formatName is null
     * @throws IllegalArgumentException if the formatName is blank or empty
     */
    public ExportCacheKey(Dataset dataset, DatasetVersion version, String formatName) {
        this.dataset = Objects.requireNonNull(dataset);
        this.version = Objects.requireNonNull(version);
        if (Objects.requireNonNull(formatName).isBlank()) {
            throw new IllegalArgumentException("formatName must not be blank or empty");
        }
        this.formatName = formatName;
    }
    
    /**
     * Convenience wrapper to create a cache key fro ma version and format alone.
     * Note: the entity object must have a reference to the dataset present!
     * @param version the dataset version
     * @param formatName the target format
     * @throws NullPointerException if either version, the dataset in the version or the format are null
     * @throws IllegalArgumentException if the format name is blank or empty
     */
    public ExportCacheKey(DatasetVersion version, String formatName) {
        this(Objects.requireNonNull(version).getDataset(), version, formatName);
    }
    
    /** The one canonical, version-qualified aux tag. */
    public String auxTag() {
        return "export_" + formatName + "_" + version.getFriendlyVersionNumber() + ".cached";
    }
}
