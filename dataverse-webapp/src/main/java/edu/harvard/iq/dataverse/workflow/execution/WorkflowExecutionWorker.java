package edu.harvard.iq.dataverse.workflow.execution;

import com.google.common.base.Stopwatch;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.workflow.artifacts.WorkflowArtifactServiceBean;
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
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.time.Clock;
import java.time.Duration;

import static edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionScheduler.JMS_QUEUE_RESOURCE_NAME;

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
@MessageDriven(name = "WorkflowExecutionWorker", activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = JMS_QUEUE_RESOURCE_NAME),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "true")
})
public class WorkflowExecutionWorker implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionWorker.class);

    private final DatasetRepository datasets;

    private final WorkflowExecutionRepository executions;

    private final WorkflowExecutionContextFactory contextFactory;

    private final WorkflowExecutionScheduler scheduler;

    private final WorkflowExecutionStepRunner runner;

    private final WorkflowArtifactServiceBean artifacts;

    private final Instance<WorkflowExecutionListener> executionListeners;

    private final Clock clock;

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public WorkflowExecutionWorker(DatasetRepository datasets, WorkflowExecutionRepository executions,
                                   WorkflowExecutionContextFactory contextFactory, WorkflowExecutionScheduler scheduler,
                                   WorkflowExecutionStepRunner runner, WorkflowArtifactServiceBean artifacts,
                                   Instance<WorkflowExecutionListener> executionListeners) {
        this(datasets, executions, contextFactory, scheduler, runner, artifacts, executionListeners, Clock.systemUTC());
    }

    public WorkflowExecutionWorker(DatasetRepository datasets, WorkflowExecutionRepository executions,
                                   WorkflowExecutionContextFactory contextFactory, WorkflowExecutionScheduler scheduler,
                                   WorkflowExecutionStepRunner runner, WorkflowArtifactServiceBean artifacts,
                                   Instance<WorkflowExecutionListener> executionListeners, Clock clock) {
        this.datasets = datasets;
        this.executions = executions;
        this.contextFactory = contextFactory;
        this.scheduler = scheduler;
        this.runner = runner;
        this.artifacts = artifacts;
        this.executionListeners = executionListeners;
        this.clock = clock;
    }

    // -------------------- LOGIC --------------------

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void onMessage(Message message) {
        Stopwatch watch = new Stopwatch().start();
        try {
            WorkflowExecutionMessage messageBody = (WorkflowExecutionMessage) ((ObjectMessage) message).getObject();
            WorkflowExecutionContext executionContext = contextFactory.workflowExecutionContextOf(messageBody);
            if (messageBody.isRollback()) {
                rollbackStep(executionContext, messageBody.getLastStepFailure());
            } else {
                executeStep(executionContext, messageBody.getLastStepSuccess(), messageBody.getExternalData());
            }
            log.trace("Spent {} to handle message {}", Duration.ofMillis(watch.elapsedMillis()), messageBody);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error processing message " + message, e);
        } finally {
            watch.stop();
        }
    }

    // -------------------- PRIVATE --------------------

    private void executeStep(WorkflowExecutionContext ctx, Success lastStepResult, String externalData) {
        log.trace("{} to be executed", ctx);
        if (ctx.hasMoreStepsToExecute()) {
            WorkflowExecutionStepContext step = ctx.nextStepToExecute(executions);
            log.trace("{} next to execute", step);
            Stopwatch watch = new Stopwatch().start();
            try {
                WorkflowStepResult stepResult = runner.executeStep(step, lastStepResult, externalData);
                log.trace("Spent {} executing {}", Duration.ofMillis(watch.elapsedMillis()), step);

                if (stepResult instanceof Success) {
                    stepCompleted(ctx, step, (Success) stepResult);
                } else if (stepResult instanceof Pending) {
                    stepPaused(ctx, step, (Pending) stepResult);
                } else if (stepResult instanceof Failure) {
                    stepFailed(ctx, step, (Failure) stepResult);
                }
            } catch (Exception e) {
                stepFailed(ctx, step, e, "exception while executing step: " + e.getMessage());
            } finally {
                watch.stop();
            }
        } else {
            workflowCompleted(ctx);
        }
    }

    private void stepCompleted(WorkflowExecutionContext ctx, WorkflowExecutionStepContext step, Success stepResult) {
        log.trace("{} finished successfully", step);
        step.success(stepResult.getData(), clock);
        ctx.save(datasets);
        artifacts.saveAll(ctx.getExecution().getId(), stepResult);
        scheduler.executeNextWorkflowStep(ctx, stepResult);
    }

    private void stepPaused(WorkflowExecutionContext ctx, WorkflowExecutionStepContext step, Pending stepResult) {
        log.trace("{} paused", step);
        step.pause(stepResult.getData(), clock);
    }

    private void stepFailed(WorkflowExecutionContext ctx, WorkflowExecutionStepContext step, Failure stepResult) {
        log.warn("{} failed - {} - rolling back", step, stepResult.getReason());
        step.failure(stepResult.getData(), clock);
        artifacts.saveAll(ctx.getExecution().getId(), stepResult);
        workflowFailed(ctx, stepResult);
    }

    private void stepFailed(WorkflowExecutionContext ctx, WorkflowExecutionStepContext step, Exception ex, String msg) {
        log.error(String.format("%s failed - %s", step, msg), ex);
        step.failure(new Failure(msg).getData(), clock);
        workflowFailed(ctx, ex, msg);
    }

    private void workflowCompleted(WorkflowExecutionContext ctx) {
        log.trace("{} completed", ctx);

        try {
            ctx.finish(executions, clock);

            unlockDatasetForWorkflow(ctx);

            executionListeners.forEach(handler -> handler.onSuccess(ctx));
        } catch (CommandException ex) {
            workflowFailed(ctx, ex, "exception while finalizing the publication: " + ex.getMessage());
        }
    }

    private void workflowFailed(WorkflowExecutionContext ctx, Exception ex, String msg) {
        log.error(String.format("%s failed - %s", ctx, msg), ex);
        workflowFailed(ctx, new Failure(msg));
    }

    private void workflowFailed(WorkflowExecutionContext ctx, Failure stepResult) {
        ctx.finish(executions, clock);
        scheduler.rollbackNextWorkflowStep(ctx, stepResult);
    }

    private void rollbackStep(WorkflowExecutionContext ctx, Failure failure) {
        log.trace("{} to be rolled back", ctx);
        if (ctx.hasMoreStepsToRollback()) {
            WorkflowExecutionStepContext step = ctx.nextStepToRollback(executions);
            log.trace("{} next to roll back", step);
            runner.rollbackStep(step, failure);
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
        datasets.unlockDataset(ctx.getDataset(), DatasetLock.Reason.Workflow);
    }
}
