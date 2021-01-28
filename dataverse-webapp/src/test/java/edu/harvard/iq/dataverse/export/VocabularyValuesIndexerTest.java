package edu.harvard.iq.dataverse.export;


import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class VocabularyValuesIndexerTest {

    VocabularyValuesIndexer indexer = new VocabularyValuesIndexer();

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should create index of controlled vocabulary values found in dataset version")
    void indexTest() {

        // given
        String vocabularyType1 = "collectionMode";
        String vocabularyType2 = "dataSourceType";

        HashMap<String, List<String>> vocabularyData = new HashMap<>();
        vocabularyData.put(vocabularyType1, Arrays.asList("focusgroup", "recording", "other"));
        vocabularyData.put(vocabularyType2, Arrays.asList("processes.workflows", "eventsinteractions"));
        DatasetVersion datasetVersion = createDatasetVersionWithGivenVocabularies("socialscience", vocabularyData);

        // when
        Map<String, Map<String, String>> index = indexer.indexLocalizedNamesOfUsedKeysByTypeAndValue(
                datasetVersion, Locale.ENGLISH);

        // then
        assertThat(index.keySet()).containsExactlyInAnyOrder(vocabularyType1, vocabularyType2);

        assertThat(index.get(vocabularyType1)).hasSize(3);
        assertThat(index.get(vocabularyType1).get("focusgroup")).isEqualTo("Focus group");
        assertThat(index.get(vocabularyType1).get("recording")).isEqualTo("Recording");
        assertThat(index.get(vocabularyType1).get("other")).isEqualTo("Other");

        assertThat(index.get(vocabularyType2)).hasSize(2);
        assertThat(index.get(vocabularyType2).get("processes.workflows")).isEqualTo("Processes: Workflow(s)");
        assertThat(index.get(vocabularyType2).get("eventsinteractions")).isEqualTo("Events/Interactions");
    }

    // -------------------- PRIVATE --------------------

    private DatasetVersion createDatasetVersionWithGivenVocabularies(String metadataBlockName,
                                                                     Map<String, List<String>> vocabularyData) {
        DatasetVersion datasetVersion = new DatasetVersion();
        MetadataBlock metadataBlock = new MetadataBlock();
        metadataBlock.setName(metadataBlockName);
        Random idGenerator = new Random();

        for (Map.Entry<String, List<String>> entry : vocabularyData.entrySet()) {
            DatasetFieldType type = new DatasetFieldType(entry.getKey(), FieldType.TEXT, true);
            type.setMetadataBlock(metadataBlock);
            Set<ControlledVocabularyValue> values = entry.getValue().stream()
                    .map(v -> new ControlledVocabularyValue(Math.abs(idGenerator.nextLong()), v, type))
                    .collect(Collectors.toSet());
            DatasetField field = new DatasetField();
            field.getControlledVocabularyValues().addAll(values);
            datasetVersion.getDatasetFields().add(field);
        }

        return datasetVersion;
    }
}