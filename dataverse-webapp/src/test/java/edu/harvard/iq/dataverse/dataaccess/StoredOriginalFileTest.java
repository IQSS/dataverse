package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StoredOriginalFileTest {

    @Mock
    private StorageIO<DataFile> storageIO;

    private byte[] originalFileBytes = "original".getBytes();
    private ByteArrayInputStream originalFileInputStream = new ByteArrayInputStream(originalFileBytes);


    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should retreive storage content as saved in aux file")
    public void retreive() throws IOException {
        // given
        DataFile dataFile = createDataFileWithDataTable("text/csv", 1234L);

        when(storageIO.getDataFile()).thenReturn(dataFile);
        when(storageIO.getFileName()).thenReturn("ingested.tab");
        when(storageIO.getAuxFileAsInputStream(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION))
                .thenReturn(originalFileInputStream);

        // when
        StorageIO<DataFile> originalFileStorageIO = StoredOriginalFile.retreive(storageIO);

        // then
        assertThat(originalFileStorageIO.getInputStream()).hasBinaryContent(originalFileBytes);
    }

    @Test
    @DisplayName("Should retreive storage with assigned size from DataTable original size")
    public void retreive_file_size() throws IOException {
        // given
        DataFile dataFile = createDataFileWithDataTable("text/csv", 1234L);

        when(storageIO.getDataFile()).thenReturn(dataFile);
        when(storageIO.getFileName()).thenReturn("ingested.tab");
        when(storageIO.getAuxFileAsInputStream(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION))
                .thenReturn(originalFileInputStream);

        // when
        StorageIO<DataFile> originalFileStorageIO = StoredOriginalFile.retreive(storageIO);

        // then
        assertThat(originalFileStorageIO.getSize()).isEqualTo(1234);
    }

    @Test
    @DisplayName("Should retrive storage with assigned size from aux file size as fallback")
    public void retreive_file_size_fallback() throws IOException {
        // given
        DataFile dataFile = createDataFileWithDataTable("text/csv", null);

        when(storageIO.getDataFile()).thenReturn(dataFile);
        when(storageIO.getFileName()).thenReturn("ingested.tab");
        when(storageIO.getAuxFileAsInputStream(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION))
                .thenReturn(originalFileInputStream);
        when(storageIO.getAuxObjectSize(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION))
                .thenReturn(5555L);

        // when
        StorageIO<DataFile> originalFileStorageIO = StoredOriginalFile.retreive(storageIO);

        // then
        assertThat(originalFileStorageIO.getSize()).isEqualTo(5555);
    }

    @ParameterizedTest
    @DisplayName("Should correct mime type of original file")
    @CsvSource({
        "application/x-spss-sav, application/x-spss-sav",
        "application/x-dvn-somestring-zip, application/zip",
        ", application/x-unknown"})
    public void retreive_mime_type(String originalFileMimeType, String expectedFileMimeType) throws IOException {
        // given
        DataFile dataFile = createDataFileWithDataTable(originalFileMimeType, 1234L);

        when(storageIO.getDataFile()).thenReturn(dataFile);
        when(storageIO.getFileName()).thenReturn("ingested.tab");
        when(storageIO.getAuxFileAsInputStream(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION))
                .thenReturn(originalFileInputStream);

        // when
        StorageIO<DataFile> originalFileStorageIO = StoredOriginalFile.retreive(storageIO);

        // then
        assertThat(originalFileStorageIO.getMimeType()).isEqualTo(expectedFileMimeType);
    }

    @ParameterizedTest
    @DisplayName("Should generate filename of original file based on ingested filename and original mime type")
    @CsvSource({
        "ingested.tab, application/x-spss-sav, ingested.sav",
        "ingested.tab, application/x-spss-por, ingested.por",
        "ingested.tab, application/x-stata-14, ingested.dta",
        "ingested.tab, application/x-rlang-transport, ingested.RData",
        "ingested.tab, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, ingested.xlsx",
        "ingested.tab, text/csv, ingested.csv",
        "ingested.tab, text/tsv, ingested.tsv",
        "ingested, text/csv, ingested",
        "ingested.ztab, text/csv, ingested.ztab",
        "ingested.tab.bak, text/csv, ingested.tab.bak",
        "ingested.tab, , ingested",})
    public void retreive_filename(String fileName, String originalFileMimeType, String expectedFileName) throws IOException {
        // given
        DataFile dataFile = createDataFileWithDataTable(originalFileMimeType, 1234L);

        when(storageIO.getDataFile()).thenReturn(dataFile);
        when(storageIO.getFileName()).thenReturn(fileName);
        when(storageIO.getAuxFileAsInputStream(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION))
                .thenReturn(originalFileInputStream);

        // when
        StorageIO<DataFile> originalFileStorageIO = StoredOriginalFile.retreive(storageIO);

        // then
        assertThat(originalFileStorageIO.getFileName()).isEqualTo(expectedFileName);
    }

    // -------------------- PRIVATE --------------------

    private DataFile createDataFileWithDataTable(String originalFileFormat, Long originalFileSize) {
        DataFile dataFile = new DataFile();

        DataTable dataTable = new DataTable();
        dataTable.setOriginalFileFormat(originalFileFormat);
        dataTable.setOriginalFileSize(originalFileSize);
        dataFile.setDataTable(dataTable);

        return dataFile;
    }
}
