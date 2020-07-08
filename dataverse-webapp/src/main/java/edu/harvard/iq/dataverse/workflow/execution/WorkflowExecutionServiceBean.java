package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.inject.Inject;
import java.time.Clock;
import java.util.Optional;

/**
 * Service bean for executing {@link Workflow}s
 *
 * @author kaczynskid
 */
@Singleton
public class WorkflowExecutionServiceBean {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionServiceBean.class);

    private final DatasetRepository datasets;

    private final WorkflowExecutionRepository executions;

    private final WorkflowExecutionContextFactory contextFactory;

    private final WorkflowExecutionScheduler scheduler;

    private final Clock clock;

    // -------------------- CONSTRUCTORS --------------------


    @Inject
    public WorkflowExecutionServiceBean(DatasetRepository datasets, WorkflowExecutionRepository executions,
                                        WorkflowExecutionContextFactory contextFactory,
                                        WorkflowExecutionScheduler scheduler) {
        this(datasets, executions, contextFactory, scheduler, Clock.systemUTC());
    }

    public WorkflowExecutionServiceBean(DatasetRepository datasets, WorkflowExecutionRepository executions,
                                        WorkflowExecutionContextFactory contextFactory,
                                        WorkflowExecutionScheduler scheduler, Clock clock) {
        this.datasets = datasets;
        this.executions = executions;
        this.contextFactory = contextFactory;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    // -------------------- LOGIC --------------------

    /**
     * Starts executing {@link Workflow} under the passed context.
     *
     * @param workflow workflow to execute.
     * @param ctx context in which the workflow is executed.
     * @return WorkflowExecution that was started.
     */
    public WorkflowExecution start(Workflow workflow, WorkflowContext ctx)  {
        lockDatasetForWorkflow(ctx);
        WorkflowExecutionContext context = contextFactory.workflowExecutionContextOf(ctx, workflow);
        context.start(executions, clock);
        scheduler.executeFirstWorkflowStep(context);
        return context.getExecution();
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
    public Optional<WorkflowExecution> resume(String invocationId, String externalData) {
        return executions.findByInvocationId(invocationId)
                .filter(WorkflowExecution::isPaused)
                .map(contextFactory::workflowExecutionContextOf)
                .map(executionContext -> {
                    scheduler.resumePausedWorkflowStep(executionContext, externalData);
                    return executionContext.execution;
                });
    }

    /**
     * Finds most recent workflow execution of given trigger type for given dataset version.
     *
     * @param triggerType trigger type to match.
     * @param datasetId dataset ID.
     * @param majorVersionNumber dataset major version number.
     * @param minorVersionNumber dataset minor version number.
     * @return WorkflowExecution matching given criteria.
     */
    public Optional<WorkflowExecution> findLatestByTriggerTypeAndDatasetVersion(
            WorkflowContext.TriggerType triggerType, long datasetId, long majorVersionNumber, long minorVersionNumber) {

        return executions.findLatestByTriggerTypeAndDatasetVersion(
                triggerType.name(), datasetId, majorVersionNumber, minorVersionNumber);
    }

    // -------------------- PRIVATE --------------------

    private void lockDatasetForWorkflow(WorkflowContext ctx) {
        log.trace("Creating workflow lock");
        datasets.lockDataset(ctx.getDataset(), ctx.getRequest().getAuthenticatedUser(), DatasetLock.Reason.Workflow);
    }
}
