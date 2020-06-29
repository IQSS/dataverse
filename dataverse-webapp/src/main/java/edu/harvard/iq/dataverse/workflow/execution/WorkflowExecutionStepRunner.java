package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.workflow.WorkflowStepRegistry;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.time.Clock;

/**
 * Workflow step runner ensuring separate transaction for each step.
 *
 * @author kaczynskid
 */
@Singleton
public class WorkflowExecutionStepRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionStepRunner.class);

    private final WorkflowStepRegistry steps;

    private final Clock clock;

    @Inject
    public WorkflowExecutionStepRunner(WorkflowStepRegistry steps) {
        this(steps, Clock.systemUTC());
    }

    public WorkflowExecutionStepRunner(WorkflowStepRegistry steps, Clock clock) {
        this.steps = steps;
        this.clock = clock;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public WorkflowStepResult executeStep(WorkflowExecutionStepContext step, Success lastStepResult, String externalData) {
        if (step.isPaused()) {
            log.info("{} - resuming", step);
            return step.resume(externalData, steps, clock);
        } else {
            log.info("{} - starting", step);
            return step.start(lastStepResult.getData(), steps, clock);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void rollbackStep(WorkflowExecutionStepContext step, Failure failure) {
        try {
            log.info("{} - rollback", step);
            step.rollback(failure, steps, clock);
        } catch (Exception e) {
            log.warn(String.format("%s - rollback error", step), e);
        }
    }
}
