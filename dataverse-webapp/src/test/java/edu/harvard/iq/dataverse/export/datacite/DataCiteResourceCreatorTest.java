package edu.harvard.iq.dataverse.export.datacite;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is only minimal test for the class, as the more comprehensive tests
 * are located in DOIDataCiteRegisterServiceTest.
 */
class DataCiteResourceCreatorTest {

    @Test
    void create() {
        // given
        Dataset dataset = new Dataset();
        DatasetVersion version = new DatasetVersion();
        Dataverse dataverse = new Dataverse();
        version.setDataset(dataset);
        dataset.setOwner(dataverse);
        dataset.setVersions(Collections.singletonList(version));

        // when
        DataCiteResource dataCiteResource = new DataCiteResourceCreator()
                .create("doi:10.5072/FK2/ZJLYL1", null, dataset);

        // then
        assertThat(dataCiteResource)
                .extracting(r -> r.getIdentifier().getValue(), DataCiteResource::getPublisher, DataCiteResource::getPublicationYear)
                .containsExactly("10.5072/FK2/ZJLYL1", ":unav", "9999");
        assertThat(dataCiteResource)
                .extracting(r -> r.getCreators().size(),
                        r -> r.getContributors().size(),
                        r -> r.getRelatedIdentifiers().size(),
                        r -> r.getFundingReferences().size())
                .containsExactly(0, 0, 0, 0);
    }
}