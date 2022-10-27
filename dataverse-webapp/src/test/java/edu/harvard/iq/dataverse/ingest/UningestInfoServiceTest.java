package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UningestInfoServiceTest {

    private DataFileServiceBean dataFileService = new DataFileServiceBean();
    private UningestInfoService service = new UningestInfoService(dataFileService);


    @ParameterizedTest
    @CsvSource({
            "true, text/xml-graphml, true, B, false",
            "true, text/csv, false, A, true",
            "true, text/csv, true, A, false",
            "false, text/csv, true, A, true",
            "false, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, true, D, false",
            "false, text/tsv, true, A, true"
    })
    void listUningestableFiles_AND_hasUningestableFiles__variousFiles(boolean expectedEmpty, String mime, boolean singleVersion,
                                             char ingestStatus, boolean hasDataTable) {
        // given
        Dataset dataset = prepareDatasetWithDataFile(mime, singleVersion, ingestStatus, hasDataTable ? new DataTable() : null);

        // when
        List<DataFile> files = service.listUningestableFiles(dataset);
        boolean hasUningestableFiles = service.hasUningestableFiles(dataset);


        // then
        assertThat(files).hasSize(expectedEmpty ? 0 : 1);
        assertThat(hasUningestableFiles).isEqualTo(!expectedEmpty);
    }

    @Test
    void listUningestableFiles_AND_hasUningestableFiles__uinngestableDatasets_releasedDataset() {
        // given
        Dataset releasedDataset = prepareDatasetWithDataFile("text/csv", true, 'D', null);
        releasedDataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.RELEASED);

        // when
        List<DataFile> files = service.listUningestableFiles(releasedDataset);
        boolean hasUningestableFiles = service.hasUningestableFiles(releasedDataset);

        // then
        assertThat(files).isEmpty();
        assertThat(hasUningestableFiles).isFalse();
    }

    @Test
    void listUningestableFiles_AND_hasUningestableFiles__uinngestableDatasets_emptyDataset() {
        // given
        Dataset noFilesDataset = new Dataset();

        // when
        List<DataFile> files = service.listUningestableFiles(noFilesDataset);
        boolean hasUningestableFiles = service.hasUningestableFiles(noFilesDataset);

        // then
        assertThat(files).isEmpty();
        assertThat(hasUningestableFiles).isFalse();
    }

    // -------------------- PRIVATE --------------------

    private Dataset prepareDatasetWithDataFile(String mime, boolean singleVersion, char ingestStatus, DataTable dataTable) {
        Dataset dataset = MocksFactory.makeDataset();
        DatasetVersion version = dataset.getLatestVersion();
        DataFile file = dataset.getFiles().get(0);
        FileMetadata fileMetadata = file.getFileMetadata();
        fileMetadata.setDatasetVersion(version);
        file.setContentType(mime);
        file.setIngestStatus(ingestStatus);
        file.setDataTable(dataTable);
        if (!singleVersion) {
            List<FileMetadata> metadatas = file.getFileMetadatas();
            metadatas.add(metadatas.get(0));
        }
        dataset.getFiles().removeIf(f -> !file.equals(f));
        return dataset;
    }
}