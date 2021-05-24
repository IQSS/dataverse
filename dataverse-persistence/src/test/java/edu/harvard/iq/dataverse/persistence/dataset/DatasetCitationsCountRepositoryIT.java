package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.junit.Test;

import javax.inject.Inject;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class DatasetCitationsCountRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private DatasetCitationsCountRepository repository;


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
