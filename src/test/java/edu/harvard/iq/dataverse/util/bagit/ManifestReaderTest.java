package edu.harvard.iq.dataverse.util.bagit;

import edu.harvard.iq.dataverse.util.bagit.ManifestReader.ManifestChecksum;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProviderFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Optional;

/**
 *
 * @author adaybujeda
 */
public class ManifestReaderTest {

    private static final Path FIXTURE_LOCATION = Path.of("src/test/resources/bagit/manifest");

    private FileDataProviderFactory dataProviderFactory = new FileDataProviderFactory();
    private ManifestReader target = new ManifestReader();


    @Test
    public void getManifestChecksums_should_try_all_checksum_types_to_find_manifest_and_return_empty_when_none_found() {
        FileDataProvider fileDataProvider = Mockito.mock(FileDataProvider.class);
        Optional<ManifestChecksum> manifestChecksums = target.getManifestChecksums(fileDataProvider, Path.of(""));

        MatcherAssert.assertThat(manifestChecksums.isEmpty(), Matchers.is(true));

        for (BagChecksumType type: BagChecksumType.values()) {
            Mockito.verify(fileDataProvider).getInputStreamProvider(Path.of(type.getFileName()));
        }
    }

    @Test
    public void getManifestChecksums_should_return_valid_ManifestChecksum_object_when_valid_manifest_found() throws Exception {
        FileDataProvider fixtureDataProvider = dataProviderFactory.getFileDataProvider(FIXTURE_LOCATION);
        Optional<ManifestChecksum> manifestChecksums = target.getManifestChecksums(fixtureDataProvider, Path.of("valid"));

        MatcherAssert.assertThat(manifestChecksums.isEmpty(), Matchers.is(false));
        MatcherAssert.assertThat(manifestChecksums.get().getManifestFile(), Matchers.is(Path.of("valid/manifest-sha256.txt")));
        MatcherAssert.assertThat(manifestChecksums.get().getType(), Matchers.is(BagChecksumType.SHA256));
        MatcherAssert.assertThat(manifestChecksums.get().getFileChecksums().size(), Matchers.is(2));
        MatcherAssert.assertThat(manifestChecksums.get().getFileChecksums().get(Path.of("valid/data/file-line-1.txt")), Matchers.is("hash-line-1"));
        MatcherAssert.assertThat(manifestChecksums.get().getFileChecksums().get(Path.of("valid/data/file-line-2.txt")), Matchers.is("hash-line-2"));
    }

    @Test
    public void getManifestChecksums_should_return_empty_when_manifest_has_invalid_format() throws Exception {
        FileDataProvider fixtureDataProvider = dataProviderFactory.getFileDataProvider(FIXTURE_LOCATION);
        Optional<ManifestChecksum> manifestChecksums = target.getManifestChecksums(fixtureDataProvider, Path.of("invalid_format"));

        MatcherAssert.assertThat(manifestChecksums.isEmpty(), Matchers.is(true));
    }

    @Test
    public void getManifestChecksums_should_return_empty_when_dataprovider_throws_exception() throws Exception {
        FileDataProvider fileDataProvider = Mockito.mock(FileDataProvider.class);
        Mockito.when(fileDataProvider.getInputStreamProvider(Mockito.any())).thenThrow(new NullPointerException("Test Exception"));
        Optional<ManifestChecksum> manifestChecksums = target.getManifestChecksums(fileDataProvider, Path.of(""));

        MatcherAssert.assertThat(manifestChecksums.isEmpty(), Matchers.is(true));
    }

    @Test
    public void getSupportedManifest_should_return_empty_when_no_supported_manifest_found() throws Exception {
        FileDataProvider fileDataProvider = Mockito.mock(FileDataProvider.class);
        Optional<Path> manifest = target.getSupportedManifest(fileDataProvider, Path.of(""));

        MatcherAssert.assertThat(manifest.isEmpty(), Matchers.is(true));

        for (BagChecksumType type: BagChecksumType.values()) {
            Mockito.verify(fileDataProvider).getInputStreamProvider(Path.of(type.getFileName()));
        }
    }

    @Test
    public void getSupportedManifest_should_return_manifest_path_when_found() throws Exception {
        FileDataProvider fixtureDataProvider = dataProviderFactory.getFileDataProvider(FIXTURE_LOCATION);

        Optional<Path> manifest = target.getSupportedManifest(fixtureDataProvider, Path.of("valid"));

        MatcherAssert.assertThat(manifest.isPresent(), Matchers.is(true));
        MatcherAssert.assertThat(manifest.get(), Matchers.is(Path.of("valid/manifest-sha256.txt")));
    }

}