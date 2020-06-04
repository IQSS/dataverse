package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

@Entity
@Table(name = "workflow_execution_step")
public class WorkflowExecutionStep implements JpaEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_execution_id")
    private WorkflowExecution execution;

    @Column(name = "index")
    private int index;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "step_type")
    private String stepType;

    @Column(name = "description")
    private String description;

    @Column(name = "started_at")
    private Instant startedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workflow_execution_step_input_params",
            joinColumns = @JoinColumn(name = "workflow_execution_step_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private Map<String, String> inputParams = new HashMap<>();

    @Column(name = "paused_at")
    private Instant pausedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workflow_execution_step_paused_data",
            joinColumns = @JoinColumn(name = "workflow_execution_step_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private Map<String, String> pausedData = new HashMap<>();

    @Column(name = "resumed_at")
    private Instant resumedAt;

    @Column(name = "resumed_data")
    private String resumedData;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "finished_successfully")
    private Boolean finishedSuccessfully;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workflow_execution_step_output_params",
            joinColumns = @JoinColumn(name = "workflow_execution_step_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private Map<String, String> outputParams = new HashMap<>();

    @Column(name = "rolled_back_at")
    private Instant rolledBackAt;

    // -------------------- CONSTRUCTORS --------------------

    WorkflowExecutionStep() {
        // required by ORM
    }

    public WorkflowExecutionStep(WorkflowExecution execution, int index, WorkflowStepData data) {
        this.execution = requireNonNull(execution, "A workflow execution is required");
        this.index = index;
        this.providerId = requireNonNull(data, "A workflow step data is required").getProviderId();
        this.stepType = data.getStepType();
        this.description = data.getParent().getName() + " #" + index;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public WorkflowExecution getExecution() {
        return execution;
    }

    public int getIndex() {
        return index;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getStepType() {
        return stepType;
    }

    public String getDescription() {
        return description;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Map<String, String> getInputParams() {
        return inputParams;
    }

    public Instant getPausedAt() {
        return pausedAt;
    }

    public Map<String, String> getPausedData() {
        return pausedData;
    }

    public Instant getResumedAt() {
        return resumedAt;
    }

    public String getResumedData() {
        return resumedData;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Boolean getFinishedSuccessfully() {
        return finishedSuccessfully;
    }

    public Map<String, String> getOutputParams() {
        return outputParams;
    }

    public Instant getRolledBackAt() {
        return rolledBackAt;
    }

    // -------------------- LOGIC --------------------

    public boolean isStarted() {
        return startedAt != null;
    }

    public void start(Map<String, String> inputParams, Clock clock) {
        if (isStarted()) {
            throw new IllegalStateException("Cannot start step - already started");
        }
        this.startedAt = requireNonNull(clock, "A clock is required").instant();
        this.inputParams.putAll(ofNullable(inputParams).orElseGet(HashMap::new));
    }

    public boolean isPaused() {
        return pausedAt != null;
    }

    public void pause(Map<String, String> pausedData, Clock clock) {
        if (!isStarted()) {
            throw new IllegalStateException("Cannot pause step - not started");
        }
        if (isPaused()) {
            throw new IllegalStateException("Cannot pause step - already paused");
        }
        this.pausedAt = requireNonNull(clock, "A clock is required").instant();
        this.pausedData.putAll(ofNullable(pausedData).orElseGet(HashMap::new));
    }

    public boolean isResumed() {
        return resumedAt != null;
    }

    public void resume(String data, Clock clock) {
        if (!isPaused()) {
            throw new IllegalStateException("Cannot resume step - not paused");
        }
        if (isResumed()) {
            throw new IllegalStateException("Cannot resume step - already resume");
        }
        this.resumedAt = requireNonNull(clock, "A clock is required").instant();
        this.resumedData = data;
    }

    public boolean isFinished() {
        return finishedAt != null;
    }

    public void success(Map<String, String> outputParams, Clock clock) {
        finish(true, outputParams, clock);
    }

    public void failure(Map<String, String> outputParams, Clock clock) {
        finish(false, outputParams, clock);
    }

    public boolean isRolledBack() {
        return rolledBackAt != null;
    }

    public boolean isRollBackNeeded() {
        return isStarted() && !isRolledBack();
    }

    public void rollBack(Clock clock) {
        if (!isStarted()) {
            throw new IllegalStateException("Cannot rollback step - not started");
        }
        if (isRolledBack()) {
            throw new IllegalStateException("Cannot rollback step - already rolled back");
        }
        this.rolledBackAt = requireNonNull(clock, "A clock is required").instant();
    }

    // -------------------- PRIVATE --------------------

    private void finish(boolean successfully, Map<String, String> outputParams, Clock clock) {
        if (!isStarted() || (isPaused() && !isResumed())) {
            throw new IllegalStateException("Cannot finish step - not running");
        }
        if (isFinished()) {
            throw new IllegalStateException("Cannot finish step - already finished");
        }
        this.finishedAt = requireNonNull(clock, "A clock is required").instant();
        this.finishedSuccessfully = successfully;
        this.outputParams.putAll(ofNullable(outputParams).orElseGet(HashMap::new));
    }
}
