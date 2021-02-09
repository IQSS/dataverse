package edu.harvard.iq.dataverse.persistence.group;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


public class MailDomainGroupRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private MailDomainGroupRepository repository;

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