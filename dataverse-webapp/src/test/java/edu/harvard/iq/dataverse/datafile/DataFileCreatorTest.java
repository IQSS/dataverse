package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.datasetutility.VirusFoundException;
import edu.harvard.iq.dataverse.license.TermsOfUseFactory;
import edu.harvard.iq.dataverse.license.TermsOfUseFormMapper;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestError;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.JhoveConfigurationInitializer;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.atIndex;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataFileCreatorTest {

    @InjectMocks
    private DataFileCreator dataFileCreator = new DataFileCreator();
    
    @Mock
    private SettingsServiceBean settingsService;
    @Mock
    private AntivirFileScanner antivirFileScanner;
    @Mock
    private FileTypeDetector fileTypeDetector;
    @Mock
    private ArchiveUncompressedSizeCalculator uncompressedCalculator;
    @Mock
    private TermsOfUseFactory termsOfUseFactory;
    @Mock
    private TermsOfUseFormMapper termsOfUseFormMapper;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void before() throws IOException {
        System.setProperty(SystemConfig.FILES_DIRECTORY, tempDir.toString());
    }
    
    @AfterEach
    void after() {
        System.clearProperty(SystemConfig.FILES_DIRECTORY);
    }
    
    // -------------------- TESTS --------------------
    
    @Test
    void createDataFiles_shouldThrowExceptionOnTooBigFile() throws IOException {
        // given
        byte[] zipBytes = UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_64.png");

        when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(1024L);

        // when & then
        assertThatThrownBy(() -> dataFileCreator.createDataFiles(new ByteArrayInputStream(zipBytes), "filename.png", "image/png"))
            .isInstanceOf(FileExceedsMaxSizeException.class);
        assertThat(Files.walk(tempDir).filter(Files::isRegularFile)).isEmpty();
    }
    
    @ParameterizedTest
    @CsvSource({
        "application/supplied, application/x-stata, application/x-stata",
        "application/supplied, application/fits-gzipped, application/fits-gzipped",
        "application/supplied, application/zip, application/zip",
        "application/octet-stream, application/detected, application/detected",
        "text/csv, application/detected, text/csv",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/detected, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/supplied, image/jpeg, application/supplied"})
    void createDataFiles_shouldPickCorrectContentType(String suppliedContentType, String detectedContentType, String expectedContentType) throws IOException {
        // given
        when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(1024L);
        when(settingsService.getValueForKey(Key.FileFixityChecksumAlgorithm)).thenReturn("MD5");
        when(fileTypeDetector.determineFileType(any(), any())).thenReturn(detectedContentType);
        // when
        List<DataFile> datafiles = dataFileCreator.createDataFiles(new ByteArrayInputStream(new byte[0]), "filename", suppliedContentType);
        // then
        assertThat(datafiles).hasSize(1);
        assertThat(datafiles).element(0).extracting(DataFile::getContentType).isEqualTo(expectedContentType);
    }
    
    @Test
    void createDataFiles_shouldThrowExceptionOnVirusInfectedFile() throws IOException {
        // given
        byte[] zipBytes = UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_64.png");

        when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(1024L*1024L);
        when(settingsService.isTrueForKey(Key.AntivirusScannerEnabled)).thenReturn(true);
        when(antivirFileScanner.isFileOverSizeLimit(any(), any())).thenReturn(false);
        when(antivirFileScanner.scan(any())).thenReturn(new AntivirScannerResponse(true, "Infected file"));
        when(fileTypeDetector.determineFileType(any(), any())).thenReturn("image/png");
        

        // when & then
        assertThatThrownBy(() -> dataFileCreator.createDataFiles(new ByteArrayInputStream(zipBytes), "filename.png", "image/png"))
            .isInstanceOf(VirusFoundException.class);
        assertThat(Files.walk(tempDir).filter(Files::isRegularFile)).isEmpty();
    }
    
    @Test
    void createDataFiles_shouldUnpackGzippedFitsFile() throws IOException {
        // given
        byte[] zipBytes = UnitTestUtils.readFileToByteArray("jhove/sample.fits.gz");

        when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(1024L*1024L);
        when(settingsService.getValueForKey(Key.FileFixityChecksumAlgorithm)).thenReturn("MD5");
        when(fileTypeDetector.determineFileType(any(), any())).thenReturn("application/fits-gzipped");

        // when
        List<DataFile> datafiles = dataFileCreator.createDataFiles(new ByteArrayInputStream(zipBytes), "sample.fits.gz", "application/fits-gzipped");
        
        // then
        assertThat(datafiles).hasSize(1)
            .satisfies(dataFile -> {
                assertThat(dataFile.getContentType()).isEqualTo("application/fits");
                assertThat(dataFile.getChecksumType()).isEqualTo(ChecksumType.MD5);
                assertThat(dataFile.getChecksumValue()).isEqualTo("a4791e42cd1045892f9c41f11b50bad8");
                assertThat(dataFile.getStorageIdentifier()).isNotEmpty();
                assertThat(dataFile.getFileMetadatas().get(0).getLabel()).isEqualTo("sample.fits");
            }, atIndex(0));
        assertThat(Files.walk(tempDir).filter(Files::isRegularFile)).hasSize(1)
            .element(0, InstanceOfAssertFactories.PATH)
            .hasFileName(datafiles.get(0).getStorageIdentifier());
    }
    
    @Test
    void createDataFiles_shouldNotUnpackGzippedFitsFileIfTooBigAfterUnpack() throws IOException {
        // given
        byte[] zipBytes = UnitTestUtils.readFileToByteArray("jhove/sample.fits.gz");

        when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(zipBytes.length + 1L);
        when(settingsService.getValueForKey(Key.FileFixityChecksumAlgorithm)).thenReturn("MD5");
        when(fileTypeDetector.determineFileType(any(), any())).thenReturn("application/fits-gzipped");

        // when
        List<DataFile> datafiles = dataFileCreator.createDataFiles(new ByteArrayInputStream(zipBytes), "sample.fits.gz", "application/fits-gzipped");
        
        // then
        assertThat(datafiles).hasSize(1)
            .satisfies(dataFile -> {
                assertThat(dataFile.getContentType()).isEqualTo("application/fits-gzipped");
                assertThat(dataFile.getChecksumType()).isEqualTo(ChecksumType.MD5);
                assertThat(dataFile.getChecksumValue()).isEqualTo("bb566e4c4afef02a279471c64e964602");
                assertThat(dataFile.getStorageIdentifier()).isNotEmpty();
                assertThat(dataFile.getFileMetadatas().get(0).getLabel()).isEqualTo("sample.fits.gz");
            }, atIndex(0));

        assertThat(Files.walk(tempDir).filter(Files::isRegularFile)).hasSize(1)
            .element(0, InstanceOfAssertFactories.PATH)
            .hasFileName(datafiles.get(0).getStorageIdentifier());
    }
    
    @Test
    void createDataFiles_shouldUnpackZipFile() throws IOException {
        // given
        byte[] zipBytes = UnitTestUtils.readFileToByteArray("jhove/archive.zip");

        lenient().when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(1024*1024L);
        lenient().when(settingsService.getValueForKey(Key.FileFixityChecksumAlgorithm)).thenReturn("MD5");
        lenient().when(settingsService.getValueForKeyAsLong(Key.ZipUploadFilesLimit)).thenReturn(1000L);
        when(fileTypeDetector.determineFileType(any(), any())).thenReturn("application/zip");
        when(fileTypeDetector.determineFileType(any(), eq("plaintext_ascii.txt"))).thenReturn("text/plain");
        when(fileTypeDetector.determineFileType(any(), eq("plaintext_utf8.txt"))).thenReturn("text/plain");

        // when
        List<DataFile> datafiles = dataFileCreator.createDataFiles(new ByteArrayInputStream(zipBytes), "archive.zip", "application/zip");
        
        // then
        assertThat(datafiles).hasSize(2)
            .satisfies(dataFile -> {
                assertThat(dataFile.getContentType()).isEqualTo("text/plain");
                assertThat(dataFile.getChecksumType()).isEqualTo(ChecksumType.MD5);
                assertThat(dataFile.getChecksumValue()).isEqualTo("0cc175b9c0f1b6a831c399e269772661");
                assertThat(dataFile.getStorageIdentifier()).isNotEmpty();
                assertThat(dataFile.getFileMetadatas().get(0).getLabel()).isEqualTo("plaintext_ascii.txt");
            }, atIndex(0))
            .satisfies(dataFile -> {
                assertThat(dataFile.getContentType()).isEqualTo("text/plain");
                assertThat(dataFile.getChecksumType()).isEqualTo(ChecksumType.MD5);
                assertThat(dataFile.getChecksumValue()).isEqualTo("d5c256b294dabd02bd496a2b1de99bd2");
                assertThat(dataFile.getStorageIdentifier()).isNotEmpty();
                assertThat(dataFile.getFileMetadatas().get(0).getLabel()).isEqualTo("plaintext_utf8.txt");
            }, atIndex(1));

        assertThat(Files.walk(tempDir).filter(Files::isRegularFile))
            .extracting(f -> f.getFileName().toString())
            .containsExactlyInAnyOrder(
                    datafiles.get(0).getStorageIdentifier(),
                    datafiles.get(1).getStorageIdentifier());
    }
    
    @Test
    void createDataFiles_shouldNotUnpackMacOsFilesystemFiles() throws IOException {
        // given
        byte[] zipBytes = UnitTestUtils.readFileToByteArray("jhove/archive_with_dirs_and_mac_fake_files.zip");

        lenient().when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(1024*1024L);
        lenient().when(settingsService.getValueForKey(Key.FileFixityChecksumAlgorithm)).thenReturn("MD5");
        lenient().when(settingsService.getValueForKeyAsLong(Key.ZipUploadFilesLimit)).thenReturn(1000L);
        when(fileTypeDetector.determineFileType(any(), any())).thenReturn("application/zip");
        when(fileTypeDetector.determineFileType(any(), eq("plaintext_ascii.txt"))).thenReturn("text/plain");
        when(fileTypeDetector.determineFileType(any(), eq("plaintext_utf8.txt"))).thenReturn("text/plain");

        // when
        List<DataFile> datafiles = dataFileCreator.createDataFiles(new ByteArrayInputStream(zipBytes), "archive.zip", "application/zip");
        
        // then
        assertThat(datafiles).hasSize(2)
            .satisfies(dataFile -> {
                assertThat(dataFile.getFileMetadatas().get(0).getLabel()).isEqualTo("plaintext_utf8.txt");
                assertThat(dataFile.getFileMetadatas().get(0).getDirectoryLabel()).isEqualTo("folder1");
                assertThat(dataFile.getContentType()).isEqualTo("text/plain");
                assertThat(dataFile.getChecksumType()).isEqualTo(ChecksumType.MD5);
                assertThat(dataFile.getChecksumValue()).isEqualTo("d5c256b294dabd02bd496a2b1de99bd2");
                assertThat(dataFile.getStorageIdentifier()).isNotEmpty();
            }, atIndex(0))
            .satisfies(dataFile -> {
                assertThat(dataFile.getFileMetadatas().get(0).getLabel()).isEqualTo("plaintext_ascii.txt");
                assertThat(dataFile.getFileMetadatas().get(0).getDirectoryLabel()).isEqualTo("folder1");
                assertThat(dataFile.getContentType()).isEqualTo("text/plain");
                assertThat(dataFile.getChecksumType()).isEqualTo(ChecksumType.MD5);
                assertThat(dataFile.getChecksumValue()).isEqualTo("0cc175b9c0f1b6a831c399e269772661");
                assertThat(dataFile.getStorageIdentifier()).isNotEmpty();
            }, atIndex(1));

        assertThat(Files.walk(tempDir).filter(Files::isRegularFile))
            .extracting(f -> f.getFileName().toString())
            .containsExactlyInAnyOrder(
                    datafiles.get(0).getStorageIdentifier(),
                    datafiles.get(1).getStorageIdentifier());
    }
    
    @Test
    void createDataFiles_shouldPreserveDirectoryStructure() throws IOException {
        // given
        byte[] zipBytes = UnitTestUtils.readFileToByteArray("jhove/archive_with_dirs_and_mac_fake_files.zip");

        lenient().when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(1024*1024L);
        lenient().when(settingsService.getValueForKey(Key.FileFixityChecksumAlgorithm)).thenReturn("MD5");
        lenient().when(settingsService.getValueForKeyAsLong(Key.ZipUploadFilesLimit)).thenReturn(1000L);
        lenient().when(fileTypeDetector.determineFileType(any(), eq("archive.zip"))).thenReturn("application/zip");
        lenient().when(fileTypeDetector.determineFileType(any(), eq("plaintext_utf8.txt"))).thenReturn("text/plain");
        lenient().when(fileTypeDetector.determineFileType(any(), eq("plaintext_ascii.txt"))).thenReturn("text/plain");

        // when
        List<DataFile> datafiles = dataFileCreator.createDataFiles(new ByteArrayInputStream(zipBytes), "archive.zip", "application/zip");
        
        // then
        assertThat(datafiles).hasSize(2)
            .satisfies(dataFile -> {
                assertThat(dataFile.getFileMetadatas().get(0).getLabel()).isEqualTo("plaintext_utf8.txt");
                assertThat(dataFile.getFileMetadatas().get(0).getDirectoryLabel()).isEqualTo("folder1");
                assertThat(dataFile.getContentType()).isEqualTo("text/plain");
                assertThat(dataFile.getChecksumType()).isEqualTo(ChecksumType.MD5);
                assertThat(dataFile.getChecksumValue()).isEqualTo("d5c256b294dabd02bd496a2b1de99bd2");
                assertThat(dataFile.getStorageIdentifier()).isNotEmpty();
            }, atIndex(0))
            .satisfies(dataFile -> {
                assertThat(dataFile.getFileMetadatas().get(0).getLabel()).isEqualTo("plaintext_ascii.txt");
                assertThat(dataFile.getFileMetadatas().get(0).getDirectoryLabel()).isEqualTo("folder1");
                assertThat(dataFile.getContentType()).isEqualTo("text/plain");
                assertThat(dataFile.getChecksumType()).isEqualTo(ChecksumType.MD5);
                assertThat(dataFile.getChecksumValue()).isEqualTo("0cc175b9c0f1b6a831c399e269772661");
                assertThat(dataFile.getStorageIdentifier()).isNotEmpty();
            }, atIndex(1));

        assertThat(Files.walk(tempDir).filter(Files::isRegularFile))
            .extracting(f -> f.getFileName().toString())
            .containsExactlyInAnyOrder(
                    datafiles.get(0).getStorageIdentifier(),
                    datafiles.get(1).getStorageIdentifier());
    }
    
    @Test
    void createDataFiles_shouldNotUnpackZipFileIfTooManyFilesToUnpack() throws IOException {
        // given
        byte[] zipBytes = UnitTestUtils.readFileToByteArray("jhove/archive.zip");

        lenient().when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(1024*1024L);
        lenient().when(settingsService.getValueForKey(Key.FileFixityChecksumAlgorithm)).thenReturn("MD5");
        lenient().when(settingsService.getValueForKeyAsLong(Key.ZipUploadFilesLimit)).thenReturn(1L);
        lenient().when(fileTypeDetector.determineFileType(any(), eq("archive.zip"))).thenReturn("application/zip");
        lenient().when(fileTypeDetector.determineFileType(any(), eq("plaintext_utf8.txt"))).thenReturn("text/plain");
        lenient().when(fileTypeDetector.determineFileType(any(), eq("plaintext_ascii.txt"))).thenReturn("text/plain");

        // when
        List<DataFile> datafiles = dataFileCreator.createDataFiles(new ByteArrayInputStream(zipBytes), "archive.zip", "application/zip");
        
        // then
        assertThat(datafiles).hasSize(1)
            .satisfies(dataFile -> {
                assertThat(dataFile.getFileMetadatas().get(0).getLabel()).isEqualTo("archive.zip");
                assertThat(dataFile.getContentType()).isEqualTo("application/zip");
                assertThat(dataFile.getChecksumType()).isEqualTo(ChecksumType.MD5);
                assertThat(dataFile.getChecksumValue()).isEqualTo("6246a24606128a4f34ae799d1e0d457d");
                assertThat(dataFile.getStorageIdentifier()).isNotEmpty();
                assertThat(dataFile.getIngestReport().getErrorKey()).isEqualTo(IngestError.UNZIP_FILE_LIMIT_FAIL);
            }, atIndex(0));

        assertThat(Files.walk(tempDir).filter(Files::isRegularFile))
            .extracting(f -> f.getFileName().toString())
            .contains(datafiles.get(0).getStorageIdentifier());
    }
    
    @Test
    void createDataFiles_shouldNotUnpackZipFileIfContainsTooBigFile() throws IOException {
        // given
        byte[] zipBytes = UnitTestUtils.readFileToByteArray("jhove/archive_single_file.zip");

        lenient().when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(zipBytes.length + 1L);
        lenient().when(settingsService.getValueForKey(Key.FileFixityChecksumAlgorithm)).thenReturn("MD5");
        lenient().when(settingsService.getValueForKeyAsLong(Key.ZipUploadFilesLimit)).thenReturn(1000L);
        when(fileTypeDetector.determineFileType(any(), any())).thenReturn("application/zip");

        // when
        List<DataFile> datafiles = dataFileCreator.createDataFiles(new ByteArrayInputStream(zipBytes), "archive.zip", "application/zip");
        
        // then
        assertThat(datafiles).hasSize(1)
            .satisfies(dataFile -> {
                assertThat(dataFile.getFileMetadatas().get(0).getLabel()).isEqualTo("archive.zip");
                assertThat(dataFile.getContentType()).isEqualTo("application/zip");
                assertThat(dataFile.getChecksumType()).isEqualTo(ChecksumType.MD5);
                assertThat(dataFile.getChecksumValue()).isEqualTo("a1e42cad3947efd6e5d10bdb43e8e074");
                assertThat(dataFile.getStorageIdentifier()).isNotEmpty();
                assertThat(dataFile.getIngestReport().getErrorKey()).isEqualTo(IngestError.UNZIP_SIZE_FAIL);
            }, atIndex(0));
        
        assertThat(Files.walk(tempDir).filter(Files::isRegularFile)).hasSize(1)
            .element(0, InstanceOfAssertFactories.PATH)
            .hasFileName(datafiles.get(0).getStorageIdentifier());
    }
    
    @Test
    void createDataFiles_shouldRezipShapefiles() throws IOException {
        // given
        byte[] zipBytes = UnitTestUtils.readFileToByteArray("jhove/fake_shapefile.zip");
        
        lenient().when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(1024*1024L);
        lenient().when(settingsService.getValueForKey(Key.FileFixityChecksumAlgorithm)).thenReturn("MD5");
        lenient().when(fileTypeDetector.determineFileType(any(), any())).thenReturn("application/zipped-shapefile");
        
        // when
        List<DataFile> datafiles = dataFileCreator.createDataFiles(new ByteArrayInputStream(zipBytes), "fake_shapefile.zip", "application/zipped-shapefile");
        // then
        assertThat(datafiles).hasSize(5).extracting(df -> df.getLatestFileMetadata().getLabel())
            .containsExactlyInAnyOrder("shape1.zip", "shape2.zip", "shape2", "shape2.txt", "README.txt");
    }
    
    @Test
    void createDataFiles_shouldCalculateUncompressedSize() throws IOException {
        // given
        lenient().when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(1024*1024L);
        lenient().when(settingsService.getValueForKey(Key.FileFixityChecksumAlgorithm)).thenReturn("MD5");
        lenient().when(fileTypeDetector.determineFileType(any(), any())).thenReturn("application/something");
        when(uncompressedCalculator.calculateUncompressedSize(any(), any(), any())).thenReturn(101L);
        
        // when
        List<DataFile> datafiles = dataFileCreator.createDataFiles(new ByteArrayInputStream(new byte[0]), "name", "application/something");
        // then
        assertThat(datafiles).hasSize(1);
        assertThat(datafiles.get(0).getUncompressedSize()).isEqualTo(101L);
    }
}
