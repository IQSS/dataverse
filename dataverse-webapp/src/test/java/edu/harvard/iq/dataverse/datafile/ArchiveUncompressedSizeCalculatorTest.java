package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class ArchiveUncompressedSizeCalculatorTest {

    @InjectMocks
    private ArchiveUncompressedSizeCalculator uncompressedSizeCalculator;

    @Mock
    private SettingsServiceBean settingsService;

    @TempDir
    File tempDir;

    // -------------------- TESTS --------------------

    @Test
    void createDataFiles_shouldComputeUncompressedSizeForZipFile() throws IOException {
        // given
        Path filePath = UnitTestUtils.copyFileFromClasspath("jhove/archive.zip", tempDir.toPath().resolve("file"));
        // when
        Long uncompressedSize = uncompressedSizeCalculator.calculateUncompressedSize(filePath, "application/zip", "archive.zip");
        // then
        assertThat(uncompressedSize).isEqualTo(4L);
    }

    @Test
    void createDataFiles_shouldComputeUncompressedSizeForRarFile() throws IOException {
        // given
        Path filePath = UnitTestUtils.copyFileFromClasspath("jhove/archive.rar", tempDir.toPath().resolve("file"));
        // when
        Long uncompressedSize = uncompressedSizeCalculator.calculateUncompressedSize(filePath, "application/vnd.rar", "archive.rar");
        // then
        assertThat(uncompressedSize).isEqualTo(4L);
    }

    @Test
    void createDataFiles_shouldComputeUncompressedSizeFor7zFile() throws IOException {
        // given
        Path filePath = UnitTestUtils.copyFileFromClasspath("jhove/archive.7z", tempDir.toPath().resolve("file"));
        // when
        Long uncompressedSize = uncompressedSizeCalculator.calculateUncompressedSize(filePath, "application/x-7z-compressed", "archive.7z");
        // then
        assertThat(uncompressedSize).isEqualTo(4L);
    }

    @Test
    void createDataFiles_shouldComputeUncompressedSizeForGzFile() throws IOException {
        // given
        Path filePath = UnitTestUtils.copyFileFromClasspath("jhove/dummy.pdf.gz", tempDir.toPath().resolve("file"));

        lenient().when(settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.GzipMaxInputFileSizeInBytes)).thenReturn(1024*1024L);
        lenient().when(settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.GzipMaxOutputFileSizeInBytes)).thenReturn(1024*1024L);

        // when
        Long uncompressedSize = uncompressedSizeCalculator.calculateUncompressedSize(filePath, "application/gzip", "dummy.pdf.gz");
        // then
        assertThat(uncompressedSize).isEqualTo(13264L);
    }

  
  @Test
  void createDataFiles_shouldNotComputeUncompressedSizeForGzFileIfOutputFileIsTooBig() throws IOException {
      // given
      Path filePath = UnitTestUtils.copyFileFromClasspath("jhove/dummy.pdf.gz", tempDir.toPath().resolve("file"));

      lenient().when(settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.GzipMaxInputFileSizeInBytes)).thenReturn(1024*1024L);
      lenient().when(settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.GzipMaxOutputFileSizeInBytes)).thenReturn(1L);

      // when
      Long uncompressedSize = uncompressedSizeCalculator.calculateUncompressedSize(filePath, "application/gzip", "dummy.pdf.gz");
      // then
      assertThat(uncompressedSize).isEqualTo(0L);
  }
    
    @Test
    void createDataFiles_shouldNotComputeUncompressedSizeForGzFileIfItIsTooBig() throws IOException {
        // given
        Path filePath = UnitTestUtils.copyFileFromClasspath("jhove/dummy.pdf.gz", tempDir.toPath().resolve("file"));

        lenient().when(settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.GzipMaxInputFileSizeInBytes)).thenReturn(1L);
        lenient().when(settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.GzipMaxOutputFileSizeInBytes)).thenReturn(1024*1024L);
        
        // when
        Long uncompressedSize = uncompressedSizeCalculator.calculateUncompressedSize(filePath, "application/gzip", "dummy.pdf.gz");
        // then
        assertThat(uncompressedSize).isEqualTo(0L);
    }

}
