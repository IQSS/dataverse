package edu.harvard.iq.dataverse.persistence.dataset;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.common.DBItegrationTest;
import edu.harvard.iq.dataverse.persistence.DvObject;

public class DatasetRepositoryIT extends DBItegrationTest {

    private DatasetRepository datasetRepository = new DatasetRepository(getEntityManager());


    //-------------------- TESTS --------------------

    @Test
    public void findByOwnerId() {
        // when
        List<Dataset> datasets = datasetRepository.findByOwnerId(19L);
        // then
        assertNotNull(datasets);
        assertEquals(2, datasets.size());
        assertThat(datasets.stream().map(Dataset::getId).collect(toList()), containsInAnyOrder(56L, 57L));
    }

    @Test
    public void findByOwnerId_dataverse_without_datasets() {
        // when
        List<Dataset> datasets = datasetRepository.findByOwnerId(67L);
        // then
        assertNotNull(datasets);
        assertEquals(0, datasets.size());
    }

    @Test
    public void findByOwnerId_non_existing_dataverse() {
        // when
        List<Dataset> datasets = datasetRepository.findByOwnerId(9999L);
        // then
        assertNotNull(datasets);
        assertEquals(0, datasets.size());
    }


    @Test
    public void findIdsByOwnerId() {
        // when
        List<Long> datasetIds = datasetRepository.findIdsByOwnerId(19L);
        // then
        assertNotNull(datasetIds);
        assertEquals(2, datasetIds.size());
        assertThat(datasetIds, containsInAnyOrder(56L, 57L));
    }

    @Test
    public void findIdsByOwnerId_dataverse_without_datasets() {
        // when
        List<Long> datasetIds = datasetRepository.findIdsByOwnerId(67L);
        // then
        assertNotNull(datasetIds);
        assertEquals(0, datasetIds.size());
    }

    @Test
    public void findIdsByOwnerId_non_existing_dataverse() {
        // when
        List<Long> datasetIds = datasetRepository.findIdsByOwnerId(9999L);
        // then
        assertNotNull(datasetIds);
        assertEquals(0, datasetIds.size());
    }

    @Test
    public void findByNonRegisteredIdentifier() {
        // when
        List<Dataset> datasetsFound = datasetRepository.findByNonRegisteredIdentifier();

        // then
        org.assertj.core.api.Assertions.assertThat(datasetsFound).size().isEqualTo(7);
        org.assertj.core.api.Assertions.assertThat(datasetsFound).extracting(DvObject::getId)
                .containsOnly(52L, 56L, 57L, 66L, 72L, 73L, 74L);
    }
}
