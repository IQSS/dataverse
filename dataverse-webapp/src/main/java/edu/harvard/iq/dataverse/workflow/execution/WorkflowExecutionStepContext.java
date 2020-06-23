package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionStep;
import edu.harvard.iq.dataverse.workflow.WorkflowStepRegistry;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Step execution context is an extension of {@link WorkflowExecutionContext} adding extra information
 * internal to specific workflow step processing.
 *
 * @author kaczynskid
 */
class WorkflowExecutionStepContext extends WorkflowExecutionContext {

    private final WorkflowExecutionStep stepExecution;
    private final WorkflowExecutionRepository executions;

    // -------------------- CONSTRUCTORS --------------------

    WorkflowExecutionStepContext(WorkflowExecutionContext workflowContext,
                                 WorkflowExecutionStep stepExecution,
                                 WorkflowExecutionRepository executions) {
        super(workflowContext);
        this.stepExecution = stepExecution;
        this.executions = executions;
    }

    // -------------------- GETTERS --------------------

    WorkflowExecutionStep getStepExecution() {
        return stepExecution;
    }

    // -------------------- LOGIC --------------------

    Supplier<WorkflowStepResult> start(Map<String, String> defaultParameters, WorkflowStepRegistry steps, Clock clock) {
        stepExecution.start(getStepInputParams(defaultParameters), clock);
        executions.save(execution);

        WorkflowStep step = getWorkflowStep(steps);
        return () -> step.run(this);
    }

    boolean isPaused() {
        return stepExecution.isPaused();
    }

    void pause(Map<String, String> pausedData, Clock clock) {
        stepExecution.pause(pausedData, clock);
        executions.save(execution);
    }

    Supplier<WorkflowStepResult> resume(String externalData, WorkflowStepRegistry steps, Clock clock) {
        stepExecution.resume(externalData, clock);
        executions.save(execution);

        WorkflowStep step = getWorkflowStep(steps);
        return () -> step.resume(this, stepExecution.getPausedData(), externalData);
    }

    void success(Map<String, String> outputParams, Clock clock) {
        stepExecution.success(outputParams, clock);
        executions.save(execution);
    }

    void failure(Map<String, String> outputParams, Clock clock) {
        stepExecution.failure(outputParams, clock);
        executions.save(execution);
    }

    Runnable rollback(Failure reason, WorkflowStepRegistry steps, Clock clock) {
        stepExecution.rollBack(clock);
        executions.save(execution);

        WorkflowStep step = getWorkflowStep(steps);
        return () -> step.rollback(this, reason);
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
        return "Workflow execution " + execution.getInvocationId() + " of step #" + stepExecution.getIndex();
    }
}
