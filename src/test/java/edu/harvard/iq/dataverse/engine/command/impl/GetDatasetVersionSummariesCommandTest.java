package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.datasetversionsummaries.DatasetVersionSummary;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDatasetVersionSummariesCommandTest {

    @Mock
    private CommandContext contextMock;
    @Mock
    private DataverseRequest requestMock;
    @Mock
    private Dataset datasetMock;
    @Mock
    private PermissionServiceBean permissionsMock;
    @Mock
    private DatasetVersionServiceBean versionServiceMock;
    @Mock
    private AuthenticatedUser userMock;
    @Mock
    private DatasetVersion versionMock;

    private static final Long DATASET_ID = 42L;

    @BeforeEach
    void setUp() {
        // Mock the context to return our mock services
        when(contextMock.permissions()).thenReturn(permissionsMock);
        when(contextMock.datasetVersion()).thenReturn(versionServiceMock);

        // Mock the request to return a user
        when(requestMock.getUser()).thenReturn(userMock);

        // Mock the dataset to have a specific ID
        when(datasetMock.getId()).thenReturn(DATASET_ID);
    }

    @Test
    @DisplayName("execute should call findVersions with 'canViewUnpublished' as TRUE when user has permission")
    void execute_should_call_findVersions_with_true_when_user_has_permission() throws CommandException {
        // Arrange
        when(permissionsMock.hasPermissionsFor(requestMock.getUser(), datasetMock, EnumSet.of(Permission.ViewUnpublishedDataset)))
                .thenReturn(true);
        when(versionServiceMock.findVersions(anyLong(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        GetDatasetVersionSummariesCommand command = new GetDatasetVersionSummariesCommand(requestMock, datasetMock, null, null);

        // Act
        command.execute(contextMock);

        // Assert
        ArgumentCaptor<Boolean> canViewUnpublishedCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(versionServiceMock).findVersions(
                eq(DATASET_ID),
                eq(null),
                eq(null),
                canViewUnpublishedCaptor.capture(),
                eq(true)
        );
        assertThat(canViewUnpublishedCaptor.getValue()).isTrue();
    }

    @Test
    @DisplayName("execute should call findVersions with 'canViewUnpublished' as FALSE when user lacks permission")
    void execute_should_call_findVersions_with_false_when_user_lacks_permission() throws CommandException {
        // Arrange
        when(permissionsMock.hasPermissionsFor(requestMock.getUser(), datasetMock, EnumSet.of(Permission.ViewUnpublishedDataset)))
                .thenReturn(false);
        when(versionServiceMock.findVersions(anyLong(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        GetDatasetVersionSummariesCommand command = new GetDatasetVersionSummariesCommand(requestMock, datasetMock, null, null);

        // Act
        command.execute(contextMock);

        // Assert
        ArgumentCaptor<Boolean> canViewUnpublishedCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(versionServiceMock).findVersions(
                eq(DATASET_ID),
                eq(null),
                eq(null),
                canViewUnpublishedCaptor.capture(),
                eq(true)
        );
        assertThat(canViewUnpublishedCaptor.getValue()).isFalse();
    }

    @Test
    @DisplayName("execute should pass pagination parameters correctly to findVersions")
    void execute_should_pass_pagination_parameters_correctly() throws CommandException {
        // Arrange
        Integer expectedLimit = 10;
        Integer expectedOffset = 20;
        when(versionServiceMock.findVersions(anyLong(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        GetDatasetVersionSummariesCommand command = new GetDatasetVersionSummariesCommand(requestMock, datasetMock, expectedLimit, expectedOffset);

        // Act
        command.execute(contextMock);

        // Assert
        verify(versionServiceMock).findVersions(
                eq(DATASET_ID),
                eq(expectedOffset),
                eq(expectedLimit),
                anyBoolean(),
                eq(true)
        );
    }

    @Test
    @DisplayName("execute should correctly convert DatasetVersion list to DatasetVersionSummary list")
    void execute_should_convert_versions_to_summaries() throws CommandException {
        // Arrange: Mock the service to return a list with one version
        when(versionServiceMock.findVersions(anyLong(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(versionMock));

        // Arrange: Prepare a dummy summary object
        DatasetVersionSummary dummySummary = new DatasetVersionSummary(1L, "V1", null, null, null, null);

        // Arrange: Mock the static 'from' method to return our dummy summary
        try (MockedStatic<DatasetVersionSummary> mockedStatic = Mockito.mockStatic(DatasetVersionSummary.class)) {
            mockedStatic.when(() -> DatasetVersionSummary.from(versionMock))
                    .thenReturn(Optional.of(dummySummary));

            GetDatasetVersionSummariesCommand command = new GetDatasetVersionSummariesCommand(requestMock, datasetMock, null, null);

            // Act
            List<DatasetVersionSummary> result = command.execute(contextMock);

            // Assert
            assertThat(result)
                    .isNotNull()
                    .hasSize(1)
                    .containsExactly(dummySummary);
        }
    }
}
