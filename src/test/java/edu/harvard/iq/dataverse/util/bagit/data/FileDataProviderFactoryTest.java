package edu.harvard.iq.dataverse.util.bagit.data;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;

/**
 *
 * @author adaybujeda
 */
public class FileDataProviderFactoryTest {

    private static final String FIXTURE_DIRECTORY = "src/test/resources/bagit/data";

    private final FileDataProviderFactory target = new FileDataProviderFactory();

    @Test
    public void should_return_FolderDataProvider_when_parameter_is_path() {
        FileDataProvider result = target.getFileDataProvider(Path.of(UUID.randomUUID().toString()));

        MatcherAssert.assertThat("should return FolderDataProvider when parameter is path", result instanceof FolderDataProvider);
    }

    @Test
    public void should_return_ZipFileDataProvider_when_parameter_is_file() throws IOException {
        FileDataProvider result = target.getFileDataProvider(Path.of(FIXTURE_DIRECTORY, "FileDataProviderFactoryTest.zip").toFile());

        MatcherAssert.assertThat("should return ZipFileDataProvider when parameter is file", result instanceof ZipFileDataProvider);
    }

    @Test
    public void should_return_DataFileDataProvider_when_parameter_is_datafiles() {
        FileDataProvider result = target.getFileDataProvider("test-name", Collections.emptyList());

        MatcherAssert.assertThat("should return DataFileDataProvider when parameter is datafiles", result instanceof DataFileDataProvider);
    }

}