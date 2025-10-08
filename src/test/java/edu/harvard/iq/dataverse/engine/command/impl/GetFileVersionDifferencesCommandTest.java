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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetFileVersionDifferencesCommandTest {

    @Mock
    private CommandContext contextMock;
    @Mock
    private DataverseRequest requestMock;
    @Mock
    private FileMetadata fileMetadataMock;
    @Mock
    private FileMetadataVersionsHelper versionsHelperMock;
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
        // Mock the context to return our mock services
        when(contextMock.permissions()).thenReturn(permissionsMock);
        when(contextMock.files()).thenReturn(fileServiceMock);

        // Mock the chain of objects from FileMetadata -> Dataset
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

        GetFileVersionDifferencesCommand command = new GetFileVersionDifferencesCommand(requestMock, fileMetadataMock, null, null, versionsHelperMock);

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

        GetFileVersionDifferencesCommand command = new GetFileVersionDifferencesCommand(requestMock, fileMetadataMock, null, null, versionsHelperMock);

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

        GetFileVersionDifferencesCommand command = new GetFileVersionDifferencesCommand(requestMock, fileMetadataMock, expectedLimit, expectedOffset, versionsHelperMock);

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
    void execute_should_convert_history_to_differences(
            // Mocks for a version with METADATA CHANGES
            @Mock VersionedFileMetadata vfm_changed, @Mock FileMetadata currentFm_changed,
            @Mock FileMetadata previousFm_changed, @Mock DatasetVersion version_changed,
            // Mocks for a version with NO METADATA CHANGES
            @Mock VersionedFileMetadata vfm_same, @Mock FileMetadata currentFm_same,
            @Mock FileMetadata previousFm_same, @Mock DatasetVersion version_same,
            // Mocks for underlying file objects
            @Mock DataFile dataFile1, @Mock DataFile dataFile2
    ) throws CommandException {

        // Arrange: Prepare a history list with two entries.

        // --- Scenario 1: A version where file metadata (e.g., the label) has changed. ---
        when(vfm_changed.getFileMetadata()).thenReturn(currentFm_changed);
        when(vfm_changed.getDatasetVersion()).thenReturn(version_changed);
        when(versionsHelperMock.getPreviousFileMetadata(fileMetadataMock, version_changed)).thenReturn(previousFm_changed);
        // Mock the underlying data to trigger a difference in compareMetadata()
        when(currentFm_changed.getLabel()).thenReturn("new_name.txt");
        when(previousFm_changed.getLabel()).thenReturn("old_name.txt");
        when(currentFm_changed.getDataFile()).thenReturn(dataFile1);
        when(previousFm_changed.getDataFile()).thenReturn(dataFile1);

        // --- Scenario 2: A version where the file exists but its metadata is identical to the previous version. ---
        when(vfm_same.getFileMetadata()).thenReturn(currentFm_same);
        when(vfm_same.getDatasetVersion()).thenReturn(version_same);
        when(versionsHelperMock.getPreviousFileMetadata(fileMetadataMock, version_same)).thenReturn(previousFm_same);
        // Mock the underlying data to be identical in compareMetadata()
        when(currentFm_same.isRestricted()).thenReturn(false);
        when(previousFm_same.isRestricted()).thenReturn(false);
        when(currentFm_same.getLabel()).thenReturn("file.txt");
        when(previousFm_same.getLabel()).thenReturn("file.txt");
        when(currentFm_same.getDescription()).thenReturn("description");
        when(previousFm_same.getDescription()).thenReturn("description");
        when(currentFm_same.getProvFreeForm()).thenReturn(null);
        when(previousFm_same.getProvFreeForm()).thenReturn(null);
        when(currentFm_same.getCategoriesByName()).thenReturn(Collections.emptyList());
        when(previousFm_same.getCategoriesByName()).thenReturn(Collections.emptyList());
        when(currentFm_same.getDataFile()).thenReturn(dataFile2);
        when(previousFm_same.getDataFile()).thenReturn(dataFile2);

        // Mock the service to return our prepared history list.
        when(fileServiceMock.findFileMetadataHistory(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(List.of(vfm_changed, vfm_same));

        GetFileVersionDifferencesCommand command = new GetFileVersionDifferencesCommand(requestMock, fileMetadataMock, null, null, versionsHelperMock);

        // Act
        List<FileVersionDifference> result = command.execute(contextMock);

        // Assert
        assertThat(result).hasSize(2);

        // --- Verify the first difference object (METADATA HAS CHANGED) ---
        FileVersionDifference diff1 = result.get(0);
        // Assert the correct FileMetadata objects were passed to the constructor.
        assertThat(diff1.getNewFileMetadata()).isEqualTo(currentFm_changed);
        assertThat(diff1.getOriginalFileMetadata()).isEqualTo(previousFm_changed);
        // Assert that compareMetadata() correctly identified a difference.
        assertThat(diff1.isSame()).isFalse();

        // --- Verify the second difference object (METADATA IS THE SAME) ---
        FileVersionDifference diff2 = result.get(1);
        // Assert the correct FileMetadata objects were passed to the constructor.
        assertThat(diff2.getNewFileMetadata()).isEqualTo(currentFm_same);
        assertThat(diff2.getOriginalFileMetadata()).isEqualTo(previousFm_same);
        // Assert that compareMetadata() correctly identified the metadata as identical.
        assertThat(diff2.isSame()).isTrue();
    }
}
