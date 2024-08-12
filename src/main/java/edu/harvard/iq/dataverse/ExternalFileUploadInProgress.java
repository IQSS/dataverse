/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse;

import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author landreev
 * 
 * The name of the class is provisional. I'm open to better-sounding alternatives,
 * if anyone can think of any. 
 * But I wanted to avoid having the word "Globus" in the entity name. I'm adding 
 * it specifically for the Globus use case. But I'm guessing there's a chance 
 * this setup may come in handy for other types of datafile uploads that happen
 * externally. (?)
 */
@NamedQueries({
	@NamedQuery( name="ExternalFileUploadInProgress.deleteByTaskId",
		query="DELETE FROM ExternalFileUploadInProgress f WHERE f.taskId=:taskId"),
        @NamedQuery(name = "ExternalFileUploadInProgress.findByTaskId",
                query = "SELECT f FROM ExternalFileUploadInProgress f WHERE f.taskId=:taskId")})
@Entity
public class ExternalFileUploadInProgress implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * Rather than saving various individual fields defining the datafile, 
     * which would essentially replicate the DataFile table, we are simply 
     * storing the full json record as passed to the API here.  
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String fileInfo;
    
    /**
     * This is Globus-specific task id associated with the upload in progress
     */  
    @Column(nullable = false)
    private String taskId; 
    
    /**
     * The Dataset to which the files are being added.
     * (@todo may not be necessary? - since the corresponding task is tied to a specific
     * dataset already?)
     */
    /*@ManyToOne
    private Dataset dataset;*/
    
    /*public ExternalFileUploadInProgress(String taskId, Dataset dataset, String fileInfo) {
        this.taskId = taskId;
        this.fileInfo = fileInfo;
        this.dataset = dataset;
    }*/
    
    public ExternalFileUploadInProgress(String taskId, String fileInfo) {
        this.taskId = taskId;
        this.fileInfo = fileInfo;
    }
    
    public String getFileInfo() {
        return fileInfo;
    }
    
    public void setFileInfo(String fileInfo) {
        this.fileInfo = fileInfo;
    }
    
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    /*public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }*/

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ExternalFileUploadInProgress)) {
            return false;
        }
        ExternalFileUploadInProgress other = (ExternalFileUploadInProgress) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.ExternalFileUploadInProgress[ id=" + id + " ]";
    }
    
}
