package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.workflow.WorkflowStepRegistry;
import edu.harvard.iq.dataverse.workflow.listener.WorkflowExecutionListener;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.time.Clock;
import java.util.function.Supplier;

/**
 * A JMS {@link MessageListener} handling {@link WorkflowExecutionMessage}'s. Takes care of actual workflow steps
 * execution, one at a time, always scheduling the next step separately. Finally, when there is no more steps
 * to execute, it finishes the workflow. Also support rolling back the workflow one step at a time.
 * <p>
 * Each workflow with N steps will emit at least N + 1 events - one per each step execution and extra one
 * for finishing the flow. Each message contains the same workflow data and differs only with last step
 * result. Upon receiving each message we fetch current workflow state from database, and if there is a next
 * step to execute, we execute it, and if all were already done, we finish the workflow execution.
 * 
 * @author kaczynskid
 */
@MessageDriven(mappedName = "jms/queue/dataverseWorkflow", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")
})
public class WorkflowExecutionWorker implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionWorker.class);

    private final DatasetRepository datasets;

    private final WorkflowStepRegistry steps;

    private final WorkflowExecutionRepository executions;

    private final WorkflowExecutionContextFactory contextFactory;

    private final WorkflowExecutionScheduler scheduler;

    private final Instance<WorkflowExecutionListener> executionListeners;

    private final Clock clock;

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public WorkflowExecutionWorker(DatasetRepository datasets, WorkflowStepRegistry steps,
                                   WorkflowExecutionRepository executions, WorkflowExecutionContextFactory contextFactory,
                                   WorkflowExecutionScheduler scheduler, Instance<WorkflowExecutionListener> executionListeners) {
        this(datasets, steps, executions, contextFactory, scheduler, executionListeners, Clock.systemUTC());
    }

    public WorkflowExecutionWorker(DatasetRepository datasets, WorkflowStepRegistry steps,
                                   WorkflowExecutionRepository executions, WorkflowExecutionContextFactory contextFactory,
                                   WorkflowExecutionScheduler scheduler, Instance<WorkflowExecutionListener> executionListeners,
                                   Clock clock) {
        this.datasets = datasets;
        this.steps = steps;
        this.executions = executions;
        this.contextFactory = contextFactory;
        this.scheduler = scheduler;
        this.executionListeners = executionListeners;
        this.clock = clock;
    }

    // -------------------- LOGIC --------------------

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void onMessage(Message message) {
        try {
            WorkflowExecutionMessage messageBody = (WorkflowExecutionMessage) ((ObjectMessage) message).getObject();
            WorkflowExecutionContext executionContext = contextFactory.workflowExecutionContextOf(messageBody);
            if (messageBody.isRollback()) {
                rollbackStep(executionContext, messageBody.getLastStepFailure());
            } else {
                executeStep(executionContext, messageBody.getLastStepSuccess(), messageBody.getExternalData());
            }
        } catch (JMSException e) {
            throw new RuntimeException("Unexpected error processing message " + message, e);
        }
    }

    // -------------------- PRIVATE --------------------

    private void executeStep(WorkflowExecutionContext ctx, Success lastStepResult, String externalData) {
        if (ctx.hasMoreStepsToExecute()) {
            WorkflowExecutionStepContext step = ctx.nextStepToExecute(executions);
            try {
                WorkflowStepResult stepResult = executeStep(step, lastStepResult, externalData);

                if (stepResult instanceof Success) {
                    stepCompleted(ctx, step, (Success) stepResult);
                } else if (stepResult instanceof Pending) {
                    stepPaused(ctx, step, (Pending) stepResult);
                } else if (stepResult instanceof Failure) {
                    stepFailed(ctx, step, (Failure) stepResult);
                }
            } catch (Exception e) {
                stepFailed(ctx, step, e, "exception while executing step: " + e.getMessage());
            }
        } else {
            workflowCompleted(ctx);
        }
    }

    private WorkflowStepResult executeStep(WorkflowExecutionStepContext step, Success lastStepResult, String externalData) {
        if (step.isPaused()) {
            log.info("{} - resuming", step);
            return getInNewTransaction(
                    step.resume(externalData, steps, clock)
            );
        } else {
            log.info("{} - starting", step);
            return getInNewTransaction(
                    step.start(lastStepResult.getData(), steps, clock)
            );
        }
    }

    private void stepCompleted(WorkflowExecutionContext ctx, WorkflowExecutionStepContext step, Success stepResult) {
        log.info("{} finished successfully", step);
        step.success(stepResult.getData(), clock);
        ctx.save(datasets);
        scheduler.executeNextWorkflowStep(ctx, stepResult);
    }

    private void stepPaused(WorkflowExecutionContext ctx, WorkflowExecutionStepContext step, Pending stepResult) {
        log.info("{} paused", step);
        step.pause(stepResult.getData(), clock);
    }

    private void stepFailed(WorkflowExecutionContext ctx, WorkflowExecutionStepContext step, Exception ex, String msg) {
        log.error(String.format("%s failed - %s", step, msg), ex);
        step.failure(new Failure(msg).getData(), clock);
        workflowFailed(ctx, ex, msg);
    }

    private void stepFailed(WorkflowExecutionContext ctx, WorkflowExecutionStepContext step, Failure stepResult) {
        log.warn("{} failed - {} - rolling back", step, stepResult.getReason());
        step.failure(stepResult.getData(), clock);
        workflowFailed(ctx, stepResult);
    }

    private void workflowCompleted(WorkflowExecutionContext ctx) {
        log.info("Workflow {} completed", ctx.getInvocationId());

        try {
            ctx.finish(executions, clock);

            unlockDatasetForWorkflow(ctx);

            executionListeners.forEach(handler -> handler.onSuccess(ctx));
        } catch (CommandException ex) {
            workflowFailed(ctx, ex, "exception while finalizing the publication: " + ex.getMessage());
        }
    }

    private void workflowFailed(WorkflowExecutionContext ctx, Exception ex, String msg) {
        log.error(String.format("Workflow %s failed - %s", ctx.getInvocationId(), msg), ex);
        workflowFailed(ctx, new Failure(msg));
    }

    private void workflowFailed(WorkflowExecutionContext ctx, Failure stepResult) {
        ctx.finish(executions, clock);
        scheduler.rollbackNextWorkflowStep(ctx, stepResult);
    }

    private void rollbackStep(WorkflowExecutionContext ctx, Failure failure) {
        if (ctx.hasMoreStepsToRollback()) {
            WorkflowExecutionStepContext step = ctx.nextStepToRollback(executions);
            try {
                log.info("{} - rollback", step);
                runInNewTransaction(
                        step.rollback(failure, steps, clock)
                );
            } catch (Exception e) {
                log.warn(String.format("%s - rollback error", step), e);
            }

            scheduler.rollbackNextWorkflowStep(ctx, failure);
        } else {
            try {
                unlockDatasetForWorkflow(ctx);
            } catch (CommandException ex) {
                log.error("Error restoring dataset locks state after rollback: " + ex.getMessage(), ex);
            }

            executionListeners.forEach(handler -> handler.onFailure(ctx, failure));
        }
    }

    private void unlockDatasetForWorkflow(WorkflowContext ctx) {
        log.trace("Removing workflow lock");
        runInNewTransaction(() ->
                datasets.unlockDataset(ctx.getDataset(), DatasetLock.Reason.Workflow)
        );
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    <T> T getInNewTransaction(Supplier<T> logic) {
        return logic.get();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    void runInNewTransaction(Runnable logic) {
        logic.run();
    }
}
