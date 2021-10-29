package edu.harvard.iq.dataverse.validation.datasetfield.validators;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class StandardUrlValidatorTest {
    StandardUrlValidator validator = new StandardUrlValidator();

    @ParameterizedTest
    @CsvSource(value = {
            "http://repod.icm.edu.pl,true",
            "mxrdr.icm.edu.pl,false"
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