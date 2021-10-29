package edu.harvard.iq.dataverse.validation.datasetfield.validators;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class StandardInputValidatorTest {
    private StandardInputValidator validator = new StandardInputValidator();

    @ParameterizedTest
    @CsvSource(value = {
            "value||true",
            "||true",
            "asdf|^[a-zA-Z ]{5,5}$|false",
            "as DF|^[a-zA-Z ]{5,5}$|true"
    }, delimiter = '|')
    void isValid(String value, String format, boolean expectedResult) {
        // given
        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldType(new DatasetFieldType());
        datasetField.setValue(value);

        // when
        ValidationResult result =
                validator.isValid(datasetField,
                        StringUtils.isNotBlank(format)
                                ? Collections.singletonMap("format", format)
                                : Collections.emptyMap(),
                        Collections.emptyMap());

        // then
        assertThat(result.isOk()).isEqualTo(expectedResult);
    }
}