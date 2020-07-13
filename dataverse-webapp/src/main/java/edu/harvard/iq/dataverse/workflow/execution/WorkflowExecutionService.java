package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.dataset.DatasetLockServiceBean;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionContextSource;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionStepRepository;
import edu.harvard.iq.dataverse.workflow.artifacts.WorkflowArtifactServiceBean;
import edu.harvard.iq.dataverse.workflow.listener.WorkflowExecutionListener;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.Success;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Optional;

/**
 * Internal service bean for executing {@link Workflow}s.
 * Apart from main business logic it is responsible for managing database transactions.
 *
 * @author kaczynskid
 */
@Singleton
public class WorkflowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionService.class);

    private final DatasetLockServiceBean datasetLocks;

    private final WorkflowExecutionRepository executions;

    private final WorkflowExecutionStepRepository stepExecutions;

    private final WorkflowExecutionContextFactory contextFactory;

    private final WorkflowArtifactServiceBean artifacts;

    private final Instance<WorkflowExecutionListener> executionListeners;


    // -------------------- CONSTRUCTORS --------------------

    /**
     * @deprecated for use by EJB proxy only.
     */
    public WorkflowExecutionService() {
        this(null, null, null, null, null, null);
    }

    @Inject
    public WorkflowExecutionService(DatasetLockServiceBean datasetLocks,
                                    WorkflowExecutionRepository executions,
                                    WorkflowExecutionStepRepository stepExecutions,
                                    WorkflowExecutionContextFactory contextFactory,
                                    WorkflowArtifactServiceBean artifacts,
                                    Instance<WorkflowExecutionListener> executionListeners) {
        this.datasetLocks = datasetLocks;
        this.executions = executions;
        this.stepExecutions = stepExecutions;
        this.contextFactory = contextFactory;
        this.artifacts = artifacts;
        this.executionListeners = executionListeners;
    }

    // -------------------- LOGIC --------------------

    /**
     * Starts executing {@link Workflow} under the passed context.
     *
     * @param workflow workflow to execute.
     * @param ctx context in which the workflow is executed.
     * @return context of workflow execution that was started.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public WorkflowExecutionContext start(Workflow workflow, WorkflowContext ctx)  {
        lockDatasetForWorkflow(ctx);
        WorkflowExecutionContext context = contextFactory.workflowExecutionContextOf(ctx, workflow);
        context.start(executions);
        return context;
    }

    /**
     * Loads {@link WorkflowExecutionContext} using data stored in {@link WorkflowExecutionContextSource}.
     *
     * @param source provided {@link WorkflowExecutionContextSource} to compose the context for.
     * @return ready to use {@link WorkflowExecutionContext}.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public WorkflowExecutionContext loadExecutionContext(WorkflowExecutionContextSource source) {
        return contextFactory.workflowExecutionContextOf(source);
    }

    /**
     * Starting the resume process for a pending workflow. We first delete the
     * pending workflow to minimize double invocation, and then asynchronously
     * resume the work.
     *
     * @param invocationId Workflow execution invocation ID to resume.
     * @return WorkflowExecution that was resumed.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Optional<WorkflowExecutionContext> resume(String invocationId) {
        return executions.findByInvocationId(invocationId)
                .filter(WorkflowExecution::isPaused)
                .map(contextFactory::workflowExecutionContextOf);
    }

    /**
     * Marks given step as completed.
     * @param stepCtx completed step.
     * @param stepResult step completion result.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void stepCompleted(WorkflowExecutionStepContext stepCtx, Success stepResult) {
        log.trace("{} finished successfully", stepCtx);
        stepCtx.succeeded(stepResult.getData(), stepExecutions);

        artifacts.createAll(stepCtx.getExecution().getId(), stepResult.getArtifacts());
    }

    /**
     * Marks given step as paused.
     * @param stepCtx paused step.
     * @param stepResult step pause data.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void stepPaused(WorkflowExecutionStepContext stepCtx, Pending stepResult) {
        log.trace("{} paused", stepCtx);
        stepCtx.paused(stepResult.getData(), stepExecutions);
    }

    /**
     * Marks given step as failed.
     * @param stepCtx failed step.
     * @param stepResult step failure cause.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void stepFailed(WorkflowExecutionStepContext stepCtx, Failure stepResult) {
        log.warn("{} failed - {} - rolling back", stepCtx, stepResult.getReason());
        stepCtx.failed(stepResult.getData(), stepExecutions);

        artifacts.createAll(stepCtx.getExecution().getId(), stepResult.getArtifacts());

        workflowFailed(stepCtx, stepResult);
    }

    /**
     * Marks given as rolled back.
     * @param stepCtx rolled back step.
     * @param failure step roll back cause.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void stepRolledBack(WorkflowExecutionStepContext stepCtx, Failure failure) {
        stepCtx.rolledBack(stepExecutions);
        log.trace(String.format("%s rolled back", stepCtx));
    }

    /**
     * Marks workflow as completed.
     * @param executionCtx completed execution.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void workflowCompleted(WorkflowExecutionContext executionCtx) {
        try {
            executionCtx.finish(executions);
            log.trace("{} completed", executionCtx);

            unlockDatasetForWorkflow(executionCtx);

            executionListeners.forEach(handler -> handler.onSuccess(executionCtx));
        } catch (CommandException ex) {
            workflowFailed(executionCtx, ex, "exception while finalizing the publication: " + ex.getMessage());
        }
    }

    /**
     * Marks workflow as failed.
     * @param executionCtx failed execution.
     * @param ex cause of workflow failure.
     * @param msg workflow failure message.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void workflowFailed(WorkflowExecutionContext executionCtx, Exception ex, String msg) {
        log.error(String.format("%s failed - %s", executionCtx, msg), ex);
        workflowFailed(executionCtx, new Failure(msg));
    }

    /**
     * Marks workflow as failed.
     * @param executionCtx failed execution.
     * @param failure cause of workflow failure.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void workflowFailed(WorkflowExecutionContext executionCtx, Failure failure) {
        executionCtx.finish(executions);
        log.trace("{} failed", executionCtx);
    }

    /**
     * Marks workflow as rolled back.
     * @param executionCtx rolled back execution.
     * @param failure cause of roll back.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void workflowRolledBack(WorkflowExecutionContext executionCtx, Failure failure) {
        try {
            unlockDatasetForWorkflow(executionCtx);
        } catch (CommandException ex) {
            log.error("Error restoring dataset locks state after rollback: " + ex.getMessage(), ex);
        }

        executionListeners.forEach(handler -> handler.onFailure(executionCtx, failure));
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
        datasetLocks.lockDataset(ctx.getDatasetId(), ctx.getRequest().getAuthenticatedUser(), DatasetLock.Reason.Workflow);
    }

    private void unlockDatasetForWorkflow(WorkflowContext ctx) {
        log.trace("Removing workflow lock");
        datasetLocks.unlockDataset(ctx.getDatasetId(), DatasetLock.Reason.Workflow);
    }
}
