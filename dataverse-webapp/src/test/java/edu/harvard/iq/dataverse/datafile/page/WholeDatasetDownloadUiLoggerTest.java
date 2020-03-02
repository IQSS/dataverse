package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.dataset.DownloadDatasetLogService;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class WholeDatasetDownloadUiLoggerTest {

    private static long DATASET_ID = 100L;
    private static long DATAFILE_BASE_ID = 1000L;
    private static long DATASET_VERSION_BASE_ID = 2000L;

    @Mock
    private DownloadDatasetLogService downloadDatasetLogService;

    @InjectMocks
    private WholeDatasetDownloadUiLogger wholeDatasetDownloadUiLogger;

    @Test
    public void shouldDoNothingIfNullInput() {
        // given
        List<FileMetadata> filesDownloaded = null;

        // when
        wholeDatasetDownloadUiLogger.incrementLogIfDownloadingWholeDataset(filesDownloaded);

        // then
        verify(downloadDatasetLogService, never()).incrementDownloadCountForDataset(anyLong());
    }

    @Test
    public void shouldDoNothingIfEmptyInput() {
        // given
        List<FileMetadata> filesDownloaded = Collections.emptyList();

        // when
        wholeDatasetDownloadUiLogger.incrementLogIfDownloadingWholeDataset(filesDownloaded);

        // then
        verify(downloadDatasetLogService, never()).incrementDownloadCountForDataset(anyLong());
    }

    @Test
    public void shouldDoNothingIfCannotExtractDatasetId() {
        // given
        List<FileMetadata> filesDownloaded = Collections.singletonList(new FileMetadata());

        // when
        wholeDatasetDownloadUiLogger.incrementLogIfDownloadingWholeDataset(filesDownloaded);

        // then
        verify(downloadDatasetLogService, never()).incrementDownloadCountForDataset(anyLong());
    }

    @Test
    public void shouldIncrementCountWhenDownloadingAllFiles_oneVersion() {
        // given
        List<FileMetadata> filesDownloaded = createMetadataWithSingleFileInSingleVersion();

        // when
        wholeDatasetDownloadUiLogger.incrementLogIfDownloadingWholeDataset(filesDownloaded);

        // then
        verify(downloadDatasetLogService, times(1)).incrementDownloadCountForDataset(anyLong());
    }

    @Test
    public void shouldIncrementCountWhenDownloadingAllFiles_manyVersions() {
        // given
        Map<Long, List<FileMetadata>> versionsWithFiles = createMetadataWithManyFileInManyVersion();

        // Get files from first version
        List<FileMetadata> filesFromFirstVersion = versionsWithFiles.get(DATASET_VERSION_BASE_ID + 1);

        // Take only the file that is present in both versions, but is not the only file in the version
        // form which is being taken (however it is the only file in other version so we should have a match)
        List<FileMetadata> filesDownloaded = filesFromFirstVersion.stream()
                .filter(m -> m.getDataFile().getId() == DATAFILE_BASE_ID + 1)
                .collect(toList());

        // when
        wholeDatasetDownloadUiLogger.incrementLogIfDownloadingWholeDataset(filesDownloaded);

        // then
        verify(downloadDatasetLogService, times(1)).incrementDownloadCountForDataset(anyLong());
    }

    @Test
    public void shouldNotIncrementCountWhenDownloadedFilesDoNotMatchWholeListFromAnyVersion() {
        // given
        Map<Long, List<FileMetadata>> versionsWithFiles = createMetadataWithManyFileInManyVersion();

        // Get files from first version
        List<FileMetadata> filesFromFirstVersion = versionsWithFiles.get(DATASET_VERSION_BASE_ID + 1);

        // Take only the file that is present only in second version, but is not the only file in that version
        List<FileMetadata> filesDownloaded = filesFromFirstVersion.stream()
                .filter(m -> m.getDataFile().getId() != DATAFILE_BASE_ID + 1)
                .collect(toList());

        // when
        wholeDatasetDownloadUiLogger.incrementLogIfDownloadingWholeDataset(filesDownloaded);

        // then
        verify(downloadDatasetLogService, never()).incrementDownloadCountForDataset(anyLong());
    }

    private List<FileMetadata> createMetadataWithSingleFileInSingleVersion() {
        List<FileMetadata> result = new ArrayList<>();
        FileMetadata fileMetadata = new FileMetadata();


        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setId(DATASET_VERSION_BASE_ID + 1);
        datasetVersion.setVersionState(VersionState.RELEASED);
        datasetVersion.setFileMetadatas(result);

        Dataset dataset = new Dataset();
        dataset.setId(DATASET_ID);
        dataset.setVersions(Collections.singletonList(datasetVersion));

        DataFile dataFile = new DataFile();
        dataFile.setId(DATAFILE_BASE_ID + 1);
        dataFile.setOwner(dataset);

        fileMetadata.setDatasetVersion(datasetVersion);
        fileMetadata.setDataFile(dataFile);

        result.add(fileMetadata);

        return result;
    }

    private Map<Long, List<FileMetadata>> createMetadataWithManyFileInManyVersion() {
        Map<Long, List<FileMetadata>> result = new HashMap<>();
        FileMetadata fileMetadata1_1 = new FileMetadata();
        FileMetadata fileMetadata1_2 = new FileMetadata();
        FileMetadata fileMetadata2_1 = new FileMetadata();

        // 1st version: file 1 & file 2
        DatasetVersion datasetVersion1 = new DatasetVersion();
        datasetVersion1.setId(DATASET_VERSION_BASE_ID + 1);
        datasetVersion1.setVersionState(VersionState.RELEASED);
        List<FileMetadata> files1 = Stream.of(fileMetadata1_1, fileMetadata2_1).collect(toList());
        datasetVersion1.setFileMetadatas(files1);
        result.put(DATASET_VERSION_BASE_ID + 1, files1);

        // 2nd version: file 1
        DatasetVersion datasetVersion2 = new DatasetVersion();
        datasetVersion2.setId(DATASET_VERSION_BASE_ID + 2);
        datasetVersion2.setVersionState(VersionState.RELEASED);
        List<FileMetadata> files2 = Stream.of(fileMetadata1_2).collect(toList());
        datasetVersion2.setFileMetadatas(files2);
        result.put(DATASET_VERSION_BASE_ID + 2, files2);

        Dataset dataset = new Dataset();
        dataset.setId(DATASET_ID);
        dataset.setVersions(Stream.of(datasetVersion1, datasetVersion2)
                .collect(toList()));

        DataFile dataFile1 = new DataFile();
        dataFile1.setId(DATAFILE_BASE_ID + 1);
        dataFile1.setOwner(dataset);

        DataFile dataFile2 = new DataFile();
        dataFile2.setId(DATAFILE_BASE_ID + 2);
        dataFile2.setOwner(dataset);

        // Metadata 1_1 and 1_2 points to the same file, but have different versions
        fileMetadata1_1.setDatasetVersion(datasetVersion1);
        fileMetadata1_1.setDataFile(dataFile1);

        fileMetadata1_2.setDatasetVersion(datasetVersion2);
        fileMetadata1_2.setDataFile(dataFile1);

        fileMetadata2_1.setDatasetVersion(datasetVersion1);
        fileMetadata2_1.setDataFile(dataFile2);

        return result;
    }

}