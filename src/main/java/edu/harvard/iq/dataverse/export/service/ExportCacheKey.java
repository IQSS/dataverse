package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.DatasetVersion;

import java.util.Objects;

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
public record ExportCacheKey(String auxTag) {
    
    public static final String TAG_PREFIX = "export_";
    public static final String TAG_SUFFIX = ".cached";
    
    /**
     * Constructs an ExportCacheKey instance with the specified dataset version, and format name.
     * @param version the dataset version associated with this cache key; must not be null
     * @param formatName the format name used for export operations; must not be null or blank
     * @throws NullPointerException if the dataset, version, or formatName is null
     * @throws IllegalArgumentException if the formatName is blank or empty
     */
    public ExportCacheKey(DatasetVersion version, String formatName) {
        this(auxTag(version, formatName));
    }
    
    /** The one canonical, version-qualified aux tag. */
    static String auxTag(DatasetVersion version, String formatName) {
        Objects.requireNonNull(version);
        if (Objects.requireNonNull(formatName).isBlank()) {
            throw new IllegalArgumentException("formatName must not be blank or empty");
        }
        return TAG_PREFIX + formatName + "_" + version.getFriendlyVersionNumber() + TAG_SUFFIX;
    }
}
