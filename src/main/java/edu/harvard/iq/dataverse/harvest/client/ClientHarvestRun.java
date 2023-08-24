/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client;

import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 *
 * @author Leonid Andreev
 *
 * This is a record of an attempted harvesting client run. (Should it be named
 * HarvestingClientRunResult instead?)
 */
@Entity
public class ClientHarvestRun implements Serializable {

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

    public enum RunResultType { SUCCESS, FAILURE, INPROGRESS, INTERRUPTED };
    
    private static String RESULT_LABEL_SUCCESS = "SUCCESS";
    private static String RESULT_LABEL_FAILURE = "FAILED";
    private static String RESULT_LABEL_INPROGRESS = "IN PROGRESS";
    private static String RESULT_DELETE_IN_PROGRESS = "DELETE IN PROGRESS";
    private static String RESULT_LABEL_INTERRUPTED = "INTERRUPTED";
    
    @ManyToOne
    @JoinColumn(nullable = false)
    private HarvestingClient harvestingClient;

    public HarvestingClient getHarvestingClient() {
        return harvestingClient;
    }

    public void setHarvestingClient(HarvestingClient harvestingClient) {
        this.harvestingClient = harvestingClient;
    }

    private RunResultType harvestResult; 

    public RunResultType getResult() {
        return harvestResult;
    }
    
    public String getResultLabel() {
        if (harvestingClient != null && harvestingClient.isDeleteInProgress()) {
            return RESULT_DELETE_IN_PROGRESS;
        }
        
        if (isSuccess()) {
            return RESULT_LABEL_SUCCESS;
        } else if (isFailed()) {
            return RESULT_LABEL_FAILURE;
        } else if (isInProgress()) {
            return RESULT_LABEL_INPROGRESS;
        } else if (isInterrupted()) {
            return RESULT_LABEL_INTERRUPTED;
        }
        return null;
    }
    
    public String getDetailedResultLabel() {
        if (harvestingClient != null && harvestingClient.isDeleteInProgress()) {
            return RESULT_DELETE_IN_PROGRESS;
        }
        if (isSuccess() || isInterrupted()) {
            String resultLabel = getResultLabel();
            
            resultLabel = resultLabel.concat("; "+harvestedDatasetCount+" harvested, ");
            resultLabel = resultLabel.concat(deletedDatasetCount+" deleted, ");
            resultLabel = resultLabel.concat(failedDatasetCount+" failed.");
            return resultLabel;
        } else if (isFailed()) {
            return RESULT_LABEL_FAILURE;
        } else if (isInProgress()) {
            return RESULT_LABEL_INPROGRESS;
        }
        return null;
    }

    public void setResult(RunResultType harvestResult) {
        this.harvestResult = harvestResult;
    }

    public boolean isSuccess() {
        return RunResultType.SUCCESS == harvestResult;
    }

    public void setSuccess() {
        harvestResult = RunResultType.SUCCESS;
    }

    public boolean isFailed() {
        return RunResultType.FAILURE == harvestResult;
    }

    public void setFailed() {
        harvestResult = RunResultType.FAILURE;
    }
    
    public boolean isInProgress() {
        return RunResultType.INPROGRESS == harvestResult ||
                (harvestResult == null && startTime != null && finishTime == null);
    }
    
    public void setInProgress() {
        harvestResult = RunResultType.INPROGRESS;
    }

    public boolean isInterrupted() {
        return RunResultType.INTERRUPTED == harvestResult;
    }
    
    public void setInterrupted() {
        harvestResult = RunResultType.INTERRUPTED;
    }
    
    // Time of this harvest attempt:
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date startTime;

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Temporal(value = TemporalType.TIMESTAMP)
    private Date finishTime;

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    // Tese are the Dataset counts from that last harvest:
    // (TODO: do we need to differentiate between *created* (new), and *updated* 
    // harvested datasets? -- L.A. 4.4
    private Long harvestedDatasetCount = 0L;
    private Long failedDatasetCount = 0L;
    private Long deletedDatasetCount = 0L;

    public Long getHarvestedDatasetCount() {
        return harvestedDatasetCount;
    }

    public void setHarvestedDatasetCount(Long harvestedDatasetCount) {
        this.harvestedDatasetCount = harvestedDatasetCount;
    }

    public Long getFailedDatasetCount() {
        return failedDatasetCount;
    }

    public void setFailedDatasetCount(Long failedDatasetCount) {
        this.failedDatasetCount = failedDatasetCount;
    }

    public Long getDeletedDatasetCount() {
        return deletedDatasetCount;
    }

    public void setDeletedDatasetCount(Long deletedDatasetCount) {
        this.deletedDatasetCount = deletedDatasetCount;
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
        if (!(object instanceof ClientHarvestRun)) {
            return false;
        }
        ClientHarvestRun other = (ClientHarvestRun) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.harvest.client.HarvestingClientRun[ id=" + id + " ]";
    }

}
