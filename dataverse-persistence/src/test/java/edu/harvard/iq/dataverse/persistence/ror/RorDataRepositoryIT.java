package edu.harvard.iq.dataverse.persistence.ror;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.common.DBItegrationTest;

public class RorDataRepositoryIT extends DBItegrationTest {

    private RorDataRepository rorDataRepository = new RorDataRepository(getEntityManager());

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
