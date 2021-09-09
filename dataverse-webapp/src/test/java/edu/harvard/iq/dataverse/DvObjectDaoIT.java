package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.DvObject;
import org.assertj.core.api.Assertions;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class DvObjectDaoIT extends WebappArquillianDeployment {

    @Inject
    private DvObjectDao dvObjectDao;

    @Test
    public void findDvo_withNumber() {
        //given
        String id = "1";

        //when
        final DvObject dvo = dvObjectDao.findDvo(id);

        //then
        Assertions.assertThat(dvo).isNotNull();
    }

    @Test
    public void findDvo_withAlias() {
        //given
        String id = "ownmetadatablocks";

        //when
        final DvObject dvo = dvObjectDao.findDvo(id);

        //then
        Assertions.assertThat(dvo).isNotNull();
    }

    @Test
    public void findDvo_withGlobalId() {
        //given
        String id = "doi:10.18150/FK2/QTVQKL";

        //when
        final DvObject dvo = dvObjectDao.findDvo(id);

        //then
        Assertions.assertThat(dvo).isNotNull();
    }
}
