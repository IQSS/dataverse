package edu.harvard.iq.dataverse.featured;

/**
 * Data class holding a given dataverse with the calculated number of datasets in it (and nested dataverses).
 */
public class DataverseDatasetCount {
    private final Long dataverseId;
    private final Long datasetCount;

    // -------------------- CONSTRUCTORS --------------------

    public DataverseDatasetCount(Long dataverseId, Long datasetCount) {
        this.dataverseId = dataverseId;
        this.datasetCount = datasetCount;
    }

    // -------------------- GETTERS --------------------

    public Long getDatasetCount() {
        return datasetCount;
    }

    public Long getDataverseId() {
        return dataverseId;
    }
}
