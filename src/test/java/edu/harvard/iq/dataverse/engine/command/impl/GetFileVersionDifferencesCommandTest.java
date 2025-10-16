package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private DatasetVersionServiceBean datasetVersionServiceMock;
    @Mock
    private Dataset datasetMock;
    @Mock
    private DatasetVersion datasetVersionMock;
    @Mock
    private DataFile dataFileMock;

    private static final Long DATASET_ID = 1L;

    @BeforeEach
    void setUp() {
        when(fileMetadataMock.getDataFile()).thenReturn(dataFileMock);
    }

    /**
     * Helper method to set up mocks required for tests that execute the full command logic.
     */
    private void setupMocksForSuccessfulExecution() {
        when(contextMock.permissions()).thenReturn(permissionsMock);
        when(contextMock.files()).thenReturn(fileServiceMock);
        when(fileMetadataMock.getDatasetVersion()).thenReturn(datasetVersionMock);
        when(datasetVersionMock.getDataset()).thenReturn(datasetMock);
        when(datasetMock.getId()).thenReturn(DATASET_ID);
    }

    @Test
    @DisplayName("execute should call findFileMetadataHistory with 'canViewUnpublished' as TRUE when user has permission")
    void execute_should_call_history_with_true_when_user_has_permission() throws CommandException {
        // Arrange
        setupMocksForSuccessfulExecution();
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
        setupMocksForSuccessfulExecution();
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
        setupMocksForSuccessfulExecution();
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
        setupMocksForSuccessfulExecution();
        // This test needs an extra mock because it processes items from the history list
        when(contextMock.datasetVersion()).thenReturn(datasetVersionServiceMock);

        // Create mock history: 3 versions of a file's metadata
        FileMetadata fm3 = new FileMetadata(); // Newest
        fm3.setId(3L);
        fm3.setDatasetVersion(datasetVersionMock);
        FileMetadata fm2 = new FileMetadata(); // Middle
        fm2.setId(2L);
        fm2.setDatasetVersion(datasetVersionMock);
        FileMetadata fm1 = new FileMetadata(); // Oldest
        fm1.setId(1L);
        fm1.setDatasetVersion(datasetVersionMock);

        VersionedFileMetadata vfm3 = mock(VersionedFileMetadata.class);
        VersionedFileMetadata vfm2 = mock(VersionedFileMetadata.class);
        VersionedFileMetadata vfm1 = mock(VersionedFileMetadata.class);
        when(vfm3.getFileMetadata()).thenReturn(fm3);
        when(vfm2.getFileMetadata()).thenReturn(fm2);
        when(vfm1.getFileMetadata()).thenReturn(fm1);

        List<VersionedFileMetadata> history = List.of(vfm3, vfm2, vfm1);
        when(fileServiceMock.findFileMetadataHistory(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(history);

        when(fileServiceMock.getPreviousFileMetadata(any(FileMetadata.class))).thenAnswer(
                invocation -> {
                    FileMetadata current = invocation.getArgument(0);
                    if (current != null && current.getId().equals(3L)) {
                        return fm2;
                    }
                    if (current != null && current.getId().equals(2L)) {
                        return fm1;
                    }
                    return null;
                }
        );

        // Mock the logic to retrieve contributor names
        when(datasetVersionServiceMock.getContributorsNames(any(DatasetVersion.class))).thenReturn(Collections.emptyList().toString());

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

        // Verify setting contributor for each file metadata
        verify(datasetVersionServiceMock, times(3)).getContributorsNames(datasetVersionMock);
    }

    @Test
    @DisplayName("execute should throw InvalidCommandArgumentsException for negative limit")
    void execute_should_throw_exception_for_negative_limit() {
        // Arrange
        Integer invalidLimit = -1;
        GetFileVersionDifferencesCommand command = new GetFileVersionDifferencesCommand(requestMock, fileMetadataMock, invalidLimit, 0);

        // Act & Assert
        assertThrows(InvalidCommandArgumentsException.class, () -> command.execute(contextMock));
    }

    @Test
    @DisplayName("execute should throw InvalidCommandArgumentsException for negative offset")
    void execute_should_throw_exception_for_negative_offset() {
        // Arrange
        Integer invalidOffset = -1;
        GetFileVersionDifferencesCommand command = new GetFileVersionDifferencesCommand(requestMock, fileMetadataMock, 10, invalidOffset);

        // Act & Assert
        assertThrows(InvalidCommandArgumentsException.class, () -> command.execute(contextMock));
    }
}
