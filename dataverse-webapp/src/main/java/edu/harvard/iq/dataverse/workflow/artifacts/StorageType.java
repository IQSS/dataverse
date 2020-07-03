package edu.harvard.iq.dataverse.workflow.artifacts;

/**
 * Storage type for workflow artifacts. For every constant of this enum corresponding {@link StorageService}
 * implementation should be made available. Constant identifiers should not exceed 64 characters.
 *
 * <p><b>IMPORTANT:</b> do not change any of existing identifiers as this will cause data inconsistencies
 * and break the functionality of downloading artifacts.
 */
public enum StorageType {
    DATABASE;
}
