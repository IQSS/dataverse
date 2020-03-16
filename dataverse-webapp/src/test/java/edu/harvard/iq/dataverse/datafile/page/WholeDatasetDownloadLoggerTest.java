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

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class WholeDatasetDownloadLoggerTest {

    private static long DATASET_ID = 100L;
    private static long DATAFILE_BASE_ID = 1000L;
    private static long DATASET_VERSION_BASE_ID = 2000L;

    private enum Files {
        FIRST, SECOND
    }

    @Mock
    private DownloadDatasetLogService downloadDatasetLogService;

    @InjectMocks
    private WholeDatasetDownloadLogger wholeDatasetDownloadLogger;

    @Test
    public void shouldDoNothingIfNullInput() {
        // given
        List<DataFile> filesDownloaded = null;

        // when
        wholeDatasetDownloadLogger.incrementLogIfDownloadingWholeDataset(filesDownloaded);

        // then
        verify(downloadDatasetLogService, never()).logWholeSetDownload(anyLong());
    }

    @Test
    public void shouldDoNothingIfEmptyInput() {
        // given
        List<DataFile> filesDownloaded = Collections.emptyList();

        // when
        wholeDatasetDownloadLogger.incrementLogIfDownloadingWholeDataset(filesDownloaded);

        // then
        verify(downloadDatasetLogService, never()).logWholeSetDownload(anyLong());
    }

    @Test
    public void shouldDoNothingIfCannotExtractDatasetId() {
        // given
        List<DataFile> filesDownloaded = Collections.singletonList(new DataFile());

        // when
        wholeDatasetDownloadLogger.incrementLogIfDownloadingWholeDataset(filesDownloaded);

        // then
        verify(downloadDatasetLogService, never()).logWholeSetDownload(anyLong());
    }

    @Test
    public void shouldIncrementCountWhenDownloadingAllFiles_oneVersion() {
        // given
        List<DataFile> fileList = createDatafileWithSingleVersionInDataset();

        // when
        wholeDatasetDownloadLogger.incrementLogIfDownloadingWholeDataset(fileList);

        // then
        verify(downloadDatasetLogService, times(1)).logWholeSetDownload(anyLong());
    }

    @Test
    public void shouldIncrementCountWhenDownloadingAllFiles_manyVersions() {
        // given
        Map<Files, DataFile> datafiles = createDatafilesWithManyVersionsInDataset();

        // Take a file that is present in both versions and is the only file in one of the versions
        DataFile file = datafiles.get(Files.FIRST);

        // when
        wholeDatasetDownloadLogger.incrementLogIfDownloadingWholeDataset(Collections.singletonList(file));

        // then
        verify(downloadDatasetLogService, times(1)).logWholeSetDownload(anyLong());
    }

    @Test
    public void shouldNotIncrementCountWhenDownloadedFilesDoNotMatchWholeListFromAnyVersion() {
        // given
        Map<Files, DataFile> datafiles = createDatafilesWithManyVersionsInDataset();

        // Take a file that is present only in second version, but is not the only file in that version
        DataFile file = datafiles.get(Files.SECOND);

        // when
        wholeDatasetDownloadLogger.incrementLogIfDownloadingWholeDataset(Collections.singletonList(file));

        // then
        verify(downloadDatasetLogService, never()).logWholeSetDownload(anyLong());
    }

    private List<DataFile> createDatafileWithSingleVersionInDataset() {
        Dataset dataset = new Dataset();
        dataset.setId(DATASET_ID);

        DataFile dataFile = new DataFile();
        dataFile.setId(DATAFILE_BASE_ID + 1);
        dataFile.setOwner(dataset);

        FileMetadata fileMetadata = new FileMetadata();

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setId(DATASET_VERSION_BASE_ID + 1);
        datasetVersion.setVersionState(VersionState.RELEASED);
        datasetVersion.setFileMetadatas(Collections.singletonList(fileMetadata));

        dataset.setVersions(Collections.singletonList(datasetVersion));

        fileMetadata.setDatasetVersion(datasetVersion);
        fileMetadata.setDataFile(dataFile);

        return Collections.singletonList(dataFile);
    }

    private Map<Files, DataFile> createDatafilesWithManyVersionsInDataset() {
        Map<Files, DataFile> result = new EnumMap<>(Files.class);

        Dataset dataset = new Dataset();
        dataset.setId(DATASET_ID);

        DataFile dataFile1 = new DataFile();
        dataFile1.setId(DATAFILE_BASE_ID + 1);
        dataFile1.setOwner(dataset);
        result.put(Files.FIRST, dataFile1);

        DataFile dataFile2 = new DataFile();
        dataFile2.setId(DATAFILE_BASE_ID + 2);
        dataFile2.setOwner(dataset);
        result.put(Files.SECOND, dataFile2);

        FileMetadata fileMetadata1_1 = new FileMetadata();
        FileMetadata fileMetadata1_2 = new FileMetadata();
        FileMetadata fileMetadata2_1 = new FileMetadata();

        // 1st version: file 1 & file 2
        DatasetVersion datasetVersion1 = new DatasetVersion();
        datasetVersion1.setId(DATASET_VERSION_BASE_ID + 1);
        datasetVersion1.setVersionState(VersionState.RELEASED);
        List<FileMetadata> files1 = Stream.of(fileMetadata1_1, fileMetadata2_1).collect(toList());
        datasetVersion1.setFileMetadatas(files1);

        // 2nd version: file 1
        DatasetVersion datasetVersion2 = new DatasetVersion();
        datasetVersion2.setId(DATASET_VERSION_BASE_ID + 2);
        datasetVersion2.setVersionState(VersionState.RELEASED);
        List<FileMetadata> files2 = Stream.of(fileMetadata1_2).collect(toList());
        datasetVersion2.setFileMetadatas(files2);

        dataset.setVersions(Stream.of(datasetVersion1, datasetVersion2)
                .collect(toList()));

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