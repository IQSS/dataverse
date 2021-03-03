package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

import javax.enterprise.context.ApplicationScoped;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
class VocabularyValuesIndexer {

    // -------------------- LOGIC --------------------

    /**
     * Creates index of the localized (using the given {@link Locale})
     * vocabulary values that are used in the provided {@link DatasetVersion}.
     * <p>
     * Result is grouped by name of the dataset field, then by internal name
     * of the value.
     */
    Map<String, Map<String, String>> indexLocalizedNamesOfUsedKeysByTypeAndValue(DatasetVersion datasetVersion, Locale locale) {
        return datasetVersion.getDatasetFields().stream()
                .filter(f -> !f.getControlledVocabularyValues().isEmpty())
                .flatMap(f -> f.getControlledVocabularyValues().stream())
                .collect(Collectors.groupingBy(v -> v.getDatasetFieldType().getName(),
                        Collectors.toMap(ControlledVocabularyValue::getStrValue,
                                ControlledVocabularyValue::getLocaleStrValue,
                                (prev, next) -> next)));
    }
}
