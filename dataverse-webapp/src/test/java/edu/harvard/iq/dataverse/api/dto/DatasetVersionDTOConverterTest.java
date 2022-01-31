package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetVersionDTOConverterTest {

    @Test
    void convert() {
        // given
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setId(1L);
        datasetVersion.setVersionNumber(3L);
        datasetVersion.setMinorVersionNumber(5L);
        datasetVersion.setCreateTime(new Timestamp(0));
        Dataset dataset = new Dataset();
        dataset.setStorageIdentifier("storage-id");
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        // when
        DatasetVersionDTO converted = new DatasetVersionDTO.Converter().convert(datasetVersion);

        // then
        assertThat(converted)
                .extracting(DatasetVersionDTO::getId, DatasetVersionDTO::getStorageIdentifier, DatasetVersionDTO::getVersionState,
                        DatasetVersionDTO::getVersionNumber, DatasetVersionDTO::getVersionMinorNumber, DatasetVersionDTO::getCreateTime)
                .containsExactly(1L, "storage-id", "RELEASED",
                        3L, 5L, "1970-01-01T00:00:00Z");
    }
}