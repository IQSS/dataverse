package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;

/**
 * This record encapsulates information related to the dataset, the version of the dataset,
 * and the format name used for the export, enabling precise identification
 * of cache entries for export operations.
 */
public record ExportCacheKey(Dataset dataset, DatasetVersion version, String formatName) {
    
    /** The one canonical, version-qualified aux tag. Always used to write. */
    public String auxTag() {
        return "export_" + formatName + "_" + version.getFriendlyVersionNumber() + ".cached";
    }
}
