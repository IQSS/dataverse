package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.persistence.workflow.WorkflowContextSource;
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
public class WorkflowExecutionMessage implements Serializable, WorkflowContextSource {

    private final String triggerType;
    private final long datasetId;
    private final long majorVersionNumber;
    private final long minorVersionNumber;
    private final String userId;
    private final String ipAddress;
    private final boolean datasetExternallyReleased;
    private final long workflowId;
    private final long workflowExecutionId;
    private final WorkflowStepResult lastStepResult;
    private final String externalData;

    // -------------------- CONSTRUCTORS --------------------

    WorkflowExecutionMessage(String triggerType, long datasetId, long majorVersionNumber, long minorVersionNumber,
                             String userId, String ipAddress, boolean datasetExternallyReleased, long workflowId,
                             long workflowExecutionId, WorkflowStepResult lastStepResult, String externalData) {
        this.triggerType = triggerType;
        this.datasetId = datasetId;
        this.majorVersionNumber = majorVersionNumber;
        this.minorVersionNumber = minorVersionNumber;
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.datasetExternallyReleased = datasetExternallyReleased;
        this.workflowId = workflowId;
        this.workflowExecutionId = workflowExecutionId;
        this.lastStepResult = lastStepResult;
        this.externalData = externalData;
    }

    // -------------------- GETTERS --------------------

    @Override
    public String getTriggerType() {
        return triggerType;
    }

    @Override
    public long getDatasetId() {
        return datasetId;
    }

    @Override
    public long getMajorVersionNumber() {
        return majorVersionNumber;
    }

    @Override
    public long getMinorVersionNumber() {
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

    public long getWorkflowId() {
        return workflowId;
    }

    public long getWorkflowExecutionId() {
        return workflowExecutionId;
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
                "triggerType='" + triggerType + '\'' +
                ", datasetId=" + datasetId +
                ", majorVersionNumber=" + majorVersionNumber +
                ", minorVersionNumber=" + minorVersionNumber +
                ", userId='" + userId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", datasetExternallyReleased=" + datasetExternallyReleased +
                ", workflowId=" + workflowId +
                ", workflowExecutionId=" + workflowExecutionId +
                ", lastStepResult=" + lastStepResult +
                ", externalData='" + externalData + '\'' +
                '}';
    }
}
