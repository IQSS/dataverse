package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDatasetVersionCountCommandTest {

    @Mock
    private CommandContext commandContextMock;
    @Mock
    private DataverseRequest dataverseRequestMock;
    @Mock
    private DatasetVersionServiceBean datasetVersionServiceMock;
    @Mock
    private PermissionServiceBean permissionServiceMock;
    @Mock
    private Dataset datasetMock;
    @Mock
    private AuthenticatedUser userMock;

    private static final Long TEST_DATASET_ID = 42L;

    @BeforeEach
    void setUp() {
        when(commandContextMock.datasetVersion()).thenReturn(datasetVersionServiceMock);
        when(commandContextMock.permissions()).thenReturn(permissionServiceMock);
        when(datasetMock.getId()).thenReturn(TEST_DATASET_ID);
        when(dataverseRequestMock.getUser()).thenReturn(userMock);
    }

    @Test
    @DisplayName("Should count ALL versions when user has ViewUnpublishedDataset permission")
    void execute_whenUserCanViewUnpublished_countsAllVersions() throws CommandException {
        // Arrange
        // Simulate that the user has the required permission
        when(permissionServiceMock.hasPermissionsFor(
                eq(userMock),
                eq(datasetMock),
                eq(EnumSet.of(Permission.ViewUnpublishedDataset))
        )).thenReturn(true);

        // Define the expected count when all versions are included
        Long expectedTotalCount = 10L;
        when(datasetVersionServiceMock.getDatasetVersionCount(TEST_DATASET_ID, true)).thenReturn(expectedTotalCount);

        GetDatasetVersionCountCommand command = new GetDatasetVersionCountCommand(dataverseRequestMock, datasetMock);

        // Act
        Long actualCount = command.execute(commandContextMock);

        // Assert
        assertEquals(expectedTotalCount, actualCount, "The count should include all versions (published and unpublished).");

        // Verify that the permission check was performed
        verify(permissionServiceMock).hasPermissionsFor(
                eq(userMock),
                eq(datasetMock),
                eq(EnumSet.of(Permission.ViewUnpublishedDataset))
        );

        // Verify the service was called with 'true' to indicate unpublished versions should be counted
        verify(datasetVersionServiceMock).getDatasetVersionCount(TEST_DATASET_ID, true);
    }

    @Test
    @DisplayName("Should count ONLY published versions when user lacks ViewUnpublishedDataset permission")
    void execute_whenUserCannotViewUnpublished_countsOnlyPublishedVersions() throws CommandException {
        // Arrange
        // Simulate that the user does NOT have the required permission
        when(permissionServiceMock.hasPermissionsFor(
                eq(userMock),
                eq(datasetMock),
                eq(EnumSet.of(Permission.ViewUnpublishedDataset))
        )).thenReturn(false);

        // Define the expected count when only published versions are included
        Long expectedPublishedCount = 5L;
        when(datasetVersionServiceMock.getDatasetVersionCount(TEST_DATASET_ID, false)).thenReturn(expectedPublishedCount);

        GetDatasetVersionCountCommand command = new GetDatasetVersionCountCommand(dataverseRequestMock, datasetMock);

        // Act
        Long actualCount = command.execute(commandContextMock);

        // Assert
        assertEquals(expectedPublishedCount, actualCount, "The count should only include published versions.");

        // Verify that the permission check was performed
        verify(permissionServiceMock).hasPermissionsFor(
                eq(userMock),
                eq(datasetMock),
                eq(EnumSet.of(Permission.ViewUnpublishedDataset))
        );

        // Verify the service was called with 'false' to indicate only published versions should be counted
        verify(datasetVersionServiceMock).getDatasetVersionCount(TEST_DATASET_ID, false);
    }
}
