package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;

import javax.persistence.Id;
import javax.persistence.IdClass;

import javax.persistence.JoinColumn;

import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Records the last time a {@link User} handled a {@link DatasetVersion}.
 * @author skraffmiller
 */
@Entity
@Table(name="DatasetVersion_DataverseUser")
@IdClass(DatasetVersionDatasetUserId.class)
public class DatasetVersionUser implements Serializable {

    @Id
    private String userIdentifier;
    
    @Id
    private long datasetversionid;

    @ManyToOne
    @JoinColumn(name = "datasetversion_id")
    private DatasetVersion datasetVersion;

    @Column( nullable=false )
    private Timestamp lastUpdateDate;

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String dataverseuserid) {
        this.userIdentifier = dataverseuserid;
    }

    public long getDatasetversionid() {
        return datasetversionid;
    }

    public void setDatasetversionid(long datasetversionid) {
        this.datasetversionid = datasetversionid;
    }

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

   
}
