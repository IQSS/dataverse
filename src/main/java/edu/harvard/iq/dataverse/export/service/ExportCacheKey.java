package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.DatasetVersion;

import java.io.Serializable;

/**
 * This record encapsulates information related to the dataset, the version of the dataset,
 * and the format name used for the export, enabling precise identification
 * of cache entries for export operations.
 * <p>
 * Note: This cache key is thread-safe, as the JPA entities are not kept, but the read-only aux tag is
 *       derived at construction time. Even if the version entity is altered between usages, the cache key is stable.
 *       The cache itself derives the target auxiliary storage (dataset or datafile) at runtime.
 *       In addition, by not keeping an JPA entity reference, garbage collection is facilitated.
 */
public record ExportCacheKey(String formatName, String datasetId, String friendlyVersion) implements Serializable {
    
    public static final String TAG_PREFIX = "export_";
    public static final String TAG_SUFFIX = ".cached";
    
    /**
     * Constructs an ExportCacheKey instance identifying a specific cached export by its format name, dataset
     * identifier, and friendly version string. It's not meant for public use. It will verify parameter validity
     * and overrides the default, implicit record constructor.
     * <p>
     * @implNote This constructor cannot be made private because it's not allowed by Java Record Specification.
     *           While one may construct arbitrary cache keys, no one should.
     *           Always use {@link #ExportCacheKey(DatasetVersion, String)} instead.
     *
     * @param formatName the export format name (e.g., "dvn", "csv"); must not be null or blank
     * @param datasetId the unique identifier of the dataset this cache key belongs to; must not be null or blank
     * @param friendlyVersion the human-readable version label of the dataset; must not be null or blank
     * @throws IllegalArgumentException if any parameter is null or blank
     */
    public ExportCacheKey(String formatName, String datasetId, String friendlyVersion) {
        this.formatName = requireNonBlank(formatName, "formatName");
        this.datasetId = requireNonBlank(datasetId, "datasetId");
        this.friendlyVersion = requireNonBlank(friendlyVersion, "friendlyVersion");
    }
    
    /**
     * Constructs an ExportCacheKey instance with the specified dataset version, and format name.
     * @param version the dataset version associated with this cache key; must not be null
     * @param formatName the format name used for export operations; must not be null or blank
     * @throws IllegalArgumentException if the formatName is blank or empty
     */
    public ExportCacheKey(DatasetVersion version, String formatName) {
        this(requireNonBlank(formatName, "formatName"), getDatasetId(version), checkVersion(version));
    }
    
    /** The one canonical, version-qualified aux tag. */
    public String auxTag() {
        return TAG_PREFIX + formatName + "_" + friendlyVersion + TAG_SUFFIX;
    }
    
    private static String getDatasetId(DatasetVersion datasetVersion) {
        checkVersion(datasetVersion);
        if (datasetVersion.getDataset() == null) {
            throw new IllegalArgumentException("datasetVersion's dataset must not be null");
        }
        if (datasetVersion.getDataset().getId() == null || datasetVersion.getDataset().getId() < 1) {
            throw new IllegalArgumentException("datasetVersion's dataset must have a non-null ID greater 0");
        }
        return datasetVersion.getDataset().getId().toString();
    }
    
    private static String checkVersion(DatasetVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("version must not be null");
        }
        return requireNonBlank(version.getFriendlyVersionNumber(), "version's friendlyVersion");
    }
    
    private static String requireNonBlank(String text, String parameterName) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(parameterName + " must not be null or blank");
        }
        return text;
    }
}
