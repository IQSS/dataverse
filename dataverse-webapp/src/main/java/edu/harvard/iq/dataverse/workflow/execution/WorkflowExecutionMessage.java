package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionContextSource;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.io.Serializable;

/**
 * A message payload carrying all workflow execution related data for the next step of the workflow
 * to be executed upon receiving of this message.
 * <p>
 * When {@link #lastStepResult} is a {@link edu.harvard.iq.dataverse.workflow.step.Failure}, than
 * upon receiving of this message a step rollback should be performed instead.
 *
 * @author kaczynskid
 */
public class WorkflowExecutionMessage implements Serializable, WorkflowExecutionContextSource {

    private final long id;
    private final long workflowId;
    private final String triggerType;
    private final long datasetId;
    private final long majorVersionNumber;
    private final long minorVersionNumber;
    private final String userId;
    private final String ipAddress;
    private final boolean datasetExternallyReleased;
    private final WorkflowStepResult lastStepResult;
    private final String externalData;

    // -------------------- CONSTRUCTORS --------------------

    WorkflowExecutionMessage(long id, long workflowId, String triggerType,
                             long datasetId, long majorVersionNumber,long minorVersionNumber,
                             String userId, String ipAddress, boolean datasetExternallyReleased,
                             WorkflowStepResult lastStepResult, String externalData) {
        this.triggerType = triggerType;
        this.datasetId = datasetId;
        this.majorVersionNumber = majorVersionNumber;
        this.minorVersionNumber = minorVersionNumber;
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.datasetExternallyReleased = datasetExternallyReleased;
        this.workflowId = workflowId;
        this.id = id;
        this.lastStepResult = lastStepResult;
        this.externalData = externalData;
    }

    // -------------------- GETTERS --------------------

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getWorkflowId() {
        return workflowId;
    }

    @Override
    public String getTriggerType() {
        return triggerType;
    }

    @Override
    public Long getDatasetId() {
        return datasetId;
    }

    @Override
    public Long getVersionNumber() {
        return majorVersionNumber;
    }

    @Override
    public Long getMinorVersionNumber() {
        return minorVersionNumber;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public boolean isDatasetExternallyReleased() {
        return datasetExternallyReleased;
    }

    public boolean isRollback() {
        return lastStepResult instanceof Failure;
    }

    public Failure getLastStepFailure() {
        if (!isRollback()) {
            throw new IllegalStateException("Not a Failure");
        }
        return (Failure) lastStepResult;
    }

    public Success getLastStepSuccess() {
        if (isRollback()) {
            throw new IllegalStateException("Not a Success");
        }
        return (Success) lastStepResult;
    }

    public String getExternalData() {
        return externalData;
    }

    @Override
    public String toString() {
        return "WorkflowExecutionMessage{" +
                "id=" + id +
                ", workflowId=" + workflowId +
                ", triggerType='" + triggerType + '\'' +
                ", datasetId=" + datasetId +
                ", majorVersionNumber=" + majorVersionNumber +
                ", minorVersionNumber=" + minorVersionNumber +
                ", userId='" + userId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", datasetExternallyReleased=" + datasetExternallyReleased +
                ", lastStepResult=" + lastStepResult +
                ", externalData='" + externalData + '\'' +
                '}';
    }
}
