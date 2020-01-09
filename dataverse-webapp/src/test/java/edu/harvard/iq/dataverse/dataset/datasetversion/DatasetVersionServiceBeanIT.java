package edu.harvard.iq.dataverse.dataset.datasetversion;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.guestbook.GuestbookServiceBean;
import edu.harvard.iq.dataverse.persistence.MockMetadataFactory;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@RunWith(Arquillian.class)
public class DatasetVersionServiceBeanIT extends WebappArquillianDeployment {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private DatasetVersionServiceBean datasetVersionService;
    @Inject
    private DatasetDao datasetDao;
    @Inject
    private DatasetFieldServiceBean datasetFieldService; 
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
    @Transactional(TransactionMode.ROLLBACK)
    public void updateDatasetVersion__dataset_with_draft() {
        // given
        Dataset dataset = datasetDao.find(52L);
        modifyFileLicense(dataset);
        dataset.setGuestbook(guestbookService.find(2L));

        // when
        datasetVersionService.updateDatasetVersion(dataset.getEditVersion(), true);

        // then
        Dataset dbDataset = datasetDao.find(52L);
        assertEquals(2L, (long) dbDataset.getGuestbook().getId());
    }
    
    @Test
    public void updateDatasetVersion__dataset_without_draft() {
        // given
        Dataset dataset = datasetDao.find(57L);
        DatasetVersion editDatasetVersion = dataset.getEditVersion();
        
        DatasetFieldType depositorFieldType = datasetFieldService.findByName(DatasetFieldConstant.depositor);
        DatasetField depositorField = DatasetField.createNewEmptyDatasetField(depositorFieldType, editDatasetVersion);
        MockMetadataFactory.fillDepositorField(depositorField, "Depositor name");
        editDatasetVersion.getDatasetFields().add(depositorField);

        // when
        datasetVersionService.updateDatasetVersion(editDatasetVersion, true);

        // then
        Dataset dbDataset = datasetDao.find(57L);
        
        assertThat(dbDataset.getVersions(), hasSize(3));
        
        DatasetVersion dbLastVersion = dbDataset.getLatestVersion();
        DatasetVersion dbPrevVersion = dbDataset.getVersions().get(1);
        DatasetVersion dbPrev2Version = dbDataset.getVersions().get(2);
        
        assertEquals(VersionState.DRAFT, dbLastVersion.getVersionState());
        assertEquals("Depositor name", dbLastVersion.getDatasetField(depositorFieldType).getRawValue());
        assertNull(dbPrevVersion.getDatasetField(depositorFieldType));
        assertNull(dbPrev2Version.getDatasetField(depositorFieldType));
    }

    // -------------------- PRIVATE --------------------

    private void modifyFileLicense(Dataset dataset) {
        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        termsOfUse.setAllRightsReserved(true);
        termsOfUse.setLicense(null);


        dataset.getLatestVersion().getFileMetadatas().get(0).setTermsOfUse(termsOfUse);
    }
}
