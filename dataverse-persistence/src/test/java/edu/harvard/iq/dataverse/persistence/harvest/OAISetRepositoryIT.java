package edu.harvard.iq.dataverse.persistence.harvest;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class OAISetRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private OAISetRepository repository;


    @Before
    public void setUp() {
        repository.save(buildOaiSet("Oai Set 1", "oai_set_1"));
        repository.save(buildOaiSet("Oai Set 2", "oai_set_2"));
        repository.save(buildOaiSet("Oai Set 3", "oai_set_3"));
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
    
    // -------------------- PRIVATE --------------------
    
    private OAISet buildOaiSet(String name, String specName) {
        OAISet oaiSet = new OAISet();
        oaiSet.setName(name);
        oaiSet.setSpec(specName);
        return oaiSet;
    }
}
