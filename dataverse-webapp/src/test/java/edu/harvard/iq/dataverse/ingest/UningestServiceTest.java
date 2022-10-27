package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.MapLayerMetadataServiceBean;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileRepository;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.DataTableRepository;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UningestServiceTest {

    @Mock private StorageIO<DataFile> storage;
    @Mock private DataAccess dataAccess;
    @Mock private DataTableRepository dataTableRepository;
    @Mock private DataFileRepository dataFileRepository;
    @Mock private MapLayerMetadataServiceBean mapLayerMetadataService;
    @Mock private DatasetVersionRepository datasetVersionRepository;
    @Mock private DatasetVersionServiceBean datasetVersionService;

    @InjectMocks
    private UningestService service;

    private AuthenticatedUser user = MocksFactory.makeAuthenticatedUser("User", "Qwerty");

    @BeforeEach
    void setUp() {
        service.setDataAccess(dataAccess);
        lenient().when(dataFileRepository.save(any(DataFile.class))).thenAnswer(i -> i.getArgument(0));
    }

    // -------------------- TESTS --------------------

    @Test
    void uningest__tabularFile() throws IOException {
        // given
        DataFile file = prepareDataFile();
        when(dataAccess.getStorageIO(any(DataFile.class))).thenReturn(storage);

        // when
        service.uningest(file, user);

        // then
        verify(dataAccess).getStorageIO(file);
        verify(storage).revertBackupAsAux(anyString());
        verify(dataTableRepository).deleteById(11L);
        assertThat(file.getDataTable()).isNull();
        assertThat(file.getIngestStatus()).isEqualTo(DataFile.INGEST_STATUS_NONE);
        verify(dataFileRepository, atLeastOnce()).save(file);
        verify(mapLayerMetadataService).findMetadataByDatafile(file);
        verify(storage).deleteAllAuxObjects();
        DatasetVersion version = file.getFileMetadata().getDatasetVersion();
        verify(datasetVersionRepository).save(version);
        verify(datasetVersionService).fixMissingUnf(eq(version.getId().toString()), eq(true));
    }

    @Test
    void uningest__nonTabularFile() throws IOException {
        // given
        DataFile file = prepareDataFile();
        file.setDataTable(null);

        // when
        service.uningest(file, user);

        // then
        verify(dataAccess, never()).getStorageIO(any());
        verify(storage, never()).revertBackupAsAux(anyString());
        verify(dataTableRepository, never()).deleteById(11L);
        assertThat(file.getIngestStatus()).isEqualTo(DataFile.INGEST_STATUS_NONE);
        verify(dataFileRepository, atLeastOnce()).save(file);
        verify(mapLayerMetadataService).findMetadataByDatafile(file);
        verify(storage, never()).deleteAllAuxObjects();
        DatasetVersion version = file.getFileMetadata().getDatasetVersion();
        verify(datasetVersionRepository).save(version);
        verify(datasetVersionService).fixMissingUnf(eq(version.getId().toString()), eq(true));
    }

    @Test
    void uningest__throwOnImproperInput() {
        // given
        DataFile multipleVersions = prepareDataFile();
        List<FileMetadata> metadatas = multipleVersions.getFileMetadatas();
        metadatas.add(metadatas.get(0));

        DataFile nonDraftFile = prepareDataFile();
        nonDraftFile.getFileMetadata().getDatasetVersion()
                .setVersionState(DatasetVersion.VersionState.RELEASED);

        // when & then
        assertThatThrownBy(() -> service.uningest(multipleVersions, user))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.uningest(nonDraftFile, user))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------- PRIVATE --------------------

    private DataFile prepareDataFile() {
        Dataset dataset = MocksFactory.makeDataset();
        DatasetVersion version = dataset.getLatestVersion();
        dataset.getLatestVersion().setId(1L);
        dataset.getFiles().forEach(f -> f.getFileMetadata().setDatasetVersion(version));
        DataFile file = dataset.getFiles().get(0);
        DataTable dataTable = new DataTable();
        dataTable.setId(11L);
        file.setDataTable(dataTable);
        return file;
    }
}