package edu.harvard.iq.dataverse.persistence.dataverse;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class DataverseFeaturedDataverseRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private DataverseFeaturedDataverseRepository repository;
    @Inject
    private DataverseRepository dvRepository;
    @Inject
    private DatasetRepository dsRepository;

    //-------------------- TESTS --------------------

    @Test
    public void findByDataverseIdOrderByDisplayOrder() {
        assertThat(repository.findByDataverseIdOrderByDisplayOrder(1L).stream()
                .map(DvObject::getId)).containsExactly(19L, 23L, 20L, 21L);
    }

    @Test
    public void findByDataverseIdOrderByNameAsc() {
        assertThat(repository.findByDataverseIdOrderByNameAsc(1L).stream()
                .map(DvObject::getId)).containsExactly(21L, 19L, 23L, 20L);
    }

    @Test
    public void findByDataverseIdOrderByNameDesc() {
        assertThat(repository.findByDataverseIdOrderByNameDesc(1L).stream()
                .map(DvObject::getId)).containsExactly(20L, 23L, 19L, 21L);
    }
}
