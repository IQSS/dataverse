package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.User;
import jena.query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileDownloadHelperTest {

    @Mock
    private PermissionServiceBean permissionServiceBean;

    @Mock
    private DataverseSession dataverseSession;

    @Mock
    private PermissionServiceBean.StaticPermissionQuery staticPermissionQuery;

    private FileDownloadHelper fileDownloadHelper;
    private FileMetadata fileMetadata;

    @BeforeEach
    void setup() {
        fileDownloadHelper = new FileDownloadHelper();
        fileDownloadHelper.permissionService = permissionServiceBean;
        fileDownloadHelper.session = dataverseSession;

        fileMetadata = new FileMetadata();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(new Dataset());
        fileMetadata.setDatasetVersion(datasetVersion);
        fileMetadata.setDataFile(new DataFile());
    }

    @Test
    void testDoesSessionUserHavePermission_withoutPermissionToCheck() {
        assertFalse(fileDownloadHelper.doesSessionUserHavePermission(null, new FileMetadata()));
    }

    @Test
    void testDoesSessionUserHavePermission_forEditDatasetPermission() {
        // if the permission service is called with a Dataset, return a static permission query
        when(permissionServiceBean.userOn(ArgumentMatchers.any(), ArgumentMatchers.any(Dataset.class))).thenReturn(staticPermissionQuery);

        when(staticPermissionQuery.has(Permission.EditDataset)).thenReturn(false);
        assertFalse(fileDownloadHelper.doesSessionUserHavePermission(Permission.EditDataset, this.fileMetadata));

        when(staticPermissionQuery.has(Permission.EditDataset)).thenReturn(true);
        assertTrue(fileDownloadHelper.doesSessionUserHavePermission(Permission.EditDataset, this.fileMetadata));
    }

    @Test
    void testDoesSessionUserHavePermission_forDownloadFilePermission() {
        // if the permission service is called with a DataFile, return a static permission query
        when(permissionServiceBean.userOn(ArgumentMatchers.any(), ArgumentMatchers.any(DataFile.class))).thenReturn(staticPermissionQuery);

        when(staticPermissionQuery.has(Permission.DownloadFile)).thenReturn(false);
        assertFalse(fileDownloadHelper.doesSessionUserHavePermission(Permission.DownloadFile, this.fileMetadata));

        when(staticPermissionQuery.has(Permission.DownloadFile)).thenReturn(true);
        assertTrue(fileDownloadHelper.doesSessionUserHavePermission(Permission.DownloadFile, this.fileMetadata));
    }

    @Test
    void testDoesSessionUserHavePermission_forAnyOtherPermission() {
        assertFalse(fileDownloadHelper.doesSessionUserHavePermission(Permission.ManageDataversePermissions, this.fileMetadata));
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
    void testCanDownloadFile() {

    }
}
