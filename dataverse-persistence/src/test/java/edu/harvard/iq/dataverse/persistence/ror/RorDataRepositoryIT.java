package edu.harvard.iq.dataverse.persistence.ror;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.assertj.core.api.Assertions;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

@Transactional(TransactionMode.ROLLBACK)
public class RorDataRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private RorDataRepository rorDataRepository;

    @Test
    public void truncateAll(){
        //when
        final List<RorData> dataBefore = rorDataRepository.findAll();
        rorDataRepository.truncateAll();
        final List<RorData> dataAfter = rorDataRepository.findAll();

        //then
        Assertions.assertThat(dataBefore.size()).isEqualTo(1);
        Assertions.assertThat(dataAfter.size()).isEqualTo(0);

    }
}
