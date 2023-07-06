package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeAuthenticatedUser;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class FileDownloadServiceBeanTest {

    @Mock
    private PermissionServiceBean permissionServiceBeanStub;
    @Mock
    private DataverseRequestServiceBean dataverseRequestServiceBeanMock;
    @Mock
    private MakeDataCountLoggingServiceBean makeDataCountLoggingServiceBeanMock;

    private FileDownloadServiceBean sut;

    private User testUser;

    @BeforeEach
    public void setUp() {
        sut = new FileDownloadServiceBean();
        sut.permissionService = permissionServiceBeanStub;
        sut.dvRequestService = dataverseRequestServiceBeanMock;
        sut.mdcLogService = makeDataCountLoggingServiceBeanMock;
        testUser = makeAuthenticatedUser("Test", "Test");
    }

    @Test
    public void testCanDownloadFile_withoutUser() {
        assertFalse(sut.canDownloadFile(null, new FileMetadata()));
    }

    @Test
    public void testCanDownloadFile_withoutFileMetadata() {
        assertFalse(sut.canDownloadFile(testUser, null));
    }

    @Test
    void testCanDownloadFile_withNullMetadataId() {
        FileMetadata testFileMetadata = new FileMetadata();
        testFileMetadata.setId(null);

        assertFalse(sut.canDownloadFile(testUser, testFileMetadata));
    }

    @Test
    void testCanDownloadFile_withNullDataFileId() {
        FileMetadata testFileMetadata = new FileMetadata();
        testFileMetadata.setId(1L);
        DataFile testDataFile = new DataFile();
        testDataFile.setId(null);
        testFileMetadata.setDataFile(testDataFile);

        assertFalse(sut.canDownloadFile(testUser, testFileMetadata));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void testCanDownloadFile_forDeaccessionedFile(boolean hasPermission) {
        DataFile testDataFile = new DataFile();
        testDataFile.setId(2L);

        DatasetVersion testDatasetVersion = new DatasetVersion();
        testDatasetVersion.setDataset(new Dataset());
        testDatasetVersion.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);

        FileMetadata testFileMetadata = new FileMetadata();
        testFileMetadata.setId(1L);
        testFileMetadata.setDataFile(testDataFile);
        testFileMetadata.setDatasetVersion(testDatasetVersion);

        mockPermissionResponseUserOn(hasPermission);

        Assertions.assertEquals(hasPermission, sut.canDownloadFile(testUser, testFileMetadata));
    }

    @Test
    void testCanDownloadFile_forUnrestrictedReleasedFile() {
        DataFile testDataFile = new DataFile();
        testDataFile.setId(2L);

        DatasetVersion testDatasetVersion = new DatasetVersion();
        testDatasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata testFileMetadata = new FileMetadata();
        testFileMetadata.setId(1L);
        testFileMetadata.setRestricted(false);
        testFileMetadata.setDataFile(testDataFile);
        testFileMetadata.setDatasetVersion(testDatasetVersion);

        assertTrue(sut.canDownloadFile(testUser, testFileMetadata));
    }

    @Test
    void testCanDownloadFile_forUnrestrictedReleasedActiveEmbargoFile() {
        DataFile testDataFile = new DataFile();
        testDataFile.setId(2L);

        // With an embargo, an unrestricted file should only be accessible if the embargo has ended

        Embargo testEmbargo = new Embargo(LocalDate.now().plusDays(3), "Still embargoed");
        testDataFile.setEmbargo(testEmbargo);

        DatasetVersion testDatasetVersion = new DatasetVersion();
        testDatasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata testFileMetadata = new FileMetadata();
        testFileMetadata.setId(1L);
        testFileMetadata.setRestricted(false);
        testFileMetadata.setDataFile(testDataFile);
        testFileMetadata.setDatasetVersion(testDatasetVersion);
        mockPermissionResponseRequestOn(false);

        assertFalse(sut.canDownloadFile(testUser, testFileMetadata));
    }

    @Test
    void testCanDownloadFile_forUnrestrictedReleasedExpiredEmbargoFile() {
        DataFile testDataFile = new DataFile();
        testDataFile.setId(2L);

        // With an embargo, an unrestricted file should only be accessible if the embargo has ended

        Embargo testEmbargo = new Embargo(LocalDate.now().minusDays(3), "Was embargoed");
        testDataFile.setEmbargo(testEmbargo);

        DatasetVersion testDatasetVersion = new DatasetVersion();
        testDatasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata testFileMetadata = new FileMetadata();
        testFileMetadata.setId(1L);
        testFileMetadata.setRestricted(false);
        testFileMetadata.setDataFile(testDataFile);
        testFileMetadata.setDatasetVersion(testDatasetVersion);

        assertTrue(sut.canDownloadFile(testUser, testFileMetadata));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void testCanDownloadFile_forRestrictedReleasedFile(boolean hasPermission) {
        DataFile testDataFile = new DataFile();
        testDataFile.setId(2L);

        DatasetVersion testDatasetVersion = new DatasetVersion();
        testDatasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata testFileMetadata = new FileMetadata();
        testFileMetadata.setId(1L);
        testFileMetadata.setRestricted(true);
        testFileMetadata.setDataFile(testDataFile);
        testFileMetadata.setDatasetVersion(testDatasetVersion);

        mockPermissionResponseRequestOn(hasPermission);

        Assertions.assertEquals(hasPermission, sut.canDownloadFile(testUser, testFileMetadata));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void testCanDownloadFile_forRestrictedReleasedFileWithActiveEmbargo(boolean hasPermission) {
        DataFile testDataFile = new DataFile();
        testDataFile.setId(2L);

        // With an active embargo, a restricted file should have the same access regardless of
        // embargo state (with an active embargo, there's no way to request permissions,
        // so the hasPermission=true case primarily applies to the original dataset
        // creators)

        Embargo testEmbargo = new Embargo(LocalDate.now().plusDays(3), "Still embargoed");
        testDataFile.setEmbargo(testEmbargo);
        DatasetVersion testDatasetVersion = new DatasetVersion();
        testDatasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata testFileMetadata = new FileMetadata();
        testFileMetadata.setId(1L);
        testFileMetadata.setRestricted(true);
        testFileMetadata.setDataFile(testDataFile);
        testFileMetadata.setDatasetVersion(testDatasetVersion);

        mockPermissionResponseRequestOn(hasPermission);

        Assertions.assertEquals(hasPermission, sut.canDownloadFile(testUser, testFileMetadata));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void testCanDownloadFile_forRestrictedReleasedFileWithExpiredEmbargo(boolean hasPermission) {
        DataFile testDataFile = new DataFile();
        testDataFile.setId(2L);

        // With an embargo, a restricted file should have the same access regardless of
        // embargo state (with an active embargo, there's no way to request permissions,
        // so the hasPermission=true case primarily applies to the original dataset
        // creators)

        Embargo testEmbargo = new Embargo(LocalDate.now().minusDays(3), "No longer embargoed");
        testDataFile.setEmbargo(testEmbargo);
        DatasetVersion testDatasetVersion = new DatasetVersion();
        testDatasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata testFileMetadata = new FileMetadata();
        testFileMetadata.setId(1L);
        testFileMetadata.setRestricted(true);
        testFileMetadata.setDataFile(testDataFile);
        testFileMetadata.setDatasetVersion(testDatasetVersion);

        mockPermissionResponseRequestOn(hasPermission);

        Assertions.assertEquals(hasPermission, sut.canDownloadFile(testUser, testFileMetadata));
    }

    private void mockPermissionResponseUserOn(boolean response) {
        PermissionServiceBean.StaticPermissionQuery staticPermissionQueryMock = mock(PermissionServiceBean.StaticPermissionQuery.class);

        when(permissionServiceBeanStub.userOn(ArgumentMatchers.any(), ArgumentMatchers.any(Dataset.class))).thenReturn(staticPermissionQueryMock);
        when(staticPermissionQueryMock.has(Permission.EditDataset)).thenReturn(response);
    }

    private void mockPermissionResponseRequestOn(boolean response) {
        PermissionServiceBean.RequestPermissionQuery requestPermissionQueryMock = mock(PermissionServiceBean.RequestPermissionQuery.class);

        when(permissionServiceBeanStub.requestOn(ArgumentMatchers.any(), ArgumentMatchers.any(DataFile.class))).thenReturn(requestPermissionQueryMock);
        when(requestPermissionQueryMock.has(Permission.DownloadFile)).thenReturn(response);
    }
}
