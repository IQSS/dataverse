package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DuplicatesServiceTest {

    private DuplicatesService service = new DuplicatesService();

    // -------------------- TESTS --------------------

    @Test
    void listDuplicates() {
        // given
        List<DataFile> existingFiles = list(
                fileOf("1-1", "111"), fileOf("1-2", "111"),
                fileOf("2-1", "222"),
                fileOf("4-1", "444"), fileOf("4-2", "444"));
        List<DataFile> newFiles = list(
                fileOf("I-1", "111"),
                fileOf("II-1", "222"), fileOf("II-2", "222"),
                fileOf("III-1", "333"), fileOf("III-2", "333"));

        // when
        List<DuplicatesService.DuplicateGroup> result = service.listDuplicates(existingFiles, newFiles);

        // then
        assertThat(result)
                .extracting(DuplicatesService.DuplicateGroup::getExistingDuplicatesLabels,
                        r -> r.getDuplicates().stream()
                                .map(DuplicatesService.DuplicateItem::getLabel)
                                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder(
                        tuple(list("1-1", "1-2"), list("I-1")),
                        tuple(list("2-1"), list("II-1", "II-2")),
                        tuple(list(), list("III-1", "III-2")));
    }

    @Test
    void hasDuplicatesInUploadedFiles__noDuplicates() {
        // given
        List<DataFile> existing = list(fileOf("a-1", "aaa"), fileOf("a-2", "aaa"));
        List<DataFile> newFiles = list(fileOf("B-1", "bbb"), fileOf("C-1", "ccc"));

        // when
        boolean result = service.hasDuplicatesInUploadedFiles(existing, newFiles);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void hasDuplicatesInUploadedFiles__duplicatesWithExisting() {
        // given
        List<DataFile> existing = list(fileOf("c-1", "ccc"), fileOf("c-2", "ccc"));
        List<DataFile> newFiles = list(fileOf("A-1", "aaa"), fileOf("C-1", "ccc"));

        // when
        boolean result = service.hasDuplicatesInUploadedFiles(existing, newFiles);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void hasDuplicatesInUploadedFiles__duplicatesInNew() {
        // given
        List<DataFile> existing = list();
        List<DataFile> newFiles = list(fileOf("B-1", "bbb"), fileOf("B-2", "bbb"));

        // when
        boolean result = service.hasDuplicatesInUploadedFiles(existing, newFiles);

        // then
        assertThat(result).isTrue();
    }

    // -------------------- PRIVATE --------------------

    private DataFile fileOf(String name, String checksum) {
        DataFile dataFile = new DataFile();
        dataFile.setId(1L);
        dataFile.setChecksumValue(checksum);
        FileMetadata metadata = new FileMetadata();
        metadata.setLabel(name);
        dataFile.setFileMetadatas(Collections.singletonList(metadata));
        return dataFile;
    }

    private <T> List<T> list(T... elements) {
        return Arrays.stream(elements).collect(Collectors.toList());
    }
}