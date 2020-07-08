package edu.harvard.iq.dataverse.workflow.artifacts;

import com.google.common.io.InputSupplier;

import javax.enterprise.inject.Vetoed;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static java.nio.file.Files.newInputStream;

/**
 * Allows storing binary data in form of files on local disk directory.
 */
@Vetoed
public class DiskWorkflowArtifactStorage implements WorkflowArtifactStorage {

    private final Path storage;

    public DiskWorkflowArtifactStorage(Path storage) {
        if (!Files.exists(storage)) {
            throw new IllegalArgumentException("Path " + storage + " does not exist.");
        }
        if (!Files.isDirectory(storage)) {
            throw new IllegalArgumentException("Path " + storage + " is not a directory.");
        }
        if (!Files.isWritable(storage)) {
            throw new IllegalArgumentException("Path " + storage + " is not writable.");
        }
        this.storage = storage;
    }

    @Override
    public Optional<InputSupplier<InputStream>> read(String location) {
        Path path = storage.resolve(location);
        if (Files.exists(path)) {
            return Optional.of(() -> newInputStream(path));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String write(InputSupplier<InputStream> data) throws IOException {
        String location = UUID.randomUUID().toString();
        Path path = storage.resolve(location);
        try (InputStream in = data.getInput()) {
            Files.copy(in, path);
        }
        return location;
    }

    @Override
    public void delete(String location) throws IOException {
        Path path = storage.resolve(location);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }
}
