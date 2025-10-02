package edu.harvard.iq.dataverse.datasetversionsummaries;

public class DatasetVersionSummaryContentSimple extends DatasetVersionSummaryContent {

    private final Content value;

    public DatasetVersionSummaryContentSimple(Content value) {
        this.value = value;
    }

    public Content getValue() {
        return value;
    }

    public enum Content {
        firstPublished,
        previousVersionDeaccessioned,
        firstDraft
    }
}
