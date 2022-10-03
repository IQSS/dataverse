package edu.harvard.iq.dataverse.util.bagit;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.util.bagit.ManifestReader.ManifestChecksum;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider;
import edu.harvard.iq.dataverse.util.bagit.data.StringDataProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author adaybujeda
 */
public class BagValidatorTest {

    private static final Path SUPPORTED_MANIFEST = Path.of("manifest-md5.txt");

    private ManifestReader manifestReader;
    private BagValidator target;

    @Before
    public void beforeEachTest() {
        manifestReader = Mockito.mock(ManifestReader.class);
        target = Mockito.spy(new BagValidator(manifestReader));
    }

    @Test
    public void hasBagItPackage_should_return_false_when_bagit_file_not_found() {
        FileDataProvider fileDataProvider = createDataProviderWithRandomFiles("file.txt", "other_file.txt");

        boolean result = target.hasBagItPackage(fileDataProvider);

        MatcherAssert.assertThat(result, Matchers.is(false));
        Mockito.verifyZeroInteractions(manifestReader);
    }

    @Test
    public void hasBagItPackage_should_return_false_when_manifest_not_found() {
        FileDataProvider fileDataProvider = createDataProviderWithRandomFiles("file.txt", "bagit.txt", "other_file.txt");
        Path expectedBagRoot = Path.of("");
        Mockito.when(manifestReader.getSupportedManifest(fileDataProvider, expectedBagRoot)).thenReturn(Optional.empty());

        boolean result = target.hasBagItPackage(fileDataProvider);

        MatcherAssert.assertThat(result, Matchers.is(false));
        Mockito.verify(manifestReader).getSupportedManifest(fileDataProvider, expectedBagRoot);
    }

    @Test
    public void hasBagItPackage_should_return_true_when_bagit_file_and_manifest_in_data_provider() {
        FileDataProvider fileDataProvider = createDataProviderWithRandomFiles("file.txt", "bagit.txt", "other_file.txt");
        Path expectedBagRoot = Path.of("");
        Mockito.when(manifestReader.getSupportedManifest(fileDataProvider, expectedBagRoot)).thenReturn(Optional.of(SUPPORTED_MANIFEST));

        boolean result = target.hasBagItPackage(fileDataProvider);

        MatcherAssert.assertThat(result, Matchers.is(true));
        Mockito.verify(manifestReader).getSupportedManifest(fileDataProvider, expectedBagRoot);
    }

    @Test
    public void hasBagItPackage_should_return_true_when_bagit_file_and_manifest_in_directory_in_data_provider() {
        FileDataProvider fileDataProvider = createDataProviderWithRandomFiles("some_dir/other_dir/bagit.txt");
        Path expectedBagRoot = Path.of("some_dir/other_dir");
        Mockito.when(manifestReader.getSupportedManifest(fileDataProvider, expectedBagRoot)).thenReturn(Optional.of(SUPPORTED_MANIFEST));

        boolean result = target.hasBagItPackage(fileDataProvider);

        MatcherAssert.assertThat(result, Matchers.is(true));
        Mockito.verify(manifestReader).getSupportedManifest(fileDataProvider, expectedBagRoot);
    }

    @Test
    public void validateChecksums_should_return_error_when_no_bagit_file_in_data_provider() throws Exception {
        FileDataProvider fileDataProvider = createDataProviderWithRandomFiles("file.txt", "other_file.txt");

        BagValidation result = target.validateChecksums(fileDataProvider);

        MatcherAssert.assertThat(result.success(), Matchers.is(false));
        MatcherAssert.assertThat(result.getErrorMessage().isEmpty(), Matchers.is(false));
        Mockito.verify(target).getMessage(Mockito.eq("bagit.validation.bag.file.not.found"), Mockito.any());

        Mockito.verifyZeroInteractions(manifestReader);
    }

    @Test
    public void validateChecksums_should_call_manifest_reader_with_expected_bagroot() throws Exception {
        FileDataProvider fileDataProvider = createDataProviderWithRandomFiles("dir/dir2/bagit.txt");
        Path expectedBagRoot = Path.of("dir/dir2");
        Mockito.when(manifestReader.getManifestChecksums(fileDataProvider, expectedBagRoot)).thenReturn(Optional.empty());

        target.validateChecksums(fileDataProvider);

        Mockito.verify(manifestReader).getManifestChecksums(fileDataProvider, expectedBagRoot);
    }

    @Test
    public void validateChecksums_should_return_error_when_manifest_reader_returns_empty() throws Exception {
        FileDataProvider fileDataProvider = createDataProviderWithRandomFiles("bagit.txt");
        Path expectedBagRoot = Path.of("");
        Mockito.when(manifestReader.getManifestChecksums(fileDataProvider, expectedBagRoot)).thenReturn(Optional.empty());

        BagValidation result = target.validateChecksums(fileDataProvider);

        MatcherAssert.assertThat(result.success(), Matchers.is(false));
        MatcherAssert.assertThat(result.getErrorMessage().isEmpty(), Matchers.is(false));
        Mockito.verify(target).getMessage(Mockito.eq("bagit.validation.manifest.not.supported"), Mockito.any());

        Mockito.verify(manifestReader).getManifestChecksums(fileDataProvider, expectedBagRoot);
    }

    @Test
    public void validateChecksums_should_return_error_when_data_provider_do_not_have_file_in_checksum() throws Exception {
        FileDataProvider fileDataProvider = Mockito.spy(createDataProviderWithRandomFiles("bagit.txt"));
        ManifestChecksum checksums =  new ManifestChecksum(Path.of("test"), BagChecksumType.MD5, Map.of(Path.of("not-found.txt"), "checksum"));
        Path expectedBagRoot = Path.of("");

        Mockito.when(manifestReader.getManifestChecksums(fileDataProvider, expectedBagRoot)).thenReturn(Optional.of(checksums));

        BagValidation result = target.validateChecksums(fileDataProvider);

        MatcherAssert.assertThat(result.success(), Matchers.is(false));
        MatcherAssert.assertThat(result.getErrorMessage().isEmpty(), Matchers.is(true));
        MatcherAssert.assertThat(result.getFileResults().size(), Matchers.is(checksums.getFileChecksums().size()));
        for(Path filePath: checksums.getFileChecksums().keySet()) {
            MatcherAssert.assertThat(result.getFileResults().get(filePath).isError(), Matchers.is(true));
        }
        Mockito.verify(target, Mockito.times(checksums.getFileChecksums().size())).getMessage(Mockito.eq("bagit.validation.file.not.found"), Mockito.any());

        Mockito.verify(manifestReader).getManifestChecksums(fileDataProvider, expectedBagRoot);
        Mockito.verify(fileDataProvider).getFilePaths();
        Mockito.verify(fileDataProvider).getInputStreamProvider(Path.of("not-found.txt"));
    }

    @Test
    public void validateChecksums_should_return_success_when_checksums_match() throws Exception {
        FileDataProvider fileDataProvider = createDataProviderWithRandomFiles("bagit.txt");
        ManifestChecksum checksums = createChecksums(fileDataProvider.getFilePaths(), true);
        Path expectedBagRoot = Path.of("");

        Mockito.when(manifestReader.getManifestChecksums(fileDataProvider, expectedBagRoot)).thenReturn(Optional.of(checksums));

        BagValidation result = target.validateChecksums(fileDataProvider);

        MatcherAssert.assertThat(result.success(), Matchers.is(true));
        MatcherAssert.assertThat(result.getErrorMessage().isEmpty(), Matchers.is(true));
        MatcherAssert.assertThat(result.getFileResults().size(), Matchers.is(checksums.getFileChecksums().size()));
        for(Path filePath: checksums.getFileChecksums().keySet()) {
            MatcherAssert.assertThat(result.getFileResults().get(filePath).isSuccess(), Matchers.is(true));
            MatcherAssert.assertThat(result.getFileResults().get(filePath).getMessage(), Matchers.nullValue());
        }

        Mockito.verify(manifestReader).getManifestChecksums(fileDataProvider, expectedBagRoot);
    }

    @Test
    public void validateChecksums_should_return_error_when_checksums_do_not_match() throws Exception {
        FileDataProvider fileDataProvider = createDataProviderWithRandomFiles("bagit.txt");
        ManifestChecksum checksums = createChecksums(fileDataProvider.getFilePaths(), false);
        Path expectedBagRoot = Path.of("");

        Mockito.when(manifestReader.getManifestChecksums(fileDataProvider, expectedBagRoot)).thenReturn(Optional.of(checksums));

        BagValidation result = target.validateChecksums(fileDataProvider);

        MatcherAssert.assertThat(result.success(), Matchers.is(false));
        MatcherAssert.assertThat(result.getErrorMessage().isEmpty(), Matchers.is(true));
        MatcherAssert.assertThat(result.getFileResults().size(), Matchers.is(checksums.getFileChecksums().size()));
        for(Path filePath: checksums.getFileChecksums().keySet()) {
            MatcherAssert.assertThat(result.getFileResults().get(filePath).isError(), Matchers.is(true));
            MatcherAssert.assertThat(result.getFileResults().get(filePath).getMessage(), Matchers.containsString("Invalid checksum"));
        }

        Mockito.verify(manifestReader).getManifestChecksums(fileDataProvider, expectedBagRoot);
    }

    @Test
    public void validateChecksums_should_return_error_when_max_errors_reached_and_stop_processing() throws Exception {
        BagValidator target = new BagValidator(1, 1, 0, manifestReader);
        FileDataProvider fileDataProvider = createDataProviderWithRandomFiles(true, "bagit.txt");
        ManifestChecksum checksums = createChecksums(fileDataProvider.getFilePaths(), false);
        Path expectedBagRoot = Path.of("");

        Mockito.when(manifestReader.getManifestChecksums(fileDataProvider, expectedBagRoot)).thenReturn(Optional.of(checksums));

        BagValidation result = target.validateChecksums(fileDataProvider);

        MatcherAssert.assertThat(result.success(), Matchers.is(false));
        MatcherAssert.assertThat(result.getErrorMessage().isEmpty(), Matchers.is(true));
        MatcherAssert.assertThat(result.getFileResults().size(), Matchers.is(checksums.getFileChecksums().size()));
        MatcherAssert.assertThat(result.errors(), Matchers.greaterThan(1l));
        MatcherAssert.assertThat( result.getFileResults().values().stream().filter(item -> item.isPending()).count(), Matchers.greaterThan(1l));
        MatcherAssert.assertThat( result.getFileResults().values().stream().filter(item -> item.isSuccess()).count(), Matchers.is(0l));

        Mockito.verify(manifestReader).getManifestChecksums(fileDataProvider, expectedBagRoot);
    }

    @Test
    public void validateChecksums_should_return_error_when_executor_service_throws_interrupted_exception() throws Exception {
        ExecutorService executorServiceMock = Mockito.mock(ExecutorService.class);
        Mockito.when(executorServiceMock.awaitTermination(Mockito.anyLong(), Mockito.any())).thenThrow(new InterruptedException("Interrupted"));

        BagValidator target = Mockito.spy(new BagValidator(1, 1, 0, manifestReader));
        Mockito.when(target.getExecutorService()).thenReturn(executorServiceMock);

        FileDataProvider fileDataProvider = createDataProviderWithRandomFiles(true, "bagit.txt");
        ManifestChecksum checksums = createChecksums(fileDataProvider.getFilePaths(), false);
        Mockito.when(manifestReader.getManifestChecksums(fileDataProvider, Path.of(""))).thenReturn(Optional.of(checksums));

        BagValidation result = target.validateChecksums(fileDataProvider);

        MatcherAssert.assertThat(result.success(), Matchers.is(false));
        MatcherAssert.assertThat(result.getErrorMessage().isEmpty(), Matchers.is(false));
        Mockito.verify(target).getMessage(Mockito.eq("bagit.validation.exception"), Mockito.any());
    }

    private FileDataProvider createDataProviderWithRandomFiles(String... filePathItems) {
        return createDataProviderWithRandomFiles(false, filePathItems);
    }

    private FileDataProvider createDataProviderWithRandomFiles(boolean withDelay, String... filePathItems) {
        List<String> randomItems = Stream.generate(() -> RandomStringUtils.randomAlphabetic(100)).limit(10).collect(Collectors.toList());
        List<String> allFileItems = Lists.newArrayList(filePathItems);
        allFileItems.addAll(randomItems);
        List<Path> filePaths = allFileItems.stream().map(filePathItem -> Path.of(filePathItem)).collect(Collectors.toList());
        return new StringDataProvider(withDelay, filePaths);
    }

    private ManifestChecksum createChecksums(List<Path> filePaths, boolean validChecksum) throws Exception {
        List<BagChecksumType> types = BagChecksumType.asList();
        BagChecksumType bagChecksumType = types.get(new Random().nextInt(types.size()));
        Map<Path, String> checksums = new HashMap<>();
        for (Path path : filePaths) {
            String checksum = validChecksum ?  bagChecksumType.getInputStreamDigester().digest(IOUtils.toInputStream(path.toString(), "UTF-8")) : "invalid";
            checksums.put(path, checksum);
        }
        return new ManifestChecksum(Path.of(bagChecksumType.getFileName()), bagChecksumType, checksums);
    }

}