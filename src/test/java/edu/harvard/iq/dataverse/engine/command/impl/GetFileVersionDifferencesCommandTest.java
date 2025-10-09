package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetFileVersionDifferencesCommandTest {

    @Mock
    private CommandContext contextMock;
    @Mock
    private DataverseRequest requestMock;
    @Mock
    private FileMetadata fileMetadataMock;
    @Mock
    private PermissionServiceBean permissionsMock;
    @Mock
    private DataFileServiceBean fileServiceMock;
    @Mock
    private Dataset datasetMock;
    @Mock
    private DatasetVersion datasetVersionMock;
    @Mock
    private DataFile dataFileMock;

    private static final Long DATASET_ID = 1L;

    @BeforeEach
    void setUp() {
        when(contextMock.permissions()).thenReturn(permissionsMock);
        when(contextMock.files()).thenReturn(fileServiceMock);

        when(fileMetadataMock.getDatasetVersion()).thenReturn(datasetVersionMock);
        when(datasetVersionMock.getDataset()).thenReturn(datasetMock);
        when(datasetMock.getId()).thenReturn(DATASET_ID);
        when(fileMetadataMock.getDataFile()).thenReturn(dataFileMock);
    }

    @Test
    @DisplayName("execute should call findFileMetadataHistory with 'canViewUnpublished' as TRUE when user has permission")
    void execute_should_call_history_with_true_when_user_has_permission() throws CommandException {
        // Arrange
        when(permissionsMock.hasPermissionsFor(requestMock.getUser(), datasetMock, EnumSet.of(Permission.ViewUnpublishedDataset)))
                .thenReturn(true);
        when(fileServiceMock.findFileMetadataHistory(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(Collections.emptyList());

        GetFileVersionDifferencesCommand command = new GetFileVersionDifferencesCommand(requestMock, fileMetadataMock, null, null);

        // Act
        command.execute(contextMock);

        // Assert
        ArgumentCaptor<Boolean> canViewUnpublishedCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(fileServiceMock).findFileMetadataHistory(
                eq(DATASET_ID),
                eq(dataFileMock),
                canViewUnpublishedCaptor.capture(),
                eq(null),
                eq(null)
        );
        assertThat(canViewUnpublishedCaptor.getValue()).isTrue();
    }

    @Test
    @DisplayName("execute should call findFileMetadataHistory with 'canViewUnpublished' as FALSE when user lacks permission")
    void execute_should_call_history_with_false_when_user_lacks_permission() throws CommandException {
        // Arrange
        when(permissionsMock.hasPermissionsFor(requestMock.getUser(), datasetMock, EnumSet.of(Permission.ViewUnpublishedDataset)))
                .thenReturn(false);
        when(fileServiceMock.findFileMetadataHistory(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(Collections.emptyList());

        GetFileVersionDifferencesCommand command = new GetFileVersionDifferencesCommand(requestMock, fileMetadataMock, null, null);

        // Act
        command.execute(contextMock);

        // Assert
        ArgumentCaptor<Boolean> canViewUnpublishedCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(fileServiceMock).findFileMetadataHistory(
                eq(DATASET_ID),
                eq(dataFileMock),
                canViewUnpublishedCaptor.capture(),
                eq(null),
                eq(null)
        );
        assertThat(canViewUnpublishedCaptor.getValue()).isFalse();
    }

    @Test
    @DisplayName("execute should pass pagination parameters correctly to findFileMetadataHistory")
    void execute_should_pass_pagination_parameters_correctly() throws CommandException {
        // Arrange
        Integer expectedLimit = 10;
        Integer expectedOffset = 20;
        when(fileServiceMock.findFileMetadataHistory(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(Collections.emptyList());

        GetFileVersionDifferencesCommand command = new GetFileVersionDifferencesCommand(requestMock, fileMetadataMock, expectedLimit, expectedOffset);

        // Act
        command.execute(contextMock);

        // Assert
        verify(fileServiceMock).findFileMetadataHistory(
                eq(DATASET_ID),
                eq(dataFileMock),
                anyBoolean(),
                eq(expectedLimit),
                eq(expectedOffset)
        );
    }

    @Test
    @DisplayName("execute should correctly convert file history to FileVersionDifference list")
    void execute_should_convert_history_to_differences() throws CommandException {
        // Arrange
        // Create mock history: 3 versions of a file's metadata
        FileMetadata fm3 = new FileMetadata(); // Newest
        fm3.setId(3L);
        FileMetadata fm2 = new FileMetadata(); // Middle
        fm2.setId(2L);
        FileMetadata fm1 = new FileMetadata(); // Oldest
        fm1.setId(1L);

        VersionedFileMetadata vfm3 = mock(VersionedFileMetadata.class);
        VersionedFileMetadata vfm2 = mock(VersionedFileMetadata.class);
        VersionedFileMetadata vfm1 = mock(VersionedFileMetadata.class);
        when(vfm3.getFileMetadata()).thenReturn(fm3);
        when(vfm2.getFileMetadata()).thenReturn(fm2);
        when(vfm1.getFileMetadata()).thenReturn(fm1);

        List<VersionedFileMetadata> history = List.of(vfm3, vfm2, vfm1);
        when(fileServiceMock.findFileMetadataHistory(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(history);

        // Mock the logic to find the direct predecessor of each version
        when(fileServiceMock.getPreviousFileMetadata(fm3)).thenReturn(fm2);
        when(fileServiceMock.getPreviousFileMetadata(fm2)).thenReturn(fm1);
        when(fileServiceMock.getPreviousFileMetadata(fm1)).thenReturn(null); // The oldest has no predecessor

        GetFileVersionDifferencesCommand command = new GetFileVersionDifferencesCommand(requestMock, fileMetadataMock, null, null);

        // Act
        List<FileVersionDifference> differences = command.execute(contextMock);

        // Assert
        assertThat(differences).hasSize(3);

        // Check the difference for the newest version (v3 vs v2)
        assertThat(differences.get(0).getNewFileMetadata()).isEqualTo(fm3);
        assertThat(differences.get(0).getOriginalFileMetadata()).isEqualTo(fm2);

        // Check the difference for the middle version (v2 vs v1)
        assertThat(differences.get(1).getNewFileMetadata()).isEqualTo(fm2);
        assertThat(differences.get(1).getOriginalFileMetadata()).isEqualTo(fm1);

        // Check the difference for the oldest version (v1 vs null)
        assertThat(differences.get(2).getNewFileMetadata()).isEqualTo(fm1);
        assertThat(differences.get(2).getOriginalFileMetadata()).isNull();
    }
}
