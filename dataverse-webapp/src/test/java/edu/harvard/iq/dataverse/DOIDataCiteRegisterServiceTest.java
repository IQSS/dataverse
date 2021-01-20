package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DOIDataCiteRegisterServiceTest {

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should create DataCite xml file")
    void getMetadataFromDvObject() {

        // given & when
        String xml = DOIDataCiteRegisterService.getMetadataFromDvObject(
                "doi:test", Collections.emptyMap(), createDataset());

        // then
        assertThat(xml).isNotBlank();
    }

    @Test
    @DisplayName("Should remove non-breaking spaces from description")
    void getMetadataFromDvObject__removeNonBreakingSpaces() {

        // given
        Dataset dataset = createDataset();
        DatasetVersion version = mock(DatasetVersion.class);
        dataset.setVersions(Collections.singletonList(version));
        when(version.getDescriptionPlainText()).thenReturn("&nbsp;Description&nbsp;&nbsp;&nbsp;1&nbsp;");
        when(version.getRootDataverseNameforCitation()).thenReturn("");
        when(version.getTitle()).thenReturn("");

        // when
        String xml = DOIDataCiteRegisterService.getMetadataFromDvObject(
                "doi:test", Collections.emptyMap(), dataset);

        // then
        assertThat(xml).contains(
                "<description descriptionType=\"Abstract\">\u00A0Description\u00A0\u00A0\u00A01\u00A0</description>");
    }

    // -------------------- PRIVATE --------------------

    private Dataset createDataset() {
        Dataverse dataverse = new Dataverse();
        Dataset dataset = new Dataset();
        dataset.setOwner(dataverse);
        return dataset;
    }
}