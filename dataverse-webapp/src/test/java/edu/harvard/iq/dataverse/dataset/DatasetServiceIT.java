package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class DatasetServiceIT extends WebappArquillianDeployment {

    @Inject
    private DatasetService datasetService;

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private AuthenticationServiceBean authenticationServiceBean;

    @Inject
    private DatasetDao datasetDao;

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void removeDatasetThumbnail() {
        //given
        Dataset datasetWithFiles = datasetDao.find(52L);
        datasetWithFiles.setThumbnailFile(datasetWithFiles.getFiles().get(0));
        Dataset datasetWithThumbnail = datasetDao.merge(datasetWithFiles);

        //when
        datasetService.removeDatasetThumbnail(datasetWithThumbnail);

        //then
        Dataset updatedDataset = datasetDao.find(52L);
        Assert.assertNull(updatedDataset.getThumbnailFile());
    }

    @Test
    public void changeDatasetThumbnail() {
        //given
        Dataset datasetWithFiles = datasetDao.find(52L);

        //when
        datasetService.changeDatasetThumbnail(datasetWithFiles, datasetWithFiles.getFiles().get(0));
        Dataset updatedDataset = datasetDao.find(52L);

        //then
        Assert.assertEquals(datasetWithFiles.getFiles().get(0), updatedDataset.getThumbnailFile());
    }

    @Test
    public void shouldSetDatasetEmbargoDate() {
        // given
        Dataset draftDataset = datasetDao.find(66L);
        Date embargoDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));

        //
        datasetService.setDatasetEmbargoDate(draftDataset, embargoDate);

        // then
        Dataset dbDataset = datasetDao.find(66L);
        Assert.assertTrue(dbDataset.getEmbargoDate().isDefined());
        Assert.assertEquals(embargoDate, dbDataset.getEmbargoDate().get());
    }

    @Test
    public void shouldLiftDatasetEmbargoDate() {
        // given
        Dataset draftDataset = datasetDao.find(66L);
        Date embargoDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        draftDataset.setEmbargoDate(embargoDate);
        datasetDao.merge(draftDataset);

        //
        datasetService.liftDatasetEmbargoDate(draftDataset);

        // then
        Dataset dbDataset = datasetDao.find(66L);
        Assert.assertTrue(dbDataset.getEmbargoDate().isEmpty());
    }
}
