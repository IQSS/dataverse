package edu.harvard.iq.dataverse.util.bagit.data;

import edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider.InputStreamProvider;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 * @author adaybujeda
 */
public class ZipFileDataProviderTest {

    private static final String NAME = UUID.randomUUID().toString();

    @Test
    public void getName_should_return_configured_name() {
        ZipFileDataProvider target = new ZipFileDataProvider(NAME, Mockito.mock(ZipFile.class));
        MatcherAssert.assertThat(target.getName(), Matchers.is(NAME));
    }

    @Test
    public void getFilePaths_should_iterate_through_all_zip_entries() {
        ZipFile zipFileMock = Mockito.mock(ZipFile.class);
        mockZipEnumeration(zipFileMock, "zip1", "zip2", "zip3");

        ZipFileDataProvider target = new ZipFileDataProvider(NAME, zipFileMock);
        List<Path> result = target.getFilePaths();

        Mockito.verify(zipFileMock).getEntries();
        MatcherAssert.assertThat(result.size(), Matchers.is(3));
        MatcherAssert.assertThat(result, Matchers.hasItem(Path.of("zip1")));
        MatcherAssert.assertThat(result, Matchers.hasItem(Path.of("zip2")));
        MatcherAssert.assertThat(result, Matchers.hasItem(Path.of("zip3")));
    }

    @Test
    public void getFilePaths_should_ignore_directories() {
        ZipFile zipFileMock = Mockito.mock(ZipFile.class);
        mockZipEnumeration(zipFileMock, "zip1", "dir1/", "zip2", "dir2/");

        ZipFileDataProvider target = new ZipFileDataProvider(NAME, zipFileMock);
        List<Path> result = target.getFilePaths();

        MatcherAssert.assertThat(result.size(), Matchers.is(2));
        MatcherAssert.assertThat(result, Matchers.hasItem(Path.of("zip1")));
        MatcherAssert.assertThat(result, Matchers.hasItem(Path.of("zip2")));

        Mockito.verify(zipFileMock).getEntries();
    }

    @Test
    public void getInputStreamProvider_should_return_empty_when_file_path_is_not_found() {
        Path fileNotFound = Path.of(UUID.randomUUID().toString());
        ZipFile zipFileMock = Mockito.mock(ZipFile.class);
        Mockito.when(zipFileMock.getEntry(fileNotFound.toString())).thenReturn(null);

        ZipFileDataProvider target = new ZipFileDataProvider(NAME, zipFileMock);
        Optional<InputStreamProvider> result = target.getInputStreamProvider(fileNotFound);

        MatcherAssert.assertThat(result.isEmpty(), Matchers.is(true));

        Mockito.verify(zipFileMock).getEntry(fileNotFound.toString());
    }

    @Test
    public void getInputStreamProvider_should_return_inputStream_for_file_from_zip() throws Exception {
        ZipFile zipFileMock = Mockito.mock(ZipFile.class);
        ZipArchiveEntry zipEntryMock = Mockito.mock(ZipArchiveEntry.class);
        InputStream inputStreamMock = Mockito.mock(InputStream.class);
        Path filePath = Path.of(UUID.randomUUID().toString());

        Mockito.when(zipFileMock.getInputStream(zipEntryMock)).thenReturn(inputStreamMock);
        Mockito.when(zipFileMock.getEntry(filePath.toString())).thenReturn(zipEntryMock);

        ZipFileDataProvider target = new ZipFileDataProvider(NAME, zipFileMock);
        Optional<InputStreamProvider> result = target.getInputStreamProvider(filePath);

        MatcherAssert.assertThat(result.isEmpty(), Matchers.is(false));
        MatcherAssert.assertThat(result.get().getInputStream(), Matchers.is(inputStreamMock));

        Mockito.verify(zipFileMock).getEntry(filePath.toString());
        Mockito.verify(zipFileMock).getInputStream(zipEntryMock);
    }

    @Test
    public void close_should_call_zipfile_close_method() throws Exception {
        ZipFile zipFileMock = Mockito.mock(ZipFile.class);

        ZipFileDataProvider target = new ZipFileDataProvider(NAME, zipFileMock);
        target.close();

        Mockito.verify(zipFileMock).close();
    }

    private void mockZipEnumeration(ZipFile zipFileMock, String... zipEntryNames) {
        List<ZipArchiveEntry> zipArchiveEntries = Arrays.stream(zipEntryNames).map(name -> {
            ZipArchiveEntry zipEntry = Mockito.mock(ZipArchiveEntry.class);
            Mockito.when(zipEntry.getName()).thenReturn(name);
            Mockito.when(zipEntry.isDirectory()).thenReturn(name.endsWith("/"));
            return zipEntry;
        }).collect(Collectors.toList());

        Mockito.when(zipFileMock.getEntries()).thenReturn(Collections.enumeration(zipArchiveEntries));
    }

}