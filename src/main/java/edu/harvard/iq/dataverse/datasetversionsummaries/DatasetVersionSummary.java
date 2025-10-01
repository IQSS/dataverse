package edu.harvard.iq.dataverse.datasetversionsummaries;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.datasetversionsummaries.DatasetVersionSummaryContentSimple.Content;

import java.util.Optional;

/**
 * An immutable data carrier representing a summary of a DatasetVersion.
 */
public record DatasetVersionSummary(
        String versionId,
        String versionNote,
        String contributorNames,
        String publicationDate,
        DatasetVersionSummaryContent content
) {

    /**
     * Creates a DatasetVersionSummary from a DatasetVersion entity.
     * <p>
     * This factory method is the preferred way to create summaries, as it encapsulates
     * all the logic for determining the correct summary content.
     *
     * @param datasetVersion The entity to convert.
     * @return An {@link Optional} containing the summary, or an empty Optional if the input is null.
     */
    public static Optional<DatasetVersionSummary> from(DatasetVersion datasetVersion) {
        return Optional.ofNullable(datasetVersion).map(version -> new DatasetVersionSummary(
                version.getFriendlyVersionNumber(),
                version.getVersionNote(),
                version.getContributorNames(),
                version.getPublicationDateAsString(),
                determineContent(version)
        ));
    }

    /**
     * Determines the appropriate summary content based on the version's state.
     * The logic is prioritized to handle the most specific cases first.
     *
     * @return The determined {@link DatasetVersionSummaryContent}, or {@code null} if the version
     * state does not match any known summary condition.
     */
    private static DatasetVersionSummaryContent determineContent(DatasetVersion version) {
        // Priority 1: The version has explicit differences calculated.
        if (version.getDefaultVersionDifference() != null) {
            // TODO
            return new DatasetVersionSummaryContentDifferences();
        }

        // Priority 2: Deaccessioned status is a critical override.
        if (version.isDeaccessioned()) {
            return new DatasetVersionSummaryContentDeaccessioned(version.getDeaccessionNote(), version.getDeaccessionLink());
        }

        // Priority 3: Standard draft or released versions.
        if (version.isReleased() || version.isDraft()) {
            boolean isFirstVersion = version.getPriorVersionState() == null;
            if (isFirstVersion) {
                return new DatasetVersionSummaryContentSimple(version.isReleased() ? Content.firstPublished : Content.firstDraft);
            }

            if (version.getPriorVersionState() == VersionState.DEACCESSIONED) {
                return new DatasetVersionSummaryContentSimple(Content.previousVersionDeaccessioned);
            }
        }

        return null;
    }
}
