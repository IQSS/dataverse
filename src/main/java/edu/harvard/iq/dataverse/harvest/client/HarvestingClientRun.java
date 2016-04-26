/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Leonid Andreev
 * 
 * This is a record of an attempted harvesting client run. 
 * (Should it be named HarvestingClientRunResult instead?)
 */
@Entity
public class HarvestingClientRun implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private String harvestResult; // TODO: should this me an enum instead? -- L.A. 4.4
    
    public String getHarvestResult() {
        return harvestResult;
    }

    public void setHarvestResult(String harvestResult) {
        this.harvestResult = harvestResult;
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
    
    private Long harvestedDatasetCount;
    private Long failedDatasetCount;
    private Long deletedDatasetCount;
    
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
        if (!(object instanceof HarvestingClientRun)) {
            return false;
        }
        HarvestingClientRun other = (HarvestingClientRun) object;
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
