package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
class FileDownloadHelperTest {

    @Mock
    private PermissionServiceBean permissionServiceBean;

    @Mock
    private DataverseSession dataverseSession;

    @Mock
    private DataverseRequestServiceBean dataverseRequestServiceBean;

    private FileDownloadHelper fileDownloadHelper;

    @BeforeEach
    void setup() {
        fileDownloadHelper = new FileDownloadHelper();
        fileDownloadHelper.permissionService = permissionServiceBean;
        fileDownloadHelper.session = dataverseSession;
        fileDownloadHelper.dvRequestService = dataverseRequestServiceBean;
    }

    @Test
    void testDoesSessionUserHavePermission_withoutPermissionToCheck() {
        assertFalse(fileDownloadHelper.doesSessionUserHavePermission(null, new FileMetadata()));
    }

    @Test
    void testDoesSessionUserHavePermission_forEditDatasetPermission() {
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(new Dataset());
        DataFile dataFile = new DataFile();

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDatasetVersion(datasetVersion);
        fileMetadata.setDataFile(dataFile);

        mockPermissionResponseUserOn(Permission.EditDataset, false);
        assertFalse(fileDownloadHelper.doesSessionUserHavePermission(Permission.EditDataset, fileMetadata));

        mockPermissionResponseUserOn(Permission.EditDataset, true);
        assertTrue(fileDownloadHelper.doesSessionUserHavePermission(Permission.EditDataset, fileMetadata));
    }

    @Test
    void testDoesSessionUserHavePermission_forDownloadFilePermission() {
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(new Dataset());
        DataFile dataFile = new DataFile();

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDatasetVersion(datasetVersion);
        fileMetadata.setDataFile(dataFile);

        mockPermissionResponseUserOn(Permission.DownloadFile, false);
        assertFalse(fileDownloadHelper.doesSessionUserHavePermission(Permission.DownloadFile, fileMetadata));

        mockPermissionResponseUserOn(Permission.DownloadFile, true);
        assertTrue(fileDownloadHelper.doesSessionUserHavePermission(Permission.DownloadFile, fileMetadata));
    }

    @Test
    void testDoesSessionUserHavePermission_forAnyOtherPermission() {
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(new Dataset());
        DataFile dataFile = new DataFile();

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDatasetVersion(datasetVersion);
        fileMetadata.setDataFile(dataFile);

        assertFalse(fileDownloadHelper.doesSessionUserHavePermission(Permission.ManageDataversePermissions, fileMetadata));
    }

    @Test
    void testDoesSessionUserHavePermission_withNullDataset() {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDatasetVersion(new DatasetVersion());

        assertFalse(fileDownloadHelper.doesSessionUserHavePermission(Permission.EditDataset, fileMetadata));
    }

    @Test
    void testDoesSessionUserHavePermission_withNullDataFile() {
        FileMetadata fileMetadata = new FileMetadata();

        assertFalse(fileDownloadHelper.doesSessionUserHavePermission(Permission.DownloadFile, fileMetadata));
    }

    @Test
    void testCanDownloadFile_withoutFileMetadata() {
        assertFalse(fileDownloadHelper.canDownloadFile(null));
    }

    @Test
    void testCanDownloadFile_withNullMetadataId() {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(null);

        assertFalse(fileDownloadHelper.canDownloadFile(fileMetadata));
    }

    @Test
    void testCanDownloadFile_withNullDataFileId() {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);
        DataFile dataFile = new DataFile();
        dataFile.setId(null);
        fileMetadata.setDataFile(dataFile);

        assertFalse(fileDownloadHelper.canDownloadFile(fileMetadata));
    }

    @ParameterizedTest
    @CsvSource({ "false", "true" })
    void testCanDownloadFile_forDeaccessionedFile(boolean hasPermission) {
        DataFile dataFile = new DataFile();
        dataFile.setId(2L);

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(new Dataset());
        datasetVersion.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(datasetVersion);

        mockPermissionResponseUserOn(Permission.EditDataset, hasPermission);

        assertEquals(hasPermission, fileDownloadHelper.canDownloadFile(fileMetadata), "Initial response does not match expectation!");

        // call again to exercise the cache and ensure it returns the same result
        assertEquals(hasPermission, fileDownloadHelper.canDownloadFile(fileMetadata), "Cached response does not match initial response!");
    }

    @Test
    void testCanDownloadFile_forUnrestrictedReleasedFile() {
        DataFile dataFile = new DataFile();
        dataFile.setId(2L);

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);
        fileMetadata.setRestricted(false);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(datasetVersion);

        assertTrue(fileDownloadHelper.canDownloadFile(fileMetadata));

        // call again to exercise the cache and ensure it returns the same result
        assertTrue(fileDownloadHelper.canDownloadFile(fileMetadata));
    }

    @Test
    void testCanDownloadFile_forUnrestrictedReleasedActiveEmbargoFile() {
        DataFile dataFile = new DataFile();
        dataFile.setId(2L);

        // With an embargo, an unrestricted file should only be accessible if the embargo has ended

        Embargo emb = new Embargo(LocalDate.now().plusDays(3), "Still embargoed");
        dataFile.setEmbargo(emb);
        
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);
        fileMetadata.setRestricted(false);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(datasetVersion);
        mockPermissionResponseRequestOn(Permission.DownloadFile, false);
        assertFalse(fileDownloadHelper.canDownloadFile(fileMetadata));
    }

    @Test
    void testCanDownloadFile_forUnrestrictedReleasedExpiredEmbargoFile() {
        DataFile dataFile = new DataFile();
        dataFile.setId(2L);

        // With an embargo, an unrestricted file should only be accessible if the embargo has ended

        Embargo emb = new Embargo(LocalDate.now().minusDays(3), "Was embargoed");
        dataFile.setEmbargo(emb);
        
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);
        fileMetadata.setRestricted(false);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(datasetVersion);
        
        assertTrue(fileDownloadHelper.canDownloadFile(fileMetadata));
    }

    @Test
    void testCanNotDownloadFile_forExpiredRetentionFile() {
        DataFile dataFile = new DataFile();
        dataFile.setId(2L);

        // With an expired retention end date, an unrestricted file should not be accessible

        Retention ret = new Retention(LocalDate.now().minusDays(1), "Retention period expired");
        dataFile.setRetention(ret);

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);
        fileMetadata.setRestricted(false);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(datasetVersion);

        assertFalse(fileDownloadHelper.canDownloadFile(fileMetadata));
    }

    @Test
    void testCanDownloadFile_forUnrestrictedReleasedNotExpiredRetentionFile() {
        DataFile dataFile = new DataFile();
        dataFile.setId(2L);

        // With a retention end date in the future, an unrestricted file should be accessible

        Retention ret = new Retention(LocalDate.now(), "Retention period NOT expired");
        dataFile.setRetention(ret);

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);
        fileMetadata.setRestricted(false);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(datasetVersion);

        assertTrue(fileDownloadHelper.canDownloadFile(fileMetadata));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void testCanDownloadFile_forRestrictedReleasedFile(boolean hasPermission) {
        DataFile dataFile = new DataFile();
        dataFile.setId(2L);

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);
        fileMetadata.setRestricted(true);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(datasetVersion);

        mockPermissionResponseRequestOn(Permission.DownloadFile, hasPermission);

        assertEquals(hasPermission, fileDownloadHelper.canDownloadFile(fileMetadata), "Initial response does not match expectation!");

        // call again to exercise the cache and ensure it returns the same result
        assertEquals(hasPermission, fileDownloadHelper.canDownloadFile(fileMetadata), "Cached response does not match initial response!");
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void testCanDownloadFile_forRestrictedReleasedFileWithActiveEmbargo(boolean hasPermission) {
        DataFile dataFile = new DataFile();
        dataFile.setId(2L);

        // With an active embargo, a restricted file should have the same access regardless of
        // embargo state (with an active embargo, there's no way to request permissions,
        // so the hasPermission=true case primarily applies to the original dataset
        // creators)

        Embargo emb = new Embargo(LocalDate.now().plusDays(3), "Still embargoed");
        dataFile.setEmbargo(emb);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);
        fileMetadata.setRestricted(true);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(datasetVersion);

        mockPermissionResponseRequestOn(Permission.DownloadFile, hasPermission);
        
        assertEquals(hasPermission, fileDownloadHelper.canDownloadFile(fileMetadata), "Initial response does not match expectation!");
    }
    
    @ParameterizedTest
    @CsvSource({"false", "true"})
    void testCanDownloadFile_forRestrictedReleasedFileWithExpiredEmbargo(boolean hasPermission) {
        DataFile dataFile = new DataFile();
        dataFile.setId(2L);

        // With an embargo, a restricted file should have the same access regardless of
        // embargo state (with an active embargo, there's no way to request permissions,
        // so the hasPermission=true case primarily applies to the original dataset
        // creators)

        Embargo emb = new Embargo(LocalDate.now().minusDays(3), "No longer embargoed");
        dataFile.setEmbargo(emb);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);
        fileMetadata.setRestricted(true);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(datasetVersion);

        mockPermissionResponseRequestOn(Permission.DownloadFile, hasPermission);

        assertEquals(hasPermission, fileDownloadHelper.canDownloadFile(fileMetadata), "Initial response does not match expectation!");

    }


    private void mockPermissionResponseUserOn(Permission permission, boolean response) {
        PermissionServiceBean.StaticPermissionQuery staticPermissionQuery = mock(PermissionServiceBean.StaticPermissionQuery.class);

        if (permission.equals(Permission.EditDataset)) {
            when(permissionServiceBean.userOn(ArgumentMatchers.any(), ArgumentMatchers.any(Dataset.class))).thenReturn(staticPermissionQuery);
        } else {
            when(permissionServiceBean.userOn(ArgumentMatchers.any(), ArgumentMatchers.any(DataFile.class))).thenReturn(staticPermissionQuery);
        }

        when(staticPermissionQuery.has(permission)).thenReturn(response);
    }

    private void mockPermissionResponseRequestOn(Permission permission, boolean response) {
        PermissionServiceBean.RequestPermissionQuery requestPermissionQuery = mock(PermissionServiceBean.RequestPermissionQuery.class);

        if (permission.equals(Permission.EditDataset)) {
            when(permissionServiceBean.requestOn(ArgumentMatchers.any(), ArgumentMatchers.any(Dataset.class))).thenReturn(requestPermissionQuery);
        } else {
            when(permissionServiceBean.requestOn(ArgumentMatchers.any(), ArgumentMatchers.any(DataFile.class))).thenReturn(requestPermissionQuery);
        }

        when(requestPermissionQuery.has(permission)).thenReturn(response);
    }
}
