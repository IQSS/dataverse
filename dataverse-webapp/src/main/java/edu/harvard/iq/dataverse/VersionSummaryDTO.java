package edu.harvard.iq.dataverse;

/**
 * Model class with dataset version summary.
 * It is used to present list of dataset versions
 * in form of a table.
 * 
 * @author madryk
 */
public class VersionSummaryDTO {
    
    private DatasetVersion version;
    private String contributorNames;
    private boolean showLink;
    private boolean canBeCompared;
    private DatasetVersionDifference differenceFromPreviousVersion;
    
    // -------------------- CONSTRUCTORS --------------------
    
    public VersionSummaryDTO(DatasetVersion version, String contributorNames, boolean showLink, boolean canBeCompared,
            DatasetVersionDifference differenceFromPreviousVersion) {
        this.version = version;
        this.contributorNames = contributorNames;
        this.showLink = showLink;
        this.canBeCompared = canBeCompared;
        this.differenceFromPreviousVersion = differenceFromPreviousVersion;
    }

    // -------------------- GETTERS --------------------
    
    public DatasetVersion getVersion() {
        return version;
    }

    public String getContributorNames() {
        return contributorNames;
    }

    public boolean isShowLink() {
        return showLink;
    }

    public boolean isCanBeCompared() {
        return canBeCompared;
    }

    public DatasetVersionDifference getDifferenceFromPreviousVersion() {
        return differenceFromPreviousVersion;
    }
}
