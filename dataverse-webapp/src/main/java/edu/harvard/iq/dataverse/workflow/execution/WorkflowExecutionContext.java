package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionStep;

import java.time.Clock;
import java.util.Map;

/**
 * Workflow execution context in an extension of {@link WorkflowContext} adding extra information
 * internal to workflow processing.
 *
 * @author kaczynskid
 */
public class WorkflowExecutionContext extends WorkflowContext {

    protected final Workflow workflow;
    protected final WorkflowExecution execution;
    private final ApiToken apiToken;
    private final Map<String, Object> settings;

    // -------------------- CONSTRUCTORS --------------------

    WorkflowExecutionContext(WorkflowExecutionContext other) {
        this(other.getWorkflow(), other, other.getExecution(), other.apiToken, other.settings);
    }

    public WorkflowExecutionContext(Workflow workflow, WorkflowContext context, WorkflowExecution execution,
                                    ApiToken apiToken, Map<String, Object> settings) {
        super(context);
        this.workflow = workflow;
        this.execution = execution;
        this.apiToken = apiToken;
        this.settings = settings;
    }

    // -------------------- GETTERS --------------------

    public Workflow getWorkflow() {
        return workflow;
    }

    public WorkflowExecution getExecution() {
        return execution;
    }

    public ApiToken getApiToken() {
        return apiToken;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public String getInvocationId() {
        return execution.getInvocationId();
    }

    // -------------------- LOGIC --------------------

    void start(WorkflowExecutionRepository executions, Clock clock) {
        execution.start(request.getUser().getIdentifier(),
                request.getSourceAddress().toString(),
                clock);
        executions.save(execution);
    }

    boolean hasMoreStepsToExecute() {
        return execution.hasMoreStepsToExecute(workflow.getSteps());
    }

    WorkflowExecutionStepContext nextStepToExecute(WorkflowExecutionRepository executions) {
        if (!hasMoreStepsToExecute()) {
            throw new IllegalStateException("No more steps to run");
        }
        WorkflowExecutionStep nextStep = execution.nextStepToExecute(workflow.getSteps());
        if (nextStep.isNew()) {
            executions.saveAndFlush(execution);
        }
        // we need to call getLastStep() again here from execution, because when persisting new steps above
        // JPA will copy the object and nextStep will not be attached to session and will have no id assigned
        return new WorkflowExecutionStepContext(this, execution.getLastStep(), executions);
    }

    void finish(WorkflowExecutionRepository executions, Clock clock) {
        execution.finish(clock);
        executions.save(execution);
    }

    boolean hasMoreStepsToRollback() {
        return execution.hasMoreStepsToRollback();
    }

    WorkflowExecutionStepContext nextStepToRollback(WorkflowExecutionRepository executions) {
        if (!hasMoreStepsToRollback()) {
            throw new IllegalStateException("No more steps to rollback");
        }
        WorkflowExecutionStep nextStep = execution.nextStepToRollback();
        return new WorkflowExecutionStepContext(this, nextStep, executions);
    }

    @Override
    public String toString() {
        return "Workflow execution " + execution.getInvocationId() + " (id=" + execution.getId() + ")";
    }
}
