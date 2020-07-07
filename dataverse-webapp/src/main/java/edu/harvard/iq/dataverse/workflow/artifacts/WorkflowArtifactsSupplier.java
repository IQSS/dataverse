package edu.harvard.iq.dataverse.workflow.artifacts;

import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactSource;

import java.util.List;

public interface WorkflowArtifactsSupplier {
    List<WorkflowArtifactSource> getArtifacts();
}
