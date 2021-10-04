package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DataFileUploadInfoTest {

    private DataFileUploadInfo dataFileUploadInfo;

    @BeforeEach
    void setUp() {
        dataFileUploadInfo = new DataFileUploadInfo();
    }

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should allow to store and retrieve size for source file for datafiles")
    void storeAndRetrieveSize() {
        // given
        List<DataFile> dataFiles = createDataFilesWithStorageIds("123-456");

        // when
        dataFileUploadInfo.addSizeAndDataFiles(1234L, dataFiles);

        // then
        assertThat(dataFileUploadInfo.getSourceFileSize(dataFiles.get(0))).isEqualTo(1234L);
    }

    @Test
    @DisplayName("Should return 0 as a size of source file for datafiles not added to info object")
    void storeAndRetrieveSize__notAdded() {
        // given
        List<DataFile> dataFiles = createDataFilesWithStorageIds("123-456");

        // when & then
        assertThat(dataFileUploadInfo.getSourceFileSize(dataFiles.get(0))).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should signal that size of original size can be subtracted if all datafiles created from source file " +
            "were removed from upload")
    void allowSizeSubtraction() {
        // given
        List<DataFile> dataFiles = createDataFilesWithStorageIds("123-456", "234-567");

        dataFileUploadInfo.addSizeAndDataFiles(1234L, dataFiles);
        dataFileUploadInfo.removeFromDataFilesToSave(dataFiles.get(0));
        dataFileUploadInfo.removeFromDataFilesToSave(dataFiles.get(1));

        // when
        boolean canSubtractSize1 = dataFileUploadInfo.canSubtractSize(dataFiles.get(0));
        boolean canSubtractSize2 = dataFileUploadInfo.canSubtractSize(dataFiles.get(1));

        // then
        assertThat(canSubtractSize1).isTrue();
        assertThat(canSubtractSize2).isTrue();
    }

    @Test
    @DisplayName("Should signal that size of original size cannot be subtracted if not all datafiles created from source " +
            "file were removed from upload")
    void disallowSizeSubtraction() {
        // given
        List<DataFile> dataFiles = createDataFilesWithStorageIds("123-456", "234-567");

        dataFileUploadInfo.addSizeAndDataFiles(1234L, dataFiles);
        dataFileUploadInfo.removeFromDataFilesToSave(dataFiles.get(0));

        // when
        boolean canSubtractSize1 = dataFileUploadInfo.canSubtractSize(dataFiles.get(0));
        boolean canSubtractSize2 = dataFileUploadInfo.canSubtractSize(dataFiles.get(1));

        // then
        assertThat(canSubtractSize1).isFalse();
        assertThat(canSubtractSize2).isFalse();
    }

    // -------------------- PRIVATE --------------------

    private List<DataFile> createDataFilesWithStorageIds(String... storageIds) {
        return Arrays.stream(storageIds)
                .map(s -> {
                    DataFile dataFile = new DataFile();
                    dataFile.setStorageIdentifier(s);
                    return dataFile;
                })
                .collect(Collectors.toList());
    }
}