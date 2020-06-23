package edu.harvard.iq.dataverse.workflow.listener;

import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;

/**
 * Defines callback methods executed upon certain points of workflow execution lifecycle.
 *
 * @author kaczynskid
 */
public interface WorkflowExecutionListener {

    /**
     * Executed upon successful workflow completion.
     * @param executionContext completed workflow execution context.
     */
    default void onSuccess(WorkflowExecutionContext executionContext) {
    }

    /**
     * Executed upon workflow failure, after rollback was performed.
     * @param executionContext failed workflow execution context.
     * @param failure reason for workflow failure.
     */
    default void onFailure(WorkflowExecutionContext executionContext, Failure failure) {
    }
}
