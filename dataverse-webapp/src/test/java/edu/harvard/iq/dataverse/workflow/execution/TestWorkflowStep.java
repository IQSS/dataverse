package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Configurable {@link WorkflowStep} implementation to be used for testing.
 * Allows to trigger custom behaviours to test different workflow execution scenarios.
 * @author kaczynskid
 */
public class TestWorkflowStep implements WorkflowStep {

    private final Map<String, String> params;

    private BiFunction<WorkflowExecutionContext, Map<String, String>, WorkflowStepResult> runningLogic =
            (ctx, params) -> new Success(params);

    private TriFunction<WorkflowExecutionContext, Map<String, String>, String, WorkflowStepResult> resumingLogic =
            (ctx, inData, exData) -> { throw new IllegalStateException(); };

    private BiConsumer<WorkflowExecutionContext, Failure> failureConsumer =
            (ctx, f) -> { };

    // -------------------- CONSTRUCTORS --------------------

    public TestWorkflowStep(Map<String, String> params) {
        this.params = params;
    }

    public TestWorkflowStep withRunningLogic(
            BiFunction<WorkflowExecutionContext, Map<String, String>, WorkflowStepResult> runningLogic) {
        this.runningLogic = runningLogic;
        return this;
    }

    public TestWorkflowStep withResumingLogic(
            TriFunction<WorkflowExecutionContext, Map<String, String>, String, WorkflowStepResult> resumingLogic) {
        this.resumingLogic = resumingLogic;
        return this;
    }

    public TestWorkflowStep withFailureConsumer(BiConsumer<WorkflowExecutionContext, Failure> failureConsumer) {
        this.failureConsumer = failureConsumer;
        return this;
    }

    public TestWorkflowStep pausingAndResumingSuccessfully() {
        return withRunningLogic((ctx, params) -> new Pending(params))
                .withResumingLogic((ctx, inData, exData) -> new Success(inData));
    }

    // -------------------- LOGIC --------------------

    @Override
    public WorkflowStepResult run(WorkflowExecutionContext context) {
        return runningLogic.apply(context, params);
    }

    @Override
    public WorkflowStepResult resume(WorkflowExecutionContext context, Map<String, String> internalData, String externalData) {
        return resumingLogic.apply(context, internalData, externalData);
    }

    @Override
    public void rollback(WorkflowExecutionContext context, Failure reason) {
        failureConsumer.accept(context, reason);
    }

    // -------------------- INNER CLASSES --------------------

    interface TriFunction<X, Y, Z, R> {
        R apply(X x, Y y, Z z);
    }
}
