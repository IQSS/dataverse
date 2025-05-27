package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.storageuse.UploadSessionQuotaLimit;
import edu.harvard.iq.dataverse.util.JhoveFileType;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static edu.harvard.iq.dataverse.DataFile.ChecksumType.MD5;
import static org.apache.commons.io.file.FilesUncheck.createDirectories;
import static org.apache.commons.io.file.PathUtils.deleteDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;


@LocalJvmSettings
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CreateNewDataFilesTest {
    // TODO keep constants for annotations in sync with class name
    Path testDir = Path.of("target/test/").resolve(getClass().getSimpleName());
    PrintStream original_stderr;

    @Mock
    Dataset mockDataset;
    @Mock
    DatasetVersion mockDatasetVersion;

    @BeforeEach
    public void setupMock() {
        Mockito.when(mockDataset.getId()).thenReturn(2L);
        Mockito.when(mockDataset.getEffectiveDatasetFileCountLimit()).thenReturn(1000);
        Mockito.when(mockDatasetVersion.getDataset()).thenReturn(mockDataset);
    }
    @BeforeEach
    public void cleanTmpDir() throws IOException {
        original_stderr = System.err;
        if(testDir.toFile().exists())
            deleteDirectory(testDir);
    }

    @AfterEach void restoreStderr() {
        System.setErr(original_stderr);
    }

    @Test
    @JvmSetting(key = JvmSettings.FILES_DIRECTORY, value = "target/test/CreateNewDataFilesTest/tmp")
    public void execute_fails_to_upload_when_tmp_does_not_exist() throws FileNotFoundException {

        var cmd = createCmd("scripts/search/data/shape/shapefile.zip", mockDatasetVersion, 1000L, 500L);
        cmd.setDatasetService(mockDatasetServiceBean());
        var ctxt = mockCommandContext(mockSysConfig(true, 0L, MD5, 10));

        assertThatThrownBy(() -> cmd.execute(ctxt))
            .isInstanceOf(CommandException.class)
            .hasMessageContaining("Failed to save the upload as a temp file (temp disk space?)")
            .hasRootCauseInstanceOf(NoSuchFileException.class)
            .getRootCause()
            .hasMessageStartingWith("target/test/CreateNewDataFilesTest/tmp/temp/tmp");
    }

    @Test
    @JvmSetting(key = JvmSettings.FILES_DIRECTORY, value = "target/test/CreateNewDataFilesTest/tmp")
    public void execute_fails_on_size_limit() throws Exception {
        createDirectories(Path.of("target/test/CreateNewDataFilesTest/tmp/temp"));

        var cmd = createCmd("scripts/search/data/binary/3files.zip", mockDatasetVersion, 1000L, 500L);
        cmd.setDatasetService(mockDatasetServiceBean());
        var ctxt = mockCommandContext(mockSysConfig(true, 50L, MD5, 0));
        try (var mockedStatic = Mockito.mockStatic(JhoveFileType.class)) {
            mockedStatic.when(JhoveFileType::getJhoveConfigFile).thenReturn("conf/jhove/jhove.conf");

            assertThatThrownBy(() -> cmd.execute(ctxt))
                .isInstanceOf(CommandException.class)
                .hasMessage("This file size (462 B) exceeds the size limit of 50 B.");
        }
    }

    @Test
    @JvmSetting(key = JvmSettings.FILES_DIRECTORY, value = "target/test/CreateNewDataFilesTest/tmp")
    public void execute_loads_individual_files_from_uploaded_zip() throws Exception {
        var tempDir = testDir.resolve("tmp/temp");
        createDirectories(tempDir);

        var cmd = createCmd("src/test/resources/own-cloud-downloads/greetings.zip", mockDatasetVersion, 1000L, 500L);
        cmd.setDatasetService(mockDatasetServiceBean());
        var ctxt = mockCommandContext(mockSysConfig(false, 1000000L, MD5, 10));
        try (MockedStatic<JhoveFileType> mockedStatic = Mockito.mockStatic(JhoveFileType.class)) {
            mockedStatic.when(JhoveFileType::getJhoveConfigFile).thenReturn("conf/jhove/jhove.conf");

            // the test
            var result = cmd.execute(ctxt);

            assertThat(result.getErrors()).hasSize(0);
            assertThat(result.getDataFiles().stream().map(dataFile ->
                dataFile.getFileMetadata().getDirectoryLabel() + "/" + dataFile.getDisplayName()
            )).containsExactlyInAnyOrder(
                "DD-1576/goodbye.txt", "DD-1576/hello.txt"
            );
            var storageIds = result.getDataFiles().stream().map(DataFile::getStorageIdentifier).toList();
            assertThat(tempDir.toFile().list())
                .containsExactlyInAnyOrderElementsOf(storageIds);
        }
    }

    @Test
    @JvmSetting(key = JvmSettings.FILES_DIRECTORY, value = "target/test/CreateNewDataFilesTest/tmp")
    public void execute_rezips_sets_of_shape_files_from_uploaded_zip() throws Exception {
        var tempDir = testDir.resolve("tmp/temp");
        createDirectories(tempDir);

        var cmd = createCmd("src/test/resources/own-cloud-downloads/shapes.zip", mockDatasetVersion, 1000L, 500L);
        cmd.setDatasetService(mockDatasetServiceBean());
        var ctxt = mockCommandContext(mockSysConfig(false, 100000000L, MD5, 10));
        try (var mockedJHoveFileType = Mockito.mockStatic(JhoveFileType.class)) {
            mockedJHoveFileType.when(JhoveFileType::getJhoveConfigFile).thenReturn("conf/jhove/jhove.conf");

            // the test
            var result = cmd.execute(ctxt);

            assertThat(result.getErrors()).hasSize(0);
            assertThat(result.getDataFiles().stream().map(dataFile ->
                (dataFile.getFileMetadata().getDirectoryLabel() + "/" + dataFile.getDisplayName())
                    .replaceAll(".*/dataDir/", "")
            )).containsExactlyInAnyOrder(
                "shape1.zip",
                "shape2/shape2",
                "shape2/shape2.pdf",
                "shape2/shape2.txt",
                "shape2/shape2.zip",
                "extra/shp_dictionary.xls",
                "extra/notes",
                "extra/README.MD"
            );
            var storageIds = result.getDataFiles().stream().map(DataFile::getStorageIdentifier).toList();
            assertThat(tempDir.toFile().list())
                .containsExactlyInAnyOrderElementsOf(storageIds);
        }
    }

    @Disabled("Too slow. Intended for manual execution.")
    @Test
    @JvmSetting(key = JvmSettings.FILES_DIRECTORY, value = "/tmp/test/CreateNewDataFilesTest/tmp")
    public void extract_zip_performance() throws Exception {
        /*
         Developed to test performance difference between the old implementation with ZipInputStream and the new ZipFile implementation.
         Play with numbers depending on:
         - the time you want to spend on this test
         - how much system stress you want to examine
        */
        var nrOfZipFiles = 20;
        var avgNrOfFilesPerZip = 300;
        var avgFileLength = 5000;

        var tmpUploadStorage = Path.of("/tmp/test/CreateNewDataFilesTest/tmp/temp");
        if(tmpUploadStorage.toFile().exists()) {
            deleteDirectory(tmpUploadStorage);
        }
        createDirectories(tmpUploadStorage); // temp in target would choke intellij

        var chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        var random = new SecureRandom();
        var totalNrOfFiles = 0;
        var totalFileSize = 0;
        var totalTime = 0L;
        var tmp = Path.of(Files.createTempDirectory(null).toString());
        var ctxt = mockCommandContext(mockSysConfig(false, 100000000L, MD5, 10000));
        try (var mockedJHoveFileType = Mockito.mockStatic(JhoveFileType.class)) {
            mockedJHoveFileType.when(JhoveFileType::getJhoveConfigFile).thenReturn("conf/jhove/jhove.conf");
            for (var zipNr = 1; zipNr <= nrOfZipFiles; zipNr++) {
                // build the zip
                var zip = tmp.resolve(zipNr + "-data.zip");
                var nrOfFilesInZip = random.nextInt(avgNrOfFilesPerZip * 2);
                try (var zipStream = new ZipOutputStream(new FileOutputStream(zip.toFile()))) {
                    for (var fileInZipNr = 1; fileInZipNr <= nrOfFilesInZip; fileInZipNr++) {
                        // build content for a file
                        var stringLength = random.nextInt(avgFileLength * 2 -5);
                        StringBuilder sb = new StringBuilder(stringLength);
                        for (int i = 1; i <= stringLength; i++) {// zero length causes buffer underflow
                            sb.append(chars.charAt(random.nextInt(chars.length())));
                        }
                        // add the file to the zip
                        zipStream.putNextEntry(new ZipEntry(fileInZipNr + ".txt"));
                        zipStream.write((sb.toString()).getBytes());
                        zipStream.closeEntry();
                        totalFileSize += stringLength;
                    }
                }

                // upload the zip
                var before = DateTime.now();
                @NotNull CreateNewDataFilesCommand cmd = createCmd(zip.toString(), mockDatasetVersion, 1000L, 500L);
                cmd.setDatasetService(mockDatasetServiceBean());
                var result = cmd
                    .execute(ctxt);
                totalTime += DateTime.now().getMillis() - before.getMillis();

                assertThat(result.getErrors()).hasSize(0);
                assertThat(result.getDataFiles()).hasSize(nrOfFilesInZip);
                totalNrOfFiles += nrOfFilesInZip;

                // report after each zip to have some data even when aborting a test that takes too long
                System.out.println(MessageFormat.format(
                    "Total time: {0}ms; nr of zips {1} total nr of files {2}; total file size {3}",
                    totalTime, zipNr, totalNrOfFiles, totalFileSize
                ));
            }
            assertThat(tmpUploadStorage.toFile().list()).hasSize(totalNrOfFiles);
        }
    }

    private static @NotNull CreateNewDataFilesCommand createCmd(String name, DatasetVersion dsVersion, long allocatedQuotaLimit, long usedQuotaLimit) throws FileNotFoundException {
        return new CreateNewDataFilesCommand(
            Mockito.mock(DataverseRequest.class),
            dsVersion,
            new FileInputStream(name),
            "example.zip",
            "application/zip",
            null,
            new UploadSessionQuotaLimit(allocatedQuotaLimit, usedQuotaLimit),
            "sha");
    }

    private static @NotNull CommandContext mockCommandContext(SystemConfig sysCfg) {
        var ctxt = Mockito.mock(CommandContext.class);
        Mockito.when(ctxt.systemConfig()).thenReturn(sysCfg);
        return ctxt;
    }

    private static @NotNull SystemConfig mockSysConfig(boolean isStorageQuataEnforced, long maxFileUploadSizeForStore, DataFile.ChecksumType checksumType, int zipUploadFilesLimit) {
        var sysCfg = Mockito.mock(SystemConfig.class);
        Mockito.when(sysCfg.isStorageQuotasEnforced()).thenReturn(isStorageQuataEnforced);
        Mockito.when(sysCfg.getMaxFileUploadSizeForStore(any())).thenReturn(maxFileUploadSizeForStore);
        Mockito.when(sysCfg.getFileFixityChecksumAlgorithm()).thenReturn(checksumType);
        Mockito.when(sysCfg.getZipUploadFilesLimit()).thenReturn(zipUploadFilesLimit);
        return sysCfg;
    }

    private static @NotNull DatasetServiceBean mockDatasetServiceBean() {
        var datasetService = Mockito.mock(DatasetServiceBean.class);
        Mockito.when(datasetService.getDataFileCountByOwner(2L)).thenReturn(0L);
        return datasetService;
    }
}
