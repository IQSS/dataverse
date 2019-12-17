package edu.harvard.iq.dataverse.dataset.datasetversion;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.guestbook.GuestbookServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class DatasetVersionServiceBeanIT extends WebappArquillianDeployment {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private DatasetVersionServiceBean datasetVersionService;
    @Inject
    private DatasetDao datasetDao;
    @Inject
    private GuestbookServiceBean guestbookService;
    @Inject
    private DataverseSession dataverseSession;
    @EJB
    private AuthenticationServiceBean authenticationServiceBean;

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    // -------------------- TESTS --------------------

    @Test
    public void shouldUpdateDatasetVersion() {
        // given
        Dataset dataset = datasetDao.find(52L);
        modifyFileLicense(dataset);

        // when
        dataset.setGuestbook(guestbookService.find(2L));
        datasetVersionService.updateDatasetVersion(dataset.getEditVersion(), true);

        // then
        Dataset dbDataset = datasetDao.find(52L);
        assertEquals(2L, (long) dbDataset.getGuestbook().getId());
    }

    // -------------------- PRIVATE --------------------

    private void modifyFileLicense(Dataset dataset) {
        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        License license = new License();
        license.setName("");
        license.setPosition(99L);
        license.setUrl("");
        termsOfUse.setAllRightsReserved(true);
        termsOfUse.setLicense(license);

        dataset.getLatestVersion().getFileMetadatas().get(0).setTermsOfUse(termsOfUse);
    }
}
