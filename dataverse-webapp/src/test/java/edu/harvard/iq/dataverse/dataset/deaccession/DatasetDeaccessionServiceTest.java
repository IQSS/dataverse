package edu.harvard.iq.dataverse.dataset.deaccession;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.DeaccessionDatasetVersionCommand;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DatasetDeaccessionServiceTest {
    @InjectMocks
    private DatasetDeaccessionService deaccesssionService;

    @Mock
    private EjbDataverseEngine commandEngine;
    @Mock
    private DataverseRequestServiceBean dvRequestService;
    @Mock
    private DatasetVersionServiceBean datasetVersionService;

    private Dataset dataset = new Dataset();

    @BeforeEach
    public void setUp() {
        dataset.setId(1L);

        DatasetVersion ver1 = buildDtsetVersion(1L, 1L, DatasetVersion.VersionState.RELEASED);
        DatasetVersion ver2 = buildDtsetVersion(2L, 2L, DatasetVersion.VersionState.RELEASED);
        DatasetVersion verDraft = buildDtsetVersion(3L, 3L, DatasetVersion.VersionState.DRAFT);

        dataset.setVersions(Lists.newArrayList(ver1, ver2, verDraft));

        when(commandEngine.submit(any(DeaccessionDatasetVersionCommand.class))).thenReturn(this.dataset.getLatestVersion());
        when(datasetVersionService.find(1L)).thenReturn(this.dataset.getVersions().get(0));
        when(datasetVersionService.find(2L)).thenReturn(this.dataset.getVersions().get(1));
        when(datasetVersionService.find(3L)).thenReturn(this.dataset.getVersions().get(2));
    }

    @Test
    public void deaccessVersion() {
        // given
        int versionToDeaccess = 1;

        // when
        deaccesssionService.deaccessVersion(dataset.getVersions().get(versionToDeaccess), "testReason", "testForwardUrl");

        // then
        verify(commandEngine, times(1)).submit(any(DeaccessionDatasetVersionCommand.class));
    }

    @Test
    public void deaccessVersions() {
        // given & when
        deaccesssionService.deaccessVersions(dataset.getVersions(), "TestReasons", "fakeUrl");

        // then
        verify(commandEngine, times(3)).submit(any(DeaccessionDatasetVersionCommand.class));
    }

    @Test
    public void deaccessReleasedVersions() {
        // given & when
        deaccesssionService.deaccessReleasedVersions(dataset.getVersions(), "TestReasons", "fakeUrl");

        // then
        verify(commandEngine, times(2)).submit(any(DeaccessionDatasetVersionCommand.class));
    }

    // -------------------- PRIVATE ---------------------
    private DatasetVersion buildDtsetVersion(long id, long versionNumber, DatasetVersion.VersionState state) {
        DatasetVersion version = new DatasetVersion();
        version.setId(id);
        version.setVersionNumber(versionNumber);
        version.setDataset(this.dataset);
        version.setVersionState(state);
        version.setCreateTime(Timestamp.valueOf(LocalDateTime.now()));

        return version;
    }
}
