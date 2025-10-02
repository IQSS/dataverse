package edu.harvard.iq.dataverse.datasetversionsummaries;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.DatasetVersionDifference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link DatasetVersionSummary} record and its factory methods.
 */
@ExtendWith(MockitoExtension.class)
class DatasetVersionSummaryTest {

    @Mock
    private DatasetVersion versionMock;

    @Mock
    private DatasetVersionDifference differenceMock;

    @Test
    @DisplayName("from() should return an empty Optional when the input DatasetVersion is null")
    void from_whenVersionIsNull_shouldReturnEmptyOptional() {
        // Act
        Optional<DatasetVersionSummary> result = DatasetVersionSummary.from(null);

        // Assert
        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("from() should map all basic properties from the DatasetVersion correctly")
    void from_whenVersionIsNotNull_shouldMapPropertiesCorrectly() {
        // Arrange
        when(versionMock.getId()).thenReturn(42L);
        when(versionMock.getFriendlyVersionNumber()).thenReturn("V1.0");
        when(versionMock.getVersionNote()).thenReturn("Initial version note.");
        when(versionMock.getContributorNames()).thenReturn("Contributor One, Contributor Two");
        when(versionMock.getPublicationDateAsString()).thenReturn("2025-10-02");
        when(versionMock.isReleased()).thenReturn(true);
        when(versionMock.getPriorVersionState()).thenReturn(null);

        // Act
        Optional<DatasetVersionSummary> summaryOpt = DatasetVersionSummary.from(versionMock);

        // Assert
        assertThat(summaryOpt).isPresent();
        DatasetVersionSummary summary = summaryOpt.get();

        assertThat(summary.id()).isEqualTo(42L);
        assertThat(summary.versionNumber()).isEqualTo("V1.0");
        assertThat(summary.versionNote()).isEqualTo("Initial version note.");
        assertThat(summary.contributorNames()).isEqualTo("Contributor One, Contributor Two");
        assertThat(summary.publicationDate()).isEqualTo("2025-10-02");
    }

    @Test
    @DisplayName("Content should be 'Differences' if a version difference is present (highest priority)")
    void from_whenVersionHasDifferences_shouldCreateDifferencesContent() {
        // Arrange
        when(versionMock.getDefaultVersionDifference()).thenReturn(differenceMock);

        // Act
        Optional<DatasetVersionSummary> summaryOpt = DatasetVersionSummary.from(versionMock);

        // Assert
        assertThat(summaryOpt).isPresent();
        assertThat(summaryOpt.get().content()).isInstanceOf(DatasetVersionSummaryContentDifferences.class);
        DatasetVersionSummaryContentDifferences content = (DatasetVersionSummaryContentDifferences) summaryOpt.get().content();
        assertThat(content.getDatasetVersionDifference()).isSameAs(differenceMock);
    }

    @Test
    @DisplayName("Content should be 'Deaccessioned' if the version is deaccessioned")
    void from_whenVersionIsDeaccessioned_shouldCreateDeaccessionedContent() {
        // Arrange
        when(versionMock.getDefaultVersionDifference()).thenReturn(null); // No differences
        when(versionMock.isDeaccessioned()).thenReturn(true);
        when(versionMock.getDeaccessionNote()).thenReturn("Reason for deaccession.");
        when(versionMock.getDeaccessionLink()).thenReturn("http://example.com/deaccession");

        // Act
        Optional<DatasetVersionSummary> summaryOpt = DatasetVersionSummary.from(versionMock);

        // Assert
        assertThat(summaryOpt).isPresent();
        DatasetVersionSummaryContentDeaccessioned content = (DatasetVersionSummaryContentDeaccessioned) summaryOpt.get().content();

        assertThat(content.getDeaccessionNote()).isEqualTo("Reason for deaccession.");
        assertThat(content.getDeaccessionLink()).isEqualTo("http://example.com/deaccession");
    }

    @Test
    @DisplayName("Content should be 'firstPublished' for the first released version")
    void from_whenIsFirstPublishedVersion_shouldCreateFirstPublishedContent() {
        // Arrange
        when(versionMock.getDefaultVersionDifference()).thenReturn(null);
        when(versionMock.isDeaccessioned()).thenReturn(false);
        when(versionMock.isReleased()).thenReturn(true);
        when(versionMock.getPriorVersionState()).thenReturn(null); // This makes it the "first" version

        // Act
        Optional<DatasetVersionSummary> summaryOpt = DatasetVersionSummary.from(versionMock);

        // Assert
        assertThat(summaryOpt).isPresent();
        assertThat(summaryOpt.get().content())
                .isInstanceOf(DatasetVersionSummaryContentSimple.class)
                .extracting("value")
                .isEqualTo(DatasetVersionSummaryContentSimple.Content.firstPublished);
    }

    @Test
    @DisplayName("Content should be 'firstDraft' for the first draft version")
    void from_whenIsFirstDraftVersion_shouldCreateFirstDraftContent() {
        // Arrange
        when(versionMock.getDefaultVersionDifference()).thenReturn(null);
        when(versionMock.isDeaccessioned()).thenReturn(false);
        when(versionMock.isDraft()).thenReturn(true);
        when(versionMock.getPriorVersionState()).thenReturn(null); // This makes it the "first" version

        // Act
        Optional<DatasetVersionSummary> summaryOpt = DatasetVersionSummary.from(versionMock);

        // Assert
        assertThat(summaryOpt).isPresent();
        assertThat(summaryOpt.get().content())
                .isInstanceOf(DatasetVersionSummaryContentSimple.class)
                .extracting("value")
                .isEqualTo(DatasetVersionSummaryContentSimple.Content.firstDraft);
    }

    @Test
    @DisplayName("Content should be 'previousVersionDeaccessioned' if the prior version was deaccessioned")
    void from_whenPriorVersionWasDeaccessioned_shouldCreateAppropriateContent() {
        // Arrange
        when(versionMock.getDefaultVersionDifference()).thenReturn(null);
        when(versionMock.isDeaccessioned()).thenReturn(false);
        when(versionMock.isReleased()).thenReturn(true);
        when(versionMock.getPriorVersionState()).thenReturn(VersionState.DEACCESSIONED);

        // Act
        Optional<DatasetVersionSummary> summaryOpt = DatasetVersionSummary.from(versionMock);

        // Assert
        assertThat(summaryOpt).isPresent();
        assertThat(summaryOpt.get().content())
                .isInstanceOf(DatasetVersionSummaryContentSimple.class)
                .extracting("value")
                .isEqualTo(DatasetVersionSummaryContentSimple.Content.previousVersionDeaccessioned);
    }

    @Test
    @DisplayName("Content should be null for a standard released version that is not the first")
    void from_whenIsStandardUpdate_shouldHaveNullContent() {
        // Arrange
        when(versionMock.getDefaultVersionDifference()).thenReturn(null);
        when(versionMock.isDeaccessioned()).thenReturn(false);
        when(versionMock.isReleased()).thenReturn(true);
        when(versionMock.getPriorVersionState()).thenReturn(VersionState.RELEASED); // Prior version exists

        // Act
        Optional<DatasetVersionSummary> summaryOpt = DatasetVersionSummary.from(versionMock);

        // Assert
        assertThat(summaryOpt).isPresent();
        assertThat(summaryOpt.get().content()).isNull();
    }

    @Test
    @DisplayName("Content should be null for an unhandled version state like ARCHIVED")
    void from_whenStateIsUnhandled_shouldHaveNullContent() {
        // Arrange
        when(versionMock.getDefaultVersionDifference()).thenReturn(null);
        when(versionMock.isDeaccessioned()).thenReturn(false);
        when(versionMock.isReleased()).thenReturn(false);
        when(versionMock.isDraft()).thenReturn(false);
        // Assuming VersionState is ARCHIVED, isArchived() would be true but the logic doesn't check for it.

        // Act
        Optional<DatasetVersionSummary> summaryOpt = DatasetVersionSummary.from(versionMock);

        // Assert
        assertThat(summaryOpt).isPresent();
        assertThat(summaryOpt.get().content()).isNull();
    }
}