package edu.harvard.iq.dataverse.persistence.dataverse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.common.DBItegrationTest;
import edu.harvard.iq.dataverse.persistence.DvObject;

public class DataverseFeaturedDataverseRepositoryIT extends DBItegrationTest {

    private DataverseFeaturedDataverseRepository repository = new DataverseFeaturedDataverseRepository(getEntityManager());

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
