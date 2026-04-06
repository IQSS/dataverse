package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "curationstatus", indexes = {
        @Index(name = "index_curationstatus_datasetversion", columnList = "datasetversion_id")
    })
public class CurationStatus implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String label;

    @ManyToOne
    @JoinColumn(name = "datasetversion_id", nullable = false)
    private DatasetVersion datasetVersion;

    @ManyToOne
    @JoinColumn(name = "authenticateduser_id", nullable = true)
    private AuthenticatedUser authenticatedUser;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date createTime;

    // Constructors, getters, and setters

    public CurationStatus() {
    }

    public CurationStatus(String label, DatasetVersion datasetVersion, AuthenticatedUser authenticatedUser) {
        this.label = label;
        this.datasetVersion = datasetVersion;
        this.authenticatedUser = authenticatedUser;
        this.createTime = new Date();
    }

    // Getters and setters for all fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public boolean isNoStatus() {
        return label == null || label.trim().isEmpty();
    }
}