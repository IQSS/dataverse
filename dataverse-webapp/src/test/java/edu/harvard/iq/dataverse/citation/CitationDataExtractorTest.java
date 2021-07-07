package edu.harvard.iq.dataverse.citation;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Year;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CitationDataExtractorTest {

    private CitationDataExtractor dataExtractor = new CitationDataExtractor();

    private CitationTestUtils utils = new CitationTestUtils();

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should extract data for citation")
    void create() throws ParseException {

        // given & when
        CitationData citationData = dataExtractor.create(
                utils.createATestDatasetVersion("Dataset Title", true));

        // then
        assertThat(citationData.getAuthorsString()).isEqualTo("First Last");
        assertThat(citationData.getFileTitle()).isNull();
        assertThat(citationData.getPersistentId().asString()).isEqualTo("doi:10.5072/FK2/LK0D1H");
        assertThat(citationData.getPublisher()).isEqualTo("LibraScholar");
        assertThat(citationData.getTitle()).isEqualTo("Dataset Title");
        assertThat(citationData.getVersion()).isEqualTo("V1");
        assertThat(citationData.getYear()).isEqualTo("1955");
    }

    @Test
    @DisplayName("Should extract data year for citation from harvested dataset, year from distribution date")
    void create_forHarvested_yearFromDistributionDate() throws ParseException {

        // given & when
        DatasetVersion datasetVersion = utils.createHarvestedTestDatasetVersionWithDistributionDate("Dataset Title", true);
        CitationData citationData = dataExtractor.create(datasetVersion);


        // then
        assertThat(citationData.getAuthorsString()).isEqualTo("First Last");
        assertThat(citationData.getFileTitle()).isNull();
        assertThat(citationData.getPersistentId().asString()).isEqualTo("doi:10.5072/FK2/LK0D1H");
        assertThat(citationData.getPublisher()).isNull();
        assertThat(citationData.getTitle()).isEqualTo("Dataset Title");
        assertThat(citationData.getVersion()).isNull();
        assertThat(citationData.getYear()).isEqualTo("2020");
    }

    @Test
    @DisplayName("Should extract data for citation from harvested dataset")
    void create_forHarvested() throws ParseException {

        // given & when
        DatasetVersion datasetVersion = utils.createHarvestedTestDatasetVersion("Dataset Title", true);
        CitationData citationData = dataExtractor.create(datasetVersion);

        // then
        assertThat(citationData.getAuthorsString()).isEqualTo("First Last");
        assertThat(citationData.getFileTitle()).isNull();
        assertThat(citationData.getPersistentId().asString()).isEqualTo("doi:10.5072/FK2/LK0D1H");
        assertThat(citationData.getPublisher()).isNull();
        assertThat(citationData.getTitle()).isEqualTo("Dataset Title");
        assertThat(citationData.getVersion()).isNull();
        assertThat(citationData.getYear()).isEqualTo(String.valueOf(Year.now().getValue()));
    }

    @Test
    @DisplayName("Should create DataCite metadata")
    void getDataCiteMetadata() throws ParseException {

        //given
        CitationData citationData = dataExtractor.create(
                utils.createATestDatasetVersion("Dataset Title", true));

        //when
        Map<String, String> properties = citationData.getDataCiteMetadata();

        //then
        assertThat(properties).hasSize(4);
        assertThat(properties)
                .extractingByKeys("datacite.creator", "datacite.publisher", "datacite.title", "datacite.publicationyear")
                .containsExactly("First Last", "LibraScholar", "Dataset Title", "1955");
    }

    @Test
    @DisplayName("Should create DataCite metadata for harvested dataset")
    void getDataCiteMetadata_forHarvested() throws ParseException {

        //given
        DatasetVersion datasetVersion = utils.createHarvestedTestDatasetVersion("Dataset Title", true);
        CitationData citationData = dataExtractor.create(datasetVersion);

        //when
        Map<String, String> properties = citationData.getDataCiteMetadata();

        //then
        assertThat(properties).hasSize(4);
        assertThat(properties)
                .extractingByKeys("datacite.creator", "datacite.publisher", "datacite.title", "datacite.publicationyear")
                .containsExactly("First Last", ":unav", "Dataset Title", Year.now().toString());
    }
}
