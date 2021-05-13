package edu.harvard.iq.dataverse.dataaccess;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ImageThumbConverterTest {

    private static final String STORAGE_MAIN_DIRECTORY = "/tmp/files";
    private static final Path DATASET_BASE_PATH = Paths.get(STORAGE_MAIN_DIRECTORY, "10.1010", "FK2", "ABCD");
    private static final String DATASET_STORAGE_ID = "file://10.1010/FK2/ABCD";
    private static final String DATAFILE_STORAGE_ID = "datafilestorageid";

    @InjectMocks
    private ImageThumbConverter imageThumbConverter;

    @Mock
    private SystemConfig systemConfig;

    private DataFile dataFile;


    @BeforeEach
    void beforeEach() throws IOException {
        FileUtils.deleteDirectory(new File(STORAGE_MAIN_DIRECTORY));
        DATASET_BASE_PATH.toFile().mkdirs();

        Dataset dataset = new Dataset();
        dataset.setStorageIdentifier(DATASET_STORAGE_ID);

        dataFile = new DataFile();
        dataFile.setOwner(dataset);
        dataFile.setStorageIdentifier(DATAFILE_STORAGE_ID);
    }

    @AfterEach
    void afterEach() throws IOException {
        FileUtils.deleteDirectory(new File(STORAGE_MAIN_DIRECTORY));
    }

    // -------------------- TESTS --------------------

    @Test
    void isThumbnailAvailable() throws IOException {
        // given
        dataFile.setContentType("image/png");
        copyFromClasspath("images/coffeeshop.png", DATASET_BASE_PATH.resolve(DATAFILE_STORAGE_ID));

        // when
        boolean thumbnailAvailable = imageThumbConverter.isThumbnailAvailable(dataFile);

        // then
        assertThat(thumbnailAvailable).isTrue();
        assertThat(DATASET_BASE_PATH.resolve("datafilestorageid.thumb64")).hasBinaryContent(
                UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_64.png"));
    }

    @Test
    void isThumbnailAvailable_different_size() throws IOException {
        // given
        dataFile.setContentType("image/png");
        copyFromClasspath("images/coffeeshop.png", DATASET_BASE_PATH.resolve(DATAFILE_STORAGE_ID));

        // when
        boolean thumbnailAvailable = imageThumbConverter.isThumbnailAvailable(dataFile, 48);

        // then
        assertThat(thumbnailAvailable).isTrue();
        assertThat(DATASET_BASE_PATH.resolve("datafilestorageid.thumb48")).hasBinaryContent(
                UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_48.png"));
    }

    @Test
    void isThumbnailAvailable__image_too_big() throws IOException {
        // given
        dataFile.setContentType("image/png");
        dataFile.setFilesize(543938);
        copyFromClasspath("images/coffeeshop.png", DATASET_BASE_PATH.resolve(DATAFILE_STORAGE_ID));
        Mockito.when(systemConfig.getThumbnailSizeLimitImage()).thenReturn(100L);

        // when
        boolean thumbnailAvailable = imageThumbConverter.isThumbnailAvailable(dataFile);

        // then
        assertThat(thumbnailAvailable).isFalse();
        assertThat(DATASET_BASE_PATH.resolve("datafilestorageid.thumb64")).doesNotExist();
    }

    @Test
    void isThumbnailAvailable__not_supported_content_type() throws IOException {
        // given
        dataFile.setContentType("text/plain");
        copyFromClasspath("images/sample.txt", DATASET_BASE_PATH.resolve(DATAFILE_STORAGE_ID));

        // when
        boolean thumbnailAvailable = imageThumbConverter.isThumbnailAvailable(dataFile);

        // then
        assertThat(thumbnailAvailable).isFalse();
        assertThat(DATASET_BASE_PATH.resolve("datafilestorageid.thumb64")).doesNotExist();
    }

    @Test
    void getImageThumbnailAsInputStream() throws IOException {
        // given
        dataFile.setContentType("image/png");
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setLabel("image.png");
        dataFile.setFileMetadatas(Lists.newArrayList(fileMetadata));

        copyFromClasspath("images/coffeeshop.png", DATASET_BASE_PATH.resolve(DATAFILE_STORAGE_ID));

        // when
        InputStreamIO thumbnailStreamIO = imageThumbConverter.getImageThumbnailAsInputStream(dataFile, 48);

        // then
        assertThat(thumbnailStreamIO.getInputStream()).hasBinaryContent(UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_48.png"));
        assertThat(thumbnailStreamIO.getFileName()).isEqualTo("image.png");
        assertThat(thumbnailStreamIO.getSize()).isEqualTo(4779);
        assertThat(thumbnailStreamIO.getMimeType()).isEqualTo("image/png");
        assertThat(DATASET_BASE_PATH.resolve("datafilestorageid.thumb48")).hasBinaryContent(
                UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_48.png"));
    }

    @Test
    void getImageThumbnailAsBase64() throws IOException {
        // given
        dataFile.setContentType("image/png");
        copyFromClasspath("images/coffeeshop.png", DATASET_BASE_PATH.resolve(DATAFILE_STORAGE_ID));

        // when
        String thumbnailBase64 = imageThumbConverter.getImageThumbnailAsBase64(dataFile, 48);

        // then
        assertThat(thumbnailBase64)
            .startsWith("data:image/png;base64,")
            .hasSizeGreaterThan("data:image/png;base64,".length());
        assertThat(DATASET_BASE_PATH.resolve("datafilestorageid.thumb48")).hasBinaryContent(
                UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_48.png"));
    }

    // -------------------- PRIVATE --------------------

    private void copyFromClasspath(String classpath, Path target) throws IOException {
        Files.write(target, UnitTestUtils.readFileToByteArray(classpath));
    }
}
