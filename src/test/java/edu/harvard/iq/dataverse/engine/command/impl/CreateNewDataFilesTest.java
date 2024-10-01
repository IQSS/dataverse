package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static edu.harvard.iq.dataverse.DataFile.ChecksumType.MD5;
import static org.apache.commons.io.file.FilesUncheck.createDirectories;
import static org.apache.commons.io.file.PathUtils.deleteDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;


@LocalJvmSettings
public class CreateNewDataFilesTest {
    // TODO keep constants for annotations in sync with class name
    Path testDir = Path.of("target/test/").resolve(getClass().getSimpleName());
    PrintStream original_stderr;

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

        mockTmpLookup();
        var cmd = createCmd("scripts/search/data/shape/shapefile.zip", mockDatasetVersion(), 1000L, 500L);
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

        mockTmpLookup();
        var cmd = createCmd("scripts/search/data/binary/3files.zip", mockDatasetVersion(), 1000L, 500L);
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

        mockTmpLookup();
        var cmd = createCmd("src/test/resources/own-cloud-downloads/greetings.zip", mockDatasetVersion(), 1000L, 500L);
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

        mockTmpLookup();
        var cmd = createCmd("src/test/resources/own-cloud-downloads/shapes.zip", mockDatasetVersion(), 1000L, 500L);
        var ctxt = mockCommandContext(mockSysConfig(false, 100000000L, MD5, 10));
        try (var mockedJHoveFileType = Mockito.mockStatic(JhoveFileType.class)) {
            mockedJHoveFileType.when(JhoveFileType::getJhoveConfigFile).thenReturn("conf/jhove/jhove.conf");

            // the test
            var result = cmd.execute(ctxt);

            assertThat(result.getErrors()).hasSize(0);
            assertThat(result.getDataFiles().stream().map(dataFile ->
                (dataFile.getFileMetadata().getDirectoryLabel() + "/" + dataFile.getDisplayName())
                    .replaceAll(".*temp/shp_[-0-9]*/", "")
            )).containsExactlyInAnyOrder(
                "dataDir/shape1.zip",
                "dataDir/shape2/shape2",
                "dataDir/shape2/shape2.pdf",
                "dataDir/shape2/shape2.txt",
                "dataDir/shape2/shape2.zip",
                "dataDir/extra/shp_dictionary.xls",
                "dataDir/extra/notes",
                "dataDir/extra/README.MD"
            );
            var storageIds = result.getDataFiles().stream().map(DataFile::getStorageIdentifier).toList();
            assertThat(tempDir.toFile().list())
                .containsExactlyInAnyOrderElementsOf(storageIds);
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

    private static void mockTmpLookup() {
        JvmSettings mockFilesDirectory = Mockito.mock(JvmSettings.class);
        Mockito.when(mockFilesDirectory.lookup()).thenReturn("/mocked/path");
    }

    private static @NotNull DatasetVersion mockDatasetVersion() {
        var dsVersion = Mockito.mock(DatasetVersion.class);
        Mockito.when(dsVersion.getDataset()).thenReturn(Mockito.mock(Dataset.class));
        return dsVersion;
    }

}
