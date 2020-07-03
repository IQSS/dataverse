package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
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
    private String encoding;

    /**
     * The type of storage for the given artifact.
     */
    @Column(name = "storage_type")
    private String storageType;

    /**
     * Location of artifact within the storage.
     */
    @Column(name = "storage_location")
    private String storageLocation;

    // -------------------- CONSTRUCTORS --------------------

    public WorkflowArtifact() { }

    public WorkflowArtifact(Long id, Long workflowExecutionId, Instant createdAt,
                            String name, String encoding, String storageType, String storageLocation) {
        this.id = id;
        this.workflowExecutionId = workflowExecutionId;
        this.createdAt = createdAt;
        this.name = name;
        this.encoding = encoding;
        this.storageType = storageType;
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

    public String getStorageType() {
        return storageType;
    }

    public String getStorageLocation() {
        return storageLocation;
    }
}
