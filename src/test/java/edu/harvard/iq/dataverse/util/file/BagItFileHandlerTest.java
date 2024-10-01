package edu.harvard.iq.dataverse.util.file;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.bagit.BagValidation;
import edu.harvard.iq.dataverse.util.bagit.BagValidator;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProviderFactory;
import edu.harvard.iq.dataverse.util.bagit.data.FileUtilWrapper;
import edu.harvard.iq.dataverse.util.bagit.data.StringDataProvider;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author adaybujeda
 */
public class BagItFileHandlerTest {

    private static final File FILE = new File("BagItFileHandlerTest");
    private static final BagValidation BAG_VALIDATION_SUCCESS = new BagValidation(Optional.empty());

    private static FileUtilWrapper FILE_UTIL;
    private static SystemConfig SYSTEM_CONFIG;
    private static DatasetVersion DATASET_VERSION;

    private FileDataProviderFactory fileDataProviderFactory;
    private BagValidator bagValidator;
    private BagItFileHandlerPostProcessor postProcessor;

    private BagItFileHandler target;

    @BeforeEach
    public void beforeEachTest() {
        FILE_UTIL = Mockito.mock(FileUtilWrapper.class, Mockito.RETURNS_DEEP_STUBS);
        SYSTEM_CONFIG = Mockito.mock(SystemConfig.class, Mockito.RETURNS_DEEP_STUBS);
        DATASET_VERSION = Mockito.mock(DatasetVersion.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(SYSTEM_CONFIG.getZipUploadFilesLimit()).thenReturn(20000);
        Mockito.when(SYSTEM_CONFIG.getMaxFileUploadSizeForStore(Mockito.any())).thenReturn(20000l);
        Mockito.when(SYSTEM_CONFIG.getFileFixityChecksumAlgorithm()).thenReturn(DataFile.ChecksumType.MD5);
        Mockito.when(DATASET_VERSION.getDataset().getEffectiveStorageDriverId()).thenReturn("temp");

        fileDataProviderFactory = Mockito.mock(FileDataProviderFactory.class);
        bagValidator = Mockito.mock(BagValidator.class);
        postProcessor = Mockito.spy(new BagItFileHandlerPostProcessor());
        target = new BagItFileHandler(FILE_UTIL, fileDataProviderFactory, bagValidator, postProcessor);
    }

    @Test
    public void isBagItPackage_should_return_false_when_no_bagIt_file_detected() throws IOException {
        FileDataProvider fileDataProvider = Mockito.mock(FileDataProvider.class);
        Mockito.when(fileDataProviderFactory.getFileDataProvider(FILE)).thenReturn(fileDataProvider);
        Mockito.when(bagValidator.hasBagItPackage(fileDataProvider)).thenReturn(false);

        boolean result = target.isBagItPackage(FILE.getName(), FILE);
        MatcherAssert.assertThat(result, Matchers.is(false));
        Mockito.verify(bagValidator).hasBagItPackage(fileDataProvider);
        Mockito.verify(fileDataProvider).close();
    }

    @Test
    public void isBagItPackage_should_return_true_when_bagIt_file_detected() throws IOException {
        FileDataProvider fileDataProvider = Mockito.mock(FileDataProvider.class);
        Mockito.when(fileDataProviderFactory.getFileDataProvider(FILE)).thenReturn(fileDataProvider);
        Mockito.when(bagValidator.hasBagItPackage(fileDataProvider)).thenReturn(true);

        boolean result = target.isBagItPackage(FILE.getName(), FILE);
        MatcherAssert.assertThat(result, Matchers.is(true));
        Mockito.verify(bagValidator).hasBagItPackage(fileDataProvider);
        Mockito.verify(fileDataProvider).close();
    }

    @Test
    public void handleBagItPackage_should_return_error_when_no_files_in_data_provider() throws IOException {
        FileDataProvider fileDataProvider = Mockito.mock(FileDataProvider.class);
        Mockito.when(fileDataProvider.getFilePaths()).thenReturn(Collections.emptyList());
        Mockito.when(fileDataProviderFactory.getFileDataProvider(FILE)).thenReturn(fileDataProvider);

        CreateDataFileResult result = target.handleBagItPackage(SYSTEM_CONFIG, DATASET_VERSION, FILE.getName(), FILE);
        MatcherAssert.assertThat(result.success(), Matchers.is(false));
        createDataFileResultAsserts(result);

        handleBagItPackageAsserts(fileDataProvider);
        Mockito.verifyNoInteractions(postProcessor);
    }

    @Test
    public void handleBagItPackage_should_return_success_with_datafiles_when_bagIt_package_is_valid() throws Exception {
        String bagEntry1 = "dir/path/" + UUID.randomUUID();
        String bagEntry2 = "dir/test/" + UUID.randomUUID();
        DataProviderWithDataFiles dataProviderWithDataFiles = createDataProviderWithDataFiles(bagEntry1, bagEntry2);
        FileDataProvider dataProviderSpy = Mockito.spy(dataProviderWithDataFiles.dataProvider);
        Mockito.when(fileDataProviderFactory.getFileDataProvider(FILE)).thenReturn(dataProviderSpy);
        Mockito.when(bagValidator.validateChecksums(Mockito.any())).thenReturn(BAG_VALIDATION_SUCCESS);


        CreateDataFileResult result = target.handleBagItPackage(SYSTEM_CONFIG, DATASET_VERSION, FILE.getName(), FILE);
        MatcherAssert.assertThat(result.success(), Matchers.is(true));
        createDataFileResultAsserts(result);
        for(DataFile expectedDataFile: dataProviderWithDataFiles.dataFiles) {
            MatcherAssert.assertThat(result.getDataFiles(), Matchers.hasItems(expectedDataFile));
        }

        handleBagItPackageAsserts(dataProviderSpy);
        createDataFileAsserts(dataProviderWithDataFiles.dataProvider.getFilePaths());
        Mockito.verify(postProcessor).process(Mockito.any());
    }

    @Test
    public void handleBagItPackage_should_call_postprocessor_when_successful() throws Exception {
        String bagEntry = "dir/path/" + UUID.randomUUID();
        DataProviderWithDataFiles dataProviderWithDataFiles = createDataProviderWithDataFiles(bagEntry);
        FileDataProvider dataProviderSpy = Mockito.spy(dataProviderWithDataFiles.dataProvider);
        Mockito.when(fileDataProviderFactory.getFileDataProvider(FILE)).thenReturn(dataProviderSpy);
        Mockito.when(bagValidator.validateChecksums(Mockito.any())).thenReturn(BAG_VALIDATION_SUCCESS);


        CreateDataFileResult result = target.handleBagItPackage(SYSTEM_CONFIG, DATASET_VERSION, FILE.getName(), FILE);
        MatcherAssert.assertThat(result.success(), Matchers.is(true));
        createDataFileResultAsserts(result);
        Mockito.verify(postProcessor).process(Mockito.any());
        handleBagItPackageAsserts(dataProviderSpy);
        createDataFileAsserts(dataProviderWithDataFiles.dataProvider.getFilePaths());
        Mockito.verify(postProcessor).process(Mockito.any());
    }

    @Test
    public void handleBagItPackage_should_set_file_data_metadata() throws Exception {
        String bagEntry = "dir/path/" + UUID.randomUUID();
        DataProviderWithDataFiles dataProviderWithDataFiles = createDataProviderWithDataFiles(bagEntry);
        FileDataProvider dataProviderSpy = Mockito.spy(dataProviderWithDataFiles.dataProvider);
        Mockito.when(fileDataProviderFactory.getFileDataProvider(FILE)).thenReturn(dataProviderSpy);
        Mockito.when(bagValidator.validateChecksums(Mockito.any())).thenReturn(BAG_VALIDATION_SUCCESS);
        Mockito.when(FILE_UTIL.determineFileType(Mockito.any(), Mockito.any())).thenReturn("TEST_TYPE");


        CreateDataFileResult result = target.handleBagItPackage(SYSTEM_CONFIG, DATASET_VERSION, FILE.getName(), FILE);
        MatcherAssert.assertThat(result.success(), Matchers.is(true));
        createDataFileResultAsserts(result);
        MatcherAssert.assertThat(result.getDataFiles().size(), Matchers.is(1));
        MatcherAssert.assertThat(result.getDataFiles().get(0), Matchers.is(dataProviderWithDataFiles.dataFiles.get(0)));
        MatcherAssert.assertThat(result.getDataFiles().get(0).getDirectoryLabel(), Matchers.is("dir/path"));
        MatcherAssert.assertThat(result.getDataFiles().get(0).getContentType(), Matchers.is("TEST_TYPE"));

        handleBagItPackageAsserts(dataProviderSpy);
        createDataFileAsserts(dataProviderWithDataFiles.dataProvider.getFilePaths());
        Mockito.verify(postProcessor).process(Mockito.any());
    }

    @Test
    public void handleBagItPackage_should_ignore_exceptions_when_calculating_content_type() throws Exception {
        String bagEntry = UUID.randomUUID().toString();
        DataProviderWithDataFiles dataProviderWithDataFiles = createDataProviderWithDataFiles(bagEntry);
        FileDataProvider dataProviderSpy = Mockito.spy(dataProviderWithDataFiles.dataProvider);
        Mockito.when(fileDataProviderFactory.getFileDataProvider(FILE)).thenReturn(dataProviderSpy);
        Mockito.when(bagValidator.validateChecksums(Mockito.any())).thenReturn(BAG_VALIDATION_SUCCESS);
        Mockito.when(FILE_UTIL.determineFileType(Mockito.any(), Mockito.any())).thenThrow(new IOException("Error"));


        CreateDataFileResult result = target.handleBagItPackage(SYSTEM_CONFIG, DATASET_VERSION, FILE.getName(), FILE);
        MatcherAssert.assertThat(result.success(), Matchers.is(true));
        createDataFileResultAsserts(result);
        MatcherAssert.assertThat(result.getDataFiles().size(), Matchers.is(1));
        MatcherAssert.assertThat(result.getDataFiles().get(0), Matchers.is(dataProviderWithDataFiles.dataFiles.get(0)));
        MatcherAssert.assertThat(result.getDataFiles().get(0).getContentType(), Matchers.nullValue());

        handleBagItPackageAsserts(dataProviderSpy);
        createDataFileAsserts(dataProviderWithDataFiles.dataProvider.getFilePaths());
        Mockito.verify(postProcessor).process(Mockito.any());
    }

    @Test
    public void handleBagItPackage_should_ignore_nulls_datafiles_created_by_FileUtil() throws Exception {
        String bagEntry = UUID.randomUUID().toString();
        String returnNullDataFile = "return_null" + UUID.randomUUID().toString();
        DataProviderWithDataFiles dataProviderWithDataFiles = createDataProviderWithDataFiles(bagEntry, returnNullDataFile);
        FileDataProvider dataProviderSpy = Mockito.spy(dataProviderWithDataFiles.dataProvider);
        Mockito.when(fileDataProviderFactory.getFileDataProvider(FILE)).thenReturn(dataProviderSpy);
        Mockito.when(bagValidator.validateChecksums(Mockito.any())).thenReturn(BAG_VALIDATION_SUCCESS);


        CreateDataFileResult result = target.handleBagItPackage(SYSTEM_CONFIG, DATASET_VERSION, FILE.getName(), FILE);
        MatcherAssert.assertThat(result.success(), Matchers.is(true));
        createDataFileResultAsserts(result);

        DataFile expectedDataFile = dataProviderWithDataFiles.dataFiles.stream().filter(dataFile -> dataFile.getCurrentName().equals(bagEntry)).findFirst().get();
        MatcherAssert.assertThat(result.getDataFiles().size(), Matchers.is(1));
        MatcherAssert.assertThat(result.getDataFiles(), Matchers.hasItems(expectedDataFile));

        handleBagItPackageAsserts(dataProviderSpy);
        createDataFileAsserts(dataProviderWithDataFiles.dataProvider.getFilePaths());
        Mockito.verify(postProcessor).process(Mockito.any());
    }

    @Test
    public void handleBagItPackage_should_return_error_when_FileExceedsMaxSizeException_is_thrown() throws Exception {
        String bagEntry = UUID.randomUUID().toString();
        String exceptionDataFile = "FileExceedsMaxSizeException" + UUID.randomUUID();
        DataProviderWithDataFiles dataProviderWithDataFiles = createDataProviderWithDataFiles(bagEntry, exceptionDataFile);
        FileDataProvider dataProviderSpy = Mockito.spy(dataProviderWithDataFiles.dataProvider);
        Mockito.when(fileDataProviderFactory.getFileDataProvider(FILE)).thenReturn(dataProviderSpy);
        Mockito.when(bagValidator.validateChecksums(Mockito.any())).thenReturn(BAG_VALIDATION_SUCCESS);

        Mockito.when(FILE_UTIL.saveInputStreamInTempFile(Mockito.any(), Mockito.any()))
                .thenReturn(new File("test"))
                .thenThrow(new FileExceedsMaxSizeException("file too big"));


        CreateDataFileResult result = target.handleBagItPackage(SYSTEM_CONFIG, DATASET_VERSION, FILE.getName(), FILE);
        MatcherAssert.assertThat(result.success(), Matchers.is(false));
        createDataFileResultAsserts(result);
        MatcherAssert.assertThat(result.getErrors().size(), Matchers.is(1));

        handleBagItPackageAsserts(dataProviderSpy);
        createDataFileAsserts(Arrays.asList(Path.of(bagEntry)), 2);
        Mockito.verifyNoInteractions(postProcessor);
    }

    @Test
    public void handleBagItPackage_should_return_error_when_the_maximum_number_of_files_is_exceeded() throws Exception {
        Mockito.when(SYSTEM_CONFIG.getZipUploadFilesLimit()).thenReturn(1);
        DataProviderWithDataFiles dataProviderWithDataFiles = createDataProviderWithDataFiles(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        FileDataProvider dataProviderSpy = Mockito.spy(dataProviderWithDataFiles.dataProvider);
        Mockito.when(fileDataProviderFactory.getFileDataProvider(FILE)).thenReturn(dataProviderSpy);
        Mockito.when(bagValidator.validateChecksums(Mockito.any())).thenReturn(BAG_VALIDATION_SUCCESS);


        CreateDataFileResult result = target.handleBagItPackage(SYSTEM_CONFIG, DATASET_VERSION, FILE.getName(), FILE);
        MatcherAssert.assertThat(result.success(), Matchers.is(false));
        createDataFileResultAsserts(result);
        MatcherAssert.assertThat(result.getErrors().size(), Matchers.is(1));

        handleBagItPackageAsserts(dataProviderSpy);
        Mockito.verifyNoInteractions(postProcessor);
    }

    @Test
    public void handleBagItPackage_should_return_error_when_bag_validation_fails() throws Exception {
        DataProviderWithDataFiles dataProviderWithDataFiles = createDataProviderWithDataFiles(UUID.randomUUID().toString());
        FileDataProvider dataProviderSpy = Mockito.spy(dataProviderWithDataFiles.dataProvider);
        Mockito.when(fileDataProviderFactory.getFileDataProvider(FILE)).thenReturn(dataProviderSpy);
        Mockito.when(bagValidator.validateChecksums(Mockito.any())).thenReturn(new BagValidation(Optional.of("ERROR")));


        CreateDataFileResult result = target.handleBagItPackage(SYSTEM_CONFIG, DATASET_VERSION, FILE.getName(), FILE);
        MatcherAssert.assertThat(result.success(), Matchers.is(false));
        createDataFileResultAsserts(result);

        handleBagItPackageAsserts(dataProviderSpy);
        createDataFileAsserts(dataProviderWithDataFiles.dataProvider.getFilePaths());
        Mockito.verifyNoInteractions(postProcessor);
    }

    private void createDataFileResultAsserts(CreateDataFileResult result) {
        MatcherAssert.assertThat(result.getFilename(), Matchers.is(FILE.getName()));
        MatcherAssert.assertThat(result.getType(), Matchers.is(BagItFileHandler.FILE_TYPE));
    }

    private void handleBagItPackageAsserts(FileDataProvider dataProviderMock) throws IOException{
        Mockito.verify(dataProviderMock).getFilePaths();
        Mockito.verify(dataProviderMock).close();

        Mockito.verify(fileDataProviderFactory).getFileDataProvider(Mockito.any(File.class));

        Mockito.verify(SYSTEM_CONFIG).getZipUploadFilesLimit();
        Mockito.verify(SYSTEM_CONFIG).getMaxFileUploadSizeForStore(Mockito.any());
        Mockito.verify(SYSTEM_CONFIG).getFileFixityChecksumAlgorithm();

        Mockito.verify(FILE_UTIL).deleteFile(FILE.toPath());
    }

    private void createDataFileAsserts(List<Path> filePaths) throws Exception {
        createDataFileAsserts(filePaths, filePaths.size());
    }

    private void createDataFileAsserts(List<Path> filePaths, int saveInputStreamCalls) throws Exception {
        Mockito.verify(FILE_UTIL, Mockito.times(saveInputStreamCalls)).saveInputStreamInTempFile(Mockito.any(), Mockito.any());

        for(Path filePath: filePaths) {
            Mockito.verify(FILE_UTIL).createSingleDataFile(Mockito.any(), Mockito.any(), Mockito.any(),
                    Mockito.eq(filePath.getFileName().toString()), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        }
    }

    private DataProviderWithDataFiles createDataProviderWithDataFiles(String... filePathItems) throws Exception {
        List<Path> filePaths = new ArrayList<>();
        List<DataFile> dataFiles = new ArrayList<>();

        for(String filePath:  filePathItems) {
            String fileName = Path.of(filePath).getFileName().toString();
            DataFile dataFile = new DataFile();
            dataFile.setId(MocksFactory.nextId());
            dataFile.getFileMetadatas().add(new FileMetadata());
            dataFile.getLatestFileMetadata().setLabel(fileName);

            if(filePath.startsWith("return_null")) {
                dataFile = null;
            }

            Mockito.when(FILE_UTIL.createSingleDataFile(Mockito.any(), Mockito.any(), Mockito.any(),
                    Mockito.eq(fileName), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dataFile);

            filePaths.add(Path.of(filePath));
            dataFiles.add(dataFile);
        }

        return new DataProviderWithDataFiles(new StringDataProvider(false, filePaths), dataFiles);
    }

    private static class DataProviderWithDataFiles {
        final FileDataProvider dataProvider;
        final List<DataFile> dataFiles;

        public DataProviderWithDataFiles(FileDataProvider dataProvider, List<DataFile> dataFiles) {
            this.dataProvider = dataProvider;
            this.dataFiles = dataFiles;
        }
    }

}