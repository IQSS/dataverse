/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

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

    public enum RunResultType { SUCCESS, RUN_FAILED, RUN_IN_PROGRESS, DELETE_FAILED };
    
    private static String RESULT_LABEL_SUCCESS = "SUCCESS";
    private static String RESULT_LABEL_RUN_FAILED = "RUN FAILED";
    private static String RESULT_LABEL_RUN_IN_PROGRESS = "RUN IN PROGRESS";
    private static String RESULT_LABEL_DELETE_IN_PROGRESS = "DELETE IN PROGRESS";
    private static String RESULT_LABEL_DELETE_FAILED = "DELETE FAILED";
    
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
            return RESULT_LABEL_DELETE_IN_PROGRESS;
        } else if (isDeleteFailed()) {
            return RESULT_LABEL_DELETE_FAILED;
        } else if (isSuccess()) {
            return RESULT_LABEL_SUCCESS;
        } else if (isRunFailed()) {
            return RESULT_LABEL_RUN_FAILED;
        } else if (isRunInProgress()) {
            return RESULT_LABEL_RUN_IN_PROGRESS;
        }
        return null;
    }
    
    public String getDetailedResultLabel() {
        if (harvestingClient != null && harvestingClient.isDeleteInProgress()) {
            return RESULT_LABEL_DELETE_IN_PROGRESS;
        } else if (isDeleteFailed()) {
            return RESULT_LABEL_DELETE_FAILED;
        } else if (isSuccess()) {
            String resultLabel = RESULT_LABEL_SUCCESS;
            resultLabel = resultLabel.concat("; "+harvestedDatasetCount+" harvested, ");
            resultLabel = resultLabel.concat(deletedDatasetCount+" deleted, ");
            resultLabel = resultLabel.concat(failedDatasetCount+" failed.");
            return resultLabel;
        } else if (isRunFailed()) {
            return RESULT_LABEL_RUN_FAILED;
        } else if (isRunInProgress()) {
            return RESULT_LABEL_RUN_IN_PROGRESS;
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

    public boolean isRunFailed() {
        return RunResultType.RUN_FAILED == harvestResult;
    }

    public void setRunFailed() {
        harvestResult = RunResultType.RUN_FAILED;
    }
    
    public boolean isRunInProgress() {
        return RunResultType.RUN_IN_PROGRESS == harvestResult ||
                (harvestResult == null && startTime != null && finishTime == null);
    }
    
    public void setRunInProgress() {
        harvestResult = RunResultType.RUN_IN_PROGRESS;
    }

    public boolean isDeleteFailed() {
        return RunResultType.DELETE_FAILED == harvestResult;
    }

    public void setDeleteFailed() {
        harvestResult = RunResultType.DELETE_FAILED;
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
