package edu.harvard.iq.dataverse.persistence.dataset;

import java.util.Objects;

/**
 * Defines dataset version natural identifier.
 *
 * @author kaczynskid
 */
public interface DatasetVersionIdentifier {

    /**
     * @return dataset identifier.
     */
    Long getDatasetId();

    /**
     * @return dataset major version number.
     */
    Long getVersionNumber();

    /**
     * @return dataset minor version number.
     */
    Long getMinorVersionNumber();

    /**
     * Asserts if this object is identified by given identifier.
     * @param identifier identifier to check.
     * @return true if this object is identifiable by given identifier.
     */
    default boolean isIdentifiedBy(DatasetVersionIdentifier identifier) {
        return Objects.equals(getDatasetId(), identifier.getDatasetId())
                && Objects.equals(getVersionNumber(), identifier.getVersionNumber())
                && Objects.equals(getMinorVersionNumber(), identifier.getMinorVersionNumber());
    }
}
