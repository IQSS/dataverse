package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Acceptance and unit tests for {@link UpdateDatasetVersionCommand}.
 * Verifies that updating a dataset version does not dirty existing files by
 * updating their modification timestamps unnecessarily (O(N) JPA flush bottleneck).
 *
 * References:
 * https://groups.google.com/g/dataverse-community/c/pzSjF1YPaJw/m/bQWX3W_kBwAJ
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UpdateDatasetVersionCommandTest {

    @Mock
    private CommandContext commandContextMock;
    @Mock
    private PermissionServiceBean permissionServiceMock;
    @Mock
    private DatasetServiceBean datasetServiceMock;
    @Mock
    private DatasetVersionServiceBean datasetVersionServiceMock;
    @Mock
    private DvObjectServiceBean dvObjectServiceMock;
    @Mock
    private DatasetFieldServiceBean datasetFieldServiceMock;
    @Mock
    private SystemConfig systemConfigMock;
    @Mock
    private EntityManager entityManagerMock;
    @Mock
    private PidProvider pidProviderMock;

    private AuthenticatedUser testUser;
    private DataverseRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = MocksFactory.makeAuthenticatedUser("Dataverse", "Admin");
        testRequest = MocksFactory.makeRequest(testUser);

        when(commandContextMock.permissions()).thenReturn(permissionServiceMock);
        when(commandContextMock.datasets()).thenReturn(datasetServiceMock);
        when(commandContextMock.datasetVersion()).thenReturn(datasetVersionServiceMock);
        when(commandContextMock.dvObjects()).thenReturn(dvObjectServiceMock);
        when(commandContextMock.dsField()).thenReturn(datasetFieldServiceMock);
        when(commandContextMock.systemConfig()).thenReturn(systemConfigMock);
        when(commandContextMock.em()).thenReturn(entityManagerMock);

        when(entityManagerMock.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(dvObjectServiceMock.getEffectivePidGenerator(any())).thenReturn(pidProviderMock);
        when(pidProviderMock.registerWhenPublished()).thenReturn(true);
        when(systemConfigMock.isFilePIDsEnabledForCollection(any())).thenReturn(false);
        when(datasetFieldServiceMock.getCVocConf(anyBoolean())).thenReturn(Collections.emptyMap());

        DatasetLock lock = new DatasetLock(DatasetLock.Reason.EditInProgress, testUser);
        when(datasetServiceMock.addDatasetLock(any(), any(), any(), any())).thenReturn(lock);
    }

    @Test
    @DisplayName("Adding a new file must NOT update modificationTime on existing files")
    void testAddingFileDoesNotModifyExistingFilesTimestamps() throws Exception {
        Dataset dataset = new Dataset();
        dataset.setId(100L);
        dataset.setOwner(MocksFactory.makeDataverse());

        DatasetVersion editVersion = dataset.getOrCreateEditVersion();
        editVersion.setId(200L);
        editVersion.setDatasetFields(new ArrayList<>());
        when(datasetVersionServiceMock.find(any())).thenReturn(editVersion);

        // 1. Existing file uploaded in the past
        Timestamp historicalTimestamp = Timestamp.from(Instant.now().minus(7, ChronoUnit.DAYS));
        DataFile existingFile = new DataFile();
        existingFile.setId(1L);
        existingFile.setOwner(dataset);
        existingFile.setCreateDate(historicalTimestamp);
        existingFile.setModificationTime(historicalTimestamp);
        existingFile.setCreator(testUser);

        FileMetadata existingFmd = new FileMetadata();
        existingFmd.setId(10L);
        existingFmd.setDataFile(existingFile);
        existingFmd.setDatasetVersion(editVersion);
        existingFile.setFileMetadatas(new ArrayList<>(List.of(existingFmd)));
        editVersion.getFileMetadatas().add(existingFmd);

        // 2. New file being uploaded
        DataFile newFile = new DataFile();
        newFile.setId(2L);
        newFile.setOwner(dataset);
        newFile.setCreateDate(null);
        newFile.setModificationTime(null);
        newFile.setCreator(null);

        FileMetadata newFmd = new FileMetadata();
        newFmd.setId(20L);
        newFmd.setDataFile(newFile);
        newFmd.setDatasetVersion(editVersion);
        newFile.setFileMetadatas(new ArrayList<>(List.of(newFmd)));
        editVersion.getFileMetadatas().add(newFmd);

        dataset.setFiles(new ArrayList<>(List.of(existingFile, newFile)));

        // Execute UpdateDatasetVersionCommand
        UpdateDatasetVersionCommand cmd = new UpdateDatasetVersionCommand(dataset, testRequest, editVersion);
        cmd.setValidateLenient(true);
        cmd.execute(commandContextMock);

        // ASSERTIONS:
        // New file must have creation date, modification time, and creator set
        assertNotNull(newFile.getCreateDate(), "New file createDate must be set");
        assertNotNull(newFile.getModificationTime(), "New file modificationTime must be set");
        assertEquals(testUser, newFile.getCreator(), "New file creator must be set to the executing user");

        // Existing file modification time MUST NOT be changed to command timestamp!
        assertEquals(historicalTimestamp, existingFile.getModificationTime(),
                "Existing file modificationTime must not be overwritten when updating dataset version");
    }

    @Test
    @DisplayName("Updating variable metadata must update modificationTime ONLY on target file")
    void testUpdatingVariableMetadataOnlyModifiesTargetFile() throws Exception {
        Dataset dataset = new Dataset();
        dataset.setId(101L);
        dataset.setOwner(MocksFactory.makeDataverse());

        DatasetVersion editVersion = dataset.getOrCreateEditVersion();
        editVersion.setId(201L);
        editVersion.setDatasetFields(new ArrayList<>());
        when(datasetVersionServiceMock.find(any())).thenReturn(editVersion);

        Timestamp historicalTimestamp = Timestamp.from(Instant.now().minus(5, ChronoUnit.DAYS));

        // Target file whose variable metadata is being updated
        DataFile targetFile = new DataFile();
        targetFile.setId(11L);
        targetFile.setOwner(dataset);
        targetFile.setCreateDate(historicalTimestamp);
        targetFile.setModificationTime(historicalTimestamp);

        FileMetadata targetFmd = new FileMetadata();
        targetFmd.setId(110L);
        targetFmd.setDataFile(targetFile);
        targetFmd.setDatasetVersion(editVersion);
        targetFile.setFileMetadatas(new ArrayList<>(List.of(targetFmd)));
        editVersion.getFileMetadatas().add(targetFmd);

        // Untouched other file
        DataFile untouchedFile = new DataFile();
        untouchedFile.setId(12L);
        untouchedFile.setOwner(dataset);
        untouchedFile.setCreateDate(historicalTimestamp);
        untouchedFile.setModificationTime(historicalTimestamp);

        FileMetadata untouchedFmd = new FileMetadata();
        untouchedFmd.setId(120L);
        untouchedFmd.setDataFile(untouchedFile);
        untouchedFmd.setDatasetVersion(editVersion);
        untouchedFile.setFileMetadatas(new ArrayList<>(List.of(untouchedFmd)));
        editVersion.getFileMetadatas().add(untouchedFmd);

        dataset.setFiles(new ArrayList<>(List.of(targetFile, untouchedFile)));

        // Execute command targeting targetFmd
        UpdateDatasetVersionCommand cmd = new UpdateDatasetVersionCommand(dataset, testRequest, targetFmd);
        cmd.setValidateLenient(true);
        cmd.execute(commandContextMock);

        // ASSERTIONS:
        // Untouched file modificationTime must remain historical
        assertEquals(historicalTimestamp, untouchedFile.getModificationTime(),
                "Untouched file modificationTime must remain unchanged");

        // Target file modificationTime must be updated
        assertNotNull(targetFile.getModificationTime());
        assertEquals(cmd.getTimestamp(), targetFile.getModificationTime(),
                "Target file modificationTime must be updated to command timestamp");
    }

    @Test
    @DisplayName("Existing file with null modificationTime should be safely initialized")
    void testHealsNullModificationTimeOnExistingFiles() throws Exception {
        Dataset dataset = new Dataset();
        dataset.setId(102L);
        dataset.setOwner(MocksFactory.makeDataverse());

        DatasetVersion editVersion = dataset.getOrCreateEditVersion();
        editVersion.setId(202L);
        editVersion.setDatasetFields(new ArrayList<>());
        when(datasetVersionServiceMock.find(any())).thenReturn(editVersion);

        Timestamp historicalTimestamp = Timestamp.from(Instant.now().minus(3, ChronoUnit.DAYS));
        DataFile fileWithNullModTime = new DataFile();
        fileWithNullModTime.setId(30L);
        fileWithNullModTime.setOwner(dataset);
        fileWithNullModTime.setCreateDate(historicalTimestamp);
        fileWithNullModTime.setModificationTime(null);

        FileMetadata fmd = new FileMetadata();
        fmd.setId(300L);
        fmd.setDataFile(fileWithNullModTime);
        fmd.setDatasetVersion(editVersion);
        fileWithNullModTime.setFileMetadatas(new ArrayList<>(List.of(fmd)));
        editVersion.getFileMetadatas().add(fmd);

        dataset.setFiles(new ArrayList<>(List.of(fileWithNullModTime)));

        UpdateDatasetVersionCommand cmd = new UpdateDatasetVersionCommand(dataset, testRequest, editVersion);
        cmd.setValidateLenient(true);
        cmd.execute(commandContextMock);

        assertNotNull(fileWithNullModTime.getModificationTime(),
                "Null modificationTime on existing file should be safely initialized");
        assertEquals(cmd.getTimestamp(), fileWithNullModTime.getModificationTime());
    }
}
