package edu.harvard.iq.dataverse.validation.datasetfield.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.datasetfield.FieldValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class GeoboxValueValidatorTest {

    private FieldValidator validator = new GeoboxValueValidator();

    private GeoboxTestUtil geoboxUtil = new GeoboxTestUtil();

    @ParameterizedTest(name = "[{index}] It''s {2} that value {1} of {0} is valid")
    @CsvSource(delimiter = '|' , value = {
        // Test |       Value | Expected
            "X1 |        abcd |    false",
            "Y1 |        -1e1 |    false",
            "X2 |        0x11 |    false",
            "Y2 |       11aab |    false",
            "X1 |          17 |     true",
            "X2 |        -180 |     true",
            "X1 |           0 |     true",
            "X2 |     179.999 |     true",
            "X1 |       180.1 |    false",
            "X2 |     -201.32 |    false",
            "Y1 | 89.99999999 |     true",
            "Y2 |     -18.321 |     true",
            "Y1 |           0 |     true",
            "Y2 | 90.00000001 |    false",
            "Y1 |     -100.12 |    false"
    })
    void isValid(GeoboxFields field, String value, boolean expected) {
        // given
        DatasetField datasetField = geoboxUtil.buildSingle(field, value);

        // when
        ValidationResult result = validator.isValid(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.isOk()).isEqualTo(expected);
    }
}