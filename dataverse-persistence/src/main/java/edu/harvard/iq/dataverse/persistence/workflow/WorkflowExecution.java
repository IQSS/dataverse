package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Entity
@Table(name = "workflow_execution")
public class WorkflowExecution implements JpaEntity<Long>, WorkflowContextSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id")
    private long workflowId;

    @Column(name = "trigger_type")
    private String triggerType;

    @Column(name = "invocation_id")
    private String invocationId;

    @Column(name = "dataset_id")
    private long datasetId;

    @Column(name = "major_version_number")
    private long majorVersionNumber;

    @Column(name = "minor_version_number")
    private long minorVersionNumber;

    @Column(name = "dataset_externally_released")
    private boolean datasetExternallyReleased;

    @Column(name = "description")
    private String description;

    @Column(name = "started_at", columnDefinition = "TIMESTAMP")
    private Instant startedAt;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "finished_at", columnDefinition = "TIMESTAMP")
    private Instant finishedAt;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("index ASC")
    private List<WorkflowExecutionStep> steps = new ArrayList<>();

    // -------------------- CONSTRUCTORS --------------------

    WorkflowExecution() {
        // required by ORM
    }

    public WorkflowExecution(long workflowId, String triggerType, long datasetId, long majorVersionNumber,
                             long minorVersionNumber, boolean datasetExternallyReleased, String description) {
        this.workflowId = workflowId;
        this.triggerType = requireNonNull(triggerType, "A triggerType is required");
        this.invocationId = UUID.randomUUID().toString();
        this.datasetId = datasetId;
        this.majorVersionNumber = majorVersionNumber;
        this.minorVersionNumber = minorVersionNumber;
        this.datasetExternallyReleased = datasetExternallyReleased;
        this.description = requireNonNull(description, "A description is required");
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    public long getWorkflowId() {
        return workflowId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public String getInvocationId() {
        return invocationId;
    }

    public long getDatasetId() {
        return datasetId;
    }

    public long getMajorVersionNumber() {
        return majorVersionNumber;
    }

    public long getMinorVersionNumber() {
        return minorVersionNumber;
    }

    public boolean isDatasetExternallyReleased() {
        return datasetExternallyReleased;
    }

    public String getDescription() {
        return description;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public List<WorkflowExecutionStep> getSteps() {
        return steps;
    }

    // -------------------- LOGIC --------------------

    public boolean isStarted() {
        return startedAt != null;
    }

    public void start(String userId, String ipAddress, Clock clock) {
        if (isStarted()) {
            throw new IllegalStateException("Cannot start workflow - already started");
        }
        this.startedAt = requireNonNull(clock, "A clock is required").instant();
        this.userId = requireNonNull(userId, "A userId is required");
        this.ipAddress = requireNonNull(ipAddress, "An ipAddress is required");
    }

    public boolean isPaused() {
        if (isEmpty(steps)) {
            return false;
        }
        WorkflowExecutionStep lastStep = steps.get(steps.size() - 1);
        return lastStep.isPaused() && !lastStep.isResumed();
    }

    public boolean hasMoreStepsToExecute(List<WorkflowStepData> definedSteps) {
        if (isEmpty(steps)) {
            return hasElementAt(definedSteps, 0);
        } else {
            WorkflowExecutionStep lastStep = steps.get(steps.size() - 1);
            if (lastStep.isFinished()) {
                return hasElementAt(definedSteps, lastStep.getIndex() + 1);
            } else {
                return true;
            }
        }
    }

    public WorkflowExecutionStep nextStepToExecute(List<WorkflowStepData> definedSteps) {
        if (!isStarted()) {
            throw new IllegalStateException("Cannot execute workflow - not started");
        }
        if (isFinished()) {
            throw new IllegalStateException("Cannot execute workflow - already finished");
        }

        if (steps.isEmpty()) {
            return newStepExecution(definedSteps, 0);
        }

        WorkflowExecutionStep lastStep = steps.get(steps.size() - 1);
        if (lastStep.isFinished()) {
            return newStepExecution(definedSteps, lastStep.getIndex() + 1);
        } else {
            return lastStep;
        }
    }

    public boolean isFinished() {
        return finishedAt != null;
    }

    public void finish(Clock clock) {
        if (!isStarted()) {
            throw new IllegalStateException("Cannot finish workflow - not started");
        }
        if (isFinished()) {
            throw new IllegalStateException("Cannot finish workflow - already finished");
        }
        this.finishedAt = requireNonNull(clock, "A clock is required").instant();
    }

    public boolean hasMoreStepsToRollback() {
        if (isEmpty(steps)) {
            return false;
        } else {
            return reverseStepsStream()
                    .anyMatch(WorkflowExecutionStep::isRollBackNeeded);
        }
    }

    public WorkflowExecutionStep nextStepToRollback() {
        if (!isFinished()) {
            throw new IllegalStateException("Cannot rollback workflow - not finished");
        }
        return reverseStepsStream()
                .filter(WorkflowExecutionStep::isRollBackNeeded)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nothing to rollback"));
    }

    // -------------------- PRIVATE --------------------

    WorkflowExecutionStep newStepExecution(List<WorkflowStepData> definedSteps, int index) {
        if (!hasElementAt(definedSteps, index)) {
            throw new IllegalArgumentException("No step definition for index " + index);
        }

        WorkflowStepData data = definedSteps.get(index);
        WorkflowExecutionStep step = new WorkflowExecutionStep(this, index, data);
        steps.add(step);
        return step;
    }

    private Stream<WorkflowExecutionStep> reverseStepsStream() {
        return IntStream.iterate(steps.size() - 1, i -> --i)
                .limit(steps.size())
                .mapToObj(steps::get);
    }

    private static boolean hasElementAt(Collection<?> collection, int index) {
        return collection != null && collection.size() > index;
    }
}
