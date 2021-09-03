package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.util.JhoveConfigurationInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class FileTypeDetectorTest {

    private FileTypeDetector fileTypeDetector = new FileTypeDetector();

    @TempDir
    File tempDir;

    @BeforeEach
    void before() throws IOException {
        new JhoveConfigurationInitializer().initializeJhoveConfig();
    }

    // -------------------- TESTS --------------------

    @ParameterizedTest
    @CsvSource({
        "dta/50by1000.dta, application/x-stata",
        "images/coffeeshop.jpg, image/jpeg",
        "images/coffeeshop.tiff, image/tiff",
        "images/coffeeshop.png, image/png",
        "jhove/dummy.pdf, application/pdf",
        "jhove/plaintext_ascii.txt, text/plain",
        "jhove/plaintext_utf8.txt, text/plain",
        "jhove/archive.zip, application/zip",
        "jhove/archive.rar, application/x-rar-compressed",
        "jhove/archive5.rar, application/x-rar-compressed",
        "jhove/dummy.pdf.gz, application/gzip",
        "jhove/sample.fits, application/fits",
        "jhove/sample.fits.gz, application/fits-gzipped",
        "jhove/fake_shapefile.zip, application/zipped-shapefile",
        "jhove/graphml.xml, text/xml-graphml",
        "jhove/empty, application/octet-stream"})
    public void determineFileType_based_on_file_content_only(String fileClasspath, String expectedFileType) throws IOException {
        // given
        File file = UnitTestUtils.copyFileFromClasspath(fileClasspath, tempDir.toPath().resolve("file")).toFile();
        // when & then
        assertThat(fileTypeDetector.determineFileType(file, "file")).isEqualTo(expectedFileType);
    }
    
    @ParameterizedTest
    @CsvSource({
        "f.dta, application/x-stata",
        "f.jpg, image/jpeg",
        "f.tiff, image/tiff",
        "f.png, image/png",
        "f.pdf, application/pdf",
        "f.txt, text/plain",
        "f.zip, application/zip",
        "f.rar, application/octet-stream",
        "f.pdf.gz, application/x-gzip",
        "f.fits, application/fits",
        "f.fits.gz, application/x-gzip",
        "f.zip, application/zip",
        "f.xml, text/xml",
        "f.do, text/x-stata-syntax",
        "f.sas, text/x-sas-syntax",
        "f.sps, text/x-spss-syntax",
        "f.csv, text/csv",
        "f.tsv, text/tsv"})
    public void determineFileType_based_on_filename_only(String filename, String expectedFileType) throws IOException {
        // given
        File file = UnitTestUtils.copyFileFromClasspath("jhove/empty", tempDir.toPath().resolve("file")).toFile();
        // when & then
        assertThat(fileTypeDetector.determineFileType(file, filename)).isEqualTo(expectedFileType);
    }

    @ParameterizedTest
    @CsvSource({
        "dta/50by1000.dta, f.dta, application/x-stata",
        "images/coffeeshop.jpg, f.jpg, image/jpeg",
        "images/coffeeshop.tiff, f.jpg, image/tiff",
        "images/coffeeshop.png, f.png, image/png",
        "jhove/dummy.pdf, f.pdf, application/pdf",
        "jhove/plaintext_ascii.txt, f.txt, text/plain",
        "jhove/plaintext_utf8.txt, f.txt, text/plain",
        "jhove/archive.zip, f.zip, application/zip",
        "jhove/archive.rar, f.rar, application/x-rar-compressed",
        "jhove/archive5.rar, f.rar, application/x-rar-compressed",
        "jhove/dummy.pdf.gz, f.pdf.gz, application/gzip",
        "jhove/sample.fits, f.fits, application/fits",
        "jhove/sample.fits.gz, f.fits.gz, application/fits-gzipped",
        "jhove/fake_shapefile.zip, f.zip, application/zipped-shapefile",
        "jhove/graphml.xml, f.xml, text/xml-graphml",})
    public void determineFileType_based_on_file_content_and_filename(
            String fileClasspath, String filename, String expectedFileType) throws IOException {

        // given
        File file = UnitTestUtils.copyFileFromClasspath(fileClasspath, tempDir.toPath().resolve("file")).toFile();
        // when & then
        assertThat(fileTypeDetector.determineFileType(file, filename)).isEqualTo(expectedFileType);
    }
}
