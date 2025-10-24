package edu.harvard.iq.dataverse.datasetversionsummaries;

import edu.harvard.iq.dataverse.DatasetVersionDifference;

public class DatasetVersionSummaryContentDifferences extends DatasetVersionSummaryContent {

    private final DatasetVersionDifference datasetVersionDifference;

    public DatasetVersionSummaryContentDifferences(DatasetVersionDifference datasetVersionDifference) {
        this.datasetVersionDifference = datasetVersionDifference;
    }

    public DatasetVersionDifference getDatasetVersionDifference() {
        return datasetVersionDifference;
    }
}
