package edu.harvard.iq.dataverse.persistence.group;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.common.DBItegrationTest;


public class MailDomainGroupRepositoryIT extends DBItegrationTest {

    private MailDomainGroupRepository repository = new MailDomainGroupRepository(getEntityManager());

    // -------------------- TESTS --------------------

    @Test
    public void findByAlias() {

        // when
        Optional<MailDomainGroup> gr1 = repository.findByAlias("gr1");

        // then
        assertThat(gr1.isPresent()).isTrue();
        MailDomainGroup group = gr1.get();
        assertThat(group.getPersistedGroupAlias()).isEqualTo("gr1");
    }
}