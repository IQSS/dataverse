package edu.harvard.iq.dataverse.validation.datasetfield.validators;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class StandardDateValidatorTest {

    private StandardDateValidator validator = new StandardDateValidator();

    @ParameterizedTest
    @CsvSource({
            "1999AD, false",
            "1999, true",
            "44BCE, false",
            "2004-10-27, true",
            "2002-08, true",
            "[1999?], false",
            "966, false",
            "0966, true",
            "1999-1, false",
            "1999-1-1, false",
            "1999-01-1, false",
            "-1999, true",
            "-999, false",
            "-0999, true",
            "-1991-01, true",
            "10000, false",
            "1999-13, false",
            "2020-02-29, true",
            "2019-02-29, false",
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