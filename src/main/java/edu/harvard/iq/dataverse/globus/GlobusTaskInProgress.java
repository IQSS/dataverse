/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse.globus;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author landreev
 */
@Entity
public class GlobusTaskInProgress implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Globus-side identifier of the task in progress, upload or download
     */
    @Column(nullable = false)
    private String taskId;

    GlobusTaskInProgress(String taskIdentifier, TaskType taskType, Dataset dataset, String clientToken, ApiToken token, Timestamp timestamp) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
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
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    /**
     * Globus API token that should be used to monitor the status of the task
     */
    @Column(nullable = false)
    private String globusToken;
    
    /**
     * This is the Dataverse API token of the user who initiated the Globus task
     */
    private String apiToken;
    
    @ManyToOne
    private Dataset dataset;
    
    @Column( nullable = false )
    private Timestamp startTime;
    
    
    public GlobusTaskInProgress(String taskId, TaskType taskType, Dataset dataset, String clientToken, String apiToken, Timestamp startTime) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.globusToken = clientToken;
        this.apiToken = apiToken; 
        this.dataset = dataset;
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
    
    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
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
