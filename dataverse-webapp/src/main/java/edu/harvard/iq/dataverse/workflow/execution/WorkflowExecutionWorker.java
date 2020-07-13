package edu.harvard.iq.dataverse.workflow.execution;

import com.google.common.base.Stopwatch;
import edu.harvard.iq.dataverse.workflow.WorkflowStepRegistry;
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
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
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

    private final WorkflowExecutionService executions;

    private final WorkflowExecutionScheduler scheduler;

    private final WorkflowStepRegistry steps;

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public WorkflowExecutionWorker(WorkflowExecutionService executions,
                                   WorkflowExecutionScheduler scheduler,
                                   WorkflowStepRegistry steps) {
        this.executions = executions;
        this.scheduler = scheduler;
        this.steps = steps;
    }

    // -------------------- LOGIC --------------------

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void onMessage(Message message) {
        Stopwatch watch = new Stopwatch().start();
        try {
            WorkflowExecutionMessage messageBody = (WorkflowExecutionMessage) ((ObjectMessage) message).getObject();
            WorkflowExecutionContext executionContext = executions.loadExecutionContext(messageBody);
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
            WorkflowExecutionStepContext step = ctx.nextStepToExecute();
            log.trace("{} next to execute", step);
            Stopwatch watch = new Stopwatch().start();
            try {
                WorkflowStepResult stepResult = executeStep(step, lastStepResult, externalData);
                log.trace("Spent {} executing {}", Duration.ofMillis(watch.elapsedMillis()), step);

                if (stepResult instanceof Success) {
                    stepCompleted(step, (Success) stepResult);
                } else if (stepResult instanceof Pending) {
                    executions.stepPaused(step, (Pending) stepResult);
                } else if (stepResult instanceof Failure) {
                    stepFailed(step, (Failure) stepResult);
                }
            } catch (Exception e) {
                log.warn(String.format("%s - execution error", step), e);
                stepFailed(step, new Failure("exception while executing step: " + e.getMessage()));
            } finally {
                watch.stop();
            }
        } else {
            executions.workflowCompleted(ctx);
        }
    }

    private WorkflowStepResult executeStep(WorkflowExecutionStepContext step, Success lastStepResult, String externalData) {
        if (step.isPaused()) {
            log.trace("{} - resuming", step);
            return step.resume(externalData, steps);
        } else {
            log.trace("{} - starting", step);
            return step.start(lastStepResult.getData(), steps);
        }
    }

    private void stepCompleted(WorkflowExecutionStepContext step, Success stepResult) {
        executions.stepCompleted(step, stepResult);
        scheduler.executeNextWorkflowStep(step, stepResult);
    }

    private void stepFailed(WorkflowExecutionStepContext step, Failure stepResult) {
        executions.stepFailed(step, stepResult);
        scheduler.rollbackNextWorkflowStep(step, stepResult);
    }

    private void rollbackStep(WorkflowExecutionContext ctx, Failure failure) {
        log.trace("{} to be rolled back", ctx);
        if (ctx.hasMoreStepsToRollback()) {
            WorkflowExecutionStepContext step = ctx.nextStepToRollback();
            log.trace("{} next to roll back", step);
            Stopwatch watch = new Stopwatch().start();
            try {
                rollbackStep(step, failure);
                log.trace("Spent {} rolling back {}", Duration.ofMillis(watch.elapsedMillis()), step);
                stepRolledBack(step, failure);
            } catch (Exception e) {
                log.warn(String.format("%s - rollback error", step), e);
                stepRolledBack(step, new Failure("exception while rolling back step: " + e.getMessage()));
            } finally {
                watch.stop();
            }
        } else {
            executions.workflowRolledBack(ctx, failure);
        }
    }

    private void rollbackStep(WorkflowExecutionStepContext step, Failure failure) {
        log.trace("{} - rollback", step);
        step.rollback(failure, steps);
    }

    private void stepRolledBack(WorkflowExecutionStepContext step, Failure failure) {
        executions.stepRolledBack(step, failure);
        scheduler.rollbackNextWorkflowStep(step, failure);
    }
}
