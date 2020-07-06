package edu.harvard.iq.dataverse.workflow.artifacts;

import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifact;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Service from storing and retrieving artifact data for the given {@link StorageType}.
 * The implementations should be registered in {@link WorkflowArtifactServiceBean} (compare
 * {@link DatabaseStorageService}) and should be used via that service bean.
 */
public interface StorageService {

    /**
     * Should return {@link StorageType} on which this implementations operates.
     */
    StorageType getStorageType();

    /**
     * Should save the data from the stream supplier for the given {@link WorkflowArtifact} into
     * storage and return location string that allows to access the saved data form this storage
     * unambiguously.
     */
    String save(Supplier<InputStream> inputStreamSupplier);

    /**
     * Should return {@link Optional } containing {@link InputStream} with the stored data
     * for the given location parameter or empty {@link Optional} if there is no data stored for
     * the location.
     */
    Optional<Supplier<InputStream>> readAsStream(String location);

    /**
     * Should delete stored data for the given location parameter.
     */
    void delete(String location);
}
