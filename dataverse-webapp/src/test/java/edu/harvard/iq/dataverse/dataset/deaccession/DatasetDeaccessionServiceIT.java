package edu.harvard.iq.dataverse.dataset.deaccession;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class DatasetDeaccessionServiceIT extends WebappArquillianDeployment {

    @EJB
    private DatasetDeaccessionService deaccessionService;

    @Inject
    private DatasetDao datasetDao;

    @Inject
    private DataverseSession dataverseSession;

    @EJB
    private AuthenticationServiceBean authenticationServiceBean;

    @Inject
    private DatasetVersionServiceBean versionsService;

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void shouldDeaccessVersion() {
        // given
        Dataset dataset = datasetDao.find(56L);
        DatasetVersion versionToBeDeaccessed = dataset.getReleasedVersion();
        int versionsCount = dataset.getVersions().size();

        // when
        deaccessionService.deaccessVersions(Collections.singletonList(versionToBeDeaccessed), "TestReason", "https://www.google.com/");

        // then
        Dataset dbDataset = datasetDao.find(56L);

        assertEquals(DatasetVersion.VersionState.DEACCESSIONED, versionsService.find(versionToBeDeaccessed.getId()).getVersionState());
        assertEquals(versionsCount, dbDataset.getVersions().size());
        assertTrue(dbDataset.getVersions()
                .stream()
                .anyMatch(version -> version.getVersionState().equals(DatasetVersion.VersionState.DEACCESSIONED)));
    }

    @Test
    public void shouldDeaccessVersions() {
        // given
        Dataset dataset = datasetDao.find(56L);
        int versionsCount = dataset.getVersions().size();

        // when
        deaccessionService.deaccessVersions(dataset.getVersions(), "TestReason", "https://www.google.com/");

        // then
        Dataset dbDataset = datasetDao.find(56L);

        assertEquals(versionsCount, dbDataset.getVersions().size());
        assertTrue(dbDataset.getVersions().stream()
                .allMatch(version -> version.getVersionState().equals(DatasetVersion.VersionState.DEACCESSIONED)));
    }

    @Test
    public void shouldDeaccessReleasedVersions() {
        // given
        Dataset dataset = datasetDao.find(56L);
        int versionsCount = dataset.getVersions().size();

        // when
        deaccessionService.deaccessReleasedVersions(dataset.getVersions(), "TestReason", "https://www.google.com/");

        // then
        Dataset dbDataset = datasetDao.find(56L);
        assertEquals(versionsCount, dbDataset.getVersions().size());
        assertFalse(dbDataset.getVersions().stream()
                .anyMatch(version -> version.getVersionState().equals(DatasetVersion.VersionState.RELEASED)));
    }
}
