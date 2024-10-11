package edu.harvard.iq.dataverse.dataset.deaccession;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @BeforeEach
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

        assertEquals(DatasetVersion.VersionState.DEACCESSIONED, versionsService.getById(versionToBeDeaccessed.getId()).getVersionState());
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
