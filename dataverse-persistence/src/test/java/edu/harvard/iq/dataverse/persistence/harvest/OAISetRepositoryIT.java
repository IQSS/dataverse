package edu.harvard.iq.dataverse.persistence.harvest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.common.DBItegrationTest;

public class OAISetRepositoryIT extends DBItegrationTest {

    private OAISetRepository repository = new OAISetRepository(getEntityManager());


    @BeforeEach
    public void setUp() {
        repository.save(new OAISet("Oai Set 1", "oai_set_1"));
        repository.save(new OAISet("Oai Set 2", "oai_set_2"));
        repository.save(new OAISet("Oai Set 3", "oai_set_3"));
    }
    
    // -------------------- TESTS --------------------

    @Test
    public void findBySpecName() {
        // when
        Optional<OAISet> oaiSet = repository.findBySpecName("oai_set_2");
        // then
        assertThat(oaiSet).isPresent();
        assertThat(oaiSet.get())
            .extracting(OAISet::getName, OAISet::getSpec)
            .containsExactly("Oai Set 2", "oai_set_2");
    }
    
    @Test
    public void findAllBySpecNameNot() {
        // when
        List<OAISet> oaiSets = repository.findAllBySpecNameNot("oai_set_2");
        // then
        assertThat(oaiSets)
            .extracting(OAISet::getName, OAISet::getSpec)
            .containsExactlyInAnyOrder(
                    tuple("Oai Set 1", "oai_set_1"),
                    tuple("Oai Set 3", "oai_set_3")
            );
    }
}
