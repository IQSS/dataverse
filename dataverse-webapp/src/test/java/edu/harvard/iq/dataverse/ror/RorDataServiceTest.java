package edu.harvard.iq.dataverse.ror;

import edu.harvard.iq.dataverse.persistence.ror.RorData;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

class RorDataServiceTest {

    private RorConverter rorConverter;
    private RorTransactionsService rorTransactionsService;
    private RorDataService rorDataService;

    private File rorData;

    @BeforeEach
    void beforeEach() {
        rorConverter = Mockito.mock(RorConverter.class);
        rorTransactionsService = Mockito.mock(RorTransactionsService.class);
        rorDataService = new RorDataService(rorConverter, rorTransactionsService, 1);
    }

    @AfterEach
    void afterEach() throws IOException {
        Files.deleteIfExists(rorData.toPath());
    }

    @Test
    void refreshRorData() throws IOException {
        // given
        final File file = copyRorDataFromClasspath();

        // when
        Mockito.when(rorConverter.toEntity(Mockito.any())).thenReturn(new RorData());

        final RorDataService.UpdateResult updateResult = rorDataService.refreshRorData(file, FormDataContentDisposition
                .name("niceFile")
                .fileName(".json")
                .build());

        // then
        Assertions.assertThat(updateResult.getTotal()).isEqualTo(2);
        Mockito.verify(rorTransactionsService, Mockito.times(3)).saveMany(Mockito.any());
        Mockito.verify(rorTransactionsService, Mockito.times(1)).truncateAll();

    }

    private File copyRorDataFromClasspath() throws IOException {
        final File file = Paths.get("src", "test", "resources", "json", "ror", "rorData.json").toFile();
        final Path tempFile = Files.createTempFile("", "");
        Files.copy(file.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        rorData = tempFile.toFile();
        return rorData;
    }

}