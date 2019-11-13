package edu.harvard.iq.dataverse.dataset.deaccession;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class DatasetDeaccessionServiceIT extends WebappArquillianDeployment {

    @Inject
    private DatasetDeaccesssionService deaccesssionService;

    @Inject
    private DatasetServiceBean datasetService;

    @Inject
    private DataverseSession dataverseSession;

    @EJB
    private AuthenticationServiceBean authenticationServiceBean;

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void shouldDeaccessVersion() {
        // given
        Dataset dataset = datasetService.find(56L);

        // when
        DatasetVersion deaccessedVersion = deaccesssionService.deaccessVersion(dataset.getReleasedVersion());

        // then
        Dataset dbDataset = datasetService.find(56L);
        assertEquals(deaccessedVersion.getVersionState(), DatasetVersion.VersionState.DEACCESSIONED);
        assertTrue(dbDataset.getVersions().contains(deaccessedVersion));

        assertTrue(dbDataset.getVersions()
                .stream()
                .map(DatasetVersion::getVersionState).collect(Collectors.toList())
                .contains(DatasetVersion.VersionState.DEACCESSIONED));
        assertTrue(dbDataset.getVersions()
                .stream()
                .map(DatasetVersion::getVersionState).collect(Collectors.toList())
                .contains(DatasetVersion.VersionState.RELEASED));
        assertTrue(dbDataset.getVersions()
                .stream()
                .map(DatasetVersion::getVersionState).collect(Collectors.toList())
                .contains(DatasetVersion.VersionState.DRAFT));
    }

    @Test
    public void shouldDeaccessVersions() {
        // given
        Dataset dataset = datasetService.find(56L);

        // when
        List<DatasetVersion> deaccessedVersions = deaccesssionService.deaccessVersions(dataset.getVersions(), "TestReason", "TestForwardURL");

        // then
        Dataset dbDataset = datasetService.find(56L);

        assertTrue(dbDataset.getVersions()
                .stream()
                .map(DatasetVersion::getVersionState).collect(Collectors.toList())
                .contains(DatasetVersion.VersionState.DEACCESSIONED));
        assertFalse(dbDataset.getVersions()
                .stream()
                .map(DatasetVersion::getVersionState).collect(Collectors.toList())
                .contains(DatasetVersion.VersionState.RELEASED));
        assertFalse(dbDataset.getVersions()
                .stream()
                .map(DatasetVersion::getVersionState).collect(Collectors.toList())
                .contains(DatasetVersion.VersionState.DRAFT));
    }
}
