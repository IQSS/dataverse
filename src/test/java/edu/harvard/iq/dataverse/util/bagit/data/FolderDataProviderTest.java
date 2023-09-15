package edu.harvard.iq.dataverse.util.bagit.data;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author adaybujeda
 */
public class FolderDataProviderTest {

    private static final String FIXTURE_DIRECTORY = "src/test/resources/bagit/data";

    @Test
    public void getName_should_return_configured_name() {
        Path folderLocation = Path.of(UUID.randomUUID().toString());

        FolderDataProvider target = new FolderDataProvider(folderLocation);

        MatcherAssert.assertThat(target.getName(), Matchers.is(folderLocation.toString()));
    }

    @Test
    public void getFilePaths_should_return_empty_when_folder_do_not_exits() {
        Path folderLocation = Path.of(UUID.randomUUID().toString());

        FolderDataProvider target = new FolderDataProvider(folderLocation);
        List<Path> result = target.getFilePaths();

        MatcherAssert.assertThat(result.size(), Matchers.is(0));
    }

    @Test
    public void getFilePaths_should_return_empty_when_listing_files_throws_exception() throws IOException {
        Path folderLocation = Mockito.mock(Path.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(folderLocation.toFile().exists()).thenReturn(true);

        FileUtilWrapper fileUtilMock = Mockito.mock(FileUtilWrapper.class);
        Mockito.when(fileUtilMock.list(folderLocation)).thenThrow(new IOException("ERROR"));

        FolderDataProvider target = new FolderDataProvider(fileUtilMock, folderLocation);
        List<Path> result = target.getFilePaths();

        MatcherAssert.assertThat(result.size(), Matchers.is(0));

        Mockito.verify(folderLocation.toFile()).exists();
        Mockito.verify(fileUtilMock).list(folderLocation);
    }

    @Test
    public void getFilePaths_should_iterate_through_all_files_within_folderLocation() {
        Path folderLocation = Path.of(FIXTURE_DIRECTORY, "FolderDataProviderTest");

        FolderDataProvider target = new FolderDataProvider(folderLocation);
        List<Path> result = target.getFilePaths();

        MatcherAssert.assertThat(result.size(), Matchers.is(2));
        MatcherAssert.assertThat(result, Matchers.hasItem(Path.of("file1.txt")));
        MatcherAssert.assertThat(result, Matchers.hasItem(Path.of("file2.csv")));
    }

    @Test
    public void getInputStreamProvider_should_return_empty_when_file_do_no_exits() throws Exception {
        Path missingFile = Path.of(UUID.randomUUID().toString());
        Path folderLocation = Path.of(UUID.randomUUID().toString());

        FolderDataProvider target = new FolderDataProvider(folderLocation);
        Optional<FileDataProvider.InputStreamProvider> result = target.getInputStreamProvider(missingFile);

        MatcherAssert.assertThat(result.isEmpty(), Matchers.is(true));
    }

    @Test
    public void getInputStreamProvider_should_return_inputstream_when_file_exits() throws Exception {
        Path folderLocation = Path.of(FIXTURE_DIRECTORY, "FolderDataProviderTest");
        Path existingFile = Path.of("file1.txt");

        FolderDataProvider target = new FolderDataProvider(folderLocation);
        Optional<FileDataProvider.InputStreamProvider> result = target.getInputStreamProvider(existingFile);

        MatcherAssert.assertThat(result.isEmpty(), Matchers.is(false));
    }

}