package edu.harvard.iq.dataverse.workflow.handler;

import edu.harvard.iq.dataverse.workflow.WorkflowExecutionContext;

/**
 * Interface that is used after the workflow failed, it was made to be an interface so it can be implemented
 * in external repositories.
 * <p>
 * Currently all implementations are going to be used after workflow fails.
 */
public interface WorkflowFailureHandler {

    void handleFailure(WorkflowExecutionContext workflowExecutionContext);
}
