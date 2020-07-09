package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionStep;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionStepRepository;
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
    protected final ApiToken apiToken;
    protected final Map<String, Object> settings;

    protected final WorkflowExecutionRepository executions;
    protected final WorkflowExecutionStepRepository stepExecutions;
    protected final Clock clock;

    // -------------------- CONSTRUCTORS --------------------

    WorkflowExecutionContext(WorkflowExecutionContext other) {
        this(other.getWorkflow(), other, other.getExecution(), other.apiToken, other.settings,
                other.executions, other.stepExecutions, other.clock);
    }

    WorkflowExecutionContext(Workflow workflow, WorkflowContext context, WorkflowExecution execution,
                                    ApiToken apiToken, Map<String, Object> settings, WorkflowExecutionRepository executions,
                                    WorkflowExecutionStepRepository stepExecutions, Clock clock) {
        super(context);
        this.workflow = workflow;
        this.execution = execution;
        this.apiToken = apiToken;
        this.settings = settings;
        this.executions = executions;
        this.stepExecutions = stepExecutions;
        this.clock = clock;
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

    void start() {
        execution.start(request.getUser().getIdentifier(),
                request.getSourceAddress().toString(),
                clock);
        executions.save(execution);
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
        if (nextStep.isNew()) {
            log.trace("### Run {}\nNew {}", execution, nextStep);
            nextStep = stepExecutions.save(nextStep);
        }
        log.trace("### Run {}\nNext {}", execution, nextStep);
        return new WorkflowExecutionStepContext(this, nextStep);
    }

    void finish() {
        execution.finish(clock);
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
