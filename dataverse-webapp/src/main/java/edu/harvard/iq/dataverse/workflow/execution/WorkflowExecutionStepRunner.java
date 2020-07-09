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

/**
 * Workflow step runner ensuring separate transaction for each step.
 *
 * @author kaczynskid
 */
@Singleton
public class WorkflowExecutionStepRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionStepRunner.class);

    private final WorkflowStepRegistry steps;

    // -------------------- CONSTRUCTORS --------------------

    /**
     * @deprecated for use by EJB proxy only.
     */
    public WorkflowExecutionStepRunner() {
        this(null);
    }

    @Inject
    public WorkflowExecutionStepRunner(WorkflowStepRegistry steps) {
        this.steps = steps;
    }

    // -------------------- LOGIC --------------------

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public WorkflowStepResult executeStep(WorkflowExecutionStepContext step, Success lastStepResult, String externalData) {
        if (step.isPaused()) {
            log.trace("{} - resuming", step);
            return step.resume(externalData, steps);
        } else {
            log.trace("{} - starting", step);
            return step.start(lastStepResult.getData(), steps);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void rollbackStep(WorkflowExecutionStepContext step, Failure failure) {
        try {
            log.trace("{} - rollback", step);
            step.rollback(failure, steps);
        } catch (Exception e) {
            log.warn(String.format("%s - rollback error", step), e);
        }
    }
}
