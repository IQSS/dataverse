/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;
import java.io.Serializable;

/**
 *
 * @author skraffmiller
 */

public class DatasetVersionDatasetUserId implements Serializable {

    private long dataverseuserid;    
    private long datasetversionid;

    @Override
    public int hashCode() {
        return (int) (dataverseuserid + datasetversionid);
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetVersionDatasetUserId)) {
            return false;
        }
        DatasetVersionDatasetUserId other = (DatasetVersionDatasetUserId) object;
        return (this.dataverseuserid == other.dataverseuserid && this.datasetversionid == other.datasetversionid ); 
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetDataverseUserId[ id=" + (dataverseuserid + datasetversionid) + " ]";
    }

    public long getDataverseUserId() {
        return dataverseuserid;
    }

    public void setDataverseUserId(long dataverseUserId) {
        this.dataverseuserid = dataverseUserId;
    }
    
    public long getDatavsetVersionId() {
        return datasetversionid;
    }

    public void setDatasetVersionId(long datasetversionid) {
        this.datasetversionid = datasetversionid;
    }
    
}
