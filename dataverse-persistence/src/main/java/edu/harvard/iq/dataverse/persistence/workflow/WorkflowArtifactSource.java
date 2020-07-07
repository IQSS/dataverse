package edu.harvard.iq.dataverse.persistence.workflow;

import com.google.common.io.InputSupplier;

import java.io.InputStream;

import static java.util.Objects.requireNonNull;

public class WorkflowArtifactSource {

    private final String name;
    private final String encoding;
    private final InputSupplier<InputStream> dataSupplier;

    public WorkflowArtifactSource(String name, String encoding, InputSupplier<InputStream> dataSupplier) {
        this.name = requireNonNull(name);
        this.encoding = requireNonNull(encoding);
        this.dataSupplier = requireNonNull(dataSupplier);
    }

    public String getName() {
        return name;
    }

    public String getEncoding() {
        return encoding;
    }

    public InputSupplier<InputStream> getDataSupplier() {
        return dataSupplier;
    }
}
