package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;

/**
 * Enum representing the different erasure behaviors based on storage type.
 */
public enum EraseStoreBehavior {
    /**
     * Full erasure: delete the main file and all auxiliary files (File, S3, Swift stores)
     */
    FULL_ERASURE,
    
    /**
     * Auxiliary only: delete only cached auxiliary files (RemoteOverlayStore)
     */
    AUX_ONLY,
    
    /**
     * Full erasure via S3: delete the main file and all auxiliary files via S3 access (Globus S3-accessible)
     */
    FULL_VIA_S3,
    
    /**
     * Manual required: delete only cached auxiliary files; user must manually delete the main file (Globus non-S3)
     */
    MANUAL_REQUIRED;
    
    /**
     * Determine the erasure behavior for a given DataFile based on its storage type.
     *
     * @param df the DataFile to determine erasure behavior for
     * @return the EraseStoreBehavior for the file's storage type
     * @throws CommandExecutionException if the storage type is unsupported
     */
    public static EraseStoreBehavior getEraseBehavior(DataFile df) {
        String driverId = DataAccess.getStorageDriverFromIdentifier(df.getStorageIdentifier());
        String type = DataAccess.getDriverType(driverId);
        switch (type) {
            case DataAccess.FILE:
            case DataAccess.S3:
            case DataAccess.SWIFT:
                return EraseStoreBehavior.FULL_ERASURE;
            case DataAccess.REMOTE:
                return EraseStoreBehavior.AUX_ONLY;
            case DataAccess.GLOBUS:
                return StorageIO.isDataverseAccessible(driverId)
                        ? EraseStoreBehavior.FULL_VIA_S3
                        : EraseStoreBehavior.MANUAL_REQUIRED;
            default:
                return EraseStoreBehavior.AUX_ONLY;
        }
    }
}
