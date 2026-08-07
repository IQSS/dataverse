package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Embargo;
import edu.harvard.iq.dataverse.FileMetadata;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The {@code FileEmbargoExpiryInvalidator} class implements the {@link ExportCacheInvalidator} interface to determine
 * whether a cached export should be invalidated due to the expiration of an embargo on any file within a dataset.
 * This invalidation ensures that stale cached exports do not persist beyond the embargo period.
 * <p>
 * Note: This code was originally a part of {@code ExportService}, written mostly by qqmyers.
 *       Back there it was targeting DDI format only, but with pluggable exports, any format may export file metadata.
 */
public final class FileEmbargoExpiryInvalidator implements ExportCacheInvalidator {
    
    private static final Logger logger = Logger.getLogger(FileEmbargoExpiryInvalidator.class.getCanonicalName());
    
    @Override
    public boolean isStale(ExportCacheKey key) {
        return isStaleDueToExpiredEmbargo(key.dataset());
    }
    
    /**
     * Checks whether a cached export has been rendered stale because an embargo
     * on one of the dataset's files ended after the last export ran.
     */
    private boolean isStaleDueToExpiredEmbargo(Dataset dataset) {
        Date lastExportDate = dataset.getLastExportTime();
        // if lastExportDate == null, assume it's not set because we're exporting for the
        // first time now (e.g. during publish) and therefore no changes are needed
        if (lastExportDate == null) {
            return false;
        }
        LocalDate exportLocalDate = lastExportDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        logger.fine("Last export date: " + exportLocalDate);
        // Track which embargoes we've already checked
        Set<Long> embargoIds = new HashSet<>();
        // Check for all files in the latest released version
        for (FileMetadata fm : dataset.getLatestVersionForCopy().getFileMetadatas()) {
            // ToDo? This loop is necessary because we have not stored the date when the
            // next embargo in this datasetversion will end. If we knew that (another
            // dataset/datasetversion column), we could make one check that nextembargoEnd
            // exists and is after the last export and before now versus scanning through
            // files until we potentially find such an embargo.
            Embargo e = fm.getDataFile().getEmbargo();
            if (e == null || embargoIds.contains(e.getId())) {
                continue;
            }
            logger.fine("Datafile: " + fm.getDataFile().getId() + ", embargo end date: " + e.getFormattedDateAvailable());
            if (e.getDateAvailable().isAfter(exportLocalDate) && e.getDateAvailable().isBefore(LocalDate.now(ZoneId.systemDefault()))) {
                // The embargo ended after the last export and before the current date,
                // so the cached export needs to be refreshed.
                logger.fine("Request that the cached export be cleared.");
                return true;
            }
            embargoIds.add(e.getId());
        }
        return false;
    }
}
