package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Map;

/**
 * Workflow execution context in an extension of {@link WorkflowContext} adding extra information
 * internal to workflow processing.
 *
 * @author kaczynskid
 */
public class WorkflowExecutionContext extends WorkflowContext {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionContext.class);

    protected final Workflow workflow;
    protected final WorkflowExecution execution;
    protected final Map<String, Object> settings;
    protected final Clock clock;

    // -------------------- CONSTRUCTORS --------------------

    WorkflowExecutionContext(WorkflowExecutionContext other) {
        this(other.workflow, other, other.execution, other.settings, other.clock);
    }

    WorkflowExecutionContext(Workflow workflow, WorkflowContext context, WorkflowExecution execution,
                             Map<String, Object> settings, Clock clock) {
        super(context);
        this.workflow = workflow;
        this.execution = execution;
        this.settings = settings;
        this.clock = clock;
    }

    // -------------------- GETTERS --------------------

    public Workflow getWorkflow() {
        return workflow;
    }

    public WorkflowExecution getExecution() {
        return execution;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public String getInvocationId() {
        return execution.getInvocationId();
    }

    // -------------------- LOGIC --------------------

    void start(WorkflowExecutionRepository executions) {
        execution.start(request.getUser().getIdentifier(),
                request.getSourceAddress().toString(),
                clock);
        executions.saveAndFlush(execution);
        log.trace("### Start {}", execution);
    }

    boolean hasMoreStepsToExecute() {
        return execution.hasMoreStepsToExecute(workflow.getSteps());
    }

    WorkflowExecutionStepContext nextStepToExecute() {
        if (!hasMoreStepsToExecute()) {
            throw new IllegalStateException("No more steps to run");
        }
        WorkflowExecutionStep nextStep = execution.nextStepToExecute(workflow.getSteps());
        log.trace("### Run {}\nNext {}", execution, nextStep);
        return new WorkflowExecutionStepContext(this, nextStep);
    }

    void finish(WorkflowExecutionRepository executions) {
        execution.finish(clock);
        executions.saveAndFlush(execution);
        log.trace("### Finish {}", execution);
    }

    boolean hasMoreStepsToRollback() {
        return execution.hasMoreStepsToRollback();
    }

    WorkflowExecutionStepContext nextStepToRollback() {
        if (!hasMoreStepsToRollback()) {
            throw new IllegalStateException("No more steps to rollback");
        }
        WorkflowExecutionStep nextStep = execution.nextStepToRollback();
        log.trace("### Run {}\nRollback {}", execution, nextStep);
        return new WorkflowExecutionStepContext(this, nextStep);
    }

    @Override
    public String toString() {
        return "Workflow execution " + execution.getInvocationId() + " (id=" + execution.getId() + ")";
    }
}
