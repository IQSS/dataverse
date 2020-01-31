package edu.harvard.iq.dataverse.search.response;

import edu.harvard.iq.dataverse.search.query.SearchObjectType;

import java.util.Objects;

/**
 * Model class that contains number of dvObjects based on type in search response.
 * (that is how many Dataverses, Datasets, ...)
 * 
 * @author madryk
 */
public class DvObjectCounts {

    private long dataversesCount;
    private long datasetsCount;
    private long datafilesCount;

    // -------------------- CONSTRUCTORS --------------------

    public DvObjectCounts(long dataversesCount, long datasetsCount, long datafilesCount) {
        super();
        this.dataversesCount = dataversesCount;
        this.datasetsCount = datasetsCount;
        this.datafilesCount = datafilesCount;
    }

    // -------------------- GETTERS --------------------

    public long getDataversesCount() {
        return dataversesCount;
    }
    public long getDatasetsCount() {
        return datasetsCount;
    }
    public long getDatafilesCount() {
        return datafilesCount;
    }

    // -------------------- LOGIC --------------------

    /**
     * Modifies count number for dvObject associated with
     * the given type
     */
    public void setCountByObjectType(SearchObjectType type, long count) {
        switch (type) {
        case DATAVERSES:
            dataversesCount = count;
            break;
        case DATASETS:
            datasetsCount = count;
            break;
        case FILES:
            datafilesCount = count;
            break;
        default:
            break;
        }
    }
    
    /**
     * Returns {@link DvObjectCounts} with all counts set
     * to zero.
     */
    public static DvObjectCounts emptyDvObjectCounts() {
        return new DvObjectCounts(0, 0, 0);
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
        return Objects.hash(datafilesCount, datasetsCount, dataversesCount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DvObjectCounts other = (DvObjectCounts) obj;
        return datafilesCount == other.datafilesCount && datasetsCount == other.datasetsCount
                && dataversesCount == other.dataversesCount;
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "DvObjectCounts [dataversesCount=" + dataversesCount + ", datasetsCount=" + datasetsCount
                + ", datafilesCount=" + datafilesCount + "]";
    }

}
