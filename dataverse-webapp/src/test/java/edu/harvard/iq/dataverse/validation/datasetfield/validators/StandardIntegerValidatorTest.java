package edu.harvard.iq.dataverse.validation.datasetfield.validators;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class StandardIntegerValidatorTest {
    StandardIntegerValidator validator = new StandardIntegerValidator();

    @ParameterizedTest
    @CsvSource(value = {
            "44,true",
            "-44,true",
            "44.125,false",
            "abcd,false",
    })
    void isValid(String value, boolean expectedResult) {
        // given
        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldType(new DatasetFieldType());
        datasetField.setValue(value);

        // when
        ValidationResult result =
                validator.isValid(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.isOk()).isEqualTo(expectedResult);
    }
}