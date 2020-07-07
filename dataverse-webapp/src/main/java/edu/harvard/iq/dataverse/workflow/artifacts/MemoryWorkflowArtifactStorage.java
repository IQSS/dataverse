package edu.harvard.iq.dataverse.workflow.artifacts;

import com.google.common.io.InputSupplier;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;

/**
 * Simple in-memory implementation of {@link WorkflowArtifactStorage}.
 * Meant mainly for usage in tests.
 */
public class MemoryWorkflowArtifactStorage implements WorkflowArtifactStorage {

    private static final Map<String, byte[]> storage = new HashMap<>();

    @Override
    public Type getType() {
        return Type.MEMORY;
    }

    @Override
    public Optional<InputSupplier<InputStream>> read(String location) {
        return ofNullable(storage.get(location))
                .map(bytes -> () -> new ByteArrayInputStream(bytes));
    }

    @Override
    public String write(InputSupplier<InputStream> data) throws IOException {
        String location = UUID.randomUUID().toString();
        try (InputStream in = data.getInput()) {
            storage.put(location, IOUtils.toByteArray(in));
        }
        return location;
    }

    @Override
    public void delete(String location) {
        storage.remove(location);
    }
}
