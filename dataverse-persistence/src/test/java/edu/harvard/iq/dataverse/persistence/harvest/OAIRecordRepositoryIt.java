package edu.harvard.iq.dataverse.persistence.harvest;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class OAIRecordRepositoryIt extends PersistenceArquillianDeployment {

    @Inject
    private OAIRecordRepository repository;


    @Before
    public void setUp() {
        repository.save(new OAIRecord("set_name_1", "global_id_1", new Date()));
        repository.save(new OAIRecord("set_name_1", "global_id_2", new Date()));
        repository.save(new OAIRecord("set_name_1", "global_id_3", new Date()));
        repository.save(new OAIRecord("set_name_2", "global_id_1", new Date()));
        repository.save(new OAIRecord("set_name_2", "global_id_4", new Date()));
    }
    
    // -------------------- TESTS --------------------

    @Test
    public void findBySetName() {
        // when
        List<OAIRecord> oaiRecords = repository.findBySetName("set_name_1");
        // then
        assertThat(oaiRecords)
            .extracting(OAIRecord::getGlobalId, OAIRecord::getSetName)
            .containsExactlyInAnyOrder(
                tuple("global_id_1", "set_name_1"),
                tuple("global_id_2", "set_name_1"),
                tuple("global_id_3", "set_name_1")
            );
    }

    @Test
    public void deleteBySetName() {
        // when
        repository.deleteBySetName("oai_set_2");
        // then
        assertThat(repository.findAll())
            .extracting(OAIRecord::getSetName)
            .noneMatch(setName -> StringUtils.equals(setName, "oai_set_2"));
    }

}
