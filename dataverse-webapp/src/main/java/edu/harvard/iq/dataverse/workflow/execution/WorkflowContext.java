package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
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
public class WorkflowContext {

    public enum TriggerType {
        PrePublishDataset, PostPublishDataset
    }

    protected final TriggerType type;
    protected final Dataset dataset;
    protected final long nextVersionNumber;
    protected final long nextMinorVersionNumber;
    protected final DataverseRequest request;
    protected final boolean datasetExternallyReleased;

    // -------------------- CONSTRUCTORS --------------------

    WorkflowContext(WorkflowContext other) {
        this(other.type, other.dataset, other.nextVersionNumber, other.nextMinorVersionNumber,
                other.request, other.datasetExternallyReleased);
    }

    public WorkflowContext(TriggerType type, Dataset dataset, DataverseRequest request, boolean datasetExternallyReleased) {
        this(type, dataset, dataset.getLatestVersion().getVersionNumber(),
                dataset.getLatestVersion().getMinorVersionNumber(), request, datasetExternallyReleased);
    }

    public WorkflowContext(TriggerType type, Dataset dataset, long nextVersionNumber, long nextMinorVersionNumber,
                           DataverseRequest request, boolean datasetExternallyReleased) {
        this.type = type;
        this.dataset = dataset;
        this.nextVersionNumber = nextVersionNumber;
        this.nextMinorVersionNumber = nextMinorVersionNumber;
        this.request = request;
        this.datasetExternallyReleased = datasetExternallyReleased;
    }

    // -------------------- GETTERS --------------------

    public TriggerType getType() {
        return type;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public long getNextVersionNumber() {
        return nextVersionNumber;
    }

    public long getNextMinorVersionNumber() {
        return nextMinorVersionNumber;
    }

    public boolean isMinorRelease() {
        return nextMinorVersionNumber != 0;
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
                dataset.getId(), nextVersionNumber, nextMinorVersionNumber,
                datasetExternallyReleased, workflow.getName());
    }

    void save(DatasetRepository datasets) {
        datasets.save(getDataset());
    }
}
