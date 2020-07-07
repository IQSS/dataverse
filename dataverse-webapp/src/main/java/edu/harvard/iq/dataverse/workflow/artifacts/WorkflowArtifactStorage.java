package edu.harvard.iq.dataverse.workflow.artifacts;

import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Stores and retrieves binary data.
 */
public interface WorkflowArtifactStorage {

    /**
     * Storage type for workflow artifacts. Constant identifiers should not exceed 64 characters.
     * <p><b>IMPORTANT:</b> do not change any of existing identifiers as this will cause data inconsistencies
     * and break the functionality of downloading artifacts.
     */
    enum Type {
        MEMORY,
        DISK,
        DATABASE
    }

    /**
     * {@link Type} for storage on which this implementations operates.
     */
    Type getType();

    /**
     * Reads specified location for stored data.
     * @param location location within storage to read from.
     * @return {@link Optional} containing {@link InputStream} of stored data, or empty one if location was not found.
     */
    Optional<InputSupplier<InputStream>> read(String location);

    /**
     * Writes given data into storage.
     * @param data stored data to write.
     * @return location of stored data.
     */
    String write(InputSupplier<InputStream> data) throws IOException;

    /**
     * Deletes data under specified location.
     * @param location location to delete data from.
     */
    void delete(String location) throws IOException;
}
