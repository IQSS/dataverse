package edu.harvard.iq.dataverse.validation.datasetfield.validators;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class RorValidatorTest {

    private RorValidator validator = new RorValidator();

    @ParameterizedTest
    @CsvSource({
            "https://ror.org/01q8f6705, true",
            "https://ror.org/01pp8nd67, true",
            "https://ror.org/00kv9pj15, true",
            "https://ror.org/019wvm592, true",
            "https://ror.org/05cggb038, true",
            "https://ror.org/04xfq0f34, true",
            "https://ror.org/04xfq0f34, true",
            "https://ror.org/14xfq0f34, false",
            "https://ror.org/04XFQ0F34, false",
            ", false",
            "https://ror.org/ZENNEZ12345, false",
            "https://ror.org/112222233, false",
            "https://ror.org/qq12321pp, false",
            "https://ror.org/, false",
            "11abcde12, false"
    })
    void isValid(String value, boolean expectedResult) {
        // given
        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldType(new DatasetFieldType());
        datasetField.setValue(value);

        // when
        ValidationResult result = validator.isValid(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.isOk()).isEqualTo(expectedResult);
    }
}