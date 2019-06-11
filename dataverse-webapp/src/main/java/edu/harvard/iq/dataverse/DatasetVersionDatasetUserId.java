package edu.harvard.iq.dataverse;
import java.io.Serializable;

/**
 * Id Class for {@link DatasetVersionUser}, representing a composite key.
 * @author skraffmiller
 */

public class DatasetVersionDatasetUserId implements Serializable {

    private String userIdentifier;    
    private long datasetversionid;

    @Override
    public int hashCode() {
        return (int) (userIdentifier.hashCode() ^ datasetversionid);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DatasetVersionDatasetUserId)) {
            return false;
        }
        DatasetVersionDatasetUserId other = (DatasetVersionDatasetUserId) object;
        return (this.userIdentifier.equals(other.userIdentifier) && this.datasetversionid == other.datasetversionid ); 
    }

    @Override
    public String toString() {
        return "DatasetVersionDatasetUserId{" + "userIdentifier=" + userIdentifier + ", datasetversionid=" + datasetversionid + '}';
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public long getDatasetversionid() {
        return datasetversionid;
    }

    public void setDatasetversionid(long datasetversionid) {
        this.datasetversionid = datasetversionid;
    }
    
}
