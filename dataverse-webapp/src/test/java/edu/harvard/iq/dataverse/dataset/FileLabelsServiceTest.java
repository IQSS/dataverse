package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.api.dto.FileLabelsChangeOptionsDTO;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FileLabelsServiceTest {

    @Mock
    private DatasetVersionRepository datasetVersionRepository;

    @InjectMocks
    private FileLabelsService service;

    // -------------------- TESTS --------------------

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // "Pattern | Ids to exclude | Ids to include | Expected ids in result"
            "         * |                |                |          1,2,3,4,5,6,7",
            "      file |                |                |                1,2,6,7",
            "     file* |                |                |                    1,2",
            "     file* |                |            3,4 |                1,2,3,4",
            "     *abc* |                |                |                  3,4,5",
            "     *abc* |            3,4 |                |                      5",
    })
    void prepareFileLabels(String pattern, String excludeIds, String includeIds, String expectedAffected) {
        // given
        FileLabelsChangeOptionsDTO options = new FileLabelsChangeOptionsDTO();
        options.setPattern(pattern);
        options.setFilesToIncludeIds(extractToList(includeIds));
        options.setFilesToExcludeIds(extractToList(excludeIds));
        Dataset dataset = createDatasetWithFileLabels(createTestFileList());

        // when
        List<FileLabelInfo> result = service.prepareFileLabels(dataset, options);

        // then
        assertThat(result.stream()
                    .filter(FileLabelInfo::isAffected)
                    .map(FileLabelInfo::getId)
                    .collect(Collectors.toList()))
                .isEqualTo(extractToList(expectedAffected));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            //   From |   To | Expected changed labels
            "       a |    b | bbc-1.csv, bbc-2.csv",
            "       ^ | old_ | old_file2.jpg, old_abc-1.csv, old_abc-2.csv, old_123file.xls, old_321file.xls",
            "      .$ |    Z | file2.jpZ, abc-1.csZ, abc-2.csZ, 123file.xlZ, 321file.xlZ",
            "      '' |   '' | ''",
            "-?[0-9]* |   '' | file_1.jpg, abc_1.csv, abc_2.csv, file.xls, file_1.xls" // Duplicates are handled
    })
    void changeLabels(String from, String to, String expectedLabels) {
        // given
        FileLabelsChangeOptionsDTO options = new FileLabelsChangeOptionsDTO();
        options.setFrom(from);
        options.setTo(to);
        List<FileLabelInfo> input = createTestFileList().stream()
                .map(e -> new FileLabelInfo(e._1(), e._2(), e._2().matches(".*[0-9].*")))
                .collect(Collectors.toList());

        // when
        List<FileLabelInfo> result = service.changeLabels(input, options);

        // then
        assertThat(result.stream()
                    .filter(FileLabelInfo::isAffected)
                    .map(FileLabelInfo::getLabelAfterChange)
                    .collect(Collectors.toList()))
                .isEqualTo(extractToList(expectedLabels, e -> e));
    }

    @Test
    void updateDataset() {
        // given
        Dataset dataset = createDatasetWithFileLabels(createTestFileList());
        List<FileLabelInfo> input = createTestFileList().stream()
                .map(e -> new FileLabelInfo(e._1(), e._2(),
                        e._1().equals(7L) ? "qwerty.txt" : null, e._1().equals(7L)))
                .collect(Collectors.toList());

        // when
        service.updateDataset(dataset, input, new FileLabelsChangeOptionsDTO());

        // then
        List<FileMetadata> resultFileMetadatas = dataset.getLatestVersion().getFileMetadatas();
        assertThat(resultFileMetadatas)
                .extracting(FileMetadata::getLabel)
                .contains("qwerty.txt");
        assertThat(resultFileMetadatas)
                .extracting(FileMetadata::getLabel)
                .doesNotContain("321file.xls");

        Mockito.verify(datasetVersionRepository, Mockito.times(1)).save(Mockito.any());
    }

    // -------------------- PRIVATE --------------------

    private List<Tuple2<Long, String>> createTestFileList() {
        List<Tuple2<Long, String>> result = new ArrayList<>();
        String[] labels = {
                "file.jpg",
                "file2.jpg",
                "abc.csv",
                "abc-1.csv",
                "abc-2.csv",
                "123file.xls",
                "321file.xls"
        };
        long id = 1L;
        for (String label : labels) {
            result.add(Tuple.of(id, label));
            id++;
        }
        return result;
    }

    private List<Long> extractToList(String stringizedList) {
        return extractToList(stringizedList, Long::valueOf);
    }

    private <T> List<T> extractToList(String stringizedList, Function<String, T> mapper) {
        return StringUtils.isNotBlank(stringizedList)
                ? Stream.of(stringizedList
                    .split(","))
                    .map(String::trim)
                    .map(mapper)
                    .collect(Collectors.toList())
                : Collections.emptyList();
    }

    private Dataset createDatasetWithFileLabels(List<Tuple2<Long, String>> labels) {
        Dataset result = new Dataset();
        DatasetVersion editVersion = result.getEditVersion();
        List<FileMetadata> fileMetadatas = editVersion.getFileMetadatas();
        for (Tuple2<Long, String> label : labels) {
            FileMetadata metadata = new FileMetadata();
            metadata.setLabel(label._2());
            DataFile dataFile = new DataFile();
            dataFile.setId(label._1());
            metadata.setDataFile(dataFile);
            fileMetadatas.add(metadata);
        }
        return result;
    }
}