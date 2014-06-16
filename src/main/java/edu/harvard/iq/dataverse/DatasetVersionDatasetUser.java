/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Entity;

import javax.persistence.Id;
import javax.persistence.IdClass;

import javax.persistence.JoinColumn;

import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 *
 * @author skraffmiller
 */
@Entity
@Table(name="DatasetVersion_DataverseUser")
@IdClass(DatasetVersionDatasetUserId.class)
public class DatasetVersionDatasetUser implements Serializable{

    @Id
    private long dataverseuserid;
    
    @Id
    private long datasetversionid;

    public long getDataverseuserid() {
        return dataverseuserid;
    }

    public void setDataverseuserid(long dataverseuserid) {
        this.dataverseuserid = dataverseuserid;
    }

    public long getDatasetversionid() {
        return datasetversionid;
    }

    public void setDatasetversionid(long datasetversionid) {
        this.datasetversionid = datasetversionid;
    }
    
    @ManyToOne
    @JoinColumn(name = "dataverseuser_id")
    private DataverseUser dataverseUser;
    
    public DataverseUser getDataverseUser() {
        return dataverseUser;
    }

    public void setDataverseUser(DataverseUser dataverseUser) {
        this.dataverseUser = dataverseUser;
    }
    
    @ManyToOne
    @JoinColumn(name = "datasetversion_id")
    private DatasetVersion datasetVersion;

    public Timestamp getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Timestamp lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }
    
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    private Timestamp lastUpdateDate;

   
}
