package edu.harvard.iq.dataverse.datasetversionsummaries;

public class DatasetVersionSummaryContentDeaccessioned extends DatasetVersionSummaryContent {

    private final String deaccessionNote;
    private final String deaccessionLink;

    public DatasetVersionSummaryContentDeaccessioned(String deaccessionNote, String deaccessionLink) {
        this.deaccessionNote = deaccessionNote;
        this.deaccessionLink = deaccessionLink;
    }

    public String getDeaccessionNote() {
        return deaccessionNote;
    }

    public String getDeaccessionLink() {
        return deaccessionLink;
    }
}
