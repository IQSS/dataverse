package edu.harvard.iq.dataverse.citation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
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
        assertThat(citationData.getUNF()).isNull();
        assertThat(citationData.getVersion()).isEqualTo("V1");
        assertThat(citationData.getYear()).isEqualTo("1955");
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
}