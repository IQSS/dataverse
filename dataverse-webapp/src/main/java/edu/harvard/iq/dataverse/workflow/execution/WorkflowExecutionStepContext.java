package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionStep;
import edu.harvard.iq.dataverse.workflow.WorkflowStepRegistry;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Step execution context is an extension of {@link WorkflowExecutionContext} adding extra information
 * internal to specific workflow step processing.
 *
 * @author kaczynskid
 */
class WorkflowExecutionStepContext extends WorkflowExecutionContext {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionStepContext.class);

    private final WorkflowExecutionStep stepExecution;

    // -------------------- CONSTRUCTORS --------------------

    WorkflowExecutionStepContext(WorkflowExecutionContext workflowContext, WorkflowExecutionStep stepExecution) {
        super(workflowContext);
        this.stepExecution = stepExecution;
    }

    // -------------------- GETTERS --------------------

    WorkflowExecutionStep getStepExecution() {
        return stepExecution;
    }

    // -------------------- LOGIC --------------------

    WorkflowStepResult start(Map<String, String> defaultParameters, WorkflowStepRegistry steps) {
        stepExecution.start(getStepInputParams(defaultParameters), clock);
        log.trace("### Run {}\nStart {}", execution, stepExecution);
        WorkflowStep step = getWorkflowStep(steps);
        return step.run(this);
    }

    boolean isPaused() {
        return stepExecution.isPaused();
    }

    void pause(Map<String, String> pausedData) {
        stepExecution.pause(pausedData, clock);
        log.trace("### Run {}\nPause {}", execution, stepExecution);
    }

    WorkflowStepResult resume(String externalData, WorkflowStepRegistry steps) {
        stepExecution.resume(externalData, clock);
        log.trace("### Run {}\nResume {}", execution, stepExecution);
        WorkflowStep step = getWorkflowStep(steps);
        return step.resume(this, stepExecution.getPausedData(), externalData);
    }

    void success(Map<String, String> outputParams) {
        stepExecution.success(outputParams, clock);
        log.trace("### Run {}\nSuccess {}", execution, stepExecution);
    }

    void failure(Map<String, String> outputParams) {
        stepExecution.failure(outputParams, clock);
        log.trace("### Run {}\nFailed {}", execution, stepExecution);
    }

    void rollback(Failure reason, WorkflowStepRegistry steps) {
        stepExecution.rollBack(clock);
        log.trace("### Run {}\nRollback {}", execution, stepExecution);
        WorkflowStep step = getWorkflowStep(steps);
        step.rollback(this, reason);
    }

    // -------------------- PRIVATE --------------------

    private Map<String, String> getStepInputParams(Map<String, String> defaultParameters) {
        Map<String, String> inputParams = new HashMap<>(defaultParameters);
        inputParams.putAll(workflow.getSteps().get(stepExecution.getIndex()).getStepParameters());
        return inputParams;
    }

    private WorkflowStep getWorkflowStep(WorkflowStepRegistry steps) {
        return steps.getStep(stepExecution.getProviderId(), stepExecution.getStepType(), stepExecution.getInputParams());
    }

    @Override
    public String toString() {
        return "Workflow execution " + execution.getInvocationId() + " (id=" + execution.getId() + ") " +
                " of step #" + stepExecution.getIndex() + " (id=" + stepExecution.getId() + ")";
    }
}
