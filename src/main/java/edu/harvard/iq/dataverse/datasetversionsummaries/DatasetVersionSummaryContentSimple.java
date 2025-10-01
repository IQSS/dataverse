package edu.harvard.iq.dataverse.datasetversionsummaries;

public class DatasetVersionSummaryContentSimple extends DatasetVersionSummaryContent {

    private Content value;

    public DatasetVersionSummaryContentSimple(Content value) {
        this.value = value;
    }

    public enum Content {
        firstPublished,
        previousVersionDeaccessioned,
        firstDraft
    }
}
