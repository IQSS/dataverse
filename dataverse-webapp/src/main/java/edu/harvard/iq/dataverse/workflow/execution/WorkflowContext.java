package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionIdentifier;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;

/**
 * The context in which a workflow is performed. Contains information steps might
 * need, such as the dataset being worked on and version data.
 * <p>
 * Design-wise, this class allows us to add parameters to {@link WorkflowStep} without
 * changing its method signatures, which would break break client code.
 *
 * @author michael
 */
public class WorkflowContext implements DatasetVersionIdentifier {

    public enum TriggerType {
        PrePublishDataset, PostPublishDataset
    }

    protected final TriggerType type;
    protected final Long datasetId;
    protected final Long versionNumber;
    protected final Long minorVersionNumber;
    protected final DataverseRequest request;
    protected final boolean datasetExternallyReleased;

    // -------------------- CONSTRUCTORS --------------------

    WorkflowContext(WorkflowContext other) {
        this(other.type, other.datasetId, other.versionNumber, other.minorVersionNumber,
                other.request, other.datasetExternallyReleased);
    }

    public WorkflowContext(TriggerType type, Dataset dataset, DataverseRequest request, boolean datasetExternallyReleased) {
        this(type, dataset.getId(), dataset.getLatestVersion().getVersionNumber(),
                dataset.getLatestVersion().getMinorVersionNumber(), request, datasetExternallyReleased);
    }

    public WorkflowContext(TriggerType type, long datasetId, long versionNumber, long minorVersionNumber,
                           DataverseRequest request, boolean datasetExternallyReleased) {
        this.type = type;
        this.datasetId = datasetId;
        this.versionNumber = versionNumber;
        this.minorVersionNumber = minorVersionNumber;
        this.request = request;
        this.datasetExternallyReleased = datasetExternallyReleased;
    }

    // -------------------- GETTERS --------------------

    public TriggerType getType() {
        return type;
    }

    @Override
    public Long getDatasetId() {
        return datasetId;
    }

    @Override
    public Long getVersionNumber() {
        return versionNumber;
    }

    @Override
    public Long getMinorVersionNumber() {
        return minorVersionNumber;
    }

    public boolean isMinorRelease() {
        return minorVersionNumber != 0;
    }

    public DataverseRequest getRequest() {
        return request;
    }

    public boolean isDatasetExternallyReleased() {
        return datasetExternallyReleased;
    }

    // -------------------- LOGIC --------------------

    WorkflowExecution asExecutionOf(Workflow workflow) {
        return new WorkflowExecution(workflow.getId(), type.name(),
                datasetId, versionNumber, minorVersionNumber,
                datasetExternallyReleased, workflow.getName());
    }
}
