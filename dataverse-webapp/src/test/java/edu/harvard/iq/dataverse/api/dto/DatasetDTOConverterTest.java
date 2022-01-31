package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetDTOConverterTest {

    @Test
    void convert() {
        // given
        Dataset dataset = new Dataset();
        Dataverse dataverse = new Dataverse();
        dataverse.setName("dataverse-name");
        dataset.setOwner(dataverse);
        dataset.setGlobalId(new GlobalId("doi", "123", "4567"));
        dataset.setId(1L);
        dataset.setIdentifier("identifier");
        dataset.setPublicationDate(new Timestamp(0L));
        dataset.setStorageIdentifier("storage-id");

        // when
        DatasetDTO converted = new DatasetDTO.Converter().convert(dataset);

        // then
        assertThat(converted)
                .extracting(DatasetDTO::getId, DatasetDTO::getIdentifier, DatasetDTO::getPersistentUrl,
                        DatasetDTO::getProtocol, DatasetDTO::getAuthority, DatasetDTO::getPublisher,
                        DatasetDTO::getPublicationDate, DatasetDTO::getStorageIdentifier, DatasetDTO::getHasActiveGuestbook,
                        DatasetDTO::getEmbargoDate, DatasetDTO::getEmbargoActive)
                .containsExactly(1L, "identifier", "https://doi.org/123/identifier",
                        "doi", "123", "dataverse-name",
                        "1970-01-01", "storage-id", false,
                        null, false);
    }
}