package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Clock;
import java.time.Instant;

@Entity
@Table(name = "workflowartifact")
public class WorkflowArtifact implements JpaEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_execution_id")
    private Long workflowExecutionId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "name")
    private String name;

    /**
     * Allows to store encoding data in case of text artifacts.
     * The string should contain encoding's name that could be recognized by
     * {@link java.nio.charset.Charset}#forName method.
     */
    @Column(name = "encoding")
    private String encoding;

    /**
     * Location of artifact within the storage.
     */
    @Column(name = "storage_location")
    private String storageLocation;

    // -------------------- CONSTRUCTORS --------------------

    public WorkflowArtifact() { }

    public WorkflowArtifact(Long workflowExecutionId, WorkflowArtifactSource source, String storageLocation, Clock clock) {
        this(workflowExecutionId, source.getName(), source.getEncoding(), storageLocation, clock);
    }

    public WorkflowArtifact(Long workflowExecutionId, String name, String encoding,
                            String storageLocation, Clock clock) {
        this.workflowExecutionId = workflowExecutionId;
        this.createdAt = clock.instant();
        this.name = name;
        this.encoding = encoding;
        this.storageLocation = storageLocation;
    }

    // -------------------- GETTERS --------------------

    @Override
    public Long getId() {
        return id;
    }

    public Long getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getName() {
        return name;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getStorageLocation() {
        return storageLocation;
    }
}
