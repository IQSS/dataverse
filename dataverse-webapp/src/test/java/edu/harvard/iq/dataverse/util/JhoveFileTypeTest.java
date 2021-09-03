package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.UnitTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class JhoveFileTypeTest {

    private JhoveFileType jhove = new JhoveFileType();

    @TempDir
    File tempDir;


    @BeforeEach
    void before() throws IOException {
        new JhoveConfigurationInitializer().initializeJhoveConfig();
    }
    
    // -------------------- TESTS --------------------

    @ParameterizedTest
    @CsvSource({
        "images/coffeeshop.jpg, image/jpeg",
        "images/coffeeshop.tiff, image/tiff",
        "jhove/dummy.pdf, application/pdf",
        "jhove/plaintext_ascii.txt, text/plain; charset=US-ASCII",
        "jhove/plaintext_utf8.txt, text/plain; charset=UTF-8",
        "jhove/archive.zip, application/octet-stream",
        "jhove/empty, "})
    void getFileMimeType(String fileClasspath, String expectedFileType) throws IOException {
        // given
        File file = UnitTestUtils.copyFileFromClasspath(fileClasspath, tempDir.toPath().resolve("file")).toFile();
        // when & then
        assertThat(jhove.getFileMimeType(file)).isEqualTo(expectedFileType);
    }
}
