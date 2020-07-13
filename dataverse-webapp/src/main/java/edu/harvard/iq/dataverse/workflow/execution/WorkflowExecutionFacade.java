package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionIdentifier;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;

import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.Optional;

/**
 * Public API entry point for workflow execution features.
 *
 * @author kaczynskid
 */
@Singleton
public class WorkflowExecutionFacade {

    private final WorkflowExecutionService executions;

    private final WorkflowExecutionScheduler scheduler;

    // -------------------- CONSTRUCTORS --------------------

    /**
     * @deprecated for use by EJB proxy only.
     */
    public WorkflowExecutionFacade() {
        this(null, null);
    }

    @Inject
    public WorkflowExecutionFacade(WorkflowExecutionService executions, WorkflowExecutionScheduler scheduler) {
        this.executions = executions;
        this.scheduler = scheduler;
    }

    // -------------------- LOGIC --------------------

    /**
     * Starts executing {@link Workflow} under the passed context.
     *
     * @param workflow workflow to execute.
     * @param ctx context in which the workflow is executed.
     * @return context of workflow execution that was started.
     */
    public WorkflowExecutionContext start(Workflow workflow, WorkflowContext ctx) {
        WorkflowExecutionContext executionContext = executions.start(workflow, ctx);
        scheduler.executeFirstWorkflowStep(executionContext);
        return executionContext;
    }

    /**
     * Starting the resume process for a pending workflow. We first delete the
     * pending workflow to minimize double invocation, and then asynchronously
     * resume the work.
     *
     * @param invocationId Workflow execution invocation ID to resume.
     * @param externalData the response from the remote system.
     * @return WorkflowExecution that was resumed.
     */
    public Optional<WorkflowExecutionContext> resume(String invocationId, String externalData) {
        return executions.resume(invocationId)
                .map(context -> {
                    scheduler.resumePausedWorkflowStep(context, externalData);
                    return context;
                });
    }

    /**
     * Finds most recent workflow execution of given trigger type for given dataset version.
     *
     * @param triggerType trigger type to match.
     * @param versionIdentifier dataset version identifier.
     * @return WorkflowExecution matching given criteria.
     */
    public Optional<WorkflowExecution> findLatestByTriggerTypeAndDatasetVersion(
            WorkflowContext.TriggerType triggerType, DatasetVersionIdentifier versionIdentifier) {
        return executions.findLatestByTriggerTypeAndDatasetVersion(
                triggerType,
                versionIdentifier.getDatasetId(),
                versionIdentifier.getVersionNumber(),
                versionIdentifier.getMinorVersionNumber());
    }
}
