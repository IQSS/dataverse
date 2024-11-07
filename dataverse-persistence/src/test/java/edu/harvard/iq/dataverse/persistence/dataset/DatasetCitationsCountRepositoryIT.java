package edu.harvard.iq.dataverse.persistence.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.common.DBItegrationTest;

public class DatasetCitationsCountRepositoryIT extends DBItegrationTest {

    private DatasetCitationsCountRepository repository = new DatasetCitationsCountRepository(getEntityManager());


    //-------------------- TESTS --------------------

    @Test
    public void findByDatasetId_existing_count() {
        // when
        Optional<DatasetCitationsCount> citationCount = repository.findByDatasetId(56L);
        // then
        assertThat(citationCount).isPresent()
            .get().extracting(DatasetCitationsCount::getCitationsCount)
            .isEqualTo(8);
    }
    
    @Test
    public void findByDatasetId_not_existing_count() {
        // when
        Optional<DatasetCitationsCount> citationCount = repository.findByDatasetId(999L);
        // then
        assertThat(citationCount).isNotPresent();
    }
    
    
}
