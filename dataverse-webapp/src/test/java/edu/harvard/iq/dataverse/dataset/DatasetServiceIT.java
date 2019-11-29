package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.GenericDao;
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
    private GenericDao genericDao;

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void removeDatasetThumbnail() {
        //given
        Dataset datasetWithFiles = genericDao.find(52, Dataset.class);
        datasetWithFiles.setThumbnailFile(datasetWithFiles.getFiles().get(0));
        Dataset datasetWithThumbnail = genericDao.merge(datasetWithFiles);

        //when
        datasetService.removeDatasetThumbnail(datasetWithThumbnail);

        //then
        Dataset updatedDataset = genericDao.find(52, Dataset.class);
        Assert.assertNull(updatedDataset.getThumbnailFile());
    }

    @Test
    public void changeDatasetThumbnail() {
        //given
        Dataset datasetWithFiles = genericDao.find(52, Dataset.class);

        //when
        datasetService.changeDatasetThumbnail(datasetWithFiles, datasetWithFiles.getFiles().get(0));
        Dataset updatedDataset = genericDao.find(52, Dataset.class);

        //then
        Assert.assertEquals(datasetWithFiles.getFiles().get(0), updatedDataset.getThumbnailFile());
    }
}
