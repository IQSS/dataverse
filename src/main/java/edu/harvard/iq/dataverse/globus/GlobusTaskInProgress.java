package edu.harvard.iq.dataverse.globus;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 *
 * @author landreev
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "taskid")})
public class GlobusTaskInProgress implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Globus-side identifier of the task in progress, upload or download
     */
    @Column(nullable=false, unique = true)
    private String taskId;

    /**
     * I was considering giving this enum type a more specific name "TransferType"
     * - but maybe there will be another use case where we need to keep track of
     * Globus tasks that are not data transfers (?)
     */
    public enum TaskType {

        UPLOAD("UPLOAD"),
        DOWNLOAD("DOWNLOAD");

        private final String text;

        private TaskType(final String text) {
            this.text = text;
        }

        public static TaskType fromString(String text) {
            if (text != null) {
                for (TaskType taskType : TaskType.values()) {
                    if (text.equals(taskType.text)) {
                        return taskType;
                    }
                }
            }
            throw new IllegalArgumentException("TaskType must be one of these values: " + Arrays.asList(TaskType.values()) + ".");
        }

        @Override
        public String toString() {
            return text;
        }
    }
    
    @Column(nullable=false)
    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    /**
     * Globus API token that should be used to monitor the status of the task
     */
    @Column(nullable=false)
    private String globusToken;
    
    /**
     * This is the the user who initiated the Globus task
     */    
    @ManyToOne
    @JoinColumn
    private AuthenticatedUser user;
    
    // @Column(nullable=false) @todo we will need a flyway script in order to make
    // this field nullable
    private String ruleId;
    
    @JoinColumn(nullable = false)
    @ManyToOne
    private Dataset dataset;
    
    @Column
    private Timestamp startTime;
    
    public GlobusTaskInProgress() {
    }

    GlobusTaskInProgress(String taskId, TaskType taskType, Dataset dataset, String globusToken, AuthenticatedUser authUser, String ruleId, Timestamp startTime) {
        this.taskId = taskId; 
        this.taskType = taskType; 
        this.dataset = dataset;
        this.globusToken = globusToken; 
        this.user = authUser; 
        this.ruleId = ruleId;
        this.startTime = startTime; 
    }
    

        
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public String getGlobusToken() {
        return globusToken;
    }

    public void setGlobusToken(String clientToken) {
        this.globusToken = clientToken;
    }
    
    public AuthenticatedUser getLocalUser() {
        return user;
    }

    public void setLocalUser(AuthenticatedUser authUser) {
        this.user = authUser;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }
    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof GlobusTaskInProgress)) {
            return false;
        }
        GlobusTaskInProgress other = (GlobusTaskInProgress) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.globus.GlobusTaskInProgress[ id=" + id + " ]";
    }
    
}
