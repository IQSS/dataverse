package edu.harvard.iq.dataverse.util.bagit.data;

import edu.harvard.iq.dataverse.DataFile;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider.InputStreamProvider;

/**
 *
 * @author adaybujeda
 */
public class DataFileDataProviderTest {

    private static final String EMPTY_DRIVER = "";
    private static final String EMPTY_DIRECTORY = "";
    private static final String FIXTURE_DIRECTORY = "src/test/resources/bagit/data";
    private static final String NAME = UUID.randomUUID().toString();

    @Test
    public void getName_should_return_configured_name() {
        DataFileDataProvider target = new DataFileDataProvider(NAME, Collections.emptyList());
        MatcherAssert.assertThat(target.getName(), Matchers.is(NAME));
    }

    @Test
    public void getFilePaths_should_iterate_through_all_datafiles() {
        List<DataFile> dataFiles = createDataFiles(EMPTY_DRIVER, "dir", "file1.txt", "file2.csv", "file3.py");

        DataFileDataProvider target = new DataFileDataProvider(NAME, dataFiles);
        List<Path> result = target.getFilePaths();

        MatcherAssert.assertThat(result.size(), Matchers.is(3));
        MatcherAssert.assertThat(result, Matchers.hasItem(Path.of("dir","file1.txt")));
        MatcherAssert.assertThat(result, Matchers.hasItem(Path.of("dir","file2.csv")));
        MatcherAssert.assertThat(result, Matchers.hasItem(Path.of("dir","file3.py")));
    }

    @Test
    public void getInputStreamProvider_should_return_empty_when_filePath_is_not_found() {
        Path filePath = Path.of(UUID.randomUUID().toString());

        DataFileDataProvider target = new DataFileDataProvider(NAME, Collections.emptyList());
        Optional<InputStreamProvider> result = target.getInputStreamProvider(filePath);

        MatcherAssert.assertThat(result.isEmpty(), Matchers.is(true));
    }

    @Test
    public void getInputStreamProvider_should_return_empty_when_datafile_do_not_use_tmp_driver() throws Exception {
        Path filePath = Path.of(UUID.randomUUID().toString());
        List<DataFile> dataFiles = createDataFiles("file://", EMPTY_DIRECTORY, filePath.toString());

        DataFileDataProvider target = new DataFileDataProvider(NAME, dataFiles);
        Optional<InputStreamProvider> result = target.getInputStreamProvider(filePath);

        MatcherAssert.assertThat(result.isEmpty(), Matchers.is(true));
    }

    @Test
    public void getInputStreamProvider_should_return_empty_when_filePath_is_found_but_file_do_no_exits() throws Exception {
        Path missingFile = Path.of(UUID.randomUUID().toString());
        List<DataFile> dataFiles = createDataFiles(EMPTY_DRIVER, EMPTY_DIRECTORY, missingFile.toString());

        DataFileDataProvider target = new DataFileDataProvider(NAME, dataFiles);
        Optional<InputStreamProvider> result = target.getInputStreamProvider(missingFile);

        MatcherAssert.assertThat(result.isEmpty(), Matchers.is(true));
    }

    @Test
    public void getInputStreamProvider_should_return_inputStream_when_filePath_is_found_and_file_exits() throws Exception {
        FileUtilWrapper fileUtilMock = Mockito.mock(FileUtilWrapper.class);
        Mockito.when(fileUtilMock.getFilesTempDirectory()).thenReturn(FIXTURE_DIRECTORY);

        String existingFileName = "DataFileDataProviderTest.txt";
        List<DataFile> dataFiles = createDataFiles(EMPTY_DRIVER, EMPTY_DIRECTORY, existingFileName);

        DataFileDataProvider target = new DataFileDataProvider(fileUtilMock, NAME, dataFiles);
        Optional<InputStreamProvider> result = target.getInputStreamProvider(Path.of(existingFileName));

        MatcherAssert.assertThat(result.isPresent(), Matchers.is(true));
    }

    @Test
    public void getInputStreamProvider_should_return_inputStream_when_filePath_is_found_and_datafile_uses_tmp_driver() throws Exception {
        FileUtilWrapper fileUtilMock = Mockito.mock(FileUtilWrapper.class);
        Mockito.when(fileUtilMock.getFilesTempDirectory()).thenReturn(FIXTURE_DIRECTORY);

        String existingFileName = "DataFileDataProviderTest.txt";
        List<DataFile> dataFiles = createDataFiles("tmp://", EMPTY_DIRECTORY, existingFileName);

        DataFileDataProvider target = new DataFileDataProvider(fileUtilMock, NAME, dataFiles);
        Optional<InputStreamProvider> result = target.getInputStreamProvider(Path.of(existingFileName));

        MatcherAssert.assertThat(result.isPresent(), Matchers.is(true));
    }

    private List<DataFile> createDataFiles(String driver, String dir, String... dataFileNames) {
        List<DataFile> dataFiles = Arrays.stream(dataFileNames).map(fileName -> {
            DataFile dataFile = Mockito.mock(DataFile.class);
            Mockito.when(dataFile.getDirectoryLabel()).thenReturn(dir);
            Mockito.when(dataFile.getCurrentName()).thenReturn(fileName);
            Mockito.when(dataFile.getStorageIdentifier()).thenReturn(driver + fileName);
            return dataFile;
        }).collect(Collectors.toList());

        return dataFiles;
    }

}